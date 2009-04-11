package net.lukeMurphey.nsia;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.Iterator;
import java.util.Vector;

import net.lukeMurphey.nsia.Application.DatabaseAccessType;

import net.lukeMurphey.nsia.DerbyDatabaseInitializer.DatabaseIndex;

import net.lukeMurphey.nsia.eventLog.EventLogField;
import net.lukeMurphey.nsia.eventLog.EventLogMessage;

public class ReindexerWorker implements WorkerThread {

	private boolean shutdown = false;
	private int total = -1;
	private int currentProgress = -1;
	private State state = State.INITIALIZED;
	private Exception exceptionThrown = null;
	
	@Override
	public boolean canPause() {
		return false;
	}

	@Override
	public int getProgress() {
		if( state == State.STARTED){
			return (currentProgress * 100) / total;
		}
		else{
			return 0;
		}
	}

	@Override
	public State getStatus() {
		return state;
	}

	@Override
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

	@Override
	public String getTaskDescription() {
		return "Database Index Defragmenter";
	}

	@Override
	public void pause() {
		//This task cannot be paused
	}

	@Override
	public boolean reportsProgress() {
		return true;
	}

	@Override
	public void terminate() {
		shutdown = true;
	}

	@Override
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
					EventLogMessage message = new EventLogMessage(EventLogMessage.Category.TASK_TERMINATED);
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
					
					EventLogMessage message = new EventLogMessage(EventLogMessage.Category.TASK_FAILED);
					message.addField(new EventLogField(EventLogField.FieldName.TASK, "Database Reindexer"));
					message.addField(new EventLogField(EventLogField.FieldName.MESSAGE, "Exception thrown while defragmenting index " + indexer.getIndexName() ));
					Application.getApplication().logExceptionEvent(message, e);
				}
			}
			
		}
		catch(NoDatabaseConnectionException e){
			exceptionThrown = e;
			
			EventLogMessage message = new EventLogMessage(EventLogMessage.Category.TASK_FAILED);
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
				
				EventLogMessage message = new EventLogMessage(EventLogMessage.Category.INTERNAL_ERROR);
				message.addField(new EventLogField(EventLogField.FieldName.TASK, "Database Reindexer"));
				message.addField(new EventLogField(EventLogField.FieldName.MESSAGE, "Exception thrown while attempting to close database connection"));
				Application.getApplication().logExceptionEvent(message, e);
			}
		}
		
		// Send a log message indicating that the indexes were defragmented
		EventLogMessage message = new EventLogMessage(EventLogMessage.Category.TASK_COMPLETED);
		message.addField(new EventLogField(EventLogField.FieldName.TASK, "Database Reindexer"));
		message.addField(new EventLogField(EventLogField.FieldName.MESSAGE, "" + currentProgress + " of " + total + " indexes defragmented" ));
		Application.getApplication().logEvent(message);
		
		state = State.STOPPED;
	}

	@Override
	public Throwable getException() {
		return exceptionThrown;
	}

}
