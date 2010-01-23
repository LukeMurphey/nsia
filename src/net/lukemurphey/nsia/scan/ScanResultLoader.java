package net.lukemurphey.nsia.scan;

import java.sql.*;
import java.util.*;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.MaxMinCount;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.scan.ScanRule.ScanResultLoadFailureException;

/**
 * This class loads scan results from the database and creates a result that corresponds to the correct data
 * type.
 * @author luke
 *
 */
public class ScanResultLoader {
	
	/**
	 * Load the scan result corresponding to the result identifier.
	 * @param scanRuleId
	 * @param scanResultId
	 * @return
	 * @throws ScanResultLoadFailureException 
	 */
	private static ScanResult getScanResult( long scanRuleId, long scanResultId ) throws ScanResultLoadFailureException{
		
		// 1 -- Determine rule type and load basic inherited attributes
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet result = null;
		Exception initCause = null;
		
		try{
			connection = Application.getApplication().getDatabaseConnection( Application.DatabaseAccessType.SCANNER );
			
			if( scanResultId == -1){
				statement = connection.prepareStatement("Select * from ScanResult where ScanRuleID = ? order by ScanDate desc");
				statement.setLong(1, scanRuleId);
			}
			else{
				statement = connection.prepareStatement("Select * from ScanResult where ScanResultID = ?");
				statement.setLong(1, scanResultId);
			}
			
			statement.setFetchSize(1);
			result = statement.executeQuery();
			
			return getResult(result);
			
		} catch (NoDatabaseConnectionException e) {
			initCause = e;
			throw new ScanResultLoadFailureException("The scan result could not be loaded", e);
		} catch (SQLException e) {
			initCause = e;
			throw new ScanResultLoadFailureException("The scan result could not be loaded", e);
		} finally {
			
			try{
				if (result != null )
					result.close();
				
				if (statement != null )
					statement.close();
				
				if (connection != null )
					connection.close();
			}
			catch(SQLException ex){
				if(ex.getCause() == null && initCause != null) {
					ex.initCause(initCause);
				}
				
				throw new ScanResultLoadFailureException("The scan result could not be loaded", ex);
			}
		}
	}
	
	/**
	 * Get the last scan results for the given rule. 
	 * @param scanRuleId
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws ScanResultLoadFailureException 
	 */
	public static ScanResult getLastScanResult( long scanRuleId ) throws ScanResultLoadFailureException{
		return getScanResult( scanRuleId, -1 );
	}
	
	/**
	 * Get the scan result corresponding to the result identifier.
	 * @param scanResultId
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws ScanResultLoadFailureException 
	 */
	public static ScanResult getScanResult( long scanResultId ) throws ScanResultLoadFailureException{
		return getScanResult( -1, scanResultId );
	}
	
	/**
	 * Gets the scan results where the parent scan result is the given value
	 * @param scanResultId
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws ScanResultLoadFailureException 
	 */
	public static ScanResult[] getLinkedScanResults( long parentScanResultId ) throws ScanResultLoadFailureException{
		
		
		PreparedStatement statement = null;
		ResultSet result = null;
		Connection connection = null;
		Exception initCause = null;
		
		try{
			connection = Application.getApplication().getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
			Vector<ScanResult> scanResults = new Vector<ScanResult>();
			statement = connection.prepareStatement("Select * from ScanResult where ParentScanResultID = ?");
			statement.setLong(1, parentScanResultId);
			
			result = statement.executeQuery();
			
			boolean endFound = false;
			while( endFound == false ){
				ScanResult current = getResult(result);
				
				if( current == null ){
					endFound = true;
				}
				else
				{
					scanResults.add(current);
				}
			}
			
			ScanResult[] scanResultArray = new ScanResult[scanResults.size()];
			scanResults.toArray(scanResultArray);
			
			return scanResultArray;
			
		} catch (NoDatabaseConnectionException e) {
			initCause = e;
			throw new ScanResultLoadFailureException("The scan result could not be loaded", e);
		} catch (SQLException e) {
			initCause = e;
			throw new ScanResultLoadFailureException("The scan result could not be loaded", e);
		}
		finally{
			try{
				if( statement != null ){
					statement.close();
				}
				
				if( result != null ){
					result.close();
				}
				
				if( connection != null ){
					connection.close();
				}
			}
			catch(SQLException ex){
				if(ex.getCause() == null) {
					if(initCause != null) {
						ex.initCause(initCause);
					}
				}
			}
		}
		
		
	}
	
