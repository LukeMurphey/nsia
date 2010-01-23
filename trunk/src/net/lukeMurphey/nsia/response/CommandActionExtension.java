package net.lukemurphey.nsia.response;

import java.util.Calendar;
import java.util.Hashtable;
import java.util.TimeZone;

import net.lukemurphey.nsia.extension.ArgumentFieldsInvalidException;
import net.lukemurphey.nsia.extension.Extension;
import net.lukemurphey.nsia.extension.ExtensionInstallationException;
import net.lukemurphey.nsia.extension.ExtensionRemovalException;
import net.lukemurphey.nsia.extension.ExtensionType;
import net.lukemurphey.nsia.extension.FieldLayout;
import net.lukemurphey.nsia.extension.PrototypeField;

public class CommandActionExtension extends Extension{

	public CommandActionExtension(){
		super("CommandAction", "Run a Program", ExtensionType.INCIDENT_RESPONSE_MODULE);
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.set(2008, 4, 19, 8 , 39, 2);
		
		this.lastUpdated = cal.getTime();
		
		this.versionMajor = 1;
		this.versionMinor = 0;
		this.revision = 0;
	}
	
	@Override
	public Object createInstance(Hashtable<String, String> arguments) throws ArgumentFieldsInvalidException {
		return new CommandAction(arguments);
	}

	@Override
	public FieldLayout getFieldLayout() {
		return CommandAction.getLayout();
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
