package net.lukemurphey.nsia.eventlog;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.eventlog.CommonEventExpressionMessage.CommonEventExpressionField;
import net.lukemurphey.nsia.eventlog.CommonEventExpressionMessage.ExtensionField;

public class CommonEventExpressionMessageFormatter extends MessageFormatter {

	@Override
	public String formatMessage(EventLogMessage message) {
		CommonEventExpressionMessage cee = new CommonEventExpressionMessage();
		
		EventLogField[] fields = message.getFields();
		
		// 1 -- Add the required, default fields
		cee.addExtensionField( new ExtensionField( CommonEventExpressionField.VENDOR, Application.APPLICATION_VENDOR ) );
		cee.addExtensionField( new ExtensionField( CommonEventExpressionField.PRODUCT, Application.APPLICATION_NAME ) );
		cee.addExtensionField( new ExtensionField( CommonEventExpressionField.PRODUCT_VERSION, Application.getVersion() ) );
		cee.addExtensionField( new ExtensionField( CommonEventExpressionField.EVENT_ID, Integer.toString(message.getEventType().ordinal()) ) );
		cee.addExtensionField( new ExtensionField( "event", message.getEventType().getName() ) );
		cee.addExtensionField( new ExtensionField( CommonEventExpressionField.SYSLOG_PRIORITY, String.valueOf( message.getEventType().getSeverity().getSyslogEquivalent() ) ) );
		cee.addExtensionField( new ExtensionField( EventLogField.FieldName.SEVERITY.getSimpleNameFormat(), String.valueOf( message.getEventType().getSeverity().toString() ) ) );
		
		// 2 -- Add each additional field, try to match up the field name with one of the default fields if possible
		for(int c = 0; c < fields.length; c++){
			if( fields[c].getName() == EventLogField.FieldName.SOURCE_USER_ID ){
				cee.addExtensionField( new ExtensionField( CommonEventExpressionField.SRC_USER_ID, fields[c].getValue() ) );
			}
			else if( fields[c].getName() == EventLogField.FieldName.SOURCE_USER_NAME ){
				cee.addExtensionField( new ExtensionField( CommonEventExpressionField.SRC_USER, fields[c].getValue() ) );
			}
			else if( fields[c].getName() == EventLogField.FieldName.TARGET_USER_NAME ){
				cee.addExtensionField( new ExtensionField( CommonEventExpressionField.USER, fields[c].getValue() ) );
			}
			else if( fields[c].getName() == EventLogField.FieldName.TARGET_USER_ID ){
				cee.addExtensionField( new ExtensionField( CommonEventExpressionField.USER_ID, fields[c].getValue() ) );
			}
			else if( fields[c].getName() == EventLogField.FieldName.GROUP_ID ){
				cee.addExtensionField( new ExtensionField( CommonEventExpressionField.USER_GROUP_ID, fields[c].getValue() ) );
			}
			else if( fields[c].getName() == EventLogField.FieldName.GROUP_NAME ){
				cee.addExtensionField( new ExtensionField( CommonEventExpressionField.USER_GROUP, fields[c].getValue() ) );
			}
			else if( fields[c].getName() == EventLogField.FieldName.SOURCE_ADDRESS ){
				cee.addExtensionField( new ExtensionField( CommonEventExpressionField.SRC_HOST, fields[c].getValue() ) );
			}
			else if( fields[c].getName() == EventLogField.FieldName.URL ){
				cee.addExtensionField( new ExtensionField( CommonEventExpressionField.URL, fields[c].getValue() ) );
			}
			else if( fields[c].getName() == EventLogField.FieldName.FILE ){
				cee.addExtensionField( new ExtensionField( CommonEventExpressionField.FILE_NAME, fields[c].getValue() ) );
			}
			else{
				cee.addExtensionField( new ExtensionField( fields[c].getName().getSimpleNameFormat(), fields[c].getValue() ) );
			}
		}
		
		return cee.getCEEMessage();
	}

	@Override
	public String getDescription() {
		return "Common Event Expression";
	}

}
