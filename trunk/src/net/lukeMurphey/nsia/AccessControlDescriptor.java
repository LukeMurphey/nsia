package net.lukeMurphey.nsia;


/**
 * This class describes an access control descriptor. The access control descriptor indicates what permissions are available relative
 * to an ACL protected object.
 * @author luke
 *
 */
public abstract class AccessControlDescriptor {
	
	protected Subject subjectType;
	protected int subjectId;
	
	public enum Subject{
		USER,
		GROUP
	}
	
	public enum Action{
		UNSPECIFIED,
		DENY,
		PERMIT
	}
	
	/**
	 * Create an access control descriptor object.
	 * @param read
	 * @param modify
	 * @param create
	 * @param execute
	 * @param delete
	 * @param control
	 * @param subjectType
	 * @param subjectId
	 * @param objectId
	 */
	public void init( Subject subjectType, int subjectId ) {
		
		 // 0 -- Precondition check

		//	 0.1 -- Ensure that the subject type is valid
		if( subjectType == null  )
			throw new IllegalArgumentException("The access control descriptor given is not valid (null)");
		
		// 1 -- Set the values
		this.subjectType = subjectType;
		this.subjectId = subjectId;
	}
	
	/**
	 * Return the type of the entry.
	 * @return
	 */
	public Subject getSubjectType(){
		return subjectType;
	}
	
	/**
	 * Retrieve the identifier that identifies the subject (group or user) that this ACL applies to.
	 * @return
	 */
	public int getSubjectId(){
		return subjectId;
	}
	
	/**
	 * Determines if the subject of the ACL is a group.
	 * @return
	 */
	public boolean isGroup(){
		return subjectType == Subject.GROUP;
	}
	
	/**
	 * Determines if the subject of the ACL is a user.
	 * @return
	 */
	public boolean isUser(){
		return subjectType == Subject.USER;
	}
	
	/**
	 * Method resolves the permissions using the ACL policy. This method is intended to be used in deriving the users' permissions in
	 * a situation where multiple permission sets may cascade. For example, a user may have a deny ACL in one group that overrides an
	 * unspecified setting in another group.
	 * @param existingPermission Represents the currently resolved permission setting; this permission can be reduced if the setterPermission is more restrictive or upgraded from unspecified to permitted
	 * @param setterPermission The users permission to the object 
	 * @return
	 */
	protected Action resolvePermission(Action existingPermission, Action setterPermission ){
		if( existingPermission == Action.DENY || setterPermission == Action.DENY )
			return Action.DENY;
		else if( setterPermission == Action.UNSPECIFIED ) // This line should only be reachable if both permission sets are not deny
			return existingPermission;
		else if( setterPermission == Action.PERMIT ) // This line should only be reachable if both permission sets are not deny and the spectific permissions
			return Action.PERMIT;
		
		//This should never be executed (assert)
		return Action.UNSPECIFIED;
	}
	
}
