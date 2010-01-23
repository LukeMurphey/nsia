package net.lukemurphey.nsia.trustBoundary;

import java.security.NoSuchAlgorithmException;

import java.sql.SQLException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.lukemurphey.nsia.AccessControlDescriptor;
import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.ClientData;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.LocalPasswordAuthentication;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.NumericalOverflowException;
import net.lukemurphey.nsia.PasswordAuthenticationValidator;
import net.lukemurphey.nsia.RightDescriptor;
import net.lukemurphey.nsia.SessionManagement;
import net.lukemurphey.nsia.SessionStatus;
import net.lukemurphey.nsia.UserManagement;
import net.lukemurphey.nsia.Authentication.AuthenticationResult;
import net.lukemurphey.nsia.SessionManagement.SessionInfo;
import net.lukemurphey.nsia.UserManagement.UserDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.eventlog.EventLogMessage.Category;

/**
 * This class acts as wrapper around the classes that perform authentication and session management. Class methods
 * perform functions such as session construction, deconstruction, session status, etc. 
 * @author luke
 *
 */
public class ApiSessionManagement extends ApiHandler{

	
	
	public ApiSessionManagement(Application appRes){
		super(appRes);
	}
	
	/**
	 * Method authenticates the user based on the password and constructs a session based if the authentication is 
	 * successful. Returns a session identifier as a string if successful, or null if unsuccessful. 
	 * @param userName
	 * @param password
	 * @return
	 * @throws GeneralizedException
	 */
	public String authenticate( String userName, String password ) throws GeneralizedException{
		return authenticate( userName, password, null);
	}
	
	/**
	 * Method authenticates the user based on the password and constructs a session based if the authentication is 
	 * successful. Returns a session identifier as a string if successful, or null if unsuccessful. 
	 * @param userName
	 * @param password
	 * @return
	 * @throws GeneralizedException
	 */
	public String authenticate( String userName, String password, ClientData clientData ) throws GeneralizedException{
		
		// 0 -- Precondition Checks
		
		//	 0.1 -- Username cannot be null or empty
		if( userName == null || userName.length() == 0 ){
			if( clientData == null){
				appRes.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_USERNAME_EMPTY);
			}
			else{
				appRes.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_USERNAME_EMPTY, new EventLogField( FieldName.SOURCE_ADDRESS, clientData.getSourceAddress().toString() ) );
			}
			
