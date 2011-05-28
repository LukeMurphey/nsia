package net.lukemurphey.nsia;

import java.util.Vector;
import java.sql.Connection;
import java.sql.SQLException;

import net.lukemurphey.nsia.Application.DatabaseAccessType;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.eventlog.EventLogMessage.EventType;
import net.lukemurphey.nsia.scan.ScanException;
import net.lukemurphey.nsia.scan.ScanResult;
import net.lukemurphey.nsia.scan.ScanRule;
import net.lukemurphey.nsia.scan.ScanRuleLoader;
import net.lukemurphey.nsia.scan.ScanRule.ScanRuleLoadFailureException;

public class RuleScanWorker implements WorkerThread{

	private Vector<Long> ruleIDsToScan = null;
	private Vector<ScanRule> instantiatedRulesToScan = null;
	private Vector<Long> scanResultIDs = new Vector<Long>();
	private int currentOffset = 0;
	private ScanRule ruleBeingScanned = null;
	private boolean terminate = false;
	private State state = State.INITIALIZED;
	private Throwable exceptionThrown = null;
	
	public RuleScanWorker( Vector<Long> rulesToScan ){
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure rules to scan is not null
		if( rulesToScan == null ){
			throw new IllegalArgumentException("The rules to scan cannot be null");
		}
		
		//	 0.2 -- Make sure rules to scan is not null
		if( rulesToScan.size() == 0 ){
			throw new IllegalArgumentException("The rules to scan cannot be zero");
		}
		
		
		// 1 -- Initialize the class
		this.ruleIDsToScan = rulesToScan;
	}
	
	public RuleScanWorker( ScanRule ruleToScan ){
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure rules to scan is not null
		if( ruleToScan == null ){
			throw new IllegalArgumentException("The rule to scan cannot be null");
		}
		
		// 1 -- Initialize the class
		this.instantiatedRulesToScan = new Vector<ScanRule>();
		instantiatedRulesToScan.add(ruleToScan);
	}
	
	public RuleScanWorker( ScanRule[] rulesToScan ){
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure rules to scan is not null
		if( rulesToScan == null ){
			throw new IllegalArgumentException("The rules to scan cannot be null");
		}
		
		//	 0.2 -- Make sure rules to scan is not null
		if( rulesToScan.length == 0 ){
			throw new IllegalArgumentException("The rules to scan cannot be zero");
		}
		
		
		// 1 -- Initialize the class
		this.instantiatedRulesToScan = new Vector<ScanRule>();
		
		for(ScanRule rule : rulesToScan){
			instantiatedRulesToScan.add(rule);
		}
	}
	
	public int numberOfRulesToScan(){
		int total = 0;
		
		if(instantiatedRulesToScan != null){
			total += instantiatedRulesToScan.size();
		}
		
		if(ruleIDsToScan != null){
			total += ruleIDsToScan.size();
		}
		
		return total;
	}
	
	public RuleScanWorker( long[] rules ){
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure rules to scan is not null
		if( rules == null ){
			throw new IllegalArgumentException("The rules to scan cannot be null");
		}
		
		//	 0.2 -- Make sure rules to scan is not null
		if( rules.length == 0 ){
			throw new IllegalArgumentException("The rules to scan cannot be zero");
		}
		
		
		// 1 -- Initialize the class
		this.ruleIDsToScan = new Vector<Long>();
		
		for(int c = 0; c < rules.length; c++){
			ruleIDsToScan.add( Long.valueOf(rules[c]) );
		}
	}
	
	public boolean canPause() {
		return false;
	}

	public int getProgress() {
		
		if( numberOfRulesToScan() == 1 ){
			if( ruleBeingScanned != null && ruleBeingScanned instanceof WorkerThread ){
				WorkerThread thread = (WorkerThread)ruleBeingScanned;
				return thread.getProgress();
			}
			else{
				return -1;
			}
		}
		else{
			int percentagePerRule = 0;
			percentagePerRule = 100 / numberOfRulesToScan();
			
			int percentageDone = currentOffset * percentagePerRule;
			
			if( ruleBeingScanned != null && ruleBeingScanned instanceof WorkerThread ){
				WorkerThread thread = (WorkerThread)ruleBeingScanned;
				percentageDone +=  (percentagePerRule * (thread.getProgress()))/100;
			}
			
			return percentageDone;
		}
	}

	public State getStatus() {
		return state;
	}

