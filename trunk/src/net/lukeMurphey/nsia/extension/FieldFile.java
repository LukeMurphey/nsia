package net.lukeMurphey.nsia.extension;

import net.lukeMurphey.nsia.extension.FieldValidator.FieldValidatorResult;

public class FieldFile extends PrototypeField {

	private int width = 0;
	private int height = 0;
	private FieldValidator validator;
	
	public FieldFile( String name, String title, int width, int height, FieldValidator validator ){
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
	
	@Override
	public FieldValidatorResult validate(String value) {
		return validator.validate(value);
	}
	
	public int getWidth(){
		return width;
	}
	
	public int getHeight(){
		return height;
	}

}
