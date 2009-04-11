package net.lukeMurphey.nsia;

/**
 * This exception is returned by the XML-RPC functions when an internal error occurs. This exception
 * does not contain sensitive data but rather a blanket message that indicates that the operation
 * could not be completed.
 * @author luke
 *
 */
public class GeneralizedException extends Exception{

	private static final long serialVersionUID = 1143346757L;
	
	public GeneralizedException( Throwable throwable){
		super(throwable);
	}
	
	public GeneralizedException(){
		super();
	}
	
	public String getMessage(){
		return "An internal exception occurred, the operation could not be completed"; 
	}
}
