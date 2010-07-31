package net.lukemurphey.nsia;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;

import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;

import org.apache.commons.codec.binary.Hex;

/**
 * This class represents the authenticator for local password authentication against the SiteSentry user
 * database. Security is enhanced by implementing a PBKDF2 algorithm per RFC2898. Specific security enhancements
 * include:
 * <ul>
 *  <li>Passwords are stored in a hashed state</li>
 *  <li>Passwords are processed using a salt</li>
 *  <li>Passwords are hashed numerous times to frustrate brute attacks against the passwords hashes</li>
 *  <li>The database account used by the authentication system is restricted in that it does not allow the authentication 
 *  system to write to the users table and thus prevents successful SQL injection attacks from adding accounts</li>
 *  <li>The system fails closed</li>
 *  <li>The username is filtered to ensure that only valid characters are allowed, the password is not filtered since 
 *  this would reduce the number of characters possible in the password. However, passwords are not included in the SQL
 *  queries to look up a user and is thus exempt from SQL injection attacks</li>
 *  <li>SQL is processed using prepared statements to prevent injection attacks</li>
 *  <li>A arbitrary and random time delay is inserted into the function to prevent side channel attacks</li>
 * </ul>
 * @author luke
 *
 */

public class LocalPasswordAuthentication extends Authentication{

	private static long DEFAULT_AUTHENTICATION_ATTEMPT_LIMIT = 4;
	private static long DEFAULT_AUTHENTICATION_AGGREGATION_PERIOD_SECONDS = 3600;//60 minutes
	private static long SECONDS_AUTHENTICATION_DELAY = 1; //Minimum delay between any authentication attempt and the time it completes
	
	public LocalPasswordAuthentication(Application appResources) {
		super(appResources);
	}

	/**
	 * This method attempts to perform authentication to the local password database.
	 * @precondition The userName must not null or blank and must consist of valid characters.
	 * @postcondition An authentication result will be returned if successful, a null ClientData instance will result in less auditing information logging
	 * @param userName
	 * @param validator
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws SQLException
	 * @throws InputValidationException
	 * @throws NoDatabaseConnectionException
	 * @throws NumericalOverflowException
	 * @throws NotFoundException 
	 */
	public AuthenticationResult authenticate( String userName, PasswordAuthenticationValidator validator, ClientData clientData ) throws NoSuchAlgorithmException, SQLException, InputValidationException, NoDatabaseConnectionException, NumericalOverflowException {
		
		// 0 -- Precondition check
		//The preconditions for the username will be checked by the UserManagement class 
		
		// 1 -- Determine if the user is authorized
		UserManagement userControl = new UserManagement(appRes);
		UserManagement.UserDescriptor userDescriptor;
		
		try{
			userDescriptor = userControl.getUserDescriptor( userName );
		}
		catch(NotFoundException e){
			return new AuthenticationResult( AuthenticationResult.AUTH_INVALID_USER, null);
		}
		
		//	 1.1 -- Determine if the user is unknown
		//randomWait(); //This method introduces a slight delay in the authentication system to reduce the chance of a side-channel timing attack
		long startTime = System.currentTimeMillis();
		
		if( userDescriptor == null ){
			
			/* Increase the login attempt count to facilitate account blocking.
			   Note that this is being done even though the username is invalid
			   so that the system can indicate that the account is now blocked
			   without revealing which are real accounts. */
			incrementAuthenticationFailedCount( userName, appRes.getApplicationConfiguration().getAuthenticationAttemptAggregationCount() );
			
			timedDelay( startTime, SECONDS_AUTHENTICATION_DELAY);
			return new AuthenticationResult( AuthenticationResult.AUTH_INVALID_USER, null);
		}
		
		//	 1.2 -- Check the account status and determine if the account is still active and return if it is not		
		if( userDescriptor.getAccountStatus() == UserManagement.AccountStatus.ADMINISTRATIVELY_LOCKED ){
			timedDelay( startTime, SECONDS_AUTHENTICATION_DELAY);
			return new AuthenticationResult( AuthenticationResult.AUTH_ACCOUNT_ADMINISTRATIVELY_LOCKED, null);
		}
		else if( userDescriptor.getAccountStatus() == UserManagement.AccountStatus.BRUTE_FORCE_LOCKED){
			timedDelay( startTime, SECONDS_AUTHENTICATION_DELAY);
			return new AuthenticationResult( AuthenticationResult.AUTH_ACCOUNT_BRUTE_FORCE_LOCKED, null);
		}
		else if( userDescriptor.getAccountStatus() == UserManagement.AccountStatus.DISABLED ){
			timedDelay( startTime, SECONDS_AUTHENTICATION_DELAY);
			return new AuthenticationResult( AuthenticationResult.AUTH_ACCOUNT_DISABLED, null);
		}
		
		//	 1.3 -- Determine if the user name has been blocked due to repeated authentication attempts
		boolean loginNameBlocked = isAccountBruteForceLocked( userDescriptor.getUserName() );
		
		/*long maximumLoginAttempts = appRes.getApplicationParameters().getParameter("Security.MaximumAuthenticationAttemptLimit",DEFAULT_AUTHENTICATION_ATTEMPT_LIMIT);
		long actualAttempts = getAuthenticationFailedCount(userDescriptor.getUserName(), appRes.getApplicationParameters().getParameter("Security.AuthenticationAttemptAggregationPeriod", DEFAULT_AUTHENTICATION_AGGREGATION_PERIOD_SECONDS));
		boolean loginNameBlocked = false;
		
		if( actualAttempts >= maximumLoginAttempts ){
			loginNameBlocked = true;
		}*/
		
		//	 1.4 -- Check the password		
		String attemptedPasswordHash = PBKDF2(userDescriptor.getPasswordHashAlgorithm(), validator.getPassword(), userDescriptor.getPasswordHashSalt(), userDescriptor.getPasswordHashIterationCount());
		SessionManagement sessions = new SessionManagement( appRes );
		
		if( attemptedPasswordHash.matches(userDescriptor.getPasswordHash())){
			if( loginNameBlocked ){
				timedDelay( startTime, SECONDS_AUTHENTICATION_DELAY);
				return new AuthenticationResult( AuthenticationResult.AUTH_ACCOUNT_BRUTE_FORCE_LOCKED, null );
			}else{
				
				// Update the password if the authentication parameters have changed
				if( userDescriptor.getPasswordHashIterationCount() != appRes.getApplicationConfiguration().getHashIterations() )
					userControl.changePassword(userDescriptor.getUserID(), validator.getPassword());
				
				clearAuthenticationFailedCount( userName );
				//timedDelay( startTime, SECONDS_AUTHENTICATION_DELAY);
				return new AuthenticationResult( AuthenticationResult.AUTH_SUCCESS, sessions.createSession( userDescriptor.getUserID(), clientData ));
			}
		}
		else{
			if( loginNameBlocked ){
				timedDelay( startTime, SECONDS_AUTHENTICATION_DELAY);
				return new AuthenticationResult( AuthenticationResult.AUTH_ACCOUNT_BRUTE_FORCE_LOCKED, null );
			}else{
				// Increase the login attempt count to facilitate account blocking
				incrementAuthenticationFailedCount( userDescriptor.getUserName(), appRes.getApplicationConfiguration().getAuthenticationAttemptAggregationCount() );
				timedDelay( startTime, SECONDS_AUTHENTICATION_DELAY);
				return new AuthenticationResult( AuthenticationResult.AUTH_INVALID_PASSWORD, null);
			}
		}
	}
	
