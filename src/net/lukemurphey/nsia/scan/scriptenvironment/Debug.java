package net.lukemurphey.nsia.scan.scriptenvironment;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.eventlog.EventLog;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.eventlog.EventLogMessage.EventType;
import net.lukemurphey.nsia.scan.Definition;

/**
 * The Debug class provides definitions a mechanism to create debug log messages.
 * This way, definition authors can add debug log messages to their definitions
 * while writing and testing new definitions.
 * @author Luke
 *
 */
public class Debug {
	
	private Definition definition;
	
	public Debug( Definition definition ){
		
		// 0 -- Precondition check: make sure a definition was provided
		if( definition == null ){
			throw new IllegalArgumentException("The definition argument cannot be null");
		}
		
		// 1 -- Configure the class
		this.definition = definition;
	}
	
	public void sendMessage( String message ){
		
		// 0 -- Make sure a valid message was provided.
		if( message == null ){
			return;//No message to send
		}
		
		// 1 -- Create the log message
		EventLog log = Application.getApplication().getEventLog();
		
		EventLogMessage eventLogMessage = new EventLogMessage(EventType.DEFINITION_DEBUG_MESSAGE,
				new EventLogField(FieldName.MESSAGE, message),
				new EventLogField(FieldName.DEFINITION_ID, definition.getID()),
				new EventLogField(FieldName.DEFINITION_REVISION, definition.getRevision()),
				new EventLogField(FieldName.DEFINITION_NAME, definition.getFullName()));
		
		log.logEvent(eventLogMessage);
	}

}
