package net.lukemurphey.nsia;

import java.sql.*;

import java.util.*;

import net.lukemurphey.nsia.eventlog.EventLogMessage;

/**
 * The access control class provides a method to determine if a user or group is allowed access to a 
 * given object and to update the control lists. The access control lists support both object
 * permissions and rights. Permissions are applied for objects (such as a site group) where rights
 * are global operations (such as the ability to shutdown the manager).
 * <br>
 * The permissions for users are resolved by finding the most restrictive permissions from the rules
 * that apply directly to user as well as the users' group. For example, a group that contains the 
 * user that has a deny read permission will override the permit read in another group that the user
 * is also a member of.
 *   
 * @author luke
 *
 */
public class AccessControl {
	private final Application appRes;
	private final Object mutexAclExistence = new Object();
	
	public AccessControl( Application applicationResources ){
		// 0 -- Precondition check
		if( applicationResources == null ){
			throw new IllegalArgumentException("The application resource object cannot be null");
		}
		
		// 1 -- Store the object
		appRes = applicationResources;
	}
	
	/**
	 * Retrieve the given user's permissions for the object. Null will be returned if the user is not found in the tables.
	 * @precondition The user ID must exist in the permission tables (or null will be returned) and a database connection must exist
	 * @postcondition An access control descriptor will be returned indicating the permissions granted to the user 
	 * @param userId
	 * @param objectId
	 * @param resolveUserGroupPermissions Include the permissions from the user's groups (as opposed to just the entries related to the specific user)
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public ObjectPermissionDescriptor getUserPermissions(int userId, long objectId, boolean resolveUserGroupPermissions ) throws SQLException, NoDatabaseConnectionException{
		
		// 0 -- Precondition check
		// Preconditions will be checked by the subroutines
		ObjectPermissionDescriptor userPermissions = getPermissionsByUser( userId, objectId );
		ObjectPermissionDescriptor groupPermissions = null; // NOPMD by luke on 5/26/07 10:00 AM
		
		if( resolveUserGroupPermissions ){
			groupPermissions = getPermissionsByGroups( userId, objectId );
		}
		
		if( groupPermissions == null && userPermissions == null )
			return null;
		else if( groupPermissions == null )
			return userPermissions;
		else if (userPermissions == null )
			return groupPermissions;
		
		return userPermissions.resolvePermissions(groupPermissions);		
	}
	
	/**
	 * Get the permissions that the given user has for the object associated with the object ID. This method resolves the overall
	 * access control granted to the user based on the ACLs specified for the groups that the user is a member of.
	 * @precondition A database connection must be available and the user ID must exist in the database (or null will be returned)
	 * @postcondition An access control descriptor will be returned indicating the users permissions to the given object, or null if no user was found
	 * @param userId
	 * @param objectId
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	private ObjectPermissionDescriptor getPermissionsByGroups(int userId, long objectId ) throws SQLException, NoDatabaseConnectionException{
		// 0 -- Precondition check
		// The database connection will be checked in the try block
		Connection connection = null;
		
		// 1 -- Determine the level of permissions
		AccessControlDescriptor.Action read = AccessControlDescriptor.Action.UNSPECIFIED;
		AccessControlDescriptor.Action modify = AccessControlDescriptor.Action.UNSPECIFIED;
		AccessControlDescriptor.Action create = AccessControlDescriptor.Action.UNSPECIFIED;
		AccessControlDescriptor.Action delete = AccessControlDescriptor.Action.UNSPECIFIED;
		AccessControlDescriptor.Action execute = AccessControlDescriptor.Action.UNSPECIFIED;
		AccessControlDescriptor.Action control = AccessControlDescriptor.Action.UNSPECIFIED;
		
		PreparedStatement groupsStatement = null;
		ResultSet groupsResult = null;
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.PERMISSIONS);
			
			if( connection == null )
				throw new NoDatabaseConnectionException();
			
			groupsStatement = connection.prepareStatement("Select * from GroupUsersMap where userId = ?");
			groupsStatement.setInt(1, userId);
			groupsResult = groupsStatement.executeQuery();
			
			int c = 0;
			while( groupsResult.next() ){
				int groupId = groupsResult.getInt("groupId");
				
				c++;
				statement = connection.prepareStatement("Select * from Permissions where objectId = ? and groupId = ?");
				statement.setLong(1, objectId);
				statement.setInt(2, groupId);
				result = statement.executeQuery();
				
				if( result.next() ){
					GroupManagement groupManagement = new GroupManagement(appRes); // NOPMD by luke on 5/26/07 10:04 AM
					
					if( groupManagement.isGroupActive(groupId) ){
						
						AccessControlDescriptor.Action groupRead = convertActionFromInt( result.getInt("Read") );
						AccessControlDescriptor.Action groupModify = convertActionFromInt( result.getInt("Modify") );
						AccessControlDescriptor.Action groupCreate = convertActionFromInt( result.getInt("Create") );
						AccessControlDescriptor.Action groupDelete = convertActionFromInt( result.getInt("Delete") );
						AccessControlDescriptor.Action groupExecute = convertActionFromInt( result.getInt("Execute") );
						AccessControlDescriptor.Action groupControl = convertActionFromInt( result.getInt("Control") );
						
						read = resolvePermission( read, groupRead);
						modify = resolvePermission( modify, groupModify);
						create = resolvePermission( create, groupCreate);
						delete = resolvePermission( delete, groupDelete);
						execute = resolvePermission( execute, groupExecute);
						control = resolvePermission( control, groupControl );
					}
				}
			}
			
			if( c > 0 )
				return new ObjectPermissionDescriptor(read, modify, create, execute, delete, control, AccessControlDescriptor.Subject.USER, userId, objectId);
			else
				return null; //User was not found
			
		}
		finally{
			if( groupsStatement != null )
				groupsStatement.close();
			
			if( groupsResult != null )
				groupsResult.close();
			
			if( result != null )
				result.close();
			
			if( statement != null )
				statement.close();
			
			if( connection != null )
				connection.close();
		}
	}
	
	private static AccessControlDescriptor.Action convertActionFromInt( int value ) {
		
		AccessControlDescriptor.Action values[] = AccessControlDescriptor.Action.values();
		
		for(int c = 0; c < values.length; c++){
			if( values[c].ordinal() == value )
				return values[c];	
		}

		return AccessControlDescriptor.Action.UNSPECIFIED;
		}
	
	/**
	 * Get the permissions the group has to the given object. 
	 * @param groupId
	 * @param objectId
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public ObjectPermissionDescriptor getGroupPermissions( int groupId, long objectId ) throws SQLException, NoDatabaseConnectionException{
		
		// 0 -- Precondition check
		// The database connection will be checked in the try block
		Connection connection = null;
		
		// 1 -- Determine the level of permissions
		AccessControlDescriptor.Action read = AccessControlDescriptor.Action.UNSPECIFIED;
		AccessControlDescriptor.Action modify = AccessControlDescriptor.Action.UNSPECIFIED;
		AccessControlDescriptor.Action create = AccessControlDescriptor.Action.UNSPECIFIED;
		AccessControlDescriptor.Action delete = AccessControlDescriptor.Action.UNSPECIFIED;
		AccessControlDescriptor.Action execute = AccessControlDescriptor.Action.UNSPECIFIED;
		AccessControlDescriptor.Action control = AccessControlDescriptor.Action.UNSPECIFIED;
		
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.PERMISSIONS);
			
			if( connection == null )
				throw new NoDatabaseConnectionException();
			
			statement = connection.prepareStatement("Select * from Permissions where objectId = ? and groupId = ?");
			statement.setLong(1, objectId);
			statement.setLong(2, groupId);
			result = statement.executeQuery();
			
			if( result.next() ){
				read = convertActionFromInt( result.getInt("Read") );
				modify = convertActionFromInt( result.getInt("Modify") );
				create = convertActionFromInt( result.getInt("Create") );
				delete = convertActionFromInt( result.getInt("Delete") );
				execute = convertActionFromInt( result.getInt("Execute") );
				control = convertActionFromInt( result.getInt("Control") );
			}
		} finally {
			if (result != null )
				result.close();
			
			if (statement != null )
				statement.close();
			
			if( connection != null )
				connection.close();
		}
		
		return new ObjectPermissionDescriptor(read, modify, create, execute, delete, control, AccessControlDescriptor.Subject.GROUP, groupId, objectId);
	}
	
	
	/**
	 * Method resolves the permissions using the ACL policy. This method is intended to be used in deriving the users' permissions in
	 * a situation where multiple permission sets may cascade. For example, a user may have a deny ACL in one group that overrides an
	 * unspecified setting in another group.
	 * @param existingPermission Represents the currently resolved permission setting; this permission can be reduced if the setterPermission is more restrictive or upgraded from unspecified to permitted
	 * @param setterPermission The users permission to the object 
	 * @return
	 */
	private AccessControlDescriptor.Action resolvePermission(AccessControlDescriptor.Action existingPermission, AccessControlDescriptor.Action setterPermission ){
		if( existingPermission == AccessControlDescriptor.Action.DENY || setterPermission == AccessControlDescriptor.Action.DENY ){
			return AccessControlDescriptor.Action.DENY;
		}
		else if( setterPermission == AccessControlDescriptor.Action.UNSPECIFIED ){ // This line should only be reachable if both permission sets are not deny
			return existingPermission;
		}
		else if( setterPermission == AccessControlDescriptor.Action.PERMIT ){ // This line should only be reachable if both permission sets are not deny and the spectific permissions
			return AccessControlDescriptor.Action.PERMIT;
		}
		
		//This should never be executed (assert)
		return AccessControlDescriptor.Action.UNSPECIFIED;
	}
	