			return null;
		}
		
		//	 0.2 -- Make sure the username is valid
		Pattern nameRegex = Pattern.compile( UserManagement.USERNAME_REGEX );
		Matcher matcher = nameRegex.matcher(userName);
		if( !matcher.matches() ){
			if( clientData == null)
				appRes.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_USERNAME_EMPTY);
			else
				appRes.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_USERNAME_EMPTY, new EventLogField( FieldName.SOURCE_ADDRESS, clientData.getSourceAddress().toString() ) );
			
			appRes.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_USERNAME_ILLEGAL, new EventLogField( FieldName.TARGET_USER_NAME, userName ) );
			return null;
		}
		
		//	 0.3 -- Username must not be overly long (this makes SQL injection more difficult)
		if( userName.length() > UserManagement.USERNAME_LENGTH ){
			if( clientData == null)
				appRes.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_USERNAME_EMPTY);
			else
				appRes.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_USERNAME_EMPTY, new EventLogField( FieldName.SOURCE_ADDRESS, clientData.getSourceAddress().toString() ) );
			
			appRes.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_LENGTH_EXCESSIVE, new EventLogField( FieldName.LENGTH, userName.length()), new EventLogField( FieldName.TARGET_USER_NAME, userName ) );
			return null;
		}
		
		// 1 -- Authenticate
		
		//	 1.1 -- Try to authenticate
		LocalPasswordAuthentication localPasswordAuth = new LocalPasswordAuthentication(appRes);
		PasswordAuthenticationValidator passwordAuth = new PasswordAuthenticationValidator( password );
		AuthenticationResult result;
		
		try {
			result = localPasswordAuth.authenticate(userName, passwordAuth, clientData);
		} catch (NoSuchAlgorithmException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		} catch (NumericalOverflowException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		}
		

		//	 1.2 -- Make a decision on the result
		if( result.getAuthenticationStatus() == AuthenticationResult.AUTH_ACCOUNT_ADMINISTRATIVELY_LOCKED ){
			if( clientData == null)
				appRes.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_ACCOUNT_DISABLED, new EventLogField( FieldName.TARGET_USER_NAME, userName ));
			else
				appRes.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_ACCOUNT_DISABLED, new EventLogField( FieldName.TARGET_USER_NAME, userName ) , new EventLogField( FieldName.SOURCE_ADDRESS, clientData.getSourceAddress().toString() ) );
			
			return null;
		}
		else if( result.getAuthenticationStatus() == AuthenticationResult.AUTH_ACCOUNT_BRUTE_FORCE_LOCKED ){
			if( clientData == null)
				appRes.logEvent(EventLogMessage.Category.AUTHENTICATION_USERNAME_BLOCKED, new EventLogField( FieldName.TARGET_USER_NAME, userName ) );
			else
				appRes.logEvent(EventLogMessage.Category.AUTHENTICATION_USERNAME_BLOCKED, new EventLogField( FieldName.TARGET_USER_NAME, userName ), new EventLogField( FieldName.SOURCE_ADDRESS, clientData.getSourceAddress().toString() ) );
			
			return null;
		}
		else if( result.getAuthenticationStatus() == AuthenticationResult.AUTH_ACCOUNT_DISABLED ){
			if( clientData == null)
				appRes.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_ACCOUNT_DISABLED, new EventLogField( FieldName.TARGET_USER_NAME, userName ));
			else
				appRes.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_ACCOUNT_DISABLED, new EventLogField( FieldName.TARGET_USER_NAME, userName ), new EventLogField( FieldName.SOURCE_ADDRESS, clientData.getSourceAddress().toString() ) );
			
			return null;
		}
		else if( result.getAuthenticationStatus() == AuthenticationResult.AUTH_FAILED ){//This should not be returned
			if( clientData == null)
				appRes.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_PASSWORD_WRONG, new EventLogField( FieldName.TARGET_USER_NAME, userName ));
			else
				appRes.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_PASSWORD_WRONG, new EventLogField( FieldName.TARGET_USER_NAME, userName ), new EventLogField( FieldName.SOURCE_ADDRESS, clientData.getSourceAddress().toString() ) );
			
			return null;
		}
		else if( result.getAuthenticationStatus() == AuthenticationResult.AUTH_INVALID_PASSWORD ){
			if( clientData == null)
				appRes.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_PASSWORD_ILLEGAL, new EventLogField( FieldName.TARGET_USER_NAME, userName ));
			else
				appRes.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_PASSWORD_ILLEGAL, new EventLogField( FieldName.TARGET_USER_NAME, userName ), new EventLogField( FieldName.SOURCE_ADDRESS, clientData.getSourceAddress().toString() ) );
			
			return null;
		}
		else if( result.getAuthenticationStatus() == AuthenticationResult.AUTH_INVALID_USER ){
			if( clientData == null)
				appRes.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_USERNAME_INVALID, new EventLogField( FieldName.TARGET_USER_NAME, userName ));
			else
				appRes.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_USERNAME_INVALID, new EventLogField( FieldName.TARGET_USER_NAME, userName ), new EventLogField( FieldName.SOURCE_ADDRESS, clientData.getSourceAddress().toString() ) );
			
			return null;
		}
		else if( result.getAuthenticationStatus() == AuthenticationResult.AUTH_SUCCESS ){
			
			EventLogMessage message = new EventLogMessage(EventLogMessage.Category.AUTHENTICATION_SUCCESS, new EventLogField( FieldName.TARGET_USER_NAME, userName ));
			
			// Get the session information in order to add additional details
			try{
				SessionInfo sessionInfo = sessionManagement.getSessionInfo(result.getSessionIdentifier());
				message.addField( new EventLogField(FieldName.TARGET_USER_ID, sessionInfo.getUserId()) );
				message.addField( new EventLogField(FieldName.SESSION_TRACKING_NUMBER, sessionInfo.getTrackingNumber()) );
			}
			catch(Exception e){
				appRes.logExceptionEvent(Category.INTERNAL_ERROR, e);
			}

			if( clientData != null){
				message.addField( new EventLogField( FieldName.SOURCE_ADDRESS, clientData.getSourceAddress().toString() ) );
			}
			
			appRes.logEvent(message);
			
			return result.getSessionIdentifier();
		}
		else{
			appRes.logEvent( EventLogMessage.Category.INTERNAL_ERROR, new EventLogField( FieldName.MESSAGE, "Invalid authentication result code"  ));
			return null;
		}
	}
	
	/**
	 * Terminate the session associated with the session ID.
	 * @param sessionIdentifier
	 * @throws GeneralizedException
	 */
	public boolean terminateSession( String sessionIdentifier ) throws GeneralizedException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the session identifier is valid
		//	 The session identifier will be checked in the subsequent method calls
		
		// 1 -- Terminate session
		SessionManagement.SessionInfo sessionInfo;
		
		//	 1.1 -- Get the session information for tracking purposes
		try {
			sessionInfo = sessionManagement.getSessionInfo( sessionIdentifier );
		} catch (InputValidationException e1) {
			appRes.logEvent(EventLogMessage.Category.SESSION_ID_ILLEGAL, new EventLogField( FieldName.SESSION_ID, sessionIdentifier ) );
			return false;
		} catch (SQLException e1) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e1 );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e1) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e1 );
			throw new GeneralizedException();
		}
		
		//	 1.2 -- Get the session status and exit if the SID is not tied to a valid session
		if( sessionInfo.getSessionStatus() == SessionStatus.SESSION_NULL ){
			appRes.logEvent(EventLogMessage.Category.SESSION_INVALID_TERMINATION_ATTEMPT, new EventLogField( FieldName.SESSION_ID, sessionIdentifier ) );
			return false;
		}
		
		//	 1.3 -- Terminate the session
		try {
			if( sessionManagement.terminateSession( sessionIdentifier ) ){
				UserManagement.UserDescriptor userDescriptor;
				
				try{
					userDescriptor = userManagement.getUserDescriptor(sessionInfo.getUserId());
				}
				catch( NotFoundException e){
					appRes.logEvent(EventLogMessage.Category.SESSION_ENDED,
							new EventLogField( FieldName.SOURCE_USER_ID, sessionInfo.getUserId() ),
							new EventLogField( FieldName.SOURCE_USER_NAME, sessionInfo.getUserName() ) );
					return true;
				}
				appRes.logEvent(EventLogMessage.Category.SESSION_ENDED,
						new EventLogField( FieldName.TARGET_USER_NAME, userDescriptor.getUserName()),
						new EventLogField( FieldName.TARGET_USER_ID, userDescriptor.getUserID() ),
						new EventLogField( FieldName.SOURCE_USER_NAME, sessionInfo.getUserName()),
						new EventLogField( FieldName.SOURCE_USER_ID, sessionInfo.getUserId() ) );
				return true;
			}
			else{
				appRes.logEvent(EventLogMessage.Category.SESSION_INVALID_TERMINATION_ATTEMPT, new EventLogField( FieldName.SESSION_ID, sessionIdentifier ) );
				return false;
			}
		} catch (InputValidationException e) {
			appRes.logEvent(EventLogMessage.Category.SESSION_ID_ILLEGAL, new EventLogField( FieldName.SESSION_ID, sessionIdentifier ) );
			return false;
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Terminate the session associated with the tracking number.
	 * @param trackingNumber
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException 
	 * @throws NoSessionException 
	 */
	public boolean terminateSession( String sessionIdentifer, long trackingNumber ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has permission
		checkRight( sessionIdentifer, "Users.Sessions.Delete");
		
		//	 0.2 -- Make sure the session identifier is valid
		//	 The session identifier will be checked in the subsequent method calls
		
		// 1 -- Terminate session
		SessionManagement.SessionInfo sessionInfo;
		
		//	 1.1 -- Get the session information for tracking purposes
		try {
			sessionInfo = sessionManagement.getSessionInfo( trackingNumber );
		} catch (InputValidationException e1) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e1 );
			return false;
		} catch (SQLException e1) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e1 );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e1) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e1 );
			throw new GeneralizedException();
		}
		
		//	 1.2 -- Get the session status and exit if the SID is not tied to a valid session
		if( sessionInfo.getSessionStatus() == SessionStatus.SESSION_NULL ){
			appRes.logEvent(EventLogMessage.Category.SESSION_INVALID_TERMINATION_ATTEMPT, new EventLogField( FieldName.SESSION_TRACKING_NUMBER, trackingNumber ) );
			return false;
		}
		
		//	 1.3 -- Terminate the session
		try {
			if( sessionManagement.terminateSession( trackingNumber ) ){
				
				UserManagement.UserDescriptor userDescriptor;
				
				try{
					userDescriptor = userManagement.getUserDescriptor(sessionInfo.getUserId());
				}
				catch( NotFoundException e){
					appRes.logEvent(EventLogMessage.Category.SESSION_ENDED, 
							new EventLogField( FieldName.SOURCE_USER_NAME, sessionInfo.getUserName()),
							new EventLogField( FieldName.SOURCE_USER_ID, sessionInfo.getUserId() ) );
					return true;
				}
				
				appRes.logEvent(EventLogMessage.Category.SESSION_ENDED,
						new EventLogField( FieldName.TARGET_USER_NAME, userDescriptor.getUserName()),
						new EventLogField( FieldName.TARGET_USER_ID, userDescriptor.getUserID() ),
						new EventLogField( FieldName.SOURCE_USER_NAME, sessionInfo.getUserName()),
						new EventLogField( FieldName.SOURCE_USER_ID, sessionInfo.getUserId() ) );
				return true;
			}
			else{
				appRes.logEvent(EventLogMessage.Category.SESSION_INVALID_TERMINATION_ATTEMPT, new EventLogField( FieldName.SESSION_TRACKING_NUMBER, trackingNumber ) );
				return false;
			}
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			return false;
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e  );
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Get the status indicator associated with the given session.
	 * @param sessionIdentifier
	 * @return
	 * @throws GeneralizedException
	 */
	public int getSessionStatus( String sessionIdentifier ) throws GeneralizedException{
		SessionManagement.SessionInfo sessionInfo;
		
		try {
			sessionInfo = sessionManagement.getSessionInfo( sessionIdentifier );
		} catch (InputValidationException e1) {
			appRes.logEvent(EventLogMessage.Category.SESSION_ID_ILLEGAL, new EventLogField( FieldName.SESSION_ID, sessionIdentifier ) );
			throw new GeneralizedException();
		} catch (SQLException e1) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e1 );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e1) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e1 );
			throw new GeneralizedException();
		}
		
		if( sessionInfo.getSessionStatus() == SessionStatus.SESSION_EXPIRED ){
			appRes.logEvent( EventLogMessage.Category.SESSION_INACTIVITY_EXPIRED, new EventLogField(FieldName.TARGET_USER_NAME, sessionInfo.getUserName()), new EventLogField(FieldName.TARGET_USER_ID, sessionInfo.getUserId()), new EventLogField(FieldName.SESSION_TRACKING_NUMBER, sessionInfo.getTrackingNumber()) );
		}
		else if( sessionInfo.getSessionStatus() == SessionStatus.SESSION_HIJACKED ){
			appRes.logEvent( EventLogMessage.Category.SESSION_HIJACKED, new EventLogField(FieldName.TARGET_USER_NAME, sessionInfo.getUserName()), new EventLogField(FieldName.TARGET_USER_ID, sessionInfo.getUserId()), new EventLogField(FieldName.SESSION_TRACKING_NUMBER, sessionInfo.getTrackingNumber()) );
		}
		else if( sessionInfo.getSessionStatus() == SessionStatus.SESSION_INACTIVE ){
			appRes.logEvent( EventLogMessage.Category.SESSION_INACTIVITY_EXPIRED, new EventLogField(FieldName.TARGET_USER_NAME, sessionInfo.getUserName()), new EventLogField(FieldName.TARGET_USER_ID, sessionInfo.getUserId()), new EventLogField(FieldName.SESSION_TRACKING_NUMBER, sessionInfo.getTrackingNumber()) );
		}
		else if( sessionInfo.getSessionStatus() == SessionStatus.SESSION_LIFETIME_EXCEEDED ){
			appRes.logEvent( EventLogMessage.Category.SESSION_MAX_TIME_EXCEEDED, new EventLogField(FieldName.TARGET_USER_NAME, sessionInfo.getUserName()), new EventLogField(FieldName.TARGET_USER_ID, sessionInfo.getUserId()), new EventLogField(FieldName.SESSION_TRACKING_NUMBER, sessionInfo.getTrackingNumber()) );;
		}
		
		return sessionInfo.getSessionStatus().getStatusId();
	}
	
	/**
	 * Retrieves an updated session identifier or returns null if the session identifier could not
	 * be refreshed (such as if the old session identifier is invalid or illegal).
	 * @param sessionIdentifier
	 * @return
	 * @throws GeneralizedException
	 */
	public String refreshSessionIdentifier( String sessionIdentifier ) throws GeneralizedException{
		return refreshSessionIdentifier(sessionIdentifier, true);
	}
	
	/**
	 * Retrieves an updated session identifier or returns null if the session identifier could not
	 * be refreshed (such as if the old session identifier is invalid or illegal).
	 * @param sessionIdentifier
	 * @return
	 * @throws GeneralizedException
	 */
	public String refreshSessionIdentifier( String sessionIdentifier, boolean resetActivity ) throws GeneralizedException{
		String newSessionIdentifier;
		
		try {
			newSessionIdentifier = sessionManagement.refreshSessionIdentifier( sessionIdentifier, resetActivity );
		} catch (NoSuchAlgorithmException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logEvent(EventLogMessage.Category.SESSION_ID_ILLEGAL, new EventLogField( FieldName.SESSION_ID, sessionIdentifier ) );
			throw new GeneralizedException();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}
		
		return newSessionIdentifier;
	}
	
	/**
	 * Disables the user's sessions for the given user ID. The number of sessions destructed will be returned, or -1 if the operation
	 * failed.
	 * @param sessionIdentifier
	 * @param userId
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public int disableUserSessions( String sessionIdentifier, int userId ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		
		// 0 -- Make sure the session is legal
		checkSession( sessionIdentifier );
		
		// 1 -- Make sure the user has sufficient permissions and perform the operation if they do

		SessionManagement.SessionInfo sessionInfo = null;
		RightDescriptor acl = null;
		
		try{
			
			try {
				
				sessionInfo = sessionManagement.getSessionInfo(sessionIdentifier);
				acl = accessControl.getUserRight(sessionInfo.getUserId(), "Users.Sessions.Delete", true);
				
			}catch(NotFoundException e){
				appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
				throw new GeneralizedException();
			}
			
			UserDescriptor user = userManagement.getUserDescriptor( sessionInfo.getUserId());
			String userName = null;
			
			if( user != null )
				userName = user.getUserName();
			
			//Determine if the permissions are sufficient to allow access
			if( acl == null )
				return -1;
			else if( acl.getRight() == AccessControlDescriptor.Action.PERMIT ){
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT,
						new EventLogField( FieldName.OPERATION, "Discard user sessions"),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName),
						new EventLogField( FieldName.SOURCE_USER_ID, sessionInfo.getUserId()),
						new EventLogField( FieldName.TARGET_USER_ID, userId ) );
				return sessionManagement.disableUserSessions( userId );
			}
			else if( acl.getRight() == AccessControlDescriptor.Action.DENY ){
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY,
						new EventLogField( FieldName.OPERATION, "Discard user sessions"),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName),
						new EventLogField( FieldName.SOURCE_USER_ID, sessionInfo.getUserId()),
						new EventLogField( FieldName.TARGET_USER_ID, userId ) );
				
				throw new InsufficientPermissionException();
			}
			else{
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT,
						new EventLogField( FieldName.OPERATION, "Discard user sessions"),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName),
						new EventLogField( FieldName.SOURCE_USER_ID, sessionInfo.getUserId()),
						new EventLogField( FieldName.TARGET_USER_ID, userId ) );
				
				throw new InsufficientPermissionException();
			}
			
		} catch (InputValidationException e) {
			appRes.logEvent(EventLogMessage.Category.SESSION_ID_ILLEGAL, new EventLogField( FieldName.SESSION_ID, sessionIdentifier ) );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		} catch (NotFoundException e) {
			
			appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT,
					new EventLogField( FieldName.OPERATION, "Discard user sessions"),
					new EventLogField( FieldName.SOURCE_USER_NAME, sessionInfo.getUserName()),
					new EventLogField( FieldName.SOURCE_USER_ID, sessionInfo.getUserId()),
					new EventLogField( FieldName.TARGET_USER_ID, userId ) );
			
			throw new InsufficientPermissionException();
		}
		
		return -1;
	}
	
	/**
	 * Get the user descriptor for the user with the given session identifier.
	 * @throws NoSessionException 
	 */
	public UserDescriptor getUserInfo( String sessionIdentifier ) throws GeneralizedException, NoSessionException{
		return super.getUserInfo( sessionIdentifier );
	}
	
	/**
	 * Get a list of the currently active user sessions.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public SessionManagement.SessionInfo[] getUserSessions( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		
		// 1 -- Make sure user has a session
		checkSession( sessionIdentifier );
		
		// 2 -- Make sure the user has the necessary permissions
		SessionManagement.SessionInfo sessionInfo = null;
		RightDescriptor acl;
		
		try {
			
			sessionInfo = sessionManagement.getSessionInfo(sessionIdentifier);
			
			try{
				acl = accessControl.getUserRight(sessionInfo.getUserId(), "Users.Sessions.View", true);
			
			}catch(NotFoundException e){
				appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
				throw new GeneralizedException();
			}
			
			UserDescriptor user = userManagement.getUserDescriptor( sessionInfo.getUserId());
			String userName = null;
			
			if( user != null ){
				userName = user.getUserName();
			}
			
			//Determine if the permissions are sufficient to allow access
			if (user.isUnrestricted())
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, new EventLogField( FieldName.OPERATION, "Enumerate user sessions" ), new EventLogField( FieldName.SOURCE_USER_NAME, userName), new EventLogField( FieldName.SOURCE_USER_ID, sessionInfo.getUserId() ) );
			else if( acl == null )
				return null;
			else if( acl.getRight() == AccessControlDescriptor.Action.PERMIT ){
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, new EventLogField( FieldName.OPERATION, "Enumerate user sessions" ), new EventLogField( FieldName.SOURCE_USER_NAME, userName), new EventLogField( FieldName.SOURCE_USER_ID, sessionInfo.getUserId() ) );
			}
			else if( acl.getRight() == AccessControlDescriptor.Action.DENY ){
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY, new EventLogField( FieldName.OPERATION, "Enumerate user sessions" ), new EventLogField( FieldName.SOURCE_USER_NAME, userName), new EventLogField( FieldName.SOURCE_USER_ID, sessionInfo.getUserId() ) );
				throw new InsufficientPermissionException();
			}
			else{
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, new EventLogField( FieldName.OPERATION, "Enumerate user sessions" ), new EventLogField( FieldName.SOURCE_USER_NAME, userName), new EventLogField( FieldName.SOURCE_USER_ID, sessionInfo.getUserId() ) );
				throw new InsufficientPermissionException();
			}
			
		} catch (InputValidationException e) {
			appRes.logEvent(EventLogMessage.Category.SESSION_ID_ILLEGAL, new EventLogField( FieldName.SESSION_ID, sessionIdentifier ) );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}
		catch (NotFoundException e) {
			if( sessionInfo != null )
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, new EventLogField( FieldName.OPERATION, "Enumerate user sessions" ), new EventLogField( FieldName.SOURCE_USER_ID, sessionInfo.getUserId() ) );
			else
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT,  new EventLogField( FieldName.OPERATION, "Enumerate user sessions" ), new EventLogField( FieldName.SOURCE_USER_ID, "Unknown" ) );
			
			throw new InsufficientPermissionException();
		}
		
		// 3 -- Get the session info
		SessionManagement.SessionInfo[] currentSessions;
		try {
			currentSessions = sessionManagement.getCurrentSessions();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}
		
		return currentSessions;
	}
	
}
