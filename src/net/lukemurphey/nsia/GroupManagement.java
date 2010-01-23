package net.lukemurphey.nsia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.*;
import java.util.*;

/**
 * This class manages user groups; or collections of users for the purposes of establishing access control lists. 
 * @author luke
 *
 */
public class GroupManagement {
	
	public static final String GROUP_NAME_REGEX = ".{1,64}";//"[-a-zA-Z0-9.@+!$ ]{1,64}";
	public static final int GROUP_NAME_LENGTH = 64;
	public static final String GROUP_DESCRIPTION_REGEX = ".{0,512}";//"[-a-zA-Z0-9.@+!$ ()\\n\\t]{0,512}";
	public static final int GROUP_DESCRIPTION_LENGTH = 512;
	
	public enum State{
		ACTIVE,
		INACTIVE
	}
	
	private Application appRes;
	
	public GroupManagement( Application appResources ){
		appRes = appResources;
	}
	
	/**
	 * Retrieve the group that corresponds to the given name.
	 * @precondition The group name must be valid
	 * @postcondition The identifier associated with the group name will be returned  
	 * @param groupName
	 * @return
	 * @throws InputValidationException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	public int getGroupID( String groupName ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		// 0 -- Precondition Check
		Pattern groupNamePattern = Pattern.compile(GROUP_NAME_REGEX);
		Matcher groupNameMatcher = groupNamePattern.matcher(groupName);
		
		if( !groupNameMatcher.matches() )
			throw new InputValidationException("The group name contains invalid characters", "GroupName", groupName );
		
		// 1 -- Attempt to locate the group associated with the given name
		PreparedStatement statement = null;
		ResultSet result = null;
		Connection connection = null;
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.USER_QUERY);
			
			statement = connection.prepareStatement("Select * from Groups where GroupName = ?");
			statement.setString(1, groupName);
			result = statement.executeQuery();
			
			if( result.next() )
				return result.getInt("GroupID");
			else
				return -1;
		} finally {
			if (result != null )
				result.close();
			
			if (statement != null )
				statement.close();
			
			if( connection != null )
				connection.close();
		}
		
	}
	
	/**
	 * Method adds a new user group corresponding the name given. The group will not be added if another group of the same name exists.
	 * @precondition The group name and description must be valid and the group name must not already exist.
	 * @postcondition The new group will be added and the group identifier returned, or -1 if the addition failed. 
	 * @param groupName
	 * @param groupDescription
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws InputValidationException
	 */
	public synchronized int addGroup( String groupName, String groupDescription) throws SQLException, NoDatabaseConnectionException, InputValidationException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the group name is valid
		if( groupName == null || groupName.length() == 0 )
			throw new IllegalArgumentException("Group name is invalid (null)");
		
		if( groupName.length() == 0 )
			throw new IllegalArgumentException("Group name is invalid (empty string)");
		
		Pattern groupNamePattern = Pattern.compile(GROUP_NAME_REGEX);
		Matcher matcher = groupNamePattern.matcher( groupName );
		if( !matcher.matches() )
			throw new InputValidationException("Group name contains invalid characters", "GroupName", groupName );
		
		//	 0.2 -- Make sure the description is valid
		if( groupDescription == null )
			groupDescription = "";
		Pattern groupDescriptionPattern = Pattern.compile(GROUP_DESCRIPTION_REGEX);
		Matcher groupDescriptionMatcher = groupDescriptionPattern.matcher( groupDescription );
		
		if( !groupDescriptionMatcher.matches() )
			throw new InputValidationException("Group description contains invalid characters", "GroupDescription", groupDescription );
		
		// 1 -- Make sure the group does not already exist
		if( getGroupID( groupName ) != -1 )
			return -1;
		
		// 2 -- Add the group
		Connection conn = null;
		
		PreparedStatement preparedStatement = null;
		ResultSet keys = null;
		
		try{
			conn = appRes.getDatabaseConnection( Application.DatabaseAccessType.USER_UPDATE );
			preparedStatement = conn.prepareStatement("Insert into Groups (GroupName, GroupDescription, Status) values (?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
			
			preparedStatement.setString(1, groupName );
			preparedStatement.setString(2, groupDescription );
			preparedStatement.setInt(3, GroupManagement.State.ACTIVE.ordinal() );
			
			if( preparedStatement.executeUpdate() < 1 )
				return -1;
			
			// 3 -- Return the group ID
			keys = preparedStatement.getGeneratedKeys();
			if( keys.next() )
				return keys.getInt(1);
			else
				return -1;
		} finally {
			if (keys != null )
				keys.close();
			
			if (preparedStatement != null )
				preparedStatement.close();
			
			if (conn != null )
				conn.close();
		}
	}
	
