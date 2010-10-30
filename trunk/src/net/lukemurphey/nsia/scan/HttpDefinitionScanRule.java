package net.lukemurphey.nsia.scan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager; 
import org.apache.commons.lang.StringUtils;

import org.htmlparser.Parser;
import org.htmlparser.util.ParserException;

import java.io.IOException;
import java.net.*;
import java.util.*;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.Application.DatabaseAccessType;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.eventlog.EventLogMessage.EventType;
import net.lukemurphey.nsia.scan.Definition.Severity;

/**
 * The HTTP definition scan rule analyzes a resource with the set of definitions currently available in the definition set.
 * @author Luke
 *
 */
public class HttpDefinitionScanRule extends ScanRule{

	private URL url = null; 
	private DefinitionSet signatureSet = null;
	public final static String RULE_TYPE = "HTTP/SignatureScan";
	private static MultiThreadedHttpConnectionManager connectionManager = null;
	private HttpClient httpClient = null;
	private boolean waitToLogCompletion = false;
	private HttpMethod httpMethod = null;
	private boolean terminated = false;
	private Object httpMethodMutex = new Object();
	
	HttpDefinitionScanRule(Application applicationResources, DefinitionSet signatureSet, URL url) {
		super(applicationResources);
		this.url = url;
		this.signatureSet = signatureSet;
	}
	
	HttpDefinitionScanRule(Application applicationResources, DefinitionSet signatureSet, URL url, HttpClient client) {
		super(applicationResources);
		this.url = url;
		this.signatureSet = signatureSet;
		this.httpClient = client;
	}
	
	@Override
	public void delete() throws SQLException, NoDatabaseConnectionException {
		deleteRule(scanRuleId);
	}
	
	/**
	 * This function will allow the logging of completion to be suppressed. This method exists because the scan result
	 * may be updated by other scan results that are scanning the content that is linked from within the current page.
	 * The original event log entry will be incorrect log entry is sent and another scan result updates it. This is common
	 * with reports of broken links since the fact that a broken link exists cannot be verified until the child linked
	 * content is scanned.
	 * @param suppress
	 */
	protected void suppressLoggingToEventLog(boolean suppress){
		this.waitToLogCompletion = suppress;
	}
	
	/**
	 * Get a connection manager for the HTTP client.
	 * @return
	 */
	public static synchronized MultiThreadedHttpConnectionManager getConnectionManager(){
		if( connectionManager == null ){
			connectionManager = new MultiThreadedHttpConnectionManager();
			HttpConnectionManagerParams params = new HttpConnectionManagerParams();
			params.setMaxTotalConnections(100);
			params.setConnectionTimeout( 60 * 1000); //Seconds to wait until a connection is established
			params.setSoTimeout( 60 * 1000); //Seconds to timeout if no data is received within the timeframe given
			connectionManager.setParams(params);
		}
		
		return connectionManager;
	}
	
	/**
	 * Many browsers accept URLs that fail to meet the RFC standard (RFC2396, RFC3986) for URIs. This method will
	 * escape the URIs to replace the invalid characters with the escape codes in order to allow the URI
	 * to be processed by an HTTP client.
	 */
	private String escapeURL(String url){
		String result = url;
		
		result = StringUtils.replace(result, " " , "%20");
		result = StringUtils.replace(result, "<" , "%3C");
		result = StringUtils.replace(result, ">" , "%3E");
		result = StringUtils.replace(result, "\"" , "%22");
		result = StringUtils.replace(result, "'" , "%27");
		result = StringUtils.replace(result, "," , "%2C");
		result = StringUtils.replace(result, ":" , "%3A");
	
		return result;
	}
	
	/**
	 * Represents a scan result that includes a parser for traversing the HTML code.
	 * @author Luke
	 *
	 */
	protected static class HttpSignatureScanResultWithParser{
		
		private HttpDefinitionScanResult scanResult;
		private Parser parser;
		private int httpResponseCode = -1;
		private Vector<URL> extractedURLs = new Vector<URL>();
		
