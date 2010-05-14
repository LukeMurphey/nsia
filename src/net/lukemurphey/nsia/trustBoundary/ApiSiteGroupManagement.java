package net.lukemurphey.nsia.trustBoundary;

import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.SiteGroupManagement;
import net.lukemurphey.nsia.UserManagement.UserDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;

public class ApiSiteGroupManagement extends ApiHandler {
	
	public ApiSiteGroupManagement(Application appRes) {
		super(appRes);
	}
	
	/**
	 * Ensure that the site group name is valid.
	 * @param requesterUserName
	 * @param requesterUserId
	 * @param siteGroupName
	 * @throws InputValidationException 
	 */
	private void checkSiteGroupName( String requesterUserName, long requesterUserId, String siteGroupName) throws InputValidationException {
		if( siteGroupName == null || siteGroupName.length() == 0 )
			throw new InputValidationException("Site group name is invalid (null)", "GroupName", "" );
		
		if( siteGroupName.length() == 0 )
			throw new InputValidationException("Site group name is invalid (empty string)", "GroupName", "" );
		
		Pattern groupNamePattern = Pattern.compile( SiteGroupManagement.SITE_GROUP_NAME_REGEX);
		Matcher matcher = groupNamePattern.matcher( siteGroupName );
		if( !matcher.matches() )
			throw new InputValidationException("Site group name contains invalid characters", "GroupName", siteGroupName );
		
	}
	
