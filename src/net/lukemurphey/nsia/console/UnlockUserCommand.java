package net.lukemurphey.nsia.console;

import java.sql.SQLException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NumericalOverflowException;
import net.lukemurphey.nsia.UserManagement;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;

public class UnlockUserCommand extends ConsoleCommand {

	public UnlockUserCommand(Application application, String... names) {
		super("<username>", "Unlocks the user's account (if locked due to repeated authentication attempts)", application, names);
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

		try{
			userManagement.clearAuthFailedCount(input[1]);
			System.out.println("Account unlocked, authentication failed count cleared");
		}
		catch(SQLException e){
			application.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			System.out.println("Account could not be unlocked, a SQL exception occurred");
			return CommandResult.ERROR;
		}
		catch(NoDatabaseConnectionException e){
			application.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			System.out.println("Account could not be unlocked, no database connection exists");
			return CommandResult.ERROR;
		}
		catch(InputValidationException e){
			System.out.println("Username is illegal (contains disallowed characters)");
			application.logEvent(EventLogMessage.EventType.USER_NAME_ILLEGAL, new EventLogField(FieldName.TARGET_USER_NAME, input[1] ) );
			return CommandResult.ERROR;
		}
		catch (NumericalOverflowException e) {
			System.out.println("Account could not be unlocked, numerical overflow exception");
			application.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e);
			return CommandResult.ERROR;
		} 
		
		return CommandResult.EXECUTED_CORRECTLY;
	}

}
