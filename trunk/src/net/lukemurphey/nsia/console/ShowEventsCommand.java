package net.lukemurphey.nsia.console;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.eventlog.EventLog;

public class ShowEventsCommand extends ConsoleCommand {

	public ShowEventsCommand( Application application, String... names) {
		super("true | false", "Enables/disables printing of the log messages to the console interface as they arrive", application, names);
	}
	
	public CommandResult run(String[] input) {
		// 0 -- Precondition Check
		
		//	 0.1 -- too many arguments
		if( input.length > 2 ){
			System.out.println("Error: too many arguments provided, syntax of the command is \"" + getSampleInvocation() + "\"");
			return CommandResult.SYNTAX_ERROR;
		}
		
		// 1 -- Perform the action
		if( input.length > 1 && input[1].equalsIgnoreCase("false") )
			commandHideLogMessages();
		else if( input.length == 1 || (input.length > 1 && input[1].equalsIgnoreCase("true") ) )
			commandShowLogMessages();
		else{
			System.out.println("Error: action must be either \"true\" or \"false\", syntax of the command is \"" + getSampleInvocation() + "\"");
			return CommandResult.SYNTAX_ERROR;
		}
		
		return CommandResult.EXECUTED_CORRECTLY;
	}

	private void commandShowLogMessages() {
		EventLog eventlog = application.getEventLog();
		eventlog.repeatMessagesToConsole(true);
		System.out.println("Printing of event log messages to the console is enabled");
	}

	private void commandHideLogMessages() {
		EventLog eventlog = application.getEventLog();
		eventlog.repeatMessagesToConsole(false);
		System.out.println("Printing of event log messages to the console is disabled");
	}
}
