package net.lukemurphey.nsia.rest;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.lukemurphey.nsia.scan.DefinitionSet;
import net.lukemurphey.nsia.scan.DefinitionSet.DefinitionVersionID;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DefinitionsInfo extends RESTRequest {

	public DefinitionsInfo( URL endPointURL ){
		setEndpointURL(endPointURL);
	}
	
	public DefinitionsInfo(  ) throws RESTRequestFailedException{
		// Find the endpoint URL in the index on the server
		setEndpointURL( EndpointIndex.getEndpoint("definitions_version").getURL() );
	}
	
	private DefinitionVersionID parseDefinitionsData(Document doc) throws MalformedURLException, ParseException{

		Element el = doc.getDocumentElement();
		
		if( el.getNodeName().equalsIgnoreCase("Definitions") ){

			String version = el.getAttribute("Version");

			// Get the expiration date
			String dateString = el.getAttribute("Date");
			//Date date = parseStandardDateFormat(dateString);
			
			SimpleDateFormat dateFormat = new SimpleDateFormat(DefinitionSet.DEFINITION_SET_DATE_FORMAT);
			Date date = dateFormat.parse(dateString);
			
			return new DefinitionVersionID(version, date);
		}
		
		return null;

	}
	
	public static DefinitionVersionID getCurrentDefinitionsVersion( URL url ) throws RESTRequestFailedException{
		
		DefinitionsInfo definitionsInfo = new DefinitionsInfo( url );
		
		return definitionsInfo.getCurrentDefinitionsVersion();
	}
	
	public DefinitionVersionID getCurrentDefinitionsVersion( ) throws RESTRequestFailedException{
		
		// Perform the HTTP request to get the end-points
		Document doc = doGet( url );
		
		try {
			return parseDefinitionsData( doc );
		} catch (MalformedURLException e) {
			throw new RESTRequestFailedException("Unable to obtain definitions version information", e);
		} catch (ParseException e) {
			throw new RESTRequestFailedException("Unable to parse definition set date", e);
		}
		
	}
	
}
