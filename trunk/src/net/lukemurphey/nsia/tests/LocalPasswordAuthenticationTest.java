package net.lukemurphey.nsia.tests;

import junit.framework.TestCase;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.net.*;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.Authentication;
import net.lukemurphey.nsia.ClientData;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.LocalPasswordAuthentication;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NumericalOverflowException;
import net.lukemurphey.nsia.PasswordAuthenticationValidator;
import net.lukemurphey.nsia.TestsConfig;

public class LocalPasswordAuthenticationTest extends TestCase {
	Application appRes;
	LocalPasswordAuthentication localPwd;
	
	public LocalPasswordAuthenticationTest() throws BindException, SQLException, InputValidationException, Exception{
		appRes = TestsConfig.getApplicationResource();
		localPwd = new LocalPasswordAuthentication(appRes);
		
		try {
			appRes.connectToDatabase( TestsConfig.DB_PATH, TestsConfig.DB_PASSWORD, TestsConfig.DB_DRIVER );
			//EventLog eventlog = new EventLog( new File(TestsConfig.LOG_FILE) );
			//appRes.setEventLog( eventlog );
			
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		//} catch (FileNotFoundException e) {
		//	e.printStackTrace();
		}
		
	}
	
	public static void main(String[] args) {
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.LocalPasswordAuthentication.authenticate(String, PasswordAuthenticationValidator)'
	 */
	public void testAuthenticateStringPasswordAuthenticationValidator() throws NoSuchAlgorithmException, SQLException, InputValidationException, NoDatabaseConnectionException, NumericalOverflowException, UnknownHostException {
		PasswordAuthenticationValidator validator = new PasswordAuthenticationValidator("password");
		
		ClientData clientData = new ClientData( InetAddress.getLocalHost(), "Eclipse Test JUnit Case" );
		
		Authentication.AuthenticationResult authResult = localPwd.authenticate("Luke", validator, clientData);
		if( authResult.getAuthenticationStatus() != Authentication.AuthenticationResult.AUTH_SUCCESS )
			fail("The authentication failed for a valid user");
		
		validator = new PasswordAuthenticationValidator("invalidPassword");
		authResult = localPwd.authenticate("Luke", validator, clientData);
		if( authResult.getAuthenticationStatus() == Authentication.AuthenticationResult.AUTH_SUCCESS )
			fail("The authentication succeeded for a invalid user");
		
		
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.Authentication.incrementAuthenticationFailedCount(String, long)'
	 */
	public void testIncrementAuthenticationFailedCount() throws SQLException, NumericalOverflowException, InputValidationException, NoDatabaseConnectionException {
		//long prevCount = localPwd.getAuthenticationFailedCount("L33tHax0r",800000000L);
		
		//localPwd.incrementAuthenticationFailedCount("L33tHax0r", 800000000L);
		//assertEquals( prevCount+1, localPwd.getAuthenticationFailedCount("L33tHax0r",800000000L) );
		
		//localPwd.incrementAuthenticationFailedCount("L33tHax0r", 800000000L);
		//assertEquals( prevCount+2, localPwd.getAuthenticationFailedCount("L33tHax0r",800000000L) );
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.Authentication.getAuthenticationFailedCount(String, long)'
	 */
	public void testGetAuthenticationFailedCount() throws SQLException  {
		//assertEquals( 6, localPwd.getAuthenticationFailedCount("SomeName",800000000L) );
	}

}
