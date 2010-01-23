package net.lukemurphey.nsia.xmlRpcInterface;

import java.net.UnknownHostException;
import java.util.*;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.DisallowedOperationException;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.InvalidLocalPartException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.PasswordInvalidException;
import net.lukemurphey.nsia.UserManagement.UserDescriptor;
import net.lukemurphey.nsia.trustBoundary.ApiUserManagement;


public class XmlrpcUserManagement extends XmlrpcHandler {
	
	public static final String DEFAULT_HASH_ALGORITHM = "sha-512";
	public static final long DEFAULT_HASH_ITERATION_COUNT = 10000;
	
	protected ApiUserManagement userManager;
	
	public XmlrpcUserManagement(Application appRes){
		super(appRes);
		userManager = new ApiUserManagement(appRes);
	}
	
	/**
	 * Method unlocks accounts that are locked due to repeated authentication attempts.
	 * @param sessionIdentifier
	 * @param userName
	 * @throws GeneralizedException
	 * @throws InputValidationException
	 * @throws InsufficientPermissionException
	 * @throws NoSessionException
	 */
	public void clearAuthFailedCount( String sessionIdentifier, String userName ) throws GeneralizedException, InputValidationException, InsufficientPermissionException, NoSessionException{
		userManager.clearAuthFailedCount( sessionIdentifier, userName );
	}
	
	/**
	 * Create an account with the given parameters for the new user's account.
	 * @param sessionIdentifier
	 * @param userName
	 * @param realName
	 * @param password
	 * @param emailAddress
	 * @param unrestricted
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws InputValidationException
	 * @throws UnknownHostException
	 * @throws InvalidLocalPartException
	 * @throws DisallowedOperationException 
	 */
	public double addAccount( String sessionIdentifier, String userName, String realName, String password, String emailAddress, boolean unrestricted ) throws NoSessionException, GeneralizedException, InsufficientPermissionException, InputValidationException, UnknownHostException, InvalidLocalPartException, DisallowedOperationException{
		return userManager.addAccount( sessionIdentifier, userName, realName, password, emailAddress, unrestricted);
	}
	
	/**
	 * Change the password for the given user.
	 * @param sessionIdentifier
	 * @param userId
	 * @param newPassword
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws InputValidationException
	 * @throws PasswordInvalidException 
	 */
	public boolean changePassword( String sessionIdentifier, int userId, String newPassword, String confirmationPassword ) throws NoSessionException, GeneralizedException, InsufficientPermissionException, InputValidationException, PasswordInvalidException{
		return userManager.changePassword( sessionIdentifier, userId, newPassword, confirmationPassword );
	}
	
	/**
	 * Disable the account associated with the given user ID.
	 * @param sessionIdentifier
	 * @param userId
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws DisallowedOperationException 
	 */
	public boolean deleteAccount( String sessionIdentifier, int userId ) throws NoSessionException, GeneralizedException, InsufficientPermissionException, DisallowedOperationException{
		return userManager.deleteAccount( sessionIdentifier, userId );
	}
	
	/**
	 * Disable the account associated with the given user ID.
	 * @param sessionIdentifier
	 * @param userId
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws DisallowedOperationException 
	 */
	public boolean disableAccount( String sessionIdentifier, int userId ) throws NoSessionException, GeneralizedException, InsufficientPermissionException, DisallowedOperationException{
		return userManager.disableAccount( sessionIdentifier, userId );
	}
	
