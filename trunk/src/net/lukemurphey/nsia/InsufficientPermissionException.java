package net.lukemurphey.nsia;

public class InsufficientPermissionException extends Exception {

	private static final long serialVersionUID = -4334004759941878792L;
	private String message = null;
	
	public void setMessage(String message){
		this.message = message;
	}
	
	public String getMessage(){
		return message;
	}
	
}
