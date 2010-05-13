package net.lukemurphey.nsia.scan.scriptenvironment;

import java.net.MalformedURLException;
import java.net.URL;

public class OptionsMethod extends WebClient {

	public OptionsMethod(String url) throws MalformedURLException {
		super(HttpMethod.OPTIONS, url);
	}
	
	public OptionsMethod(URL url) {
		super(HttpMethod.OPTIONS, url);
	}

}
