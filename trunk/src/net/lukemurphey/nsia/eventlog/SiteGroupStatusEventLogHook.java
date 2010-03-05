package net.lukemurphey.nsia.eventlog;

import java.util.Vector;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.eventlog.EventLogMessage.Category;
import net.lukemurphey.nsia.response.Action;
import net.lukemurphey.nsia.response.ActionFailedException;

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
	
	/**
	 * Get all of the event hooks for the given site group.
	 * @param app
	 * @param siteGroupID
	 * @return
	 */
	public static SiteGroupStatusEventLogHook[] getSiteGroupEventLogHooks(Application app, long siteGroupID){
		
		// 1 -- Get the list of hooks
		EventLogHook[] allHooks = app.getEventLog().getHooks();
		
		Vector<SiteGroupStatusEventLogHook> siteGroupHooks = new Vector<SiteGroupStatusEventLogHook>();
		
		for(int c = 0; c < allHooks.length; c++){
			if( allHooks[c] instanceof SiteGroupStatusEventLogHook && ((SiteGroupStatusEventLogHook)allHooks[c]).getSiteGroupID() == siteGroupID ){
				siteGroupHooks.add( (SiteGroupStatusEventLogHook) allHooks[c] );
			}
		}
		
		SiteGroupStatusEventLogHook[] hooksArray = new SiteGroupStatusEventLogHook[siteGroupHooks.size()];
		siteGroupHooks.toArray(hooksArray);
		return hooksArray;
	}

}