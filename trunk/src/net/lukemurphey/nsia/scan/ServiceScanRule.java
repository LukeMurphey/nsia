package net.lukemurphey.nsia.scan;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoConnectorConfig;
import org.apache.mina.transport.socket.nio.SocketConnector;

//For the port scan handler
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.WorkerThread;
import net.lukemurphey.nsia.Application.DatabaseAccessType;
import net.lukemurphey.nsia.scan.NetworkPortRange.Protocol;
import net.lukemurphey.nsia.scan.NetworkPortRange.SocketState;

import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.ConnectException;
import java.net.PortUnreachableException;
import java.net.SocketTimeoutException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import java.sql.*;

import javax.script.ScriptException;

public class ServiceScanRule extends ScanRule implements WorkerThread {

	public static final NetworkPortRange[] WELL_KNOWN_PORTS = { new NetworkPortRange(1, 1023, NetworkPortRange.Protocol.TCP, NetworkPortRange.SocketState.UNDEFINED), new NetworkPortRange(1, 1023, NetworkPortRange.Protocol.UDP, NetworkPortRange.SocketState.UNDEFINED)};
	public static final NetworkPortRange[] COMMON_PORTS = {
		new NetworkPortRange(20, 23, NetworkPortRange.Protocol.TCP, NetworkPortRange.SocketState.UNDEFINED),
		new NetworkPortRange(25, 25, NetworkPortRange.Protocol.TCP, NetworkPortRange.SocketState.UNDEFINED),
		new NetworkPortRange(43, 43, NetworkPortRange.Protocol.TCP, NetworkPortRange.SocketState.UNDEFINED),
		new NetworkPortRange(53, 53, NetworkPortRange.Protocol.TCP, NetworkPortRange.SocketState.UNDEFINED),
		new NetworkPortRange(53, 53, NetworkPortRange.Protocol.UDP, NetworkPortRange.SocketState.UNDEFINED),
		new NetworkPortRange(68, 68, NetworkPortRange.Protocol.UDP, NetworkPortRange.SocketState.UNDEFINED),
		new NetworkPortRange(69, 69, NetworkPortRange.Protocol.UDP, NetworkPortRange.SocketState.UNDEFINED),
		new NetworkPortRange(79, 79, NetworkPortRange.Protocol.TCP, NetworkPortRange.SocketState.UNDEFINED),
		new NetworkPortRange(80, 80, NetworkPortRange.Protocol.TCP, NetworkPortRange.SocketState.UNDEFINED),
		new NetworkPortRange(110, 110, NetworkPortRange.Protocol.TCP, NetworkPortRange.SocketState.UNDEFINED),
		new NetworkPortRange(115, 115, NetworkPortRange.Protocol.TCP, NetworkPortRange.SocketState.UNDEFINED),
		new NetworkPortRange(119, 119, NetworkPortRange.Protocol.TCP, NetworkPortRange.SocketState.UNDEFINED),
		new NetworkPortRange(123, 123, NetworkPortRange.Protocol.UDP, NetworkPortRange.SocketState.UNDEFINED),
		new NetworkPortRange(137, 137, NetworkPortRange.Protocol.UDP, NetworkPortRange.SocketState.UNDEFINED),
		new NetworkPortRange(137, 137, NetworkPortRange.Protocol.TCP, NetworkPortRange.SocketState.UNDEFINED),
		new NetworkPortRange(138, 138, NetworkPortRange.Protocol.UDP, NetworkPortRange.SocketState.UNDEFINED),
		new NetworkPortRange(138, 138, NetworkPortRange.Protocol.TCP, NetworkPortRange.SocketState.UNDEFINED),
		new NetworkPortRange(139, 139, NetworkPortRange.Protocol.TCP, NetworkPortRange.SocketState.UNDEFINED),
		new NetworkPortRange(139, 139, NetworkPortRange.Protocol.UDP, NetworkPortRange.SocketState.UNDEFINED),
		new NetworkPortRange(143, 143, NetworkPortRange.Protocol.TCP, NetworkPortRange.SocketState.UNDEFINED),
		new NetworkPortRange(161, 161, NetworkPortRange.Protocol.TCP, NetworkPortRange.SocketState.UNDEFINED),
		new NetworkPortRange(161, 161, NetworkPortRange.Protocol.UDP, NetworkPortRange.SocketState.UNDEFINED),
		new NetworkPortRange(162, 162, NetworkPortRange.Protocol.TCP, NetworkPortRange.SocketState.UNDEFINED),
		new NetworkPortRange(162, 162, NetworkPortRange.Protocol.UDP, NetworkPortRange.SocketState.UNDEFINED),
		new NetworkPortRange(194, 194, NetworkPortRange.Protocol.TCP, NetworkPortRange.SocketState.UNDEFINED),
		new NetworkPortRange(194, 194, NetworkPortRange.Protocol.UDP, NetworkPortRange.SocketState.UNDEFINED),
		new NetworkPortRange(220, 220, NetworkPortRange.Protocol.TCP, NetworkPortRange.SocketState.UNDEFINED)
	};
	public static final NetworkPortRange[] ALL_PORTS = { new NetworkPortRange(0, 65535, NetworkPortRange.Protocol.TCP, NetworkPortRange.SocketState.UNDEFINED), new NetworkPortRange(0, 65535, NetworkPortRange.Protocol.UDP, NetworkPortRange.SocketState.UNDEFINED)};
	
