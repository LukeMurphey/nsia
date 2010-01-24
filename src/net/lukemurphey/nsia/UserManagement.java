package net.lukemurphey.nsia;

import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.regex.*;
import java.util.*;

import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;



/**
 * The following class performs information retrieval and configuration for the user accounts.
 * @author luke
 *
 */
public class UserManagement {
	
	//Account status
	public enum AccountStatus{
		VALID_USER,
		INVALID_USER,
		DISABLED,
		ADMINISTRATIVELY_LOCKED,
		BRUTE_FORCE_LOCKED
	}
	
	private Application appRes= null;
	public static final String USERNAME_REGEX = "[-A-Z0-9a-z_ .]{0,256}";
	public static final int USERNAME_LENGTH = 32;
	
	public static final String REALNAME_REGEX = "[-A-Z0-9a-z_ ().]{0,256}";
	public static final int REALNAME_LENGTH = 128;
	
	private Pattern nameRegex = null;
	private Pattern realNameRegex = null;
	
	
	public UserManagement( Application appResources ){
		appRes = appResources;
		nameRegex = Pattern.compile(USERNAME_REGEX);
		realNameRegex = Pattern.compile(REALNAME_REGEX);
	}
	
	/**
	 * Return the account status associated with the integer.
	 * @param value
	 * @return
	 */
	private AccountStatus convertStatusFromInt( int value ){
		
		AccountStatus[] states = AccountStatus.values();
		
		for( int c = 0; c < states.length; c++ ){
			if( states[c].ordinal() == value )
				return states[c];
		}
		
		return AccountStatus.DISABLED;
	}
	
	/**
	 * Retrieves a description of the given user, or returns null if no user matches the given user ID.
	 * @precondition A database connection must be available, the userId must be greater than 0, and the user must exist.
	 * @postcondition A user descriptor will be returned if the user exists; null will be returned if the user does not
	 * @param userId The user ID
	 * @return A user descriptor will be returned if the user exists; an exception will be thrown if no user exists with the given name
	 * @throws SQLException
	 * @throws InputValidationException 
	 * @throws NoDatabaseConnectionException 
	 * @throws NotFoundException 
	 */
	public UserDescriptor getUserDescriptor( int userId ) throws SQLException, NoDatabaseConnectionException, NotFoundException{
		// 0 -- precondition check
		
		//	 0.1 -- Make sure the argument is valid
		if( userId < 1 )
			throw new IllegalArgumentException("The user ID must be greater than 0");
		
		//	 0.2 -- Determine if the class is configured in such a way to perform the query
		//if( appRes.getDatabaseConnection(ApplicationResources.DB_USER_QUERY_SUBSYSTEM) != null );
		
		// 1 -- Retrieve the user descriptor
		PreparedStatement statement = null;
		ResultSet result = null;
		Connection connection = null;
		
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.USER_QUERY);
			statement = connection.prepareStatement("Select * from Users where UserID = ?");
			statement.setInt(1,userId);
			result = statement.executeQuery();
			
