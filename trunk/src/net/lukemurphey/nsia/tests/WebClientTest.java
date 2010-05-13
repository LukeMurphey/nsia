package net.lukemurphey.nsia.tests;

import java.io.IOException;

import org.apache.commons.httpclient.HttpException;

import net.lukemurphey.nsia.scan.scriptenvironment.GetMethod;
import net.lukemurphey.nsia.scan.scriptenvironment.WebClient.HttpResult;
import junit.framework.TestCase;

public class WebClientTest extends TestCase {

	public void testRun() throws HttpException, IOException {
		GetMethod get = new GetMethod("http://google.com");
		
		HttpResult result = get.run();
		
		String body = result.getResponseBodyAsString();
		
		if( body == null || body.length() == 0 ){
			fail("Web-client was unable to contact google.com");
		}
		
	}

}
