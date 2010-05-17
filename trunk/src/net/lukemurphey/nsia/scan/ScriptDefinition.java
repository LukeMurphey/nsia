package net.lukemurphey.nsia.scan;

import javax.script.*;

import sun.org.mozilla.javascript.internal.Context;

import java.util.Iterator;
import java.util.Vector;
import java.util.regex.*; 

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.Application.DatabaseAccessType;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogMessage.EventType;
import net.lukemurphey.nsia.scan.scriptenvironment.Debug;
import net.lukemurphey.nsia.scan.scriptenvironment.StringUtils;
import net.lukemurphey.nsia.scan.scriptenvironment.Result;
import net.lukemurphey.nsia.scan.scriptenvironment.Variables;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.*;

/**
 * Script definitions (a.k.a. ThreatScript definitions) are used to performed advanced analysis of web-site content. Script definitions
 * are written in a high-level programming language and can therefore perform more advanced analysis than Pattern definitions. Additionally,
 * Script definitions can declare a baselining function that allows them to self-baseline on content that user indicates is clean.
 * 
 * The ScriptDefinition class must be capable of gracefully handling cases where the analysis script contains a defect. Therefore, the 
 * ScriptDefinition class will identify lexical and some semantic errors when the definition is compiled. Additionally, the ScriptDefinition
 * class will monitor the execution of the script at runtime and terminate it if it runs too long. Any detected error will be reported to
 * the event logging sub-system.
 * 
 * ScriptDefinition will populate the scripts namespace with objects reguired for execution. For security reasons a custom class-loader will
 * be used that restricts the classes that can be loaded and uses simplified names for the packages.
 * @author Luke
 * 
 */
public class ScriptDefinition extends Definition {

	//The maximum amount of time the script is allowed to run
	private static final int MAX_SCRIPT_RUNTIME = 10000;
	
	//The maximum time a script terminate is allowed
	private static final int MAX_SCRIPT_TERMINATE_RUNTIME = 10000;
	
	//The code to be executed
	private String script;
	
	//The type of scripting engine to execute
	private String scriptingEngine;
	
	//Whether or not the definition is invasive (may cause permanent effects on the scanned resources)
	private boolean isInvasive = false;
	
	/**
	 * This enumeration represents the requested operation.
	 */
	public enum Operation{
		SCAN, BASELINE
	}
	
	//The default scripting language
	public static final String DEFAULT_ENGINE = "ECMAScript";
	
	//The following regular expression finds comments in the ThreatScript
	private static final Pattern COMMENTS_REGEX = Pattern.compile("(/\\*.*?\\*/)|(//.*$)", Pattern.MULTILINE | Pattern.DOTALL);
	
