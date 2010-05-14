package net.lukemurphey.nsia.eventlog;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.Application.DatabaseAccessType;
import net.lukemurphey.nsia.response.Action;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The event log hook allows code to be executed after an event is received. This can be used for triggering actions associated with application events.
 * @author Luke Murphey
 *
 */
public abstract class EventLogHook implements Serializable{

	private static final long serialVersionUID = -3742174021147412985L;
	protected int eventlogHookID = -1;
	
	public abstract void processEvent( EventLogMessage message ) throws EventLogHookException;
	
	public abstract Action getAction();
	
	public int getEventLogHookID(){
		return eventlogHookID;
	}
	
	public static EventLogHook loadFromDatabase( Connection connection, int eventlogHookID ) throws SQLException{
		
		// 0 -- Precondition check
		if( connection == null ){
			throw new IllegalArgumentException("The database connection cannot be null");
		}
		
		
		// 1 -- Load the parameters
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try{
			// 1.1 -- Setup and perform the database query
			statement = connection.prepareStatement("Select * from EventLogHook where EventLogHookID = ?");
			statement.setInt(1, eventlogHookID);
			result = statement.executeQuery();
			
			if( result.next() ){
				
				EventLogHook eventlogHook = loadFromDatabase(result);
				
				return eventlogHook;
			}
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
	
	protected static EventLogHook loadFromDatabase( ResultSet result ) throws SQLException{
		
		// 0 -- Precondition check
		if( result == null ){
			throw new IllegalArgumentException("The result set to load from cannot be null");
		}
		
		int hookID = -1;
		
		try{
			// 1 -- Get the identifier for the event log hook
			hookID = result.getInt("EventLogHookID");
			
			// 2 -- Load bytes of the class to be instantiated
			//byte[] bytes = result.getBytes("State");
			Blob bytes = result.getBlob("State");
			
			if( bytes == null ){
				return null;
			}
			
			InputStream byteInStream = bytes.getBinaryStream();

			//ByteArrayInputStream byteInStream = new ByteArrayInputStream(bytes);
			ObjectInputStream inStream = new ObjectInputStream(byteInStream);
	
			// 3 -- Instantiate the class
			EventLogHook eventlogHook;
			
			try{
				eventlogHook = (EventLogHook)inStream.readObject();
			
				// 4 -- Assign the identifier
				eventlogHook.eventlogHookID = hookID;
				
				return eventlogHook;
			}
			catch(InvalidClassException e){
				//This occurs when a class was modified and is different from the version that provided the serialized data.
				EventLogMessage message = new EventLogMessage(EventLogMessage.EventType.INTERNAL_ERROR);
				message.addField(new EventLogField( EventLogField.FieldName.MESSAGE, "Unable to load the event log hook for ID \"" + hookID + "\"") );
				Application.getApplication().logExceptionEvent(message, e);
			}
		}
		catch(ClassNotFoundException e){
			//Note that class was not found
			e.printStackTrace();
			EventLogMessage message = new EventLogMessage(EventLogMessage.EventType.INTERNAL_ERROR);
			message.addField(new EventLogField( EventLogField.FieldName.MESSAGE, "Unable to instantiate the event log hook for ID \"" + hookID + "\"") );
			Application.getApplication().logExceptionEvent(message, e);
		}
		catch(IOException e){
			//Note that class was not loaded
			e.printStackTrace();
			EventLogMessage message = new EventLogMessage(EventLogMessage.EventType.INTERNAL_ERROR);
			message.addField(new EventLogField( EventLogField.FieldName.MESSAGE, "Unable to instantiate the event log hook for ID \"" + hookID + "\"") );
			Application.getApplication().logExceptionEvent(message, e);
		}

		return null;
	}
	
	/**
	 * Removes the database entry associated with the hook. After calling this method, the hook will no longer be represented in the database.
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public void delete() throws SQLException, NoDatabaseConnectionException{
		delete(this.eventlogHookID);
	}
	
	public static void delete( long hookID ) throws SQLException, NoDatabaseConnectionException{
		
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet result = null;
		Exception initCause = null;
		long actionID = -1;
		
		
		try{
			connection = Application.getApplication().getDatabaseConnection(DatabaseAccessType.ACTION);
			
			// 1 -- Identify the action ID of the associated action (that must also be deleted)
			preparedStatement = connection.prepareStatement("Select ActionID from EventLogHook where EventLogHookID = ?");
			preparedStatement.setLong(1, hookID);
			
			result = preparedStatement.executeQuery();
			
			//	 1.1 -- Get the identifier of the associated action
			if( result.next() ){
				actionID = result.getLong("ActionID");
			}
			
			//	 1.2 -- Close the result set and statement
			result.close();
			preparedStatement.close();
			
			
			// 2 -- Delete the action
			if( actionID >= 0 ){
				preparedStatement = connection.prepareStatement("Delete from Action where ActionID = ?");
				preparedStatement.setLong(1, actionID);
				preparedStatement.executeUpdate();
				
				preparedStatement.close();
				preparedStatement = null;
			}
			
			// 3 -- Delete the event log hook
			preparedStatement = connection.prepareStatement("Delete from EventLogHook where EventLogHookID = ?");
			preparedStatement.setLong(1, hookID);
			preparedStatement.executeUpdate();
			
		}
		catch(SQLException e){
			initCause = e;
		}
		finally{
			
			try{
				if( result != null ){
					result.close();
				}
				
				if( preparedStatement != null ){
					preparedStatement.close();
				}
				
				if( connection != null ){
					connection.close();
				}
			}
			catch(SQLException e){
				if( initCause != null && e.getCause() != null ){
					e.initCause(initCause);
					throw e;
				}
				
				throw e;
			}
		}
		
		
	}
	
	public void saveToDatabase() throws SQLException, IOException, NoDatabaseConnectionException{

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
			throw e;
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
				if( e.getCause() == null ){
					e.initCause(cause);
					throw e;
				}
				else if( cause != null ){
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
			if( eventlogHookID >= 0){
				statement = connection.prepareStatement("Update EventLogHook set State = ? where EventLogHookID = ?");
				
				statement.setBytes(1, bytes);
				statement.setInt(2, eventlogHookID);
				statement.executeUpdate();
			}
			else{
				statement = connection.prepareStatement("Insert into EventLogHook (State, ActionID) values (?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
				statement.setBytes(1, bytes);
				
				// Get the action and determine if it has an action ID. If it doesn't save it to the database (this assigns an action ID)
				Action action = getAction();
				
				if( action.getActionID() < 0 ){
					action.save();
				}
				
				statement.setInt(2, action.getActionID());
				statement.executeUpdate();
				
				keys = statement.getGeneratedKeys();
				
				if( keys.next() ){
					eventlogHookID = keys.getInt(1);
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
	
}