	/**
	 * This method checks the password given to determine if it matches the one in the local password database. Note, that this is intended to be used for
	 * checking the password for functions such as updating the password, not for authentication.
	 * @precondition The userId must be valid.
	 * @postcondition An authentication result will be returned if successful
	 * @param userName
	 * @param validator
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws SQLException
	 * @throws InputValidationException
	 * @throws NoDatabaseConnectionException
	 * @throws NumericalOverflowException
	 * @throws NotFoundException 
	 */
	public boolean checkPassword( int userId, PasswordAuthenticationValidator validator ) throws NoSuchAlgorithmException, SQLException, InputValidationException, NoDatabaseConnectionException, NumericalOverflowException, NotFoundException {
		
		// 0 -- Precondition check
		//The preconditions for the username will be checked by the UserManagement class 
		
		// 1 -- Determine if the user is authorized
		UserManagement userControl = new UserManagement(appRes);
		UserManagement.UserDescriptor userDescriptor = userControl.getUserDescriptor( userId );
		
		//	 1.1 -- Determine if the user is unknown		
		if( userDescriptor == null ){
			return false;
		}
		
		//	 1.2 -- Check the password		
		String attemptedPasswordHash = PBKDF2(userDescriptor.getPasswordHashAlgorithm(), validator.getPassword(), userDescriptor.getPasswordHashSalt(), userDescriptor.getPasswordHashIterationCount());
		
		if( attemptedPasswordHash.matches(userDescriptor.getPasswordHash())){
			return true;
		}
		else{
			return false;
		}
	}
	