	private static ScanResult getResult( ResultSet result ) throws SQLException, NoDatabaseConnectionException, ScanResultLoadFailureException{
		
		// 1 -- Load the scan result from the result set
		
		//	 1.1 -- get the rule type
		String ruleType;
		if( result.next() ){
			ruleType = result.getString("RuleType");
		}
		else
			return null;
		
		//	 1.2 -- Get the necessary attributes
		long scanResultId = result.getLong("ScanResultID");
		long parentScanResultId = result.getLong("ParentScanResultID");
		long scanRuleId = result.getLong("ScanRuleID");
		int deviations = result.getInt("Deviations");
		int incompletes = result.getInt("Incompletes");
		int accepts = result.getInt("Accepts");
		ScanResultCode scanResultCode =ScanResultCode.getScanResultCodeById(result.getInt("ScanResultCode"));
		Timestamp scanTime = result.getTimestamp("ScanDate");
		
		
		// 2 -- Create the correct type of scan result
		ScanResult scanResult;
		
		
		//Type: HTTP Static Data
		if( ruleType.matches(HttpStaticScanRule.RULE_TYPE)){
			scanResult = HttpStaticScanResult.loadFromDatabase( scanRuleId, scanResultId, scanResultCode, scanTime, deviations, incompletes, accepts );
		}
		// Type: HTTP Auto Discovery
		else if( ruleType.matches(HttpSeekingScanRule.RULE_TYPE)){
			scanResult = HttpSeekingScanResult.loadFromDatabase( scanRuleId, scanResultId, scanResultCode, scanTime, deviations, incompletes, accepts );
		}
		// Type: HTTP Signature Scan
		else if( ruleType.matches(HttpDefinitionScanRule.RULE_TYPE)){
			scanResult = HttpDefinitionScanResult.loadFromDatabase( scanRuleId, scanResultId, scanResultCode, scanTime, deviations );
		}
		// Type: Service Scan
		else if( ruleType.matches(ServiceScanRule.RULE_TYPE)){
			scanResult = ServiceScanResult.loadFromDatabase( scanRuleId, scanResultId, scanResultCode, scanTime, deviations, incompletes, accepts );
		}
		//Type: unknown
		else{
			return null;
		}
		
		scanResult.setScanResultID(scanResultId);
		scanResult.parentScanResultId = parentScanResultId;
		return scanResult;
	}
	
	public static int getNumberOfScanResults( long scanRuleId ) throws NoDatabaseConnectionException, SQLException{
		
		// 1 -- Perform the operation
		PreparedStatement statement = null;
		ResultSet result = null;
		Connection connection = Application.getApplication().getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
		
		try{
			statement = connection.prepareStatement("Select count(*) from ScanResult where ScanRuleID = ?");
			statement.setLong(1, scanRuleId);
			
			result = statement.executeQuery();
			
			if( result != null && result.next() ){
				return result.getInt(1);
			}
			else
				return 0;
			
		}finally{
			if(statement != null)
				statement.close();
			
			if( result != null)
				result.close();
			
			if( connection != null )
				connection.close();
		}		
		
	}
	
	public static ScanResult[] getScanResults( long scanRuleId, int count) throws NoDatabaseConnectionException, SQLException, ScanResultLoadFailureException{
		return getScanResults( scanRuleId, -1, count);
	}
	
	public static ScanResult[] getScanResults( long scanRuleId, long firstScanResultId, int count ) throws NoDatabaseConnectionException, SQLException, ScanResultLoadFailureException{
		return getScanResults(scanRuleId, firstScanResultId, count, true);
	}
	
	public static long getMinEntry( long scanRuleId ) throws NoDatabaseConnectionException, SQLException{
		return getEntryNum(Application.getApplication(), scanRuleId, true);
	}
	
	public static long getMaxEntry( long scanRuleId ) throws NoDatabaseConnectionException, SQLException{
		return getEntryNum(Application.getApplication(), scanRuleId, false);
	}
	
