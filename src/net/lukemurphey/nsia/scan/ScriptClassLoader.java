package net.lukemurphey.nsia.scan;

import net.lukemurphey.nsia.scan.scriptenvironment.DeleteMethod;
import net.lukemurphey.nsia.scan.scriptenvironment.GetMethod;
import net.lukemurphey.nsia.scan.scriptenvironment.HeadMethod;
import net.lukemurphey.nsia.scan.scriptenvironment.JSoup;
import net.lukemurphey.nsia.scan.scriptenvironment.OptionsMethod;
import net.lukemurphey.nsia.scan.scriptenvironment.PostMethod;
import net.lukemurphey.nsia.scan.scriptenvironment.PutMethod;
import net.lukemurphey.nsia.scan.scriptenvironment.Result;
import net.lukemurphey.nsia.scan.scriptenvironment.TraceMethod;

import java.net.URL;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;
import java.util.regex.Pattern;


/**
 * This class loader is intended for use within the scripting environments. It exists for two reasons:
 * <ul>
 * 	<li>To restrict the classes that signature scripts can load (don't give the scripts undue functionality, such as the ability to open listening network sockets)</li>
 * 	<li>To provide shortcuts to loadable class (prevents having to use messy importPackage statements)</li>
 * </ul>
 * @author luke
 *
 */

public class ScriptClassLoader extends ClassLoader {
	
	private static Vector<String> WHITELIST = null;
	private static Hashtable<String, String> SHORTCUTS = null; 
	private static final Object INITIALIZE_MUTEX = new Object();
	
	public ScriptClassLoader(){
        super( ScriptClassLoader.class.getClassLoader() );
        
        if( SHORTCUTS == null ){
	        synchronized (INITIALIZE_MUTEX) {
	        	populateShortcuts();
	        }
        }
    }
	
	private static void populateShortcuts(){
		synchronized (INITIALIZE_MUTEX) {
			if( SHORTCUTS == null ){
				Hashtable<String, String> temp = new Hashtable<String, String>();
				
				temp.put("ThreatScript.Result", Result.class.getName());
				temp.put("HTTP.URL", URL.class.getName());
				temp.put("HTTP.TagNameFilter", org.htmlparser.filters.TagNameFilter.class.getName());
				temp.put("HTTP.JSoup", JSoup.class.getName());
				 
				//Web request methods
				temp.put("HTTP.GetRequest", GetMethod.class.getName());
				temp.put("HTTP.PostRequest", PostMethod.class.getName());
				temp.put("HTTP.DeleteRequest", DeleteMethod.class.getName());
				temp.put("HTTP.PutRequest", PutMethod.class.getName());
				temp.put("HTTP.TraceRequest", TraceMethod.class.getName());
				temp.put("HTTP.HeadRequest", HeadMethod.class.getName());
				temp.put("HTTP.OptionsRequest", OptionsMethod.class.getName());
				
				temp.put("ThreatScript.DataAnalysis", ScriptSignatureUtils.class.getName() );
				temp.put("ThreatScript.Vector", net.lukemurphey.nsia.scan.scriptenvironment.Vector.class.getName() );
				temp.put("ThreatScript.Date", Date.class.getName() );
				temp.put("ThreatScript.Pattern", Pattern.class.getName() );
				
				SHORTCUTS = temp;
			}
			
			if( WHITELIST == null ){
				Vector<String> temp = new Vector<String>();
				
				temp.add("sun.org.mozilla.javascript.internal.ContextFactory");
				
				WHITELIST = temp;
			}
		}
		
	}
	
	public Class<?> loadClass(String className) throws ClassNotFoundException {
		
		String resolvedClass = SHORTCUTS.get(className);
		
		if( resolvedClass != null ){
			return super.loadClass(resolvedClass);
		}
		else{
			if( WHITELIST.contains(className) ){
				return super.loadClass(className);
			}
			else{
				throw new ClassNotFoundException("Class " + className + " not found");
			}
		}
	}
	
}
