package net.lukemurphey.nsia;

import java.io.IOException;

import net.lukemurphey.nsia.rest.ApplicationVersionInfo;
import net.lukemurphey.nsia.rest.RESTRequestFailedException;

/**
 * Provides mechanisms for determining if a newer version of NSIA is available.
 * @author Luke
 *
 */
public class VersionManagement {

	private static ApplicationVersionDescriptor cachedVersionInfo = null;
	private static boolean versionBeingChecked = false;
	private static long versionLastChecked = -1;
	
	private static synchronized ApplicationVersionDescriptor getVersionID() throws RESTRequestFailedException, IOException{
		
		ApplicationVersionInfo appInfo = new ApplicationVersionInfo();
		
		ApplicationVersionDescriptor result = appInfo.getCurrentApplicationVersion();
		
		
        if ( result != null ){
        	cachedVersionInfo = result;
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
	public static boolean isNewerVersionAvailable( boolean dontBlock ) throws RESTRequestFailedException, IOException{
		
		ApplicationVersionDescriptor latestAvailable = getNewestVersionAvailableID(dontBlock);
		
		if( latestAvailable == null ){
			return false;
		}
		
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
	 * @throws RESTRequestFailedException
	 * @throws IOException
	 */
	public static synchronized ApplicationVersionDescriptor getNewestVersionAvailableID( boolean dontBlock ) throws RESTRequestFailedException, IOException{
		
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
	
	/**
	 * Returns a boolean indicating if a newer version of the application is available.
	 * @return
	 * @throws RESTRequestFailedException
	 * @throws IOException
	 */
	public static boolean isUpdateAvailable() throws RESTRequestFailedException, IOException{
		ApplicationVersionDescriptor latestAvailable = getNewestVersionAvailableID( false );
		
		ApplicationVersionDescriptor current = new ApplicationVersionDescriptor(Application.getVersion());
		
		if( ApplicationVersionDescriptor.isLaterVersion(current, latestAvailable) ){
			return true;
		}
		else{
			return true;
		}
	}
	
}
