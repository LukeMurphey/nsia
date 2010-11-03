package net.lukemurphey.nsia.scan.scriptenvironment;

import java.net.URL;
import java.util.Vector;

import sun.org.mozilla.javascript.internal.NativeArray;
import sun.org.mozilla.javascript.internal.NativeJavaObject;

/**
 * This class communicates the result of a scan and is intended to be used by the signature scripts.
 * @author luke
 *
 */
public class Result{
	public boolean matched = false;
	public String description = null;
	public int detectStart;
	public int detectEnd;
	public Vector<URL> urls = new Vector<URL>();
	
	public Result( Vector<URL> urls ){
		this.matched = false;
		this.urls = new Vector<URL>();
		if( urls != null ){
			this.urls.addAll(urls);
		}
	}
	
	public Result( NativeArray extractedURLs){
		
		if( extractedURLs != null) {
			addURLs(extractedURLs);
		}
	}
	
	public Result(boolean matched, String description){
		this.matched = matched;
		this.description = description;
	}
	
	public Result(boolean matched, String description, NativeArray extractedURLs){
		this.matched = matched;
		this.description = description;
		
		if( extractedURLs != null) {
			addURLs(extractedURLs);
		}
	}
	
	public Result(boolean matched ){
		this.matched = matched;
	}
	
	public boolean matched(){
		return matched;
	}
	
	public String getDescription(){
		return description;
	}
	
	public int getDetectionStart(){
		return detectStart;
	}
	
	public int getDetectionEnd(){
		return detectEnd;
	}
	
	public Vector<URL> getURLs(){
		return urls;
	}
	
	public int addURLs( NativeArray arr ){

		for (Object o : arr.getIds()) {
			int index = (Integer) o;
			Object object = arr.get(index, null);
			
			// Unwrap the item if necessary
			if( object instanceof NativeJavaObject ){
				NativeJavaObject njo = (NativeJavaObject) object;
				object = njo.unwrap();
			}
			
			// Make sure the type is correct
			if( object instanceof URL ){
				urls.add((URL)object);
			}
		}
		
		return urls.size();
	}
}
