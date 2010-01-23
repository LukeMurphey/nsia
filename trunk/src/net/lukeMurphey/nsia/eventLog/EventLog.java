package net.lukemurphey.nsia.eventlog;

import java.util.Iterator;
import java.util.Vector;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.Application.DatabaseAccessType;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.eventlog.EventLogMessage.Category;

import org.apache.log4j.*;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.LoggingEvent;
//import org.apache.log4j.net.SyslogAppender;
import org.apache.log4j.varia.NullAppender;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * This class encapsulates a basic facility that stores log entries to the Log4j logging mechanism.
 * The logging system configuration is configured using the log4J properties file, see
 * http://logging.apache.org/log4j/docs/manual.html
 * 
 * @author luke
 *
 */

public class EventLog {
	
	protected EventLogSeverity loggingLevel = EventLogSeverity.INFORMATIONAL; //Prevents entries from being logged; logged iff severity >= loggingLevel
	
	private static final Logger logger = Logger.getLogger(net.lukemurphey.nsia.eventlog.EventLog.class.getName());
	private static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
	
	private Vector<EventLogHook> hooks = new Vector<EventLogHook>();
	
	private boolean repeatToConsole = false;
	private MessageFormatter formatter = null;
	private Application application = null;
	private SyslogNGAppender syslogAppender = null;
	private String logServeraddress = null;
	private int logServerport = -1;
	private boolean logServerEnabled = false;
	private SyslogNGAppender.Protocol logServerprotocol = null;
	
	public class LoggerErrorHandler implements ErrorHandler{

		public static final int LOG_CACHING = 10000;
		public static final int LOG_CACHE_FULL = 10001;
		public static final int LOG_CACHE_EMPTY = 10002;
		public static final int LOG_CACHE_EMPTYING = 10003;
		public static final int LOG_MESSAGE_SEND_FAILED = 10004;
		public static final int LOG_SERVER_CONNECTION_NOT_ESTABLISHED = 10005;
		public static final int LOG_SERVER_AVAILABLE = 10006;
		
		private LoggerErrorHandler(){
			//Only instantiable within the given class
		}
		
		@Override
		public void error(String message) {
			EventLog.this.logEvent( new EventLogMessage( Category.LOG_FAILED, new EventLogField(FieldName.MESSAGE, message)), true );
		}

		private Category getCategoryFromCode(int errorCode){
			if( errorCode == LOG_CACHING){
				return Category.LOGS_BEING_CACHED;
			}
			else if( errorCode == LOG_CACHE_EMPTY){
				return Category.LOG_CACHE_EMPTY;
			}
			else if( errorCode == LOG_CACHE_FULL){
				return Category.LOG_CACHE_FULL;
			}
			else if( errorCode == LOG_CACHE_EMPTYING){
				return Category.LOG_CACHE_EMPTYING;
			}
			else if( errorCode == LOG_SERVER_CONNECTION_NOT_ESTABLISHED){
				return Category.LOG_SERVER_UNAVAILABLE;
			}
			else if( errorCode == LOG_SERVER_AVAILABLE){
				return Category.LOG_SERVER_AVAILABLE;
			}
			else{
				return Category.LOG_FAILED;
			}
		}
		
		@Override
		public void error(String message, Exception exception, int code) {
			Category category = getCategoryFromCode(code);
			
			if( exception == null ){
				EventLog.this.logEvent( new EventLogMessage( category, new EventLogField(FieldName.MESSAGE, message)), false );
			}
			else{
				EventLog.this.logExceptionEvent( new EventLogMessage( category, new EventLogField(FieldName.MESSAGE, message)), exception, false );
			}
		}

		@Override
		public void error(String message, Exception exception, int code,
				LoggingEvent event) {
			error(message, exception, code);
		}

		@Override
		public void setAppender(Appender appender) {
			
		}

		@Override
		public void setBackupAppender(Appender appender) {
			
		}

		@Override
		public void setLogger(Logger logger) {
			
		}

		@Override
		public void activateOptions() {
			
		}
		
	}
	
