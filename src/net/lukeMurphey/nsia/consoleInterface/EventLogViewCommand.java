package net.lukeMurphey.nsia.consoleInterface;

import java.sql.SQLException;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.InputValidationException;
import net.lukeMurphey.nsia.NoDatabaseConnectionException;
import net.lukeMurphey.nsia.NotFoundException;
import net.lukeMurphey.nsia.eventLog.EventLogViewer;
import net.lukeMurphey.nsia.eventLog.EventLogViewer.EventLogEntry;

public class EventLogViewCommand extends ConsoleCommand {

	public EventLogViewCommand(Application application, String... names) {
		super("<event log entry ID>", "Opens an event log entry for viewing", application, names);
	}

	@Override
	public CommandResult run(String[] input) throws SQLException, NoDatabaseConnectionException, InputValidationException {
		
		int entryId;
		
		// 0 -- Precondition check
		if( input.length <= 1){
			System.out.println("Error: not enough arguments provided, syntax of the command is \" " + getSampleInvocation() + "\"");
			return CommandResult.SYNTAX_ERROR;
		}
		
		else if( input.length > 2){
			System.out.println("Error: too many arguments provided, syntax of the command is \"" + getSampleInvocation() + "\"");
			return CommandResult.SYNTAX_ERROR;
		}
		
		try{
			entryId = Integer.parseInt(input[1]);
		}
		catch(NumberFormatException e){
			System.out.println("Error: the event log entry ID is not a valid integer");
			return CommandResult.SYNTAX_ERROR;
		}
		
		
		// 1 -- Get the entry to view
		Vector<String[]> tableData = new Vector<String[]>();
		EventLogViewer viewer = new EventLogViewer(application);
		tableData.add( new String[]{"Log Entry", ""});
		
		try{
			EventLogEntry entry = viewer.getEntry(entryId);
			
			tableData.add( new String[]{"Severity", entry.getSeverity().toString()});
			tableData.add( new String[]{"Date", entry.getDate().toString()});
			tableData.add( new String[]{"Entry ID", String.valueOf( entryId )});
			tableData.add( new String[]{"Message", entry.getMessage()});
			
			String[] notes = StringUtils.split(entry.getNotes(), ",");
			
			for( int c = 0; c < notes.length; c++){
				boolean isLast = false;
				
				if( c == (notes.length-1)  ){
					isLast = true;
				}
				
				if( c == 0 && isLast ){
					tableData.add( new String[]{"Details", notes[c].trim() });
				}
				else if( c == 0 ){
					tableData.add( new String[]{"Details", notes[c].trim() + "," });
				}
				else if( isLast ){
					tableData.add( new String[]{"", notes[c].trim() });
				}
				else{
					tableData.add( new String[]{"", notes[c].trim() + "," });
				}
				
			}
			
			System.out.println(getTableFromString(tableData, true));
			
		}
		catch( NotFoundException e){
			System.out.println("No event log entry was found with ID " + entryId);
		}

		
		return CommandResult.EXECUTED_CORRECTLY;
	}

}
