package net.lukemurphey.nsia.scan;

import java.sql.*;
import java.util.*;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.SiteGroupManagement;
import net.lukemurphey.nsia.SiteGroupScanResult;
import net.lukemurphey.nsia.Application.DatabaseAccessType;
import net.lukemurphey.nsia.SiteGroupManagement.SiteGroupDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogMessage.EventType;
import net.lukemurphey.nsia.scan.Definition.Severity;
import net.lukemurphey.nsia.scan.ScanRule.ScanResultLoadFailureException;

/**
 * This class is used for recalling the scan data and determining the state of scanned rules.
 * @author luke
 *
 */
public class ScanData  {
	
	protected Application application;
	protected SiteGroupManagement siteGroupManagement;
	
	public ScanData( Application app ){
		application = app;
		siteGroupManagement = new SiteGroupManagement(application);
	}
	
	/**
	 * Get all of the site group scan results.
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 * @throws ScanResultLoadFailureException 
	 * @throws NotFoundException 
	 */
	public SiteGroupScanResult[] getSiteGroupStatus() throws SQLException, NoDatabaseConnectionException, ScanResultLoadFailureException{
		
		// 1 -- Get the site group IDs
		Connection connection = null;
		
		//	 1.1 -- Get all active groups
		PreparedStatement siteGroupListStatment = null;
		ResultSet siteGroupResultSet = null;
		
		try{
			connection = application.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
			siteGroupListStatment = connection.prepareStatement("Select * from SiteGroups");
			siteGroupResultSet = siteGroupListStatment.executeQuery();
			
			Vector<SiteGroupScanResult> siteGroupResultsVector = new Vector<SiteGroupScanResult>(); 
			
			while( siteGroupResultSet.next() ){
				
				SiteGroupScanResult currentResult;
				
				try {
					currentResult = getSiteGroupStatus( siteGroupResultSet.getInt("SiteGroupID") );
				} catch (InputValidationException e) {
					Application.getApplication().getEventLog().logExceptionEvent(new EventLogMessage(EventType.INTERNAL_ERROR), e);
					currentResult = null;
				}catch (NotFoundException e) {
					Application.getApplication().getEventLog().logExceptionEvent(new EventLogMessage(EventType.INTERNAL_ERROR), e);
					currentResult = null;
				}
				
				if( currentResult != null ){
					siteGroupResultsVector.add(currentResult);
				}
			}
			
			//	 1.2 -- Produce the resulting array
			SiteGroupScanResult[] siteGroupResultsArray = new SiteGroupScanResult[siteGroupResultsVector.size()];
			
			for(int c = 0; c < siteGroupResultsVector.size(); c++){
				siteGroupResultsArray[c] = siteGroupResultsVector.get(c);
			}
			
			return siteGroupResultsArray;
		} finally {
			if (siteGroupResultSet != null )
				siteGroupResultSet.close();
			
			if (siteGroupListStatment != null )
				siteGroupListStatment.close();
			
			if (connection != null )
				connection.close();
		}
	}
	
