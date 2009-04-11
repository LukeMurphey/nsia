package net.lukeMurphey.nsia.responseModule;

import java.util.Hashtable;

import net.lukeMurphey.nsia.extension.ArgumentFieldsInvalidException;
import net.lukeMurphey.nsia.extension.Extension;
import net.lukeMurphey.nsia.extension.ExtensionInstallationException;
import net.lukeMurphey.nsia.extension.ExtensionRemovalException;
import net.lukeMurphey.nsia.extension.ExtensionType;
import net.lukeMurphey.nsia.extension.FieldLayout;
import net.lukeMurphey.nsia.extension.PrototypeField;

public class SSHCommandActionExtension extends Extension {

	public SSHCommandActionExtension() {
		super("SSHCommandAction", "Run an command on a remote host using SSH", ExtensionType.INCIDENT_RESPONSE_MODULE);
	}

	@Override
	public Object createInstance(Hashtable<String, String> arguments) throws ArgumentFieldsInvalidException {
		return new SSHCommandAction(arguments);
	}

	@Override
	public FieldLayout getFieldLayout() {
		return SSHCommandAction.getLayout();
	}

	@Override
	public PrototypeField[] getFields() {
		return SSHCommandAction.getLayout().getFields();
	}

	@Override
	public void install() throws ExtensionInstallationException {
		
	}

	@Override
	public void uninstall() throws ExtensionRemovalException {
		
	}

}