	public EventLog(){
		
		// 1 -- Configure the Apache logger
		Logger apacheLogger = Logger.getLogger("org.apache");
		apacheLogger.setLevel(EventLogSeverity.OFF);//TODO Replace with application specific log types
		
		Logger httpClientLogger = Logger.getLogger("httpclient");
		httpClientLogger.setLevel(EventLogSeverity.OFF);		

		// 2 -- Configure the Jetty logger
		Logger jettyLogger = Logger.getLogger("org.mortbay");
		jettyLogger.setLevel(EventLogSeverity.OFF);//TODO Replace with application specific log types
		
		// 3 -- Configure the freemarker logger
		Logger freemarkerLogger = Logger.getLogger("freemarker.cache");
		freemarkerLogger.setLevel(EventLogSeverity.OFF);
		
		Logger freemarkerRuntimeLogger = Logger.getLogger("freemarker.runtime");
		freemarkerRuntimeLogger.setLevel(EventLogSeverity.OFF);
		
		Logger freemarkerBeansLogger = Logger.getLogger("freemarker.beans");
		freemarkerBeansLogger.setLevel(EventLogSeverity.OFF);
		
		// 4 -- Configure the default logger 
		NullAppender appender = new NullAppender();
		logger.addAppender(appender);
		logger.setLevel(EventLogSeverity.ALL);
	}
	
	/**
	 * @precondition None.
	 * @postcondition The event logger will print all logs messages to the console interface if the argument is true. 
	 * @param repeat A boolean indicating if the log messages should be copied to the console
	 * @return
	 */
	public void repeatMessagesToConsole(boolean repeat){
		repeatToConsole = repeat;
	}
	
	/**
	 * Gets a reference to the current message formatter.
	 * @return
	 */
	public MessageFormatter getMessageFormatter( ){
		return formatter;
	}
	
	/**
	 * Sets the message formatter that should be used to format messages before they are transmitted to an external server.
	 * @param formatter
	 */
	public void setMessageFormatter( MessageFormatter formatter ){
		this.formatter = formatter;
	}
	
	/**
	 * Stops the event log class from sending events to the given log server.
	 */
	public void clearLogServer( ){
		logServeraddress = null;
		logServerport = -1;
		syslogAppender.close();
	}

	/**
	 * Sets the log server that all messages should be sent to.
	 * @param address
	 * @param port
	 * @param protocol
	 * @param enabled
	 */
	public void setLogServer( String address, int port, SyslogNGAppender.Protocol protocol, boolean enabled ){
		
		// 0 -- Precondition Check
		
		// 0.1 -- Make sure the log server address is valid
		if( address == null ){
			throw new IllegalArgumentException("The syslog server address cannot be null");
		}
		
		// 0.2 -- Make sure the port is valid
		if( port < 0 || port > 65535 ){
			throw new IllegalArgumentException("The port must not be less than 0 and must be less than 65535");
		}

		
		// 1 -- Configure the class
		logServeraddress = address;
		logServerport = port;
		logServerprotocol = protocol;
		logServerEnabled = enabled;
		
		setupSyslog();
	}
	
	/**
	 * Sets the log server that all messages should be sent to.
	 * @param address
	 * @param port
	 */
	public void setLogServer( String address, int port ){
		
		// 0 -- Precondition Check
		
		// 0.1 -- Make sure the log server address is valid
		if( address == null ){
			throw new IllegalArgumentException("The syslog server address cannot be null");
		}
		
		// 0.2 -- Make sure the port is valid
		if( port < 0 || port > 65535 ){
			throw new IllegalArgumentException("The port must not be less than 0 and must be less than 65535");
		}

		
		// 1 -- Configure the class
		logServeraddress = address;
		logServerport = port;
		
		setupSyslog();
	}
	
	public void setLogServer( String address ){
		
		// 0 -- Precondition Check
		if( address == null ){
			throw new IllegalArgumentException("The syslog server address cannot be null");
		}

		
		// 1 -- Configure the class
		logServeraddress = address;
		
		setupSyslog();
	}
	
	public void setLogServerProtocol( SyslogNGAppender.Protocol protocol ){
		this.logServerprotocol = protocol;
	}
	
	public boolean isLogServerLogginEnabled(){
		return logServerEnabled;
	}
	
	public void logServerLoggingEnabled(boolean enable){
		if(logServerEnabled != enable){
			logServerEnabled = enable;
			setupSyslog();
		}
	}
	
	public void setLogServerPort( int port ){
		
		// 0 -- Precondition Check
		if( port < 0 || port > 65535 ){
			throw new IllegalArgumentException("The port must not be less than 0 and must be less than 65535");
		}

		
		// 1 -- Configure the class
		logServerport = port;
		
		setupSyslog();
	}
	
