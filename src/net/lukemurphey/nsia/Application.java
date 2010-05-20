package net.lukemurphey.nsia;

import com.martiansoftware.jsap.*;

import java.io.*;
import java.sql.*;

import java.util.*;
import java.util.Date;




import net.lukemurphey.nsia.ApplicationStateMonitor.ApplicationStateDataPoint;
import net.lukemurphey.nsia.DatabaseInitializer.DatabaseInitializationState;
import net.lukemurphey.nsia.WorkerThread.State;
import net.lukemurphey.nsia.console.ConsoleListener;
import net.lukemurphey.nsia.eventlog.EventLog;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogSeverity;
import net.lukemurphey.nsia.eventlog.MessageFormatter;
import net.lukemurphey.nsia.eventlog.MessageFormatterFactory;
import net.lukemurphey.nsia.eventlog.SyslogNGAppender;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;

import org.apache.commons.dbcp.*;

import java.net.BindException;

/**
 * This class contains a list of prerequisite resources that are necessary for each class to operate.
 * @author luke
 *
 */
public final class Application {
	
	private static Application appRes;
	
	private EventLog eventlog = null;
	private ApplicationConfiguration appConfig = null; // NOPMD by luke on 5/26/07 10:50 AM
	
	public static final String APPLICATION_NAME = "NSIA";
	public static final String APPLICATION_VENDOR = "ThreatFactor";
	
	public static final int VERSION_MAJOR = 0;
	public static final int VERSION_MINOR = 8;
	public static final int VERSION_REVISION = 103;
	public static final String VERSION_STATUS = null;
	public static final String DATABASE_LOCATION = "../var/database";
	
	private final Object metricsMonitorMutex = new Object();
	
	public static final int THREAD_WARNING_THRESHOLD = 50; 
	public static final int THREAD_CRITICAL_THRESHOLD = 80;
	
	public static final long MEMORY_WARNING_THRESHOLD = 70; 
	public static final long MEMORY_CRITICAL_THRESHOLD = 90;
	
	public static final long DBCONNECTION_WARNING_THRESHOLD = 30; 
	public static final long DBCONNECTION_CRITICAL_THRESHOLD = 40;
	
	private ApplicationStateMonitor metricsMonitor;
	
	private static String buildNumber = null;
	
	//The following is a list of database connections configured for specific functions
	public enum DatabaseAccessType{
		USER_QUERY,
		ADMIN,
		PERMISSIONS,
		FIREWALL,
		EVENT_LOG,
		SESSION,
		SCANNER,
		ACTION,
		USER_UPDATE
	}
	
	//Indicates where the shutdown command is generated from
	public enum ShutdownRequestSource{
		UNSPECIFIED,
		API,
		CLI
	}
	
	//
	public enum RunMode{
		CLI,
		GUI,
		SERVICE
	}
	
	/* 
	 * This boolean indicates if the application is already being shutdown.
	 * This is used to ensure that any shutdown hooks don't attempt to start
	 * another shutdown sequence after one has already began.
	 */
	private Boolean shutdownInProgress = Boolean.FALSE; 
	
	private String databaseLocation;
	private String databaseDriver;
	private final Firewall firewall;
	private final ScannerController scannerController;
	private final NetworkManager manager;
	private SessionManagement sessionManagement; // NOPMD by luke on 5/26/07 10:41 AM
	private long startTime = 0; // NOPMD by luke on 5/26/07 10:44 AM
	private org.apache.commons.dbcp.BasicDataSource connectionBroker;
	private boolean usingInternalDatabase;
	private Vector<WorkerThreadDescriptor> workerThreadQueue = new Vector<WorkerThreadDescriptor>();
	private RunMode runMode;
	private Timer timer;
	private ReindexerWorker reindexer = null;
	
	public static class WorkerThreadDescriptor{
		
		protected WorkerThreadDescriptor( WorkerThread thread, String hash){
			this.thread = thread;
			this.hash = hash;
			this.userId = -1;
		}
		
		/*protected WorkerThreadDescriptor( int workerId, WorkerThread thread, String hash ){
			this.thread = thread;
			this.hash = hash;
			this.workerId = workerId;
		}*/
		
		protected WorkerThreadDescriptor( WorkerThread thread, String hash, int userId){
			this.thread = thread;
			this.hash = hash;
			this.userId = userId;
		}
		
		protected String hash;
		protected WorkerThread thread;
		protected int userId = -1;
		//protected int workerId;
		
		public String getUniqueName(){
			return hash;
		}
		
		public WorkerThread getWorkerThread(){
			return thread;
		}
		
		public int getUserID(){
			return userId;
		}
		
		/*public int getWorkerID(){
			return workerId;
		}*/
	}
	
	private class TimerTaskWorker extends TimerTask{
		
		private WorkerThread worker;
		private String uniqueName;
		
		public TimerTaskWorker(WorkerThread worker, String uniqueName){
			this.worker = worker;
			this.uniqueName = uniqueName;
		}
		
		public void run(){
			try{
				addWorkerToQueue(worker, uniqueName);
				Thread thread = new Thread(worker, uniqueName);
				thread.start();
			}
			catch(DuplicateEntryException e){
				//Ignore this exception, the task was already started.
			}
		}
	}
	
	public Application(){
		//This constructor instantiates a blank application and is intended only for test cases.
		firewall = null;
		scannerController = null;
		manager = null;
	}
	
	public Application( boolean connecToDatabase ) throws NoDatabaseConnectionException{
		//This constructor instantiates a application object that is intended only for test cases.
		firewall = null;
		scannerController = null;
		manager = null;
		
		if( connecToDatabase ){
			try{
				connectToInternalDatabase(false);
			}
			catch(Exception e){
				throw new NoDatabaseConnectionException(e);
			}
			
			appRes = this;
		}
	}
	
	/**
	 * Default constructor
	 * @throws JSAPException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 *
	 */
	public Application( String[] args ) throws JSAPException, NoDatabaseConnectionException{
		
		// 0 -- Perform basic startup routines
		
		//	 0.1 -- Set the user agent identification
		System.getProperties().setProperty("httpclient.useragent", "NSIA");
		
		// 1 -- Parse the command-line arguments
		
		// First, find out if the application was initiated using the startup routine
		if( args.length == 4 && args[0].equalsIgnoreCase("--install")){
			System.out.println("Completing installation");
			completeInstall(args[1], args[3], args[2]);
			//The function above should not return, therefore, don't let it continue just in case.
			System.exit(-1);
		}
		
		JSAP jsap;
		jsap = getCommandLineProcessor();
		JSAPResult commandLineData = jsap.parse( args );
		
		//Show help if the command-line data parsing failed
		if( !commandLineData.success() ){
			System.err.println(); // NOPMD by luke on 5/26/07 10:42 AM
            System.err.println("Usage: " + APPLICATION_NAME); // NOPMD by luke on 5/26/07 10:42 AM
            System.err.println( jsap.getHelp() ); // NOPMD by luke on 5/26/07 10:43 AM
            System.exit(1);
		}
		
		// 2 -- Load the configuration file
		Properties properties = null;
		
		try{
			properties = loadConfigFile(commandLineData.getString("configFile"));
		}
		catch (IOException e) {
			System.err.println("Configuration file could not be loaded: " + e.getMessage()); // NOPMD by luke on 5/26/07 10:43 AM
			System.exit(-1);
		}
		
		// 3 -- Create the event log facility
		eventlog = new EventLog();
		
		// 4 -- Connect to the database
		connectToDatabase(properties);
		
		appRes = this;
		
		// 5 -- Create the application parameter and configuration manager
		appConfig = new ApplicationConfiguration( this );
		eventlog.setApplication(this);
		
		try {
			eventlog.loadHooks();
		} catch (SQLException e) {
			logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e );
		}
		
