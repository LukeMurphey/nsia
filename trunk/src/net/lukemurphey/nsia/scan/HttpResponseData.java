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
	
	private DataSpecimen responseBody;
	private Header[] headers;
	private int responseCode;
	private String queryString;
	private StatusLine statusLine;
	private Method method;
	private Parser parser = null;
	private String originalLocation;
	private String finalLocation;
	
	private static final int DEFAULT_LENGTH_LIMIT = 3145728;//3 MB
	
	private static final Pattern REGEX_GET_ENCODING = Pattern.compile("[-a-zA-Z_ 0-9/;]*charset=[ ]*([-a-zA-Z_0-9]*)");
	private static final Pattern REGEX_CONTENT_TYPE = Pattern.compile("[-.+a-zA-Z0-9]+/[-.+a-zA-Z0-9]+");
	//private static final Pattern REGEX_CONTENT_TYPE = Pattern.compile("[a-zA-Z]+");
	
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
			
			byte[] responseBodyBytes = new byte[DEFAULT_LENGTH_LIMIT];
			int bytesRead = 0;
			int bytesReadTotal = 0;
			boolean downloadComplete = false;
			
			while( downloadComplete == false ){
				bytesRead = responseBodyStream.read( responseBodyBytes, bytesReadTotal, Math.min(1024, responseBodyBytes.length - bytesReadTotal));
				
				if( bytesRead <= 0 ){
					downloadComplete = true;
				}
				
				if( bytesRead == responseBodyBytes.length){
					downloadComplete = true;
				}
				
				bytesReadTotal += bytesRead;
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
	
	public int getResponseCode(){
		return responseCode;
	}
	
	public DataSpecimen getDataSpecimen(){
		return responseBody;
	}
	
	public Parser getDocumentParser() throws ParserException{

		parser = new Parser();
		
		if( responseBody != null ){
			parser.setEncoding(responseBody.getEncoding().toString());
			parser.setInputHTML(getResponseAsString());
		}
		
		//parser = new Parser(getResponseAsString(), new DefaultParserFeedback());
		
		return parser;
	}
	
	public String getQueryString(){
		return queryString;
	}
	
	/*public URI getURI(){
		return uri;
	}*/
	
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
	
	public StatusLine getStatusLine(){
		return statusLine;
	}
	
	public byte[] getResponseAsBytes(){
		if( responseBody != null ){
			return responseBody.getBytes();
		}
		else{
			return null;
		}
	}
	
	public String getHeaderValue( String headerName ){
		for( int c = 0; c < headers.length; c++){
			if( headers[c].getName().matches(headerName) )
				return headers[c].getValue();
		}
		
		return null;
	}
	
	public String getResponseAsString(){
		if( responseBody != null ){
			return responseBody.getString();
		}
		else{
			return null;
		}
	}
	
	public String getContentType(){
		if( responseBody != null ){
			return responseBody.getContentType();
		}
		else{
			return null;
		}
	}
	
	public Method getMethod(){
		return method;
	}

}
