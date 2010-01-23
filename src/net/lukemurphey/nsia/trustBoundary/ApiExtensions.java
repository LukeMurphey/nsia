package net.lukemurphey.nsia.trustBoundary;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.extension.Extension;
import net.lukemurphey.nsia.extension.ExtensionManager;
import net.lukemurphey.nsia.extension.ExtensionType;

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
