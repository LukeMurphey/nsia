package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.GroupManagement;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.UserManagement;
import net.lukemurphey.nsia.GroupManagement.GroupDescriptor;
import net.lukemurphey.nsia.UserManagement.AccountStatus;
import net.lukemurphey.nsia.UserManagement.UserDescriptor;
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
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class GroupView extends View {

	public static final String VIEW_NAME = "group";
	
	public GroupView() {
		super("Group", VIEW_NAME, Pattern.compile("[0-9]+"));
	}
	
	public static String getURL( GroupDescriptor group ) throws URLInvalidException{
		GroupView view = new GroupView();
		
		return view.createURL(group.getGroupId());
	}
	
	public static String getURL( int groupID ) throws URLInvalidException{
		GroupView view = new GroupView();
		
		return view.createURL(groupID);
	}
	
	public static class UserGroupInfo{
		
		private boolean isMember;
		private UserDescriptor user;
		
		public UserGroupInfo( UserDescriptor user, boolean isMember ){
			this.user = user;
			this.isMember = isMember;
		}
		
		public long getUserID(){
			return user.getUserID();
		}
		
		public String getUserName(){
			return user.getUserName();
		}
		
		public boolean isEnabled(){
			return user.getAccountStatus() == AccountStatus.VALID_USER;
		}
		
		public AccountStatus getStatus(){
			return user.getAccountStatus();
		}
		
		public boolean isMemberOf(){
			return isMember;
		}
		
		public String getFullname(){
			return user.getFullname();
		}
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {
		
		// 1 -- Get the group if one exists
		GroupDescriptor group = null;
		GroupManagement groupMgmt = null;
		
		{
			// 1.1 -- Get the group ID
			int groupID;
			
			try{
				groupID = Integer.valueOf( args[0] );
			}
			catch( NumberFormatException e ){
				Dialog.getDialog(response, context, data, "The Group ID provided is not valid", "Group ID Invalid", DialogType.WARNING);
				return true;
			}
			
			// 1.2 -- Get the group descriptor
			groupMgmt = new GroupManagement(Application.getApplication());
			
			try{
				group = groupMgmt.getGroupDescriptor(groupID);
			}
			catch(NotFoundException e){
				Dialog.getDialog(response, context, data, "No group was found with the given ID", "Group Not Found", DialogType.WARNING);
				return true;
			} catch (SQLException e) {
				throw new ViewFailedException(e);
			} catch (NoDatabaseConnectionException e) {
				throw new ViewFailedException(e);
			} catch (InputValidationException e) {
				throw new ViewFailedException(e);
			}
			
			if( group == null ){
				Dialog.getDialog(response, context, data, "No group was found with the given ID", "Group Not Found", DialogType.WARNING);
				return true;
			}
			
			data.put("group", group);
		}
		
		// 2 -- Get the page content
		
		//	 2.1 -- Get the menu
		data.put("menu", Menu.getGroupMenuItems(context, group));
		
		//	 2.2 -- Get the breadcrumbs
		Vector<Link> breadcrumbs = new Vector<Link>();
		breadcrumbs.add(  new Link("Main Dashboard", StandardViewList.getURL("main_dashboard")) );
		breadcrumbs.add(  new Link("Group Management", GroupListView.getURL()) );
		breadcrumbs.add(  new Link("Group: " + group.getGroupName(), GroupView.getURL( group.getGroupId() )) );
		
		data.put("title", "Group: " + group.getGroupName());
		
		data.put("breadcrumbs", breadcrumbs);
		
		//	 2.3 -- Get the dashboard headers
		Shortcuts.addDashboardHeaders(request, response, data);
		
		// 3 -- Check rights
		try {
			if( Shortcuts.hasRight( context.getSessionInfo(), "Groups.View", "View user ID " + group.getGroupId() + ") " + group.getGroupName() ) == false ){
				Shortcuts.getPermissionDeniedDialog(response, data, "You do not have permission to view groups");
				return true;
			}
		} catch (GeneralizedException e) {
			throw new ViewFailedException(e);
		}
		
		// 4 -- Determine if the user can enumerate the users
		boolean canEnumUsers = false;
		
		try{
			canEnumUsers = Shortcuts.hasRight( context.getSessionInfo(), "Groups.Membership.Edit", "List users for group membership");
		}
		catch( GeneralizedException e ){
			throw new ViewFailedException(e);
		}
		
		data.put("can_enum_users", canEnumUsers);
		
		// 5 -- Enumerate the users in the list
		StringBuffer includedUsers = new StringBuffer();
		Vector<UserGroupInfo> userMembership = new Vector<UserGroupInfo>();
		
		try {
			UserManagement userMgmt = new UserManagement(Application.getApplication());
			UserDescriptor[] users = userMgmt.getUserDescriptors();
			
			for (UserDescriptor user : users) {
				userMembership.add( new UserGroupInfo(user, groupMgmt.isUserMemberOfGroup(user.getUserID(), group.getGroupId())) );
				
				if( includedUsers.length() > 0 ){
					includedUsers.append(",");
				}
				
				includedUsers.append(user.getUserID());
			}
			
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		}
		
		data.put("users", userMembership);
		data.put("included_users", includedUsers);
		
		TemplateLoader.renderToResponse("GroupView.ftl", data, response);
		
		return true;
		
	}

}
