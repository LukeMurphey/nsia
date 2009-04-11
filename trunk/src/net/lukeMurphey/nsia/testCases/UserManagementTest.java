package net.lukeMurphey.nsia.testCases;

import java.net.BindException;
import java.sql.SQLException;

import net.lukeMurphey.nsia.InputValidationException;
import net.lukeMurphey.nsia.NoDatabaseConnectionException;
import net.lukeMurphey.nsia.NotFoundException;
import net.lukeMurphey.nsia.TestsConfig;
import net.lukeMurphey.nsia.UserManagement;

import junit.framework.TestCase;

public class UserManagementTest extends TestCase {
	UserManagement userManagement;
	
	public static void main(String[] args) {
	}

	public UserManagementTest(String name) throws BindException, SQLException, InputValidationException, Exception {
		super(name);
		
		userManagement = new UserManagement(TestsConfig.getApplicationResource());
	}

	/*
	 * Test method for 'net.lukeMurphey.siteSentry.UserManagement.getUserDescriptor(long)'
	 */
	public void testGetUserDescriptorLong() throws SQLException, NoDatabaseConnectionException, NotFoundException {
		UserManagement.UserDescriptor userDesc = userManagement.getUserDescriptor(1);
		if( userDesc == null )
			fail("The user was not found");
	}

	/*
	 * Test method for 'net.lukeMurphey.siteSentry.UserManagement.getUserDescriptor(String)'
	 */
	public void testGetUserDescriptorString() throws SQLException, InputValidationException, NoDatabaseConnectionException, NotFoundException {
		UserManagement.UserDescriptor userDesc = userManagement.getUserDescriptor("Luke");
		if( userDesc == null )
			fail("The user was not found using case sensitive call");
		
		userDesc = userManagement.getUserDescriptor("luke");
		if( userDesc == null )
			fail("The user was not found using case insensitive call");
	}

	/*
	 * Test method for 'net.lukeMurphey.siteSentry.UserManagement.getUserId(String)'
	 */
	public void testGetUserId() throws SQLException, InputValidationException, NoDatabaseConnectionException {
		long userId = userManagement.getUserID("luke");
		
		if( userId == -1 )
			fail("The user was not found");
	}

	/*
	 * Test method for 'net.lukeMurphey.siteSentry.UserManagement.disableAccount(long)'
	 */
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

	/*
	 * Test method for 'net.lukeMurphey.siteSentry.UserManagement.addAccount(String, String, String, String, long, EmailAddress, boolean)'
	 */
	public void testAddAccount() {

	}

	/*
	 * Test method for 'net.lukeMurphey.siteSentry.UserManagement.changePassword(long, String)'
	 */
	public void testChangePassword() {

	}

	/*
	 * Test method for 'net.lukeMurphey.siteSentry.UserManagement.updateUserInfo(long, String, EmailAddress, boolean)'
	 */
	public void testUpdateUserInfo() {

	}

}
