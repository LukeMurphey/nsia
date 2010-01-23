package net.lukemurphey.nsia.scan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.util.Vector;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.scan.ScanRule.ScanResultLoadFailureException;


public class ServiceScanResult extends ScanResult {

	private String address;
	private NetworkPortRange[] portsScanned;
	private NetworkPortRange[] portsExpectedOpen;
	private NetworkPortRange[] differencesObserved;
	
	private ServiceScanResult(ScanResultCode scanResultCode, Timestamp timeOfScan){
		super(scanResultCode, timeOfScan);
	}
	
	protected ServiceScanResult(ScanResultCode scanResultCode, Timestamp timeOfScan, String address, NetworkPortRange[] portsScanned, NetworkPortRange[] portsExpectedOpen, NetworkPortRange[] portDifferences) {
		super(scanResultCode, timeOfScan);
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the expected array is not empty
		if( address == null || address.length() == 0 )
			throw new IllegalArgumentException("The address to scan must not be null or empty");
		
		//	 0.2 -- Make sure the expected array is not null
		if( portsExpectedOpen == null )
			throw new IllegalArgumentException("The expected port range must not be null");
		
		//	 0.3 -- Make sure the scan range array is not null
		if( portsScanned == null )
			throw new IllegalArgumentException("The expected port range must not be null");
		
		//	 0.4 -- Make sure the scan range array is not empty
		if( portsScanned.length == 0 )
			throw new IllegalArgumentException("The expected port range must have at least one entry");
		
		//	 0.5 -- Make sure the differences range array is not null
		if( portDifferences == null )
			throw new IllegalArgumentException("The range of differences observed must not be null");
		
		
		// 1 -- Set the values
		this.address = address;
		this.portsScanned = portsScanned;
		this.portsExpectedOpen = portsExpectedOpen;
		this.differencesObserved = portDifferences;
		
		// Calculate the number of deviations
		deviations = 0;
		for(int c = 0; c < portDifferences.length; c++ ){
			
			if( portDifferences[c] != null ){
				deviations += portDifferences[c].getNumberOfPorts();
			}
		}
		
	}

	public String getRuleType() {
		return ServiceScanRule.RULE_TYPE;
	}

	public String getSpecimenDescription() {
		return address;
	}

	public long saveToDatabase(Connection connection, long scanRuleId) throws SQLException {
		// 0 -- Precondition check
		if( connection == null ){
			throw new IllegalArgumentException("The database connection cannot be null");
		}
		
		
		// 1 -- Save the main scan result record
		long scanResultId = saveToDatabaseInitial(connection, scanRuleId, ServiceScanRule.RULE_TYPE);
		
		
		// 2 -- Save the specific scan result record
		PreparedStatement statement = null;
		
		NetworkPortRange[] portsUnexpectedOpen;
		NetworkPortRange[] portsUnexpectedClosed;
		NetworkPortRange[] portsUnexpectedNotResponding;
		
		{
			Vector<NetworkPortRange> portsUnexpectedOpenVector = new Vector<NetworkPortRange>();
			Vector<NetworkPortRange> portsUnexpectedClosedVector = new Vector<NetworkPortRange>();
			Vector<NetworkPortRange> portsUnexpectedNotRespondingVector = new Vector<NetworkPortRange>();
			
			for (NetworkPortRange networkPortRange : differencesObserved) {
				if( networkPortRange.getState() == NetworkPortRange.SocketState.CLOSED){
					portsUnexpectedClosedVector.add(networkPortRange);
				}
				else if( networkPortRange.getState() == NetworkPortRange.SocketState.OPEN){
					portsUnexpectedOpenVector.add(networkPortRange);
				}
				else{
					portsUnexpectedNotRespondingVector.add(networkPortRange);
				}
			}
			
			portsUnexpectedOpen = new NetworkPortRange[portsUnexpectedOpenVector.size()];
			portsUnexpectedOpenVector.toArray(portsUnexpectedOpen);
			
			portsUnexpectedClosed = new NetworkPortRange[portsUnexpectedClosedVector.size()];
			portsUnexpectedClosedVector.toArray(portsUnexpectedClosed);
			
			portsUnexpectedNotResponding = new NetworkPortRange[portsUnexpectedNotRespondingVector.size()];
			portsUnexpectedNotRespondingVector.toArray(portsUnexpectedNotResponding);
		}
		
		try{
			statement = connection.prepareStatement("Insert into ServiceScanResult (ScanResultID, PortsExpectedOpen, PortsScanned, PortsUnexpectedClosed, PortsUnexpectedOpen, PortsUnexpectedNotResponding, Server) values(?, ?, ?, ?, ?, ?, ?)");
			statement.setLong(1, scanResultId);
			statement.setString(2, NetworkPortRange.convertToString( portsExpectedOpen ) );
			statement.setString(3, NetworkPortRange.convertToString( portsScanned ));
			statement.setString(4, NetworkPortRange.convertToString( portsUnexpectedClosed ));
			statement.setString(5, NetworkPortRange.convertToString( portsUnexpectedOpen ));
			statement.setString(6, NetworkPortRange.convertToString( portsUnexpectedNotResponding ));
			statement.setString(7, this.address);
			
			statement.execute();
		}
		
		finally{
			if( statement != null){
				statement.close();
			}
		}
		incompletes = 0;
		accepts = 0;
		for (NetworkPortRange scannedRange : portsScanned) {
			accepts += scannedRange.getNumberOfPorts();
		}
		
		accepts = accepts - deviations;
		
		saveToDatabaseFinalize(connection, scanResultId, deviations, accepts, incompletes, scanRuleId);
		return scanResultId;
	}

