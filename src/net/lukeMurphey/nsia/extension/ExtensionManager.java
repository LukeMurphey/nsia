package net.lukemurphey.nsia.extension;

import java.util.Vector;

import net.lukemurphey.nsia.response.CommandActionExtension;
import net.lukemurphey.nsia.response.EmailActionExtension;
import net.lukemurphey.nsia.response.LogFileActionExtension;
import net.lukemurphey.nsia.response.SSHCommandActionExtension;

public class ExtensionManager {
	
	private Vector<Extension> extensions = new Vector<Extension>();
	private static ExtensionManager globalExtensionManager = null;
	
	private ExtensionManager(){
		addExtension(new CommandActionExtension());
		addExtension(new LogFileActionExtension());
		addExtension(new SSHCommandActionExtension());
		addExtension(new EmailActionExtension());
	}
	
	public Extension[] getAllExtensions(){
		Extension[] ext = new Extension[extensions.size()];
		extensions.toArray(ext);
		
		return ext;
	}
	
	public Extension[] getExtensions( ExtensionType extensionType ){
		
		Vector<Extension> matchingExtensions = new Vector<Extension>();
		
		for (Extension ext : extensions) {
			if( extensionType == ext.getExtensionType() ){
				matchingExtensions.add(ext);
			}
		}
		
		Extension[] tmp = new Extension[extensions.size()];
		matchingExtensions.toArray(tmp);
		
		return tmp;
	}
	
	public Extension getExtension( ExtensionType extensionType, String name ){
		
		for (Extension ext : extensions) {
			if( extensionType == ext.getExtensionType() && ext.getName().equals(name) ){
				return ext;
			}
		}
		
		return null;
	}
	
	private void addExtension(Extension extension){
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the extension is not null
		if( extension == null ){
			throw new IllegalArgumentException("The extension to add cannot be null");
		}
		
		
		// 1 -- Add it to the list
		extensions.add(extension);
	}
	
	public static ExtensionManager getExtensionManager(){
		
		if( globalExtensionManager == null ){
			globalExtensionManager = new ExtensionManager();
		}
		
		return globalExtensionManager;
	}
	
}
