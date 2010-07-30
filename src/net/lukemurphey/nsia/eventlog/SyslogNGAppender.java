package net.lukemurphey.nsia.eventlog;

import net.lukemurphey.nsia.eventlog.EventLog.LoggerErrorHandler;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ErrorCode;

import java.util.Iterator;
import java.util.Vector;
import java.io.*;
import java.net.*;

public class SyslogNGAppender extends AppenderSkeleton {

	public enum Protocol{
		TCP, UDP
	}
	
	//The protocol used to transmit the log message
	private Protocol protocol = null;
	
	//The server to submit the log messages to
	private String server = null;
	
	//The default port to use for sending the log messages
	private int port = 514;
	
	//The cache of the log messages
	private Vector<LoggingEvent> messageCache = new Vector<LoggingEvent>();
	
	//The socket to use for sending the log messages
	private SocketWrapper socket = null;
	
	//The timestamp of the last connection attempt
	private long lastConnectionAttempt= -1;
	
	//The delay between a connection failure and an attempt to transmit the log messages again
	private static int RECONNECT_DELAY = 60000;
	
	//The maximum size of the log message buffer
	private int maxBufferSize = 8000;
	
	//Indicates the connection failed
	private boolean connectionErrorNoted = false;
	
	//Indicates whether the cache maximum size was reached
	private boolean maxCacheErrorNoted = false;
	
	/**
	 * This class wraps the different types of sockets such that the API remains the same to the class. This helps reduce the complexity of the surrounding class.
	 * @author Luke Murphey
	 *
	 */
	private static class SocketWrapper{

		//The TCP socket
		private Socket tcpSocket = null;
		
		//The UDP socket
		private DatagramSocket udpSocket = null;
		
		//Default port to use
		private int udpPort = 514;
		
		//The address to send the logs to
		private InetAddress udpAddress = null;
		
		private SocketWrapper(){
			//Only instantiable within this class
		}
		
		/**
		 * Create a TCP socket to the log server.
		 * @param serverAddress
		 * @param port
		 * @return
		 * @throws UnknownHostException
		 * @throws IOException
		 */
		public static SocketWrapper createTCPSocket(String serverAddress, int port) throws UnknownHostException, IOException{
			SocketWrapper wrapper = new SocketWrapper();
			wrapper.tcpSocket = new Socket(serverAddress, port);
			return wrapper;
		}
		
		/**
		 * Create a UDP socket to the log server.
		 * @param serverAddress
		 * @param port
		 * @return
		 * @throws UnknownHostException
		 * @throws IOException
		 */
		public static SocketWrapper createUDPSocket(String serverAddress, int port) throws UnknownHostException, IOException{
			SocketWrapper wrapper = new SocketWrapper();
			
			InetAddress address = InetAddress.getByName(serverAddress);
			wrapper.udpAddress = address;
			wrapper.udpPort = port;
			wrapper.udpSocket = new DatagramSocket();
			
			return wrapper;
		}
		
		/**
		 * Close the socket connection.
		 * @throws IOException
		 */
		public void close() throws IOException{
			if( tcpSocket != null ){
				tcpSocket.close();
			}
			
			if(udpSocket != null){
				udpSocket.close();
			}
		}
		
		/**
		 * Get the socket type being used.
		 * @return
		 */
		public Protocol socketType(){
			if( tcpSocket != null ){
				return Protocol.TCP;
			}
			else if(udpSocket != null){
				return Protocol.UDP;
			}
			else{
				return null;
			}
		}
		
		/**
		 * Send the given log message to the server.
		 * @param message
		 * @return
		 * @throws IOException
		 */
		public boolean sendMessage(String message) throws IOException{
			byte[] bytes = message.getBytes();
			
			if( tcpSocket != null ){
				OutputStream outStream = tcpSocket.getOutputStream();
				outStream.write(bytes);
				/*Flush the message ASAP since TCP syslog generally assumes that a single packet will contain the entire message.
				Failure to flush the message means that another message may be truncated to this one in a single packet; this may
				be interpreted as a single log event by the receiver*/
				outStream.flush();
				return true;
			}
			else if(udpSocket != null ){
				DatagramPacket packet = new DatagramPacket(bytes, bytes.length, udpAddress, udpPort);
				udpSocket.send(packet);
				return true;
			}
			
			return false;
		}
	}
	
