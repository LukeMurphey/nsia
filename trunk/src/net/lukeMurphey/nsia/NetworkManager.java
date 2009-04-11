package net.lukeMurphey.nsia;

import java.net.BindException;


/**
 * This class provides an information source and configuration point for the core application.
 * @author luke
 *
 */
public final class NetworkManager {
	private int serverPort = 8443;
	private boolean sslEnabled = false;
	
	private HttpServer webServer;
	private final Object httpServerMutex = new Object();

	public NetworkManager( ){
	}
	
	
	/**
	 * Set the TCP port that the manager should listen on and specify if SSL should be
	 * used.
	 * @param serverTcpPort
	 * @param enableSsl
	 */
	public void setServerPort( int serverTcpPort, boolean enableSsl ){
		// 0 -- Precondition check
		
		//	 0.1 -- Ensure server port is valid
		if( serverTcpPort < 1 )
			throw new IllegalArgumentException("Server port is invalid");
		
		// 1 -- Set the options
		serverPort = serverTcpPort;
		sslEnabled = enableSsl;
		//this.defaultDeny = defaultDeny;
	}
	
	/**
	 * Starts the master HTTP Listener
	 * @throws Exception
	 * @throws BindException
	 * @throws InterruptedException 
	 */
	public void startListener() throws BindException, Exception{ // NOPMD by luke on 5/26/07 11:10 AM
		synchronized (httpServerMutex){
			// 1 -- Shutdown the previous instances to prevent multiple listeners
			if( webServer != null )
				webServer.shutdownServer();
			
			// 2 -- Spawn the appropriate listener
			webServer = new HttpServer();
			
			// 3 -- Start the listener
			webServer.startServer( serverPort, sslEnabled);
		}
	}
	
	/**
	 * Stops the master HTTP Listener
	 */
	public void stopListener(){
		synchronized (httpServerMutex){
			try{
				if( webServer != null ){
					webServer.shutdownServer();
					webServer = null; // NOPMD by luke on 5/26/07 11:10 AM
				}
			}
			catch( InterruptedException e){
				//Ignore, InterruptedException don't matter in this context.
			}
			catch( Exception e){
				//Ignore, InterruptedException don't matter in this context.
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Indicates if the internal web services provider is running.
	 * @return
	 */
	public boolean isListenerRunning(){
		synchronized (httpServerMutex) {
			if( webServer == null )
				return false;
			else{
				return true;
			}
		}
	}
	
	/**
	 * Get the port that the manager is listening on.
	 * @return
	 */
	public int getServerPort(){
		return serverPort; 
	}
	
	/**
	 * Determines if the server is using SSL or not.
	 * @return
	 */
	public boolean sslEnabled(){
		return sslEnabled;
	}
	
}
