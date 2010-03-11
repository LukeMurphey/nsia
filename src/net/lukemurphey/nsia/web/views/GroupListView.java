package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.GroupManagement;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
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

public class GroupListView extends View {

	public static final String VIEW_NAME = "groups_list";
	
	public GroupListView() {
		super("Group", VIEW_NAME);
	}

	public static String getURL() throws URLInvalidException{
		GroupListView view = new GroupListView();
		
		return view.createURL();
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {

		// 1 -- Get the page content
		
		//	 1.1 -- Get the menu
		data.put("menu", Menu.getGenericMenu(context));
		
		//	 1.2 -- Get the breadcrumbs
		Vector<Link> breadcrumbs = new Vector<Link>();
		breadcrumbs.add(  new Link("Main Dashboard", StandardViewList.getURL("main_dashboard")) );
		breadcrumbs.add(  new Link("Group Management", createURL()) );
		data.put("breadcrumbs", breadcrumbs);
		
		//	 1.3 -- Get the dashboard headers
		Shortcuts.addDashboardHeaders(request, response, data);
		data.put("title", "Groups");
		
		// 2 -- Check permissions
		try {
			if( Shortcuts.hasRight( context.getSessionInfo(), "Groups.View", "View user groups") == false ){
				Shortcuts.getPermissionDeniedDialog(response, data, "You do not have permission to view the user groups");
				return true;
			}
		} catch (GeneralizedException e) {
			throw new ViewFailedException(e);
		}
		
		// 3 -- Get the groups
		GroupManagement groupMgmt = new GroupManagement(Application.getApplication());
		
		try {
			data.put("groups", groupMgmt.getGroupDescriptors());
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (InputValidationException e) {
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		}
		
		data.put("ACTIVE", GroupManagement.State.ACTIVE);
		data.put("INACTIVE", GroupManagement.State.INACTIVE);
		
		TemplateLoader.renderToResponse("GroupList.ftl", data, response);
		
		return true;
	}

}
