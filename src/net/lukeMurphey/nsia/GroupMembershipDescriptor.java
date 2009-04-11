package net.lukeMurphey.nsia;

import net.lukeMurphey.nsia.GroupManagement.GroupDescriptor;

import java.sql.*;

/**
 * The group membership descriptor indicates which groups a given user is member of.
 * @author luke
 *
 */
public class GroupMembershipDescriptor {
	
	GroupDescriptor[] groupDescriptors;
	boolean[] isMember;
	int userId;
	
	/**
	 * This constructor creates a group membership descriptor
	 * @param groupDesc
	 * @param membershipMatrix
	 * @param userId
	 */
	public GroupMembershipDescriptor( GroupDescriptor[] groupDesc, boolean[] membershipMatrix, int userId ){
		// 0 -- Precondition check
		if( membershipMatrix.length != groupDesc.length )
			throw new IllegalArgumentException("The length of the membership matrix is not equal to number of group descriptors");
		
		this.userId = userId;
		
		groupDescriptors = new GroupManagement.GroupDescriptor[groupDesc.length];
		isMember = new boolean[membershipMatrix.length];
		
		System.arraycopy(groupDesc, 0, groupDescriptors, 0, groupDesc.length);
		System.arraycopy(isMember, 0, membershipMatrix, 0, membershipMatrix.length);
	}
	
	/**
	 * This constructor loads the group descriptors from the group manager given.
	 * @param userId
	 * @param groupManager
	 * @throws SQLException
	 * @throws InputValidationException
	 * @throws NoDatabaseConnectionException 
	 */
	public GroupMembershipDescriptor( int userId, GroupManagement groupManager ) throws SQLException, InputValidationException, NoDatabaseConnectionException{
		
		groupDescriptors = groupManager.getGroupDescriptors();
		
		// 1 -- Create the arrays
		//if( groupDescriptors.length > 0 )
		isMember = new boolean[groupDescriptors.length];
		
		//	 1.1 -- Preset the values to false
		for( int c = 0; c < isMember.length; c++ )
			isMember[c] = false;
		
		// 2 -- Load each of the values
		for( int c = 0; c < isMember.length; c++ )
			isMember[c] = groupManager.isUserMemberOfGroup( userId, groupDescriptors[c].getGroupId() );
	}
	
	/**
	 * Return the number of entries.
	 * @return
	 */
	public int getSize(){
		return isMember.length;
	}
	
	/**
	 * Determines if the user is a member of the given group.
	 * @param groupId
	 * @return
	 */
	public boolean isMemberOfGroupByID( int groupId ){
		if( isMember == null )
			return false;
		
		for( int c = 0; c < isMember.length; c++){
			if( groupDescriptors[c].getGroupId() == groupId )
				return isMember[c];
		}
		
		// Group not found, must not be a member
		return false;
	}
	
	/**
	 * Get the user represented in this descriptor.
	 * @return
	 */
	public int getUserId(){
		return userId;
	}
	
	/**
	 * Get the group descriptors represented in this class.
	 * @return
	 */
	public GroupDescriptor[] getGroupDescriptors(){
		GroupManagement.GroupDescriptor[] groupDescriptorsCopy = new GroupManagement.GroupDescriptor[groupDescriptors.length];
		
		System.arraycopy(groupDescriptors, 0, groupDescriptorsCopy, 0, groupDescriptors.length);
		
		return groupDescriptorsCopy;
	}
	
	/**
	 * Get the specified group descriptor (or null if not found).
	 * @return
	 */
	public GroupDescriptor getGroupDescriptorByID( int groupId ){
		if( isMember == null )
			return null;
		
		for( int c = 0; c < isMember.length; c++){
			if( groupDescriptors[c].getGroupId() == groupId )
				return groupDescriptors[c];
		}
		
		return null;
	}
	
	
	/**
	 * Get the specified group descriptor at the specified index.
	 * @param c
	 * @return
	 */
	public GroupDescriptor getGroupDescriptor( int c ){
		
		if( groupDescriptors != null && c < groupDescriptors.length )
			return groupDescriptors[c];
		else
			return null;
	}
	
	/**
	 * Determine if the user is a member of the group at the specified index.
	 * @param c
	 * @return
	 */
	public boolean isMemberOfGroup( int c ){
		
		if( isMember != null && c < isMember.length )
			return isMember[c];
		else
			return false;
	}
	
	/**
	 * Determine if the group corresponding to the given identifier is in the list.
	 * @param groupId
	 * @return
	 */
	public boolean isGroupInList( int groupId ){
		if( isMember == null )
			return false;
		
		for( int c = 0; c < isMember.length; c++){
			if( groupDescriptors[c].getGroupId() == groupId )
				return true;
		}
		
		// Group not found, must not be a member
		return false;
	}
	
	/*public Hashtable toHashtable(){
		Hashtable hashtable= new Hashtable();
		hashtable.put("UserID", new Double( userId ));
		
		Vector groupDescs;
		
		for( int c = 0; c < groupDescriptors.length; c++ ){
			Hashtable groupDeschash = groupDescriptors[c].toHashtable();
		}
		
		
	}*/

}
