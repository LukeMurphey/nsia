package net.lukeMurphey.nsia.testCases;

import junit.framework.TestCase;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.net.*;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.Authentication;
import net.lukeMurphey.nsia.ClientData;
import net.lukeMurphey.nsia.InputValidationException;
import net.lukeMurphey.nsia.LocalPasswordAuthentication;
import net.lukeMurphey.nsia.NoDatabaseConnectionException;
import net.lukeMurphey.nsia.NumericalOverflowException;
import net.lukeMurphey.nsia.PasswordAuthenticationValidator;
import net.lukeMurphey.nsia.TestsConfig;

public class LocalPasswordAuthenticationTest extends TestCase {
	Application appRes;
	LocalPasswordAuthentication localPwd;
	
	public LocalPasswordAuthenticationTest() throws BindException, SQLException, InputValidationException, Exception{
		appRes = TestsConfig.getApplicationResource();
		localPwd = new LocalPasswordAuthentication(appRes);
		
		try {
			appRes.connectToDatabase( TestsConfig.DB_PATH, TestsConfig.DB_PASSWORD, TestsConfig.DB_DRIVER );
			//EventLog eventLog = new EventLog( new File(TestsConfig.LOG_FILE) );
			//appRes.setEventLog( eventLog );
			
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
	 * Test method for 'net.lukeMurphey.siteSentry.LocalPasswordAuthentication.authenticate(String, PasswordAuthenticationValidator)'
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
	 * Test method for 'net.lukeMurphey.siteSentry.Authentication.incrementAuthenticationFailedCount(String, long)'
	 */
	public void testIncrementAuthenticationFailedCount() throws SQLException, NumericalOverflowException, InputValidationException, NoDatabaseConnectionException {
		//long prevCount = localPwd.getAuthenticationFailedCount("L33tHax0r",800000000L);
		
		//localPwd.incrementAuthenticationFailedCount("L33tHax0r", 800000000L);
		//assertEquals( prevCount+1, localPwd.getAuthenticationFailedCount("L33tHax0r",800000000L) );
		
		//localPwd.incrementAuthenticationFailedCount("L33tHax0r", 800000000L);
		//assertEquals( prevCount+2, localPwd.getAuthenticationFailedCount("L33tHax0r",800000000L) );
	}

	/*
	 * Test method for 'net.lukeMurphey.siteSentry.Authentication.getAuthenticationFailedCount(String, long)'
	 */
	public void testGetAuthenticationFailedCount() throws SQLException  {
		//assertEquals( 6, localPwd.getAuthenticationFailedCount("SomeName",800000000L) );
	}

}
