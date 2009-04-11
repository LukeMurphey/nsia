package net.lukeMurphey.nsia.testCases;

import junit.framework.TestCase;
/*import java.io.*;
import javax.xml.parsers.ParserConfigurationException;

import net.lukeMurphey.nsia.EventLog;
import net.lukeMurphey.nsia.StringTable;
import net.lukeMurphey.nsia.StringTable.Message;

import org.xml.sax.SAXException;*/

public class EventLogTest extends TestCase {
	
	public static void main(String [] args){
		
	}
	/*public void testEventLog() throws ParserConfigurationException, SAXException, IOException{
		EventLog eventLog = null;
		try {
			eventLog = new EventLog(new File("testLog.log"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			fail("File could not be opened: " + e.getMessage());
		}
		
		//eventLog.logEvent(EventLogSeverity.DEBUG, "Test: This is a test message");
		
		StringTable stringTable = StringTable.loadStringTable("eng");
		StringTable.Message message = stringTable.getMessage(StringTable.MSGID_FIREWALL_DENY, "source address = 216.127.16.201:49152, target address = 110.23.41.123:8443");
		if( message == null )
			fail("Message is null");
		
		//System.out.println(message.getMessageText());
		eventLog.logEvent(message);		
	}*/

}
