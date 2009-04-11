package net.lukeMurphey.nsia.trustBoundary;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.lukeMurphey.nsia.AccessControl;
import net.lukeMurphey.nsia.AccessControlDescriptor;
import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.GroupManagement;
import net.lukeMurphey.nsia.InputValidationException;
import net.lukeMurphey.nsia.InsufficientPermissionException;
import net.lukeMurphey.nsia.NoDatabaseConnectionException;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.NotFoundException;
import net.lukeMurphey.nsia.ObjectPermissionDescriptor;
import net.lukeMurphey.nsia.RightDescriptor;
import net.lukeMurphey.nsia.SessionManagement;
import net.lukeMurphey.nsia.SessionStatus;
import net.lukeMurphey.nsia.SiteGroupManagement;
import net.lukeMurphey.nsia.UserManagement;
import net.lukeMurphey.nsia.UserManagement.UserDescriptor;
import net.lukeMurphey.nsia.eventLog.EventLogField;
import net.lukeMurphey.nsia.eventLog.EventLogMessage;
import net.lukeMurphey.nsia.eventLog.EventLogField.FieldName;
import net.lukeMurphey.nsia.scanRules.ScanData;

abstract public class ApiHandler {
	
	protected Application appRes;
	protected SessionManagement sessionManagement;
	protected UserManagement userManagement;
	protected GroupManagement groupManagement;
	protected AccessControl accessControl;
	protected SiteGroupManagement siteGroupManagement;
	protected ScanData scanData;
	protected static final boolean DEFAULT_DENY = true; 
	
	public ApiHandler(Application appRes){
		
		// 0 -- Precondition check
		if( appRes == null )
			throw new IllegalArgumentException("The application resources cannot be null");
		
		// 1 -- Set the parameters
		this.appRes = appRes;
		sessionManagement = new SessionManagement( appRes );
		userManagement = new UserManagement( appRes );
		groupManagement = new GroupManagement( appRes );
		accessControl = new AccessControl( appRes );
		siteGroupManagement = new SiteGroupManagement( appRes );
		scanData = new ScanData( appRes );
	}
	