	/**
	 * Retrieve the users' permissions that are specified for the object.
	 * @precondition A database connection must exist and the user ID must exist in the permissions table (or null will be returned)
	 * @postcondition An access control descriptor will be returned that indicates the users' permission to the given object
	 * @param userId
	 * @param objectId
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	private ObjectPermissionDescriptor getPermissionsByUser(int userId, long objectId ) throws SQLException, NoDatabaseConnectionException{
		
		// 0 -- Precondition check
		// The database connection will be checked in thr try block
		Connection connection = null;
		
		// 1 -- Determine the level of permissions
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.PERMISSIONS);
			
			if( connection == null )
				throw new NoDatabaseConnectionException();
			
			statement = connection.prepareStatement("Select * from Permissions where objectId = ? and userId = ?");
			statement.setLong(1, objectId);
			statement.setInt(2, userId);
			result = statement.executeQuery();
			if( result.next() ){
				AccessControlDescriptor.Action read = convertActionFromInt( result.getInt("Read") );
				AccessControlDescriptor.Action modify = convertActionFromInt( result.getInt("Modify") );
				AccessControlDescriptor.Action create = convertActionFromInt( result.getInt("Create") );
				AccessControlDescriptor.Action delete = convertActionFromInt( result.getInt("Delete") );
				AccessControlDescriptor.Action execute = convertActionFromInt( result.getInt("Execute") );
				AccessControlDescriptor.Action control = convertActionFromInt( result.getInt("Control") );
			
				return new ObjectPermissionDescriptor(read, modify, create, execute, delete, control, AccessControlDescriptor.Subject.USER, userId, objectId);
			}
			else
				return null;
			
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
	 * Set the permissions using the object ID in the access control descriptor.
	 * @param accessControlDescriptor
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 */
	public long setPermissions( ObjectPermissionDescriptor accessControlDescriptor ) throws NoDatabaseConnectionException, SQLException{ // NOPMD by luke on 5/26/07 11:13 AM
		synchronized( mutexAclExistence ){

			Connection connection = null;
			PreparedStatement objMapStatement = null;
			ResultSet objMapresult = null;
			PreparedStatement aclExistsStatement = null;
			ResultSet aclExistsResult = null;
			PreparedStatement statement = null;
			ResultSet result = null;

			try{
				// 0 -- Precondition check

				//	 0.1 -- Make sure database connection is available
				connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.PERMISSIONS);

				if( connection == null )
					throw new NoDatabaseConnectionException();

				// 0.2 -- Make sure object ID is valid
				long objectId = accessControlDescriptor.getObjectId();

				if( objectId >= 0){
					objMapStatement = connection.prepareStatement("Select * from ObjectMap where ObjectID = ?");
					objMapStatement.setLong( 1, objectId );
					objMapresult = objMapStatement.executeQuery();

					if( !objMapresult.next() )
						return -1; //Entry has not been allocated yet
				}

				// 1 -- Determine if ACL entry already exists
				boolean aclExistsAlready = false;

				if( accessControlDescriptor.isGroup() )
					aclExistsStatement = connection.prepareStatement("Select * from Permissions where ObjectID = ? and GroupID = ?");
				else
					aclExistsStatement = connection.prepareStatement("Select * from Permissions where ObjectID = ? and UserID = ?");

				aclExistsStatement.setLong(1,objectId);
				aclExistsStatement.setLong(2,accessControlDescriptor.getSubjectId());

				aclExistsResult = aclExistsStatement.executeQuery();

				if(aclExistsResult.next())
					aclExistsAlready = true;


				// 2a -- Update the existing ACL entry
				if(aclExistsAlready){

					if( accessControlDescriptor.isGroup() )
						statement = connection.prepareStatement("Update Permissions set Modify=?, \"Create\"=?, \"Delete\"=?, \"Read\"=?, \"Execute\"=?, Control=? where ObjectID = ? and GroupID = ?");
					else
						statement = connection.prepareStatement("Update Permissions set Modify=?, \"Create\"=?, \"Delete\"=?, \"Read\"=?, \"Execute\"=?, Control=? where ObjectID = ? and UserID = ?");

					statement.setInt( 1, accessControlDescriptor.getModifyPermission().ordinal() );
					statement.setInt( 2, accessControlDescriptor.getCreatePermission().ordinal() );
					statement.setInt( 3, accessControlDescriptor.getDeletePermission().ordinal() );
					statement.setInt( 4, accessControlDescriptor.getReadPermission().ordinal() );
					statement.setInt( 5, accessControlDescriptor.getExecutePermission().ordinal() );
					statement.setInt( 6, accessControlDescriptor.getControlPermission().ordinal() );
					statement.setLong( 7, objectId );
					statement.setInt( 8, accessControlDescriptor.getSubjectId() );
					statement.execute();

					return objectId;
				}


				// 2b -- Create a new ACL entry (since one does not already exist)
				else{

					//	 2.1b -- Create the ObjectMap entry to allocate the unique identifier

					//	   2.1.1b -- Allocate the object map entry if not already created
					if( objectId < 0 ){
						statement = connection.prepareStatement("Insert into ObjectMap", PreparedStatement.RETURN_GENERATED_KEYS);
						//statement.setString( 1, databaseTableName );
						statement.execute();
						result = statement.getGeneratedKeys();
						statement.close();
						statement = null;
						
						if( !result.next() ){
							return -1;
						}
						else
							objectId = result.getLong(1);
					}

					//	 2.1.2b -- Create the Permissions entry
					if( accessControlDescriptor.isGroup() )
						statement = connection.prepareStatement("Insert into Permissions (ObjectID, ParentObjectID, UserID, GroupID, Modify, \"Create\", \"Delete\", \"Read\", \"Execute\", Control) values (?, ?, -1, ?, ?, ?, ?, ?, ?, ?)");
					else
						statement = connection.prepareStatement("Insert into Permissions (ObjectID, ParentObjectID, UserID, GroupID, Modify, \"Create\", \"Delete\", \"Read\", \"Execute\", Control) values (?, ?, ?, -1, ?, ?, ?, ?, ?, ?)");

					statement.setLong( 1, objectId );
					statement.setLong( 2, -1 );
					statement.setInt( 3, accessControlDescriptor.getSubjectId() );
					statement.setInt( 4, accessControlDescriptor.getModifyPermission().ordinal() );
					statement.setInt( 5, accessControlDescriptor.getCreatePermission().ordinal() );
					statement.setInt( 6, accessControlDescriptor.getDeletePermission().ordinal() );
					statement.setInt( 7, accessControlDescriptor.getReadPermission().ordinal() );
					statement.setInt( 8, accessControlDescriptor.getExecutePermission().ordinal() );
					statement.setInt( 9, accessControlDescriptor.getControlPermission().ordinal() );
					statement.execute();
					
					return objectId;
				}

			} finally {
				if (result != null )
					result.close();

				if (statement != null )
					statement.close();

				if( connection != null )
					connection.close();

				if( objMapStatement != null )
					objMapStatement.close();

				if( objMapresult != null )
					objMapresult.close();

				if( objMapresult != null )
					objMapresult.close();

				if( aclExistsStatement != null )
					aclExistsStatement.close();

				if( aclExistsResult != null )
					aclExistsResult.close();			
			}
		}
	}
	
	/**
	 * This method removes the ACLs associated with the user and the given object ID.
	 * @precondition A database connection must be available
	 * @postcondition The ACLs for the given object and user will be deleted
	 * @param userId
	 * @param objectId
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 */
	public boolean deleteUserPermissions( long userId, long objectId ) throws NoDatabaseConnectionException, SQLException{
		// 0 -- Precondition check
		// The database connection will be checked in the try block
		Connection connection = null;
		
		// 1 -- Remove the permissions
		synchronized( mutexAclExistence ){
			PreparedStatement statement = null;
			try{
				connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.PERMISSIONS);
				if( connection == null )
					throw new NoDatabaseConnectionException();
				
				statement = connection.prepareStatement("Delete from Permissions where UserID = ? and ObjectID = ?");
				statement.setLong(1, userId);
				statement.setLong(2, objectId);
				statement.executeUpdate();
				
			} finally {
				if (statement != null )
					statement.close();
				
				if( connection != null )
					connection.close();
			}
		}
		return true;
	}
	
	/**
	 * This method removes the ACLs associated with the group and the given object ID.
	 * @precondition A database connection must be available
	 * @postcondition The ACLs for the given object and user will be deleted
	 * @param userId
	 * @param objectId
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 */
	public boolean deleteGroupPermissions( long groupId, long objectId ) throws NoDatabaseConnectionException, SQLException{
		// 0 -- Precondition check
		// The database connection will be checked in the try block
		Connection connection = null;
		
		// 1 -- Remove the permissions
		PreparedStatement statement = null;
		synchronized (mutexAclExistence){
			try{
				connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.PERMISSIONS);
				if( connection == null )
					throw new NoDatabaseConnectionException();
				
				
				statement = connection.prepareStatement("Delete from Permissions where GroupID = ? and ObjectID = ?");
				statement.setLong(1, groupId);
				statement.setLong(2, objectId);
				statement.executeUpdate();
				
			} finally {
				
				if (statement != null )
					statement.close();
				
				if( connection != null )
					connection.close();
			}
		}
		return true;
	}
	
	/**
	 * Delete the rights for the given group.
	 * @param groupId
	 * @param rightName
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 */
	public boolean deleteGroupRight( long groupId, String rightName ) throws NoDatabaseConnectionException, SQLException{
		// 0 -- Precondition check
		// The database connection will be checked in the try block
		Connection connection = null;
		
		// 1 -- Get the object ID for the right
		PreparedStatement statement = null;
		PreparedStatement statement2 = null;
		ResultSet result = null;
		
		synchronized(mutexAclExistence){
			try{
				connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.PERMISSIONS);
				if( connection == null )
					throw new NoDatabaseConnectionException();
				
				
				statement = connection.prepareStatement("Select * from Rights where RightName = ?");
				statement.setString( 1, rightName );
				result = statement.executeQuery();
				
				if( !result.next() ){
					return false; //The right was not found
				}
				
				long objectId = result.getLong("ObjectID");
				
				// 2 -- Remove the permissions
				statement2 = connection.prepareStatement("Delete from Permissions where GroupID = ? and ObjectID = ?");
				statement2.setLong(1, groupId);
				statement2.setLong(2, objectId);
				statement2.executeUpdate();
			} finally {
				if (statement2 != null )
					statement2.close();
				
				if (statement != null )
					statement.close();
				
				if( connection != null )
					connection.close();
				
				if( result != null )
					result.close();
			}
		}
		
		return true;
	}
	
	/**
	 * Delete the rights for the given user.
	 * @param userId
	 * @param rightName
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 */
	public boolean deleteUserRight( long userId, String rightName ) throws NoDatabaseConnectionException, SQLException{
		// 0 -- Precondition check
		// The database connection will be checked in the try block
		Connection connection = null;
		
		// 1 -- Get the object ID for the right
		PreparedStatement statement = null;
		PreparedStatement statement2 = null;
		ResultSet result = null;
		
		synchronized(mutexAclExistence){
			try{
				connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.PERMISSIONS);
				if( connection == null )
					throw new NoDatabaseConnectionException();
				
				statement = connection.prepareStatement("Select * from Rights where RightName = ?");
				statement.setString( 1, rightName );
				result = statement.executeQuery();
				
				if( !result.next() ){
					return false; //The right was not found
				}
				
				long objectId = result.getLong("ObjectID");
				
				// 2 -- Remove the permissions
				statement2 = connection.prepareStatement("Delete from Permissions where UserID = ? and ObjectID = ?");
				statement2.setLong(1, userId);
				statement2.setLong(2, objectId);
				statement2.executeUpdate();
				
			} finally {
				if (statement2 != null )
					statement2.close();
				
				if (statement != null )
					statement.close();
				
				if( connection != null )
					connection.close();
				
				if( result != null )
					result.close();
			}
		}
		
		return true;
	}
	
	/**
	 * Get the names of the supported rights
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	protected String[] getPossibleRights() throws SQLException, NoDatabaseConnectionException{
		
		// 1 -- Get the list of possible rights
		Vector<String> possibleRights = new Vector<String>();
		
		{
			Connection connection = null;
			
			// 1.1 -- Get the object ID for the right
			PreparedStatement statement = null;
			ResultSet result = null;
			
			try{
				connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.PERMISSIONS);
				
				statement = connection.prepareStatement("Select RightName from Rights");
				result = statement.executeQuery();
				
				// 1.2 -- Get the access descriptor for the object ID
				while( result.next() ){
					String value = result.getString("RightName");
					
					if( value != null ){
						possibleRights.add(value);
					}
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
		
		// 1.3 -- Return the list as an array
		String[] possibleRightsArray = new String[possibleRights.size()];
		possibleRights.toArray( possibleRightsArray );
		
		return possibleRightsArray;
	}
	
	/**
	 * Get all right descriptors that corresponds to the given user.
	 * @precondition The user ID and the right name must be valid
	 * @postcondition An access control descriptor indicating the user's rights will be returned or null if the right could not be found
	 * @param userId
	 * @param resolveUserGroupPermissions
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public RightDescriptor[] getUserRights( int userId, boolean resolveUserGroupPermissions ) throws SQLException, NoDatabaseConnectionException{
		
		// 1 -- Get the rights for the user
		String[] supportedRights = getPossibleRights();
		Vector<RightDescriptor> rights = new Vector<RightDescriptor>();
		
		for (String right : supportedRights) {
			try{
				rights.add(  getRight( userId, right, false, resolveUserGroupPermissions ) ) ;
			}
			catch(NotFoundException e){
				appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e);
			}
		}
		
		RightDescriptor[] rightsArray = new RightDescriptor[rights.size()];
		rights.toArray(rightsArray);
		
		return rightsArray;
	}
	
	/**
	 * Get the right descriptor that corresponds to the given user. Returns null if the right does not exist.
	 * @precondition The user ID and the right name must be valid
	 * @postcondition An access control descriptor indicating the user's rights will be returned or null if the right could not be found
	 * @param userId
	 * @param right
	 * @param recurseGroupAcls
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws NotFoundException 
	 */
	public RightDescriptor getUserRight( int userId, String right, boolean resolveUserGroupPermissions ) throws SQLException, NoDatabaseConnectionException, NotFoundException{
		return getRight( userId, right, false, resolveUserGroupPermissions );
	}
	
	/**
	 * Get the access control descriptor that corresponds to the given group.
	 * @param groupId
	 * @param right
	 * @param recurseGroupAcls
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws NotFoundException 
	 */
	public RightDescriptor getGroupRight( int groupId, String right ) throws SQLException, NoDatabaseConnectionException, NotFoundException{
		return getRight( groupId, right, true, true );
	}
	
	
	/**
	 * Creates a new right with the given name and description.
	 * @param rightName
	 * @param rightDescription
	 * @return
	 * @throws SQLException 
	 * @throws NoDatabaseConnectionException 
	 */
	public boolean createNewRight( String rightName, String rightDescription ) throws SQLException, NoDatabaseConnectionException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the name is valid
		if( rightName == null || rightName.trim().length() == 0){ // NOPMD by luke on 5/26/07 10:22 AM
			throw new IllegalArgumentException("The right name must not be empty or null");
		}
		
		// 1 -- Allocate the ObjectID
		long objectId = allocateObjectId("UserRights", this.appRes);
		
		
		// 2 -- Insert the Information Describing the Right
		Connection connection = null;
		PreparedStatement statement = null;
		int results;
		
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.PERMISSIONS);
			statement = connection.prepareStatement("insert into Rights(RightName, RightDescription, ObjectID) values (?,?,?)");
			statement.setString(1, rightName);
			statement.setString(2, rightDescription);
			statement.setLong(3, objectId);
			
			results = statement.executeUpdate();
		}
		finally{
			if( connection != null )
				connection.close();
			
			if( statement != null )
				statement.close();
		}
		
		// 3 -- Return a value indicating if the update was successful
		return results > 0 ;
	}
	
	/**
	 * Get the rights associated with the specified user or group.
	 * @param id
	 * @param right
	 * @param isGroupPermission
	 * @param recurseGroupAcls
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws NotFoundException 
	 */
	private RightDescriptor getRight( int id, String right, boolean isGroupPermission, boolean recurseGroupAcls ) throws SQLException, NoDatabaseConnectionException, NotFoundException{
		
		// 0 -- Precondition check
		Connection connection = null;
		
		// 1 -- Get the object ID for the right
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.PERMISSIONS);
			
			statement = connection.prepareStatement("Select * from Rights where RightName = ?");
			statement.setString(1, right);
			result = statement.executeQuery();
			
			if( !result.next() ){
				throw new NotFoundException("No such right exists (\"" + right + "\")");
			}
			
			// 2 -- Get the access descriptor for the object ID
			long objectId = result.getLong("ObjectID");
			
			ObjectPermissionDescriptor objectPermissionDescriptor;
			
			if( isGroupPermission  ){ //Is a group
				objectPermissionDescriptor = getGroupPermissions(id, objectId);				
			}
			else{ //Is a user
				if( recurseGroupAcls ){
					objectPermissionDescriptor = getPermissionsByGroups( id, objectId );
					
					// Resolve the permissions specific to the user (not necessary if the group permissions explicitly denied access)
					if( objectPermissionDescriptor != null && objectPermissionDescriptor.getExecutePermission() != AccessControlDescriptor.Action.DENY ){
						ObjectPermissionDescriptor userObjectPermissionDescriptor = getPermissionsByUser( id, objectId );
						
						if( userObjectPermissionDescriptor != null && objectPermissionDescriptor != null){
							objectPermissionDescriptor = userObjectPermissionDescriptor.resolvePermissions(objectPermissionDescriptor);
						}
						else{
							objectPermissionDescriptor = userObjectPermissionDescriptor;
						}
					}
					// No group permission descriptor was found, just use the user descriptor
					else{
						objectPermissionDescriptor = getPermissionsByUser( id, objectId );
					}

				}
				else{
					objectPermissionDescriptor = getPermissionsByUser( id, objectId );
				}
			}
			
			if( objectPermissionDescriptor == null ){
				if( isGroupPermission ){
					objectPermissionDescriptor = new ObjectPermissionDescriptor( ObjectPermissionDescriptor.Action.UNSPECIFIED, ObjectPermissionDescriptor.Action.UNSPECIFIED, ObjectPermissionDescriptor.Action.UNSPECIFIED, ObjectPermissionDescriptor.Action.UNSPECIFIED, ObjectPermissionDescriptor.Action.UNSPECIFIED, ObjectPermissionDescriptor.Action.UNSPECIFIED, AccessControlDescriptor.Subject.GROUP, id, objectId );
				}
				else{
					objectPermissionDescriptor = new ObjectPermissionDescriptor( ObjectPermissionDescriptor.Action.UNSPECIFIED, ObjectPermissionDescriptor.Action.UNSPECIFIED, ObjectPermissionDescriptor.Action.UNSPECIFIED, ObjectPermissionDescriptor.Action.UNSPECIFIED, ObjectPermissionDescriptor.Action.UNSPECIFIED, ObjectPermissionDescriptor.Action.UNSPECIFIED, AccessControlDescriptor.Subject.USER, id, objectId );
				}
			}
			
			return new RightDescriptor(objectPermissionDescriptor.getExecutePermission(), objectPermissionDescriptor.getSubjectType(), id, right );
			
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
	 * Set the right ACLs for the given user.
	 * @param userId
	 * @param rightName
	 * @param accessControlDescriptor
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public boolean setPermissions( RightDescriptor rightDescriptor) throws SQLException, NoDatabaseConnectionException{
		return setRight( rightDescriptor );
	}
	
	/**
	 * Sets the rights for the given user or group based on the access control specifications given.
	 * @param id
	 * @param right
	 * @param accessControlDescriptor
	 * @param isGroupPermission
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public synchronized boolean setRight( RightDescriptor rightDescriptor ) throws SQLException, NoDatabaseConnectionException{
		
		// 0 -- Precondition check
		Connection connection = appRes.getDatabaseConnection( Application.DatabaseAccessType.PERMISSIONS );
		
		// 1 -- Get the object ID for the right
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try{
			statement = connection.prepareStatement("Select * from Rights where RightName = ?");
			statement.setString(1, rightDescriptor.getRightName() );
			result = statement.executeQuery();
			
			if( !result.next() ){
				return false; //The right was not found
			}
			
			long objectId = result.getLong("ObjectID");
			
			if( objectId < 1 ){
				objectId = allocateObjectId("Rights", Application.getApplication());
				PreparedStatement statementAddObjectId = null;
				
				statementAddObjectId = connection.prepareStatement("Update Rights set ObjectID = ? where RightName = ?");
				statementAddObjectId.setLong(1, objectId );
				statementAddObjectId.setString(2, rightDescriptor.getRightName() );
				statementAddObjectId.executeUpdate();
				
				statementAddObjectId.close();
			}
			
			// 2 -- Set the ACL for the right
			/* Note that we create an object permission descriptor that references a right. Although an individual right is global and singular (only one exists),
			 * rights are represented as objects. The RightDescriptor object is just an easy way of dealing with individual rights and addressing them by name as
			 * opposed to addressing them by object ID (which can be obtuse). 
			 */
			ObjectPermissionDescriptor objectPermissionDescriptor = new ObjectPermissionDescriptor( AccessControlDescriptor.Action.UNSPECIFIED, AccessControlDescriptor.Action.UNSPECIFIED, AccessControlDescriptor.Action.UNSPECIFIED, rightDescriptor.getRight(), AccessControlDescriptor.Action.UNSPECIFIED, AccessControlDescriptor.Action.UNSPECIFIED, rightDescriptor.getSubjectType(), rightDescriptor.getSubjectId(), objectId );
			
			return setPermissions( objectPermissionDescriptor ) >= 0;
			
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
	 * This method retrieves all ACL entries for the given object.
	 * @param objectId
	 * @return
	 * @throws SQLException 
	 * @throws NoDatabaseConnectionException 
	 */
	public ObjectPermissionDescriptor[] getAllAclEntries( long objectId ) throws SQLException, NoDatabaseConnectionException{
		
		// 0 -- Precondition check
		// The database connection will be checked in the try block
		Connection connection = null;
		
		// 1 -- Retieve the list of entries
		PreparedStatement statement = null;
		ResultSet result = null;
		Vector<ObjectPermissionDescriptor> aces = new Vector<ObjectPermissionDescriptor>();
		
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.PERMISSIONS);
			
			if( connection == null )
				throw new NoDatabaseConnectionException();
			
			statement = connection.prepareStatement("Select * from Permissions where objectId = ?");
			statement.setLong(1, objectId);
			result = statement.executeQuery();
			
			while( result.next() ){
				AccessControlDescriptor.Action read = convertActionFromInt( result.getInt("Read") );
				AccessControlDescriptor.Action modify = convertActionFromInt( result.getInt("Modify") );
				AccessControlDescriptor.Action create = convertActionFromInt( result.getInt("Create") );
				AccessControlDescriptor.Action delete = convertActionFromInt( result.getInt("Delete") );
				AccessControlDescriptor.Action execute = convertActionFromInt( result.getInt("Execute") );
				AccessControlDescriptor.Action control = convertActionFromInt( result.getInt("Control") );
			
				int userId = result.getInt("UserID");
				int groupId = result.getInt("GroupID");
				AccessControlDescriptor.Subject subjectType;
				
				int id;
				if( userId >= 0 ){
					subjectType = AccessControlDescriptor.Subject.USER;
					id = userId;
				}
				else{
					subjectType = AccessControlDescriptor.Subject.GROUP;
					id = groupId;
				}
				
				ObjectPermissionDescriptor objectPermissionDescriptor = new ObjectPermissionDescriptor(read, modify, create, execute, delete, control, subjectType, id, objectId );
				
				aces.add( objectPermissionDescriptor );
			}
			
		} finally {
			if (result != null )
				result.close();
			
			if (statement != null )
				statement.close();
			
			if( connection != null )
				connection.close();
		}
		
		// 2 -- Produce the array version of the list
		ObjectPermissionDescriptor[] acl = new ObjectPermissionDescriptor[aces.size()];
		for(int c = 0; c < aces.size(); c++){
			ObjectPermissionDescriptor entry = aces.get(c);
			acl[c] = entry;
		}
		
		return acl;
	}
	
	/**
	 * Creates a new object identifier entry that will be used for associating an Access Control List entry to an object.  
	 * @param databaseTableDescription
	 * @param app
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public static synchronized long allocateObjectId( String databaseTableDescription, Application app ) throws SQLException, NoDatabaseConnectionException{
		
		// 0 -- Precondition check
		
		// 1 -- Connect to the database and create the object map entry
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try{
			connection = app.getDatabaseConnection( Application.DatabaseAccessType.PERMISSIONS );
			
			statement = connection.prepareStatement("Insert into ObjectMap(\"Table\") values(?)", PreparedStatement.RETURN_GENERATED_KEYS);
			statement.setString( 1, databaseTableDescription );
			
			statement.executeUpdate();
			
			result = statement.getGeneratedKeys();
			
			if( result.next() )
				return result.getLong(1);
			else
				return -1;
		}
		finally{
			if (statement != null )
				statement.close();
			
			if (connection != null )
				connection.close();
			
			if (result != null )
				result.close();
		}
	}
}
