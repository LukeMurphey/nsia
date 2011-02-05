package net.lukemurphey.nsia;

import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;


public class SiteGroupManagement {
	public static final String SITE_GROUP_NAME_REGEX = "[-a-zA-Z0-9.@!$ 90()]{1,64}";
	public static final int SITE_GROUP_NAME_LENGTH = 64;
	public static final String SITE_GROUP_DESCRIPTION_REGEX = "[-a-zA-Z0-9@!$ ()\\n\\t]{0,512}";
	public static final int SITE_GROUP_DESCRIPTION_LENGTH = 512;
	
	public enum State{
		ACTIVE,
		INACTIVE
	}
	
	
	private Application application;
	
	public SiteGroupManagement( Application app ){
		application = app;
	}
	
	/**
	 * Create a new site group according to the parameters given. This method assumes that the Site Group should be created in
	 * an enabled (active) state.
	 * @param siteGroupName
	 * @param siteGroupDescription
	 * @return
	 * @throws SQLException
	 * @throws InputValidationException
	 * @throws NoDatabaseConnectionException 
	 */
	public int addGroup( String siteGroupName, String siteGroupDescription ) throws SQLException, InputValidationException, NoDatabaseConnectionException{
		return addGroup( siteGroupName, siteGroupDescription, State.ACTIVE );
	}
	
	/**
	 * Create a new site group according to the parameters given.
	 * @param siteGroupName
	 * @param siteGroupDescription
	 * @param siteGroupStateIndicator
	 * @return
	 * @throws SQLException
	 * @throws InputValidationException
	 * @throws NoDatabaseConnectionException 
	 */
	public synchronized int addGroup( String siteGroupName, String siteGroupDescription, State siteGroupStateIndicator ) throws SQLException, InputValidationException, NoDatabaseConnectionException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the group name is valid
		if( siteGroupName == null || siteGroupName.length() == 0 )
			throw new IllegalArgumentException("Site group name is invalid (null)");
		
		if( siteGroupName.length() == 0 )
			throw new IllegalArgumentException("Site group name is invalid (empty string)");
		
		Pattern groupNamePattern = Pattern.compile(SITE_GROUP_NAME_REGEX);
		Matcher matcher = groupNamePattern.matcher( siteGroupName );
		
		if( !matcher.matches() )
			throw new InputValidationException("Site group name contains invalid characters", "GroupName", siteGroupName );
		
		//	 0.2 -- Make sure the description is valid
		// The site group description will be saved as an empty string when it is saved to the database if it is null 
		
		/*
		Pattern groupDescriptionPattern = Pattern.compile(SITE_GROUP_DESCRIPTION_REGEX);
		Matcher groupDescriptionMatcher = groupDescriptionPattern.matcher( siteGroupDescription );
		
		if( !groupDescriptionMatcher.matches() )
			throw new InputValidationException("Site group description contains invalid characters", "SiteGroupDescription", siteGroupDescription );
		*/
		
		// 1 -- Make sure the group does not already exist
		if( getGroupID( siteGroupName ) != -1 ){
			return -1;
		}
		
		// 2 -- Add the group
		
		//	 2.1 -- Get the object identifier
		long objectId = AccessControl.allocateObjectId("SiteGroups", application );
		
		//	 2.2 -- Create the group
		Connection conn = application.getDatabaseConnection( Application.DatabaseAccessType.ADMIN );
		PreparedStatement preparedStatement = null;
		ResultSet keys = null;
		
		try{
			preparedStatement = conn.prepareStatement("Insert into SiteGroups (Name, Description, Status, ObjectID) values (?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
			
			preparedStatement.setString(1, siteGroupName );
			
			if( siteGroupDescription == null ){
				preparedStatement.setString(2, "" );
			}
			else{
				preparedStatement.setString(2, siteGroupDescription );
			}
			
			preparedStatement.setInt(3, siteGroupStateIndicator.ordinal() );
			preparedStatement.setLong(4, objectId );
			
			if( preparedStatement.executeUpdate() < 1 ){
				return -1;
			}
			
			// 3 -- Return the group ID
			keys = preparedStatement.getGeneratedKeys();
			
			if( keys.next() ){
				return keys.getInt(1);
			}
			else{
				return -1;
			}
			
		} finally {
			if (preparedStatement != null )
				preparedStatement.close();
			
			if (keys != null )
				keys.close();
			
			if (conn != null )
				conn.close();
		}
		
	}
	
