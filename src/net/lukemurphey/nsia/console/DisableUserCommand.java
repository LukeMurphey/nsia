package net.lukemurphey.nsia.console;

import java.sql.SQLException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.UserManagement;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;

public class DisableUserCommand extends ConsoleCommand {

	public DisableUserCommand(Application application, String... names) {
		super("<username>", "Disables the user", application, names);
	}

	public CommandResult run(String[] input) {

		// 0 -- Precondition Check
		if( input.length > 2 ){
			System.out.println("Error: too many arguments provided, syntax of the command is \"" + getSampleInvocation() + "\"");
			return CommandResult.ERROR;
		}
		
		if( input.length < 2 ){
			System.out.println("Error: too few arguments provided, syntax of the command is \"" + getSampleInvocation() + "\"");
			return CommandResult.ERROR;
		}
		
		
		// 1 -- Enable the user account
		UserManagement userManagement = new UserManagement(application);
		int userId;
		
		//	 1.1 -- Get the user ID
		try{
			userId = userManagement.getUserID(input[1]);
		}
		catch(SQLException e){
			application.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			System.out.println("Account could not be enabled, a SQL exception occurred");
			return CommandResult.ERROR;
		}
		catch(NoDatabaseConnectionException e){
			application.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			System.out.println("Account could not be enabled, no database connection exists");
			return CommandResult.ERROR;
		}
		catch(InputValidationException e){
			System.out.println("Username is illegal (contains disallowed characters)");
			application.logEvent(EventLogMessage.EventType.USER_NAME_ILLEGAL, new EventLogField(FieldName.TARGET_USER_NAME, input[1]));
			return CommandResult.ERROR;
		}
		
		if( userId < 0 ){
			System.out.println("No user exists with the name given");
			application.logEvent(EventLogMessage.EventType.USER_ID_INVALID, new EventLogField(FieldName.TARGET_USER_NAME, input[1]) );
			return CommandResult.ERROR;
		}
		
		//	 1.2 -- Enable the account
		try{
			if( userManagement.disableAccount(userId) )
				System.out.println("Account disabled");
			else
				System.out.println("Account unsuccessfully disabled");
		}
		catch(SQLException e){
			application.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			System.out.println("Account could not be enabled, a SQL exception occurred");
			return CommandResult.ERROR;
		}
		catch(NoDatabaseConnectionException e){
			application.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			System.out.println("Account could not be enabled, no database connection exists");
			return CommandResult.ERROR;
		}
		catch(InputValidationException e){
			System.out.println("Username is illegal (contains disallowed characters)");
			application.logEvent(EventLogMessage.EventType.USER_NAME_ILLEGAL, new EventLogField(FieldName.TARGET_USER_NAME, input[2]) );
			return CommandResult.ERROR;
		}
		
		return CommandResult.EXECUTED_CORRECTLY;
	}

}
