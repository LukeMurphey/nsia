package net.lukeMurphey.nsia.consoleInterface;

import java.sql.SQLException;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.InputValidationException;
import net.lukeMurphey.nsia.NoDatabaseConnectionException;
import net.lukeMurphey.nsia.UserManagement;
import net.lukeMurphey.nsia.eventLog.EventLogField;
import net.lukeMurphey.nsia.eventLog.EventLogMessage;
import net.lukeMurphey.nsia.eventLog.EventLogField.FieldName;

public class EnableUserCommand extends ConsoleCommand {

	public EnableUserCommand(Application application, String... names) {
		super("<username>", "Enables the user", application, names);
	}

	public CommandResult run(String[] input) {
		// 0 -- Precondition Check
		if( input.length > 2 ){
			System.out.println("Error: too many arguments provided, syntax of the command is \"" + getSampleInvocation() + "\"");
			return CommandResult.SYNTAX_ERROR;
		}
		
		if( input.length < 2 ){
			System.out.println("Error: too few arguments provided, syntax of the command is \"" + getSampleInvocation() + "\"");
			return CommandResult.SYNTAX_ERROR;
		}
		
		
		// 1 -- Enable the user account
		UserManagement userManagement = new UserManagement(application);
		long userId;
		
		//	 1.1 -- Get the user ID
		try{
			userId = userManagement.getUserID(input[1]);
		}
		catch(SQLException e){
			System.out.println("Account could not be enabled, a SQL exception occurred");
			application.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
			return CommandResult.ERROR;
		}
		catch(NoDatabaseConnectionException e){
			System.out.println("Account could not be enabled, no database connection exists");
			application.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			return CommandResult.ERROR;
		}
		catch(InputValidationException e){
			System.out.println("Username is illegal (contains disallowed characters)");
			application.logEvent(EventLogMessage.Category.USER_NAME_ILLEGAL, new EventLogField( FieldName.TARGET_USER_NAME, input[1]) );
			return CommandResult.ERROR;
		}
		
		if( userId < 0 ){
			System.out.println("No user exists with the name given");
			application.logEvent(EventLogMessage.Category.USER_NAME_INVALID, new EventLogField( FieldName.TARGET_USER_NAME, input[1]) );
			return CommandResult.ERROR;
		}
		
		//	 1.2 -- Enable the account
		try{
			if( userManagement.enableAccount(userId) )
				System.out.println("Account enabled");
			else
				System.out.println("Account unsuccessfully enabled");
		}
		catch(SQLException e){
			System.out.println("Account could not be enabled, a SQL exception occurred");
			application.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
			return CommandResult.ERROR;
		}
		catch(NoDatabaseConnectionException e){
			System.out.println("Account could not be enabled, no database connection exists");
			application.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			return CommandResult.ERROR;
		}
		catch(InputValidationException e){
			System.out.println("Username is illegal (contains disallowed characters)");
			application.logEvent(EventLogMessage.Category.USER_NAME_ILLEGAL, new EventLogField( FieldName.TARGET_USER_NAME,input[2]) );
			return CommandResult.ERROR;
		}
		
		return CommandResult.EXECUTED_CORRECTLY;
	}

}
