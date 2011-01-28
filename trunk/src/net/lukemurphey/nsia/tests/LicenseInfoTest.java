package net.lukemurphey.nsia.tests;

import java.io.IOException;
import java.net.URL;

import junit.framework.TestCase;

import net.lukemurphey.nsia.LicenseDescriptor;
import net.lukemurphey.nsia.rest.EndpointIndex;
import net.lukemurphey.nsia.rest.LicenseInfo;
import net.lukemurphey.nsia.rest.RESTRequestFailedException;
import net.lukemurphey.nsia.rest.EndpointIndex.Endpoint;

public class LicenseInfoTest extends TestCase {

	private String testURLstring = "http://threatfactor.com/rest/NSIA/";
	private URL testURL = null;
	
	public void testGetLicense() throws RESTRequestFailedException, IOException {
		
		String restURL = TestApplication.getProperty("value.test.resturl");
		
		if( restURL == null ){
			restURL = testURLstring;
		}
		
		String licenseKey = TestApplication.getProperty("value.test.licensekey");
	    
		if( licenseKey != null ){
			//Application.getApplication().getApplicationConfiguration().getUniqueInstallationID();
			testURL = new URL(restURL);
			Endpoint endpoint = EndpointIndex.getEndpoint( testURL, "license" );
			
			LicenseDescriptor descriptor = LicenseInfo.getLicenseInformation(endpoint.getURL(), licenseKey, "TEST");
			
			if( descriptor == null ){
				fail("A license descriptor was not returned");
			}
		}
			
	}
	

	
}
