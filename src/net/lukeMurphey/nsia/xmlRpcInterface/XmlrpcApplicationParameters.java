package net.lukeMurphey.nsia.xmlRpcInterface;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.InsufficientPermissionException;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.trustBoundary.ApiApplicationParameters;

/**
 * The following class serves as an interface to application parameter class that contains various
 * application configuration options.
 * @author luke
 *
 */
public class XmlrpcApplicationParameters extends XmlrpcHandler{

	protected ApiApplicationParameters applicationParameters;
	
	public XmlrpcApplicationParameters(Application appRes) {
		super(appRes);
		
		applicationParameters = new ApiApplicationParameters( appRes );
	}
	
	/**
	 * Get the application configuration parameter with the given name. 
	 * @param name
	 * @param defaultValue
	 * @return
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException 
	 * @throws NoSessionException 
	 */
	public long getParameter( String sessionIdentifier, String name, int defaultValue ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		return applicationParameters.getParameter( sessionIdentifier, name, defaultValue );
	}
	
	/**
	 * Get the application configuration parameter with the given name.
	 * @param name
	 * @param defaultValue
	 * @return
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException 
	 * @throws NoSessionException 
	 */
	public String getParameter( String sessionIdentifier, String name, String defaultValue ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		String value = applicationParameters.getParameter( sessionIdentifier, name, defaultValue);
		
		if( value != null )
			return value;
		else
			return EMPTY_STRING;
	}
	
	/**
	 * Set the application configuration parameter with the given name.
	 * @param name
	 * @param defaultValue
	 * @return
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException 
	 * @throws NoSessionException 
	 */
	public void setParameter( String sessionIdentifier, String name, String defaultValue ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		applicationParameters.setParameter( sessionIdentifier, name, defaultValue);
	}
	
	/**
	 * Set the application configuration parameter with the given name.
	 * @param name
	 * @param defaultValue
	 * @return
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException 
	 * @throws NoSessionException 
	 */
	public void setParameter( String sessionIdentifier, String name, int defaultValue ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		applicationParameters.setParameter(sessionIdentifier, name, String.valueOf( defaultValue ));
	}
	
	/**
	 * Determine if the parameter has been defined.
	 * @param name
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException 
	 * @throws NoSessionException 
	 */
	public boolean doesParameterExist( String sessionIdentifier, String name ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		return applicationParameters.doesParameterExist( sessionIdentifier, name );
	}

}
