package net.lukeMurphey.nsia;

import java.sql.*;

import net.lukeMurphey.nsia.eventLog.EventLogMessage;
import net.lukeMurphey.nsia.scanRules.ScanRule;
import net.lukeMurphey.nsia.scanRules.ScanResult;
import net.lukeMurphey.nsia.scanRules.ScanRuleLoader;

/**
 * The scanner class faciltates the scanning of a scan rule.
 * @author luke
 *
 */
public class Scanner extends Thread{
	long ruleId;
	Application appRes;
	ScanRule scan;
	ScanResult scanResult;
	
	public Scanner( Application applicationResources ){
		//super("Scanner");
		
		// 0 -- Precondition check
		//	 0.1 -- Make sure the resource object is not null
		if( applicationResources == null )
			throw new IllegalArgumentException("Application resource cannot be null");
		
		appRes = applicationResources;
	}
	
	/**
	 * Set the rule to be scanned and prepare the class for scanning.
	 * @precondition The database connection must be available, the rule must be valid
	 * @postcondition The scan parameters will be prepared and ready for a call to run
	 * @param ruleId
	 * @throws Exception 
	 * @throws SQLException 
	 */
	public boolean prepareScan( long ruleId ) throws SQLException, Exception{
		
		// 1 -- Load the rule
		scan = ScanRuleLoader.getScanRule(ruleId);
		if( scan == null ){
			return false;
		}
		else{
			this.ruleId = ruleId;
			this.setName(scan.toString());
			return true;
		}
		
		/*
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the database connection exists
		Connection connection = null; //Will be checked in the try block below
		
		// 1 -- Get the rule information
		PreparedStatement ruleStatement = null;
		ResultSet result = null;
		
		try{
			connection = appRes.getDatabaseConnection( Application.DatabaseAccessType.SCANNER );
			
			if( connection == null )
				throw new NoDatabaseConnectionException();
			
			ruleStatement = connection.prepareStatement("Select * from ScanRule where ScanRuleID = ?");
			ruleStatement.setLong(1, ruleId);
			result = ruleStatement.executeQuery();
			
			if( result.next() ){ //The rule was found
				
				String ruleType = result.getString("RuleType");
				
				if( ruleType.matches( HttpStaticScanRule.RULE_TYPE) ){
					scan = new HttpStaticScanRule( appRes );
				}

				if( scan != null )
					scan.loadFromDatabase( ruleId );
				else
					return false;
				
				this.ruleId = ruleId;
				this.setName(scan.toString());
				
				return true;
			}
			else{
				return false;
			}
		} catch(Exception e){
			throw e;
		} finally {
			if (result != null )
				result.close();
			
			if (ruleStatement != null )
				ruleStatement.close();
			
			if (connection != null )
				connection.close();
			
		}
		*/
	}
	
	/**
	 * Method causes a new thread to be created which performs the scanning and retrieves the results. Note that the class must be
	 * prepared first with a successful call to prepareScan().
	 * @precondition The Rule ID must be set, the rule must have an associated scan class and a database connection must be available
	 * @postcondition The scan result field will contain the result of the scan results, or null if the scan failed
	 */
	public void run(){
		
		// 0 -- Precondition check
		if( ruleId < 1 )
			return;
		Connection connection = null;
		
		// 1 -- Run the rule
		try {
			scanResult = doScan();
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
			scanResult.saveToDatabase(connection,ruleId);
			
		} catch (SQLException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
		} catch (Exception e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e);
		}
		finally{
			try{
				if( connection != null )
					connection.close();
			}
			catch(Exception e){
				//Do nothing. This exception may leave a database connection open but the connection pooler should detect and free it in this unlikely scenario
			}
		}
		
	}
	
	/**
	 * Method performs the scanning and retrieves the results. Note that the class must be
	 * prepared first with a successful call to prepareScan().
	 * @throws Exception 
	 * @throws SQLException 
	 * @precondition The Rule ID must be set, the rule must have an associated scan class and a database connection must be available
	 * @postcondition The scan result will be returned null if the scan failed
	 */
	public ScanResult doScan() throws SQLException, Exception{
		// 0 -- Precondition check
		if( ruleId < 1 )
			return null;
		
		// 1 -- Do the scan
		ScanCallback callback = new ScanCallback(appRes);
		scan.setCallback(callback);
		ScanResult result = scan.doScan();
		return result;
	}
	
	/**
	 * Gets the rule ID that the scanner is set to scan.
	 * @precondition None
	 * @postcondition The ID associated with the rule will be returned (or -1 if not set).
	 * @return
	 */
	public long getRuleId(){
		return ruleId;
	}
	
	/**
	 * Method causes the results of the last scan to be saved to the database.
	 * @precondition Scan results must exist and a valid database connection must be available
	 * @postcondition The scan results will stored in the database according to the scan class specific schema 
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	public void saveScanResults() throws SQLException, NoDatabaseConnectionException{
		
		// 0 -- Preconditon Check
		
		//	 0.1 -- Make sure the scan results exist
		if( scanResult == null )
			throw new IllegalStateException("Scan results do not exist");
		
		//	 0.2 -- Make sure a database connection exists
		Connection connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
		
		if( connection == null )
			throw new NoDatabaseConnectionException();
		
		// 1 -- Save the results
		try{
			scanResult.saveToDatabase( connection, ruleId );
		}
		finally{
			if (connection != null )
				connection.close();
		}
	}
}
