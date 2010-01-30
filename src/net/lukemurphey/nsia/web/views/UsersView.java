package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.UserManagement;
import net.lukemurphey.nsia.UserManagement.UserDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.StandardViewList;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.templates.TemplateLoader;

public class UsersView extends View {

	public static final String VIEW_NAME ="users_list";
	
	public UsersView() {
		super("Users", VIEW_NAME);
	}
	
	public static String getURL( ) throws URLInvalidException{
		UsersView v = new UsersView();
		
		return v.createURL();
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {

		// 1 -- Check permissions
		//TODO Check permissions
		//checkRight( sessionIdentifier, "Users.View" );
		
		// 2 -- Get the users
		UserDescriptor[] users;
		try {
			UserManagement userManagement = new UserManagement(Application.getApplication());
			
			users = userManagement.getUserDescriptors( );
		} catch (SQLException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new ViewFailedException(e);
		}
		
		data.put("users", users);
		data.put("ADMINISTRATIVELY_LOCKED", UserManagement.AccountStatus.ADMINISTRATIVELY_LOCKED);
		data.put("BRUTE_FORCE_LOCKED", UserManagement.AccountStatus.BRUTE_FORCE_LOCKED);
		data.put("DISABLED", UserManagement.AccountStatus.DISABLED);
		data.put("INVALID_USER", UserManagement.AccountStatus.INVALID_USER);
		data.put("VALID_USER", UserManagement.AccountStatus.VALID_USER);
		
		// 3 -- Get the menu
		Vector<Link> menu = new Vector<Link>();
		menu.add( new Link("Site Groups") );
		menu.add( new Link("Add Group", StandardViewList.getURL(SiteGroupEditView.VIEW_NAME, "New")) );
		
		menu.add( new Link("User Management") );
		menu.add( new Link("Add New User", UserEditView.getURL("New")) );
		menu.add( new Link("View Logged in Users", "ADDURL") );
		
		menu.add( new Link("Group Management") );
		menu.add( new Link("List Groups", "ADDURL" ) );
		menu.add( new Link("Add New Group", "ADDURL" ) );
		data.put("menu", menu);
		
		// 4 -- Get the breadcrumbs
		Vector<Link> breadcrumbs = new Vector<Link>();
		breadcrumbs.add(  new Link("Main Dashboard", StandardViewList.getURL("main_dashboard")) );
		breadcrumbs.add(  new Link("User Management", createURL()) );
		data.put("breadcrumbs", breadcrumbs);
		
		//Get the dashboard headers
		Shortcuts.addDashboardHeaders(request, response, data);
		data.put("title", "Users");
		
		TemplateLoader.renderToResponse("Users.ftl", data, response);
		
		return true;
	}

}
