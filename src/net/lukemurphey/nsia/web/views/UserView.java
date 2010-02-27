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
import net.lukemurphey.nsia.GroupMembershipDescriptor;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.UserManagement;
import net.lukemurphey.nsia.GroupManagement.GroupDescriptor;
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

public class UserView extends View {

	public static final String VIEW_NAME = "user";
	
	public UserView() {
		super("User", VIEW_NAME, Pattern.compile("[0-9]+"));
	}

	public static String getURL( int userID ) throws URLInvalidException{
		UserView v = new UserView();
		
		return v.createURL( userID );
	}
	
	public static String getURL( UserDescriptor user ) throws URLInvalidException{
		UserView v = new UserView();
		
		return v.createURL( user.getUserID() );
	}
	
	public static class UserGroupInfo{
		
		private boolean isMember;
		private GroupDescriptor group;
		
		public UserGroupInfo( GroupDescriptor group, boolean isMember ){
			this.group = group;
			this.isMember = isMember;
		}
		
		public long getID(){
			return group.getGroupId();
		}
		
		public String getName(){
			return group.getGroupName();
		}
		
		public GroupManagement.State getStatus(){
			return group.getGroupState();
		}
		
		public boolean isMemberOf(){
			return isMember;
		}
		
		public String getDescription(){
			return group.getDescription();
		}
		
	}
	
	/**
	 * Get an array indicating which groups the user is member of.
	 * @param user
	 * @return
	 * @throws ViewFailedException
	 */
	private UserGroupInfo[] getUserGroupInfo( UserDescriptor user ) throws ViewFailedException{
		
		try{
			GroupManagement groupMgmt = new GroupManagement(Application.getApplication());
			
			GroupMembershipDescriptor groupMembership = groupMgmt.getGroupMembership(user.getUserID());
			
			Vector<UserGroupInfo> groupInfo = new Vector<UserGroupInfo>();
			
			for (GroupDescriptor group: groupMgmt.getGroupDescriptors()) {
				groupInfo.add( new UserGroupInfo(group, groupMembership.isMemberOfGroup(group)));
			}
			
			UserGroupInfo[] groupInfoArray = new UserGroupInfo[groupInfo.size()];
			groupInfo.toArray(groupInfoArray);
			return groupInfoArray;
		}
		catch(NoDatabaseConnectionException e){
			throw new ViewFailedException(e);
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (InputValidationException e) {
			throw new ViewFailedException(e);
		}
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {

		// 0 -- Check permissions
		//TODO check rights
		
		// 1 -- Get the user
		int userID;
		
		try{
			userID = Integer.valueOf( args[0] );
		}
		catch( NumberFormatException e ){
			Dialog.getDialog(response, context, data, "The User ID provided is not valid", "User ID Invalid", DialogType.WARNING);
			return true;
		}
		
		UserManagement userMgmt = new UserManagement(Application.getApplication());
		UserDescriptor user = null;
		
		try{
			user = userMgmt.getUserDescriptor(userID);
		}
		catch(NotFoundException e){
			Dialog.getDialog(response, context, data, "No user was found with the given ID", "User Not Found", DialogType.WARNING);
			return true;
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		}
		
		if( user == null ){
			Dialog.getDialog(response, context, data, "No user was found with the given ID", "User Not Found", DialogType.WARNING);
			return true;
		}
		
		data.put("user", user);
		
		// 2 -- Get the user group membership
		UserGroupInfo[] userGroups = null;
		try{
			boolean can_enum_groups = Shortcuts.hasRight( context.getSessionInfo(), "Groups.View");
			data.put("can_enum_groups", can_enum_groups );
			
			if( can_enum_groups ){
				userGroups = getUserGroupInfo(user);
			}
		}
		catch( GeneralizedException e ){
			throw new ViewFailedException(e);
		}
		
		// Get the list of included groups
		String included_groups = null;
		
		for (UserGroupInfo userGroupInfo : userGroups) {
			if( included_groups == null ){
				included_groups = "" + userGroupInfo.getID();
			}
			else{
				included_groups = included_groups + "," +  userGroupInfo.getID();
			}
		}
		
		data.put("included_groups", included_groups);
		data.put("groups", userGroups);
		
		// 3 -- Get the menu
		data.put("menu", Menu.getUserMenu(context, user));
		
		// 4 -- Get the breadcrumbs
		Vector<Link> breadcrumbs = new Vector<Link>();
		breadcrumbs.add(  new Link("Main Dashboard", StandardViewList.getURL("main_dashboard")) );
		breadcrumbs.add(  new Link("User Management", UsersView.getURL()) );
		breadcrumbs.add(  new Link("View User: " + user.getUserName(), UserView.getURL(user)) );
		data.put("breadcrumbs", breadcrumbs);
		
		//Get the dashboard headers
		Shortcuts.addDashboardHeaders(request, response, data);
		data.put("title", "User: " + user);
		
		//checkRight( sessionIdentifier, "Groups.View");
		data.put("can_enum_groups", true);//TODO change and add permissions
		
		TemplateLoader.renderToResponse("User.ftl", data, response);
		
		return true;
	}

}