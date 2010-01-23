package net.lukemurphey.nsia;

public class NoDatabaseConnectionException extends Exception {
	
	public static final long serialVersionUID = 1139617875L;
	
	public NoDatabaseConnectionException(){
		super("Database connection unavailable");
	}
	
	public NoDatabaseConnectionException(Throwable throwable){
		super("Database connection unavailable", throwable);
	}

}
