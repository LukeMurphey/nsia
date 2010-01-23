package net.lukemurphey.nsia.extension;

public class ExtensionRemovalException extends Exception {
	
	private static final long serialVersionUID = 5839230293847237L;

	public ExtensionRemovalException(String message, Throwable t){
		super( message, t );
	}
	
	public ExtensionRemovalException(String message){
		super( message );
	}

}
