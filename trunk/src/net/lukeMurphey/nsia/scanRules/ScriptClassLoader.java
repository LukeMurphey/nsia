package net.lukeMurphey.nsia.scanRules;


import java.util.Hashtable;
import java.util.Vector;

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
				
				temp.put("ThreatScript.Result", "net.lukeMurphey.nsia.scanRules.Result");
				temp.put("HTTP.URL", "java.net.URL");
				temp.put("HTTP.TagNameFilter", "org.htmlparser.filters.TagNameFilter");
				temp.put("ThreatScript.DataAnalysis", "net.lukeMurphey.nsia.scanRules.ScriptSignatureUtils");
				
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
		
		//System.out.println( "Looking for :" + className );
		
		String resolvedClass = SHORTCUTS.get(className);
		
		if( resolvedClass != null ){
			//System.out.println( "Accepted (shortcut):" + className );
			return super.loadClass(resolvedClass);
		}
		else{
			if( WHITELIST.contains(className) ){
				//System.out.println( "Accepted (whitelist):" + className );
				return super.loadClass(className);
			}
			else{
				//System.out.println( "Rejected:" + className );
				throw new ClassNotFoundException();
			}
		}
	}
	
}
