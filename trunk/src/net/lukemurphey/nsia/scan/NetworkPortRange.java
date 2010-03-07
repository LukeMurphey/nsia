package net.lukemurphey.nsia.scan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Comparator;
import java.util.Arrays;
import java.util.Vector;

import net.lukemurphey.nsia.GenericUtils;

/**
 * This class represents a port range for a specific protocol as well as a reachablility state (open, closed, no response). A single entry
 * can be represented by a range with a single port.
 * @author luke
 *
 */
public class NetworkPortRange{

	public enum Protocol{
		UDP, TCP;
	}
	
	public enum SocketState{
		OPEN, CLOSED, NO_RESPONSE, UNDEFINED;
	}
	
	//public final static int PROTOCOL_UDP = 0;
	//public final static int PROTOCOL_TCP = 1;
	
	//public final static int STATE_OPEN = 0;
	//public final static int STATE_CLOSED = 1;
	//public final static int STATE_NO_RESPONSE = 2;
	//public final static int STATE_UNDEFINED = 3;
	
	private static final Pattern REGEX_RANGE = Pattern.compile("([0-9]+)[ ]*\\-[ ]*([0-9]+)");
	private static final Pattern REGEX_PORT = Pattern.compile("[0-9]+");
	
	private static final Pattern REGEX_RANGE_PROTOCOL = Pattern.compile("(UDP|TCP)(\\\\|/)([0-9]+)[ ]*\\-[ ]*([0-9]+)");
	private static final Pattern REGEX_PORT_PROTOCOL = Pattern.compile("(UDP|TCP)(\\\\|/)([0-9]+)");
	
	private int startPort;
	private int endPort;
	private Protocol protocol;
	private SocketState state;
	
	
	public NetworkPortRange( int startPort, int endPort, Protocol protocol, SocketState state){
		
		// 0 -- Precondition check

		//	 0.1 -- Make sure the port number is valid
		if(startPort < 0 || startPort > 65535 ){
			throw new IllegalArgumentException("Port cannot be greater that 65535 or less than zero");
		}

		//	 0.2 -- Make sure the protocol is valid
		if( protocol == null ){
			throw new IllegalArgumentException("The protocol cannot be null");
		}

		//	 0.3 -- Make sure the state is valid
		if( state == null ){
			throw new IllegalArgumentException("The socket state cannot be null");
		}

		//	 0.4 -- Make sure ports are in order
		if( startPort > endPort ){
			throw new IllegalArgumentException("The start port must not be greater than the end port");
		}
		
		
		// 1 -- Create the ports
		this.startPort = startPort;
		this.endPort = endPort;
		this.protocol = protocol;
		this.state = state;
	}
	
	public NetworkPortRange( int startPort, int endPort, Protocol protocol){
		this( startPort, endPort, protocol, SocketState.UNDEFINED);
	}
	
	public NetworkPortRange( int startPort, Protocol protocol, SocketState state){
		this( startPort, startPort, protocol, state);
	}
	
	public NetworkPortRange( int startPort, Protocol protocol){
		this( startPort, startPort, protocol, SocketState.UNDEFINED);
	}
	
	public boolean overlapsWith( NetworkPortRange range ){
		return overlapsWith(range, false);
	}
	
	public boolean overlapsWith( NetworkPortRange range, boolean ignoreState ){
		
		if( range.protocol != protocol || (ignoreState == false && range.state != state) ){
			return false;
		}
		
		/*
		 *  this : [\\\\\\]
		 *  range:    [\\\\\\]
		 */
		if( range.startPort <= endPort && range.startPort >= startPort ){
			return true;
		}
		/*
		 *  this :    [\\\\\\]
		 *  range: [\\\\\\]
		 */
		else if( range.startPort <= startPort && range.endPort >= startPort ){
			return true;
		}
		// Reject if the above two conditions did not match.
		else{
			return false;
		}
	}

