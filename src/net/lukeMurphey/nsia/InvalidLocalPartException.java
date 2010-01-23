package net.lukemurphey.nsia;

//http://www.ietf.org/rfc/rfc2822.txt
public class InvalidLocalPartException extends Exception {
	private static final long serialVersionUID = 1139883213L;
	
	public InvalidLocalPartException(String message){
		super(message);
	}
}
