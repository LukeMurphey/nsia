package net.lukemurphey.nsia.tests;

import junit.framework.TestCase;

import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class EventLogMessageTest extends TestCase {

	public void testEventLogMessage() {
		
		Calendar calendar = Calendar.getInstance( TimeZone.getTimeZone("GMT"));
		calendar.set( 2008, 1, 2, 2, 16, 32);
		
		Date date = calendar.getTime();
		
		EventLogField[] fields = {new EventLogField(FieldName.SOURCE_USER_ID, "1"), new EventLogField(FieldName.SOURCE_USER_NAME, "Luke.Murphey.Admin"), new EventLogField(FieldName.RIGHT, "System.Shutdown")};
		EventLogMessage message = new EventLogMessage(EventLogMessage.EventType.ACCESS_CONTROL_DENY, date, fields );
		
		if( !message.toString().equals("[2008/01/02 20:16:32 CST, Warning] Access Control: Denied action { source_user_id = 1, source_user_name = Luke.Murphey.Admin, right = System.Shutdown }") ){
			fail("The message string did not match the specified format");
			//System.out.println( message.toString() );
		}
	}
	
	public void testEventLogMessageWithQuotes() {
		
		Calendar calendar = Calendar.getInstance( TimeZone.getTimeZone("GMT"));
		calendar.set( 2008, 1, 2, 2, 16, 32);
		
		Date date = calendar.getTime();
		
		EventLogField[] fields = {new EventLogField(FieldName.MESSAGE, "<QUOTE>\"</QUOTE>"), new EventLogField(FieldName.RULE_SPECIMEN, "http://1.web?trees,foliage"), new EventLogField(FieldName.SOURCE_USER_NAME, "Luke.Murphey.Admin"), new EventLogField(FieldName.RIGHT, "System.Shutdown")};
		EventLogMessage message = new EventLogMessage(EventLogMessage.EventType.ACCESS_CONTROL_DENY, date, fields );
		
		if( !message.toString().equals("[2008/01/02 20:16:32 CST, Warning] Access Control: Denied action { message = \"<QUOTE>\\\"</QUOTE>\", specimen = \"http://1.web?trees,foliage\", source_user_name = Luke.Murphey.Admin, right = System.Shutdown }") ){
			fail("The message string did not match the specified format");
		}
	}

}
