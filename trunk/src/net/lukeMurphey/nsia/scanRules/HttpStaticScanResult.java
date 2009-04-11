package net.lukeMurphey.nsia.scanRules;

import java.sql.*;
import java.net.*;
import java.util.*;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.NoDatabaseConnectionException;

/**
 * The scan result object contains a description of the scan results. Only a HttpScan class should be able
 * to create this class, thus, the constructor is not public.
 * @author luke
 *
 */
public class HttpStaticScanResult extends ScanResult {
	protected int expectedResponseCode = -1;
	protected String expectedDataHash = null;
	protected String expectedDataHashAlgorithm = null;
	protected URL specimenUrl = null;
	protected HttpHeaderScanResult[] headerResults;
	
	//Actual data
	protected int actualResponseCode = -1;
	protected String actualDataHash = null;
	protected String actualDataHashAlgorithm = null;
	
	/**
	 * The constructor is not public to prevent arbitrary construction of results.
	 * @param scanResultCode
	 */
	protected HttpStaticScanResult(ScanResultCode scanResultCode, Timestamp timeOfScan) {
		super(scanResultCode, timeOfScan);
		headerResults = new HttpHeaderScanResult[0];
	}
	
	/**
	 * Determine the number of deviations based on the scan result.
	 * @precondition The hash, response code, and headers must already be set to be evaluated properly
	 * @postcondition The number of deviations will be returned and cached for performance reasons
	 * @return Number of deviations observed from the last scan. 
	 */
	public int getDeviations(){
		//Determine if the number of deviations has already been calculated
		if( deviations < 0 ){
			
			deviations = 0;//Assume no deviations unless otherwise proven (innocent until proven guilty)
			
			// Check the hash
			if( expectedDataHashAlgorithm != null && actualDataHashAlgorithm != null && actualDataHashAlgorithm.matches(expectedDataHashAlgorithm))
				if( expectedDataHash == null || actualDataHash == null || !expectedDataHash.matches(actualDataHash))
					deviations++;
			
			// Check the headers
			for(int c = 0; c < headerResults.length; c++){
				if( headerResults[c].getRuleState() != HttpHeaderScanResult.ACCEPTED && headerResults[c].getRuleState() != HttpHeaderScanResult.ACCEPTED_BY_DEFAULT )
					deviations++;
			}
			// Check the response code
			if( actualResponseCode != expectedResponseCode )
				deviations++;
			
			// Return the results (note the results will now be cached, no recomputation is necessary)
			return deviations;
		}
		else
			return deviations;
	}
	/**
	 * Get the expected HTTP response code.
	 * @precondition None
	 * @postcondition The expected response code is returned
	 * @return The expected response code, or -1 if not set
	 */
	public int getExpectedResponseCode(){
		return expectedResponseCode;
	}
	
	/**
	 * Get the actually observed HTTP response code.
	 * @precondition None
	 * @postcondition The actual response code is returned
	 * @return The actual response code, or -1 if not set
	 */
	public int getActualResponseCode(){
		return actualResponseCode;
	}
	
	/**
	 * Get the expected hash value for the HTTP data.
	 * @precondition None
	 * @postcondition The expected hash value is returned
	 * @return The expected hash value, or null if not set
	 */
	public String getExpectedHashValue(){
		return expectedDataHash;
	}
	
	/**
	 * Get the expected algorithm for the HTTP data hash value.
	 * @precondition None
	 * @postcondition The expected algorithm for the HTTP data hash value is returned
	 * @return The expected algorithm for the HTTP data hash value, or null if not set
	 */
	public String getExpectedHashAlgorithm(){
		return expectedDataHashAlgorithm;
	}
	
	/**
	 * Get the actual hash value for the HTTP data.
	 * @precondition None
	 * @postcondition The actual hash value is returned
	 * @return The actual hash value, or null if not set
	 */
	public String getActualHashValue(){
		return actualDataHash;
	}
	
	/**
	 * Get the actual algorithm for the HTTP data hash value.
	 * @precondition None
	 * @postcondition The actual algorithm for the HTTP data hash value is returned, or null if not set
	 * @return The actual algorithm for the HTTP data hash value
	 */
	public String getActualHashAlgorithm(){
		return actualDataHashAlgorithm;
	}
	
	/**
	 * Get the URL to be examined.
	 * @precondition None
	 * @postcondition The URL to be evaluated is returned, or null if not set
	 * @return A URL to be analyzed, or null if not set
	 */
	public URL getUrl(){
		return specimenUrl;
	}
	
	/**
	 * Get the header rule match results.
	 * @precondition None (although null will be returned if the no header results have been populated yet)
	 * @postcondition Returns the header rule results, or null if none exist
	 * @return
	 */
	public HttpHeaderScanResult[] getHeaderRuleResults(){
		
		HttpHeaderScanResult[] headerResultsCopy = new HttpHeaderScanResult[headerResults.length];
		
		System.arraycopy(headerResults, 0, headerResultsCopy, 0, headerResults.length);
		return headerResultsCopy;
	}
	
