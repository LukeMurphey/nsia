package net.lukemurphey.nsia.tests;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.Authentication;
import net.lukemurphey.nsia.ClientData;
import net.lukemurphey.nsia.EmailAddress;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InvalidLocalPartException;
import net.lukemurphey.nsia.LocalPasswordAuthentication;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.NumericalOverflowException;
import net.lukemurphey.nsia.PasswordAuthenticationValidator;
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

	public void testAddAccount() throws NoSuchAlgorithmException, UnknownHostException, SQLException, InputValidationException, NoDatabaseConnectionException, InvalidLocalPartException, NotFoundException {
		int userID = this.userManagement.addAccount("testAddAccount", "testAddAccount", "^&89dsd879uaidst67", EmailAddress.getByAddress( "test@whatever.com" ), false);
		
		if( userManagement.getUserDescriptor(userID) == null ){
			fail("User account was not successfully created");
		}
	}

	public void testAddAccountIdentical() throws NoSuchAlgorithmException, UnknownHostException, SQLException, InputValidationException, NoDatabaseConnectionException, InvalidLocalPartException, NotFoundException {
		int userID = this.userManagement.addAccount("someUser", "someUser", "^&89dsd879uaidst67", EmailAddress.getByAddress( "test@whatever.com" ), false);
		
		if( userManagement.getUserDescriptor(userID) == null ){
			fail("User account was not successfully created");
		}
		
		int secondUserID = this.userManagement.addAccount("someUser", "someUser", "^&89dsd879uaidst67", EmailAddress.getByAddress( "test@whatever.com" ), false);
		
		if( secondUserID >= 0 ){
			fail("Account with same user name was allowed to be created");
		}
	}
	
	public void testAddAccountIdenticalDiffCase() throws NoSuchAlgorithmException, UnknownHostException, SQLException, InputValidationException, NoDatabaseConnectionException, InvalidLocalPartException, NotFoundException {
		int userID = this.userManagement.addAccount("someUser", "someUser", "^&89dsd879uaidst67", EmailAddress.getByAddress( "test@whatever.com" ), false);
		
		if( userManagement.getUserDescriptor(userID) == null ){
			fail("User account was not successfully created");
		}
		
		int secondUserID = this.userManagement.addAccount("SomEUser", "someUser", "^&89dsd879uaidst67", EmailAddress.getByAddress( "test@whatever.com" ), false);
		
		if( secondUserID >= 0 ){
			fail("Account with same user name was allowed to be created with different case in filename");
		}
	}
	
	public void testChangePassword() throws NoSuchAlgorithmException, UnknownHostException, SQLException, InputValidationException, NoDatabaseConnectionException, InvalidLocalPartException, NotFoundException, NumericalOverflowException {
		int userID = this.userManagement.addAccount("someUser", "someUser", "^&89dsd879uaidst67", EmailAddress.getByAddress( "test@whatever.com" ), false);
		
		if( userManagement.getUserDescriptor(userID) == null ){
			fail("User account was not successfully created");
		}
		
		userManagement.changePassword(userID, "opensesame");
		
		//Make sure the password changed
		PasswordAuthenticationValidator validator = new PasswordAuthenticationValidator("opensesame"); //This is actual password of the user
		ClientData clientData = new ClientData( InetAddress.getLocalHost(), "Eclipse Test JUnit Case" );
		LocalPasswordAuthentication localPwd = new LocalPasswordAuthentication( app );
		Authentication.AuthenticationResult authResult = localPwd.authenticate("someUser", validator, clientData);
		
		if( authResult.getAuthenticationStatus() != Authentication.AuthenticationResult.AUTH_SUCCESS ){
			fail("The authentication failed for the user after changing the password");
		}
	}

	public void testUpdateUserInfo() {

	}

}
