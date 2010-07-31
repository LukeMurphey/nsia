package net.lukemurphey.nsia.tests;

public class TestApplicationException extends Exception {

	private static final long serialVersionUID = 4230727685347340820L;

	public TestApplicationException(){
		super();
	}
	
	public TestApplicationException( Throwable t ){
		super(t);
	}
	
}
