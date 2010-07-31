package net.lukemurphey.nsia.tests;

import java.sql.SQLException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.SiteGroupScanResult;
import net.lukemurphey.nsia.scan.ScanData;
import net.lukemurphey.nsia.scan.ScanRule.ScanResultLoadFailureException;

import junit.framework.TestCase;

public class ScanDataTest extends TestCase {

	Application app = null;
	ScanData scanData; 
	
	public void setUp() throws TestApplicationException{
		app = TestApplication.getApplication();
		scanData = new ScanData( app );
	}
	
	public void tearDown(){
		TestApplication.stopApplication();
	}
	
	public void testGetSiteGroupStatus() throws SQLException, InputValidationException, NoDatabaseConnectionException, NotFoundException, ScanResultLoadFailureException{
		SiteGroupScanResult result = scanData.getSiteGroupStatus( 1 );
		
		if( result == null ){
			fail("Result was null, Site Group status could not be retrieved");
		}
	}

}