	//The following regular expression find the meta options in the comments
	private static final Pattern OPTION_REGEX = Pattern.compile("(Version|Name|ID|Message|Reference|Severity|Invasive)[ ]*\\:[ ]*([-\\w.\"/\\\\ (),?%=]+)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	
	public static ScriptDefinition parse(String script) throws InvalidDefinitionException{
		return new ScriptDefinition( script );
	}
	
	public static ScriptDefinition parse(String script, String scriptingEngine) throws InvalidDefinitionException{
		return new ScriptDefinition( script, scriptingEngine );
	}
	
	public static ScriptDefinition parse(String script, int localID) throws InvalidDefinitionException{
		return new ScriptDefinition( script, localID );
	}
	
	public static ScriptDefinition parse(String script, String scriptingEngine, int localID) throws InvalidDefinitionException{
		return new ScriptDefinition( script, scriptingEngine, localID );
	}
	
	private ScriptDefinition( String script ) throws InvalidDefinitionException{
		this(script, null);
	}
	
	private ScriptDefinition( String script, int localID ) throws InvalidDefinitionException{
		this(script, null, localID);
	}
	
	private ScriptDefinition( String script, String scriptingEngine) throws InvalidDefinitionException{
		this(script, null, -1);
	}
	
	private ScriptDefinition( String script, String scriptingEngine, int localID) throws InvalidDefinitionException{
		
		// 0 -- Precondition Check
		
		//	 0.1 -- make sure the script is not null
		if( script == null ){
			throw new IllegalArgumentException("The script must not be null");
		}
		
		definitionType = "ThreatScript";
		this.script = script;
		this.scriptingEngine = scriptingEngine;
		
		//Make sure the script parses
		getScriptEngine();
		
		// Populate the local variables from the comments
		this.id = -1;
		populateDetailsFromComments( script );
		this.localId = localID;
	}

	/**
	 * Strips the leading quote marks (converts "value" to value)
	 * @param value
	 * @return
	 */
	private String stripLeadingQuotes( String value ){
		value = value.trim();
		
		if( value.startsWith("\"") && value.endsWith("\"")){
			value = value.substring(1, value.length() - 1 );
		}
		
		return value;
	}
	
	/**
	 * Get a scripting engine to be used in the analysis
	 * @return
	 * @throws InvalidDefinitionException
	 */
	private ScriptEngine getScriptEngine( ) throws InvalidDefinitionException{
		
		// 1 -- Initialize the scripting engine
		ScriptEngineManager sem = new ScriptEngineManager();
		ScriptEngine scriptEngine = null;
		
		// 2 -- Set the context such that the script class loader will be used.
		Context context = Context.enter();
		context.setApplicationClassLoader(new ScriptClassLoader());
		//context.setClassShutter(new ScriptClassShutter());
		
		// 3 -- Get the relevant engine
		if( scriptingEngine == null ){
			scriptEngine = sem.getEngineByName(DEFAULT_ENGINE);
		}
		else
		{
			scriptEngine = sem.getEngineByName(scriptingEngine);
		}
		
		// Exit the context
		Context.exit();
		
		// 4 -- Try to compile the script; throw an exception if the script could not be compiled
		try{
			scriptEngine.eval(script);
		}
		catch(ScriptException e){
			throw new InvalidDefinitionException("The definition script does not appear to be valid (the scripting engine rejected it)", e);
		}
		
		return scriptEngine;
	}
	
	/**
	 * Retrieves the script details (name, version, etc) from the script comments.
	 * @param script
	 * @throws InvalidDefinitionException
	 */
	private void populateDetailsFromComments(String script) throws InvalidDefinitionException{
		
		// 0 -- Precondition check
		
		// 1 -- Loop through the comments and get the meta-data  
		String completeName = null;
		String version = null;
		String identifier = null;
		String message = null;
		
		Matcher commentMatcher = COMMENTS_REGEX.matcher(script);
		
		while( commentMatcher.find() ){
			
			Matcher optionsMatcher = OPTION_REGEX.matcher( commentMatcher.group(0) );
			
			while( optionsMatcher.find() ){
				String name = optionsMatcher.group(1);
				String value = optionsMatcher.group(2);
				
				//Get the version
				if( name.equalsIgnoreCase( "Version" ) ){
					version = stripLeadingQuotes( value );
				}
				
				//Get the name
				else if( name.equalsIgnoreCase( "Name" ) ){
					completeName = stripLeadingQuotes( value );
				}
				
				//Get the ID
				else if( name.equalsIgnoreCase( "ID" ) ){
					identifier = stripLeadingQuotes( value );
				}
				
				//Get the default message
				else if( name.equalsIgnoreCase( "Message" ) ){
					message = stripLeadingQuotes( value );
				}
				
				//Get any references
				else if( name.equalsIgnoreCase( "Reference" ) ){
					Reference reference = Reference.parse( stripLeadingQuotes( value ) );
					references.add(reference);
				}
				
				//Determine if the definition is invasive
				else if( name.equalsIgnoreCase( "Invasive" ) ){
					String tmpValue = stripLeadingQuotes( value );
					
					if( tmpValue.equalsIgnoreCase("true")){
						isInvasive = true;
					}
					else if( tmpValue.equalsIgnoreCase("false")){
						isInvasive = false;
					}
					else{
						throw new InvalidDefinitionException("The invasive flag is invalid (must be either \"true\" or \"false\")");
					}
				}
				
				//Get the severity
				else if( name.equalsIgnoreCase( "Severity" ) ){
					String severity = stripLeadingQuotes( value );
					if( severity.equalsIgnoreCase("Low")){
						this.severity = Severity.LOW;
					}
					else if( severity.equalsIgnoreCase("Medium") || severity.equalsIgnoreCase("Med")){
						this.severity = Severity.MEDIUM;
					}
					else if( severity.equalsIgnoreCase("High")){
						this.severity = Severity.HIGH;
					}
					else{
						throw new InvalidDefinitionException("The severity is invalid");
					}
				}
			}
		}
		
		
		// 2 -- Determine if the parameters are valid
		
		//	 2.1 -- The name
		if( completeName == null ){
			throw new InvalidDefinitionException("The script must provide a name. The name must be provided in a comment; example: \"//Name = Exploit.Suspicious.StealthJavaScript\"");
		}
		else{
			parseFullName(completeName);
		}
		
		//	 2.2 -- The version
		if( version == null ){
			throw new InvalidDefinitionException("The script must provide a version. The version must be provided in a comment; example: \"//Version = 1\"");
		}
		else{
			try{
				this.revision = Integer.parseInt(version);
			}
			catch(NumberFormatException e){
				throw new InvalidDefinitionException("The version number is not a valid number");
			}
		}
		
		//	 2.3 -- The identifier
		if( identifier != null ){
			try{
				this.id = Integer.parseInt(identifier);
			}
			catch(NumberFormatException e){
				throw new InvalidDefinitionException("The ID number is not a valid number");
			}
			
			if( this.id < 1 ){
				throw new InvalidDefinitionException("The value for the ID must be greater than zero");
			}
		}
		
		//	 2.4 -- The message
		if( message == null ){
			throw new InvalidDefinitionException("The script must provide a default message. The message must be provided in a comment; example: \"//Message = Obfuscated JavaScript Detected\"");
		}
		else{
			this.message = message;
		}
		
		//	 2.5 -- The severity
		if( severity == null || severity == Severity.UNDEFINED ){
			throw new InvalidDefinitionException("The script must provide severity level; example: \"//Severity = Medium\"");
		}
		else{
			this.message = message;
		}
		
	}
	
	/**
	 * Returns the actual script code.
	 * @return
	 */
	public String getScript(){
		return script;
	}
	
	/**
	 * This function invokes the "baseline" method of the rule. The baseline method is intended to reset the baseline such that the data associated
	 * with the given scan result will no longer trigger the definition.
	 * @param scanResult
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws ScriptException
	 * @throws IOException 
	 * @throws InvalidDefinitionException 
	 */
	public boolean baseline( ScanResult scanResult ) throws SQLException, NoDatabaseConnectionException, ScriptException, IOException, InvalidDefinitionException{
		
		// 0 -- Precondition check
		if( scanResult == null ){
			throw new IllegalArgumentException("The scan result cannot be null");
		}
		
		// 1 -- Baseline the scan result
		return baseline( scanResult.getRuleID(), scanResult.getScanResultID(), scanResult.getSpecimenDescription() );
	}
	
	/**
	 * Indicates if the definition is invasive.
	 * @return
	 */
	public boolean isInvasive(){
		return isInvasive;
	}
	
	/**
	 * Get the saved script data.
	 * @param connection
	 * @param scanRuleID
	 * @param uniqueResourceName
	 * @return
	 * @throws SQLException
	 */
	private SavedScriptData getSavedScriptData( Connection connection, long scanRuleID, String uniqueResourceName ) throws SQLException{
		SavedScriptData data = null;
		
		try {
			data = SavedScriptData.load( connection, scanRuleID, this.toString(), uniqueResourceName);
		} catch (IOException e) {
			EventLogMessage message = new EventLogMessage(EventType.SCAN_ENGINE_EXCEPTION);
			message.addField(new EventLogField(EventLogField.FieldName.DEFINITION_NAME, this.name));

			if( this.id >= 0 ){
				message.addField(new EventLogField(EventLogField.FieldName.DEFINITION_ID, this.id));
			}

			Application.getApplication().logExceptionEvent(message, e);

			return new SavedScriptData(scanRuleID, this.name);
		} catch (ClassNotFoundException e) {

			EventLogMessage message = new EventLogMessage(EventType.SCAN_ENGINE_EXCEPTION);
			message.addField(new EventLogField(EventLogField.FieldName.DEFINITION_NAME, this.name));

			if( this.id >= 0 ){
				message.addField(new EventLogField(EventLogField.FieldName.DEFINITION_ID, this.id));
			}

			Application.getApplication().logExceptionEvent(message, e);

			return new SavedScriptData(scanRuleID, this.name);
		}
		
		return data;
	}

	/**
	 * Baseline the scan rule based on the findings in the given scan result ID.
	 * @param scanRuleID
	 * @param scanResultID
	 * @param uniqueResourceName
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws ScriptException
	 * @throws IOException
	 * @throws InvalidDefinitionException 
	 */
	private boolean baseline( long scanRuleID, long scanResultID, String uniqueResourceName ) throws SQLException, NoDatabaseConnectionException, ScriptException, IOException, InvalidDefinitionException{
		Connection connection = null;
		boolean baselineComplete = false;
		
		try{
			connection = Application.getApplication().getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			// 1 -- Setup the environment
			SavedScriptData data = getSavedScriptData(connection, scanRuleID, uniqueResourceName);
			Environment env = new Environment(data);
			
			
			// 2 -- Execute the script
			
			//	 2.1 -- Call the baseline method
			Invocable invocable = (Invocable)getScriptEngine();
			
			try {
				Object result = invocable.invokeFunction("baseline", env );
				baselineComplete = true;
				if( result != null && result instanceof Boolean ){
					baselineComplete = ((Boolean)result).booleanValue();
				}
				
			} catch (NoSuchMethodException e) {
				//No baseline function is defined, just return false noting that the baseline method is unavailable
				return false;
			}
	
			//	 2.2 -- Save the state variables
			if( data != null ){
				data.save(connection, uniqueResourceName);
			}
			
		}
		finally{
			if( connection != null ){
				connection.close();
			}
		}
		
		return baselineComplete;		
	}
	
	/**
	 * This method populates the script engine's environment with classes and objects it needs to execute the definition.
	 * @param scriptEngine
	 */
	private void populateBindings( ScriptEngine scriptEngine ){
		//Note: SimpleBindings can also be used to populate the script environment
		
		scriptEngine.put("StringUtils", new StringUtils());
		scriptEngine.put("Debug", new Debug(this));
	}
	
	/**
	 * This method evaluates the given data with this definition. The variables argument includes the list of variables that have been set by other definitions that were evalauted
	 * in the current pass. The rule identifier is used in order to load the saved script data for the given definition. This method will attempt to load a database connection
	 * from the default application class.
	 * @param httpResponse
	 * @param variables List of variables set by other definitions evaluated in the current pass
	 * @param ruleId The rule ID that this definition is being evaluated for (used to load the script data which is stored per definition and rule combination)
	 * @return
	 * @throws ScriptException
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws NoSuchMethodException 
	 * @throws IOException 
	 * @throws DefinitionEvaluationException 
	 * @throws SQLException 
	 */
	public Result evaluate( HttpResponseData httpResponse, Variables variables, long ruleId ) throws DefinitionEvaluationException, SQLException{
		Connection connection = null;
		
		try{
			connection = Application.getApplication().getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
			return evaluate( httpResponse, variables, ruleId, connection );
		}
		catch(NoDatabaseConnectionException e){
			throw new DefinitionEvaluationException("Database connection could not be established", e);
		}
		finally{
			if( connection != null ){
				connection.close();
			}
		}
		
	}
	
	/**
	 * Represents the result of calling terminate on a script definition.
	 * @author Luke
	 *
	 */
	private enum TerminateResult{
		NO_TERMINATE_DECLARED, TERMINATED_SUCCESSFULLY, TERMINATE_UNSUCCESSFUL;
	}
	
	/**
	 * Attempts to terminate a script-definition by calling the terminate function.
	 * @param invocable
	 * @param maxRuntime
	 * @param ruleID
	 * @return
	 */
	@SuppressWarnings("deprecation")
	private TerminateResult terminate( Invocable invocable, int maxRuntime, long ruleID ){
		
		TerminatorThread terminatorThread = new TerminatorThread( invocable );
		terminatorThread.setName("ScriptDefinition " + this.getFullName() + " (terminate call)" );
		
		synchronized (terminatorThread.mutex) {
			try {
				terminatorThread.start();
				terminatorThread.mutex.wait(maxRuntime);
			} catch (InterruptedException e1) {
				//Thread was forcibly awoken, ignore this and let the thread complete
			}
			finally{
				//Force it to terminate if the thread is still running
				if( terminatorThread.isRunning() ){
					terminatorThread.stop(); //Yes, it is deprecated, but the script runtime does not have a function to shutdown the script so this is the only way.
					return TerminateResult.TERMINATE_UNSUCCESSFUL;
				}
			}
		}
		
		//Log an error if the terminate function failed
		if( terminatorThread.getThrowable() != null ){
			DefinitionErrorList.logError(this.getFullName(), this.revision, "Runtime exception", "Rule ID " + ruleID , this.id, this.localId);
		}
		
		// Return a value indicating if the terminate function was called.
		if( terminatorThread.declaresTerminate() ){
			return TerminateResult.TERMINATED_SUCCESSFULLY;
		}
		else{
			return TerminateResult.NO_TERMINATE_DECLARED;
		}
		
	}
	
	/**
	 * Dispatches a thread to perform an analysis of the given HTTP response and returns the result. The analysis
	 * thread is terminated if it fails to complete within a the time set according to the maxRuntime argument.
	 * No limit is used if the maxRuntime argument is set to 0.  
	 * @param maxRuntime
	 * @param invocable
	 * @param httpResponse
	 * @param variables
	 * @param env
	 * @param ruleId
	 * @return
	 * @throws DefinitionEvaluationException
	 */
	@SuppressWarnings("deprecation")
	private Result performAnalysis( int maxRuntime, Invocable invocable, HttpResponseData httpResponse, Variables variables, Environment env, long ruleId ) throws DefinitionEvaluationException{
		Result result = null;
		
		if( maxRuntime <= 0 ){
			try {
				result = (Result)invocable.invokeFunction("analyze", httpResponse, variables, env );
			} catch (ScriptException e) {
				DefinitionErrorList.logError(this.getFullName(), this.revision, "Runtime exception", "Rule ID " + ruleId , this.id, this.localId);
				throw new DefinitionEvaluationException("The definition threw an exception (ID " + ruleId + ", definition \"" + this.getFullName() + "\")", e);
			} catch (NoSuchMethodException e) {
				DefinitionErrorList.logError(this.getFullName(), this.revision, "Missing method", "Rule ID " + ruleId , this.id, this.localId);
				throw new DefinitionEvaluationException("The definition threw an exception (ID " + ruleId + ", definition \"" + this.getFullName() + "\")", e);
			}
		}
		else {
			// Create and start the thread that will be responsible for performing the scan
			InvokerThread thread = new InvokerThread(invocable, httpResponse, variables, env );
			thread.setName("ScriptDefinition " + this.getFullName() );
			
			synchronized (thread.mutex) {
				try {
					thread.start();
					thread.mutex.wait(maxRuntime);
				} catch (InterruptedException e1) {
					//Thread was forcibly awoken, ignore this and let the thread complete
				} catch(Throwable t){
					t.printStackTrace();
				}
				
			}
			
			//If the thread is still running, then log that the script failed to complete within the defined time period
			if( thread.isRunning() ){
				//First, let's try to call the terminate function (if it exists)
				TerminateResult terminateResult = terminate(invocable, MAX_SCRIPT_TERMINATE_RUNTIME, ruleId);
				
				if( terminateResult == TerminateResult.TERMINATED_SUCCESSFULLY ){
					DefinitionErrorList.logError(this.getFullName(), this.revision, "Execution exceeded limit (but was terminated successfully using terminate call)", "Rule ID " + ruleId , this.id, this.localId);
				}
				else if( terminateResult == TerminateResult.TERMINATE_UNSUCCESSFUL ){
					DefinitionErrorList.logError(this.getFullName(), this.revision, "Execution exceeded limit and terminate request timed out", "Rule ID " + ruleId , this.id, this.localId);
				}
			}
			
			//Log the error and force the thread to stop if it is still running
			if( thread.isRunning() ){
				thread.stop(); //This function is deprecated, however, it is the only way to terminate this thread since the script engine does not have a method for terminating execution
				DefinitionErrorList.logError(this.getFullName(), this.revision, "Execution exceeded limit", "Rule ID " + ruleId , this.id, this.localId);
				throw new DefinitionEvaluationException("The definition exceeded the maximum timeout (ID " + ruleId + ", definition \"" + this.getFullName() + "\")");
			}
			else if (thread.getThrowable() != null){
				DefinitionErrorList.logError(this.getFullName(), this.revision, "Runtime exception", "Rule ID " + ruleId , this.id, this.localId);
				throw new DefinitionEvaluationException("The definition threw an exception (ID " + ruleId + ", definition \"" + this.getFullName() + "\")", thread.getThrowable());
			}
			else{
				result = thread.getResult();
			}
		}
		
		return result;
	}
	
	/**
	 * This method evaluates the given data with this definition. The variables argument includes the list of variables that have been set by other definitions that were evalauted
	 * in the current pass. The rule identifier is used in order to load the saved script data for the given definition. 
	 * @param httpResponse
	 * @param variables List of variables set by other definitions evaluated in the current pass
	 * @param ruleId The rule ID that this definition is being evaluated for (used to load the script data which is stored per definition and rule combination)
	 * @param connection The database connection.
	 * @return
	 * @throws ScriptException
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws NoSuchMethodException 
	 * @throws IOException 
	 */
	public Result evaluate( HttpResponseData httpResponse, Variables variables, long ruleId, Connection connection ) throws DefinitionEvaluationException{
		
		// 0 -- Precondition Check
		if( httpResponse == null ){
			throw new IllegalArgumentException("The HTTP response argument must not be null");
		}
		
		// 1 -- Setup the environment
		SavedScriptData data;
		
		try{
			data = getSavedScriptData(connection, ruleId, httpResponse.getDataSpecimen().getFilename());
		}
		catch(SQLException e){
			DefinitionErrorList.logError(this.getFullName(), this.revision, "Serialized data could not be loaded", "Rule ID " + ruleId , this.id, this.localId);
			throw new DefinitionEvaluationException("The serialized data could not be loaded from the database for the definition (rule ID " + ruleId + ", definition \"" + this.getFullName() + "\")", e);
		}
		
		ScriptEngine scriptEngine;
		
		//Throw an exception if the definition is not valid
		try {
			scriptEngine = getScriptEngine();
		} catch (InvalidDefinitionException e) {
			throw new DefinitionEvaluationException("Definition is invalid", e);
		}
		
		Environment env = new Environment(data);
		populateBindings(scriptEngine);
		
		// 2 -- Execute the script
		
		//	 2.1 -- Call the analyze method
		Invocable invocable = (Invocable)scriptEngine;
		Result result = performAnalysis(MAX_SCRIPT_RUNTIME, invocable, httpResponse, variables, env, ruleId );
		
		//	 2.2 -- Save the state variables
		if( data != null ){
			try {
				data.save(connection, httpResponse.getDataSpecimen().getFilename());
			} catch (SQLException e) {
				DefinitionErrorList.logError(this.getFullName(), this.revision, "Database error prevented serialization", "Rule ID " + ruleId , this.id, this.localId);
				throw new DefinitionEvaluationException("The definition threw an exception while being serialized to the database (" + ruleId + ", definition \"" + this.getFullName() + "\")", e);
			} catch (IOException e) {
				DefinitionErrorList.logError(this.getFullName(), this.revision, "IO error prevented serialization", "Rule ID " + ruleId , this.id, this.localId);
				throw new DefinitionEvaluationException("The definition threw an exception while being serialized to the database (" + ruleId + ", definition \"" + this.getFullName() + "\")", e);
			}
		}
		
		if(result == null ){
			return new Result(false);
		}
		
	    return result;
	}
	
	/**
	 * This class ised to call the terminate function on the script definitions if necessary
	 * @author Luke
	 *
	 */
	private class TerminatorThread extends Thread{
		
		//The object that contains the invocable functions
		private Invocable invocable;
		
		//Contains any exceptions generated when executing the function
		private Throwable e = null;
		
		//Indicates if the script declares a terminate function
		private boolean terminateExists = true;
		
		//Indicates if the thread is still running (completed it's terminate call)
		private boolean isRunning = false;
		
		//The mutual exclusion used to enforce thread safety and call notify on
		private Object mutex = new Object();
		
		public TerminatorThread(Invocable invocable){
			this.invocable = invocable;
		}
		
		@Override
		public void run(){

			try{
				isRunning = true;
				invocable.invokeFunction("terminate");
				terminateExists = true;
			} catch (NoSuchMethodException e) {
				terminateExists = false;
			} catch(ScriptException e){
				this.e = e;
			} catch (Exception e) {
				this.e = e;
			} catch (Throwable e) {
				this.e = e;
			}
			finally{
				isRunning = false;

				synchronized (mutex) {
					mutex.notifyAll();
				}
			}
		}
		
		/**
		 * Indicates if the thread is still running.
		 * @return
		 */
		public boolean isRunning(){
			return isRunning;
		}
		
		/**
		 * Get any exceptions thrown by the invocable function.
		 * @return
		 */
		public Throwable getThrowable(){
			return e;
		}
		
		/**
		 * Indicates if the invocable declares a terminate function.
		 * @return
		 */
		public boolean declaresTerminate(){
			return terminateExists;
		}
	}
	
	/**
	 * This thread calls the ThreatScript using a new thread (so it can be terminated).
	 * @author Luke
	 *
	 */
	private class InvokerThread extends Thread{
		
		//The object that contains the invocable functions
		private Invocable invocable;
		
		//The result object from the call
		private Result result = null;
		
		//The HTTP response data that was passed to the thread
		private HttpResponseData httpResponse;
		
		//The variables that were passed to the thread
		private Variables variables;
		
		//The environment that was passed to the thread
		private Environment env;
		
		//Contains any exceptions generated when executing the function
		private Throwable e = null;
		
		//Indicates if the thread is running
		private boolean isRunning = false;
		
		//Mutex used to prevent multi-thread access to the invocable function 
		private Object mutex = new Object();
		
		public InvokerThread(Invocable invocable, HttpResponseData httpResponse, Variables variables, Environment env){
			this.invocable = invocable;
			this.httpResponse = httpResponse;
			this.variables = variables;
			this.env = env;
		}
		
		@Override
		public void run(){
			
			isRunning = true;
			
			try{
				result = (Result)invocable.invokeFunction("analyze", httpResponse, variables, env );
			} catch(ScriptException e){
				this.e = e;
			} catch (NoSuchMethodException e) {
				this.e = e;
			} catch (Exception e) {
				this.e = e;
			} catch (Throwable e) {
				this.e = e;
			}
			
			isRunning = false;
			
			synchronized(mutex){
				mutex.notifyAll();
			}
			
		}
		
		/**
		 * Indicates if the thread is still running.
		 * @return
		 */
		public boolean isRunning(){
			return isRunning;
		}
		
		/**
		 * Gets the result from the invocable function.
		 * @return
		 */
		public Result getResult(){
			return result;
		}
		
		/**
		 * Get any exceptions thrown by the invocable function.
		 * @return
		 */
		public Throwable getThrowable(){
			return e;
		}
	}
	
	/**
	 * The environment class stores values that script definitions need to store.
	 * @author Luke
	 *
	 */
	private class Environment{
		private SavedScriptData data;
		
		public Environment(SavedScriptData data){
			this.data = data;
		}
		
		@SuppressWarnings("unused")
		public NameValuePair get(String name){
			return data.get(name);
		}
		
		@SuppressWarnings("unused")
		public void set(String name, int value){
			data.set(name, Integer.valueOf( value ));
		}
		
		@SuppressWarnings("unused")
		public void set(String name, short value){
			data.set(name, Short.valueOf( value ));
		}
		
		@SuppressWarnings("unused")
		public void set(String name, long value){
			data.set(name, Long.valueOf( value ) );
		}
		
		@SuppressWarnings("unused")
		public void set(String name, float value){
			data.set(name, Float.valueOf( value) );
		}
		
		@SuppressWarnings("unused")
		public void set(String name, boolean value){
			data.set(name, Boolean.valueOf(value));
		}
		
		@SuppressWarnings("unused")
		public void set(String name, double value){
			data.set(name, Double.valueOf(value));
		}
		
		@SuppressWarnings("unused")
		public void set(String name, char value){
			data.set(name, Character.valueOf( value ));
		}
		
		@SuppressWarnings("unused")
		public void set(String name, Serializable value){
			data.set(name, value);
		}
		
		@SuppressWarnings("unused")
		public void set(String name, Externalizable value){
			data.set(name, value);
		}
	}
	
	/**
	 * This class provides a wrapper around data variables that can be stored by the script (in order to make it stateful).
	 * @author luke
	 *
	 */
	protected static class SavedScriptData{
		
		//The rule ID associated with this saved script data
		private long ruleId;
		
		//The name of the rule associated with this saved script data
		private String definitionName;
		
		//The values stored for this script
		private Vector<NameValuePair> pairs = new Vector<NameValuePair>();
		
		//This boolean indicates if any of the items have been changes in the data 
		private boolean itemsChanged = false;
		
		public SavedScriptData( long ruleId, String definitionName){
			this.ruleId = ruleId;
			this.definitionName = definitionName;
		}
		
		/**
		 * Load the saved script data.
		 * @param connection
		 * @param ruleId
		 * @param definitionName
		 * @param uniqueResourceName
		 * @return
		 * @throws SQLException
		 * @throws IOException
		 * @throws ClassNotFoundException
		 */
		public static SavedScriptData load( Connection connection, long ruleId, String definitionName, String uniqueResourceName) throws SQLException, IOException, ClassNotFoundException{
			PreparedStatement statement = null;
			ResultSet results = null;
			
			SavedScriptData env = new SavedScriptData(ruleId, definitionName);
			
			try{
				
				// 1 -- Load the environment  
				if( uniqueResourceName == null ){
					statement = connection.prepareStatement("Select * from ScriptEnvironment where DefinitionName = ? and RuleID = ?");
					statement.setString(1, definitionName);
					statement.setLong(2, ruleId);
				}
				else{
					statement = connection.prepareStatement("Select * from ScriptEnvironment where DefinitionName = ? and RuleID = ? and UniqueResourceName = ?");
					statement.setString(1, definitionName);
					statement.setLong(2, ruleId);
					statement.setString(3, uniqueResourceName);
				}
				//statement.setLong(3, resultID);  //and ScanResultID = ?
				
				results = statement.executeQuery();
				
				while( results.next() ){
					
					byte[] bytes = results.getBytes("Value");
					
					ByteArrayInputStream byteInStream = new ByteArrayInputStream(bytes);
					ObjectInputStream inStream = new ObjectInputStream(byteInStream);
					
					NameValuePair pair = new NameValuePair(results.getString("Name"), inStream.readObject());
					env.pairs.add(pair);
				}
			}
			finally{
				if( statement != null ){
					statement.close();
				}
				
				if( results != null ){
					results.close();
				}
			}
			
			return env;
		}
		
		/**
		 * Get the rule ID associated with this saved script data.
		 * @return
		 */
		public long getRuleID(){
			return ruleId;
		}

		/**
		 * Get the definition name associated with this saved script data.
		 * @return
		 */
		public String getDefinitionName(){
			return definitionName;
		}
		
		/**
		 * Get the value associated with the given name.
		 * @param name
		 * @return
		 */
		public NameValuePair get(String name){
			synchronized ( pairs ){
				Iterator<NameValuePair> iterator = pairs.iterator();
				
				while(iterator.hasNext()){
					NameValuePair pair = iterator.next();
					
					if( pair.nameMatches(name)){
						return pair;
					}
				}
				
				return null;
			}
		}
		
		/**
		 * Remove the value at the given index.
		 * @param location
		 */
		public void remove( int location ){
			synchronized ( pairs ){
				itemsChanged = true;
				pairs.remove(location);
			}
		}
		
		/**
		 * Remove the value associated with the given name.
		 * @param name
		 */
		public void remove( String name ){
			synchronized ( pairs ){
				removeInternal(name);
			}
		}
		
		private void removeInternal( String name ){

			Iterator<NameValuePair> iterator = pairs.iterator();

			while(iterator.hasNext()){
				NameValuePair pair = iterator.next();

				if( pair.nameMatches(name)){
					iterator.remove();
					itemsChanged = true;
				}
			}
		}
		
		/**
		 * Remove all values.
		 */
		public void removeAll( ){
			synchronized ( pairs ){
				itemsChanged = true;
				pairs.clear();
			}
		}
		
		/**
		 * Set the value of the name specified.
		 * @param name
		 * @param value
		 */
		public void set( String name, Serializable value ){
			synchronized ( pairs ){
				removeInternal(name);
				pairs.add( new NameValuePair(name, value) );
				itemsChanged = true;
			}
		}
		
		/**
		 * Set the value of the name specified.
		 * @param name
		 * @param value
		 */
		public void set( String name, Externalizable value ){
			synchronized ( pairs ){
				itemsChanged = true;
				pairs.add( new NameValuePair(name, value) );
			}
		}
		
		/**
		 * Save the saved script data to the database.
		 * @param connection
		 * @param uniqueResourceName
		 * @throws SQLException
		 * @throws IOException
		 */
		public synchronized void save( Connection connection, String uniqueResourceName) throws SQLException, IOException{
			if( itemsChanged == true ){
				
				// Get rid of the existing items
				invalidateOldItems( connection, ruleId, definitionName, uniqueResourceName );
				
				// Overwrite the new items
				for( int c = 0; c < pairs.size(); c++){
					saveItem( pairs.get(c), connection, ruleId, definitionName, uniqueResourceName);
				}
			}
		}
		
		/**
		 * Delete old items so that they are recognized as no longer current.
		 * @param connection
		 * @param ruleId
		 * @param definitionName
		 * @param uniqueResourceName
		 * @throws SQLException
		 */
		private static void invalidateOldItems( Connection connection, long ruleId, String definitionName, String uniqueResourceName ) throws SQLException{
			PreparedStatement statement = null;
			
			try{
				if( uniqueResourceName == null ){
					statement = connection.prepareStatement("Delete from ScriptEnvironment where DefinitionName = ? and RuleID = ? and UniqueResourceName is null");
					statement.setString(1, definitionName);
					statement.setLong(2, ruleId);
				}
				else{
					statement = connection.prepareStatement("Delete from ScriptEnvironment where DefinitionName = ? and RuleID = ? and UniqueResourceName = ?");
					statement.setString(1, definitionName);
					statement.setLong(2, ruleId);
					statement.setString(3, uniqueResourceName);
				}
				
				statement.execute();
			}
			finally{
				if( statement != null ){
					statement.close();
				}
			}
		}
		
		/**
		 * Persist the given entry to the database.
		 * @param pair
		 * @param connection
		 * @param ruleId
		 * @param definitionName
		 * @param uniqueResourceName
		 * @throws SQLException
		 * @throws IOException
		 */
		private static void saveItem( NameValuePair pair, Connection connection, long ruleId, String definitionName, String uniqueResourceName ) throws SQLException, IOException{
			
			// 0 -- Precondition check: don't attempt to save the value if it is null
			if( pair == null ){
				return;
			}
			
			// 1 -- Enter the entry into the database
			ByteArrayOutputStream byteOutStream = null; 
			ObjectOutputStream outStream = null;
			
			PreparedStatement insertStatement = null;
			
			try{
				
				if( uniqueResourceName != null ){
					insertStatement = connection.prepareStatement("Insert into ScriptEnvironment (DefinitionName, RuleID, Name, Value, UniqueResourceName) values(?, ?, ?, ?, ?)");
					insertStatement.setString(1, definitionName);
					insertStatement.setLong(2, ruleId);
					insertStatement.setString(3, pair.getName());
					insertStatement.setString(5, uniqueResourceName);
				}
				else{
					insertStatement = connection.prepareStatement("Insert into ScriptEnvironment (DefinitionName, RuleID, Name, Value) values(?, ?, ?, ?)");
					insertStatement.setString(1, definitionName);
					insertStatement.setLong(2, ruleId);
					insertStatement.setString(3, pair.getName());
				}
				
				byteOutStream = new ByteArrayOutputStream();
				outStream = new ObjectOutputStream(byteOutStream);
				
				if( pair.getValue() instanceof Serializable ){
					outStream.writeObject(pair.getValue());
				}
				else if( pair.getValue() instanceof Externalizable ){
					((Externalizable)pair.getValue()).writeExternal( outStream );
				}
				else{
					//The object cannot be written to the database, it is not Serializable or Externalizable
					EventLogMessage message = new EventLogMessage(EventType.DEFINITION_ERROR);
					message.addField(new EventLogField(EventLogField.FieldName.DEFINITION_NAME, definitionName));
					message.addField(new EventLogField(EventLogField.FieldName.MESSAGE, "The field named \"" + pair.getName() + "\" could not be saved to the database because it is neither Serializable or Externalizable"));
					message.addField(new EventLogField(EventLogField.FieldName.VALUE, pair.getName()));
					Application.getApplication().logEvent(message);
				}
				
				byte[] bytes = byteOutStream.toByteArray();
				
				insertStatement.setBytes(4, bytes);
				
				insertStatement.execute();
			}
			finally{
				if( insertStatement != null ){
					insertStatement.close();
				}
			}
			
		}
	}
	
	/**
	 * This class represents a name/value pair.
	 * @author Luke
	 *
	 */
	protected static class NameValuePair{
		private int pairId = -1;
		private String name;
		private Object value;
		
		public NameValuePair( String name, Object value){
			// 0 -- Precondition Check
			if( name == null){
				throw new IllegalArgumentException("The name must not be null");
			}
			
			if( value == null){
				throw new IllegalArgumentException("The value must not be null");
			}
			
			// 1 -- Set the parameters
			this.name = name;
			this.value = value;
		}
		
		public NameValuePair( int pairId, String name, Object value){
			// 0 -- Precondition Check
			if( pairId < 0){
				throw new IllegalArgumentException("The pair ID must be greater than zero");
			}
			
			if( name == null){
				throw new IllegalArgumentException("The name must not be null");
			}
			
			if( value == null){
				throw new IllegalArgumentException("The value must not be null");
			}
			
			// 1 -- Set the parameters
			this.name = name;
			this.value = value;
			this.pairId = pairId;
		}
		
		public boolean nameMatches(String name){
			if( this.name.equalsIgnoreCase(name) ){
				return true;
			}
			else{
				return false;
			}
		}
		
		public String getName(){
			return name;
		}
		
		public Object getValue(){
			return value;
		}
		
		/*public int getValueInt(){
			return Integer.parseInt(value);
		}
		
		public float getValueFloat(){
			return Float.parseFloat(value);
		}
		
		public boolean getValueDouble(){
			return Boolean.parseBoolean(value);
		}
		
		public boolean getValueShort(){
			return Boolean.parseBoolean(value);
		}
		
		public boolean getValueCharacter(){
			return Boolean.parseBoolean(value);
		}
		
		public boolean getValueBoolean(){
			return Boolean.parseBoolean(value);
		}*/
		
		public boolean isPairIDSet(){
			if( pairId < 0 ){
				return false;
			}
			else{
				return true;
			}
		}
		
		public int getPairID(){
			return pairId;
		}
	}

}
