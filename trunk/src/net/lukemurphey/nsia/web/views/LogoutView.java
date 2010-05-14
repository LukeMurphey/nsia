package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.SessionManagement;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.StandardViewList;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;

public class LogoutView extends View {

	public LogoutView() {
		super("Logout", "logout");
	}

	public static String getURL() throws URLInvalidException{
		LogoutView view = new LogoutView();
		return view.createURL();
	}
	
	@Override
	protected boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException, IOException, URLInvalidException, ViewNotFoundException {
		
		// 1 -- Determine if a session identifier exists
		Cookie[] cookies = request.getCookies();
		String sessionID = null;
		
		for (Cookie cookie : cookies) {
			if( cookie.getName().equals("SessionID") ){
				sessionID = cookie.getValue();
			}
		}
		
		// 2 -- Logout the session
		if( sessionID != null ){
			Application app = Application.getApplication();
			
			SessionManagement session_mgmt = new SessionManagement(app);
			try {
				session_mgmt.terminateSession(sessionID);
			} catch (InputValidationException e) {
				app.logEvent(EventLogMessage.EventType.SESSION_ID_ILLEGAL, new EventLogField( FieldName.SESSION_ID, sessionID ) );
			} catch (SQLException e) {
				app.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
				throw new ViewFailedException(e);
			} catch (NoDatabaseConnectionException e) {
				app.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e );
				throw new ViewFailedException(e);
			}
		}
		
		// 3 -- Redirect the user to the login
		response.sendRedirect( StandardViewList.getURL("login") + "?LoggedOut" );
		
		return true;
		
	}

}
