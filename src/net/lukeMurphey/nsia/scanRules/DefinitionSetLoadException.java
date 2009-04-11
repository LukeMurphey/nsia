package net.lukeMurphey.nsia.scanRules;

public class DefinitionSetLoadException extends Exception{
	
	private static final long serialVersionUID = 8715491978951889333L;

	public DefinitionSetLoadException( String message ){
		super(message);
	}
	
	public DefinitionSetLoadException( String message, Exception exception ){
		super(message, exception);
	}

}
