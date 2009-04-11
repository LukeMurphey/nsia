package net.lukeMurphey.nsia.trustBoundary;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.InsufficientPermissionException;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.extension.Extension;
import net.lukeMurphey.nsia.extension.ExtensionManager;
import net.lukeMurphey.nsia.extension.ExtensionType;

public class ApiExtensions extends ApiHandler{

	private ExtensionManager extensionManager;
	
	public ApiExtensions(Application appRes) {
		super(appRes);
		extensionManager = ExtensionManager.getExtensionManager();
	}

	public Extension[] getAllExtensions( String sessionIdentifier ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has sufficient permissions
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		
		// 1 -- Get the extensions
		return extensionManager.getAllExtensions();
	}
	
	public Extension getExtension( String sessionIdentifier, ExtensionType type, String name ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has sufficient permissions
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		
		// 1 -- Get the extensions
		return extensionManager.getExtension(type, name);
	}
	
	public Extension[] getExtensions( String sessionIdentifier, ExtensionType type ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has sufficient permissions
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		
		// 1 -- Get the extensions
		return extensionManager.getExtensions(type);
	}
	
}
