package net.lukemurphey.nsia;

import net.lukemurphey.nsia.Application.ShutdownRequestSource;

public class DelayedShutdown implements WorkerThread{

	private int shutdownSeconds = 60;
	private long startSeconds = -1;
	private boolean running = false;
	private boolean terminationRequested = false;
	private Application application;
	private ShutdownRequestSource source;
	//private Exception exceptionThrown = null;
	
	public DelayedShutdown( int secondsUntilShutdown, Application application, ShutdownRequestSource source ){
		
		// 0 -- Precondition check
		if( application == null ){
			throw new IllegalArgumentException("Application cannot be null");
		}
		
		// 1 -- Set the parameters
		shutdownSeconds = secondsUntilShutdown;
		this.application = application;
		this.source = source;
		application.getNetworkManager().stopListener();
	}
	
	public boolean canPause() {
		return false;
	}

	public int getProgress() {
		int percentage = (int)(System.currentTimeMillis() - startSeconds) / (shutdownSeconds * 1000);
		
		if( percentage > 100 ){
			return 100;
		}
		else{
			return percentage;
		}
	}

	public State getStatus() {
		if( terminationRequested && running ){
			return State.STOPPING;
		}
		else if( !terminationRequested && running ){
			return State.STARTED;
		}
		else{
			return State.STOPPED;
		}
	}

	public String getStatusDescription() {
		return "System is shutting down in ";
	}

	public String getTaskDescription() {
		return "System Shutdown";
	}

	public void pause() {
		// Ignore, this object cannot be paused
	}

	public boolean reportsProgress() {
		return true;
	}

	public void terminate() {
		terminationRequested = true;
	}

	public void run() {
		running = true;
		terminationRequested = false;
		
		startSeconds = System.currentTimeMillis();
		
		try{
			while( System.currentTimeMillis() < (startSeconds + (shutdownSeconds * 1000)) && terminationRequested == false){
				Thread.sleep(100);
			}
		}
		catch(InterruptedException e){
			//Someone has woke this thread because they are aborting a shutdown
		}
		
		if( !terminationRequested ){
			application.shutdown(source);
		}
		
		running = false;
	}

	public Throwable getException() {
		//return exceptionThrown;
		return null;
	}

}
