package net.lukemurphey.nsia.extension;

import net.lukemurphey.nsia.extension.FieldValidator.FieldValidatorResult;

public class FieldText extends PrototypeField{

	private int width = 0;
	private int height = 0;
	private FieldValidator validator;
	
	public FieldText( String name, String title, int width, int height, FieldValidator validator ){
		super(name, title);
		
		// 0 -- Precondition check
		if( width <= 0 ){
			throw new IllegalArgumentException("The width must be greater than 0");
		}
		
		if( height <= 0 ){
			throw new IllegalArgumentException("The height must be greater than 0");
		}
		
		if( validator == null ){
			throw new IllegalArgumentException("The validator cannot be null");
		}
		
		// 1 -- Set the values
		this.width = width;
		this.height = height;
		this.validator = validator;
	}

	public FieldValidatorResult validate(String value) {
		FieldValidatorResult result = validator.validate(value);
		return result;
	}

	public int getWidth(){
		return width;
	}
	
	public int getHeight(){
		return height;
	}
	
	@Override
	public String getType() {
		return "text";
	}
}