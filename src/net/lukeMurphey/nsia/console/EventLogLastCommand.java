package net.lukemurphey.nsia.console;

import java.sql.SQLException;
import java.util.Vector;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GenericUtils;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.eventlog.EventLogViewer;
import net.lukemurphey.nsia.eventlog.EventLogViewer.EventLogEntry;
import net.lukemurphey.nsia.eventlog.EventLogViewer.EventLogFilter;

public class EventLogLastCommand extends ConsoleCommand {

	private static final int MAX_TO_DISPLAY = 100;
	
	public EventLogLastCommand(Application application, String... names) {
		super("<number to display> (<event name filter>)", "List the last few event log messages", application, names);
	}

	@Override
	public CommandResult run(String[] input) throws SQLException, NoDatabaseConnectionException, InputValidationException {
		
		int entriesCount = 10;
		String contentFilter = null;
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the correct number of arguments was provided 
		/*if( input.length <= 1 ){
			System.out.println("Error: not enough arguments provided, syntax of the command is \"" + getSampleInvocation() + "\"");
			return CommandResult.SYNTAX_ERROR;
		}
		else */if( input.length > 3 ){
			System.out.println("Error: too many arguments provided, syntax of the command is \"" + getSampleInvocation() + "\"");
			return CommandResult.SYNTAX_ERROR;
		}
		
		//	 0.2 -- Parse out the number of entries to show and make sure the result is acceptable
		try{
			if( input.length >= 2 ){
				entriesCount = Integer.parseInt( input[1] );
			}
		}
		catch(NumberFormatException e){
			System.out.println("Error: The number of entries to list is not a valid integer");
			return CommandResult.SYNTAX_ERROR;
		}
		
		if( entriesCount > MAX_TO_DISPLAY ){
			System.out.println("Warning: Cannot display more than 100 entries, only " + MAX_TO_DISPLAY + " will be shown");
			entriesCount = MAX_TO_DISPLAY;
		}
		else if( entriesCount <= 0 ){
			System.out.println("Error: The number of entries to list must be greater then 0");
			return CommandResult.SYNTAX_ERROR;
		}
		
		
		// 1 -- Get the filter
		if( input.length >= 3 ){
			contentFilter = input[2];
		}
		
		// 2 -- Perform the command
		Vector<String[]> tableData = new Vector<String[]>(entriesCount + 2);
		
		//	 2.1 -- Create the filter that will get the events
		EventLogFilter filter = new EventLogFilter(entriesCount);
		
		if( contentFilter != null ){
			filter.setContentFilter(contentFilter);
		}
		filter.setEntryID(Integer.MAX_VALUE, false);
		EventLogViewer viewer = new EventLogViewer(application);
		
		//	 2.2 -- Get the events
		EventLogEntry[] entries = viewer.getEntries(filter);
		tableData.add(new String[]{"Severity", "Date", "Entry ID", "Message", "Details" });
		
		//	 2.3 -- Create a tables of the event log entries
		for(int c = entries.length-1; c >= 0; c--){
			tableData.add(new String[]{entries[c].getSeverity().toString(), entries[c].getDate().toString(), String.valueOf(entries[c].getEntryID()), entries[c].getMessage(), GenericUtils.shortenString(entries[c].getNotes(), 32) });
			entries[c].getSeverity().toString();
		}
		
		//	 2.4 -- Output the entries or a message indicating that no entries where found
		if( entries.length > 0){
			System.out.println(getTableFromString(tableData, true) );
		}
		else{
			//tableData.add(new String[]{"Severity"});
			System.out.println("No entries found");
		}
		
		return null;
	}

}
