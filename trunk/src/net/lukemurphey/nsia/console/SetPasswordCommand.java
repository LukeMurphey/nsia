package net.lukemurphey.nsia.console;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.Channels;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.UserManagement;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;

import java.io.Console;

public class SetPasswordCommand extends ConsoleCommand {

	
	public SetPasswordCommand(Application application, String... names) {
		super("<username> <password>", "Sets the password for the given username", application, names);
	}

	public CommandResult run(String input[]){
		// 0 -- Precondition Check
		if( input.length < 2 ){
			System.out.println("Error: too few arguments provided, syntax of the command is \"" + getSampleInvocation() + "\"");
			return CommandResult.SYNTAX_ERROR;
		}
		
		if( input.length > 2 ){
			System.out.println("Error: too many arguments provided, syntax of the command is \"" + getSampleInvocation() + "\"");
			return CommandResult.SYNTAX_ERROR;
		}
		
		
		// 1 -- Set the password
		UserManagement userManagement = new UserManagement(application);
		int userId;
		
		//	 1.1 -- Get the user ID
		try{
			userId = userManagement.getUserID(input[1]);
		}
		catch(SQLException e){
			application.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
			System.out.println("Password could not be set, a SQL exception occurred");
			return CommandResult.ERROR;
		}
		catch(NoDatabaseConnectionException e){
			application.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			System.out.println("Password could not be set, no database connection exists");
			return CommandResult.ERROR;
		}
		catch(InputValidationException e){
			System.out.println("Username is illegal (contains disallowed characters)");
			application.logEvent(EventLogMessage.Category.USER_NAME_ILLEGAL, new EventLogField( FieldName.TARGET_USER_NAME,  input[1]) ) ;
			return CommandResult.ERROR;
		}
		
		if( userId < 0 ){
			System.out.println("No user exists with the name given");
			application.logEvent( EventLogMessage.Category.USER_ID_INVALID, new EventLogField( FieldName.TARGET_USER_NAME, input[1]),  new EventLogField( FieldName.TARGET_USER_ID, userId));
			return CommandResult.ERROR;
		}
		
		//	 1.2 -- Get the password
		String password;
		
		Console console = System.console();
		
		if( console != null ){
			System.out.print("Enter the user's password:");
			password = new String( System.console().readPassword() );
			
			System.out.print("Please the confirm the password:");
			String passwordConfirm = new String( System.console().readPassword() );
			
			if( !password.equals(passwordConfirm) ){
				System.err.println("Error: The passwords do not match, user's password was not updated");
				return CommandResult.ERROR;
			}
		}
		else{
			
			InputStream inputStreamChannel = null;
			InputStreamReader inputStreamReader = null;
			BufferedReader in = null;
			
			try {
				inputStreamChannel = Channels.newInputStream((new FileInputStream(FileDescriptor.in)).getChannel());
				inputStreamReader = new InputStreamReader(inputStreamChannel);
				in = new BufferedReader( inputStreamReader );
				
				System.out.print("Enter the user's password: ");
				password = in.readLine();
			} catch (IOException e) {
				System.err.println("Password was not successfully read");
				return CommandResult.ERROR;
			}
		}
		
		//	 1.2 -- Set the password for the user ID
		try{
			if( userManagement.changePassword(userId, password) )
				System.out.println("Password successfully changed");
			else
				System.out.println("Password was not successfully changed");
			}
		catch(SQLException e){
			System.out.println("Password could not be set, a SQL exception occurred");
			application.logExceptionEvent( EventLogMessage.Category.SQL_EXCEPTION, e );
			return CommandResult.ERROR;
		}
		catch(NoSuchAlgorithmException e){
			System.out.println("Password could not be set, the hash algorithm is unknown");
			application.logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e );
			return CommandResult.ERROR;
		}
		catch(NoDatabaseConnectionException e){
			System.out.println("Password could not be set, no database connection exists");
			application.logExceptionEvent( EventLogMessage.Category.DATABASE_FAILURE, e );
			return CommandResult.ERROR;
		}
		catch(InputValidationException e){
			System.out.println("Password is illegal (contains disallowed characters)");
			application.logEvent( EventLogMessage.Category.PASSWORD_ILLEGAL, new EventLogField(FieldName.TARGET_USER_ID, userId) );
			return CommandResult.ERROR;
		}
		
		return CommandResult.EXECUTED_CORRECTLY;
	}
}
