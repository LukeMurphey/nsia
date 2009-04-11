package net.lukeMurphey.nsia.consoleInterface;

import java.sql.SQLException;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.NoDatabaseConnectionException;
import net.lukeMurphey.nsia.UserManagement;
import net.lukeMurphey.nsia.UserManagement.UserDescriptor;

public class ListUsersCommand extends ConsoleCommand {

	public ListUsersCommand(Application application, String... names) {
		super( null, "Lists user accounts", application, names);
	}

	public CommandResult run(String[] input) throws SQLException, NoDatabaseConnectionException{
		UserManagement userManagement = new UserManagement(application);
		
		UserDescriptor[] userDescriptors = userManagement.getUserDescriptors();

		String[][] table = new String[userDescriptors.length+1][5];
		table[0] = new String[]{ "Login Name", "Full Name", "User ID", "Status", "Is Unrestricted" };
		
		for( int c = 0; c < userDescriptors.length; c++ ){
			String status = null;
			
			if( userDescriptors[c].getAccountStatus() == UserManagement.AccountStatus.ADMINISTRATIVELY_LOCKED)
				status = "Administratively Locked" ;
			else if( userDescriptors[c].getAccountStatus() == UserManagement.AccountStatus.BRUTE_FORCE_LOCKED)
				status = "Locked due to repeated authentication failures";
			else if( userDescriptors[c].getAccountStatus() == UserManagement.AccountStatus.DISABLED)
				status = "Disabled";
			else //if( userDescriptors[c].getAccountStatus() == UserManagement.ACCOUNT_VALID_USER)
				status = "Active";
			
			String unrestricted;
			
			if( userDescriptors[c].isUnrestricted() ){
				unrestricted = "yes";
			}
			else{
				unrestricted = "no";
			}
			
			table[c+1] = new String[]{userDescriptors[c].getUserName(), userDescriptors[c].getFullname(), String.valueOf(userDescriptors[c].getUserID()), status, unrestricted };
		}
		
		System.out.println( getTableFromString(table, true) );
		
		return CommandResult.EXECUTED_CORRECTLY;
		
	}

}
