package net.lukeMurphey.nsia;

public class LicenseValidationException extends Exception {

	private static final long serialVersionUID = -1088159488370356358L;
	
	public LicenseValidationException( String message ){
		super( message );
	}
	
	public LicenseValidationException( String message, Throwable t ){
		super( message, t );
	}
	
	public LicenseValidationException( Throwable t ){
		super( t );
	}

}