	@Override
	public String toString(){
		StringBuffer result = new StringBuffer();
		
		if( this.protocol == Protocol.TCP){
			result.append("TCP\\");
		}
		else{
			result.append("UDP\\");
		}
		
		result.append(startPort);
		
		if( endPort > startPort ){
			result.append("-");
			result.append(endPort);
		}
		
		return result.toString();
	}
	
	public int getNumberOfPorts(){
		return endPort - startPort + 1;
	}
	
	public int getStartPort(){
		return startPort;
	}
	
	public int getEndPort(){
		return endPort;
	}
	
	public SocketState getState(){
		return state;
	}
	
	public Protocol getProtocol(){
		return protocol;
	}
	
	/**
	 * Takes a range and removes the parts that overlap with the rage provided as the second parameter.
	 * @param range
	 * @param subtractRange
	 * @return
	 */
	public static NetworkPortRange[] removeFromRange(NetworkPortRange range, NetworkPortRange subtractRange ){
		NetworkPortRange first = null;
		NetworkPortRange second = null;
		
		// If subtract range starts before the source range
		/*     [//////]
		 *                [/////////]
		 */
		if( range.getEndPort() < subtractRange.getStartPort() ){
			NetworkPortRange[] result = new NetworkPortRange[1];
			result[0] = range;
			return result;
		}
		
		// If subtract range starts after the source range
		/*              [///////]
		 *  [/////////]
		 */
		else if( range.getStartPort() > subtractRange.getEndPort() ){
			NetworkPortRange[] result = new NetworkPortRange[1];
			result[0] = range;
			return result;
		}
		
		// If subtract range starts in the middle of the source range
		/*     [////...
		 *   [///////////////]
		 */
		else if( subtractRange.getStartPort() <= range.getStartPort() && subtractRange.getEndPort() >= range.getEndPort() ){
			first = null;
			second = null;
		}
		
		// If subtract range starts in the middle of the source range
		/*     [////...
		 *         [/////////]
		 */
		else if( range.getEndPort() >= subtractRange.getStartPort() ){
			
			// The first part of the range is null if the subtraction range removes the start of the range
			if(  subtractRange.getStartPort() <= range.getStartPort() ){
				first = null;
			}
			else if( range.getEndPort() >= subtractRange.getStartPort() ){
				first = new NetworkPortRange( range.getStartPort(), (subtractRange.getStartPort() - 1), range.protocol );
			}
			
			// If subtract range starts in the middle and ends in the middle of the source range
			/*     [////........./////]
			 *         [/////////]
			 */
			if( range.getEndPort() > (  subtractRange.getEndPort() ) ){
				second =  new NetworkPortRange( subtractRange.getEndPort() + 1, range.getEndPort(), range.protocol);
			}
		}
		
		// If subtract range starts in the middle of the source range
		/*                 ...///]
		 *         [/////////]
		 */
		else {//if( range.getEndPort() < subtractRange.getStartPort() ){
			first = new NetworkPortRange( subtractRange.getStartPort() + 1, range.getEndPort(), range.protocol );
		}

		if( first == null && second != null ){
			NetworkPortRange[] result = new NetworkPortRange[1];
			result[0] = second;
			return result;
		}
		else if( first != null && second != null ){
			NetworkPortRange[] result = new NetworkPortRange[2];
			result[0] = first;
			result[1] = second;
			return result;
		}
		else if( first != null && second == null ){
			NetworkPortRange[] result = new NetworkPortRange[1];
			result[0] = first;
			return result;
		}
		else{
			NetworkPortRange[] result = new NetworkPortRange[0];
			return result;
		}
		
	}
	
	public static String convertToString( NetworkPortRange[] ports ){
		
		if( ports == null){
			return "";
		}
		else{
			StringBuffer result = new StringBuffer();
			
			for (int c = 0; c < ports.length; c++){
				if( c > 0 ){
					result.append("\n");
				}
				
				result.append(ports[c].toString());
			}
			
			return result.toString();
		}
	}
	