		public HttpSignatureScanResultWithParser(HttpDefinitionScanResult scanResult, Parser parser){
			this.scanResult = scanResult;
			this.parser = parser;
		}
		
		public HttpSignatureScanResultWithParser(HttpDefinitionScanResult scanResult, Parser parser, int httpResponseCode){
			this.scanResult = scanResult;
			this.parser = parser;
			this.httpResponseCode = httpResponseCode;
		}
		
		public HttpSignatureScanResultWithParser(HttpDefinitionScanResult scanResult, Parser parser, Vector<URL> extractedURLs ){
			this.scanResult = scanResult;
			this.parser = parser;
			this.extractedURLs.addAll(extractedURLs);
		}
		
		public HttpSignatureScanResultWithParser(HttpDefinitionScanResult scanResult, Parser parser, int httpResponseCode, Vector<URL> extractedURLs ){
			this.scanResult = scanResult;
			this.parser = parser;
			this.httpResponseCode = httpResponseCode;
			this.extractedURLs.addAll(extractedURLs);
		}
		
		public HttpDefinitionScanResult getScanResult(){
			return scanResult;
		}
		
		public Parser getParser(){
			return parser;
		}
		
		public int getHttpResponseCode(){
			return httpResponseCode;
		}
		
	}
	
	/**
	 * Perform a scan and return the result along with a parser.
	 * @param parentScanResult
	 * @return
	 * @throws ScanException
	 */
	public HttpSignatureScanResultWithParser doScanAndReturnParser( HttpDefinitionScanResult parentScanResult ) throws ScanException {
		return doScanInternal(parentScanResult);
	}
	
	@Override
	public ScanResult doScan() throws ScanException {
		HttpSignatureScanResultWithParser result = doScanInternal(null);
		return result.scanResult;
	}
	
	/**
	 * Retrieve the exceptions associated with the given site-group. Note that this method returns all of the policies
	 * for the site-group regardless of the rule ID; the rule ID will be checked when the policy is reviewed to determine
	 * if it matches a given observation.
	 * @param siteGroupID
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	private DefinitionPolicySet loadSignatureExceptions(long siteGroupID) throws SQLException, NoDatabaseConnectionException{
		DefinitionPolicyManagement signatureManagement = new DefinitionPolicyManagement(appRes);

		if( this.scanRuleId > -1 ){
			try{
				siteGroupID = ScanRule.getAssociatedSiteGroupID(scanRuleId);

				return signatureManagement.getPolicySet(siteGroupID);
			}
			catch(NotFoundException e){
				return signatureManagement.getPolicySet();
			}
		}
		else{
			return signatureManagement.getPolicySet();
		}
	}
	
	/**
	 * Get the HTTP response from the given URL.
	 **/ 
	private HttpResponseData getResponseData(URL url, HttpClient client ) throws HttpException, IOException{
		
		// 1 -- Initialize the HTTP client
		HostConfiguration hostConfig = new HostConfiguration();
		hostConfig.setHost(url.getHost(), url.getPort(), url.getProtocol());
		
		// 2 -- Perform the GET
		try{
			HttpClient httpClient;
			synchronized(httpMethodMutex){

				httpMethod = new GetMethod( escapeURL( url.getPath() ) );
		
				httpMethod.setFollowRedirects(true);
				//httpMethod.setRequestHeader("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
		
				if(client == null){
					httpClient = new HttpClient();
				}
				else{
					httpClient = client;
				}
			}
			
			httpClient.executeMethod( hostConfig, httpMethod );
			
			// Download the content
			HttpResponseData httpResponse = new HttpResponseData( httpMethod, url.toString() );
			httpMethod.releaseConnection();
			
			return httpResponse;
		}
		finally{
			if( httpMethod != null )
			{
				httpMethod.releaseConnection();
			}
		}
	}
	
