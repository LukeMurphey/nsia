package net.lukemurphey.nsia.tests;

import junit.framework.TestCase;
import net.lukemurphey.nsia.eventlog.CommonEventFormatMessage;
import net.lukemurphey.nsia.eventlog.CommonEventFormatMessage.CommonExtensionDictionaryField;
import net.lukemurphey.nsia.eventlog.CommonEventFormatMessage.ExtensionField;

public class CommonEventFormatMessageTest extends TestCase {

	public void testAddExtensionField() {
		 CommonEventFormatMessage message = new CommonEventFormatMessage("ThreatFactor", "Website Auditor", "1.0", "267", "Scan Result: Rule Failed", 10);
		 
		 message.addExtensionField(new ExtensionField(CommonExtensionDictionaryField.DESTINATION_ADDRESS, "192.168.10.8") );
		 ExtensionField[] fields = message.getExtensionFields();
		 
		 if(fields.length != 1){
			 fail("Number of extension fields was not the expected value (should have been 1 but returned " + fields.length + "");
		 }
		 else{
			 if( fields[0].getName().equals(CommonExtensionDictionaryField.DESTINATION_ADDRESS.getName()) == false ){
				 fail("The extension field added did not return the expected value (should have been " + CommonExtensionDictionaryField.DESTINATION_ADDRESS.getName() + " but was " + fields[0].getName() + ")");
			 }

			 if( fields[0].getValue().equals("192.168.10.8") == false ){
				 fail("The extension field added did not return the expected value (should have been 192.168.10.8 but was " + fields[0].getValue() + "");
			 }
		 }
		
	}

	public void testEscapeFieldPrefix() {
		CommonEventFormatMessage message = new CommonEventFormatMessage("ThreatFactor", "Website | Auditor", "1.0", "267", "Scan Result: Rule Failed", 10);
		 
		message.addExtensionField(new ExtensionField(CommonExtensionDictionaryField.DESTINATION_ADDRESS, "192.168.10.8") );
		
		String result = message.getCEFMessage();
		                   
		if( result.equals("CEF:0|ThreatFactor|Website \\| Auditor|1.0|267|Scan Result: Rule Failed|10| dst=192.168.10.8") == false){
			fail("The CEF message returned was not the expected value:" + result);
		}
	}
	
	public void testEscapeFieldEqualsInExtension() {
		CommonEventFormatMessage message = new CommonEventFormatMessage("ThreatFactor", "Website Auditor", "1.0", "267", "Scan Result: Rule Failed", 10);
		 
		message.addExtensionField(new ExtensionField(CommonExtensionDictionaryField.FILE_NAME, "1+1=2") );
		
		String result = message.getCEFMessage();
		
		if( result.equals("CEF:0|ThreatFactor|Website Auditor|1.0|267|Scan Result: Rule Failed|10| fname=1+1\\=2") == false){
			fail("The CEF message returned was not the expected value:" + result);
		}
	}

	public void testGetCEFMessage() {
		CommonEventFormatMessage message = new CommonEventFormatMessage("ThreatFactor", "Website Auditor", "1.0", "267", "Scan Result: Rule Failed", 10);
		 
		message.addExtensionField(new ExtensionField(CommonExtensionDictionaryField.DESTINATION_ADDRESS, "192.168.10.8") );
		
		String result = message.getCEFMessage();
		
		if( result.equals("CEF:0|ThreatFactor|Website Auditor|1.0|267|Scan Result: Rule Failed|10| dst=192.168.10.8") == false){
			fail("The CEF message returned was not the expected value:" + result);
		}
	}

}
