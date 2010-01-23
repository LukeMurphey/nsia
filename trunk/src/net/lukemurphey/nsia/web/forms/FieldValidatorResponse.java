package net.lukemurphey.nsia.web.forms;

/**
 * Indicates the validator matched the given input and allows a message to be returned (may be used to indicate why the input did not match).
 * @author Luke
 *
 */
public class FieldValidatorResponse {

	private boolean isValid = true;
	private String message;
	
	public FieldValidatorResponse( String message ){
		this.message = message;
	}
	
	public FieldValidatorResponse( boolean isValid, String message ){
		this.message = message;
		this.isValid = isValid;
	}

	public FieldValidatorResponse( boolean isValid ){
		this.isValid = isValid;
	}
	
	public boolean isValid(){
		return isValid;
	}
	
	public String getMessage(){
		return message;
	}
	
	public boolean hasMessage(){
		return message != null;
	}
	
}
