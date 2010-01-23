package net.lukemurphey.nsia.web;

import java.sql.SQLException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.UserManagement;
import net.lukemurphey.nsia.SessionManagement.SessionInfo;
import net.lukemurphey.nsia.UserManagement.UserDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;
import net.lukemurphey.nsia.web.SessionMessages.SessionMessageEntry;

public class RequestContext {

	private SessionInfo session_info;
	private SessionMessages session_messages;
	private UserDescriptor user;
	
	public RequestContext(){
		//Default constructor
	}
	
	public RequestContext(SessionInfo session_info, SessionMessages session_messages){
		this.session_info = session_info;
		this.session_messages = session_messages;
	}
	
	public RequestContext(SessionMessages session_messages){
		session_info = null;
		this.session_messages = session_messages;
	}
	
	public SessionInfo getSessionInfo(){
		return session_info;
	}
	
	public UserDescriptor getUser(){
		if( user == null ){
			
			//user = getUserInfo(session_info.getUserId());
			// TODO handle excessive error handling
			try {
				user = getUserInfo(session_info.getUserId());
			}catch (SQLException e){
				Application.getApplication().logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
				//throw new ViewFailedException(e);
			}catch (NoDatabaseConnectionException e) {
				Application.getApplication().logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e );
				//throw new ViewFailedException(e);
			}catch (NotFoundException e) {
				Application.getApplication().logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
				//throw new ViewFailedException(e);
			}
		}
		
		return user;
	}
	
	
	/**
	 * Get the user descriptor associated the user.
	 * @param userID
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws NotFoundException
	 */
	private UserDescriptor getUserInfo( int userID ) throws SQLException, NoDatabaseConnectionException, NotFoundException{
		UserManagement userManagement = new UserManagement(Application.getApplication());
		return userManagement.getUserDescriptor( userID );
	}
	
	public SessionMessages getSessionMessages(){
		return session_messages;
	}
	
	public SessionMessageEntry[] getMessages(){
		return session_messages.getMessages(session_info);
	}
	
	public void addMessage( String message, MessageSeverity sev){
		session_messages.addMessage(session_info, message, sev);
	}
	
}
