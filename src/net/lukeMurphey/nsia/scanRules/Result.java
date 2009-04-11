package net.lukeMurphey.nsia.scanRules;

/**
 * This class communicates the result of a scan and is intended to be used by the signature scripts.
 * @author luke
 *
 */
public class Result{
	public boolean matched = false;
	public String description = null;
	public int detectStart;
	public int detectEnd;
	
	public Result(boolean matched, String description){
		this.matched = matched;
		this.description = description;
	}
	
	public Result(boolean matched ){
		this.matched = matched;
	}
	
	public boolean matched(){
		return matched;
	}
	
	public String getDescription(){
		return description;
	}
	
	public int getDetectionStart(){
		return detectStart;
	}
	
	public int getDetectionEnd(){
		return detectEnd;
	}
}
