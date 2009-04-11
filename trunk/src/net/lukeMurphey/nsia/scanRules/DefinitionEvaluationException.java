package net.lukeMurphey.nsia.scanRules;

public class DefinitionEvaluationException extends Exception {

	private static final long serialVersionUID = -4817917751095306929L;
	private int definitionID = -1;
	private String definitionName = null;
	
	public DefinitionEvaluationException(String message){
		super(message);
	}
	
	public DefinitionEvaluationException(String message, Throwable t){
		super(message, t);
	}
	
	public DefinitionEvaluationException(String message, Throwable t, int definitionID, String definitionName){
		super(message, t);
		this.definitionID = definitionID;
		this.definitionName = definitionName;
	}
	
	public int getDefinitionID(){
		return definitionID;
	}
	
	public String getDefinitionName(){
		return definitionName;
	}
}
