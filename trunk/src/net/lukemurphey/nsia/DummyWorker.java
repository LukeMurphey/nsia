package net.lukemurphey.nsia;

/**
 * The dummy worker does nothing but pauses for a given number of seconds. This is used for testing the application but should not be used in production.
 * @author Luke
 *
 */
public class DummyWorker implements WorkerThread {

	private int progress = 0;
	private State status = State.STOPPED; 
	private int delays_secs = 20;
	private boolean terminate = false;
	private String taskDesc = "Dummy Worker";
	
	public DummyWorker( String taskDesc, int delays_secs ){
		this.delays_secs = delays_secs;
		this.taskDesc = taskDesc;
	}
	
	public DummyWorker( ){ }
	
	public boolean canPause() {
		return false;
	}

	public Throwable getException() {
		return null;
	}

	public int getProgress() {
		return progress;
	}

	public State getStatus() {
		return status;
	}

	public String getStatusDescription() {
		return "Waiting for a total of " + delays_secs + " seconds; currently " + getProgress() + "% done.";
	}

	public String getTaskDescription() {
		return taskDesc;
	}

	public void pause() {
		//Does not support pausing
	}

	public boolean reportsProgress() {
		return true;
	}

	public void terminate() {
		terminate = true;
	}

	public void run() {
		status = State.STARTED;
		int waitedSecs = 0;
		terminate = false;
		
		while( terminate == false && waitedSecs < delays_secs ){
			try {
				Thread.sleep(1000);
				waitedSecs = waitedSecs + 1;
				progress =  ((100*waitedSecs) / delays_secs );
			} catch (InterruptedException e) {
				terminate = true;
			}
		}
		
		status = State.STOPPED;
	}

}
