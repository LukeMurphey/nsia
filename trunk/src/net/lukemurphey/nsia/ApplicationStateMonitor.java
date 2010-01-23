package net.lukemurphey.nsia;

import java.util.Hashtable;

public class ApplicationStateMonitor extends Thread {
	
	private int interval = 10;
	private int maxEntries = 500;
	static long lastDataCollection = 0;
	private int currentPointer = 0;
	private int length = 0;
	private boolean stopRunning = false;
	
	private int scannedDeviations = 0;
	private int scannedIncomplete = 0;
	private int scannedPassed = 0;
	
	Application application = null;
	ApplicationStateDataPoint[] metricsEntries;
	
	/**
	 * Main constructor
	 * @param interval
	 * @param entries
	 * @param manager
	 */
	public ApplicationStateMonitor( int interval, int entries, Application application ){
		super("Application State Monitor");
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the interval is valid
		if( interval > 0 )
			this.interval = interval;
		else
			this.interval = 10;
		
		//	 0.2 -- Make sure the manager reference is valid
		if( application == null )
			throw new IllegalArgumentException("Application reference cannot be null");
		else
			this.application = application;
		
		// 0.3 -- Make sure the entries argument is valid
		if( entries < 1 )
			throw new IllegalArgumentException("Entries must be greater than zero");
		else
			maxEntries = entries;
		
		
		// 1 -- Prepare the class
		metricsEntries = new ApplicationStateDataPoint[maxEntries];
	}
	
	/**
	 * The class below represents a single entry in the array of metrics results.
	 * @author luke
	 *
	 */
	public static class ApplicationStateDataPoint{
		public int threadCount;
		public long usedMemory;
		public int databaseConnections;
		public int rulesRejectedCount;
		public int rulesAcceptedCount;
		public int rulesIncompleteCount;
		
		public Hashtable<String, Object> toHashtable(){
			Hashtable<String, Object> hash = new Hashtable<String, Object>();
			
			hash.put("ThreadCount", Integer.valueOf(threadCount));
			hash.put("UsedMemory", Double.valueOf(usedMemory));
			hash.put("DatabaseConnections", Integer.valueOf(databaseConnections));
			
			hash.put("RejectedRules", Integer.valueOf(rulesRejectedCount));
			hash.put("AcceptedRules", Integer.valueOf(rulesAcceptedCount));
			hash.put("IncompleteRules", Integer.valueOf(rulesIncompleteCount));
			
			return hash;
		}
	}
	
	/**
	 * Start the metrics monitor.
	 */
	public void run(){
		enterMainLoop();
	}
	
	/**
	 * Shutdown the application state monitor.
	 *
	 */
	public void shutdown(){
		stopRunning = true;
		this.interrupt();
	}
	
	public void incrementIncompleteRulesCount(){
		synchronized(this){
			scannedIncomplete++;
		}
	}
	
	public void incrementFailedRulesCount(){
		synchronized(this){
			scannedDeviations++;
		}
	}

	public void incrementPassedRulesCount(){
		synchronized(this){
			scannedPassed++;
		}
	}
	
	/**
	 * Get the current data set.
	 * @return
	 */
	public ApplicationStateDataPoint[] getData(){
		synchronized(this){
			ApplicationStateDataPoint[] metrics = new ApplicationStateDataPoint[length];
		
			int entry = currentPointer;
			for( int c = 0; c < length; c++ ){ //maxEntries

				if( ( entry + 1 ) >= length )
					entry = 0;
				else
					entry++;

				ApplicationStateDataPoint dataPoint = new ApplicationStateDataPoint();
				dataPoint.threadCount = metricsEntries[ entry ].threadCount;
				dataPoint.usedMemory = metricsEntries[ entry ].usedMemory;
				dataPoint.databaseConnections = metricsEntries[ entry ].databaseConnections;

				dataPoint.rulesAcceptedCount = metricsEntries[ entry ].rulesAcceptedCount;
				dataPoint.rulesIncompleteCount = metricsEntries[ entry ].rulesIncompleteCount;
				dataPoint.rulesRejectedCount = metricsEntries[ entry ].rulesRejectedCount;
				metrics[c] = dataPoint;
			}
			
			return metrics;
		}
	}
	
