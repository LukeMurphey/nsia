package net.lukemurphey.nsia.xmlRpcInterface;

import java.sql.SQLException;
import java.util.*;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.GroupManagement;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.trustBoundary.ApiGroupManagement;

public class XmlrpcGroupManagement extends XmlrpcHandler {

	private ApiGroupManagement untrustGroupManagement;
	
	public XmlrpcGroupManagement(Application appRes) {
		super(appRes);
		
		untrustGroupManagement = new ApiGroupManagement( appRes );
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
		
		return untrustGroupManagement.getGroupId( sessionIdentifier, groupName );
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
		return untrustGroupManagement.addGroup( sessionIdentifier, groupName, groupDescription);
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
	public boolean updateGroupInfo( String sessionIdentifier, int groupId, String groupName, String groupDescription) throws SQLException, NoDatabaseConnectionException, InputValidationException, GeneralizedException, InsufficientPermissionException, NoSessionException{
		return untrustGroupManagement.updateGroupInfo( sessionIdentifier, groupId, groupName, groupDescription);
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
		return untrustGroupManagement.disableGroup( sessionIdentifier, groupId );
	}
	
	/**
	 * Delete the group with the given identifier.
	 * @param sessionIdentifier
	 * @param groupId
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public boolean deleteGroup( String sessionIdentifier, int groupId ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		return untrustGroupManagement.deleteGroup( sessionIdentifier, groupId );
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
		return untrustGroupManagement.enableGroup( sessionIdentifier, groupId );
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
	public Hashtable<String, Object> getGroupDescriptor( String sessionIdentifier, int groupId ) throws SQLException, NoSessionException, GeneralizedException, InsufficientPermissionException, NotFoundException{

		//	1 -- Get the user information
		GroupManagement.GroupDescriptor groupDescriptor;
		groupDescriptor = untrustGroupManagement.getGroupDescriptor( sessionIdentifier, groupId );
		
		Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
		
		hashtable.put( "GroupName", groupDescriptor.getGroupName());
		hashtable.put( "GroupDescription", groupDescriptor.getDescription());
		hashtable.put( "GroupID", Integer.valueOf( groupDescriptor.getGroupId() ));
		hashtable.put( "GroupStatus", Integer.valueOf( groupDescriptor.getGroupState().ordinal() ) );
		
		return hashtable;
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
	public Hashtable<String, Object> getGroupDescriptor( String sessionIdentifier, String groupName ) throws SQLException, NoSessionException, GeneralizedException, InsufficientPermissionException, NotFoundException{

		//	1 -- Get the user information
		GroupManagement.GroupDescriptor groupDescriptor;
		groupDescriptor = untrustGroupManagement.getGroupDescriptor( sessionIdentifier, groupName );
		
		Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
		hashtable.put( "GroupName", groupDescriptor.getGroupName());
		hashtable.put( "GroupDescription", groupDescriptor.getDescription());
		hashtable.put( "GroupID", Integer.valueOf( groupDescriptor.getGroupId() ));
		hashtable.put( "GroupStatus", Integer.valueOf( groupDescriptor.getGroupState().ordinal() ) );
		
		return hashtable;
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
	 */
	public boolean addUserToGroup( String sessionIdentifier, int userId, int groupId ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		return untrustGroupManagement.addUserToGroup( sessionIdentifier, userId, groupId);
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
	 */
	public boolean removeUserFromGroup( String sessionIdentifier, int userId, int groupId ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		return untrustGroupManagement.removeUserFromGroup( sessionIdentifier, userId, groupId );
		
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
	public Vector<Hashtable<String, Object>> getGroupDescriptors( String sessionIdentifier ) throws SQLException, NoSessionException, GeneralizedException, InsufficientPermissionException{
		GroupManagement.GroupDescriptor[] groupDescriptors = untrustGroupManagement.getGroupDescriptors( sessionIdentifier );
		
		Vector<Hashtable<String, Object>> groupDescVector = new Vector<Hashtable<String, Object>>();
		for ( int c = 0; c < groupDescriptors.length; c++){
			Hashtable<String, Object> groupDesc = new Hashtable<String, Object>();
			
			GroupManagement.GroupDescriptor groupDescriptor = groupDescriptors[c]; 
			groupDesc.put( "GroupName", groupDescriptor.getGroupName());
			groupDesc.put( "GroupDescription", groupDescriptor.getDescription());
			groupDesc.put( "GroupID", Integer.valueOf( groupDescriptor.getGroupId() ));
			groupDesc.put( "GroupStatus", Integer.valueOf( groupDescriptor.getGroupState().ordinal()) );
			
			groupDescVector.add(groupDesc);
		}
		
		return groupDescVector;
	}

}
