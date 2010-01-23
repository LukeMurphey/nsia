package net.lukemurphey.nsia.xmlRpcInterface;

import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.*;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.DuplicateEntryException;
import net.lukemurphey.nsia.FirewallRule;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InetAddressRange;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.trustBoundary.ApiFirewallManagement;

public class XmlrpcFirewallManagement extends XmlrpcHandler {
	
	//private Firewall firewall = new Firewall( appRes );
	private ApiFirewallManagement firewallManager = new ApiFirewallManagement(appRes);
	
	public XmlrpcFirewallManagement(Application appRes) {
		super(appRes);
	}
	
	/**
	 * Set the default policy for the firewall. 
	 * @param sessionIdentifier
	 * @param denyByDefault
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	public void setDefaultDeny( String sessionIdentifier, boolean denyByDefault ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		firewallManager.setDefaultDeny( sessionIdentifier, denyByDefault );
	}
	
	/**
	 * Get the default policy for the firewall.
	 * @param sessionIdentifier
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	public boolean getDefaultDeny( String sessionIdentifier ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		return firewallManager.getDefaultDeny(sessionIdentifier);
	}
	
	/**
	 * Get a list of the firewall rules.
	 * @param sessionIdentifier
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	public Vector<Hashtable<String, Object>> getFirewallRules( String sessionIdentifier ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		FirewallRule[] rules = firewallManager.getFirewallRules( sessionIdentifier );
		
		Vector<Hashtable<String, Object>> firewallRules = new Vector<Hashtable<String, Object>>();
		
		for( int c = 0; c < rules.length; c++ ){
			firewallRules.add( convertRuleToVector(rules[c]) );
		}
		
		return firewallRules;
	}
	
	/**
	 * Determine if the given IP address would be permitted per the current firewall configuration.
	 * @param sessionIdentifier
	 * @param address
	 * @return
	 * @throws UnknownHostException
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	public int isAllowed( String sessionIdentifier, String address ) throws UnknownHostException, InsufficientPermissionException, GeneralizedException, NoSessionException{

		// 1 -- Perform the operation
		return firewallManager.isAllowed( sessionIdentifier, address ).ordinal();
	}
	
	
	/**
	 * Retrieve all of the firewall rules that match on the given address.
	 * @param sessionIdentifier
	 * @param address
	 * @return
	 * @throws UnknownHostException
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	public Vector<Hashtable<String, Object>> getMatchingFirewallRules( String sessionIdentifier, String address ) throws UnknownHostException, InsufficientPermissionException, GeneralizedException, NoSessionException{

		// 1 -- Perform the operation
		FirewallRule[] rules = firewallManager.getMatchingFirewallRules( sessionIdentifier, address );
		
		Vector<Hashtable<String, Object>> firewallRules = new Vector<Hashtable<String, Object>>();
		
		for( int c = 0; c < rules.length; c++ ){
			firewallRules.add( convertRuleToVector(rules[c]) );
		}
		
		return firewallRules;
	}
	
	/**
	 * Retrieve the firewall rule that matches the given ID.
	 * @param sessionIdentifier
	 * @param ruleId
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 * @throws NotFoundException 
	 */
	public Hashtable<String, Object> getFirewallRuleById( String sessionIdentifier, int ruleId ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, NotFoundException{
		
		// 1 -- Perform the operation
		return convertRuleToVector( firewallManager.getFirewallRuleById( sessionIdentifier, ruleId ) );
	}
	
	/**
	 * Add the firewall rule that corresponds to the given parameters.
	 * @param sessionIdentifier
	 * @param startAddressStr
	 * @param endAddressStr
	 * @param ruleDeny
	 * @param expireDate
	 * @throws DuplicateEntryException
	 * @throws SQLException
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws UnknownHostException
	 * @throws NoSessionException 
	 */
	public void addFirewallRule( String sessionIdentifier, String startAddressStr, String endAddressStr, boolean ruleDeny, Date expireDate ) throws DuplicateEntryException, SQLException, InsufficientPermissionException, GeneralizedException, UnknownHostException, NoSessionException{

		// 1 -- Perform the operation
		firewallManager.addFirewallRule( sessionIdentifier, startAddressStr, endAddressStr, ruleDeny, expireDate );
	}
	
	/**
	 * Remove the firewall rule that corresponds with the given identifier.
	 * @param sessionIdentifier
	 * @param ruleId
	 * @throws DuplicateEntryException
	 * @throws SQLException
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws UnknownHostException
	 * @throws NoSessionException 
	 */
	public void removeFirewallRule( String sessionIdentifier, int ruleId ) throws DuplicateEntryException, SQLException, InsufficientPermissionException, GeneralizedException, UnknownHostException, NoSessionException{

		// 1 -- Perform the operation
		firewallManager.removeFirewallRule( sessionIdentifier, ruleId );
	}
	
	/**
	 * Convert the firewall rule to a XML-RPC friendly version.
	 * @param firewallRule
	 * @return
	 */
	private Hashtable<String, Object> convertRuleToVector( FirewallRule firewallRule ){
		Hashtable<String, Object> rulesDesc = new Hashtable<String, Object>();
		InetAddressRange addressRange = firewallRule.getAddressRange();
		rulesDesc.put( "StartAddress", addressRange.getStartAddressString() );
		rulesDesc.put( "EndAddress", addressRange.getEndAddressString() );
		rulesDesc.put( "RuleID", Long.valueOf( firewallRule.getRuleId() ));
		rulesDesc.put( "ExpireTime", firewallRule.getRuleExpireTime());
		rulesDesc.put( "DenyRule", Boolean.valueOf( firewallRule.isDenyRule() ));
		
		return rulesDesc;
	}
	
	
	
	
}
