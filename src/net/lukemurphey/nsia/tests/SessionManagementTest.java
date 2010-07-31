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


public class SessionManagementTest extends TestCase {
	private SessionManagement sessionMgt;
	private String sID;

	Application app = null;
	
	public void setUp() throws Exception{
		app = TestApplication.getApplication();
		sessionMgt = new SessionManagement( app );
		
		ClientData clientData = new ClientData( InetAddress.getLocalHost(), "SiteSentry Test (main session)");
		sID = sessionMgt.createSession( 1, clientData );
	}
	
	public void tearDown(){
		TestApplication.stopApplication();
	}

	public void testCreateSession() throws UnknownHostException, NoSuchAlgorithmException, SQLException, NoDatabaseConnectionException, InputValidationException {
		ClientData clientData = new ClientData( InetAddress.getLocalHost(), "SiteSentry Test (test creation)");
		sID = sessionMgt.createSession( 1, clientData );
		
		if( sID == null )
			fail("The session could not be created");
		
	}

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

	public void testGetSessionStatusValid() throws SQLException, InputValidationException, NoDatabaseConnectionException {
		if( sID != null ){
			SessionStatus sessionStatus = sessionMgt.getSessionStatus( sID );
			if( sessionStatus != SessionStatus.SESSION_ACTIVE)
				fail("Session status should be active");
		}	
	}
	
	public void testGetSessionStatusInvalid() throws SQLException, InputValidationException, NoDatabaseConnectionException {
		SessionStatus sessionStatus = sessionMgt.getSessionStatus( "ABCDEF1234567890" );
		
		if( sessionStatus != SessionStatus.SESSION_NULL)
			fail("Session status should be null");
	}

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

	public void testGetSessionInfo() throws InputValidationException, SQLException, NoDatabaseConnectionException {
		SessionManagement.SessionInfo sessionInfo = sessionMgt.getSessionInfo( sID );
		
		if( sessionInfo.getSessionStatus() != SessionStatus.SESSION_ACTIVE )
			fail("The session was not disabled properly");
	}

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
		
		if( sessionInfo.length < 1 )
			fail("No active sessions where found even though at least one valid one should exist");
	}

}