	/**
	 * Parse the list of ports and port ranges in the string.
	 * @param portsList
	 * @throws LineParseException 
	 */
	public static NetworkPortRange[] parseRange( String portsList ) throws LineParseException{
		return parseRange(SocketState.UNDEFINED, portsList);
	}
	
	/**
	 * Parse the list of ports and port ranges in the string.
	 * @param state
	 * @param portsList
	 * @throws LineParseException 
	 */
	public static NetworkPortRange[] parseRange( SocketState state, String portsList ) throws LineParseException{
		
		// 1 -- Create the list of ports
		int entries = 0;
		String currentLine;
		NetworkPortRange[] portsListArray= new NetworkPortRange[32];
        
		BufferedReader reader = new BufferedReader( new StringReader( portsList ) );
		
		try {
			
			int lineNumber = 0;
			
			//	1.1 -- Split the list by endlines
			while ((currentLine = reader.readLine()) != null) {
				
				currentLine = currentLine.trim();
				
				// Only try to extract the entry if the string is not empty
				if (currentLine.length() > 0){
					Matcher range = REGEX_RANGE_PROTOCOL.matcher(currentLine);
					Matcher port = REGEX_PORT_PROTOCOL.matcher(currentLine);
					Protocol protocol;
					
					
					
					//	Resize the array if necessary
					if( entries >= portsListArray.length ){
						portsListArray = (NetworkPortRange[])GenericUtils.resizeArray(portsListArray, portsListArray.length + 16);
					}
					
					// 1.2 -- Determine what the type of the string is (range or single entry)
					if( range.matches() ){
						
						if( range.group(1).equals("UDP") )
							protocol = Protocol.UDP;
						else
							protocol = Protocol.TCP;
						
						int start = Integer.parseInt( range.group(3) );
						int end = Integer.parseInt( range.group(4) );
						
						portsListArray[entries] = new NetworkPortRange(start, end, protocol, state);
					}
					else if( port.matches() ){
						if( port.group(1).equals("UDP") )
							protocol = Protocol.UDP;
						else
							protocol = Protocol.TCP;
						
						portsListArray[entries] = new NetworkPortRange(Integer.parseInt(port.group(3)), protocol, state);
					}
					else{
						throw new LineParseException("The port number is invalid", lineNumber);
					}
					
					entries++;
				}
				
				lineNumber++;
			}

		} catch(IOException e) {
			e.printStackTrace();
		}
		
		
		// 2 -- Resize the array to cut out empty entries
		if( entries < portsListArray.length ){
			portsListArray = (NetworkPortRange[])GenericUtils.resizeArray(portsListArray, entries);
		}

		return portsListArray;
	}
	
	/**
	 * Parse the list of ports and port ranges in the string.
	 * @param state
	 * @param protocol
	 * @param portsList
	 * @throws LineParseException 
	 */
	public static NetworkPortRange[] parseRange( Protocol protocol, SocketState state, String portsList) throws LineParseException{
		
		// 1 -- Create the list of ports
		int entries = 0;
		String currentLine;
		NetworkPortRange[] portsListArray= new NetworkPortRange[32];
        
		BufferedReader reader = new BufferedReader( new StringReader( portsList ) );
		
		try {
			
			int lineNumber = 0;
			
			//	1.1 -- Split the list by endlines
			while ((currentLine = reader.readLine()) != null) {
				
				currentLine = currentLine.trim();
				
				// Only try to extract the entry if the string is not empty
				if (currentLine.length() > 0){
					Matcher range = REGEX_RANGE.matcher(currentLine);
					Matcher port = REGEX_PORT.matcher(currentLine);
					
					//	Resize the array if necessary
					if( entries >= portsListArray.length ){
						portsListArray = (NetworkPortRange[])GenericUtils.resizeArray(portsListArray, portsListArray.length + 16);
					}
					
					// 1.2 -- Determine what the type of the string is (range or single entry)
					if( range.matches() ){
						int start = Integer.parseInt( currentLine.substring( 0, currentLine.indexOf("-") ).trim() );
						int end = Integer.parseInt( currentLine.substring( currentLine.indexOf("-") + 1, currentLine.length()).trim() );
						
						portsListArray[entries] = new NetworkPortRange(start, end, protocol, state);
					}
					else if( port.matches() ){
						portsListArray[entries] = new NetworkPortRange(Integer.parseInt(currentLine), protocol, state);
					}
					else{
						throw new LineParseException("The port number is invalid", lineNumber);
					}
					
					entries++;
				}
				
				lineNumber++;
			}

		} catch(IOException e) {
			e.printStackTrace();
		}
		
		
		// 2 -- Resize the array to cut out empty entries
		if( entries < portsListArray.length ){
			portsListArray = (NetworkPortRange[])GenericUtils.resizeArray(portsListArray, entries);
		}

		return portsListArray;
	}
	
