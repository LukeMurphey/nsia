package net.lukemurphey.nsia.tests;

import junit.framework.TestCase;

import net.lukemurphey.nsia.scan.LineParseException;
import net.lukemurphey.nsia.scan.NetworkPortRange;
import net.lukemurphey.nsia.scan.NetworkPortRange.Protocol;
import net.lukemurphey.nsia.scan.NetworkPortRange.SocketState;

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
	
	public void testRangeSplitCutEnd() throws LineParseException{
		
		NetworkPortRange range = new NetworkPortRange(4, 10, NetworkPortRange.Protocol.TCP);
		NetworkPortRange subtractRange = new NetworkPortRange(6, 12, NetworkPortRange.Protocol.TCP);
		
		NetworkPortRange[] result = NetworkPortRange.removeFromRange(range, subtractRange);
		
		if( result.length == 1 ){
			if( result[0].getStartPort() != 4 ){
				fail("Start port is not expected");
			}
			
			if( result[0].getEndPort() != 5 ){
				fail("End port is not expected");
			}
		}
		else{
			fail("Range is invalid");
		}
	}
	
	public void testRangeSplitCutMid() throws LineParseException{
		
		NetworkPortRange range = new NetworkPortRange(4, 20, NetworkPortRange.Protocol.TCP);
		NetworkPortRange subtractRange = new NetworkPortRange(6, 12, NetworkPortRange.Protocol.TCP);
		
		NetworkPortRange[] result = NetworkPortRange.removeFromRange(range, subtractRange);
		
		if( result.length == 2 ){
			if( result[0].getStartPort() != 4 || result[0].getEndPort() != 5){
				fail("First port range is not expected");
			}
			
			if( result[1].getStartPort() != 13 || result[1].getEndPort() != 20){
				fail("Second port range is not expected");
			}
		}
		else{
			fail("Range is invalid");
		}
	}
	
	public void testRangeSplitCutStart() throws LineParseException{
		
		NetworkPortRange range = new NetworkPortRange(4, 20, NetworkPortRange.Protocol.TCP);
		NetworkPortRange subtractRange = new NetworkPortRange(1, 7, NetworkPortRange.Protocol.TCP);
		
		NetworkPortRange[] result = NetworkPortRange.removeFromRange(range, subtractRange);
		
		if( result.length == 1 ){
			if( result[0].getStartPort() != 8 || result[0].getEndPort() != 20){
				fail("Port range is not expected");
			}
		}
		else{
			fail("Range is invalid");
		}
	}
	
	public void testRangeSplitCutBeforeStart() throws LineParseException{
		
		NetworkPortRange range = new NetworkPortRange(1, 2, NetworkPortRange.Protocol.TCP);
		NetworkPortRange subtractRange = new NetworkPortRange(4, 7, NetworkPortRange.Protocol.TCP);
		
		NetworkPortRange[] result = NetworkPortRange.removeFromRange(range, subtractRange);
		
		if( result.length == 1 ){
			if( result[0].getStartPort() != 1 || result[0].getEndPort() != 2){
				fail("Port range is not expected");
			}
		}
		else{
			fail("Range is invalid");
		}
	}
	
	public void testRangeSplitCutAfterEnd() throws LineParseException{
		
		NetworkPortRange range = new NetworkPortRange(19, 20, NetworkPortRange.Protocol.TCP);
		NetworkPortRange subtractRange = new NetworkPortRange(1, 7, NetworkPortRange.Protocol.TCP);
		
		NetworkPortRange[] result = NetworkPortRange.removeFromRange(range, subtractRange);
		
		if( result.length == 1 ){
			if( result[0].getStartPort() != 19 || result[0].getEndPort() != 20){
				fail("Port range is not expected");
			}
		}
		else{
			fail("Range is invalid");
		}
	}
	
	public void testRangeSplitCutAtEnd() throws LineParseException{
		
		NetworkPortRange range = new NetworkPortRange(1, 4, NetworkPortRange.Protocol.TCP);
		NetworkPortRange subtractRange = new NetworkPortRange(4, 7, NetworkPortRange.Protocol.TCP);
		
		NetworkPortRange[] result = NetworkPortRange.removeFromRange(range, subtractRange);
		
		if( result.length == 1 ){
			if( result[0].getStartPort() != 1 || result[0].getEndPort() != 3){
				fail("Port range is not expected");
			}
		}
		else{
			fail("Range is invalid");
		}
	}
	
	public void testRangeSplitCutAtStart() throws LineParseException{
		
		NetworkPortRange range = new NetworkPortRange(4, 7, NetworkPortRange.Protocol.TCP);
		NetworkPortRange subtractRange = new NetworkPortRange(7, 12, NetworkPortRange.Protocol.TCP);
		
		NetworkPortRange[] result = NetworkPortRange.removeFromRange(range, subtractRange);
		
		if( result.length == 1 ){
			if( result[0].getStartPort() != 4 || result[0].getEndPort() != 6){
				fail("Port range is not expected");
			}
		}
		else{
			fail("Range is invalid");
		}
	}
	
	public void testRangeSplitCutJustAfterEnd() throws LineParseException{
		
		NetworkPortRange range = new NetworkPortRange(1, 4, NetworkPortRange.Protocol.TCP);
		NetworkPortRange subtractRange = new NetworkPortRange(5, 7, NetworkPortRange.Protocol.TCP);
		
		NetworkPortRange[] result = NetworkPortRange.removeFromRange(range, subtractRange);
		
		if( result.length == 1 ){
			if( result[0].getStartPort() != 1 || result[0].getEndPort() != 4){
				fail("Port range is not expected");
			}
		}
		else{
			fail("Range is invalid");
		}
	}
	
	public void testRangeSplitCutJustBeforeStart() throws LineParseException{
		
		NetworkPortRange range = new NetworkPortRange(4, 7, NetworkPortRange.Protocol.TCP);
		NetworkPortRange subtractRange = new NetworkPortRange(8, 12, NetworkPortRange.Protocol.TCP);
		
		NetworkPortRange[] result = NetworkPortRange.removeFromRange(range, subtractRange);
		
		if( result.length == 1 ){
			if( result[0].getStartPort() != 4 || result[0].getEndPort() != 7){
				fail("Port range is not expected");
			}
		}
		else{
			fail("Range is invalid");
		}
	}
	
	public void testOverlapNone(){
		NetworkPortRange range1 = new NetworkPortRange(4, 7, NetworkPortRange.Protocol.TCP);
		NetworkPortRange range2 = new NetworkPortRange(8, 32, NetworkPortRange.Protocol.TCP);
		
		if( range1.overlapsWith(range2) ){
			fail("Incorrect response, ranges do not overlap");
		}
	}
	
	public void testOverlapNone2(){
		NetworkPortRange range1 = new NetworkPortRange(8, 32, NetworkPortRange.Protocol.TCP);
		NetworkPortRange range2 = new NetworkPortRange(4, 7, NetworkPortRange.Protocol.TCP);
		
		if( range1.overlapsWith(range2) ){
			fail("Incorrect response, ranges do not overlap");
		}
	}
	
	public void testOverlapAtStart(){
		NetworkPortRange range1 = new NetworkPortRange(8, 32, NetworkPortRange.Protocol.TCP);
		NetworkPortRange range2 = new NetworkPortRange(1, 8, NetworkPortRange.Protocol.TCP);
		
		if( !range1.overlapsWith(range2) ){
			fail("Incorrect response, ranges do overlap");
		}
	}
	
	public void testOverlapAtEnd(){
		NetworkPortRange range1 = new NetworkPortRange(8, 32, NetworkPortRange.Protocol.TCP);
		NetworkPortRange range2 = new NetworkPortRange(32, 48, NetworkPortRange.Protocol.TCP);
		
		if( !range1.overlapsWith(range2) ){
			fail("Incorrect response, ranges do overlap");
		}
	}
	
	public void testOverlapIdentical(){
		NetworkPortRange range1 = new NetworkPortRange(8, 8, NetworkPortRange.Protocol.TCP);
		NetworkPortRange range2 = new NetworkPortRange(8, 8, NetworkPortRange.Protocol.TCP);
		
		if( !range1.overlapsWith(range2) ){
			fail("Incorrect response, ranges do overlap");
		}
	}
	
	public void testOverlapCompletely(){
		NetworkPortRange range1 = new NetworkPortRange(8, 49, NetworkPortRange.Protocol.TCP);
		NetworkPortRange range2 = new NetworkPortRange(32, 48, NetworkPortRange.Protocol.TCP);
		
		if( !range1.overlapsWith(range2) ){
			fail("Incorrect response, ranges do overlap");
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
