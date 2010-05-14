package net.lukemurphey.nsia.trustBoundary;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Vector;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogHook;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogSeverity;
import net.lukemurphey.nsia.eventlog.RuleStatusEventLogHook;
import net.lukemurphey.nsia.eventlog.SiteGroupStatusEventLogHook;
import net.lukemurphey.nsia.eventlog.SystemStatusEventLogHook;
import net.lukemurphey.nsia.extension.ArgumentFieldsInvalidException;
import net.lukemurphey.nsia.response.Action;

public class ApiActions extends ApiHandler {
	
	public ApiActions(Application appRes) {
		super(appRes);
	}
	
	
	public SiteGroupStatusEventLogHook[] getSiteGroupEventLogHooks(String sessionIdentifier, long siteGroupID){
		
		// 0 -- Precondition Check
		//TODO need to check session and permissions
		
		// 1 -- Get the list of hooks
		EventLogHook[] allHooks = appRes.getEventLog().getHooks();
		
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
	
	public RuleStatusEventLogHook[] getRuleStatusEventLogHooks(String sessionIdentifier, long ruleID){
		
		// 0 -- Precondition Check
		//TODO need to check session and permissions
		
		
		// 1 -- Get the list of hooks
		EventLogHook[] allHooks = appRes.getEventLog().getHooks();
		
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
	
	
	public SystemStatusEventLogHook[] getSystemStatusEventLogHooks(String sessionIdentifier){
		
		// 0 -- Precondition Check
		//TODO need to check session and permissions
		
		
		// 1 -- Get the list of hooks
		EventLogHook[] allHooks = appRes.getEventLog().getHooks();
		
		Vector<SystemStatusEventLogHook> ruleHooks = new Vector<SystemStatusEventLogHook>();
		
		for(int c = 0; c < allHooks.length; c++){
			if( allHooks[c] instanceof SystemStatusEventLogHook ){
				ruleHooks.add( (SystemStatusEventLogHook) allHooks[c] );
			}
		}
		
		SystemStatusEventLogHook[] hooksArray = new SystemStatusEventLogHook[ruleHooks.size()];
		ruleHooks.toArray(hooksArray);
		return hooksArray;
	}
	
	public boolean updateEventLogHook(String sessionIdentifier, int actionID, Hashtable<String, String> moduleArguments) throws ArgumentFieldsInvalidException, GeneralizedException{
		EventLogHook hook = getEventLogHook(sessionIdentifier, actionID);
		
		if( hook != null ){

			hook.getAction().configure(moduleArguments);

			try {
				hook.saveToDatabase();
			} catch (SQLException e) {
				appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e );
				throw new GeneralizedException();
			} catch (IOException e) {
				appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e );
				throw new GeneralizedException();
			} catch (NoDatabaseConnectionException e) {
				appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
				throw new GeneralizedException();
			}
			
			return true;
		}
		else{
			return false;
		}
		
	}
	
	public EventLogHook getEventLogHook(String sessionIdentifier, int actionID){
		
		// 0 -- Precondition Check
		//TODO need to check session and permissions
		
		
		// 1 -- Get the list of hooks
		EventLogHook[] allHooks = appRes.getEventLog().getHooks();
		
		for(int c = 0; c < allHooks.length; c++){
			
			if( allHooks[c].getEventLogHookID() == actionID ){
				return allHooks[c];
			}
		}
		
		return null; //Not found
	}
	
	public void addSiteGroupAction(String sessionIdentifier, Action action, int siteGroupID) throws GeneralizedException{
		// 0 -- Precondition Check
		//TODO need to check session and permissions
		
		// 1 -- Add the appropriate hook
		SiteGroupStatusEventLogHook hook = new SiteGroupStatusEventLogHook(action, siteGroupID, EventLogSeverity.WARNING);
		
		try {
			hook.saveToDatabase();
		
			appRes.getEventLog().addHook(hook);
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (IOException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}
	}
	
	public void addRuleAction(String sessionIdentifier, Action action, long ruleID) throws GeneralizedException{
		// 0 -- Precondition Check
		//TODO need to check session and permissions
		
		// 1 -- Add the appropriate hook
		RuleStatusEventLogHook hook = new RuleStatusEventLogHook(action, ruleID, EventLogSeverity.WARNING);
		try {
			hook.saveToDatabase();
		
			appRes.getEventLog().addHook(hook);
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (IOException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}
	}
	
	public void addSystemAction(String sessionIdentifier, Action action) throws GeneralizedException{
		// 0 -- Precondition Check
		//TODO need to check session and permissions
		
		// 1 -- Add the appropriate hook
		SystemStatusEventLogHook hook = new SystemStatusEventLogHook(action, EventLogSeverity.WARNING);
		try {
			hook.saveToDatabase();
		
			appRes.getEventLog().addHook(hook);
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (IOException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}
	}
	
	public void deleteAction( String sessionIdentifier, long[] hookID ) throws GeneralizedException{
		// 0 -- Precondition Check
		//super.checkDelete(sessionIdentifier, objectId, "Delete");
		
		// 1 -- Delete each action
		for (long id : hookID) {
			deleteAction( sessionIdentifier, id );
		}
	}
	
	public void deleteAction( String sessionIdentifier, long hookID ) throws GeneralizedException{
		
		// 0 -- Precondition check
		
		
		// 1 -- Delete the action
		try{
			appRes.getEventLog().deleteHook(hookID);
			EventLogMessage message = new EventLogMessage(EventLogMessage.EventType.RESPONSE_ACTION_DELETED, new EventLogField(EventLogField.FieldName.RESPONSE_ACTION_ID, hookID));
			appRes.logEvent(message);
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}
		
	}
}
