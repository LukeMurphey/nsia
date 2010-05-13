package net.lukemurphey.nsia.scan.scriptenvironment;

import java.net.MalformedURLException;
import java.net.URL;

public class HeadMethod extends WebClient {

	public HeadMethod(String url) throws MalformedURLException {
		super(HttpMethod.HEAD, url);
	}
	
	public HeadMethod(URL url) {
		super(HttpMethod.HEAD, url);
	}

}
