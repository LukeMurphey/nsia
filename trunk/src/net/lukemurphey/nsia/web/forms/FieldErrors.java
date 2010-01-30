package net.lukemurphey.nsia.web.forms;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

/**
 * Provides a hashed-based lookup of field errors.
 * @author Luke
 *
 */
public class FieldErrors /*implements Map<String, FieldError>*/{
	private HashMap<String, FieldError> errors = new HashMap<String, FieldError>();

	public void clear() {
		errors.clear();
	}

	public FieldError get(Object key) {
		return errors.get(key);
	}

	public boolean isEmpty() {
		return errors.isEmpty();
	}
	
	public FieldError put(FieldError value) {
		//return errors.put(value.getName().toLowerCase(), value);
		return errors.put(value.getName(), value);
	}

	public int size() {
		return errors.size();
	}
	
	public boolean fieldHasError(String field){
		if( getError(field) != null ){
			return true;
		}
		else{
			return false;
		}
	}
	
	public FieldError getError(String field){
		return errors.get(field);
	}
	
	public List<FieldError> values() {
		Vector<FieldError> t = new Vector<FieldError>();
		
		for (FieldError fieldError : errors.values()) {
			t.add(fieldError);
		}
		
		return t;
	}
	
	
}