	public static final int SCAN_ALL = 0;
	public static final int SCAN_COMMON = 1;
	public static final int SCAN_WELL_KNOWN = 2;
	public static final int SCAN_CUSTOM = 3;
	
	public static final String RULE_TYPE = "Service Scan";
	
	private String address;
	
	private NetworkPortRange[] portsExpectedOpen = null;
	private NetworkPortRange[] portsToBeScanned = null;
	
	private Exception exceptionThrown = null;
	
	private int numberOfPortsToScan = 0;
	private int currentPort = 0;
	private boolean continueExecuting = true;
	private State state = State.INITIALIZED;
	
	private ScanResult lastScanResult = null;
	
	private static class PortScanResult{
		public int port;
		public NetworkPortRange.Protocol protocol;
		public NetworkPortRange.SocketState state;
		public ConnectFuture connectFuture;
		
		public PortScanResult(int port, NetworkPortRange.Protocol protocol){
			this.port = port;
			this.protocol = protocol;
			
			this.connectFuture = null;
		}
		
	}
	
	//This object contains the scan results generated during an asynchronous scan
	private Vector<PortScanResult> currentScanResults;
	
	//The following class is responsible for the asynchronous port scanning
	private static class PortScanProtocolHandler extends IoHandlerAdapter {
		
		private ServiceScanRule scanRule;
		private int port;
		private NetworkPortRange.Protocol protocol;
		
		public PortScanProtocolHandler(ServiceScanRule scanRule, int port, NetworkPortRange.Protocol protocol ){
			// 0 -- Precondition check
			if( scanRule == null){
				throw new IllegalArgumentException("The reference to the service scan rule that executing the scan cannot be null (otherwise, the results cannot be reported back");
			}
			
			if( protocol == null){
				throw new IllegalArgumentException("The protocol cannot be null");
			}
			
			// 1 -- Initialize the class
			this.port = port;
			this.scanRule = scanRule;
			this.protocol = protocol;
		}
		
		@Override
		public void exceptionCaught(IoSession session, Throwable t) throws Exception {
			session.close();
		}
		
	    public void sessionOpened(IoSession session) {
	    	scanRule.setResult(port, NetworkPortRange.SocketState.OPEN, protocol);
	        session.close();
	    }

	    public void sessionClosed(IoSession session) {

	    }

	    public void sessionIdle(IoSession session, IdleStatus status) {
	        // Close the connection if reader is idle.
	        if (status == IdleStatus.READER_IDLE){
	            session.close();
	        }
	    }

	    public void messageReceived(IoSession session, Object message) {
	    	/*org.apache.mina.common.ByteBuffer buf = (org.apache.mina.common.ByteBuffer) message;
	        // Print out read buffer content.
	        while (buf.hasRemaining()) {
	            System.out.print((char) buf.get());
	        }
	        System.out.flush();*/
	    }
	}
	
	public ServiceScanRule(Application applicationResources) {
		super(applicationResources);
	}

