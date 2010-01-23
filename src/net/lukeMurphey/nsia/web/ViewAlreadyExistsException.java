package net.lukemurphey.nsia.web;

public class ViewAlreadyExistsException extends Exception {

	private static final long serialVersionUID = -5561974392146154742L;

	public ViewAlreadyExistsException(){
		super();
	}
	
	public ViewAlreadyExistsException( String message ){
		super(message);
	}
	
	public ViewAlreadyExistsException( Throwable t ){
		super(t);
	}
	
	public ViewAlreadyExistsException( String message, Throwable t ){
		super(message, t);
	}
	
}
