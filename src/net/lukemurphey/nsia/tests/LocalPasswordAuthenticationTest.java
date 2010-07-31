package net.lukemurphey.nsia.tests;

import junit.framework.TestCase;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.io.IOException;
import java.net.*;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.Authentication;
import net.lukemurphey.nsia.ClientData;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.LocalPasswordAuthentication;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NumericalOverflowException;
import net.lukemurphey.nsia.PasswordAuthenticationValidator;

public class LocalPasswordAuthenticationTest extends TestCase {
	LocalPasswordAuthentication localPwd;
	Application app = null;
	
	public void setUp() throws NoDatabaseConnectionException, IOException{
		app = TestApplication.getApplication();
		localPwd = new LocalPasswordAuthentication( app );
	}
	
	public void tearDown(){
		TestApplication.stopApplication();
	}

	public void testAuthenticateStringPasswordAuthenticationValidatorValid() throws UnknownHostException, NoSuchAlgorithmException, SQLException, InputValidationException, NoDatabaseConnectionException, NumericalOverflowException{
		PasswordAuthenticationValidator validator = new PasswordAuthenticationValidator("asdfasdf"); //This is actual password of the user
		ClientData clientData = new ClientData( InetAddress.getLocalHost(), "Eclipse Test JUnit Case" );
		
		Authentication.AuthenticationResult authResult = localPwd.authenticate("Test", validator, clientData);
		
		if( authResult.getAuthenticationStatus() != Authentication.AuthenticationResult.AUTH_SUCCESS ){
			fail("The authentication failed for a valid user");
		}
		
	}
	
	public void testAuthenticateStringPasswordAuthenticationValidatorInvalid() throws NoSuchAlgorithmException, SQLException, InputValidationException, NoDatabaseConnectionException, NumericalOverflowException, UnknownHostException {

		PasswordAuthenticationValidator validator = new PasswordAuthenticationValidator("invalidPassword");
		ClientData clientData = new ClientData( InetAddress.getLocalHost(), "Eclipse Test JUnit Case" );
		
		Authentication.AuthenticationResult authResult = localPwd.authenticate("Test", validator, clientData);
		
		if( authResult.getAuthenticationStatus() == Authentication.AuthenticationResult.AUTH_SUCCESS ){
			fail("The authentication succeeded for a invalid user");
		}
	}

	
	public void testValidAccountLockout() throws SQLException, NumericalOverflowException, InputValidationException, NoDatabaseConnectionException, NoSuchAlgorithmException, UnknownHostException {
		
		PasswordAuthenticationValidator validator = new PasswordAuthenticationValidator("invalidPassword");
		ClientData clientData = new ClientData( InetAddress.getLocalHost(), "Eclipse Test JUnit Case" );
		
		//Lock the account due to repeated authentication attempts
		for( int c = 0; c < 5; c++){
			localPwd.authenticate("Test", validator, clientData);
		}
		
		if( !localPwd.isAccountBruteForceLocked("Test") ){
			fail("Account was not locked out");
		}
	}
	
	public void testInvalidAccountLockout() throws SQLException, NumericalOverflowException, InputValidationException, NoDatabaseConnectionException, NoSuchAlgorithmException, UnknownHostException {
		
		PasswordAuthenticationValidator validator = new PasswordAuthenticationValidator("invalidPassword");
		ClientData clientData = new ClientData( InetAddress.getLocalHost(), "Eclipse Test JUnit Case" );
		
		//Lock the account due to repeated authentication attempts
		for( int c = 0; c < 5; c++){
			localPwd.authenticate("NotARealUser", validator, clientData);
		}
		
		if( !localPwd.isAccountBruteForceLocked("NotARealUser") ){
			fail("Account was not locked out");
		}
	}

}
