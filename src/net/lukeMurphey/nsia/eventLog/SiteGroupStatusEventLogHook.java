package net.lukeMurphey.nsia.eventLog;

import net.lukeMurphey.nsia.eventLog.EventLogMessage.Category;
import net.lukeMurphey.nsia.responseModule.Action;
import net.lukeMurphey.nsia.responseModule.ActionFailedException;

public class SiteGroupStatusEventLogHook extends EventLogHook {

	private static final long serialVersionUID = 1L;
	private Action action;
	private String siteGroupID;
	private int siteGroupIDInt;
	private EventLogSeverity minimumSeverity;
	
	public SiteGroupStatusEventLogHook(Action action, int siteGroupID, EventLogSeverity minimumSeverity){
		
		// 0 -- Precondition check
		if( action == null ){
			throw new IllegalArgumentException("The action to perform cannot be null");
		}
		
		if( minimumSeverity == null ){
			throw new IllegalArgumentException("The minimum severity level cannot be null");
		}
		
		if( siteGroupID < 0 ){
			throw new IllegalArgumentException("The identifier of the site group must be greater than zero");
		}
		
		// 1 -- Initialize the class
		this.action = action;
		this.minimumSeverity = minimumSeverity;
		this.siteGroupID = Long.toString(siteGroupID);
		this.siteGroupIDInt = siteGroupID;
	}
	
	public int getSiteGroupID(){
		return siteGroupIDInt;
	}
	
	@Override
	public void processEvent( EventLogMessage message) throws EventLogHookException {
		
		// 0 -- Precondition check
		if( message == null ){
			return;
		}
		
		// 1 -- Stop if the severity is below the threshold
		if( message.getSeverity().toInt() < minimumSeverity.toInt() ){
			return;
		}
		
		try{
			// 2 -- Stop if the scope of the event is a SiteGroup and the event matches
			if( message.getCategory() == Category.RULE_REJECTED || message.getCategory() == Category.RULE_FAILED ){
				EventLogField field = message.getField(EventLogField.FieldName.SITE_GROUP_ID);
				
				if( field != null && field.getDescription().equals( siteGroupID ) ){
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
