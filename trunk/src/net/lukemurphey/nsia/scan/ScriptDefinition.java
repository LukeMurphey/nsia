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
import net.lukemurphey.nsia.eventlog.EventLogMessage.Category;
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

public class ScriptDefinition extends Definition {

	private static final int MAX_SCRIPT_RUNTIME = 10000;
	private String script;
	private ScriptEngine scriptEngine;
	private boolean isInvasive = false;
	
	/**
	 * This enumeration represents the requested operation.
	 */
	public enum Operation{
		SCAN, BASELINE
	}
	
	public static final String DEFAULT_ENGINE= "ECMAScript";
	
	private static final Pattern COMMENTS_REGEX = Pattern.compile("(/\\*.*?\\*/)|(//.*$)", Pattern.MULTILINE | Pattern.DOTALL);
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
		
		// 1 -- Initialize the scripting engine
		ScriptEngineManager sem = new ScriptEngineManager();
		
		//	 1.1 -- Set the context such that the script class loader will be used.
		Context context = Context.enter();
		context.setApplicationClassLoader(new ScriptClassLoader());
		//context.setClassShutter(new ScriptClassShutter());
		
		//	 1.2 -- Get the relevant engine
		if( scriptingEngine == null ){
			scriptEngine = sem.getEngineByName(DEFAULT_ENGINE);
		}
		else
		{
			scriptEngine = sem.getEngineByName(scriptingEngine);
		}
		
		//	 1.3 -- Exit the context
		Context.exit();

		
		// 2 -- Try to compile the script
		this.id = -1;
		populateDetailsFromComments( script );
		this.script = script;
		
		//	 2.1 -- Throw an exception if the identifier was not set
		/*if(this.id == -1){
			throw new InvalidDefinitionException("The definition script does not have a valid ID");
		}*/
		