	protected static ServiceScanResult loadFromDatabase( long scanRuleId, long scanResultId, ScanResultCode resultCode, Timestamp scanTime, int deviations, int incompletes, int accepts ) throws SQLException, NoDatabaseConnectionException, ScanResultLoadFailureException{
		Application application = Application.getApplication();
		Connection connection = null;
		
		// 1 -- Load the basic scan result
		ServiceScanResult scanResult = new ServiceScanResult(resultCode, scanTime);
		scanResult.deviations = deviations;
		scanResult.incompletes = incompletes;
		scanResult.accepts = accepts;
		scanResult.ruleId = scanRuleId;
		scanResult.scanResultId = scanResultId;
		
		
		// 2 -- Load the class specific attributes
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try{
			// 2.1 -- Load the core attributes
			connection = application.getDatabaseConnection( Application.DatabaseAccessType.SCANNER );
			
			statement = connection.prepareStatement("Select * from ServiceScanResult where ScanResultID = ?");
			statement.setLong(1, scanResultId);
			
			result = statement.executeQuery();
			if( result.next() ){
				
				// 2.1.1 -- Get the server address
				scanResult.address = result.getString("Server");
				
				Vector<NetworkPortRange> differences = new Vector<NetworkPortRange>();
				
				// 2.1.2 -- Get the ports scanned
				try{
					scanResult.portsScanned = NetworkPortRange.parseRange(  result.getString("PortsScanned") );
				}
				catch(LineParseException e){
					throw new ScanResultLoadFailureException("The list of ports scanned could not be loaded", e);
				}
				
				// 2.1.3 -- Get the ports unexpectedly observed open
				try{
					NetworkPortRange portsObservedOpen[] = NetworkPortRange.parseRange( result.getString("PortsUnexpectedOpen") );
					
					for (NetworkPortRange networkPortRange : portsObservedOpen) {
						differences.add( new NetworkPortRange( networkPortRange.getStartPort(), networkPortRange.getEndPort(), networkPortRange.getProtocol(), NetworkPortRange.SocketState.OPEN ) );
					}
					
				}
				catch(LineParseException e){
					throw new ScanResultLoadFailureException("The list of ports that were observed unexpectedly open could not be loaded", e);
				}
				
				// 2.1.4 -- Get the the ports unexpectedly observed closed
				try{
					NetworkPortRange portsObservedClosed[] = NetworkPortRange.parseRange( result.getString("PortsUnexpectedClosed") );
					
					for (NetworkPortRange networkPortRange : portsObservedClosed) {
						differences.add( new NetworkPortRange( networkPortRange.getStartPort(), networkPortRange.getEndPort(), networkPortRange.getProtocol(),  NetworkPortRange.SocketState.CLOSED ) );
					}
				}
				catch(LineParseException e){
					throw new ScanResultLoadFailureException("The list of ports that were observed unexpectedly closed could not be loaded", e);
				}
				
				// 2.1.5 -- Get the the ports unexpectedly observed not responding
				try{
					NetworkPortRange portsObservedNotResponding[] = NetworkPortRange.parseRange( result.getString("PortsUnexpectedNotResponding") );
					
					for (NetworkPortRange networkPortRange : portsObservedNotResponding) {
						differences.add( new NetworkPortRange( networkPortRange.getStartPort(), networkPortRange.getEndPort(), networkPortRange.getProtocol(),  NetworkPortRange.SocketState.NO_RESPONSE ) );
					}
				}
				catch(LineParseException e){
					throw new ScanResultLoadFailureException("The list of ports that were observed unexpectedly not responding could not be loaded", e);
				}
				
				// 2.1.6 -- Get the ports expected open
				try{
					scanResult.portsExpectedOpen = NetworkPortRange.parseRange(  result.getString("PortsExpectedOpen") );
				}
				catch(LineParseException e){
					throw new ScanResultLoadFailureException("The list of ports expected open could not be loaded", e);
				}
				
				// 2.1.7 -- Compute the deviations count
				scanResult.deviations = 0;
				for (NetworkPortRange diff : differences) {
					scanResult.deviations = scanResult.deviations + diff.getNumberOfPorts();
				}
				
				NetworkPortRange[] diffArray = new NetworkPortRange[differences.size()];
				differences.toArray(diffArray);
				scanResult.differencesObserved = diffArray;
				
				//Mark up the ports scanned list with the information actually obtained from the scan
				scanResult.portsScanned = NetworkPortRange.computeScannedResultRange(diffArray, scanResult.portsScanned, scanResult.portsExpectedOpen);
			}
			
			// Done, return the result
			return scanResult;
		} finally {
			
			if (connection != null )
				connection.close();
			
			if (statement != null )
				statement.close();
			
			if (result != null )
				result.close();
		}
	}
	
