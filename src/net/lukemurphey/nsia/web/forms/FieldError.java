package net.lukemurphey.nsia.web.forms;

/**
 * Describes a field whose value is not valid. This class is typically used for forms processing.
 * @author Luke
 *
 */
public class FieldError {

	private String name;
	private String value;
	private String message;
	
	public FieldError( String name, String value, String message ){
		this.name = name;
		this.value = value;
		this.message = message;
	}
	
	public String getName(){
		return name;
	}
	
	public String getValue(){
		return value;
	}
	
	public String getMessage(){
		return message;
	}
}
