package net.lukemurphey.nsia.tests;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import net.lukemurphey.nsia.rest.EndpointIndex;
import net.lukemurphey.nsia.rest.RESTRequestFailedException;
import net.lukemurphey.nsia.rest.EndpointIndex.Endpoint;

import junit.framework.TestCase;

public class GetEndpointsTest extends TestCase {

	private String testURLstring = "http://127.0.0.1:8081/rest/NSIA/";
	private URL testURL = null;
	
	public void testGetEndpoints() throws UnknownHostException, MalformedURLException, RESTRequestFailedException {
		//Application.getApplication().getApplicationConfiguration().getUniqueInstallationID();
		testURL = new URL(testURLstring);
		Endpoint[] endpoints = EndpointIndex.getEndpoints( testURL );
		if( endpoints.length < 4){
			fail("Less than 4 endpoints were returned (" + endpoints.length + ")");
		}
	}
	
}