	/**
	 * Enable the account associated with the given user ID.
	 * @param sessionIdentifier
	 * @param userId
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public boolean enableAccount( String sessionIdentifier, int userId ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		return userManager.enableAccount( sessionIdentifier, userId);
	}
	
	/**
	 * Get user the descriptor for the given user ID.
	 * @param sessionIdentifier
	 * @param userId
	 * @return
	 * @throws SQLException
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws NotFoundException 
	 * @throws NoDatabaseConnectionException 
	 */
	public Hashtable<String, Object> getUserDescriptor( String sessionIdentifier, int userId ) throws NoSessionException, GeneralizedException, InsufficientPermissionException, NotFoundException{
		
		UserDescriptor userDescriptor;
		userDescriptor = userManager.getUserDescriptor( sessionIdentifier, userId );
		
		Hashtable<String, Object> userInfo = new Hashtable<String, Object>();
		
		if( userDescriptor.getEmailAddress() == null )
			userInfo.put( "EmailAddress", "");
		else
			userInfo.put( "EmailAddress", userDescriptor.getEmailAddress());
		
		userInfo.put( "Username", userDescriptor.getUserName());
		userInfo.put( "UserID", Integer.valueOf( (int)userDescriptor.getUserID() ));
		userInfo.put( "UnrestrictedAccount", Boolean.valueOf( userDescriptor.isUnrestricted() ));
		userInfo.put( "AccountStatus", Integer.valueOf( userDescriptor.getAccountStatus().ordinal() ));
		userInfo.put( "Fullname", userDescriptor.getFullname());
		userInfo.put( "BruteForceLocked", Boolean.valueOf( userDescriptor.isBruteForceLocked() ));
		
		return userInfo;
	}
	
	/**
	 * Get all user the descriptors.
	 * @param sessionIdentifier
	 * @param userId
	 * @return
	 * @throws SQLException
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public Vector<Hashtable<String, Object>> getUserDescriptors( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{

		//	1 -- Get the user information
		UserDescriptor[] userDescriptor = userManager.getUserDescriptors( sessionIdentifier );
		
		Vector<Hashtable<String, Object>> userDescs = new Vector<Hashtable<String, Object>>();
		
		for( int c = 0; c < userDescriptor.length; c++){
			Hashtable<String, Object> userInfo = new Hashtable<String, Object>();
			if( userDescriptor[c].getEmailAddress() != null )
					userInfo.put( "EmailAddress", userDescriptor[c].getEmailAddress().toString() );
			userInfo.put( "Username", userDescriptor[c].getUserName());
			userInfo.put( "UserID", Integer.valueOf( (int)userDescriptor[c].getUserID() ));
			userInfo.put( "UnrestrictedAccount", Boolean.valueOf( userDescriptor[c].isUnrestricted() ));
			userInfo.put( "AccountStatus", Integer.valueOf( userDescriptor[c].getAccountStatus().ordinal() ));
			
			if( userDescriptor[c].getFullname() == null )
				userInfo.put( "Fullname", EMPTY_STRING);
			else
				userInfo.put( "Fullname", userDescriptor[c].getFullname());
			
			userInfo.put( "BruteForceLocked", Boolean.valueOf( userDescriptor[c].isBruteForceLocked() ));
			
			userDescs.add( userInfo );
		}
		
		return userDescs;
	}
	
	/**
	 * Get the user descriptor for the given user name.
	 * @param sessionIdentifier
	 * @param userName
	 * @return
	 * @throws SQLException
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws InputValidationException
	 * @throws NotFoundException 
	 * @throws NoDatabaseConnectionException 
	 */
	public Hashtable<String, Object> getUserDescriptor( String sessionIdentifier, String userName ) throws NoSessionException, GeneralizedException, InsufficientPermissionException, InputValidationException, NotFoundException{
		
		//	1 -- Get the user information
		UserDescriptor userDescriptor = userManager.getUserDescriptor( sessionIdentifier, userName );
		
		Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
		
		hashtable.put( "EmailAddress", userDescriptor.getEmailAddress().toString());
		hashtable.put( "Username", userDescriptor.getUserName());
		hashtable.put( "UserID", Integer.valueOf( (int)userDescriptor.getUserID() ) );
		hashtable.put( "UnrestrictedAccount", String.valueOf( userDescriptor.isUnrestricted() ));
		hashtable.put( "AccountStatus", Integer.valueOf( userDescriptor.getAccountStatus().ordinal() ));
		hashtable.put( "Fullname", userDescriptor.getFullname());
		hashtable.put( "BruteForceLocked", Boolean.valueOf( userDescriptor.isBruteForceLocked() ));
		
		return hashtable;
	}
	
