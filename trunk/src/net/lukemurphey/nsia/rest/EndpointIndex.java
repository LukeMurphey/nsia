package net.lukemurphey.nsia.rest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Get a list of the available endpoints.
 * @author Luke Murphey
 *
 */
public class EndpointIndex extends RESTRequest {

	// This is the default REST endpoint that lists all of the available endpoints
	public static final String DEFAULT_ENDPOINT = "http://threatfactor.com/rest/NSIA/";
	
	/**
	 * Represents the available REST endpoints
	 * @author Luke Murphey
	 *
	 */
	public static class Endpoint{
		
		private URL url;
		private String name;
		private boolean requiresLicense = false;
		
		public Endpoint( URL url, String name ){
			this.name = name;
			this.url = url;
		}
		
		public Endpoint( URL url, String name, boolean requiresLicense ){
			this.name = name;
			this.url = url;
			this.requiresLicense = requiresLicense;
		}
		
		public String getName(){
			return name;
		}
		
		public URL getURL(){
			return url;
		}
		
		public boolean requiresLicense(){
			return requiresLicense;
		}
	}
	
	public EndpointIndex(){
		try {
			url = new URL(DEFAULT_ENDPOINT);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("The default endpoint URL is malformed");
		}
	}
	
	public EndpointIndex( URL endPointURL ){
		setEndpointURL(endPointURL);
	}
	
	/**
	 * Get the list of endpoints from the REST endpoint at the given URL.
	 * @param url
	 * @return
	 * @throws RESTRequestFailedException
	 * @throws MalformedURLException
	 */
	public Endpoint[] getEndpoints( ) throws RESTRequestFailedException{
		
		// Perform the HTTP request to get the end-points
		Document doc = doGet( url );
		
		// Parse out the methods
		Vector<Endpoint> endpoints;
		try {
			endpoints = parseMethods( doc );
		
			Endpoint[] endpoints_array = new Endpoint[endpoints.size()];
			endpoints.toArray(endpoints_array);
			
			return endpoints_array;
		} catch (MalformedURLException e) {
			throw new RESTRequestFailedException("Unable to parse URL form list of endpoints", e);
		}
	}
	
	/**
	 * Get the list of endpoints from the given response
	 * @param doc
	 * @return
	 * @throws MalformedURLException
	 */
	private Vector<Endpoint> parseMethods(Document doc) throws MalformedURLException{

		Vector<Endpoint> endpoints = new Vector<Endpoint>();

		Element root = doc.getDocumentElement();

		NodeList nl = root.getElementsByTagName("Endpoint");

		if(nl != null && nl.getLength() > 0) {
			for(int i = 0 ; i < nl.getLength();i++) {

				// Get the method element
				Element el = (Element)nl.item(i);

				String href = el.getAttribute("URL");

				// Get the method name
				String methodName = el.getAttribute("Name");
				
				// Determine if you must have a license to access the endpoint
				String requiresLicenseString = el.getAttribute("RequiresLicense");
				boolean requiresLicense = false;
				
				if( requiresLicenseString == null || requiresLicenseString.length() == 0 ){
					requiresLicense = false;
				}
				else if ( requiresLicenseString.equalsIgnoreCase("True") ){
					requiresLicense = true;
				}

				// Add it to list
				endpoints.add( new Endpoint( new URL( url, href ), methodName, requiresLicense ) );
			}
		}

		return endpoints;

	}
	
	/**
	 * 
	 * @return
	 * @throws RESTRequestFailedException
	 */
	public static Endpoint[] getEndpoints( URL url ) throws RESTRequestFailedException{
		EndpointIndex getEndpoints = new EndpointIndex( url );
		return getEndpoints.getEndpoints( );
	}
	
	/**
	 * Get the endpoint with the given name.
	 * @param url
	 * @param name
	 * @return
	 * @throws RESTRequestFailedException
	 */
	public static Endpoint getEndpoint( URL url, String name ) throws RESTRequestFailedException{
		EndpointIndex getEndpoints = new EndpointIndex( url );
		Endpoint[] endpoints = getEndpoints.getEndpoints( );
		
		for (Endpoint endpoint : endpoints) {
			if( endpoint.getName().equalsIgnoreCase( name )){
				return endpoint;
			}
		}
		
		return null;
	}
	
	/**
	 * Get the endpoint with the given name.
	 * @param name
	 * @return
	 * @throws RESTRequestFailedException
	 */
	public static Endpoint getEndpoint( String name ) throws RESTRequestFailedException{
		try {
			return getEndpoint( new URL( DEFAULT_ENDPOINT ), name );
		} catch (MalformedURLException e) {
			throw new RESTRequestFailedException("Default endpoint is not a valid URL");
		}
	}
	
}
