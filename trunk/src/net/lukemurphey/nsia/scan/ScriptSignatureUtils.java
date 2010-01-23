package net.lukemurphey.nsia.scan;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ScriptSignatureUtils {
	
	public static String md5(HttpResponseData httpResponse) throws NoSuchAlgorithmException{
		MessageDigest messageDigest = MessageDigest.getInstance("MD5");
		
		messageDigest.update(httpResponse.getResponseAsBytes());
		byte[] bytes = messageDigest.digest();
		
		return byteArrayToHexString( bytes );
		
	}
	
	public static String md5(String value) throws NoSuchAlgorithmException{
		MessageDigest messageDigest = MessageDigest.getInstance("MD5");
		
		try{
			messageDigest.update( value.getBytes("UTF-8") );
		}
		catch(UnsupportedEncodingException e){
			messageDigest.update( value.getBytes() );
		}
		
		byte[] bytes = messageDigest.digest();
		
		return byteArrayToHexString( bytes );
		
	}
	
	private static String byteArrayToHexString(byte in[]) {

	    byte ch = 0x00;
	    int i = 0; 

	    if (in == null || in.length <= 0)
	        return null;

	    String pseudo[] = { "0", "1", "2", "3", "4", "5",
							"6", "7", "8", "9", "A", "B",
							"C", "D", "E", "F"};

	    StringBuffer out = new StringBuffer(in.length * 2);

	    while (i < in.length) {

	        ch = (byte) (in[i] & 0xF0); // Strip offhigh nibble
	        ch = (byte) (ch >>> 4); //shift the bits down
	        ch = (byte) (ch & 0x0F);//	 must do this if high order bit is on!
	        out.append(pseudo[ (int) ch]); // convert the nibble to a String Character
	        ch = (byte) (in[i] & 0x0F); // Strip off low nibble 
	        out.append(pseudo[ (int) ch]); // convert the nibble to a String Character
	        i++;

	    }
	    
	    String result = new String(out);
	    return result;

	}

}
