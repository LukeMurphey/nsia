package net.lukemurphey.nsia.console;

import net.lukemurphey.nsia.*;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;

public class ShutdownCommand extends ConsoleCommand {

	public ShutdownCommand(Application application, String... names) {
		super( null, "Shuts down the application", application, names);
	}

	public CommandResult run(String[] input) {
		// 0 -- Precondition Check
		if( input.length > 1 ){
			System.out.println("Error: too many arguments provided, syntax of the command is \"" + getSampleInvocation() + "\"");
			return CommandResult.SYNTAX_ERROR;
		}
			
		// 1 -- Perform the action
		/*
		//	 1.1 -- Determine if the user has requested a delayed shutdown
		int delay;
		if( input.length > 1 ){
			try{
				delay = Integer.parseInt( input[1] );
			}
			catch( NumberFormatException e ){
				System.out.println("Error: The shutdown delay is not a valid number");
				return true;
			}
			
		}
		else{
			
		}*/
		
		System.out.print("System is shutting down...");
		application.logEvent( EventLogMessage.Category.APPLICATION_SHUTTING_DOWN, new EventLogField(FieldName.UPTIME, GenericUtils.getTimeDescription(application.getUptime()/1000) ) );
		application.shutdown(Application.ShutdownRequestSource.CLI);
		System.out.println("Done");
		return CommandResult.TERMINATE_CONSOLE;
	}

}
