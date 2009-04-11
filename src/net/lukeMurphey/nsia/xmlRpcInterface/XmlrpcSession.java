package net.lukeMurphey.nsia.xmlRpcInterface;

import java.util.*;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.InsufficientPermissionException;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.SessionManagement;
import net.lukeMurphey.nsia.trustBoundary.ApiSessionManagement;

/**
 * This class acts as wrapper around the classes that perform authentication and session management. Class methods
 * perform functions such as session contruction, deconstruction, session status, etc. 
 * @author luke
 *
 */
public class XmlrpcSession extends XmlrpcHandler{

	protected ApiSessionManagement sessionManager;
	
	public XmlrpcSession(Application appRes){
		super(appRes);
		
		sessionManager = new ApiSessionManagement( appRes );
	}
	
	/**
	 * Method authenticates the user based on the password and constructs a session based if the authentication is 
	 * successful. Returns a session identifier as a string if successful, or null if unsuccessful. 
	 * @param userName
	 * @param password
	 * @return
	 * @throws GeneralizedException
	 */
	public String authenticate( String userName, String password ) throws GeneralizedException{		
		//TODO Log IP Address with login event
		String sessionIdentifier = sessionManager.authenticate( userName, password);
		
		if( sessionIdentifier == null )
			return EMPTY_STRING;
		else
			return sessionIdentifier;
	}
	
	/**
	 * Terminate the session associated with the session ID.
	 * @param sessionIdentifier
	 * @throws GeneralizedException
	 */
	public boolean terminateSession( String sessionIdentifier ) throws GeneralizedException{
		
		return sessionManager.terminateSession( sessionIdentifier );
	}
	
	/**
	 * Terminate the session associated with the tracking number.
	 * @param trackingNumber
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException 
	 * @throws NoSessionException 
	 */
	public boolean terminateSession( String sessionIdentifier, int trackingNumber ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		
		return sessionManager.terminateSession( sessionIdentifier, trackingNumber );
	}
	
	/**
	 * Get the status indicator associated with the given session.
	 * @param sessionIdentifier
	 * @return
	 * @throws GeneralizedException
	 */
	public int getSessionStatus( String sessionIdentifier ) throws GeneralizedException{
		return sessionManager.getSessionStatus( sessionIdentifier );
	}
	
	/**
	 * Retrieves an updated session identifier or returns null if the session identfier could not
	 * be refreshed (such as if the old session identifier is invalid or illegal).
	 * @param sessionIdentifier
	 * @return
	 * @throws GeneralizedException
	 */
	public String refreshSessionIdentifier( String sessionIdentifier ) throws GeneralizedException{
		return sessionManager.refreshSessionIdentifier( sessionIdentifier );
	}
	
	/**
	 * Disables the user's sessions for the given user ID. The number of sessions destructed will be returned, or -1 if the operation
	 * failed.
	 * @param sessionIdentifier
	 * @param userId
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public int disableUserSessions( String sessionIdentifier, int userId ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		
		return sessionManager.disableUserSessions( sessionIdentifier, userId);
	}
	
	/**
	 * Get a list of the currently active user sessions.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public Vector<Hashtable<String, Object>> getUserSessions( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		
		sessionManager.getUserSessions( sessionIdentifier );
		
		// 3 -- Get the session info
		Vector<Hashtable<String, Object>> userSessions = new Vector<Hashtable<String, Object>>();
		SessionManagement.SessionInfo[] currentSessions = sessionManager.getUserSessions( sessionIdentifier );
		
		for( int c = 0; c < currentSessions.length; c++){
			Hashtable<String, Object> sessInfo = new Hashtable<String, Object>();
			
			sessInfo.put("LastActivity", Double.valueOf(currentSessions[c].getLastActivity().getTime()));
			sessInfo.put("SessionCreated", Double.valueOf(currentSessions[c].getSessionCreated().getTime()));
						
			sessInfo.put("SessionStatus", Integer.valueOf(currentSessions[c].getSessionStatus().getStatusId()));
			
			sessInfo.put("TrackingNumber", Double.valueOf( currentSessions[c].getTrackingNumber()));
			sessInfo.put("UserId", Integer.valueOf(String.valueOf( currentSessions[c].getUserId())) );
			sessInfo.put("UserName", String.valueOf( currentSessions[c].getUserName() ) );
			userSessions.add(sessInfo);
		}
		
		return userSessions;
	}
	
}
