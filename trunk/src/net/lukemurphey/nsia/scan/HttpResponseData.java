package net.lukemurphey.nsia.scan;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.regex.*;

import net.lukemurphey.nsia.GenericUtils;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;
import org.htmlparser.*;
import org.htmlparser.util.ParserException;

public class HttpResponseData  {
	
	public enum Method{
		GET, POST, DELETE, HEAD, PUT, TRACE
	}
	
	//Data that was included as part of the response body in the HTTP response
	private DataSpecimen responseBody;
	
	//The HTTP headers returned
	private Header[] headers;
	
	//The response code from the HTTP response
	private int responseCode;
	
	//The query string used to perform the HTTP request
	private String queryString;
	
	//The status line resulting from the HTTP request
	private StatusLine statusLine;
	
	//The method used to perform the HTTP request
	private Method method;
	
	//The parser used to review the HTML returned in the HTTP response
	private Parser parser = null;
	
	//The original location that was (or will be) requested
	private String originalLocation;
	
	//The final location (following any redirects performed)
	private String finalLocation;
	
	//The maximum number of bytes that will be downloaded
	private int sizeLimit = DEFAULT_LENGTH_LIMIT;
	
	//The default maximum number of bytes that will be downloaded
	private static final int DEFAULT_LENGTH_LIMIT = 1048576; //1 MB
	
	//A regular expression to find the encoding of the returned data
	private static final Pattern REGEX_GET_ENCODING = Pattern.compile("[-a-zA-Z_ 0-9/;]*charset=[ ]*([-a-zA-Z_0-9]*)");
	
	//A regular expression to find the content-type of the returned data
	private static final Pattern REGEX_CONTENT_TYPE = Pattern.compile("[-.+a-zA-Z0-9]+/[-.+a-zA-Z0-9]+");
	
	public HttpResponseData(HttpMethod httpMethod ) throws URIException{
		this( httpMethod, httpMethod.getURI().toString() );
	}
	
	public HttpResponseData(HttpMethod httpMethod, String serverAddress ) throws URIException{

		// 0 -- Precondition Checks
		
		//	0.1 -- Make sure that method is not null
		if( httpMethod == null ){
			throw new IllegalArgumentException("The HTTP method must not be null");
		}
		
		// 1 -- Initialize the class
		headers = httpMethod.getResponseHeaders();
		try {
			
			String contentType = null;
			
			if( httpMethod.getResponseHeader("Content-Type") != null && httpMethod.getResponseHeader("Content-Type").getValue() != null ){
				Matcher matcher = REGEX_CONTENT_TYPE.matcher( httpMethod.getResponseHeader("Content-Type").getValue() );
				if( matcher.find() ){
					contentType = matcher.group(0);
				}
			}
			
			InputStream responseBodyStream = httpMethod.getResponseBodyAsStream();
			
			byte[] responseBodyBytes = new byte[sizeLimit];
			int bytesRead = 0;
			int bytesReadTotal = 0;
			boolean downloadComplete = false;
			
			while( downloadComplete == false ){
				
				//If the stream is null then stop trying to download more bytes from it
				if( responseBodyStream == null ){
					downloadComplete = true;
				}
				else{
					bytesRead = responseBodyStream.read( responseBodyBytes, bytesReadTotal, Math.min(1024, responseBodyBytes.length - bytesReadTotal));
					
					if( bytesRead <= 0 ){
						downloadComplete = true;
					}
					
					//Determine if we hit the limit on the amount of input allowed
					if( bytesRead == responseBodyBytes.length){
						downloadComplete = true;
					}
					
					bytesReadTotal += bytesRead;
				}
			}
			
			if( bytesReadTotal > 0 ){
				responseBodyBytes = (byte[])GenericUtils.resizeArray(responseBodyBytes, bytesReadTotal);
			}
			else{
				responseBodyBytes = new byte[0];
			}
			
			//Note: the method may have redirected to another location. Therefore, get the final location and report that as the location of the resource.
			finalLocation = new URL(new URL(serverAddress), httpMethod.getURI().toString()).toString();
			
			responseBody = new DataSpecimen( responseBodyBytes, null, finalLocation, contentType );
			autoSetEncoding();
			
		} catch (IOException e) {
			responseBody = null;
		}
		
		responseCode = httpMethod.getStatusCode();
		queryString = httpMethod.getQueryString();
		statusLine = httpMethod.getStatusLine();
		originalLocation = serverAddress;
		
		if( httpMethod instanceof GetMethod ){
			method = Method.GET;
		}
		else if( httpMethod instanceof PutMethod ){
			method = Method.PUT;
		}
		else if( httpMethod instanceof DeleteMethod ){
			method = Method.DELETE;
		}
		else if( httpMethod instanceof HeadMethod ){
			method = Method.HEAD;
		}
		else if( httpMethod instanceof PostMethod ){
			method = Method.POST;
		}
		else if( httpMethod instanceof TraceMethod ){
			method = Method.TRACE;
		}
	}
	
