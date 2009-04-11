package net.lukeMurphey.nsia.consoleInterface;

import java.sql.SQLException;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.InputValidationException;
import net.lukeMurphey.nsia.NoDatabaseConnectionException;
import net.lukeMurphey.nsia.NumericalOverflowException;
import net.lukeMurphey.nsia.UserManagement;
import net.lukeMurphey.nsia.eventLog.EventLogField;
import net.lukeMurphey.nsia.eventLog.EventLogMessage;
import net.lukeMurphey.nsia.eventLog.EventLogField.FieldName;

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
			application.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
			System.out.println("Account could not be unlocked, a SQL exception occurred");
			return CommandResult.ERROR;
		}
		catch(NoDatabaseConnectionException e){
			application.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			System.out.println("Account could not be unlocked, no database connection exists");
			return CommandResult.ERROR;
		}
		catch(InputValidationException e){
			System.out.println("Username is illegal (contains disallowed characters)");
			application.logEvent(EventLogMessage.Category.USER_NAME_ILLEGAL, new EventLogField(FieldName.TARGET_USER_NAME, input[1] ) );
			return CommandResult.ERROR;
		}
		catch (NumericalOverflowException e) {
			System.out.println("Account could not be unlocked, numerical overflow exception");
			application.logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e);
			return CommandResult.ERROR;
		} 
		
		return CommandResult.EXECUTED_CORRECTLY;
	}

}
