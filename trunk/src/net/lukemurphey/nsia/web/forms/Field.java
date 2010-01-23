package net.lukemurphey.nsia.web.forms;

/**
 * Represents a field in a web form.
 * @author Luke
 *
 */
public class Field {

	private String name;
	private FieldValidator validator;
	private boolean required = false; 
	
	public Field( String name, FieldValidator validator, boolean required ){

		// 0 -- Precondition check
		
		//	 0.1 -- Check the name
		if( name == null ){
			throw new IllegalArgumentException("The name cannot be null");
		}
		
		//	 0.2 -- Check the validator
		if( validator == null ){
			throw new IllegalArgumentException("The validator cannot be null");
		}
		
		// 1 -- Initialize the class
		this.name = name;
		this.validator = validator;
		this.required = required;
	}
	
	public Field( String name, FieldValidator validator  ){
		this(name, validator, false);
	}
	
	/**
	 * Creates a field with no validator. No validator means that any input will be accepted.
	 * @param name
	 */
	public Field( String name ){
		// 0 -- Precondition check
		
		//	 0.1 -- Check the name
		if( name == null ){
			throw new IllegalArgumentException("The name cannot be null");
		}
		
		// 1 -- Initialize the class
		this.name = name;
	}
	
	/**
	 * Get the name of the field.
	 * @return
	 */
	public String getName(){
		return name;
	}
	
	/**
	 * Get the validator for this field.
	 * @return
	 */
	public FieldValidator getValidator(){
		return validator;
	}
	
	/**
	 * Validate the field.
	 * @param value
	 * @return
	 */
	public FieldValidatorResponse validate( String value ){
		
		if( validator != null ){
			return validator.validate(value);
		}
		else{
			//Accept the value as valid by default
			return new FieldValidatorResponse(true);
		}
	}
	
	/**
	 * Determines if the field must be supplied with a value.
	 * @return
	 */
	public boolean isRequired(){
		return required;
	}
}
