package net.lukeMurphey.nsia;

public class NoSessionException extends Exception {

	static final long serialVersionUID = 1142797705L;
	
	private SessionStatus sessionStatus;
	/**
	 * Creates an exception that indicates that the user attempted an XML-RPC call without a valid session.
	 * @precondition The session status must not be null and must not indicate a valid session
	 * @postcondition The exception will be configured to the given session status indicator
	 * @param sessionStatus
	 */
	public NoSessionException ( SessionStatus sessionStatus ){
		
		// 0 -- Precondition check
		
		//	0.1 -- Cannot be null
		if( sessionStatus == null )
			throw new IllegalArgumentException("The session status cannot be null");
		
		//	 0.2 -- Cannot be a valid session
		if( sessionStatus == SessionStatus.SESSION_ACTIVE )
			throw new IllegalArgumentException("The session status cannot be a valid session");
		
		// 1 -- Configure the class
		this.sessionStatus = sessionStatus;
	}
	
	public String getMessage(){
		return sessionStatus.toString();
	}
}
