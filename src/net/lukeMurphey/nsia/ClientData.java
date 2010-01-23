package net.lukemurphey.nsia;

import java.net.*;

/**
 * This class provides stores the information related to a client that connects to the manager. The class stores the address
 * that initiated the connection to the manager (connection address) and the user-agent information (if available). <p>Furthermore,
 * some systems may be intiating the connection on behalf of a remote client. For example, a web console will connect to the
 * manager and will be the reported source even though a client that has connected to the web console is the real source. This
 * information is relevant for auditing purposes and is thus stored here. However, the local device will not be able to determine
 * the IP and user agent data for the remote system. Thus, the console has the option of reporting information regarding the 
 * remote source.
 * <p>
 * Obviously, the manager cannot verify the information that a web console is reporting. Thus, the remote user information as 
 * reported from a connecting system should be considered potentially unreliable.  
 * @author luke
 *
 */
public class ClientData {
	private InetAddress remoteAddress;
	private InetAddress connectionAddress;
	
	private boolean intermediateAgentInvolved;
	
	private String remoteClientData;
	private String connectionClientData;
	
	/**
	 * Constructor for connections where the client is connecting directly. For example, in the case of a thick client on a users' desktop. 
	 * @param sourceAddress
	 * @param sourceClientData
	 */
	public ClientData( InetAddress sourceAddress, String sourceClientData ){
		// 0 -- Precondition check
		
		intermediateAgentInvolved = false;
		
		connectionAddress = sourceAddress;
		connectionClientData = sourceClientData;
		
		remoteClientData = sourceClientData;
		remoteAddress = sourceAddress;
	}
	
	
	/**
	 * Constructor for a system that is proxying a connection for another client and wishes to report information regarding the remote user.
	 * For example, a web console (the connecting source) can report the IP of the source that is connecting through it to get to the manager.
	 * The source that is initiating the connection to the web console would be defined as the "remote client."  
	 * @param sourceAddress
	 * @param sourceClientData
	 * @param remoteSourceAddress
	 * @param remoteClientData
	 */
	public ClientData( InetAddress sourceAddress, String sourceClientData, InetAddress remoteSourceAddress, String remoteClientData ){
		intermediateAgentInvolved = true;
		
		connectionAddress = sourceAddress;
		connectionClientData = sourceClientData;
		
		this.remoteClientData = remoteClientData;
		remoteAddress = remoteSourceAddress;
	}
	
	/**
	 * Get the address that initiated the connection to the application.
	 * @precondition None
	 * @postcondition The address of the connection initiator will be returned.
	 * @return
	 */
	public InetAddress getSourceAddress(){
		return connectionAddress;
	}
	
	/**
	 * Get the user-agent data of the client that initiated the connection to the application.
	 * @precondition None
	 * @postcondition The user-agent for the system that initiated a connection to the application will be returned.
	 * @return
	 */
	public String getSourceClientData(){
		return connectionClientData;
	}
	
	/**
	 * Get the address that of the remote client. This address will be the same as the source address if no remote client is reported from the connecting system.
	 * @precondition None
	 * @postcondition The address of the remote connection initiator will be returned.
	 * @return
	 */
	public InetAddress getRemoteSourceAddress(){
		return remoteAddress;
	}
	
	/**
	 * Get the user-agent data of the remote client. This value will be the same as the user-agent data from the source address if no remote client is reported from the connecting system.
	 * @precondition None
	 * @postcondition The user-agent for the remote connection initiator will be returned.
	 * @return
	 */
	public String getRemoteSourceClientData(){
		return remoteClientData;
	}
	
	/**
	 * Determines if an intermediate agent appears to be involved. Note that this value may not be accurate if the client is indicating inaccurate information.
	 * @precondition None
	 * @postcondition A boolean indicating if the connection is from a source that is initiating a connection on behalf of another client. 
	 * @return
	 */
	public boolean isIntermediateAgentInvolved(){
		return intermediateAgentInvolved;
	}
}