	/**
	 * Resolve the group identifier associated with the site group name.
	 * @param sessionIdentifier
	 * @param groupName
	 * @return
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws InsufficientPermissionException
	 * @throws InputValidationException
	 */
	public long getGroupId( String sessionIdentifier, String groupName ) throws GeneralizedException, NoSessionException, InsufficientPermissionException, InputValidationException{
		
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		//	 0.2 -- Make sure the user has permission
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		String requesterUserName = userDescriptor.getUserName();
		long requesterUserId = userDescriptor.getUserID();
		checkRight( sessionIdentifier, "SiteGroups.View" );
		
		//	 0.3 -- Make sure the site group name is valid
		checkSiteGroupName( requesterUserName, requesterUserId, groupName);
		
		try {
			return siteGroupManagement.getGroupID( groupName );
		}catch (SQLException e){
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		}catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
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
		long requesterUserId = userDescriptor.getUserID();
		checkRight( sessionIdentifier, "SiteGroups.Add" );
		
		//	 0.3 -- Make sure the group name is valid
		checkSiteGroupName( requesterUserName, requesterUserId, groupName );
		
		//	1 -- Try to update the account
		try {
			
			int newGroupId = siteGroupManagement.addGroup( groupName, groupDescription );
			if( newGroupId > -1 ){
				
				appRes.logEvent(EventLogMessage.EventType.SITE_GROUP_ADDED,
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
						new EventLogField( FieldName.SITE_GROUP_NAME, groupName ),
						new EventLogField( FieldName.SITE_GROUP_ID, newGroupId ) );
				
				return newGroupId;
			}
			else{
				
				appRes.logEvent(EventLogMessage.EventType.OPERATION_FAILED,
						new EventLogField( FieldName.OPERATION, "Add new site group" ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
						new EventLogField( FieldName.SITE_GROUP_NAME, groupName ) );
				
				return -1;
			}
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (IllegalArgumentException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
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
		long requesterUserId = userDescriptor.getUserID();
		checkRight( sessionIdentifier, "SiteGroups.Edit" );
		
		//	 0.3 -- Make sure the group name is valid
		checkSiteGroupName( requesterUserName, requesterUserId, groupName );
		
		//	1 -- Try to update the account
		try {
			
			boolean updateStatus = siteGroupManagement.updateGroupInfo( groupId, groupName, groupDescription );
			if( updateStatus ){
				
				appRes.logEvent(EventLogMessage.EventType.SITE_GROUP_MODIFIED,
						new EventLogField( FieldName.SITE_GROUP_ID, groupId ),
						new EventLogField( FieldName.SITE_GROUP_NAME, groupName ) ,
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() )
						);
				
				return true;
			}
			else{
				
				appRes.logEvent(EventLogMessage.EventType.OPERATION_FAILED, new EventLogField[] {
						new EventLogField( FieldName.OPERATION, "Update site group" ),
						new EventLogField( FieldName.SITE_GROUP_ID, groupId ),
						new EventLogField( FieldName.SITE_GROUP_NAME, groupName ) ,
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ) }
						);
				
				return false;
			}
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		} catch (IllegalArgumentException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Deletethe site group and all of the associated rules.
	 * @param sessionIdentifier
	 * @param groupId
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public boolean deleteGroup( String sessionIdentifier, int groupId ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		//	 0.2 -- Make sure the user has permission
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		checkRight( sessionIdentifier, "SiteGroups.Delete" );
		
		try{
			if( siteGroupManagement.deleteGroup( groupId ) ){
				
				appRes.logEvent(EventLogMessage.EventType.SITE_GROUP_DELETED,
						new EventLogField( FieldName.SITE_GROUP_ID, groupId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() )
						);
				
				return true;
			}
			else{
				
				appRes.logEvent(EventLogMessage.EventType.SITE_GROUP_ID_INVALID,
						new EventLogField( FieldName.SITE_GROUP_ID, groupId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() )
						);
				
				return true;
			}
		}catch (SQLException e){
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		}catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Disable the associated site group. The rules in this group will no longer be evaluated automatically.
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
		checkRight( sessionIdentifier, "SiteGroups.Edit" );
		
		try{
			if( siteGroupManagement.disableGroup( groupId ) ){
				
				appRes.logEvent(EventLogMessage.EventType.SITE_GROUP_DISABLED,
						new EventLogField( FieldName.SITE_GROUP_ID, groupId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ) );
				
				return true;
			}
			else{
				
				appRes.logEvent(EventLogMessage.EventType.SITE_GROUP_ID_INVALID,
						new EventLogField( FieldName.SITE_GROUP_ID, groupId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ) );
				
				return true;
			}
		}catch (SQLException e){
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		}catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e );
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
		checkRight( sessionIdentifier, "SiteGroups.Edit" );
		
		try{
			if( siteGroupManagement.enableGroup( groupId ) ){
				
				appRes.logEvent(EventLogMessage.EventType.SITE_GROUP_REENABLED,
						new EventLogField( FieldName.SITE_GROUP_ID, groupId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ) );
				
				return true;
			}
			else{
				
				appRes.logEvent(EventLogMessage.EventType.SITE_GROUP_ID_INVALID,
						new EventLogField( FieldName.SITE_GROUP_ID, groupId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ) );
				
				return true;
			}
		}catch (SQLException e){
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		}catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		}
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
	public SiteGroupManagement.SiteGroupDescriptor getGroupDescriptor( String sessionIdentifier, int groupId ) throws NoSessionException, GeneralizedException, InsufficientPermissionException, NotFoundException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		//	 0.2 -- Make sure the user has permission
		checkRight( sessionIdentifier, "SiteGroups.View" );
		
		//	1 -- Get the user information
		SiteGroupManagement.SiteGroupDescriptor groupDescriptor;
		try {
			groupDescriptor = siteGroupManagement.getGroupDescriptor( groupId );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
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
	public SiteGroupManagement.SiteGroupDescriptor getGroupDescriptor( String sessionIdentifier, String groupName ) throws NoSessionException, GeneralizedException, InsufficientPermissionException, NotFoundException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		//	 0.2 -- Make sure the user has permission
		checkRight( sessionIdentifier, "SiteGroups.View" );
		
		//	1 -- Get the user information
		SiteGroupManagement.SiteGroupDescriptor groupDescriptor;
		try {
			groupDescriptor = siteGroupManagement.getGroupDescriptor( groupName );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		
		return groupDescriptor;
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
	public SiteGroupManagement.SiteGroupDescriptor[] getGroupDescriptors( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		//	 0.2 -- Make sure the user has permission
		checkRight( sessionIdentifier, "SiteGroups.View" );
		
		//	1 -- Get the group descriptors information
		SiteGroupManagement.SiteGroupDescriptor[] groupDescriptors;
		try {
			groupDescriptors = siteGroupManagement.getGroupDescriptors( );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		
		return groupDescriptors;
	}
	
}
