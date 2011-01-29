package net.lukemurphey.nsia.tests;

import java.io.IOException;
import java.net.URL;

import net.lukemurphey.nsia.rest.DefinitionsInfo;
import net.lukemurphey.nsia.rest.EndpointIndex;
import net.lukemurphey.nsia.rest.RESTRequestFailedException;
import net.lukemurphey.nsia.rest.EndpointIndex.Endpoint;
import net.lukemurphey.nsia.scan.DefinitionSet.DefinitionVersionID;
import junit.framework.TestCase;

public class DefinitionsVersionInfoTest extends TestCase {
	
	private String testURLstring = "http://threatfactor.com/rest/NSIA/";
	private URL testURL = null;
	
	public void testGetDefinitionVersionInfo() throws RESTRequestFailedException, IOException {
		String restURL = TestApplication.getProperty("value.test.resturl");
		
		if( restURL == null ){
			restURL = testURLstring;
		}
		
		testURL = new URL(restURL);
		
		Endpoint endpoint = EndpointIndex.getEndpoint( testURL, "definitions_version" );
		
		DefinitionVersionID version = DefinitionsInfo.getCurrentDefinitionsVersion(endpoint.getURL());
		
		if( version == null ){
			fail("Definition version information was not returned");
		}
	}
}