	/**
	 * Determine if the session is valid and throw an exception if not. This method throws an exception as opposed to 
	 * returning a value to make session checking easier. If the session identifier has expired, then the method will attempt
	 * to get an updated session identifier on behalf of the client.
	 * @param sessionId
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 */
	protected void checkSession( String sessionId ) throws NoSessionException, GeneralizedException{
		SessionStatus sessionStatus;
		
		try {
			SessionManagement.SessionInfo sessionInfo = sessionManagement.getSessionInfo( sessionId );
			
			if( sessionInfo == null ){
				throw new NoSessionException(SessionStatus.SESSION_NULL);
			}
			else{
				sessionStatus = sessionInfo.getSessionStatus();
			}
			
		} catch (InputValidationException e1) {
			appRes.logEvent(EventLogMessage.Category.SESSION_ID_ILLEGAL, new EventLogField( FieldName.SESSION_ID, sessionId ) );
			throw new GeneralizedException();
		} catch (SQLException e1) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e1  );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e1) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e1 );
			throw new GeneralizedException();
		}
		
		if( sessionStatus != SessionStatus.SESSION_ACTIVE ){
			/*String newSid = null;
			try{
				newSid = sessionManagement.refreshSessionIdentifier(sessionId);
			} catch (NoSuchAlgorithmException e){
				appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e);
				throw new GeneralizedException();
			} catch (InputValidationException e) {
				appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e);
				throw new GeneralizedException();
			} catch (SQLException e) {
				appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
				throw new GeneralizedException();
			} catch (NoDatabaseConnectionException e) {
				appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e);
				throw new GeneralizedException();
			}
			
			if( newSid == null )
				throw new NoSessionException(sessionStatus);
			else{
				
			}*/
			
			throw new NoSessionException(sessionStatus);
		}
		else{
			
		}
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param rightName
	 * @param operationTitle
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	protected boolean checkRight( String sessionIdentifier, String rightName ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		return checkRight( sessionIdentifier, rightName, null );
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param rightName
	 * @param operationTitle
	 * @param annotation
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	protected boolean checkRight( String sessionIdentifier, String rightName, String annotation ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		try {
			
			SessionManagement.SessionInfo sessionInfo = sessionManagement.getSessionInfo(sessionIdentifier);		
			
			if( sessionInfo.getSessionStatus() != SessionStatus.SESSION_ACTIVE ){
				throw new NoSessionException(sessionInfo.getSessionStatus());
			}
			
			
			RightDescriptor acl = null;
			
			try{
				acl = accessControl.getUserRight(sessionInfo.getUserId(), rightName, true);
			}
			catch(NotFoundException e){
				acl = null;
			}
			
			UserDescriptor user = userManagement.getUserDescriptor( sessionInfo.getUserId());
			String userName = null;
			
			if( user != null )
				userName = user.getUserName();
			
			EventLogField[] fields;
			
			if( annotation != null ){
				fields = new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation),
						new EventLogField( FieldName.RIGHT, rightName ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID, sessionInfo.getUserId() ) };
			}
			else{
				fields = new EventLogField[] {
						new EventLogField( FieldName.RIGHT, rightName ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID, sessionInfo.getUserId() ) };
			}
			
			//Determine if the permissions are sufficient to allow access
			if( user.isUnrestricted() == true ){
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, fields );
						
				return true;
			}
			else if( acl == null ){
				if( !DEFAULT_DENY )
					return false;
				else{
					
					appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, fields );
					
					throw new InsufficientPermissionException();
				}
			}
			else if( acl.getRight() == AccessControlDescriptor.Action.PERMIT ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, fields );
				
				return true;
			}
			else if( acl.getRight() == AccessControlDescriptor.Action.DENY ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY, fields );
				
				throw new InsufficientPermissionException();
			}
			else{
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, fields );
				
				throw new InsufficientPermissionException();
			}
			
		} catch (InputValidationException e) {
			appRes.logEvent(EventLogMessage.Category.SESSION_ID_ILLEGAL, new EventLogField( FieldName.SESSION_ID, sessionIdentifier ) );
			throw new GeneralizedException();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (NotFoundException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		}
		
		
	}
	
	/**
	 * Get the user information for the associated session.
	 * @param sessionIdentifier
	 * @return
	 * @throws GeneralizedException
	 * @throws NotFoundException 
	 */
	protected UserDescriptor getUserInfo( String sessionIdentifier ) throws GeneralizedException, NoSessionException{
		SessionManagement.SessionInfo sessionInfo;
		
		try {
			sessionInfo = sessionManagement.getSessionInfo(sessionIdentifier);
		} catch (InputValidationException e1) {
			appRes.logEvent(EventLogMessage.Category.SESSION_ID_ILLEGAL, new EventLogField( FieldName.SESSION_ID, sessionIdentifier ) );
			throw new GeneralizedException();
		} catch (SQLException e1) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e1 );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e1) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e1 );
			throw new GeneralizedException();
		}
		
		UserDescriptor user = null;
		try {
			user = userManagement.getUserDescriptor( sessionInfo.getUserId());
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (NotFoundException e) {
			throw new NoSessionException(SessionStatus.SESSION_NULL);
		}
		
		
		return user;
	}
	
	/**
	 * Validate the given user name. Create log entries are required if the username does not pass validation.
	 * @param requesterUserName
	 * @param requesterUserId
	 * @param userName
	 * @throws InputValidationException
	 */
	protected void checkUserName( String requesterUserName, long requesterUserId, String userName ) throws InputValidationException{

		//	 0.3 -- Make sure the user name is valid
		if( userName == null ){
			appRes.logEvent(EventLogMessage.Category.USER_NAME_NULL,
					new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
					new EventLogField( FieldName.SOURCE_USER_ID , requesterUserId ) );
			
			throw new InputValidationException("Username cannot be null", "username", "null");
		}
		
		if( userName.length() == 0 ){
			
			appRes.logEvent(EventLogMessage.Category.USER_NAME_EMPTY,
					new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
					new EventLogField( FieldName.SOURCE_USER_ID , requesterUserId ) );
			
			throw new InputValidationException("Username cannot contain 0 characters", "username", userName);
		}
		
		Pattern nameRegex = Pattern.compile(UserManagement.USERNAME_REGEX);
		Matcher matcher = nameRegex.matcher( userName );
		
		if( !matcher.matches() ){
			
			appRes.logEvent(EventLogMessage.Category.USER_NAME_ILLEGAL,
					new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
					new EventLogField( FieldName.SOURCE_USER_ID , requesterUserId ),
					new EventLogField( FieldName.TARGET_USER_NAME , requesterUserId ));
			
			throw new InputValidationException("Username contains invalid characters", "username", userName );
		}
	}
	
	/**
	 * Validate the given group name. Create log entries are required if the name does not pass validation.
	 * @param requesterUserName
	 * @param requesterUserId
	 * @param groupName
	 * @throws InputValidationException
	 */
	protected void checkGroupName( String requesterUserName, long requesterUserId, String groupName ) throws InputValidationException{

		//	 0.3 -- Make sure the user name is valid
		if( groupName == null ){
			
			appRes.logEvent(EventLogMessage.Category.GROUP_NAME_NULL,
					new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
					new EventLogField( FieldName.SOURCE_USER_ID , requesterUserId ) );
			
			throw new InputValidationException("Group name cannot be null", "groupname", "null");
		}
		
		if( groupName.length() == 0 ){
			
			appRes.logEvent(EventLogMessage.Category.GROUP_NAME_EMPTY,
					new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
					new EventLogField( FieldName.SOURCE_USER_ID , requesterUserId ) );
			
			throw new InputValidationException("Group name cannot contain 0 characters", "groupname", groupName);
		}
		
		Pattern nameRegex = Pattern.compile(GroupManagement.GROUP_NAME_REGEX);
		Matcher matcher = nameRegex.matcher( groupName );
		
		if( !matcher.matches() ){
			
			appRes.logEvent(EventLogMessage.Category.GROUP_NAME_ILLEGAL,
					new EventLogField( FieldName.SOURCE_USER_NAME, requesterUserName ),
					new EventLogField( FieldName.SOURCE_USER_ID , requesterUserId ),
					new EventLogField( FieldName.GROUP_NAME , groupName ) );
			
			throw new InputValidationException("Group name contains invalid characters", "groupname", groupName );
		}
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param objectId
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	protected boolean checkCreate( String sessionIdentifier, long objectId ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		return checkCreate( sessionIdentifier, objectId, null);
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param objectId
	 * @param annotation
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	protected boolean checkCreate( String sessionIdentifier, long objectId, String annotation ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		try {
			
			SessionManagement.SessionInfo sessionInfo = sessionManagement.getSessionInfo(sessionIdentifier);		
			
			if( sessionInfo.getSessionStatus() != SessionStatus.SESSION_ACTIVE ){
				throw new NoSessionException(sessionInfo.getSessionStatus());
			}
			
			ObjectPermissionDescriptor acl = accessControl.getUserPermissions(sessionInfo.getUserId(), objectId, true);
			
			long userId = sessionInfo.getUserId();
			UserDescriptor user = userManagement.getUserDescriptor( sessionInfo.getUserId());
			String userName = null;
			
			if( user != null )
				userName = user.getUserName();
			
			
			//Determine if the permissions are sufficient to allow access
			if( user.isUnrestricted() == true ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Create" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
						new EventLogField( FieldName.TARGET_USER_ID , userId ) } );
				
				return true;
			}
			else if( acl == null )
				if( !DEFAULT_DENY )
					return false;
				else{
					
					appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, new EventLogField[] {
							new EventLogField( FieldName.MESSAGE, annotation ),
							new EventLogField( FieldName.OPERATION, "Create" ),
							new EventLogField( FieldName.OBJECT_ID, objectId ),
							new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
							new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
							new EventLogField( FieldName.TARGET_USER_ID , userId ) } );
					
					throw new InsufficientPermissionException();
				}
			else if( acl.getCreatePermission() == AccessControlDescriptor.Action.PERMIT ){
								
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Create" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
						new EventLogField( FieldName.TARGET_USER_ID , userId ) } );
				
				return true;
			}
			else if( acl.getCreatePermission() == AccessControlDescriptor.Action.DENY ){
								
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Create" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
						new EventLogField( FieldName.TARGET_USER_ID , userId ) } );
				
				throw new InsufficientPermissionException();
			}
			else{
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Create" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
						new EventLogField( FieldName.TARGET_USER_ID , userId ) } );
				
				throw new InsufficientPermissionException();
			}
			
		} catch (InputValidationException e) {
			appRes.logEvent(EventLogMessage.Category.SESSION_ID_ILLEGAL, new EventLogField( FieldName.SESSION_ID, sessionIdentifier ) );
			throw new GeneralizedException();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch( NotFoundException e){
			return false;
		}
	}
	
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param objectId
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	protected boolean checkExecute( String sessionIdentifier, long objectId ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		return checkExecute( sessionIdentifier, objectId, null);
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param objectId
	 * @param annotation
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	protected boolean checkExecute( String sessionIdentifier, long objectId, String annotation) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		try {
			
			SessionManagement.SessionInfo sessionInfo = sessionManagement.getSessionInfo(sessionIdentifier);		
			
			if( sessionInfo.getSessionStatus() != SessionStatus.SESSION_ACTIVE ){
				throw new NoSessionException(sessionInfo.getSessionStatus());
			}
			
			ObjectPermissionDescriptor acl = accessControl.getUserPermissions(sessionInfo.getUserId(), objectId, true);
			
			long userId = sessionInfo.getUserId();
			UserDescriptor user = userManagement.getUserDescriptor( sessionInfo.getUserId());
			String userName = null;
			
			if( user != null )
				userName = user.getUserName();
			
			//Determine if the permissions are sufficient to allow access
			if( user.isUnrestricted() == true ){
		
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Execute" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
						new EventLogField( FieldName.TARGET_USER_ID , userId ) } );

				return true;
			}
			else if( acl == null )
				if( !DEFAULT_DENY )
					return false;
				else{
					
					appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, new EventLogField[] {
							new EventLogField( FieldName.MESSAGE, annotation ),
							new EventLogField( FieldName.OPERATION, "Execute" ),
							new EventLogField( FieldName.OBJECT_ID, objectId ),
							new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
							new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
							new EventLogField( FieldName.TARGET_USER_ID , userId ) } );
					
					throw new InsufficientPermissionException();
				}
			else if( acl.getExecutePermission() == AccessControlDescriptor.Action.PERMIT ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Execute" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
						new EventLogField( FieldName.TARGET_USER_ID , userId ) } );
				
				return true;
			}
			else if( acl.getExecutePermission() == AccessControlDescriptor.Action.DENY ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Execute" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
						new EventLogField( FieldName.TARGET_USER_ID , userId ) } );
				
				throw new InsufficientPermissionException();
			}
			else{
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Execute" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
						new EventLogField( FieldName.TARGET_USER_ID , userId ) } );
				
				throw new InsufficientPermissionException();
			}
			
		} catch (InputValidationException e) {
			appRes.logEvent(EventLogMessage.Category.SESSION_ID_ILLEGAL, new EventLogField( FieldName.SESSION_ID, sessionIdentifier ) );
			throw new GeneralizedException();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch( NotFoundException e){
			return false;
		}
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param objectId
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	protected boolean checkModify( String sessionIdentifier, long objectId ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		return checkModify(sessionIdentifier, objectId, null);
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param objectId
	 * @param annotation
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	protected boolean checkModify( String sessionIdentifier, long objectId, String annotation) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		try {
			
			SessionManagement.SessionInfo sessionInfo = sessionManagement.getSessionInfo(sessionIdentifier);		
			
			if( sessionInfo.getSessionStatus() != SessionStatus.SESSION_ACTIVE ){
				throw new NoSessionException(sessionInfo.getSessionStatus());
			}
			
			ObjectPermissionDescriptor acl = accessControl.getUserPermissions(sessionInfo.getUserId(), objectId, true);
			
			long userId = sessionInfo.getUserId();
			UserDescriptor user = userManagement.getUserDescriptor( sessionInfo.getUserId());
			String userName = null;
			
			if( user != null )
				userName = user.getUserName();
			
			//Determine if the permissions are sufficient to allow access
			if( user.isUnrestricted() == true ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Modify"),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
						new EventLogField( FieldName.TARGET_USER_ID , userId ) } );

				
				return true;
			}
			else if( acl == null )
				if( !DEFAULT_DENY )
					return false;
				else{
					
					appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, new EventLogField[] {
							new EventLogField( FieldName.MESSAGE, annotation ),
							new EventLogField( FieldName.OPERATION, "Modify"),
							new EventLogField( FieldName.OBJECT_ID, objectId ),
							new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
							new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
							new EventLogField( FieldName.TARGET_USER_ID , userId ) } );
					
					throw new InsufficientPermissionException();
				}
			else if( acl.getModifyPermission() == AccessControlDescriptor.Action.PERMIT ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Modify"),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
						new EventLogField( FieldName.TARGET_USER_ID , userId ) } );
				
				return true;
			}
			else if( acl.getModifyPermission() == AccessControlDescriptor.Action.DENY ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Modify"),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
						new EventLogField( FieldName.TARGET_USER_ID , userId ) } );
				
				throw new InsufficientPermissionException();
			}
			else{
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Modify"),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
						new EventLogField( FieldName.TARGET_USER_ID , userId ) } );
				
				throw new InsufficientPermissionException();
			}
			
		} catch (InputValidationException e) {
			appRes.logEvent(EventLogMessage.Category.SESSION_ID_ILLEGAL, new EventLogField( FieldName.SESSION_ID, sessionIdentifier ) );
			throw new GeneralizedException();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch( NotFoundException e){
			return false;
		}
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param objectId
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	protected boolean checkControl( String sessionIdentifier, long objectId) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		return checkControl( sessionIdentifier, objectId, null );
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param objectId
	 * @param annotation
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	protected boolean checkControl( String sessionIdentifier, long objectId, String annotation) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		try {
			
			SessionManagement.SessionInfo sessionInfo = sessionManagement.getSessionInfo(sessionIdentifier);		
			
			if( sessionInfo.getSessionStatus() != SessionStatus.SESSION_ACTIVE ){
				throw new NoSessionException(sessionInfo.getSessionStatus());
			}
			
			ObjectPermissionDescriptor acl = accessControl.getUserPermissions(sessionInfo.getUserId(), objectId, true);
			
			UserDescriptor user = userManagement.getUserDescriptor( sessionInfo.getUserId());
			String userName = null;
			
			if( user != null )
				userName = user.getUserName();
			
			//Determine if the permissions are sufficient to allow access
			if( user.isUnrestricted() == true ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Control" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
				
				return true;
			}
			else if( acl == null )
				if( !DEFAULT_DENY )
					return false;
				else{
					
					appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, new EventLogField[] {
							new EventLogField( FieldName.MESSAGE, annotation ),
							new EventLogField( FieldName.OPERATION, "Control" ),
							new EventLogField( FieldName.OBJECT_ID, objectId ),
							new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
							new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
					
					throw new InsufficientPermissionException();
				}
			else if( acl.getControlPermission() == AccessControlDescriptor.Action.PERMIT ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Control" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
				
				return true;
			}
			else if( acl.getControlPermission() == AccessControlDescriptor.Action.DENY ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Control" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
				
				throw new InsufficientPermissionException();
			}
			else{
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Control" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
				
				throw new InsufficientPermissionException();
			}
			
		} catch (InputValidationException e) {
			appRes.logEvent(EventLogMessage.Category.SESSION_ID_ILLEGAL, new EventLogField( FieldName.SESSION_ID, sessionIdentifier ) );
			throw new GeneralizedException();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch( NotFoundException e){
			return false;
		}
	}
	
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param objectId
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	protected boolean checkDelete( String sessionIdentifier, long objectId) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		return checkDelete(sessionIdentifier, objectId, null);
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param objectId
	 * @param annotation
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	protected boolean checkDelete( String sessionIdentifier, long objectId, String annotation) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		try {
			
			SessionManagement.SessionInfo sessionInfo = sessionManagement.getSessionInfo(sessionIdentifier);		
			
			if( sessionInfo.getSessionStatus() != SessionStatus.SESSION_ACTIVE ){
				throw new NoSessionException(sessionInfo.getSessionStatus());
			}
			
			ObjectPermissionDescriptor acl = accessControl.getUserPermissions(sessionInfo.getUserId(), objectId, true);
			
			UserDescriptor user = userManagement.getUserDescriptor( sessionInfo.getUserId());
			String userName = null;
			
			if( user != null )
				userName = user.getUserName();
			
			//Determine if the permissions are sufficient to allow access
			if( user.isUnrestricted() == true ){
	
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Delete" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );

				return true;
			}
			else if( acl == null )
				if( !DEFAULT_DENY )
					return false;
				else{
					
					appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, new EventLogField[] {
							new EventLogField( FieldName.MESSAGE, annotation ),
							new EventLogField( FieldName.OPERATION, "Delete" ),
							new EventLogField( FieldName.OBJECT_ID, objectId ),
							new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
							new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
					
					throw new InsufficientPermissionException();
				}
			else if( acl.getDeletePermission() == AccessControlDescriptor.Action.PERMIT ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Delete" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
				
				return true;
			}
			else if( acl.getDeletePermission() == AccessControlDescriptor.Action.DENY ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Delete" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
				
				throw new InsufficientPermissionException();
			}
			else{
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Delete" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
				
				throw new InsufficientPermissionException();
			}
			
		} catch (InputValidationException e) {
			appRes.logEvent(EventLogMessage.Category.SESSION_ID_ILLEGAL, new EventLogField( FieldName.SESSION_ID, sessionIdentifier ) );
			throw new GeneralizedException();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch( NotFoundException e){
			return false;
		}
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param objectId
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	protected boolean checkRead( String sessionIdentifier, long objectId) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		return checkRead( sessionIdentifier, objectId, null);
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param objectId
	 * @param annotation
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	protected boolean checkRead( String sessionIdentifier, long objectId, String annotation) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		try {
			
			SessionManagement.SessionInfo sessionInfo = sessionManagement.getSessionInfo(sessionIdentifier);		
			
			if( sessionInfo.getSessionStatus() != SessionStatus.SESSION_ACTIVE ){
				throw new NoSessionException(sessionInfo.getSessionStatus());
			}
			
			ObjectPermissionDescriptor acl = accessControl.getUserPermissions(sessionInfo.getUserId(), objectId, true);
			
			UserDescriptor user = userManagement.getUserDescriptor( sessionInfo.getUserId());
			String userName = null;
			
			if( user != null )
				userName = user.getUserName();
			
			//Determine if the permissions are sufficient to allow access
			if( user.isUnrestricted() == true ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Read" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
				
				return true;
			}
			else if( acl == null )
				if( !DEFAULT_DENY )
					return false;
				else{
					
					appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, new EventLogField[] {
							new EventLogField( FieldName.MESSAGE, annotation ),
							new EventLogField( FieldName.OPERATION, "Read" ),
							new EventLogField( FieldName.OBJECT_ID, objectId ),
							new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
							new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
					
					throw new InsufficientPermissionException();
				}
			else if( acl.getReadPermission() == AccessControlDescriptor.Action.PERMIT ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Read" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
				
				return true;
			}
			else if( acl.getReadPermission() == AccessControlDescriptor.Action.DENY ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Read" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
				
				throw new InsufficientPermissionException();
			}
			else{
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Read" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
				
				throw new InsufficientPermissionException();
			}
			
		} catch (InputValidationException e) {
			appRes.logEvent(EventLogMessage.Category.SESSION_ID_ILLEGAL, new EventLogField( FieldName.SESSION_ID, sessionIdentifier ) );
			throw new GeneralizedException();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch( NotFoundException e){
			return false;
		}
	}
	

}
