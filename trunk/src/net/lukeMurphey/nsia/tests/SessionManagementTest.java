package net.lukemurphey.nsia.tests;

import junit.framework.TestCase;

import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.ClientData;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.SessionManagement;
import net.lukemurphey.nsia.SessionStatus;
import net.lukemurphey.nsia.TestsConfig;


public class SessionManagementTest extends TestCase {
	private SessionManagement sessionMgt;
	private Application appRes;
	private String sID;
	
	public static void main(String[] args) {
	}

	public SessionManagementTest(String name) throws BindException, SQLException, InputValidationException, Exception {
		super(name);
		appRes = TestsConfig.getApplicationResource();
		sessionMgt = new SessionManagement( appRes );
	}

	protected void setUp() throws Exception {
		super.setUp();
		ClientData clientData = new ClientData( InetAddress.getLocalHost(), "SiteSentry Test (main session)");
		sID = sessionMgt.createSession( 1, clientData );
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.SessionManagement.createSession(long, ClientData)'
	 */
	public void testCreateSession() throws UnknownHostException, NoSuchAlgorithmException, SQLException, NoDatabaseConnectionException, InputValidationException {
		ClientData clientData = new ClientData( InetAddress.getLocalHost(), "SiteSentry Test (test creation)");
		sID = sessionMgt.createSession( 1, clientData );
		if( sID == null )
			fail("The session could not be created");
		
		//System.out.println("Session Id =" + sID );
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.SessionManagement.refreshSessionIdentifier(String)'
	 */
	public void testRefreshSessionIdentifier() throws NoSuchAlgorithmException, InputValidationException, SQLException, NoDatabaseConnectionException {
		String oldSid = sID;
		sID = sessionMgt.refreshSessionIdentifier( sID );
		if( sID == null ){
			sID = oldSid;//Try to fail over to the old SID
			fail("The session refresh failed, session destroyed");
		}
		else if ( sID.equals(oldSid))
			fail("The session refresh failed, session identifier unchanged");
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.SessionManagement.getSessionStatus(String)'
	 */
	public void testGetSessionStatus() throws SQLException, InputValidationException, NoDatabaseConnectionException {
		if( sID != null ){
			SessionStatus sessionStatus = sessionMgt.getSessionStatus( sID );
			if( sessionStatus != SessionStatus.SESSION_ACTIVE)
				fail("Session status should be active");
		}
		
		{
			SessionStatus sessionStatus = sessionMgt.getSessionStatus( "ABCDEF1234567890" );
			if( sessionStatus != SessionStatus.SESSION_NULL)
				fail("Session status should be null");
		}
			
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.SessionManagement.terminateSession(String)'
	 */
	public void testTerminateSession() throws InputValidationException, SQLException, UnknownHostException, NoSuchAlgorithmException, NoDatabaseConnectionException {
		if( sID != null ){
			sessionMgt.terminateSession( sID );
			
			SessionStatus sessionStatus = sessionMgt.getSessionStatus( sID );
			if( sessionStatus != SessionStatus.SESSION_NULL)
				fail("Session status should be null");
			
			//Recreate sessions for other tests
			testCreateSession();
		}
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.SessionManagement.getSessionInfo(String)'
	 */
	public void testGetSessionInfo() throws InputValidationException, SQLException, NoDatabaseConnectionException {
		SessionManagement.SessionInfo sessionInfo = sessionMgt.getSessionInfo( sID );
		if( sessionInfo.getSessionStatus() != SessionStatus.SESSION_ACTIVE )
			fail("The session was not disabled properly");
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.SessionManagement.disableUserSessions(long)'
	 */
	public void testDisableUserSessions() throws UnknownHostException, NoSuchAlgorithmException, SQLException, NoDatabaseConnectionException, InputValidationException {
		ClientData clientData = new ClientData( InetAddress.getLocalHost(), "SiteSentry Test (To be disabled)");
		String disableSession = sessionMgt.createSession( 100, clientData );
		sessionMgt.disableUserSessions( 100 );
		SessionManagement.SessionInfo sessionInfo = sessionMgt.getSessionInfo( disableSession );
		if( sessionInfo.getSessionStatus() != SessionStatus.SESSION_ADMIN_TERMINATED )
			fail("The session was not disabled properly");
	}
	
	public void testGetCurrentSessions() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		SessionManagement.SessionInfo[] sessionInfo = sessionMgt.getCurrentSessions();
		System.out.println(sessionInfo.length);
		if( sessionInfo.length < 1 )
			fail("No active sessions where found even though at least one valid one should exist");
	}

}
