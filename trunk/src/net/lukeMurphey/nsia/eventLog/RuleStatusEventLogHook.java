package net.lukeMurphey.nsia.eventLog;

import net.lukeMurphey.nsia.eventLog.EventLogMessage.Category;
import net.lukeMurphey.nsia.responseModule.Action;
import net.lukeMurphey.nsia.responseModule.ActionFailedException;

public class RuleStatusEventLogHook extends EventLogHook {

	private static final long serialVersionUID = 1L;
	private Action action;
	private String ruleIDString;
	private long ruleID;
	private EventLogSeverity minimumSeverity;
	
	public RuleStatusEventLogHook(Action action, long ruleID, EventLogSeverity minimumSeverity){
		
		// 0 -- Precondition check
		if( action == null ){
			throw new IllegalArgumentException("The action to perform cannot be null");
		}
		
		if( minimumSeverity == null ){
			throw new IllegalArgumentException("The minimum severity level cannot be null");
		}
		
		if( ruleID < 0 ){
			throw new IllegalArgumentException("The identifier of the scan rule must be greater than zero");
		}
		
		// 1 -- Initialize the class
		this.action = action;
		this.minimumSeverity = minimumSeverity;
		this.ruleIDString = Long.toString(ruleID);
		this.ruleID = ruleID;
	}
	
	public long getRuleID(){
		return ruleID;
	}
	
	@Override
	public void processEvent(EventLogMessage message) throws EventLogHookException {
		// 0 -- Precondition check
		if( message == null ){
			return;
		}
		
		// 1 -- Stop if the severity is below the threshold
		if( message.getSeverity().toInt() < minimumSeverity.toInt() ){
			return;
		}
		
		try{
			
			// 2 -- Perform the action if the event is for a rule status change and the identifier matches
			if( message.getCategory() == Category.RULE_REJECTED || message.getCategory() == Category.RULE_FAILED ){
				EventLogField field = message.getField(EventLogField.FieldName.RULE_ID);
				
				if( field != null && field.getDescription().equals( ruleIDString ) ){
					//Perform action
					action.execute(message);
				}
			}
		}
		catch(ActionFailedException e){
			throw new EventLogHookException(e);
		}
	}
	
	public Action getAction(){
		return action;
	}

}
