package net.lukemurphey.nsia.scan.scriptenvironment;

import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;

import sun.org.mozilla.javascript.internal.NativeArray;
import sun.org.mozilla.javascript.internal.NativeJavaObject;

/**
 * This class provides a storage mechanism to the ThreatScript definitions that handles serializes and deserializes to the same type (default
 * arrays within the ScriptEngine do not). This class will also perform conversions as necessary to ensure that the objects added are unwrapped
 * or converted as necessary to ensure that they are stored and retrieved correctly.  
 * @author Luke
 *
 */
public class Vector implements Serializable, Iterable<Object>{

	private static final long serialVersionUID = 5884639481148363627L;

	private java.util.Vector<Object> items = null;

	/**
	 * Default constructor
	 */
	public Vector(){
		items = new java.util.Vector<Object>();
	}

	/**
	 * Copy constructor
	 * @param v
	 */
	public Vector( Vector v ){
		items = new java.util.Vector<Object>(v.length() + 10);

		pushAll(v);
	}

	/**
	 * Constructor that copies items from an existing NativeArray object (which is provided by the ScriptEngine when a Java
	 * method is called from a script).
	 * @param arr
	 */
	public Vector( NativeArray arr ){

		items = new java.util.Vector<Object>( ((int) arr.getLength()) + 10 );

		pushAll(arr);
	}

	/**
	 * Constructor with default capacity.
	 * @param initialCapacity
	 */
	public Vector( int initialCapacity ){
		items = new java.util.Vector<Object>( initialCapacity );
	}

	/**
	 * Retrieves the number of items stored.
	 * @return
	 */
	public int length(){
		return items.size();
	}

	public int pushAll( NativeArray arr ){

		items = new java.util.Vector<Object>( ((int) arr.getLength()) + 10 );

		for (Object o : arr.getIds()) {
			int index = (Integer) o;
			Object object = arr.get(index, null);

			push(object);
		}

		return length();
	}

	/**
	 * Add all of the items given to the end of the list.
	 * @param v
	 * @return
	 */
	public int pushAll( Vector v ){

		for (Object object : v.items) {
			push(processObject(object));
		}

		return length();
	}



	/**
	 * Provide a string of all of the items (based on repeated calls to <i>toString()</i>).
	 * @return
	 */
	public String join(){
		return join(null);
	}

	/**
	 * Remove and return the first object in the list.
	 * @return
	 */
	public Object shift(){
		Object obj = items.get( 0 );
		items.remove( obj );
		return obj;
	}

	/**
	 * Add the provided objects to the front of the list.
	 * @param objects
	 * @return
	 */
	public int unshift( Object... objects ){

		for( int c = objects.length - 1; c >= 0; c--){
			items.add(0, processObject(objects[c]));
		}

		return length();
	}

	/**
	 * Provide a string of all of the items (based on repeated calls to <i>toString()</i>).
	 * @param str The string to place between each item 
	 * @return
	 */
	public String join( String str ){
		StringBuffer b = new StringBuffer();

		for (Object obj : items) {
			if( b.length() > 0 && str != null ){
				b.append( str );
			}

			b.append(obj);
		}

		return b.toString();
	}

	/**
	 * Add the given item to the end of the list.
	 * @param obj
	 * @return
	 */
	public int push( Object obj ){
		items.add( processObject(obj) );
		return length();
	}

	/**
	 * Process the object to unwrap the contained class.
	 * @param object
	 * @return
	 */
	private Object processObject( Object object ){
		if( object instanceof NativeJavaObject){
			NativeJavaObject n = (NativeJavaObject)object;
			return n.unwrap();
		}

		return object;
	}

	/**
	 * Remove and return the first item.
	 * @return
	 */
	public Object pop(){
		Object obj = items.get( items.size() - 1 );
		items.remove( obj );
		return obj;
	}

	/**
	 * Get the object at the given index.
	 * @param index
	 * @return
	 */
	public Object get(int index){
		return items.get(index);
	}

	/**
	 * Set the object at the given index.
	 * @param index
	 * @param obj
	 * @return
	 */
	public Object set(int index, Object obj){
		return items.set(index, processObject(obj));
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException{
		out.writeObject(items);
	}

	@SuppressWarnings("unchecked")
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException{
		items = (java.util.Vector<Object>)in.readObject();
	}

	public Iterator<Object> iterator() {
		return items.iterator();
	}

}
