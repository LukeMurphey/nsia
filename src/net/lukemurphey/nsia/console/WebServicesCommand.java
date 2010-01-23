package net.lukemurphey.nsia.console;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NetworkManager;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;

public class WebServicesCommand extends ConsoleCommand {

	public WebServicesCommand(Application application, String... names) {
		super("start|stop", "Starts or stops the internal web server", application, names);
	}

	public CommandResult run(String[] input) {
		
		// 0 -- Precondition Check
		
		//	 0.1 -- Not enough arguments
		if( input.length < 2 ){
			System.out.println("Error: no action provided, indicate whether to start or stop the internal web server (example \"" + getSampleInvocation() + "\"");
			return CommandResult.SYNTAX_ERROR;
		}
		
		//	 0.2 -- too many arguments
		else if( input.length > 2 ){
			System.out.println("Error: too many arguments provided, syntax of the command is \"" + getSampleInvocation() + "\"");
			return CommandResult.SYNTAX_ERROR;
		}
		
		// 1 -- Perform the action
		if( input[1].equalsIgnoreCase("start") )
			commandStartWebServices();
		else if( input[1].equalsIgnoreCase("stop") )
			commandStopWebServices();
		
		return CommandResult.EXECUTED_CORRECTLY;
	}
	
	private boolean commandStartWebServices( ){
		NetworkManager manager = application.getNetworkManager();
		
		if( manager == null )
			System.out.println("Web services manager unavailable");
		else if( manager.isListenerRunning() ){
			System.out.println("Web server is already running");
		}
		else{
			System.out.print("Starting web server...");
			//application.logEvent(StringTable.MSGID_SCANNER_STOPPED); // Create entry for shutting down web server
			try{
				manager.startListener();
			}catch(Exception e){
				System.out.println("Server could not be started, exception occurred");
				application.logEvent( EventLogMessage.Category.OPERATION_FAILED, new EventLogField( FieldName.OPERATION, "Start web services") );
			}
			System.out.println("Done");
		}
		
		return true;
	}
	
	private boolean commandStopWebServices( ){
		NetworkManager manager = application.getNetworkManager();
		
		if( manager == null )
			System.out.println("Web services manager unavailable");
		else if( !manager.isListenerRunning() ){
			System.out.println("Web server is not running");
		}
		else{
			System.out.print("Shutting down web server...");
			//application.logEvent(StringTable.MSGID_SCANNER_STOPPED); // Create entry for shutting down web server
			manager.stopListener();
		}
		
		System.out.println("Done");
		
		System.out.println("Warning: the internal web server has been shutdown. Users will not be able to connect and manage " + Application.APPLICATION_VENDOR + " " + Application.APPLICATION_NAME + " remotely.");
		return true;
	}

}