	public SyslogNGAppender( String server, int port, Protocol protocol){
		setPort(port);
		setServer(server);
		setProtocol(protocol);
	}
	
	public SyslogNGAppender( String server, int port, int maxCacheSize, Protocol protocol){
		setPort(port);
		setServer(server);
		setMaxCacheSize( maxCacheSize );
		setProtocol(protocol);
	}

	/**
	 * Set the maximum size of the cache (before discarding the messages)
	 * @param maxCache
	 */
	public void setMaxCacheSize( int maxCache ){
		if( maxCache >= 0 ){
			maxBufferSize = maxCache;
		}
		else{
			throw new IllegalArgumentException("The maximum message cache size must be greater than or equal to zero");
		}
	}
	
	/**
	 * Set the port to send the log messages to.
	 * @param port
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Set the protocol to use to send the log messages to the server.
	 * @param protocol
	 */
	private void setProtocol(Protocol protocol){
		if( protocol == null ){
			throw new IllegalArgumentException("The protocol cannot be null");
		}
		
		this.protocol = protocol;
	}
	
	/**
	 * Get the protocol to use to send the log messages to the server.
	 * @return
	 */
	public synchronized Protocol getProtocol(){
		return socket.socketType();
	}
	
	/**
	 * Get the port to use when sending the log messages to the server.
	 * @return
	 */
	public int getPort() {
		return this.port;
	}
	
	/**
	 * Set the server to send the log messages to.
	 * @param server
	 */
	public void setServer(String server){
		this.server = server;
	}
	
	/**
	 * Get the server to send the log messages to.
	 * @return
	 */
	public String getServer(){
		return server;
	}
	
	/**
	 * Get the maximum size of the cache (before discarding the messages)
	 * @return
	 */
	public int getMaxCacheSize(){
		return maxBufferSize;
	}
	
	/**
	 * Returns a boolean indicating if the max cache size was reached.
	 * @return
	 */
	public boolean maxCacheReached(){
		return maxCacheErrorNoted;
	}
	
	/**
	 * Returns a boolean indicating if the log server is accepting the log messages.
	 * @return
	 */
	public boolean isLogServerResponding(){
		return !connectionErrorNoted;
	}
	
	/**
	 * Get the number of messages currently cached.
	 */
	public int getCachedMessageCount(){
		return messageCache.size();
	}
	
	/**
	 * Activate the options defined.
	 */
	public synchronized void activateOptions() {
		try {
			if(protocol == Protocol.TCP){
				socket = SocketWrapper.createTCPSocket(server, port);
			}
			else{
				socket = SocketWrapper.createUDPSocket(server, port);
			}
			
		} catch (IOException e) {
			errorHandler.error("Failed to open server socket", e, LoggerErrorHandler.LOG_SERVER_CONNECTION_NOT_ESTABLISHED);
		}
	}

	@Override
	protected void append(LoggingEvent event) {
		
		// 1 -- Try to clear the message cache
		if( messageCache.size() > 0 ){
			
			Iterator<LoggingEvent> it = messageCache.iterator();
			boolean cacheTransmissionStarted = false;
			
			while(it.hasNext()){
				
				if( sendEvent(it.next()) ){
					if( cacheTransmissionStarted == false ){
						cacheTransmissionStarted = true;
						if( messageCache.size() > 1 ){
							errorHandler.error( "" + messageCache.size() + " log messages have been cached due to connectivity issues with the log server; the cache will now be forwarded to the log server.", null, LoggerErrorHandler.LOG_CACHE_EMPTYING ); 
						}
					}
					it.remove();
				}
				else{
					// Clearing the message cache failed, therefore, cache the next even since the destination is not currently receiving events 
					addToCache(event);					
					return;
				}
			}
			
			if(messageCache.size() == 0){
				errorHandler.error("Log cache has been cleared; all cached log messages were sent to the log server.", null, LoggerErrorHandler.LOG_CACHE_EMPTY);
			}
		}
		
		// 2 -- Send the actual event
		if( sendEvent(event) == false ){
			addToCache(event);
		}

	}
	
