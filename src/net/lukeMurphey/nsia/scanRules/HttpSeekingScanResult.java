package net.lukeMurphey.nsia.scanRules;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.MaxMinCount;
import net.lukeMurphey.nsia.NoDatabaseConnectionException;
import net.lukeMurphey.nsia.Application.DatabaseAccessType;
import net.lukeMurphey.nsia.eventLog.EventLogMessage;
import net.lukeMurphey.nsia.scanRules.HttpDefinitionScanResult.SignatureScanResultFilter;
import net.lukeMurphey.nsia.scanRules.ScanRule.ScanResultLoadFailureException;
import net.lukeMurphey.nsia.NameIntPair;

public class HttpSeekingScanResult extends ScanResult {
	
	private HttpDefinitionScanResult[] findings;
	private String domain;
	private boolean partialResults = false;
	private Vector<NameIntPair> contentTypes = null;
	
	
	protected HttpSeekingScanResult(ScanResultCode scanResultCode, Timestamp timeOfScan) {
		super(scanResultCode, timeOfScan);
	}

	public HttpSeekingScanResult( Vector<HttpDefinitionScanResult> matches, String domainWildcard, long scanRuleId, ScanResultCode resultCode, Timestamp timeOfScan ){
		super(resultCode, timeOfScan);
		
		findings = new HttpDefinitionScanResult[matches.size()];
		matches.toArray(findings);
		
		this.ruleId = scanRuleId;
		domain = domainWildcard;
	}
	
	public HttpDefinitionScanResult[] getFindings(){
		loadFindings();
		
		HttpDefinitionScanResult[] tempResults = new HttpDefinitionScanResult[findings.length];
		System.arraycopy(findings, 0, tempResults, 0, findings.length);
		
		return tempResults;
	}
	
	public Hashtable<Definition.Severity, Integer> getSignatureMatchSeverities() throws SQLException, NoDatabaseConnectionException{
		return HttpDefinitionScanResult.getSignatureMatchSeverities(this.scanResultId);
	}
	
	@Override
	public String getRuleType() {
		return HttpSeekingScanRule.RULE_TYPE;
	}

	@Override
	public String getSpecimenDescription() {
		return domain;
	}
	
	@Override
	public int getDeviations() {
		int devs = 0;
		
		if( findings == null ){
			return deviations;
		}
		
		for( int c = 0; c < findings.length; c++){
			if( findings[c].deviations > 0){
				devs++;
			}
		}
		
		deviations = devs;
		return devs;
	}
	
	private void updateCounts(){
		
		if( deviations >= 0 || incompletes >= 0 || accepts >= 0 ){
			return;
		}
		
		deviations = 0;
		accepts = 0;
		incompletes = 0;
		
		for( int c =0; c < findings.length; c++){
			if( findings[c].getDeviations() > 0 ){
				deviations = deviations + 1;
			}
			else if( findings[c].getResultCode() == ScanResultCode.SCAN_COMPLETED ){
				accepts = accepts + 1;
			}
			else{
				incompletes = incompletes + 1;
			}
		}
	}

	protected static HttpSeekingScanResult loadFromDatabase( long scanRuleId, long scanResultId, ScanResultCode resultCode, Timestamp scanTime, int deviations, int incompletes, int accepts ) throws SQLException, NoDatabaseConnectionException, ScanResultLoadFailureException{
		return loadFromDatabase( Application.getApplication(), scanRuleId, scanResultId, resultCode, scanTime, deviations, incompletes, accepts);
	}
	
