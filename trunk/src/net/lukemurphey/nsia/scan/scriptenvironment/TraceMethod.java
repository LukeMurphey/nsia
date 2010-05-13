package net.lukemurphey.nsia.scan.scriptenvironment;

import java.net.MalformedURLException;
import java.net.URL;

public class TraceMethod extends WebClient {

	public TraceMethod(String url) throws MalformedURLException {
		super(HttpMethod.TRACE, url);
	}

	public TraceMethod(URL url) {
		super(HttpMethod.TRACE, url);
	}

	
}
