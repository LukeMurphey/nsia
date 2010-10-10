package net.lukemurphey.nsia;

import java.sql.*;
import java.util.*;

import net.lukemurphey.nsia.SiteGroupManagement.SiteGroupDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogMessage.EventType;
import net.lukemurphey.nsia.scan.ScanResult;
import net.lukemurphey.nsia.scan.ScanResultLoader;
import net.lukemurphey.nsia.scan.ScanRule.ScanResultLoadFailureException;


/**
 * The scanner controller manages the individual scans. This class polls the database to determine which rules need scanning
 * and dispatches scan events accordingly. The scanner controller can be directed to perform gratuitous scans outside of the
 * normal schedule as well. The scanner controller does not automatically scan by default but requires a call to enableScanning() to
 * enable constant scanning. Subsequent calls to disableScanning() will stop the class from scanning. 
 * @author luke
 *
 */
public class ScannerController extends Thread{
	
	private int maxScanThreads = 30;
	
	// The following fields define the scanner actions and state
	private boolean scanningEnabled = false;
	private boolean activelyScanning = false;
	private boolean shutdownScanner = false;
	
	/*
	 * The minimum amount of time that must elapse before the rule is automatically scanned.
	 * This delay gives the user some time to continue editing the rule before it is automatically
	 * scanned. Additionally, this prevents a rule from being double-scanned when a user manually
	 * scans the rule after creating it.
	 */
	private static int SCAN_EDIT_DELAY_MINUTES = 3;
	
	/*
	 * Defines the amount of time the scanner should wait when first coming online. This is done
	 * in order to allow the host OS to get it's network interfaces up. Otherwise, all websites
	 * may look like they are offline since the network is not yet prepared.
	 */
	private static int DEFAULT_SCANNER_START_DELAY = 30 * 1000;
	
	// The application objects that contains the context and resources
	private Application appRes;
	
	// The frequency to check for new scan jobs requiring dispatch.
	private long loopFrequency = 10000; //10 seconds
	
	// The threads that are performing the scans.
	protected Vector<Thread> scanThreads = new Vector<Thread>();
	
	// Defines the various states that the scanenr may be in.
	public enum ScannerState{
		PAUSED,
		STARTING,
		RUNNING,
		TERMINATING,
		TERMINATED,
		PAUSING
	}
	
	public ScannerController(Application applicationResources){
		super("Scanner Controller");
		appRes = applicationResources;
	}
	
	/**
	 * Enables automatically scanning of rules.
	 */
	public void enableScanning(){
		scanningEnabled = true;
	}
	
	/**
	 * Disables automatically scanning of rules.
	 */
	public void disableScanning(){
		scanningEnabled = false;
	}
	
