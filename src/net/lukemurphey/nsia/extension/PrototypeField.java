package net.lukemurphey.nsia.extension;

import org.apache.commons.lang.StringUtils;

import net.lukemurphey.nsia.extension.FieldValidator.FieldValidatorResult;

/**
 * The prototype represents a field used by an extension and is designed to be used within a field layout.
 * @author luke
 *
 */
public abstract class PrototypeField implements Cloneable {

	private int layoutWidth = 1;
	private String name;
	private String title;
	private String help;
	private String overrideValue = null;
	
	public PrototypeField(String name, String title){
		
		// 0 -- Precondition check
		//Will be checked in the setter methods
		
		// 1 -- Initialize the class
		setName(name);
		setTitle(title);
		setLayoutWidth(1);
	}
	
	public PrototypeField(String name, String title, String help){

		// 0 -- Precondition check
		//Will be checked in the setter methods
		
		// 1 -- Initialize the class
		setName(name);
		setTitle(title);
		setHelp(help);
		setLayoutWidth(1);
	}
	
	public PrototypeField(String name, String title, String help, int layoutWidth){

		// 0 -- Precondition check
		//Will be checked in the setter methods
		
		// 1 -- Initialize the class
		setName(name);
		setTitle(title);
		setHelp(help);
		setLayoutWidth(layoutWidth);
	}
	
	public PrototypeField(String name, String title, int layoutWidth){
		
		// 0 -- Precondition check
		//Will be checked in the setter methods
		
		// 1 -- Initialize the class
		setName(name);
		setTitle(title);
		setLayoutWidth(layoutWidth);
	}
	
	/**
	 * Get the title of the field.
	 * @return
	 */
	public String getTitle(){
		return title;
	}
	
	/**
	 * Get the name of the field.
	 * @return
	 */
	public String getName(){
		return name;
	}
	
	/**
	 * Get the help description for this field (may be null).
	 * @return
	 */
	public String getHelp(){
		return help;
	}
	
	/**
	 * Get the width of this field in terms of how many units or cells it ought to encompass.
	 * @return
	 */
	public int getLayoutWidth(){
		return layoutWidth;
	}
	
	/**
	 * Get the default value (may be null).
	 * @return
	 */
	public String getDefaultValue(){
		return overrideValue;
	}
	
	/**
	 * Set teh default value.
	 * @param value
	 */
	public void setDefaultValue(String value){
		this.overrideValue = value;
	}
	
	/**
	 * Set the help desription associated with this field.
	 * @param help
	 */
	public void setHelp( String help){
		this.help = help;
	}
	
	/**
	 * Set the layout width in terms of how many units or cells it ought to encompass.
	 * @param width
	 */
	public void setLayoutWidth( int width ){
		
		// 0 -- Precondition check
		if( width < 1 ){
			throw new IllegalArgumentException("The width must be at least one");
		}
		
		// 1 -- Set the width
		layoutWidth = width;
	}
	
	/**
	 * Set the name of the field.
	 * @param name
	 */
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

	/**
	 * Returns a string describing what type of the field this is (password, text, etc.)
	 * @return
	 */
	public abstract String getType();
	
	/**
	 * Set the human readable field title.
	 * @param title
	 */
	public void setTitle( String title ){
		// 1 -- Set the title
		this.title = title;
	}
	
	/**
	 * Validate the field value based on the provided value. 
	 * @param value
	 * @return
	 */
	public abstract FieldValidatorResult validate(String value);

	public Object clone() throws CloneNotSupportedException {
        return super.clone();
	}
	
}
