package net.lukeMurphey.nsia.testCases;

import java.net.BindException;
import java.sql.SQLException;

import net.lukeMurphey.nsia.GroupManagement;
import net.lukeMurphey.nsia.InputValidationException;
import net.lukeMurphey.nsia.NoDatabaseConnectionException;
import net.lukeMurphey.nsia.NotFoundException;
import net.lukeMurphey.nsia.TestsConfig;

import junit.framework.TestCase;

public class GroupManagementTest extends TestCase {
	GroupManagement groupManagement;
	
	public static void main(String[] args) {
	}

	public GroupManagementTest(String name) throws BindException, SQLException, InputValidationException, Exception {
		super(name);
		
		groupManagement = new GroupManagement(TestsConfig.getApplicationResource());
	}

	/*
	 * Test method for 'net.lukeMurphey.siteSentry.GroupManagement.getGroupId(String)'
	 */
	public void testGetGroupId() throws InputValidationException, SQLException, NoDatabaseConnectionException {
		if( groupManagement.getGroupID("Analysts") < 1 )
			fail("Group could not be found");
	}

	/*
	 * Test method for 'net.lukeMurphey.siteSentry.GroupManagement.addGroup(String, String)'
	 */
	public void testAddGroup() {

	}

	/*
	 * Test method for 'net.lukeMurphey.siteSentry.GroupManagement.getGroupDescriptor(long)'
	 */
	public void testGetGroupDescriptorLong() throws SQLException, InputValidationException, NoDatabaseConnectionException, NotFoundException {
		GroupManagement.GroupDescriptor groupDesc = groupManagement.getGroupDescriptor(1);
		
		if( groupDesc == null )
			fail("The group could not be found");
	}

	/*
	 * Test method for 'net.lukeMurphey.siteSentry.GroupManagement.getGroupDescriptor(String)'
	 */
	public void testGetGroupDescriptorString() throws SQLException, InputValidationException, NoDatabaseConnectionException, NotFoundException {
		GroupManagement.GroupDescriptor groupDesc = groupManagement.getGroupDescriptor("Analysts");
		
		if( groupDesc == null )
			fail("The group could not be found");
	}

	/*
	 * Test method for 'net.lukeMurphey.siteSentry.GroupManagement.updateGroupInfo(long, String, String)'
	 */
	public void testUpdateGroupInfo() throws SQLException, InputValidationException, NoDatabaseConnectionException, NotFoundException {
		GroupManagement.GroupDescriptor groupDesc = groupManagement.getGroupDescriptor(1);
		String oldDesc = groupDesc.getDescription();
		
		String newDesc = oldDesc + " Test";
		groupManagement.updateGroupInfo(1, groupDesc.getGroupName(), newDesc );
		GroupManagement.GroupDescriptor groupDescAfter = groupManagement.getGroupDescriptor(1);
		
		if( groupDescAfter.getDescription().length() != newDesc.length() )
			fail("The new description was not saved");
		
		groupManagement.updateGroupInfo(1, groupDesc.getGroupName(), oldDesc );
		groupDescAfter = groupManagement.getGroupDescriptor(1);
		if( groupDescAfter.getDescription().length() != oldDesc.length() )
			fail("The old description was not re-saved");
	}

	/*
	 * Test method for 'net.lukeMurphey.siteSentry.GroupManagement.disableGroup(long)'
	 */
	public void testDisableGroup() throws SQLException, NoDatabaseConnectionException, InputValidationException, NotFoundException {
		if( !groupManagement.disableGroup(1) )
			fail("The account could not be disabled");
		
		GroupManagement.GroupDescriptor groupDesc = groupManagement.getGroupDescriptor(1);
		if( groupDesc.getGroupState() != GroupManagement.State.INACTIVE )
			fail("Account was not disabled");
		
		if( !groupManagement.enableGroup(1) )
			fail("The account could not be re-enabled");
		
		groupDesc = groupManagement.getGroupDescriptor(1);
		if( groupDesc.getGroupState() != GroupManagement.State.ACTIVE )
			fail("Account was not re-enabled");
		
	}

	/*
	 * Test method for 'net.lukeMurphey.siteSentry.GroupManagement.getGroupDescriptors()'
	 */
	public void testGetGroupDescriptors() throws SQLException, InputValidationException, NoDatabaseConnectionException {
		if( groupManagement.getGroupDescriptors() == null || groupManagement.getGroupDescriptors().length == 0 )
			fail("No groups were found");
	}

}
