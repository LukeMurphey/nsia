package net.lukemurphey.nsia.scan;

import javax.script.Invocable;
import javax.script.ScriptException;

import net.lukemurphey.nsia.scan.scriptenvironment.Result;
import net.lukemurphey.nsia.scan.scriptenvironment.Variables;

/**
 * This thread calls the ThreatScript using a new thread (so it can be terminated).
 * @author Luke
 *
 */
public class InvokerThread extends Thread{
	
	//The object that contains the invocable functions
	private Invocable invocable;
	
	//The result object from the call
	private Result result = null;
	
	//The HTTP response data that was passed to the thread
	private HttpResponseData httpResponse;
	
	//The variables that were passed to the thread
	private Variables variables;
	
	//The environment that was passed to the thread
	private Environment env;
	
	//Contains any exceptions generated when executing the function
	private Throwable e = null;
	
	//Indicates if the thread is running
	private boolean isRunning = false;
	
	//Mutex used to prevent multi-thread access to the invocable function 
	private Object mutex = null;
	
	public InvokerThread(Invocable invocable, HttpResponseData httpResponse, Variables variables, Environment env, Object mutex){
		
		// 0 -- Precondition check
		if( mutex == null ){
			throw new IllegalArgumentException("The mutex cannot be null");
		}
		
		// 1 -- Initialize the class
		this.invocable = invocable;
		this.httpResponse = httpResponse;
		this.variables = variables;
		this.env = env;
		this.mutex = mutex;
	}
	
	@Override
	public void run(){
		
		isRunning = true;
		
		try{
			result = (Result)invocable.invokeFunction("analyze", httpResponse, variables, env );
		} catch(ScriptException e){
			this.e = e;
		} catch (NoSuchMethodException e) {
			this.e = e;
		} catch (Exception e) {
			this.e = e;
		} catch (Throwable e) {
			this.e = e;
		}
		
		isRunning = false;
		
		synchronized(mutex){
			mutex.notifyAll();
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
	 * Gets the result from the invocable function.
	 * @return
	 */
	public Result getResult(){
		return result;
	}
	
	/**
	 * Get any exceptions thrown by the invocable function.
	 * @return
	 */
	public Throwable getThrowable(){
		return e;
	}
}