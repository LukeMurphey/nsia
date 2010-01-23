package net.lukemurphey.nsia;

import org.mortbay.http.HttpContext;
import org.mortbay.http.SocketListener;
import org.mortbay.http.SslListener;

/*
 import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.webapp.WebAppContext;
*/

import org.mortbay.jetty.Server;


import java.net.BindException;


/**
 * The HTTP server serves the HTTP content (including both the XML-RPC handlers and HTML content providers).
 * @author luke
 *
 */
public class HttpServer {

	protected Server server;
	
	/**
	 * Start the HTTP server; the class will open a listener on the port specified.
	 * @param serverPort
	 * @param enableSSL
	 * @throws Exception 
	 * @throws Exception
	 */
	public void startServer( int serverPort, boolean enableSSL ) throws Exception, BindException{ // NOPMD by luke on 5/26/07 10:52 AM
		
		// 1 -- Create the server and configure the listener
		server = new Server();
		//SocketConnector listener; //For Jetty 6
		SocketListener listener; //For Jetty 5

		//Context root = new Context(server, "/", Context.SESSIONS);
		//root.addServlet(new ServletHolder(new DefaultServlet()), "/*")
		
		//	 1.1a -- Create the SSL listener
		if( enableSSL ){
			//SslSocketConnector sslListener = new SslSocketConnector(); //For Jetty 6
			SslListener sslListener = new SslListener(); // For Jetty 5
			
			// 1.1.1a -- Set keystore
			String sslKeystore = Application.getApplication().getApplicationConfiguration().getKeystore();
			if( sslKeystore != null )
				sslListener.setKeystore( sslKeystore );
			
			// 1.1.2a -- Set password
			String sslPassword = Application.getApplication().getApplicationConfiguration().getSslPassword();
			if( sslPassword != null )
				sslListener.setPassword( sslPassword );
			
			// 1.1.3a -- Set key password
			String sslKeyPassword = Application.getApplication().getApplicationConfiguration().getSslKeyPassword();
			if( sslKeyPassword != null )
				sslListener.setKeyPassword( sslKeyPassword );
			else if( sslPassword != null )
				sslListener.setKeyPassword( sslPassword );
			
			listener = sslListener;
		}
		
		//	 1.1b -- Create the non-SSL listener
		else{
			//listener = new SocketConnector(); //For Jetty 6
			listener = new SocketListener();
		}
		
		//	 1.2 -- Perform miscellaneous configuration changes
		//org.apache.commons.logging.LogFactory = null;
		/*LogFactory logFactory = new LogFactory(); //
		logFactory.getFactory().get */
		
		//	 1.3 -- Start the listening server
		listener.setPort( serverPort );
	 	
		//For Jetty 5:
		server.setRootWebApp("webConsole");
	 	server.addWebApplication( "/", "../lib/webConsole.war");
	 	server.addListener(listener);
	 	
		
		//For Jetty 6:
		/*
		WebAppContext wac = new WebAppContext();
		wac.setWar("lib/webConsole.war");
		wac.setContextPath("/"); 
		server.setHandler(wac);
		
	 	server.addConnector(listener);
	 	*/

	 	server.start();
	}
	
	/**
	 * Stop the HTTP server service.
	 * @throws InterruptedException
	 */
	public void shutdownServer() throws Exception{
		
		for (HttpContext context : server.getContexts()) {
			context.setStopGracefully(true);
			context.stop(true);
		}
		
		while( server.getConnections() > 0 ){
			Thread.sleep(1000);
			System.out.println("Waiting, connection count = " + server.getConnections() );
		}
		
		server.stop(true);
	}
}