	public static MaxMinCount getEntryInfo( long scanRuleId ) throws NoDatabaseConnectionException, SQLException{
		
		// 0 -- Precondition check
		if( scanRuleId < 0){
			throw new IllegalArgumentException("The scan rule ID must be greater than or equal to 0");
		}
		
		// 1 -- Load the results
		
		//	 1.1 -- Retrieve the results from the database
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		Connection connection = Application.getApplication().getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
		
		int max = -1;
		int min = -1;
		int count = 0;
		
		try{
			statement = connection.prepareStatement("Select Max(ScanResultID), Min(ScanResultID), Count(*) from ScanResult where ScanRuleID = ?");
			
			statement.setLong(1, scanRuleId);
			
			resultSet = statement.executeQuery();
			
			if( resultSet.next() ){
				max = resultSet.getInt(1);
				min = resultSet.getInt(2);
				count = resultSet.getInt(3);
			}
			
			return new MaxMinCount(max, min, count);
			
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
	
	private static long getEntryNum( Application application, long scanRuleId, boolean getMin ) throws NoDatabaseConnectionException, SQLException{
		
		// 0 -- Precondition check
		if( scanRuleId < 0){
			throw new IllegalArgumentException("The scan rule ID must be greater than or equal to 0");
		}
		
		// 1 -- Load the results
		
		//	 1.1 -- Retrieve the results from the database
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		Connection connection = application.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
		
		try{
			if( getMin == true ){
				statement = connection.prepareStatement("Select Min(ScanResultID) from ScanResult where ScanRuleID = ?");
			}
			else{
				statement = connection.prepareStatement("Select Max(ScanResultID) from ScanResult where ScanRuleID = ?");
			}
			
			statement.setLong(1, scanRuleId);
			
			resultSet = statement.executeQuery();
			
			if( resultSet.next() ){
				return resultSet.getLong(1);
			}
			else{
				return -1;
			}
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
	
	public static ScanResult[] getScanResults( long scanRuleId, long firstScanResultId, int count, boolean getResultsAfter ) throws NoDatabaseConnectionException, SQLException, ScanResultLoadFailureException{
		
		// 0 -- Precondition check
		if( count <= 0){
			throw new IllegalArgumentException("The number of items to return must be greater than 0");
		}
		
		// 1 -- Load the results
		
		//	 1.1 -- Retrieve the results from the database
		PreparedStatement statement = null;
		ResultSet result = null;
		Connection connection = Application.getApplication().getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
		
		try{
			if( firstScanResultId == -1 ){
				statement = connection.prepareStatement("Select * from ScanResult where ScanRuleID = ? order by ScanDate desc");
				
				statement.setMaxRows(count);
				statement.setLong(1,scanRuleId);
			}
			else{
				if( getResultsAfter == true ){
					statement = connection.prepareStatement("Select * from ScanResult where ScanResultID >= ? and ScanRuleID = ? order by ScanDate asc");
				}
				else{
					statement = connection.prepareStatement("Select * from ScanResult where ScanResultID <= ? and ScanRuleID = ? order by ScanDate desc");
				}
				
				statement.setMaxRows(count);
				statement.setLong(1,firstScanResultId);
				statement.setLong(2,scanRuleId);
			}
			
			result = statement.executeQuery();
			Vector<ScanResult> scanResults = new Vector<ScanResult>();
			
			boolean endFound = false;
			while(endFound == false){
				ScanResult scanResult = getResult(result);
				if( scanResult != null){
					scanResults.add(scanResult);
				}
				else{
					endFound = true;
				}
			}
			
			// 1.1.1 -- Convert the vector to an array
			ScanResult[] resultsArray = new ScanResult[scanResults.size()];
			
			if( getResultsAfter == true ){
				for( int c = 0; c < scanResults.size(); c++){
					resultsArray[resultsArray.length - 1 - c] = (ScanResult)scanResults.get(c);
				}
			}
			else{
				for( int c = 0; c < scanResults.size(); c++){
					resultsArray[c] = (ScanResult)scanResults.get(c);
				}
			}
			
			return resultsArray;
			
		}finally{
			if(statement != null)
				statement.close();
			
			if(result != null)
				result.close();
			
			if(connection != null)
				connection.close();
		}
		
	}

}
