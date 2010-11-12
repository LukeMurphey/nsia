package net.lukemurphey.nsia;


import java.security.*;
import java.sql.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.*;
import org.apache.commons.codec.binary.Hex;



/**
 * This class creates and maintains user sessions in such a way to enforce security. The XML-RPC and HTTP related connections must be maintained
 * in a secure manner; this class provides the means to maintain the connection. Security is enforced by:
 * <ul>
 *  <li>Session Timeouts: Sessions require re-authentication after a given time</li>
 * 	<li>Inactive Session Timeout: Sessions are ended after a brief period of inactivity</li>
 *  	<li>Strong Session Variables: Session variables are long and randomly generated</li>
 *  	<li>Strict SID Assignment: Session identifiers are not adopted</li>
 *  	<li>Session Sanity Checks: Sanity checks are performed to block likely malicious activity</li>
 *  	<li>Dynamic Session Identifiers: SIDs can be changed frequently during a session</li>
 *  	<li>Session Hijack Detection: Old session IDs are retained during a session; the session is terminated if an old SID is used</li>
 * </ul>
 * The session identifier is only temporarily associated with a given session. This allows the session identifier to be dynamic. To facilitate
 * this a session-unique tracking number is granted. This tracking number will identify a given session even when the session identifier changes.
 * The tracking number does not need to be random since the client cannot view it. 
 * @author luke
 *
 */
public class SessionManagement {
	private Application appRes = null;
	public static final int SESSION_IDENTIFIER_LENGTH = 64;
	public static final String SESSION_IDENTIFIER_REGEX = "[A-Fa-f0-9]*";
	private Pattern sessionIdentifierRegex = Pattern.compile(SESSION_IDENTIFIER_REGEX);
	
	public SessionManagement( Application appResources ){
		appRes = appResources;
	}
	
	/**
	 * This methods creates a session for the given user ID. This class does <i>not</i> determine if the user ID is a real user
	 * and it assumes that the user is authorized. The classes that perform validation must ensure that the user is valid before
	 * creating a session. This method is snychronized to ensure that :
	 * <ol>
	 * <li>A race condition does not allow two sessions to be assigned identical session identifiers or tracking numbers</li>
	 * <li>Users are not granted more active sessions than allowed simultaneously</li>
	 * </ol>
	 * 
	 * @precondition The user ID must be valid and the client data must not be null and should be populated
	 * @postcondition A session identified will be returned that is associated with the session created
	 * @param userId
	 * @param clientData
	 * @return The session identifier for the given session
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws NoSuchAlgorithmException
	 * @throws InputValidationException 
	 */
	public synchronized String createSession( long userId, ClientData clientData ) throws SQLException, NoDatabaseConnectionException, NoSuchAlgorithmException, InputValidationException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Determine if the user ID is valid
		if( userId < 1 )
			throw new IllegalArgumentException("The user ID must be greater than 0");
		
		// 1 -- Create the session
		
		//Cleanup old sessions
		removeOldSessions();
		
		Connection conn = null;
		PreparedStatement statement = null;
		
