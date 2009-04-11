package net.lukeMurphey.nsia.testCases;

import net.lukeMurphey.nsia.AccessControlDescriptor;
import net.lukeMurphey.nsia.ObjectPermissionDescriptor;
import junit.framework.TestCase;

public class AccessControlDescriptorTest extends TestCase {

	ObjectPermissionDescriptor noExecute = new ObjectPermissionDescriptor(AccessControlDescriptor.Action.UNSPECIFIED, AccessControlDescriptor.Action.UNSPECIFIED, AccessControlDescriptor.Action.UNSPECIFIED, AccessControlDescriptor.Action.DENY, AccessControlDescriptor.Action.UNSPECIFIED, AccessControlDescriptor.Action.UNSPECIFIED, AccessControlDescriptor.Subject.USER, 1, 1);
	ObjectPermissionDescriptor allowExcute = new ObjectPermissionDescriptor(AccessControlDescriptor.Action.UNSPECIFIED, AccessControlDescriptor.Action.UNSPECIFIED, AccessControlDescriptor.Action.UNSPECIFIED, AccessControlDescriptor.Action.PERMIT, AccessControlDescriptor.Action.UNSPECIFIED, AccessControlDescriptor.Action.UNSPECIFIED, AccessControlDescriptor.Subject.USER, 1, 1);
	ObjectPermissionDescriptor unspecified = new ObjectPermissionDescriptor(AccessControlDescriptor.Action.UNSPECIFIED, AccessControlDescriptor.Action.UNSPECIFIED, AccessControlDescriptor.Action.UNSPECIFIED, AccessControlDescriptor.Action.UNSPECIFIED, AccessControlDescriptor.Action.UNSPECIFIED, AccessControlDescriptor.Action.UNSPECIFIED, AccessControlDescriptor.Subject.USER, 1, 1);
	ObjectPermissionDescriptor noRead = new ObjectPermissionDescriptor(AccessControlDescriptor.Action.DENY, AccessControlDescriptor.Action.UNSPECIFIED, AccessControlDescriptor.Action.UNSPECIFIED, AccessControlDescriptor.Action.UNSPECIFIED, AccessControlDescriptor.Action.UNSPECIFIED, AccessControlDescriptor.Action.UNSPECIFIED, AccessControlDescriptor.Subject.USER, 1, 1);
	
	public static void main(String[] args) {
	}

	public AccessControlDescriptorTest(String name) {
		super(name);
	}

	/*
	 * Test method for 'net.lukeMurphey.siteSentry.AccessControlDescriptor.getReadPermission()'
	 */
	public void testGetReadPermission() {
		if( noRead.getReadPermission() != AccessControlDescriptor.Action.DENY )
			fail("Read permission should have been denied");
		
		if( noExecute.getReadPermission() != AccessControlDescriptor.Action.UNSPECIFIED )
			fail("Read permission should have been unspecified");
	}

	/*
	 * Test method for 'net.lukeMurphey.siteSentry.AccessControlDescriptor.getModifyPermission()'
	 */
	public void testGetModifyPermission() {
		if( noRead.getReadPermission() != AccessControlDescriptor.Action.DENY )
			fail("Read permission should have been denied");
		
		if( noExecute.getReadPermission() != AccessControlDescriptor.Action.UNSPECIFIED )
			fail("Read permission should have been unspecified");
	}

	/*
	 * Test method for 'net.lukeMurphey.siteSentry.AccessControlDescriptor.getCreatePermission()'
	 */
	public void testGetCreatePermission() {
		if( noRead.getCreatePermission() != AccessControlDescriptor.Action.UNSPECIFIED )
			fail("Create permission should have been unspecified");
	}

	/*
	 * Test method for 'net.lukeMurphey.siteSentry.AccessControlDescriptor.getExecutePermission()'
	 */
	public void testGetExecutePermission() {
		if( noExecute.getExecutePermission() != AccessControlDescriptor.Action.DENY )
			fail("Execute permission should have been denied");
		
		if( unspecified.getExecutePermission() != AccessControlDescriptor.Action.UNSPECIFIED )
			fail("Execute permission should have been unspecified");
	}

	/*
	 * Test method for 'net.lukeMurphey.siteSentry.AccessControlDescriptor.getDeletePermission()'
	 */
	public void testGetDeletePermission() {
		if( noRead.getDeletePermission() != AccessControlDescriptor.Action.UNSPECIFIED )
			fail("Delete permission should have been unspecified");
	}

	/*
	 * Test method for 'net.lukeMurphey.siteSentry.AccessControlDescriptor.resolvePermissions(AccessControlDescriptor)'
	 */
	public void testResolvePermissions() {
		// Deny should override
		ObjectPermissionDescriptor result = noExecute.resolvePermissions(allowExcute);
		
		if( result.getExecutePermission() != AccessControlDescriptor.Action.DENY )
			fail("Execute permission should have been denied");
		
		// Permit should override unspecified
		//result = allowExcute.resolvePermissions(unspecified);
		
		//if( result.getExecutePermission() != AccessControlDescriptor.Action.PERMIT )
			//fail("Execute permission should have been allowed");
		
		//Unspecified should do nothing
		//result = noExecute.resolvePermissions(allowExcute);
		
		//if( result.getReadPermission() != AccessControlDescriptor.Action.DENY )
			//fail("Read permission should have been unspecified");

	}

}
