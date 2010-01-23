package net.lukemurphey.nsia.console;


import java.sql.SQLException;
import java.util.Vector;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.Application.WorkerThreadDescriptor;
import net.lukemurphey.nsia.WorkerThread.State;

public class TaskListCommand extends ConsoleCommand {

	public TaskListCommand(Application application, String... names) {
		super("(all)", "Lists the running tasks", application, names);
	}
	
	@Override
	public CommandResult run(String[] input) throws SQLException,
			NoDatabaseConnectionException, InputValidationException {
		
		// 0 -- Precondition check
		if( input.length > 2 ){
			System.out.println("Error: too many arguments provided, syntax of the command is \"" + getSampleInvocation() + "\"");
			return CommandResult.ERROR;
		}
		
		
		// 1 -- List the tasks 
		WorkerThreadDescriptor[] threads = Application.getApplication().getWorkerThreadQueue();
		Vector<String[]> table = new Vector<String[]>();

		table.add( new String[]{ "Task ID", "User ID", "Task Description", "Progress" } );

		boolean showAll = false;
		if( input.length == 2 ){
			if( input[1].equalsIgnoreCase("all")){
				showAll = true;
			}
			else{
				showAll = false;
			}
		}

		for(int c = 0; c < threads.length; c++)
		{
			if( threads[c].getWorkerThread().getStatus() == State.STOPPED && showAll == true )
			{
				table.add( new String[]{ threads[c].getUniqueName(), String.valueOf( threads[c].getUserID() ), threads[c].getWorkerThread().getTaskDescription(), threads[c].getWorkerThread().getStatusDescription() } );
			}
			else if(threads[c].getWorkerThread().getStatus() != State.STOPPED)
			{
				table.add( new String[]{ threads[c].getUniqueName(), String.valueOf( threads[c].getUserID() ), threads[c].getWorkerThread().getTaskDescription(), threads[c].getWorkerThread().getStatusDescription() } );
			}
		}

		String[][] tableArray = new String[table.size()][];
		table.toArray(tableArray);
		
		System.out.println( getTableFromString(tableArray, true) );
		if( table.size() == 1 ){
			System.out.println("No running tasks\n");
		}
		
		// 2 -- List information about the task
		
		return CommandResult.ERROR;
	}
	
}
