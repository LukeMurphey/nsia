package net.lukemurphey.nsia.rest;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;

import net.lukemurphey.nsia.ApplicationVersionDescriptor;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ApplicationVersionInfo extends RESTEndpointClient {

	public ApplicationVersionInfo( URL endPointURL ){
		setEndpointURL(endPointURL);
	}
	
	public ApplicationVersionInfo(  ) throws RESTRequestFailedException{
		// Find the endpoint URL in the index on the server
		setEndpointURL( EndpointIndex.getEndpoint("application_version").getURL() );
	}
	
	private ApplicationVersionDescriptor parseAppData(Document doc) throws MalformedURLException, ParseException{

		Element el = doc.getDocumentElement();
		
		if( el.getNodeName().equalsIgnoreCase("Application") ){

			String version = el.getAttribute("Version");

			// Get the expiration date
			String dateString = el.getAttribute("Date");
			Date date = parseStandardDateFormat(dateString);
			
			return new ApplicationVersionDescriptor(version, date);
		}
		
		return null;

	}
	
	public static ApplicationVersionDescriptor getCurrentApplicationVersion( URL url ) throws RESTRequestFailedException{
		
		ApplicationVersionInfo appInfo = new ApplicationVersionInfo( url );
		
		return appInfo.getCurrentApplicationVersion();
	}
	
	public ApplicationVersionDescriptor getCurrentApplicationVersion( ) throws RESTRequestFailedException{
		
		// Perform the HTTP request to get the end-points
		Document doc = doGet( );
		
		try {
			return parseAppData( doc );
		} catch (MalformedURLException e) {
			throw new RESTRequestFailedException("Unable to obtain definitions version information", e);
		} catch (ParseException e) {
			throw new RESTRequestFailedException("Unable to parse definition set date", e);
		}
		
	}
	
}
