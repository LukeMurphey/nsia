package net.lukemurphey.nsia;

import java.io.IOException;
import java.util.Vector;


import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcException;

/**
 * Provides mechanisms for determining if a newer version of NSIA is available.
 * @author Luke
 *
 */
public class VersionManagement {

	private static final String NSIA_SUPPORT_API_URL = "https://threatfactor.com/xmlrpc/";
	private static String cachedVersionInfo = null;
	private static boolean versionBeingChecked = false;
	private static long versionLastChecked = -1;
	
	private static synchronized String getVersionID() throws XmlRpcException, IOException{
		XmlRpcClient client = new XmlRpcClient( NSIA_SUPPORT_API_URL );
		
		Vector<String> params = new Vector<String>();
		params.add("release");
		
		Object result = client.execute( "NSIA.latestVersion", params );

		if ( result != null && result instanceof XmlRpcException ){
			throw (XmlRpcException)result;
		}
        if ( result != null && result instanceof String && result.toString().length() > 0 ){
        	cachedVersionInfo = result.toString();
        }
        else{
        	cachedVersionInfo = null;
        }
        
        return cachedVersionInfo;
	}
	
	/**
	 * Indicates if a newer version of NSIA is available.
	 * @param dontBlock
	 * @return
	 * @throws XmlRpcException
	 * @throws IOException
	 */
	public static boolean isNewerVersionAvailableID( boolean dontBlock ) throws XmlRpcException, IOException{
		
		String version = getNewestVersionAvailableID(dontBlock);
		
		if( version == null || version.length() == 0){
			return false;
		}
		
		ApplicationVersionDescriptor latestAvailable = new ApplicationVersionDescriptor(version);
		ApplicationVersionDescriptor current = new ApplicationVersionDescriptor(Application.getVersion());
		
		if( ApplicationVersionDescriptor.isLaterVersion(current, latestAvailable) ){
			return true;
		}
		else {
			return false;
		}
	}
	
	/**
	 * Get the newest available version ID for NSIA.
	 * @param dontBlock
	 * @return
	 * @throws XmlRpcException
	 * @throws IOException
	 */
	public static synchronized String getNewestVersionAvailableID( boolean dontBlock ) throws XmlRpcException, IOException{
		
		// 1.1 -- Determine if the cached version information is recent (check every 4 hours or 14400000 seconds)
		boolean lastCheckFresh = (versionLastChecked + 14400000) > System.currentTimeMillis();
		
		// 1.2 -- Return the existing version information if the connection should not wait for the response or the current version information is up to date
		if( lastCheckFresh || dontBlock == true ){
			
			if( lastCheckFresh == false && versionBeingChecked == false ){
				VersionChecker checker = new VersionChecker();
				checker.start();
			}
			
			return cachedVersionInfo;
		}
		
		// 1.3 -- Get the version information
		else{
			return getVersionID();
		}
	}
	
	/**
	 * This class will download the version information and ensure it is populated such that VersionManagement.getVersionID() will return a cached version of it.
	 * @author Luke
	 *
	 */
	private static class VersionChecker extends Thread{
		
		public UncaughtExceptionHandler exHandler = null;
		
		public void run(){
			try{
				versionBeingChecked = true;
				versionLastChecked = System.currentTimeMillis();
				VersionManagement.getVersionID();
			}
			catch(Exception e){
				if( exHandler != null ){
					exHandler.uncaughtException(this, e);
				}
			}
			finally{
				versionBeingChecked = false;
			}
		}
		
		
	}
	
	public static boolean isUpdateAvailable() throws XmlRpcException, IOException{
		String latestVersion = getNewestVersionAvailableID( false );
		
		ApplicationVersionDescriptor latestAvailable = new ApplicationVersionDescriptor(latestVersion);
		ApplicationVersionDescriptor current = new ApplicationVersionDescriptor(Application.getVersion());
		
		if( ApplicationVersionDescriptor.isLaterVersion(current, latestAvailable) ){
			return true;
		}
		else{
			return true;
		}
	}
	
}
