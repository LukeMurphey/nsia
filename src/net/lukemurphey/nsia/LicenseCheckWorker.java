package net.lukemurphey.nsia;

import java.sql.SQLException;

import net.lukemurphey.nsia.eventlog.EventLogMessage;

public class LicenseCheckWorker implements WorkerThread{

	private Exception exceptionThrown = null;
	private State state = State.INITIALIZED;
	
	@Override
	public boolean canPause() {
		return false;
	}

	@Override
	public Throwable getException() {
		return exceptionThrown;
	}

	@Override
	public int getProgress() {
		return -1;
	}

	@Override
	public State getStatus() {
		return state;
	}

	@Override
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

	@Override
	public String getTaskDescription() {
		return "License Validator";
	}

	@Override
	public void pause() {
		// This thread cannot be paused
	}

	@Override
	public boolean reportsProgress() {
		return false;
	}

	@Override
	public void terminate() {
		// This thread cannot be terminated
	}

	@Override
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
