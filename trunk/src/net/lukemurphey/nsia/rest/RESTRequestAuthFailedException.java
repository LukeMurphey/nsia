package net.lukemurphey.nsia.rest;

public class RESTRequestAuthFailedException extends RESTRequestFailedException {

	private static final long serialVersionUID = -4259954098824487621L;

	public RESTRequestAuthFailedException( String message ){
		super( message );
	}
	
	public RESTRequestAuthFailedException( ){
		super( "Authentication failed" );
	}
	
	
}
