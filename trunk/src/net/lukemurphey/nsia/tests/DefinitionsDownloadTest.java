package net.lukemurphey.nsia.tests;

import java.io.IOException;
import java.net.URL;

import net.lukemurphey.nsia.rest.DefinitionsDownload;
import net.lukemurphey.nsia.rest.EndpointIndex;
import net.lukemurphey.nsia.rest.RESTRequestFailedException;
import net.lukemurphey.nsia.rest.EndpointIndex.Endpoint;
import junit.framework.TestCase;

public class DefinitionsDownloadTest extends TestCase {

	private String testURLstring = "http://threatfactor.com/rest/NSIA/";
	private URL testURL = null;
	
	public void testGetDefinitionsAsString() throws RESTRequestFailedException, IOException {
		String restURL = TestApplication.getProperty("value.test.resturl");
		
		if( restURL == null ){
			restURL = testURLstring;
		}
		
		testURL = new URL(restURL);
		
		String licenseKey = TestApplication.getProperty("value.test.licensekey");
	    
		if( licenseKey != null ){
		
			Endpoint endpoint = EndpointIndex.getEndpoint( testURL, "definitions_download" );
			
			String definitionsXML = DefinitionsDownload.getDefinitionsAsString(endpoint.getURL(), licenseKey, "TEST");
			
			if( definitionsXML == null ){
				fail("Definition set XML was not returned");
			}
		}
	}
	
}
