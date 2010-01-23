package net.lukemurphey.nsia;

import java.util.Hashtable;

public class SessionStatus{

	// The following constants define the various states that a session can be in.
	public final static SessionStatus SESSION_NULL = new SessionStatus(0, "No session exists");
	public final static SessionStatus SESSION_ACTIVE = new SessionStatus(1, "The session is active");
	public final static SessionStatus SESSION_EXPIRED = new SessionStatus(2, "The session has expired");
	public final static SessionStatus SESSION_IDENTIFIER_EXPIRED = new SessionStatus(3, "The session identifier has expired");
	public final static SessionStatus SESSION_HIJACKED = new SessionStatus(4, "A session hijack attempt was detected for this session");
	public final static SessionStatus SESSION_ADMIN_TERMINATED = new SessionStatus(5, "This session has been explicitly terminated");
	public final static SessionStatus SESSION_LIFETIME_EXCEEDED = new SessionStatus(6, "The session has exceeded its maximum lifetime");
	public final static SessionStatus SESSION_INACTIVE = new SessionStatus(7, "The session was inactive for too long");
	
	private int statusId = 0;
	private String description = null;
	
	/**
	 * This is intended to be an enumerated type so the constructor is private.
	 */
	private SessionStatus(int statInt, String desc){
		statusId = statInt;
		description = desc;
	}
	
	/**
	 * Get a description of the session status.
	 * @return
	 */
	public String getDescription(){
		return description;
	}
	
	/**
	 * Retrieve the integer value represented by session status.
	 * @precondition None
	 * @postcondition The status ID is returned
	 * @return severity level
	 */
	public int getStatusId(){
		return statusId;
	}
	
	/**
	 * Retrieves a string description of the session status.
	 * @precondition None
	 * @postcondition A string description of the session status is returned.
	 */
	public String toString(){
		return description;
	}
	
	/**
	 * Determine if the session statuses are identical.
	 * @param sessionStatus
	 * @return
	 */
	public boolean equals(Object sessionStatus){
		
		if( sessionStatus == null ){
			return false;
		}
		
		if( sessionStatus instanceof SessionStatus){
			return ((SessionStatus)sessionStatus).getStatusId() == statusId;
		}
		else{
			return false;
		}
	}
	
	public int hashCode(){
		return statusId;
	}
	
	/**
	 * Returns a session status that corresponds to the status id given.
	 * @precondition The status ID must be valid to find a match, an invalid id will return null session status (note that a null session indicates that no session exists for the user).
	 * @postcondition A status object will be returned that corresponds to the status indicator. 
	 * @param statusId
	 * @return
	 */
	public static SessionStatus getStatusById( int statusId ){
		if( statusId == SESSION_ACTIVE.getStatusId() )
			return SESSION_ACTIVE;
		else if(statusId == SESSION_ADMIN_TERMINATED.getStatusId() )
			return SESSION_ADMIN_TERMINATED;
		else if(statusId == SESSION_EXPIRED.getStatusId() )
			return SESSION_EXPIRED;
		else if(statusId == SESSION_HIJACKED.getStatusId() )
			return SESSION_HIJACKED;
		else if(statusId == SESSION_IDENTIFIER_EXPIRED.getStatusId() )
			return SESSION_IDENTIFIER_EXPIRED;
		else if(statusId == SESSION_INACTIVE.getStatusId() )
			return SESSION_INACTIVE;
		else if(statusId == SESSION_LIFETIME_EXCEEDED.getStatusId() )
			return SESSION_LIFETIME_EXCEEDED;
		else
			return SESSION_NULL;
	}
	
	public Hashtable<String, Object> toHashtable(){
		Hashtable<String, Object> hash = new Hashtable<String, Object>();
		hash.put("StatusId", Integer.valueOf(statusId) );
		hash.put("Description", description);
		
		return hash;
	}
}
