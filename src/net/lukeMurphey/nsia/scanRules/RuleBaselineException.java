package net.lukeMurphey.nsia.scanRules;

public class RuleBaselineException extends Exception {

	private static final long serialVersionUID = 3457478312143812584L;

	public RuleBaselineException(String message, Throwable t){
		super(message, t);
	}
	
	public RuleBaselineException(String message){
		super(message);
	}
}
