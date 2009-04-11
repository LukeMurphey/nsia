package net.lukeMurphey.nsia.testCases;

import java.io.IOException;

import org.apache.xmlrpc.XmlRpcException;

import junit.framework.TestCase;
import net.lukeMurphey.nsia.InputValidationException;
import net.lukeMurphey.nsia.NoDatabaseConnectionException;
import net.lukeMurphey.nsia.scanRules.DefinitionArchive;
import net.lukeMurphey.nsia.scanRules.DefinitionSet.DefinitionVersionID;
import net.lukeMurphey.nsia.scanRules.DefinitionSetLoadException;
import net.lukeMurphey.nsia.scanRules.DefinitionUpdateFailedException;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;


public class ContentSignatureArchiveTest extends TestCase {

	public void testGetLatestID() throws XmlRpcException, IOException{
		DefinitionVersionID latestID = DefinitionArchive.getLatestAvailableDefinitionSetID();
		
		System.out.println( "Latest available ID is " + latestID.toString() );
	}
	
	public void testRevisionIDParse() throws XmlRpcException, IOException{
		
		DefinitionVersionID rev = new DefinitionVersionID("1.2 beta");
		
		if( rev.toString().equalsIgnoreCase("1.2 beta") == false ){
			fail("Format of parsed version identifier is unexpected (" + rev.toString() + ")" );
		}
	}
	
	public void testRevisionDate() throws XmlRpcException, IOException, ParseException{
		
		Date date = DefinitionArchive.getLatestAvailableDefinitionSetDate();
		
		System.out.println( "Latest available definitions are dated " + date.toString() );
	}
	
	public void testLoadSignaturesFromServer() throws DefinitionUpdateFailedException, DefinitionSetLoadException, SQLException, NoDatabaseConnectionException, InputValidationException {
		
		DefinitionArchive archive = DefinitionArchive.getArchive(true);
		
		DefinitionVersionID rev  = archive.updateDefinitions();
		
		System.out.println( "Definitions loaded from server with version identifier of " + rev.toString() );
	}
	
}
