package net.lukemurphey.nsia.scan.scriptenvironment;

import java.net.MalformedURLException;
import java.net.URL;

public class PutMethod extends WebClient {

	public PutMethod(String url) throws MalformedURLException {
		super(HttpMethod.PUT, url);
	}
	
	public PutMethod(URL url) {
		super(HttpMethod.PUT, url);
	}

}
