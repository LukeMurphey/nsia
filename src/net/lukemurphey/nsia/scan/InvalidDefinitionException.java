package net.lukemurphey.nsia.scan;

public class InvalidDefinitionException extends Exception{

	private static final long serialVersionUID = 2166685802207816126L;
	
	public InvalidDefinitionException( String message ){
		super(message);
	}
	
	public InvalidDefinitionException( String message, Exception exception ){
		super(message, exception);
	}
}
