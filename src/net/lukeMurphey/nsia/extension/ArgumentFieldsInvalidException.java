package net.lukemurphey.nsia.extension;

public class ArgumentFieldsInvalidException extends Exception {

	private static final long serialVersionUID = -2764120562827505346L;

	private PrototypeField invalidField;
	
	public ArgumentFieldsInvalidException( String message, PrototypeField invalidField ){
		super(message);
		this.invalidField = invalidField;
	}
	
	public PrototypeField getInvalidField(){
		return invalidField;
	}
}
