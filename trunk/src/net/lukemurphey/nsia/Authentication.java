package net.lukemurphey.nsia;

import java.sql.*;
import java.sql.Timestamp;
import java.security.*;
import java.util.regex.*;

/**
 * The authentication class is an abstract class for the implementation for various types authentication systems (RADIUS, 
 * Active Directory, LDAP, etc.). The class includes the getAuthenticationCount method which determines if a username
 * has been attempted too many times.
 * @author luke
 *
 */
public abstract class Authentication {
	
	protected Application appRes= null;
	
	public Authentication( Application appResources ){
		appRes = appResources;
	}
	
	/**
	 * The following function is an abstract method that must be implemented by classes that perform authentication.
	 * @precondition None
	 * @postcondition None
	 * @param userName The login name
	 * @param validator The users' token that validates the user (password, two-factor passcode, etc.)
	 * @param remoteAddress The address of the system that is initiating the connection (included only for audit purposes)
	 */
	public AuthenticationResult authenticate( String userName, AuthenticationValidator validator, ClientData clientData ){
		/* Java will not allow an abstract method to be overridden by a concrete class that includes an argument that is a sub-class of the super-class argument (the AuthenticationValidator).
		 This class was implemented with a non-abstract constructor for this reason. */
		return null;
	}
	
	/**
	 * This method increments the number of authentication attempts for the given username. Functions performing authentication 
	 * should be using method instead of their own methods since race conditions could exist that allow a thread to overwrite
	 * the count after another thread has updated the value. Thus, the system could otherwise allow too many authentication
	 * attempts. The synchronized method below will prevent this situation from occurring. 
	 * @precondition The username must consist of valid characters, not be null or empty; the time to aggregate the login attempts must be > zero
	 * @postcondition The authentication attempt list will indicate that the username attempted to authenticate and failed
	 * @param userName The username associated with the failed authentication attempt
	 * @param aggregateAttemptsTimeSeconds The amount of time that defines the "sliding window" that authentication attempts are added together and considered a brute force attack.  
	 * @throws SQLException 
	 * @throws NumericalOverflowException 
	 * @throws InputValidationException 
	 * @throws NoDatabaseConnectionException 
	 */
	protected synchronized void incrementAuthenticationFailedCount( String userName, long aggregateAttemptsTimeSeconds ) throws SQLException, NumericalOverflowException, InputValidationException, NoDatabaseConnectionException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Username cannot be null
		if( userName == null )
			return;
		
		//	 0.2 -- Username cannot be empty
		if( userName.length() < 1 )
			return;
		
		//	 0.3 -- Username must contain valid characters
		Pattern pattern = Pattern.compile(UserManagement.USERNAME_REGEX);
		Matcher matcher = pattern.matcher(userName);
		
		if( !matcher.matches() )
			throw new InputValidationException( "The username contains invalid characters", "Username", userName);
		
		//	 0.4 -- Username must not be overly long (this makes SQL injection more difficult)
		if( userName.length() > UserManagement.USERNAME_LENGTH )
			throw new InputValidationException("The username contains too many characters (" + userName.length() + ")", "Username", userName);
		
		
		// 1 -- Get the current authentication failed count
		Connection conn = null;
		
		PreparedStatement statementAttempts = null;
		PreparedStatement statementAddAuthFailed = null;
		ResultSet result = null;
		PreparedStatement increaseAttempts = null;
		
