package net.lukemurphey.nsia;

import java.net.*;
import java.util.regex.*;

import org.apache.commons.lang.NullArgumentException;

/**
 * The IP address range represents a contigous range of IP addresses (such as a subnet). 
 * @author luke
 *
 */

public class InetAddressRange{

	private int startOfRange;
	private int endOfRange;
	
	/**
	 * Creates an inclusive IP address range corresponding to the start and ending addresses given. An illegal argument
	 * exception will be thrown if the IP address range is invalid, that is, if the start of the range is not after the end. 
	 * @precondition The start of the range must not be after the end of the range.
	 * @postcondition An object will be returned corresponding to the range given.
	 * @param start
	 * @param end
	 */
	private InetAddressRange( InetAddress start, InetAddress end ){
		int startInt = getIntFromAddress( start );
		int endInt = getIntFromAddress( end );
		
		if( startInt >= 0 && endInt >=0 && startInt > endInt) //0.0.0.0 to 127.255.255.255 (illegal if start of range is not before end) 
			throw new IllegalArgumentException("The start IP address must not be greater than the end address");
		else if( startInt < 0 && endInt < 0 && endInt < startInt) // 128.0.0.0 to 255.255.255.255 (illegal if start of range is not before end) 
			throw new IllegalArgumentException("The start IP address must not be greater than the end address");
		else if( startInt < 0 && endInt >= 0) //128.0.0.0 to 127.255.255.255 (always illegal)
			throw new IllegalArgumentException("The start IP address must not be greater than the end address");
		
		startOfRange = startInt;
		endOfRange = endInt;
	}
	
	/**
	 * Creates an inclusive IP address range corresponding to the start and ending addresses given. An illegal argument
	 * exception will be thrown if the IP address range is invalid, that is, if the start of the range is not after the end. 
	 * @precondition The start of the range must not be after the end of the range, the ranges must be IP addresses and not null
	 * @postcondition An object will be returned corresponding to the range given.
	 * @param start
	 * @param end
	 */
	private InetAddressRange( HostAddress start, HostAddress end ){
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the start address is not null
		if( start == null )
			throw new NullArgumentException("The start address cannot be null");
		
		//	 0.2 -- Make sure the start address is an IP address
		if( start.addressType() != HostAddress.IP_ADDRESS)
			throw new IllegalArgumentException("The start address must be an IP address");
		
		//	 0.3 -- Make sure the end address is not null
		if( start == null )
			throw new NullArgumentException("The end address cannot be null");
		
		//	 0.4 -- Make sure the end address is an IP address
		if( end.addressType() != HostAddress.IP_ADDRESS)
			throw new IllegalArgumentException("The end address must be an IP address");
		
		
		// 1 -- 
		int startInt = getIntFromAddress( start );
		int endInt = getIntFromAddress( end );
		
		if( startInt >= 0 && endInt >=0 && startInt > endInt) //0.0.0.0 to 127.255.255.255 (illegal if start of range is not before end) 
			throw new IllegalArgumentException("The start IP address must not be greater than the end address");
		else if( startInt < 0 && endInt < 0 && endInt < startInt) // 128.0.0.0 to 255.255.255.255 (illegal if start of range is not before end) 
			throw new IllegalArgumentException("The start IP address must not be greater than the end address");
		else if( startInt < 0 && endInt >= 0) //128.0.0.0 to 127.255.255.255 (always illegal)
			throw new IllegalArgumentException("The start IP address must not be greater than the end address");
		
		startOfRange = startInt;
		endOfRange = endInt;
	}
	
	/**
	 * Creates an inclusive IP address range corresponding to the start and ending addresses given. An illegal argument
	 * exception will be thrown if the IP address range is invalid, that is, if the start of the range is not after the end. 
	 * @precondition The start of the range must not be after the end of the range.
	 * @postcondition An object will be returned corresponding to the range given.
	 * @param start
	 * @param end
	 */
	private InetAddressRange( int startInt, int endInt ){
		
		if( startInt >= 0 && endInt >=0 && startInt > endInt) //0.0.0.0 to 127.255.255.255 (illegal if start of range is not before end) 
			throw new IllegalArgumentException("The start IP address must not be greater than the end address");
		else if( startInt < 0 && endInt < 0 && endInt < startInt) // 128.0.0.0 to 255.255.255.255 (illegal if start of range is not before end) 
			throw new IllegalArgumentException("The start IP address must not be greater than the end address");
		else if( startInt < 0 && endInt >= 0) //128.0.0.0 to 127.255.255.255 (always illegal)
			throw new IllegalArgumentException("The start IP address must not be greater than the end address");
		
		startOfRange = startInt;
		endOfRange = endInt;
	}
	
	/**
	 * Construct an address range corresponding to the start and end addresses given.
	 * @precondition The start of the range must not be after the end of the range.
	 * @postcondition An object will be returned corresponding to the range given.
	 * @param start
	 * @param end
	 * @return
	 */
	public static InetAddressRange getByRange( InetAddress start, InetAddress end ){
		InetAddressRange inetAddressRange = new InetAddressRange(start, end);
		return inetAddressRange;
	}
	
