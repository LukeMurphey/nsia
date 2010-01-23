package net.lukemurphey.nsia;

public class DuplicateEntryException extends Exception {
	private static final long serialVersionUID = 1141858556L;
	
	public DuplicateEntryException(String message){
		super(message);
	}
	
	public DuplicateEntryException(){
		super();
	}
}
