package net.lukeMurphey.nsia.testCases;

import java.net.BindException;
import java.sql.SQLException;

import net.lukeMurphey.nsia.InputValidationException;
import net.lukeMurphey.nsia.NoDatabaseConnectionException;
import net.lukeMurphey.nsia.NotFoundException;
import net.lukeMurphey.nsia.SiteGroupScanResult;
import net.lukeMurphey.nsia.TestsConfig;
import net.lukeMurphey.nsia.scanRules.ScanData;
import net.lukeMurphey.nsia.scanRules.ScanRule.ScanResultLoadFailureException;

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
