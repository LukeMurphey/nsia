package net.lukemurphey.nsia.tests;

import junit.framework.TestCase;
import java.net.*;
import java.sql.*;

import net.lukemurphey.nsia.FirewallRule;
import net.lukemurphey.nsia.InetAddressRange;

public class FirewallRuleTest extends TestCase {
	
	FirewallRule firewallDenyRule;
	FirewallRule firewallAcceptRule;
	
	public static void main(String[] args) {
	}

	public FirewallRuleTest(String name) throws UnknownHostException {
		super(name);
		InetAddressRange inetAddressRange = InetAddressRange.getByRange(InetAddress.getByName("127.0.0.1"), InetAddress.getByName("127.255.255.254"));
		firewallDenyRule = new FirewallRule(inetAddressRange, true, 1);
		firewallAcceptRule = new FirewallRule(inetAddressRange, false, 2);
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.FirewallRule.FirewallRule(InetAddressRange, boolean, long, Timestamp)'
	 */
	public void testFirewallRuleInetAddressRangeBooleanLongTimestamp() throws UnknownHostException {
		InetAddressRange inetAddressRange = InetAddressRange.getByRange(InetAddress.getByName("127.0.0.1"), InetAddress.getByName("127.255.255.254"));
		FirewallRule oldFirewallRule = new FirewallRule(inetAddressRange, false, 1, new Timestamp(System.currentTimeMillis() - 10000));
		
		InetAddress address = InetAddress.getByName("127.0.0.3");
		if( oldFirewallRule.isAccepted(address) != FirewallRule.Result.ACCEPT )
			fail("The temporarily blocked rule should have been accepted since the block has expired");
		
		FirewallRule tempBlockedFirewallRule = new FirewallRule(inetAddressRange, true, 1, new Timestamp(System.currentTimeMillis() + 100000));
		
		if( tempBlockedFirewallRule.isAccepted(address) != FirewallRule.Result.REJECT )
			fail("The temporarily blocked rule should have been rejected since the block is active");
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.FirewallRule.FirewallRule(InetAddressRange, boolean, long)'
	 */
	public void testFirewallRuleInetAddressRangeBooleanLong() throws UnknownHostException {
		InetAddressRange inetAddressRange = InetAddressRange.getByRange(InetAddress.getByName("127.0.0.1"), InetAddress.getByName("127.255.255.254"));
		FirewallRule acceptFirewallRule = new FirewallRule(inetAddressRange, false, 1);
		
		// Test the accept policy
		InetAddress inAddress = InetAddress.getByName("127.0.0.3");
		InetAddress outAddress = InetAddress.getByName("191.1.3.11");
		
		if( acceptFirewallRule.isAccepted(inAddress) != FirewallRule.Result.ACCEPT )
			fail("The firewall rule should have accepted");
		
		if( acceptFirewallRule.isAccepted(outAddress) != FirewallRule.Result.NOT_MATCH )
			fail("The firewall rule should have not matched");
		
		
		// Test a reject policy
		FirewallRule rejectFirewallRule = new FirewallRule(inetAddressRange, true, 1);
		
		if( rejectFirewallRule.isAccepted(inAddress) != FirewallRule.Result.REJECT )
			fail("The firewall rule should have rejected");
		
		if( rejectFirewallRule.isAccepted(outAddress) != FirewallRule.Result.NOT_MATCH )
			fail("The firewall rule should have not matched");
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.FirewallRule.isDenyRule()'
	 */
	public void testIsDenyRule() {
		if( firewallDenyRule.isDenyRule() != true )
			fail("The rule was not properly flagged as a deny rule");
		
		if( firewallAcceptRule.isDenyRule() == true )
			fail("The rule was not properly flagged as an accept rule");
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.FirewallRule.getAddressRange()'
	 */
	public void testGetAddressRange() throws UnknownHostException {
		InetAddress inAddress = InetAddress.getByName("127.0.0.3");
		InetAddress outAddress = InetAddress.getByName("191.1.3.11");
		
		InetAddressRange inetAddressRange = firewallDenyRule.getAddressRange();
		if( !inetAddressRange.isWithinRange( inAddress ))
			fail("The range did not include the range");
		
		if( inetAddressRange.isWithinRange( outAddress ))
			fail("The range included the range");
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.FirewallRule.getRuleExpireTime()'
	 */
	public void testGetRuleExpireTime() throws UnknownHostException {
		InetAddressRange inetAddressRange = InetAddressRange.getByRange(InetAddress.getByName("127.0.0.1"), InetAddress.getByName("127.255.255.254"));
		
		Timestamp expireTime = new Timestamp(System.currentTimeMillis() - 10000);
		FirewallRule oldFirewallRule = new FirewallRule(inetAddressRange, false, 1, expireTime);
		Timestamp returnedExpireTime = oldFirewallRule.getRuleExpireTime();
		
		if( returnedExpireTime.getTime() != expireTime.getTime() )
			fail("The expiration times do not match but should have");
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.FirewallRule.isExpired()'
	 */
	public void testIsExpired() throws UnknownHostException {
		InetAddressRange inetAddressRange = InetAddressRange.getByRange(InetAddress.getByName("127.0.0.1"), InetAddress.getByName("127.255.255.254"));
		FirewallRule oldFirewallRule = new FirewallRule(inetAddressRange, false, 1, new Timestamp(System.currentTimeMillis() - 10000));

		if( !oldFirewallRule.isExpired() )
			fail("The temporarily blocked rule have indicated an expired status");
		
		FirewallRule tempBlockedFirewallRule = new FirewallRule(inetAddressRange, true, 1, new Timestamp(System.currentTimeMillis() + 100000));
		
		if( tempBlockedFirewallRule.isExpired() )
			fail("The temporarily blocked rule should have indicated an active status");
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.FirewallRule.getRuleId()'
	 */
	public void testGetRuleId() throws UnknownHostException {
		InetAddressRange inetAddressRange = InetAddressRange.getByRange(InetAddress.getByName("127.0.0.1"), InetAddress.getByName("127.255.255.254"));
		FirewallRule oldFirewallRule = new FirewallRule(inetAddressRange, false, 100, new Timestamp(System.currentTimeMillis() - 10000));

		if( oldFirewallRule.getRuleId() != 100 )
			fail("Incorrect rule identifier was returned");
	}
}
