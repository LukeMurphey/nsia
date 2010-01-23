package net.lukemurphey.nsia;

import java.io.*;
import java.net.MalformedURLException;

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
	
	public enum Type{
		DEMO, STANDARD, PARTNER, NON_PROFIT, BETA_TEST, UNDEFINED;
	}
	
	public enum LicenseStatus{
		EXPIRED("Expired"), DISABLED("Disabled"), ILLEGAL("Illegal Key"), UNLICENSED("Unlicensed"), DEMO("Demo License"), ACTIVE("Active"), UNVALIDATED("Unvalidated");
		
		private String description;
		private LicenseStatus(String description){
			this.description = description;
		}
		
		public String getDescription(){
			return description;
		}
	}
	
	private static final Pattern LICENSE_KEY_REGEX = Pattern.compile("[a-zA-Z0-9]{4,4}(-?[a-zA-Z0-9]{4,4}){4,4}");
	public static final String LICENSE_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	
	public static final String LICENSE_SUPPORT_API_URL = "https://threatfactor.com/xmlrpc/";
	
	public static class LicenseDescriptor{
		private String licensee;
		private String licenseKey;
		private Date expires;
		private Type licenseType;
		private LicenseStatus status;
		
		private LicenseDescriptor( ){
			
		}
		
		protected LicenseDescriptor( LicenseStatus status ){
			this.expires = null;
			this.licensee = null;
			this.licenseKey = null;
			this.licenseType = null;
			this.status = status;
		}
		
		protected LicenseDescriptor( String licensee, String licenseKey, Date expires, Type licenseType, LicenseStatus status){
			this.expires = expires;
			this.licensee = licensee;
			this.licenseKey = licenseKey;
			this.licenseType = licenseType;
			this.status = status;
		}
		
		public static LicenseDescriptor uncheckedLicense( String licenseKey ){
			LicenseDescriptor desc = new LicenseDescriptor();
			desc.expires = null;
			desc.licensee = null;
			desc.licenseKey = licenseKey;
			desc.licenseType = null;
			desc.status = LicenseStatus.UNVALIDATED;
			
			return desc;
		}
		
		public String getKey(){
			return licenseKey;
		}
		
		public Date getExpirationDate(){
			if( expires != null ){
				return (Date)expires.clone();
			}
			else{
				return null;
			}
		}
		
		public Type getType(){
			return licenseType;
		}
		
		public LicenseStatus getStatus(){
			return status;
		}
		
		public String getLicensee(){
			return licensee;
		}
		
		public boolean isValid(){
			return LicenseManagement.validate(this);
		}
	}
	
	//private Application application = null;
	
	private LicenseManagement(){
		
	}
	
	public void loadLicenseKey(File file){
		/*
		// 1 -- Get the public key
		File keyFile = new File("Public.der");        
		byte[] encodedKey = new byte[(int)keyFile.length()];

		new FileInputStream(keyFile).read(encodedKey);

		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedKey);

		// 2 -- Initialize the public key and associated cipher
		KeyFactory kf = KeyFactory.getInstance("RSA");
		PublicKey pk = kf.generatePublic(publicKeySpec);

		Cipher rsa = Cipher.getInstance("RSA");

		rsa.init(Cipher.DECRYPT_MODE, pk);
		OutputStream os = new CipherOutputStream(
		        new FileOutputStream("encrypted.rsa"), rsa);

		Writer out = new OutputStreamWriter(os);
		out.write("Hello World!!");
		out.close();
		os.close();
		*/
	}
	
	public static boolean validate( LicenseDescriptor licenseDescriptor ){
		
		// 1 -- Stop if the license key was not returned
		if( licenseDescriptor == null ){
			return false;
		}
		
		// 2 -- Determine if the status is active
		if( licenseDescriptor.getStatus() != LicenseStatus.ACTIVE ){
			return false;
		}
		
		// 3 -- Determine if the license has expired
		if( licenseDescriptor.getExpirationDate().before( new Date() )){
			licenseDescriptor.status = LicenseStatus.EXPIRED;
			return false;
		}
		
		return true;
	}
	
	public static boolean validateLicenseKey( String key ) throws LicenseValidationException, InputValidationException{
		
		LicenseDescriptor licenseKey = getKeyInfo( key );
		
		return validate( licenseKey );
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
	        	validate(licenseDescriptor);
	        	
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

