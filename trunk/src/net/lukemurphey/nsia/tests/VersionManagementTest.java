package net.lukemurphey.nsia.tests;

import junit.framework.TestCase;
import net.lukemurphey.nsia.ApplicationVersionDescriptor;

public class VersionManagementTest extends TestCase {

	public void testVersionCheckLater(){
		ApplicationVersionDescriptor vd1 = new ApplicationVersionDescriptor("0.9.5");
		ApplicationVersionDescriptor vd2 = new ApplicationVersionDescriptor("1.0.0");
		
		if( ApplicationVersionDescriptor.isLaterVersion(vd1, vd2) == false ){
			fail("Incorrectly indicated that version 1.0.0 is later than 0.9.5");
		}
	}
	
	public void testVersionCheckPrior(){
		ApplicationVersionDescriptor vd1 = new ApplicationVersionDescriptor("0.9.5");
		ApplicationVersionDescriptor vd2 = new ApplicationVersionDescriptor("1.0.0");
		
		if( ApplicationVersionDescriptor.isLaterVersion(vd2, vd1)){
			fail("Incorrectly indicated that version 1.0.0 is later than 0.9.5");
		}
	}
	
	public void testVersionCheckSame(){
		ApplicationVersionDescriptor vd1 = new ApplicationVersionDescriptor("1.0.0");
		ApplicationVersionDescriptor vd2 = new ApplicationVersionDescriptor("1.0.0");
		
		if( ApplicationVersionDescriptor.isLaterVersion(vd1, vd2)){
			fail("Incorrectly indicated that version 1.0.0 is later than 1.0.0");
		}
	}
	
}
