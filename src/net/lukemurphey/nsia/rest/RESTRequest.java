package net.lukemurphey.nsia.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * This class serves as a base class for REST operations.
 * @author Luke Murphey
 *
 */
public class RESTRequest {
	
	public final int SOCKET_TIMEOUT = 5 * 60 * 1000; // 5 minutes; this timeout applies to the time it takes for the socket to return data (not including the data to initiate the connection, see below for the connection timeout)
	public final int CONNECTION_TIMEOUT = 30 * 1000; // 30 seconds; this timeout applies to the time it takes to initiate a TCP connection to the manager
	public final int REQUEST_TIMEOUT = 30 * 60 * 1000; //30 minutes; this timeout applies to the time that the entire request takes (even if a successful TCP connection is made and the socket returns some data)
	
	// The HTTP connection manager the configured the HTTP client
	protected HttpConnectionManager manager = null;
	
	// The URL to be accessed
	protected URL url = null;
	
	// The identification and password to use when accessing the endpoint
	String id = null;
	String password = null;
	
    //Mutex for waiting to time-out the operation if necessary
	Object timeoutMutex = new Object();
	
	//Indicates if a timeout was reached
	boolean timeoutReached = false;
	
	//This is the method to be invoked. This is made a class variable so that abort can be called if necessary
	HttpMethod method = null;
	
	/**
	 * Get the HTTP client with credentials supplied (if defined in the class)
	 * @return
	 */
	public HttpClient getHttpClient(){

		HttpClient client = new HttpClient(getConnectionManager());

		// Supply credentials if defined
		if( id != null ){
			client.getState().setCredentials(
					new AuthScope(url.getHost(), url.getPort(), "threatfactor_nsia"),
					new UsernamePasswordCredentials(id, password)
			);
		}

		/* The parameter below will cause the HTTP client to provide the authentication credentials during the first
		 * HTTP request. Otherwise, two HTTP requests will be performed.
		 *
		 * See http://hc.apache.org/httpclient-3.x/authentication.html#Preemptive_Authentication for details.
		 */
		client.getParams().setAuthenticationPreemptive(true);
		return client;
	}
	
	/**
	 * Determine if the result is a message indicating that the operation succeeded or failed.
	 * @param doc
	 * @return
	 */
	protected boolean isResultSuccess(Document doc){

		Element root = doc.getDocumentElement();
		String success = root.getAttribute("success");

		if ( success.equalsIgnoreCase("1") || success.equalsIgnoreCase("true") ){
			return true;
		}
		else{
			return false;   
		}
	}
	
	/**
	 * Convert the given XML document to a string. 
	 * @param doc
	 * @return
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerException
	 */
	protected String documentToString(Document doc) throws TransformerFactoryConfigurationError, TransformerException{
		StringWriter stw = new StringWriter();
		Transformer serializer = TransformerFactory.newInstance().newTransformer();
		serializer.transform(new DOMSource(doc), new StreamResult(stw));
		return stw.toString(); 

	}	
	
	/**
	 * Get a connection manager.
	 * @return
	 */
	HttpConnectionManager getConnectionManager(){
		return getConnectionManager(SOCKET_TIMEOUT, CONNECTION_TIMEOUT);
	}
	
	/**
	 * Get the connection manager using the defined timeout parameters.
	 * @param socketTimeout
	 * @param connectionTimeout
	 * @return
	 */
	synchronized HttpConnectionManager getConnectionManager(int socketTimeout, int connectionTimeout){
		if( manager == null ){
			HttpConnectionManagerParams params = new HttpConnectionManagerParams();
			params.setSoTimeout(socketTimeout);
			params.setConnectionTimeout(connectionTimeout);
			params.setStaleCheckingEnabled(true);
			HostConfiguration host = new HostConfiguration();
			host.setHost( url.getHost() );
			params.setMaxConnectionsPerHost(host, 3);
			//manager = new MultiThreadedHttpConnectionManager();
			manager = new SimpleHttpConnectionManager();
			manager.setParams(params);
		}

		return manager;
	}
	
	/**
	 * Set the URL of the endpoint.
	 * @param endpointURL
	 */
	public void setEndpointURL( URL endpointURL ){
		if( endpointURL == null ){
			throw new IllegalArgumentException("The endpoint URL cannot be null");
		}
		
		this.url = endpointURL;
	}
	
	/**
	 * Perform a get request against the given URL.
	 * @param url
	 * @return
	 * @throws RESTRequestFailedException
	 */
	protected Document doGet( URL url ) throws RESTRequestFailedException{
		return doGet( url, null, null );
	}

	/**
	 * Perform a get request against the given URL.
	 * @param url
	 * @param id
	 * @param authenticationData
	 * @return
	 * @throws RESTRequestFailedException
	 */
	protected Document doGet( URL url, String id, String password ) throws RESTRequestFailedException{
		GetMethod get = null;
		
		this.id = id;
		this.password = password;
		
		try{
			get = new GetMethod(url.toExternalForm());
			return doHTTP( get );
		}
		finally{
			if( get != null ){
				get.releaseConnection();
			}
		}
	}
	
