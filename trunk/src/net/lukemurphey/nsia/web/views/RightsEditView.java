package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.AccessControl;
import net.lukemurphey.nsia.AccessControlDescriptor;
import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.GroupManagement;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.RightDescriptor;
import net.lukemurphey.nsia.UserManagement;
import net.lukemurphey.nsia.AccessControlDescriptor.Action;
import net.lukemurphey.nsia.AccessControlDescriptor.Subject;
import net.lukemurphey.nsia.GroupManagement.GroupDescriptor;
import net.lukemurphey.nsia.UserManagement.UserDescriptor;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.StandardViewList;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;
import net.lukemurphey.nsia.web.templates.TemplateLoader;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class RightsEditView extends View {

	public static final String VIEW_NAME = "rights_editor";
	
	public RightsEditView() {
		super("Rights", VIEW_NAME, Pattern.compile("User|Group", Pattern.CASE_INSENSITIVE), Pattern.compile("[0-9]+"));
	}

	public static String getURL(UserDescriptor user) throws URLInvalidException{
		RightsEditView view = new RightsEditView();
		return view.createURL("User", user.getUserID());
	}
	
	public static String getURL(GroupDescriptor group) throws URLInvalidException{
		RightsEditView view = new RightsEditView();
		return view.createURL("Group", group.getGroupId());
	}
	
	public class Right{
		private String name;
		private String description;
		private boolean permitted;
		
		protected Right( String name, String description, boolean permitted ){
			this.name = name;
			this.description = description;
			this.permitted = permitted;
		}
		
		public boolean isPermitted(){
			return permitted;
		}
		
		public String getName(){
			return name;
		}
		
		public String getDescription(){
			return description;
		}
	}
	
	private Right getRight( String right, String description, int subjectID, AccessControlDescriptor.Subject subjectType, AccessControl accessControl ) throws SQLException, NoDatabaseConnectionException, NotFoundException{
		 RightDescriptor rightDescriptor = getRightDescriptor(right, description, subjectID, subjectType, accessControl);
		 
		 return new Right(right, description, rightDescriptor.getRight() == Action.PERMIT);
	}
	
	private RightDescriptor getRightDescriptor( String right, String description, int subjectID, AccessControlDescriptor.Subject subjectType, AccessControl accessControl ) throws SQLException, NoDatabaseConnectionException, NotFoundException{
		// 1 -- Get the relevant right descriptor
		RightDescriptor rightDescriptor = null;
		
		if( subjectType == AccessControlDescriptor.Subject.USER){
			rightDescriptor = accessControl.getUserRight( subjectID, right, false);
		}
		else{
			rightDescriptor = accessControl.getGroupRight(subjectID, right);
		}
		
		return rightDescriptor;
	}
	
	public enum Tab{
		USER_MANAGEMENT, GROUP_MANAGEMENT, SITE_GROUP_MANAGEMENT, SYSTEM_CONFIGURATION;
		
		public static Tab getFromOrdinal(int ord){
			for (Tab tab : Tab.values()) {
				if(tab.ordinal() == ord ){
					return tab;
				}
			}
			
			return Tab.USER_MANAGEMENT;
		}
	}
	
	private boolean setRight(HttpServletRequest request, String rightName, int subjectId, AccessControlDescriptor.Subject subjectType, AccessControl accessControl ) throws NoSessionException, GeneralizedException, ViewFailedException{
		
		AccessControlDescriptor.Action right;
		
		if( request.getParameter(rightName) != null)
			right = AccessControlDescriptor.Action.PERMIT;
		else
			right = AccessControlDescriptor.Action.DENY;
		
		RightDescriptor rightDescriptor = new RightDescriptor(right, subjectType, subjectId, rightName);
		
		try{
			return accessControl.setRight(rightDescriptor);
		}
		catch(NoDatabaseConnectionException e){
			throw new ViewFailedException(e);
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		}
	}
	
	private int setRights( RequestContext context, HttpServletRequest request, HttpServletResponse response,  Tab tabIndex, int subjectId, Subject subjectType ) throws NoSessionException, GeneralizedException, ViewFailedException{
		AccessControl accessControl = new AccessControl(Application.getApplication());
		
		int setFailures = 0;
		
		if( tabIndex == Tab.USER_MANAGEMENT ){
			if( !setRight( request, "Users.Add", subjectId, subjectType, accessControl) ) setFailures++;
			if( !setRight( request, "Users.Edit", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( request, "Users.View", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( request, "Users.Rights.View", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( request, "Users.Delete", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( request, "Users.Unlock",  subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( request, "Users.UpdatePassword", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( request, "Users.UpdateOwnPassword", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( request, "Users.Sessions.Delete", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( request, "Users.Sessions.View", subjectId, subjectType, accessControl)) setFailures++;
			
			context.addMessage("Rights successfully updated", MessageSeverity.SUCCESS);
		}
		else if( tabIndex == Tab.GROUP_MANAGEMENT ){
			if( !setRight( request, "Groups.Add", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( request, "Groups.View", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( request, "Groups.Edit", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( request, "Groups.Delete", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( request, "Groups.Membership.Edit", subjectId, subjectType, accessControl)) setFailures++;
			
			context.addMessage("Rights successfully updated", MessageSeverity.SUCCESS);
		}
		else if( tabIndex == Tab.SITE_GROUP_MANAGEMENT ){
			if( !setRight( request, "SiteGroups.View", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( request, "SiteGroups.Add", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( request, "SiteGroups.Delete", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( request, "SiteGroups.Edit", subjectId, subjectType, accessControl)) setFailures++;
			
			context.addMessage("Rights successfully updated", MessageSeverity.SUCCESS);
		}
		else if( tabIndex == Tab.SYSTEM_CONFIGURATION ){
			if( !setRight( request, "System.Information.View", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( request, "System.Configuration.Edit", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( request, "Administration.ClearSqlWarnings", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( request, "System.Firewall.View", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( request, "System.Firewall.Edit", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( request, "System.ControlScanner", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( request, "SiteGroups.ScanAllRules", subjectId, subjectType, accessControl)) setFailures++;
			
			context.addMessage("Rights successfully updated", MessageSeverity.SUCCESS);
		}
		
		return setFailures;
	}
	
	private Vector<Right> getRights( Tab tabIndex, int subjectId, AccessControlDescriptor.Subject subjectType, AccessControl accessControl ) throws SQLException, NoDatabaseConnectionException, NotFoundException{
		Vector<Right> rights = new Vector<Right>();
		
		if( tabIndex == Tab.USER_MANAGEMENT ){
			rights.add( getRight( "Users.Add", "Create New Users", subjectId, subjectType, accessControl ) ) ;
			rights.add( getRight( "Users.Edit", "Edit User", subjectId, subjectType, accessControl ) ) ;
			rights.add( getRight( "Users.View", "View Users", subjectId, subjectType, accessControl ) ) ;
			//rights.add( getRight( "Users.Rights.View", "View Users' Rights", subjectId, subjectType, accessControl ) ) ;
			rights.add( getRight( "Users.Delete", "Delete Users", subjectId, subjectType, accessControl ) ) ;
			
			rights.add( getRight( "Users.Unlock", "Unlock Accounts (due to repeated authentication attempts)", subjectId, subjectType, accessControl ) ) ;
			rights.add( getRight( "Users.UpdatePassword", "Update Other's Password (applies only to the other users' accounts)", subjectId, subjectType, accessControl ) ) ;
			rights.add( getRight( "Users.UpdateOwnPassword", "Update Account Details (applies only to the users' own account)", subjectId, subjectType, accessControl ) ) ;
			rights.add( getRight( "Users.Sessions.Delete", "Delete Users' Sessions (kick users off)", subjectId, subjectType, accessControl ) ) ;
			rights.add( getRight( "Users.Sessions.View", "View Users' Sessions (see who is logged in)", subjectId, subjectType, accessControl ) ) ;
		}
		else if( tabIndex == Tab.GROUP_MANAGEMENT ){
			rights.add( getRight( "Groups.Add", "Create New Groups", subjectId, subjectType, accessControl ) ) ;
			rights.add( getRight( "Groups.View", "View Groups", subjectId, subjectType, accessControl ) ) ;
			rights.add( getRight( "Groups.Edit", "Edit Groups", subjectId, subjectType, accessControl ) ) ;
			rights.add( getRight( "Groups.Delete", "Delete Groups", subjectId, subjectType, accessControl ) ) ;
			rights.add( getRight( "Groups.Membership.Edit", "Manage Group Membership", subjectId, subjectType, accessControl ) ) ;
		}
		else if( tabIndex == Tab.SITE_GROUP_MANAGEMENT ){
			rights.add( getRight( "SiteGroups.View", "View Site Groups", subjectId, subjectType, accessControl ) ) ;
			rights.add( getRight( "SiteGroups.Add", "Create New Site Group", subjectId, subjectType, accessControl ) ) ;
			rights.add( getRight( "SiteGroups.Delete", "Delete Site Groups", subjectId, subjectType, accessControl ) ) ;
			rights.add( getRight( "SiteGroups.Edit", "Edit Site Groups", subjectId, subjectType, accessControl ) ) ;
		}
		else {//if( tabIndex == Tab.SYSTEM_CONFIGURATION){
			rights.add( getRight( "System.Information.View", "View System Information and Status", subjectId, subjectType, accessControl ) ) ;
			rights.add( getRight( "System.Configuration.Edit", "Modify System Configuration", subjectId, subjectType, accessControl ) ) ;
			rights.add( getRight( "System.Firewall.View", "View Firewall Configuration", subjectId, subjectType, accessControl ) ) ;
			rights.add( getRight( "System.Firewall.Edit", "Change Firewall Configuration", subjectId, subjectType, accessControl ) ) ;
			rights.add( getRight( "System.ControlScanner", "Start/Stop Scanner", subjectId, subjectType, accessControl ) ) ;
			rights.add( getRight( "SiteGroups.ScanAllRules", "Allow Gratuitous Scanning of All Rules", subjectId, subjectType, accessControl ) ) ;
		}
		
		return rights;
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {
		
		// 0 -- Check permissions
		//TODO check rights
		
		// 1 -- Determine which tab is displayed
		Tab tabIndex = Tab.USER_MANAGEMENT;
		
		if( request.getParameter("TabIndex") != null ){
			try{
				tabIndex = Tab.getFromOrdinal( Integer.valueOf( request.getParameter("TabIndex")));
			}
			catch(NumberFormatException e){
				//Ignore, the error and just show the user management tab
			}
		}
		
		data.put("tabIndex", tabIndex.ordinal());
		data.put("USER_MANAGEMENT", Tab.USER_MANAGEMENT.ordinal());
		data.put("GROUP_MANAGEMENT", Tab.GROUP_MANAGEMENT.ordinal());
		data.put("SYSTEM_CONFIGURATION", Tab.SYSTEM_CONFIGURATION.ordinal());
		data.put("SITE_GROUP_MANAGEMENT", Tab.SITE_GROUP_MANAGEMENT.ordinal());
		
		// 2 -- Enumerate the rights
		AccessControl accessControl = new AccessControl(Application.getApplication());
		boolean isUser = true;
		Subject subjectType = Subject.USER;
		int subjectID;
		Shortcuts.addDashboardHeaders(request, response, data);
		
		try{
			subjectID = Integer.valueOf( args[1] );
		}
		catch(NumberFormatException e ){
			Dialog.getDialog(response, context, data, "The identifier for the user or group is not a valid number.", "User or Group ID Invalid", DialogType.WARNING);
			return true;
		}
		
		if( "Group".equalsIgnoreCase( args[0] ) ){
			isUser = false;
			subjectType = Subject.GROUP;
		}
		
		Vector<Right> rights = null;
		
		try{
			if( isUser ){
				rights = getRights(tabIndex, subjectID, Subject.USER, accessControl);
			}
			else{
				rights = getRights(tabIndex, subjectID, Subject.GROUP, accessControl);
			}
		}
		catch(NotFoundException e ){
			throw new ViewFailedException(e);
			//Dialog.getDialog(response, context, data, "The identifier for the user or group is not a valid number.", "User or Group ID Invalid", DialogType.WARNING);
			//return true;
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		}
		
		// 3 -- Set the rights if requested
		if( "POST".equalsIgnoreCase( request.getMethod() ) ){
			try {
				setRights(context, request, response, tabIndex, subjectID, subjectType);
				
				if( subjectType == Subject.GROUP ){
					response.sendRedirect( createURL( "Group", subjectID) + "?TabIndex=" + tabIndex.ordinal() );
				}
				else{
					response.sendRedirect( createURL( "User", subjectID) + "?TabIndex=" + tabIndex.ordinal() );
				}
				
				return true; //Return since we submitted a redirect to the HTTP response
			} catch (NoSessionException e) {
				throw new ViewFailedException(e);
			} catch (GeneralizedException e) {
				throw new ViewFailedException(e);
			}
		}
		
		data.put("rights", rights);
		
		// 4 -- Get the menu
		Vector<Link> menu = new Vector<Link>();
		menu.add( new Link("Site Groups") );
		menu.add( new Link("Add Group", StandardViewList.getURL(SiteGroupEditView.VIEW_NAME, "New")) );
		
		menu.add( new Link("User Management") );
		menu.add( new Link("Add New User", UserEditView.getURL()) );
		menu.add( new Link("View Logged in Users", "ADDURL") );
		
		menu.add( new Link("Group Management") );
		menu.add( new Link("List Groups", "ADDURL" ) );
		menu.add( new Link("Add New Group", "ADDURL" ) );
		data.put("menu", menu);
		
		// 4 -- Get the breadcrumbs
		Vector<Link> breadcrumbs = new Vector<Link>();
		breadcrumbs.add(  new Link("Main Dashboard", StandardViewList.getURL("main_dashboard")) );
		if( isUser ){
			UserManagement userMgmt = new UserManagement(Application.getApplication());
			UserDescriptor user;
			try {
				user = userMgmt.getUserDescriptor(subjectID);
			} catch (SQLException e) {
				throw new ViewFailedException(e);
			} catch (NoDatabaseConnectionException e) {
				throw new ViewFailedException(e);
			} catch (NotFoundException e) {
				throw new ViewFailedException(e);
			}
			data.put("user", user);
			breadcrumbs.add( new Link("User Management", UsersView.getURL()) );
			breadcrumbs.add( new Link("View User: " + user.getUserName(), UserView.getURL(user)) );
			breadcrumbs.add( new Link("Rights", RightsEditView.getURL(user) ) );
		}
		else{
			GroupManagement groupMgmt = new GroupManagement(Application.getApplication());
			GroupDescriptor group;
			try {
				group = groupMgmt.getGroupDescriptor(subjectID);
			} catch (SQLException e) {
				throw new ViewFailedException(e);
			} catch (NoDatabaseConnectionException e) {
				throw new ViewFailedException(e);
			} catch (NotFoundException e) {
				throw new ViewFailedException(e);
			} catch (InputValidationException e) {
				throw new ViewFailedException(e);
			}
			data.put("group", group);
			breadcrumbs.add( new Link("Edit Group: " + group.getGroupName(), GroupEditView.getURL(group)) );
			breadcrumbs.add( new Link("Group Management", GroupListView.getURL() ) );
			breadcrumbs.add( new Link("Rights", RightsEditView.getURL(group) ) );
		}
		
		data.put("isUser", isUser);
		data.put("breadcrumbs", breadcrumbs);
		data.put("title", "Rights Management");
		
		// 5 -- Render the page
		TemplateLoader.renderToResponse("RightsEditView.ftl", data, response);
		return true;
	}

}
