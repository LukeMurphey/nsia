package net.lukemurphey.nsia.web;

public class URLInvalidException extends Exception {

	private static final long serialVersionUID = -5866443540344360124L;

	public URLInvalidException( String message ){
		super(message);
	}
	
	public URLInvalidException( Throwable t ){
		super(t);
	}
	
	public URLInvalidException( String message, Throwable t ){
		super(message, t);
	}
	
}
