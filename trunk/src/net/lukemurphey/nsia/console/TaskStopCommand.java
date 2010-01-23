package net.lukemurphey.nsia.console;

import java.sql.SQLException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.Application.WorkerThreadDescriptor;
import net.lukemurphey.nsia.WorkerThread.State;

public class TaskStopCommand extends ConsoleCommand {

	public TaskStopCommand(Application application, String... names) {
		super("<task ID>", "Stops running background tasks", application, names);
	}
	
	@Override
	public CommandResult run(String[] input) throws SQLException,
			NoDatabaseConnectionException, InputValidationException {
		
		// 0 -- Precondition Check
		if( input.length < 2 ){
			System.out.println("Error: not enough arguments provided, syntax of the command is \"" + getSampleInvocation() + "\"");
			return CommandResult.SYNTAX_ERROR;
		}
		
		if( input.length > 2 ){
			System.out.println("Error: too many arguments provided, syntax of the command is \"" + getSampleInvocation() + "\"");
			return CommandResult.SYNTAX_ERROR;
		}

		// 1 -- Stop the Task
		String taskToStop = input[1];
		
		WorkerThreadDescriptor[] threadQueue = application.getWorkerThreadQueue(true);
		
		for( WorkerThreadDescriptor thread : threadQueue ){
			if( thread.getUniqueName().equalsIgnoreCase(taskToStop)){
				thread.getWorkerThread().terminate();
				System.out.print("Stopping task...");
				
				for(int c = 0; c < 4; c++){
					
					try{
						if( thread.getWorkerThread().getStatus() != State.STOPPED ){
							Thread.sleep(1000);
						}
						else{
							System.out.println("done");
							return CommandResult.EXECUTED_CORRECTLY;
						}
					}
					catch(InterruptedException e){
						//Ignore this exception, not a big deal if the thread was awoken
					}
				}
				
				System.out.println("(backgrounding)");
				return CommandResult.EXECUTED_CORRECTLY;
			}
		}
		
		
		System.out.println("No task found with the given identifier");
		return CommandResult.EXECUTED_CORRECTLY;
	}

}