		//	 2.2 -- Try to compile the script; throw an exception if the script could not be compiled
		try{
			scriptEngine.eval(script);
		}
		catch(ScriptException e){
			throw new InvalidDefinitionException("The definition script does not appear to be valid (the scripting engine rejected it)", e);
		}
		
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
				if( name.equalsIgnoreCase( "Version" ) ){
					version = stripLeadingQuotes( value );
				}
				else if( name.equalsIgnoreCase( "Name" ) ){
					completeName = stripLeadingQuotes( value );
				}
				else if( name.equalsIgnoreCase( "ID" ) ){
					identifier = stripLeadingQuotes( value );
				}
				else if( name.equalsIgnoreCase( "Message" ) ){
					message = stripLeadingQuotes( value );
				}
				else if( name.equalsIgnoreCase( "Reference" ) ){
					Reference reference = Reference.parse( stripLeadingQuotes( value ) );
					references.add(reference);
				}
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
	 */
	public boolean baseline( ScanResult scanResult ) throws SQLException, NoDatabaseConnectionException, ScriptException, IOException{
		
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
	
	private SavedScriptData getSavedScriptData( Connection connection, long scanRuleID, String uniqueResourceName ) throws SQLException{
		SavedScriptData data = null;
		
			
			try {
				data = SavedScriptData.load( connection, scanRuleID, this.toString(), uniqueResourceName);
			} catch (IOException e) {
				EventLogMessage message = new EventLogMessage(Category.SCAN_ENGINE_EXCEPTION);
				message.addField(new EventLogField(EventLogField.FieldName.DEFINITION_NAME, this.name));
				
				if( this.id >= 0 ){
					message.addField(new EventLogField(EventLogField.FieldName.DEFINITION_ID, this.id));
				}
				
				Application.getApplication().logExceptionEvent(message, e);
				
				return new SavedScriptData(scanRuleID, this.name);
			} catch (ClassNotFoundException e) {
				
				EventLogMessage message = new EventLogMessage(Category.SCAN_ENGINE_EXCEPTION);
				message.addField(new EventLogField(EventLogField.FieldName.DEFINITION_NAME, this.name));
				
				if( this.id >= 0 ){
					message.addField(new EventLogField(EventLogField.FieldName.DEFINITION_ID, this.id));
				}
				
				Application.getApplication().logExceptionEvent(message, e);
				
				return new SavedScriptData(scanRuleID, this.name);
			}
		
		return data;
	}

	private boolean baseline( long scanRuleID, long scanResultID, String uniqueResourceName ) throws SQLException, NoDatabaseConnectionException, ScriptException, IOException{
		Connection connection = null;
		boolean baselineComplete = false;
		
		try{
			connection = Application.getApplication().getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			
			// 1 -- Setup the environment
			SavedScriptData data = getSavedScriptData(connection, scanRuleID, uniqueResourceName);
			Environment env = new Environment(data);
			
			
			// 2 -- Execute the script
			
			//	 2.1 -- Call the baseline method
			Invocable invocable = (Invocable)scriptEngine;
			
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
				result = (Result)invocable.invokeFunction("analyze", httpResponse, Operation.SCAN, variables, env, false );
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
			InvokerThread thread = new InvokerThread(invocable, httpResponse, Operation.SCAN, variables, env, false );
			thread.setName("ScriptDefinition " + this.getFullName() );
			
			synchronized (thread.mutex) {
				try {
					thread.start();
					thread.mutex.wait(maxRuntime);
				} catch (InterruptedException e1) {
					//Thread was forceably awoken, ignore this and let the thread complete
				}
			}
			
			//If the thread is still running, then log that the script failed to complete within the defined time period
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
		
		Environment env = new Environment(data);
		populateBindings(scriptEngine);
		
		// 2 -- Execute the script
		
		//	 2.1 -- Call the analyze method
		Invocable invocable = (Invocable)scriptEngine;
		Result result = performAnalysis(MAX_SCRIPT_RUNTIME, invocable, httpResponse, variables, env, ruleId );
		
		//	 2.3 -- Save the state variables
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
	
	private class InvokerThread extends Thread{
		private Invocable invocable;
		private Result result = null;
		private HttpResponseData httpResponse;
		private Operation operation;
		private Variables variables;
		private boolean defaultRule;
		private Environment env;
		
		private Throwable e = null;
		private boolean isRunning = false;
		private Object mutex = new Object();
		
		public InvokerThread(Invocable invocable, HttpResponseData httpResponse, Operation operation, Variables variables, Environment env, boolean defaultRule){
			this.invocable = invocable;
			this.httpResponse = httpResponse;
			this.variables = variables;
			this.defaultRule = defaultRule;
			this.operation = operation;
			this.env = env;
		}
		
		@Override
		public void run(){
			
			isRunning = true;
			
			try{
				result = (Result)invocable.invokeFunction("analyze", httpResponse, operation, variables, env, this.defaultRule );
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
		
		public boolean isRunning(){
			return isRunning;
		}
		
		public Result getResult(){
			return result;
		}
		
		public Throwable getThrowable(){
			return e;
		}
	}
	
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
		private long ruleId;
		private String definitionName;
		private Vector<NameValuePair> pairs = new Vector<NameValuePair>();
		private boolean itemsChanged = false;
		
		public SavedScriptData( long ruleId, String definitionName){
			this.ruleId = ruleId;
			this.definitionName = definitionName;
		}
		
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
		
		public long getRuleID(){
			return ruleId;
		}

		public String getDefinitionName(){
			return definitionName;
		}
		
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
		
		public void remove( int location ){
			synchronized ( pairs ){
				itemsChanged = true;
				pairs.remove(location);
			}
		}
		
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
		
		public void removeAll( ){
			synchronized ( pairs ){
				itemsChanged = true;
				pairs.clear();
			}
		}
		
		public void set( String name, Serializable value ){
			synchronized ( pairs ){
				removeInternal(name);
				pairs.add( new NameValuePair(name, value) );
				itemsChanged = true;
			}
		}
		
		public void set( String name, Externalizable value ){
			synchronized ( pairs ){
				itemsChanged = true;
				pairs.add( new NameValuePair(name, value) );
			}
		}
		
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
					EventLogMessage message = new EventLogMessage(Category.DEFINITION_ERROR);
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
