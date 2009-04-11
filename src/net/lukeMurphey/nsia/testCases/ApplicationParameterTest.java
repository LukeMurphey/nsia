package net.lukeMurphey.nsia.testCases;

import junit.framework.TestCase;
import java.sql.*;

import net.lukeMurphey.nsia.ApplicationParameters;
import net.lukeMurphey.nsia.InputValidationException;
import net.lukeMurphey.nsia.NoDatabaseConnectionException;

public class ApplicationParameterTest extends TestCase {
	public ApplicationParameters params = null;
	
	public static void main(String[] args) {
		ApplicationParameterTest test = new ApplicationParameterTest();
		
		try {
			test.testSetParameter();
			test.testGetParameter();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * Get a connection to the database
	 */
	public ApplicationParameterTest(){
		
		// 1 -- Create the parameter class
		params = new ApplicationParameters();
	}
	
	/*
	 * Test method for 'net.lukeMurphey.siteSentry.ApplicationParameter.getParameter(String, String)'
	 */
	public void testGetParameter() throws Exception {
		String value = params.getParameter("Administration.SyslogServer", null);
		System.out.println("Administration.SyslogServer = " + value);
		assertNotNull(value);
	}

	/*
	 * Test method for 'net.lukeMurphey.siteSentry.ApplicationParameter.setParameter(String, String)'
	 */
	public void testSetParameter() throws Exception {
		params.setParameter("Administration.SyslogServer", "192.168.10.5");
		params.setParameter("Administration.SyslogEnabled", "true");
	}

	/*
	 * Test method for 'net.lukeMurphey.siteSentry.ApplicationParameter.doesParameterExist(String)'
	 */
	public void testDoesParameterExist() throws InputValidationException, NoDatabaseConnectionException, SQLException {
		assertEquals(params.doesParameterExist("Administration.SyslogServer"), true);
		assertEquals(params.doesParameterExist("D03sN073x1s7"), false);
		assertEquals(params.doesParameterExist(null), false);
	}

}
