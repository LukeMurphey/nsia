package net.lukemurphey.nsia.rest;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;

import net.lukemurphey.nsia.LicenseDescriptor;
import net.lukemurphey.nsia.LicenseDescriptor.LicenseStatus;
import net.lukemurphey.nsia.LicenseDescriptor.Type;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class accesses the REST endpoint to get information about a given license key.
 * @author Luke Murphey
 *
 */
public class LicenseInfo extends RESTEndpointClient {
	
	public LicenseInfo( String id, String password ) throws RESTRequestFailedException{
		// Find the endpoint URL in the index on the server
		setEndpointURL( EndpointIndex.getEndpoint("license").getURL() );
		this.id = id;
		this.password = password;
	}
	
	public LicenseInfo( URL endPointURL, String id, String password ){
		setEndpointURL(endPointURL);
		this.id = id;
		this.password = password;
	}
	
	private LicenseDescriptor parseLicenseData(Document doc) throws MalformedURLException, ParseException{

		Element el = doc.getDocumentElement();

		if( el.getNodeName().equalsIgnoreCase("License") ){

			String licensee = el.getAttribute("Licensee");

			// Get the expiration date
			String expirationDateString = el.getAttribute("ExpirationDate");
			Date expirationDate = parseStandardDateFormat(expirationDateString);
			
			// Get the status
			String status_string = el.getAttribute("Status");
			LicenseStatus status = LicenseStatus.valueOf(status_string);
			
			// Get the status
			String key = el.getAttribute("Key");
			
			// Get the type
			String type_string = el.getAttribute("Type");
			Type type = Type.valueOf(type_string);
			
			// Determine if the license has expired (according to the server)
			String licenseExpiredString = el.getAttribute("LicenseExpired");
			boolean isExpired = false;
			
			if( licenseExpiredString == null || licenseExpiredString.length() == 0 ){
				isExpired = false;
			}
			else if ( licenseExpiredString.equalsIgnoreCase("True") ){
				isExpired = true;
			}
			
			// The server says that the license has expired so force the status to expired
			if( isExpired && status == LicenseStatus.ACTIVE ){
				status = LicenseStatus.EXPIRED;
			}

			return new LicenseDescriptor(licensee, key, expirationDate, type, status);
		}
		
		return null;

	}
	
	public static LicenseDescriptor getLicenseInformation( URL url, String licenseKey, String installationID ) throws RESTRequestFailedException{
		
		LicenseInfo licenseInfo = new LicenseInfo( url, installationID, licenseKey );
		
		return licenseInfo.getLicenseInformation();
	}
	
	public LicenseDescriptor getLicenseInformation( ) throws RESTRequestFailedException{
		
		// Perform the HTTP request to get the end-points
		Document doc = doGet( );
		
		try {
			return parseLicenseData( doc );
		} catch (MalformedURLException e) {
			throw new RESTRequestFailedException("Unable to obtain license information", e);
		} catch (ParseException e) {
			throw new RESTRequestFailedException("Unable to parse expiration date", e);
		}
		
	}
	
}
