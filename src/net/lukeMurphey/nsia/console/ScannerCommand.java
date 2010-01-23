package net.lukemurphey.nsia.console;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.ScannerController;
import net.lukemurphey.nsia.eventlog.EventLogMessage;

public class ScannerCommand extends ConsoleCommand {

	public ScannerCommand(Application application, String... names) {
		super("start | stop", "Starts or stops the scanner", application, names );
	}


	public CommandResult run(String[] input) {
		// 0 -- Precondition Check
		
		//	 0.1 -- Not enough arguments
		if( input.length < 2 ){
			System.out.println("Error: no action provided, indicate whether to start or stop the scanner (example \"" + getSampleInvocation() + "\"");
			return CommandResult.SYNTAX_ERROR;
		}
		
		//	 0.2 -- too many arguments
		else if( input.length > 2 ){
			System.out.println("Error: too many arguments provided, syntax of the command is \"" + getSampleInvocation() + "\"");
			return CommandResult.SYNTAX_ERROR;
		}
		
		// 1 -- Perform the action
		if( input[1].equalsIgnoreCase("start") )
			commandStartScanner();
		else if( input[1].equalsIgnoreCase("stop") )
			commandStopScanner();
		else
			System.out.println("Error: action must be either \"start\" or \"stop\", syntax of the command is \"" + input[0] + " start | stop\")");
		
		return CommandResult.EXECUTED_CORRECTLY;
	}
	

	private boolean commandStartScanner( ){
		ScannerController scannerController = application.getScannerController();
		
		if( scannerController == null && application.getScannerController().isCurrentlyScanning() ){
			System.out.println("Scanner is already running");
		}
		else if( scannerController != null  ){
			System.out.print("Starting scanner...");
			application.logEvent(EventLogMessage.Category.SCANNER_STARTED);
			scannerController.enableScanning();
			System.out.println("Done");
		}
		
		return true;
	}
	
	private boolean commandStopScanner( ){
		ScannerController scannerController = application.getScannerController();
		
		if( scannerController == null )
			System.out.println("Scanner controller unavailable");
		else if( !application.getScannerController().isCurrentlyScanning() ){
			System.out.println("Scanner is already paused");
		}
		else{
			System.out.print("Stopping scanner...");
			application.logEvent(EventLogMessage.Category.SCANNER_STOPPED);
			scannerController.disableScanning();
			int milliseconds = 0;
			while( scannerController.getScanningState() == ScannerController.ScannerState.PAUSING && milliseconds < 5000){
				try{
					Thread.sleep(100);
				}
				catch( InterruptedException e){
					//Ignore, we don't care if the thread was awoken
				}
				milliseconds += 100;
			}
			
			if( scannerController.getScanningState() != ScannerController.ScannerState.PAUSED )
				System.out.println("(backgrounding)");
			else
				System.out.println("Done");
		}
		
		return true;
	}

}
