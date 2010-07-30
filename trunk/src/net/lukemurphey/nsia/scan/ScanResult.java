package net.lukemurphey.nsia.scan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Hashtable;

import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.scan.ScanRule.ScanResultLoadFailureException;


/**
 * The scan result class provides a method to enclose the result of a scan. The constructor is protected to
 * prevent arbitrary creation of result objects.  
 * @author luke
 *
 */
public abstract class ScanResult {
	protected ScanResultCode resultCode = ScanResultCode.UNREADY;
	protected int deviations = -1;
	protected int incompletes = -1;
	protected int accepts = -1;
	protected Timestamp scanTime;
	protected long ruleId = -1;
	protected long scanResultId = VALUE_NOT_SET;
	protected long parentScanResultId = VALUE_NOT_SET;
	
	private final static long VALUE_NOT_SET = -1; 
	
	/**
	 * Get the number of deviations found in the scan. Note that a deviation occurs whenever the scan finds
	 * something that does not accepted by a rule.  
	 * @return
	 */
	public int getDeviations(){
		return deviations;
	}
	
	/**
	 * Get the number of attributes or resources that could not be completely scanned.  
	 * @return
	 */
	public int getIncompletes(){
		return incompletes;
	}
	
	/**
	 * Get the number of resources or attributes accepted by the scan.  
	 * @return
	 */
	public int getAccepts(){
		return accepts;
	}
	
	public abstract String getRuleType();
	
	/**
	 * The constructor is protected since only Scan classes are supposed to generate scan results.
	 * @param scanResultCode
	 */
	protected ScanResult( ScanResultCode scanResultCode, Timestamp timeOfScan ){
		resultCode = scanResultCode;
		scanTime = timeOfScan;
	}
	
	/**
	 * Loads the scan result from the specified database connection.
	 * @param connection
	 * @param scanResultId
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws ScanResultLoadFailureException
	 */
	protected ScanResult( Connection connection, long scanResultId ) throws SQLException, NoDatabaseConnectionException, ScanResultLoadFailureException{
		this.loadScanResultParams(connection, scanResultId);
	}
	
	/**
	 * Loads the scan result from the specified database connection.
	 * @throws ScanResultLoadFailureException 
	 * @throws SQLException 
	 */
	protected ScanResult( ResultSet result ) throws ScanResultLoadFailureException {
		try{
			if( loadScanResult(result) == false ){
				throw new ScanRule.ScanResultLoadFailureException("Scan result could not be loaded correctly (result set contained no data)");
			}
		}
		catch(SQLException e){
			throw new ScanRule.ScanResultLoadFailureException("Scan result could not be loaded correctly", e);
		}
	}
	
	/**
	 * Get the rule identifier associated with the scan result. Returns -1 if no rule is associated with the scan result.
	 * @return
	 */
	public long getRuleID(){
		return ruleId;
	}
	
	/**
	 * Sets the scan result identifier.
	 * @return
	 */
	protected void setScanResultID(long scanResultId){
		
		// 0 -- Precondition check
		if( scanResultId < VALUE_NOT_SET )
			throw new IllegalArgumentException("The scan result identifier is invalid");
		
		// 1 -- Set the value
		this.scanResultId = scanResultId;
	}
	
	/**
	 * Returns the scan result identifier.
	 * @return
	 */
	public long getScanResultID(){
		return scanResultId;
	}
	
	/**
	 * Returns the state of the status code
	 * @precondition None
	 * @postcondition The current result code will be returned
	 * @return
	 */
	public ScanResultCode getResultCode(){
		return resultCode;
	}
	
	/**
	 * Get the identifier of the scan result that contains this entry (note that -1 will be returned of a parent does not exist).
	 * @return
	 */
	public long getParentScanResultID(){
		return parentScanResultId;
	}
	
	/**
	 * Get the time that the scan was performed.
	 * @return
	 */
	public Timestamp getScanTime(){
		return scanTime;
	}
	
	/**
	 * Retrieve a hashtable representation of the class contents.
	 * @return
	 */
	public Hashtable<String, Object> toHashtable(){
		Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
		
		hashtable.put("ResultCode", Integer.valueOf( getResultCode().getId() ) );
		hashtable.put("ScanTime",  Double.valueOf( scanTime.getTime() ) );
		hashtable.put("Deviations", Integer.valueOf( deviations ) );
		
		return hashtable;
	}
	
	/**
	 * This method causes the class to store the results in the database (using the supplied
	 * connection).
	 * @param connection
	 * @param scanRuleId
	 * @return
	 */
	public abstract long saveToDatabase( Connection connection, long scanRuleId ) throws SQLException;
	