	/**
	 * Determines if the given account is locked due to repeated (and failed) authentication attempts.
	 * @param username
	 * @return
	 * @throws InputValidationException 
	 * @throws SQLException 
	 * @throws NoDatabaseConnectionException 
	 */
	public boolean isAccountBruteForceLocked(String username ) throws NoDatabaseConnectionException, SQLException{

		long maximumLoginAttempts = DEFAULT_AUTHENTICATION_ATTEMPT_LIMIT;
		
		try {
			maximumLoginAttempts = appRes.getApplicationConfiguration().getAuthenticationAttemptLimit();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
		} catch (InputValidationException e) {
			appRes.logEvent(EventLogMessage.EventType.SYSTEM_PARAMETER_NAME_ILLEGAL, new EventLogField(EventLogField.FieldName.PARAMETER, "Security.MaximumAuthenticationAttemptLimit" ));
		}
		long aggregationTime = DEFAULT_AUTHENTICATION_AGGREGATION_PERIOD_SECONDS;
		
		try{
			aggregationTime = appRes.getApplicationConfiguration().getAuthenticationAttemptAggregationCount();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
		} catch (InputValidationException e) {
			appRes.logEvent(EventLogMessage.EventType.SYSTEM_PARAMETER_NAME_ILLEGAL, new EventLogField(EventLogField.FieldName.PARAMETER, "Security.AuthenticationAttemptAggregationPeriod" ));
		}
		
		long actualAttempts = getAuthenticationFailedCount( username, aggregationTime);
		
		return actualAttempts >= maximumLoginAttempts;
	}
	
	/**
	 * Creates a string containing random characters to the length specified. Note that the length must be greater than 7.
	 * @param length
	 * @return
	 * @throws NoSuchAlgorithmException 
	 */
	public static String generateRandomPassword( int length ) throws NoSuchAlgorithmException{
		// 0 -- Precondition check
		
		if( length < 8 )
			throw new IllegalArgumentException("The length of the generated password must be greater than 7 characters");
		
		// 1 -- Generate the password
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");

		StringBuffer stringBuffer = new StringBuffer(length);
		
		// Characters possible: 48-57 [0-9], 65-90 [A-Z], 97-122 [a-z]
		for( int c = 0; c < length; c++ ){
			int value = random.nextInt(61);
			if( value < 10 )
				stringBuffer.append ( (char)(value + 48) );
			else if ( value < 36 )
				stringBuffer.append ( (char)(value + 55) );
			else
				stringBuffer.append ( (char)(value + 61) );
		}
		
		
		//byte[] chars = new byte [length];
		//random.nextBytes( chars );
		
		// 2 -- Convert the password to valid characters
		// a-z, A-Z, 0-9, !-)
		
		return stringBuffer.toString();// new String( chars );
	}
	
	/**
	 * The following is a implementation of the PBKDF2 (password-based key derivative function 2.0) as defined in RFC 2898.
	 * @see <a href="http://www.ietf.org/rfc/rfc2898.txt">RFC 2898</a>
	 * @see <a href="http://en.wikipedia.org/wiki/Key_derivation_function">Wikipedia description of PBKDF2</a>
	 * @param hashAlgorithm The hash algorithm to use (SHA512, SHA384, MD5, etc.)
	 * @param password
	 * @param salt
	 * @param iterationCount
	 * @return The hash corresponding to the hash results of the PBKDF2 
	 * @throws NoSuchAlgorithmException If the defined hash algorithm is unknown
	 */
	public static String PBKDF2( String hashAlgorithm, String password, String salt, long iterationCount ) throws NoSuchAlgorithmException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the hash algorithm is not null
		if( hashAlgorithm == null )
			throw new IllegalArgumentException("The hash algorithm cannot be null");
		
		//	 0.2 -- Make sure the iteration count is valid
		if( iterationCount < 1 )
			throw new IllegalArgumentException("The iteration count for PBKDF2 must be greater than 0");
		
		// 1 -- Generate the password hash
		MessageDigest messageDigest = MessageDigest.getInstance(hashAlgorithm);
		
		//	 1.1 -- Insert the salt and get the byte representation
		byte[] hashBytes = (password + salt).getBytes();
		
		//http://rfc.net/rfc2898.html#p9
		//	 1.2 -- Derive the password hash
		for( int c = 0; c < iterationCount; c++){
			hashBytes = messageDigest.digest(hashBytes);
		}
		
		//	 1.3 -- Return the result as a hex string representation 
		String passwordHash = new String(Hex.encodeHex(hashBytes));
		return passwordHash;
	}
	
	/**
	 * Method generates a salt string (based on SHA1) to the given length. The salt is 
	 * returned as a string of hexadecimal characters. The string is generated using a 
	 * random number generator.
	 * @precondition The length must be greater than zero, and SHA1PRNG must be available
	 * @postcondition A hex encoded representation of the salt will be returned that is unique
	 * @param length The length of the salt
	 * @return
	 * @throws NoSuchAlgorithmException 
	 * @throws NoDatabaseConnectionException 
	 * @throws InputValidationException 
	 * @throws SQLException 
	 */
	public static String generateSalt( int length ) throws NoSuchAlgorithmException, SQLException, InputValidationException, NoDatabaseConnectionException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the length is valid
		if( length == 0 )
			throw new IllegalArgumentException("Salt creation failed since the given length is invalid");
		
		// 1 -- Generate the hex encoded salt
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");

		byte[] salt = new byte [length/2];
		random.nextBytes( salt );

		
		return new String(Hex.encodeHex(salt));
	}
}
