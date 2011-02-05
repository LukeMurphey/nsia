package net.lukemurphey.nsia.xmlRpcInterface;

import java.util.*;
import java.sql.*;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.SiteGroupManagement;
import net.lukemurphey.nsia.SiteGroupManagement.SiteGroupDescriptor;
import net.lukemurphey.nsia.trustBoundary.ApiSiteGroupManagement;

public class XmlrpcSiteGroupManagement extends XmlrpcHandler {
	
	private ApiSiteGroupManagement siteGroupManager;
	
	public XmlrpcSiteGroupManagement(Application appRes) {
		super(appRes);
		
		siteGroupManager = new ApiSiteGroupManagement( appRes );
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
		return siteGroupManager.getGroupId( sessionIdentifier, groupName );
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
	public long addGroup( String sessionIdentifier, String groupName, String groupDescription) throws GeneralizedException, InsufficientPermissionException, NoSessionException, InputValidationException{
		return siteGroupManager.addGroup(sessionIdentifier, groupName, groupDescription );
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
		return siteGroupManager.updateGroupInfo( sessionIdentifier, groupId, groupName, groupDescription );
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
		return siteGroupManager.disableGroup( sessionIdentifier, groupId);
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
		return siteGroupManager.enableGroup( sessionIdentifier, groupId);
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

		//Hashtable hashtable = new Hashtable();
		
		//	1 -- Get the user information
		SiteGroupManagement.SiteGroupDescriptor groupDescriptor = siteGroupManager.getGroupDescriptor( sessionIdentifier, groupId);
		
		/*hashtable.put( "SiteGroupName", groupDescriptor.getGroupName());
		hashtable.put( "SiteGroupDescription", groupDescriptor.getDescription());
		hashtable.put( "SiteGroupID", new Integer( (int)groupDescriptor.getGroupId() ));
		hashtable.put( "SiteGroupStatus", new Integer( groupDescriptor.getGroupState() ));*/
		
		return groupDescriptor.toHashtable();
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

		Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
		SiteGroupDescriptor groupDescriptor = siteGroupManager.getGroupDescriptor(sessionIdentifier, groupName);
		
		hashtable.put( "SiteGroupName", groupDescriptor.getGroupName());
		hashtable.put( "SiteGroupDescription", groupDescriptor.getDescription());
		hashtable.put( "SiteGroupID", Integer.valueOf( groupDescriptor.getGroupId() ));
		hashtable.put( "SiteGroupStatus", Integer.valueOf( groupDescriptor.getGroupState().ordinal() ) );
		
		return hashtable;
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
		//	1 -- Get the group descriptors information
		SiteGroupManagement.SiteGroupDescriptor[] groupDescriptors = siteGroupManager.getGroupDescriptors(sessionIdentifier);
		
		Vector<Hashtable<String, Object>> groupDescVector = new Vector<Hashtable<String, Object>>();
		for ( int c = 0; c < groupDescriptors.length; c++){
			Hashtable<String, Object> groupDesc = new Hashtable<String, Object>();
			
			SiteGroupManagement.SiteGroupDescriptor groupDescriptor = groupDescriptors[c]; 
			groupDesc.put( "SiteGroupName", groupDescriptor.getGroupName());
			groupDesc.put( "SiteGroupDescription", groupDescriptor.getDescription());
			groupDesc.put( "SiteGroupID", Integer.valueOf( groupDescriptor.getGroupId() ));
			groupDesc.put( "SiteGroupStatus", Integer.valueOf( groupDescriptor.getGroupState().ordinal() ) );
			
			groupDescVector.add(groupDesc);
		}
		
		return groupDescVector;
	}
	
}
