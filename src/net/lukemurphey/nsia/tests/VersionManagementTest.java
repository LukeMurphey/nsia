package net.lukemurphey.nsia.tests;

import junit.framework.TestCase;
import net.lukemurphey.nsia.VersionManagement;
import net.lukemurphey.nsia.VersionManagement.VersionDescriptor;

public class VersionManagementTest extends TestCase {

	public void testVersionCheckLater(){
		VersionManagement.VersionDescriptor vd1 = new VersionManagement.VersionDescriptor("0.9.5");
		VersionManagement.VersionDescriptor vd2 = new VersionManagement.VersionDescriptor("1.0.0");
		
		if( VersionDescriptor.isLaterVersion(vd1, vd2) == false ){
			fail("Incorrectly indicated that version 1.0.0 is later than 0.9.5");
		}
	}
	
	public void testVersionCheckPrior(){
		VersionManagement.VersionDescriptor vd1 = new VersionManagement.VersionDescriptor("0.9.5");
		VersionManagement.VersionDescriptor vd2 = new VersionManagement.VersionDescriptor("1.0.0");
		
		if( VersionDescriptor.isLaterVersion(vd2, vd1)){
			fail("Incorrectly indicated that version 1.0.0 is later than 0.9.5");
		}
	}
	
	public void testVersionCheckSame(){
		VersionManagement.VersionDescriptor vd1 = new VersionManagement.VersionDescriptor("1.0.0");
		VersionManagement.VersionDescriptor vd2 = new VersionManagement.VersionDescriptor("1.0.0");
		
		if( VersionDescriptor.isLaterVersion(vd1, vd2)){
			fail("Incorrectly indicated that version 1.0.0 is later than 1.0.0");
		}
	}
	
}
