package net.lukemurphey.nsia;

import java.io.Serializable;
import java.net.*;
import java.util.regex.*;

/**
 * This class represents an email address and is conceptually similar to InetAddress. The email addresses
 * are allowed per the specifications in RFC2822. 
 * @author luke
 *
 */
public class EmailAddress implements Serializable{
	
	private static final long serialVersionUID = 9125919743291939606L;
	private String mailUser;
	private HostAddress mailHost;
	private static String EMAIL_LOCAL_PART_REGEX = "^[_a-zA-Z0-9-*+]+(\\.[_a-zA-Z0-9-*+]+)*";
	
	/**
	 * Create an email address object. The host name and local-part must be valid (non-null, non-empty and contains valid
	 * characters per RFC2822).
	 * @precondition The host name and local-part must be valid (non-null, non-empty and contains valid characters per RFC2822).
	 * @postcondition An emailAddress object corresponding to the given arguments will be returned.
	 * @param mailUser
	 * @param mailHost
	 * @throws UnknownHostException
	 */
	private EmailAddress( String mailUser, InetAddress mailHost ) throws UnknownHostException{
		
		if( mailHost == null )
			throw new UnknownHostException("Host name is null");
		
		this.mailUser = mailUser;
		this.mailHost = new HostAddress(mailHost.toString());
	}
	
	/**
	 * Create an email address object. The host name and local-part must be valid (non-null, non-empty and contains valid
	 * characters per RFC2822).
	 * @precondition The host name and local-part must be valid (non-null, non-empty and contains valid characters per RFC2822).
	 * @postcondition An emailAddress object corresponding to the given arguments will be returned.
	 * @param mailUser
	 * @param mailHost
	 * @throws UnknownHostException
	 */
	private EmailAddress( String mailUser, HostAddress mailHost ) throws UnknownHostException{
		
		if( mailHost == null )
			throw new UnknownHostException("Host name is null");
		
		this.mailUser = mailUser;
		this.mailHost = mailHost;
	}
	
	/**
	 * Create an email address object. The host name and local-part must be valid (non-null, non-empty and contains valid
	 * characters per RFC2822).
	 * @precondition The host name and local-part must be valid (non-null, non-empty and contains valid characters per RFC2822).
	 * @postcondition An emailAddress object corresponding to the given arguments will be returned.
	 * @throws UnknownHostException
	 */
	public static EmailAddress getByAddress( String address ) throws UnknownHostException, InvalidLocalPartException{
		
		// 0 -- Precondition check
		if( address == null )
			throw new UnknownHostException("Email address is null");
		
		if( address.length() == 0 )
			throw new UnknownHostException("Email address is empty");
		
		// 1 -- Check the local-part
		String[] parsedAddress = address.split("@");
		
		if( parsedAddress[0] == null || parsedAddress[0].length() == 0 )
			throw new InvalidLocalPartException("Email address contains invalid characters");
		
		Pattern localPartRegex = Pattern.compile( EMAIL_LOCAL_PART_REGEX );
		Matcher matcher = localPartRegex.matcher( parsedAddress[0] );
		
		if( !matcher.matches() )
			throw new InvalidLocalPartException("Email address contains invalid characters");
		
		// 2 -- Check the domain
		
		if( parsedAddress.length == 1 || parsedAddress[1] == null || parsedAddress[1].length() == 0 )
			throw new UnknownHostException("Host address is empty");
		
		HostAddress mailHost = new HostAddress( parsedAddress[1] );
		
		return new EmailAddress( parsedAddress[0], mailHost );
		
	}
	
	/**
	 * Create an email address object. The host name and local-part must be valid (non-null, non-empty and contains valid
	 * characters per RFC2822).
	 * @precondition The host name and local-part must be valid (non-null, non-empty and contains valid characters per RFC2822).
	 * @postcondition An emailAddress object corresponding to the given arguments will be returned.
	 * @param mailUser
	 * @param mailHost
	 * @throws UnknownHostException
	 */
	public static EmailAddress getByAddress( String mailUser, InetAddress mailHost ) throws UnknownHostException, InvalidLocalPartException{
		
		// 0 -- Precondition check
		if( mailUser == null )
			throw new InvalidLocalPartException("Local-part is null");
		
		if( mailUser.length() == 0 )
			throw new InvalidLocalPartException("Local-part is empty");
		
		if( mailHost == null )
			throw new UnknownHostException("Host name is null");

		
		// 1 -- Check the local-part	
		Pattern localPartRegex = Pattern.compile( EMAIL_LOCAL_PART_REGEX );
		Matcher matcher = localPartRegex.matcher( mailUser );
		
		if( !matcher.matches() )
			throw new InvalidLocalPartException("Email address contains invalid characters");
		
		return new EmailAddress( mailUser, mailHost );
	}
	
	/**
	 * Return the local part of the email address ("BillJohnson" of "BillJohnson@somehost.om").
	 * @return
	 */
	public String getLocalPart(){
		return mailUser;
	}
	
	/**
	 * Returns the domain part of the email address.
	 * @return
	 */
	public HostAddress getDomain(){
		return mailHost;
	}
	
	/**
	 * Returns a string representation of the given email address.
	 */
	public String toString(){
		return mailUser + "@" + mailHost.toString();
	}
	
}
