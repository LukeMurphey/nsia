package net.lukemurphey.nsia.web;

public class MiddlewareException extends Exception {

	private static final long serialVersionUID = -2695335783265577632L;
	
	public MiddlewareException(){}
	
	public MiddlewareException( String message){
		super(message);
	}
	
	public MiddlewareException( String message, Throwable t){
		super(message, t);
	}
	
	public MiddlewareException( Throwable t){
		super(t);
	}

}
