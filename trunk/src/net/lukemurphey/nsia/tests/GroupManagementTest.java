package net.lukemurphey.nsia.tests;

import java.io.IOException;
import java.net.BindException;
import java.sql.SQLException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GroupManagement;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.GroupManagement.GroupDescriptor;

import junit.framework.TestCase;

public class GroupManagementTest extends TestCase {
	
	GroupManagement groupManagement;
	Application app = null;
	
	public void setUp() throws NoDatabaseConnectionException, IOException{
		app = TestApplication.getApplication();
		groupManagement = new GroupManagement(app);
	}
	
	public void tearDown(){
		TestApplication.stopApplication();
	}
	
	public GroupManagementTest(String name) throws BindException, SQLException, InputValidationException, Exception {
		super(name);
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.GroupManagement.getGroupId(String)'
	 */
	public void testGetGroupId() throws InputValidationException, SQLException, NoDatabaseConnectionException {
		if( groupManagement.getGroupID("Analysts") < 1 )
			fail("Group could not be found");
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.GroupManagement.addGroup(String, String)'
	 */
	public void testAddGroup() throws SQLException, NoDatabaseConnectionException, InputValidationException, NotFoundException {
		int groupID = groupManagement.addGroup("SomeTestGroup", "For testing...");
		
		GroupDescriptor groupDescriptor = groupManagement.getGroupDescriptor(groupID);
		
		if( groupDescriptor == null ){
			fail("Group was not successfully created");
		}
		else if(groupDescriptor.getGroupName().equals("SomeTestGroup") == false ){
			fail("Group was not loaded correctly");
		}
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.GroupManagement.getGroupDescriptor(long)'
	 */
	public void testGetGroupDescriptorLong() throws SQLException, InputValidationException, NoDatabaseConnectionException, NotFoundException {
		GroupManagement.GroupDescriptor groupDesc = groupManagement.getGroupDescriptor(1);
		
		if( groupDesc == null )
			fail("The group could not be found");
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.GroupManagement.getGroupDescriptor(String)'
	 */
	public void testGetGroupDescriptorString() throws SQLException, InputValidationException, NoDatabaseConnectionException, NotFoundException {
		GroupManagement.GroupDescriptor groupDesc = groupManagement.getGroupDescriptor("Analysts");
		
		if( groupDesc == null )
			fail("The group could not be found");
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.GroupManagement.updateGroupInfo(long, String, String)'
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
	 * Test method for 'net.lukemurphey.siteSentry.GroupManagement.disableGroup(long)'
)	 */
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
	 * Test method for 'net.lukemurphey.siteSentry.GroupManagement.getGroupDescriptors()'
	 */
	public void testGetGroupDescriptors() throws SQLException, InputValidationException, NoDatabaseConnectionException {
		if( groupManagement.getGroupDescriptors() == null || groupManagement.getGroupDescriptors().length == 0 )
			fail("No groups were found");
	}

}