		try{
			conn = appRes.getDatabaseConnection( Application.DatabaseAccessType.SESSION );
			statement = conn.prepareStatement("Insert into Sessions (UserID, TrackingNumber, SessionID, SessionIDCreated, Status, SessionCreated, LastActivity, RemoteUserAddress, RemoteUserData, ConnectionAddress, ConnectionData ) values (?,?,?,?,?,?,?,?,?,?,?)");
			
			Timestamp now = new Timestamp( System.currentTimeMillis() );
			String sessionId = generateSessionIdentifier(SESSION_IDENTIFIER_LENGTH);
			
			statement.setLong(1, userId ); //UserId
			statement.setLong(2, getTrackingNumber() ); //TrackingNumber
			statement.setString(3, sessionId ); //SessionId
			statement.setTimestamp(4, now ); //SessionIdCreated
			statement.setInt(5, SessionStatus.SESSION_ACTIVE.getStatusId() ); //Status
			statement.setTimestamp(6, now ); //SessionCreated
			statement.setTimestamp(7, now ); //LastActivity
			if( clientData != null ){
				statement.setString(8, clientData.getRemoteSourceAddress().toString() ); //RemoteUserAddress
				statement.setString(9, clientData.getRemoteSourceClientData() ); //RemoteUserData
				statement.setString(10, clientData.getSourceAddress().toString() ); //ConnectionAddress
				statement.setString(11, clientData.getSourceClientData() ); //ConnectionData
			}
			else{
				statement.setString(8, "" ); //RemoteUserAddress
				statement.setString(9, "" ); //RemoteUserData
				statement.setString(10, "" ); //ConnectionAddress
				statement.setString(11, "" ); //ConnectionData
			}
			
			statement.executeUpdate();
			
			return sessionId;
		} finally {
			if (statement != null )
				statement.close();
			
			if (conn != null )
				conn.close();
		}
	}
	
	
	/**
	 * Method generates a session identifier string (based on SHA1) to the given length. The identifier is 
	 * returned as a string of hexadecimal characters. The session identifier is generated using a 
	 * random number generator.
	 * @precondition The length must be greater than zero, and SHA1PRNG must be available
	 * @postcondition A hex encoded representation of the session identifier will be returned that is unique
	 * @param length The length of the session identifier
	 * @return
	 * @throws NoSuchAlgorithmException 
	 * @throws NoDatabaseConnectionException 
	 * @throws InputValidationException 
	 * @throws SQLException 
	 */
	private String generateSessionIdentifier( int length ) throws NoSuchAlgorithmException, SQLException, InputValidationException, NoDatabaseConnectionException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the length is valid
		if( length == 0 )
			throw new IllegalArgumentException("Session identifier creation failed since the given length is invalid");
		
		// 1 -- Generate the hex encoded session identifier
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		
		byte[] sid = new byte [length/2];
		random.nextBytes( sid );
		
		//Ensure that session identifier is unique (has not been allocate already) before granting
		String sessionIdentifier = new String(Hex.encodeHex(sid));
		while( getSessionStatus(sessionIdentifier) != SessionStatus.SESSION_NULL ){
			random.nextBytes( sid );
			sessionIdentifier = new String(Hex.encodeHex(sid));
		}
		
		return sessionIdentifier;
	}
	
	/**
	 * Create a tracking number that will be associated with the session identifier to track the session across a number of 
	 * session identifiers. This number is not random (likely sequential) since it is not given to the remote client. Note
	 * that this function <i>does not allocate</i> the tracking number. Thus, this method should only be called from the
	 * method that allocates sessions, and the allocation method must be synchronized to prevent a race condition that may
	 * cause multiple sessions to be assigned the same tracking number.  
	 * @precondition A database connection must be present
	 * @postcondition A currently unused tracking number will be returned
	 * @return
	 * @throws NoDatabaseConnectionException 
	 * @throws SQLException 
	 */
	private long getTrackingNumber() throws NoDatabaseConnectionException, SQLException{
		// 0 -- Precondition check
		
		// 1 -- Allocate a tracking number
		Random random = new Random();
		
		int trackingNumber;
		
		//	 1.1 -- Prepare the SQL statements to be used in identifying a tracking number
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try{
			conn = appRes.getDatabaseConnection( Application.DatabaseAccessType.SESSION );
			statement = conn.prepareStatement("Select * from Sessions where TrackingNumber = ?");
			
			//	 1.2 -- Find an open tracking number
			for( int c = 0; c < 2000; c++ ){
				trackingNumber = random.nextInt(32767);
				statement.setInt(1, trackingNumber);
				result = statement.executeQuery();
				
				if( !result.next() )
					return trackingNumber;
			}
			
			//The following should never be executed (should only be executed when a tracking number cannot be allocated; this should not happen)
			//assert false;
			return -1;
		} finally {
			if (result != null )
				result.close();
			
			if (statement != null )
				statement.close();
			
			if (conn != null )
				conn.close();
		}
		
	}
	
	/**
	 * This is the function operates resets session activity as if .  
	 * @param sessionIdentifier
	 * @return
	 * @throws InputValidationException 
	 * @throws NoDatabaseConnectionException 
	 * @throws SQLException 
	 */
	public synchronized void resetSessionActivity( String sessionIdentifier ) throws InputValidationException, NoDatabaseConnectionException, SQLException{
		
		// 0 -- Precondition check
		if( sessionIdentifier == null )
			return;
		
		Matcher matcher = sessionIdentifierRegex.matcher(sessionIdentifier);
		if( !matcher.matches() ){
			throw new InputValidationException("Malformed session identifier", "Session Identifier", sessionIdentifier);
		}
		
		// 1 -- Determine if the session is valid
		Connection conn = null;
		PreparedStatement statement = null;
		Timestamp lastActivity = new Timestamp(System.currentTimeMillis());
		
		try{
			conn = appRes.getDatabaseConnection(Application.DatabaseAccessType.SESSION);
			statement = conn.prepareStatement("Update Sessions set LastActivity = ? where SessionID = ? and Status = ?");
			statement.setTimestamp(1, lastActivity);
			statement.setString(2, sessionIdentifier);
			statement.setInt(3, SessionStatus.SESSION_ACTIVE.getStatusId() ); //Status
			statement.executeUpdate();
			
		} finally {
			
			if (statement != null )
				statement.close();
			
			if (conn != null )
				conn.close();
		}
	}
	
	/**
	 * This is the function operates identically to the <i>refreshSessionIdentifier( String sessionIdentifier, boolean resetActivity )</i> method
	 * except that the <>resetActivity</i> argument is automatically false.  
	 * @param sessionIdentifier
	 * @return
	 */
	public String refreshSessionIdentifier( String sessionIdentifier )throws InputValidationException, SQLException, NoSuchAlgorithmException, NoDatabaseConnectionException{
		return refreshSessionIdentifier( sessionIdentifier, false);
	}
	
	/**
	 * This method takes the session identifier for a current session and creates a new session identifier that associates to the same
	 * session. This is done for security reasons to make the session identifier difficult to hijack. Attackers must take great care
	 * in regards to timing when attempting to hijack a session since failure will cause the application to detect the attempt and 
	 * tear down associated sessions. Attackers must also silence the authenticate users' system since the use of old session identifiers
	 * by the authentic user will be detected as a session anomaly.
	 * <p>
	 * Note that the session identifier given will no longer be valid at the end the given function (if successful). The returned
	 * session ID must be used instead. Note that this function is synchronized to prevent an application from refreshing the same session
	 * identifier twice simultaneously (which would spawn two sessions) and to prevent two sessions from being granted the same session ID.
	 * @precondition The session identifier given must not be null or empty and must be associated with a real session to be successful
	 * @postcondition The old session identifier will be invalidated and a new session identifier will be returned
	 * @param sessionIdentifier The session identifier for the given session
	 * @return
	 * @throws InputValidationException 
	 * @throws SQLException 
	 * @throws NoSuchAlgorithmException 
	 * @throws NoDatabaseConnectionException 
	 */
	public synchronized String refreshSessionIdentifier( String sessionIdentifier, boolean resetActivity ) throws InputValidationException, SQLException, NoSuchAlgorithmException, NoDatabaseConnectionException{
		
		// 0 -- Precondition check
		if( sessionIdentifier == null )
			return null;
		
		Matcher matcher = sessionIdentifierRegex.matcher(sessionIdentifier);
		if( !matcher.matches() ){
			throw new InputValidationException("Malformed session identifier", "Session Identifier", sessionIdentifier);
		}
		
		// 1 -- Determine if the session is valid
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet result = null;
		PreparedStatement newSidStatement = null;
		PreparedStatement oldSidStatement = null;
		
		try{
			conn = appRes.getDatabaseConnection(Application.DatabaseAccessType.SESSION);
			statement = conn.prepareStatement("Select * from Sessions where SessionID = ?");
			statement.setString(1, sessionIdentifier);
			result = statement.executeQuery();
			
			//	 1.1 -- Session must be available
			if( !result.next() )
				return null;
			
			//	 1.2 -- Session must be active
			int status = result.getInt("Status");
			
			if( status != SessionStatus.SESSION_ACTIVE.getStatusId() && status != SessionStatus.SESSION_IDENTIFIER_EXPIRED.getStatusId() ){
				return null;
			}
			
			// 2 -- Create a new session for the user
			/* Note the creation of the new session identifier is done before discarding the old one to prevent
			 * concurrent functions calls from failing. If the old one was discarded first, then procedure calls 
			 * would fail until the new SID was created. However, the applications should be designed to suspend procedure
			 * calls until the SID is refreshed.
			 */ 
			
			String newSid = generateSessionIdentifier( SESSION_IDENTIFIER_LENGTH );
			long trackingNumber = result.getLong("TrackingNumber");
			long userId = result.getLong("UserID");
			Timestamp lastActivity;
			
			if( resetActivity == false ){
				lastActivity = result.getTimestamp("LastActivity");
			}
			else{
				lastActivity = new Timestamp(System.currentTimeMillis());
			}
			
			Timestamp sessionCreated = result.getTimestamp("SessionCreated");
			String remoteSourceAddress = result.getString("RemoteUserAddress");
			String remoteSourceData = result.getString("RemoteUserData");
			String connectionSourceAddress = result.getString("ConnectionAddress");
			String connectionSourceData = result.getString("ConnectionData");
			
			newSidStatement = conn.prepareStatement("Insert into Sessions (UserID, TrackingNumber, SessionID, SessionIDCreated, Status, SessionCreated, LastActivity, RemoteUserAddress, RemoteUserData, ConnectionAddress, ConnectionData ) values (?,?,?,?,?,?,?,?,?,?,?)");
			
			Timestamp now = new Timestamp( System.currentTimeMillis() );
			
			newSidStatement.setLong(1, userId ); //UserId
			newSidStatement.setLong(2, trackingNumber ); //TrackingNumber
			newSidStatement.setString(3, newSid ); //SessionId
			newSidStatement.setTimestamp(4, now ); //SessionIdCreated
			newSidStatement.setInt(5, SessionStatus.SESSION_ACTIVE.getStatusId() ); //Status
			newSidStatement.setTimestamp(6, sessionCreated ); //SessionCreated
			newSidStatement.setTimestamp(7, lastActivity ); //LastActivity
			newSidStatement.setString(8, remoteSourceAddress ); //RemoteUserAddress
			newSidStatement.setString(9, remoteSourceData ); //RemoteUserData
			newSidStatement.setString(10, connectionSourceAddress ); //ConnectionAddress
			newSidStatement.setString(11, connectionSourceData ); //ConnectionData
			
			conn.setAutoCommit(false);
			newSidStatement.executeUpdate();
			
			// 3 -- Discard the old session identifier
			oldSidStatement = conn.prepareStatement("Update Sessions set Status = ? where SessionID = ?");
			oldSidStatement.setInt(1, SessionStatus.SESSION_IDENTIFIER_EXPIRED.getStatusId());
			oldSidStatement.setString(2, sessionIdentifier);
			oldSidStatement.executeUpdate();
			
			// 4 -- Return the new session ID
			conn.commit();
			return newSid;
		} finally {
			
			if (result != null )
				result.close();
			
			if (statement != null )
				statement.close();
			
			if (newSidStatement != null )
				newSidStatement.close();
			
			if (oldSidStatement != null )
				oldSidStatement.close();
			
			if (conn != null )
				conn.close();
		}
	}
	
	/**
	 * Resolves the status of the session associated with the given session identifier.
	 * @precondition A database connection exists
	 * @postcondition A status indicator will be returned that indicates the status of the given session idenifier.
	 * @param sessionIdentifier
	 * @return
	 * @throws SQLException 
	 * @throws InputValidationException 
	 * @throws NoDatabaseConnectionException 
	 */
	public SessionStatus getSessionStatus( String sessionIdentifier ) throws SQLException, InputValidationException, NoDatabaseConnectionException{
		
		// 0 -- Precondition check
		if( sessionIdentifier == null )
			return null;
		
		Matcher matcher = sessionIdentifierRegex.matcher(sessionIdentifier);
		if( !matcher.matches() )
			throw new InputValidationException("Malformed session identifer", "Session Identifier", sessionIdentifier);
		
		//updateSessionStates();
		
		Connection conn = appRes.getDatabaseConnection(Application.DatabaseAccessType.SESSION);
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try{
			statement = conn.prepareStatement("Select * from Sessions where SessionID = ?");
			statement.setString(1, sessionIdentifier);
			result = statement.executeQuery();
			
			//	 1.1 -- Session must be available
			if( !result.next() )
				return SessionStatus.SESSION_NULL;
			
			//	 1.2 -- Session must be active
			int status = result.getInt("Status");
			
			return SessionStatus.getStatusById( status );
		} finally {
			if (result != null )
				result.close();
			
			if (statement != null )
				statement.close();
			
			if (conn != null )
				conn.close();
		}
	}
	
	/**
	 * Remove old session data that is no longer relevant. Sessions can deleted when:
	 * <ol>
	 * 	<li>The session has had no activity for too long</li>
	 *  <li>The session has exceeded the maximum session time (re-authentication necessary)</li>
	 *  <li>The session has been administratively disabled</li>
	 *  <li>The session has been terminated by the user</li>
	 *  <li>The session has been involved in a hijack attempt</li>
	 * </ol>
	 * @throws SQLException 
	 * @throws InputValidationException 
	 * @throws NoDatabaseConnectionException 
	 * @precondition A database connection must be available
	 * @postcondition Session data for sessions that are terminated and very old expired session data will be removed
	 *
	 */
	private void removeOldSessions() throws SQLException, NoDatabaseConnectionException, InputValidationException{
		// 0 -- Precondition check
		Connection conn = appRes.getDatabaseConnection( Application.DatabaseAccessType.SESSION );
		
		if( conn == null )
			throw new NoDatabaseConnectionException();
		
		// 1 -- Delete sessions that are old
		long retentionPeriod = 7200000L;//2 hours: keep sessions that should be delete for 2 hours (so the user can be notified that their session is dead)
		
		//	 1.1 -- Determine if session has exceeded the maximum session time
		long curSeconds = System.currentTimeMillis(); 
		
		Timestamp oldestSessionLifetime = new Timestamp( curSeconds - (appRes.getApplicationConfiguration().getSessionLifetime() * 1000) - retentionPeriod );
		PreparedStatement preparedStatement = null;
		
		try{
			preparedStatement = conn.prepareStatement("Delete from Sessions where SessionCreated < ?");
			preparedStatement.setTimestamp(1, oldestSessionLifetime);
			preparedStatement.execute();
			
			//	 1.2 -- Determine if session has had no activity for too long
			long maximumSessionInactivitySecs = appRes.getApplicationConfiguration().getSessionInactivityThreshold();
			
			if( maximumSessionInactivitySecs > 0 ){
				Timestamp oldestInactiveSessionLifetime = new Timestamp( curSeconds - (maximumSessionInactivitySecs * 1000) - retentionPeriod );
				
				preparedStatement = conn.prepareStatement("Delete from Sessions where SessionCreated < ? and Status = " + SessionStatus.SESSION_ACTIVE.getStatusId());
				preparedStatement.setTimestamp(1, oldestInactiveSessionLifetime);
				preparedStatement.execute();
			}
		} finally {
			
			if (preparedStatement != null )
				preparedStatement.close();
			
			if (conn != null )
				conn.close();
		}		
		
	}
	
	
	/*private void updateSessionStates() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		// 0 -- Precondition check
		Connection conn = appRes.getDatabaseConnection( Application.DB_SESSION_SUBSYSTEM );
		
		
		// 1 -- Mark sessions that have exceeded the maximum session time
		long curSeconds = System.currentTimeMillis();
		PreparedStatement sessionLifetimestatement = null;
		
		try{
			sessionLifetimestatement = conn.prepareStatement("Update Sessions set Status = ? where SessionCreated < ?");
			
			long maximumSessionSecs = appRes.getApplicationConfiguation().getSessionLifetime();
			Timestamp maxSessionThreshold = new Timestamp( curSeconds - (maximumSessionSecs * 1000) );
			
			if( maximumSessionSecs < 1 ){ // Session lifetime is disabled
				//Do nothing, the sessions don't expire
			}
			else if( maximumSessionSecs > 1000000L ){ //Session lifetime is too long (may result in a numerical overflow)
				maxSessionThreshold = new Timestamp( curSeconds - (1000000L * 1000) );
				sessionLifetimestatement.setInt(1, SessionStatus.SESSION_LIFETIME_EXCEEDED.getStatusId());
				sessionLifetimestatement.setTimestamp( 2, maxSessionThreshold);
				sessionLifetimestatement.executeUpdate();
				appRes.logEvent(StringTable.MSGID_ILLEGAL_CONFIG, "Maximum session time too long (" + maximumSessionSecs + ")");
			}
			else if( maximumSessionSecs < 300 ){ // Session lifetime is too short (less than 5 minutes)
				maxSessionThreshold = new Timestamp( curSeconds - (DEFAULT_SESSION_LIFETIME * 1000) );
				sessionLifetimestatement.setInt(1, SessionStatus.SESSION_LIFETIME_EXCEEDED.getStatusId());
				sessionLifetimestatement.setTimestamp( 2, maxSessionThreshold);
				sessionLifetimestatement.executeUpdate();
				appRes.logEvent(StringTable.MSGID_ILLEGAL_CONFIG, "Maximum session time too short (" + maximumSessionSecs + ")");
			}
			
		} finally {
			
			if (sessionLifetimestatement != null )
				sessionLifetimestatement.close();
			
			if (conn != null )
				conn.close();
		}
		
		
		// 2 -- Mark sessions that have had no activity for a given time
		//long maximumSessionInactivitySecs = appParams.getParameter("Security.SessionInactivityThreshold", DEFAULT_SESSION_INACTIVITY_THRESHOLD);
	}*/
	
	
	/**
	 * Terminate the session associated with the given session identfier.
	 * @precondition The session identifier must be valid and a database connection must be present
	 * @postcondition The given session will be terminated
	 * @param sessionIdentifier
	 * @return
	 * @throws InputValidationException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	public boolean terminateSession( String sessionIdentifier ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		// 0 -- Precondition check
		// The preconditions will be checked in the getSessionInfo method
		
		SessionInfo sessionInfo = getSessionInfo( sessionIdentifier );
		
		// 1 -- Do not attempt to terminate the session if it is not active
		//updateSessionStates();
		if( sessionInfo.getSessionStatus() != SessionStatus.SESSION_ACTIVE )
			return false;
		
		// 2 -- Terminate the session
		Connection conn = appRes.getDatabaseConnection(Application.DatabaseAccessType.SESSION);
		PreparedStatement preparedStatement = null;
		
		try{
			preparedStatement = conn.prepareStatement("Delete from Sessions where TrackingNumber = ?");
			preparedStatement.setLong(1, sessionInfo.getTrackingNumber() );
			
			if( preparedStatement.executeUpdate() > 0 )
				return true;
			else
				return false;
		} finally {
			if (preparedStatement != null )
				preparedStatement.close();
			
			if (conn != null )
				conn.close();
		}
	}
	
	/**
	 * Terminate the session associated with the given tracking number.
	 * @precondition The session identifier must be valid and a database connection must be present
	 * @postcondition The given session will be terminated
	 * @param trackingNumber
	 * @return
	 * @throws InputValidationException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	public boolean terminateSession( long trackingNumber ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		// 0 -- Precondition check
		// The preconditions will be checked in the getSessionInfo method
		
		// 1 -- Do not attempt to terminate the session if it is not active
		//updateSessionStates();
		
		// 2 -- Terminate the session
		Connection conn = appRes.getDatabaseConnection(Application.DatabaseAccessType.SESSION);
		PreparedStatement preparedStatement = null;
		
		try{
			preparedStatement = conn.prepareStatement("Delete from Sessions where TrackingNumber = ?");
			preparedStatement.setLong(1, trackingNumber );
			
			if( preparedStatement.executeUpdate() > 0 )
				return true;
			else
				return false;
		} finally {
			if (preparedStatement != null )
				preparedStatement.close();
			
			if (conn != null )
				conn.close();
			
		}
	}
	
	/**
	 * Get information related to the given session and its state.
	 * @precondition The session identifier must be valid, and a database connection must be present
	 * @postcondition Information related to the current session will be returned
	 * @param sessionIdentifier
	 * @return
	 * @throws InputValidationException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	public SessionInfo getSessionInfo( String sessionIdentifier ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		
		// 0 -- Precondition check
		if( sessionIdentifier == null )
			return new SessionInfo();
		
		Matcher matcher = sessionIdentifierRegex.matcher(sessionIdentifier);
		if( !matcher.matches() )
			throw new InputValidationException("Malformed session identifer", "Session Identifier", sessionIdentifier);
		
		// 1 -- Determine if the session is valid
		Connection conn = appRes.getDatabaseConnection(Application.DatabaseAccessType.SESSION);
		
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try{
			statement = conn.prepareStatement("Select * from Sessions where SessionID = ?");
			statement.setString(1, sessionIdentifier);
			result = statement.executeQuery();
			
			//	 1.1 -- Session must be available
			if( !result.next() )
				return new SessionInfo();
			
			// 2 -- Populate the session information
			SessionInfo sessionInfo = constructSessionInfo( result );
			
			return sessionInfo;
		} finally {
			if (result != null )
				result.close();
			
			if (statement != null )
				statement.close();
			
			if (conn != null )
				conn.close();
		}
	}
	
	
	/**
	 * Get the session associated with the given tracking number. The tracking number identifies a session across multiple
	 * session identifiers. Thus, the tracking number or the session identifier can be used to identify users' session.
	 * @param trackingNumber
	 * @return
	 * @throws InputValidationException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public SessionInfo getSessionInfo( long trackingNumber ) throws InputValidationException, SQLException, NoDatabaseConnectionException {
		// 0 -- Precondition check
		//if( trackingNumber < 0 )
		//	throw new InputValidationException("The session tracking number must be greater than 0", "Tracking Number", String.valueOf( trackingNumber ) );
		
		// 1 -- Determine if the session is valid
		Connection conn = appRes.getDatabaseConnection(Application.DatabaseAccessType.SESSION);
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try{
			statement = conn.prepareStatement("Select * from Sessions where TrackingNumber = ? and Status = 1");
			statement.setLong(1, trackingNumber);
			result = statement.executeQuery();
			
			//	 1.1 -- Session must be available
			if( !result.next() )
				return new SessionInfo();
			
			
			// 2 -- Populate the session information
			SessionInfo sessionInfo = constructSessionInfo( result );
			
			return sessionInfo;
		} finally {
			if (result != null )
				result.close();
			
			if (statement != null )
				statement.close();
			
			if (conn != null )
				conn.close();
		}
	}
	
	/**
	 * Disable any and all session associated with the given user identifier. All sessions will be marked as
	 * administratively disabled. This method is not intended for normal session termination.
	 * @param userId
	 * @return The number of session disabled
	 * @throws SQLException 
	 * @throws InputValidationException 
	 * @throws NoDatabaseConnectionException 
	 */
	public int disableUserSessions( int userId ) throws SQLException, NoDatabaseConnectionException, InputValidationException{
		
		// 0 -- precondition check
		
		//	 0.1 -- Make sure the argument is valid
		if( userId < 1 )
			throw new IllegalArgumentException("The user ID must be greater than 0");
		
		//	 0.2 -- Determine if the class is configured in such a way to connect to the authentication system
		Connection conn = appRes.getDatabaseConnection(Application.DatabaseAccessType.USER_QUERY);
		
		// 1 -- Update any active sessions for the user to disabled
		
		PreparedStatement preparedStatement = null;
		
		try{
			preparedStatement = conn.prepareStatement("Update Sessions set Status = ? where UserID = ? and Status = ?");
			preparedStatement.setInt( 1, SessionStatus.SESSION_ADMIN_TERMINATED.getStatusId() );
			preparedStatement.setInt( 2, userId );
			preparedStatement.setInt( 3, SessionStatus.SESSION_ACTIVE.getStatusId() );
			
			return preparedStatement.executeUpdate();
		} finally {
			
			if (preparedStatement != null )
				preparedStatement.close();
			
			if (conn != null )
				conn.close();
		}
	}
	
	/**
	 * Get a list of the current sessions. Note that this function is intended to report on all sessions currently in
	 * the database. Thus, it may return some sessions that are no longer valid (such as those administratively disabled).
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException 
	 */
	public SessionInfo[] getCurrentSessions() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		
		// 0 -- Precondition check
		Connection conn = appRes.getDatabaseConnection( Application.DatabaseAccessType.SESSION);
		if( conn == null )
			throw new NoDatabaseConnectionException();
		
		// 1 -- Get a list of the current sessions
		PreparedStatement preparedStatement = null;
		
		try{
			preparedStatement = conn.prepareStatement("Select * from Sessions where Status <> ?");
			preparedStatement.setInt(1, SessionStatus.SESSION_EXPIRED.getStatusId());
			
			ResultSet resultSet = preparedStatement.executeQuery();
			
			//	 1.1 -- Get the list of the sessions
			Vector<SessionInfo> sessionsVector = new Vector<SessionInfo>();
			while( resultSet.next() ){
				sessionsVector.add( constructSessionInfo( resultSet ) );
			}
			
			//	 1.2 -- Convert to an array
			SessionInfo[] sessionInfo = null;
			sessionInfo = new SessionInfo[ sessionsVector.size()];
			for( int c = 0; c < sessionsVector.size(); c++){
				sessionInfo[c] = (SessionInfo)sessionsVector.get(c);
			}
			
			// Return the result
			return sessionInfo;
		} finally {
			if (preparedStatement != null )
				preparedStatement.close();
			
			if (conn != null )
				conn.close();
		}
		
	}
	
	/**
	 * This method in intended to construct a session information from the current row in the given result set. The result set must be
	 * from the sessions table and contain all fields. This method is intended to prevent the repetition of the code (boilerplate code)
	 * to create the given data. It also prevents unnecessary SQL queries resulting from subsequent function calls to retieve the data. 
	 * @param result
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws InputValidationException
	 */
	private SessionInfo constructSessionInfo( ResultSet result ) throws SQLException, NoDatabaseConnectionException, InputValidationException{
		// 0 -- Precondition check
		if( result == null ){
			throw new IllegalArgumentException("Result set is null");
		}
		
		// 1 -- Populate the session information
		int trackingNumber = result.getInt("TrackingNumber");
		int userId = result.getInt("UserID");
		Timestamp lastActivity = result.getTimestamp("LastActivity");
		Timestamp sessionCreated = result.getTimestamp("SessionCreated");
		//Timestamp sessionIdCreated = result.getTimestamp("SessionIDCreated");
		String remoteSourceAddress = result.getString("RemoteUserAddress");
		String remoteSourceData = result.getString("RemoteUserData");
		String connectionSourceAddress = result.getString("ConnectionAddress");
		String connectionSourceData = result.getString("ConnectionData");
		String sessionIdentifier = result.getString("SessionID");
		
		// 2 -- Resolve the session status
		int status = result.getInt("Status");
		
		//	 2.1 -- Determine if session has exceeded the maximum session time
		long curSeconds = System.currentTimeMillis(); 
		
		long maximumSessionSecs = appRes.getApplicationConfiguration().getSessionLifetime();
		long oldestSessionLifetimeThreshold = curSeconds - (maximumSessionSecs * 1000);
		
		if( maximumSessionSecs < 1 ){ // Session lifetime is disabled
			//Do nothing, the sessions don't expire
		}
		else{
			if( sessionCreated.getTime() < oldestSessionLifetimeThreshold )
				status = SessionStatus.SESSION_LIFETIME_EXCEEDED.getStatusId();
		}
		
		//	 2.2 -- Determine if session has had no activity for too long
		if( status == SessionStatus.SESSION_ACTIVE.getStatusId() ){//Do not bother resolving if the session is already determined to be inactive
			long maximumSessionInactivitySecs = appRes.getApplicationConfiguration().getSessionInactivityThreshold();
			long oldestSessionInactivityThreshold = curSeconds - (maximumSessionInactivitySecs * 1000);
			
			if( maximumSessionInactivitySecs < 1 ){ // Session inactivity threshold is disabled
				//Do nothing, the sessions don't expire after inactivity
			}
			else{
				// Check to determine if the session has expired
				
				// Look at the last activity if it is greater than the create time...
				if( lastActivity != null && lastActivity.getTime() >= sessionCreated.getTime() ){
					if( lastActivity.getTime() < oldestSessionInactivityThreshold ){
						status = SessionStatus.SESSION_INACTIVE.getStatusId();
					}
				}
				
				// Otherwise, look at the create time to determine if the session is inactive
				else if( sessionCreated.getTime() < oldestSessionInactivityThreshold ){
					status = SessionStatus.SESSION_INACTIVE.getStatusId();
				}
			}
		}
		
		//	 2.3 -- Determine if session identifier is too old
		/*if( status == SessionStatus.SESSION_ACTIVE.getStatusId()  ){

			if( isSessionIdentiferExpired(sessionIdCreated.getTime(), curSeconds) == true )
				status = SessionStatus.SESSION_IDENTIFIER_EXPIRED.getStatusId();
		}*/
			
		
		// 3 -- Get the username associated with the session
		UserManagement userManagement = new UserManagement(appRes);
		UserManagement.UserDescriptor userDesc = null;
		
		try{
			userDesc = userManagement.getUserDescriptor(userId);
		}catch(NotFoundException e){
			userDesc = null;
		}
		
		String username = null;
		
		if( userDesc != null )
			username = userDesc.getUserName();
		
		// 4 -- Create and return the result
		SessionStatus sessionStatus = SessionStatus.getStatusById( status );
		SessionInfo sessionInfo = new SessionInfo( sessionStatus, sessionIdentifier, trackingNumber, userId, username, lastActivity, sessionCreated, remoteSourceAddress, remoteSourceData, connectionSourceAddress, connectionSourceData );
		
		
		return sessionInfo;
	}
	
	/**
	 *  Determines if a session identifier created at the time given has expired.
	 * @param sessionIdCreateTime
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	protected boolean isSessionIdentiferExpired(long sessionIdCreateTime ) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return isSessionIdentiferExpired( sessionIdCreateTime, System.currentTimeMillis() );
	}
	
	/**
	 * Determines if a session identifier created at the time given has expired.
	 * @param sessionIdCreateTime
	 * @param curSeconds
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	protected boolean isSessionIdentiferExpired(long sessionIdCreateTime, long curSeconds) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		long maximumSessionIdSecs =appRes.getApplicationConfiguration().getSessionIdentifierLifetime();
		
		if( maximumSessionIdSecs < 1 ){
			//Session identifiers do not expire, indicate this by returning false
			return false;
		}
		else
		{
			long oldestSessionIdThreshold = curSeconds - (maximumSessionIdSecs * 1000);
		
			if( sessionIdCreateTime < oldestSessionIdThreshold )
				return true;
		}
		
		return false;
	}
	
	/**
	 * The Session info class provides information about the currently existing session. Most of the fields will not be
	 * populated (i.e. null) if the data does not exist (such as in the case of a session that is not found to be valid).  
	 * @author luke
	 *
	 */
	public static class SessionInfo{
		
		private int trackingNumber;
		private int userId;
		private Timestamp lastActivity;
		private Timestamp sessionCreated;
		private String remoteSourceAddress;
		private String remoteSourceData;
		private String connectionSourceAddress;
		private String connectionSourceData;
		private SessionStatus sessionStatus;
		private String sessionIdentifier;
		private String userName;
		
		/**
		 * The following constructor is intended for sessions that are or at one time where legitimate.
		 * @param sessionStatus
		 * @param sessionIdentifier
		 * @param trackingNumber
		 * @param userId
		 * @param lastActivity
		 * @param sessionCreated
		 * @param remoteSourceAddress
		 * @param remoteSourceData
		 * @param connectionSourceAddress
		 * @param connectionSourceData
		 */
		public SessionInfo( SessionStatus sessionStatus, String sessionIdentifier, int trackingNumber, int userId, Timestamp lastActivity, Timestamp sessionCreated, String remoteSourceAddress, String remoteSourceData, String connectionSourceAddress, String connectionSourceData ){
			this.trackingNumber = trackingNumber;
			this.userId = userId;
			this.lastActivity =lastActivity;
			this.sessionCreated = sessionCreated;
			this.remoteSourceAddress =remoteSourceAddress;
			this.remoteSourceData = remoteSourceData;
			this.connectionSourceAddress = connectionSourceAddress;
			this.connectionSourceData = connectionSourceData;
			this.sessionIdentifier = sessionIdentifier;
			this.sessionStatus = sessionStatus;
		}
		
		/**
		 * The following constructor is intended for sessions that are or at one time where legitimate.
		 * @param sessionStatus
		 * @param sessionIdentifier
		 * @param trackingNumber
		 * @param userId
		 * @param lastActivity
		 * @param sessionCreated
		 * @param remoteSourceAddress
		 * @param remoteSourceData
		 * @param connectionSourceAddress
		 * @param connectionSourceData
		 */
		public SessionInfo( SessionStatus sessionStatus, String sessionIdentifier, int trackingNumber, int userId, String username, Timestamp lastActivity, Timestamp sessionCreated, String remoteSourceAddress, String remoteSourceData, String connectionSourceAddress, String connectionSourceData ){
			this.trackingNumber = trackingNumber;
			this.userId = userId;
			this.lastActivity =lastActivity;
			this.sessionCreated = sessionCreated;
			this.remoteSourceAddress =remoteSourceAddress;
			this.remoteSourceData = remoteSourceData;
			this.connectionSourceAddress = connectionSourceAddress;
			this.connectionSourceData = connectionSourceData;
			this.sessionIdentifier = sessionIdentifier;
			this.sessionStatus = sessionStatus;
			this.userName = username;
		}
		
		public Hashtable<String, Object> toHashtable(){
			Hashtable<String, Object> hash = new Hashtable<String, Object>();
			hash.put("TrackingNumber", new Double(trackingNumber) );
			hash.put("UserID",  new Double(userId) );
			hash.put("LastActivity", lastActivity);
			hash.put("SessionCreated", sessionCreated);
			hash.put("RemoteSourceAddress", remoteSourceAddress);
			hash.put("RemoteSourceData", remoteSourceData);
			hash.put("ConnectionSourceAddress", connectionSourceAddress);
			hash.put("ConnectionSourceData", connectionSourceData);
			hash.put("SessionStatus", sessionStatus);
			hash.put("SessionID", sessionIdentifier);
			
			return hash;
		}
		
		/**
		 * The default constructor creates a null session; or one that indicates that no valid session exists. The additional data
		 * fields will be null since they do not apply.
		 *
		 */
		public SessionInfo( ){
			sessionStatus = SessionStatus.SESSION_NULL;
		}
		
		/**
		 * Get the status of the session. Note that a null session will indicate that the additional data
		 * fields will be null since they do not apply.
		 * @precondition None
		 * @postcondition A session status indicator will be returned
		 * @return
		 */
		public SessionStatus getSessionStatus(){
			return sessionStatus;
		}
		
		/**
		 * Retrieves the session tracking number for the given session; or -1 of none exists.
		 * @precondition None
		 * @postcondition The tracking number will be returned, or -1 if no session exists
		 * @return
		 */
		public long getTrackingNumber(){
			return trackingNumber;
		}
		
		/**
		 * Retrieves the session tracking number for the given session; or -1 of none exists.
		 * @precondition None
		 * @postcondition The user ID will be returned, or -1 if no session exists
		 * @return
		 */
		public int getUserId(){
			return userId;
		}
		
		/**
		 * Retrieves the time that activity from the user was noted; or null if no session exists.
		 * @precondition None
		 * @postcondition The last time that activity from the user was noted will be returned, or null if no session exists
		 * @return
		 */
		public Timestamp getLastActivity(){
			return lastActivity;
		}
		
		/**
		 * Retrieves the date that the given session was created; or null if no session exists.
		 * @precondition None
		 * @postcondition The date that the given session was created will be returned, or null if no session exists
		 * @return
		 */
		public Timestamp getSessionCreated(){
			return sessionCreated;
		}
		
		/**
		 * Retrieves string representation of the remote address and domain for the source of the given session; or null if no session exists.
		 * @precondition None
		 * @postcondition The string representation of the remote address and domain for the source will be returned, or null if no session exists
		 * @return
		 */
		public String getRemoteSourceAddress(){
			return remoteSourceAddress;
		}
		
		/**
		 * Retrieves the user name associated with this session (based on the user ID).
		 * @precondition None
		 * @postcondition The username of the session owner, or null if not found
		 * @return
		 */
		public String getUserName(){
			return userName;
		}
		
		/**
		 * Retrieves the data field associated with the remote user for the given session; or null if no session exists.
		 * @precondition None
		 * @postcondition The data field associated with the remote user will be returned, or null if no session exists
		 * @return
		 */
		public String getRemoteSourceData(){
			return remoteSourceData;
		}
		
		/**
		 * Retrieves string representation of the source address and domain for the source of the given session; or null if no session exists.
		 * @precondition None
		 * @postcondition The string representation of the source address and domain for the source will be returned, or null if no session exists
		 * @return
		 */
		public String getSourceAddress(){
			return connectionSourceAddress;
		}
		
		/**
		 * Retrieves the data field associated with the connecting user for the given session; or null if no session exists.
		 * @precondition None
		 * @postcondition The data field associated with the connecting user will be returned, or null if no session exists
		 * @return
		 */
		public String getSourceData(){
			return connectionSourceData;
		}
		
		/**
		 * Retrieves the session identifier for the given session; or null if no session exists.
		 * @precondition None
		 * @postcondition The session identifier will be returned, or null if no session exists
		 * @return
		 */
		public String getSessionIdentifier(){
			return sessionIdentifier;
		}
		
	}
	
	
}