	protected static NetworkPortRange[] computeDifferences(NetworkPortRange[] expectedOpen, NetworkPortRange[] scanned, NetworkPortRange[] observedClosed, NetworkPortRange[] observedOpen){
		
		Vector<NetworkPortRange> differences = new Vector<NetworkPortRange>();
		
		// 1 -- Identify open ports that are unexpectedly open
		for (NetworkPortRange portObservedopen : observedOpen) {
			
			for(int c = 0; c < portObservedopen.getNumberOfPorts(); c++){
				boolean isExpectedOpen = false;
				
				// 1.1 -- Determine if the port should be open
				for (NetworkPortRange portExpectedOpen : expectedOpen) {
					
					if( portExpectedOpen.getProtocol() == portObservedopen.getProtocol() && portExpectedOpen.getStartPort() >= (portObservedopen.getStartPort() + c) && portExpectedOpen.getStartPort() <= (portObservedopen.getStartPort() + c)){
						isExpectedOpen = true;
						break;
					}
				}
				
				// 1.2 -- Add the port to the differences list if it should not be open
				if( isExpectedOpen == false ){
					differences.add(new NetworkPortRange(portObservedopen.getStartPort() + c, portObservedopen.getProtocol(), NetworkPortRange.SocketState.OPEN));
				}
			}
		}
		
		// 2 -- Identify closed ports that are unexpectedly closed
		for (NetworkPortRange portObservedClosed : observedClosed) {
			
			for(int c = 0; c < portObservedClosed.getNumberOfPorts(); c++){
				boolean isExpectedOpen = false;
				
				// 1.1 -- Determine if the port should be open
				for (NetworkPortRange portExpectedOpen : expectedOpen) {
					
					if( portExpectedOpen.getProtocol() == portObservedClosed.getProtocol() && portExpectedOpen.getStartPort() >= (portObservedClosed.getStartPort() + c) && portExpectedOpen.getStartPort() <= (portObservedClosed.getStartPort() + c)){
						isExpectedOpen = true;
						break;
					}
				}
				
				// 1.2 -- Add the port to the differences list if it should not be closed
				if( isExpectedOpen == true ){
					differences.add(new NetworkPortRange(portObservedClosed.getStartPort() + c, portObservedClosed.getProtocol(), portObservedClosed.getState()));
				}
			}
		}
		
		// 3 -- Create the array
		NetworkPortRange differencesArray[] = new NetworkPortRange[differences.size()];
		
		differences.toArray( differencesArray );
		
		return differencesArray;
	}
	
	public NetworkPortRange[] getDifferences(){
		NetworkPortRange[] result = new NetworkPortRange[differencesObserved.length];
		System.arraycopy(differencesObserved, 0, result, 0, result.length);
		
		return result;
	}
	
	public NetworkPortRange[] getPortsScanned(){
		NetworkPortRange[] result = new NetworkPortRange[portsScanned.length];
		System.arraycopy(portsScanned, 0, result, 0, result.length);
		
		return result;
	}
	
	public NetworkPortRange[] getPortsExpectedOpen(){
		NetworkPortRange[] result = new NetworkPortRange[portsExpectedOpen.length];
		System.arraycopy(portsExpectedOpen, 0, result, 0, result.length);
		
		return result;
	}

}
