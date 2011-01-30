package net.lukemurphey.nsia;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents the version of NSIA and allows version identifiers to be compared.
 * @author Luke
 *
 */
public class ApplicationVersionDescriptor{
	private static Pattern VERSION_REGEX = Pattern.compile("([0-9]*)?([.]([0-9]*))?([.]([0-9]*))?[ ]*(\\((.*)\\))?"); //([0-9]*)?([.]([0-9]*))?([.]([0-9]*))?[ ]*(\((.*)\))? 
	
	private int major;
	private int minor = -1;
	private int revision = -1;
	private String branch;
	private Date releaseDate = null;
	
	public ApplicationVersionDescriptor( String version ){
		this(version, null);
	}
	
	public ApplicationVersionDescriptor( String version, Date releaseDate ){
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
		
		this.releaseDate = releaseDate;
	}
	
	public Date getReleaseDate(){
		return releaseDate;
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
	
	/**
	 * Determines if a later version exists than the current version of NSIA based on the two version descriptors provided.
	 * @param current
	 * @param latestAvailable
	 * @return
	 */
	public static boolean isLaterVersion( ApplicationVersionDescriptor current, ApplicationVersionDescriptor latestAvailable ){
		
		if( latestAvailable.getBranch() != null && latestAvailable.getBranch().equalsIgnoreCase(current.getBranch()) == false ){
			return false;
		}
		
		else if( latestAvailable.getMajor() > current.getMajor() ){
			return true;
		}
		
		else if( latestAvailable.getMajor() == current.getMajor() && latestAvailable.getMinor() > current.getMinor() ){
			return true;
		}
		
		else if( latestAvailable.getMajor() == current.getMajor() && latestAvailable.getMinor() == current.getMinor() && latestAvailable.getRevision() > current.getRevision() ){
			return true;
		}
		
		return false;
		
	}
}