	/**
	 * Construct an address range corresponding to the start and end addresses given.
	 * @precondition The start of the range must not be after the end of the range, the addresses must be IP addresses and not be null.
	 * @postcondition An object will be returned corresponding to the range given.
	 * @param start
	 * @param end
	 * @return
	 */
	public static InetAddressRange getByRange( HostAddress start, HostAddress end ){
		InetAddressRange inetAddressRange = new InetAddressRange(start, end);
		return inetAddressRange;
	}
	
	/**
	 * Construct an address range corresponding to the start and end addresses given.
	 * @precondition The start of the range must not be after the end of the range.
	 * @postcondition An object will be returned corresponding to the range given.
	 * @param start
	 * @param end
	 * @return
	 */
	public static InetAddressRange getByRange( int start, int end ){
		InetAddressRange inetAddressRange = new InetAddressRange(start, end);
		return inetAddressRange;
	}
	
	/**
	 * Construct an address range corresponding to the start and end addresses given.
	 * @precondition The start of the range must not be after the end of the range.
	 * @postcondition An object will be returned corresponding to the range given.
	 * @param start
	 * @param end
	 * @return
	 * @throws SubnetMaskInvalidException 
	 */
	public static InetAddressRange getBySubnetRange( InetAddress start, InetAddress subnetMask ){

		// 0 --Precondition check
		if( start == null || subnetMask == null )
			throw new IllegalArgumentException("Subnet mask and start address must not be null");
		
		int subnetMaskLong = getIntFromAddress ( subnetMask );
		int startAddress = getIntFromAddress ( start );
		startAddress = startAddress & ~subnetMaskLong;
		
		int endAddress = (startAddress + subnetMaskLong);
		
		InetAddressRange inetAddressRange = new InetAddressRange(startAddress, endAddress);
		return inetAddressRange;
	}
	
	/**
	 * Determine if the given address is within the loopback range.
	 * @precondition A valid address must be given
	 * @postcondition True will be returned if the address is valid, otherwise false will be returned.
	 * @param inetAddress
	 * @return
	 * @throws UnknownHostException
	 */
	public static boolean isAddressLoopback( InetAddress inetAddress ){
		int addressInt = getIntFromAddress( inetAddress );
		int internalStart = 2130706433;//getIntFromAddress( InetAddress.getByName("127.0.0.1") );
		int internalEnd  = 2147483646;//getIntFromAddress( InetAddress.getByName("127.255.255.254") );
		if( addressInt <= internalEnd && addressInt >= internalStart)
			return true;
		else
			return false;
	}
	
	/**
	 * Determine if the given address is within the loopback range.
	 * @precondition A valid address must be given
	 * @postcondition True will be returned if the address is valid, otherwise false will be returned.
	 * @param inetAddress
	 * @return
	 * @throws UnknownHostException
	 */
	public static boolean isAddressLoopback( HostAddress inetAddress ){
		int addressInt = getIntFromAddress( inetAddress );
		int internalStart = 2130706433;//getIntFromAddress( InetAddress.getByName("127.0.0.1") );
		int internalEnd  = 2147483646;//getIntFromAddress( InetAddress.getByName("127.255.255.254") );
		if( addressInt <= internalEnd && addressInt >= internalStart)
			return true;
		else
			return false;
	}
	
	/**
	 * Construct an address range corresponding to the string which defines the start and end address. The range
	 * may be:
	 * <ul>
	 * 	<li>Single IP address (192.168.10.2)</li>
	 * 	<li>Domain name (google.com)</li>
	 * 	<li>IP Range (193.0.0.1-193.0.0.255)</li>
	 * 	<li>CIDR notation (193.0.0.1/24)</li>
	 * </ul>
	 * @precondition The string must be a IP range and start of the range must not be after the end of the range.
	 * @postcondition An object will be returned corresponding to the range given.
	 * @param start
	 * @param end
	 * @return
	 * @throws InputValidationException 
	 * @throws UnknownHostException 
	 */
	public static InetAddressRange getByRange( String range ) throws InputValidationException, UnknownHostException{

		//
		String[] ips = range.split("-");
		
		if( ips.length != 2)
			return null;
		
		String startIp = ips[0].trim();
		String endIp =  ips[1].trim();
		
		Pattern singleIp = Pattern.compile("(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)");
		Matcher matcher = singleIp.matcher(startIp);
		
		
		if( !matcher.matches() )
			throw new InputValidationException( "Start address is invalid", "Start IP Address", startIp);
		
		matcher = singleIp.matcher(endIp);
		if( !matcher.matches() )
			throw new InputValidationException( "End address is invalid", "End IP Address", endIp);
		
		InetAddressRange inetAddressRange = new InetAddressRange( InetAddress.getByName(startIp), InetAddress.getByName(endIp));
		
		return inetAddressRange;

	}
	