	public ServiceScanRule(Application application, String address, NetworkPortRange[] expectedOpenPortRange, NetworkPortRange[] scanPortRange) {
		super(application);
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the address is valid
		if( address == null ){
			throw new IllegalArgumentException("The address to scan cannot be null");
		}
		
		if( address.isEmpty() ){
			throw new IllegalArgumentException("The address to scan cannot be empty");
		}
		
		//	 0.2 -- Make sure the expected port range is valid
		if( expectedOpenPortRange == null ){
			throw new IllegalArgumentException("The expected port range cannot be null");
		}
		
		//	 0.3 -- Make sure the scan port range is valid
		
		//	 	0.3.1 -- Make sure the range is not null 
		if( scanPortRange == null ){
			throw new IllegalArgumentException("The scan port range cannot be null");
		}
		
		//	 	0.3.2 -- Make sure the range is not empty
		if( scanPortRange.length == 0 ){
			throw new IllegalArgumentException("The scan port range cannot be empty");
		}
		
		//	 	0.3.3 -- Calculate the number of ports involved and make sure that at least one exists
		for(int c = 0; c < scanPortRange.length; c++ ){
			
			if( scanPortRange[c] == null ){
				throw new IllegalArgumentException("The scan port range contains a null entry");
			}
			
			numberOfPortsToScan += scanPortRange[c].getNumberOfPorts();
		}
		
		
		// 1 -- Set the data
		this.address = address;
		
		this.portsExpectedOpen = new NetworkPortRange[expectedOpenPortRange.length];
		this.portsToBeScanned = new NetworkPortRange[scanPortRange.length];
		
		System.arraycopy(expectedOpenPortRange, 0, this.portsExpectedOpen, 0, expectedOpenPortRange.length);
		System.arraycopy(scanPortRange, 0, this.portsToBeScanned, 0, scanPortRange.length);
	}
	
	public void delete() throws SQLException, NoDatabaseConnectionException {
		ScanRule.deleteRule( getRuleId());
	}

	public ScanResult doScan() throws ScanException {
		
		Timestamp timeOfScan = new Timestamp(System.currentTimeMillis());
		NetworkPortRange[] portDifferences = null;
		
		try{
			portDifferences = scanPortsAsync();
		}
		catch(IOException e){
			throw new ScanException("The scan failed due to an exception", e);
		}
		
		
		logScanResult(ScanResultCode.SCAN_COMPLETED, portDifferences.length, ServiceScanRule.RULE_TYPE, "", false);
		return new ServiceScanResult(ScanResultCode.SCAN_COMPLETED, timeOfScan, address, portsToBeScanned, portsExpectedOpen, portDifferences );
		
	}

	public NetworkPortRange[] getPortsToScan(){
		NetworkPortRange[] range = new NetworkPortRange[portsToBeScanned.length];
		
		System.arraycopy(portsToBeScanned, 0, range, 0, portsToBeScanned.length);
		return range;
	}
	
	public NetworkPortRange[] getPortsExpectedOpen(){
		NetworkPortRange[] range = new NetworkPortRange[portsExpectedOpen.length];
		
		System.arraycopy(portsExpectedOpen, 0, range, 0, portsExpectedOpen.length);
		return range;
	}
	
	public String getServerAddress(){
		return address;
	}
	
	public String getRuleType() {
		return ServiceScanRule.RULE_TYPE;
	}

	public String getSpecimenDescription() {
		return address;
	}
	
	public String toString(){
		return "Scanner : Port Scan (" + address + ")";
	}

