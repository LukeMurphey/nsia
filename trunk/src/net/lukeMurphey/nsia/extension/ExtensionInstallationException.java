package net.lukemurphey.nsia.extension;

public class ExtensionInstallationException extends Exception{

	private static final long serialVersionUID = 1304872903020384L;

	public ExtensionInstallationException(String message, Throwable t){
		super( message, t );
	}
	
	public ExtensionInstallationException(String message){
		super( message );
	}
}