	/**
	 * Determines if the address given lies between the range identified by the given range.
	 * @precondition None
	 * @postcondition Returns a boolean indicating if the given address is within the given range
	 * @param address
	 * @return
	 */
	public boolean isWithinRange( InetAddress address ){
		int addressInt = getIntFromAddress( address );
		
		if( startOfRange >= 0 && endOfRange >=0) //0.0.0.0 to 127.255.255.255
			return (addressInt >= startOfRange) && (addressInt <= endOfRange);
		else if( startOfRange < 0 && endOfRange < 0) // 128.0.0.0 to 255.255.255.255
			return (addressInt <= startOfRange) && (addressInt >= endOfRange);
		else //0.0.0.0 to 255.255.255.255
			return (addressInt >= startOfRange) && (addressInt >= endOfRange);
	}
	
	/**
	 * Determines if the address given lies between the range identified by the given range.
	 * @precondition None
	 * @postcondition Returns a boolean indicating if the given address is within the given range
	 * @param address
	 * @return
	 */
	public boolean isWithinRange( HostAddress address ){
		int addressInt = getIntFromAddress( address );
		
		if( startOfRange >= 0 && endOfRange >=0) //0.0.0.0 to 127.255.255.255
			return (addressInt >= startOfRange) && (addressInt <= endOfRange);
		else if( startOfRange < 0 && endOfRange < 0) // 128.0.0.0 to 255.255.255.255
			return (addressInt <= startOfRange) && (addressInt >= endOfRange);
		else //0.0.0.0 to 255.255.255.255
			return (addressInt >= startOfRange) && (addressInt >= endOfRange);
	}
	
	/**
	 * Retrieves an integer corresponding to the address given.
	 * @precondition None
	 * @postcondition Returns an integer corresponding to the address. 
	 * @param address
	 * @return
	 */
	private static int getIntFromAddress( InetAddress address ){
		byte[] addressBytes = address.getAddress();
		int addressInt = 0;
		
		addressInt = ((int)addressBytes[0] & 0xFF) * 16777216;
		addressInt += ((int)addressBytes[1] & 0xFF)* 65536;
		addressInt += ((int)addressBytes[2] & 0xFF)* 256;
		addressInt += ((int)addressBytes[3] & 0xFF);
		
		return addressInt;
	}
	
	/**
	 * Retrieves an integer corresponding to the address given.
	 * @precondition None
	 * @postcondition Returns an integer corresponding to the address. 
	 * @param address
	 * @return
	 */
	private static int getIntFromAddress( HostAddress address ){
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the address is not null
		if(address == null)
			throw new NullArgumentException("Address cannot be null");
	      
		//	 0.2 -- Make sure the address has four quads
		String addressString = address.toString();
		String[] temp = addressString.split("\\.");
		if( temp.length != 4 )
			throw new IllegalArgumentException("Argument is not in a dotted-quad IP address format");
		
		//	 0.3 -- Make sure the quads are valid (and insert them into an array)
		int[] addressBytes = new int[4];
		
		for(int c = 0; c < 4; c++){
			addressBytes[c] = Integer.parseInt(temp[c]);
			if( addressBytes[c] > 255 || addressBytes[c] < 0 )
				throw new IllegalArgumentException("Argument is not in a dotted-quad IP address format (value at position " + c + " is not valid ["+ addressBytes[c] + "])");
		}
		
		// 1 -- Compute the value
		int addressInt = (addressBytes[0]) * 16777216;
		addressInt += (addressBytes[1])* 65536;
		addressInt += (addressBytes[2])* 256;
		addressInt += (addressBytes[3]);
		
		return addressInt;
	}
	
	/**
	 * Derives a dotted quad string from the integer representation of an IP address.
	 * @precondition None
	 * @postcondition A string will be returned that represents the IP address
	 * @param addressInt
	 * @return
	 */
	private String getAddressFromInt( int addressInt ){
		int quad1 = addressInt >> 24;
		
		if( quad1 < 0 )
			quad1 = 256 + quad1;
		
		int quad2 = (addressInt & 0x00ff0000) >> 16;
		int quad3 = (addressInt & 0x0000ff00) >> 8;
		int quad4 = addressInt & 0x000000ff;
		
		return quad1 + "." + quad2 + "." + quad3 + "." + quad4;
	}
	
	/**
	 * Retrieve a string representation of the start of the address range.
	 * @precondition None
	 * @postcondition A string representation of the start of the range will be returned
	 * @return
	 */
	public String getStartAddressString(){
		return getAddressFromInt( startOfRange );
	}
	
	/**
	 * Retrieve a string representation of the end of the address range.
	 * @precondition None
	 * @postcondition A string representation of the end of the range will be returned
	 * @return
	 */
	public String getEndAddressString(){
		return getAddressFromInt( endOfRange );
	}
	
	/**
	 * Returns a string description of the range.
	 * @precondition None
	 * @postcondition A string representation of the address will be returned
	 */
	public String toString(){
		return getStartAddressString() + "-" + getEndAddressString();
	}
}