	/**
	 * Save the rule to the database as a new entry and automatically generate a rule ID.
	 * @throws IllegalStateException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	public long saveToDatabase() throws IllegalStateException, SQLException, NoDatabaseConnectionException{
		if( scanRuleId == -1 ){
			throw new IllegalStateException("Scan rule must not be less than zero");
		}
		else{
			return saveToDatabaseEx( scanRuleId );
		}
	}
	
	/**
	 * Save the rule to the database as a new entry and automatically generate a rule ID. The rule will be associated 
	 * with the sitegroup identified in the argument.
	 * @throws IllegalStateException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	public long saveNewRuleToDatabase( long siteGroupId ) throws IllegalStateException, SQLException, NoDatabaseConnectionException{
		if( siteGroupId < 0 ){
			throw new IllegalArgumentException("Site group identifer must not be less than zero");
		}
		else{
			return saveNewRuleToDatabaseEx( siteGroupId );
		}
	}

	/**
	 * Save the rule to the database and associate with the given site group.
	 * @param siteGroupId
	 * @throws IllegalStateException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	private synchronized long saveNewRuleToDatabaseEx( long siteGroupId ) throws IllegalStateException, SQLException, NoDatabaseConnectionException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the scan rule ID is valid
		if( siteGroupId < -1 )
			throw new IllegalArgumentException("Site group ID is invalid (must not be less than zero)");
		
		//	 0.3 -- Make sure a database connection exists
		Connection connection = null;
		
		
		// 2 -- Save the current rule
		PreparedStatement statement = null;
		
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
			
			scanRuleId = createRule( siteGroupId, getScanFrequency(), ServiceScanRule.RULE_TYPE, RULE_STATE_ACTIVE );
			
			statement = connection.prepareStatement("Insert into ServiceScanRule (PortsOpen, PortsToScan, Server, ScanRuleID) values (?, ?, ?, ?)");
			statement.setString( 1, NetworkPortRange.convertToString( portsExpectedOpen ) );
			statement.setString( 2, NetworkPortRange.convertToString( portsToBeScanned ) );
			statement.setString( 3, address );
			statement.setLong( 4, scanRuleId );
			
			statement.execute();
			
			return scanRuleId;
			
		} finally {
			if (statement != null )
				statement.close();
			
			if (connection != null )
				connection.close();
		}
	}
	
	/**
	 * Save the rule to the database using the given scan rule ID.
	 * @param scanRuleId
	 * @throws IllegalStateException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	private synchronized long saveToDatabaseEx( long scanRuleId ) throws IllegalStateException, SQLException, NoDatabaseConnectionException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the scan rule ID is valid
		if( scanRuleId < 0 )
			throw new IllegalArgumentException("Scan rule ID is invalid (must not be less than zero)");
		
		//	 0.2 -- Make sure a database connection exists (will be check in try loop below)
		Connection connection = null;
		
		
		// 1 -- Save the current rule
		PreparedStatement statement = null;
		PreparedStatement generalStatement = null;
		
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.SCANNER);
			statement = connection.prepareStatement("Update ServiceScanRule set PortsOpen = ?, PortsToScan = ?, Server = ? where ScanRuleID = ?");
			statement.setString( 1, NetworkPortRange.convertToString( portsExpectedOpen ) );
			statement.setString( 2, NetworkPortRange.convertToString( portsToBeScanned ) );
			statement.setString( 3, address );
			statement.setLong( 4, scanRuleId );
			
			statement.executeUpdate();
			
			this.scanRuleId = scanRuleId; //Retain the latest scan rule ID
			
			
			// 2 -- Save the generic rule attributes
			generalStatement = connection.prepareStatement("Update ScanRule set ScanFrequency = ?, ScanDataObsolete = ? where ScanRuleID = ?");
			generalStatement.setInt( 1, this.getScanFrequency() );
			generalStatement.setBoolean( 2, true);
			generalStatement.setLong( 3, scanRuleId );
			generalStatement.executeUpdate();
			
			return this.scanRuleId;
			
		} finally {
			
			if (statement != null )
				statement.close();
			
			if (generalStatement != null )
				generalStatement.close();
			
			if (connection != null )
				connection.close();
		}
		
	}
	
	public boolean loadFromDatabase(long scanRuleId) throws NotFoundException, NoDatabaseConnectionException, SQLException, ScanRuleLoadFailureException {
		// 0 -- Precondition check
		if( scanRuleId < 0 )
			throw new IllegalArgumentException("The scan rule identifier must be greater than zero");
		
		
		// 1 -- Load the rule from the database
		Connection connection = null;
		PreparedStatement statement = null;
		PreparedStatement generalRuleStatement = null;
		ResultSet generalRuleResult = null;
		ResultSet result = null;
		
		try{

			connection = appRes.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			// 1.1 -- Load the general rule attributes
			generalRuleStatement = connection.prepareStatement("Select * from ScanRule where ScanRuleID = ?");
			generalRuleStatement.setLong(1, scanRuleId );
			generalRuleResult = generalRuleStatement.executeQuery();
			
			if( !generalRuleResult.next() ){
				return false;
			}
			
			int scanFrequency = generalRuleResult.getInt("ScanFrequency");
			this.scanFrequency = scanFrequency;
			super.scanRuleId = scanRuleId;
			
			// 1.2 -- Load the specific rule attributes
			statement = connection.prepareStatement( "Select * from ServiceScanRule where ScanRuleID = ?" );
			statement.setLong(1, scanRuleId);

			result = statement.executeQuery();

			if( !result.next() ){
				return false;
			}

			String portsOpen = result.getString("PortsOpen");
			String portsToScan = result.getString("PortsToScan");

			try{
				portsExpectedOpen = NetworkPortRange.parseRange(NetworkPortRange.SocketState.OPEN, portsOpen);
			}
			catch(LineParseException e){
				throw new ScanRuleLoadFailureException("The scan rule could not be loaded, the expected ports list is invalid", e);
			}

			try{
				portsToBeScanned = NetworkPortRange.parseRange(NetworkPortRange.SocketState.OPEN, portsToScan);
				
				for (NetworkPortRange range : portsToBeScanned) {
					numberOfPortsToScan += range.getNumberOfPorts();
				}
			}
			catch(LineParseException e){
				throw new ScanRuleLoadFailureException("The scan rule could not be loaded, the list of ports to scan is invalid", e);
			}

			address = result.getString("Server");

			return true;

		}finally{
			if( connection != null )
				connection.close();
			
			if( statement != null )
				statement.close();
			
			if( result != null )
				result.close();
			
			if( generalRuleStatement != null )
				generalRuleStatement.close();
			
			if( generalRuleResult != null )
				generalRuleResult.close();
		}
	}

	public ScanResult loadScanResult(long scanResultId) throws NotFoundException, NoDatabaseConnectionException, SQLException, ScanResultLoadFailureException {
		// 0 -- Precondition check
		if( scanResultId < 0 )
			throw new IllegalArgumentException("The scan result identifier must be greater than zero");
		
		return null;
	}
	
	private NetworkPortRange[] scanPortsAsync() throws IOException{
		
		synchronized(this){
			continueExecuting = true;
		}
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the expected array is not empty
		if( address == null || address.length() == 0 )
			throw new IllegalArgumentException("The address to scan must not be null or empty");
		
		//	 0.2 -- Make sure the expected array is not null
		if( portsExpectedOpen == null )
			throw new IllegalArgumentException("The expected port range must not be null");
		
		//	 0.3 -- Make sure the scan range array is not null
		if( portsToBeScanned == null )
			throw new IllegalArgumentException("The port range to be scanned must not be null");
		
		//	 0.4 -- Make sure the scan range array is not empty
		if( portsToBeScanned.length == 0 )
			throw new IllegalArgumentException("The port range to be scanned must have at least one entry");
		
		
		// 1 -- Perform the port scan
		
		//	 1.1 -- Create the TCP/IP connector
        SocketConnector connector = new SocketConnector();
        
        //	 1.2 -- Set connect timeout
        ((IoConnectorConfig) connector.getDefaultConfig())
                .setConnectTimeout(30);

        //	 1.3 -- Create the object to contain the current scan objects
        Vector<ConnectFuture> inProgressScans = new Vector<ConnectFuture>();
        int asyncLimit = 200;
        
        //	 1.4 -- Create the object responsible for containing the results from the asynchronous scan
        this.currentScanResults = new Vector<PortScanResult>(100);
        
        //	 1.5 -- Start the scan
        for (NetworkPortRange port : portsToBeScanned) {
			
        	int numberOfPorts = port.getNumberOfPorts();
        	
        	for(int c = 0; c < numberOfPorts; c++){
        		
	        	// Make sure to exit if the scan was canceled
	        	if(continueExecuting == false){
	        		break;
	        	}
	        	
	        	// The maximum number of scan threads has been reached, wait on one to complete and then check the others
	        	if( inProgressScans.size() >= asyncLimit ){
	        		
	            	// Remove scan objects that are complete
	            	Iterator<ConnectFuture> iterator = inProgressScans.iterator();
	            	{
	            		while(iterator.hasNext()){
	    	        		ConnectFuture cf = iterator.next();
	    	        		processConnection(cf);
	    	        		
	    	        		iterator.remove();
	            		}
	            	}
	        	}
	        	
	        	// Add the scan object
	        	int currentPortNum = port.getStartPort()+c;
	        	PortScanResult resultToAdd = new PortScanResult(currentPortNum, port.getProtocol());
	        	this.currentScanResults.add( resultToAdd );
	        	
	        	ConnectFuture cf = connector.connect(new InetSocketAddress(
	        			this.address, currentPortNum), new PortScanProtocolHandler(this, currentPortNum, port.getProtocol()));
	        		
	        	inProgressScans.add(cf);
	        	resultToAdd.connectFuture = cf;
        	}

	}
        // All port scanners have been initiated, wait for them to complete
        for (ConnectFuture cf : inProgressScans) {
        	processConnection(cf);
		}
        
        // 2 -- Compile the results
        Vector<NetworkPortRange> deviations = new Vector<NetworkPortRange>();
        
        for (PortScanResult result : currentScanResults) {
        	boolean isExpectedOpen = false;
        	
        	// 2.1 -- Determine if the given port was found in the expected open list
	        for (NetworkPortRange expectedOpen : portsExpectedOpen) {
	        	if( expectedOpen.getNumberOfPorts() > 0 && result.port >= expectedOpen.getStartPort() && result.port <= expectedOpen.getEndPort()){
					 isExpectedOpen = true;
	        	}
			}
	        
	        // 2.2 -- If the port is expected to be open but it is not or if the port is closed but should be open, then insert an exception 
			 if( (result.state == NetworkPortRange.SocketState.OPEN && !isExpectedOpen) || (result.state == NetworkPortRange.SocketState.CLOSED && isExpectedOpen) ){
				 deviations.add( new NetworkPortRange(result.port, result.protocol, result.state) );
			 }
        }

        // 3 -- Convert the result to an array
        NetworkPortRange[] deviations_array = new NetworkPortRange[deviations.size()];
        deviations.toArray(deviations_array);
        state = State.STOPPED;
        return deviations_array;
	}
	
	private void processConnection( ConnectFuture cf ){
		synchronized (cf) {
			try{
				if( cf.getSession() == null && cf.isConnected() == false )
				{
					cf.wait(10000);
					cf.getSession();
				}
			}
			catch(InterruptedException e){
        		//Ignore this exception, the thread was likely was interrupted due to a request to cancel the scan 
        	}
			catch(RuntimeIOException e){
				
				if( e.getCause().getMessage().contains("timed out") ){
					setResult(cf, NetworkPortRange.SocketState.NO_RESPONSE);
				}
				else if( e.getCause().getMessage().contains("refused") ){
					setResult(cf, NetworkPortRange.SocketState.CLOSED);
				}
				else {
					setResult(cf, NetworkPortRange.SocketState.NO_RESPONSE);
				}
			}
    	}
	}
	
	private void setResult(int port, SocketState state, Protocol protocol) {
		
		// 0 -- Precondition check
		if( state == null ){
			throw new IllegalArgumentException("State cannot be null");
		}
		
		// 1 -- Set the object
		for (PortScanResult scanResult : this.currentScanResults) {
			if( scanResult.port == port && scanResult.protocol == protocol ){
				scanResult.state = state;
				scanResult.connectFuture = null; //Allow the connection future to be garbage collected
				currentPort++;
				logScanResult(ScanResultCode.SCAN_COMPLETED, 0, ServiceScanRule.RULE_TYPE, null, false);//scanResult.protocol + "\"" + scanResult.port
			}
		}
	}
	
	private void setResult( ConnectFuture cf, NetworkPortRange.SocketState state){
		
		// 0 -- Precondition check
		if( state == null ){
			throw new IllegalArgumentException("State cannot be null");
		}
		
		// 1 -- Set the object
		for (PortScanResult scanResult : this.currentScanResults) {
			if( cf == scanResult.connectFuture ){
				scanResult.state = state;
				scanResult.connectFuture = null; //Allow the connection future to be garbage collected
				currentPort++;
				logScanResult(ScanResultCode.SCAN_COMPLETED, 0, ServiceScanRule.RULE_TYPE, null, false);//scanResult.protocol + "\"" + scanResult.port
			}
		}
	}
	
	/*private PortScanResult getResult(int port, Protocol protocol ){
		
		for (PortScanResult scanResult : this.currentScanResults) {
			if( scanResult.port == port && scanResult.protocol == protocol ){
				return scanResult;
			}
		}
		
		return null;
	}
	
	private PortScanResult getResult( ConnectFuture cf ){
		
		for (PortScanResult scanResult : this.currentScanResults) {
			if( cf == scanResult.connectFuture ){
				return scanResult;
			}
		}
		
		return null;
	}*/
	
