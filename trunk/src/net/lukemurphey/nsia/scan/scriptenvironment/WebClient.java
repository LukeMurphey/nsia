package net.lukemurphey.nsia.scan.scriptenvironment;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import net.lukemurphey.nsia.scan.HttpResponseData;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.TraceMethod;

/**
 * This class provides an simple method to download content from a website for analysis. This class provides methods that will terminate the operation
 * if it exceeds certain thresholds (such as time or the size of the response body).
 * @author Luke
 *
 */
public class WebClient {

	// The various methods that are allowed
	public enum HttpMethod{
		HEAD, GET, POST, PUT, DELETE, TRACE, OPTIONS;
	}
	
	//This is the method to be invoked
	HttpMethodBase method = null;
	
	//The URL to access
	URL url = null;
	
	//The number of seconds to wait before forcibly stopping the connection 
	int downloadTimeoutSeconds = 5; 
	
	//The maximum amount of data to download
	int downloadBytesMax = 1024 * 256; //256 bytes 
	
	//Mutex for waiting to time-out the download if necessary 
	Object httpMethodMutex = new Object();
	
	//Result of the operation
	boolean timeOutReached = false;
	
	/**
	 * Construct a web-client for the given operation and URL.
	 * @param httpMethod
	 * @param url
	 * @throws MalformedURLException 
	 */
	public WebClient( HttpMethod httpMethod, String url ) throws MalformedURLException{
		
		// 0 -- Precondition Check
		if( url == null ){
			throw new IllegalArgumentException("The URL cannot be null");
		}

		initialize( httpMethod, new URL(url) );
	}
	
	/**
	 * Construct a web-client for the given operation and URL (as a String).
	 * @param httpMethod
	 * @param url
	 * @throws MalformedURLException 
	 */
	public WebClient( HttpMethod httpMethod, URL url ) {
		initialize( httpMethod, url );
	}
	
	/**
	 * Initialize the web client
	 * @param httpMethod
	 * @param url
	 * @throws MalformedURLException
	 */
	private void initialize( HttpMethod httpMethod, URL url ){
		
		// 0 -- Precondition Check
		if( url == null ){
			throw new IllegalArgumentException("The URL cannot be null");
		}
		
		this.url = url;
		
		// 1 -- Determine which method is requested and initialize it
		if( httpMethod == HttpMethod.HEAD ){
			method = new HeadMethod( url.toString() );
		}
		else if( httpMethod == HttpMethod.GET ){
			method = new GetMethod( url.toString() );
		}
		else if( httpMethod == HttpMethod.POST ){
			method = new PostMethod( url.toString() );
		}
		else if( httpMethod == HttpMethod.PUT ){
			method = new PutMethod( url.toString() );
		}
		else if( httpMethod == HttpMethod.DELETE ){
			method = new DeleteMethod( url.toString() );
		}
		else if( httpMethod == HttpMethod.TRACE ){
			method = new TraceMethod( url.toString() );
		}
		else if( httpMethod == HttpMethod.OPTIONS ){
			method = new OptionsMethod( url.toString() );
		}
	}
	
	/**
	 * Set the maximum amount of time (in seconds) that the request will be allowed to perform it operation.
	 * @param seconds
	 */
	public void setTimeLimit( int seconds ){
		downloadTimeoutSeconds = seconds;
	}
	
	/**
	 * Set the maximum amount of data (in bytes) that will be downloaded.
	 * @param bytes
	 */
	public void setSizeLimit( int bytes ){
		if( bytes <= 0 ){
			throw new IllegalArgumentException("Maximum number of bytes to download must be greater than 0");
		}
		
		downloadBytesMax = bytes;
	}
	
	/*
	public void setFollowForwards( int limit ){
		
	}
	*/
	
	/**
	 * Represents the result of an HTTP request. 
	 * @author Luke
	 *
	 */
	public static class HttpResult{
		HttpResponseData httpResponse;
		boolean timeOutReached = false;
		
		public HttpResult( HttpResponseData response, boolean timeOutReached ){
			httpResponse = response;
			this.timeOutReached = timeOutReached;
		}
		
		public String getResponseBodyAsString(){
			return httpResponse.getResponseAsString();
		}
		
		public byte[] getResponseBodyAsBytes(){
			return httpResponse.getResponseAsBytes();
		}
		
		public String getFinalLocation(){
			return httpResponse.getLocation();
		}
		
		public String getRequestedLocation(){
			return httpResponse.getRequestedLocation();
		}
		
		public String getHeaderValue(String headerName){
			return httpResponse.getHeaderValue(headerName);
		}
		
		public String getQueryString(){
			return httpResponse.getQueryString();
		}
		
		public String getContentType(){
			return httpResponse.getContentType();
		}
		
		public StatusLine getStatusLine(){
			return httpResponse.getStatusLine();
		}
		
		public int getResponseCode(){
			return httpResponse.getResponseCode();
		}
		
		public boolean sizeLimitReached(){
			return httpResponse.wasDownloadLimitReached();
		}
		
		/**
		 * Indicates if time-out was reached.
		 * @return
		 */
		public boolean timeOutReached(){
			return timeOutReached;
		}
	}
	
	/**
	 * This class stops a download once a timeout is reached.
	 * @author Luke
	 *
	 */
	private class TimeoutThread extends Thread{
		
		private boolean stop = false;
		
		public void terminate(){
			stop = true;
			this.interrupt();
		}
		
		/**
		 * Start monitoring the thread and call abort if it exceeds the timeout.
		 */
		public void run(){
			try {
				long start = System.currentTimeMillis();
				long waitFor = downloadTimeoutSeconds * 1000;
				
				synchronized (httpMethodMutex) {
					if( stop == false ){
						httpMethodMutex.wait(waitFor);
					}
				}
				
				//Abort the method if we hit the timeout (since we are forcing the download to stop)
				if( ((System.currentTimeMillis() - start) * 1000 ) >= waitFor ){
					method.abort();
					timeOutReached = true;
				}
				
			} catch (InterruptedException e) {
				//Ignore this exception, interrupt may have been called
			}
		}
	}
	
	/**
	 * Run the web-client on the given URL and return the result.
	 * @return
	 * @throws HttpException
	 * @throws IOException
	 */
	public HttpResult run() throws HttpException, IOException{
		
		// 1 -- Initialize the HTTP client
		HostConfiguration hostConfig = new HostConfiguration();
		hostConfig.setHost(url.getHost(), url.getPort(), url.getProtocol());
		
		HttpClient httpClient = new HttpClient();
		
		//Start the timeout thread that will end the operation if client cannot download the content in time
		TimeoutThread timeoutThread = new TimeoutThread();
		timeoutThread.setName("Timeout for Web-client to " + url);
		timeoutThread.start();
		
		try{
			method.setFollowRedirects(true);
			httpClient.executeMethod( hostConfig, method );

			HttpResponseData httpResponse = new HttpResponseData(method, downloadBytesMax);

			return new HttpResult(httpResponse, timeOutReached);
		}
		finally{
			method.releaseConnection();

			synchronized (httpMethodMutex) {
				httpMethodMutex.notify();
				timeoutThread.terminate();
			}
		}
	}
	
	/**
	 * Stop the current download.
	 */
	public void terminate(){
		synchronized(httpMethodMutex){
			method.abort();
		}
	}
}
