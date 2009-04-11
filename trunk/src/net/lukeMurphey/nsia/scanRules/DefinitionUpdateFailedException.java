package net.lukeMurphey.nsia.scanRules;

public class DefinitionUpdateFailedException extends Exception {

	private static final long serialVersionUID = 109900123812381L;

	public DefinitionUpdateFailedException( String message, Throwable t){
		super(message,t);
	}
	
	public DefinitionUpdateFailedException( String message ){
		super(message);
	}
	
	public DefinitionUpdateFailedException( Throwable t ){
		super(t);
	}

}
