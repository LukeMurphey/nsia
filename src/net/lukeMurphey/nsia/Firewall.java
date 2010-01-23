package net.lukemurphey.nsia;

import java.util.*;
import java.net.*;
import java.sql.*;

import net.lukemurphey.nsia.FirewallRule.Result;

public class Firewall {
	private Application appRes;
	private Vector<FirewallRule> firewallRules;
	private boolean defaultDeny = true;
	
	public enum Action{
		REJECT,
		ACCEPT,
		REJECTED_BY_DEFAULT,
		ACCEPTED_BY_DEFAULT
	}
	
	public enum Mode{
		OPEN,
		RESTRICTED
	}
	
	private Mode firewallMode = Mode.RESTRICTED;
	
	/**
	 * Instantiate a firewall rule set evaluator.
	 * @param applicationResources
	 */
	public Firewall( Application applicationResources ){
		
		// 0 -- Precondition check
		if( applicationResources == null )
			throw new IllegalArgumentException("The application resource cannot be null");
		
		appRes = applicationResources;
	}
	
	/**
	 * Set the firewall to accept connections normally (allow even if the address is not the local
	 * machine).
	 *
	 */
	public void setOpenMode(){
		firewallMode = Mode.OPEN;
	}
	
	/**
	 * Set the firewall to accept connections only from the local machine (loopback address range)
	 *
	 */
	public void setRestrictedMode(){
		firewallMode = Mode.RESTRICTED;
	}
	
	/**
	 * Set the policy to default accept or default deny.
	 * @precondition None
	 * @postcondition The policy will be default deny or accept depending on the arguments.
	 * @param denyByDefault
	 */
	public void setDefaultDeny( boolean denyByDefault ){
		defaultDeny = denyByDefault;
	}
	
	/**
	 * Get the current policy (default accept or default deny).
	 * @precondition None
	 * @postcondition The policy will be returned
	 * @param denyByDefault
	 */
	public boolean getDefaultDeny( ){
		return defaultDeny;
	}
	
	/**
	 * Retrieve an array consisting of the current firewall rules.
	 * @precondition The firewall rule vector must be populated, or null will be returned.
	 * @postcondition An array with the firewall rules will returned or null if no rules exist. 
	 * @return
	 */
	public FirewallRule[] getFirewallRules(){
		if( firewallRules == null )
			return null;
		
		FirewallRule[] firewallRulesArray = new FirewallRule[firewallRules.size()];
		
		for(int c = 0; c < firewallRules.size(); c++ ){
			firewallRulesArray[c] = (FirewallRule)firewallRules.get(c);
		}
		
		return firewallRulesArray;
	}
	
	/**
	 * The method causes the class to load the firewall rules from the database. The existing rules in the class will be cleared.
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 */
	public synchronized void loadFirewallRulesFromDatabase() throws NoDatabaseConnectionException, SQLException{
		// 0 -- Precondition check
		// The database connection will be checked in the try block
		Connection connection = null;
		
		// 1 -- Load database rules
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.FIREWALL);
			if( connection == null )
				throw new NoDatabaseConnectionException();
			
			statement = connection.prepareStatement("Select * from Firewall where RuleState = 1");
			result = statement.executeQuery();
			Vector<FirewallRule> tempFirewallRules = new Vector<FirewallRule>();
			
