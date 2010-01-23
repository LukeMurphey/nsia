package net.lukemurphey.nsia;

import java.io.IOException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcException;

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
	
	public static class VersionDescriptor{
		private static Pattern VERSION_REGEX = Pattern.compile("([0-9]*)?([.]([0-9]*))?([.]([0-9]*))?[ ]*(\\((.*)\\))?"); //([0-9]*)?([.]([0-9]*))?([.]([0-9]*))?[ ]*(\((.*)\))? 
		
		private int major;
		private int minor = -1;
		private int revision = -1;
		private String branch;
		
		public VersionDescriptor( String version ){
			Matcher matcher = VERSION_REGEX.matcher(version);
			
			if( matcher.find() ){
				major = Integer.parseInt( matcher.group(1) );
				
				try{
					minor = Integer.parseInt( matcher.group(3) );
				}
				catch(NumberFormatException e){
					minor = -1;
				}
				
				try{
					revision = Integer.parseInt( matcher.group(5) );
				}
				catch(NumberFormatException e){
					revision = -1;
				}
				
				branch = matcher.group(7);
			}
		}
		
		public int getMajor(){
			return major;
		}
		
		public int getMinor(){
			return minor;
		}
		
		public int getRevision(){
			return revision;
		}
		
		public String getBranch(){
			return branch;
		}
		
		public String toString(){
			String versionID = "" + getMajor();
			
			if( getMinor() >= 0 ){
				versionID += "." + getMinor();
				
				if( getRevision() >= 0 ){
					versionID += "." + getRevision();
				}
			}
			
			if( getBranch() != null && getBranch().length() > 0){
				versionID += " (" + getBranch() + ")";
			}
			
			return versionID;
		}
		
		public static boolean isLaterVersion( VersionDescriptor current, VersionDescriptor latestAvailable ){
			
			if( latestAvailable.getBranch() != null && latestAvailable.getBranch().equalsIgnoreCase(current.getBranch()) == false ){
				return false;
			}
			
			else if( latestAvailable.getMajor() > current.getMajor() ){
				return true;
			}
			
			else if( latestAvailable.getMinor() > current.getMinor() ){
				return true;
			}
			
			else if( latestAvailable.getRevision() > current.getRevision() ){
				return true;
			}
			
			return false;
			
		}
	}
	
	public static boolean isNewerVersionAvailableID( boolean dontBlock ) throws XmlRpcException, IOException{
		
		String version = getNewestVersionAvailableID(dontBlock);
		
		if( version == null || version.length() == 0){
			return false;
		}
		
		VersionDescriptor latestAvailable = new VersionDescriptor(version);
		VersionDescriptor current = new VersionDescriptor(Application.getVersion());
		
		if( VersionDescriptor.isLaterVersion(current, latestAvailable) ){
			return true;
		}
		else {
			return false;
		}
	}
	
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
		
		VersionDescriptor latestAvailable = new VersionDescriptor(latestVersion);
		VersionDescriptor current = new VersionDescriptor(Application.getVersion());
		
		if( VersionDescriptor.isLaterVersion(current, latestAvailable) ){
			return true;
		}
		else{
			return true;
		}
	}
	
}
