package net.lukeMurphey.nsia;

public class DisallowedOperationException extends Exception{

	private static final long serialVersionUID = 5633213802751832942L;

	public DisallowedOperationException( String message ){
		super(message);
	}
}