	/**
	 * This method scans the port/protocol given to determine if it is open, closed or filtered.
	 * @param address
	 * @param port
	 * @param protocol
	 * @param timeoutMilliseconds
	 * @return
	 * @throws IOException
	 */
	public static NetworkPortRange.SocketState scanPort( String address, int port, NetworkPortRange.Protocol protocol, int timeoutMilliseconds) throws IOException{
		if( protocol == NetworkPortRange.Protocol.TCP )
			return scanPortTCP( address, port, timeoutMilliseconds);
		else
			return scanPortUDP( address, port, timeoutMilliseconds);
	}
	
	/**
	 * Scans the TCP ports to determine if it is open, closed or filtered.
	 * @param address
	 * @param port
	 * @param timeoutMilliseconds
	 * @return
	 * @throws IOException
	 */
	private static NetworkPortRange.SocketState scanPortTCP( String address, int port, int timeoutMilliseconds) throws IOException{
		Socket s = null;
        try
        {
             s = new Socket ();
             InetSocketAddress addr = new InetSocketAddress ( address, port );
             s.connect ( addr, timeoutMilliseconds );

             // If the source and destination ports are the same AND the target host
             // is localhost, the port will show up as open.  This can not be the case
             // because a system will allocate a port as a source port if that
             // port is already listening.  So we explicitly set the port as "closed".
             if ( s.getPort() == s.getLocalPort()
                    && address.startsWith ( "127." ) )
             {
            	 return NetworkPortRange.SocketState.CLOSED;
             }
             
             return NetworkPortRange.SocketState.OPEN;
        }
        catch ( ConnectException e )
        {
        	return NetworkPortRange.SocketState.CLOSED;
        }
        catch ( PortUnreachableException e )
        {
        	return NetworkPortRange.SocketState.CLOSED;
        }
        catch ( SocketTimeoutException e )
        {
        	return NetworkPortRange.SocketState.NO_RESPONSE;
        }
        finally
        {
        	s.close();
        }
	}
	
