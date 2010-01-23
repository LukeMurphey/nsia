package net.lukemurphey.nsia;

/**
 * This exception is intended to indicate that an object could not be found (such as user could not be found with the user identifier).
 * @author Luke Murphey
 *
 */
public class NotFoundException extends Exception {
	
	private static final long serialVersionUID = 5308110965817608073L;
	
	public NotFoundException( String message ){
		super(message);
	}

}
