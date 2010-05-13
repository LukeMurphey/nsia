package net.lukemurphey.nsia.scan.scriptenvironment;

import java.net.MalformedURLException;
import java.net.URL;

public class DeleteMethod extends WebClient {

	public DeleteMethod(String url) throws MalformedURLException {
		super(HttpMethod.DELETE, url);
	}
	
	public DeleteMethod(URL url) {
		super(HttpMethod.DELETE, url);
	}

}
