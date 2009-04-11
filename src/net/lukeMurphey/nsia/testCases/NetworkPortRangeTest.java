package net.lukeMurphey.nsia.testCases;

import junit.framework.TestCase;

import net.lukeMurphey.nsia.scanRules.LineParseException;
import net.lukeMurphey.nsia.scanRules.NetworkPortRange;
import net.lukeMurphey.nsia.scanRules.NetworkPortRange.Protocol;
import net.lukeMurphey.nsia.scanRules.NetworkPortRange.SocketState;

public class NetworkPortRangeTest extends TestCase{

	public void testRangeParse() throws LineParseException{
		
		NetworkPortRange portRange[] = NetworkPortRange.parseRange(NetworkPortRange.Protocol.TCP, NetworkPortRange.SocketState.OPEN, " 1 \n 2- 5 ");
		
		int totalCount = 0;
		
		for( int c = 0; c < portRange.length; c++){
			if( portRange[c] != null )
				totalCount += portRange[c].getNumberOfPorts();
		}
		
		if( totalCount != 5 ){
			fail("Total count was not the expected value (five)");
		}
	}
	
	public void testRangeParseWithProtocol() throws LineParseException{
		
		NetworkPortRange portRange[] = NetworkPortRange.parseRange( NetworkPortRange.SocketState.OPEN, "UDP\\1 \n TCP\\2- 5 ");
		
		int totalCount = 0;
		
		for( int c = 0; c < portRange.length; c++){
			if( portRange[c] != null )
				totalCount += portRange[c].getNumberOfPorts();
		}
		
		if( totalCount != 5 ){
			fail("Total count was not the expected value (five)");
		}
	}
	
	public void testRangeToString() throws LineParseException{
		
		NetworkPortRange portRange = new NetworkPortRange(20, 22, NetworkPortRange.Protocol.TCP);
		
		String result = portRange.toString();
		
		if( result.equals("TCP\\20-22") == false ){
			fail("The array of ports was not converted correctly");
		}
	}
	
	public void testRangeArrayToString() throws LineParseException{
		
		NetworkPortRange portRange[] = NetworkPortRange.parseRange( NetworkPortRange.SocketState.OPEN, "UDP\\1 \n TCP\\2-5 ");
		String result = NetworkPortRange.convertToString( portRange );
		
		if( result.equals("UDP\\1\nTCP\\2-5") == false ){
			fail("The array of ports was not converted correctly");
		}
	}
	
	public void testComputeSummary(){
		
		NetworkPortRange[] scanned = new NetworkPortRange[]{ new NetworkPortRange(1,5, Protocol.TCP), new NetworkPortRange(1,5, Protocol.UDP)};
		NetworkPortRange[] deviations = new NetworkPortRange[]{ new NetworkPortRange(3,3, Protocol.TCP, SocketState.NO_RESPONSE)};
		NetworkPortRange[] expectedOpen = new NetworkPortRange[]{ new NetworkPortRange(3,3, Protocol.TCP) };
		
		NetworkPortRange[] range = NetworkPortRange.computeScannedResultRange(deviations, scanned, expectedOpen);
		
		if( range.length != 4){
			fail("The result is not the expected length (observed " + range.length + ", should have been " + 4 +")");
		}
		else{
			if(
					range[0].getStartPort() != 1
					|| range[0].getEndPort() != 2
					|| range[0].getProtocol() != Protocol.TCP
					|| range[0].getState() != NetworkPortRange.SocketState.CLOSED){
				fail("The result does not match the expected value (at " + range[0] + ")");
			}
			else if(
					range[1].getStartPort() != 3
					|| range[1].getEndPort() != 3
					|| range[1].getProtocol() != Protocol.TCP
					|| range[1].getState() != NetworkPortRange.SocketState.NO_RESPONSE){
				fail("The result does not match the expected value (at " + range[1] + ")");
			}
			else if(
					range[2].getStartPort() != 4
					|| range[2].getEndPort() != 5
					|| range[2].getProtocol() != Protocol.TCP
					|| range[2].getState() != NetworkPortRange.SocketState.CLOSED){
				fail("The result does not match the expected value (at " + range[2] + ")");
			}
			else if(
					range[3].getStartPort() != 1
					|| range[3].getEndPort() != 5
					|| range[3].getProtocol() != Protocol.UDP
					|| range[3].getState() != NetworkPortRange.SocketState.CLOSED){
				fail("The result does not match the expected value (at " + range[3] + ")");
			}
		}
		
		/*for (NetworkPortRange networkPortRange : range) {
			System.out.println(networkPortRange.toString() + " " + networkPortRange.getState());
			fail("The result is not the expected length (observed " + range.length + ", should have been " + 4 +")");
		}*/
	}
}
