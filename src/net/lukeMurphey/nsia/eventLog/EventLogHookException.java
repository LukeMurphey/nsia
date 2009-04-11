package net.lukeMurphey.nsia.eventLog;

public class EventLogHookException extends Exception {

	private static final long serialVersionUID = 1209315159694492474L;

	
	public EventLogHookException( String message, Throwable t){
		super(message, t);
	}
	
	public EventLogHookException( String message){
		super(message);
	}
	
	public EventLogHookException( Throwable t){
		super(t);
	}
}
