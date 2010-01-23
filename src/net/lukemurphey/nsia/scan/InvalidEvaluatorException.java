package net.lukemurphey.nsia.scan;

public class InvalidEvaluatorException extends Exception {

	private static final long serialVersionUID = 8021359969855699034L;
	
	public InvalidEvaluatorException( String message ){
		super(message);
	}
	
	public InvalidEvaluatorException( String message, Exception exception ){
		super(message, exception);
	}

}
