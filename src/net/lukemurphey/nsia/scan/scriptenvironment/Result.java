package net.lukemurphey.nsia.scan.scriptenvironment;

import java.net.URL;
import java.util.Vector;

import net.lukemurphey.nsia.scan.URLToScan;

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
	public Vector<URLToScan> urls = new Vector<URLToScan>();
	
	public Result( Vector<URL> urls ){
		this.matched = false;
		
		if( urls != null ){
			addURLs(urls);
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
	
	public Vector<URLToScan> getURLs(){
		return urls;
	}
	
	public int addURLs( Vector<URL> urls ){
		return addURLs( urls, URLToScan.IGNORE_DOMAIN_RESTRICTION_DEFAULT);
	}
	
	public int addURLs( Vector<URL> urls, boolean ignoreDomainRestriction ){

		for (URL url : urls) {
			this.urls.add( new URLToScan(url, ignoreDomainRestriction) );
		}
		
		return urls.size();
	}
	
	public void addURL( URL url, boolean ignoreDomainRestriction ){
		urls.add(  new URLToScan( url, ignoreDomainRestriction ) );
	}
	
	public void addURL( NativeJavaObject url, boolean ignoreDomainRestriction ){
		
		Object urlUnWrapped = null;
		
		// Unwrap the item if necessary
		if( url instanceof NativeJavaObject ){
			NativeJavaObject njo = (NativeJavaObject) url;
			urlUnWrapped = njo.unwrap();
		}
		
		// Make sure the type is correct
		if( urlUnWrapped instanceof URL ){
			addURL( (URL)urlUnWrapped, ignoreDomainRestriction);
		}		
		
	}
	
	public int addURLs( NativeArray arr ){
		return addURLs( arr, URLToScan.IGNORE_DOMAIN_RESTRICTION_DEFAULT);
	}
	
	public int addURLs( NativeArray arr, boolean ignoreDomainRestriction ){

		for (Object o : arr.getIds()) {
			int index = (Integer) o;
			Object object = arr.get(index, null);
			
			// Unwrap the item if necessary
			if( object instanceof NativeJavaObject ){
				addURL( (NativeJavaObject)object, ignoreDomainRestriction);
			}
			else if( object instanceof URL ){
				addURL( (URL)object, ignoreDomainRestriction);
			}
		}
		
		return urls.size();
	}
}
