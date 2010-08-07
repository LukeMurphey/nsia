package net.lukemurphey.nsia.response;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.Application.DatabaseAccessType;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogMessage.EventType;
import net.lukemurphey.nsia.extension.ArgumentFieldsInvalidException;
import net.lukemurphey.nsia.extension.FieldLayout;
import net.lukemurphey.nsia.extension.PrototypeField;
import net.lukemurphey.nsia.extension.FieldValidator.FieldValidatorResult;
import net.lukemurphey.nsia.scan.ScanResult;

import org.apache.commons.lang.StringUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public abstract class Action implements Serializable  {
	
	private static final long serialVersionUID = -5095063314974282138L;

	public static class MessageVariable{
		private String name;
		private String value;
		public static final String ARGUMENT_NAME = "\\$[a-zA-Z0-9_]+";
		
		public MessageVariable(String name, String value){
			
			// 0 -- Precondition check
			
			//	 0.1 -- Make sure name is not null
			if( name == null ){
				throw new IllegalArgumentException("The name cannot be null");
			}
			
			//	 0.2 -- Make sure name is properly formatted
			if( Pattern.matches(ARGUMENT_NAME, name) == false ){
				throw new IllegalArgumentException("The name is not valid");
			}
			 
			
			// 1 -- Initialize the class
			this.name = name;
			this.value = value;
		}
		
		public String getName(){
			return name;
		}
		
		public String getValue(){
			return value;
		}
		
		public static Vector<MessageVariable> getMessageVariables(EventLogMessage logMessage){
			Vector<MessageVariable> vars = new Vector<MessageVariable>();
			
			for(EventLogField field : logMessage.getFields()){
				vars.add( new MessageVariable( "$" + field.getName().getSimpleNameFormat(), field.getValue() )  );
			}
			
			vars.add( new MessageVariable( "$EventType", logMessage.getEventType().getName()) );
			vars.add( new MessageVariable( "$EventTypeID", Integer.toString( logMessage.getEventType().ordinal()) ) );
			vars.add( new MessageVariable( "$event_type", logMessage.getEventType().getName()) );
			vars.add( new MessageVariable( "$event_type_id", Integer.toString( logMessage.getEventType().ordinal()) ) );
			
			vars.add( new MessageVariable( "$SeverityID", Integer.toString( logMessage.getSeverity().getSyslogEquivalent()) ) );
			vars.add( new MessageVariable( "$Severity", logMessage.getSeverity().toString() ) );
			vars.add( new MessageVariable( "$severity_id", Integer.toString( logMessage.getSeverity().getSyslogEquivalent()) ) );
			
			vars.add( new MessageVariable( "$Date", logMessage.getDate().toString() ) );
			
			vars.add( new MessageVariable( "$Message", logMessage.getMessageName() ) );
			return vars;
		}
		
		public static Vector<MessageVariable> getMessageVariablesFreemarker(EventLogMessage logMessage){
			Vector<MessageVariable> vars = new Vector<MessageVariable>();
			
			for(EventLogField field : logMessage.getFields()){
				vars.add( new MessageVariable( "${" + field.getName().getSimpleNameFormat() + "}", field.getValue() )  );
			}
			
			vars.add( new MessageVariable( "${EventType}", logMessage.getEventType().getName()) );
			vars.add( new MessageVariable( "${EventTypeID}", Integer.toString( logMessage.getEventType().ordinal()) ) );
			vars.add( new MessageVariable( "${event_type}", logMessage.getEventType().getName()) );
			vars.add( new MessageVariable( "${event_type_id}", Integer.toString( logMessage.getEventType().ordinal()) ) );
			
			vars.add( new MessageVariable( "${SeverityID}", Integer.toString( logMessage.getSeverity().getSyslogEquivalent()) ) );
			vars.add( new MessageVariable( "${Severity}", logMessage.getSeverity().toString() ) );
			vars.add( new MessageVariable( "${severity_id}", Integer.toString( logMessage.getSeverity().getSyslogEquivalent()) ) );

			vars.add( new MessageVariable( "${Date}", logMessage.getDate().toString() ) );
			vars.add( new MessageVariable( "${Message}", logMessage.getMessageName() ) );
			
			return vars;
		}
		
		public String processMessageTemplate( String templateStr, HashMap<String, Object> vars){
			StringReader reader = new StringReader(templateStr);
			Configuration cfg = new Configuration();
			cfg.setStrictSyntaxMode(false);
			cfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
			
			try {
				Template template = new Template("email_template", reader, cfg);
				StringWriter writer = new StringWriter();
				template.process(vars, writer);
				
				writer.flush();
				return writer.getBuffer().toString();
			} catch (IOException e) {
				Application.getApplication().logExceptionEvent(new EventLogMessage(EventType.RESPONSE_ACTION_FAILED), e);
			} catch (TemplateException e) {
				Application.getApplication().logExceptionEvent(new EventLogMessage(EventType.RESPONSE_ACTION_FAILED), e);
			}
			
			return null;
		}
		
		public static String processMessageTemplate( String template, Vector<MessageVariable> vars){
			
			// 1 -- Identify each variable in the text
			Pattern pattern = Pattern.compile(ARGUMENT_NAME, Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(template);
			Vector<String> varsWithinTemplate = new Vector<String>();
			
			while( matcher.find() ){
				varsWithinTemplate.add( matcher.group(0) );
			}
			
			
			// 2 -- Find the variable in the list and replace it
			for( int c = 0; c < varsWithinTemplate.size(); c++ ){
				String value = null;
				
				// 2.1 -- Identify the variable that matches the name
				for( int d = 0; d < vars.size(); d++ ){
					if( StringUtils.equalsIgnoreCase( vars.get(d).getName(), varsWithinTemplate.get(c)) ){
						value = vars.get(d).getValue();
					}
				}
				
				// 2.2 -- If the match was found, then do the replacement
				if( value != null ){
					template = StringUtils.replace(template, varsWithinTemplate.get(c), value);
				}
				
				// 2.3 -- If the match was not found, then substitute in text that notes that the variable was not found 
				else{
					template = StringUtils.replace(template, varsWithinTemplate.get(c), "[" + varsWithinTemplate.get(c).substring(1) + " was undefined]");
				}
				
			}
			
			
			// 3 -- Return the result
			return template;
		}
	}
	
	protected String description;
	protected String extendedDescription;
	private int actionID = -1;
	
	public abstract void execute(EventLogMessage logMessage) throws ActionFailedException;
	
	public abstract void execute(ScanResult scanResult) throws ActionFailedException;
	
	protected String getMessage(String templateString, ScanResult scanResult) throws ActionFailedException{
		try{
			Configuration cfg = new Configuration();
			cfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
			
			StringReader reader = new StringReader(templateString);
			Template template = new Template( "default_template", reader, cfg);
			
			HashMap<String, Object> data = new HashMap<String, Object>();
			data.put("scan_result", scanResult);
			
			StringWriter writer = new StringWriter();
			template.process(data, writer);
			
			return writer.toString();
		}
		catch( IOException e ){
			throw new ActionFailedException("Exception generated while rendering message field", e);
		} catch (TemplateException e) {
			throw new ActionFailedException("Exception generated while rendering message field", e);
		}
	}
	
	protected Action(String description, String extenededDescription){
		
		// 0 -- Precondition check
		if( description == null ){
			throw new IllegalArgumentException("The description of the action cannot be null");
		}
		
		
		// 1 -- Initialize the class
		this.description = description;
		this.extendedDescription = extenededDescription;
	}
	
	public String getUserLongDescription(){
		return extendedDescription;
	}
	
	public String getDescription(){
		return description;
	}
	
	public int getActionID(){
		return actionID;
	}
	
	public abstract Hashtable<String, String> getValues();
	
	public abstract FieldLayout getLayoutWithValues();
	
	public void configure( Hashtable<String, String> arguments ) throws ArgumentFieldsInvalidException{
		
		// 1 -- Get a list of the fields that must be configured
		FieldLayout layout = getLayoutWithValues();
		PrototypeField[] fields = layout.getFields();
		
		// 2 -- Loop through each field and validate it, then set it
		for(PrototypeField field : fields){
			String value = arguments.get(field.getName());
			
			// 2.1 -- Stop if the field was not included
			if( value == null ){
				throw new ArgumentFieldsInvalidException("The " + field.getName() + " field was not provided", field);
			}
			
			// 2.2 -- If the field was included, then make sure it is valid
			else{
				FieldValidatorResult result = field.validate(value);
				
				if( result.validated() == false ){
					if( result.getMessage() == null ){
						throw new ArgumentFieldsInvalidException("The " + field.getName() + " field is invalid", field);
					}
					else{
						throw new ArgumentFieldsInvalidException(result.getMessage(), field);
					}
				}
				
				// 2.3 -- Store the value since it validated
				setField(field.getName(), value);
			}
		}
	}
	
	/**
	 * Set the field that is associated with the given name.
	 * @param name
	 * @param value
	 */
	protected abstract void setField(String name, String value);
	
	public void save() throws NoDatabaseConnectionException, SQLException, IOException{
		
		// 1 -- Get the serialized stream from the object
		byte[] bytes = null;
		ByteArrayOutputStream byteOutStream = null; 
		ObjectOutputStream outStream = null;
		IOException cause = null;
		
		try{
			byteOutStream = new ByteArrayOutputStream();
			outStream = new ObjectOutputStream(byteOutStream);
			
			outStream.writeObject(this);
			bytes = byteOutStream.toByteArray();
			
		}
		catch(IOException e){
			cause = e;
		}
		finally{
			
			try{
				if( byteOutStream != null ){
					byteOutStream.close();
				}
				
				if( outStream != null ){
					outStream.close();
				}
			}
			catch(IOException e){
				if( e.getCause() == null && cause != null ){
					e.initCause(cause);
					throw e;
				}
				else if( e.getCause() != null && cause != null){
					throw cause;
				}
				else{
					throw e;
				}
			}
			
		}
		
		
		// 2 -- Write the bytes to the database
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet keys = null;
		
		try{
			connection = Application.getApplication().getDatabaseConnection(DatabaseAccessType.ACTION);
			
			// If the action identifier is greater or equal to zero, then the action was previously saved to the database and the existing item should be updated 
			if( actionID >= 0){
				statement = connection.prepareStatement("Update Action set State = ? where ActionID = ?");
				
				statement.setBytes(1, bytes);
				statement.setInt(2, actionID);
				statement.executeUpdate();
			}
			else{
				statement = connection.prepareStatement("Insert into Action (State) values (?)", PreparedStatement.RETURN_GENERATED_KEYS);
				statement.setBytes(1, bytes);
				statement.executeUpdate();
				
				keys = statement.getGeneratedKeys();
				
				if( keys.next() ){
					actionID = keys.getInt(1);
				}
			}
		}
		finally{
			if( connection != null ){
				connection.close();
			}
			
			if( statement != null ){
				statement.close();
			}
			
			if( keys != null ){
				keys.close();
			}
		}
		
	}
	
	public static Action loadFromDatabase( Connection connection, int actionID ) throws SQLException, ActionInstantiationException{
		
		// 0 -- Precondition check
		if( connection == null ){
			throw new IllegalArgumentException("The database connection cannot be null");
		}
		
		
		// 1 -- Load the parameters
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try{
			// 1.1 -- Setup and perform the database query
			statement = connection.prepareStatement("Select * from Action where ActionID = ?");
			statement.setInt(1, actionID);
			result = statement.executeQuery();
			
			// 1.2 -- Load the action object from the result (if the connection succeeded)
			if( result.next() ){
				
				byte[] bytes = result.getBytes("State");
				
				ByteArrayInputStream byteInStream = new ByteArrayInputStream(bytes);
				ObjectInputStream inStream = new ObjectInputStream(byteInStream);
				
				Action action = (Action)inStream.readObject();
				
				return action;
			}
		}
		catch(ClassNotFoundException e){
			throw new ActionInstantiationException(e);
		}
		catch(IOException e){
			throw new ActionInstantiationException(e);
		}
		finally{
			
			if( statement != null ){
				statement.close();
			}
			
			if( result != null ){
				result.close();
			}
		}
		
		return null; //The loading of the object failed
		
	}
	
	public abstract String getConfigDescription();
}

