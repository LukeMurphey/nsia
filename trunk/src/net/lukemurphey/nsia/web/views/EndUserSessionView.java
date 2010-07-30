package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.SessionManagement;
import net.lukemurphey.nsia.SessionStatus;
import net.lukemurphey.nsia.UserManagement;
import net.lukemurphey.nsia.SessionManagement.SessionInfo;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class EndUserSessionView extends View {

	public static final String VIEW_NAME = "user_session_end"; 
	
	public EndUserSessionView() {
		super("User/Session/Terminate", VIEW_NAME, Pattern.compile("[0-9]+"));
	}

	public static String getURL( int sessionTrackingID ) throws URLInvalidException{
		EndUserSessionView view = new EndUserSessionView();
		return view.createURL(sessionTrackingID);
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {
		
		try {
			
			// 1 -- Get the session tracking number
			int sessionTrackingNumber;
			
			try{
				sessionTrackingNumber = Integer.valueOf( args[0] );
			}catch(NumberFormatException e){
				throw new ViewFailedException(e);
			}
			
			// 2 -- Get the user and session information
			SessionManagement sessionManagement = new SessionManagement(Application.getApplication());
			SessionInfo sessionInfo = null;
			UserManagement.UserDescriptor userDescriptor;
			
			sessionInfo = sessionManagement.getSessionInfo(sessionTrackingNumber);
			
			if( sessionInfo.getSessionStatus() != SessionStatus.SESSION_NULL ){
				//Get the user associated with the session
				UserManagement userManagement = new UserManagement(Application.getApplication());
				try {
					userDescriptor = userManagement.getUserDescriptor(sessionInfo.getUserId());
				} catch (NotFoundException e1) {
					throw new ViewFailedException(e1);
				}
			}
			else{
				userDescriptor = null;
			}
			
			// 3 -- Get the page content
			try {

				String annotation = null;
				
				if( userDescriptor != null ){
					annotation = "Terminate session for user ID " + userDescriptor.getUserID() + " (" + userDescriptor.getUserName() + ")";
				}
				
				if( Shortcuts.hasRight( context.getSessionInfo(), "Users.Sessions.Delete", annotation) == false ){
					context.addMessage("You do not have permission to end user sessions", MessageSeverity.WARNING);
					response.sendRedirect( UserSessionsView.getURL() );
					return true;
				}
			} catch (GeneralizedException e) {
				throw new ViewFailedException(e);
			}
			
			// 4 -- Post a message if the session is invalid
			if( sessionInfo.getSessionStatus() == SessionStatus.SESSION_NULL ){
				Dialog.getDialog(response, context, data, "No session exists with the given identifier", "Session Tracking Number Invalid", DialogType.WARNING, new Link("Return to the session list", UserSessionsView.getURL()));
				return true;
			}
			
			// 5 -- Terminate the session
			if( sessionManagement.terminateSession(sessionTrackingNumber) ){
				
				//Log that the session was terminated
				Application.getApplication().logEvent(EventLogMessage.EventType.SESSION_ENDED,
						new EventLogField( FieldName.TARGET_USER_NAME, userDescriptor.getUserName()),
						new EventLogField( FieldName.TARGET_USER_ID, userDescriptor.getUserID() ),
						new EventLogField( FieldName.SOURCE_USER_NAME, context.getUser().getUserName()),
						new EventLogField( FieldName.SOURCE_USER_ID, context.getUser().getUserID() ) );
			}
			else{
				Application.getApplication().logEvent(EventLogMessage.EventType.SESSION_INVALID_TERMINATION_ATTEMPT,
						new EventLogField( FieldName.SESSION_TRACKING_NUMBER, sessionTrackingNumber ));
			}
			
			context.addMessage("Session successfully terminated", MessageSeverity.SUCCESS);
			response.sendRedirect( UserSessionsView.getURL() );
			
		} catch (InputValidationException e) {
			throw new ViewFailedException(e);
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		}
		
		return true;
	}

}
