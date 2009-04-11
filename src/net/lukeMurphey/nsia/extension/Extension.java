package net.lukeMurphey.nsia.extension;

import org.apache.commons.lang.StringUtils;

import java.util.Date;
import java.util.Hashtable;

public abstract class Extension {

	private String name;
	private String description;
	private ExtensionType extensionType;
	
	protected int versionMajor;
	protected int versionMinor;
	protected int revision;
	
	protected Date lastUpdated;
	
	protected Extension(String name, String description, ExtensionType extensionType){
		
		// 0 -- Precondition check
		// Note that the name and description will be checked in the setter methods following the precondition check
		if( extensionType == null ){
			throw new IllegalArgumentException("The ExtensionType must not be null");
		}
		
		// 1 -- Initialize the class
		setName(name);
		setDescription(description);
		this.extensionType = extensionType;
		
	}
	
	protected void setName(String newName){
		
		// 0 -- Precondition check
		if( newName == null ){
			throw new IllegalArgumentException("The name must not be null");
		}
		
		if( StringUtils.isBlank(newName)){
			throw new IllegalArgumentException("The name must not be blank");
		}
		
		// 1 -- Set the name
		name = newName;
	}
	
	protected void setDescription(String desc){
		
		// 0 -- Precondition check
		if( desc == null ){
			throw new IllegalArgumentException("The name must not be null");
		}
		
		if( StringUtils.isBlank(desc) ){
			throw new IllegalArgumentException("The description must not be blank");
		}
		
		// 1 -- Set the description
		description = desc;
	}
	
	public String getName(){
		return name;
	}
	
	public String getDescription(){
		return description;
	}
	
	public int getMajorVersion(){
		return versionMajor;
	}
	
	public int getMinorVersion(){
		return versionMinor;
	}
	
	public int getRevision(){
		return revision;
	}

	public Date getDateLastUpdated(){
		return (Date)lastUpdated.clone();
	}
	
	public abstract void install() throws ExtensionInstallationException;
	
	public abstract void uninstall() throws ExtensionRemovalException;
	
	public ExtensionType getExtensionType(){
		return extensionType;
	}
	
	public abstract Object createInstance(Hashtable<String, String> arguments ) throws ArgumentFieldsInvalidException;
	
	public abstract FieldLayout getFieldLayout();
	
	public abstract PrototypeField[] getFields();
	
}