			while( result.next() ){
				boolean deny;
				
				// Determine if the rule is a deny or accept
				if( result.getInt("Action") == 0 )
					deny = true;
				else
					deny = false;
				
				InetAddressRange addressRange = InetAddressRange.getByRange( new HostAddress(result.getString("IpStart")), new HostAddress(result.getString("IpEnd")) );
				FirewallRule firewallRule;
					
				if( result.getTimestamp("ExpirationDate") == null )
					firewallRule = new FirewallRule( addressRange, deny, result.getLong("RuleID") );
				else
					firewallRule = new FirewallRule( addressRange, deny, result.getLong("RuleID"),  result.getTimestamp("ExpirationDate"));
					
				tempFirewallRules.add(firewallRule);

			}
			
			
			// 2 -- Make the rules the current set
			firewallRules = tempFirewallRules;
			
		} finally {
			if (result != null )
				result.close();
			
			if (statement != null )
				statement.close();
			
			if (connection != null )
				connection.close();
		}
	}
	
	/**
	 * Get the firewall mode (restricted or open mode).
	 * @return
	 */
	public Mode getFirewallMode(){
		return firewallMode;
	}
	
	/**
	 * Determine if the given address is accepted or rejected by the ruleset. Note that the loopback address range
	 * ( 127.0.0.2 - 127.255.255.254) will always be accepted to prevent locking out local users.
	 * @precondition The address given must not be null
	 * @postcondition An integer will be returned specifying if the address is accepted
	 * @param address
	 * @return
	 */
	public Action isAllowed( HostAddress address ){
		
		// 0 -- Precondition check
		
		//	 0.1 -- The address must not be null
		if( address == null )
			throw new IllegalArgumentException("The IP address must not be null");
		
		
		// 1 -- Make sure to always accept loopback addresses (otherwise everyone will be locked out, even the local administrator)
		if( InetAddressRange.isAddressLoopback(address) )
			return Action.ACCEPT;
		
		if( firewallMode == Mode.RESTRICTED )
			return Action.REJECT;
		
		
		// 2 -- Determine if the rule is specfically matched
		if( firewallRules == null ){ //Invoke the default action if no rules are set 
			if( defaultDeny )
				return Action.REJECTED_BY_DEFAULT;
			else
				return Action.ACCEPTED_BY_DEFAULT;
		}
		
		Iterator<FirewallRule> iterator = firewallRules.iterator();
		
		while( iterator.hasNext() ){
			FirewallRule currentRule = (FirewallRule)iterator.next();
			FirewallRule.Result status = currentRule.isAccepted( address );
			
			if( status == FirewallRule.Result.REJECT )
				return Action.REJECT;
			else if( status == FirewallRule.Result.ACCEPT )
				return Action.ACCEPT;
		}
		
		
		// 2 -- Determine the default action (the following lines should only be executed of a specific match could not be found)
		if( defaultDeny )
			return Action.REJECTED_BY_DEFAULT;
		else
			return Action.ACCEPTED_BY_DEFAULT;
	}
	
	/**
	 * Get all rules that match the given address (both reject and accept rules).
	 * @precondition The address must not be null
	 * @postcondition An array of rules that match the given address will be returned
	 * @param address
	 * @return
	 */
	public FirewallRule[] getMatchingFirewallRules( InetAddress address ){
		
		// 0 -- Precondition check
		if( firewallRules == null )
			return new FirewallRule[0];
		
		// 1 -- Determine what rules match
		Vector<FirewallRule> matchedRules = new Vector<FirewallRule>();
		
		Iterator<FirewallRule> currentIterator = firewallRules.iterator();
		while( currentIterator.hasNext() ){
			FirewallRule firewallRule = (FirewallRule) currentIterator.next();
			
			if( firewallRule.isAccepted( address ) != Result.NOT_MATCH )
				matchedRules.add( firewallRule );
		}
		
		// 2 -- Convert the rule vector to an array
		FirewallRule[] firewallRulesArray = new FirewallRule[matchedRules.size()];
		
		for(int c = 0; c < matchedRules.size(); c++ ){
			firewallRulesArray[c] = matchedRules.get(c); //Don't insert rules that are null (i.e. have been deleted since they matched)
		}
		
		return firewallRulesArray;
	}
	
	/**
	 * Get the firewall rule that matches the identifier given or return null if no such rule exists.
	 * @precondition None
	 * @postcondition The rule with associated with the given ID will be returned, or null if no such rule exists
	 * @param ruleId
	 * @return
	 * @throws NotFoundException 
	 */
	public FirewallRule getFirewallRuleById(long ruleId ) throws NotFoundException{
		
		// 0 -- Precondition check
		if( firewallRules == null ) // Make sure a rules list exists, return null if no rules exist
			return null;
		
		// 1 -- Cycle through the rules to determine if any of them match the given ID
		Iterator<FirewallRule> currentIterator = firewallRules.iterator();
		
		while( currentIterator.hasNext() ){
			FirewallRule currentRule = (FirewallRule) currentIterator.next();
			if( currentRule.getRuleId() == ruleId )
				return currentRule;
		}
		
		throw new NotFoundException("No firewall rule exists with the given ID");
	}
	
	/**
	 * Determine if a firewall rule exists with the given identifier.
	 * @precondition None
	 * @postcondition returns true if the firewall rule exists
	 * @param ruleId
	 * @return
	 */
	public boolean doesFirewallRuleExist(long ruleId ){
		
		// 0 -- Precondition check
		if( firewallRules == null ) // Make sure a rules list exists, return null if no rules exist
			return false;
		
		// 1 -- Cycle through the rules to determine if any of them match the given ID
		Iterator<FirewallRule> currentIterator = firewallRules.iterator();
		while( currentIterator.hasNext() ){
			FirewallRule currentRule = (FirewallRule) currentIterator.next();
			if( currentRule.getRuleId() == ruleId )
				return true;
		}
		
		return false;
	}
	
	/**
	 * Remove the rule associated with the given identifier.
	 * @precondition The rule identifier must exist in the list
	 * @postcondition The rule associated with the identifier will be removed from the list and the persistent storage (database)
	 * @param ruleId
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException 
	 */
	public boolean removeFirewallRule( long ruleId ) throws SQLException, NoDatabaseConnectionException{
		// 0 -- Precondition check
		// The database connection will be checked in the try block
		Connection connection = null;
		
		// 1 -- Remove the rule from the database
		PreparedStatement statement  = null;
		
		try{
			connection = appRes.getDatabaseConnection( Application.DatabaseAccessType.FIREWALL );
			statement = connection.prepareStatement("Delete from Firewall where RuleID = ?");
			statement.setLong(1, ruleId);
			statement.executeUpdate();		
			
			// 2 -- Remove the rule from the firewall rule list
			boolean ruleFound = false;
			for( int c = 0; c < firewallRules.size(); c++ ){
				FirewallRule firewallRule = (FirewallRule)firewallRules.get(c);
				if( firewallRule.getRuleId() == ruleId ){
					firewallRules.remove(c);
					ruleFound = true;
				}
			}
			
			return ruleFound;
		} finally {
			
			if (statement != null )
				statement.close();
			
			if (connection != null )
				connection.close();
		}
	}
	
	/**
	 * Add the given firewall rule to the current rule set. This rule will become active as soon as it is added.
	 * @precondition The rule must not be null and must not have the same ID as a preexisting rule
	 * @precondition The rule will be added to the list (if not null and if another rule does not already exist with the same ID) 
	 * @param firewallRule
	 * @throws DuplicateEntryException
	 * @throws SQLException 
	 * @throws NoDatabaseConnectionException 
	 */
	public synchronized void addFirewallRule( FirewallRule firewallRule ) throws DuplicateEntryException, SQLException, NoDatabaseConnectionException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the rule is not null
		if( firewallRule == null )
			throw new IllegalArgumentException("The firewall rule cannot be null");
		
		//	 0.2 -- Make sure the rule does not already exist (by ID)
		if( doesFirewallRuleExist( firewallRule.getRuleId() ) )
			throw new DuplicateEntryException("Firewall rule cannot be added since one already exists with the given identifier (" + firewallRule.getRuleId() + ")");
		
		//	 0.3 -- Make sure a database connection is available
		// The database connection will be checked in the try block
		Connection connection = null;
		
		// 1 -- Add the rule to the list
		firewallRules.add(firewallRule);
		
		// 2 -- Add the rule to the persistent storage
		Timestamp expiredate = firewallRule.getRuleExpireTime();
		
		PreparedStatement statement = null;
		
		try{
			connection = appRes.getDatabaseConnection(Application.DatabaseAccessType.FIREWALL);
			
			if( expiredate == null)
				statement = connection.prepareStatement("Insert into Firewall (IpStart, IpEnd, Action, RuleState) values (?, ?, ?, ?) ");
			else
				statement = connection.prepareStatement("Insert into Firewall (IpStart, IpEnd, Action, RuleState, ExpirationDate) values (?, ?, ?, ?, ?) ");
			
			int action;
			if( firewallRule.isDenyRule() )
				action = 0;
			else
				action = 1;
			
			statement.setString(1, firewallRule.getAddressRange().getStartAddressString());
			statement.setString(2, firewallRule.getAddressRange().getEndAddressString());
			statement.setInt(3, action);
			statement.setInt(4, 1);
			
			if( expiredate != null)
				statement.setTimestamp(5, firewallRule.getRuleExpireTime());
			
		} finally {			
			if (statement != null )
				statement.close();
			
			if (connection != null )
				connection.close();
		}
	}
}
