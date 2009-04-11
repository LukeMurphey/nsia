package net.lukeMurphey.nsia.testCases;

import java.net.*;

import net.lukeMurphey.nsia.EmailAddress;
import net.lukeMurphey.nsia.InvalidLocalPartException;
import junit.framework.TestCase;

public class EmailAddressTest extends TestCase {

	public static void main(String[] args) {
	}

	/*
	 * Test method for 'net.lukeMurphey.siteSentry.EmailAddress.toString()'
	 */
	public void testToString() throws UnknownHostException, InvalidLocalPartException {
		String testResult;
		
		//Test constructor one
		EmailAddress emailAddress = EmailAddress.getByAddress("Somebody@Somesite.com");
		testResult = emailAddress.toString();
		
		if( !testResult.matches("Somebody@Somesite.com") )
			fail("Email address failed: " + emailAddress.toString() );
		
		//Test constructor two
		emailAddress = EmailAddress.getByAddress("Somebody", InetAddress.getByName("Somesite.com"));
		testResult = emailAddress.toString();
		
		if( !testResult.matches("Somebody@Somesite.com") )
			fail("Email address failed: " + emailAddress.toString() );

	}

}
