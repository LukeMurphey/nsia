package net.lukemurphey.nsia.response;

public class ActionInstantiationException extends Exception {

	private static final long serialVersionUID = -5378192313175132020L;

	public ActionInstantiationException(Throwable t){
		super(t);
	}
	
	public ActionInstantiationException(String message, Throwable t){
		super(message, t);
	}
	
	public ActionInstantiationException(String message){
		super(message);
	}
}
