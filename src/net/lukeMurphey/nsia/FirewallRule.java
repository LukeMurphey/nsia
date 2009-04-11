package net.lukeMurphey.nsia;

import java.net.*;
import java.sql.*;

public class FirewallRule {
	private InetAddressRange addressRange;
	private boolean denyRule = true;
	private long ruleId = -1;
	private Timestamp expiresOn;
	
	public enum Result{
		NOT_MATCH,
		ACCEPT,
		REJECT
	}
	
	/**
	 * Creates a firewall rule corresponding to the given effective address range and rule type (deny or accept).
	 * The expire date indicates when the rule is expired. If expired, the rule is nullified (i.e. accepts default action).
	 * @precondition The address range must not be null and the rule ID must be greater than zero.
	 * @postcondition A new firewall rule will be returned that corresponds to the given parameters.
	 * @param ruleAddressRange
	 * @param ruleDeny
	 * @param ruleId
	 * @param ruleExpireTime
	 */
	public FirewallRule( InetAddressRange ruleAddressRange, boolean ruleDeny, long ruleId, Timestamp ruleExpireTime ){
		// 0 -- Precondition check
		if( ruleAddressRange == null )
			throw new IllegalArgumentException("The IP address range cannot be null");
		
		if( ruleId < 0 )
			throw new IllegalArgumentException("The rule ID must not be less than zero");
		
		// 1 -- Method body
		addressRange = ruleAddressRange;
		denyRule = ruleDeny;
		this.ruleId = ruleId;
		expiresOn = ruleExpireTime;
	}
	
	/**
	 * Creates a firewall rule corresponding to the given effective address range and rule type (deny or accept).
	 * @precondition The address range must not be null and the rule ID must be greater than zero.
	 * @postcondition A new firewall rule will be returned that corresponds to the given parameters.
	 * @param ruleAddressRange
	 * @param ruleDeny
	 * @param ruleId
	 */
	public FirewallRule( InetAddressRange ruleAddressRange, boolean ruleDeny, long ruleId ){
		this( ruleAddressRange, ruleDeny, ruleId, null );
	}
	
	/**
	 * Creates a firewall rule corresponding to the given effective address range and rule type (deny or accept).
	 * @precondition The address range must not be null.
	 * @postcondition A new firewall rule will be returned that corresponds to the given parameters.
	 * @param ruleAddressRange
	 * @param ruleDeny
	 */
	public FirewallRule( InetAddressRange ruleAddressRange, boolean ruleDeny ){
		// 0 -- Precondition check
		if( ruleAddressRange == null )
			throw new IllegalArgumentException("The IP address range cannot be null");
		
		// 1 -- Method body
		addressRange = ruleAddressRange;
		denyRule = ruleDeny;
	}
	
	/**
	 * Creates a firewall rule corresponding to the given effective address range and rule type (deny or accept).
	 * @precondition The address range must not be null.
	 * @postcondition A new firewall rule will be returned that corresponds to the given parameters.
	 * @param ruleAddressRange
	 * @param ruleDeny
	 * @param ruleExpireTime
	 */
	public FirewallRule( InetAddressRange ruleAddressRange, boolean ruleDeny, Timestamp ruleExpireTime ){
		// 0 -- Precondition check
		if( ruleAddressRange == null )
			throw new IllegalArgumentException("The IP address range cannot be null");
		
		// 1 -- Method body
		addressRange = ruleAddressRange;
		denyRule = ruleDeny;
		expiresOn = ruleExpireTime;
	}
	
	/**
	 * Indicates if this rule is a deny rule.
	 * @precondition None
	 * @postcondition A boolean indicating if the rule is deny rule will be returned. 
	 * @return
	 */
	public boolean isDenyRule(){
		return denyRule;
	}
	
	/**
	 * Get the address range for the given rule.
	 * @precondition None
	 * @postcondition The address range for the firewall rule will be returned
	 * @return
	 */
	public InetAddressRange getAddressRange(){
		return addressRange;
	}
	
	/**
	 * Retrieves the time that the rule expires on. Returns null if no expiration time
	 * is set.
	 * @precondition None
	 * @postcondition The time that the rule will expire on is returned.
	 *
	 */
	public Timestamp getRuleExpireTime(){
		return expiresOn;
	}
	
	/**
	 * Determines if the given rule has expired.
	 * @precondition The expire time must be set or the function will automatically return true;
	 * @postcondition A boolean indicating if the rule has expired will be returned.
	 * @return
	 */
	public boolean isExpired(){
		
		// 0 -- Precondition Check
		
		//	 0.1 -- The expiration date must be set or the rule will assumed to be non-expiring
		if( expiresOn == null )
			return false;
		
		// 1 -- Determine if the expiration date has been met or exceeded
		long currentTime = System.currentTimeMillis();
		long expireTime = expiresOn.getTime();
		
		if( currentTime > expireTime )
			return true;
		else
			return false;
	}
	
	/**
	 * Retrieves the identifier associated with the rule.
	 * @precondition The rule must be set to a valid value
	 * @postcondition The rule value will be returned that is associated with this firewall rule
	 * @return
	 */
	public long getRuleId(){
		return ruleId;
	}
	
	/**
	 * Determines if the address range given is accepted by the this firewall rule.
	 * @precondition The address must not be null (or an IllegalArgument exception will be returned)
	 * @postcondition A boolean will be returned that indicates if the address is permitted per the current rule
	 * @param address
	 * @param defaultDeny
	 * @return
	 */
	public Result isAccepted( InetAddress address){
		
		// 0 -- Precondition check
		if( address == null )
			throw new IllegalArgumentException("The IP address cannot be null");
		
		// 1 -- Method body
		if( addressRange.isWithinRange(address) )
			if( denyRule )
				return Result.REJECT;
			else
				return Result.ACCEPT;
		else
			return Result.NOT_MATCH;
	}
	
	/**
	 * Determines if the address range given is accepted by the this firewall rule.
	 * @precondition The address must not be null (or an IllegalArgument exception will be returned)
	 * @postcondition A boolean will be returned that indicates if the address is permitted per the current rule
	 * @param address
	 * @param defaultDeny
	 * @return
	 */
	public Result isAccepted( HostAddress address){
		
		// 0 -- Precondition check
		if( address == null )
			throw new IllegalArgumentException("The IP address cannot be null");
		
		// 1 -- Method body
		if( addressRange.isWithinRange(address) )
			if( denyRule )
				return Result.REJECT;
			else
				return Result.ACCEPT;
		else
			return Result.NOT_MATCH;
	}
	
}
