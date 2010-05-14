package net.lukemurphey.nsia.eventlog;

import java.util.Vector;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.eventlog.EventLogMessage.EventType;
import net.lukemurphey.nsia.response.Action;
import net.lukemurphey.nsia.response.ActionFailedException;

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
			if( message.getEventType() == EventType.RULE_REJECTED || message.getEventType() == EventType.RULE_FAILED ){
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
	
	/**
	 * Retrieves all of the rule status event log hooks for the rule with the given ID.
	 * @param app
	 * @param ruleID
	 * @return
	 */
	public static RuleStatusEventLogHook[] getRuleStatusEventLogHooks(Application app, long ruleID){
		
		// 1 -- Get the list of hooks
		EventLogHook[] allHooks = app.getEventLog().getHooks();
		
		Vector<RuleStatusEventLogHook> ruleHooks = new Vector<RuleStatusEventLogHook>();
		
		for(int c = 0; c < allHooks.length; c++){
			if( allHooks[c] instanceof RuleStatusEventLogHook && ((RuleStatusEventLogHook)allHooks[c]).getRuleID() == ruleID ){
				ruleHooks.add( (RuleStatusEventLogHook) allHooks[c] );
			}
		}
		
		RuleStatusEventLogHook[] hooksArray = new RuleStatusEventLogHook[ruleHooks.size()];
		ruleHooks.toArray(hooksArray);
		return hooksArray;
	}

}