	/**
	 * Sets the details regarding the actual hash of the HTTP data.
	 * @precondition None
	 * @postcondition The actual hash algorithm and value will be set
	 * @param hashAlgorithm The hash algorithm (SHA-1, MD5, etc.)
	 * @param hashData A hexadecimal representation of the hash value
	 */
	protected void setActualHash( String hashAlgorithm, String hashData ){
		actualDataHashAlgorithm = hashAlgorithm;
		actualDataHash = hashData;
	}
	
	/**
	 * Sets the actual response code.
	 * @param responseCode
	 */
	protected void setActualResponseCode( int responseCode ){
		actualResponseCode = responseCode;
	}
	
	/**
	 * This method will cause the class to automatically save the results to the database. For this to work,
	 * the database needs to be prepared with tables to retain the data.
	 * @throws SQLException 
	 * @precondition The database needs to be configured with the appropriate schema
	 * @postcondition The rule result and HTTP headers will be saved in the database
	 */
	public long saveToDatabase( Connection connection, long scanRuleId ) throws SQLException{
		
		// 0 -- Precondition check
		if( connection == null )
			return -1;
		
		// 1 -- Save the data to the parent record (the scan result tuple)
		long scanResultId = saveToDatabaseInitial( connection, scanRuleId, HttpStaticScanRule.RULE_TYPE  );
		
		if( scanResultId == -1 )//Exit if the parent record could not be created
			return -1;
		
		// 2 -- Save the data for the HTTP scan result 
		PreparedStatement statement = null;
		PreparedStatement headerStatement = null;
		
		try{
			statement = connection.prepareStatement("Insert into HttpHashScanResult (ScanResultID, ActualHashAlgorithm, ActualHashData, ActualResponseCode, ExpectedHashAlgorithm, ExpectedHashData, ExpectedResponseCode, LocationUrl) values (?, ?, ?, ?, ?, ?, ?, ?)");
			statement.setLong(1, scanResultId );
			statement.setString(2, actualDataHashAlgorithm );
			statement.setString(3,actualDataHash );
			statement.setInt(4, actualResponseCode );
			statement.setString(5, expectedDataHashAlgorithm );
			statement.setString(6, expectedDataHash );
			statement.setInt(7, expectedResponseCode );
			statement.setString(8, specimenUrl.toString() );
			
			long httpResultId = statement.executeUpdate();
			
			if( httpResultId < 0 )
				return -1;
			
			// 3 -- Save the data for the HTTP headers
			headerStatement = connection.prepareStatement("Insert into HttpHeaderScanResult (ScanResultID, MatchAction, ExpectedHeaderName, HeaderNameType, ActualHeaderName, ExpectedHeaderValue, HeaderValueType, ActualHeaderValue, RuleResult) values (?, ?, ?, ?, ?, ?, ?, ?, ?)");
			if(headerResults!=null){
				for( int c = 0; c < headerResults.length; c++ ){
					headerStatement.setLong(1, scanResultId );
					headerStatement.setInt(2, headerResults[c].ruleAction );
					headerStatement.setString(3,headerResults[c].nameRule );
					headerStatement.setInt(4, headerResults[c].nameRuleType );
					headerStatement.setString(5, headerResults[c].nameActual );
					headerStatement.setString(6, headerResults[c].valueRule );
					headerStatement.setInt(7, headerResults[c].valueRuleType );
					headerStatement.setString(8, headerResults[c].valueActual );
					headerStatement.setInt(9, headerResults[c].ruleResult );
					
					headerStatement.executeUpdate();
				}
			}
			
			saveToDatabaseFinalize( connection, scanResultId, getDeviations(), scanRuleId);
			
			// 4 -- Return from the function
			return httpResultId;
			
		} finally {
			if (statement != null )
				statement.close();
			
			if (headerStatement != null )
				headerStatement.close();
		}
		
	}
	
	/**
	 * Create a hashtable version of the class
	 */
	public Hashtable<String, Object> toHashtable(){
		
		Hashtable<String, Object> hashtable = super.toHashtable();
		
		Vector<Hashtable<String, Object>> headerScanResultsVector = new Vector<Hashtable<String, Object>>();
		
		for( int c = 0; c < headerResults.length; c++ ){
			HttpHeaderScanResult headerScanResult = headerResults[c];
			headerScanResultsVector.add( headerScanResult.toHashtable() );
		}
		
		hashtable.put( "Class", getClass().getName() );
		hashtable.put("ActualHash", actualDataHash);
		hashtable.put("ActualHashAlgorithm", actualDataHashAlgorithm);
		hashtable.put("ActualResponseCode", Integer.valueOf( actualResponseCode ) );
		hashtable.put("ExpectedDataHash", expectedDataHash);
		hashtable.put("ExpectedHashAlgorithm", expectedDataHashAlgorithm);
		hashtable.put("ExpectedResponseCode", Integer.valueOf( expectedResponseCode ) );
		hashtable.put("URL", specimenUrl);
		hashtable.put("HeaderScanResults", headerScanResultsVector );
		return hashtable;
		
	}
	