	/**
	 * This method creates the initial parent record for the result record. It must be followed
	 * by a call to saveToDatabaseFinalize() to make the record official. This is broken into a two step
	 * process so that the auto-generated key for the scan result record can be determined without fear
	 * of causing a crash due to a race condition. The deviation count will be set to -1 to indicate that
	 * the record is not yet valid. The call to saveToDatabaseFinalize() will make the record valid.
	 * @param connection
	 * @param scanRuleId
	 * @return
	 * @throws SQLException 
	 */
	protected long saveToDatabaseInitial( Connection connection, long scanRuleId, String ruleType ) throws SQLException{
		// 0 -- Precondition check
		if( connection == null )
			return -1;
		
		// 1 -- Save the record
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try{
			statement = connection.prepareStatement("Insert into ScanResult(ScanRuleID, RuleType, ScanDate, Deviations, ScanResultCode, ParentScanResultID) values (?, ?, ?, -1, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
			statement.setLong(1, scanRuleId);
			statement.setString(2, ruleType);
			statement.setTimestamp(3, scanTime);
			statement.setInt(4, resultCode.getId());
			statement.setLong(5, this.parentScanResultId);
			
			if( statement.executeUpdate() < 0 )
				return -1;
			
			// 2 -- Get the result
			result = statement.getGeneratedKeys();
			if( result.next() ){
				return result.getLong(1);
			}
			else{
				return -1;
			}
			
		} finally {
			if (result != null )
				result.close();
			
			if (statement != null )
				statement.close();
		}
	}
	
	/**
	 * This method sets a temporary scan result as valid.
	 * @precondition The deviation count must not be less than zero and the database connection must be valid, and a temporary scan result must exist that is associated with the scan result ID.
	 * @postcondition The temporary database tuple for the scan result will be set to the given deviation count, indicating it is complete. 
	 * @param connection
	 * @param scanResultId
	 * @param deviations
	 * @param scanRuleId
	 * @throws SQLException 
	 */
	protected boolean saveToDatabaseFinalize( Connection connection, long scanResultId, long deviations, long scanRuleId ) throws SQLException{
		return saveToDatabaseFinalize(connection, scanResultId, deviations, 0, 0, scanRuleId);
	}
	
	/**
	 * This method sets a temporary scan result as valid.
	 * @precondition The deviation count must not be less than zero and the database connection must be valid, and a temporary scan result must exist that is associated with the scan result ID.
	 * @postcondition The temporary database tuple for the scan result will be set to the given deviation count, indicating it is complete. 
	 * @param connection
	 * @param scanResultId
	 * @param deviations
	 * @param accepts
	 * @param incompletes
	 * @param scanRuleId
	 * @throws SQLException 
	 */
	protected boolean saveToDatabaseFinalize( Connection connection, long scanResultId, long deviations, long accepts, long incompletes, long scanRuleId ) throws SQLException{
		// 0 -- Precondition check
		if( connection == null ){
			return false;
		}
		
		if( deviations < 0 ){
			throw new IllegalArgumentException("Deviation count cannot be less than 0");
		}
		
		if( accepts < 0 ){
			throw new IllegalArgumentException("Accepted count cannot be less than 0");
		}
		
		if( incompletes < 0 ){
			throw new IllegalArgumentException("Incomplete count cannot be less than 0");
		}
		
		// 1 -- Save the record
		PreparedStatement statement = null;
		PreparedStatement statementUpdate = null;
		
		try{
			statement = connection.prepareStatement("Update ScanResult set Deviations = ?, Incompletes = ?, Accepts = ? where ScanResultID = ?");
			statement.setLong(1, deviations);
			statement.setLong(2, incompletes);
			statement.setLong(3, accepts);
			statement.setLong(4, scanResultId);
			
			if( statement.executeUpdate() < 0 )
				return false;
			
			// 2 -- Note that rule has been evaluated and the scan data is thus no longer obsolete
			if( scanRuleId >= 0 ){
				statementUpdate = connection.prepareStatement("Update ScanRule set ScanDataObsolete = ? where ScanRuleID = ?");
				statementUpdate.setLong(1, 0);
				statementUpdate.setLong(2, scanRuleId);
				
				statementUpdate.executeUpdate();
			}
			
			// 3 -- return
			this.scanResultId = scanResultId;
			this.ruleId = scanRuleId;
			return true;
		} finally {
			if (statementUpdate != null )
				statementUpdate.close();
			
			if (statement != null )
				statement.close();
		}
	}
	
	public abstract String getSpecimenDescription();
	
	/**
	 * Load the basic (inherited) scan result attributes from the result set.
	 * @param result
	 * @return
	 * @throws SQLException
	 */
	protected boolean loadScanResult( ResultSet result ) throws SQLException{
		// 0 -- Precondition check
		//	 0.1 -- Make sure the database connection exists
		// (the database connection will be checked in the try block)
		
		//	 0.2 -- Ensure the scan result ID is valid
		
		// 1 -- Load the basic scan result attributes		
		try{
			
			if( result == null || !result.next() ){
				return false;//Record was not found
			}
			
			this.resultCode = ScanResultCode.getScanResultCodeById(result.getInt("ScanResultCode"));
			 
			this.scanTime = result.getTimestamp("ScanDate");
			this.ruleId = result.getLong("ScanRuleID");
			this.deviations = result.getInt("Deviations");
			this.accepts = result.getInt("Accepts");
			this.incompletes = result.getInt("Incompletes");
			this.parentScanResultId = result.getLong("ParentScanResultID");
			
			return true;
		} finally {
			
			if (result != null )
				result.close();
		}
	}
	
	protected boolean loadScanResultParams( Connection connection, long scanResultId ) throws SQLException, NoDatabaseConnectionException, ScanResultLoadFailureException{ 
		
		// 0 -- Precondition check
		//	 0.1 -- Make sure the database connection exists
		// (the database connection will be checked in the try block)
		
		//	 0.2 -- Ensure the scan result ID is valid
		
		// 1 -- Load the basic scan result attributes
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try{
			if( connection == null )
				throw new NoDatabaseConnectionException();
			
			statement = connection.prepareStatement("Select * from ScanResult where ScanResultID = ?");
			statement.setLong(1, scanResultId);
			result = statement.executeQuery();
			
			return loadScanResult(result);
			
		} finally {
			
			if (result != null )
				result.close();
			
			if (statement != null )
				statement.close();
		}
	}
	
}

