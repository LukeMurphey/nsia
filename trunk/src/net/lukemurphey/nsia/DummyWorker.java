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
	
	@Override
	public boolean canPause() {
		return false;
	}

	@Override
	public Throwable getException() {
		return null;
	}

	@Override
	public int getProgress() {
		return progress;
	}

	@Override
	public State getStatus() {
		return status;
	}

	@Override
	public String getStatusDescription() {
		return "Waiting for a total of " + delays_secs + " seconds; currently " + getProgress() + "% done.";
	}

	@Override
	public String getTaskDescription() {
		return taskDesc;
	}

	@Override
	public void pause() {
		//Does not support pausing
	}

	@Override
	public boolean reportsProgress() {
		return true;
	}

	@Override
	public void terminate() {
		terminate = true;
	}

	@Override
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
