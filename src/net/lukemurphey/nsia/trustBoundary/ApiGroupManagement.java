package net.lukemurphey.nsia.trustBoundary;

import java.sql.SQLException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.GroupManagement;
import net.lukemurphey.nsia.GroupMembershipDescriptor;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.UserManagement.UserDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;

public class ApiGroupManagement extends ApiHandler {

	public ApiGroupManagement(Application appRes) {
		super(appRes);
	}
	
	/**
	 * Resolve the group identifier associated with the group name.
	 * @param sessionIdentifier
	 * @param groupName
	 * @return
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws InsufficientPermissionException
	 * @throws InputValidationException
	 */
	public int getGroupId( String sessionIdentifier, String groupName ) throws GeneralizedException, NoSessionException, InsufficientPermissionException, InputValidationException{
		
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		//	 0.2 -- Make sure the user has permission
		UserDescriptor userDesc = getUserInfo( sessionIdentifier );
		String requesterUserName = userDesc.getUserName();
		int requesterUserId = userDesc.getUserID();
		checkRight( sessionIdentifier, "Groups.View" );
		
		//	 0.3 -- Make sure the user name is valid
		checkGroupName( requesterUserName, requesterUserId, groupName);
		
		try {
			return groupManagement.getGroupID( groupName );
		}catch (SQLException e){
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		}catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Create a new users group with the given attributes.
	 * @param sessionIdentifier
	 * @param groupName
	 * @param groupDescription
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws InputValidationException
	 * @throws GeneralizedException 
	 * @throws InsufficientPermissionException 
	 * @throws NoSessionException 
	 * @throws InputValidationException 
	 */
	public int addGroup( String sessionIdentifier, String groupName, String groupDescription) throws GeneralizedException, InsufficientPermissionException, NoSessionException, InputValidationException{
		
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		//	 0.2 -- Make sure the user has permission
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		String requesterUserName = userDescriptor.getUserName();
		int requesterUserId = userDescriptor.getUserID();
		checkRight( sessionIdentifier, "Groups.Add" );
		
		//	 0.3 -- Make sure the group name is valid
		checkGroupName( requesterUserName, requesterUserId, groupName );
		
		//	1 -- Try to update the account
		try {
			
			int newGroupId = groupManagement.addGroup( groupName, groupDescription );
			if( newGroupId > -1 ){
				appRes.logEvent(EventLogMessage.Category.GROUP_ADDED,
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName()),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID()),
						new EventLogField( FieldName.GROUP_ID, newGroupId) );
				
				return newGroupId;
			}
			else{

				appRes.logEvent(EventLogMessage.Category.OPERATION_FAILED, new EventLogField[]{
						new EventLogField( EventLogField.FieldName.OPERATION, "Add new group" ),
						new EventLogField( EventLogField.FieldName.GROUP_NAME, groupName ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, userDescriptor.getUserID() )} );
						
				return -1;
			}
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		} catch (IllegalArgumentException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Update the information associated with the given user
	 * @param sessionIdentifier
	 * @param groupId
	 * @param groupName
	 * @param groupDescription
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws InputValidationException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws NoSessionException
	 */
	public boolean updateGroupInfo( String sessionIdentifier, int groupId, String groupName, String groupDescription) throws InputValidationException, GeneralizedException, InsufficientPermissionException, NoSessionException{
		
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		//	 0.2 -- Make sure the user has permission
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		String requesterUserName = userDescriptor.getUserName();
		int requesterUserId = userDescriptor.getUserID();
		checkRight( sessionIdentifier, "Groups.Edit" );
		
		//	 0.3 -- Make sure the group name is valid
		checkGroupName( requesterUserName, requesterUserId, groupName );
		
		//	1 -- Try to update the account
		try {
			
			boolean updateStatus = groupManagement.updateGroupInfo( groupId, groupName, groupDescription );
			if( updateStatus ){
				
				appRes.logEvent(EventLogMessage.Category.GROUP_MODIFIED, new EventLogField[]{
						new EventLogField( EventLogField.FieldName.GROUP_ID, groupId ),
						new EventLogField( EventLogField.FieldName.GROUP_NAME, groupName ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, userDescriptor.getUserID() )} );
				
				return true;
			}
			else{
				
				appRes.logEvent(EventLogMessage.Category.OPERATION_FAILED, new EventLogField[]{
						new EventLogField( EventLogField.FieldName.OPERATION, "Update group" ),
						new EventLogField( EventLogField.FieldName.GROUP_ID, groupId ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, userDescriptor.getUserID() )} );
				
				return false;
			}
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		} catch (IllegalArgumentException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Disable the associated group. All users who are members of this group will no longer be affected by ACLs that apply
	 * to this group. The application shall act as if this group no longer exists.
	 * @param sessionIdentifier
	 * @param groupId
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public boolean disableGroup( String sessionIdentifier, int groupId ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		//	 0.2 -- Make sure the user has permission
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		checkRight( sessionIdentifier, "Groups.Edit" );
		
		try{
			if( groupManagement.disableGroup( groupId ) ){
				
				appRes.logEvent(EventLogMessage.Category.GROUP_DISABLED, new EventLogField[]{
						new EventLogField( EventLogField.FieldName.GROUP_ID, groupId ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, userDescriptor.getUserID() )} );
				
				return true;
			}
			else{
				
				appRes.logEvent(EventLogMessage.Category.GROUP_ID_INVALID, new EventLogField[]{
						new EventLogField( EventLogField.FieldName.GROUP_ID, groupId ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, userDescriptor.getUserID() )} );
				
				return true;
			}
		}catch (SQLException e){
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		}catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}
	}
	
	public boolean deleteGroup( String sessionIdentifier, int groupId ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		//	 0.2 -- Make sure the user has permission
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		checkRight( sessionIdentifier, "Groups.Delete" );
		
		try{
			if( groupManagement.deleteGroup( groupId ) ){
				
				appRes.logEvent(EventLogMessage.Category.GROUP_DELETED, new EventLogField[]{
						new EventLogField( EventLogField.FieldName.GROUP_ID, groupId ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, userDescriptor.getUserID() )} );
				
				return true;
			}
			else{
				
				appRes.logEvent(EventLogMessage.Category.GROUP_ID_INVALID, new EventLogField[]{
						new EventLogField( EventLogField.FieldName.GROUP_ID, groupId ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, userDescriptor.getUserID() )} );
				
				return true;
			}
		}catch (SQLException e){
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		}catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Re-enable the associated group. All users who are members of this group will no now be affected by ACLs that apply
	 * to this group.
	 * @param sessionIdentifier
	 * @param groupId
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public boolean enableGroup( String sessionIdentifier, int groupId ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		//	 0.2 -- Make sure the user has permission
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		checkRight( sessionIdentifier, "Groups.Edit" );
		
		try{
			if( groupManagement.enableGroup( groupId ) ){

				appRes.logEvent(EventLogMessage.Category.GROUP_REENABLED, new EventLogField[]{
						new EventLogField( EventLogField.FieldName.GROUP_ID, groupId ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, userDescriptor.getUserID() )} );
				
				return true;
			}
			else{
				
				appRes.logEvent(EventLogMessage.Category.GROUP_ID_INVALID, new EventLogField[]{
						new EventLogField( EventLogField.FieldName.GROUP_ID, groupId ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, userDescriptor.getUserID() )} );
				
				
				return true;
			}
		}catch (SQLException e){
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		}catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}
	}
	
	/**
	 * The simple group descriptor is a subset of the complete group descriptor. This version includes only
	 * the information that is available to all users of the system and thus does not need read permissions
	 * for group management.
	 * @param sessionIdentifier
	 * @param groupId
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws NotFoundException 
	 */
	public SimpleGroupDescriptor getSimpleGroupDescriptor( String sessionIdentifier, int groupId ) throws NoSessionException, GeneralizedException, NotFoundException{
		checkSession(sessionIdentifier);
		
		GroupManagement.GroupDescriptor groupDesc;
		
		try{
			groupDesc = groupManagement.getGroupDescriptor(groupId);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		
		if( groupDesc != null){
			return new SimpleGroupDescriptor(groupDesc);
		}
		else
			return null;
	}
	
	/**
	 * Get the group descriptor for the group with the given group identifier.
	 * @param sessionIdentifier
	 * @param groupId
	 * @return
	 * @throws SQLException
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws NotFoundException 
	 */
	public GroupManagement.GroupDescriptor getGroupDescriptor( String sessionIdentifier, int groupId ) throws NoSessionException, GeneralizedException, InsufficientPermissionException, NotFoundException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		//	 0.2 -- Make sure the user has permission
		checkRight( sessionIdentifier, "Groups.View" );
		
		//	1 -- Get the user information
		GroupManagement.GroupDescriptor groupDescriptor;
		try {
			groupDescriptor = groupManagement.getGroupDescriptor( groupId );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		
		return groupDescriptor;
	}
	
	/**
	 * Get the group descriptor for the group with the given group identifier.
	 * @param sessionIdentifier
	 * @param groupId
	 * @return
	 * @throws SQLException
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws NotFoundException 
	 */
	public GroupManagement.GroupDescriptor getGroupDescriptor( String sessionIdentifier, String groupName ) throws SQLException, NoSessionException, GeneralizedException, InsufficientPermissionException, NotFoundException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		//	 0.2 -- Make sure the user has permission
		checkRight( sessionIdentifier, "Groups.View" );
		
		//	1 -- Get the user information
		GroupManagement.GroupDescriptor groupDescriptor;
		try {
			groupDescriptor = groupManagement.getGroupDescriptor( groupName );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		
		return groupDescriptor;
	}
	
	/**
	 * Get a descriptor indicating which group a user is a member of.
	 * @param sessionIdentifier
	 * @param userId
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	public GroupMembershipDescriptor getGroupMembership( String sessionIdentifier, int userId ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		checkRight( sessionIdentifier, "Groups.View");
		
		try {
			return groupManagement.getGroupMembership( userId );
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Method adds the specified user to the given group.
	 * @param sessionIdentifier
	 * @param userId
	 * @param groupId
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws ApplicationException
	 */
	public boolean addUserToGroup( String sessionIdentifier, int userId, int groupId ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		//	 0.2 -- Make sure the user has permission
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		checkRight( sessionIdentifier, "Groups.Membership.Edit" );
		
		//	1 -- Add the user to the group
		try{
			if( groupManagement.addUserToGroup( userId, groupId )){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_ENTRY_SET, new EventLogField[]{
						new EventLogField( EventLogField.FieldName.GROUP_ID, groupId ),
						new EventLogField( EventLogField.FieldName.TARGET_USER_ID, userId ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, userDescriptor.getUserID() )} );

				return true;
			}
			else{
				
				appRes.logEvent(EventLogMessage.Category.OPERATION_FAILED, new EventLogField[]{
						new EventLogField( EventLogField.FieldName.OPERATION, "Add user to group" ),
						new EventLogField( EventLogField.FieldName.GROUP_ID, groupId ),
						new EventLogField( EventLogField.FieldName.TARGET_USER_ID, userId ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, userDescriptor.getUserID() )} );
				
				return false;
			}
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		}
		catch (NotFoundException e) {
			
			appRes.logEvent(EventLogMessage.Category.OPERATION_FAILED, new EventLogField[]{
					new EventLogField( EventLogField.FieldName.OPERATION, "Add user to group" ),
					new EventLogField( EventLogField.FieldName.GROUP_ID, groupId ),
					new EventLogField( EventLogField.FieldName.MESSAGE, "Group not found" ),
					new EventLogField( EventLogField.FieldName.TARGET_USER_ID, userId ),
					new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
					new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, userDescriptor.getUserID() )});
			
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Method removes the specified users from the given group.
	 * @param sessionIdentifier
	 * @param userId
	 * @param groupId
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws ApplicationException
	 */
	public boolean removeUserFromGroup( String sessionIdentifier, int userId, int groupId ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		//	 0.2 -- Make sure the user has permission
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		checkRight( sessionIdentifier, "Groups.Membership.Edit" );
		
		//	1 -- Add the user to the group
		try {
			if( groupManagement.removeUserFromGroup( userId, groupId ) ){
				
				appRes.logEvent(EventLogMessage.Category.USER_REMOVED_FROM_GROUP, new EventLogField[]{
						new EventLogField( EventLogField.FieldName.GROUP_ID, groupId ),
						new EventLogField( EventLogField.FieldName.TARGET_USER_ID, userId ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, userDescriptor.getUserID() )});
				
				return true;
			}
			else{
				
				appRes.logEvent(EventLogMessage.Category.OPERATION_FAILED, new EventLogField[]{
						new EventLogField( EventLogField.FieldName.OPERATION, "Remove user from group" ),
						new EventLogField( EventLogField.FieldName.GROUP_ID, groupId ),
						new EventLogField( EventLogField.FieldName.TARGET_USER_ID, userId ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, userDescriptor.getUserID() )});
				
				return false;
			}
			
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		}catch (NotFoundException e) {
			
			appRes.logEvent(EventLogMessage.Category.OPERATION_FAILED, new EventLogField[]{
					new EventLogField( EventLogField.FieldName.OPERATION, "Remove user from group" ),
					new EventLogField( EventLogField.FieldName.GROUP_ID, groupId ),
					new EventLogField( EventLogField.FieldName.MESSAGE, "Group not found" ),
					new EventLogField( EventLogField.FieldName.TARGET_USER_ID, userId ),
					new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
					new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, userDescriptor.getUserID() )});
			
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		
	}
	
	/**
	 * This method allows a group identifier to be resolved to a group name. This operation is allowed even if
	 * the user does not have read permissions for the groups.
	 * @param groupId
	 * @return
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 * @throws NotFoundException 
	 */
	public String resolveGroupName(String sessionIdentifier, int groupId) throws GeneralizedException, NoSessionException, NotFoundException{
		checkSession(sessionIdentifier);
		
		GroupManagement.GroupDescriptor groupDesc;
		
		try{
			groupDesc= groupManagement.getGroupDescriptor(groupId);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		
		if( groupDesc == null)
			return null;
		else
			return groupDesc.getGroupName();
	}
	
	/**
	 * Retrieve all of the group descriptors that currently exist.
	 * @param sessionIdentifier
	 * @return
	 * @throws SQLException
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public GroupManagement.GroupDescriptor[] getGroupDescriptors( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		//	 0.2 -- Make sure the user has permission
		//Hashtable hashtable = getUserInfo( sessionIdentifier );
		checkRight( sessionIdentifier, "Groups.View" );
		
		//	1 -- Get the group descriptors information
		GroupManagement.GroupDescriptor[] groupDescriptors;
		try {
			groupDescriptors = groupManagement.getGroupDescriptors( );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		
		
		return groupDescriptors;
	}
	
	public SimpleGroupDescriptor[] getSimpleGroupDescriptors(String sessionIdentifier) throws NoSessionException, GeneralizedException{
		GroupManagement.GroupDescriptor[] groupDescriptors;
		
		//checkControl(sessionIdentifier);
		
		try {
			groupDescriptors = groupManagement.getGroupDescriptors( );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		
		SimpleGroupDescriptor[] simpleGroupDescs = new SimpleGroupDescriptor[groupDescriptors.length];
		
		for(int c =0; c < groupDescriptors.length; c++){
			simpleGroupDescs[c] = new SimpleGroupDescriptor(groupDescriptors[c]);
		}
		
		return simpleGroupDescs;
	}

}
