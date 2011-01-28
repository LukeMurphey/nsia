package net.lukemurphey.nsia;

import java.io.*;
import java.net.MalformedURLException;

import net.lukemurphey.nsia.LicenseDescriptor.LicenseStatus;
import net.lukemurphey.nsia.LicenseDescriptor.Type;

import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcException;

/*import javax.crypto.*;
import java.security.spec.*;
import java.security.*;*/
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;
import java.util.regex.Pattern;

public class LicenseManagement {
	
	private static final Pattern LICENSE_KEY_REGEX = Pattern.compile("[a-zA-Z0-9]{4,4}(-?[a-zA-Z0-9]{4,4}){4,4}");
	public static final String LICENSE_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	
	public static final String LICENSE_SUPPORT_API_URL = "https://threatfactor.com/xmlrpc/";
	
	//private Application application = null;
	
	private LicenseManagement(){
		
	}
	
	public static boolean validateLicenseKey( String key ) throws LicenseValidationException, InputValidationException{
		
		LicenseDescriptor licenseKey = getKeyInfo( key );
		
		return licenseKey.isValid();
	}
	
	public static LicenseDescriptor getKeyInfo( String key ) throws LicenseValidationException, InputValidationException{
		
		// 1 -- Return a unlicensed key descriptor if the key is null
		if( key == null ){
			return new LicenseDescriptor(LicenseStatus.UNLICENSED);
		}
		
		// 2 -- Make sure the license key is legal
		if( LICENSE_KEY_REGEX.matcher(key).matches() == false ){
			throw new InputValidationException("The license key is not a valid key", "LicenseKey", key);
		}
		
		// 3 -- Get the license information from the server
		try{
			XmlRpcClient client = new XmlRpcClient( LICENSE_SUPPORT_API_URL );
			
			Vector<String> params = new Vector<String>();
			params.add(key);
			
			Object result = client.execute( "License.getDescriptor", params );
	
			if ( result != null && result instanceof XmlRpcException ){
				throw (XmlRpcException)result;
			}
			
	        if ( result != null && result instanceof Hashtable<?, ?> ){
	        	@SuppressWarnings("unchecked")
	        	Hashtable<String, Object> hashTableResult = (Hashtable<String, Object>)result;
	        	String licensee = (String)hashTableResult.get("Licensee");
	        	String expiresStr = (String)hashTableResult.get("ExpirationDate");
	        	String statusStr = (String)hashTableResult.get("Status");
	        	String typeStr = (String)hashTableResult.get("Type");
	        	
	        	Type type;
	        	
	        	if( typeStr == null){
	        		type = Type.UNDEFINED;
	        	}
	        	else{
	        		type = Type.valueOf(typeStr);
	        	}
	        	LicenseStatus status = LicenseStatus.valueOf(statusStr);
	        	
	        	SimpleDateFormat dateFormat = new SimpleDateFormat(LICENSE_DATE_FORMAT);
	        	Date expires = null;
	        	
	        	if( expiresStr != null ){
	        		expires = dateFormat.parse(expiresStr);
	        	}
	        	
	        	LicenseDescriptor licenseDescriptor = new LicenseDescriptor(licensee, key, expires, type, status);
	        	
	            return licenseDescriptor;
	        }
	        else{
	        	return null;
	        }
		}
		catch( MalformedURLException e ){
			throw new LicenseValidationException("The License API key is invalid", e);
		} catch (XmlRpcException e) {
			throw new LicenseValidationException("Could not validate the license key with the parent server", e);
		} catch (IOException e) {
			throw new LicenseValidationException("Could not validate the license key with the parent server", e);
		} catch (ParseException e) {
			throw new LicenseValidationException("The license key could not be validated because the expiration date is invalid", e);
		}
        
	}

}

