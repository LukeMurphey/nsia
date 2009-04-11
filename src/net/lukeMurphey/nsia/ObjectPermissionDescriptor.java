package net.lukeMurphey.nsia;

public class ObjectPermissionDescriptor extends AccessControlDescriptor{

	private long objectId;
	
	protected AccessControlDescriptor.Action read;
	protected AccessControlDescriptor.Action modify;
	protected AccessControlDescriptor.Action create;
	protected AccessControlDescriptor.Action delete;
	protected AccessControlDescriptor.Action execute;
	protected AccessControlDescriptor.Action control;
	
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
	public ObjectPermissionDescriptor(AccessControlDescriptor.Action read, AccessControlDescriptor.Action modify, AccessControlDescriptor.Action create, AccessControlDescriptor.Action execute, AccessControlDescriptor.Action delete, AccessControlDescriptor.Action control, AccessControlDescriptor.Subject subjectType, int subjectId, long objectId  ) {
		// 0 -- Precondition check
		
		//	 0.1 -- Ensure that the subject type is valid
		// Will be checked in the super class initialization function
		
		//	 0.2 -- Ensure that the rightname is valid
		if( objectId < 0 )
			throw new IllegalArgumentException("object identifier is invalid (must not be less than zero)");
		
		// 1 -- Set the values
		this.read = read;
		this.modify = modify;
		this.create = create;
		this.delete = delete;
		this.execute = execute;
		this.control = control;
		super.init( subjectType, subjectId );
		
		this.objectId = objectId;
	}
	
	/**
	 * Retrieve the user ID associated with the rule, ir return -1 if the rule has not been assigned an ID. Note that 
	 * the rule is not assigned a rule ID until it is sent to persistant storage (such as the database).
	 *
	 */
	public long getObjectId(){
		return objectId;
	}
	
	
	/**
	 * Retrieve the read permission setting.
	 * @precondition None
	 * @postcondition The permission setting will be returned
	 * @return
	 */
	public AccessControlDescriptor.Action getReadPermission() {
		return read;
	}
	
	/**
	 * Retrieve the modify permission setting.
	 * @precondition None
	 * @postcondition The permission setting will be returned
	 * @return
	 */
	public AccessControlDescriptor.Action getModifyPermission() {
		return modify;
	}
	
	/**
	 * Retrieve the create permission setting.
	 * @precondition None
	 * @postcondition The permission setting will be returned
	 * @return
	 */
	public AccessControlDescriptor.Action getCreatePermission() {
		return create;
	}
	
	/**
	 * Retrieve the execute permission setting.
	 * @precondition None
	 * @postcondition The permission setting will be returned
	 * @return
	 */
	public AccessControlDescriptor.Action getExecutePermission() {
		return execute;
	}
	
	/**
	 * Retrieve the delete permission setting.
	 * @precondition None
	 * @postcondition The permission setting will be returned
	 * @return
	 */
	public AccessControlDescriptor.Action getDeletePermission() {
		return delete;
	}
	
	/**
	 * Retrieve the control permission setting.
	 * @precondition None
	 * @postcondition The permission setting will be returned
	 * @return
	 */
	public AccessControlDescriptor.Action getControlPermission() {
		return control;
	}
	
	/**
	 * Determine if the object ID was specified.
	 
	public boolean isObjectIdSet(){
		return objectIdSet;
	}*/
	
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
	public ObjectPermissionDescriptor resolvePermissions(ObjectPermissionDescriptor objectPermissionDescriptor ){
				
		AccessControlDescriptor.Action read2 = objectPermissionDescriptor.getReadPermission();
		AccessControlDescriptor.Action modify2 = objectPermissionDescriptor.getModifyPermission();
		AccessControlDescriptor.Action create2 = objectPermissionDescriptor.getCreatePermission();
		AccessControlDescriptor.Action delete2 = objectPermissionDescriptor.getDeletePermission();
		AccessControlDescriptor.Action execute2 = objectPermissionDescriptor.getExecutePermission();
		AccessControlDescriptor.Action control2 = objectPermissionDescriptor.getControlPermission();
		
		read = resolvePermission( read, read2);
		modify = resolvePermission( modify, modify2);
		create = resolvePermission( create, create2);
		delete = resolvePermission( delete, delete2);
		execute = resolvePermission( execute, execute2);
		control = resolvePermission( control, control2);
		
		return new ObjectPermissionDescriptor(read, modify, create, execute, delete, control, objectPermissionDescriptor.getSubjectType(), objectPermissionDescriptor.getSubjectId(), objectPermissionDescriptor.getObjectId() );
	}
}
