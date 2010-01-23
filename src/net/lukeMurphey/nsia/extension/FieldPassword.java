package net.lukemurphey.nsia.extension;

import net.lukemurphey.nsia.extension.FieldValidator.FieldValidatorResult;

public class FieldPassword extends PrototypeField {

	private FieldValidator validator = null; 
	
	public FieldPassword(String name, String title, int layoutWidth, FieldValidator validator ) {
		super(name, title, layoutWidth);
		
		
		// 0 -- Precondition check
		if( validator == null ){
			throw new IllegalArgumentException("The validator cannot be null");
		}
		
		// 1 -- Set the validator
		this.validator = validator;
	}

	@Override
	public FieldValidatorResult validate(String value) {
		FieldValidatorResult result = validator.validate(value);
		return result;
	}

}
