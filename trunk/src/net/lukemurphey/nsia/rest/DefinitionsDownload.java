package net.lukemurphey.nsia.rest;

import java.net.URL;

/**
 * This endpoint client gets the current definition set from the server.
 * @author Luke
 *
 */
public class DefinitionsDownload extends RESTEndpointClient {

	public DefinitionsDownload( String id, String password ) throws RESTRequestFailedException{
		// Find the endpoint URL in the index on the server
		setEndpointURL( EndpointIndex.getEndpoint("definitions_download").getURL() );
		this.id = id;
		this.password = password;
	}
	
	public DefinitionsDownload( URL endPointURL, String id, String password ){
		setEndpointURL(endPointURL);
		this.id = id;
		this.password = password;
	}
	
	public String getDefinitionsAsString() throws RESTRequestFailedException{
		return doGetToString();
	}
	
	public static String getDefinitionsAsString( String password, String id ) throws RESTRequestFailedException{
		DefinitionsDownload download = new DefinitionsDownload(id, password);
		return download.getDefinitionsAsString();
	}
	
	public static String getDefinitionsAsString( URL url, String password, String id ) throws RESTRequestFailedException{
		DefinitionsDownload download = new DefinitionsDownload(url, id, password);
		return download.getDefinitionsAsString();
	}
	
}
