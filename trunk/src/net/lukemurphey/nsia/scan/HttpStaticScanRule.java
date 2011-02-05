package net.lukemurphey.nsia.scan;

import java.security.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.sql.Timestamp; // NOPMD by luke on 5/26/07 11:19 AM

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;
import java.util.*;
import java.util.regex.*;
import org.apache.commons.codec.binary.*;


/**
 * This class performs scans to determine if an HTTP resource has changed. The scans are performed using hash results and thus
 * are highly sensitive to changes without necessitating storage of the entire HTTP resource. This class also contains static 
 * methods for adding and removing scan class rules from the persistant storage (database). 
 * @author luke
 *
 */
public class HttpStaticScanRule extends ScanRule {
	
	private int expectedResponseCode = -1;
	private String expectedDataHash = null;
	private String expectedDataHashAlgorithm = null;
	private boolean followRedirects = false; //Indicates whether to follow HTTP redirects
	private URL specimenUrl = null;
	private Vector<HttpHeaderRule> headerRules = null; //The list of header rules to be evaluated
	private Vector<Long> headerRulesAwaitingDeletion;
	
	public static final String RULE_TYPE = "HTTP/Static";
	
	/*The following parameter determines if a rule that is not specifically accepted should be rejected.
	 This value is invoked whenever a rule does not match a reject and does not match an accept (is thus ambiguous).*/
	private boolean defaultDenyHeaders = false; 
	
	//The following integers indicate of the rule must match, or must not match
	public static final int MUST_MATCH = 0;
	public static final int MUST_NOT_MATCH = 1;
	public static final int DEFAULT_MATCH = 2;
	
	//The following values indicate if a header matched a rule and what is the action associated with the matched rule 
	private static final int DOES_NOT_MATCH = 0;
	private static final int MATCH_REJECT = 1;
	private static final int MATCH_ACCEPT= 2;
	
	//Is used to indicate that a header rule did not pass
	//private static final long HEADER_RULE_DEFAULT = -3;
	//private static final long HEADER_RULE_FAIL = -2;
	private static final long HEADER_RULE_ID_NOT_SET = -1;
	
	//The following constants are used to indicate the type of a rule (string comparison or regular expression matching)
	
	
	public HttpStaticScanRule(Application appRes){
		super(appRes);
		headerRules = new Vector<HttpHeaderRule>();
		
		headerRulesAwaitingDeletion = new Vector<Long>();
	}
	
	public HttpStaticScanRule(Application appRes, int expectedResponseCode, String expectedDataHash, String expectedDataHashAlgorithm, boolean followRedirects, URL specimenUrl, int scanFrequency ) throws NoSuchAlgorithmException, InputValidationException{
		super(appRes);
		
		// 0 -- Precondition check
		
		//	 0.1 -- Check the response code
		if( expectedResponseCode < 0 )
			throw new InputValidationException("The response code must be a positive value", "ExpectedResponseCode", Integer.toString( expectedResponseCode) );
		
		//	 0.2 -- Check the data hash code
		if( expectedDataHash == null )
			throw new InputValidationException("Data hash cannot be null", "ExpectedDataHash", "null" );
		
		//	 0.3 -- Check the data hash algorithm code
		if( expectedDataHashAlgorithm == null )
			throw new InputValidationException("Data hash algorihtm cannot be null", "ExpectedDataAlgorithm", "null" );
		
		//	 0.4 -- Check specimen URL
		if( specimenUrl == null )
			throw new InputValidationException("URL cannot be null", "URL", "null" );
		else if( specimenUrl.getHost() == null || specimenUrl.getHost().length() == 0)
			throw new InputValidationException("URL must contain a host name", "URLHostname", specimenUrl.toString() );
		
		
		// 1 -- Initialize the class fields
		headerRulesAwaitingDeletion = new Vector<Long>();
		headerRules = new Vector<HttpHeaderRule>();
		
		
		// 2 -- Set the values of variables passed into the class
		setExpectedResponseCode( expectedResponseCode );
		setExpectedDataHash( expectedDataHashAlgorithm, expectedDataHash);
		setFollowRedirects( followRedirects );
		setUrl( specimenUrl );
		setScanFrequency(scanFrequency);
	}
	
	public String toString(){
		return "Scanner : HTTP/static (" + specimenUrl.toString() + ")";
	}
	
	public String getRuleType(){
		return HttpStaticScanRule.RULE_TYPE;
	}
	
	public String getSpecimenDescription(){
		return specimenUrl.toString();
	}
	
