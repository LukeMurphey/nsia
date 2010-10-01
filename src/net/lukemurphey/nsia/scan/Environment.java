package net.lukemurphey.nsia.scan;

import java.io.Externalizable;
import java.io.Serializable;

import net.lukemurphey.nsia.scan.ScriptDefinition.NameValuePair;
import net.lukemurphey.nsia.scan.ScriptDefinition.SavedScriptData;
import sun.org.mozilla.javascript.internal.NativeArray;
import sun.org.mozilla.javascript.internal.NativeJavaObject;

/**
 * The environment class stores values that script definitions need to store.
 * @author Luke
 *
 */
public class Environment{
	private SavedScriptData data;
	
	public Environment(SavedScriptData data){
		this.data = data;
	}
	
	/**
	 * Get the item that corresponds with the name provided (or null if it does not exist).
	 * @param name The name of the parameter to find
	 * @return
	 */
	public NameValuePair get(String name){
		return getWrapped( name );
	}
	
	/**
	 * Get the item that corresponds with the name provided (or null if it does not exist).
	 * @param name The name of the parameter to find
	 * @param getUnwrapped Unwrap the object to the original value
	 * @return
	 */
	public Object get(String name, boolean getUnwrapped){
		if( getUnwrapped ){
			return getUnwrapped( name );
		}
		else{
			return getWrapped( name );
		}
	}
	
	/**
	 * Get the item that corresponds with the name provided (or null if it does not exist).
	 * @param name The name of the parameter to find
	 * @return
	 */
	private NameValuePair getWrapped(String name){
		return data.get(name);
	}
	
	/**
	 * Get the item that corresponds with the name provided (or null if it does not exist).
	 * @param name The name of the parameter to find
	 * @return
	 */
	private Object getUnwrapped(String name){
		NameValuePair nv = data.get(name);
		
		if( nv != null ){
			return nv.getValue();
		}
		else{
			return null;
		}
	}
	
	/**
	 * Set the name with the given value.
	 * @param name The name of the parameter to find
	 * @param value The value of the parameter to set
	 */
	public void set(String name, int value){
		data.set(name, Integer.valueOf( value ));
	}
	
	/**
	 * Set the name with the given value.
	 * @param name The name of the parameter to find
	 * @param value The value of the parameter to set
	 */
	public void set(String name, short value){
		data.set(name, Short.valueOf( value ));
	}
	
	/**
	 * Set the name with the given value.
	 * @param name The name of the parameter to find
	 * @param value The value of the parameter to set
	 */
	public void set(String name, long value){
		data.set(name, Long.valueOf( value ) );
	}
	
	/**
	 * Set the name with the given value.
	 * @param name The name of the parameter to find
	 * @param value The value of the parameter to set
	 */
	public void set(String name, float value){
		data.set(name, Float.valueOf( value) );
	}
	
	/**
	 * Set the name with the given value.
	 * @param name The name of the parameter to find
	 * @param value The value of the parameter to set
	 */
	public void set(String name, boolean value){
		data.set(name, Boolean.valueOf(value));
	}
	
	/**
	 * Set the name with the given value.
	 * @param name The name of the parameter to find
	 * @param value The value of the parameter to set
	 */
	public void set(String name, double value){
		data.set(name, Double.valueOf(value));
	}
	
	/**
	 * Set the name with the given value.
	 * @param name The name of the parameter to find
	 * @param value The value of the parameter to set
	 */
	public void set(String name, char value){
		data.set(name, Character.valueOf( value ));
	}
	
	/**
	 * The scripting engine may try to set an array which corresponds to a NativeArray class.
	 * This class must be converted to an Object array in order to be serialized correctly.
	 * Additionally, each object within the array may need to be unwrapped to the original
	 * object in order for serialization to be possible. This method will perform the necessary
	 * conversions so that the result can be serialized correctly.
	 * @param name
	 * @param arr
	 * @param isSpecimenSpecific
	 */
	public void set(String name, NativeArray arr, boolean isSpecimenSpecific){

		Object [] array = new Object[(int) arr.getLength()];
		for (Object o : arr.getIds()) {
		    int index = (Integer) o;
		    Object object = arr.get(index, null);
		    
		    // Unwrap the original object if wrapped in a Native Java Object
		    if( object instanceof NativeJavaObject){
		    	NativeJavaObject n = (NativeJavaObject)object;
		    	array[index] = n.unwrap();
		    }
		    else{
		    	array[index] = object;
		    }
		}
		
		data.set(name, array, isSpecimenSpecific);
	}
	
