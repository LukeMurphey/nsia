package net.lukemurphey.nsia.tests;

import java.sql.SQLException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.UserManagement;

import junit.framework.TestCase;

public class UserManagementTest extends TestCase {
	UserManagement userManagement;
	Application app = null;
	
	public void setUp() throws TestApplicationException{
		app = TestApplication.getApplication();
		userManagement = new UserManagement(app);
	}
	
	public void tearDown(){
		TestApplication.stopApplication();
	}

	public void testGetUserDescriptorLong() throws SQLException, NoDatabaseConnectionException, NotFoundException {
		UserManagement.UserDescriptor userDesc = userManagement.getUserDescriptor(1);
		if( userDesc == null )
			fail("The user was not found");
	}

	public void testGetUserDescriptorString() throws SQLException, InputValidationException, NoDatabaseConnectionException, NotFoundException {
		UserManagement.UserDescriptor userDesc = userManagement.getUserDescriptor("Test");
		
		if( userDesc == null )
			fail("The user was not found using case sensitive call");
	}
	
	public void testGetUserDescriptorStringWrongCase() throws SQLException, InputValidationException, NoDatabaseConnectionException, NotFoundException {
		UserManagement.UserDescriptor userDesc = userManagement.getUserDescriptor("test");
		
		if( userDesc == null )
			fail("The user was not found using case insensitive call");
	}

	public void testGetUserId() throws SQLException, InputValidationException, NoDatabaseConnectionException {
		long userId = userManagement.getUserID("Test");
		
		if( userId == -1 )
			fail("The user was not found");
	}

	public void testDisableAccount() throws SQLException, NoDatabaseConnectionException, InputValidationException, NotFoundException {
		if( !userManagement.disableAccount(1) )
			fail("The account could not be disabled");
		
		UserManagement.UserDescriptor userDesc = userManagement.getUserDescriptor(1);
		if( userDesc.getAccountStatus() != UserManagement.AccountStatus.DISABLED )
			fail("Account was not disabled");
		
		if( !userManagement.enableAccount(1) )
			fail("The account could not be re-enabled");
		
		 userDesc = userManagement.getUserDescriptor(1);
		if( userDesc.getAccountStatus() != UserManagement.AccountStatus.VALID_USER )
			fail("Account was not re-enabled");
	}

	public void testAddAccount() {

	}

	public void testChangePassword() {

	}

	public void testUpdateUserInfo() {

	}

}