	public String getStatusDescription() {
		
		if( currentOffset > (numberOfRulesToScan() - 1) ){
			return "Scan complete";
		}
		else if( ruleBeingScanned != null && ruleBeingScanned instanceof WorkerThread ){
			WorkerThread thread = (WorkerThread)ruleBeingScanned;
			return "Scanning rule " + (currentOffset + 1) + " of " + numberOfRulesToScan() + " (" + thread.getStatusDescription() + ")";
		}
		else{
			
			return "Scanning rule " + (currentOffset + 1) + " of " + numberOfRulesToScan();
		}
	}

	public String getTaskDescription() {
		return "Web-Application Scan";
	}

	public void pause() {
		if( ruleBeingScanned != null && ruleBeingScanned instanceof WorkerThread ){
			WorkerThread thread = (WorkerThread)ruleBeingScanned;
			if( thread.canPause() ){
				state = State.PAUSING;
				thread.pause();
				state = State.PAUSED;
			}
		}
	}

	public boolean reportsProgress() {
		return true;
	}

	public void terminate() {
		state = State.STOPPING;
		terminate = true;
		
		if( ruleBeingScanned != null && ruleBeingScanned instanceof WorkerThread ){
			WorkerThread thread = (WorkerThread)ruleBeingScanned;
			thread.terminate();
		}
		
		state = State.STOPPED;
	}

	public void run() {
		exceptionThrown = null;
		state = State.STARTING;
		while( exceptionThrown == null && terminate == false && currentOffset < numberOfRulesToScan()){
			
			try{
				
				ScanRule rule;
				
				if( ruleIDsToScan != null ){
					rule = ScanRuleLoader.getScanRule(ruleIDsToScan.get(currentOffset).longValue());
				}
				else{
					rule = instantiatedRulesToScan.get(currentOffset);
				}
				
				state = State.STARTED;
				ruleBeingScanned = rule;
				
				ScanCallback callback = new ScanCallback(Application.getApplication());
				rule.setCallback(callback);
				
				ScanResult result = rule.doScan();
				
				Connection connection = Application.getApplication().getDatabaseConnection(DatabaseAccessType.SCANNER);
				
				if( ruleIDsToScan != null ){
					scanResultIDs.add( result.saveToDatabase( connection, ruleIDsToScan.get(currentOffset).longValue() ) );
				}
				else{
					scanResultIDs.add( result.saveToDatabase( connection, -1 ) );//TODO Need to replace with method that allows the scanRuleID to be null (to indicate it is not related to a scan rule)
				}
				
				if( connection != null) {
					connection.close();
				}
				currentOffset++;
			} catch(ScanRuleLoadFailureException e){
				Application.getApplication().logExceptionEvent( new EventLogMessage( EventType.TASK_FAILED, new EventLogField(FieldName.TASK, getStatusDescription()) ), e);
				exceptionThrown = e;
			} catch (ScanException e) {
				exceptionThrown = e;
				Application.getApplication().logExceptionEvent( new EventLogMessage( EventType.TASK_FAILED, new EventLogField(FieldName.TASK, getStatusDescription()) ), e);
			} catch (NotFoundException e) {
				exceptionThrown = e;
				Application.getApplication().logExceptionEvent( new EventLogMessage( EventType.TASK_FAILED, new EventLogField(FieldName.TASK, getStatusDescription()) ), e);
			} catch (NoDatabaseConnectionException e) {
				exceptionThrown = e;
				Application.getApplication().logExceptionEvent( new EventLogMessage( EventType.DATABASE_FAILURE, new EventLogField(FieldName.TASK, getStatusDescription()) ), e);
			} catch (SQLException e) {
				exceptionThrown = e;
				Application.getApplication().logExceptionEvent( new EventLogMessage( EventType.SQL_EXCEPTION, new EventLogField(FieldName.TASK, getStatusDescription()) ), e);
			}
		}
		
		state = State.STOPPED;
	}

	public long[] getScanResultIDs(){
		long[] result = new long[scanResultIDs.size()];
		
		/* Note that we must loop through the result in such a way that if a new result is added after the
		 * above statement that the number of entries that we put into the array never exceeds the maximum
		 * length (even if a new scan result ID was added to the vector after the time that the array
		 * was initialized)
		 */
		for(int c = 0; c < result.length; c++){
			result[c] = scanResultIDs.get(c);
		}
		
		return result;
		
	}
	
	public Throwable getException() {
		return exceptionThrown;
	}

}