	/**
	 * Perform the REST request using the given HTTP method.
	 * @param method
	 * @return
	 * @throws RESTRequestFailedException
	 */
	protected Document doHTTP( HttpMethod method ) throws RESTRequestFailedException{
		
		// Tell the method to perform authentication if an ID was provided
		if( id != null ){
			method.setDoAuthentication( true );
		}
		
		this.method = method;
		InputStream is = null;
		HttpClient client = null;
		
		//Start the timeout thread that will terminate the operation if is freezes
		TimeoutThread timeoutThread = new TimeoutThread();
		timeoutThread.start();
		
		try{
			
			// Start the HTTP client
			client = getHttpClient();
			int status = client.executeMethod( method );
			
			// Throw an exception if the timeout was reached
			if( timeoutReached ){
				throw new RESTRequestFailedException("REST request timed out");
			}
			
			// Stop if authentication failed
			if( status == 401 ){
				throw new RESTRequestAuthFailedException();
			}
			
			// Throw an error if the server did not respond correctly to the response
			if( status != 200 ){
				throw new RESTRequestFailedException("HTTP request failed (returned HTTP code " + status + " for " + method.getURI() + ")", status);
			}
			
			// Get the response from the server
			is = method.getResponseBodyAsStream();
			
			// Parse the XML and create the method objects
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db;
			Document doc = null;
			
			db = dbf.newDocumentBuilder();
			doc = db.parse( is );
			
			return doc;
			
		} catch (HttpException e) {
			throw new RESTRequestFailedException("REST request failed due to an HTTP exception", e);
		} catch (IOException e) {
			throw new RESTRequestFailedException("REST request failed due to an IO exception", e);
		} catch (ParserConfigurationException e) {
			throw new RESTRequestFailedException("REST request failed since the response could not parsed due to a parser configuration problem", e);
		} catch (SAXException e) {
			throw new RESTRequestFailedException("REST request failed since the response could not parsed", e);
		}
		finally{
			
			// Release the HTTP connection
			method.releaseConnection();
			
			// Terminate the timeout thread
			synchronized ( timeoutMutex) {
				timeoutThread.terminate();
				//timeoutThread.notify();
			}
			
			// Close any idle connections
			if( manager != null ){
				manager.closeIdleConnections(0);
			}
			
			// Close the input stream
			try{
				if( is != null ){
					is.close();
				}
			}
			catch( IOException e ){
				throw new RESTRequestFailedException("REST request failed due to an IO exception", e);
			}
		}
		
	}
	
	// The date format for all dates in XML
	public static final SimpleDateFormat ISO8601FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	
	/**
	 * Parse the given date into a Java object
	 * @param date
	 * @return
	 * @throws ParseException
	 */
	public static Date parseStandardDateFormat( String date ) throws ParseException{
		return ISO8601FORMAT.parse(date);
	}
	
	/**
	 * Get a string version of the given date in ISO8601 date format.
	 * @param date
	 * @return
	 */
	public static String getDateAsISO8601String(Date date){
		String result = ISO8601FORMAT.format(date);
        //convert YYYYMMDDTHH:mm:ss+HH00 into YYYYMMDDTHH:mm:ss+HH:00
		//- note the added colon for the Timezone
		result = result.substring(0, result.length()-2)+ ":" + result.substring(result.length()-2);
		return result;
	}
	
	/**
	 * This thread will monitor the REST operation and terminate it if it goes too long.
	 * @author Luke Murphey
	 *
	 */
	private class TimeoutThread extends Thread{
		
		// If true, the thread will terminate
		private boolean stop = false;
		
		/**
		 * Default constructor
		 */
		public TimeoutThread(){
			setName("REST operation timeout monitor: " + method);
		}
		
		/**
		 *  Begin monitoring the REST operation and terminate it if to goes on too long
		 */
		public void run(){
			
			// The time that the thread began to wait
			long startTime = System.currentTimeMillis();
			
			// The number of seconds to wait until terminating the operation
			long delayTime = REQUEST_TIMEOUT;
			
			try{
				synchronized (timeoutMutex) {
					
					// Wait for the specified delay
					if( stop == false ){
						timeoutMutex.wait(delayTime);
					}
					
					// Determine if the timeout was reached
					if( (System.currentTimeMillis() - startTime ) >= delayTime ){
						method.abort();
						timeoutReached = false;
					}
					
				}
			} catch (InterruptedException e) {
				// Ignore this exception, it just means the thread was interrupted
			}
		}
		
		/**
		 * Terminate the timeout thread.
		 */
		public void terminate(){
			stop = true;
			this.interrupt();
		}
	}

}
