package net.lukemurphey.nsia;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipOutputStream;

import net.lukemurphey.nsia.Application.DatabaseAccessType;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.eventlog.EventLogMessage.EventType;


public class DatabaseBackup implements WorkerThread{

	//private int startTime = -1;
	private State state = State.INITIALIZED;
	private Exception exceptionThrown = null;
	private boolean stop = false;
	private Phase phase = null;
	private String BACKUP_LOCATION = "../var/backups/";
	
	private enum Phase{
		INITIALIZING, EXPORTING_DATABASE, COMPRESSING_ARCHIVE, CLEANING_UP, DONE
	}
	
	public boolean canPause() {
		return false;
	}

	public int getProgress() {
		if( state == State.STOPPED ){
			return 100;
		}
		else if( state != State.STARTED && state != State.STOPPING ){
			return 0;
		}
		else if(phase == null || phase == Phase.INITIALIZING){
			return 0;
		}
		else if(phase == Phase.EXPORTING_DATABASE){
			return 25;
		}
		else if(phase == Phase.COMPRESSING_ARCHIVE){
			return 50;
		}
		else if(phase == Phase.CLEANING_UP){
			return 75;
		}
		else if(phase == Phase.DONE){
			return 100;
		}
		
		else return 0;
		
		//return GenericUtils.twoStateProgress(6000, startTime);
	}

	public State getStatus() {
		return state;
	}

	public String getStatusDescription() {
		
		if( state == State.STOPPED ){
			return "Backup complete";
		}
		else if(phase == null || phase == Phase.INITIALIZING){
			return "Backup process initializing";
		}
		else if(phase == Phase.EXPORTING_DATABASE){
			return "Exporting database to temporary location";
		}
		else if(phase == Phase.COMPRESSING_ARCHIVE){
			return "Compressing database backup archive";
		}
		else if(phase == Phase.CLEANING_UP){
			return "Cleaning up temporary files";
		}
		else if(phase == Phase.DONE){
			return "Backup complete";
		}
		
		else return "Backup in process";
	}
	
	public void run(){
		
		FileOutputStream outputStream = null;
		ZipOutputStream zos  = null;
		String backupFile = null;
		
		try{
			
			//startTime = (int)System.currentTimeMillis();
			state = State.STARTED;
			phase = Phase.INITIALIZING;
			
			// 1 -- Create the filename
			Date date = new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH.mm.ss Z");
			String tempDir = System.getProperty("java.io.tmpdir");
			
			//	 1.1 -- Add the trailing slash if it doesn't have one already
			if ( !(tempDir.endsWith("/") || tempDir.endsWith("\\")) ){
			   tempDir = tempDir + System.getProperty("file.separator");
			}
			
			//	 1.2 -- Make the directory
			File temporaryDirectory = new File(tempDir + "backup_" + dateFormat.format( date ) );
			
			if( temporaryDirectory.mkdir() == false ){
				throw new IOException("Could not delete directory \"" + temporaryDirectory.getAbsolutePath() + "\"");
			}
			
			if( stop == true ){
				state = State.STOPPED;
				return;
			}
			
			// 2 -- Backup the database to a directory
			phase = Phase.EXPORTING_DATABASE;
			backupToDirectory(temporaryDirectory);
			
			
			if( stop == true ){
				state = State.STOPPED;
				return;
			}
			
			// 3 -- Zip the contents of the directory
			phase = Phase.COMPRESSING_ARCHIVE;
			
			//	 3.1 -- Create the backups directory if it does not exist
			File backupsDir = new File(BACKUP_LOCATION);
			if(backupsDir.exists() == false ){
				if( backupsDir.mkdir() == false ){
					throw new IOException("Could not create directory to store databaes backups");
				}
			}
			
			//	 3.2 -- Stream the database backup to the backups directory
			backupFile = BACKUP_LOCATION + "backup_" + dateFormat.format( date ) + ".zip";
			outputStream = new FileOutputStream( new File(backupFile) );
			zos = new ZipOutputStream(outputStream);
			GenericUtils.zipDir(temporaryDirectory, zos);

			if( stop == true ){
				state = State.STOPPED;
				return;
			}
			
			// 4 -- Delete the temporary directory
			phase = Phase.CLEANING_UP;
			
			GenericUtils.deleteDirectory(temporaryDirectory);
			state = State.STOPPED;
		} catch(IOException e){
			exceptionThrown = e;
		} catch (NoDatabaseConnectionException e) {
			exceptionThrown = e;
		} catch (SQLException e) {
			exceptionThrown = e;
		}
		finally{
			try{
				if( zos != null ){
					zos.close();
				}
				
				if( outputStream != null ){
					outputStream.close();
				}
				
			}
			catch(IOException e){
				if( exceptionThrown != null && exceptionThrown.getCause() == null ){
					e.initCause( exceptionThrown );
					exceptionThrown = e;
				}
				else if(exceptionThrown == null){
					exceptionThrown = e;
				}
			}
		}
		
		// 5 -- Log the result
		
		//	 5.1 -- The task encountered an error while executing
		if( exceptionThrown != null ){
			EventLogMessage message = new EventLogMessage(EventType.TASK_FAILED, new EventLogField(FieldName.TASK, "Database Backup") );
			message.addField(new EventLogField(FieldName.MESSAGE, "Exception thrown while backing up database"));
			Application.getApplication().logExceptionEvent(message, exceptionThrown);
		}
		
		//	 5.2 -- The task was stopped
		else if(stop == true){
			EventLogMessage message = new EventLogMessage(EventType.TASK_TERMINATED, new EventLogField(FieldName.TASK, "Database Backup") );
			Application.getApplication().logEvent(message);
		}
		
		//	 5.3 -- The task completed successfully
		else{
			EventLogMessage message = new EventLogMessage(EventType.TASK_COMPLETED, new EventLogField(FieldName.TASK, "Database Backup") );
			message.addField(new EventLogField(FieldName.MESSAGE, "Database backup successful"));
			message.addField(new EventLogField(FieldName.FILE, backupFile));
			Application.getApplication().logEvent(message);
		}
		
		state = State.STOPPED;
	}
	
	private void backupToDirectory(File directory) throws NoDatabaseConnectionException, SQLException{
		
		// 0 -- Precondition check
		Connection connection = null;
		CallableStatement statement = null;
		SQLException initCause = null;
		try{
			connection = Application.getApplication().getDatabaseConnection(DatabaseAccessType.ADMIN);
			
			statement = connection.prepareCall("CALL SYSCS_UTIL.SYSCS_BACKUP_DATABASE(?)");
			statement.setString(1, directory.getAbsolutePath());
			statement.execute();
		}
		catch(SQLException e){
			initCause = e;
		}
		finally{
			
			try{
				if( connection != null ){
					connection.close();
				}
				
				if( statement != null ){
					statement.close();
				}
			}
			catch(SQLException e){
				
				if( initCause == null ){
					throw e;
				}
				else if( e.getCause() == null ){
					e.initCause(initCause);
					throw e;
				}
				else if( e.getCause() != null ){
					throw initCause;
				}
			}
		}
	}
	
	public void pause() {
		//This thread cannot be paused.
	}

	public boolean reportsProgress() {
		return true;
	}

	public void terminate() {
		if( state == State.STARTED || state == State.STARTING ){
			state= State.STOPPING;
		}
		
		stop = true;
	}

	public String getTaskDescription() {
		return "Database Backup";
	}

	public Throwable getException() {
		return exceptionThrown;
	}

}