	public void run(){
		try {
			//Start the scanner enabled if it should be setup to scan by default
			scanningEnabled = appRes.getApplicationConfiguration().isDefaultScanningEnabled();
			
			/*
			 * Delay starting the scanner immediately. Sometimes, the host OS will still be bringing the interfaces online and we don't want
			 * this to look like the sites are offline.
			 */
			if( scanningEnabled ){
				sleep(DEFAULT_SCANNER_START_DELAY);
			}
			
			enterScanningLoop();
		} catch (SQLException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			return;
		} catch (Exception e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e );
			return;
		}
	}
	
	/**
	 * Get the state of the scanner controller (determine if it is scanning)
	 * @return
	 */
	public ScannerState getScanningState(){
		if( shutdownScanner && activelyScanning )
			return ScannerState.TERMINATING;
		else if ( shutdownScanner && !activelyScanning )
			return ScannerState.TERMINATED;
		else if ( scanningEnabled && !activelyScanning )
			return ScannerState.STARTING;
		else if ( scanningEnabled && activelyScanning )
			return ScannerState.RUNNING;
		else if ( !scanningEnabled && activelyScanning )
			return ScannerState.PAUSING;
		else //if ( !scanningEnabled && !activelyScanning )
			return ScannerState.PAUSED;
	}
	
	/**
	 * Determine if the scan controller is performing scans when necessary.
	 * @precondition None
	 * @postcondition A boolean will be returned indicating whether the controller is performing scans automatically
	 * @return
	 */
	public boolean scanningEnabled(){
		return scanningEnabled;
	}
	
	/**
	 * Sets the maximum number of threads the class will create for scanning purposes.
	 * @precondition The threadcount must be valid (greater that 1)
	 * @param threadCount
	 */
	public void setMaxScanThreads(int threadCount ){
		
		// 0 -- Precondition check
		
		//	 0.1 -- Is the thread count valid
		if( threadCount < 2 )
			throw new IllegalArgumentException("The maximum thread count must be greater that 1");
		
		// 1 -- Set the parameter
		maxScanThreads = threadCount;
	}
	
	/**
	 * This method causes the scanner controller to stop completely. Unlike, the disableScanning
	 * method, this method causes the scanner controller to stop entirely by ending the thread. 
	 *
	 */
	public void shutdown(){
		shutdownScanner = true;
	}
	/**
	 * This method initiates the main scanning loop where the scanning takes place
	 * @throws SQLException, Exception 
	 * @throws InterruptedException 
	 *
	 */
	public void enterScanningLoop() throws Exception{

		// 0 -- Precondition check
		Connection connection =null;

		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
			if( connection == null )
				throw new NoDatabaseConnectionException();

			long lastLoopStart = 0L;
			long tDelta;

			setPriority( 3 );

			while( shutdownScanner == false ){
				if( scanningEnabled == false ){
					try {
						//yield();
						Thread.sleep( 500 );
					} catch (InterruptedException e) {
						/*The interrupted exception is ignored for two reasons:
						 * 1) This exception should probably be unchecked anyways since it is only possible when a call to interrupt() occurs.
						 *    This application does not call interrupt() and the JRE does not call it directly either.
						 * 2) The function is only intended to prevent excessive CPU usage. An rare interruption is non-critical and will not change the data.*/ 
					}
				}

				if( scanningEnabled == true ){
					//yield();
					activelyScanning = true;

					// 1 -- Wait for the next scanning period
					if( lastLoopStart != 0 ){
						tDelta = System.currentTimeMillis() - lastLoopStart;
						if( tDelta < loopFrequency){
							try {
								//yield();
								Thread.sleep( loopFrequency - tDelta );
							} catch (InterruptedException e) {
								/*The interrupted exception is ignored for two reasons:
								 * 1) This exception should probably be unchecked anyways since it is only possible when a call to interrupt() occurs.
								 *    This application does not call interrupt() and the JRE does not call it directly either.
								 * 2) The function is only intended to prevent excessive CPU usage. An rare interruption is non-critical and will not change the data.*/ 
							}
						}
					}

					lastLoopStart = System.currentTimeMillis();

					// 2 -- Look for rules that require scanning and dispatch accordingly
					PreparedStatement ruleStatement = null;
					ResultSet ruleResults = null;

					SiteGroupManagement groupManagement = new SiteGroupManagement( Application.getApplication() );

					SiteGroupManagement.SiteGroupDescriptor[] siteGroupDescriptors = groupManagement.getGroupDescriptors();

					try{
						
						ruleStatement = getRuleScanStatement(connection, false);
						ruleResults = ruleStatement.executeQuery();		

						while( ruleResults.next() ){
							long ruleId = ruleResults.getLong("ScanRuleID");
							long siteGroupId = ruleResults.getLong("SiteGroupID");
							long scanFrequency = ruleResults.getLong("ScanFrequency");
							boolean scanDataObsolete = ruleResults.getBoolean("ScanDataObsolete");

							ScanResult lastScanResult = null;
							
							try{
								lastScanResult = ScanResultLoader.getLastScanResult( ruleId );
							}
							catch(ScanResultLoadFailureException e){
								appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
							}
							
							Timestamp lastScanned;

							if( lastScanResult != null ){ //If a scan result was found
								lastScanned = lastScanResult.getScanTime();
							}
							else{
								lastScanned = new Timestamp(0);
							}

							SiteGroupDescriptor siteGroupDescriptor = null;
							for( int c = 0; c < siteGroupDescriptors.length; c++){
								if( siteGroupId == siteGroupDescriptors[c].getGroupId() )
									siteGroupDescriptor = siteGroupDescriptors[c];
							}


							// 2.1 -- Do not scan the rule if it is associated with a site group that is disabled. or if it is not associated with a site group
							if( siteGroupDescriptor == null || siteGroupDescriptor.getGroupState() != SiteGroupManagement.State.ACTIVE ){
								/*
								 * Skip this rule, either one of the following is true:
								 *   1) The associated site group is not found (this must be an orphaned rule)
								 *   2) The associated site group is disabled (thus rules should no longer be scanned) 
								 */
							}
							
							// 2.2 -- Scan rules where the rule has changed and the result data needs to updated accordingly (i.e. the scan data is for an older version of the rule)
							else if( scanDataObsolete && appRes.getApplicationConfiguration().isRescanOnEditEnabled() ){
								dispatchScanner( ruleId );
							}
							
							// 2.3 -- Scan rules where the scan data has expired (and need re-scanning)
							else if( (System.currentTimeMillis() - lastScanned.getTime())/1000 > scanFrequency ){
								dispatchScanner( ruleId );
							}
						} // rules while loop

						// 3 -- Wait for the dispatched threads to complete
						while( scanThreads.size() > 0){
							for( int c = 0; c < scanThreads.size(); c++){
								Thread thread = (Thread) scanThreads.get(c);
								if( thread == null || !thread.isAlive() )
									scanThreads.remove(c);
							}
							//yield();
							Thread.sleep(200);
						} // dispatched threads join while loop
					}
					finally{
						if( ruleStatement != null )
							ruleStatement.close();

						if( ruleResults != null )
							ruleResults.close();
					}
				}//End of scanning while loop

				activelyScanning = false;

				// 4 -- Exiting now, make sure that the threads get a chance to finish (otherwise, partial results will be retained)
				long waitStart = System.currentTimeMillis();
				boolean waitTimeExceeded = false;

				while( scanThreads.size() > 0 ){ //Exit if the threads cannot complete within one minute
					if( (System.currentTimeMillis() - waitStart) < 60000 )
						waitTimeExceeded = true;
					for( int c = 0; c < scanThreads.size(); c++){
						Thread thread = (Thread) scanThreads.get(c);
						if( thread == null || !thread.isAlive() )
							scanThreads.remove(c);
					}
					//yield();
					Thread.sleep(200);
				}

				// Scanning threads not complete for 1 minute since scanner shutdown request; threads forcably terminated
				if( waitTimeExceeded ){
					// Log event via event log (that the scan threads need to be forced to terminate)
					appRes.logEvent( new EventLogMessage(EventType.SCAN_THREADS_FAILED_TO_TERMINATE) );
				}

			} // Main while loop

		}
		catch(Exception e){
			e.printStackTrace();
			throw e;
		}
		finally{// Connection closure finally statement
			if (connection != null )
				connection.close();
		}
	}
	
	/**
	 * Determine if the scanner controller is scanning the resources.
	 * @return
	 */
	public boolean isCurrentlyScanning(){
		return activelyScanning;
	}
	
	/**
	 * This method dispatches a thread to scan the given rule. The method will block if too many threads are allocated to
	 * wait until a thread completes execution. A scanner thread will not be dispatched if another thread is currently in
	 * the queue to scan the rule (prevents duplicate scanners threads).  
	 * @precondition The maximum thread count must be valid, otherwise, a single thread limit will be used. Furthermore, a thread will not be created if one is in the queue to scan the rule.
	 * @postcondition A thread will be dispatched to scan the given rule. Note that this may happen after blocking and waiting to dispatch a thread.
	 * @param ruleId
	 * @throws SQLException
	 * @throws Exception
	 */
	private synchronized void dispatchScanner( long ruleId ) throws Exception{
		
		// 1 -- Determine if a thread has already been created to scan the given rule
		for( int c = 0; c < scanThreads.size(); c++){
			Scanner scanner = (Scanner)scanThreads.get(c);
			if( scanner != null )
				if( scanner.getRuleId() == ruleId )
					return; //Don't dispatch, a thread is already scanning the rule
		}
		
		// 2 -- Determine if thread limit has been reached and block until it is free
		while( scanThreads.size() >= getMaxScanThreads() ){
			//Try cleaning out any expired threads
			for( int c = 0; c < scanThreads.size(); c++ ){
				Thread curThread = (Thread)scanThreads.get(c);
				if( curThread == null || !curThread.isAlive() )
					scanThreads.remove(c);
			}
			//yield();
			//Thread.sleep(500);
		}
		
		// 3 -- Dispatch the scanning thread
		Scanner scanner = new Scanner( appRes );
		if( scanner.prepareScan( ruleId ) ){
			scanner.start();
			scanThreads.add(scanner); //Add the thread to the list so that a maximum thread limit can be enforced
		}
	}
	
	private ResultSet getLastScanResult( long ruleId ) throws SQLException, NoDatabaseConnectionException{
		// 0 -- Precondition check
		
		//	 0.1 -- Rule ID must be valid
		//	 No specific precondition check required, a bad rule ID will result in no results
		
		// 1 -- Get the last scan result associated with the rule
		Connection conn = null;
		
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		
		try{
			conn = appRes.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
			statement = conn.prepareStatement("Select * from ScanResult where ScanRuleID = ? order by ScanDate Desc");
			statement.setLong(1, ruleId );
			resultSet = statement.executeQuery();
			
			if( resultSet.next() )
				return resultSet;
			else
				return null;
		} finally {
			if (resultSet != null )
				resultSet.close();
			
			if (statement != null )
				statement.close();
			
			if (conn != null )
				conn.close();
		}
	}
	
	/**
	 * Get the current limit for the number of active threads.
	 * @return
	 */
	public int getMaxScanThreads(){
		if( maxScanThreads < 1)
			return 1;
		else
			return maxScanThreads;
	}
	
	/**
	 * Method causes all rules whose need scanning based on the scan frequency to be re-scanned.
	 * @precondition The database connection must be available
	 * @postcondition All expired scan data will be rescanned and the data will be recorded in the database if the argument is so configured
	 * @param autoSaveResults
	 * @return
	 * @throws Exception 
	 */
	public ScanResult[] scanExpiredRules( boolean autoSaveResults ) throws Exception{
		
		// 0 -- Precondition check
		Connection connection = null; // Will be checked in try body
		
		// 1 -- Look for rules that require scanning and dispatch accordingly
		PreparedStatement ruleStatement = null;
		ResultSet ruleResults = null;
		
		Vector<ScanResult> scanResults = new Vector<ScanResult>();
		
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
			
			if( connection == null ){
				throw new NoDatabaseConnectionException();
			}
			
			ruleStatement = getRuleScanStatement(connection, false);
			
			ruleResults = ruleStatement.executeQuery();	
			
			while( ruleResults.next() ){
				
				long ruleId = ruleResults.getLong("ScanRuleID");
				long scanFrequency = ruleResults.getLong("ScanFrequency");
				boolean scanDataObsolete = ruleResults.getBoolean("ScanDataObsolete");
				
				ResultSet lastScanResults = getLastScanResult( ruleId );
				Timestamp lastScanned = lastScanResults.getTimestamp("ScanDate");
				
				// 2.1 -- Scan rules where the rule has changed and the result data needs to updated accordingly (i.e. the scan data is for an older version of the rule)
				if( scanDataObsolete ){
					
					Scanner scanner = new Scanner(appRes);
					
					//Perform the scan if the scanner successfully loaded the rule
					if( scanner.prepareScan( ruleId ) ){
						
						//Perform the scan
						ScanResult scanResult = scanner.doScan();
						
						if( scanResult != null ){
							
							//Add the scan results to the list
							scanResults.add(scanResult);
							
							//Save the results if they should be auto-saved
							if(autoSaveResults){
								
								Connection conn = appRes.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
								scanResult.saveToDatabase(conn, ruleId);
								
								if( conn != null ){
									conn.close();
								}
							}
						}
					}
				}
				// 2.2 -- Scan rules where the scan data has expired (and need re-scanning)
				else if( (System.currentTimeMillis() - lastScanned.getTime())/1000 > scanFrequency ){
					
					Scanner scanner = new Scanner(appRes);
					
					if( scanner.prepareScan( ruleId ) ){
						
						//Perform the scan
						ScanResult scanResult = scanner.doScan();
						
						if( scanResult != null ){
							
							//Add the scan results to the list
							scanResults.add(scanResult);
							
							//Save the results if they should be auto-saved
							if(autoSaveResults){
								
								Connection conn = appRes.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
								scanResult.saveToDatabase( conn, ruleId);
								
								if( conn != null){
									conn.close();
								}
							}
						}
					}
				}
			}
			
			// 2 -- Convert the vector to an array
			ScanResult[] scanResultsArray = new ScanResult[scanResults.size()];
			for( int c = 0; c < scanResults.size(); c++){
				ScanResult scanResult = (ScanResult)scanResults.get(c);
				scanResultsArray[c] = scanResult;
			}
			
			return scanResultsArray;
			
		} finally {
			if (ruleStatement != null )
				ruleStatement.close();
			
			if (ruleResults != null )
				ruleResults.close();
			
			if (connection != null )
				connection.close();
		}
	}
	
	/**
	 * Method performs scan on all rules that are marked as needing updating. These are rules that were modified and thus
	 * have obsoleted the existing scan data.
	 * @precondition A database connection must exist
	 * @postcondition The rules will scanned and the results saved if the argument requests it  
	 * @param autoSaveResults Causes the results to be saved in the database (to indicate that the rules were scanned)
	 * @return
	 * @throws Exception
	 */
	public ScanResult[] scanUpdatedRules( boolean autoSaveResults ) throws Exception{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure a database connection is available
		Connection connection = null; // Will be checked in try body
		
		// 1 -- Scan all rules that have been updated
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try{
			connection = appRes.getDatabaseConnection( Application.DatabaseAccessType.SCANNER );
			
			if( connection == null ){
				throw new NoDatabaseConnectionException();
			}
				
			statement = getRuleScanStatement(connection, true);
			result = statement.executeQuery();
			
			//	 1.1 -- Get the number of rules scanned
			int rulesScannedCount = 0;
			
			while (result.next()){
				rulesScannedCount++;
			}
			
			if(rulesScannedCount == 0 ){
				return null;
			}
			
			//	 1.2 -- Scan the rules
			result.first();
			
			ScanResult[] scanResults = new ScanResult[rulesScannedCount];
			
			int c = 0;
			while (result.next()){
				long scanRuleId =  result.getLong("ScanRuleID");
				
				Scanner scanner = new Scanner(appRes);
				scanner.prepareScan( scanRuleId );
				
				ScanResult scanResult = scanner.doScan();
				
				if( autoSaveResults ){
					scanResult.saveToDatabase(connection, scanRuleId );
				}
				
				scanResults[c] = scanResult;
			}
			
			// 2 -- Return the results
			return scanResults;
			
		} finally {
			if (result != null )
				result.close();
			
			if (statement != null )
				statement.close();
			
			if (connection != null )
				connection.close();
		}
	}
	
	/**
	 * Method causes the given rule to be scanned and the results returned.
	 * @precondition The Rule ID must be valid, the rule must have an associated scan class and a database connection must be available
	 * @postcondition The scan result will be returned null if the scan failed
	 * @param scanRuleId
	 * @return
	 * @throws SQLException
	 * @throws Exception
	 */
	public ScanResult scanRule( long scanRuleId, boolean autoSaveResults ) throws SQLException, Exception{
		// 0 -- Precondition check
		//the rule is checked in the subsequent call to doScan()
		
		// 1 -- Scan using the rule
		Scanner scanner = new Scanner(appRes);
		scanner.prepareScan(scanRuleId);
		
		ScanResult scanResult = scanner.doScan();
		if( autoSaveResults ){
			
			Connection connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
			scanResult.saveToDatabase( connection, scanRuleId );
			
			if( connection != null )
				connection.close();
		}
		
		return scanResult;
	}
	
	/**
	 * Get a SQL statement that will retrieve the scan rules to be scanned for the given site-group
	 * @param connection
	 * @param siteGroupID
	 * @return
	 * @throws SQLException
	 */
	private PreparedStatement getRuleScanStatement( Connection connection, boolean filterToObsoleted, long siteGroupID ) throws SQLException{
		PreparedStatement ruleStatement = null;
		
		if( filterToObsoleted ){
			ruleStatement = connection.prepareStatement("Select * from ScanRule where State = 1 and ScanDataObsolete <> 0 and (created = null or created < ?) and (modified = null or modified < ?) and SiteGroupID = ?");
		}
		else{
			ruleStatement = connection.prepareStatement("Select * from ScanRule where State = 1 and (created = null or created < ?) and (modified = null or modified < ?) and SiteGroupID = ?");
		}
		
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MINUTE, -SCAN_EDIT_DELAY_MINUTES);
		Timestamp earliestCreate = new Timestamp(calendar.getTime().getTime());
		
		ruleStatement.setTimestamp(1, earliestCreate);
		ruleStatement.setTimestamp(2, earliestCreate);
		ruleStatement.setLong(3, siteGroupID);
		
		return ruleStatement;
	}
	
	/**
	 * Get a SQL statement that will retrieve the scan rules to be scanned.
	 * @param connection
	 * @return
	 * @throws SQLException
	 */
	private PreparedStatement getRuleScanStatement( Connection connection, boolean filterToObsoleted ) throws SQLException{
		
		PreparedStatement ruleStatement;
		
		if( filterToObsoleted ){
			ruleStatement = connection.prepareStatement("Select * from ScanRule where State = 1 and ScanDataObsolete <> 0 and (created is null or created < ?) and (modified is null or modified < ?)");
		}
		else{
			ruleStatement = connection.prepareStatement("Select * from ScanRule where State = 1 and (created is null or created < ?) and (modified is null or modified < ?)");
		}
		
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MINUTE, -SCAN_EDIT_DELAY_MINUTES);
		Timestamp earliestCreate = new Timestamp(calendar.getTime().getTime());
		
		ruleStatement.setTimestamp(1, earliestCreate);
		ruleStatement.setTimestamp(2, earliestCreate);
		
		return ruleStatement;
	}
	
	/**
	 * Method causes all rules to be scanned.
	 * @param autoSaveResults Store the results in the database.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws Exception
	 */
	public ScanResult[] scanAllRules( boolean autoSaveResults ) throws Exception{
		
		// 0 -- Precondition check
		Connection connection = null; // Will be checked in try body
		
		
		// 1 -- Look for rules that require scanning and dispatch accordingly
		PreparedStatement ruleStatement = null;
		ResultSet ruleResults = null;
		
		Vector<ScanResult> scanResults = new Vector<ScanResult>();
		
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
			
			if( connection == null ){
				throw new NoDatabaseConnectionException();
			}
			
			ruleStatement = getRuleScanStatement(connection, false); 
			
			ruleResults = ruleStatement.executeQuery();		
			
			while( ruleResults.next() ){
				long ruleId = ruleResults.getLong("ScanRuleID");
				
				Scanner scanner = new Scanner(appRes);
				if( scanner.prepareScan( ruleId ) ){
					ScanResult scanResult = scanner.doScan();
					if( scanResult != null ){
						scanResults.add(scanResult);
						if(autoSaveResults){
							Connection dbConnection = appRes.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
							scanResult.saveToDatabase(dbConnection, ruleId);
							
							if( dbConnection != null )
								dbConnection.close();
						}
					}
				}
			}
		} finally {
			if (ruleResults != null )
				ruleResults.close();
			
			if (ruleStatement != null )
				ruleStatement.close();
			
			if (connection != null )
				connection.close();
		}
		
		// 2 -- Convert the vector to an array
		ScanResult[] scanResultsArray = new ScanResult[scanResults.size()];
		for( int c = 0; c < scanResults.size(); c++){
			ScanResult scanResult = (ScanResult)scanResults.get(c);
			scanResultsArray[c] = scanResult;
		}
		
		return scanResultsArray;
	}
	
	/**
	 * Method causes all rules associated with the given site group to be scanned.
	 * @param siteGroupId
	 * @param autoSaveResults
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws Exception
	 */
	public ScanResult[] scanSiteGroup( long siteGroupId, boolean autoSaveResults ) throws Exception{
		// 0 -- Precondition check
		Connection connection = null; //Will be checked in try body		
		
		// 1 -- Look for rules that require scanning and dispatch accordingly
		PreparedStatement ruleStatement = null;
		ResultSet ruleResults = null;
		
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
			
			if( connection == null ){
				throw new NoDatabaseConnectionException();
			}
			
			ruleStatement = getRuleScanStatement(connection, false, siteGroupId);
			
			ruleResults = ruleStatement.executeQuery();		
			Vector<ScanResult> scanResults = new Vector<ScanResult>();
			
			while( ruleResults.next() ){
				long ruleId = ruleResults.getLong("ScanRuleID");
				
				Scanner scanner = new Scanner(appRes);
				
				if( scanner.prepareScan( ruleId ) ){
					ScanResult scanResult = scanner.doScan();
					if( scanResult != null ){
						scanResults.add(scanResult);
						if(autoSaveResults){
							Connection conn = appRes.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
							scanResult.saveToDatabase(conn, ruleId);
							
							if( conn != null )
								conn.close();
						}
					}
				}
			}
			
			// 2 -- Convert the vector to an array
			ScanResult[] scanResultsArray = new ScanResult[scanResults.size()];
			for( int c = 0; c < scanResults.size(); c++){
				ScanResult scanResult = (ScanResult)scanResults.get(c);
				scanResultsArray[c] = scanResult;
			}
			
			return scanResultsArray;
		} finally {
			if (ruleStatement != null )
				ruleStatement.close();
			
			if (ruleResults != null )
				ruleResults.close();
			
			if (connection != null )
				connection.close();
		}
	}
	
	/**
	 * Method sets the time that the scanner controller will poll the rulesets for updates.
	 * @precondition The loop frequency must be > 500 msecs and < 1 hour
	 * @postcondition The loop frequency will be set and the controller will begin using the updated frequency at the next loop
	 * @param loopFreq
	 */
	public void setPollingLoopFrequency( long loopFreq ){
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the loop frequency is not invalid
		if( loopFreq < 500 )
			throw new IllegalArgumentException("Loop frequency time is excessively low");
		
		if( loopFreq > 3600000 )//.5 secs to 1 hour
			throw new IllegalArgumentException("Loop frequency time is excessively high");
		
		// 1 -- Set the frequency
		loopFrequency = loopFreq;
	}
	
	/**
	 * Retrieves the pooling loop frequency.
	 * @precondition None
	 * @postcondition The polling loop frequency will be returned
	 * @return
	 */
	public long getPollingLoopFrequency(){
		return loopFrequency;
	}	
}
