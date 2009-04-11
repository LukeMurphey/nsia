package net.lukeMurphey.nsia.responseModule;

import java.util.Calendar;
import java.util.Hashtable;
import java.util.TimeZone;

import net.lukeMurphey.nsia.extension.ArgumentFieldsInvalidException;
import net.lukeMurphey.nsia.extension.Extension;
import net.lukeMurphey.nsia.extension.ExtensionInstallationException;
import net.lukeMurphey.nsia.extension.ExtensionRemovalException;
import net.lukeMurphey.nsia.extension.ExtensionType;
import net.lukeMurphey.nsia.extension.FieldLayout;
import net.lukeMurphey.nsia.extension.PrototypeField;


public class LogFileActionExtension extends Extension {

	public LogFileActionExtension(){
		super("LogFileAction", "Append a message to a text file", ExtensionType.INCIDENT_RESPONSE_MODULE);
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.set(2008, 4, 26, 6 , 48, 59);
		
		this.lastUpdated = cal.getTime();
		
		this.versionMajor = 1;
		this.versionMinor = 0;
		this.revision = 0;
	}
	
	@Override
	public Object createInstance(Hashtable<String, String> arguments)
			throws ArgumentFieldsInvalidException {
		return new LogFileAction(arguments);
	}
	
	@Override
	public FieldLayout getFieldLayout() {
		return LogFileAction.getLayout();
	}

	@Override
	public PrototypeField[] getFields() {
		return CommandAction.getLayout().getFields();
	}

	@Override
	public void install() throws ExtensionInstallationException {
		
	}

	@Override
	public void uninstall() throws ExtensionRemovalException {
		
	}

}
