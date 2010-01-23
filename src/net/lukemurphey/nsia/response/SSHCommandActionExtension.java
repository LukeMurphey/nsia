package net.lukemurphey.nsia.response;

import java.util.Hashtable;

import net.lukemurphey.nsia.extension.ArgumentFieldsInvalidException;
import net.lukemurphey.nsia.extension.Extension;
import net.lukemurphey.nsia.extension.ExtensionInstallationException;
import net.lukemurphey.nsia.extension.ExtensionRemovalException;
import net.lukemurphey.nsia.extension.ExtensionType;
import net.lukemurphey.nsia.extension.FieldLayout;
import net.lukemurphey.nsia.extension.PrototypeField;

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