		try{
			conn = appRes.getDatabaseConnection( Application.DatabaseAccessType.USER_QUERY );
			statementAttempts = conn.prepareStatement("Select * from AttemptedLogins where Lower(LoginName) = ?");
			
			statementAttempts.setString(1, userName.toLowerCase());
			result = statementAttempts.executeQuery();
			
			// 2 -- Update the count of attempted authentications 
			if( !result.next() ){
				statementAddAuthFailed = conn.prepareStatement("Insert into AttemptedLogins (LoginName, FirstAttempted, Attempts) values(?, ?, 1)");
				statementAddAuthFailed.setString(1, userName);
				Timestamp now = new Timestamp( System.currentTimeMillis());
				statementAddAuthFailed.setString(2, now.toString());
				
				statementAddAuthFailed.executeUpdate();
			}
			else{
				long count = result.getLong("Attempts");
				Timestamp firstAttempt = result.getTimestamp("FirstAttempted");
				
				// 	1.1 -- If the previous attempts are expired, then reset the count			
				Timestamp now = new Timestamp( System.currentTimeMillis() );
				
				if( (now.getTime() - firstAttempt.getTime()) > (aggregateAttemptsTimeSeconds * 1000) ){
					// Reset the previous count
					removeOldLoginAttemptEntries( aggregateAttemptsTimeSeconds );
				}
				
				//	1.2 -- Set the incremented value if the attempt should still be aggregated
				else{
					int attemptNameID = result.getInt("AttemptedNameID");
					increaseAttempts = conn.prepareStatement("Update AttemptedLogins set Attempts = ? where AttemptedNameID = ?");
					
					if( !NumericalOverflowAnalysis.assertSafeIncrement( count ) )
						throw new NumericalOverflowException("Attempt to increment failed login count overflowed for username: " + userName, "Failed Login Attempt Count");
					
					increaseAttempts.setLong(1, count+1);
					increaseAttempts.setInt(2, attemptNameID );
					
					increaseAttempts.executeUpdate();
				}
			}
		} finally {
			if (result != null )
				result.close();
			
			if (statementAttempts != null )
				statementAttempts.close();
			
			if (statementAddAuthFailed != null )
				statementAddAuthFailed.close();
			
			if (increaseAttempts != null )
				increaseAttempts.close();
			
			if (conn != null )
				conn.close();
		}
		
	}
	
	/**
	 * This method clears the authentication failed count. This method is typically called whenever:
	 * <ul><li>An administrator choses to re-enable an account after it is locked via too many attempts</li>
	 * <li>A user successfully authenticates after using the wrong password (failure to clear the failed
	 * counts means the failures accumulate over time)</li></ul>
	 * @param userName
	 * @throws SQLException
	 * @throws NumericalOverflowException
	 * @throws InputValidationException
	 * @throws NoDatabaseConnectionException 
	 */
	protected synchronized void clearAuthenticationFailedCount( String userName ) throws SQLException, NumericalOverflowException, InputValidationException, NoDatabaseConnectionException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Username cannot be null
		if( userName == null )
			return;
		
		//	 0.2 -- Username cannot be empty
		if( userName.length() < 1 )
			return;
		
		//	 0.3 -- Username must contain valid characters
		Pattern pattern = Pattern.compile(UserManagement.USERNAME_REGEX);
		Matcher matcher = pattern.matcher(userName);
		
		if( !matcher.matches() )
			throw new InputValidationException( "The username contains invalid characters", "Username", userName);
		
		//	 0.4 -- Username must not be overly long (this makes SQL injection more difficult)
		if( userName.length() > UserManagement.USERNAME_LENGTH )
			throw new InputValidationException("The username contains too many characters (" + userName.length() + ")", "Username", userName);
		
		
		// 1 -- Get the current authentication failed count
		Connection conn = null;
		PreparedStatement statementAttempts = null;
		
		try{
			conn = appRes.getDatabaseConnection( Application.DatabaseAccessType.USER_QUERY );
			statementAttempts = conn.prepareStatement("Delete from AttemptedLogins where Lower(LoginName) = ?");
			
			statementAttempts.setString(1, userName.toLowerCase());
			statementAttempts.execute();
		} finally {
			if (statementAttempts != null )
				statementAttempts.close();
			
			if (conn != null )
				conn.close();
		}
	}
	
	
	/**
	 * Retrieve the number of times that a given username was used. The authentication tracks logins even if they are not actually valid accounts. This 
	 * prevents authentication attempts from revealing usernames while allowing the system to tell valid users that their account is locked. This is
	 * intended to strike a balance between usability and security without compromising either. Furthermore, the system may trick attackers into
	 * believing invalid accounts are actually valid.
	 * <p>
	 * The system can be configured to block an account using the following schemes:
	 * <ol>
	 * <li>When the attempt threshold has been reached, lock the account until N seconds has passed since the first failed attempt. This 
	 * works to throttle the attempts since only a certain number of attempts is allowed within N seconds.</li>
	 * <li>When the attempt threshold has been reached, lock the account until N seconds has passed since the last failed attempt.</li>
	 * <li>Block the account perminantly when the threshold has been reached.</li>
	 * </ol>
	 * @precondition The database connection must be available and the time to aggregate authentication attempts must be non-zero
	 * @postcondition The number of failed authentication attempts for the username will be returned
	 * @param userName The username associated with the failed authentication attempt
	 * @param aggregateAttemptsTimeSeconds The amount of time that defines the "sliding window" that authentication attempts are added together and considered a brute force attack.
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	protected long getAuthenticationFailedCount( String userName, long aggregateAttemptsTimeSeconds ) throws SQLException, NoDatabaseConnectionException{
		// 0 -- Precondition check
		
		//	 0.1 -- Is user name valid
		if( userName == null )
			return -1;
		
		//	 0.2 -- Is the time to aggregate authentication attempts valid
		if( aggregateAttemptsTimeSeconds <= 0 )
			throw new IllegalArgumentException("The time to aggregate login attempts must be non-zero.");
		
		// 1 -- Purge the old entries
		removeOldLoginAttemptEntries( aggregateAttemptsTimeSeconds );
		
		
		// 2 -- Get the failed attempt count
		PreparedStatement statementAttempts = null;
		ResultSet result = null;
		Connection conn = null;
		try{
			conn = appRes.getDatabaseConnection(Application.DatabaseAccessType.USER_QUERY);
			statementAttempts = conn.prepareStatement("Select * from AttemptedLogins where Lower(LoginName) = ?");
			
			statementAttempts.setString(1, userName.toLowerCase());
			result = statementAttempts.executeQuery();
			
			if( result.next() ){
				long count = result.getLong("Attempts");
				Timestamp firstAttempt = result.getTimestamp("FirstAttempted"); //Note: this parameter will be null if the value is null
				
				Timestamp now = new Timestamp( System.currentTimeMillis() );
				
				if( (now.getTime() - firstAttempt.getTime()) < (aggregateAttemptsTimeSeconds * 1000) )
					return count;
				else
					return 0;
			}
			else
				return 0;
			
		} finally {
			if (result != null )
				result.close();
			
			if (statementAttempts != null )
				statementAttempts.close();
			
			if (conn != null )
				conn.close();
		}
		
	}
	
	
	/**
	 * Removes login attempt entries that are outside of the given window. All entries outside of the given aggregate window will
	 * be removed to save space in the database.
	 * @precondition A valid database connection must exist
	 * @postcondition The login attempt tuples outside of the sliding window will be deleted.
	 * @param aggregateAttemptsTimeSeconds The amount of time that defines the "sliding window" that authentication attempts are added together and considered a brute force attack.
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	private void removeOldLoginAttemptEntries( long aggregateAttemptsTimeSeconds ) throws SQLException, NoDatabaseConnectionException{
		
		// 0 -- Precondition check
		if( aggregateAttemptsTimeSeconds <= 0 )
			throw new IllegalArgumentException("The time to aggregate login attempts must be non-zero.");
		
		// 1 -- Delete aged entries
		Timestamp old = new Timestamp( System.currentTimeMillis() - (aggregateAttemptsTimeSeconds * 1000) );
		Connection conn = null;
		
		PreparedStatement statement = null;
		
		try{
			conn = appRes.getDatabaseConnection(Application.DatabaseAccessType.USER_QUERY);
			statement = conn.prepareStatement("Delete from AttemptedLogins where FirstAttempted < ?");
			statement.setTimestamp(1, old);
			statement.executeUpdate();
			
		} finally {
			
			if (statement != null )
				statement.close();
			
			if (conn != null )
				conn.close();
		}
		
	}
	
	/**
	 * This class represents a successful login event and provides information relevant to a login such as the
	 * location (IP address) where the login occurred from, the date and the identifier of user who logged in.   
	 * @author luke
	 *
	 */
	public static class LoginRecord{
		private Timestamp loginDate;
		private String loginLocation;
		private long userID;
		
		protected LoginRecord(Timestamp date, String fromLocation, long userID ){
			loginDate = date;
			loginLocation = fromLocation;
			this.userID = userID;
		}
		
		public Timestamp getLoginDate(){
			return loginDate;
		}
		
		public long getUserID(){
			return userID;
		}
		
		public String getLoginLocation(){
			return loginLocation;
		}
	}
	
	/**
	 * The following method pauses for delay based on a random value. This is intended to reduce the chance of 
	 * a side channel attack.
	 * @throws NoSuchAlgorithmException 
	 * @precondition None
	 * @postcondition The function will return after a random delay
	 */
	protected void randomWait() throws NoSuchAlgorithmException{
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		long randInt = random.nextInt(2000);//Maximum delay is two seconds
		try {
			Thread.sleep(randInt);
		} catch (InterruptedException e) {
			/*The interrupted exception is ignored for two reasons:
			 * 1) This exception should probably be unchecked anyways since it is only possible when a call to interrupt() occurs.
			 *    This application does not call interrupt() and the JRE does not call it directly either.
			 * 2) The function is only intended to cause a short delay. An interruption is non-critical.*/ 
		}
	}
	
	/**
	 * This method introduces a pause that prevents side-channel attacks from being effective. The 
	 * method will sleep to ensure that the method does not return before the start time plus the
	 * seconds specified.
	 * @param startTime
	 * @param secondsDelay
	 */
	protected void timedDelay( long startTime, long secondsDelay ){
		long endTime = startTime + (secondsDelay*1000);
		
		long diff = endTime - System.currentTimeMillis();
		//System.out.println( diff + " Milli-Seconds"); //Debug
		
		if( diff > 0 ){
			try {
				Thread.sleep(diff);
			} catch (InterruptedException e) {
				/*The interrupted exception is ignored for two reasons:
				 * 1) This exception should probably be unchecked anyways since it is only possible when a call to interrupt() occurs.
				 *    This application does not call interrupt() and the JRE does not call it directly either.
				 * 2) The function is only intended to cause a short delay. An interruption is non-critical.*/ 
			}
		}
		
		
	}
	
	/**
	 * This class represents the result of an authentication attempt.
	 * @author luke
	 *
	 */
	public static class AuthenticationResult {
		public static final int AUTH_SUCCESS = 0;
		public static final int AUTH_FAILED = 1;
		public static final int AUTH_INVALID_PASSWORD = 2;
		public static final int AUTH_INVALID_USER = 3;
		public static final int AUTH_ACCOUNT_DISABLED = 4;
		public static final int AUTH_ACCOUNT_ADMINISTRATIVELY_LOCKED = 5;
		public static final int AUTH_ACCOUNT_BRUTE_FORCE_LOCKED = 6;
		
		private int authResultCode = -1;
		private int trackingNumber = -1;
		private String sessionId = null;
		
		/**
		 * The constructor is not public so that only the authentication derived classes can instantiate authentication results.
		 * @param authenticationResultCode
		 * @param sessionIdentifier
		 */
		protected AuthenticationResult( int authenticationResultCode, String sessionIdentifier ){
			authResultCode = authenticationResultCode;
			sessionId = sessionIdentifier;
		}
		
		/**
		 * The constructor is not public so that only the authentication derived classes can instantiate authentication results.
		 * @param authenticationResultCode
		 * @param sessionIdentifier
		 * @param trackingNumber
		 */
		protected AuthenticationResult( int authenticationResultCode, String sessionIdentifier, int trackingNumber ){
			authResultCode = authenticationResultCode;
			sessionId = sessionIdentifier;
			this.trackingNumber = trackingNumber;
		}
		
		/**
		 * Get the session identifier that was returned due to a successful authentication attempt.
		 * @precondition The authentication attempt must be successful or null will be returned.
		 * @postcondition A session identifier will be returned or null if the attempt failed.  
		 * @return A session identifier will be returned or null if the attempt failed.
		 */
		public String getSessionIdentifier(){
			return sessionId;
		}
		
		/**
		 * Get the tracking number associated with the session. The tracking number stays the same during the duration of the session though the session identifier may change.
		 * @return
		 */
		public int getTrackingNumber(){
			return trackingNumber;
		}
		
		/**
		 * Get the authentication result code.
		 * @precondition None
		 * @postcondition An authentication result code will be returned.
		 * @return
		 */
		public int getAuthenticationStatus(){
			return authResultCode;
		}
		
	}
	
}
