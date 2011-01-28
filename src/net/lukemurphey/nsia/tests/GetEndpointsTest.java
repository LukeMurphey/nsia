package net.lukemurphey.nsia.tests;

import java.io.IOException;
import java.net.URL;

import net.lukemurphey.nsia.rest.EndpointIndex;
import net.lukemurphey.nsia.rest.RESTRequestFailedException;
import net.lukemurphey.nsia.rest.EndpointIndex.Endpoint;

import junit.framework.TestCase;

public class GetEndpointsTest extends TestCase {

	private String testURLstring = "http://threatfactor.com/rest/NSIA/";
	private URL testURL = null;
	
	public void testGetEndpoints() throws RESTRequestFailedException, IOException {
		
		String restURL = TestApplication.getProperty("value.test.resturl");
		
		if( restURL == null ){
			restURL = testURLstring;
		}
		
		testURL = new URL(restURL);
		Endpoint[] endpoints = EndpointIndex.getEndpoints( testURL );
		
		if( endpoints.length < 4){
			fail("Less than 4 endpoints were returned (" + endpoints.length + ")");
		}
	}
	
}