	private void setupSyslog(){
		
		if( logServerEnabled == false ){
			logger.removeAppender(syslogAppender);
			logger.addAppender(new NullAppender());
			return;
		}
		
		// 0.1 -- Make sure the address is valid
		if( logServeraddress == null){
			return;
		}
		
		// 0.2 -- Make sure the port is valid
		if( logServerport < 0 || logServerport > 65535 ){
			return;
		}
		
		// 0.3 -- Make sure the protocol was set
		if( logServerprotocol == null ){
			return;
		}
		
		PatternLayout patternLayout = new PatternLayout("%m");
		
		syslogAppender = new SyslogNGAppender(logServeraddress, logServerport, logServerprotocol);
		syslogAppender.setErrorHandler(new LoggerErrorHandler());
		syslogAppender.setLayout(patternLayout);
		
		//syslogAppender = new SyslogAppender(patternLayout, logServeraddress + ":" + logServerport , SyslogAppender.LOG_USER);
		
		/*if( false ){
			PatternLayout patternLayout = new PatternLayout("%m");
			syslogAppender = new SyslogAppender(patternLayout, logServeraddress + ":" + logServerport , SyslogAppender.LOG_USER);
			syslogAppender.setErrorHandler(new LoggerErrorHandler());
		}
		else{
			PatternLayout patternLayout = new PatternLayout("%m");
			
			syslogAppender = new SyslogSocketAppender(logServeraddress, logServerport, SyslogSocketAppender.Protocol.UDP);
			syslogAppender.setErrorHandler(new LoggerErrorHandler());
			syslogAppender.setLayout(patternLayout);
		}*/
		
		// Remove all of the previous appenders
		logger.removeAllAppenders();
		
		// Add the new appender
		logger.addAppender(syslogAppender);
	}
	
	/**
	 * Get the address of the log server that messages are being sent to. Note that method will return null if no log server is set.
	 * @return
	 */
	public String getLogServerAddress(){
		return logServeraddress;
	}
	
	/**
	 * Get the port that the log messages are being sent to. Note that method will return a value less than 0 if the log port was not specified.
	 * @return
	 */
	public int getLogServerPort(){
		return logServerport;
	}
	
	/**
	 * Returns a boolean indicating if the system is logging to an external server.
	 * @return
	 */
	public boolean isExternalLoggingEnabled(){
		return logServerEnabled;
	}
	
	/**
	 * Returns a boolean indicating if the log server is responding.
	 * @return
	 */
	public boolean isLogServerResponding(){
		if( syslogAppender != null ){
			return syslogAppender.isLogServerResponding();
		}
		else{
			return true;
		}
	}
	
	/**
	 * Returns the number of messages that are being cached because they could not be delivered to the log server.
	 * @return
	 */
	public int getLogCacheSize(){
		if( syslogAppender != null ){
			return syslogAppender.getCachedMessageCount();
		}
		else{
			return 0;
		}
	}
	
	/**
	 * Returns the maximum size that the log cache is allowed before messages are discarded.
	 * @return
	 */
	public int getMaxLogCacheSize(){
		if( syslogAppender != null ){
			return syslogAppender.getMaxCacheSize();
		}
		else{
			return 0;
		}
	}
	
	/**
	 * Get the protocol (TCP or UDP) to log the messages.
	 * @return
	 */
	public SyslogNGAppender.Protocol getLogServerProtocol(){
		return logServerprotocol;
	}
	
	/**
	 * Set the application object in order to support logging using a database connection.
	 * @param app
	 */
	public void setApplication( Application app ){
		application = app;
	}
	
