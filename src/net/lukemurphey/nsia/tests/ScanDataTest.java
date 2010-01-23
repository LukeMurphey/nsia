package net.lukemurphey.nsia.tests;

import java.net.BindException;
import java.sql.SQLException;

import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.SiteGroupScanResult;
import net.lukemurphey.nsia.TestsConfig;
import net.lukemurphey.nsia.scan.ScanData;
import net.lukemurphey.nsia.scan.ScanRule.ScanResultLoadFailureException;

import junit.framework.TestCase;

public class ScanDataTest extends TestCase {

	ScanData scanData; 
	public static void main(String[] args) {
	}

	public ScanDataTest(String name) throws BindException, SQLException, InputValidationException, Exception {
		super(name);
		scanData = new ScanData( TestsConfig.getApplicationResource() );
	}
	
	public void testGetSiteGroupStatus() throws SQLException, InputValidationException, NoDatabaseConnectionException, NotFoundException, ScanResultLoadFailureException{
		SiteGroupScanResult result = scanData.getSiteGroupStatus( 1 );
		
		if( result == null )
			fail("Result was null, Site Group status could not be retrieved");
			
	}

}