	protected static HttpStaticScanResult loadFromDatabase( long scanRuleId, long scanResultId, ScanResultCode resultCode, Timestamp scanTime, int deviations, int incompletes, int accepts ) throws SQLException, NoDatabaseConnectionException{
		Application application = Application.getApplication();
		Connection connection = null;
		
		// 1 -- Load the basic scan result
		HttpStaticScanResult scanResult;
		scanResult = new HttpStaticScanResult( resultCode, scanTime );
		scanResult.deviations = deviations;
		scanResult.incompletes = incompletes;	
		scanResult.accepts = accepts;	
		
		// 2 -- Load the class specific attributes
		PreparedStatement httpStatement = null;
		ResultSet httpresult = null;
		PreparedStatement httpHeadersStatement = null;
		ResultSet httpHeaderResult = null;
		
		try{
			connection = application.getDatabaseConnection( Application.DatabaseAccessType.SCANNER );
			httpStatement = connection.prepareStatement("Select * from HttpHashScanResult where ScanResultID = ?");
			httpStatement.setLong(1, scanResultId);
			httpresult = httpStatement.executeQuery();
			
			if( httpresult.next() ){
				scanResult.actualDataHash = httpresult.getString("ActualHashData");
				scanResult.actualDataHashAlgorithm  = httpresult.getString("ActualHashAlgorithm");
				scanResult.actualResponseCode = httpresult.getInt("ActualResponseCode");
				scanResult.expectedDataHash = httpresult.getString("ExpectedHashData");
				scanResult.actualDataHashAlgorithm = httpresult.getString("ExpectedHashAlgorithm");
				scanResult.expectedResponseCode = httpresult.getInt("ExpectedResponseCode");
				scanResult.ruleId = scanRuleId;//OR httpresult.getLong("ScanRuleID");
				scanResult.scanResultId = scanResultId;//OR httpresult.getLong("ScanResultID");
				
				try {
					scanResult.specimenUrl =  new URL( httpresult.getString("LocationUrl") );
				} catch (MalformedURLException e) {
					scanResult.specimenUrl = null;
				}
			}
			else
				return null;
			
			// 3 -- Load header results
			httpHeadersStatement = connection.prepareStatement("Select * from HttpHeaderScanResult where ScanResultID = ?");
			httpHeadersStatement.setLong(1, scanResultId);
			httpHeaderResult = httpHeadersStatement.executeQuery();
			
			Vector<HttpHeaderScanResult> headerResults = new Vector<HttpHeaderScanResult>();
			
			if( httpHeaderResult.next() ){
				HttpHeaderScanResult headerScanResult = new HttpHeaderScanResult();
				headerScanResult.nameActual = httpHeaderResult.getString("ActualHeaderName");
				headerScanResult.nameRule = httpHeaderResult.getString("ExpectedHeaderName");
				headerScanResult.nameRuleType = httpHeaderResult.getInt("HeaderNameType");
				headerScanResult.ruleAction = httpHeaderResult.getInt("MatchAction");
				headerScanResult.ruleId = httpHeaderResult.getLong("HttpHeaderScanRuleID");
				headerScanResult.ruleResult = httpHeaderResult.getInt("RuleResult");
				headerScanResult.valueActual = httpHeaderResult.getString("ActualHeaderValue");
				headerScanResult.valueRule = httpHeaderResult.getString("ExpectedHeaderValue");
				headerScanResult.valueRuleType = httpHeaderResult.getInt("HeaderValueType");
				
				headerResults.add(headerScanResult);
			}
			
			HttpHeaderScanResult[] headerResultsArray = new HttpHeaderScanResult[headerResults.size()];
			for ( int c = 0; c < headerResults.size(); c++)
				headerResultsArray[c] = (HttpHeaderScanResult)headerResults.get(c);
			
			scanResult.headerResults = headerResultsArray;
			
			// Done, return the result
			return scanResult;
		} finally {
			
			if (httpStatement != null )
				httpStatement.close();
			
			if (httpresult != null )
				httpresult.close();
			
			if (httpHeadersStatement != null )
				httpHeadersStatement.close();
			
			if (httpHeaderResult != null )
				httpHeaderResult.close();
			
			if (connection != null )
				connection.close();
		}
	}
	
	public String getRuleType() {
		return HttpStaticScanRule.RULE_TYPE;
	}
	
	public String getSpecimenDescription(){
		return specimenUrl.toString();
	}
}