		scannerController = new ScannerController( this );
		firewall = new Firewall( this );
		//firewall.loadFirewallRulesFromDatabase();
		
		String siteSentryClientId = null;
		try {
			siteSentryClientId = appConfig.getHttpClientId();
			
			System.getProperties().setProperty("httpclient.useragent", siteSentryClientId );
		}
		catch (NoDatabaseConnectionException e1) {
			System.out.println( e1.getMessage() );
			//logExceptionEvent( StringTable.MSGID_INTERNAL_ERROR, e1 ); //This will
			//System.getProperties().setProperty("httpclient.useragent", "SiteSentry " + getVersion() );
		}
		catch (InputValidationException e1) {
			System.out.println( e1.getMessage() );
			//logExceptionEvent( StringTable.MSGID_INTERNAL_ERROR, e1 ); //This will
			//System.getProperties().setProperty("httpclient.useragent", "SiteSentry " + getVersion() );
		}
		catch (SQLException e1) {
			System.out.println( e1.getMessage() );
			//logExceptionEvent( StringTable.MSGID_INTERNAL_ERROR, e1 ); //This will
			//System.getProperties().setProperty("httpclient.useragent", "SiteSentry " + getVersion() );
		}
		
		// 6 -- Instantiate the manager
		manager = new NetworkManager();
		startTime = System.currentTimeMillis();
		
		// 7 -- Start the metrics monitor
		startMetricsMonitor();
		
		ShutdownHook shutdownHook = new ShutdownHook();
		Runtime.getRuntime().addShutdownHook(shutdownHook);
		
