package net.lukeMurphey.nsia;

public class NumericalOverflowException extends Exception {
	
	static final long serialVersionUID = 1139617875L;
	
	String variableDescription;
	
	public NumericalOverflowException( String message, String variableDescription){
		super(message);//Set the message using the super class constructor
		this.variableDescription = variableDescription;
	}

}
