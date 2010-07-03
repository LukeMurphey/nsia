package net.lukemurphey.nsia.upgrade;

public class UpgradeFailureException extends Exception {
	
	private static final long serialVersionUID = -4396746774986149119L;

	public UpgradeFailureException(String message){
		super(message);
	}
	
	public UpgradeFailureException(String message, Throwable t){
		super( message, t);
	}
	
	public UpgradeFailureException(Throwable t){
		super(t);
	}
	
}