	/**
	 * Scans the UDP port to determine if it is open, closed or filtered.
	 * @param address
	 * @param port
	 * @param timeoutMilliseconds
	 * @return
	 * @throws IOException
	 */
	private static NetworkPortRange.SocketState scanPortUDP( String address, int port, int timeoutMilliseconds) throws IOException{
		DatagramSocket s = null;
		try
		{
			s = new DatagramSocket ();
			s.setSoTimeout ( timeoutMilliseconds );
			s.connect ( new InetSocketAddress ( address, port ) );
			
			byte[] b = { 0 };
			
			DatagramPacket dg = new DatagramPacket ( b, 0 );
			
			s.send ( dg );
			
			dg = new DatagramPacket ( b, 0 );
			s.receive ( dg );
			
			return NetworkPortRange.SocketState.OPEN;
		}
		catch ( PortUnreachableException pue )
		{
			return NetworkPortRange.SocketState.CLOSED;
		}
		catch ( SocketTimeoutException stoe )
		{
			return NetworkPortRange.SocketState.NO_RESPONSE;
		}
		finally
		{
			s.close ();
		}
	}

	public boolean canPause() {
		return false;
	}

	public int getProgress() {
		if( numberOfPortsToScan > 0 ){
			return 100 * currentPort / numberOfPortsToScan;
		}
		else{
			return -1;
		}
	}

