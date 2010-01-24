package net.lukemurphey.nsia.trustBoundary;

import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.DisallowedOperationException;
import net.lukemurphey.nsia.EmailAddress;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.InvalidLocalPartException;
import net.lukemurphey.nsia.LocalPasswordAuthentication;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.NumericalOverflowException;
import net.lukemurphey.nsia.PasswordAuthenticationValidator;
import net.lukemurphey.nsia.PasswordInvalidException;
import net.lukemurphey.nsia.UserManagement;
import net.lukemurphey.nsia.UserManagement.UserDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;

public class ApiUserManagement extends ApiHandler {
	
	public static final String DEFAULT_HASH_ALGORITHM = "sha-512";
	public static final long DEFAULT_HASH_ITERATION_COUNT = 10000;
	
	public ApiUserManagement(Application appRes){
		super(appRes);
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
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		//	 0.2 -- Make sure the user has permission
		checkRight( sessionIdentifier, "Users.Unlock");
		
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		String requesterUserName = userDescriptor.getUserName();
		long requesterUserId =  userDescriptor.getUserID();
		
		//	 0.3 -- Make sure the user name is valid
		if( userName == null ){
			appRes.logEvent(EventLogMessage.Category.USER_NAME_NULL, new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ), new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ) );
			
			throw new InputValidationException("Username cannot be null", "username", "null");
		}
		
		if( userName.length() == 0 ){
			appRes.logEvent(EventLogMessage.Category.USER_NAME_EMPTY, new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ), new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ) );
			throw new InputValidationException("Username cannot contain 0 characters", "username", userName);
		}
		
		Pattern nameRegex = Pattern.compile(UserManagement.USERNAME_REGEX);
		Matcher matcher = nameRegex.matcher( userName );
		
		if( !matcher.matches() ){
			appRes.logEvent(EventLogMessage.Category.USER_NAME_ILLEGAL, 
					new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
					new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
					new EventLogField( FieldName.TARGET_USER_NAME, userName ) );
			throw new InputValidationException("Username contains invalid characters", "username", userName );
		}
		
		// 1 -- Unlock the account
		try {
			userManagement.clearAuthFailedCount( userName );
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.SQL_EXCEPTION, e);
			throw new GeneralizedException(e);
		} catch (NumericalOverflowException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e);
			throw new GeneralizedException(e);
		} catch (InputValidationException e) {
			appRes.logEvent(EventLogMessage.Category.USER_NAME_EMPTY,
					new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
					new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ) );
			throw new InputValidationException("Username cannot contain 0 characters", "username", userName);
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException(e);
		}
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
	public long addAccount( String sessionIdentifier, String userName, String realName, String password, String emailAddress, boolean unrestricted ) throws NoSessionException, GeneralizedException, InsufficientPermissionException, InputValidationException, UnknownHostException, InvalidLocalPartException, DisallowedOperationException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has permission
		checkRight( sessionIdentifier, "Users.Add");
		
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		String requesterUserName = userDescriptor.getUserName();
		long requesterUserId =  userDescriptor.getUserID();
		
		//	 0.2 -- Only allow unrestricted accounts to create other unrestricted accounts
		if( !userDescriptor.isUnrestricted() && unrestricted == true ){
			Application.getApplication().logEvent( EventLogMessage.Category.ACCESS_CONTROL_DENY,
					new EventLogField( FieldName.MESSAGE, "Attempt to create unrestricted account from restricted account"),
					new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
					new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId )
					);
			throw new DisallowedOperationException("Restricted users cannot create unrestricted accounts");
		}
			
		//	 0.3 -- Make sure the user name is valid
		if( userName == null ){
			appRes.logEvent(EventLogMessage.Category.USER_NAME_NULL,
					new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
					new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ) );
			throw new InputValidationException("Username cannot be null", "username", "null");
		}
		
		if( userName.length() == 0 ){
			appRes.logEvent(EventLogMessage.Category.USER_NAME_EMPTY,
					new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
					new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ) );
			throw new InputValidationException("Username cannot contain 0 characters", "username", userName);
		}
		
		Pattern nameRegex = Pattern.compile(UserManagement.USERNAME_REGEX);
		Matcher matcher = nameRegex.matcher( userName );
		
		if( !matcher.matches() ){
			appRes.logEvent(EventLogMessage.Category.USER_NAME_ILLEGAL,
					new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
					new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
					new EventLogField( FieldName.TARGET_USER_NAME, userName ) );
			
			throw new InputValidationException("Username contains invalid characters", "username", userName );
		}
		
		//	 0.4 -- Make sure the real name is valid
		if( realName == null ){
			appRes.logEvent(EventLogMessage.Category.REAL_NAME_NULL,
					new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
					new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ) );
			throw new InputValidationException("Full name cannot be null", "fullname","null" );
		}
		
		if( realName.length() == 0 ){
			appRes.logEvent(EventLogMessage.Category.REAL_NAME_ILLEGAL,
					new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
					new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
					new EventLogField( FieldName.TARGET_USER_NAME, realName ) );
			throw new InputValidationException("Full name cannot contain 0 characters", "fullname", realName );
		}
		
		Pattern realNameRegex = Pattern.compile(UserManagement.REALNAME_REGEX);
		Matcher realNameMatcher = realNameRegex.matcher( realName );
		
		if( !realNameMatcher.matches() ){
			appRes.logEvent(EventLogMessage.Category.REAL_NAME_ILLEGAL,
					new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
					new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
					new EventLogField( FieldName.TARGET_USER_NAME, realName ) );
			throw new InputValidationException("Full name contains invalid characters", "fullname",realName );
		}
		
		//	 0.5 -- Make sure the hash algorithm is valid
		//	 Will be checked when the add function is called
				
		//	 0.6 -- Make sure the email address is valid
		EmailAddress email = null;
		if( emailAddress != null && !emailAddress.isEmpty() ){
			try {
				email = EmailAddress.getByAddress( emailAddress );
			} catch (UnknownHostException e1) {
				appRes.logEvent( EventLogMessage.Category.EMAIL_UNKNOWN_HOST,
						new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
						new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
						new EventLogField( FieldName.EMAIL_ADDRESS, emailAddress ) );
				
				throw e1;
			} catch (InvalidLocalPartException e1) {
				
				appRes.logEvent( EventLogMessage.Category.EMAIL_LOCAL_PART_INVALID,
						new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
						new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
						new EventLogField( FieldName.EMAIL_ADDRESS, emailAddress ) );
				
				throw e1;
			}
		}
		
		//	1 -- Try to add the account
		try {
			
			String hashAlgorithm = appRes.getApplicationConfiguration().getHashAlgorithm();
			if( hashAlgorithm == null ){
				appRes.logEvent(EventLogMessage.Category.INTERNAL_ERROR,
						new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
						new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
						new EventLogField( FieldName.MESSAGE, "Hash algorithm set to null" ) );
				
				throw new IllegalArgumentException("Hash algorithm cannot be null");
			}else if( hashAlgorithm.length() == 0 ){
				appRes.logEvent(EventLogMessage.Category.INTERNAL_ERROR,
						new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
						new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
						new EventLogField( FieldName.MESSAGE, "Hash algorithm is an empty string" ) );
				throw new IllegalArgumentException("Hash algorithm cannot be empty");
			}
			
			long iterationCount = appRes.getApplicationConfiguration().getHashIterations();
			long newUserId = userManagement.addAccount( userName, realName, password, hashAlgorithm, iterationCount, email, unrestricted );
			
			if( newUserId > 0){
				appRes.logEvent(EventLogMessage.Category.USER_ADDED,
						new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
						new EventLogField( FieldName.TARGET_USER_ID, newUserId ),
						new EventLogField( FieldName.TARGET_USER_NAME, userName ) );
				
				return newUserId;
			}
			else{
				appRes.logEvent(EventLogMessage.Category.OPERATION_FAILED,
					new EventLogField( FieldName.OPERATION, "Add user account" ),
					new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
					new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
					new EventLogField( FieldName.TARGET_USER_ID, userName ) );
					
				return newUserId;
			}
				
				
		} catch (NoSuchAlgorithmException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException(e);
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException(e);
		} catch (InputValidationException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException(e);
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException(e);
		} catch (IllegalArgumentException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException(e);
		}
		
	}
	
	/**
	 * Change the password to a randomly created password (useful for password resets).
	 * @param sessionIdentifier
	 * @param userId
	 * @param newPasswordLength
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws InputValidationException
	 */
	public String changePasswordToRandom( String sessionIdentifier, int userId, int newPasswordLength ) throws NoSessionException, GeneralizedException, InsufficientPermissionException, InputValidationException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		//	 0.2 -- Make sure the user has permission
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		long sessionUserId = userDescriptor.getUserID();
		
		if( sessionUserId == userId ){ // Wants to change their own password
			checkRight( sessionIdentifier, "Users.UpdateOwnPassword");
		}
		else{ // Wants to change someone else's password
			checkRight( sessionIdentifier, "Users.UpdatePassword");
		}
		
		//	 0.3 -- Make sure the new password is valid		
		if( newPasswordLength == 0 ){
			
			appRes.logEvent(EventLogMessage.Category.PASSWORD_EMPTY,
					new EventLogField( FieldName.TARGET_USER_ID, userId ),
					new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
					new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ) );
			
			throw new InputValidationException("New password cannot be empty", "PasswordLength", String.valueOf( newPasswordLength ) );
		}
		
		//	 0.4 -- Make sure the user id is valid
		if( userId < 0 ){
			
			appRes.logEvent(EventLogMessage.Category.USER_ID_ILLEGAL,
					new EventLogField( FieldName.TARGET_USER_ID, userId ),
					new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
					new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ) );
			
			throw new InputValidationException( "User ID is illegal", "User ID", String.valueOf( userId ) );
		}
		
		// 	1 -- Try to change the password
		try {
			return userManagement.changePasswordToRandom( userId, newPasswordLength );
		} catch (NoSuchAlgorithmException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException(e);
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException(e);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException(e);
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException(e);
		}
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
	public boolean changePassword( String sessionIdentifier, int userId, String newPassword, String authPassword ) throws NoSessionException, GeneralizedException, InsufficientPermissionException, InputValidationException, PasswordInvalidException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		//	 0.2 -- Make sure the user has permission
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		int sessionUserId = userDescriptor.getUserID();
		
		if( sessionUserId == userId ){ // Wants to change their own password
			checkRight( sessionIdentifier, "Users.UpdateOwnPassword" );
		}
		else{ // Wants to change someone else's password
			checkRight( sessionIdentifier, "Users.UpdatePassword" );
		}
		
		//	 0.3 -- Make sure the new password is valid
		if( newPassword == null ){
			
			appRes.logEvent(EventLogMessage.Category.PASSWORD_NULL,
					new EventLogField( FieldName.TARGET_USER_ID, userId ),
					new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
					new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ) );
			
			throw new InputValidationException("New password cannot be null", "Password", "null");
		}
		
		if( newPassword.length() == 0 ){
			
			appRes.logEvent(EventLogMessage.Category.PASSWORD_EMPTY,
					new EventLogField( FieldName.TARGET_USER_ID, userId ),
					new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
					new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ) );
			
			throw new InputValidationException("New password cannot be empty", "Password", newPassword);
		}
		
		//	 0.4 -- Make sure the user id is valid
		if( userId < 0 ){
			
			appRes.logEvent(EventLogMessage.Category.PASSWORD_EMPTY,
					new EventLogField( FieldName.TARGET_USER_ID, userId ),
					new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
					new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ) );
			 
			throw new InputValidationException( "User ID is invalid", "User ID", String.valueOf( userId ) );
		}
		
		//  	1 -- Make sure the current password is correct
		LocalPasswordAuthentication localAuth = new LocalPasswordAuthentication( appRes );
		try {
			if( !localAuth.checkPassword( sessionUserId, new PasswordAuthenticationValidator( authPassword ) ) )
				throw new PasswordInvalidException();
		} catch (NoSuchAlgorithmException e1) {
			appRes.logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e1 );
			throw new GeneralizedException(e1);
		} catch (SQLException e1) {
			appRes.logExceptionEvent( EventLogMessage.Category.SQL_EXCEPTION, e1 );
			throw new GeneralizedException(e1);
		} catch (InputValidationException e1) {
			appRes.logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e1 );
			throw new GeneralizedException(e1);
		} catch (NoDatabaseConnectionException e1) {
			appRes.logExceptionEvent( EventLogMessage.Category.DATABASE_FAILURE, e1 );
			throw new GeneralizedException(e1);
		} catch (NumericalOverflowException e1) {
			appRes.logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e1 );
			throw new GeneralizedException(e1);
		} catch (NotFoundException e1) {//Current user does not appear to exist
			appRes.logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e1 );
			throw new GeneralizedException(e1);
		}
		
		// 	2 -- Try to change the password
		try {
			return userManagement.changePassword( userId, newPassword);
		} catch (NoSuchAlgorithmException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException(e);
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException(e);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException(e);
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException(e);
		}

	}
	
	/**
	 * Delete the account associated with the given user ID. Note that this method will not allow a user to delete their own account (throws a DisallowedOperationException).
	 * @param sessionIdentifier
	 * @param userId
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws DisallowedOperationException 
	 */
	public boolean deleteAccount( String sessionIdentifier, int userId ) throws NoSessionException, GeneralizedException, InsufficientPermissionException, DisallowedOperationException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		//	 0.2 -- Make sure the user has permission
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		
		//	 0.4 -- Don't allow the user to delete their own account.
		if( userDescriptor.getUserID() == userId )
			throw new DisallowedOperationException("Users are not allowed to delete their own account");
		
		//	 0.5 -- Get to make sure that the user has permission
		checkRight( sessionIdentifier, "Users.Delete" );
		
		// 1 -- Perform the operation
		try{
			if( userManagement.deleteAccount( userId ) ){
				appRes.logEvent( EventLogMessage.Category.USER_DELETED,
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
						new EventLogField( FieldName.TARGET_USER_ID, userId ));
				return true;
			}
			else{
				
				appRes.logEvent( EventLogMessage.Category.USER_ID_INVALID,
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
						new EventLogField( FieldName.TARGET_USER_ID, userId ));
				
				return true;
			}
		}catch (SQLException e){
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException(e);
		}catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException(e);
		}catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException(e);
		}
	}
	
	/**
	 * Disable the account associated with the given user ID. Note that this method will not allow a user to disable their own account (throws a DisallowedOperationException).
	 * @param sessionIdentifier
	 * @param userId
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws DisallowedOperationException 
	 */
	public boolean disableAccount( String sessionIdentifier, int userId ) throws NoSessionException, GeneralizedException, InsufficientPermissionException, DisallowedOperationException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		//	 0.2 -- Make sure the user has permission
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		
		//	 0.4 -- Don't allow the user to disable their own account.
		if( userDescriptor.getUserID() == userId )
			throw new DisallowedOperationException("Users are not allowed to disable their own account");
		
		//	 0.5 -- Make sure the user has permission
		checkRight( sessionIdentifier, "Users.Edit" );
		
		
		// 1 -- Perform the operation
		try{
			if( userManagement.disableAccount( userId ) ){
				
				appRes.logEvent( EventLogMessage.Category.USER_DISABLED,
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
						new EventLogField( FieldName.TARGET_USER_ID, userId ));
			
				return true;
			}
			else{
				
				appRes.logEvent( EventLogMessage.Category.USER_ID_INVALID,
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
						new EventLogField( FieldName.TARGET_USER_ID, userId ));
				
				return true;
			}
		}catch (SQLException e){
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException(e);
		}catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException(e);
		}catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException(e);
		}
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
	public boolean enableAccount( String sessionIdentifier, long userId ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		//	 0.2 -- Make sure the user has permission
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		
		checkRight( sessionIdentifier, "Users.Edit" );
		
		try{
			if( userManagement.enableAccount( userId ) ){
				
				appRes.logEvent( EventLogMessage.Category.USER_REENABLED,
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
						new EventLogField( FieldName.TARGET_USER_ID, userId ) );
				
				return true;
			}
			else{
				appRes.logEvent( EventLogMessage.Category.USER_ID_INVALID,
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
						new EventLogField( FieldName.TARGET_USER_ID, userId ) );
				
				return true;
			}
		}catch (SQLException e){
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException(e);
		}catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException(e);
		}catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException(e);
		}
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
	public UserDescriptor getUserDescriptor( String sessionIdentifier, int userId ) throws NoSessionException, GeneralizedException, InsufficientPermissionException, NotFoundException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		//	 0.2 -- Make sure the user has permission
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		
		if( userDescriptor.getUserID() != userId ) //User is getting their own information
			checkRight( sessionIdentifier, "Users.View" );
		
		//	1 -- Get the user information
		UserDescriptor queriedUserDescriptor;
		try {
			queriedUserDescriptor = userManagement.getUserDescriptor( userId );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
			throw new GeneralizedException(e);
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException(e);
		}
		
		return queriedUserDescriptor;
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
	public UserDescriptor[] getUserDescriptors( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		//	 0.2 -- Make sure the user has permission
		checkRight( sessionIdentifier, "Users.View" );
		
		//	1 -- Get the user information
		UserDescriptor[] userDescriptor;
		try {
			userDescriptor = userManagement.getUserDescriptors( );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException(e);
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException(e);
		}
		
		return userDescriptor;
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
	public UserDescriptor getUserDescriptor( String sessionIdentifier, String userName ) throws NoSessionException, GeneralizedException, InsufficientPermissionException, InputValidationException, NotFoundException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has permission
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		String requesterUserName = userDescriptor.getUserName();
		long requesterUserId = userDescriptor.getUserID();
		
		if( !requesterUserName.matches(userName) ) //User is getting their own information
			checkRight( sessionIdentifier, "Users.View" );
		
		//	 0.2 -- Make sure the user name is valid
		checkUserName( requesterUserName, requesterUserId, userName);
		
		//	1 -- Get the user information
		UserDescriptor queriedUserDescriptor;
		try {
			queriedUserDescriptor = userManagement.getUserDescriptor( userName );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException(e);
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException(e);
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException(e);
		}
		
		return queriedUserDescriptor;
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
	public long getUserId( String sessionIdentifier, String userName ) throws NoSessionException, GeneralizedException, InsufficientPermissionException, InputValidationException{
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		//	 0.2 -- Make sure the user has permission
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		String requesterUserName = userDescriptor.getUserName();
		long requesterUserId = userDescriptor.getUserID();
		
		if( !requesterUserName.matches(userName) ) //Allow a user to get their own information
			checkRight( sessionIdentifier, "Users.View" );
		
		//	 0.3 -- Make sure the user name is valid
		checkUserName( requesterUserName, requesterUserId, userName);
		
		try {
			return userManagement.getUserID( userName );
		}catch (SQLException e){
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException(e);
		}catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException(e);
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException(e);
		}
		
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
		return updateAccount ( sessionIdentifier, userId, userName, realName, emailAddress, null );
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
	public boolean updateAccount( String sessionIdentifier, int userId, boolean accountEnabled, String userName, String realName, String emailAddress) throws InsufficientPermissionException, GeneralizedException, NoSessionException, UnknownHostException, InputValidationException, InvalidLocalPartException, DisallowedOperationException{
		return updateAccount ( sessionIdentifier, userId, userName, realName, emailAddress, null, accountEnabled );
	}
	
	/**
	 * Update the account parameters for the given user. Note that the unrestricted option cannot be set unless
	 * the current user is also unrestricted. Note that unrestricted accounts are not subject to the access
	 * control lists and are thus essentially "root" accounts.
	 * @precondition The session identifier, user ID, real name and email address must be valid and legal. The user also needs to have modify rights to "Users.UpdateOwnPassword" to change their own account and "Users.UpdatePassword" modify rights to update others accounts.
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
	public boolean updateAccountEx( String sessionIdentifier, int userId, boolean accountEnabled, String userName, String realName, String emailAddress, boolean unrestricted) throws InsufficientPermissionException, GeneralizedException, NoSessionException, UnknownHostException, InputValidationException, InvalidLocalPartException, DisallowedOperationException{
		//	 0.1 -- Make sure the user has a valid session
		// Will checked in ACL checks below
		
		//	 0.2 -- Make sure the user has permission
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		long requesterUserId = userDescriptor.getUserID();
		
		if( userId != requesterUserId ) //User is getting their own information
			checkRight( sessionIdentifier, "Users.OwnAccount.Edit" );
		else
			checkRight( sessionIdentifier, "Users.Edit" );
		
		//	 0.3 -- Do not allow restricted accounts to create unrestricted accounts
		if( !userDescriptor.isUnrestricted() && unrestricted == true ){
			
			appRes.logEvent( EventLogMessage.Category.ACCESS_CONTROL_DENY,
					new EventLogField( FieldName.OPERATION, "Update account to unrestricted" ),
					new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
					new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
					new EventLogField( FieldName.TARGET_USER_ID, userId ) );
			
			throw new DisallowedOperationException("Restricted users cannot create unrestricted accounts");
		}
			
		//	 0.4 -- Make sure the real name is valid
		if( realName == null ){
			
			appRes.logEvent( EventLogMessage.Category.REAL_NAME_NULL,
					new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
					new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
					new EventLogField( FieldName.TARGET_USER_ID, userId ) );
			
			throw new InputValidationException("Full name cannot be null", "fullname", "null" );
		}
		
		if( realName.length() == 0 ){
			
			appRes.logEvent( EventLogMessage.Category.USER_ID_INVALID,
					new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
					new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
					new EventLogField( FieldName.REAL_NAME, realName ),
					new EventLogField( FieldName.TARGET_USER_ID, userId ) );
			
			throw new InputValidationException("Full name cannot contain 0 characters", "fullname", realName );
		}
		
		Pattern realNameRegex = Pattern.compile(UserManagement.REALNAME_REGEX);
		Matcher realNameMatcher = realNameRegex.matcher( realName );
		
		if( !realNameMatcher.matches() ){
			
			appRes.logEvent( EventLogMessage.Category.USER_ID_INVALID,
					new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
					new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
					new EventLogField( FieldName.REAL_NAME, realName ),
					new EventLogField( FieldName.TARGET_USER_ID, userId ) );
			
			throw new InputValidationException("Full name contains invalid characters", "fullname",realName );
		}
		
		//	 0.5 -- Make sure the email address is valid
		EmailAddress email = null;
		if( emailAddress != null && !emailAddress.isEmpty() ){
			try {
				email = EmailAddress.getByAddress( emailAddress );
			} catch (UnknownHostException e1) {
				
				appRes.logEvent( EventLogMessage.Category.EMAIL_UNKNOWN_HOST,
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
						new EventLogField( FieldName.EMAIL_ADDRESS, emailAddress ),
						new EventLogField( FieldName.TARGET_USER_ID, userId ) );
				
				throw e1;
			} catch (InvalidLocalPartException e1) {
				
				appRes.logEvent( EventLogMessage.Category.EMAIL_LOCAL_PART_INVALID,
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
						new EventLogField( FieldName.EMAIL_ADDRESS, emailAddress ),
						new EventLogField( FieldName.TARGET_USER_ID, userId ) );
				
				throw e1;
			}
		}
		
		
		//	1 -- Try to update the account
		try {
			
			String hashAlgorithm = appRes.getApplicationConfiguration().getHashAlgorithm();
			if( hashAlgorithm == null ){
				
				appRes.logEvent( EventLogMessage.Category.INTERNAL_ERROR,
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
						new EventLogField( FieldName.MESSAGE, "Hash algorithm is null" ),
						new EventLogField( FieldName.TARGET_USER_ID, userId ) );
				
				throw new IllegalArgumentException("Hash algorithm cannot be null");
			}else if( hashAlgorithm.length() == 0 ){
				
				appRes.logEvent( EventLogMessage.Category.INTERNAL_ERROR,
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
						new EventLogField( FieldName.MESSAGE, "Hash algorithm is an empty string" ),
						new EventLogField( FieldName.TARGET_USER_ID, userId ) );
				
				throw new IllegalArgumentException("Hash algorithm cannot be empty");
			}
			
			boolean updateStatus = userManagement.updateAccountEx( userId, userName, realName, email, unrestricted, accountEnabled );
			
			if( updateStatus ){
				
				appRes.logEvent( EventLogMessage.Category.USER_MODIFIED,
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
						new EventLogField( FieldName.TARGET_USER_ID, userId ) );
				
				return true;
			}
			else{
				
				appRes.logEvent( EventLogMessage.Category.USER_ID_INVALID,
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
						new EventLogField( FieldName.OPERATION, "Update user account" ),
						new EventLogField( FieldName.TARGET_USER_ID, userId ) );
				
				return false;
			}
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException(e);
		} catch (InputValidationException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException(e);
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.DATABASE_FAILURE, e );
			throw new GeneralizedException(e);
		} catch (IllegalArgumentException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException(e);
		}
	}
	
	/**
	 * Update the account parameters for the given user. Note that the unrestricted option cannot be set unless
	 * the current user is also unrestricted. Note that unrestricted accounts are not subject to the access
	 * control lists and are thus essentially "root" accounts.
	 * @precondition The session identifier, user ID, real name and email address must be valid and legal. The user also needs to have modify rights to "Users.UpdateOwnPassword" to change their own account and "Users.UpdatePassword" modify rights to update others accounts.
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
		
		return updateAccount( sessionIdentifier, userId, userName, realName, emailAddress, Boolean.valueOf(unrestricted) );
	}
	
	/**
	 * Update the account parameters for the given user. Note that the unrestricted option cannot be set unless
	 * the current user is also unrestricted. Note that unrestricted accounts are not subject to the access
	 * control lists and are thus essentially "root" accounts.
	 * @precondition The session identifier, user ID, real name and email address must be valid and legal. The user also needs to have modify rights to "Users.UpdateOwnPassword" to change their own account and "Users.UpdatePassword" modify rights to update others accounts.
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
	private boolean updateAccount( String sessionIdentifier, int userId, String userName, String realName, String emailAddress, Boolean unrestricted, boolean accountEnabled ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, UnknownHostException, InputValidationException, InvalidLocalPartException, DisallowedOperationException{
		//	 0.1 -- Make sure the user has a valid session
		// Will checked in ACL checks below
		
		//	 0.2 -- Make sure the user has permission
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		String requesterUserName = userDescriptor.getUserName();
		long requesterUserId = userDescriptor.getUserID();
		
		if( userId != requesterUserId ) //User is getting their own information
			checkRight( sessionIdentifier, "Users.UpdateOwnPassword" );
		else
			checkRight( sessionIdentifier, "Users.UpdatePassword" );
		
		//	 0.3 -- Do not allow restricted accounts to create unrestricted accounts
		if( unrestricted != null && !userDescriptor.isUnrestricted() && unrestricted.booleanValue() == true ){
			
			appRes.logEvent( EventLogMessage.Category.ACCESS_CONTROL_DENY,
					new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
					new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
					new EventLogField( FieldName.TARGET_USER_ID, userId ) );
			
			throw new DisallowedOperationException("Restricted users cannot create unrestricted accounts");
		}
			
		//	 0.4 -- Make sure the real name is valid
		if( realName == null ){
			
			appRes.logEvent( EventLogMessage.Category.REAL_NAME_NULL,
					new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
					new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
					new EventLogField( FieldName.MESSAGE, "Attempt to set real name to null" ),
					new EventLogField( FieldName.TARGET_USER_ID, userId ) );
			
			throw new InputValidationException("Full name cannot be null", "fullname","null" );
		}
		
		if( realName.length() == 0 ){
			
			appRes.logEvent( EventLogMessage.Category.REAL_NAME_ILLEGAL,
					new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
					new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
					new EventLogField( FieldName.MESSAGE, "Attempt to set real name to empty string" ),
					new EventLogField( FieldName.TARGET_USER_ID, userId ) );
			
			throw new InputValidationException("Full name cannot contain 0 characters", "fullname", realName );
		}
		
		Pattern realNameRegex = Pattern.compile(UserManagement.REALNAME_REGEX);
		Matcher realNameMatcher = realNameRegex.matcher( realName );
		
		if( !realNameMatcher.matches() ){
			
			appRes.logEvent( EventLogMessage.Category.REAL_NAME_ILLEGAL,
					new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
					new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
					new EventLogField( FieldName.REAL_NAME, realName ),
					new EventLogField( FieldName.TARGET_USER_ID, userId ) );
			
			throw new InputValidationException("Full name contains invalid characters", "fullname",realName );
		}
		
		//	 0.5 -- Make sure the email address is valid
		EmailAddress email = null;
		if( emailAddress != null && !emailAddress.isEmpty() ){
			try {
				email = EmailAddress.getByAddress( emailAddress );
			} catch (UnknownHostException e1) {
				
				appRes.logEvent( EventLogMessage.Category.EMAIL_UNKNOWN_HOST,
						new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
						new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
						new EventLogField( FieldName.TARGET_USER_ID, userId ),
						new EventLogField( FieldName.EMAIL_ADDRESS, emailAddress ) );
				
				throw e1;
			} catch (InvalidLocalPartException e1) {
				
				appRes.logEvent( EventLogMessage.Category.EMAIL_LOCAL_PART_INVALID,
						new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
						new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
						new EventLogField( FieldName.TARGET_USER_ID, userId ),
						new EventLogField( FieldName.EMAIL_ADDRESS, emailAddress ) );
				
				throw e1;
			}
		}
		
		//	1 -- Try to update the account
		try {
			
			String hashAlgorithm = appRes.getApplicationConfiguration().getHashAlgorithm();
			if( hashAlgorithm == null ){
				
				appRes.logEvent( EventLogMessage.Category.INTERNAL_ERROR,
						new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
						new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
						new EventLogField( FieldName.TARGET_USER_ID, userId ),
						new EventLogField( FieldName.MESSAGE, "Hash algorithm is null" ) );
				
				throw new IllegalArgumentException("Hash algorithm cannot be null");
			}else if( hashAlgorithm.length() == 0 ){
				
				appRes.logEvent( EventLogMessage.Category.INTERNAL_ERROR,
						new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
						new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
						new EventLogField( FieldName.TARGET_USER_ID, userId ),
						new EventLogField( FieldName.MESSAGE, "Hash algorithm is an empty string" ) );
				
				throw new IllegalArgumentException("Hash algorithm cannot be empty");
			}
			
			boolean updateStatus = false;
			
			if( unrestricted == null )
				updateStatus = userManagement.updateAccount( userId, userName, realName, email, accountEnabled );
			else
				updateStatus = userManagement.updateAccountEx( userId, userName, realName, email, unrestricted.booleanValue(), accountEnabled );
			
			if( updateStatus ){
				
				appRes.logEvent( EventLogMessage.Category.USER_MODIFIED,
						new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
						new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
						new EventLogField( FieldName.TARGET_USER_ID, userId ),
						new EventLogField( FieldName.TARGET_USER_NAME, userName ) );
				
				return true;
			}
			else{
				
				appRes.logEvent( EventLogMessage.Category.OPERATION_FAILED,
						new EventLogField( FieldName.OPERATION, "Update user account" ),
						new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
						new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
						new EventLogField( FieldName.TARGET_USER_ID, userId ) );
				
				return false;
			}
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException(e);
		} catch (InputValidationException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException(e);
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.DATABASE_FAILURE, e );
			throw new GeneralizedException(e);
		} catch (IllegalArgumentException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException(e);
		}
	}
	
	/**
	 * Update the account parameters for the given user. Note that the unrestricted option cannot be set unless
	 * the current user is also unrestricted. Note that unrestricted accounts are not subject to the access
	 * control lists and are thus essentially "root" accounts.
	 * @precondition The session identifier, user ID, real name and email address must be valid and legal. The user also needs to have modify rights to "Users.UpdateOwnPassword" to change their own account and "Users.UpdatePassword" modify rights to update others accounts.
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
	private boolean updateAccount( String sessionIdentifier, int userId, String userName, String realName, String emailAddress, Boolean unrestricted ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, UnknownHostException, InputValidationException, InvalidLocalPartException, DisallowedOperationException{
		//	 0.1 -- Make sure the user has a valid session
		// Will checked in ACL checks below
		
		//	 0.2 -- Make sure the user has permission
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		String requesterUserName = userDescriptor.getUserName();
		long requesterUserId = userDescriptor.getUserID();
		
		if( userId != requesterUserId ) //User is getting their own information
			checkRight( sessionIdentifier, "Users.UpdateOwnPassword" );
		else
			checkRight( sessionIdentifier, "Users.UpdatePassword" );
		
		//	 0.3 -- Do not allow restricted accounts to create unrestricted accounts
		if( unrestricted != null && !userDescriptor.isUnrestricted() && unrestricted.booleanValue() == true ){
			
			appRes.logEvent( EventLogMessage.Category.ACCESS_CONTROL_DENY,
					new EventLogField( FieldName.OPERATION, "update account to unrestricted"),
					new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
					new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
					new EventLogField( FieldName.TARGET_USER_ID, userId ) );
			
			throw new DisallowedOperationException("Restricted users cannot create unrestricted accounts");
		}
			
		//	 0.4 -- Make sure the real name is valid
		if( realName == null ){
			
			appRes.logEvent( EventLogMessage.Category.REAL_NAME_NULL,
					new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
					new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
					new EventLogField( FieldName.TARGET_USER_ID, userId ) );
			
			throw new InputValidationException("Full name cannot be null", "fullname", "null" );
		}
		
		if( realName.length() == 0 ){
			
			appRes.logEvent( EventLogMessage.Category.REAL_NAME_ILLEGAL,
					new EventLogField( FieldName.MESSAGE, "Real name cannot be an empty string" ),
					new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
					new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
					new EventLogField( FieldName.TARGET_USER_ID, userId ) );
			
			throw new InputValidationException("Full name cannot contain 0 characters", "fullname", realName );
		}
		
		Pattern realNameRegex = Pattern.compile(UserManagement.REALNAME_REGEX);
		Matcher realNameMatcher = realNameRegex.matcher( realName );
		
		if( !realNameMatcher.matches() ){
			
			appRes.logEvent( EventLogMessage.Category.REAL_NAME_ILLEGAL,
					new EventLogField( FieldName.REAL_NAME, realName ),
					new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
					new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
					new EventLogField( FieldName.TARGET_USER_ID, userId ));
			
			throw new InputValidationException("Full name contains invalid characters", "fullname",realName );
		}
		
		//	 0.5 -- Make sure the email address is valid
		EmailAddress email = null;
		if( emailAddress != null && !emailAddress.isEmpty()){
			try {
				email = EmailAddress.getByAddress( emailAddress );
			} catch (UnknownHostException e1) {
				
				appRes.logEvent( EventLogMessage.Category.EMAIL_UNKNOWN_HOST,
						new EventLogField( FieldName.EMAIL_ADDRESS, emailAddress ),
						new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
						new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
						new EventLogField( FieldName.TARGET_USER_ID, userId ));
				
				throw e1;
			} catch (InvalidLocalPartException e1) {
				
				appRes.logEvent( EventLogMessage.Category.EMAIL_LOCAL_PART_INVALID,
						new EventLogField( FieldName.EMAIL_ADDRESS, emailAddress ),
						new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
						new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
						new EventLogField( FieldName.TARGET_USER_ID, userId ) );
				
				throw e1;
			}
		}
		
		//	1 -- Try to update the account
		try {
			
			String hashAlgorithm = appRes.getApplicationConfiguration().getHashAlgorithm();
			if( hashAlgorithm == null ){
				
				appRes.logEvent( EventLogMessage.Category.INTERNAL_ERROR,
						new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
						new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
						new EventLogField( FieldName.MESSAGE, "Hash algorithm is null" ),
						new EventLogField( FieldName.TARGET_USER_ID, userId ) );
				
				throw new IllegalArgumentException("Hash algorithm cannot be null");
			}else if( hashAlgorithm.length() == 0 ){
				
				appRes.logEvent( EventLogMessage.Category.INTERNAL_ERROR,
						new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
						new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
						new EventLogField( FieldName.TARGET_USER_ID, userId ),
						new EventLogField( FieldName.MESSAGE, "Hash algorithm is an empty string" ) );
				
				throw new IllegalArgumentException("Hash algorithm cannot be empty");
			}
			
			boolean updateStatus = false;
			
			if( unrestricted == null )
				updateStatus = userManagement.updateAccount( userId, userName, realName, email );
			else
				updateStatus = userManagement.updateAccountEx( userId, userName, realName, email, unrestricted.booleanValue() );
			
			if( updateStatus ){
				
				appRes.logEvent( EventLogMessage.Category.USER_MODIFIED,
						new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
						new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
						new EventLogField( FieldName.TARGET_USER_ID, userId ) );
				
				return true;
			}
			else{
				
				appRes.logEvent( EventLogMessage.Category.OPERATION_FAILED,
						new EventLogField( FieldName.OPERATION, "Update user account" ),
						new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
						new EventLogField( FieldName.SOURCE_USER_ID, requesterUserId ),
						new EventLogField( FieldName.TARGET_USER_ID, userId ) );
				return false;
			}
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException(e);
		} catch (InputValidationException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException(e);
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.DATABASE_FAILURE, e );
			throw new GeneralizedException(e);
		} catch (IllegalArgumentException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException(e);
		}
	}
	
	/**
	 * This method allows a user identifier to be resolved to a user's name. This operation is allowed even if
	 * the user does not have read permissions for the user managment.
	 * @param userId
	 * @return
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 * @throws NotFoundException 
	 */
	public String resolveUserName(String sessionIdentifier, int userId) throws GeneralizedException, NoSessionException, NotFoundException{
		checkSession(sessionIdentifier);
		
		UserManagement.UserDescriptor userDesc;
		
		try{
			userDesc = userManagement.getUserDescriptor(userId);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException(e);
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException(e);
		}
		
		if( userDesc == null)
			return null;
		else
			return userDesc.getUserName();
	}
	
	/**
	 * The simple user descriptor is a subset of the complete user descriptor. This version includes only
	 * the information that is available to all users of the system and thus does not need read permissions
	 * for user management.
	 * @param sessionIdentifier
	 * @param userId
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws NotFoundException 
	 */
	public SimpleUserDescriptor getSimpleUserDescriptor( String sessionIdentifier, int userId ) throws NoSessionException, GeneralizedException, NotFoundException{
		checkSession(sessionIdentifier);
		
		UserDescriptor userDesc;
		
		try{
			userDesc = userManagement.getUserDescriptor(userId);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException(e);
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException(e);
		}
		
		if( userDesc != null){
			return new SimpleUserDescriptor(userDesc);
		}
		else
			return null;
	}
	

	public SimpleUserDescriptor[] getSimpleUserDescriptors(String sessionIdentifier) throws NoSessionException, GeneralizedException{
		UserDescriptor[] userDescriptors;
		
		try {
			userDescriptors = userManagement.getUserDescriptors( );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException(e);
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException(e);
		}
		
		SimpleUserDescriptor[] simpleUserDescs = new SimpleUserDescriptor[userDescriptors.length];
		
		for(int c =0; c < userDescriptors.length; c++){
			simpleUserDescs[c] = new SimpleUserDescriptor(userDescriptors[c]);
		}
		
		return simpleUserDescs;
	}
}