	/**
	 * Set the name with the given value. This method accepts Javascript native arrays and converts them to a serializable array.
	 * @param name The name of the parameter to find
	 * @param arr The value of the parameter to set (a NativeArray)
	 */
	public void set(String name, NativeArray arr){
		set(name, arr, true);
	}
	
	/**
	 * Set the name with the given value.
	 * @param name The name of the parameter to find
	 * @param value The value of the parameter to set
	 */
	public void set(String name, Serializable value){
		data.set(name, value);
	}
	
	/**
	 * Set the name with the given value.
	 * @param name The name of the parameter to find
	 * @param value The value of the parameter to set
	 */
	public void set(String name, Externalizable value){
		data.set(name, value);
	}
	
	/**
	 * Set the name with the given value.
	 * @param name The name of the parameter to find
	 * @param value The value of the parameter to set
	 * @param isSpecimenSpecific Defines whether or not this value should be returned only for the exact same specimen
	 */
	public void set(String name, int value, boolean isSpecimenSpecific){
		data.set(name, Integer.valueOf( value ), isSpecimenSpecific);
	}
	
	/**
	 * Set the name with the given value.
	 * @param name The name of the parameter to find
	 * @param value The value of the parameter to set
	 * @param isSpecimenSpecific Defines whether or not this value should be returned only for the exact same specimen
	 */
	public void set(String name, short value, boolean isSpecimenSpecific){
		data.set(name, Short.valueOf( value ), isSpecimenSpecific);
	}
	
	/**
	 * Set the name with the given value.
	 * @param name The name of the parameter to find
	 * @param value The value of the parameter to set
	 * @param isSpecimenSpecific Defines whether or not this value should be returned only for the exact same specimen
	 */
	public void set(String name, long value, boolean isSpecimenSpecific){
		data.set(name, Long.valueOf( value ), isSpecimenSpecific );
	}
	
	/**
	 * Set the name with the given value.
	 * @param name The name of the parameter to find
	 * @param value The value of the parameter to set
	 * @param isSpecimenSpecific Defines whether or not this value should be returned only for the exact same specimen
	 */
	public void set(String name, float value, boolean isSpecimenSpecific){
		data.set(name, Float.valueOf( value), isSpecimenSpecific );
	}
	
	/**
	 * Set the name with the given value.
	 * @param name The name of the parameter to find
	 * @param value The value of the parameter to set
	 * @param isSpecimenSpecific Defines whether or not this value should be returned only for the exact same specimen
	 */
	public void set(String name, boolean value, boolean isSpecimenSpecific){
		data.set(name, Boolean.valueOf(value), isSpecimenSpecific);
	}
	
	/**
	 * Set the name with the given value.
	 * @param name The name of the parameter to find
	 * @param value The value of the parameter to set
	 * @param isSpecimenSpecific Defines whether or not this value should be returned only for the exact same specimen
	 */
	public void set(String name, double value, boolean isSpecimenSpecific){
		data.set(name, Double.valueOf(value), isSpecimenSpecific);
	}
	
	/**
	 * Set the name with the given value.
	 * @param name The name of the parameter to find
	 * @param value The value of the parameter to set
	 * @param isSpecimenSpecific Defines whether or not this value should be returned only for the exact same specimen
	 */
	public void set(String name, char value, boolean isSpecimenSpecific){
		data.set(name, Character.valueOf( value ), isSpecimenSpecific);
	}
	
	/**
	 * Set the name with the given value.
	 * @param name The name of the parameter to find
	 * @param value The value of the parameter to set
	 * @param isSpecimenSpecific Defines whether or not this value should be returned only for the exact same specimen
	 */
	public void set(String name, Serializable value, boolean isSpecimenSpecific){
		data.set(name, value, isSpecimenSpecific);
	}
	
	/**
	 * Set the name with the given value.
	 * @param name The name of the parameter to find
	 * @param value The value of the parameter to set
	 * @param isSpecimenSpecific Defines whether or not this value should be returned only for the exact same specimen
	 */
	public void set(String name, Externalizable value, boolean isSpecimenSpecific){
		data.set(name, value, isSpecimenSpecific);
	}
	
	/**
	 * Remove the value with the given name.
	 * @param name The name of the parameter to remove
	 */
	public void remove(String name){
		data.remove(name);
	}
}