	public State getStatus() {
		return state;
	}

	public String getStatusDescription() {
		State status = getStatus();
		if( status == State.INITIALIZED || status == State.STARTED || status == State.STARTING ){
			return "Scanned " + currentPort + " of " + numberOfPortsToScan + " ports";
		}
		else if( status == State.STOPPING){
			return "Service scan stopping";
		}
		else{ //WorkerThread.STATE_STOPPED
			return "Service scan stopped";
		}
	}

	public void pause() {
		//This thread cannot be paused; therefore, this method will do nothing
	}

	public boolean reportsProgress() {
		return true;
	}

	public void terminate() {
		synchronized(this){
			state = State.STOPPING;
			continueExecuting = false;
		}
	}

	public String getTaskDescription() {
		return RULE_TYPE + ":" + scanRuleId;
	}

	public ScanResult getLastScanResult(){
		return lastScanResult;
	}
	
	public void run() {
		try{
			state = State.STARTED;
			lastScanResult = this.doScan();
		}
		catch(ScanException e){
			exceptionThrown = e;
		}
	}

	public void setServerAddress(String serverAddress) {
		
		// 0 -- Precondition Check
		if( address == null ){
			throw new IllegalArgumentException("The address to scan cannot be null");
		}
		
		if( address.isEmpty() ){
			throw new IllegalArgumentException("The address to scan cannot be empty");
		}
		
		// 1 -- Set the item
		this.address = serverAddress;
	}