	/**
	 * Method determines the rule identifer that contains the given header rule.
	 * @param headerRuleId
	 * @return
	 * @throws SQLException 
	 * @throws NoDatabaseConnectionException 
	 * @throws NotFoundException 
	 */
	public static long resolveRuleId( long headerRuleId ) throws SQLException, NoDatabaseConnectionException, NotFoundException{
		
		// 0 -- Precondition check
		Application appRes = Application.getApplication();
		Connection connection = null;
		
		// 1 -- Get the rule associated with the header rule
		PreparedStatement statement = null;
		ResultSet result = null;
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
			
			if( connection == null )
				return -1;
			
			statement = connection.prepareStatement("Select * from HttpHeaderScanRule where HttpHeaderScanRuleID = ?");
			statement.setLong(1, headerRuleId );
			result = statement.executeQuery();
			
			if( !result.next() )
				throw new NotFoundException("A rule could not be found with the given identifier (" + headerRuleId + ")");
			else{
				return result.getLong("ScanRuleID");
			}
		}
		finally{
			if (statement != null )
				statement.close();
			
			if (result != null )
				result.close();
			
			if (connection != null )
				connection.close();
		}
	}
	
	/**
	 * Load the rule from the database that corresponds to the scan rule identifier given.
	 * @throws NoDatabaseConnectionException 
	 * @throws ScanRuleLoadFailureException 
	 */
	public boolean loadFromDatabase( long scanRuleId ) throws SQLException, NoDatabaseConnectionException, ScanRuleLoadFailureException{
		
		// 0 -- Precondition check
		Connection connection = null;
		
		
		// 1 -- Load the main attributes
		PreparedStatement statement = null;
		ResultSet result = null;
		PreparedStatement generalRuleStatement = null;
		ResultSet generalRuleResult = null;
		PreparedStatement headerStatement = null;
		ResultSet headerResults = null;
		
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
			if( connection == null )
				return false;
			
			// 1.1 -- Load the HTTP hash scan rule specific attributes
			statement = connection.prepareStatement("Select * from HttpHashScanRule where ScanRuleID = ?");
			statement.setLong(1, scanRuleId );
			result = statement.executeQuery();
			
			if( !result.next() )
				return false;
			
			defaultDenyHeaders = result.getBoolean("DefaultDenyHeaders");
			expectedDataHash = result.getString("HashData");
			expectedDataHashAlgorithm = result.getString("HashAlgorithm");
			expectedResponseCode = result.getInt("ResponseCode");
			try{
				specimenUrl = new URL( result.getString("LocationUrl") );
			}
			catch(MalformedURLException e){
				throw new ScanRuleLoadFailureException("The URL associated with the rule is invalid", e);
			}
			
			this.scanRuleId = scanRuleId;
			
			// 1.2 -- Load the general rule attributes
			generalRuleStatement = connection.prepareStatement("Select * from ScanRule where ScanRuleID = ?");
			generalRuleStatement.setLong(1, scanRuleId );
			generalRuleResult = generalRuleStatement.executeQuery();
			
			if( !generalRuleResult.next() )
				return false;
			
			int scanFrequency = generalRuleResult.getInt("ScanFrequency");
			this.scanFrequency = scanFrequency;
			this.created = generalRuleResult.getTimestamp("Created");
			this.modified = generalRuleResult.getTimestamp("Modified");
			
			// 1.3 -- Load the header rules
			headerStatement = connection.prepareStatement("Select * from HttpHeaderScanRule where ScanRuleID = ?");
			headerStatement.setLong(1, scanRuleId );
			headerResults = headerStatement.executeQuery();
			
			while( headerResults.next() ){
				HttpHeaderRule headerRule = HttpHeaderRule.getFromResultSet(headerResults);
				addHeaderRule(headerRule);
			}
			
		} finally {
			
			if (statement != null )
				statement.close();
			
			if (result != null )
				result.close();
			
			if (generalRuleStatement != null )
				generalRuleStatement.close();
			
			if (generalRuleResult != null )
				generalRuleResult.close();
			
			if (headerStatement != null )
				headerStatement.close();
			
			if (headerResults != null )
				headerResults.close();
			
			if (connection != null )
				connection.close();
		}
		
		
		return true;
		
		
	}
	
	/**
	 * Attempt to perform a scan of the HTTP resource specified. The scan result code will be set to unready of the class
	 * was not properly prepared.
	 * @precondition A URL, expected hash algorithm and expected response code must be set
	 * @postcondition A scan result indicating the results of the analysis will be returned
	 * @throws NoSuchAlgorithmException If the specified hash algorithm is not recognized or not supported.
	 */
	public ScanResult doScan() throws ScanException{ // NOPMD by luke on 5/26/07 11:20 AM
		
		// 1 -- Make sure the class is ready to do the analysis
		Timestamp timeOfScan = new Timestamp(System.currentTimeMillis());

		if( !isReady() )
			return new HttpStaticScanResult(ScanResultCode.UNREADY, timeOfScan);
		
		
		// 2 -- Attempt to connect to the URL
		HostConfiguration hostConfig = new HostConfiguration();
		
		//	 2.1 -- Make sure the parameters are set
		int port = 80;
		String protocol = "HTTP";
		
		if( specimenUrl.getPort() >= 0 )
			port = specimenUrl.getPort();
		
		if( specimenUrl.getProtocol() != null && specimenUrl.getProtocol() != "" )
			protocol = specimenUrl.getProtocol();
		
		//	 2.2 -- Setup the client
		hostConfig.setHost(specimenUrl.getHost(), port, protocol);
		HttpMethod httpMethod = new GetMethod(specimenUrl.toString());
		
		httpMethod.setFollowRedirects(followRedirects);
		HttpClient httpClient = new HttpClient();
		
		//	 2.3 -- Connect to the host
		try {
			httpClient.executeMethod( hostConfig, httpMethod );
		} catch (HttpException e) { // NOPMD by luke on 5/26/07 11:21 AM
			//The exception will be reflected in the response code which will be retrieved below.
			//We will continue even if an exception occurred since we want to analyze the data regardless of the server status code.
		} catch (IOException e) {
			//Connection failed, create the scan result corresponding with the given point and return
			HttpStaticScanResult scanResult  = new HttpStaticScanResult(ScanResultCode.SCAN_FAILED, timeOfScan);
			
			scanResult.expectedResponseCode = expectedResponseCode;
			scanResult.expectedDataHash = expectedDataHash;
			scanResult.expectedDataHashAlgorithm = expectedDataHashAlgorithm;
			scanResult.specimenUrl = specimenUrl;
			
			logScanResult( ScanResultCode.SCAN_FAILED, scanResult.deviations, HttpStaticScanRule.RULE_TYPE, specimenUrl.toString(), "Connection failed" );
			return scanResult;
		}
		
		
		// 3 -- Retrieve and hash the data
		InputStream httpDataInStream = null;
		boolean inputError = false;
		
		try {
			httpDataInStream = httpMethod.getResponseBodyAsStream();
		} catch (IOException e) {
			// We could not get the data; the connection must have failed
			inputError = true;
		}
		
		MessageDigest messageDigest;
		
		try{
			messageDigest = MessageDigest.getInstance(expectedDataHashAlgorithm);
		}
		catch(NoSuchAlgorithmException e){
			throw new ScanException("Hash algorithm (" + expectedDataHashAlgorithm + ") was not found", e);
		}
		
		//	 3.1 -- Retrieve the data and hash it
		byte[] inputBytes = new byte[4096];
		int bytesRead = 0;
		
		if( inputError == false && httpDataInStream != null){
			try {
				while( (bytesRead = httpDataInStream.read(inputBytes)) > 0 ){
					messageDigest.update(inputBytes, 0, bytesRead);
				}
			} catch (IOException e) {
				// We could not get the data; the connection must have failed.
				// The connection failure will be noted in the scan status but does not require stopping 
				inputError = true;
			}
		}
		
		byte[] hashBytes = messageDigest.digest();
		String observedHashValue = new String(Hex.encodeHex(hashBytes));
		
		
		// 4 -- Check the headers
		Header[] headers = httpMethod.getResponseHeaders();
		Vector<HttpHeaderScanResult> headerRuleMatches = new Vector<HttpHeaderScanResult>();
		
		for( int c = 0; c < headers.length; c++ ){
			HttpHeaderScanResult ruleResult = analyzeHeader(headers[c].getName(), headers[c].getValue());
			
			headerRuleMatches.add( ruleResult );
		}
		
		
		// 5 -- Compile the results
		HttpStaticScanResult scanResult;
		
		if(inputError)
			scanResult = new HttpStaticScanResult(ScanResultCode.SCAN_FAILED, timeOfScan);
		else
			scanResult = new HttpStaticScanResult(ScanResultCode.SCAN_COMPLETED, timeOfScan);
		
		scanResult.expectedResponseCode = expectedResponseCode;
		scanResult.expectedDataHash = expectedDataHash;
		scanResult.expectedDataHashAlgorithm = expectedDataHashAlgorithm;
		scanResult.specimenUrl = specimenUrl;
		
		scanResult.setActualHash(expectedDataHashAlgorithm, observedHashValue);
		scanResult.setActualResponseCode( httpMethod.getStatusCode());
		
		//Upload the header results to the scan result object
		scanResult.headerResults = new HttpHeaderScanResult[headerRuleMatches.size()];
		for(int c = 0; c < headerRuleMatches.size(); c++){
			scanResult.headerResults[c] = headerRuleMatches.get(c);
		}
		
		
		// 6 -- Return the results
		logScanResult( ScanResultCode.SCAN_COMPLETED, scanResult.deviations, HttpStaticScanRule.RULE_TYPE, specimenUrl.toString() );
		return scanResult;
		
	}
	
	/**
	 * Evaluates the header according to the set header rules and determines if the header passes
	 * @precondition The headers rules must have already been populated
	 * @postcondition A headerRuleResult will be returned that includes the rule that matched the header (or HEADER_RULE_ID_NOT_SET if none matched) and a boolean that indicates if the rule passed 
	 * @param headerName The HTTP header name (Server, Location, etc.)
	 * @param headerValue The value of the HTTP header
	 * @return A HttpHeaderRuleResult that includes the rule that matched the header (or HEADER_RULE_ID_NOT_SET if none matched) and a boolean that indicates if the rule passed
	 */
	private HttpHeaderScanResult analyzeHeader(String headerName, String headerValue){
		
		// 0 -- Validate parameters
		// The parameters will be validated in the getDoesHeaderPassRule method call
		
		// 1 -- Determine if the if the header is accepted by the rule set
		HttpHeaderScanResult headerRuleResult = null;
		
		for(int c = 0; c < headerRules.size(); c++){
			HttpHeaderRule headerRule = headerRules.get(c);
			
			int result = getDoesHeaderPassRule(headerName, headerValue, c);
			
			// 1.1 -- If the header is specifically rejected, then reject immediately since there is no way that the header can be accepted
			if( result == MATCH_REJECT ){
				headerRuleResult = new HttpHeaderScanResult();
				
				headerRuleResult.ruleAction = HttpStaticScanRule.MUST_NOT_MATCH;
				headerRuleResult.ruleId = headerRule.getRuleId();
				headerRuleResult.ruleResult = HttpHeaderScanResult.REJECTED;
				headerRuleResult.nameActual = headerName;
				headerRuleResult.valueActual = headerValue;
				
				// 1.1.1 -- Include a description of the header name rule that matched
				headerRuleResult.nameRule = headerRule.getHeaderNameString();
				headerRuleResult.nameRuleType = headerRule.getNameRuleType();
				
				// 1.1.2 -- Include a description of the header value rule that matched
				headerRuleResult.valueRule = headerRule.getHeaderValueString();
				headerRuleResult.valueRuleType = headerRule.getValueRuleType();
				
				return headerRuleResult;
			}
			
			// 1.2 -- Indicate if the header has been accepted by at least one rule. Evaluation must continue since rule that reject have precedence over ones that accept 
			else if( result == MATCH_ACCEPT ){
				headerRuleResult = new HttpHeaderScanResult();
				
				// 1.2.1 -- Include a description of the header name rule that matched
				headerRuleResult.nameRule = headerRule.getHeaderNameString();
				headerRuleResult.nameRuleType = headerRule.getNameRuleType();
				
				// 1.2.2 -- Include a description of the header value rule that matched
				headerRuleResult.valueRule = headerRule.getHeaderValueString();
				headerRuleResult.valueRuleType = headerRule.getValueRuleType();
				
				//	1.2.3 -- Include other necessary attributes
				headerRuleResult.ruleAction = HttpStaticScanRule.MUST_MATCH;
				headerRuleResult.ruleId = headerRule.getRuleId();
				headerRuleResult.ruleResult = HttpHeaderScanResult.ACCEPTED;
				headerRuleResult.nameActual = headerName;
				headerRuleResult.valueActual = headerValue;
				
			}
		}
		
		// 2 -- Determine if the rule should be rejected based upon the default header policy
		
		//	 2.1 -- If the rule matched at least one accept rule and no reject rules then accept regardless of the default header policy
		/*	 Note: If the headerRuleResult has been populated then at least one accept rule matched and no reject rules matched. If the 
		 *         header had matched a reject rule then it would have returned already since it returns as soon as a rejecting rule is found.
		 */
		
		if( headerRuleResult != null){
			return headerRuleResult;
		}
		
		//	 2.2 -- If the rule did not match an accept rule then the default policy must be invoked
		headerRuleResult = new HttpHeaderScanResult();
		
		//headerRuleResult.ruleId = HttpHeaderScanResult. HEADER_RULE_DEFAULT;
		headerRuleResult.nameActual = headerName;
		headerRuleResult.valueActual = headerValue;
		headerRuleResult.ruleId = HEADER_RULE_ID_NOT_SET;
		
		// Note that the default rule was invoked
		if( defaultDenyHeaders ){
			headerRuleResult.ruleAction = HttpStaticScanRule.MUST_NOT_MATCH;
			headerRuleResult.ruleResult = HttpHeaderScanResult.REJECTED_BY_DEFAULT;
		}
		else{
			headerRuleResult.ruleAction = HttpStaticScanRule.MUST_MATCH;
			headerRuleResult.ruleResult = HttpHeaderScanResult.ACCEPTED_BY_DEFAULT;
		}
		
		return headerRuleResult;
	}
	
	/**
	 * The method below loads a scan result from the database corresponding to the scan result originally saved to disk.
	 * @precondition A database connection must be available and the data in the database must be valid (or an exception will be throw). Note that the result will be null if the result for the given ID could not be found.
	 * @postcondition A scan result will be returned or null if none could be found for the given identifier
	 * @param connection
	 * @param scanResultId
	 * @return
	 * @throws SQLException 
	 * @throws MalformedURLException 
	 * @throws NoDatabaseConnectionException 
	 * @throws ScanResultLoadFailureException 
	 */
	public ScanResult loadScanResult( long scanResultId ) throws SQLException, NoDatabaseConnectionException, ScanResultLoadFailureException{ // NOPMD by luke on 5/26/07 11:21 AM
		
		// 0 -- Precondition check
		//	 0.1 -- Make sure the database connection exists
		Connection connection = null;
		// The database connection will be checked in the try block
		
		//	 0.2 -- Ensure the scan result ID is valid
		
		// 1 -- Load the basic scan result attributes
		PreparedStatement statement = null;
		ResultSet httpResult = null;
		ResultSet result = null;
		PreparedStatement httpStatement = null;
		PreparedStatement httpHeaderStatement = null;
		ResultSet httpHeaderResult = null;
		
		HttpStaticScanResult scanResult;
		
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
			if( connection == null )
				throw new NoDatabaseConnectionException();
			
			statement = connection.prepareStatement("Select * from ScanResult where ScanResultID = ?");
			statement.setLong(1, scanResultId);
			result = statement.executeQuery();
			
			if( !result.next() ){
				return null;//Record was not found
			}
			
			ScanResultCode resultCode = ScanResultCode.getScanResultCodeById(result.getInt("ScanResultCode"));
			
			if( resultCode == null )
				return null;//Result ID is not valid
			
			scanResult = new HttpStaticScanResult(resultCode, result.getTimestamp("ScanDate"));
			scanResult.ruleId = result.getLong("ScanRuleID");
			
			// 2 -- Load the HTTP specific attributes
			httpStatement = connection.prepareStatement("Select * from HttpHashScanResult where ScanResultID = ?");
			httpStatement.setLong(1, scanResultId);
			httpResult = httpStatement.executeQuery();
			
			scanResult.actualDataHash = httpResult.getString("ActualHashData");
			scanResult.actualDataHashAlgorithm = httpResult.getString("ActualHashAlgorithm");
			scanResult.actualResponseCode = httpResult.getInt("ActualResponseCode");
			scanResult.expectedDataHash = httpResult.getString("ExpectedHashData");
			scanResult.expectedDataHashAlgorithm = httpResult.getString("ExpectedHashAlgorithm");
			scanResult.expectedResponseCode = httpResult.getInt("ExpectedResponseCode");
			try{
			scanResult.specimenUrl = new URL( httpResult.getString("LocationUrl") );
			}
			catch(MalformedURLException e){
				throw new ScanResultLoadFailureException("The URL associated with the scan result is invalid", e);
			}
			scanResult.deviations = httpResult.getInt("Deviations");
			
			// 3 -- Load the header scan results
			httpHeaderStatement = connection.prepareStatement("Select * from HttpHeaderScanResult where ScanResultID = ?");
			httpHeaderStatement.setLong(1, scanResultId);
			httpHeaderResult = httpHeaderStatement.executeQuery();
			
			int n = 0;
			while( httpHeaderResult.next() ){
				n++;
			}
			
			if( n > 0 ){
				httpHeaderResult.first();
				HttpHeaderScanResult[] headerResults = new HttpHeaderScanResult[n];
				int c =0;
				
				while( httpHeaderResult.next() ){
					HttpHeaderScanResult httpHeaderScanResult = new HttpHeaderScanResult();
					
					httpHeaderScanResult.nameActual = httpHeaderResult.getString("ActualHeaderName");
					httpHeaderScanResult.nameRule = httpHeaderResult.getString("ExpectedHeaderName");
					httpHeaderScanResult.nameRuleType = httpHeaderResult.getInt("HeaderNameType");
					httpHeaderScanResult.ruleAction = httpHeaderResult.getInt("MatchAction");
					httpHeaderScanResult.ruleId = httpHeaderResult.getLong("HttpHeaderScanRuleID");
					httpHeaderScanResult.ruleResult = httpHeaderResult.getInt("RuleResult");
					httpHeaderScanResult.valueActual = httpHeaderResult.getString("ActualHeaderValue");
					httpHeaderScanResult.valueRule = httpHeaderResult.getString("ExpectedHeaderValue");
					httpHeaderScanResult.valueRuleType = httpHeaderResult.getInt("HeaderValueType");
					
					headerResults[c] = httpHeaderScanResult;
					c++;
				}
				
				scanResult.headerResults = headerResults;
			}
			
			// 4 -- Return the result
			return scanResult;
			
			
		} finally {
			
			if (result != null )
				result.close();
			
			if (statement != null )
				statement.close();
			
			if (httpStatement != null )
				httpStatement.close();
			
			if (httpResult != null )
				httpResult.close();
			
			if (httpHeaderStatement != null )
				httpHeaderStatement.close();
			
			if (httpHeaderResult != null )
				httpHeaderResult.close();
			
			if (connection != null )
				connection.close();
		}
	}
	
	/**
	 * The following method compares the actual header parameters (name and value) to the header located at the given header ID.
	 * @precondition The headerName and value cannot be null
	 * @postcondition A integer will be returned indicating if the header parameters match and are accepted by the given rule 
	 * @param headerName The name of the HTTP header (Server, Location, etc.)
	 * @param headerValue The value of the HTTP header ("Apache 1.3.26" etc.) 
	 * @param headerRuleIndex The ID of the header rule in the collection of header rules
	 * @return A integer indicating whether the rule was matched (DOES_NOT_MATCH, MATCH_ACCEPT or MATCH_REJECT)
	 */
	private int getDoesHeaderPassRule(String headerName, String headerValue, int headerRuleIndex){
		
		// 0 -- Validate parameters
		if( headerRuleIndex >= headerRules.size() || headerRuleIndex < 0)
			throw new IllegalArgumentException("The header rule ID(" + headerRuleIndex + ") is not valid, no such rule exists");
		
		if( headerName == null )
			throw new IllegalArgumentException("The header name cannot be null");
		
		if( headerValue == null )
			throw new IllegalArgumentException("The header value cannot be null");
		
		
		// 1 -- Determine if the rule matches
		HttpHeaderRule headerRule = headerRules.get(headerRuleIndex);
		boolean ruleMatches = headerRule.doesNameMatch(headerName);
		
		if( ruleMatches == false )
			return DOES_NOT_MATCH;
		
		
		// 2 -- Determine if the value matches
		boolean valueMatches = headerRule.doesValueMatch(headerValue);
		
		int matchAction = headerRule.getRuleType();
		
		if( valueMatches && matchAction == MUST_MATCH ) //The header must be matched and has been (pass)
			return MATCH_ACCEPT;
		else if( valueMatches && matchAction == MUST_NOT_MATCH ) //The header must not be matched but has been (fail)
			return MATCH_REJECT;
		else if( !valueMatches && matchAction == MUST_MATCH ) //The header must be matched but the value is incorrect (fail)
			return MATCH_REJECT;
		else //if( !valueMatches && matchAction == MUST_NOT_MATCH ) //The header must not be matched and indeed does not since the value does not match (pass)
			return MATCH_REJECT;
		
	}
	
	/**
	 * Sets the HTTP expected response code (e.g. 200, 500, 302, etc.).
	 * @precondition The response code must not be less than zero (or an exception will be thrown).
	 * @postcondition The expected response code will be set 
	 * @param responseCode The HTTP response expected from the server
	 * @throws IllegalArgumentException When the response code is less than 0
	 */
	public final void setExpectedResponseCode( int responseCode ) throws IllegalArgumentException{
		if( responseCode < 0) 
			throw new IllegalArgumentException("The response code must not be less than 0");
		
		expectedResponseCode = responseCode;
	}
	
	/**
	 * Gets the expected response code, or -1 if it has not been set yet.
	 * @precondition None
	 * @postcondition The expected response code is returned (or -1 if not set) 
	 *
	 */
	public int getExpectedResponseCode(){
		return expectedResponseCode;
	}
	
	/**
	 * Sets the specifics for the expected hash. SHA-256 or stronger is recommended since defects being discovered in MD5 and SHA1 may render them less secure.
	 * @precondition The hash algorithm must be supported
	 * @postcondition The hash algorithm and hash value will be set 
	 * @param hashAlgorithm The hash algorithm (SHA1, MD5, etc.) Note that SHA-256 or stronger is recommended.
	 * @param hashValue
	 * @throws NoSuchAlgorithmException
	 */
	public final void setExpectedDataHash(String hashAlgorithm, String hashValue) throws NoSuchAlgorithmException{
		
		// 1 -- Make sure the algorithm is known
		MessageDigest.getInstance(hashAlgorithm);
		
		// 2 -- Set the parameters
		expectedDataHash = hashValue;
		expectedDataHashAlgorithm = hashAlgorithm;
	}
	
	/**
	 * Retrieves the expected hash algorithm or null if not yet set.
	 * @precondition None (although the hash must be already be set or null will be returned)
	 * @postcondition The expected hash algorithm or null will be returned.
	 * @return expected hash algorithm or null (if not set)
	 */
	public String getExpectedHashAlgorithm(){
		return expectedDataHashAlgorithm;
	}
	
	/**
	 * Gets the expected hash value.
	 * @precondition None (although the hash must be already be set or null will be returned)
	 * @postcondition The expected hash value or null will be returned.
	 * @return The expected hash value or null (if not set)
	 */
	public String getExpectedDataHashValue(){
		return expectedDataHash;
	}
	
	/**
	 * Sets the URL to be scanned.
	 * @precondition None
	 * @postcondition The URL to be scanned will be saved
	 * @param url The URL to be scanned
	 */
	public final void setUrl(URL url){
		specimenUrl = url;
	}
	
	/**
	 * Gets the URL to be scanned; or null if not set.
	 * @precondition None (although null is returned if the URL has not yet been set)
	 * @postcondition The URL is returned
	 * @return
	 */
	public URL getUrl(){
		return specimenUrl;
	}
	
	/**
	 * Sets whether all headers must match an HTTP header rule.
	 * @precondition None
	 * @postcondition The policy will be updated to reflect the argument
	 * @param defaultDeny
	 */
	public void setDefaultDenyHeaderPolicy( boolean defaultDeny ){
		defaultDenyHeaders = defaultDeny;
	}
	
	/**
	 * Get the policy regarding headers that do not match an HTTP header rule.
	 * @precondition None
	 * @postcondition A boolean is returned indicating whether the headers are rejected by default 
	 * @return
	 */
	public boolean getDefaultDenyHeaderPolicy (){
		return defaultDenyHeaders;
	}
	
	/**
	 * Set the option to follow HTTP redirects.
	 * @precondition None
	 * @postcondition The option to follow redirects will be set 
	 * @param follow Boolean indicating to follow HTTP redirects
	 */
	public final void setFollowRedirects( boolean follow){
		followRedirects = follow;
	}
	
	/**
	 * Get the option that indicates if HTTP redirects are to followed.
	 * @precondition None
	 * @postcondition A boolean indicating if redirects are followed will be returned
	 * @return Boolean indicating to follow HTTP redirects
	 */
	public boolean getFollowRedirects(){
		return followRedirects;
	}
	
	/**
	 * Returns a boolean indicating if the class is ready to scan.
	 * @precondition None
	 * @postcondition A boolean is returned regarding whether the class is ready to scan
	 * @return
	 */
	private boolean isReady(){
		// 1 -- Make sure the URL is set
		if( specimenUrl == null )
			return false;
		
		// 2 -- Make sure the hash info is set
		if( expectedDataHash == null || expectedDataHashAlgorithm == null )
			return false;
		
		// 3 -- Make sure the response code is set
		if( expectedResponseCode == -1)
			return false;
		
		// 4 -- Else return true; we appear to be ready
		return true;
	}
	
	/**
	 * The following method attempts to create a new header rule entry.
	 * @param headerRule
	 * @return
	 */
	public boolean addHeaderRule(HttpHeaderRule headerRule){
		return headerRules.add(headerRule);
	}
	
	/**
	 * Remove the header rule from the given rule.
	 * @param headerRuleId
	 * @return
	 */
	public boolean removeHeaderRule( long headerRuleId ){
		
		// 0 -- Precondition check
		if( headerRuleId < 0 )
			throw new IllegalArgumentException("The header rule identifier is invalid");
		
		
		// 1 -- Find the rule and remove it
		for( int c = 0; c < headerRules.size(); c++){
			HttpHeaderRule headerRule = headerRules.get(c);
			
			//Found the rule, remove it and return
			if( headerRule.getRuleId() == headerRuleId ){
				headerRules.remove(c);
				headerRulesAwaitingDeletion.add(Long.valueOf( headerRuleId ));
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Delete the header rule that corresponds to the identifier.
	 * @param headerScanRuleId
	 * @param appRes
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	public static boolean deleteHeaderRule( long headerScanRuleId ) throws SQLException, NoDatabaseConnectionException{
		
		// 1 -- Delete the HTTP rule
		Application appRes = Application.getApplication();
		Connection connection = null;
		PreparedStatement statement = null;
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
			statement = connection.prepareStatement("Delete * from HttpHeaderScanRule where HttpHeaderScanRuleID = ?");
			statement.setLong(1, headerScanRuleId);
			if( statement.executeUpdate() < 1 ){
				connection.close();
				statement.close();
				return false;
			}
			else{
				connection.close();
				statement.close();
				return true;
			}
		} finally{
			if( statement != null )
				statement.close();
			
			if (connection != null )
				connection.close();
		}
	}
	
	/**
	 * Delete the rule associated with the given identifier.
	 * @precondition The scan rule identifier must be valid
	 * @postcondition The rule will be deleted
	 * @param scanRuleId
	 * @return
	 * @throws SQLException 
	 * @throws NoDatabaseConnectionException 
	 */
	public static boolean deleteRule( long scanRuleId ) throws SQLException, NoDatabaseConnectionException{
		Application appRes = Application.getApplication();
		
		ScanRule.deleteRule( scanRuleId );
		
		// 1 -- Delete the HTTP header rules
		Connection connection = null;
		PreparedStatement statement = null;
		
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
			statement = connection.prepareStatement("Delete from HttpHeaderScanRule where ScanRuleID = ?");
			statement.setLong(1, scanRuleId);
			statement.executeUpdate();
			statement.close();
			statement = null;
			
			// 2 -- Delete the HTTP rule
			statement = connection.prepareStatement("Delete from HttpHashScanRule where ScanRuleID = ?");
			statement.setLong(1, scanRuleId);
			if( statement.executeUpdate() < 1 )
				return false;
			else
				return true;
		}
		finally {
			if( statement != null )
				statement.close();
			
			if (connection != null )
				connection.close();
		}
	}
	
	/**
	 * Save the rule to the database as a new entry and automatically generate a rule ID.
	 * @throws IllegalStateException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	public long saveToDatabase() throws IllegalStateException, SQLException, NoDatabaseConnectionException{
		if( scanRuleId == -1 ){
			throw new IllegalStateException("Scan rule must not be less than zero");
		}
		else{
			return saveToDatabaseEx( scanRuleId );
		}
	}
	
	/**
	 * Save the rule to the database as a new entry and automatically generate a rule ID. The rule will be associated 
	 * with the sitegroup identified in the argument.
	 * @throws IllegalStateException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	public long saveNewRuleToDatabase( long siteGroupId ) throws IllegalStateException, SQLException, NoDatabaseConnectionException{
		if( siteGroupId < 0 ){
			throw new IllegalArgumentException("Site group identifer must not be less than zero");
		}
		else{
			return saveNewRuleToDatabaseEx( siteGroupId );
		}
	}
	
	/**
	 * Save the rule to the database using the given scan rule ID.
	 * @param scanRuleId
	 * @throws IllegalStateException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	public void saveToDatabase( long scanRuleId ) throws IllegalStateException, SQLException, NoDatabaseConnectionException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the scan rule is valid ( >= 0)
		if( scanRuleId < 0 )
			throw new IllegalArgumentException("Scan rule must not be less than zero");
		
		// 1 -- Save the results
		saveToDatabaseEx( scanRuleId );
	}
	
	
	/**
	 * Save the rule to the database and associate with the given site group.
	 * @param siteGroupId
	 * @throws IllegalStateException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	private synchronized long saveNewRuleToDatabaseEx( long siteGroupId ) throws IllegalStateException, SQLException, NoDatabaseConnectionException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the scan rule ID is valid
		if( siteGroupId < -1 )
			throw new IllegalArgumentException("Site group ID is invalid (must not be less than zero)");
		
		//	 0.2 -- Make sure that the rule is ready to be saved (i.e. necessary attributes have been provided)
		if( isReady() == false)
			throw new IllegalStateException("HTTP scan class cannot be persisted to database since critical information is missing");
		
		//	 0.3 -- Make sure a database connection exists
		Connection connection = null;
		
		
		// 2 -- Save the current rule
		PreparedStatement statement = null;
		ResultSet resultKeys = null;
		
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
			
			scanRuleId = createRule( siteGroupId, getScanFrequency(), HttpStaticScanRule.RULE_TYPE, RULE_STATE_ACTIVE );
			
			statement = connection.prepareStatement("Insert into HttpHashScanRule(LocationUrl, HashAlgorithm, HashData, ResponseCode, DefaultDenyHeaders, ScanRuleID) values(?, ?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
			statement.setString( 1, specimenUrl.toString() );
			statement.setString( 2, expectedDataHashAlgorithm );
			statement.setString( 3, expectedDataHash );
			statement.setInt( 4, expectedResponseCode );
			statement.setBoolean( 5, defaultDenyHeaders );
			statement.setLong( 6, scanRuleId );
			statement.execute();
			resultKeys = statement.getGeneratedKeys();

			
			// 3 -- Save the HTTP header rules
			Iterator<HttpHeaderRule> iterator = headerRules.iterator();
			while( iterator.hasNext() ){
				HttpHeaderRule headerRule = iterator.next();
				headerRule.saveToDatabase(connection, scanRuleId);

				headerRule.saveToDatabase(connection, scanRuleId);
			}
			
			return scanRuleId;
			
		} finally {
			if (resultKeys != null )
				resultKeys.close();
			
			if (statement != null )
				statement.close();
			
			if (connection != null )
				connection.close();
		}
		
	}
	
	/**
	 * Save the rule to the database using the given scan rule ID.
	 * @param scanRuleId
	 * @throws IllegalStateException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	private synchronized long saveToDatabaseEx( long scanRuleId ) throws IllegalStateException, SQLException, NoDatabaseConnectionException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the scan rule ID is valid
		if( scanRuleId < 0 )
			throw new IllegalArgumentException("Scan rule ID is invalid (must not be less than zero)");
		
		//	 0.2 -- Make sure that the rule is ready to be saved (i.e. necessary attributes have been provided)
		if( isReady() == false)
			throw new IllegalStateException("HTTP scan class cannot be persisted to database since critical information is missing");
		
		//	 0.3 -- Make sure a database connection exists
		Connection connection = null;
		
		
		// 2 -- Save the current rule
		PreparedStatement statement = null;
		PreparedStatement generalStatement = null;
		PreparedStatement statementDeleteOldHeaders = null;
		
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
			statement = connection.prepareStatement("Update HttpHashScanRule set LocationUrl = ?, HashAlgorithm = ?, HashData = ?, ResponseCode = ?, DefaultDenyHeaders = ? where ScanRuleID = ?");
			statement.setString( 1, specimenUrl.toString() );
			statement.setString( 2, expectedDataHashAlgorithm );
			statement.setString( 3, expectedDataHash );
			statement.setInt( 4, expectedResponseCode );
			statement.setBoolean( 5, defaultDenyHeaders );
			statement.setLong( 6, scanRuleId );
			
			statement.executeUpdate();
			
			this.scanRuleId = scanRuleId; //Retain the latest scan rule ID
			
			// 3 -- Save the HTTP header rules
			
			//		3.1 -- Purge the old rules
			for( int c = 0; c < headerRulesAwaitingDeletion.size(); c++){
				Long httpHeaderRule = headerRulesAwaitingDeletion.get(c);
				statementDeleteOldHeaders =connection.prepareStatement("Delete from HttpHeaderScanRule where HttpHeaderScanRuleID = ?");
				statementDeleteOldHeaders.setLong( 1, httpHeaderRule.longValue() );
				statementDeleteOldHeaders.executeUpdate();
				
				statementDeleteOldHeaders.close();
			}
			
			//		3.2 -- Insert the new rules
			Iterator<HttpHeaderRule> iterator = headerRules.iterator();
			while( iterator.hasNext() ){
				HttpHeaderRule headerRule = iterator.next();
				headerRule.saveToDatabase(connection, scanRuleId);
			}
			
			
			// 4 -- Save the generic rule attributes
			generalStatement = connection.prepareStatement("Update ScanRule set ScanFrequency = ?, ScanDataObsolete = ?, Modified = ? where ScanRuleID = ?");
			generalStatement.setInt( 1, this.getScanFrequency() );
			generalStatement.setBoolean( 2, true);
			generalStatement.setTimestamp( 3, new Timestamp(new java.util.Date().getTime()) );
			generalStatement.setLong( 4, scanRuleId );
			generalStatement.executeUpdate();
			
			return this.scanRuleId;
			
		} finally {
			
			if (statement != null )
				statement.close();
			
			if (generalStatement != null )
				generalStatement.close();
			
			if (statementDeleteOldHeaders != null )
				statementDeleteOldHeaders.close();
			
			if (connection != null )
				connection.close();
		}
		
	}
	
	public boolean updateHeaderRule( String headerName, String headerValue, int matchAction, long headerRuleId ) {
		return updateHeaderRuleEx( headerName, headerValue, matchAction, headerRuleId );
	}
	
	public boolean updateHeaderRule( Pattern headerName, String headerValue, int matchAction, long headerRuleId ) {
		return updateHeaderRuleEx( headerName, headerValue, matchAction, headerRuleId );
	}
	
	public boolean updateHeaderRule( String headerName, Pattern headerValue, int matchAction, long headerRuleId ) {
		return updateHeaderRuleEx( headerName, headerValue, matchAction, headerRuleId );
	}
	
	public boolean updateHeaderRule( Pattern headerName, Pattern headerValue, int matchAction, long headerRuleId ){
		return updateHeaderRuleEx( headerName, headerValue, matchAction, headerRuleId );
	}
	
	/**
	 * This is in interal method designed to perform updates to the header rules.
	 * @param headerName
	 * @param headerValue
	 * @param matchAction
	 * @param headerRuleId
	 * @return
	 * @throws SQLException
	 */
	private boolean updateHeaderRuleEx( Object headerName, Object headerValue, int matchAction, long headerRuleId ) {
		
		// 1 -- Make sure the rule exists and update it in memory
		for( int c = 0; c < headerRules.size(); c++){
			HttpHeaderRule rule = headerRules.get(c);
			
			//Found the rule, update it and return
			if( rule != null && rule.getRuleId() == headerRuleId ){
				
				if (headerName instanceof String) {
					rule.setHeaderName((String) headerName);
				}
				else{//if (headerName instanceof Pattern) {
					rule.setHeaderName((Pattern) headerName);
				}
				
				if (headerValue instanceof String) {
					rule.setHeaderValue((String) headerValue);
				}
				else{//if (setHeaderValue instanceof Pattern) {
					rule.setHeaderValue((Pattern) headerValue);
				}
				
				rule.setRuleType(matchAction);

				return true;
			}
		}
		
		return false;
	}
	
	/*
	public static boolean updateHeaderRule_Immediate(Pattern headerName, Pattern headerValue, int matchAction, long headerRuleId ) throws SQLException, NoDatabaseConnectionException{
		Vector headerRule = new Vector(4);
		
		headerRule.add(HEADER_NAME, headerName);
		headerRule.add(HEADER_VALUE, headerValue);
		headerRule.add(HEADER_ACTION, new Integer(matchAction));
		headerRule.add(HEADER_RULE_ID, new Long( headerRuleId ) );
		
		HttpHeaderRule headerRule = new HttpHeaderRule();
		headerRule.nameRule
		
		return saveUpdatedHeaderRuleToDatabase( headerRule );
	}*/
	
	/**
	 * Get the header rules associated with this HTTP rule.
	 * @return
	 */
	public HttpHeaderRule[] getHeaderRules(){
		
		HttpHeaderRule[] httpHeaderRulesArray = new HttpHeaderRule[headerRules.size()];
		for( int c = 0; c < headerRules.size(); c++ ){
			httpHeaderRulesArray[c] = headerRules.get(c);
		}
		
		return httpHeaderRulesArray;
	}
	
	/**
	 * Gets the header rule that matches the header name.
	 * @param headerName
	 * @return
	 */
	public HttpHeaderRule getHeaderRule( String headerName ){
		for(int c = 0; c < headerRules.size(); c++){
			HttpHeaderRule headerRule = headerRules.get(c);
			if( headerRule.doesNameMatch(headerName) )
				return headerRule;
		}
		
		return null;
	}
	
	/**
	 * This method causes the rule to delete itself from the database.
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public void delete() throws SQLException, NoDatabaseConnectionException{
		deleteRule(scanRuleId);
		ScanRule.deleteRule(scanRuleId);
	}
	
	
	@SuppressWarnings("unchecked")
	public static HttpStaticScanRule getFromHashtable( Hashtable<String, Object> hashtable ) throws MalformedURLException{
		
		// 0 -- Precondition check
		
		String className = (String)hashtable.get("Class");
		if( className == null || !className.matches( "net.lukemurphey.siteSentry.HttpScan" ) )
			throw new IllegalArgumentException("Class name invalid");
		
		// 1 -- Get the parameters
		String expectedDataHash =  (String)hashtable.get("ExpectedHash");
		String expectedDataHashAlgorithm =  (String)hashtable.get("ExpectedHashAlgorithm");
		String specimenUrlStr =  (String)hashtable.get("URL");
		Boolean defaultDenyHeaders = (Boolean)hashtable.get("DefaultDenyHeaders");
		Integer expectedResponseCode = (Integer)hashtable.get("ExpectedResponseCode");
		Boolean followRedirects = (Boolean)hashtable.get("FollowRedirects");
		Long ruleId = (Long)hashtable.get("RuleID");
		Vector<Hashtable<String, Object>> headerRulesVector = (Vector<Hashtable<String, Object>>)hashtable.get("HeaderRules");
		Vector<HttpHeaderRule> headerRules = new Vector<HttpHeaderRule>();
		
		for ( int c = 0; c < headerRulesVector.size(); c++ ){
			HttpHeaderRule httpHeaderRule = HttpHeaderRule.getFromHashtable( (Hashtable)headerRulesVector.get(c) );
			headerRules.add( httpHeaderRule );
		}
		
		// 2 -- Set the parameters
		HttpStaticScanRule httpScan = new HttpStaticScanRule( Application.getApplication() );
		httpScan.headerRules = headerRules;
		httpScan.defaultDenyHeaders = defaultDenyHeaders.booleanValue();
		httpScan.expectedDataHashAlgorithm = expectedDataHashAlgorithm;
		httpScan.expectedDataHash = expectedDataHash;
		httpScan.expectedResponseCode = expectedResponseCode.intValue();
		httpScan.followRedirects = followRedirects.booleanValue();
		httpScan.scanRuleId = ruleId.longValue();
		URL specimenUrl = new URL( specimenUrlStr );
		httpScan.specimenUrl = specimenUrl;
		
		return httpScan;
	}
	
	/**
	 * Creates a hashtable description of the class (must be implemented as required by the super class).
	 */
	public Hashtable<String, Object> toHashtable(){
		Hashtable<String, Object> hashtable = super.toHashtable();
		
		// 1 -- Get the header rules
		Vector<Hashtable<String, Object>> vectorHeaderRules = new Vector<Hashtable<String, Object>>();
		for(int c =0; c < headerRules.size(); c++){
			HttpHeaderRule headerRule = headerRules.get(c);
			vectorHeaderRules.add(headerRule.toHashtable());
		}
		
		// 2 -- Set the other options
		hashtable.put( "Class", getClass().getName() ); //getClass().getName() + '@' + Integer.toHexString(hashCode())
		hashtable.put( "ExpectedHash", expectedDataHash );
		hashtable.put( "ExpectedHashAlgorithm", expectedDataHashAlgorithm );
		hashtable.put( "URL", specimenUrl.toString() );
		hashtable.put( "DefaultDenyHeaders", Boolean.valueOf( defaultDenyHeaders ) );
		hashtable.put( "ExpectedResponseCode", Integer.valueOf( expectedResponseCode ) );
		hashtable.put( "FollowRedirects", Boolean.valueOf( followRedirects ) );
		hashtable.put( "HeaderRules", vectorHeaderRules );
		//hashtable.put( "RuleID", new Long( scanRuleId ) );
		
		return hashtable;
	}
		
	
}