	/**
	 * Add the given log message to the cache.
	 * @param event
	 */
	private void addToCache( LoggingEvent event ){
		if( messageCache.size() < maxBufferSize ){
			messageCache.add(event);
		}
		else{
			//Discard event, the cache has hit the limit
			if(maxCacheErrorNoted == false){
				maxCacheErrorNoted = true;
				errorHandler.error("The log cache has reached its maximum size (" + maxBufferSize + "), messages will be discarded until log server connectivity is restored.", null, LoggerErrorHandler.LOG_CACHE_FULL);
			}
		}
	}
	
	/**
	 * Submit the given log message.
	 * @param event
	 * @return
	 */
	private synchronized boolean sendEvent(LoggingEvent event){
		try{

			// If a layout doesn't exist then stop
			if (this.layout == null) {
				errorHandler.error("No layout for appender " + name, null, ErrorCode.MISSING_LAYOUT);
				return false;
			}

			// If the socket was closed then stop
			if ( socket == null ){
				if( lastConnectionAttempt < (System.currentTimeMillis() - RECONNECT_DELAY)) {
					try{
						if( protocol == Protocol.TCP){
							socket = SocketWrapper.createTCPSocket(server, port);
						}
						else{
							socket = SocketWrapper.createUDPSocket(server, port);
						}
					}
					catch(UnknownHostException e){
						if( connectionErrorNoted == false ){
							errorHandler.error("Log server host address could not be resolved: " + e.getMessage(), null, LoggerErrorHandler.LOG_SERVER_CONNECTION_NOT_ESTABLISHED);
							connectionErrorNoted = true;
						}
					} catch (IOException e) {
						if( connectionErrorNoted == false ){
							errorHandler.error("Socket connection to the log server could not be created: " + e.getMessage(), null, LoggerErrorHandler.LOG_SERVER_CONNECTION_NOT_ESTABLISHED);
							connectionErrorNoted = true;
						}
					}

					lastConnectionAttempt = System.currentTimeMillis();

					if( socket == null ){
						return false;
					}
				}
				else{
					return false;
				}
			}

			// Get a string version of the message. Note that endlines are replaced with "\n"
			String message = "<" + (event.getLevel().getSyslogEquivalent() + 8) + ">" + StringUtils.replace(this.layout.format(event), "\n", "\\n") + "\r\n" ;

			// Send the event message down the TCP socket
			try{
				socket.sendMessage(message);
				
				if( connectionErrorNoted ){
					errorHandler.error("Log server connection re-established", null, LoggerErrorHandler.LOG_SERVER_AVAILABLE);
				}
				connectionErrorNoted = false;
			}
			catch(IOException e){
				errorHandler.error("Logs could not be sent to the log server for the following reason: " + e.getMessage(), null, ErrorCode.WRITE_FAILURE);

				try{
					socket.close();
				}
				catch(IOException ex){
					//This is likely to be thrown since the socket is no longer responding, just ignore it and close the socket.
				}

				socket = null;
				return false;
			}

			return true;

		}
		catch(Exception e){
			errorHandler.error("Exception thrown while attempting to send data to the log server", e, LoggerErrorHandler.LOG_MESSAGE_SEND_FAILED);
			
			return false;
		}
	}

	@Override
	public void close() {
		try {
			if (socket == null){ // Already closed.
				return;
			}
			else{
				socket.close();
			}
			
		} catch (Exception e) {
			errorHandler.error("Exception thrown while attempting to close connection", e, ErrorCode.CLOSE_FAILURE);
		}
	}

	@Override
	public boolean requiresLayout() {
		return true;
	}

}
