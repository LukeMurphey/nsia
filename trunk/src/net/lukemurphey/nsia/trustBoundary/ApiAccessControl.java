package net.lukemurphey.nsia.trustBoundary;

import java.sql.SQLException;

import net.lukemurphey.nsia.*;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;

/**
 * Class provides the API wrapped around the methods that maintain and create the access control entries
 * for the application (both permissions and rights).
 * @author luke
 *
 */
public class ApiAccessControl extends ApiHandler {
	
	public ApiAccessControl(Application app) {
		super(app);
	}
	
	/**
	 * Retrieve the given user's permissions for the object. Null will be returned if the user is not found in the tables.
	 * @precondition The user ID must exist in the permission tables (or null will be returned) and a database connection must exist
	 * @postcondition An access control descriptor will be returned indicating the permissions granted to the user 
	 * @param sessionIdentifier
	 * @param userId
	 * @param objectId
	 * @return
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws InsufficientPermissionException
	 */
	public ObjectPermissionDescriptor getUserPermissions( String sessionIdentifier, int userId, long objectId, boolean resolveUserGroupPermissions ) throws GeneralizedException, NoSessionException, InsufficientPermissionException{
		
		// 0 -- Precondition checks
		SessionStatus sessionStatus = null;
		SessionManagement.SessionInfo sessionInfo = null;
		
		try {
			sessionInfo = sessionManagement.getSessionInfo( sessionIdentifier );
			if( sessionInfo == null )
				throw new NoSessionException(SessionStatus.SESSION_NULL);
			else
				sessionStatus = sessionInfo.getSessionStatus();
		} catch (InputValidationException e1) {
			appRes.logEvent(EventLogMessage.EventType.SESSION_ID_ILLEGAL, new EventLogField( FieldName.SESSION_ID, sessionIdentifier ) );
			throw new GeneralizedException();
		} catch (SQLException e1) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e1 );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e1) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e1 );
			throw new GeneralizedException();
		}
		
		if( sessionStatus != SessionStatus.SESSION_ACTIVE){
			throw new NoSessionException( sessionStatus );
		}
		
		//	0.1a -- Determine if the user is checking their own permissions to an object (this is allowed, of course)
		if( sessionInfo.getUserId() == userId ){
			// Fall through and allow the operation
		}
		
		//	0.1b -- Determine if the user is allowed to check the permissions of another person
		else{
			checkRead( sessionIdentifier, objectId, "Get permissions for user " + userId + " relative to object ID " + objectId );
		}
		
		// 1 -- Perform the operation
		try {
			return accessControl.getUserPermissions( userId, objectId, resolveUserGroupPermissions );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Get the permissions the group has to the given object.
	 * @param sessionIdentifier
	 * @param groupId
	 * @param objectId
	 * @return
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws InsufficientPermissionException
	 */
	public ObjectPermissionDescriptor getGroupPermissions( String sessionIdentifier, int groupId, long objectId ) throws GeneralizedException, InsufficientPermissionException, NoSessionException {
		
		// 0 -- Precondition checks
		
		//	0.1 -- Determine if the user is allowed to check the permissions of the group
		checkRead( sessionIdentifier, objectId, "Get group permissions for group " + groupId + " against object ID " + objectId );

		
		// 1 -- Perform the operation
		try {
			return accessControl.getGroupPermissions( groupId, objectId );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}
	}
	
	
	/**
	 * Set the permissions specified in the access control descriptor for given user.
	 * @param sessionIdentifier
	 * @param groupId
	 * @param objectPermissionDescriptor
	 * @return
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws InsufficientPermissionException
	 */
	public long setPermissions( String sessionIdentifier, ObjectPermissionDescriptor objectPermissionDescriptor) throws GeneralizedException, NoSessionException, InsufficientPermissionException {
		
		// 0 -- Precondition checks

		//	 0.1 -- Make sure an accurate access control descriptor was given
		if( objectPermissionDescriptor == null )
			return -1;
		
		// Make sure that user has the ability to change the ACLs
		checkControl( sessionIdentifier, objectPermissionDescriptor.getObjectId(), "Set ACL" );
		
		// 1 -- Perform the operation
		try {
			long id = accessControl.setPermissions( objectPermissionDescriptor);
			if( id >= 0 )
				appRes.logEvent(EventLogMessage.EventType.ACCESS_CONTROL_ENTRY_SET, new EventLogField( FieldName.OBJECT_ID,  id ) );
			else
				appRes.logEvent(EventLogMessage.EventType.ACCESS_CONTROL_ENTRY_SET_FAILED );
			
			return id;
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}
	}
	
	/**
	 * This method removes the ACLs associated with the user and the given object ID.
	 * @precondition A database connection must be available
	 * @postcondition The ACLs for the given object and user will be deleted
	 * @param sessionIdentifier
	 * @param userId
	 * @param objectId
	 * @return
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws InsufficientPermissionException
	 */
	public boolean deleteUserPermissions( String sessionIdentifier, long userId, long objectId ) throws InsufficientPermissionException, GeneralizedException, NoSessionException {
		// 0 -- Precondition checks
		
		// 0.1 -- Make sure that user has the ability to change the ACLs
		checkControl( sessionIdentifier, objectId, "Remove permissions for user " + userId );
		
		// 1 -- Perform the operation
		try {
			if( accessControl.deleteUserPermissions( userId, objectId) ){
				appRes.logEvent(EventLogMessage.EventType.ACCESS_CONTROL_ENTRY_UNSET, new EventLogField( FieldName.OBJECT_ID, objectId), new EventLogField( FieldName.TARGET_USER_ID , userId ) );
				return true;
			}
			else{
				appRes.logEvent(EventLogMessage.EventType.ACCESS_CONTROL_ENTRY_UNSET_FAILED, new EventLogField( FieldName.OBJECT_ID, objectId), new EventLogField( FieldName.TARGET_USER_ID , userId ) );
				return false;
			}

		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}
	}
	
	/**
	 * This method removes the ACLs associated with the group and the given object ID.
	 * @precondition A database connection must be available
	 * @postcondition The ACLs for the given object and user will be deleted
	 * @param sessionIdentifier
	 * @param userId
	 * @param objectId
	 * @return
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws InsufficientPermissionException
	 */
	public boolean deleteGroupPermissions( String sessionIdentifier, long groupId, long objectId ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		// 0 -- Precondition checks

		//	 0.1 -- Make sure that user has the ability to change the ACLs
		checkControl( sessionIdentifier, objectId, "Remove permissions for group " + groupId );
		
		// 1 -- Perform the operation
		try {
			if( accessControl.deleteGroupPermissions( groupId, objectId ) ){
				appRes.logEvent(EventLogMessage.EventType.ACCESS_CONTROL_ENTRY_UNSET, new EventLogField( FieldName.OBJECT_ID, objectId), new EventLogField( FieldName.GROUP_ID , groupId ) );
				return true;
			}
			else{
				appRes.logEvent(EventLogMessage.EventType.ACCESS_CONTROL_ENTRY_UNSET_FAILED, new EventLogField( FieldName.OBJECT_ID, objectId), new EventLogField( FieldName.GROUP_ID , groupId ) );
				return false;
			}
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Delete the rights for the given group.
	 * @param sessionIdentifier
	 * @param groupId
	 * @param rightName
	 * @return
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws InsufficientPermissionException
	 */
	public boolean deleteGroupRight( String sessionIdentifier, long groupId, String rightName ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		// 0 -- Precondition checks
		
		// Make sure that user has the ability to change the ACLs
		checkRight( sessionIdentifier, "Users.Rights.Edit" );
		
		// 1 -- Perform the operation
		try {
			if( accessControl.deleteGroupRight( groupId, rightName )){
				appRes.logEvent(EventLogMessage.EventType.ACCESS_CONTROL_ENTRY_UNSET, new EventLogField( FieldName.RIGHT, rightName), new EventLogField( FieldName.GROUP_ID , groupId ) );
				return true;
			}
			else{
				appRes.logEvent(EventLogMessage.EventType.ACCESS_CONTROL_ENTRY_UNSET_FAILED, new EventLogField( FieldName.RIGHT, rightName), new EventLogField( FieldName.GROUP_ID , groupId ) );
				return false;
			}
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Delete the rights for the given user.
	 * @param sessionIdentifier
	 * @param userId
	 * @param rightName
	 * @return
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws InsufficientPermissionException
	 */
	public boolean deleteUserRight( String sessionIdentifier, long userId, String rightName ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		// 0 -- Precondition checks

		// Make sure that user has the ability to change the ACLs
		checkRight( sessionIdentifier, "Users.Rights.Edit" );
		
		// 1 -- Perform the operation
		try {
			if( accessControl.deleteUserRight( userId, rightName ) ){
				appRes.logEvent(EventLogMessage.EventType.ACCESS_CONTROL_ENTRY_UNSET, new EventLogField( FieldName.RIGHT, rightName), new EventLogField( FieldName.TARGET_USER_ID , userId ) );
				return true;
			}
			else{
				appRes.logEvent(EventLogMessage.EventType.ACCESS_CONTROL_ENTRY_UNSET_FAILED, new EventLogField( FieldName.RIGHT, rightName), new EventLogField( FieldName.TARGET_USER_ID , userId ) );
				return false;
			}
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}
	}
	
	public boolean setRight( String sessionIdentifier, RightDescriptor rightDescriptor ) throws NoSessionException, GeneralizedException{
		// 0 -- Precondition checks
		SessionStatus sessionStatus = null;
		SessionManagement.SessionInfo sessionInfo = null;
		
		try {
			sessionInfo = sessionManagement.getSessionInfo( sessionIdentifier );
			if( sessionInfo == null )
				throw new NoSessionException(SessionStatus.SESSION_NULL);
			else
				sessionStatus = sessionInfo.getSessionStatus();
		} catch (InputValidationException e1) {
			appRes.logEvent(EventLogMessage.EventType.SESSION_ID_ILLEGAL, new EventLogField( FieldName.SESSION_ID, sessionIdentifier ) );
			throw new GeneralizedException();
		} catch (SQLException e1) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e1 );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e1) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e1 );
			throw new GeneralizedException();
		}
		
		if( sessionStatus != SessionStatus.SESSION_ACTIVE){
			throw new NoSessionException( sessionStatus );
		}
		
		//	0.1 -- Determine if the user has the right to change permissions
		if( sessionInfo.getUserId() == rightDescriptor.getSubjectId() && rightDescriptor.getSubjectType() == AccessControlDescriptor.Subject.USER ){
			// Fall through and allow the operation
			//TODO Determine what rights should exist for allowing/denying changes to user and group rights
			//checkRight( sessionIdentifier, "Administration.ManageRights" ); 
		}
		
		// 1 -- Save the right to the database
		try {
			if( accessControl.setRight(rightDescriptor) ){
				
				String allowed;
				
				if( rightDescriptor.getRight() == RightDescriptor.Action.DENY )
					allowed = "deny";
				else
					allowed = "allow";
				
				if( rightDescriptor.getSubjectType() == AccessControlDescriptor.Subject.USER ){
					appRes.logEvent(EventLogMessage.EventType.ACCESS_CONTROL_ENTRY_SET, new EventLogField[]{
							new EventLogField( EventLogField.FieldName.RIGHT, rightDescriptor.getRightName() ),
							new EventLogField( EventLogField.FieldName.VALUE, allowed ),
							new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, sessionInfo.getUserName() ),
							new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, sessionInfo.getUserId() ),
							new EventLogField( EventLogField.FieldName.TARGET_USER_ID, rightDescriptor.getSubjectId() )} );
					}
				else{
					appRes.logEvent(EventLogMessage.EventType.ACCESS_CONTROL_ENTRY_SET, new EventLogField[]{
							new EventLogField( EventLogField.FieldName.RIGHT, rightDescriptor.getRightName() ),
							new EventLogField( EventLogField.FieldName.VALUE, allowed ),
							new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, sessionInfo.getUserName() ),
							new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, sessionInfo.getUserId() ),
							new EventLogField( EventLogField.FieldName.GROUP_ID, rightDescriptor.getSubjectId() )} );
				}
				
				return true;
			}
			else
				return false;
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Get the access control descriptors that corresponds to the given user.
	 * @precondition The user ID must be valid
	 * @postcondition An array of right descriptors indicating the user's rights will be returned.
	 * @param sessionIdentifier
	 * @param userId
	 * @param resolveUserGroupPermissions
	 * @return
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws InsufficientPermissionException
	 */
	public RightDescriptor[] getUserRights( String sessionIdentifier, int userId, boolean resolveUserGroupPermissions ) throws GeneralizedException, NoSessionException, InsufficientPermissionException{
		// 0 -- Precondition checks

		SessionStatus sessionStatus = null;
		SessionManagement.SessionInfo sessionInfo = null;
		
		try {
			sessionInfo = sessionManagement.getSessionInfo( sessionIdentifier );
			if( sessionInfo == null )
				throw new NoSessionException(SessionStatus.SESSION_NULL);
			else
				sessionStatus = sessionInfo.getSessionStatus();
		} catch (InputValidationException e1) {
			appRes.logEvent(EventLogMessage.EventType.SESSION_ID_ILLEGAL, new EventLogField( FieldName.SESSION_ID, sessionIdentifier ) );
			throw new GeneralizedException();
		} catch (SQLException e1) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e1 );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e1) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e1 );
			throw new GeneralizedException();
		}
		
		if( sessionStatus != SessionStatus.SESSION_ACTIVE){
			throw new NoSessionException( sessionStatus );
		}
		
		//	0.1a -- Determine if the user is checking their own permissions to an object (this is allowed, of course)
		if( sessionInfo.getUserId() == userId ){
			// Fall through and allow the operation
		}
		
		//	0.1b -- Determine if the user is allowed to check the permissions of another person
		else{
			checkRight( sessionIdentifier, "Users.Rights.View" );
		}
		
		// 1 -- Perform the operation
		try {
			return accessControl.getUserRights( userId, resolveUserGroupPermissions );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Get the right descriptor that corresponds to the given user. Returns null if the right does not exist.
	 * @precondition The user ID and the right name must be valid
	 * @postcondition An access control descriptor indicating the user's rights will be returned or null if the right could not be found
	 * @param sessionIdentifier
	 * @param userId
	 * @param right
	 * @param recurseGroupAcls
	 * @return
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws InsufficientPermissionException
	 * @throws NotFoundException 
	 */
	public RightDescriptor getUserRight( String sessionIdentifier, int userId, String right, boolean resolveUserGroupPermissions ) throws GeneralizedException, NoSessionException, InsufficientPermissionException, NotFoundException{
		// 0 -- Precondition checks

		SessionStatus sessionStatus = null;
		SessionManagement.SessionInfo sessionInfo = null;
		
		try {
			sessionInfo = sessionManagement.getSessionInfo( sessionIdentifier );
			if( sessionInfo == null )
				throw new NoSessionException(SessionStatus.SESSION_NULL);
			else
				sessionStatus = sessionInfo.getSessionStatus();
		} catch (InputValidationException e1) {
			appRes.logEvent(EventLogMessage.EventType.SESSION_ID_ILLEGAL, new EventLogField( FieldName.SESSION_ID, sessionIdentifier ) );
			throw new GeneralizedException();
		} catch (SQLException e1) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e1 );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e1) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e1 );
			throw new GeneralizedException();
		}
		
		if( sessionStatus != SessionStatus.SESSION_ACTIVE){
			throw new NoSessionException( sessionStatus );
		}
		
		//	0.1a -- Determine if the user is checking their own permissions to an object (this is allowed, of course)
		if( sessionInfo.getUserId() == userId ){
			// Fall through and allow the operation
		}
		
		//	0.1b -- Determine if the user is allowed to check the permissions of another person
		else{
			checkRight( sessionIdentifier, "Users.Rights.View" );
		}
		
		// 1 -- Perform the operation
		try {
			return accessControl.getUserRight( userId, right, resolveUserGroupPermissions );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Get the right descriptor that corresponds to the given group.
	 * @param sessionIdentifier
	 * @param userId
	 * @param right
	 * @param recurseGroupAcls
	 * @return
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws InsufficientPermissionException
	 * @throws NotFoundException 
	 */
	public RightDescriptor getGroupRight( String sessionIdentifier, int groupId, String right ) throws GeneralizedException, InsufficientPermissionException, NoSessionException, NotFoundException{
		// 0 -- Precondition checks
		
		// Make sure that user has the ability to view the ACLs
		checkRight( sessionIdentifier, "Users.Rights.View" );
		
		// 1 -- Perform the operation
		try {
			return accessControl.getGroupRight( groupId, right );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Set the right ACLs for the given user.
	 * @param sessionIdentifier
	 * @param userId
	 * @param rightName
	 * @param rightDescriptor
	 * @return
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws InsufficientPermissionException
	 */
	public boolean setPermissions( String sessionIdentifier, RightDescriptor rightDescriptor) throws InsufficientPermissionException, GeneralizedException, NoSessionException {
		// 0 -- Precondition checks

		//	 0.1 -- Make sure the access control descriptor is valid
		if( rightDescriptor == null )
			return false;
		
		// Make sure that user has the ability to change the ACLs
		checkRight( sessionIdentifier, rightDescriptor.getRightName() );
		
		// 1 -- Perform the operation
		try {
			if( accessControl.setPermissions( rightDescriptor) ){
				if( rightDescriptor.isUser() ){
					appRes.logEvent(EventLogMessage.EventType.ACCESS_CONTROL_ENTRY_UNSET, new EventLogField(FieldName.RIGHT, rightDescriptor.getRightName()), new EventLogField(FieldName.TARGET_USER_ID, rightDescriptor.getSubjectId()));
				}
				else{
					appRes.logEvent(EventLogMessage.EventType.ACCESS_CONTROL_ENTRY_UNSET, new EventLogField(FieldName.RIGHT, rightDescriptor.getRightName()), new EventLogField(FieldName.GROUP_ID, rightDescriptor.getSubjectId()));
				}
				
				return true;
			}
			else{
				if( rightDescriptor.isUser() ){
					appRes.logEvent(EventLogMessage.EventType.ACCESS_CONTROL_ENTRY_UNSET_FAILED, new EventLogField(FieldName.RIGHT, rightDescriptor.getRightName()), new EventLogField(FieldName.TARGET_USER_ID, rightDescriptor.getSubjectId()));
				}
				else{
					appRes.logEvent(EventLogMessage.EventType.ACCESS_CONTROL_ENTRY_UNSET_FAILED, new EventLogField(FieldName.RIGHT, rightDescriptor.getRightName()), new EventLogField(FieldName.GROUP_ID, rightDescriptor.getSubjectId()));
				}
				
				return false;
			}
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}
	}
	
	
	/**
	 *  This method retrieves all ACL entries for the given object.
	 * @param sessionIdentifier
	 * @param objectId
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 */
	public ObjectPermissionDescriptor[] getAllAclEntries( String sessionIdentifier, long objectId ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		// 0 -- Precondition check
		checkControl( sessionIdentifier, objectId, "Retrieve ACLs" );
		
		// 1 -- Get the ACL
		try {
			return accessControl.getAllAclEntries( objectId );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}
		
	}
	
}