	/**
	 * Perform a scan and return a scan result with an HTML parser.
	 * @param parentResult
	 * @return
	 * @throws ScanException
	 */
	private HttpSignatureScanResultWithParser doScanInternal( HttpDefinitionScanResult parentResult ) throws ScanException {
		
		// 1 -- Load the exception set
		DefinitionPolicySet signatureExceptions = null;
		long siteGroupID = -1;

		try{
			// 1.1 -- Get the site group identifier
			if( this.scanRuleId > -1 ){
				try{
					siteGroupID = ScanRule.getAssociatedSiteGroupID(scanRuleId);
				}
				catch(NotFoundException e){
					siteGroupID = -1;
				}
			}
			
			// 1.2 -- Get the signature exceptions
			signatureExceptions = loadSignatureExceptions(siteGroupID);
			Vector<DefinitionMatch> results = new Vector<DefinitionMatch>();
			
			try{
				// 2 -- Retrieve and parse the content
				HttpResponseData httpResponse;
				try{
					httpResponse = getResponseData(url, httpClient);
				}
				catch(IllegalArgumentException e){
					
					// Determine if the finding was filtered and ignore it if so
					if( signatureExceptions != null && signatureExceptions.isFiltered(siteGroupID, scanRuleId, MetaDefinition.INVALID_URI, url) ){
						return new HttpSignatureScanResultWithParser( new HttpDefinitionScanResult(ScanResultCode.SCAN_COMPLETED, new java.sql.Timestamp(System.currentTimeMillis()), url, this.scanRuleId ), null );
					}
					
					// Log the scan issue and return the relevant scan result
					if( parentResult != null && parentResult.getSpecimenDescription() != null ){
						return new HttpSignatureScanResultWithParser( new HttpDefinitionScanResult(ScanResultCode.SCAN_FAILED, new java.sql.Timestamp(System.currentTimeMillis()), url, new DefinitionMatch( MetaDefinition.INVALID_URI, "URI loaded from " + parentResult.getSpecimenDescription()), this.scanRuleId ), null );
					}
					else{
						return new HttpSignatureScanResultWithParser( new HttpDefinitionScanResult(ScanResultCode.SCAN_FAILED, new java.sql.Timestamp(System.currentTimeMillis()), url, new DefinitionMatch( MetaDefinition.INVALID_URI ), this.scanRuleId ), null );
					}
				}
				
				// Don't bother performing any analysis on the results if the scan was terminated
				if( terminated ){
					return new HttpSignatureScanResultWithParser( new HttpDefinitionScanResult(ScanResultCode.SCAN_COMPLETED, new java.sql.Timestamp(System.currentTimeMillis()), url, this.scanRuleId ), null );
				}

				//	 2.1 -- Get the parser
				Parser parser = null;
				
				try{
					parser = httpResponse.getDocumentParser();
				}
				catch (ParserException e) {
					parser = null;
				}


				// 3 -- Scan the content for signature matches
				results = signatureSet.scan(httpResponse, signatureExceptions, siteGroupID, scanRuleId);
				
				if( results == null ){
					results = new Vector<DefinitionMatch>();
				}
				else{
					for(DefinitionMatch match : results){
						logDefinitionMatch(match.getDefinitionName(), match.getDefinitionID(), this.getSpecimenDescription(), this.scanRuleId, match.getSeverity(), match.getMessage());
					}
				}
				
				//	 3.1 -- Determine if the response code matches a MetaSignature
				MetaDefinition sig = getMetaSignatureFromResponseCode( httpResponse.getResponseCode() );
				
				//	 3.1.1 -- If a match was found, then add it to the list of matches
				if( sig != null && (signatureExceptions == null || signatureExceptions.isFiltered(siteGroupID, scanRuleId, sig, url) == false) ){
					
					if( parentResult != null ){
						sig = sig.createNewWithMessage("; referenced from " + parentResult.getSpecimenDescription());
						
						// Notify the parent record that it contains a broken record
						if(		sig.localId == MetaDefinition.CONNECTION_FAILED.localId
								|| sig.localId == MetaDefinition.CONNECTION_REFUSED.localId
								|| sig.localId == MetaDefinition.CONNECTION_TIMEOUT.localId
								|| sig.localId == MetaDefinition.NO_DATA.localId
								|| sig.localId == MetaDefinition.RESPONSE_CODE_404.localId
								|| sig.localId == MetaDefinition.RESPONSE_CODE_405.localId
								|| sig.localId == MetaDefinition.RESPONSE_CODE_406.localId
								|| sig.localId == MetaDefinition.RESPONSE_CODE_409.localId
								|| sig.localId == MetaDefinition.RESPONSE_CODE_410.localId
								|| sig.localId == MetaDefinition.RESPONSE_CODE_414.localId
								|| sig.localId == MetaDefinition.RESPONSE_CODE_415.localId ){
							parentResult.addBrokenLink(this.getSpecimenDescription());
						}
					}
					
					// 3.1.2 -- Log the signature match 
					logDefinitionMatch( sig, this.getSpecimenDescription(), this.scanRuleId);
					
					results.add( new DefinitionMatch( sig ) );
				}
				
				//	 3.2 -- Rules were matched, return the scan result
				if( results.size() > 0 ){
					logScanResult( ScanResultCode.SCAN_COMPLETED, results.size(), HttpSeekingScanRule.RULE_TYPE, url.toString(), results.size() + " definitions matched", waitToLogCompletion == false );
					
					return new HttpSignatureScanResultWithParser( new HttpDefinitionScanResult(ScanResultCode.SCAN_COMPLETED, new java.sql.Timestamp(System.currentTimeMillis()), url, results, this.scanRuleId, httpResponse.getContentType() ), parser, httpResponse.getResponseCode() );
				}
				else{
					logScanResult( ScanResultCode.SCAN_COMPLETED, 0, HttpSeekingScanRule.RULE_TYPE, url.toString(), "0 definitions matched", waitToLogCompletion == false );
					
					return new HttpSignatureScanResultWithParser( new HttpDefinitionScanResult(ScanResultCode.SCAN_COMPLETED, new java.sql.Timestamp(System.currentTimeMillis()), url, results, this.scanRuleId, httpResponse.getContentType()), parser, httpResponse.getResponseCode() );
				}

			} catch (InvalidDefinitionException e) {
				appRes.logExceptionEvent(EventLogMessage.EventType.SCAN_ENGINE_EXCEPTION, e);
				throw new ScanException("Scan failed: InvalidSignatureException (" + e.getMessage() +  ")", e);
			} catch (HttpException e) {
				boolean isRedirectLoop = e.getMessage().startsWith("Circular redirect");
				boolean maxRedirects = e.getMessage().startsWith("Maximum redirects");
				
				if(isRedirectLoop && (signatureExceptions == null || signatureExceptions.isFiltered(siteGroupID, scanRuleId, MetaDefinition.REDIRECT_LOOP, url) == false ) ){
					results.add( new DefinitionMatch(MetaDefinition.REDIRECT_LOOP, e.getMessage()));
				}
				else if(maxRedirects && (signatureExceptions == null || signatureExceptions.isFiltered(siteGroupID, scanRuleId, MetaDefinition.REDIRECT_LIMIT_EXCEEDED, url) == false ) ){
					results.add( new DefinitionMatch(MetaDefinition.REDIRECT_LIMIT_EXCEEDED, e.getMessage()));
				}
				else if( !isRedirectLoop && (signatureExceptions == null || signatureExceptions.isFiltered(siteGroupID, scanRuleId, MetaDefinition.HTTP_EXCEPTION, url) == false )){
					results.add( new DefinitionMatch(MetaDefinition.HTTP_EXCEPTION, e.getMessage()));
				}
			} catch (IOException e) {
				boolean isTimeout = e.getMessage().startsWith("Connection timed out: connect");
				boolean isRefused = e.getMessage().startsWith("Connection refused: connect");
				
				if(isTimeout && (signatureExceptions == null || signatureExceptions.isFiltered(siteGroupID, scanRuleId, MetaDefinition.CONNECTION_TIMEOUT, url) == false ) ){
					results.add( new DefinitionMatch(MetaDefinition.CONNECTION_TIMEOUT, e.getMessage()));
				}
				else if(isRefused && (signatureExceptions == null || signatureExceptions.isFiltered(siteGroupID, scanRuleId, MetaDefinition.CONNECTION_REFUSED, url) == false ) ){
					results.add( new DefinitionMatch(MetaDefinition.CONNECTION_REFUSED, e.getMessage()));
				}
				else if( (!isRefused && !isTimeout) && (signatureExceptions == null || signatureExceptions.isFiltered(siteGroupID, scanRuleId, MetaDefinition.IO_EXCEPTION, url) == false ) ){
					results.add( new DefinitionMatch(MetaDefinition.IO_EXCEPTION, e.getMessage()));
				}
			}
			
			logScanResult( ScanResultCode.SCAN_COMPLETED, results.size(), HttpSeekingScanRule.RULE_TYPE, url.toString() );
			return new HttpSignatureScanResultWithParser( new HttpDefinitionScanResult(ScanResultCode.SCAN_COMPLETED, new java.sql.Timestamp(System.currentTimeMillis()), url, results, this.scanRuleId ), null );

		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new ScanException("Scan failed: NoDatabaseConnectionException", e);
		} catch(SQLException e){
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new ScanException("Scan failed: SQLException", e);
		} 
	}

