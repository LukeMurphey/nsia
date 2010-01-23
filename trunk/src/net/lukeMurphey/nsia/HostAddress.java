package net.lukemurphey.nsia;

import java.io.Serializable;
import java.util.regex.*;

public class HostAddress implements Serializable{

	private static final long serialVersionUID = -5963080860620191754L;
	private static final String VALID_HOSTNAME_REGEX = "[-0-9_.A-Za-z]+";// 
	//private static final String VALID_NBTNAME_REGEX = "[^\\./:*?\"<>|][^\\/:*?\"<>|]{0,14}";//See http ://support.microsoft.com/kb/909264
	private static final String VALID_IP_ADDRESS = "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
	//private static final String VALID_IP_ADDRESS_DECIMAL = "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
	
	private int type = -1;
	private String value = null;
	
	public static final int IP_ADDRESS = 0;
	//public static final int NETBIOS_NAME = 1;
	public static final int DOMAIN_NAME = 2;
	
	public HostAddress( String name ){
		
		// 1 -- Determine if the name is an IP address
		Pattern ipAddressRegex = Pattern.compile( VALID_IP_ADDRESS );
		Matcher matcher = ipAddressRegex.matcher( name );
		
		if( matcher.matches() ){
			type = IP_ADDRESS;
			value = name;
		}
		
		// 2 -- Determine if the name is a domain name
		Pattern domainNameRegex = Pattern.compile( VALID_HOSTNAME_REGEX );
		matcher = domainNameRegex.matcher( name );
		
		if( matcher.matches() ){
			type = DOMAIN_NAME;
			value = name;
		}
		
		// 3 -- Throw an exception if the input is not valid
		if( type == -1 ){
			throw new IllegalArgumentException("The name given is not a valid IP address or hostname");
		}
		
	}
	
	/**
	 * Retrieves the field that indicates the type of address represented by the class.
	 * @return
	 */
	public int addressType(){
		return type;
	}
	
	public String toString(){
		return value;
	}
}