	protected static HttpSeekingScanResult loadFromDatabase( Application application, long scanRuleId, long scanResultId, ScanResultCode resultCode, Timestamp scanTime, int deviations, int incompletes, int accepts ) throws SQLException, NoDatabaseConnectionException, ScanResultLoadFailureException{
		Connection connection = null;
		
		// 1 -- Load the basic scan result
		HttpSeekingScanResult scanResult = new HttpSeekingScanResult(resultCode, scanTime);
		scanResult.deviations = deviations;
		scanResult.incompletes = incompletes;
		scanResult.accepts = accepts;
		scanResult.ruleId = scanRuleId;
		scanResult.scanResultId = scanResultId;
		
		
		// 2 -- Load the class specific attributes
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try{
			// 2.1 -- Load the core attributes
			connection = application.getDatabaseConnection( Application.DatabaseAccessType.SCANNER );
			
			statement = connection.prepareStatement("Select * from HttpDiscoveryResult where ScanResultID = ?");
			statement.setLong(1, scanResultId);
			
			result = statement.executeQuery();
			if( result.next() ){
				scanResult.domain = result.getString("Domain");
			}
			
			// 2.2 -- Load the findings
			scanResult.partialResults = true;
			
			// Done, return the result
			return scanResult;
		} finally {
			
			if (connection != null )
				connection.close();
			
			if (statement != null )
				statement.close();
			
			if (result != null )
				result.close();
		}
	}
	