	public DefinitionMatch[] getLastSignaturesMatched(int count ) throws NoDatabaseConnectionException, SQLException{
		
		// 0 -- Precondition check
		if( count <= 0){
			throw new IllegalArgumentException("The count of entries to return must be greater than zero");
		}
		
		
		// 1 -- Retrieve the data
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		
		try{
			connection = application.getDatabaseConnection(DatabaseAccessType.SCANNER);
			//statement = connection.prepareStatement("Select distinct RuleName, Severity, RuleID from MatchedRule"); //order by MatchedRuleID desc
			statement = connection.prepareStatement("Select RuleName, Severity, RuleID from MatchedRule inner join ScanResult on MatchedRule.ScanResultID = ScanResult.ScanResultID where ScanDate > ? group by RuleName, Severity, RuleID"); //
			statement.setMaxRows(count);
			
			// Calculate the date that is associated with one month ago
		    Calendar cal = Calendar.getInstance();
		    
		    // If the month is January, then set the year to last year and the month to December
		    if( cal.get(Calendar.MONTH) == 1 ){
		    	cal.set( Calendar.MONTH, Calendar.DECEMBER);
		    	cal.set( Calendar.YEAR, cal.get(Calendar.YEAR) - 1);
		    }
		    // If the month is not January, then just substract one from the month value
		    else{
		    	cal.set( Calendar.MONTH, cal.get(Calendar.MONTH) - 1);    	
		    }
		    
		    java.sql.Date date = new java.sql.Date( cal.getTime().getTime() );
			
			statement.setDate(1, date);
			
			resultSet = statement.executeQuery();
			
			Vector<DefinitionMatch> dataVector = new Vector<DefinitionMatch>();
			
			while ( resultSet.next() ){
				Severity severity;
				int severityID = resultSet.getInt("Severity");
				
				if( severityID == Severity.HIGH.ordinal() ){
					severity = Severity.HIGH;
				}
				else if( severityID == Severity.MEDIUM.ordinal() ){
					severity = Severity.MEDIUM;
				}
				else if( severityID == Severity.LOW.ordinal() ){
					severity = Severity.LOW;
				}
				else{
					severity = Severity.UNDEFINED;
				}
				
				DefinitionMatch entry = new DefinitionMatch( resultSet.getString("RuleName"), severity, resultSet.getInt("RuleID"));
				
				dataVector.add( entry );
			}
			
			DefinitionMatch[] signatureMatchArray = new DefinitionMatch[dataVector.size()];
			dataVector.toArray(signatureMatchArray);
			
			return signatureMatchArray;
		}
		finally{
			if( connection != null){
				connection.close();
			}
			
			if( statement != null){
				statement.close();
			}
			
			if( resultSet != null){
				resultSet.close();
			}
		}
		
		
	}
	
	/**
	 * Get the scan result status for the given site group.
	 * @param siteGroupId
	 * @return
	 * @throws SQLException
	 * @throws InputValidationException
	 * @throws NoDatabaseConnectionException 
	 * @throws NotFoundException 
	 * @throws ScanResultLoadFailureException 
	 */
	public SiteGroupScanResult getSiteGroupStatus( int siteGroupId ) throws SQLException, InputValidationException, NoDatabaseConnectionException, NotFoundException, ScanResultLoadFailureException{
		
		// 0 -- Make sure state and parameter is valid
		
		// 1 -- Get the site group information
		SiteGroupDescriptor siteGroupDesc = siteGroupManagement.getGroupDescriptor( siteGroupId );
		
		// 2 -- Get the last scan information by getting all rules and retrieving the scan results for the given rule
		ScanResult[] scanResults = getSiteGroupScanResults( siteGroupId );
		
		// 3 -- Create and return the descriptor
		return new SiteGroupScanResult(siteGroupId, siteGroupDesc, scanResults );
	}
	
	/**
	 * Retrieve the latest scan result for the given rule.
	 * @param ruleId
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 * @throws ScanResultLoadFailureException 
	 */
	public ScanResult getLastScanResult( long ruleId ) throws SQLException, NoDatabaseConnectionException, ScanResultLoadFailureException{
		
		ScanResult scanResult = ScanResultLoader.getLastScanResult( ruleId );
		return scanResult;
	}

	/**
	 * Retieve the requested scan result.
	 * @param scanResultId
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws ScanResultLoadFailureException 
	 */
	public ScanResult getScanResult( long scanResultId ) throws SQLException, NoDatabaseConnectionException, ScanResultLoadFailureException{
		
		ScanResult scanResult = ScanResultLoader.getScanResult( scanResultId );
		return scanResult;
	}
	
	/**
	 * Get the most recent scan results.
	 * @param ruleId
	 * @param count
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws ScanResultLoadFailureException 
	 */
	public ScanResult[] getLastScanResults( long ruleId, int count ) throws SQLException, NoDatabaseConnectionException, ScanResultLoadFailureException{
		
		return ScanResultLoader.getScanResults( ruleId, count );
	}

