package net.lukeMurphey.nsia.scanRules;

public class DefinitionArchiveException extends Exception{

	private static final long serialVersionUID = 3132157000632717663L;
	
	public DefinitionArchiveException( String message ){
		super(message);
	}

	public DefinitionArchiveException( String message, Exception innerException  ){
		super(message, innerException);
	}
}