	/**
	 * Removes the given hook from the event logger and removes it from the database in order for to prevent it from loading again.
	 * @param hookID
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public void deleteHook(long hookID) throws SQLException, NoDatabaseConnectionException{
		synchronized (hooks) {
			deleteHookInternal(hookID);
		}
	}
	
	/**
	 * Removes the given hook from the event logger and removes it from the database in order for to prevent it from loading again.
	 * @param hookID
	 * @param removeFromDatabaseToo
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	private void deleteHookInternal(long hookID) throws SQLException, NoDatabaseConnectionException{
		
		Iterator<EventLogHook> iterator = hooks.iterator();
		
		while(iterator.hasNext()){
			EventLogHook hook = iterator.next();
			
			if(hook.getEventLogHookID() == hookID ){
				iterator.remove();
				hook.delete();
			}
		}
	}
	
	public void loadHooks() throws NoDatabaseConnectionException, SQLException{
		
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet result = null;
		SQLException initCause = null;
		
		try{
			connection = application.getDatabaseConnection(DatabaseAccessType.ACTION);
			
			statement = connection.prepareStatement("Select * from EventLogHook");
			
			result = statement.executeQuery();
			
			while( result.next() ){
				EventLogHook hook = EventLogHook.loadFromDatabase(result);
				
				//Add the hook if it was instantiated properly
				if( hook != null ){
					addHook(hook);
				}
			}
		}
		catch(SQLException e){
			initCause = e;
		}
		finally{
			try{
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
			catch(SQLException ex){
				if( ex.getCause() == null && initCause != null ){
					ex.initCause(initCause);
					throw ex;
				}
				else if(initCause != null ){
					throw initCause;
				}
				else{
					throw ex;
				}
			}
		}
	}
	
	/**
	 * Get the event log hook associated with the given identifier. Returns null if no hook with the given identifier is found.
	 * @param eventlogHookID
	 * @return
	 */
	public EventLogHook getHook(long eventlogHookID ){
		
		synchronized (hooks) {
			
			for (EventLogHook hook : hooks) {
				if( hook.getEventLogHookID() == eventlogHookID ){
					return hook;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Get a list of all of the EventLogHooks.
	 * @return
	 */
	public EventLogHook[] getHooks(){
		
		synchronized (hooks) {
			
			EventLogHook[] hooksArray = new EventLogHook[hooks.size()];
			
			hooks.toArray(hooksArray);
			
			return hooksArray;
		}
	}
	
	/**
	 * Add thie given EventLogHook to the current list.
	 * @param hook
	 */
	public void addHook( EventLogHook hook ){
		
		// 0 -- Precondition check
		if( hook == null ){
			throw new IllegalArgumentException("The event log hook cannot be null");
		}
		
		// 1 -- Add the hook to the list
		synchronized (hooks) {
			hooks.add(hook);
		}
	}
	
	/**
	 * Remove the given EventLogHook from the list of hooks.
	 * @param hook
	 * @return
	 */
	public boolean removeHook( EventLogHook hook ){
		synchronized (hooks) {
			return hooks.remove(hook);
		}
	}
	
	/**
	 * @precondition None.
	 * @postcondition The method will return a boolean indicating if the log messages are repeated to the console interface. 
	 * @param repeat A boolean indicating if the log messages should be copied to the console
	 * @return
	 */
	public boolean getRepeatMessagesToConsole(){
		return repeatToConsole;
	}
	
	public void logEvent( EventLogMessage message ) throws IllegalArgumentException {
		logEvent( message, true );
	}
	
	public void logEvent( EventLogMessage message, boolean sendToExternalLogger ) throws IllegalArgumentException {
		
		// 0 -- Precondition check
		if( message == null ){
			throw new IllegalArgumentException("The message cannot be null");
		}
		
		
		// 1 -- Write the event entry to the database
		writeEventToDatabase( message.getDate(), message.getSeverity(), message.getMessageName(), message.getFieldsAsString(false) );
		
		
		// 2 -- Send the log to logger
		String messageString;
		
		if( formatter == null ){
			messageString =  message.toString();
		}
		else{
			messageString = formatter.formatMessage(message);
		}
		
		
		// 3 -- Run any event log hooks
		
			for (EventLogHook hook : hooks) {
				try{
					hook.processEvent(message);
				}
				catch(EventLogHookException e){
					EventLogMessage msg = new EventLogMessage(Category.RESPONSE_ACTION_FAILED);
					
					msg.addField(new EventLogField(FieldName.RESPONSE_ACTION_ID, hook.getEventLogHookID()));
					msg.addField(new EventLogField(FieldName.RESPONSE_ACTION_NAME, hook.getAction().getDescription()));
					msg.addField(new EventLogField(FieldName.RESPONSE_ACTION_DESC, hook.getAction().getConfigDescription()));
					
					logExceptionEvent(msg, e);
				}
				
			}
		
		// 4 -- Log to the log4j logger
		if( sendToExternalLogger ){
			logger.log(message.getSeverity(), messageString);
		}
		
		
		// 5 -- Repeat the message to the console (if requested)
		if( repeatToConsole ){
			System.err.println( message.toString() );
		}
	}
	
	/**
	 * Writes the given log to the database.
	 * @param datetime
	 * @param severity
	 * @param title
	 * @param additionalData
	 */
	private synchronized void writeEventToDatabase( Date datetime, EventLogSeverity severity, String title, String additionalData ){
		
		try{
			// 1 -- Attempt to get a reference to an application object
			Application app = this.application;
			
			if( app == null ){
				app = Application.getApplication();
			}
			
			if( app == null ){
				return;
			}
			
			// 2 -- Write the entry
			PreparedStatement statement = null;
			Connection connection = null;
			
			try{
				connection = app.getDatabaseConnection(DatabaseAccessType.EVENT_LOG);
				
				statement = connection.prepareStatement("Insert into EventLog (LogDate, Severity, Title, Notes) values (?, ?, ?, ?)");
				
				SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
				statement.setString(1, sdf.format( datetime ) );
				statement.setInt(2, severity.getSeverity());
				statement.setString(3, title);
				
				// Make sure the entry is small enough, truncate it if it is too big. 
				if( additionalData.length() > 16000 ){
					statement.setString(4, additionalData.substring(0, 15990) + "[...]");
				}
				else{
					statement.setString(4, additionalData);
				}
				
				statement.execute();
			}
			catch(Exception e){
				//Swallow this exception. The application has no where else to log the exception since this is the exception logger.
				e.printStackTrace();
			}
			finally{
				if( connection != null ){
					connection.close();
				}
				
				if( statement != null ){
					statement.close();
				}
			}
		}
		catch(Exception e){
			// Swallow this exception. The application has no where else to log the exception since this is the exception logger.
			e.printStackTrace();
		}
	}
	
	/**
	 * Convert a throwable exception to a string.
	 * @param t
	 * @return
	 */
	private static String getStackTrace(Throwable t){
		return getStackTrace( t, 50);
	}
	private static String getStackTrace(Throwable t, int max_size)
    {
		StringBuffer buffer = new StringBuffer();
		int iterations = 0;
		StackTraceElement[] stack_trace = t.getStackTrace();
		
		if( stack_trace.length > max_size ){
			iterations = max_size;
		}
		else{
			iterations = stack_trace.length;
		}
		
		buffer.append(t);
		buffer.append("\n");
		
		for( int c = 0; c < iterations; c++ ){
			buffer.append(stack_trace[c]);
			
			if( c < (iterations-1) ){
				buffer.append(" at ");
			}
			
		}
		
		if( iterations != stack_trace.length){
			buffer.append("\n... (" + (stack_trace.length-iterations) +" more omitted)");
		}
		
		return buffer.toString();
    }
	
	/***
	 * Log the given exception event.
	 * @param message
	 * @param throwable
	 * @param sendToExternalLogger
	 * @throws IllegalArgumentException
	 */
	public synchronized void logExceptionEvent ( EventLogMessage message, Throwable throwable, boolean sendToExternalLogger) throws IllegalArgumentException {
		String stringRepresentation = getStackTrace( throwable );
		
		message.addField( new EventLogField(FieldName.STACK_TRACE, stringRepresentation) );
		
		logEvent(message, sendToExternalLogger);
	}
	
	/**
	 * Log the given exception event.
	 * @param logSeverity The severity of the log entry (based on Syslog)
	 * @param throwable The exception object that should be logged
	 * @throws IllegalArgumentException
	 */
	public synchronized void logExceptionEvent ( EventLogMessage message, Throwable throwable) throws IllegalArgumentException {
		String stringRepresentation = getStackTrace( throwable );
		
		message.addField( new EventLogField(FieldName.STACK_TRACE, stringRepresentation) );
		
		logEvent(message);
	}
	
	/**
	 * Gets the logging level (logs with severity less than the logging level are ignored).
	 * @precondition None
	 * @postcondition The logging level is returned
	 * @return
	 */
	public EventLogSeverity getLoggingLevel(){
		return loggingLevel;
	}
	
	/**
	 * Sets the logging level. All logs with severity values less than the given value are ignored.
	 * @param newLogLevel
	 * @return
	 */
	public void setLoggingLevel( EventLogSeverity newLogLevel ) {	
		loggingLevel = newLogLevel;
	}
	
	
}