	private void enterMainLoop(){
		
		while(stopRunning == false){
			try {
				sleep(interval * 1000);
			} catch (InterruptedException e) {
				/*
				 * This exception can safely be ignored, an interrupt will only be generated when the request is 
				 * made to shutdown the thread. This will cause the loop to be re-executed and thus terminated
				 * since the stopRunning field will be true.
				 */
			}
			
			generateMetrics();
		}
		
	}
	
	/**
	 * Generate the next round or metrics
	 *
	 */
	private void generateMetrics() {

		synchronized(this){
			// 1 -- Get memory, thread count and database connections
			long usedMem = application.getUsedMemory();
			int threadCount = application.getThreadCount();
			int databaseConnections = application.getDatabaseConnectionCount();

			// 2 -- Instantiate the data point
			ApplicationStateDataPoint newEntry = new ApplicationStateDataPoint();
			newEntry.threadCount = threadCount;
			newEntry.usedMemory = usedMem;
			newEntry.databaseConnections = databaseConnections;

			// 3 -- Get the rule scan results
			int rulesRejected = 0;
			int rulesAccepted = 0;
			int rulesIncomplete = 0;
			
			rulesRejected = scannedDeviations;
			rulesAccepted = scannedPassed;
			rulesIncomplete = scannedIncomplete;
			
			scannedDeviations = 0;
			scannedPassed = 0;
			scannedIncomplete = 0;
			
			/*
			Connection connection = null;
			PreparedStatement preparedStatement = null;
			ResultSet result = null;
			
			try{
				if( lastDataCollection == 0 )
					lastDataCollection = System.currentTimeMillis() - (10 * 1000);

				connection = Application.getApplication().getDatabaseConnection( Application.DatabaseAccessType.SCANNER );

				Timestamp timestampStart = new Timestamp(lastDataCollection);

				lastDataCollection = lastDataCollection + (10 * 1000);
				Timestamp timestampEnd = new Timestamp(lastDataCollection);

				// 3.1 -- Get the rules rejected
				preparedStatement = connection.prepareStatement("Select count(*) from ScanResult where ScanDate > ? and ScanDate <= ? and Deviations > 0");
				preparedStatement.setTimestamp( 1, timestampStart );
				preparedStatement.setTimestamp( 2, timestampEnd );
				result = preparedStatement.executeQuery();

				if( result.next() )
					rulesRejected = result.getInt(1);

				// 3.2 -- Get the rules that failed to execute
				preparedStatement = connection.prepareStatement("Select count(*) from ScanResult where ScanDate > ? and ScanDate <= ? and Deviations <= -1");
				preparedStatement.setTimestamp( 1, timestampStart );
				preparedStatement.setTimestamp( 2, timestampEnd );
				result = preparedStatement.executeQuery();

				if( result.next() )
					rulesIncomplete = result.getInt(1);

				// 3.3 -- Get the rules that accepted
				preparedStatement = connection.prepareStatement("Select count(*) from ScanResult where ScanDate > ? and ScanDate <= ? and Deviations = 0");
				preparedStatement.setTimestamp( 1, timestampStart );
				preparedStatement.setTimestamp( 2, timestampEnd );
				result = preparedStatement.executeQuery();

				if( result.next() )
					rulesAccepted = result.getInt(1);
			}
			catch( SQLException e ){
				Application.getApplication().logExceptionEvent( StringTable.MSGID_SQL_EXCEPTION, e );
			} catch (NoDatabaseConnectionException e) {
				Application.getApplication().logExceptionEvent( StringTable.MSGID_INTERNAL_ERROR, e );
			} finally {
				try{
					if( connection != null )
						connection.close();

					if( result != null )
						result.close();

					if( preparedStatement != null )
						preparedStatement.close();
				}
				catch(SQLException e ){
					Application.getApplication().logExceptionEvent( StringTable.MSGID_SQL_EXCEPTION, e );
				}
			}
			 */
			newEntry.rulesAcceptedCount = rulesAccepted;
			newEntry.rulesRejectedCount = rulesRejected;
			newEntry.rulesIncompleteCount = rulesIncomplete;

			// 4 -- Add the entry
			metricsEntries[currentPointer] = newEntry;

			// 5 -- Set the pointer for the next value
			if( (currentPointer + 1 ) >= maxEntries )
				currentPointer = 0;
			else
				currentPointer++;

			if( (length+1) != maxEntries )
				length++;
		}
	}
	

}
