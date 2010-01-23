package net.lukemurphey.nsia.extension;

import org.apache.commons.lang.StringUtils;

import net.lukemurphey.nsia.extension.FieldValidator.FieldValidatorResult;

public abstract class PrototypeField implements Cloneable {

	private int layoutWidth = 1;
	private String name;
	private String title;
	private String overrideValue = null;
	
	public PrototypeField(String name, String title){
		
		// 0 -- Precondition check
		//Will be checked in the setter methods
		
		// 1 -- Initialize the class
		setName(name);
		setTitle(title);
		setLayoutWidth(1);
	}
	
	public PrototypeField(String name, String title, int layoutWidth){
		
		// 0 -- Precondition check
		//Will be checked in the setter methods
		
		// 1 -- Initialize the class
		setName(name);
		setTitle(title);
		setLayoutWidth(layoutWidth);
	}
	
	public String getTitle(){
		return title;
	}
	
	public String getName(){
		return name;
	}
	
	public int getLayoutWidth(){
		return layoutWidth;
	}
	
	public String getDefaultValue(){
		return overrideValue;
	}
	
	public void setDefaultValue(String value){
		this.overrideValue = value;
	}
	
	public void setLayoutWidth( int width ){
		
		// 0 -- Precondition check
		if( width < 1 ){
			throw new IllegalArgumentException("The width must be at least one");
		}
		
		// 1 -- Set the width
		layoutWidth = width;
	}
	
	public void setName( String name ){
		
		// 0 -- Precondition check
		if( name == null ){
			throw new IllegalArgumentException("The name cannot be null");
		}
		
		if( StringUtils.isBlank(name)){
			throw new IllegalArgumentException("The name cannot be blank");
		}
		
		// 1 -- Set the name
		this.name = name;
	}

	public void setTitle( String title ){
	
	// 1 -- Set the title
	this.title = title;
}
	
	public abstract FieldValidatorResult validate(String value);

	public Object clone() throws CloneNotSupportedException {
        return super.clone();
	}
	
}
