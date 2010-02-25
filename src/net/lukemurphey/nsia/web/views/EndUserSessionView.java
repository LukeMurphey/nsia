package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.SessionManagement;
import net.lukemurphey.nsia.UserManagement;
import net.lukemurphey.nsia.SessionManagement.SessionInfo;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;

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
		
		// 0 -- Check permissions
		//checkRight( sessionIdentifer, "Users.Sessions.Delete");
		//TODO Check rights
		
		// 1 -- Terminate the session
		int sessionTrackingNumber;
		
		try{
			sessionTrackingNumber = Integer.valueOf( args[0] );
		}catch(NumberFormatException e){
			throw new ViewFailedException(e);
		}
		
		SessionManagement sessionManagement = new SessionManagement(Application.getApplication());
		SessionInfo sessionInfo = null;
		
		try {
			sessionInfo = sessionManagement.getSessionInfo(sessionTrackingNumber);
			 
			if( sessionManagement.terminateSession(sessionTrackingNumber) ){
				//Get the user associated with the now terminated session
				UserManagement.UserDescriptor userDescriptor;
				UserManagement userManagement = new UserManagement(Application.getApplication());
				
				try{
					userDescriptor = userManagement.getUserDescriptor(sessionInfo.getUserId());
					
					Application.getApplication().logEvent(EventLogMessage.Category.SESSION_ENDED,
							new EventLogField( FieldName.TARGET_USER_NAME, userDescriptor.getUserName()),
							new EventLogField( FieldName.TARGET_USER_ID, userDescriptor.getUserID() ),
							new EventLogField( FieldName.SOURCE_USER_NAME, context.getUser().getUserName()),
							new EventLogField( FieldName.SOURCE_USER_ID, context.getUser().getUserID() ) );
				}
				catch( NotFoundException e){
					Application.getApplication().logEvent(EventLogMessage.Category.SESSION_ENDED, 
							new EventLogField( FieldName.SOURCE_USER_NAME, sessionInfo.getUserName()),
							new EventLogField( FieldName.SOURCE_USER_ID, sessionInfo.getUserId() ) );
				}

			}
			else{
				Application.getApplication().logEvent(EventLogMessage.Category.SESSION_INVALID_TERMINATION_ATTEMPT,
						new EventLogField( FieldName.SESSION_TRACKING_NUMBER, sessionTrackingNumber ),
						new EventLogField( FieldName.SESSION_TRACKING_NUMBER, sessionTrackingNumber ));
			}
			
			context.addMessage("Session successfully terminated", MessageSeverity.SUCCESS);
			response.sendRedirect( UsersView.getURL() );
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
