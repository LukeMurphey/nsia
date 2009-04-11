package net.lukeMurphey.nsia.scanRules;

import java.util.*;

/**
 * The following class represents the results of an individual HTTP header scan result.
 * @author luke
 *
 */
public class HttpHeaderScanResult{
	//The following are the possible header scan result codes
	public static final int REJECTED_BY_DEFAULT = 0;
	public static final int ACCEPTED_BY_DEFAULT = 1;
	public static final int NOT_MATCHED = 2;
	public static final int REJECTED = 3;
	public static final int ACCEPTED = 4;
	
	//The following constants are used to indicate the type of a rule (string comparison or regular expression matching)
	public static final int RULE_TYPE_REGEX = 0;
	public static final int RULE_TYPE_STRING = 1;
	
	//Class attributes
	protected String nameRule;
	protected String valueRule;
	protected int nameRuleType;
	protected int valueRuleType;
	protected String nameActual;
	protected String valueActual;
	protected long ruleId; //This is the header rule ID
	protected int ruleResult;
	protected int ruleAction;
	
	public int getRuleState(){
		return ruleResult; 
	}
	
	public int getRuleAction(){
		return ruleAction; 
	}
	
	public long getRuleId(){
		return ruleId;
	}
	
	public boolean getIsDefaultRuleInvoked(){
		if( ruleResult == REJECTED_BY_DEFAULT ||  ruleResult == ACCEPTED_BY_DEFAULT )
			return true;
		else
			return false;
	}
	
	public String getNameRule(){
		return nameRule;
	}
	
	public String getValueRule(){
		return valueRule;
	}
	
	public int getNameRuleType(){
		return nameRuleType;
	}
	
	public int getValueRuleType(){
		return valueRuleType;
	}
	
	public String getActualName(){
		return nameActual;
	}
	
	public String getActualValue(){
		return valueActual;
	}
	
	/**
	 * Retrieve a hashtable version of the headerscan result.
	 * @return
	 */
	public Hashtable<String, Object> toHashtable(){
		Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
		
		hashtable.put( "Class", getClass().getName() );
		hashtable.put("ActualName", getActualName());
		hashtable.put("ActualValue", getActualValue() );
		hashtable.put("NameRule", getNameRule());
		hashtable.put("ValueRule", getValueRule());
		
		hashtable.put("ValueRuleType", Integer.valueOf( getValueRuleType() ) );
		hashtable.put("NameRuleType", Integer.valueOf( getNameRuleType() ) );
		
		return hashtable;
	}
} 
