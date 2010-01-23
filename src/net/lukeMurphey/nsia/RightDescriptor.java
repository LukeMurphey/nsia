package net.lukemurphey.nsia;

/**
 * The right descriptor describes the permissions a user or a group has to a right. A right is global operation, such as 
 * shutting down a service or configuring a sub-system. This is different than object-level permissions that apply
 * to objects. Only one right exists, whereas multiple objects may exist.
 * @author luke
 *
 */
public class RightDescriptor extends AccessControlDescriptor{

	protected AccessControlDescriptor.Action right;
	protected String rightName;
	
	/**
	 * Create a right descriptor.
	 * @param right
	 * @param userDescriptor
	 * @param rightName
	 */
	public RightDescriptor(Action right, UserManagement.UserDescriptor userDescriptor, String rightName ) {
		
		// 0 -- Precondition check
		
		//	 0.1 -- Ensure that the subject type is valid
		// Will be checked in the super class initialization function
		
		//	 0.2 -- Ensure that the rightname is valid
		if( rightName == null  )
			throw new IllegalArgumentException("right name is invalid (null)");
		
		//	 0.3 -- Ensure that the user descriptor is not null
		if( userDescriptor == null )
			throw new IllegalArgumentException("User descriptor is invalid (null)");
		
		// 1 -- Set the values
		
		super.init( AccessControlDescriptor.Subject.USER, userDescriptor.getUserID() );
		this.right = right;
		this.rightName = rightName;
	}
	
	/**
	 * Create a right descriptor.
	 * @param right
	 * @param groupDescriptor
	 * @param rightName
	 */
	public RightDescriptor(Action right, GroupManagement.GroupDescriptor groupDescriptor, String rightName ) {
		
		// 0 -- Precondition check
		
		//	 0.1 -- Ensure that the subject type is valid
		// Will be checked in the super class initialization function
		
		//	 0.2 -- Ensure that the rightname is valid
		if( rightName == null  )
			throw new IllegalArgumentException("right name is invalid (null)");
		
		//	 0.3 -- Ensure that the user descriptor is not null
		if( groupDescriptor == null )
			throw new IllegalArgumentException("Group descriptor is invalid (null)");
		
		// 1 -- Set the values
		
		super.init( AccessControlDescriptor.Subject.GROUP, groupDescriptor.getGroupId() );
		this.right = right;
		this.rightName = rightName;
	}
	
	/**
	 * Create a right descriptor.
	 * @param right
	 * @param subjectType
	 * @param subjectId
	 * @param rightName
	 */
	public RightDescriptor(Action right, AccessControlDescriptor.Subject subjectType, int subjectId, String rightName ) {
		// 0 -- Precondition check
		
		//	 0.1 -- Ensure that the subject type is valid
		// Will be checked in the super class initialization function
		
		//	 0.2 -- Ensure that the rightname is valid
		if( rightName == null  )
			throw new IllegalArgumentException("right name is invalid (null)");
		
		// 1 -- Set the values
		
		super.init( subjectType, subjectId );
		this.right = right;
		this.rightName = rightName;
	}
	
	/**
	 * Get the name of the right that this decriptor represents.
	 * @return
	 */
	public String getRightName(){
		return rightName;
	}
	
	/**
	 * Get the value of the right that this decriptor represents.
	 * @return
	 */
	public AccessControlDescriptor.Action getRight(){
		return right;
	}
	
	/**
	 * Resolve the strictest permissions between the two access control descriptors. This means that the following rules will apply to
	 * determine the appropriate permissions:
	 * <ol>
	 * 	<li>deny overwrites all permit and not specified (i.e. the result will be deny if either of the entries are deny)</li>
	 * 	<li>accept overwrites unspecified (i.e. the result will be accept if the entries include at least one accept)</li>
	 * 	<li>unspecified change nothing, but can be overwritten</li>
	 * </ol>
	 * @param accessControlDescriptor
	 * @return
	 */
	protected RightDescriptor resolvePermissions( RightDescriptor rightDescriptor ){
				
		Action right2 = rightDescriptor.getRight();
		
		right = resolvePermission( right, right2);
		
		return new RightDescriptor(right, rightDescriptor.getSubjectType(), rightDescriptor.getSubjectId(), rightDescriptor.getRightName() );
	}

}
