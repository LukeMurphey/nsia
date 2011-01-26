package net.lukemurphey.nsia.rest;

/**
 * An exception that describes a failed REST request.
 * @author Luke Murphey
 *
 */
public class RESTRequestFailedException extends Exception {
	
	private static final long serialVersionUID = 2858656000813426828L;
	
	// HTTP response code
	private int status = -1;
	
	public RESTRequestFailedException( String message, int status ){
		super(message);
		setStatus(status);
	}
	
	public RESTRequestFailedException( String message, Throwable cause, int status ){
		super(message, cause);
		setStatus(status);
	}
	
	public RESTRequestFailedException( String message ){
		super(message);
	}
	
	public RESTRequestFailedException( String message, Throwable cause ){
		super(message, cause);
	}
	
	private void setStatus( int status ){
		this.status = status;
	}
	
	public int getStatusCode(){
		return status;
	}

}
