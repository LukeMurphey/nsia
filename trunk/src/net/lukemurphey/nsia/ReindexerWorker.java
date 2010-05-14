package net.lukemurphey.nsia;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.Iterator;
import java.util.Vector;



import net.lukemurphey.nsia.Application.DatabaseAccessType;
import net.lukemurphey.nsia.DerbyDatabaseInitializer.DatabaseIndex;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;

public class ReindexerWorker implements WorkerThread {

	private boolean shutdown = false;
	private int total = -1;
	private int currentProgress = -1;
	private State state = State.INITIALIZED;
	private Exception exceptionThrown = null;
	
	public boolean canPause() {
		return false;
	}

	public int getProgress() {
		if( state == State.STARTED){
			return (currentProgress * 100) / total;
		}
		else{
			return 0;
		}
	}

	public State getStatus() {
		return state;
	}

	public String getStatusDescription() {
		if( state == State.INITIALIZED ){
			return "Ready";
		}
		else if( state == State.STARTING ){
			return "Preparing to reindex";
		}
		else if( state == State.STARTED ){
			return "Defragmenting indexes (" + currentProgress + " of " + total + ")";
		}
		else{
			return "" + currentProgress + " of " + total + " indexes defragmented";
		}
	}

	public String getTaskDescription() {
		return "Database Index Defragmenter";
	}

	public void pause() {
		//This task cannot be paused
	}

	public boolean reportsProgress() {
		return true;
	}

	public void terminate() {
		shutdown = true;
	}

	public void run(){
		state = State.STARTING;
		Connection connection = null;
		
		try{
			
			connection = Application.getApplication().getDatabaseConnection(DatabaseAccessType.ADMIN);
			DerbyDatabaseInitializer initializer = new DerbyDatabaseInitializer(connection);
			
			Vector<DatabaseIndex> indexers = initializer.getDatabaseIndexManagers();
			total = indexers.size();
			currentProgress = 0;
			
			state = State.STARTED;
			
			Iterator<DatabaseIndex> iterator = indexers.iterator();
			
			while(iterator.hasNext()){
				
				// Stop if shutdown was requested
				if(shutdown == true ){
					EventLogMessage message = new EventLogMessage(EventLogMessage.EventType.TASK_TERMINATED);
					message.addField(new EventLogField(EventLogField.FieldName.TASK, "Database Reindexer"));
					Application.getApplication().logEvent(message);
				}
				
				// Get the list of indexers
				DatabaseIndex indexer = iterator.next();
				
				// Drop and re-create each index
				try{
					indexer.create(true);
					currentProgress++;
				}
				catch(SQLException e){
					exceptionThrown = e;
					
					EventLogMessage message = new EventLogMessage(EventLogMessage.EventType.TASK_FAILED);
					message.addField(new EventLogField(EventLogField.FieldName.TASK, "Database Reindexer"));
					message.addField(new EventLogField(EventLogField.FieldName.MESSAGE, "Exception thrown while defragmenting index " + indexer.getIndexName() ));
					Application.getApplication().logExceptionEvent(message, e);
				}
			}
			
		}
		catch(NoDatabaseConnectionException e){
			exceptionThrown = e;
			
			EventLogMessage message = new EventLogMessage(EventLogMessage.EventType.TASK_FAILED);
			message.addField(new EventLogField(EventLogField.FieldName.TASK, "Database Reindexer"));
			message.addField(new EventLogField(EventLogField.FieldName.MESSAGE, "Exception thrown while attempting to get database connection in order to defragement indexes; indexer will exit"));
			Application.getApplication().logExceptionEvent(message, e);
		}
		finally{
			
			try{
				if( connection != null ){
					connection.close();
				}
			}
			catch(SQLException e){
				
				EventLogMessage message = new EventLogMessage(EventLogMessage.EventType.INTERNAL_ERROR);
				message.addField(new EventLogField(EventLogField.FieldName.TASK, "Database Reindexer"));
				message.addField(new EventLogField(EventLogField.FieldName.MESSAGE, "Exception thrown while attempting to close database connection"));
				Application.getApplication().logExceptionEvent(message, e);
			}
		}
		
		// Send a log message indicating that the indexes were defragmented
		EventLogMessage message = new EventLogMessage(EventLogMessage.EventType.TASK_COMPLETED);
		message.addField(new EventLogField(EventLogField.FieldName.TASK, "Database Reindexer"));
		message.addField(new EventLogField(EventLogField.FieldName.MESSAGE, "" + currentProgress + " of " + total + " indexes defragmented" ));
		Application.getApplication().logEvent(message);
		
		state = State.STOPPED;
	}

	public Throwable getException() {
		return exceptionThrown;
	}

}
