package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.SessionManagement;
import net.lukemurphey.nsia.SessionManagement.SessionInfo;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.Menu;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.StandardViewList;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.templates.TemplateLoader;

public class UserSessionsView extends View {
	
	public static String VIEW_NAME = "user_sessions";
	
	public UserSessionsView() {
		super("User/Sessions", VIEW_NAME);
	}
	
	public static String getURL() throws URLInvalidException{
		UserSessionsView view = new UserSessionsView();
		
		return view.createURL();
	}

	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {

		// 1 -- Get the sessions
		SessionManagement sessionManagement = new SessionManagement(Application.getApplication());
		SessionInfo[] sessions = null;
		
		try {
			sessions = sessionManagement.getCurrentSessions();
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (InputValidationException e) {
			throw new ViewFailedException(e);
		}
		
		data.put("title", "User Sessions");
		data.put("sessions", sessions);
		
		// 2 -- Get the menu
		data.put("menu", Menu.getGenericMenu(context));
		
		// 3 -- Get the breadcrumbs
		Vector<Link> breadcrumbs = new Vector<Link>();
		breadcrumbs.add(  new Link("Main Dashboard", StandardViewList.getURL("main_dashboard")) );
		breadcrumbs.add(  new Link("User Management", UsersView.getURL()) );
		breadcrumbs.add(  new Link("User Sessions", createURL()) );
		
		data.put("breadcrumbs", breadcrumbs);
		
		//Get the dashboard headers
		Shortcuts.addDashboardHeaders(request, response, data);
		
		// 4 -- Check permissions
		try {
			if( Shortcuts.hasRight( context.getSessionInfo(), "Users.Sessions.View", "View user sessions") == false ){
				Shortcuts.getPermissionDeniedDialog(response, data, "You do not have permission to view user sessions");
				return true;
			}
		} catch (GeneralizedException e) {
			throw new ViewFailedException(e);
		}
		
		TemplateLoader.renderToResponse("UserSessions.ftl", data, response);
		
		return true;
		
	}

}
