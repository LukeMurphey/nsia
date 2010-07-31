package net.lukemurphey.nsia.tests;

import junit.framework.TestCase;

import java.io.IOException;
import java.sql.*;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.ApplicationParameters;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;

public class ApplicationParameterTest extends TestCase {
	
	ApplicationParameters params = null;
	Application app = null;
	
	public void setUp() throws NoDatabaseConnectionException, IOException{
		app = TestApplication.getApplication();
		params = new ApplicationParameters(app);
	}
	
	public void tearDown(){
		TestApplication.stopApplication();
	}
	
	public void testGetParameter() throws Exception {
		params.setParameter("Administration.SyslogServer", "192.168.10.5");
		String value = params.getParameter("Administration.SyslogServer", null);
		
		assertNotNull(value);
	}

	public void testSetParameter() throws Exception {
		params.setParameter("Administration.SyslogServer", "192.168.10.5");
		
		String value = params.getParameter("Administration.SyslogServer", null);
		
		if( !value.equals( "192.168.10.5") ){
			fail("Parameter value was not returned successfully");
		}
	}

	public void testDoesParameterExist() throws InputValidationException, NoDatabaseConnectionException, SQLException {
		
		params.setParameter("Administration.SyslogServer", "192.168.10.5");
		
		assertEquals(params.doesParameterExist("Administration.SyslogServer"), true);
		assertEquals(params.doesParameterExist("D03sN073x1s7"), false);
		assertEquals(params.doesParameterExist(null), false);
	}

}
