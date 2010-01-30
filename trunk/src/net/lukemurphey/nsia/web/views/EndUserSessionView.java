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
import net.lukemurphey.nsia.SessionManagement;
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
		
		try {
			sessionManagement.terminateSession(sessionTrackingNumber);
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