	/**
	 * Get the scan results starting from the given scan result identifier.
	 * @param ruleId
	 * @param scanResultId
	 * @param count
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws ScanResultLoadFailureException 
	 */
	public ScanResult[] getScanResults( long ruleId, long scanResultId, int count ) throws SQLException, NoDatabaseConnectionException, ScanResultLoadFailureException{
		return ScanResultLoader.getScanResults( ruleId, scanResultId, count );
	}
	
	/**
	 * Get the rule type string for the given rule.
	 * @param ruleId
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	public String getRuleType( long ruleId ) throws SQLException, NoDatabaseConnectionException{
		
		PreparedStatement statement = null;
		ResultSet result = null;
		Connection connection = null;
		
		try{
			connection = application.getDatabaseConnection( Application.DatabaseAccessType.SCANNER );
			statement = connection.prepareStatement("Select * from ScanRule where ScanRuleID = ?");
			statement.setLong(1, ruleId);
			result = statement.executeQuery();
			
			if( result.next() ){
				return result.getString("RuleType");
			}
			else
				return null;
		}
		finally{
			if( statement != null )
				statement.close();
			
			if( result != null )
				result.close();
			
			if (connection != null )
				connection.close();
		}
		
	}
	
	/**
	 * Get all of the scan results for the site group specified.
	 * @param siteGroupId
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 * @throws ScanResultLoadFailureException 
	 */
	public ScanResult[] getSiteGroupScanResults( long siteGroupId ) throws SQLException, NoDatabaseConnectionException, ScanResultLoadFailureException{
		
		// 1 -- Get scan results
		Connection connection = null;
		
		//	 1.1 -- Get all active rules
		PreparedStatement ruleStatement = null;
		ResultSet ruleResult = null;
		
		try{
			connection = application.getDatabaseConnection( Application.DatabaseAccessType.SCANNER );
			ruleStatement = connection.prepareStatement("Select * from ScanRule where SiteGroupID = ? and State = ?");
			ruleStatement.setLong(1, siteGroupId);
			ruleStatement.setLong(2, 1); //Get only valid rules (state = 1)
			ruleResult = ruleStatement.executeQuery();
			
			//	 1.2 -- Get the state of every rule
			Vector<ScanResult> scanResultList = new Vector<ScanResult>();
			
			while( ruleResult.next() ){
				try{
					long scanRuleId = ruleResult.getLong("ScanRuleID");
					
					// 1.2.1 -- Determine if the scan data is obsolete
					boolean isScanDataObsolete = ruleResult.getBoolean("ScanDataObsolete");
					
					// 1.2.2 -- Get the last scan result
					if( isScanDataObsolete == false ){
						ScanResult result = ScanResultLoader.getLastScanResult( scanRuleId );
					
						if( result != null ){
							scanResultList.add(result);
						}
					}
				}
				catch(ScanResultLoadFailureException e){
					application.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
				}
				
			}
			
			// 1.3 -- Convert to array
			ScanResult[] scanResultListArray = new ScanResult[scanResultList.size()];
			for( int c = 0; c < scanResultList.size(); c++){
				scanResultListArray[c] = scanResultList.get(c);
			}
			
			// 2 -- Return results
			return scanResultListArray;
		} finally {
			if (ruleResult != null )
				ruleResult.close();
			
			if (ruleStatement != null )
				ruleStatement.close();
			
			if (connection != null )
				connection.close();
		}
		
	}
	
	/*public SiteGroupScanResult[] getLastSiteGroupScanResults( ) throws SQLException, InputValidationException{
	 
	 // 0 -- Make sure state and parameter is valid
	  
	  // 1 -- Get the site groups
	   SiteGroupDescriptor[] siteGroupDesc = siteGroupManagement.getGroupDescriptors();
	   
	   // 2 -- Get the last scan information by getting all rules and retrieving the scan results for the given rule
	    for( int c = 0; c < siteGroupDesc.length; c++ ){
	    SiteGroupScanResult scanResult = new SiteGroupScanResult( siteGroupDesc[c].getGroupId(), );
	    
	    }
	    
	    Vector siteGroupScanResults = new Vector();
	    
	    
	    return null;
	    }*/
}
