package net.lukemurphey.nsia.scan;

public class DefinitionParseException extends Exception {

	private static final long serialVersionUID = 6011013769213925708L;

	private int locationOfError = -1;
	
	public DefinitionParseException( String message, int errorLocation ){
		super(message);
		locationOfError = errorLocation;
	}
	
	public DefinitionParseException( String message ){
		super(message);
	}
	
	public int getErrorLocation(){
		return locationOfError;
	}
}
