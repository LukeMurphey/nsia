package net.lukeMurphey.nsia.responseModule;

public class ActionFailedException extends Exception{

	private static final long serialVersionUID = 8178258491350565907L;

	public ActionFailedException( String message ){
		super(message);
	}
	
	public ActionFailedException( String message, Throwable t ){
		super(message, t);
	}
}