	/**
	 * Finds a port/protocol combination that matches one of the entries in the given NetworkPortRange and returns the SocketState associated with the entry matched. Null is returned of no entry to found.
	 * @param port
	 * @param protocol
	 * @param state
	 * @param portRange
	 * @return
	 */
	public static SocketState find( int port, Protocol protocol, NetworkPortRange[] portRange ){

		// 1 -- Perform the operation
		return findInternal(port, protocol, null, portRange);
	}
	
	/**
	 * Determines if the given port information is contained within the port range.
	 * 
	 * @param port
	 * @param protocol
	 * @param state
	 * @param portRange
	 * @return
	 */
	public static boolean isWithin( int port, Protocol protocol, SocketState state, NetworkPortRange[] portRange ){
		
		// 0 -- Precondition check
		if( state == null ){
			throw new IllegalArgumentException("The socket state cannot be null");
		}
		
		// 1 -- Perform the operation
		return (findInternal(port, protocol, state, portRange) != null);
	}
	
	/**
	 * This function returns a list of ports scanned along with the state the port was observed in at the time of the scan. The information returned is induced from the
	 * scan results. For example, if ports TCP/1-5 are scanned, TCP/4 is expected open, and TCP/5 is flagged a deviation, then the following can be proven through inductive reasoning:
	 * <ol>
	 * 	<li>TCP/1-3 are closed (since only TCP/4 is expected open and since TCP/1-3 are not flagged as deviating from the expected</li>
	 * 	<li>TCP/4 is open (since TCP/4 is expected open did not deviate from the expected</li>
	 *  <li>TCP/5 is open (this port was identified as deviation and  only TCP/4 is expected open; therefore, this port must have been observed in an open state)</li>
	 * </ol>
	 * @param deviations
	 * @param scanned
	 * @param expectedOpen
	 * @return
	 */
	public static NetworkPortRange[] computeScannedResultRange( NetworkPortRange[] deviations, NetworkPortRange[] scanned, NetworkPortRange[] expectedOpen ){
		
		// 1 -- Create a copy of the scanned range and sort it (the copy is made so that the sort does not edit the original)
		NetworkPortRange[] tmp = new NetworkPortRange[scanned.length];
		
		System.arraycopy(scanned, 0, tmp, 0, scanned.length);
		sort(tmp);
		
		// 2 -- Compute the result
		Vector<NetworkPortRange> result = new  Vector<NetworkPortRange>();
		
		NetworkPortRange prior = null;
		
		for( int c = 0; c < scanned.length; c++ ){
			
			int curPort = scanned[c].getStartPort();
			
			//Look at each port/protocol combination and compute the correct result entry
			while( curPort >= scanned[c].getStartPort() && curPort <= scanned[c].getEndPort() ){
				SocketState newSocketState = null;
				
				// If in the deviations list, then include the status from the deviations list
				SocketState inDeviations = find(curPort, scanned[c].getProtocol(), deviations );
				if( inDeviations != null ){
					newSocketState = inDeviations;
				}
				// otherwise, if it is in the expected open list, then include it as closed
				else if( isWithin(curPort, scanned[c].getProtocol(), expectedOpen) ){
					newSocketState = SocketState.OPEN;
				}
				// otherwise, it is closed
				else{
					newSocketState = SocketState.CLOSED;
				}
				
				if( prior == null ){ //This occurs during the first run
					prior = new NetworkPortRange( scanned[c].getStartPort(), scanned[c].getStartPort(), scanned[c].getProtocol(), newSocketState );
					result.add( prior );
				}
				//Determine if incremented the prior entry accounts for this entry...
				else if( prior.state == newSocketState
					&& prior.protocol == scanned[c].getProtocol()
					&& (prior.getEndPort() + 1) == curPort ){
					
					prior.endPort += 1;
				}
				//...otherwise, add a new entry
				else{
					result.add( new NetworkPortRange( curPort, curPort, scanned[c].getProtocol(), newSocketState ) );
					prior = result.get(result.size() - 1);
				}
				curPort++;
			}
		}
		
		// 3 -- Convert the vector to an array
		NetworkPortRange[] resultArray =  new NetworkPortRange[result.size()];
		result.toArray( resultArray );
		
		return resultArray;
	}
	