			if(result.next()){
				UserDescriptor userDesc = new UserDescriptor();
				userDesc.userName = result.getString("LoginName");
				userDesc.userId = userId;
				userDesc.localPasswordHash = result.getString("PasswordHash");
				userDesc.localPasswordHashAlgorithm = result.getString("PasswordHashAlgorithm");
				userDesc.hashIterationCount = result.getInt("PasswordHashIterationCount");
				userDesc.accountStatus = convertStatusFromInt( result.getInt("AccountStatus") );
				userDesc.salt = result.getString("Salt");
				userDesc.fullname = result.getString("RealName");
				userDesc.unrestricted = result.getBoolean("Unrestricted");
				
				LocalPasswordAuthentication localAuth = new LocalPasswordAuthentication( Application.getApplication() );
				userDesc.bruteForceLocked = localAuth.isAccountBruteForceLocked( userDesc.userName );
				
				String email = result.getString("EmailAddress");
				
				if( email != null && email.length() > 0 ){
					try {
						userDesc.emailAddress = EmailAddress.getByAddress( email );
					} catch (UnknownHostException e) {
						
						if( appRes != null )
							appRes.logEvent( EventLogMessage.Category.INTERNAL_ERROR, new EventLogField( EventLogField.FieldName.MESSAGE, "Email address in database for user is invalid: Domain portion of mail address for user '" + userDesc.userName + "'/" + userId + " is invalid (" + email + ")"));
						
					} catch (InvalidLocalPartException e) {
						
						if( appRes != null )
							appRes.logEvent( EventLogMessage.Category.INTERNAL_ERROR, new EventLogField( EventLogField.FieldName.MESSAGE, "Email address in database for user is invalid: Local-part of mail address for user '" + userDesc.userName + "'/" + userId + " is invalid (" + email + ")"));
					}
				}
				
				return userDesc;
			}
			else
				throw new NotFoundException("No user exists with the given identifier");//user could not be found
		} finally {
			if (result != null )
				result.close();
			
			if (statement != null )
				statement.close();
			
			if (connection != null )
				connection.close();
		}
		
	}
	
	/**
	 * Get all user descriptors.
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	public UserDescriptor[] getUserDescriptors( ) throws SQLException, NoDatabaseConnectionException{
		// 0 -- precondition check
		
		//	 0.1 -- Determine if the class is configured in such a way to perform the query
		//if( appRes.getDatabaseConnection(ApplicationResources.DB_USER_QUERY_SUBSYSTEM) != null );
		
		// 1 -- Retrieve the user descriptor
		PreparedStatement statement = null;
		ResultSet result = null;
		Connection connection = null;
		
		
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.USER_QUERY);
			statement = connection.prepareStatement("Select * from Users");
			result = statement.executeQuery();
			
			Vector<UserDescriptor> userDescriptors = new Vector<UserDescriptor>();
			
			while( result.next() ){
				UserDescriptor userDesc = new UserDescriptor();
				userDesc.userName = result.getString("LoginName");
				userDesc.userId = result.getInt("UserID");
				userDesc.localPasswordHash = result.getString("PasswordHash");
				userDesc.localPasswordHashAlgorithm = result.getString("PasswordHashAlgorithm");
				userDesc.hashIterationCount = result.getInt("PasswordHashIterationCount");
				userDesc.accountStatus = convertStatusFromInt( result.getInt("AccountStatus") );
				userDesc.salt = result.getString("Salt");
				userDesc.unrestricted = result.getBoolean("Unrestricted");
				userDesc.fullname = result.getString("RealName");
				
				LocalPasswordAuthentication localAuth = new LocalPasswordAuthentication( Application.getApplication() );
				userDesc.bruteForceLocked = localAuth.isAccountBruteForceLocked( userDesc.userName );
				
				String email = result.getString("EmailAddress");
				
				if( email != null && email.length() > 0 ){
					try {
						userDesc.emailAddress = EmailAddress.getByAddress( email );
					} catch (UnknownHostException e) {
						
						if( appRes != null )
							appRes.logEvent( EventLogMessage.Category.INTERNAL_ERROR, new EventLogField( EventLogField.FieldName.MESSAGE, "Email address in database for user is invalid: Domain portion of mail address for user '" + userDesc.userName + "'/" + userDesc.userId + " is invalid (" + email + ")"));
						
					} catch (InvalidLocalPartException e) {
						
						if( appRes != null )
							appRes.logEvent( EventLogMessage.Category.INTERNAL_ERROR, new EventLogField( EventLogField.FieldName.MESSAGE, "Email address in database for user is invalid: Local-part of mail address for user '" + userDesc.userName + "'/" + userDesc.userId + " is invalid (" + email + ")"));
					}
				}
				
				userDescriptors.add( userDesc );
			}
			
			// 2 -- Convert the vector to an array
			UserDescriptor[] userDescriptorsArray = new UserDescriptor[userDescriptors.size()];
			for( int c = 0; c < userDescriptors.size(); c++ ){
				userDescriptorsArray[c] = (UserDescriptor)userDescriptors.get(c);
			}
			
			return userDescriptorsArray;
		} finally {
			if (result != null )
				result.close();
			
			if (statement != null )
				statement.close();
			
			if (connection != null )
				connection.close();
		}
	}
	
	/**
	 * Retrieves a description of the given user, or returns null if no user matches the given user ID.
	 * @precondition A database connection must be available, the userId must be greater than 0, and the user must exist.
	 * @postcondition A user descriptor will be returned if the user exists; null will be returned if the user does not
	 * @param userName The username
	 * @return A user descriptor will be returned if the user exists; an exception will be thrown if no user exists with the given name
	 * @throws SQLException
	 * @throws InputValidationException 
	 * @throws NoDatabaseConnectionException 
	 * @throws NotFoundException 
	 */
	public UserDescriptor getUserDescriptor( String userName ) throws SQLException, InputValidationException, NoDatabaseConnectionException, NotFoundException{
		
		// 0 -- Precondition check
		// The preconditions will be checked in the getUserDescriptor(long) and getUserId methods.
		
		// 1 -- Retrieve the user ID
		int userId = getUserID( userName );
		
		if( userId < 0 )
			return null;
		
		// 2 -- Get the user descriptor
		return getUserDescriptor( userId );
	}
	
	/**
	 * Attempts to resolve the user associated with the given username.
	 * @precondition A database connection must be available and the username must be consist of valid characters and cannot be null or empty
	 * @postcondition A user ID will be returned, or -1 if no user was found.
	 * @param userName A string that represents the username.
	 * @return User ID or -1 if the user was not found
	 * @throws SQLException
	 * @throws InputValidationException 
	 * @throws NoDatabaseConnectionException 
	 */
	public int getUserID( String userName ) throws SQLException, InputValidationException, NoDatabaseConnectionException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Username must not be null or blank
		if( userName == null || userName.length() == 0 )
			return -1;
		
		//	 0.2 -- Username must contain valid characters
		Matcher matcher = nameRegex.matcher(userName);
		if( !matcher.matches() )
			throw new InputValidationException( "The username contains invalid characters", "Username", userName);
		
		//	 0.3 -- Username must not be overly long (this makes SQL injection more difficult)
		if( userName.length() > USERNAME_LENGTH )
			throw new InputValidationException("The username contains too many characters (" + userName.length() + ")", "Username", userName);
		
		// 1 -- Check the database
		PreparedStatement statement = null;
		ResultSet result = null;
		Connection connection = null;
		
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.USER_QUERY);
			statement = connection.prepareStatement("Select * from Users where LoginName = ?");
			statement.setString(1, userName);
			result = statement.executeQuery();
			
			if( result.next() )
				return result.getInt("UserID");
			else
				return -1;
		} finally {
			if (result != null )
				result.close();
			
			if (statement != null )
				statement.close();
			
			if (connection != null )
				connection.close();
		}
	}
	
	
	/**
	 * Disable the account that corresponds to the given user ID and disable the user's currently active sessions. Note
	 * that the account will not be deleted but rather disabled.
	 * @precondition A database connection must be available, and the user ID must be valid.
	 * @postcondition The user account (if found) will be disabled and the user's sessions will be disabled.
	 * @param userId
	 * @return true if the account was successfully found and disabled
	 * @throws SQLException
	 * @throws InputValidationException 
	 * @throws NoDatabaseConnectionException 
	 */
	public boolean disableAccount( int userId ) throws SQLException, NoDatabaseConnectionException, InputValidationException{
		
		// 0 -- precondition check
		
		//	 0.1 -- Make sure the argument is valid
		if( userId < 1 )
			throw new IllegalArgumentException("The user ID must be greater than 0");
		
		//	 0.2 -- Determine if a database connection is available
		//if( appRes.getDatabaseConnection(Application.DB_USER_QUERY_SUBSYSTEM) != null );
		
		// 1 -- Set the account status to disabled
		Connection conn = appRes.getDatabaseConnection( Application.DatabaseAccessType.USER_UPDATE );
		PreparedStatement preparedStatement = null;
		
		try{
			preparedStatement = conn.prepareStatement("Update Users set AccountStatus = ? where UserID = ?");
			preparedStatement.setLong(1, UserManagement.AccountStatus.DISABLED.ordinal() );
			preparedStatement.setLong(2, userId );
			
			if( preparedStatement.executeUpdate() != 1 )
				return false;
			
			// 2 -- Disable the active sessions for the given user (set to administratively disabled)
			SessionManagement sessionManagement = new SessionManagement( appRes );
			sessionManagement.disableUserSessions( userId );
			
			return true;
		} finally {
			if (preparedStatement != null )
				preparedStatement.close();
			
			if (conn != null )
				conn.close();
		}
	}
	
	/**
	 * Enable the account that corresponds to the given user ID.
	 * @precondition A database connection must be available, and the user ID must be valid.
	 * @postcondition The user account (if found) will be enabled
	 * @param userId
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws InputValidationException
	 */
	public boolean enableAccount( long userId ) throws SQLException, NoDatabaseConnectionException, InputValidationException{
		
		// 0 -- precondition check
		
		//	 0.1 -- Make sure the argument is valid
		if( userId < 1 )
			throw new IllegalArgumentException("The user ID must be greater than 0");
		
		// 1 -- Set the account status to disabled
		Connection conn = appRes.getDatabaseConnection( Application.DatabaseAccessType.USER_UPDATE );
		PreparedStatement preparedStatement = null;
		try{
			preparedStatement = conn.prepareStatement("Update Users set AccountStatus = ? where UserID = ?");
			preparedStatement.setLong(1, UserManagement.AccountStatus.VALID_USER.ordinal() );
			preparedStatement.setLong(2, userId );
			
			if( preparedStatement.executeUpdate() != 1 )
				return false;
			
			return true;
		} finally {
			if (preparedStatement != null )
				preparedStatement.close();
			
			if (conn != null )
				conn.close();
		}
	}
	
	
	/**
	 * Delete the account that corresponds to the given user ID and disables the user's currently active sessions. Note
	 * that this function should almost never be called. Instead, the user should be disabled. Deleting the account 
	 * prevents rebiulding of audit trails corresponding to the given user.
	 * @precondition A database connection must be available, and the user ID must be valid.
	 * @postcondition The user account (if found) will be deleted and the user's sessions will be disabled.
	 * @param userId
	 * @return true if the account was successfully found and disabled
	 * @throws SQLException
	 * @throws InputValidationException 
	 * @throws NoDatabaseConnectionException 
	 */
	public boolean deleteAccount( int userId ) throws SQLException, NoDatabaseConnectionException, InputValidationException{
		
		// 0 -- precondition check
		
		//	 0.1 -- Make sure the argument is valid
		if( userId < 1 )
			throw new IllegalArgumentException("The user ID must be greater than 0");
		
		
		// 1 -- Set the account status to disabled
		Connection conn = appRes.getDatabaseConnection( Application.DatabaseAccessType.USER_UPDATE );
		PreparedStatement preparedStatement = null;
		
		try{
			preparedStatement = conn.prepareStatement("Delete from Users where UserID = ?");
			preparedStatement.setLong(1, userId );
			
			if( preparedStatement.executeUpdate() != 1 )
				return false;
			
			// 2 -- Disable the active sessions for the given user (set to administratively disabled)
			SessionManagement sessionManagement = new SessionManagement( appRes );
			sessionManagement.disableUserSessions( userId );
			
			return true;
		} finally {
			if (preparedStatement != null )
				preparedStatement.close();
			
			if (conn != null )
				conn.close();
		}
	}
	
	/**
	 * Clear the count of authentication failures. This will re-enable an account that was locked due to repeated authentication failures.
	 * @param user
	 * @throws SQLException
	 * @throws NumericalOverflowException
	 * @throws InputValidationException
	 * @throws NoDatabaseConnectionException
	 */
	public void clearAuthFailedCount( UserDescriptor user ) throws SQLException, NumericalOverflowException, InputValidationException, NoDatabaseConnectionException{
		clearAuthFailedCount(user.getUserName());
	}
	
	/**
	 * Clear the count of authentication failures. This will re-enable an account that was locked due to repeated authentication failures.
	 * @param username
	 * @throws SQLException
	 * @throws NumericalOverflowException
	 * @throws InputValidationException
	 * @throws NoDatabaseConnectionException 
	 */
	public void clearAuthFailedCount( String username ) throws SQLException, NumericalOverflowException, InputValidationException, NoDatabaseConnectionException{
		LocalPasswordAuthentication localAuth = new LocalPasswordAuthentication(appRes);
		localAuth.clearAuthenticationFailedCount( username );
	}
	
	/**
	 * Method creates a use account corresponding to the given information. The method is synchronized to prevent the creation of 
	 * two user accounts for the same username in the unlikely event that two accounts for equivalent usernames are created simultaneously.
	 * <p>
	 * An account will be created if an equivalent account does not already exist (equivalent is defined as another account with a username
	 * that is equal). Note that the usernames are case-insensitive.
	 * @param userName
	 * @param realName
	 * @param password
	 * @param emailAddress
	 * @param unrestricted
	 * @return
	 * @throws SQLException
	 * @throws NoSuchAlgorithmException
	 * @throws InputValidationException
	 * @throws NoDatabaseConnectionException
	 */
	public synchronized long addAccount( String userName, String realName, String password, EmailAddress emailAddress, boolean unrestricted ) throws SQLException, NoSuchAlgorithmException, InputValidationException, NoDatabaseConnectionException{
		String hashAlgorithm = appRes.getApplicationConfiguration().getHashAlgorithm();
		long iterationCount = appRes.getApplicationConfiguration().getHashIterations();
		
		return addAccount(userName, realName, password, hashAlgorithm, iterationCount, emailAddress, unrestricted);
	}
	
	/**
	 * Method creates a use account corresponding to the given information. The method is synchronized to prevent the creation of 
	 * two user accounts for the same username in the unlikely event that two accounts for equivalent usernames are created simultaneously.
	 * <p>
	 * An account will be created if an equivalent account does not already exist (equivalent is defined as another account with a username
	 * that is equal). Note that the usernames are case-insensitive.
	 * @precondition username, realname, password, must be valid (non-null, non-empty, valid characters); hash algorithm must be an available algorithm ; hash iteration count must be greater than zero
	 * @postcondition An account will be created if an equivalent account does not already exist (equivalent is defined as another account with a username that is equal (case-insensitive))
	 * @param userName
	 * @param realName
	 * @param password
	 * @param hashAlgorithm
	 * @param hashIterationCount
	 * @param emailAddress
	 * @param unrestricted
	 * @return -1 if account must not created or the user ID if the account was created
	 * @throws SQLException
	 * @throws NoSuchAlgorithmException
	 * @throws InputValidationException
	 * @throws NoDatabaseConnectionException 
	 */
	public synchronized long addAccount( String userName, String realName, String password, String hashAlgorithm, long hashIterationCount, EmailAddress emailAddress, boolean unrestricted ) throws SQLException, NoSuchAlgorithmException, InputValidationException, NoDatabaseConnectionException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user name is valid
		if( userName == null || userName.length() == 0 )
			throw new IllegalArgumentException("Username is invalid (null)");
		
		if( userName.length() == 0 )
			throw new IllegalArgumentException("Username is invalid (empty string)");
		
		Matcher matcher = nameRegex.matcher( userName );
		if( !matcher.matches() )
			throw new InputValidationException("Username contains invalid characters", "Username", userName );
		
		//	 0.2 -- Make sure the real name is valid
		if( realName == null || realName.length() == 0 )
			throw new IllegalArgumentException("Real name is invalid (null)");
		
		if( realName.length() == 0 )
			throw new IllegalArgumentException("Real name is invalid (empty string)");
		
		Matcher realNameMatcher = realNameRegex.matcher( realName );
		
		if( !realNameMatcher.matches() )
			throw new InputValidationException("Real name contains invalid characters", "Realname", realName );
		
		//	 0.3 -- Make sure the hash algorithm is valid
		if( hashAlgorithm == null )
			throw new IllegalArgumentException("Hash algorithm is invalid (null)");
		
		if( hashAlgorithm.length() == 0 )
			throw new IllegalArgumentException("Hash algorithm is invalid (empty)");
		
		//	 0.4 -- Make sure hash iteration count is valid
		if( hashIterationCount < 1 )
			throw new InputValidationException("Hash iteration count is illegal","Hash Iteration Count", String.valueOf(hashIterationCount) );
		
		
		// 1 -- Make sure the user does not already exist
		if( getUserID( userName ) != -1 )
			return -1;
		
		
		// 2 -- Get the parameters for the account details
		Timestamp now = new Timestamp( System.currentTimeMillis() );
		String passwordHash;
		String salt;
		
		//	 2.1 -- Generate a salt
		salt = LocalPasswordAuthentication.generateSalt(32);
		
		//	 2.2 -- Get the password hash
		passwordHash = LocalPasswordAuthentication.PBKDF2( hashAlgorithm, password, salt, hashIterationCount );
		
		
		// 3 -- Add the account
		Connection conn = appRes.getDatabaseConnection( Application.DatabaseAccessType.USER_UPDATE );
		PreparedStatement preparedStatement = null;
		ResultSet keys = null;
		
		try{
			preparedStatement = conn.prepareStatement("Insert into Users (LoginName, PasswordHash, PasswordHashAlgorithm, RealName, AccountStatus, AccountCreated, PasswordLastSet, PasswordHashIterationCount, Salt, EmailAddress, Unrestricted) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS );
			
			preparedStatement.setString(1, userName );
			preparedStatement.setString(2, passwordHash );
			preparedStatement.setString(3, hashAlgorithm );
			preparedStatement.setString(4, realName );
			preparedStatement.setInt(5, AccountStatus.VALID_USER.ordinal() );
			
			preparedStatement.setTimestamp(6, now );
			preparedStatement.setTimestamp(7, now );
			preparedStatement.setLong(8, hashIterationCount );
			preparedStatement.setString(9, salt );
			
			if( emailAddress == null )
				preparedStatement.setString(10, "" );
			else
				preparedStatement.setString(10, emailAddress.toString() );
			
			preparedStatement.setBoolean(11, unrestricted );
			
			if( preparedStatement.executeUpdate() < 1 )
				return -1;
			
			// 4 -- Return the user ID
			keys = preparedStatement.getGeneratedKeys();
			if( keys.next() )
				return keys.getLong(1);
			else
				return -1;
			
		} finally {
			if (preparedStatement != null )
				preparedStatement.close();
			
			if (keys != null )
				keys.close();
			
			if (conn != null )
				conn.close();
		}
	}
	
	/**
	 * Resets the password to a new value that is randomly generated. This is useful for password resets.
	 * @precondition The userID must be valid, a database connection must be available, the new password character length must be > 7
	 * @postcondition The entry associated with the user ID will be updated to use the new password and the new password will be
	 * returned; if the update fails, then null will be returned. Note that the password will be hashed according to the current
	 * hash iteration count.
	 * @param userId
	 * @param newPasswordLength
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 * @throws NoSuchAlgorithmException
	 */
	public String changePasswordToRandom( long userId, int newPasswordLength ) throws NoDatabaseConnectionException, SQLException, InputValidationException, NoSuchAlgorithmException{
		String newPassword = LocalPasswordAuthentication.generateRandomPassword( newPasswordLength );
		
		if( changePassword( userId, newPassword ) )
			return newPassword;
		else
			return null;
	}
	
	/**
	 * Updates the password for the user associated with the given user ID. Note that the passwords will be hashed using the current 
	 * hash iteration count. If the hash count was 4000 the last time the password was set, and the hash count is 10000 now; then the
	 * password will be hashed 10000 times. Thus, this function may be called to update the user's password to reflect a change in
	 * the iteration count.  
	 * @precondition The userID must be valid, a database connection must be available, the password must be valid (not null, not blank)
	 * @postcondition The entry associated with the user ID will be updated to use te given password. Note that the password will be hashed according to the current hash iteration count. 
	 * @param userId
	 * @param newPassword
	 * @return True if the change was successful, false if not (i.e. the account could not be found)
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 * @throws NoSuchAlgorithmException
	 */
	public boolean changePassword( long userId, String newPassword ) throws NoDatabaseConnectionException, SQLException, InputValidationException, NoSuchAlgorithmException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the new password is valid
		if( newPassword == null )
			throw new IllegalArgumentException("Password is invalid (null)");
		
		if( newPassword.length() == 0 )
			throw new IllegalArgumentException("Password is invalid (empty)");
		
		//	 0.3 -- Make sure the user id is valid
		if( userId < 1 )
			throw new IllegalArgumentException("User ID is invalid (must be greater than 0)");
		
		//	 0.4 -- Make sure database is available
		Connection conn = appRes.getDatabaseConnection( Application.DatabaseAccessType.USER_UPDATE );
		if( conn == null )
			throw new NoDatabaseConnectionException();
		
		
		// 1 -- Change the password
		long hashIterationCount = appRes.getApplicationConfiguration().getHashIterations();
		
		//	 1.2 -- Get the resulting password hash
		String salt = LocalPasswordAuthentication.generateSalt(32);
		String passwordHashAlgorithm = appRes.getApplicationConfiguration().getHashAlgorithm();
		String passwordHash = LocalPasswordAuthentication.PBKDF2( passwordHashAlgorithm, newPassword, salt, hashIterationCount);
		Timestamp now = new Timestamp( System.currentTimeMillis());
		
		//	 1.3 -- Update the tuple with the given data
		PreparedStatement prepStmt = null;
		
		try{
			prepStmt = conn.prepareStatement("Update Users set PasswordHash = ?, PasswordHashAlgorithm = ?, PasswordLastSet = ?, Salt = ?, PasswordHashIterationCount = ?  where UserID = ?");
			prepStmt.setString(1, passwordHash);
			prepStmt.setString(2, passwordHashAlgorithm);
			prepStmt.setTimestamp(3, now);
			prepStmt.setString(4, salt);
			prepStmt.setLong(5, hashIterationCount);
			prepStmt.setLong(6, userId);
			
			if( prepStmt.executeUpdate() > 0 )
				return true;
			else
				return false;
		} finally {
			if (prepStmt != null )
				prepStmt.close();
			
			if (conn != null )
				conn.close();
		}
	}
	
	/**
	 * Update the information for the user that corresponds to the given user ID.
	 * @precondition The user ID must be valid, the real name must be valid
	 * @param userId
	 * @param realName
	 * @param emailAddress
	 * @return
	 * @throws InputValidationException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public boolean updateAccount( long userId, String userName, String realName, EmailAddress emailAddress ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		// 0 -- Precondition check
		
		//	 0.1 -- User ID must be valid
		if( userId < 1 )
			throw new IllegalArgumentException("User ID is invalid (must be greater than 0)");
		
		//	 0.2 -- Make sure the real name is valid
		if( realName == null || realName.length() == 0 )
			throw new IllegalArgumentException("Real name is invalid (null)");
		
		if( realName.length() == 0 )
			throw new IllegalArgumentException("Real name is invalid (empty string)");
		
		Matcher realNameMatcher = realNameRegex.matcher( realName );
		
		if( !realNameMatcher.matches() )
			throw new InputValidationException("Real name contains invalid characters", "Realname", realName );
		
		// 0.3 -- Make sure the user name is valid
			if( userName == null || userName.length() == 0 )
				throw new IllegalArgumentException("User name is invalid (null)");
			
			if( realName.length() == 0 )
				throw new IllegalArgumentException("User name is invalid (empty string)");
			
			Matcher userNameMatcher = nameRegex.matcher( userName );
			
			if( !userNameMatcher.matches() )
				throw new InputValidationException("User name contains invalid characters", "Username", userName );
		
		//	 0.4 -- Make sure database is available
		Connection conn = appRes.getDatabaseConnection( Application.DatabaseAccessType.USER_UPDATE );
		if( conn == null )
			throw new NoDatabaseConnectionException();
		
		
		// 1 -- Update the users' information
		String emailAddressStr;
		if( emailAddress == null )
			emailAddressStr = "";
		else
			emailAddressStr = emailAddress.toString();
		
		PreparedStatement statement = null;
		
		try{
			statement = conn.prepareStatement("Update Users set LoginName =?, RealName = ?, EmailAddress = ? where UserID = ?");
			statement.setString(1, userName);
			statement.setString(2, realName);
			statement.setString(3, emailAddressStr);
			statement.setLong(4, userId);
			
			if( statement.executeUpdate() < 1 )
				return false;
			else
				return true;
		} finally {			
			if (statement != null )
				statement.close();
			
			if (conn != null )
				conn.close();
		}
	}
	
	/**
	 * Update the information for the user that corresponds to the given user ID.
	 * @precondition The user ID must be valid, the real name must be valid
	 * @param userId
	 * @param realName
	 * @param emailAddress
	 * @param accountEnabled
	 * @return
	 * @throws InputValidationException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public boolean updateAccount( long userId, String userName, String realName, EmailAddress emailAddress, boolean accountEnabled ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		// 0 -- Precondition check
		
		//	 0.1 -- User ID must be valid
		if( userId < 1 )
			throw new IllegalArgumentException("User ID is invalid (must be greater than 0)");
		
		//	 0.2 -- Make sure the real name is valid
		if( realName == null || realName.length() == 0 )
			throw new IllegalArgumentException("Real name is invalid (null)");
		
		if( realName.length() == 0 )
			throw new IllegalArgumentException("Real name is invalid (empty string)");
		
		Matcher realNameMatcher = realNameRegex.matcher( realName );
		
		if( !realNameMatcher.matches() )
			throw new InputValidationException("Real name contains invalid characters", "Realname", realName );
		
		// 0.3 -- Make sure the user name is valid
			if( userName == null || userName.length() == 0 )
				throw new IllegalArgumentException("User name is invalid (null)");
			
			if( realName.length() == 0 )
				throw new IllegalArgumentException("User name is invalid (empty string)");
			
			Matcher userNameMatcher = nameRegex.matcher( userName );
			
			if( !userNameMatcher.matches() )
				throw new InputValidationException("User name contains invalid characters", "Username", userName );
		
		//	 0.4 -- Make sure database is available
		Connection conn = appRes.getDatabaseConnection( Application.DatabaseAccessType.USER_UPDATE );
		if( conn == null )
			throw new NoDatabaseConnectionException();
		
		
		// 1 -- Update the users' information
		String emailAddressStr;
		if( emailAddress == null )
			emailAddressStr = "";
		else
			emailAddressStr = emailAddress.toString();
		
		PreparedStatement statement = null;
		
		try{
			statement = conn.prepareStatement("Update Users set LoginName =?, RealName = ?, EmailAddress = ?, AccountStatus = ? where UserID = ?");
			statement.setString(1, userName);
			statement.setString(2, realName);
			statement.setString(3, emailAddressStr);
			statement.setLong(5, userId);
			
			if( accountEnabled == false )
				statement.setLong(4, UserManagement.AccountStatus.DISABLED.ordinal() );
			else
				statement.setLong(4, UserManagement.AccountStatus.VALID_USER.ordinal() );
			
			if( statement.executeUpdate() < 1 )
				return false;
			else
				return true;
		} finally {			
			if (statement != null )
				statement.close();
			
			if (conn != null )
				conn.close();
		}
	}
	
	/**
	 * Update the information for the user that corresponds to the given user ID.
	 * @precondition The user ID must be valid, the real name must be valid
	 * @param userId
	 * @param realName
	 * @param emailAddress
	 * @param unrestricted
	 * @param accountEnabled
	 * @return
	 * @throws InputValidationException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public boolean updateAccountEx( long userId, String userName, String realName, EmailAddress emailAddress, boolean unrestricted, boolean accountEnabled ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		// 0 -- Precondition check
		
		//	 0.1 -- User ID must be valid
		if( userId < 1 )
			throw new IllegalArgumentException("User ID is invalid (must be greater than 0)");
		
		//	 0.2 -- Make sure the real name is valid
		if( realName == null || realName.length() == 0 )
			throw new IllegalArgumentException("Real name is invalid (null)");
		
		if( realName.length() == 0 )
			throw new IllegalArgumentException("Real name is invalid (empty string)");
		
		Matcher realNameMatcher = realNameRegex.matcher( realName );
		
		if( !realNameMatcher.matches() )
			throw new InputValidationException("Real name contains invalid characters", "Realname", realName );
		
		// 0.3 -- Make sure the user name is valid
			if( userName == null || userName.length() == 0 )
				throw new IllegalArgumentException("User name is invalid (null)");
			
			if( realName.length() == 0 )
				throw new IllegalArgumentException("User name is invalid (empty string)");
			
			Matcher userNameMatcher = nameRegex.matcher( userName );
			
			if( !userNameMatcher.matches() )
				throw new InputValidationException("User name contains invalid characters", "Username", userName );
		
		//	 0.4 -- Make sure database is available
		Connection conn = appRes.getDatabaseConnection( Application.DatabaseAccessType.USER_UPDATE );
		if( conn == null )
			throw new NoDatabaseConnectionException();
		
		
		// 1 -- Update the users' information
		String emailAddressStr;
		if( emailAddress == null )
			emailAddressStr = "";
		else
			emailAddressStr = emailAddress.toString();
		
		PreparedStatement statement = null;
		
		try{
			statement = conn.prepareStatement("Update Users set LoginName =?, RealName = ?, EmailAddress = ?, Unrestricted = ?, AccountStatus = ? where UserID = ?");
			statement.setString(1, userName);
			statement.setString(2, realName);
			statement.setString(3, emailAddressStr);
			statement.setBoolean(4, unrestricted);
			statement.setLong(6, userId);
			
			if( accountEnabled == false )
				statement.setLong(5, UserManagement.AccountStatus.DISABLED.ordinal() );
			else
				statement.setLong(5, UserManagement.AccountStatus.VALID_USER.ordinal() );
			
			if( statement.executeUpdate() < 1 )
				return false;
			else
				return true;
		} finally {			
			if (statement != null )
				statement.close();
			
			if (conn != null )
				conn.close();
		}
	}
	
	/**
	 * Update the information for the user that corresponds to the given user ID.
	 * @precondition The user ID must be valid, the real name must be valid
	 * @param userId
	 * @param realName
	 * @param emailAddress
	 * @param unrestricted
	 * @return
	 * @throws InputValidationException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public boolean updateAccountEx( long userId, String userName, String realName, EmailAddress emailAddress, boolean unrestricted ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		// 0 -- Precondition check
		
		//	 0.1 -- User ID must be valid
		if( userId < 1 )
			throw new IllegalArgumentException("User ID is invalid (must be greater than 0)");
		
		//	 0.2 -- Make sure the real name is valid
		if( realName == null || realName.length() == 0 )
			throw new IllegalArgumentException("Real name is invalid (null)");
		
		if( realName.length() == 0 )
			throw new IllegalArgumentException("Real name is invalid (empty string)");
		
		Matcher realNameMatcher = realNameRegex.matcher( realName );
		
		if( !realNameMatcher.matches() )
			throw new InputValidationException("Real name contains invalid characters", "Realname", realName );
		
		// 0.3 -- Make sure the user name is valid
			if( userName == null || userName.length() == 0 )
				throw new IllegalArgumentException("User name is invalid (null)");
			
			if( realName.length() == 0 )
				throw new IllegalArgumentException("User name is invalid (empty string)");
			
			Matcher userNameMatcher = nameRegex.matcher( userName );
			
			if( !userNameMatcher.matches() )
				throw new InputValidationException("User name contains invalid characters", "Username", userName );
		
		//	 0.4 -- Make sure database is available
		Connection conn = appRes.getDatabaseConnection( Application.DatabaseAccessType.USER_UPDATE );
		if( conn == null )
			throw new NoDatabaseConnectionException();
		
		
		// 1 -- Update the users' information
		String emailAddressStr;
		if( emailAddress == null )
			emailAddressStr = "";
		else
			emailAddressStr = emailAddress.toString();
		
		PreparedStatement statement = null;
		
		try{
			statement = conn.prepareStatement("Update Users set LoginName =?, RealName = ?, EmailAddress = ?, Unrestricted = ? where UserID = ?");
			statement.setString(1, userName);
			statement.setString(2, realName);
			statement.setString(3, emailAddressStr);
			statement.setBoolean(4, unrestricted);
			statement.setLong(5, userId);
			
			if( statement.executeUpdate() < 1 )
				return false;
			else
				return true;
		} finally {			
			if (statement != null )
				statement.close();
			
			if (conn != null )
				conn.close();
		}
	}
	
	
	/**
	 * The user descriptor class represents the information for a given user as it exists in the stored location (most likely persisted in a database). 
	 * @author luke
	 *
	 */
	public static class UserDescriptor{
		private String userName;
		private String localPasswordHash;
		private String localPasswordHashAlgorithm;
		private String salt;
		private int hashIterationCount = -1;
		private int userId = -1;
		private AccountStatus accountStatus = AccountStatus.INVALID_USER;
		private EmailAddress emailAddress;
		private boolean unrestricted = false;
		private String fullname;
		private boolean bruteForceLocked = false;

		
		public String getUserName(){
			return userName;
		}
		
		public String getPasswordHash(){
			return localPasswordHash;
		}
		
		public String getPasswordHashAlgorithm(){
			return localPasswordHashAlgorithm;
		}
		
		public int getUserID(){
			return userId;
		}
		
		public AccountStatus getAccountStatus(){
			return accountStatus;
		}
		
		public String getFullname(){
			return fullname;
		}
		
		public int getPasswordHashIterationCount(){
			return hashIterationCount;
		}
		
		public String getPasswordHashSalt(){
			return salt;
		}
		
		public EmailAddress getEmailAddress(){
			return emailAddress;
		}
		
		public boolean isUnrestricted(){
			return unrestricted;
		}
		
		public boolean isBruteForceLocked(){
			return bruteForceLocked;
		}
		
		public String toString(){
			return getUserName();
		}
	}
	
}