	/**
	 * Log that a definition match occurred.
	 * @param signatureName
	 * @param signatureID
	 * @param url
	 * @param ruleID
	 * @param severity
	 */
	private void logDefinitionMatch( String signatureName, int signatureID, String url, long ruleID, Severity severity ){
		logDefinitionMatch(signatureName, signatureID, url, ruleID, severity, null);
	}
	
	/**
	 * Log that a definition match occurred.
	 * @param signatureName
	 * @param signatureID
	 * @param url
	 * @param ruleID
	 * @param severity
	 * @param definitionOutput
	 */
	private void logDefinitionMatch( String signatureName, int signatureID, String url, long ruleID, Severity severity, String definitionOutput ){
		
		if( appRes != null ){
			EventLogMessage message = new EventLogMessage(EventType.DEFINITION_MATCH);
			
			message.addField(new EventLogField(FieldName.DEFINITION_NAME, signatureName));
			
			if( signatureID > -1 ){
				message.addField( new EventLogField(FieldName.DEFINITION_ID, signatureID ));
			}
			
			message.addField( new EventLogField(FieldName.SEVERITY, severity.toString() ));
			message.addField( new EventLogField(FieldName.URL, url ));
			
			if( definitionOutput != null ){
				message.addField( new EventLogField(FieldName.DEFINITION_MESSAGE, definitionOutput ));
			}
			
			if( ruleID > -1 ){
				message.addField(new EventLogField(FieldName.RULE_ID, ruleID));
			}
			
			appRes.logEvent(message);
		}
	}
	