	/**
	 * Sorts the entries in the port range according to the following rules:
	 * <ol>
	 * 	<li>UDP ranges come before TCP</li>
	 * 	<li>If the protocols match (both TCP or both UDP), then the range with the lower start port comes first</li>
	 * </ol>
	 * @param range
	 */
	public static void sort( NetworkPortRange[] range ){
		
		// 0 -- Precondition check
		if( range == null ){
			throw new IllegalArgumentException("The range cannot be null");
		}
		
		
		// 1 -- Sort the network port range
		Arrays.sort(range, new NetworkPortRangeComparator());
	}
	
	/**
	 * The following class is used for comparing two NetworkPortRange classes. The classes are sorted according to the following rules:
	 * <ol>
	 * 	<li>UDP ranges come before TCP</li>
	 * 	<li>If the protocols match (both TCP or both UDP), then the range with the lower start port comes first</li>
	 * </ol> 
	 * @author Luke Murphey
	 *
	 */
	private static class NetworkPortRangeComparator implements Comparator<NetworkPortRange> {
		
		public int compare(NetworkPortRange range1, NetworkPortRange range2) {

			if (range1.protocol == Protocol.UDP && range2.protocol == Protocol.TCP ) {
	            return -1; //range1 is before range2
			}
			else if (range1.protocol == Protocol.TCP && range2.protocol == Protocol.UDP ) {
	            return 1; //range2 is before range1
			}
			else if (range1.startPort <= range2.startPort ) {
	            return -1; //range1 is before range2
	        }
			else{
				return 1; //range2 is before range1
			}
	    }
	}
	
	/**
	 * Determines if the given port information is contained within the port range.
	 * 
	 * @param port
	 * @param protocol
	 * @param portRange
	 * @return
	 */
	public static boolean isWithin( int port, Protocol protocol, NetworkPortRange[] portRange ){
		return (findInternal(port, protocol, null, portRange) != null );
	}
	
	/**
	 * Determines if the given port information is contained within the port range and returns the entry that matches. Note the SocketState is ignored if the corresponding state argument is set to null (meaning that the socket state does not need to be equal). 
	 * @param port
	 * @param protocol
	 * @param state
	 * @param portRange
	 * @return
	 */
	private static SocketState findInternal( int port, Protocol protocol, SocketState state, NetworkPortRange[] portRange ){
		
		// 0 -- Precondition check
		if( portRange == null){
			throw new IllegalArgumentException("The network port range to compare to cannot be null");
		}
		
		if( protocol == null){
			throw new IllegalArgumentException("The port cannot be null");
		}
		
		if( port < 0 || port > 65535){
			throw new IllegalArgumentException("The network port is invalid (must be within 0-65535)");
		}
		
		// 1 -- Determine if the port is within range
		for( int c = 0; c < portRange.length; c++){
			if( protocol == portRange[c].protocol && portRange[c].startPort >= port && portRange[c].endPort <= port && (state == null || state == portRange[c].state ) ){
				return portRange[c].state;
			}
		}
		
		return null;
	}
	
}
