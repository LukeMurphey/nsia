package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GroupManagement;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.web.Link;
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

		// 0 -- Check permissions
		//TODO Check rights
		
		// 1 -- Get the groups
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
		
		// 3 -- Get the menu
		Vector<Link> menu = new Vector<Link>();
		menu.add( new Link("Site Groups") );
		menu.add( new Link("Add Group", StandardViewList.getURL(SiteGroupEditView.VIEW_NAME, "New")) );
		
		menu.add( new Link("User Management") );
		menu.add( new Link("Add New User", UserEditView.getURL("New")) );
		menu.add( new Link("View Logged in Users", "ADDURL") );
		
		menu.add( new Link("Group Management") );
		menu.add( new Link("Add New Group", "ADDURL" ) );
		data.put("menu", menu);
		
		// 4 -- Get the breadcrumbs
		Vector<Link> breadcrumbs = new Vector<Link>();
		breadcrumbs.add(  new Link("Main Dashboard", StandardViewList.getURL("main_dashboard")) );
		breadcrumbs.add(  new Link("Group Management", createURL()) );
		data.put("breadcrumbs", breadcrumbs);
		
		//Get the dashboard headers
		Shortcuts.addDashboardHeaders(request, response, data);
		data.put("title", "Groups");
		
		TemplateLoader.renderToResponse("GroupList.ftl", data, response);
		
		return true;
	}

}