	/**
	 * Log that a definition match occurred.
	 * @param sig
	 * @param url
	 * @param ruleID
	 */
	private void logDefinitionMatch( Definition sig, String url, long ruleID){
		logDefinitionMatch(sig.getFullName(), sig.id, url, ruleID, sig.getSeverity());
	}
	
	/**
	 * Get the MetaDefinition associated with the given HTTP response code if one exists; otherwise, return null if none applies.
	 * @param responseCode
	 * @return
	 */
	private MetaDefinition getMetaSignatureFromResponseCode(int responseCode){
		switch (responseCode) {
			case 400:  return MetaDefinition.RESPONSE_CODE_400;
			case 401:  return MetaDefinition.RESPONSE_CODE_401;
			case 402:  return MetaDefinition.RESPONSE_CODE_402;
			case 403:  return MetaDefinition.RESPONSE_CODE_403;
			case 404:  return MetaDefinition.RESPONSE_CODE_404;
			case 405:  return MetaDefinition.RESPONSE_CODE_405;
			case 406:  return MetaDefinition.RESPONSE_CODE_406;
			case 407:  return MetaDefinition.RESPONSE_CODE_407;
			case 408:  return MetaDefinition.RESPONSE_CODE_408;
			case 409:  return MetaDefinition.RESPONSE_CODE_409;
			case 410:  return MetaDefinition.RESPONSE_CODE_410;
			case 411:  return MetaDefinition.RESPONSE_CODE_411;
			case 412:  return MetaDefinition.RESPONSE_CODE_412;
			case 413:  return MetaDefinition.RESPONSE_CODE_413;
			case 414:  return MetaDefinition.RESPONSE_CODE_414;
			case 415:  return MetaDefinition.RESPONSE_CODE_415;
			case 416:  return MetaDefinition.RESPONSE_CODE_416;
			case 417:  return MetaDefinition.RESPONSE_CODE_417;
			
			case 500:  return MetaDefinition.RESPONSE_CODE_500;
			case 501:  return MetaDefinition.RESPONSE_CODE_501;
			case 502:  return MetaDefinition.RESPONSE_CODE_502;
			case 503:  return MetaDefinition.RESPONSE_CODE_503;
			case 504:  return MetaDefinition.RESPONSE_CODE_504;
			case 505:  return MetaDefinition.RESPONSE_CODE_505;
			
			default:   return null;
		}
	}
	
