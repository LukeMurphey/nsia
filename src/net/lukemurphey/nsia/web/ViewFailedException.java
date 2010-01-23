package net.lukemurphey.nsia.web;

public class ViewFailedException extends Exception {

	private static final long serialVersionUID = 2643324291007510640L;
	
	public ViewFailedException(){
		super();
	}
	
	public ViewFailedException( String message){
		super(message);
	}
	
	public ViewFailedException( String message, Throwable t){
		super(message, t);
	}
	
	public ViewFailedException(Throwable t){
		super(t);
	}

}
