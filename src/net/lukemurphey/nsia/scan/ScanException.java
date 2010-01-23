package net.lukemurphey.nsia.scan;

public class ScanException extends Exception{

	private static final long serialVersionUID = 3373325313747446296L;

	public ScanException( String message){
		super(message);
	}
	
	public ScanException( String message, Exception innerException){
		super(message, innerException);
	}
}
