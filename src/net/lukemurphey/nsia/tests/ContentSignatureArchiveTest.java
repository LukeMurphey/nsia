package net.lukemurphey.nsia.tests;

import java.io.IOException;

import junit.framework.TestCase;
import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.rest.RESTRequestFailedException;
import net.lukemurphey.nsia.scan.DefinitionArchive;
import net.lukemurphey.nsia.scan.DefinitionSetLoadException;
import net.lukemurphey.nsia.scan.DefinitionUpdateFailedException;
import net.lukemurphey.nsia.scan.DefinitionSet.DefinitionVersionID;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;


public class ContentSignatureArchiveTest extends TestCase {

	public void tearDown(){
		TestApplication.stopApplication();
	}
	
	public void testGetLatestID() throws RESTRequestFailedException, IOException{
		DefinitionVersionID latestID = DefinitionArchive.getLatestAvailableDefinitionSetID();
		
		if( latestID == null ){
			fail("Latest definition ID could not be obtained");
		}
		else if(latestID.formatID() <= 0){
			fail("Latest definition format ID in invalid (" + latestID.formatID() + ")");
		}
		else if(latestID.revisionID() < 0){
			fail("Latest definition revision ID in invalid (" + latestID.revisionID() + ")");
		}
	}
	
	public void testRevisionIDParse() throws IOException{
		
		DefinitionVersionID rev = new DefinitionVersionID("1.2 beta");
		
		if( rev.toString().equalsIgnoreCase("1.2 beta") == false ){
			fail("Format of parsed version identifier is unexpected (" + rev.toString() + ")" );
		}
	}
	
	public void testRevisionDate() throws RESTRequestFailedException, IOException, ParseException{
		
		Date date = DefinitionArchive.getLatestAvailableDefinitionSetDate();
		
		if( date == null ){
			fail("Latest date could not be obtained");
		}
	}
	
	public void testLoadSignaturesFromServer() throws DefinitionUpdateFailedException, DefinitionSetLoadException, NoDatabaseConnectionException, SQLException, InputValidationException, TestApplicationException, IOException {

		String licenseKey = TestApplication.getProperty("value.test.licensekey");
        
		if( licenseKey != null ){
			Application app = TestApplication.getApplication();
			
			//Set the license key so that the definitions can be loaded
			app.getApplicationConfiguration().setLicenseKey(licenseKey);
			
			DefinitionArchive archive = DefinitionArchive.getArchive(true);
			
			DefinitionVersionID rev  = archive.updateDefinitions();
			
			if( rev == null ){
				fail("Current definition set could not be obtained");
			}
		}
	}
	
}
