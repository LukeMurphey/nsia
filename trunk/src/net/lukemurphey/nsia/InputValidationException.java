package net.lukemurphey.nsia;

/**
 * This class is used to signal that some input has failed validation (such as injection attempts).
 * @author luke
 *
 */
public class InputValidationException extends Exception {
	
	static final long serialVersionUID = 1139617875L;
	
	private String fieldValue = null;
	private String fieldDesc = null;
	
	public InputValidationException( String message, String fieldDescription, String attemptedValue ){
		super( message );
		fieldDesc = fieldDescription;
		fieldValue = attemptedValue;
	}
	
	/**
	 * Returns the name (or description) of the field.
	 * @precondition The field description must be set or null will be returned.
	 * @postcondition The description is returned.
	 * @return
	 */
	public String getFieldDescription(){
		return fieldDesc;
	}
	
	/**
	 * Returns the value that was attempted.
	 * @precondition None, null will be returned if not set in the constructor
	 * @postcondition The attempted value will be set
	 * @return
	 */
	public String getAttemptedValue(){
		return fieldValue;
	}
	
}