	/**
	 * Create a list of the content types discovered.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 */
	public Vector<NameIntPair> getDiscoveredContentTypes(){
		
		if(contentTypes != null){
			return contentTypes;
		}
		else if( partialResults == false ){
			Vector<NameIntPair> contentTypesTemp = new Vector<NameIntPair>();
			
			for ( HttpDefinitionScanResult sigResult : findings ) {
				
				boolean found = false;
				for(NameIntPair pair : contentTypesTemp ){
					if( 	pair.getName() == null && sigResult.getContentType() == null
							|| ( pair.getName() != null && pair.getName().equalsIgnoreCase( sigResult.getContentType() ) ) ) {
						pair.setValue( pair.getValue() + 1);
						found = true;
					}
				}
				
				if( found == false ){
					contentTypesTemp.add( new NameIntPair( sigResult.getContentType(), 1) );
				}
			}
			
			contentTypes = contentTypesTemp;
			
			return contentTypes;
		}
		else {
			Connection connection = null;
			
			try{
				connection = Application.getApplication().getDatabaseConnection(DatabaseAccessType.SCANNER);
				contentTypes = getDiscoveredContentTypesInternal(scanResultId, connection);
				return contentTypes;
			}
			catch(SQLException e){
				Application.getApplication().logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
				return null;
			}
			catch(NoDatabaseConnectionException e){
				Application.getApplication().logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
				return null;
			}
			finally{
				try{
					if(connection != null ){
						connection.close();
					}
				}
				catch(SQLException e){
					Application.getApplication().logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	/**
	 * Create a list of the content types discovered. This method allows the list of content-types to be accessed without loading the entire
	 * result object.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 */
	public static Vector<NameIntPair> getDiscoveredContentTypes(long scanResultID ) throws NoDatabaseConnectionException, SQLException{
		
		// 0 -- Precondition check
		if( scanResultID < 0 ){
			throw new IllegalArgumentException("The scan result identifier must be zero or greater");
		}
		
		// 1 -- Retrieve the data
		Vector<NameIntPair> contentTypes = null;
		Connection connection = null;
		
		try{
			connection = Application.getApplication().getDatabaseConnection(DatabaseAccessType.SCANNER);
			contentTypes = getDiscoveredContentTypesInternal(scanResultID, connection);
			return contentTypes;
		}
		finally{
			if(connection != null ){
				connection.close();
			}
		}
	}
	
	public static MaxMinCount getScanResultInfo( long scanResultID, SignatureScanResultFilter filter, Application application ) throws SQLException, NoDatabaseConnectionException{
		return HttpDefinitionScanResult.getScanResultInfo(scanResultID, filter, application);
	}
	
	public MaxMinCount getScanResultInfo( SignatureScanResultFilter filter, Application application ) throws ScanResultLoadFailureException, SQLException, NoDatabaseConnectionException{
		return HttpDefinitionScanResult.getScanResultInfo( this.scanResultId, filter, application);
	}
	
	private static Vector<NameIntPair> getDiscoveredContentTypesInternal(long scanResultID, Connection connection ) throws SQLException, NoDatabaseConnectionException{
		
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		
		try{
			connection = Application.getApplication().getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			statement = connection.prepareStatement("select ContentType, count(*) as ContentTypeCount from SignatureScanResult inner join ScanResult on ScanResult.ScanResultID = SignatureScanResult.ScanResultID where ParentScanResultID = ? group by ContentType order by count(*) desc");
			statement.setLong(1, scanResultID);
			
			resultSet = statement.executeQuery();
			Vector<NameIntPair> contentTypes = new Vector<NameIntPair>();
			
			while( resultSet.next() ){
				String contentType = resultSet.getString("ContentType");
				int count = resultSet.getInt("ContentTypeCount");
				
				if( contentType == null ){
					contentTypes.add( new NameIntPair( "[unknown]", count ) );
				}
				else{
					contentTypes.add( new NameIntPair( contentType, count ) );
				}
			}
			
			return contentTypes;
		}
		finally{
			if( connection != null ){
				connection.close();
			}
			
			if( statement != null ){
				statement.close();
			}
			
			if( resultSet != null ){
				resultSet.close();
			}
		}
	}
	
	public HttpDefinitionScanResult[] getFindings(long start, int count, HttpDefinitionScanResult.SignatureScanResultFilter filter, boolean getResultsBefore) throws ScanResultLoadFailureException, SQLException, NoDatabaseConnectionException{
		// 1 -- Load the findings
		HttpDefinitionScanResult[] linkedResults = HttpDefinitionScanResult.loadScanResults(scanResultId, start, getResultsBefore, filter, count, Application.getApplication() );
		return linkedResults;
	}
	
	public HttpDefinitionScanResult[] getFindings(int count, boolean getResultsBefore) throws ScanResultLoadFailureException, SQLException, NoDatabaseConnectionException{
		// 1 -- Load the findings
		HttpDefinitionScanResult[] linkedResults = HttpDefinitionScanResult.loadScanResults(scanResultId, getResultsBefore, new HttpDefinitionScanResult.SignatureScanResultFilter(), count, Application.getApplication() );
		return linkedResults;
	}
	
	public HttpDefinitionScanResult[] getFindings(long start, int count, boolean getResultsBefore) throws ScanResultLoadFailureException, SQLException, NoDatabaseConnectionException{
		// 1 -- Load the findings
		HttpDefinitionScanResult[] linkedResults = HttpDefinitionScanResult.loadScanResults(scanResultId, start, getResultsBefore, new HttpDefinitionScanResult.SignatureScanResultFilter(), count, Application.getApplication() );
		return linkedResults;
	}
	
	private void loadFindings(){
		if( partialResults == true ){
			try{
				// 1 -- Load the findings
				ScanResult[] linkedResults = ScanResultLoader.getLinkedScanResults(scanResultId);
				findings = new HttpDefinitionScanResult[linkedResults.length];
				
				for(int c = 0; c < linkedResults.length; c++){
					findings[c] = (HttpDefinitionScanResult)linkedResults[c];
				}
				
				partialResults = false;
			}
			catch(ScanResultLoadFailureException e){
				Application.getApplication().logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e);
				partialResults = false;
			}
		}
	}
	
	@Override
	public long saveToDatabase(Connection connection, long scanRuleId)
			throws SQLException {
		
		// 0 -- Precondition check
		if( connection == null ){
			throw new IllegalArgumentException("The database connection cannot be null");
		}
		
		
		// 1 -- Save the main scan result record
		long scanResultId = saveToDatabaseInitial(connection, scanRuleId, HttpSeekingScanRule.RULE_TYPE);
		
		
		// 2 -- Save the specific scan result record
		
		PreparedStatement statement = null;
		
		try{
			statement = connection.prepareStatement("Insert into HttpDiscoveryResult (ScanResultID, Domain) values(?, ?)");
			statement.setLong(1, scanResultId);
			
			statement.setString(2, domain);
			
			statement.execute();
		}
		finally{
			if( statement != null){
				statement.close();
			}
		}
		
		for(int c = 0; c < findings.length; c++){
			
			HttpDefinitionScanResult result = findings[c];
			result.parentScanResultId = scanResultId;
			result.saveToDatabase(connection, -1);
		}
		
		updateCounts();
		saveToDatabaseFinalize(connection, scanResultId, getDeviations(), accepts, incompletes, scanRuleId);
		return scanResultId;
	}

}
