package net.lukemurphey.nsia.htmlInterface;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.*;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.DisallowedOperationException;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.GroupManagement;
import net.lukemurphey.nsia.GroupMembershipDescriptor;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.InvalidLocalPartException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.PasswordInvalidException;
import net.lukemurphey.nsia.UserManagement;
import net.lukemurphey.nsia.GroupManagement.GroupDescriptor;
import net.lukemurphey.nsia.UserManagement.UserDescriptor;
import net.lukemurphey.nsia.htmlInterface.Html.MessageType;
import net.lukemurphey.nsia.trustBoundary.ApiGroupManagement;
import net.lukemurphey.nsia.trustBoundary.ApiUserManagement;

import org.apache.commons.lang.StringEscapeUtils;

public class HtmlUserManagement extends HtmlContentProvider{
	
	private static final int OP_ADD_USERNAME_INVALID = 100;
	private static final int OP_ADD_PASSWORD_INVALID = 101;
	private static final int OP_ADD_REALNAME_INVALID = 102;
	private static final int OP_ADD_EMAIL_LOCAL_PART_INVALID = 103;
	private static final int OP_ADD_EMAIL_HOST_PART_INVALID = 104;
	private static final int OP_ADD_PASSWORD_NOT_SAME = 105;
	private static final int OP_ADD_DUPLICATE_ACCOUNT = 105;
	private static final int OP_UPDATE_USERNAME_INVALID = 107;
	private static final int OP_UPDATE_PASSWORD_INVALID = 108;
	private static final int OP_UPDATE_REALNAME_INVALID = 109;
	private static final int OP_UPDATE_EMAIL_LOCAL_PART_INVALID = 110;
	private static final int OP_UPDATE_EMAIL_HOST_PART_INVALID = 111;
	private static final int OP_UPDATE_PASSWORD_NOT_SAME = 112;
	private static final int OP_ACCOUNT_UNLOCKED_SUCCESS = 113;
	//private static final int OP_PASSWORD_RESET_SUCCESS = 114;
	private static final int OP_ACCOUNT_UNLOCKED_FAILED = 115;
	//private static final int OP_PASSWORD_RESET_FAILED = 116;
	private static final int OP_SET_GROUP_FAILED = 117;
	private static final int OP_SET_GROUP_SUCCESS = 118;
	private static final int OP_UPDATEPWD_AUTH_PASSWORD_INVALID = 119;
	private static final int OP_UPDATEPWD_NEW_PASSWORD_INVALID = 120;
	private static final int OP_UPDATEPWD_SUCCESS = 121;
	private static final int OP_UPDATEPWD_FAILED = 122;
	private static final int OP_UPDATEPWD_PASSWORD_NOT_SAME = 123;
	private static final int OP_UPDATEPWD = 124;
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException, IOException{
		return getHtml( requestDescriptor, null );
	}
	
	/**
	 * Get the appropriate HTML for the user managements pages. 
	 * @throws IOException 
	 */
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws NoSessionException, GeneralizedException, IOException{
		
		try{
		ApiUserManagement xUserManagement = new ApiUserManagement(Application.getApplication());
		ApiGroupManagement xGroupManagement = new ApiGroupManagement(Application.getApplication());
		
		if( actionDesc == null )
			actionDesc = performAction(requestDescriptor, xGroupManagement, xUserManagement);
		
		String userId = requestDescriptor.request.getParameter("UserID");
		
		
		// 1 -- Show the user add form
		if( actionDesc.result == ActionDescriptor.OP_ADD || actionDesc.result == OP_ADD_PASSWORD_NOT_SAME || actionDesc.result == OP_ADD_USERNAME_INVALID || actionDesc.result == OP_ADD_EMAIL_LOCAL_PART_INVALID
				|| actionDesc.result == OP_ADD_EMAIL_HOST_PART_INVALID || actionDesc.result == OP_ADD_PASSWORD_INVALID || actionDesc.result == OP_ADD_REALNAME_INVALID
				|| actionDesc.result == ActionDescriptor.OP_ADD_FAILED || actionDesc.result == OP_ADD_DUPLICATE_ACCOUNT){
			return getUserAdd(requestDescriptor, xUserManagement, actionDesc );
		}
		
		// 2 -- Show the user modification form
		else if( actionDesc.result == ActionDescriptor.OP_UPDATE || actionDesc.result == OP_UPDATE_USERNAME_INVALID || actionDesc.result == OP_UPDATE_EMAIL_LOCAL_PART_INVALID
				|| actionDesc.result == OP_UPDATE_EMAIL_HOST_PART_INVALID || actionDesc.result == OP_UPDATE_PASSWORD_INVALID
				|| actionDesc.result == OP_UPDATE_REALNAME_INVALID || actionDesc.result == OP_UPDATE_PASSWORD_NOT_SAME || actionDesc.result == ActionDescriptor.OP_UPDATE_FAILED ){
			return getUserModify(requestDescriptor, xUserManagement, actionDesc );
		}
		// 3 -- Update the users password
		else if( (userId != null && userId.length() != 0) && actionDesc.result == OP_UPDATEPWD_FAILED || actionDesc.result == OP_UPDATEPWD
				|| actionDesc.result == OP_UPDATEPWD_AUTH_PASSWORD_INVALID || actionDesc.result == OP_UPDATEPWD_PASSWORD_NOT_SAME || actionDesc.result == OP_UPDATEPWD_NEW_PASSWORD_INVALID ){
			return getUserPasswordUpdate(requestDescriptor, xUserManagement, actionDesc );
		}
		// 4 -- View a user
		else if( (userId != null && userId.length() != 0 && actionDesc.result != ActionDescriptor.OP_DELETE_SUCCESS)
				|| actionDesc.result == ActionDescriptor.OP_ENABLE_SUCCESS || actionDesc.result == ActionDescriptor.OP_DISABLE_SUCCESS || actionDesc.result == ActionDescriptor.OP_UPDATE_SUCCESS ){
			return getUserView(requestDescriptor, xUserManagement, actionDesc );
		}
		 
		
		// 5 -- Show the users list
		else
			return getUsersList(requestDescriptor, xUserManagement, actionDesc );
		
		}
		catch(NotFoundException e){
			return HtmlProblemDialog.getHtml(requestDescriptor, "User Not Found", e.getMessage(), HtmlProblemDialog.DIALOG_WARNING, "Console", "Return to List of Users" );
		}
		
	}
	
