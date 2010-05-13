package net.lukemurphey.nsia.scan.scriptenvironment;

import java.net.MalformedURLException;
import java.net.URL;

public class PostMethod extends WebClient {

	public PostMethod(String url) throws MalformedURLException {
		super(HttpMethod.POST, url);
	}
	
	public PostMethod(URL url) {
		super(HttpMethod.POST, url);
	}

}
