package net.lukeMurphey.nsia;

/**
 * This class represents a thread and provides a series of methods intended to manage and view the
 * status of a thread. 
 * @author luke
 *
 */
public interface WorkerThread extends Runnable{
	
	public enum State{
		INITIALIZED,
		STOPPED,
		STOPPING,
		STARTED,
		STARTING,
		PAUSED,
		PAUSING
	}
	
	/**
	 * Return the current status if the thread.
	 * @return
	 */
	public abstract State getStatus();
	
	/**
	 * Determine the amount complete thus far.
	 * @return
	 */
	public abstract int getProgress();
	
	/**
	 * Get a description of the current state of the thread.
	 * @return
	 */
	public abstract String getStatusDescription();
	
	/**
	 * Get a description of the task.
	 * @return
	 */
	public abstract String getTaskDescription();
	
	/**
	 * Determine if the thread reports it's progress.
	 * @return
	 */
	public abstract boolean reportsProgress();
	
	/**
	 * Pauses the current thread (if supported).
	 *
	 */
	public abstract void pause();
	
	/**
	 * Determines if the thread can be paused.
	 * @return
	 */
	public abstract boolean canPause();
	
	/**
	 * Terminates the current thread.
	 *
	 */
	public abstract void terminate();
	
	/**
	 * Returns any exceptions thrown by the worker during it's operation. Returns null if no exceptions occurred.
	 * @return
	 */
	public abstract Throwable getException();
	
}