	public void setPortsToScan(NetworkPortRange[] portsToScan) {
		
		// 0 -- Precondition check
		if( portsToScan == null ){
			throw new IllegalArgumentException("The ports to scan cannot be null");
		}
		
		// 1 -- Set the item
		portsToBeScanned = new NetworkPortRange[portsToScan.length];
		System.arraycopy(portsToScan, 0, this.portsToBeScanned, 0, portsToBeScanned.length);
	}

	public synchronized boolean baseline() throws SQLException, NoDatabaseConnectionException, DefinitionSetLoadException, InputValidationException, ScriptException, IOException{
		Connection conn = null;
		
		try{
			// 0 -- Precondition check
			if( this.scanRuleId < 0 ){
				return false;
			}
			
			conn = this.appRes.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			
			// 1 -- Load the most current scan result
			ServiceScanResult result = (ServiceScanResult)ScanResultLoader.getLastScanResult(this.scanRuleId);
			
			
			// 2 -- Get the ports that should be added or removed to the expected list
			NetworkPortRange[] diff = result.getDifferences();
			
			Vector<NetworkPortRange> newExpectedOpen = new Vector<NetworkPortRange>();
			for (NetworkPortRange networkPortRange : this.portsExpectedOpen) {
				newExpectedOpen.add(networkPortRange);
			}
			
			Vector<NetworkPortRange> toRemoveFromExpectedOpen = new Vector<NetworkPortRange>();
			Vector<NetworkPortRange> toAddToExpectedOpen = new Vector<NetworkPortRange>();
			
			for (NetworkPortRange networkPortRange : diff) {
				
				// Port was found closed but was expected to be open
				if( networkPortRange.getState() == SocketState.CLOSED || networkPortRange.getState() == SocketState.NO_RESPONSE ){
					toRemoveFromExpectedOpen.add( networkPortRange );
				}
				else if( networkPortRange.getState() == SocketState.OPEN ){ // Port was found open but was expected to be closed
					toAddToExpectedOpen.add( networkPortRange );
				}
			}
			
			
			// 3 -- Create updated port ranges
			NetworkPortRange[] expectedOpen = result.getPortsExpectedOpen();
			Vector<NetworkPortRange> expectedOpenNew = new Vector<NetworkPortRange>();
			
			// Create a copy of the existing list
			for (NetworkPortRange networkPortRange : expectedOpen) {
				expectedOpenNew.add(networkPortRange);
			}
			
			// Add in the new ports
			for (NetworkPortRange networkPortRange : toAddToExpectedOpen) {
				expectedOpenNew.add(networkPortRange);
			}
			
			// Remove the ports no longer necessary
			Vector<NetworkPortRange> expectedOpenNew2 = new Vector<NetworkPortRange>();
			//expectedOpenNew2.addAll( expectedOpenNew );
			
			for (NetworkPortRange removeRange : toRemoveFromExpectedOpen) {
				for (NetworkPortRange existingRange : expectedOpenNew) {
					if( existingRange.overlapsWith(removeRange, true) ){
						NetworkPortRange[] ranges = NetworkPortRange.removeFromRange(existingRange, removeRange);
						
						for (NetworkPortRange splitRange : ranges) {
							expectedOpenNew2.add( splitRange );
						}
						
					}
					else{
						expectedOpenNew2.add( existingRange );
					}
				}
			}
			
			// 4 -- Update the rule
			NetworkPortRange portsExceptionOpenFile[] = new NetworkPortRange[expectedOpenNew2.size()];
			expectedOpenNew2.toArray(portsExceptionOpenFile);
			
			this.portsExpectedOpen = portsExceptionOpenFile;
			this.saveToDatabase();
			
			// 5 -- Set the scan data as obsolete
			ScanRule.setScanDataObsolete(this.scanRuleId);
			
		}
		catch(ScanResultLoadFailureException e){
			return false;
		}
		finally{
			if( conn != null ){
				conn.close();
			}
		}
		
		return true;
	}
	
	public void setPortsExpectedOpen(NetworkPortRange[] portsExpectedOpen) {
		
		// 0 -- Precondition check
		if( portsExpectedOpen == null ){
			throw new IllegalArgumentException("The ports to scan cannot be null");
		}
		
		// 1 -- Set the item
		this.portsExpectedOpen = new NetworkPortRange[portsExpectedOpen.length];
		System.arraycopy(portsExpectedOpen, 0, this.portsExpectedOpen, 0, this.portsExpectedOpen.length);
		
	}

	public Throwable getException() {
		return exceptionThrown;
	}

}
