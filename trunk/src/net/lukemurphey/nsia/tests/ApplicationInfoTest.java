package net.lukemurphey.nsia.tests;

import java.io.IOException;
import java.net.URL;

import net.lukemurphey.nsia.ApplicationVersionDescriptor;
import net.lukemurphey.nsia.rest.ApplicationVersionInfo;
import net.lukemurphey.nsia.rest.EndpointIndex;
import net.lukemurphey.nsia.rest.RESTRequestFailedException;
import net.lukemurphey.nsia.rest.EndpointIndex.Endpoint;
import junit.framework.TestCase;

public class ApplicationInfoTest extends TestCase {

	private String testURLstring = "http://threatfactor.com/rest/NSIA/";
	private URL testURL = null;
	
	public void testGetApplicationInfo() throws RESTRequestFailedException, IOException {
		String restURL = TestApplication.getProperty("value.test.resturl");
		
		if( restURL == null ){
			restURL = testURLstring;
		}
		
		testURL = new URL(restURL);
		
		Endpoint endpoint = EndpointIndex.getEndpoint( testURL, "application_version" );
		
		ApplicationVersionDescriptor versionInfo = ApplicationVersionInfo.getCurrentApplicationVersion(endpoint.getURL());
		
		if( versionInfo == null ){
			fail("Application information was not returned");
		}
	}
	
}
