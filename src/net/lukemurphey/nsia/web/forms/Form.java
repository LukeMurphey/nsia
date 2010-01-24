package net.lukemurphey.nsia.web.forms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

/**
 * Represents a web form and performs validation to ensure that the form elements are acceptable.
 * @author Luke
 *
 */
public class Form {
	
	private HashMap<String,Field> fields = new HashMap<String,Field>();
	
	/**
	 * Add the given field to the list. The existing field will be overwritten if it already exists.
	 * @param field
	 */
	public void addField(Field field){
		fields.put(field.getName().toLowerCase(), field);
	}
	
	/**
	 * Gets the field associated with the given name.
	 * @param name
	 * @return
	 */
	public Field getField( String name ){
		return fields.get(name);
	}
	
	/**
	 * Add the field to the list if it is not in the list already.
	 * @param fieldsUsed
	 * @param field
	 */
	private void addIfUnique( ArrayList<Field> fieldsUsed, Field field){
		for (Field existingField : fieldsUsed) {
			if( existingField == field ){
				return;
			}
		}
		
		fieldsUsed.add(field);
	}
	
	@SuppressWarnings("unchecked")
	public FieldErrors validate( HttpServletRequest request ){
		FieldErrors errors = new FieldErrors();
		Map params = request.getParameterMap();
		Set<String> paramNames = params.keySet();
		ArrayList<Field> fieldsUsed = new ArrayList();
		
		// 1 -- Check all of the fields and make sure the values are valid
		for (String fieldName: fields.keySet()) {
			
			// 1.1 -- Get the associated form field
			Field field = fields.get(fieldName);
			
			// 1.2 -- Validate the arguments provided
			for (String paramName : paramNames) {
				if( field.getName().equalsIgnoreCase(paramName) ){
					String[] args = request.getParameterValues(paramName);
					
					for (String arg : args) {
						FieldValidatorResponse response = field.validate(arg);
						
						addIfUnique( fieldsUsed, field );
						
						if( response.isValid() == false ){
							errors.put( new FieldError(field.getName(), arg, response.getMessage() ) );
							break; //Found at least one argument with an error, no need to continue checking this field
						}
					}
					 
				}
			}
		}
		
		// 2 -- Make sure no required arguments are missing
		Set<String> fieldKeys = fields.keySet();
		
		for (String fieldName : fieldKeys) {
			
			Field field = fields.get(fieldName);
			
			// If the field is required, then determine if the field was supplied
			if( field.isRequired() == true ){
				
				boolean found = false; 
				
				// Check each used field to determine if was supplied
				for (Field usedField : fieldsUsed) {
					if(usedField == field ){
						found = true;
						break;
					}
				}
				
				// If the field was not found, then note it as an error
				if( found == false ){
					errors.put( new FieldError(field.getName(), field.getName(), "This field is required" ) );
				}
			}
		}
		
		return errors;
	}

}