	/**
	 * Get the site group ID associated the site group name.
	 * @param siteGroupName
	 * @return
	 * @throws InputValidationException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	public int getGroupID( String siteGroupName ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		// 0 -- Precondition Check
		Pattern groupNamePattern = Pattern.compile(SITE_GROUP_NAME_REGEX);
		Matcher groupNameMatcher = groupNamePattern.matcher(siteGroupName);
		
		if( !groupNameMatcher.matches() )
			throw new InputValidationException("The site group name contains invalid characters", "SiteGroupName", siteGroupName );
		
		// 1 -- Attempt to locate the group associated with the given name
		PreparedStatement statement = null;
		ResultSet result = null;
		Connection connection = application.getDatabaseConnection(Application.DatabaseAccessType.ADMIN);
		
		try{
			statement = connection.prepareStatement("Select * from SiteGroups where Name = ?");
			statement.setString(1, siteGroupName);
			result = statement.executeQuery();
			
			if( result.next() ){
				return result.getInt("SiteGroupID");
			}
			else{
				return -1;
			}
			
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
	 * Delete the site group as well as the associated rules.
	 * @param siteGroupId
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws InputValidationException
	 */
	public boolean deleteGroup( int siteGroupId ) throws SQLException, NoDatabaseConnectionException, InputValidationException{
		
		// 0 -- precondition check
		
		//	 0.1 -- Make sure the argument is valid
		if( siteGroupId < 1 )
			throw new IllegalArgumentException("The site group ID must be greater than 0");
		
		// 1 -- Delete the group
		Connection conn = application.getDatabaseConnection( Application.DatabaseAccessType.USER_UPDATE );
		PreparedStatement preparedStatement = null;
		
		try{
			preparedStatement = conn.prepareStatement("Delete from SiteGroups where SiteGroupID = ?");
			preparedStatement.setInt(1, siteGroupId );
			
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
	 * Disable the group. Doing so will prevent scans of site group rules.
	 * @param siteGroupId
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws InputValidationException
	 */
	public boolean disableGroup( int siteGroupId ) throws SQLException, NoDatabaseConnectionException, InputValidationException{
		
		// 0 -- precondition check
		
		//	 0.1 -- Make sure the argument is valid
		if( siteGroupId < 1 )
			throw new IllegalArgumentException("The site group ID must be greater than 0");
		
		
		// 1 -- Set the account status to disabled
		Connection conn = application.getDatabaseConnection( Application.DatabaseAccessType.USER_UPDATE );
		PreparedStatement preparedStatement = null;
		
		try{
			preparedStatement = conn.prepareStatement("Update SiteGroups set Status = ? where SiteGroupID = ?");
			preparedStatement.setLong(1, SiteGroupManagement.State.INACTIVE.ordinal() );
			preparedStatement.setLong(2, siteGroupId );
			
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
	 * Enable the group. Doing so will cause the rules to be evaluated as part of the normal scanning process.
	 * @param groupId
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws InputValidationException
	 */
	public boolean enableGroup( int groupId ) throws SQLException, NoDatabaseConnectionException, InputValidationException{
		
		// 0 -- precondition check
		
		//	 0.1 -- Make sure the argument is valid
		if( groupId < 1 )
			throw new IllegalArgumentException("The site group ID must be greater than 0");
		
		// 1 -- Set the account status to disabled
		Connection conn = application.getDatabaseConnection( Application.DatabaseAccessType.ADMIN );
		PreparedStatement preparedStatement = null;
		
		try{
			preparedStatement = conn.prepareStatement("Update SiteGroups set Status = ? where SiteGroupID = ?");
			preparedStatement.setLong(1, SiteGroupManagement.State.ACTIVE.ordinal() );
			preparedStatement.setLong(2, groupId );
			
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
	 * Get the site group descriptor associated with the identifier.
	 * @param groupId
	 * @return
	 * @throws SQLException
	 * @throws InputValidationException
	 * @throws NoDatabaseConnectionException 
	 * @throws NotFoundException 
	 */
	public SiteGroupDescriptor getGroupDescriptor( int groupId ) throws SQLException, InputValidationException, NoDatabaseConnectionException, NotFoundException{
		
		// 0 -- precondition check
		
		//	 0.1 -- Make sure the argument is valid
		if( groupId < 1 ){
			throw new IllegalArgumentException("The site group ID must be greater than 0");
		}
		
		
		// 1 -- Retrieve the group descriptor
		PreparedStatement statement = null;
		ResultSet result = null;
		Connection connection = application.getDatabaseConnection(Application.DatabaseAccessType.ADMIN);
		try{
			statement = connection.prepareStatement("Select * from SiteGroups where SiteGroupID = ?");
			statement.setLong(1,groupId);
			result = statement.executeQuery();
			
			if(result.next()){
				String groupName = result.getString("Name");
				String groupDescription = result.getString("Description");
				State groupStatus = convertStateFromInt( result.getInt("Status") );
				long objectId = result.getLong("ObjectID");
				
				return new SiteGroupDescriptor( groupId, groupName, groupDescription, groupStatus, objectId );
			}
			else
				throw new NotFoundException("A site group with the given identifier could not be found");//group could not be found
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
	 * Retrieve the state associated with the integer.
	 * @param value
	 * @return
	 */
	private static State convertStateFromInt( int value ){
		State[] states = State.values();
		
		for( int c = 0; c < states.length; c++){
			if( states[c].ordinal() == value )
				return states[c];
		}
		
		return State.INACTIVE;
	}
	
	/**
	 * Get the site group associated with the given name.
	 * @param groupName
	 * @return
	 * @throws SQLException
	 * @throws InputValidationException
	 * @throws NoDatabaseConnectionException 
	 * @throws NotFoundException 
	 */
	public SiteGroupDescriptor getGroupDescriptor( String groupName ) throws SQLException, InputValidationException, NoDatabaseConnectionException, NotFoundException{
		
		// 0 -- Precondition check
		// The preconditions will be checked in the getGroupDescriptor(long) and getGroupId methods.
		
		// 1 -- Retrieve the user ID
		int groupId = getGroupID( groupName );
		
		// 2 -- Get the group descriptor
		return getGroupDescriptor( groupId );
	}
	
	/**
	 * Get all of the site group descriptors.
	 * @return
	 * @throws SQLException
	 * @throws InputValidationException
	 * @throws NoDatabaseConnectionException 
	 */
	public SiteGroupDescriptor[] getGroupDescriptors() throws SQLException, InputValidationException, NoDatabaseConnectionException{
		
		PreparedStatement statement = null;
		ResultSet result = null;
		Connection connection = application.getDatabaseConnection(Application.DatabaseAccessType.USER_QUERY);
		try{
			statement = connection.prepareStatement("Select * from SiteGroups");
			result = statement.executeQuery();
			
			Vector<SiteGroupDescriptor> groups = new Vector<SiteGroupDescriptor>();
			
			while(result.next()){
				String groupName = result.getString("Name");
				int groupId = result.getInt("SiteGroupID");
				String groupDescription = result.getString("Description");
				State groupStatus = convertStateFromInt( result.getInt("Status") );
				long objectId = result.getLong("ObjectID");
				
				SiteGroupDescriptor groupDesc = new SiteGroupDescriptor( groupId, groupName, groupDescription, groupStatus, objectId );
				groups.add(groupDesc);
			}
			
			if( groups.isEmpty() )
				return null;//no groups could be found
			
			// 2 -- Convert to array
			SiteGroupDescriptor[] descriptors = new SiteGroupDescriptor[groups.size()];
			
			for( int d = 0; d < groups.size(); d++){
				descriptors[d] = groups.get(d);
			}
			
			return descriptors;
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
	 * Update the site group information per the given parameters.
	 * @param groupId
	 * @param groupName
	 * @param groupDescription
	 * @return
	 * @throws InputValidationException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public boolean updateGroupInfo( int groupId, String groupName,String groupDescription ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		// 0 -- Precondition check
		
		//	 0.1 -- User ID must be valid
		if( groupId < 1 )
			throw new IllegalArgumentException("Site group ID is invalid (must be greater than 0)");
		
		//	 0.2 -- Make sure the real name is valid
		if( groupName == null || groupName.length() == 0 )
			throw new IllegalArgumentException("Site group name is invalid (null)");
		
		if( groupName.length() == 0 )
			throw new IllegalArgumentException("Site group name is invalid (empty string)");
		
		Pattern groupNamePattern = Pattern.compile(SITE_GROUP_NAME_REGEX);
		Matcher matcher = groupNamePattern.matcher( groupName );
		
		if( !matcher.matches() )
			throw new InputValidationException("Site group name contains invalid characters", "SiteGroupName", groupName );
		
		//	 0.4 -- Make sure database is available
		Connection conn = application.getDatabaseConnection( Application.DatabaseAccessType.ADMIN );
		if( conn == null )
			throw new NoDatabaseConnectionException();
		
		
		// 1 -- Update the group information
		PreparedStatement statement = null;
		try{
			statement = conn.prepareStatement("Update SiteGroups set Name = ?, Description = ? where SiteGroupID = ?");
			statement.setString(1, groupName);
			statement.setString(2, groupDescription);
			statement.setLong(3, groupId);
			
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
	 * The group descriptor class describes a site group.
	 * @author luke
	 *
	 */
	public static class SiteGroupDescriptor{
		
		private int groupId = -1;
		private String groupDescription;
		private String groupName;
		private State groupState = State.INACTIVE;
		private long objectId;
		
		public SiteGroupDescriptor(int groupId, String groupName, String groupDescription, State groupStatus, long objectId ) throws InputValidationException{
			
			// 0 -- Precondition check
			
			//	 0.1 -- Make sure the group name is valid
			if( groupName == null )
				throw new InputValidationException( "The site group name cannot be null", "SiteGroupName", "null" );
			
			if( groupName.length() == 0 )
				throw new InputValidationException( "The site group name cannot be empty", "SiteGroupName", groupName );
			
			if( groupName.length() >  SITE_GROUP_NAME_LENGTH)
				throw new InputValidationException( "The site group name length is excessive", "SiteGroupName", groupName );
			
			Pattern groupNamePattern = Pattern.compile(SITE_GROUP_NAME_REGEX);
			Matcher groupNameMatcher = groupNamePattern.matcher(groupName);
			
			if( !groupNameMatcher.matches() )
				throw new InputValidationException("The site group name contains invalid characters", "SiteGroupName", groupName );
			
			//	 0.2 -- Make sure the description is valid
			/*if( groupDescription == null )
				groupDescription = "";
			else if( groupDescription.length() >  SITE_GROUP_DESCRIPTION_LENGTH)
				throw new InputValidationException( "The site group description length is excessive", "SiteGroupDescription", groupDescription );
			
			Pattern groupDescriptionPattern = Pattern.compile(SITE_GROUP_DESCRIPTION_REGEX);
			Matcher groupDescriptionMatcher = groupDescriptionPattern.matcher(groupDescription);
			
			if( !groupDescriptionMatcher.matches() )
				throw new InputValidationException("The site group description contains invalid characters", "SiteGroupDescription", groupDescription );
			*/
			if( groupDescription == null )
				groupDescription = "";
			
			//	 0.3 -- Make sure the status is valid
			if( groupStatus != SiteGroupManagement.State.ACTIVE )
				groupStatus = SiteGroupManagement.State.INACTIVE;
			
			// 1 -- Set the values
			this.groupId = groupId;
			this.groupName = groupName;
			this.groupDescription = groupDescription;
			this.groupState = groupStatus;
			this.objectId = objectId;
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
		 * Get the object ID (user for access control list).
		 * @return
		 */
		public long getObjectId(){
			return objectId;
		}
		
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
		
		/**
		 * Indicates if the site group is enabled.
		 * @return
		 */
		public boolean isEnabled(){
			if( getGroupState() == State.INACTIVE ){
				return false;
			}
			else{
				return true;
			}
		}
		
		/**
		 * Create a hashtable version of the class
		 * @return
		 */
		public Hashtable<String, Object> toHashtable(){
			Hashtable<String, Object> hash = new Hashtable<String, Object>();
			
			hash.put("SiteGroupID", Integer.valueOf(groupId));
			hash.put("SiteGroupDescription", groupDescription);
			hash.put("SiteGroupName", groupName);
			hash.put("SiteGroupStatus", Integer.valueOf(groupState.ordinal()));
			
			return hash;
		}
	}
}