	@Override
	public String getRuleType() {
		return RULE_TYPE;
	}

	@Override
	public String getSpecimenDescription() {
		return url.toExternalForm();
	}

	@Override
	public boolean loadFromDatabase(long scanRuleId) throws NotFoundException, NoDatabaseConnectionException, SQLException, ScanRuleLoadFailureException {
		//This rule cannot be persisted to the database
		return false;
	}

	public void terminate(){
		terminated = true;
		if( httpMethod != null){
			synchronized(httpMethodMutex){
				httpMethod.abort();
			}
		}
	}
	
	@Override
	public ScanResult loadScanResult(long scanResultId) throws NotFoundException, NoDatabaseConnectionException, SQLException, ScanResultLoadFailureException {
		
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try{
			connection = appRes.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			
			// 1 -- Load the generic attributes
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
			
			statement = connection.prepareStatement("Select * from ScanResult where ScanResultID = ?");
			statement.setLong(1, scanResultId);
			result = statement.executeQuery();
			
			if( !result.next() ){
				return null;//Record was not found
			}
			
			ScanResultCode resultCode = ScanResultCode.getScanResultCodeById(result.getInt("ScanResultCode"));
			
			if( resultCode == null )
				return null;//Result ID is not valid
			
			Timestamp scanDate = result.getTimestamp("ScanDate");
			long ruleId = result.getLong("ScanRuleID");
			
			
			statement.close();
			result.close();
			
			statement = null;
			result = null;
			
			
			// 2 -- Load the specific signature attributes
			statement = connection.prepareStatement("Select * from SignatureScanResult where ScanResultID = ?");
			statement.setLong(1, scanResultId);
			
			result = statement.executeQuery();
			
			if( result.next() ){
				
				String url = result.getString("URL");
				
				// 1 -- Load the basic scan result
				this.url = new URL(url);
				String contentType = result.getString("ContentType");
				
				Vector<DefinitionMatch> sigMatchesVector = HttpDefinitionScanResult.loadSignatureMatches(scanResultId, Application.getApplication());
				
				return new HttpDefinitionScanResult(resultCode, scanDate, new URL(url), sigMatchesVector, ruleId, contentType);
			}
			
			return null;
		
		}
		catch( MalformedURLException e){
			throw new ScanResultLoadFailureException("The scan result could not be loaded because the URL is malformed", e);
		}
		finally{
			
			if( result != null ){
				result.close();
			}
			
			if( statement != null ){
				statement.close();
			}
			
			if( connection != null ){
				connection.close();
			}
		}

	}

}
