package net.lukemurphey.nsia.scan;

import javax.script.Invocable;
import javax.script.ScriptException;

/**
 * This class used to call the terminate function on the script definitions if necessary
 * @author Luke
 *
 */
public class TerminatorThread extends Thread{
	
	//The object that contains the invocable functions
	private Invocable invocable;
	
	//Contains any exceptions generated when executing the function
	private Throwable e = null;
	
	//Indicates if the script declares a terminate function
	private boolean terminateExists = true;
	
	//Indicates if the thread is still running (completed it's terminate call)
	private boolean isRunning = false;
	
	//The mutual exclusion used to enforce thread safety and call notify on
	private Object mutex = null;
	
	public TerminatorThread(Invocable invocable, Object mutex){
		
		// 0 -- Precondition check
		if( mutex == null ){
			throw new IllegalArgumentException("The mutex cannot be null");
		}
		
		// 1 -- Initialize the class
		this.invocable = invocable;
		this.mutex = mutex;
	}
	
	@Override
	public void run(){

		try{
			isRunning = true;
			invocable.invokeFunction("terminate");
			terminateExists = true;
		} catch (NoSuchMethodException e) {
			terminateExists = false;
		} catch(ScriptException e){
			this.e = e;
		} catch (Exception e) {
			this.e = e;
		} catch (Throwable e) {
			this.e = e;
		}
		finally{
			isRunning = false;

			synchronized (mutex) {
				mutex.notifyAll();
			}
		}
	}
	
	/**
	 * Indicates if the thread is still running.
	 * @return
	 */
	public boolean isRunning(){
		return isRunning;
	}
	
	/**
	 * Get any exceptions thrown by the invocable function.
	 * @return
	 */
	public Throwable getThrowable(){
		return e;
	}
	
	/**
	 * Indicates if the invocable declares a terminate function.
	 * @return
	 */
	public boolean declaresTerminate(){
		return terminateExists;
	}
}