	/**
	 * Resolve the user ID for the given user name. The user will need to have UserManagement read rights unless the user is
	 * attempting to resolve their own user ID.
	 * @param sessionIdentifier
	 * @param userName
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws InputValidationException
	 */
	public int getUserId( String sessionIdentifier, String userName ) throws NoSessionException, GeneralizedException, InsufficientPermissionException, InputValidationException{
		
		return (int)userManager.getUserId( sessionIdentifier, userName );

		
	}
	
	/**
	 * Update the account parameters for the given user and assume that the account is a restricted account (non-root).
	 * @param sessionIdentifier
	 * @param userId
	 * @param realName
	 * @param emailAddress
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws UnknownHostException
	 * @throws InputValidationException
	 * @throws InvalidLocalPartException
	 * @throws DisallowedOperationException 
	 */
	public boolean updateAccount( String sessionIdentifier, int userId, String userName, String realName, String emailAddress ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, UnknownHostException, InputValidationException, InvalidLocalPartException, DisallowedOperationException{
		return userManager.updateAccount( sessionIdentifier, userId, userName, realName, emailAddress );
	}
	
	/**
	 * Update the account parameters for the given user and assume that the account is a restricted account (non-root).
	 * @param sessionIdentifier
	 * @param userId
	 * @param realName
	 * @param emailAddress
	 * @param accountEnabled
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws UnknownHostException
	 * @throws InputValidationException
	 * @throws InvalidLocalPartException
	 * @throws DisallowedOperationException 
	 */
	public boolean updateAccount( String sessionIdentifier, int userId, boolean accountEnabled, String userName, String realName, String emailAddress ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, UnknownHostException, InputValidationException, InvalidLocalPartException, DisallowedOperationException{
		return userManager.updateAccount( sessionIdentifier, userId, accountEnabled, userName, realName, emailAddress );
	}
	
	/**
	 * Update the account parameters for the given user. Note that the unrestricted option cannot be set unless
	 * the current user is also unrestricted. Note that unrestricted accounts are not subject to the access
	 * control lists and are thus essentially "root" accounts.
	 * @precondition The session identifier, user ID, real name and email address must be valid and legal. The user also needs to have modify rights to "Users.UpdateOwnPassword" to change their own account and "Administration.UserManagement" modify rights to update others accounts.
	 * @postcondition The account will be updated per the arguments or throw an exception or return false 
	 * @param sessionIdentifier
	 * @param userId
	 * @param realName
	 * @param emailAddress
	 * @param unrestricted
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws UnknownHostException
	 * @throws InputValidationException
	 * @throws InvalidLocalPartException
	 * @throws DisallowedOperationException 
	 */
	public boolean updateAccountEx( String sessionIdentifier, int userId, String userName, String realName, String emailAddress, boolean unrestricted ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, UnknownHostException, InputValidationException, InvalidLocalPartException, DisallowedOperationException{
		return userManager.updateAccountEx( sessionIdentifier, userId, userName, realName, emailAddress, unrestricted);
	}
	
	/**
	 * Update the account parameters for the given user. Note that the unrestricted option cannot be set unless
	 * the current user is also unrestricted. Note that unrestricted accounts are not subject to the access
	 * control lists and are thus essentially "root" accounts.
	 * @precondition The session identifier, user ID, real name and email address must be valid and legal. The user also needs to have modify rights to "Users.UpdateOwnPassword" to change their own account and "Administration.UserManagement" modify rights to update others accounts.
	 * @postcondition The account will be updated per the arguments or throw an exception or return false 
	 * @param sessionIdentifier
	 * @param userId
	 * @param realName
	 * @param emailAddress
	 * @param unrestricted
	 * @param accountEnabled
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws UnknownHostException
	 * @throws InputValidationException
	 * @throws InvalidLocalPartException
	 * @throws DisallowedOperationException 
	 */
	public boolean updateAccountEx( String sessionIdentifier, int userId, boolean accountEnabled, String userName, String realName, String emailAddress, boolean unrestricted ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, UnknownHostException, InputValidationException, InvalidLocalPartException, DisallowedOperationException{
		return userManager.updateAccountEx( sessionIdentifier, userId, accountEnabled, userName, realName, emailAddress, unrestricted);
	}
}
