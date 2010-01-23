package net.lukemurphey.nsia.web.forms;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Provides a hashed-based lookup of field errors.
 * @author Luke
 *
 */
public class FieldErrors implements Map<String, FieldError>{
	private HashMap<String, FieldError> errors = new HashMap<String, FieldError>();

	@Override
	public void clear() {
		errors.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return errors.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return errors.containsValue(value);
	}

	@Override
	public Set<java.util.Map.Entry<String, FieldError>> entrySet() {
		return errors.entrySet();
	}

	@Override
	public FieldError get(Object key) {
		//return errors.get(key.toString().toLowerCase());
		return errors.get(key);
	}

	@Override
	public boolean isEmpty() {
		return errors.isEmpty();
	}

	@Override
	public Set<String> keySet() {
		return errors.keySet();
	}

	@Override
	public FieldError put(String key, FieldError value) {
		//return errors.put(key.toLowerCase(), value);
		return errors.put(key, value);
	}
	
	public FieldError put(FieldError value) {
		//return errors.put(value.getName().toLowerCase(), value);
		return errors.put(value.getName(), value);
	}

	@Override
	public void putAll(Map<? extends String, ? extends FieldError> m) {
		errors.putAll(m);
		/*
		Set<? extends String> keys = m.keySet();
		
		for (String key : keys) {
			errors.put(key.toLowerCase(), m.get(key) );
		}*/
	}

	@Override
	public FieldError remove(Object key) {
		return errors.remove(key);
	}

	@Override
	public int size() {
		return errors.size();
	}

	@Override
	public Collection<FieldError> values() {
		return errors.values();
	}
	
	
}