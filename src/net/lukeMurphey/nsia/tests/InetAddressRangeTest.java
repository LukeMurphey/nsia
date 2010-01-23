package net.lukemurphey.nsia.tests;

import junit.framework.TestCase;
import java.net.*;

import net.lukemurphey.nsia.InetAddressRange;
import net.lukemurphey.nsia.InputValidationException;

public class InetAddressRangeTest extends TestCase {

	public static void main(String[] args) {
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.InetAddressRange.getByRange(InetAddress, InetAddress)'
	 */
	public void testGetByRange() throws UnknownHostException {
		InetAddress zero = InetAddress.getByName("0.0.0.0");
		InetAddress one = InetAddress.getByName("32.1.3.4");
		InetAddress two = InetAddress.getByName("127.255.255.255");
		InetAddress three = InetAddress.getByName("128.0.0.0");
		InetAddress four = InetAddress.getByName("132.1.3.4");
		InetAddress five = InetAddress.getByName("255.255.255.255");
		
		// 1 -- These should fail
		boolean expectionCaught = false;
		
		try{
			expectionCaught = false;
			InetAddressRange.getByRange(one, zero);
		}
		catch( IllegalArgumentException e ){
			expectionCaught = true;
		}
		finally{
			if( !expectionCaught )
				fail("Failed to throw exception");
		}
		
		try{
			expectionCaught = false;
			InetAddressRange.getByRange(two, zero);
		}
		catch( IllegalArgumentException e ){
			expectionCaught = true;
		}
		finally{
			if( !expectionCaught )
				fail("Failed to throw exception");
		}
		
		try{
			expectionCaught = false;
			InetAddressRange.getByRange(four, three);
		}
		catch( IllegalArgumentException e ){
			expectionCaught = true;
		}
		finally{
			if( !expectionCaught )
				fail("Failed to throw exception");
		}
		
		try{
			expectionCaught = false;
			InetAddressRange.getByRange(five, four);
		}
		catch( IllegalArgumentException e ){
			expectionCaught = true;
		}
		finally{
			if( !expectionCaught )
				fail("Failed to throw exception");
		}
		
		try{
			expectionCaught = false;
			InetAddressRange.getByRange(five, zero);
		}
		catch( IllegalArgumentException e ){
			expectionCaught = true;
		}
		finally{
			if( !expectionCaught )
				fail("Failed to throw exception");
		}
		
		InetAddressRange.getByRange(one, two);
		InetAddressRange.getByRange(two, three);
		InetAddressRange.getByRange(two, four);
		InetAddressRange.getByRange(three, five);
		InetAddressRange.getByRange(zero, five);
		InetAddressRange.getByRange(four, five);
		InetAddressRange.getByRange(two, five);

	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.InetAddressRange.isWithinRange(InetAddress)'
	 */
	public void testIsWithinRange() throws UnknownHostException {
		InetAddress one = InetAddress.getByName("32.1.3.4");
		InetAddress two = InetAddress.getByName("127.255.255.255");
		InetAddressRange inetAddressRange = InetAddressRange.getByRange(one, two);
		
		InetAddress within1 = InetAddress.getByName("32.1.3.4");
		InetAddress within2 = InetAddress.getByName("32.1.3.5");
		InetAddress notWithin1 = InetAddress.getByName("1.1.3.4");
		InetAddress notWithin2 = InetAddress.getByName("128.0.0.0");
		
		if( !inetAddressRange.isWithinRange(within1) )
			fail("Address is within range but returned false");
		
		if( !inetAddressRange.isWithinRange(within2) )
			fail("Address is within range but returned false");
		
		if( inetAddressRange.isWithinRange(notWithin1) )
			fail("Address is not within range but returned true");
		
		if( inetAddressRange.isWithinRange(notWithin2) )
			fail("Address is not within range but returned true");
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.InetAddressRange.getStartAddressString()'
	 */
	public void testGetStartAddressString() throws UnknownHostException {
		InetAddress one = InetAddress.getByName("32.1.3.4");
		InetAddress two = InetAddress.getByName("255.255.255.255");
		InetAddressRange inetAddressRange = InetAddressRange.getByRange(one, two);

		if(  !inetAddressRange.toString().matches("32.1.3.4-255.255.255.255") )
			fail( "Range failed to match");
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.InetAddressRange.getBySubnetRange()'
	 */
	public void testGetEndAddressString() throws UnknownHostException {
		InetAddressRange addressRange = InetAddressRange.getBySubnetRange( InetAddress.getByName( "10.2.3.4"), InetAddress.getByName( "0.0.0.255" ) );
		if(  !addressRange.toString().matches("10.2.3.0-10.2.3.255") )
			fail ("Address range not correctly computed");
	}
	
	public void testIsLoopback() throws UnknownHostException{
		InetAddress loopbackStart = InetAddress.getByName("127.0.0.1");
		InetAddress loopbackEnd = InetAddress.getByName("127.255.255.254");
		InetAddress loopbackMid = InetAddress.getByName("127.0.128.32");
		InetAddress notLoopback = InetAddress.getByName("192.0.132.24");
		
		if( !InetAddressRange.isAddressLoopback( loopbackStart ) )
			fail("Loopback start not accepted");
		
		if( !InetAddressRange.isAddressLoopback( loopbackEnd ) )
			fail("Loopback start not accepted");
		
		if( !InetAddressRange.isAddressLoopback( loopbackMid ) )
			fail("Loopback start not accepted");
		
		if( InetAddressRange.isAddressLoopback( notLoopback ) )
			fail("Non-loopback accepted");
	}
	
	public void testParseRange() throws UnknownHostException, InputValidationException{
		InetAddressRange addressRange = InetAddressRange.getByRange("1.2.3.4-1.2.3.6");
		
		if( addressRange == null )
			fail("Null returned");
		else if( !addressRange.getStartAddressString().equals("1.2.3.4") ){
			fail("Start of address range not accepted correctly:" + addressRange.getStartAddressString() );
		}
		else if( !addressRange.getEndAddressString().equals("1.2.3.6") ){
			fail("End of address range not accepted correctly:" + addressRange.getEndAddressString());
		}
		
		
		addressRange = InetAddressRange.getByRange(" 5.6.7.8 - 9.10.11.12 ");
		
		if( addressRange == null )
			fail("Null returned");
		else if( !addressRange.getStartAddressString().equals("5.6.7.8") ){
			fail("Start of address range not accepted correctly:" + addressRange.getStartAddressString() );
		}
		else if( !addressRange.getEndAddressString().equals("9.10.11.12") ){
			fail("End of address range not accepted correctly:" + addressRange.getEndAddressString());
		}
		
		
		try{
			addressRange = InetAddressRange.getByRange(" 256.6.7.8 - 256.10.11.12 ");
		}catch(InputValidationException e){
			return;
		}
		
		fail("Invalid address range accepted:" + addressRange.toString() );
		
		
	}

}