		// 8 -- Start the scheduled background tasks
		startTasks();
	}
	
	/*private int getWorkerThreadID(){
		Random generator = new Random();
		
		ListIterator<WorkerThreadDescriptor> iterator = workerThreadQueue.listIterator();
		
		boolean randValueFound = true;
		int randValue = 1;
		
		while( randValueFound == true){
			randValueFound = false;
			
			randValue = generator.nextInt();
			
			while( iterator.hasNext() ){
				if( iterator.next().workerId == randValue ){
					randValueFound = true;
				}
			}
		}
		
		return randValue;
	}*/
	
	private void completeInstall( String username, String password, String realName ){
		connectToDatabase( null );
		UserManagement userManagement = new UserManagement(this);
		
		try{
			if( userManagement.addAccount(username, realName, password, "SHA-512", 10000, null, true) < 0){
				System.exit(-1);
			}
			else{
				System.exit(0);
			}
		}
		catch(Exception e){
			System.exit(-1);
		}
	}
	
	private void connectToDatabase( Properties properties ){
		try {
			
			//4.1a -- Use the configured database (if the config file exists)
			if( properties != null && properties.getProperty("Database.Location") != null ){
				/* 
				 * If the user specified a database connection in the configuration file, then
				 * connect to the external database as specified.
				 */
				if( !connectToDatabase(
						properties.getProperty("Database.Location"),
						properties.getProperty("Database.Password"),
						properties.getProperty("Database.Driver")) ){
					System.err.println("Database connection failed, application terminating"); // NOPMD by luke on 5/26/07 10:43 AM
					System.exit(-1);
				}
			}
			
			//4.1b -- Use the internal database if an external database connection was not specified
			else{

				try{
					connectToInternalDatabase(false);
				}
				// Try to identify the cause for the database exception and act accordingly
				catch( SQLNestedException e){
					SQLException nextException =  (SQLException)e.getCause();
					
					// 4.1.1b -- Determine if the exception is due to another JVM using the database
					if( nextException != null &&
							nextException.getNextException() != null &&
							nextException.getNextException().getMessage().startsWith("Another instance of Derby may have already booted the database") ){
						System.err.println("Database connection failed: another instance of the application may already be using the database. Shutdown the other instance and try restarting the application");
						System.exit(-1);
					}
					
					// 4.1.2b -- If the error is due to the fact that the database does not exist, then try to create it
					if( e.getMessage().equalsIgnoreCase("Cannot create PoolableConnectionFactory (Database '" + DATABASE_LOCATION + "' not found.)")){
						System.out.print("Creating and initializing the internal database...");
						try{
							connectToInternalDatabase(true);
						}
						catch( SQLException e1){
							System.err.println("Database initialization failed: " + e1.getMessage());
							System.exit(-1);
						}
						catch( NoDatabaseConnectionException e1){
							System.err.println("Database initialization failed: " + e1.getMessage());
							System.exit(-1);
						}
						
						System.out.println("Done");
					}
					
					
					// 4.1.3c -- Print out the inner exception and fail 
					else if( nextException != null ){
						System.err.println("Database connection failed: " + nextException.getMessage() );
						System.exit(-1);
					}
				}
				catch( SQLException e){
					System.err.println("Database connection failed: " + e.getMessage() );
					System.err.println("Application terminating" );
					System.exit(-1);
				}
				catch( NoDatabaseConnectionException e){
					System.err.println("Database connection failed: " + e.getMessage() );
					System.err.println("Application terminating" );
					System.exit(-1);
				}
			}
		} catch (InstantiationException e) {
			System.err.println("Database driver could not be loaded");
			System.exit(-1);
		} catch (IllegalAccessException e) {
			System.err.println("Database driver could not be loaded");
			System.exit(-1);
		} catch (ClassNotFoundException e) {
			System.err.println("Database driver could not be loaded");
			System.exit(-1);
		}
	}
	
	/**
	 * Add a thread to the queue of background tasks.
	 * @param thread
	 * @throws DuplicateEntryException 
	 */
	public WorkerThreadDescriptor addWorkerToQueue(WorkerThread thread, String uniqueName) throws DuplicateEntryException{
		return addWorkerToQueue(thread, uniqueName, -1);
	}
	
	/**
	 * Add a thread to the queue of background tasks.
	 * @param thread
	 * @throws DuplicateEntryException 
	 */
	public WorkerThreadDescriptor addWorkerToQueue(WorkerThread thread, String uniqueName, int userId) throws DuplicateEntryException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the thread is not null
		if( thread == null){
			throw new IllegalArgumentException("A null thread cannot be added to the queue");
		}

		
		// 1 -- Add the thread
		synchronized(workerThreadQueue){
			for(int c = 0; c < workerThreadQueue.size(); c++) {
				if(workerThreadQueue.get(c).hash.equalsIgnoreCase(uniqueName) ){
					if( workerThreadQueue.get(c).thread.getStatus() == WorkerThread.State.STOPPED){
						workerThreadQueue.remove(c);
					}
					else{
						throw new DuplicateEntryException("A running task already exists with the given name (" + uniqueName + ")");
					}
				}
			}
			
			WorkerThreadDescriptor desc = new WorkerThreadDescriptor( thread, uniqueName, userId );
			
			workerThreadQueue.add( desc );
			return desc;
		}
	}
	
	/**
	 * Returns a reference to the application state monitor.
	 * @return
	 */
	public ApplicationStateMonitor getApplicationStateMonitor(){
		return metricsMonitor;
	}
	
	/**
	 * Return the queue of worker threads.
	 * @return
	 */
	public WorkerThreadDescriptor[] getWorkerThreadQueue(){
		return getWorkerThreadQueue(false);
	}
	
	/**
	 * Find the worker thread who's unique name matches the one given
	 * @param uniqueName
	 * @return
	 */
	public WorkerThreadDescriptor getWorkerThread(String uniqueName){
		
		// 0 -- Precondition check
		if( uniqueName == null ){
			throw new IllegalArgumentException("The unique name of the thread cannot be null");
		}
		
		
		// 1 -- Find the entry and return it
		synchronized(workerThreadQueue){
			for(int c = 0; c < workerThreadQueue.size(); c++){
				if( workerThreadQueue.get(c).hash.equalsIgnoreCase(uniqueName) ){
					return workerThreadQueue.get(c);
				}
			}
		}
		return null;
	}
	
	/**
	 * Sends a stop command to the worker thread with the given name.
	 * @param uniqueName
	 * @return
	 */
	public boolean stopWorkerThread(String uniqueName){
		WorkerThreadDescriptor worker = getWorkerThread(uniqueName);
		
		try{
			if( worker != null ){
				worker.thread.terminate();
				return true;
			}
			else{
				return false;
			}
		}
		catch(NullPointerException e){
			return false;
		}
	}
	
	/**
	 * Get the system performance metrics.
	 * @return
	 */
	public ApplicationStateDataPoint[] getMetricsData(){
		return metricsMonitor.getData();
	}
	
	/**
	 * Return the queue of worker threads. Note that the 'returnAliveThreadsOnly' is true, the method
	 * will attempt to filter out threads that are no longer active. However, note that the worker threads
	 * may complete (and become inactive) after the result is returned.
	 * @return
	 */
	public WorkerThreadDescriptor[] getWorkerThreadQueue(boolean returnAliveThreadsOnly){
		
		// 1 -- Get the worker thread queue
		Vector<WorkerThreadDescriptor> workers = new Vector<WorkerThreadDescriptor>();
		
		synchronized(workerThreadQueue){
			for(int c = 0; c < workerThreadQueue.size(); c++){
				if( returnAliveThreadsOnly == false || workerThreadQueue.get(c).thread.getStatus() == WorkerThread.State.STARTED ){
					workers.add(workerThreadQueue.get(c));
				}
			}
		}
		
		WorkerThreadDescriptor[] workersArray = new WorkerThreadDescriptor[workers.size()];
		workers.toArray(workersArray);
		
		return workersArray;
	}
	
	/**
	 * This method load the given config file. Returns null if the property file was not found at the location specified.
	 * @param configFileName
	 * @return
	 * @throws IOException 
	 */
	private Properties loadConfigFile(String configFileName) throws IOException{
		Properties p = new Properties();
		FileInputStream inputStream = null;
		
		try {
			inputStream = new FileInputStream(configFileName);
			p.load(inputStream);
			
		} catch (FileNotFoundException e) {
			return null;
		}
		finally{
			if( inputStream != null )
				inputStream.close();
		}
		
		return p;
	}
	
	/**
	 * Get the firewall object.
	 * @return
	 */
	public Firewall getFirewall(){
		return firewall;
	}
	
	/**
	 * Retrieves the data source that serves as the connection broker for the database.
	 * @return
	 */
	public BasicDataSource getDataSource(){
		return connectionBroker;
	}
	
	/**
	 * Get the relevant application parameter.
	 * @param name
	 * @param defaultValue
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public String getParameter( String name, String defaultValue ) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appConfig.getApplicationParameters().getParameter( name, defaultValue );
	}
	
	/**
	 * Get the time that the application was started.
	 * @return
	 */
	public long getApplicationStartTime(){
		return startTime;
	}
	
	/**
	 * Get the relevant application parameter.
	 * @param name
	 * @param defaultValue
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public long getParameter( String name, long defaultValue ) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appConfig.getApplicationParameters().getParameter( name, defaultValue );
	}
	
	/**
	 * Get the scanner controller object.
	 * @return
	 */
	public ScannerController getScannerController(){
		return scannerController;
	}
	
	/**
	 * Get the manager object associated with the application.
	 * @return
	 */
	public NetworkManager getNetworkManager(){
		return manager;
	}
	
	/**
	 * Gets the global application resource object
	 * @return
	 */
	public static Application getApplication(){
		return appRes;
	}
	
	/**
	 * Prepare the command-line processor with the valid parameters.
	 * @return
	 * @throws JSAPException
	 */
	private static JSAP getCommandLineProcessor() throws JSAPException{
		JSAP jsap = new JSAP();
		
		// 1 -- Configuration file option (-c)
		FlaggedOption opt1 = new FlaggedOption("configFile")
        		.setStringParser(JSAP.STRING_PARSER)
        		.setDefault("./config.ini")
        		.setRequired(false)
        		.setShortFlag('c')
        		.setLongFlag("config");
		opt1.setHelp("The location of the configuration file");
		
		jsap.registerParameter(opt1);
		
		// 2 -- Verbose messages option (-v)
		Switch sw1 = new Switch("verbose")
        		.setShortFlag('v')
        		.setLongFlag("verbose");
		sw1.setHelp("Output log event messages to standard err");
		
		jsap.registerParameter(sw1);
		
		// 3 -- Language option (-l)
		/*FlaggedOption opt2 = new FlaggedOption("language")
        		.setStringParser(JSAP.STRING_PARSER)
        		.setDefault("eng")
        		.setRequired(false)
        		.setShortFlag('l')
        		.setLongFlag("language");
		opt2.setHelp("The application language");
			
		jsap.registerParameter(opt2);*/
		
		// 2 -- GUI mode option (-gui)
		Switch sw2 = new Switch("gui").setShortFlag('g').setLongFlag("gui");
		sw2.setHelp("Starts the application with the GUI interface");
		
		jsap.registerParameter(sw2);
		
		// 3 -- Service mode option (-service)
		Switch sw3 = new Switch("service").setShortFlag('s').setLongFlag("service");
		sw3.setHelp("Starts the application in service mode");
		
		jsap.registerParameter(sw3);
		
		// Return the processor
		return jsap;
	}
	

	/**
	 * Starts the metrics monitor thread.
	 *
	 */
	public void startMetricsMonitor(){
		synchronized (metricsMonitorMutex) {
			metricsMonitor = new ApplicationStateMonitor( 10, 720, this);
			metricsMonitor.start();
		}
	}
	
	/**
	 * Start the various scheduled background tasks.
	 */
	private void startTasks(){
		
		timer = new Timer("Scheduled Task Timer", true);
		
		// 1 -- Start the definition updater
		DefinitionUpdateWorker worker = new DefinitionUpdateWorker();
		timer.scheduleAtFixedRate( new TimerTaskWorker(worker, "Definition Updater"), 1000*60*30, 1000*60*60*8); //Repeat every 30 minutes and delay for 8 hours
		
		// 2 -- Start the index defragmenter (if the internal database was used)
		if( usingInternalDatabase ){
			reindexer = new ReindexerWorker();
			Calendar cal = Calendar.getInstance();
			
			int hour = cal.get(Calendar.HOUR_OF_DAY);
			int minute = cal.get(Calendar.MINUTE);
			
			int secsTilNextRun = ((23 - hour)*3600) + (60 - minute)*60;
			timer.scheduleAtFixedRate( new TimerTaskWorker(reindexer, "Index Defragmenter"), 1000* secsTilNextRun, 1000*60*60*24); //Start at midnight and repeat every 24 hours
		}
	}
	
	/**
	 * Stops the metrics monitor thread. This method will loop until the thread successfully completes.
	 *
	 */
	public void stopMetricsMonitor(){
		
		synchronized (metricsMonitorMutex) {
			if( metricsMonitor != null){
				metricsMonitor.shutdown();
			}
			
			while( metricsMonitor.isAlive() ){
				try{
					metricsMonitorMutex.wait(100);
				}
				catch(InterruptedException e){
					//Ignore, we are shutting down the class so an interrupt is not important
				}
			}
		}
	}
	
	/**
	 * Start the application by parsing the command-line arguments and performing the required startup actions.
	 * @param args
	 * @return
	 * @throws JSAPException 
	 * @throws InputValidationException 
	 * @throws SQLException 
	 * @throws NoDatabaseConnectionException 
	 */
	public static Application startApplication(String[] args, RunMode runMode ) throws JSAPException, NoDatabaseConnectionException, SQLException, InputValidationException, BindException, Exception{ // NOPMD by luke on 5/26/07 10:46 AM
		
		appRes = new Application(args);
		
		appRes.startListener();
		appRes.scannerController.start();
		
		// Configure external logging
		try {
			String address = appRes.getApplicationConfiguration().getLogServerAddress();
			int port = appRes.getApplicationConfiguration().getLogServerPort();
			boolean enabled = appRes.getApplicationConfiguration().getLogServerEnabled();
			SyslogNGAppender.Protocol protocol;
			
			if( appRes.getApplicationConfiguration().getLogServerProtocol().equalsIgnoreCase("TCP") ){
				protocol = SyslogNGAppender.Protocol.TCP;
			}
			else{
				protocol = SyslogNGAppender.Protocol.UDP;
			}
			
			MessageFormatter formatter = MessageFormatterFactory.getFormatter( appRes.getApplicationConfiguration().getLogFormat() );
			appRes.eventlog.setMessageFormatter( formatter );
			
			if( address != null && port >= 0 && port <= 65535 ){
				appRes.eventlog.setLogServer(address, port, protocol, enabled);
			}
			
		} catch (SQLException e) {
			if( appRes.eventlog != null ){
				appRes.eventlog.logExceptionEvent( new EventLogMessage(EventLogMessage.EventType.STARTUP_ERROR, new EventLogField(FieldName.MESSAGE, "Error noted when configuring external logging")), e); 
			}
			else{
				e.printStackTrace();
			}
		} catch (InputValidationException e) {
			if( appRes.eventlog != null ){
				appRes.eventlog.logExceptionEvent( new EventLogMessage(EventLogMessage.EventType.STARTUP_ERROR, new EventLogField(FieldName.MESSAGE, "Error noted when configuring external logging")), e); 
			}
			else{
				e.printStackTrace();
			}
		} catch (Exception e) {
			if( appRes.eventlog != null ){
				appRes.eventlog.logExceptionEvent( new EventLogMessage(EventLogMessage.EventType.STARTUP_ERROR, new EventLogField(FieldName.MESSAGE, "Error noted when configuring external logging")), e); 
			}
			else{
				e.printStackTrace();
			}
		}
		
		appRes.eventlog.setLoggingLevel(EventLogSeverity.DEBUG);
		appRes.logEvent( new EventLogMessage(EventLogMessage.EventType.APPLICATION_STARTED, new EventLogField(FieldName.VERSION, Application.getVersion())));
		
		// Launch console listener for accepting command line commands
		if( runMode == RunMode.CLI ){
			ConsoleListener.startConsoleListener();
		}
		
		return appRes;
	}

	/**
	 * Start the XML-RPC listener.
	 * @throws Exception 
	 * @throws BindException 
	 *
	 */
	public void startListener() throws BindException, Exception{ // NOPMD by luke on 5/26/07 11:12 AM
		
		// 1 -- Configure the manager for network activity
		
		//	 1.1 -- Determine if SSL is enabled
		boolean sslEnabled = false;
		
		try {
			sslEnabled = appRes.getApplicationConfiguration().isSslEnabled();
		} catch (NoDatabaseConnectionException e) {
			System.err.println("Database connection unavailable");
			System.exit(-1);
		} catch (SQLException e) {
			logExceptionEvent( new EventLogMessage( EventLogMessage.EventType.SQL_EXCEPTION ), e);
			System.err.println("SQL exception prevented retrieval of application parameter (Administration.EnableSSL)");
			System.exit(-1);
		} catch (InputValidationException e) {
			logEvent( new EventLogMessage( EventLogMessage.EventType.INTERNAL_ERROR, new EventLogField(FieldName.MESSAGE, "message = input validation failure getting parameter 'Administration.EnableSSL', defaulting to disabled") ));
		}
		
		// 1.2 -- Get the server port
		int serverPort = 8443;
		try {
			serverPort = appConfig.getServerPort();
		} catch (InputValidationException e) {
			logEvent( new EventLogMessage( EventLogMessage.EventType.INTERNAL_ERROR, new EventLogField(FieldName.MESSAGE, "message = server port input validation failure, defaulting to port 8080") ));
		} catch (NoDatabaseConnectionException e) {
			System.err.println("Database connection unavailable");
			System.exit(-1);
		} catch (SQLException e) {
			System.err.println("SQL exception prevented retrieval of application parameter (Administration.ServerPort)");
			e.printStackTrace();
			System.exit(-1);
		}
		
		manager.setServerPort( serverPort, sslEnabled );
		
		manager.startListener();
	}
	
	/**
	 * Causes the application to shut itself down and terminate.
	 *
	 */
	public void shutdown(){
		shutdown( ShutdownRequestSource.UNSPECIFIED );
	}
	
	/**
	 * Get the platform architecture (x86, sparc, etc.).
	 * @precondition None
	 * @postcondition The platform architecture will be returned.
	 * @return The platform architecture
	 */
	public String getPlatformArch(){
		return System.getProperty("os.arch");
	}
	
	/**
	 * Get the OS name (Solaris, Linux, etc.).
	 * @precondition None
	 * @postcondition The OS name will be returned.
	 * @return The nOS name
	 */
	public String getOperatingSystemName(){
		return System.getProperty("os.name");
	}
	
	/**
	 * Get the OS version (5.6, 2.X, etc).
	 * @precondition None
	 * @postcondition The OS version will be returned.
	 * @return The OS version
	 */
	public String getOperatingSystemVersion(){
		return System.getProperty("os.version");
	}
	
	/**
	 * Get the vendor of the JVM.
	 * @precondition None
	 * @postcondition The vendor of the JVM will be returned.
	 * @return The vendor of the JVM
	 */
	public String getJvmVendor(){
		return System.getProperty("java.vm.vendor");
	}
	
	/**
	 * Get the version of the JVM.
	 * @precondition None
	 * @postcondition The version of the JVM will be returned.
	 * @return The version of the JVM
	 */
	public String getJvmVersion(){
		return System.getProperty("java.vm.version");
	}
	
	/**
	 * Retrieves the number of database connections that are presently allocated.
	 * @return
	 */
	public int getDatabaseConnectionCount(){
		return appRes.getDataSource().getNumActive();
	}
	
	/**
	 * Causes the application to shut itself down and terminate.
	 *
	 */
	public void shutdown( ShutdownRequestSource shutdownCommandSource ){ // NOPMD by luke on 5/26/07 10:47 AM
		
		synchronized( shutdownInProgress ){
			
			// 0 -- Precondition check
			if( shutdownInProgress.booleanValue() ){
				return;
			}
			
			shutdownInProgress = Boolean.TRUE;
			reindexer.terminate();
			timer.cancel();
			
			if( shutdownCommandSource == ShutdownRequestSource.API ){
				System.out.print("System is shutting down (received shutdown command from API interface)...");
			}else if( shutdownCommandSource == ShutdownRequestSource.UNSPECIFIED ){
				System.out.print("System is shutting down...");
			}
			
			// 1 -- Shutdown web services listener
			if( manager != null ){
				manager.stopListener();
			}
			
			// 2 -- Shutdown the scanner controller
			if(scannerController != null){
				scannerController.disableScanning();
				scannerController.shutdown();
				waitUntilThreadTerminates( scannerController, 10);
			}
			
			// 3 -- Stop background tasks
			Iterator<WorkerThreadDescriptor> iterator = workerThreadQueue.iterator();
			
			while( iterator.hasNext() ){
				WorkerThreadDescriptor desc = iterator.next();
				WorkerThread thread = desc.getWorkerThread();
				
				if( thread != null && (thread.getStatus() != State.STOPPED && thread.getStatus() != State.INITIALIZED)){
					thread.terminate();
					waitUntilThreadTerminates( thread, 10);
				}
			}
			
			// 4 -- Stop metrics monitor
			if( manager != null ){
				appRes.stopMetricsMonitor();
			}
			
			// 5 -- Disconnect database
			try{
				if( isUsingInternalDatabase() ){
					Connection connection = DriverManager.getConnection("jdbc:derby:"+ DATABASE_LOCATION +";shutdown=true");
					connection.close();
				}
			}
			catch(SQLException e){
	
				if( !e.toString().endsWith("shutdown.") ){
					System.err.println("Exception occurred while shutting down database");
					e.printStackTrace(System.err);
				}
			}
			
			System.out.println("Done");
			
			// 6 -- Shutdown console listeners
			/*
			 * Java does not have a good method to stop listening to input from the console. The standard implementations
			 * do not throw an IOException when interrupt is called. Therefore, this method will force the entire
			 * application to exit after the all interruptible threads are closed.
			 */
			if( ConsoleListener.getConsoleListener() != null ){
				System.exit(0); //Force the entire application to exit
				/*
				 * Note: when the JVM finally allows the console listening thread to be interrupted then the following function should be used and the System.exit(0) call removed:
				 * ConsoleListener.stopListener();
				 */
			}
			
			appRes = null;
		}
	}
	
	/**
	 * This method loops until the given thread completes. The return value indicates if the thread successfully terminated. 
	 * @param thread The thread that must complete
	 * @param timeLimitSeconds Indicates the number of seconds before the method quits waiting
	 * @return True if the thread successfully ended; false otherwise
	 */
	private boolean waitUntilThreadTerminates( Thread thread, int timeLimitSeconds ){
		
		// 1 -- Loop until the thread terminates
		int secondsAlive = 0;
		while( thread != null && thread.isAlive() && (timeLimitSeconds <= 0 || (secondsAlive < timeLimitSeconds))){
			try{
				Thread.sleep(1000);
				secondsAlive += 1;
			}
			catch(InterruptedException e){
				//Ignore, we don't care if the thread is awoken.
			}
		}
		
		// 2 -- Determine if the thread was successfully terminated
		return thread != null && thread.isAlive();
	}
	
	/**
	 * This method loops until the given worker thread completes. The return value indicates if the thread successfully terminated. 
	 * @param thread The thread that must complete
	 * @param timeLimitSeconds Indicates the number of seconds before the method quits waiting
	 * @return True if the thread successfully ended; false otherwise
	 */
	private boolean waitUntilThreadTerminates( WorkerThread thread, int timeLimitSeconds ){
		
		// 1 -- Loop until the thread terminates
		int secondsAlive = 0;
		while( thread != null && thread.getStatus() != State.STOPPED && (secondsAlive < timeLimitSeconds)){
			
			try{
				Thread.sleep(1000);
				secondsAlive += 1;
			}
			catch(InterruptedException e){
				//Ignore, we don't care if the thread is awoken.
			}
		}
		
		// 2 -- Determine if the thread was successfully terminated
		return thread != null && thread.getStatus() != State.STOPPED;
	}
	
	/**
	 * This function establishes a connection to the internal database. The database connection will be made in such a
	 * way to establish accounts for each of the different functions of the application. This is done to enforce separation of duties 
	 * and principle of least privelege.
	 * @precondition The internal database must exist 
	 * @postcondition The internal database will be connected 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws SQLException 
	 * @throws NoDatabaseConnectionException 
	 */
	public DatabaseInitializationState connectToInternalDatabase( boolean createIfNonExistant ) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoDatabaseConnectionException{
		// 0 -- Precondition check
		//	No preconditions will be explicitly checked.
		//	Instead, the necessary conditions will be checked during the connection operations
		
		// 1 -- Setup the database
		databaseDriver = "org.apache.derby.jdbc.EmbeddedDriver";
		
		if( createIfNonExistant )
			databaseLocation = "jdbc:derby:" + DATABASE_LOCATION + ";create=true";
		else
			databaseLocation = "jdbc:derby:" + DATABASE_LOCATION;
		
		connectionBroker = new BasicDataSource();
		connectionBroker.setMaxActive(50);
		connectionBroker.setDriverClassName(databaseDriver);
		connectionBroker.setUrl(databaseLocation);
		
		DerbyDatabaseInitializer initializer = new DerbyDatabaseInitializer (connectionBroker.getConnection());
		DatabaseInitializationState initializationState = initializer.performSetup();
		
		usingInternalDatabase = true;
		
		return initializationState;
	}
	
	/**
	 * This function establishes a connection to the database per the given arguments. The database connection will be made in such a
	 * way to establish accounts for each of the different functions of the application. This is done to enforce separation of duties 
	 * and principle of least privelege.
	 * @precondition The databaseLocation must not be empty or null, the password must not be null, the database driver must be available (if specified)
	 * @postcondition The database driver will be loaded (if available) and the database connection will be established for all contexts/subsystems 
	 * @param databaseLocation The location of the database server
	 * @param databasePassword The password to use during authentication
	 * @param databaseDriver The database driver to use
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public boolean connectToDatabase(String databaseLocation, String databasePassword, String databaseDriver) throws InstantiationException, IllegalAccessException, ClassNotFoundException{ // NOPMD by luke on 5/26/07 10:48 AM
		// 0 -- Precondition check
		
		//	 0.1 -- Ensure that the database location is valid
		if( databaseLocation == null || databaseLocation.length() == 0 )
			throw new IllegalArgumentException("The database location cannot be null or empty");
		
		//	 0.2 -- If the database password is null, then substitute it with an empty password.
		// 			Note: this will not affect the value of the argument since Java does not pass 
		//				  objects by reference (contrary to popular opinion) but rather passes
		//				  references to objects by value.
		if( databasePassword == null )
			databasePassword = "";
		
		// 1 -- Setup the database	
		if( databaseDriver != null && databaseDriver.length() > 0){
			connectionBroker = new BasicDataSource();
			connectionBroker.setMaxActive(50);
			connectionBroker.setDriverClassName(databaseDriver);
			//connectionBroker.setUsername("SiteSentry_Devel");
			connectionBroker.setPassword(databasePassword);
			connectionBroker.setUrl(databaseLocation);
		}
	
		this.databaseDriver = databaseDriver;
		this.databaseLocation = databaseLocation;
		usingInternalDatabase = false;
		
		return true;
	}
	
	
	public void logEvent( EventLogMessage message ){
		eventlog.logEvent(message);
	}
	
	/**
	 * Return the database location that is currently being used.
	 * @precondition None (although null will be returned if the database connection has not been set)
	 * @postcondition A string correponding with the database location will be returned.
	 * @return
	 */
	public String getDatabaseInfo(){
		return databaseLocation;
	}
	
	/**
	 * Return true if the built-in database is being used.
	 * @precondition None
	 * @postcondition True if the internal Derbey database is being used
	 * @return
	 */
	public boolean isUsingInternalDatabase(){
		return usingInternalDatabase;
	}
	
	/**
	 * Return the database driver that is currently being used.
	 * @precondition None (although null will be returned if the database connection has not been set)
	 * @postcondition A string correponding with the database driver will be returned.
	 * @return
	 */
	public String getDatabaseDriver(){
		return databaseDriver;
	}
	
	/**
	 * Set the event log class to be used for application logging.
	 * @precondition The event must be validly prepared
	 * @postcondition A reference to the event log will be set
	 * @param eventlog
	 */
	public void setEventLog(EventLog eventlog){
		this.eventlog = eventlog;
	}
	
	/**
	 * Get a reference to the event log. 
	 * @precondition None (although the event log class must exist or null will be returned)
	 * @postcondition A reference to the event log will be returned, or null if none exists
	 * @return A reference to the event log, or null if none exists
	 */
	public EventLog getEventLog(){
		return eventlog;
	}
	
	/**
	 * Return a value indicating if the application is running as a command-line application, under a GUI or as a service.
	 * @return
	 */
	public RunMode getRunMode(){
		return runMode;
	}
	
	/**
	 * Get the number of threads currently used by the application.
	 * @precondition None
	 * @postcondition The number of threads used will be returned.
	 * @return The number of threads used
	 */
	public int getThreadCount(){
		
		Thread[] tList = new Thread[Thread.activeCount()];
		
		int numThreads = Thread.enumerate(tList);
		int realCount = 0;
		
		for (int i = 0; i < numThreads; i++) {
			if ( tList[i] != null && tList[i].isAlive() ){
				realCount++;
			}
		}
		return realCount;
	}
	
	/**
	 * Get the number of processors available.
	 * @precondition None
	 * @postcondition The number of processors available will be returned.
	 * @return The number of processors available
	 */
	public int getProcessorCount(){
		return Runtime.getRuntime().availableProcessors();
	}
	
	/**
	 * Get the maximum amount of memory available.
	 * @precondition None
	 * @postcondition The maximum amount of memory available will be returned.
	 * @return The maximum amount of memory available (in bytes)
	 */
	public long getMaxMemory(){
		long mem = Runtime.getRuntime().maxMemory();
		
		return mem;
	}
	
	/**
	 * Get the amount of memory currently used.
	 * @precondition None
	 * @postcondition The amount of memory currently used will be returned.
	 * @return The amount of memory currently used (in bytes)
	 */
	public long getUsedMemory(){
		long mem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		
		return mem;
	}
	
	/**
	 * Get the uptime (in seconds).
	 * @precondition None
	 * @postcondition The uptime (in seconds will be returned.
	 * @return The uptime (in seconds
	 */
	public long getUptime(){
		long startTime = appRes.getApplicationStartTime();
		long currentTime = System.currentTimeMillis();
		
		return currentTime - startTime;
	}
	
	/**
	 * Get the time that the application was started.
	 * @return
	 */
	public long getStartTime(){
		return appRes.getApplicationStartTime();
	}

	/**
	 * Get the SQL warnings from the database.
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	public String[] getSqlWarnings() throws SQLException, NoDatabaseConnectionException{
		
		Connection connection = null; 
		
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.SESSION);
			SQLWarning warn = connection.getWarnings();
			Vector<String> warnings = new Vector<String>();
			
			while( warn != null ){
				warnings.add( warn.getMessage() );
				warn = warn.getNextWarning();
			}
			
			String[] warningsArray = new String[warnings.size()];
			for( int c = 0; c < warnings.size(); c++ ){
				warningsArray[c] = (String)warnings.get(c);
			}
			
			return warningsArray;
		}
		finally{
			if (connection != null )
				connection.close();
		}
	}
	
	/**
	 * Get the name of the database that is being used. 
	 * @precondition The database connection must have been initialized
	 * @postcondition String describing the database will be returned
	 * @return String describing the database
	 * @throws SQLException 
	 * @throws NoDatabaseConnectionException 
	 */
	public String getDatabaseName() throws SQLException, NoDatabaseConnectionException{
		if( connectionBroker != null ){
			
			Connection connection = null;
			
			try{
				connection = connectionBroker.getConnection();
				
				if( connection == null )
					throw new NoDatabaseConnectionException();
				DatabaseMetaData metaData = connection.getMetaData();
			
				return metaData.getDatabaseProductName();
			}
			finally{
				if( connection != null )
					connection.close();
			}
		}
		
		//We should not have gotten here unless an exception occurred that prevented us from getting the database information
		throw new NoDatabaseConnectionException();
	}
	
	/**
	 * Get the version of the database driver that is being used. 
	 * @precondition The database connection must have been initialized
	 * @postcondition String describing the database driver version will be returned
	 * @return String describing the database driver version
	 * @throws SQLException 
	 * @throws NoDatabaseConnectionException 
	 */
	public String getDatabaseVersion() throws SQLException, NoDatabaseConnectionException{
		if( connectionBroker != null ){
			
			Connection connection = null;
			
			try{
				connection = connectionBroker.getConnection();
				
				if( connection == null )
					throw new NoDatabaseConnectionException();
				DatabaseMetaData metaData = connection.getMetaData();
			
				return metaData.getDriverVersion();
			}
			finally{
				if( connection != null )
					connection.close();
			}
		}
		
		//We should not have gotten here unless an exception occurred that prevented us from getting the database information
		throw new NoDatabaseConnectionException();
	}
	
	/**
	 * Get the meta-date associated with the database that is being used. 
	 * @precondition The database connection must have been initialized
	 * @postcondition Meta-data describing the database will be returned
	 * @return Meta-data describing the database
	 * @throws SQLException 
	 * @throws NoDatabaseConnectionException 
	 */
	public DatabaseMetaData getDatabaseMetaData() throws SQLException, NoDatabaseConnectionException{
		if( connectionBroker != null ){
			
			Connection connection = null;
			
			try{
				connection = connectionBroker.getConnection();
				
				if( connection == null )
					throw new NoDatabaseConnectionException();
				DatabaseMetaData metaData = connection.getMetaData();
			
				return metaData;
			}
			finally{
				if( connection != null )
					connection.close();
			}
		}
		
		//We should not have gotten here unless an exception occurred that prevented us from getting the database information
		throw new NoDatabaseConnectionException();
	}
	
	/**
	 * Get the name of the database driver that is being used. 
	 * @precondition The database connection must have been initialized
	 * @postcondition String describing the database driver name will be returned
	 * @return String describing the database driver name
	 * @throws SQLException 
	 * @throws NoDatabaseConnectionException 
	 */
	public String getDatabaseDriverName() throws SQLException, NoDatabaseConnectionException{
		if( connectionBroker != null ){
			
			Connection connection = null;
			
			try{
				connection = connectionBroker.getConnection();
				
				if( connection == null )
					throw new NoDatabaseConnectionException();
				DatabaseMetaData metaData = connection.getMetaData();
			
				return metaData.getDriverName();
			}
			finally{
				if( connection != null )
					connection.close();
			}
		}
		
		//We should not have gotten here unless an exception occurred that prevented us from getting the database information
		throw new NoDatabaseConnectionException();
	}
	
	/**
	 * Get the version of the database that is being used. 
	 * @precondition The database connection must have been initialized
	 * @postcondition String describing the database version will be returned
	 * @return String describing the database version
	 * @throws SQLException 
	 * @throws NoDatabaseConnectionException 
	 */
	public String getDatabaseDriverVersion() throws SQLException, NoDatabaseConnectionException{
		if( connectionBroker != null ){
			
			Connection connection = null;
			
			try{
				connection = connectionBroker.getConnection();
				
				if( connection == null )
					throw new NoDatabaseConnectionException();
				DatabaseMetaData metaData = connection.getMetaData();
			
				return metaData.getDatabaseProductVersion();
			}
			finally{
				if( connection != null )
					connection.close();
			}
		}
		
		//We should not have gotten here unless an exception occurred that prevented us from getting the database information
		throw new NoDatabaseConnectionException();
	}
	
	/**
	 * Get a string containing the application version.
	 * @precondition None
	 * @postcondition A string containing the application version is returned 
	 * @return A string containing the application version
	 */
	public static String getVersion(){
		if( VERSION_STATUS != null ){
			return VERSION_MAJOR + "." + VERSION_MINOR + "." + VERSION_REVISION + " (" + VERSION_STATUS + ")";
		}
		else{
			return VERSION_MAJOR + "." + VERSION_MINOR + "." + VERSION_REVISION;
		}
	}
	
	/**
	 * Create a new database connection with the given context
	 * @param databaseContext
	 * @return
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public Connection getDatabaseConnection( DatabaseAccessType databaseContext ) throws NoDatabaseConnectionException{

			Connection connection = null; // NOPMD by luke on 5/26/07 10:36 AM
			try {
				connection = connectionBroker.getConnection();
			} catch (SQLException e) {
				logExceptionEvent( new EventLogMessage(EventLogMessage.EventType.SQL_EXCEPTION, new Date()), e);
				throw new NoDatabaseConnectionException(e);
			}

			return connection;
		
	}
	
	/**
	 * Log the given exception using the event log that is setup by the given application class instance.
	 * @param eventType
	 * @param field
	 */
	public void logEvent( EventLogMessage.EventType eventType, EventLogField ... fields ){
		eventlog.logEvent(new EventLogMessage(eventType, fields));
	}
	
	/**
	 * Log the given exception using the event log that is setup by the given application class instance.
	 * @param eventType
	 */
	public void logEvent( EventLogMessage.EventType eventType ){
		eventlog.logEvent(new EventLogMessage(eventType));
	}
	
	/**
	 * Log the given exception using the event log that is setup by the given application class instance.
	 * @param eventType
	 * @param t
	 */
	public void logExceptionEvent( EventLogMessage.EventType eventType, Throwable t ){
		eventlog.logExceptionEvent(new EventLogMessage(eventType), t);
	}
	
	/**
	 * Log the given exception using the event log that is setup by the given application class instance.
	 * @param message
	 * @param t
	 */
	public void logExceptionEvent( EventLogMessage message, Throwable t ){
		eventlog.logExceptionEvent(message, t);
	}
	
	/**
	 * Retrieve the application configuration manager.
	 * @return
	 */
	public ApplicationConfiguration getApplicationConfiguration(){
		return appConfig;
	}
	
	/**
	 * Retrieve the application parameter object associated with this resource.
	 * @return
	 */
	public ApplicationParameters getApplicationParameters(){
		return appConfig.getApplicationParameters();
	}
	
	/**
	 * Retrieve the session manager.
	 * @return
	 */
	public SessionManagement getSessionManager(){
		if( sessionManagement == null )
			sessionManagement = new SessionManagement( this );

		return sessionManagement;
	}
	

	/**
	 * Get the status of the manager's operational status.
	 * @return
	 */
	public ApplicationStatusDescriptor getManagerStatus(){
		
		//StatusEntry[] statusEntries = new StatusEntry[5];
		Vector<StatusEntry> statusEntries = new Vector<StatusEntry>(5);
		
		// 1 -- Get the memory usage
		long memoryUsage = (100*getUsedMemory()) / getMaxMemory();
		if( memoryUsage >= MEMORY_CRITICAL_THRESHOLD ){
			statusEntries.add( new StatusEntry("Memory Utilization", ApplicationStatusDescriptor.STATUS_RED, "Memory Critical", "Memory use at critical limits") );
		}
		else if( memoryUsage >= MEMORY_WARNING_THRESHOLD ){
			statusEntries.add( new StatusEntry("Memory Utilization", ApplicationStatusDescriptor.STATUS_YELLOW, "Memory Low", "Memory use high") );
		}
		else{
			statusEntries.add( new StatusEntry("Memory Utilization", ApplicationStatusDescriptor.STATUS_GREEN) );
		}
		
		// 2 -- Get the scanner status
		ScannerController scanner = appRes.getScannerController();
		if( scanner == null ){
			statusEntries.add( new StatusEntry("Scanner Status", ApplicationStatusDescriptor.STATUS_RED, "Scanner Failed", "Scanner has become non-operational") );
		}
		else {
			ScannerController.ScannerState scannerState = scanner.getScanningState();
			
			if( scannerState == ScannerController.ScannerState.PAUSING ){
				statusEntries.add( new StatusEntry("Scanner Status", ApplicationStatusDescriptor.STATUS_YELLOW, "Scanner Pausing", "Scanner is preparing to pause") );
			}
			else if( scannerState == ScannerController.ScannerState.STARTING ){
				statusEntries.add( new StatusEntry("Scanner Status", ApplicationStatusDescriptor.STATUS_YELLOW, "Scanner Starting", "Scanner is preparing to begin scans...") );
			}
			else if( scannerState == ScannerController.ScannerState.PAUSED ){
				statusEntries.add( new StatusEntry("Scanner Status", ApplicationStatusDescriptor.STATUS_YELLOW, "Scanner Paused", "Scanner is not currently operational (paused)") );
			}
			else{
				statusEntries.add( new StatusEntry("Scanner Status", ApplicationStatusDescriptor.STATUS_GREEN) );
			}
		}
		
		// 3 -- Get the thread count
		int threads = getThreadCount();
		if( threads >= THREAD_CRITICAL_THRESHOLD ){
			statusEntries.add( new StatusEntry("Thread Count", ApplicationStatusDescriptor.STATUS_RED, "Thread Count High", "Thread count high") );
		}
		else if( threads >= THREAD_WARNING_THRESHOLD ){
			statusEntries.add( new StatusEntry("Thread Count", ApplicationStatusDescriptor.STATUS_YELLOW, "Thread Count Critical", "Thread count critical") );
		}
		else{
			statusEntries.add( new StatusEntry("Thread Count", ApplicationStatusDescriptor.STATUS_GREEN) );
		}
		
		// 4 -- Get the database connection count
		int dbConnectionCount = getDatabaseConnectionCount();
		if( dbConnectionCount >= DBCONNECTION_CRITICAL_THRESHOLD ){
			statusEntries.add( new StatusEntry("Database Connections", ApplicationStatusDescriptor.STATUS_RED, "Database Connections Critical", "Database connections critical") );
		}
		else if( dbConnectionCount >= DBCONNECTION_WARNING_THRESHOLD ){
			statusEntries.add( new StatusEntry("Database Connections", ApplicationStatusDescriptor.STATUS_YELLOW, "Database Connections High", "Database connections high") );
		}
		else{
			statusEntries.add( new StatusEntry("Database Connections", ApplicationStatusDescriptor.STATUS_GREEN) );
		}
		
		// 5 -- Compile the results
		StatusEntry[] statusEntriesArray;

		statusEntriesArray = new StatusEntry[statusEntries.size()];

		for(int c = 0; c < statusEntries.size(); c++){
			statusEntriesArray[c] = (StatusEntry)statusEntries.get(c);
		}
		
		return new ApplicationStatusDescriptor( statusEntriesArray );
		
	}
	
	/**
	 * Returns a build number for the application.
	 * @return
	 */
	public static String getBuildNumber(){
		
		if( buildNumber != null )
			return buildNumber;
		
		FileReader input = null;
		BufferedReader bufRead = null;
		
		try{
			input = new FileReader("buildNumber");
			bufRead = new BufferedReader(input);
			
			buildNumber = bufRead.readLine();
			
			bufRead.close();
			bufRead = null;
		}
		catch(IOException e){
			buildNumber = "";
			
			try{
				if( input != null ){
					input.close();
				}
				
				if( bufRead != null ){
					bufRead.close();
				}
			}
			catch(IOException e2){
				//Ignore this exception, reading the buildnumber is not that important (won't cause a crash)
			}
		}
		
		return buildNumber;
	}
	
	/**
	 * Indicates if the application is shutting down.
	 * @return
	 */
	protected boolean isShuttingDown(){
		synchronized(shutdownInProgress){
			return shutdownInProgress.booleanValue();
		}
	}
	
	/**
	 * This class ensures that the application is shutdown correctly even if the application was terminated by user without doing so nicely.
	 * @author luke
	 *
	 */
	static class ShutdownHook extends Thread {
		
	    public void run() {
	    	Application application = Application.getApplication();
	    	
	    	if( application != null && application.isShuttingDown()){
				application.shutdown();
	    	}
	    }
	}
	

	/**
	 * This class represents an attribute (memory, thread count) that is assigned a status (warning, critical).
	 * @author luke
	 *
	 */
	public static class StatusEntry{
		private int status = ApplicationStatusDescriptor.STATUS_GREEN;
		private String key;
		private String longMessage = null;
		private String shortMessage = null;
		
		public StatusEntry(String key, int status, String shortMessage, String longMessage){
			this.key = key;
			this.status = status;
			this.shortMessage = shortMessage;
			this.longMessage = longMessage;
		}
		
		public StatusEntry(String key, int status){
			this.key = key;
			this.status = status;
		}
		
		public StatusEntry(String key, int status, String message){
			this.key = key;
			this.status = status;
			this.shortMessage = message;
			this.longMessage = message;
		}
		
		public int getStatus(){
			return status;
		}
		
		public String getShortMessage(){
			return shortMessage;
		}
		
		public String getLongMessage(){
			return longMessage;
		}
		
		public String getKey(){
			return key;
		}
	}
	
	/**
	 * The ManagerStatusDescriptor describes the operational state of the manager at a single point in time. 
	 * @author luke
	 *
	 */
	public static class ApplicationStatusDescriptor{
		public static final int STATUS_GREEN = 0;
		public static final int STATUS_YELLOW = 1;
		public static final int STATUS_RED = 2;
		
		private String statusDescriptionShort;
		private String statusDescriptionLong;
		private int statusLevel;
		
		private StatusEntry[] statusEntries;
		
		public ApplicationStatusDescriptor( StatusEntry[] statusEntries ){
			this.statusEntries = new StatusEntry[statusEntries.length];
			System.arraycopy(statusEntries, 0, this.statusEntries, 0, statusEntries.length);
			
			init();
		}
		
		private void init(){
			// Get the highest priority level
			for( int c = 0; c < statusEntries.length; c++){
				
				if( statusEntries[c].getStatus() > statusLevel )
					statusLevel = statusEntries[c].getStatus();
			}
			
			StringBuffer statusDescriptionShortBuffer = new StringBuffer(64);
			StringBuffer statusDescriptionLongBuffer = new StringBuffer(128);
			
			// Get the short and long messages
			for( int c = 0; c < statusEntries.length; c++){
				if( statusEntries[c].getStatus() == statusLevel ){
					
					if( statusEntries[c].getShortMessage() != null){
						if( statusDescriptionShortBuffer.length() == 0){
							statusDescriptionShortBuffer.append(statusEntries[c].getShortMessage());
						}
						else{
							statusDescriptionShortBuffer.append("; " + statusEntries[c].getShortMessage());
						}
					}
					
					if( statusEntries[c].getLongMessage() != null){
						if( statusDescriptionLongBuffer.length() == 0){
							statusDescriptionLongBuffer.append(statusEntries[c].getLongMessage());
						}
						else{
							statusDescriptionLongBuffer.append("; " + statusEntries[c].getLongMessage());
						}
					}
				}
			}
			
			if( statusDescriptionLongBuffer.length() > 0 )
				statusDescriptionLong = statusDescriptionLongBuffer.toString();
			else
				statusDescriptionLong = "Manager fully operational";
			
			if( statusDescriptionShortBuffer.length() > 0 )
				statusDescriptionShort = statusDescriptionShortBuffer.toString();
			else
				statusDescriptionShort = "Operational";
		}
		
		public StatusEntry[] getEntries(){
			StatusEntry[] statusEntriesCopy = new StatusEntry[statusEntries.length];
			System.arraycopy(statusEntries, 0, statusEntriesCopy, 0, statusEntries.length);
			
			return statusEntriesCopy;
		}
		
		public int getOverallStatus(){
			return statusLevel;
		}
		
		public String getLongDescription(){
			return statusDescriptionLong;
		}
		
		public String getShortDescription(){
				return statusDescriptionShort;
		}
		
		public StatusEntry getStatusEntry( String key ){
			for( int c = 0; c < statusEntries.length; c++){
				if( statusEntries[c].getKey().equals(key) )
					return statusEntries[c];
			}
			
			return null;
		}		
	}
	
}
