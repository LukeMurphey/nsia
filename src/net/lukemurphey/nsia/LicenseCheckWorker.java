package net.lukemurphey.nsia;

import java.sql.SQLException;

import net.lukemurphey.nsia.eventlog.EventLogMessage;

public class LicenseCheckWorker implements WorkerThread{

	private Exception exceptionThrown = null;
	private State state = State.INITIALIZED;
	
	public boolean canPause() {
		return false;
	}
	
	public Throwable getException() {
		return exceptionThrown;
	}

	public int getProgress() {
		return -1;
	}

	public State getStatus() {
		return state;
	}

	public String getStatusDescription() {
		if( state == State.STARTED ){
			return "Retrieving license information";
		}
		else if( state == State.STOPPED ){
			return "License retrieval complete";
		}
		else{
			return "Not running";
		}
	}

	public String getTaskDescription() {
		return "License Validator";
	}

	public void pause() {
		// This thread cannot be paused
	}

	public boolean reportsProgress() {
		return false;
	}

	public void terminate() {
		// This thread cannot be terminated
	}

	public void run() {
		Application appRes = Application.getApplication();
		state = State.STARTED;
		try{
			appRes.getApplicationConfiguration().getLicense();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
			exceptionThrown = e;
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			exceptionThrown = e;
		} catch (InputValidationException e) {
			exceptionThrown = e;
		}
		
		state = State.STOPPED;
	}

}