	/**
	 * Determine the encoding of the data provided.
	 */
	private void autoSetEncoding(){
		//Try to get the encoding as returned by the server
		String encodingHeader = getHeaderValue("Content-Type");
		
		if( encodingHeader != null ){
			Matcher matcher = REGEX_GET_ENCODING.matcher(encodingHeader);
			if( matcher.find() ){
				String encodingFromServer = null;
				encodingFromServer = matcher.group(1);
				
				//Set the encoding if it was not auto-detected
				if( responseBody.getEncoding() == null ){
					Charset charset = Charset.availableCharsets().get(encodingFromServer);
					
					if( charset != null ){
						responseBody.setEncoding(charset);
					}
				}
			}
		}
	}
	
	/**
	 * Set the maximum amount of data that will be downloaded.
	 * @param sizeLimit
	 */
	public void setSizeLimit(int sizeLimit ){
		if( sizeLimit < 0){
			throw new IllegalArgumentException("The maximum download size limit must be greater than zero");
		}
		
		this.sizeLimit = sizeLimit;
	}
	
	/**
	 * Get the maximum amount of data that will be downloaded.
	 * @return
	 */
	public int getSizeLimit(){
		return sizeLimit;
	}
	
	/**
	 * Get the HTTP response code.
	 * @return
	 */
	public int getResponseCode(){
		return responseCode;
	}
	
	/**
	 * Get the specimen that contains the data downloaded from the given URL.
	 * @return
	 */
	public DataSpecimen getDataSpecimen(){
		return responseBody;
	}
	
	/**
	 * Get a parser that will allow moving up the HTML tree.
	 * @return
	 * @throws ParserException
	 */
	public Parser getDocumentParser() throws ParserException{

		parser = new Parser();
		
		if( responseBody != null ){
			parser.setEncoding(responseBody.getEncoding().toString());
			parser.setInputHTML(getResponseAsString());
		}
		
		//parser = new Parser(getResponseAsString(), new DefaultParserFeedback());
		
		return parser;
	}
	
	/**
	 * Get the query string used to make the HTTP request.
	 * @return
	 */
	public String getQueryString(){
		return queryString;
	}
	
	/**
	 * This method provides information about the original location requested. This differs from the actual location because redirects
	 * may have sent the browser somewhere else.
	 */
	public String getRequestedLocation(){
		return originalLocation;
	}
	
	/**
	 * This is the final location that the HTTP client was sent to (i.e. after any redirects were completed).
	 * @return
	 */
	public String getLocation(){
		return finalLocation;
	}
	
	/**
	 * Get the status line for the given request.
	 * @return
	 */
	public StatusLine getStatusLine(){
		return statusLine;
	}
	
	/**
	 * Get the response as bytes.
	 * @return
	 */
	public byte[] getResponseAsBytes(){
		if( responseBody != null ){
			return responseBody.getBytes();
		}
		else{
			return null;
		}
	}
	
	/**
	 * Get the value of the header for the given name.
	 * @param headerName
	 * @return
	 */
	public String getHeaderValue( String headerName ){
		for( int c = 0; c < headers.length; c++){
			if( headers[c].getName().matches(headerName) )
				return headers[c].getValue();
		}
		
		return null;
	}
	
	/**
	 * Get the response as a string. The encoding will have been discovered automatically (but may be incorrect if the dat set is small and the server failed to indicate the encoding).
	 * @return
	 */
	public String getResponseAsString(){
		if( responseBody != null ){
			return responseBody.getString();
		}
		else{
			return null;
		}
	}
	
	/**
	 * Get the content-type of the string. Note that the content-type will be automatically discovered based on the file contents
	 * and may be incorrect if the amount of data is small and the server failed to include the content-type in the response.
	 * @return
	 */
	public String getContentType(){
		if( responseBody != null ){
			return responseBody.getContentType();
		}
		else{
			return null;
		}
	}
	
	/**
	 * Get the HTTP request method used.
	 * @return
	 */
	public Method getMethod(){
		return method;
	}

}