	/**
	 * Retrieve the group descriptor associated with the group identifier given.
	 * @precondition A database connection must be available, the groupId must be greater than 0, and the group must exist.
	 * @postcondition A group descriptor will be returned if the group exists; null will be returned if the group does not
	 * @param groupId
	 * @return
	 * @throws SQLException
	 * @throws InputValidationException
	 * @throws NoDatabaseConnectionException 
	 * @throws NotFoundException 
	 */
	public GroupDescriptor getGroupDescriptor( int groupId ) throws SQLException, InputValidationException, NoDatabaseConnectionException, NotFoundException{
		
		// 0 -- precondition check
		
		//	 0.1 -- Make sure the argument is valid
		if( groupId < 1 )
			throw new IllegalArgumentException("The group ID must be greater than 0");
		
		//	 0.2 -- Determine if the class is configured in such a way to perform the authentication
		// The database connection will be tested in the try block
		
		// 1 -- Retrieve the group descriptor
		PreparedStatement statement = null;
		ResultSet result = null;
		Connection connection = null;
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.USER_QUERY);
			statement = connection.prepareStatement("Select * from Groups where GroupID = ?");
			statement.setInt(1,groupId);
			result = statement.executeQuery();
			
			if(result.next()){
				String groupName = result.getString("GroupName");
				String groupDescription = result.getString("GroupDescription");
				State groupStatus = convertGroupFromInt( result.getInt("Status") );
				//int objectId = result.getLong("ObjectID");
				
				GroupDescriptor groupDesc = new GroupDescriptor( groupId, groupName, groupDescription, groupStatus );
				return groupDesc;
			}
			else
				throw new NotFoundException("No group exists with the given identifier");//group could not be found
			
		} finally {
			if (result != null )
				result.close();
			
			if (statement != null )
				statement.close();
			
			if (connection != null )
				connection.close();
		}
	}
	
	/**
	 * Convert the given integer to the appropriate enumerated value.
	 * @param value
	 * @return
	 */
	private State convertGroupFromInt( int value ){
		State[] states = State.values();
		
		for( int c = 0; c < states.length; c++){
			if( states[c].ordinal() == value )
				return states[c];
		}
		
		return State.INACTIVE;
	}
	
	/**
	 * @precondition A database connection must be available, the groupId must be greater than 0, and the group must exist.
	 * @postcondition A group descriptor will be returned if the group exists; null will be returned if the group does not
	 * @param groupName
	 * @return
	 * @throws SQLException
	 * @throws InputValidationException
	 * @throws NoDatabaseConnectionException 
	 * @throws NotFoundException 
	 */
	public GroupDescriptor getGroupDescriptor( String groupName ) throws SQLException, InputValidationException, NoDatabaseConnectionException, NotFoundException{
		
		// 0 -- Precondition check
		// The preconditions will be checked in the getGroupDescriptor(int) and getGroupId methods.
		
		// 1 -- Retrieve the user ID
		int groupId = getGroupID( groupName );
		
		// 2 -- Get the group descriptor
		return getGroupDescriptor( groupId );
	}
	
	public boolean updateGroupInfo( int groupId, String groupName,String groupDescription ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		// 0 -- Precondition check
		
		//	 0.1 -- User ID must be valid
		if( groupId < 1 )
			throw new IllegalArgumentException("Group ID is invalid (must be greater than 0)");
		
		//	 0.2 -- Make sure the real name is valid
		if( groupName == null || groupName.length() == 0 )
			throw new IllegalArgumentException("Group name is invalid (null)");
		
		if( groupName.length() == 0 )
			throw new IllegalArgumentException("Group name is invalid (empty string)");
		
		Pattern groupNamePattern = Pattern.compile(GROUP_NAME_REGEX);
		Matcher matcher = groupNamePattern.matcher( groupName );
		if( !matcher.matches() )
			throw new InputValidationException("Group name contains invalid characters", "GroupName", groupName );
		
		//	 0.3 -- Make sure the group description is valid
		Pattern groupDescriptionPattern = Pattern.compile(GROUP_DESCRIPTION_REGEX);
		Matcher groupDescriptionMatcher = groupDescriptionPattern.matcher(groupDescription);
		
		if( !groupDescriptionMatcher.matches() )
			throw new InputValidationException("The group description contains invalid characters", "GroupDescription", groupDescription );
		
		//	 0.4 -- Make sure database is available
		// The database connection will be checked in the try block
		Connection conn = null;
		
		// 1 -- Update the group information
		PreparedStatement statement = null;
		
		try{
			conn = appRes.getDatabaseConnection( Application.DatabaseAccessType.USER_UPDATE );
			if( conn == null )
				throw new NoDatabaseConnectionException();
			
			statement = conn.prepareStatement("Update Groups set GroupName = ?, GroupDescription = ? where GroupID = ?");
			statement.setString(1, groupName);
			statement.setString(2, groupDescription);
			statement.setInt(3, groupId);
			
			if( statement.executeUpdate() < 1 )
				return false;
			else
				return true;
		} finally {
			
			if (statement != null )
				statement.close();
			
			if (conn != null )
				conn.close();
		}
	}
	
	/**
	 * Delete the given group.
	 * @precondition The group identifier must be valid and must be associated with a group
	 * @postcondition The group will be disabled, false will be returned of the group could not be found
	 * @param groupId
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws InputValidationException
	 */
	public boolean deleteGroup( int groupId ) throws SQLException, NoDatabaseConnectionException{
		
		// 0 -- precondition check
		
		//	 0.1 -- Make sure the argument is valid
		if( groupId < 1 )
			throw new IllegalArgumentException("The group ID must be greater than 0");
		
		//	 0.2 -- Determine if the class is configured in such a way to connect to the authentication system
		// The database connection will be checked in the try block
		
		// 1 -- Set the account status to disabled
		Connection conn = null;
		PreparedStatement preparedStatement = null;
		
		try{
			conn = appRes.getDatabaseConnection( Application.DatabaseAccessType.USER_UPDATE );
			preparedStatement = conn.prepareStatement("Delete from Groups where GroupID = ?");
			preparedStatement.setInt(1, groupId );
			
			if( preparedStatement.executeUpdate() != 1 )
				return false;
			else
				return true;
		} finally {
			if (preparedStatement != null )
				preparedStatement.close();
			
			if (conn != null )
				conn.close();
		}
	}
	
	/**
	 * Set the given group as disabled. Users cannot be added to disabled groups and ACLs associated with the group are ignored.
	 * @precondition The group identifier must be valid and must be associated with a group
	 * @postcondition The group will be disabled, false will be returned of the group could not be found
	 * @param groupId
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws InputValidationException
	 */
	public boolean disableGroup( int groupId ) throws SQLException, NoDatabaseConnectionException{
		
		// 0 -- precondition check
		
		//	 0.1 -- Make sure the argument is valid
		if( groupId < 1 )
			throw new IllegalArgumentException("The group ID must be greater than 0");
		
		//	 0.2 -- Determine if the class is configured in such a way to connect to the authentication system
		// The database connection will be checked in the try block
		
		// 1 -- Set the account status to disabled
		Connection conn = null;
		PreparedStatement preparedStatement = null;
		
		try{
			conn = appRes.getDatabaseConnection( Application.DatabaseAccessType.USER_UPDATE );
			
			preparedStatement = conn.prepareStatement("Update Groups set Status = ? where GroupID = ?");
			preparedStatement.setInt(1, GroupManagement.State.INACTIVE.ordinal() );
			preparedStatement.setInt(2, groupId );
			
			if( preparedStatement.executeUpdate() != 1 )
				return false;
			else
				return true;
		} finally {
			if (preparedStatement != null )
				preparedStatement.close();
			
			if (conn != null )
				conn.close();
		}
	}
	
	/**
	 * Set the given group as enabled.
	 * @precondition The group identifier must be valid and must be associated with a group
	 * @postcondition The group will enabled, false will be returned of the group could not be found
	 * @param groupId
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws InputValidationException
	 */
	public boolean enableGroup( int groupId ) throws SQLException, NoDatabaseConnectionException{
		
		// 0 -- precondition check
		
		//	 0.1 -- Make sure the argument is valid
		if( groupId < 1 )
			throw new IllegalArgumentException("The group ID must be greater than 0");
		
		//	 0.2 -- Determine if the class is configured in such a way to connect to the authentication system
		// The database connection will be checked in the try block
		
		// 1 -- Set the account status to disabled
		Connection conn = null;
		PreparedStatement preparedStatement = null;
		
		try{
			conn = appRes.getDatabaseConnection( Application.DatabaseAccessType.USER_UPDATE );
			preparedStatement = conn.prepareStatement("Update Groups set Status = ? where GroupID = ?");
			preparedStatement.setInt(1, GroupManagement.State.ACTIVE.ordinal() );
			preparedStatement.setInt(2, groupId );
			
			if( preparedStatement.executeUpdate() != 1 )
				return false;
			else
				return true;
		} finally {
			if (preparedStatement != null )
				preparedStatement.close();
			
			if (conn != null )
				conn.close();
		}
	}
	
	/**
	 * Retrieve all of the group descriptors for all groups that exist.
	 * @return
	 * @throws SQLException
	 * @throws InputValidationException
	 * @throws NoDatabaseConnectionException 
	 */
	public GroupDescriptor[] getGroupDescriptors() throws SQLException, InputValidationException, NoDatabaseConnectionException{
		
		PreparedStatement statement = null;
		ResultSet result = null;
		Connection connection = null;
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.USER_QUERY);
			statement = connection.prepareStatement("Select * from Groups");
			result = statement.executeQuery();
			
			Vector<GroupDescriptor> groups = new Vector<GroupDescriptor>();
			
			while(result.next()){
				String groupName = result.getString("GroupName");
				int groupId = result.getInt("GroupID");
				String groupDescription = result.getString("GroupDescription");
				State groupStatus = convertGroupFromInt( result.getInt("Status") );
				
				GroupDescriptor groupDesc = new GroupDescriptor( groupId, groupName, groupDescription, groupStatus );
				groups.add(groupDesc);
			}
			
			if( groups.size() == 0)
				return new GroupDescriptor[0];//no groups could be found
			
			// 2 -- Convert to array
			GroupDescriptor[] descriptors = new GroupDescriptor[groups.size()];
			
			for( int d = 0; d < groups.size(); d++){
				descriptors[d] = (GroupDescriptor)groups.get(d);
			}
			
			return descriptors;
			
		} finally {
			if (result != null )
				result.close();
			
			if (statement != null )
				statement.close();
			
			if (connection != null )
				connection.close();
		}
	}
	
	/**
	 * Determines if the given user ID is associated with (that is, a member of) the given group.
	 * @param userId
	 * @param groupId
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	public boolean isUserMemberOfGroup( int userId, int groupId ) throws SQLException, NoDatabaseConnectionException{
		// 0 -- Precondition check
		
		//	 0.1 -- Database connection must be present
		Connection connection = null;
		
		// 1 -- Detemine if the user is associated with the group
		PreparedStatement associationQuery = null;
		ResultSet results = null;
		try{
			connection = appRes.getDatabaseConnection( Application.DatabaseAccessType.PERMISSIONS );
		associationQuery = connection.prepareStatement("Select * from GroupUsersMap where GroupID = ? and UserID =?");
		associationQuery.setInt(1, groupId);
		associationQuery.setInt(2, userId);
		
		results = associationQuery.executeQuery();
		
		if( results.next() )
			return true;
		else
			return false;
		}
		finally{
			if(associationQuery != null)
				associationQuery.close();
			
			if( results != null )
				results.close();
			
			if (connection != null )
				connection.close();
		}
	}
	
	/**
	 * Get a descriptor that indicates which groups a user is a member of.
	 * @param userId
	 * @return
	 * @throws SQLException
	 * @throws InputValidationException
	 * @throws NoDatabaseConnectionException 
	 */
	public GroupMembershipDescriptor getGroupMembership( int userId ) throws SQLException, InputValidationException, NoDatabaseConnectionException{
		GroupMembershipDescriptor groupMembershipDescriptor = new GroupMembershipDescriptor( userId, this );
		return groupMembershipDescriptor;
	}
	
	/**
	 * Method adds the given user to group specified. Note that this method will allow users to be added
	 * to groups regardless of whether or not the group or the user is disabled.
	 * @precondition The user ID and group ID must be valid (i.e. associated with a group)
	 * @postcondition The user will be associated with the group
	 * @param userId
	 * @param groupId
	 * @return
	 * @throws SQLException
	 * @throws ApplicationException
	 * @throws NoDatabaseConnectionException 
	 */
	public synchronized boolean addUserToGroup( int userId, int groupId ) throws SQLException, NotFoundException, NoDatabaseConnectionException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Database connection must be present
		Connection connection = appRes.getDatabaseConnection( Application.DatabaseAccessType.PERMISSIONS );
		
		//	 0.2 -- User ID must be a valid user
		
		PreparedStatement testUser = null;
		PreparedStatement testGroup = null;
		ResultSet userResult = null;
		ResultSet groupResult = null;
		PreparedStatement associationQuery = null;
		ResultSet results = null;
		PreparedStatement associationUpdate = null;
		
		try{
			testUser = connection.prepareStatement("Select * from Users where UserID = ?");
			testUser.setInt(1, userId );
			userResult = testUser.executeQuery();
			if( !userResult.next() ){
				throw new NotFoundException ("No user exists with the ID \"" + userId + "\"" );
			}
			
			//	 0.3 -- Group ID must be a valid group
			testGroup = connection.prepareStatement("Select * from Groups where GroupID =?");
			testGroup.setInt(1, groupId );
			groupResult = testGroup.executeQuery();
			if( !groupResult.next() ){
				throw new NotFoundException ("No group exists with the ID \"" + groupId + "\"" );
			}
			
			
			// 1 -- Detemine if the user is already associated with the group
			associationQuery = connection.prepareStatement("Select * from GroupUsersMap where GroupID = ? and UserID =?");
			associationQuery.setInt(1, groupId);
			associationQuery.setInt(2, userId);
			
			results = associationQuery.executeQuery();
			
			if( results.next() )
				return true;
			
			
			// 2 -- Add the user to the group if necessary (only reachable if the user is not associated with the group)
			associationUpdate = connection.prepareStatement("Insert into GroupUsersMap (GroupID, UserID) values (?, ?)");
			associationUpdate.setInt(1, groupId);
			associationUpdate.setInt(2, userId);
			
			if( associationUpdate.executeUpdate() < 1 )
				return false;
			else
				return true;
		} finally {
			
			if (testUser != null )
				testUser.close();
			
			if (testGroup != null )
				testGroup.close();
			
			if (userResult != null )
				userResult.close();
			
			if (userResult != null )
				userResult.close();
			
			if (associationQuery != null )
				associationQuery.close();
			
			if (results != null )
				results.close();
			
			if (associationUpdate != null )
				associationUpdate.close();
			
			if (connection != null )
				connection.close();
		}
		
	}
	
	/**
	 * Method removes the given user from the group specified. Note that this method will allow users to be removed
	 * from the groups regardless of whether or not the group or the user is disabled.
	 * @precondition The user ID and group ID must be valid (i.e. associated with a group)
	 * @postcondition The user will be associated with the group
	 * @param userId
	 * @param groupId
	 * @return
	 * @throws SQLException
	 * @throws NotFoundException
	 * @throws NoDatabaseConnectionException 
	 */
	public synchronized boolean removeUserFromGroup( int userId, int groupId ) throws SQLException, NotFoundException, NoDatabaseConnectionException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Database connection must be present
		Connection connection = appRes.getDatabaseConnection( Application.DatabaseAccessType.PERMISSIONS );
		
		//	 0.2 -- User ID must be a valid user
		PreparedStatement testUser = null;
		ResultSet userResult = null;
		PreparedStatement testGroup = null;
		ResultSet groupResult = null;
		PreparedStatement associationUpdate = null;
		
		try{
			testUser = connection.prepareStatement("Select * from Users where UserID =?");
			testUser.setInt(1, userId );
			userResult = testUser.executeQuery();
			if( !userResult.next() )
				throw new NotFoundException ("No user exists with the ID \"" + userId + "\"" );
			
			//	 0.3 -- Group ID must be a valid group
			testGroup = connection.prepareStatement("Select * from Groups where GroupID =?");
			testGroup.setInt(1, groupId );
			groupResult = testGroup.executeQuery();
			if( !groupResult.next() )
				throw new NotFoundException ("No group exists with the ID \"" + groupId + "\"" );
			
			
			// 1 -- Remove the user from the group
			associationUpdate = connection.prepareStatement("Delete from GroupUsersMap where GroupID = ? and UserID = ?");
			associationUpdate.setInt(1, groupId);
			associationUpdate.setInt(2, userId);
			associationUpdate.executeUpdate();
			
		} finally {
			
			if (testUser != null )
				testUser.close();
			
			if (userResult != null )
				userResult.close();
			
			if (testGroup != null )
				testGroup.close();
			
			if (groupResult != null )
				groupResult.close();
			
			if (associationUpdate != null )
				associationUpdate.close();
			
			if (connection != null )
				connection.close();
		}
		
		return true;
		
	}
	
	/**
	 * Determines if a group with the given identifier exists and is active.
	 * @param groupId
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	public boolean isGroupActive( int groupId ) throws SQLException, NoDatabaseConnectionException{
		
		PreparedStatement statement = null;
		ResultSet result = null;
		Connection connection = null;
		
		try{
			connection = appRes.getDatabaseConnection( Application.DatabaseAccessType.PERMISSIONS );
			
			statement = connection.prepareStatement("Select * from Groups where GroupID =? and Status =?");
			statement.setInt(1, groupId );
			statement.setInt(2, GroupManagement.State.ACTIVE.ordinal() );
			result = statement.executeQuery();
			
			return result.next();
			
		}finally {
			if (result != null )
				result.close();
			
			if (statement != null )
				statement.close();
			
			if (connection != null )
				connection.close();
		}
	}
	
	/**
	 * The group descriptor class describes a user group.
	 * @author luke
	 *
	 */
	public static class GroupDescriptor{
		
		private int groupId = -1;
		private String groupDescription;
		private String groupName;
		private State groupState = State.INACTIVE;
		//private long objectId = -1;
		
		public GroupDescriptor(int groupId, String groupName, String groupDescription, State groupStatus ) throws InputValidationException{
			
			// 0 -- Precondition check
			
			//	 0.1 -- Make sure the group name is valid
			if( groupName == null )
				throw new InputValidationException( "The group name cannot be null", "GroupName", "null" );
			
			if( groupName.length() == 0 )
				throw new InputValidationException( "The group name cannot be empty", "GroupName", groupName );
			
			if( groupName.length() >  GROUP_NAME_LENGTH)
				throw new InputValidationException( "The group name length is excessive", "GroupName", groupName );
			
			Pattern groupNamePattern = Pattern.compile(GROUP_NAME_REGEX);
			Matcher groupNameMatcher = groupNamePattern.matcher(groupName);
			
			if( !groupNameMatcher.matches() )
				throw new InputValidationException("The group name contains invalid characters", "GroupName", groupName );
			
			//	 0.2 -- Make sure the description is valid
			if( groupDescription == null )
				groupDescription = "";
			
			if( groupDescription.length() >  GROUP_DESCRIPTION_LENGTH)
				throw new InputValidationException( "The group description length is excessive", "GroupDescription", groupDescription );
			
			Pattern groupDescriptionPattern = Pattern.compile(GROUP_DESCRIPTION_REGEX);
			Matcher groupDescriptionMatcher = groupDescriptionPattern.matcher(groupDescription);
			
			if( !groupDescriptionMatcher.matches() )
				throw new InputValidationException("The group description contains invalid characters", "GroupDescription", groupDescription );
			
			//	 0.3 -- Make sure the status is valid
			if( groupStatus != GroupManagement.State.ACTIVE )
				groupStatus = GroupManagement.State.INACTIVE;
			
			
			// 1 -- Set the values
			this.groupId = groupId;
			this.groupName = groupName;
			this.groupDescription = groupDescription;
			this.groupState = groupStatus;
			//this.objectId = objectId;
		}
		
		/**
		 * Get the description of the group (or null if not set).
		 * @precondition None
		 * @postcondition The description will be returned or null if a description was not set.
		 * @return
		 */
		public String getDescription(){
			return groupDescription;
		}
		
		/**
		 * Gets the object ID (used for access control entries)
		 * @return
		 */
		/*public long getObjectId(){
			return objectId;
		}*/
		
		/**
		 * Get the identifier of the group.
		 * @precondition None
		 * @postcondition The identifier fir the group will be returned or -1 if not set.
		 * @return
		 */
		public int getGroupId(){
			return groupId;
		}
		
		/**
		 * Get the name of the group (or null if not set).
		 * @precondition None
		 * @postcondition The name will be returned or null if a description was not set.
		 * @return
		 */
		public String getGroupName(){
			return groupName;
		}
		
		/**
		 * Retrieve the group state (active or inactive)
		 * @return
		 */
		public State getGroupState(){
			return groupState;
		}
	}
	
}
