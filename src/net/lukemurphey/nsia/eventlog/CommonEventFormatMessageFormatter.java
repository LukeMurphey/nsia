package net.lukemurphey.nsia.eventlog;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.eventlog.CommonEventFormatMessage.CommonExtensionDictionaryField;
import net.lukemurphey.nsia.eventlog.CommonEventFormatMessage.ExtensionField;

public class CommonEventFormatMessageFormatter extends MessageFormatter {

	@Override
	public String formatMessage(EventLogMessage message) {
		CommonEventFormatMessage cef = new CommonEventFormatMessage(  Application.APPLICATION_VENDOR, Application.APPLICATION_NAME, Application.getVersion(), Integer.toString(message.getEventType().ordinal()), message.getEventType().getName(), message.getEventType().getSeverity().getSyslogEquivalent() );
		
		EventLogField[] fields = message.getFields();
		
		for(int c = 0; c < fields.length; c++){
			if( fields[c].getName() == EventLogField.FieldName.SOURCE_USER_ID ){
				cef.addExtensionField( new ExtensionField( CommonExtensionDictionaryField.SOURCE_USER_ID, fields[c].getDescription() ) );
			}
			else if( fields[c].getName() == EventLogField.FieldName.SOURCE_USER_NAME ){
				cef.addExtensionField( new ExtensionField( CommonExtensionDictionaryField.SOURCE_USER, fields[c].getDescription() ) );
			}
			else if( fields[c].getName() == EventLogField.FieldName.TARGET_USER_NAME ){
				cef.addExtensionField( new ExtensionField( CommonExtensionDictionaryField.DESTINATION_USER_NAME, fields[c].getDescription() ) );
			}
			else if( fields[c].getName() == EventLogField.FieldName.TARGET_USER_ID ){
				cef.addExtensionField( new ExtensionField( CommonExtensionDictionaryField.DESTINATION_USER_ID, fields[c].getDescription() ) );
			}
			else if( fields[c].getName() == EventLogField.FieldName.FILE ){
				cef.addExtensionField( new ExtensionField( CommonExtensionDictionaryField.FILE_NAME, fields[c].getDescription() ) );
			}
			else{
				cef.addExtensionField( new ExtensionField( fields[c].getName().getSimpleNameFormat(), fields[c].getDescription() ) );
			}
		}
		
		return cef.getCEFMessage();
	}

	@Override
	public String getDescription() {
		return "Common Event Format";
	}

}
