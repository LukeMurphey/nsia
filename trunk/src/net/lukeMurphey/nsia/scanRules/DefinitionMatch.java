package net.lukeMurphey.nsia.scanRules;

import net.lukeMurphey.nsia.scanRules.Definition.Severity;

/**
 * The DefinitionMatch stores the result of a definition match, including the name of the rule, the message and location of that the definition matched the content.
 * This class stores all of the relevant data from the definition; thus, the definition match will remain the same even if the definition itself changed. Therefore,
 * this class will retain the message from the definition at the time of the match.
 * @author luke
 *
 */
public class DefinitionMatch{
	private String definitionName;
	private String message;
	private int definitionID;
	private int detectStart;
	private int detectLength;
	private Severity severity;
	
	public DefinitionMatch( String definitionName, String message, Severity severity, int definitionID, int detectStart, int detectLength){
		// 0 -- Precondition check
		
		//	 0.1 -- Definition name cannot be null
		if( definitionName == null ){
			throw new IllegalArgumentException("The definition name cannot be null");
		}
		
		//	 0.2 -- Definition message cannot be null
		if( message == null ){
			throw new IllegalArgumentException("The definition message cannot be null");
		}
		
		
		// 1 -- Set the parameters
		this.definitionName = definitionName;
		this.message = message;
		this.definitionID = definitionID;
		this.detectLength = detectLength;
		this.detectStart = detectStart;
		this.severity = severity;
	}
	
	public DefinitionMatch( String definitionName, String message, Severity severity, int definitionID ){
		this(definitionName, message, severity, definitionID, -1, -1);
	}
	
	public DefinitionMatch( MetaDefinition definition ){
		this(definition.getFullName(), definition.getMessage(), definition.getSeverity(), definition.getID(), -1, -1);
	}
	
	public DefinitionMatch( MetaDefinition definition, String message ){
		this(definition.getFullName(), message, definition.getSeverity(), definition.getID(), -1, -1);
	}
	
	protected DefinitionMatch( String definitionName, Severity severity, int definitionID ){
		this.definitionName = definitionName;
		this.definitionID = definitionID;
		this.severity = severity;
	}
	
	public String getDefinitionName(){
		return definitionName;
	}
	
	public String getMessage(){
		return message;
	}
	
	public Severity getSeverity(){
		return severity;
	}
	
	public int getDefinitionID(){
		return definitionID;
	}
	
	public int getDetectStart(){
		return detectStart;
	}
	
	public int getDetectLength(){
		return detectLength;
	}
}
