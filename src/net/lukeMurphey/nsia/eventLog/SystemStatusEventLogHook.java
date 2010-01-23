package net.lukemurphey.nsia.eventlog;

import net.lukemurphey.nsia.eventlog.EventLogMessage.Category;
import net.lukemurphey.nsia.response.Action;
import net.lukemurphey.nsia.response.ActionFailedException;

public class SystemStatusEventLogHook extends EventLogHook {

	private static final long serialVersionUID = 1L;
	private Action action;
	private EventLogSeverity minimumSeverity;
	
	public SystemStatusEventLogHook( Action action, EventLogSeverity minimumSeverity ){
		
		// 0 -- Precondition check
		if( action == null ){
			throw new IllegalArgumentException("The action to perform cannot be null");
		}
		
		if( minimumSeverity == null ){
			throw new IllegalArgumentException("The minimum severity level cannot be null");
		}
		
		
		this.action = action;
		this.minimumSeverity = minimumSeverity;
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
			// 2 -- Make sure the event is a system event
			if( message.getCategory() == Category.SCANNER_STOPPED ){
				//Perform action
				action.execute(message);
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
