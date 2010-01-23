package net.lukemurphey.nsia.trustBoundary;

import java.net.InetAddress;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.DuplicateEntryException;
import net.lukemurphey.nsia.Firewall;
import net.lukemurphey.nsia.FirewallRule;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.HostAddress;
import net.lukemurphey.nsia.InetAddressRange;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.eventlog.EventLogMessage;

import java.net.UnknownHostException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;


public class ApiFirewallManagement extends ApiHandler {
	
	private Firewall firewall = new Firewall( appRes );
	
	public ApiFirewallManagement(Application appRes) {
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
		checkRight( sessionIdentifier, "System.Firewall.Edit");
		
		firewall.setDefaultDeny( denyByDefault );
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
		
		// 0 -- Precondition check
		checkRight( sessionIdentifier, "System.Firewall.View");
		
		return firewall.getDefaultDeny();
	}
	
	/**
	 * Get a list of the firewall rules.
	 * @param sessionIdentifier
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	public FirewallRule[] getFirewallRules( String sessionIdentifier ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		// 0 -- Precondition check
		checkRight( sessionIdentifier, "System.Firewall.View");
		
		return firewall.getFirewallRules();
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
	public Firewall.Action isAllowed( String sessionIdentifier, String address ) throws UnknownHostException, InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the address is legal
		HostAddress inetAddress = new HostAddress( address );
		
		//	 0.2 -- Make sure the user has permission
		checkRight( sessionIdentifier, "System.Firewall.View");
		
		// 1 -- Perform the operation
		return firewall.isAllowed( inetAddress );
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
	public FirewallRule[] getMatchingFirewallRules( String sessionIdentifier, String address ) throws UnknownHostException, InsufficientPermissionException, GeneralizedException, NoSessionException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the address is legal
		InetAddress inetAddress = InetAddress.getByName( address );
		
		//	 0.2 -- Make sure the user has permission
		checkRight( sessionIdentifier, "System.Firewall.View");
		
		// 1 -- Perform the operation
		return firewall.getMatchingFirewallRules( inetAddress );
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
	public FirewallRule getFirewallRuleById( String sessionIdentifier, long ruleId ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, NotFoundException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has permission
		checkRight( sessionIdentifier, "System.Firewall.View");
		
		
		// 1 -- Perform the operation
		return firewall.getFirewallRuleById( ruleId );
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
	public void addFirewallRule( String sessionIdentifier, String startAddressStr, String endAddressStr, boolean ruleDeny, Date expireDate ) throws DuplicateEntryException, InsufficientPermissionException, GeneralizedException, UnknownHostException, NoSessionException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has permission
		checkRight( sessionIdentifier, "System.Firewall.Edit");
		
		// 1 -- Perform the operation
		Timestamp ruleExpireTime = new Timestamp ( expireDate.getTime()  );
		
		InetAddress startAddress = InetAddress.getByName( startAddressStr );
		InetAddress endAddress = InetAddress.getByName( endAddressStr );
		InetAddressRange ruleAddressRange = InetAddressRange.getByRange( startAddress, endAddress );
		FirewallRule firewallRule = new FirewallRule( ruleAddressRange, ruleDeny, ruleExpireTime );
		try {
			firewall.addFirewallRule( firewallRule );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
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
	public void removeFirewallRule( String sessionIdentifier, long ruleId ) throws DuplicateEntryException, InsufficientPermissionException, GeneralizedException, UnknownHostException, NoSessionException{
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the user has permission
		checkRight( sessionIdentifier, "System.Firewall.Edit");
		
		// 1 -- Perform the operation
		try{
			firewall.removeFirewallRule( ruleId );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
}