	/**
	 * Create a view-only page of the user's information.
	 * @throws NoSessionException 
	 * @throws GeneralizedException 
	 * @throws IOException 
	 * @throws NotFoundException 
	 */
	private static ContentDescriptor getUserView(WebConsoleConnectionDescriptor requestDescriptor, ApiUserManagement xUserManagement, ActionDescriptor actionDesc) throws GeneralizedException, NoSessionException, IOException, NotFoundException{
		
		Integer userId = null;
		
		try{
			userId = Integer.valueOf( requestDescriptor.request.getParameter("UserID") );
		}
		catch (NumberFormatException e){
			userId = null;
		}
		
		UserDescriptor userDesc = null;
		
		StringBuffer body = new StringBuffer( 1024 );
		
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		if( userId != null ){
			try {
				userDesc = xUserManagement.getUserDescriptor( requestDescriptor.sessionIdentifier, userId.intValue() );
			} catch (InsufficientPermissionException e) {
				body.append( Html.getWarningNote("You do not have permission to view this user") );
				requestDescriptor.response.sendRedirect("UserManagement");
				return new ContentDescriptor("User Management", "Redirecting...");
			}
			
			
			body.append( Html.getSectionHeader( "User Management", null ));
			body.append( "<form action=\"UserManagement\" method=\"post\"><table>" );
			body.append( "<tr class=\"Background1\"><td width=\"150\" class=\"Text_3\">User ID</td><td>").append( userDesc.getUserID() ).append( "</td></tr>");
			body.append( "<tr class=\"Background1\"><td class=\"Text_3\">Username</td><td>").append( StringEscapeUtils.escapeHtml( userDesc.getUserName() )).append( "</td></tr>");
			if( userDesc.getFullname() != null )
				body.append( "<tr class=\"Background1\"><td class=\"Text_3\">Full Name</td><td>") .append( StringEscapeUtils.escapeHtml( userDesc.getFullname() ) ) .append( "</td></tr>");
			else
				body.append( "<tr class=\"Background1\"><td class=\"Text_3\">Full Name</td><td>(Unspecified)</td></tr>");
			
			String emailAddress = null;
			if( userDesc.getEmailAddress() != null )
				emailAddress = userDesc.getEmailAddress().toString();
			
			if( emailAddress != null && emailAddress.length() != 0 )
				body.append("<tr class=\"Background1\"><td class=\"Text_3\">Email Address</td><td><a href=\"mailto:").append( StringEscapeUtils.escapeHtml( emailAddress ) ).append("\">").append(emailAddress).append( "</a></td></tr>");
			else
				body.append("<tr class=\"Background1\"><td class=\"Text_3\">Email Address</td><td>(Unspecified)</td></tr>");
			
			body.append("<tr class=\"Background1\"><td class=\"Text_3\">Account Type</td><td>");
			if( userDesc.isUnrestricted() )
				body.append("<span class=\"WarnText\">Unrestricted</span></td></tr>");
			else
				body.append("Normal (Restricted)</td></tr>");
			//body += "<tr class=\"Background1\"><td>User ID</td><td>" + requestDescriptor.request.getParameter("UserID") + "</td></tr>";
			body.append("<tr class=\"Background3\"><td colspan=\"99\" align=\"right\"><input class=\"button\" type=\"submit\" value=\"Edit User\"><input type=\"hidden\" name=\"UserID\" value=\"").append(userDesc.getUserID()).append("\"><input type=\"hidden\" name=\"Action\" value=\"Edit\"></td></tr></table></form>");
			
			
			body.append(createGroupMembershipTable( userDesc, userId.intValue(), requestDescriptor));
		}
		
		// 2 -- Get the navigation bar
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "User Management", "/UserManagement");
		navPath.addPathEntry( "View User", "/UserManagement?UserID=" + requestDescriptor.request.getParameter("UserID") );
		String navigationHtml = Html.getNavigationPath( navPath );
		
		// 3 -- Get the menu items
		Vector<MenuItem> menuItems = new Vector<MenuItem>();

		menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("View List", "/", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("User Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Users", "/UserManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New User", "/UserManagement?Action=Add", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Logged in Users", "/Sessions", MenuItem.LEVEL_TWO) );
		if( userDesc != null ){
			if( userDesc.getAccountStatus() == UserManagement.AccountStatus.DISABLED )
				menuItems.add( new MenuItem("Enable User", "/UserManagement?Action=Enable&UserID=" + userId, MenuItem.LEVEL_TWO) );
			else
				menuItems.add( new MenuItem("Disable User", "/UserManagement?Action=Disable&UserID=" + userId, MenuItem.LEVEL_TWO) );
		}
		menuItems.add( new MenuItem("Delete User", "/UserManagement?Action=Delete&UserID=" + userId, MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Manage Rights", "/UserRights?UserID=" + userId, MenuItem.LEVEL_TWO) );
		//menuItems.add( new MenuItem("Reset Password", "/UserManagement?Action=ResetPassword&UserID=" + userId, MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Update Password", "/UserManagement?Action=UpdatePassword&UserID=" + userId, MenuItem.LEVEL_TWO) );
		if( userDesc != null && userDesc.isBruteForceLocked() )
			menuItems.add( new MenuItem("Unlock Account", "/UserManagement?Action=UnlockAccount&Username=" + userDesc.getUserName(), MenuItem.LEVEL_TWO) );

		
		menuItems.add( new MenuItem("Group Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Groups", "/GroupManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New Group", "/GroupManagement?Action=Add", MenuItem.LEVEL_TWO) );
		
		String menuOutput = Html.getMenu( menuItems );
		
		// 4 -- Compile the result
		if( userId == null ){
			String pageOutput = Html.getMainContent(Html.getWarningNote("User ID is invalid"), menuOutput, navigationHtml);
			return new ContentDescriptor( "User Management", pageOutput );
		}
		
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		return new ContentDescriptor( "User Management", pageOutput );
	}
	
	/**
	 * Get the page to modify users.
	 * @param request
	 * @param response
	 * @param webConsoleServlet
	 * @param httpMethod
	 * @param xUserManagement
	 * @return
	 * @throws GeneralizedException 
	 * @throws NoSessionException 
	 * @throws ServletException
	 * @throws SQLException
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws IOException 
	 * @throws IOException
	 * @throws NotFoundException 
	 * @throws NoDatabaseConnectionException 
	 */
	private static ContentDescriptor getUserModify(WebConsoleConnectionDescriptor requestDescriptor, ApiUserManagement xUserManagement, ActionDescriptor actionDesc) throws NoSessionException, GeneralizedException, IOException, NotFoundException{
		
		Integer userId = null;
		
		try{
			userId = Integer.valueOf( requestDescriptor.request.getParameter("UserID") );
		}
		catch (NumberFormatException e){
			userId = null;
		}
		
		UserDescriptor userDesc = null;
		
		StringBuffer body = new StringBuffer();
		
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		if( userId != null ){
			try {
				userDesc = xUserManagement.getUserDescriptor( requestDescriptor.sessionIdentifier, userId.intValue() );
			} catch (InsufficientPermissionException e) {
				body.append(  Html.getWarningNote("You do not have permission to view this user"));
				requestDescriptor.response.sendRedirect("UserManagement");
				return new ContentDescriptor("User Management", "Redirecting...");
			}
			
			// 2 -- Get the relevant values 
			String username = userDesc.getUserName();
			String fullname = userDesc.getFullname();
			String emailAddress = null;
			
			if( userDesc.getEmailAddress() != null )
				emailAddress = userDesc.getEmailAddress().toString();
			else
				emailAddress = "";
			
			if( fullname == null )
				fullname = "";
			
			if( requestDescriptor.request.getParameter("Username") != null )
				username = StringEscapeUtils.escapeHtml( requestDescriptor.request.getParameter("Username") );
			
			if( requestDescriptor.request.getParameter("Fullname") != null )
				fullname = StringEscapeUtils.escapeHtml( requestDescriptor.request.getParameter("Fullname") );
			
			if( requestDescriptor.request.getParameter("EmailAddress") != null )
				emailAddress = StringEscapeUtils.escapeHtml( requestDescriptor.request.getParameter("EmailAddress") );
			
			boolean isUnrestricted;
			
			if( requestDescriptor.request.getParameter("Unrestricted") != null )
				isUnrestricted = true;
			else
				isUnrestricted = userDesc.isUnrestricted();
			
			// 3 -- Output the update form
			body.append( Html.getSectionHeader( "User Management", null ) );//(String)userDesc.get("Username")
			
			body.append( "<table class=\"DataTable\"><form action=\"UserManagement\" onsubmit=\"showHourglass()\" method=\"post\">");
			if( actionDesc.result == OP_UPDATE_USERNAME_INVALID )
				body.append( "<tr class=\"ValidationFailed\">");
			else
				body.append( "<tr>");
			
			body.append( "<td class=\"TitleText\">Username</td><td><input class=\"textInput\" size=\"32\" type=\"text\" name=\"Username\" value=\"").append( username).append( "\"></td></tr>");
			
			if( actionDesc.result == OP_UPDATE_REALNAME_INVALID )
				body.append( "<tr class=\"ValidationFailed\">" );
			else
				body.append( "<tr>");
			
			body.append( "<td class=\"TitleText\">Full Name</td><td><input class=\"textInput\" size=\"32\" type=\"text\" name=\"Fullname\" value=\"").append( fullname ).append( "\"></td></tr>" );
			
			if( actionDesc.result == OP_UPDATE_EMAIL_HOST_PART_INVALID || actionDesc.result == OP_UPDATE_EMAIL_LOCAL_PART_INVALID )
				body.append("<tr class=\"ValidationFailed\"><td class=\"TitleText\">Email Address</td><td><input class=\"textInput\" size=\"32\" type=\"text\" name=\"EmailAddress\" value=\"").append( emailAddress ).append("\"></td></tr>");
			else
				body.append("<tr><td class=\"TitleText\">Email Address</td><td><input class=\"textInput\" size=\"32\" type=\"text\" name=\"EmailAddress\" value=\"").append( emailAddress ).append("\"></td></tr>");
			
			body.append("<tr><td class=\"TitleText\">Account Type</td><td><input type=\"checkbox\" name=\"Unrestricted\"");
			
			if( isUnrestricted)
				body.append(" checked>Unrestricted</td></tr>");
			else
				body.append(">Unrestricted</td></tr>");
			
			body.append("<tr class=\"lastRow\"><td colspan=\"2\" align=\"right\"><input type=\"hidden\" name=\"Action\" value=\"Edit\"><input class=\"button\" type=\"submit\" value=\"Apply\"><input type=\"hidden\" name=\"UserID\" value=\"").append(userDesc.getUserID()).append("\"></td></tr></form></table>");
		}
		
		// 2 -- Get the navigation bar
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "User Management", "/UserManagement");
		navPath.addPathEntry( "Modify User", "/UserManagement?Action=Edit&UserID=" + requestDescriptor.request.getParameter("UserID") );
		String navigationHtml = Html.getNavigationPath( navPath );
		
		// 3 -- Get the menu items
		Vector<MenuItem> menuItems = new Vector<MenuItem>();

		menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("View List", "/", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("User Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Users", "/UserManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New User", "/UserManagement?Action=Add", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Logged in Users", "/Sessions", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Delete User", "/UserManagement?Action=Delete&UserID=" + userId, MenuItem.LEVEL_TWO) );
		
		if(userDesc != null){
			if( userDesc.getAccountStatus() == UserManagement.AccountStatus.DISABLED )
			menuItems.add( new MenuItem("Enable User", "/UserManagement?Action=Enable&UserID=" + userId, MenuItem.LEVEL_TWO) );
		else
			menuItems.add( new MenuItem("Disable User", "/UserManagement?Action=Disable&UserID=" + userId, MenuItem.LEVEL_TWO) );
		}
		
		menuItems.add( new MenuItem("Manage Rights", "/UserRight?&UserID=" + userId, MenuItem.LEVEL_TWO) );
		//menuItems.add( new MenuItem("Reset Password", "/UserManagement?Action=ResetPassword&UserID=" + userId, MenuItem.LEVEL_TWO) );
		if( userDesc != null && userDesc.isBruteForceLocked() )
			menuItems.add( new MenuItem("Unlock Account", "/UserManagement?Action=UnlockAccount&Username=" + userDesc.getUserName(), MenuItem.LEVEL_TWO) );

		menuItems.add( new MenuItem("Group Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Groups", "/GroupManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New Group", "/GroupManagement?Action=Add", MenuItem.LEVEL_TWO) );
		
		String menuOutput = Html.getMenu( menuItems );
		
		// 4 -- Compile the result
		if( userId == null ){
			String pageOutput = Html.getMainContent(Html.getWarningNote("User ID is invalid"), menuOutput, navigationHtml);
			return new ContentDescriptor( "User Management", pageOutput );
		}
		
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		return new ContentDescriptor( "User Management", pageOutput );
	}
	
	/**
	 * Get the content for adding a new user.
	 * @param request
	 * @param response
	 * @param requestDescriptor
	 * @param httpMethod
	 * @param xUserManagement
	 * @param actionDesc
	 * @return
	 * @throws ServletException
	 * @throws SQLException
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws IOException
	 */
	private static ContentDescriptor getUserAdd(WebConsoleConnectionDescriptor requestDescriptor, ApiUserManagement xUserManagement, ActionDescriptor actionDesc){
		
		StringBuffer body = new StringBuffer();
		String username = "";
		String fullname = "";
		String emailAddress = "";
		
		if( requestDescriptor.request.getParameter("Username") != null )
			username = StringEscapeUtils.escapeHtml( requestDescriptor.request.getParameter("Username") );
		
		if( requestDescriptor.request.getParameter("Fullname") != null )
			fullname = StringEscapeUtils.escapeHtml( requestDescriptor.request.getParameter("Fullname") );
		
		if( requestDescriptor.request.getParameter("EmailAddress") != null )
			emailAddress = StringEscapeUtils.escapeHtml( requestDescriptor.request.getParameter("EmailAddress") );
		
		body.append(Html.getSectionHeader( "User Management", "Create New User" ) );
		
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		body.append("<table class=\"DataTable\"><form onsubmit=\"showHourglass()\" method=\"post\">");
		
		if( actionDesc.result == OP_ADD_USERNAME_INVALID )
			body.append("<tr class=\"ValidationFailed\">");
		else
			body.append("<tr>");
		
		body.append("<td class=\"Text_3\">Username</td><td><input class=\"textInput\" size=\"32\" type=\"text\" name=\"Username\" value=\"").append(username).append("\"></td></tr>");
		
		if( actionDesc.result == OP_ADD_REALNAME_INVALID )
			body.append("<tr class=\"ValidationFailed\">");
		else
			body.append("<tr>");
		
		body.append("<td class=\"TitleText\">Full Name</td><td><input class=\"textInput\" size=\"32\" type=\"text\" name=\"Fullname\" value=\"").append(fullname).append("\"></td></tr>");
		
		if( actionDesc.result == OP_ADD_EMAIL_HOST_PART_INVALID || actionDesc.result == OP_ADD_EMAIL_LOCAL_PART_INVALID )
			body.append("<tr class=\"ValidationFailed\"><td class=\"TitleText\">Email Address</td><td><input class=\"textInput\" size=\"32\" type=\"text\" name=\"EmailAddress\" value=\"").append( emailAddress ).append("\"></td></tr>");
		else
			body.append("<tr><td class=\"TitleText\">Email Address</td><td><input class=\"textInput\" size=\"32\" type=\"text\" name=\"EmailAddress\" value=\"").append( emailAddress ).append("\"></td></tr>");
		
		if( actionDesc.result == OP_ADD_PASSWORD_NOT_SAME || actionDesc.result == OP_ADD_PASSWORD_INVALID  ){
			body.append("<tr class=\"ValidationFailed\"><td class=\"TitleText\">Password</td><td><input class=\"textInput\" size=\"32\" type=\"password\" name=\"Password\"></td></tr>");
			body.append("<tr class=\"ValidationFailed\"><td class=\"TitleText\">Password (Confirm)</td><td><input class=\"textInput\" size=\"32\" type=\"password\" name=\"PasswordConfirm\"></td></tr>");
		}
		else{
			body.append("<tr><td class=\"TitleText\">Password</td><td><input class=\"textInput\" size=\"32\" type=\"password\" name=\"Password\"></td></tr>");
			body.append("<tr><td class=\"TitleText\">Password (Confirm)</td><td><input class=\"textInput\" size=\"32\" type=\"password\" name=\"PasswordConfirm\"></td></tr>");
		}
		body.append( "<tr><td class=\"TitleText\">Account Type</td><td><input type=\"checkbox\" name=\"Unrestricted\"" );
		if( requestDescriptor.request.getParameter("Unrestricted") != null )
		 body.append( " checked>Unrestricted</td></tr>");
		else
		 body.append( ">Unrestricted</td></tr>");
		
		body.append("<tr class=\"lastRow\"><td colspan=\"2\" align=\"right\"><input class=\"button\" type=\"submit\" value=\"Apply\"></td></tr></form></table>");
		
		
		// 2 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "User Management", "/UserManagement");
		navPath.addPathEntry( "Create New User", "/UserManagement?Action=Add" );
		String navigationHtml = Html.getNavigationPath( navPath );
		
		// 3 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();

		menuItems.add( new MenuItem("User Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Users", "/UserManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Logged in Users", "/Sessions", MenuItem.LEVEL_TWO) );

		menuItems.add( new MenuItem("Group Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Groups", "/GroupManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New Group", "/GroupManagement?Action=Add", MenuItem.LEVEL_TWO) );
		
		String menuOutput = Html.getMenu( menuItems );
		
		// 4 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor( "User Management", pageOutput );
	}
	

	/**
	 * Get the form to update a user's password.
	 * @param request
	 * @param response
	 * @param requestDescriptor
	 * @param httpMethod
	 * @param xUserManagement
	 * @param actionDesc
	 * @return
	 * @throws GeneralizedException 
	 * @throws NoSessionException 
	 * @throws ServletException
	 * @throws SQLException
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws NotFoundException 
	 * @throws IOException
	 */
	private static ContentDescriptor getUserPasswordUpdate( WebConsoleConnectionDescriptor requestDescriptor, ApiUserManagement xUserManagement, ActionDescriptor actionDesc) throws NoSessionException, GeneralizedException, NotFoundException{
		
		int userId = -1;
		
		// 1 -- Get the form values
		if( requestDescriptor.request.getParameter("UserID") != null ){
			try{
				userId = Integer.parseInt( requestDescriptor.request.getParameter("UserID") );
			}
			catch( NumberFormatException e ){
				userId = -2;
			}
		}
		
		// 2 -- Output the result
		StringBuffer body = new StringBuffer();
		UserDescriptor userDesc = null;
		if( userId < 0 ){
			body.append( Html.getDialog("A valid user identifier was not provided. <p><a href=\"UserManagement\">[Return to User List]</a>", "Illegal Argument", "/32_Warning", false) );
		}
		else{
			try {
				userDesc = xUserManagement.getUserDescriptor( requestDescriptor.sessionIdentifier, userId );
			} catch (InsufficientPermissionException e) {
				//String body = GenericHtmlGenerator.getWarningNote("You do not have permission to view this user");
				//response.sendRedirect("UserManagement");
				return new ContentDescriptor("User Management", "Redirecting...");
			}

			
			body.append( Html.getSectionHeader( "User Management", "Change Password" ) );

			body.append(Html.renderMessages(requestDescriptor.userId));

			body.append("<table class=\"DataTable\"><form action=\"UserManagement\" method=\"post\" onsubmit=\"showHourglass('Updating...')\" method=\"get\">" );

			//	  2.1 -- Output the username and user ID
			body.append( "<tr><td class=\"TitleText\">User</td><td>").append( userDesc.getUserName() ).append( " (" ).append( userDesc.getUserID() ).append( ")</td></tr>" );

			//	  2.2 -- Existing password field
			if( actionDesc.result == OP_UPDATEPWD_AUTH_PASSWORD_INVALID )
				body.append("<tr class=\"ValidationFailed\">");
			else
				body.append( "<tr class=\"TitleText\">" );

			body.append( "<td class=\"TitleText\">Your Current Password</td><td><input class=\"textInput\" style=\"width: 250px;\" type=\"password\" name=\"CurrentPassword\"></td></tr>" );

			//	 2.3 -- New password field
			if( actionDesc.result == OP_UPDATEPWD_NEW_PASSWORD_INVALID || actionDesc.result == OP_UPDATEPWD_PASSWORD_NOT_SAME)
				body.append( "<tr class=\"ValidationFailed\">" );
			else
				body.append( "<tr class=\"TitleText\">" );

			body.append( "<td class=\"TitleText\">New Password</td><td><input class=\"textInput\" style=\"width: 250px;\" type=\"password\" name=\"NewPassword\"></td></tr>" );

			//	 2.4 -- New password field (confirmation)
			if( actionDesc.result == OP_UPDATEPWD_NEW_PASSWORD_INVALID || actionDesc.result == OP_UPDATEPWD_PASSWORD_NOT_SAME)
				body.append( "<tr class=\"ValidationFailed\">" );
			else
				body.append( "<tr class=\"TitleText\">" );

			body.append( "<td class=\"TitleText\">Confirm New Password</td><td><input class=\"textInput\" style=\"width: 250px;\" type=\"password\" name=\"NewPasswordConfirm\"></td></tr>" );

			body.append( "<tr class=\"lastRow\"><td colspan=\"2\" align=\"right\"><input class=\"button\" type=\"submit\" name=\"Submit\" value=\"Apply\"><input type=\"hidden\" name=\"Action\" value=\"UpdatePassword\"><input type=\"hidden\" name=\"UserID\" value=\"").append( userId ).append( "\"></td></tr></form></table>" );
		}
		
		// 3 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "User Management", "/UserManagement");
		navPath.addPathEntry( "Update Password", "/UserManagement?Action=UpdatePassword&UserID=" + userId );
		String navigationHtml = Html.getNavigationPath( navPath );
		
		// 3 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();

		menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Add Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );

		menuItems.add( new MenuItem("User Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Users", "/UserManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New User", "/UserManagement?Action=Add", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Logged in Users", "/Sessions", MenuItem.LEVEL_TWO) );
		//menuItems.add( new MenuItem("Reset Password", "UserManagement?Action=ResetPassword", MenuItem.LEVEL_TWO) );
		if( userDesc != null && userDesc.getAccountStatus() == UserManagement.AccountStatus.DISABLED )
			menuItems.add( new MenuItem("Enable User", "/UserManagement?Action=Enable&UserID=" + userId, MenuItem.LEVEL_TWO) );
		else
			menuItems.add( new MenuItem("Disable User", "/UserManagement?Action=Disable&UserID=" + userId, MenuItem.LEVEL_TWO) );

		menuItems.add( new MenuItem("Group Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Groups", "/GroupManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New Group", "/GroupManagement?Action=Add", MenuItem.LEVEL_TWO) );
		
		String menuOutput = Html.getMenu( menuItems );
		
		// 4 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor( "User Management", pageOutput );
	}
	
	/**
	 * Get a list of all of the current users.
	 * @param request
	 * @param response
	 * @param requestDescriptor
	 * @param httpMethod
	 * @param xUserManagement
	 * @return
	 * @throws GeneralizedException 
	 * @throws NoSessionException 
	 * @throws ServletException
	 * @throws SQLException
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 */
	private static ContentDescriptor getUsersList(WebConsoleConnectionDescriptor requestDescriptor, ApiUserManagement xUserManagement, ActionDescriptor actionDesc  ) throws NoSessionException, GeneralizedException{
		
		StringBuffer body = new StringBuffer();
		
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		UserDescriptor[] userDescriptors = null;
		try {
			userDescriptors = xUserManagement.getUserDescriptors( requestDescriptor.sessionIdentifier );
			body.append( Html.getSectionHeader( "User Management", null ) );
		} catch (InsufficientPermissionException e) {
			body.append("<p>");
			body.append(Html.getWarningDialog("Insufficient Permission", "You do not have permission to enumerate users", "Console", "Return to Main Dashboard"));
		}
		
		if( userDescriptors != null ){
			body.append("<table><tr class=\"Background0\"><td class=\"Text_3\">User ID</td><td width=\"200\" class=\"Text_3\">Username</td><td width=\"200\" class=\"Text_3\">Full Name</td><td class=\"Text_3\">Status</td><td class=\"Text_3\">Unrestricted</td><td width=\"200\" class=\"Text_3\">Email Address</td></tr>" );
			
			for( int c = 0; c < userDescriptors.length; c++ ){
				body.append( createRow( userDescriptors[c] ) );
			}
			
			body.append( "</table>" );
		}
		
		
		// 2 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "User Management", "/UserManagement");
		String navigationHtml = Html.getNavigationPath( navPath );
		
		// 3 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();

		menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Add Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );

		menuItems.add( new MenuItem("User Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Add New User", "/UserManagement?Action=Add", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Logged in Users", "/Sessions", MenuItem.LEVEL_TWO) );

		menuItems.add( new MenuItem("Group Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Groups", "/GroupManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New Group", "/GroupManagement?Action=Add", MenuItem.LEVEL_TWO) );
		
		String menuOutput = Html.getMenu( menuItems );
		
		// 4 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor( "User Management", pageOutput );
	}
	
	/**
	 * Create a row with the information from the given user.
	 * @param userDescriptor
	 * @return
	 */
	private static String createRow( UserDescriptor userDescriptor ){
		// 1 -- Output the user ID and name
		StringBuffer output = new StringBuffer();
		output.append( "<tr class=\"Background1\"><td>").append( userDescriptor.getUserID() )
			.append(	"</td><td><a href=\"UserManagement?UserID=").append( userDescriptor.getUserID()).append("\">");
		
		output.append( StringEscapeUtils.escapeHtml( userDescriptor.getUserName() ));
		
		// 2 -- Output the icon associated with the account type
		if( userDescriptor.isUnrestricted() && userDescriptor.getAccountStatus() != UserManagement.AccountStatus.DISABLED )
			output.append( "&nbsp;&nbsp;<img src=\"/16_Admin\" alt=\"Unrestricted\"></a></td>" );
		else if( userDescriptor.isUnrestricted() && userDescriptor.getAccountStatus() == UserManagement.AccountStatus.DISABLED )
			output.append( "&nbsp;&nbsp;<img src=\"/16_AdminDisabled\" alt=\"Unrestricted\"></a></td>" );
		else if( userDescriptor.getAccountStatus() == UserManagement.AccountStatus.DISABLED )
			output.append( "&nbsp;&nbsp;<img src=\"/16_UserDisabled\" alt=\"Unrestricted\"></a></td>" );
		else
			output.append( "&nbsp;&nbsp;<img src=\"/16_User\" alt=\"Restricted\"></a></td>" );
		
		if( userDescriptor.getFullname() != null )
			output.append( "<td><a href=\"UserManagement?UserID=").append( userDescriptor.getUserID()).append( "\">").append( StringEscapeUtils.escapeHtml( userDescriptor.getFullname() ) ).append( "</td>" );
		else
			output.append( "<td>(Unspecified)</td>" );
		
		// 3 -- Output the account status
		if( userDescriptor.getAccountStatus() == UserManagement.AccountStatus.ADMINISTRATIVELY_LOCKED )
			output.append( "<td>Administratively locked</td>");
		else if( userDescriptor.isBruteForceLocked() || userDescriptor.getAccountStatus() == UserManagement.AccountStatus.BRUTE_FORCE_LOCKED )
			output.append( "<td><span class=\"WarnText\">Brute force locked</span></td>" );
		else if( userDescriptor.getAccountStatus() == UserManagement.AccountStatus.DISABLED )
			output.append( "<td>Disabled</td>" );
		else
			output.append( "<td>Active</td>" );
		
		// 4 -- Output if the account is restricted
		if( userDescriptor.isUnrestricted() )
			output.append( "<td>Unrestricted</td>" );
		else
			output.append( "<td>Restricted</td>" );
		
		//
		if( userDescriptor.getEmailAddress() == null )
			output.append( "<td class=\"Background1\">(None Specified)</td>" );
		else
			output.append( "<td class=\"Background1\"><a href=\"mailto:").append( StringEscapeUtils.escapeHtml( userDescriptor.getEmailAddress().toString() )).append( "\">").append( StringEscapeUtils.escapeHtml( userDescriptor.getEmailAddress().toString() ) ).append( "</a></td>");
		
		/*output += "</td><td class=\"Background1\">" + userDescriptor.getEmailAddress() + 
		 + "</td><td class=\"Background1\">" + userDescriptor.getUserName() + 
		 + "</td><td class=\"Background1\">" + userDescriptor.getUserName() + 
		 "</td></tr>";*/
		output.append( "</tr>" );
		return output.toString();
	}
	
	private static String createGroupMembershipTable( UserDescriptor userDescriptor, int userId, WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException{
		StringBuffer output = new StringBuffer();
		output.append("<br><span class=\"Text_1\">Group Membership</span>");
		
		
		
		
		ApiGroupManagement groupManager = new ApiGroupManagement( Application.getApplication() );
		
		GroupMembershipDescriptor groupMembership;
		try {
			groupMembership = groupManager.getGroupMembership( requestDescriptor.sessionIdentifier, userId );
		} catch (InsufficientPermissionException e) {
			return "<tr><td colspan=\"99\">" + Html.getWarningNote( "You do not have permission to enumerate who is in what group") + "</td></tr></table>";
		}
		
		String includedGroups = null;
		
		if( groupMembership.getSize() == 0){
			output.append( Html.getDialog("No groups exist yet. Create a group first, then add the user to the specified group or groups. <p><a href=\"GroupManagement?Action=New\">[Create Group Now]</a>", "No Groups Exist", "/32_Information", false) );
		}
		
		else{
			output.append("<form action=\"UserManagement\"><input type=\"hidden\" name=\"UserID\" value=\"").append( userId ).append("\"><input type=\"hidden\" name=\"Action\" value=\"SetGroup\">");
			
			output.append("<table><tr class=\"Background0\"><td class=\"Text_3\">Group Name</td><td class=\"Text_3\">GroupID</td><td class=\"Text_3\">Group Description</td></tr>" );
			
			// Get the rows
			for( int c = 0; c < groupMembership.getSize(); c++ ){
				GroupDescriptor groupDesc = groupMembership.getGroupDescriptor( c );
				boolean isMember = groupMembership.isMemberOfGroup( c );
				
				if( includedGroups == null )
					includedGroups = String.valueOf( groupDesc.getGroupId() );
				else
					includedGroups += "," + groupDesc.getGroupId();
				
				output.append("<tr class=\"Background1\"><td>");
	
				
				// Check the box if membership is true
				if( isMember )
					output.append("<input id=\"").append( c ).append( "\" type=\"checkbox\" name=\"").append( groupDesc.getGroupId() ).append( "\" checked>" );
				else
					output.append("<input id=\"").append( c ).append( "\" type=\"checkbox\" name=\"").append( groupDesc.getGroupId() ).append("\">");
				
				// Add the group name
				output.append( "<label for=\"").append( c ).append("\">").append(groupDesc.getGroupName() ).append("</label>");
				
				
				// Add the group icon
				GroupManagement.State groupStatus = groupDesc.getGroupState(); 
				if( groupStatus == GroupManagement.State.ACTIVE )
					output.append("&nbsp;&nbsp;<img alt=\"Enabled\" src=\"/16_Group\"></td>");
				else
					output.append("&nbsp;&nbsp;<img alt=\"Disabled\" src=\"/16_GroupDisabled\"></td>");
				
				// Add the group ID
				output.append("<td><a href=\"GroupManagement?GroupID=").append(groupDesc.getGroupId()).append("\">").append(groupDesc.getGroupId()).append(" [View]</a>").append("</td>");
				
				// Add the group description
				output.append( "<td>").append( Html.shortenString( groupDesc.getDescription(), 32 )).append( "</td>");
				
				// Close the tags
				output.append("</td></tr>");
			}
		
			// Make sure to include an entry so that the response can track which groups were originally included and should be toggled off
			output.append("<tr class=\"Background3\"><td class=\"alignRight\" colspan=\"3\"><input type=\"hidden\" name=\"IncludedGroups\" value=\"").append( includedGroups).append( "\"><input class=\"button\" type=\"submit\" value=\"Apply Changes\"></td></tr>");
			
			output.append("</form></table>");
			
		}
		
		return output.toString();
	}
	
	/**
	 * Perform the action that is requested (via the parameters)
	 * @param request
	 * @param response
	 * @param requestDescriptor
	 * @param httpMethod
	 * @param xUserManager
	 * @return
	 * @throws ServletException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 */
	private static ActionDescriptor performAction(WebConsoleConnectionDescriptor requestDescriptor, ApiGroupManagement groupManager, ApiUserManagement xUserManager) throws GeneralizedException, NoSessionException{
		String action = requestDescriptor.request.getParameter("Action");
		
		if( action == null )
			return new ActionDescriptor( ActionDescriptor.OP_NO_OPERATION);
		
		int userID;
		
		if( requestDescriptor.request.getParameter("UserID") != null ){
			try{
				userID = Integer.parseInt( requestDescriptor.request.getParameter("UserID") );
			}
			catch ( NumberFormatException e){
				userID = -1;
			}
		}
		else
			userID = -1;
		
		// 2 -- Delete account
		if( action.matches("Delete") && userID > -1){
			try{
				if( xUserManager.deleteAccount( requestDescriptor.sessionIdentifier, userID ) ){
					Html.addMessage(MessageType.INFORMATIONAL, "User account deleted", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_DELETE_SUCCESS );
				}
				else{
					Html.addMessage(MessageType.WARNING, "User deletion failed", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_DELETE_FAILED );
				}
			}
			catch( InsufficientPermissionException e ){
				Html.addMessage(MessageType.WARNING, "You don't have permission to delete the the account", requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_DELETE_FAILED );
			}
			catch (DisallowedOperationException e) {
				Html.addMessage(MessageType.WARNING, e.getMessage(), requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_DELETE_FAILED );
			}
		}
		
		// 3 -- Disable account
		else if( action.matches("Disable") && userID > -1){
			
			try{
				if( xUserManager.disableAccount( requestDescriptor.sessionIdentifier, userID ) ){
					Html.addMessage(MessageType.INFORMATIONAL, "User account disabled", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_DISABLE_SUCCESS );
				}
				else{
					Html.addMessage(MessageType.WARNING, "User account could not be disabled", requestDescriptor.userId.longValue() );
					return new ActionDescriptor( ActionDescriptor.OP_DISABLE_FAILED );
				}
			}
			catch( InsufficientPermissionException e ){
				Html.addMessage(MessageType.WARNING, "You don't have permission to disable the the account", requestDescriptor.userId.longValue() );
				return new ActionDescriptor( ActionDescriptor.OP_DISABLE_FAILED );
			}catch (DisallowedOperationException e) {
				Html.addMessage(MessageType.WARNING, e.getMessage(), requestDescriptor.userId.longValue() );
				return new ActionDescriptor( ActionDescriptor.OP_DISABLE_FAILED );
			}
		}
		
		// 4 -- Enable account
		else if( action.matches("Enable") && userID > -1){

			try{
				if( xUserManager.enableAccount( requestDescriptor.sessionIdentifier, userID ) ){
					Html.addMessage(MessageType.INFORMATIONAL, "User account re-enabled", requestDescriptor.userId.longValue() );
					return new ActionDescriptor( ActionDescriptor.OP_ENABLE_SUCCESS );
				}
				else{
					Html.addMessage(MessageType.WARNING,  "User account could not be re-enabled", requestDescriptor.userId.longValue() );
					return new ActionDescriptor( ActionDescriptor.OP_ENABLE_FAILED );
				}
			}
			catch( InsufficientPermissionException e ){
				Html.addMessage(MessageType.WARNING, "You don't have permission to enable the the account", requestDescriptor.userId.longValue() );
				return new ActionDescriptor( ActionDescriptor.OP_ENABLE_FAILED );
			}
		}
		
		// 5 -- Add account 
		else if( action.matches("Add") ){
			String userName = requestDescriptor.request.getParameter("Username");
			String realName = requestDescriptor.request.getParameter("Fullname");
			String password = requestDescriptor.request.getParameter("Password");
			String passwordConfirm = requestDescriptor.request.getParameter("PasswordConfirm");
			String emailAddress = requestDescriptor.request.getParameter("EmailAddress");
			boolean unrestrictedAccount = false;
			
			if( requestDescriptor.request.getParameter("Unrestricted") != null )
				unrestrictedAccount = true;
			
			if( userName != null && realName != null && password != null && emailAddress != null && passwordConfirm != null ){
				if( !passwordConfirm.matches( password )){
					Html.addMessage(MessageType.WARNING, "Password entries don't match", requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_ADD_PASSWORD_NOT_SAME );
				}
				
				try {
					// Must deal with errors due to multiple account of same name
					if( emailAddress.length() == 0 )
						emailAddress = null;
					double userId = xUserManager.addAccount( requestDescriptor.sessionIdentifier, userName, realName, password, emailAddress, unrestrictedAccount );
					
					if( userId == -1 ){
						Html.addMessage(MessageType.WARNING, "User account addition failed", requestDescriptor.userId.longValue());
						return new ActionDescriptor( ActionDescriptor.OP_ADD_FAILED );
					}
					else{
						Html.addMessage(MessageType.INFORMATIONAL, "User account successfully added", requestDescriptor.userId.longValue());
						return new ActionDescriptor( ActionDescriptor.OP_ADD_SUCCESS );
					}
					
				} catch (UnknownHostException e){
					Html.addMessage(MessageType.WARNING, "Email address is invalid (host portion)", requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_ADD_EMAIL_HOST_PART_INVALID );
				} catch (InsufficientPermissionException e){
					Html.addMessage(MessageType.WARNING, "You do not have permission to add a user", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_ADD_FAILED );
				} catch (InputValidationException e){
					if( e.getFieldDescription().matches("username")){
						Html.addMessage(MessageType.WARNING, "Username is invalid", requestDescriptor.userId.longValue());
						return new ActionDescriptor( OP_ADD_USERNAME_INVALID );
					}
					else{// if( e.getFieldDescription().matches("fullname"))
						Html.addMessage(MessageType.WARNING, "Fullname is invalid", requestDescriptor.userId.longValue());
						return new ActionDescriptor( OP_ADD_REALNAME_INVALID );
					}
				} catch (InvalidLocalPartException e) {
					Html.addMessage(MessageType.WARNING, "Email address is invalid (local portion)", requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_ADD_EMAIL_LOCAL_PART_INVALID );
				}
				catch( DisallowedOperationException e){
					Html.addMessage(MessageType.WARNING, e.getMessage(), requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_ADD_FAILED ); 
				}
			}
			else
				return new ActionDescriptor( ActionDescriptor.OP_ADD );
		}
		
		// 6 -- Edit account
		else if( action.matches("Edit") ){
			String userIdString = requestDescriptor.request.getParameter("UserID");
			long userId = -1;
			
			if( userIdString != null ){
				try{
					userId = Long.parseLong(userIdString);
				}
				catch( NumberFormatException e){
					userId = -2;
				}
			}
			
			String userName = requestDescriptor.request.getParameter("Username");
			String realName = requestDescriptor.request.getParameter("Fullname");
			String emailAddress = requestDescriptor.request.getParameter("EmailAddress");
			boolean unrestrictedAccount = false;
			
			if( requestDescriptor.request.getParameter("Unrestricted") != null )
				unrestrictedAccount = true;
			
			if( userName != null && realName != null && emailAddress != null && userId >= 0 ){
				
				try {
					if( emailAddress.trim().length() == 0 )
						emailAddress = null;
					
					boolean updatedSuccessfully = xUserManager.updateAccountEx( requestDescriptor.sessionIdentifier, (long)userId, userName, realName, emailAddress, unrestrictedAccount );
					
					if( updatedSuccessfully == false ){
						Html.addMessage(MessageType.WARNING, "User account update failed", requestDescriptor.userId.longValue());
						return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED);
					}
					else{
						Html.addMessage(MessageType.INFORMATIONAL, "User account successfully changed", requestDescriptor.userId.longValue());
						return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
					}
				} catch (UnknownHostException e){
					Html.addMessage(MessageType.WARNING, "Email address is invalid (host portion)", requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_UPDATE_EMAIL_HOST_PART_INVALID );
				} catch (InsufficientPermissionException e){
					Html.addMessage(MessageType.WARNING, "You do not have permissionto add a user", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED );
				} catch (InputValidationException e){
					if( e.getFieldDescription().matches("username")){
						Html.addMessage(MessageType.WARNING, "Username is invalid", requestDescriptor.userId.longValue());
						return new ActionDescriptor( OP_UPDATE_USERNAME_INVALID );
					}
					else{// if( e.getFieldDescription().matches("fullname"))
						Html.addMessage(MessageType.WARNING, "Fullname is invalid", requestDescriptor.userId.longValue());
						return new ActionDescriptor( OP_UPDATE_REALNAME_INVALID );
					}
				} catch (InvalidLocalPartException e) {
					Html.addMessage(MessageType.WARNING, "Email address is invalid (local portion)", requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_UPDATE_EMAIL_LOCAL_PART_INVALID );
				} catch( DisallowedOperationException e){
					Html.addMessage(MessageType.WARNING, e.getMessage(), requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED ); 
				}
			}
			else
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE );
		}
		
		// 7 -- Reset password
		/*else if( action.matches("ResetPassword") ){
			String userIdString = requestDescriptor.request.getParameter("UserID");
			long userId = -1;
			
			if( userIdString != null ){
				try{
					userId = Long.parseLong(userIdString);
				}
				catch( NumberFormatException e){
					userId = -2;
				}
			}
			String newPassword;
			try {
				newPassword = xUserManager.changePasswordToRandom( requestDescriptor.sessionIdentifier, userId, 16);
			} catch (InsufficientPermissionException e) {
				return new ActionDescriptor( OP_PASSWORD_RESET_FAILED, GenericHtmlGenerator.getWarningNote( "You don't have permission to reset the password") );
			} catch (InputValidationException e) {
				return new ActionDescriptor( OP_PASSWORD_RESET_FAILED, GenericHtmlGenerator.getWarningNote( "The password was not generated successfully") );
			}
			
			if( newPassword != null )
				return new ActionDescriptor( OP_PASSWORD_RESET_SUCCESS, GenericHtmlGenerator.getInfoNote( "The password was reset to \"" + newPassword + "\"") );
			else
				return new ActionDescriptor( OP_PASSWORD_RESET_FAILED, GenericHtmlGenerator.getWarningNote( "The password reset failed") );
		}*/
		
		// 8 -- Clear authentication failed count
		else if( action.matches("UnlockAccount") ){
			String userName = requestDescriptor.request.getParameter("Username");
			if( userName != null ){
				try {
					xUserManager.clearAuthFailedCount( requestDescriptor.sessionIdentifier, userName );
				} catch (InputValidationException e) {
					Html.addMessage(MessageType.WARNING, "The username provided is not valid", requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_ACCOUNT_UNLOCKED_FAILED );
				} catch (InsufficientPermissionException e) {
					Html.addMessage(MessageType.WARNING, "You do not have sufficient permissions to unlock accounts", requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_ACCOUNT_UNLOCKED_FAILED );
				}
				
				Html.addMessage(MessageType.INFORMATIONAL, "Authentication attempt count successfully cleared", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_ACCOUNT_UNLOCKED_SUCCESS );
			}
			else{
				Html.addMessage(MessageType.WARNING, "No username provided", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_ACCOUNT_UNLOCKED_FAILED);
			}
		}
		
		// 9 -- Associate/De-associate group
		else if( action.matches("SetGroup") && userID > -1 && requestDescriptor.request.getParameter("IncludedGroups") != null ){
			boolean insufficientPermissionNoted = false;
			String groupString = requestDescriptor.request.getParameter("IncludedGroups");
			String[] includedGroups = StringUtils.split(groupString, ',');
			
			// Detemine the desired operation for each group included
			for( int c = 0; c < includedGroups.length; c++ ){
				int groupId = -1;
				
				try{
					groupId = Integer.parseInt( includedGroups[c] );
				}catch(NumberFormatException e){
					//Do nothing, the number is not a valid format and will be skipped
				}
				
				// Determine if the associated check was marked (indicating the command to add membership) or unmarked (no membership)
				try{
					if( groupId >= 0 ){
						if( requestDescriptor.request.getParameter( includedGroups[c] ) != null )
							groupManager.addUserToGroup( requestDescriptor.sessionIdentifier, userID, groupId );
						else
							groupManager.removeUserFromGroup( requestDescriptor.sessionIdentifier, userID, groupId );
					}
				}
				catch( InsufficientPermissionException e ){
					insufficientPermissionNoted = true;
				}
				
			}
			
			if( insufficientPermissionNoted ){
				Html.addMessage(MessageType.WARNING, "Insufficient permissions to change at least one group", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_SET_GROUP_FAILED );
			}
			else{
				Html.addMessage(MessageType.INFORMATIONAL, "Group membership updated successfully", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_SET_GROUP_SUCCESS );
			}
		}
		
		// 10 -- Update password
		else if( action.matches("UpdatePassword") ){
			
			// 10.1 -- Get user ID
			int userId = -1;
			if( requestDescriptor.request.getParameter("UserID") != null ){
				try{
					userId = Integer.parseInt( requestDescriptor.request.getParameter("UserID") );
				}
				catch( NumberFormatException e ){
					userId = -2;
				}
			}
			
			// 10.2 -- Get new password and confirmation
			String newPassword = null;
			if( requestDescriptor.request.getParameter("NewPassword") != null )
				newPassword = requestDescriptor.request.getParameter("NewPassword");

			String newPasswordConfirmation = null;
			if( requestDescriptor.request.getParameter("NewPasswordConfirm") != null )
				newPasswordConfirmation = requestDescriptor.request.getParameter("NewPasswordConfirm");
			
			if( newPasswordConfirmation != null && newPassword != null && !newPasswordConfirmation.matches(newPassword) ){
				Html.addMessage(MessageType.WARNING, "New password does not match", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_UPDATEPWD_PASSWORD_NOT_SAME );
			}
			
			// 10.3 -- Get confirmation password
			String authPassword = null;
			if( requestDescriptor.request.getParameter("CurrentPassword") != null )
				authPassword = requestDescriptor.request.getParameter("CurrentPassword");
			
			// 10.4 -- Update the password 
			if( userId >= 0 && requestDescriptor.request.getParameter("Submit") != null ){
				try {
					xUserManager.changePassword( requestDescriptor.sessionIdentifier, userId, newPassword, authPassword );
				} catch (InputValidationException e) {
					Html.addMessage(MessageType.WARNING, "The password provided is not valid", requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_UPDATEPWD_NEW_PASSWORD_INVALID );
				} catch (InsufficientPermissionException e) {
					Html.addMessage(MessageType.WARNING, "You do not have sufficient permissions to update the password", requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_UPDATEPWD_FAILED );
				} catch (PasswordInvalidException e) {
					Html.addMessage(MessageType.WARNING, "Your current password is incorrect", requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_UPDATEPWD_AUTH_PASSWORD_INVALID );
				}
				
				Html.addMessage(MessageType.INFORMATIONAL, "Password successfully updated", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_UPDATEPWD_SUCCESS );
			}
			else{
				return new ActionDescriptor( OP_UPDATEPWD );
			}
		}
		
		//Fall through, no operations
		return new ActionDescriptor( ActionDescriptor.OP_NO_OPERATION);
	}
}
