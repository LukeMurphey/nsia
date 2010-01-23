package net.lukemurphey.nsia.htmlInterface;

import java.io.IOException;
import java.util.Vector;

import net.lukemurphey.nsia.AccessControlDescriptor;
import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.GroupManagement;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.RightDescriptor;
import net.lukemurphey.nsia.UserManagement;
import net.lukemurphey.nsia.GroupManagement.GroupDescriptor;
import net.lukemurphey.nsia.UserManagement.UserDescriptor;
import net.lukemurphey.nsia.htmlInterface.Html.MessageType;
import net.lukemurphey.nsia.trustBoundary.ApiAccessControl;
import net.lukemurphey.nsia.trustBoundary.ApiGroupManagement;
import net.lukemurphey.nsia.trustBoundary.ApiUserManagement;

public class HtmlUserRights extends HtmlContentProvider{
	
	private static final int USER_MANAGEMENT = 1;
	private static final int GROUP_MANAGEMENT = 2;
	private static final int SITE_GROUP_MANAGEMENT = 3;
	private static final int SYSTEM_CONFIGURATION = 4;
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException, IOException, NotFoundException{
		return getHtml( requestDescriptor, null );
	}
	
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws NoSessionException, GeneralizedException, NotFoundException{
		
		// 1 -- Create the content
		
		ApiAccessControl accessControl = new ApiAccessControl(Application.getApplication());
		int subjectId = -1;
		AccessControlDescriptor.Subject subjectType = AccessControlDescriptor.Subject.USER;
		StringBuffer body = new StringBuffer( 1024 );
		
		if( actionDesc == null ){
			performAction(requestDescriptor, accessControl);
		}
		
		body.append(Html.renderMessages(requestDescriptor.userId));

		//	 1.1 -- Get the user/group identifier that is being displayed/modified
		
		ApiUserManagement userManagement = new ApiUserManagement(Application.getApplication());
		UserDescriptor userDescriptor = null;
		
		ApiGroupManagement groupManagement = new ApiGroupManagement(Application.getApplication());
		GroupDescriptor groupDescriptor = null;
		
		//		1.1.1 -- Try to get the user identifier
		try{
			if( requestDescriptor.request.getParameter("UserID") != null ){
				subjectId = Integer.parseInt(requestDescriptor.request.getParameter("UserID"));
				subjectType = AccessControlDescriptor.Subject.USER;
			}
		}
		catch(NumberFormatException e){
			//User ID is invalid
			body.append( Html.getWarningDialog("User Identifier Invalid", "No user exists with the given identifier") );
		}
		
		if( subjectId < 0 ){
		//		1.1.2 -- Try to get the group identifier
			try{
				if( requestDescriptor.request.getParameter("GroupID") != null ){
					subjectId = Integer.parseInt(requestDescriptor.request.getParameter("GroupID"));
					subjectType = AccessControlDescriptor.Subject.GROUP;
				}
			}
			catch(NumberFormatException e){
				//Group ID is invalid
				body.append( Html.getWarningDialog("Group Identifier Invalid", "No group exists with the given identifier") );
			}
		}
		
		// 1.2 -- Display the header
		if( subjectType == AccessControlDescriptor.Subject.USER )
			body.append( Html.getSectionHeader( "User Rights Management", null ));
		else
			body.append( Html.getSectionHeader( "Group Rights Management", null ));
		
		// 1.3 -- Load the descriptor
		
		//	 1.3.1 -- Get the user descriptor
		if( subjectId > -1 && subjectType == AccessControlDescriptor.Subject.USER){
			try{
				userDescriptor = userManagement.getUserDescriptor(requestDescriptor.sessionIdentifier, subjectId);
			}catch(InsufficientPermissionException e){
				body.append( Html.getWarningDialog("Insufficient Permission", "You do not have permission to view this user") );
			}
			catch(IllegalArgumentException e){
				body.append( Html.getWarningDialog("User Identifier Invalid", "No user exists with the given identifier") );
			}
		}
		
		//	 1.3.2 -- Get the group descriptor
		if( subjectId > -1 && subjectType == AccessControlDescriptor.Subject.GROUP){
			try{
				groupDescriptor = groupManagement.getGroupDescriptor(requestDescriptor.sessionIdentifier, subjectId);
			}catch(InsufficientPermissionException e){
				body.append( Html.getWarningDialog("Insufficient Permission", "You do not have permission to view this group") );
			}
			catch(IllegalArgumentException e){
				body.append( Html.getWarningDialog("Group Identifier Invalid", "No group exists with the given identifier") );
			}
		}
		
		if( userDescriptor != null && userDescriptor.isUnrestricted())
			body.append( Html.getInfoNote("The current user is unrestricted; therefore, the rights below will be only be applied if the user demoted to restricted access") );
		
		// 1.4 -- Create the content
		
		//	 1.4.1 -- Determine which section to display
		int tabIndex = USER_MANAGEMENT;
		
		if( requestDescriptor.request.getParameter("TabIndex") != null ){
			try{
				tabIndex = Integer.parseInt(requestDescriptor.request.getParameter("TabIndex"));
			}
			catch( NumberFormatException e){
				tabIndex = USER_MANAGEMENT;
			}
		}
		
		//	 1.4.2 -- Create the subject identifier
		String subjectString;
		
		if( subjectType == AccessControlDescriptor.Subject.USER)
			subjectString = "UserID=" + subjectId;
		else
			subjectString = "GroupID=" + subjectId;
		
		body.append("<ul id=\"NavigationTabs\">");
		body.append("<li class=\"Tab1\"><a href=\"UserRights?TabIndex=" + USER_MANAGEMENT + "&" + subjectString + "\">User Management</a></li>");
		body.append("<li class=\"Tab2\"><a href=\"UserRights?TabIndex=" + GROUP_MANAGEMENT + "&" + subjectString + "\">Group Management</a></li>");
		body.append("<li class=\"Tab3\"><a href=\"UserRights?TabIndex=" + SITE_GROUP_MANAGEMENT + "&" + subjectString + "\">Site Group Management</a></li>");
		body.append("<li class=\"Tab4\"><a href=\"UserRights?TabIndex=" + SYSTEM_CONFIGURATION + "&" + subjectString + "\">System Configuration</a></li>");
		body.append("</ul>");
		
		try{
			StringBuffer formSection = new StringBuffer(1024);
			
			
			formSection.append( "<form action=\"UserRights\" method=\"post\"><table>" );
			
			formSection.append( createRightsTable( tabIndex, subjectId, subjectType, accessControl, requestDescriptor.sessionIdentifier ));

			formSection.append( "</table><p><input class=\"button\" type=\"Submit\" value=\"Apply\" name=\"Apply\">" );
			
			if(subjectType == AccessControlDescriptor.Subject.GROUP){
				formSection.append("<input type=\"hidden\" value=\"" + subjectId + "\" name=\"GroupID\">");
			}
			else{
				formSection.append("<input type=\"hidden\" value=\"" + subjectId + "\" name=\"UserID\">");
			}
			
			formSection.append("<input type=\"hidden\" value=\"" + tabIndex + "\" name=\"TabIndex\">");
			
			formSection.append( "</form>" );
			body.append(formSection);
		}catch(InsufficientPermissionException e){
			if( subjectType == AccessControlDescriptor.Subject.GROUP)
				body.append(Html.getWarningDialog("Insufficient Permission", "You do not have permission to view the rights of this group"));
			else
				body.append(Html.getWarningDialog("Insufficient Permission", "You do not have permission to view the rights of this user"));
		}
		
		
		// 2 -- Get the navigation bar
		NavigationPath navPath = new NavigationPath();
		
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		
		if( subjectType == AccessControlDescriptor.Subject.USER ){
			navPath.addPathEntry( "User Management", "/UserManagement" );
			if( subjectId > -1 ) navPath.addPathEntry( "View User", "/UserManagement?UserID=" + subjectId );
			if( subjectId > -1 ) navPath.addPathEntry( "Rights Management", "/UserRights?UserID=" + subjectId);
		}
		else if( subjectType == AccessControlDescriptor.Subject.GROUP ){
			navPath.addPathEntry( "Group Management", "/GroupManagement" );
			if( subjectId > -1 ) navPath.addPathEntry( "View Group", "/GroupManagement?GroupID=" + subjectId );
			if( subjectId > -1 ) navPath.addPathEntry( "Rights Management", "/UserRights?GroupID=" + subjectId);
		}
		
		String navigationHtml = Html.getNavigationPath( navPath );
		
		
		// 3 -- Get the menu items
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
		
		menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Site Groups", "/", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add Site Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("User Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Users", "/UserManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New User", "/UserManagement?Action=Add", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Logged in Users", "/Sessions", MenuItem.LEVEL_TWO) );
		
		if( subjectId > -1 && userDescriptor != null){
			if( userDescriptor.getAccountStatus() == UserManagement.AccountStatus.DISABLED )
				menuItems.add( new MenuItem("Enable User", "/UserManagement?Action=Enable&UserID=" + subjectId, MenuItem.LEVEL_TWO) );
			else
				menuItems.add( new MenuItem("Disable User", "/UserManagement?Action=Disable&UserID=" + subjectId, MenuItem.LEVEL_TWO) );
			
			menuItems.add( new MenuItem("Delete User", "/UserManagement?Action=Delete&UserID=" + subjectId, MenuItem.LEVEL_TWO) );
			menuItems.add( new MenuItem("Reset Password", "/UserManagement?Action=ResetPassword&UserID=" + subjectId, MenuItem.LEVEL_TWO) );
			menuItems.add( new MenuItem("Update Password", "/UserManagement?Action=UpdatePassword&UserID=" + subjectId, MenuItem.LEVEL_TWO) );
			if( userDescriptor.isBruteForceLocked() )
				menuItems.add( new MenuItem("Unlock Account", "/UserManagement?Action=UnlockAccount&Username=" + userDescriptor.getUserName(), MenuItem.LEVEL_TWO) );
		}

		
		menuItems.add( new MenuItem("Group Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Groups", "/GroupManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New Group", "/GroupManagement?Action=Add", MenuItem.LEVEL_TWO) );
		
		if( groupDescriptor != null ){
			GroupManagement.State groupStatus = groupDescriptor.getGroupState(); 
			if( groupStatus == GroupManagement.State.ACTIVE )
				menuItems.add( new MenuItem("Disable Group", "/GroupManagement?Action=Disable&GroupID=" + groupDescriptor.getGroupId(), MenuItem.LEVEL_TWO) );
			else
				menuItems.add( new MenuItem("Enable Group", "/GroupManagement?Action=Enable&GroupID=" + groupDescriptor.getGroupId(), MenuItem.LEVEL_TWO) );
		}
		
		String menuOutput = Html.getMenu( menuItems );
		
		// 4 -- Compile the result
		if( userDescriptor == null && groupDescriptor == null){
			String pageOutput = Html.getMainContent(Html.getWarningDialog("User or Group ID is invalid", "A valid user or group identifier was not provided"), menuOutput, navigationHtml);
			return new ContentDescriptor( "Rights Management", pageOutput );
		}
		
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		return new ContentDescriptor( "Rights Management", pageOutput );
	}
	
	private static StringBuffer createRightsTable( int tabIndex, int subjectId, AccessControlDescriptor.Subject subjectType, ApiAccessControl accessControl, String sessionIdentifier ) throws GeneralizedException, NoSessionException, InsufficientPermissionException{
		StringBuffer formSection = new StringBuffer(512);
		
		formSection.append("<div class=\"TabPanel\"><span class=\"SpacedInput\">");
		
		if( tabIndex == USER_MANAGEMENT ){
			//formSection.append(createSectionBreakRow("User Management"));
			formSection.append( createRow("Create New Users", "Users.Add", subjectId, subjectType, accessControl, sessionIdentifier) );
			formSection.append( createRow("Edit User", "Users.Edit", subjectId, subjectType, accessControl, sessionIdentifier) );
			formSection.append( createRow("View Users' Details", "Users.View", subjectId, subjectType, accessControl, sessionIdentifier) );
			formSection.append( createRow("View Users' Rights", "Users.Rights.View", subjectId, subjectType, accessControl, sessionIdentifier) );//TODO Resolve problem with this right
			formSection.append( createRow("Delete Users", "Users.Delete", subjectId, subjectType, accessControl, sessionIdentifier) );
			formSection.append( createRow("Unlock Accounts (due to repeated authentication attempts)", "Users.Unlock", subjectId, subjectType, accessControl, sessionIdentifier) );
			formSection.append( createRow("Update Other's Password (applies only to the other users' accounts)", "Users.UpdatePassword", subjectId, subjectType, accessControl, sessionIdentifier) );
			formSection.append( createRow("Update Account Details (applies only to the users' own account)", "Users.UpdateOwnPassword", subjectId, subjectType, accessControl, sessionIdentifier) );
			formSection.append( createRow("Delete Users' Sessions (kick users off)", "Users.Sessions.Delete", subjectId, subjectType, accessControl, sessionIdentifier) );
			formSection.append( createRow("View Users' Sessions (see who is logged in)", "Users.Sessions.View", subjectId, subjectType, accessControl, sessionIdentifier) );
		}
		else if( tabIndex == GROUP_MANAGEMENT ){
			//formSection.append(createSectionBreakRow("Group Management"));
			formSection.append( createRow("Create New Groups", "Groups.Add", subjectId, subjectType, accessControl, sessionIdentifier) );
			formSection.append( createRow("View Groups", "Groups.View", subjectId, subjectType, accessControl, sessionIdentifier) );
			formSection.append( createRow("Edit Groups", "Groups.Edit", subjectId, subjectType, accessControl, sessionIdentifier) );
			formSection.append( createRow("Delete Groups", "Groups.Delete", subjectId, subjectType, accessControl, sessionIdentifier) );
			formSection.append( createRow("Manage Group Membership", "Groups.Membership.Edit", subjectId, subjectType, accessControl, sessionIdentifier) );
		}
		else if( tabIndex == SITE_GROUP_MANAGEMENT ){
			//formSection.append(createSectionBreakRow("Site Group Manager"));
			formSection.append( createRow("View Site Groups", "SiteGroups.View", subjectId, subjectType, accessControl, sessionIdentifier) );
			formSection.append( createRow("Create New Site Group", "SiteGroups.Add", subjectId, subjectType, accessControl, sessionIdentifier) );
			formSection.append( createRow("Delete Site Groups", "SiteGroups.Delete", subjectId, subjectType, accessControl, sessionIdentifier) );
			formSection.append( createRow("Edit Site Groups", "SiteGroups.Edit", subjectId, subjectType, accessControl, sessionIdentifier) );
		}
		else {//if( tabIndex == SYSTEM_CONFIGURATION){
			//formSection.append(createSectionBreakRow("System Configuration and Status"));
			formSection.append( createRow("View System Information and Status", "System.Information.View", subjectId, subjectType, accessControl, sessionIdentifier) );
			formSection.append( createRow("Modify System Configuration", "System.Configuration.Edit", subjectId, subjectType, accessControl, sessionIdentifier) );
			//formSection.append( createRow("Clear SQL Database Warnings", "Administration.ClearSqlWarnings", subjectId, subjectType, accessControl, sessionIdentifier) );
			formSection.append( createRow("View Firewall Configuration", "System.Firewall.View", subjectId, subjectType, accessControl, sessionIdentifier) );
			formSection.append( createRow("Change Firewall Configuration", "System.Firewall.Edit", subjectId, subjectType, accessControl, sessionIdentifier) );
			formSection.append( createRow("Start/Stop Scanner", "System.ControlScanner", subjectId, subjectType, accessControl, sessionIdentifier) );
			formSection.append( createRow("Allow Gratuitous Scanning of All Rules", "SiteGroups.ScanAllRules", subjectId, subjectType, accessControl, sessionIdentifier) );
		}
		
		formSection.append("</span></div>");
		
		return formSection;
		
	}
	
	private static StringBuffer createRow(String title, String right, int subjectId, AccessControlDescriptor.Subject subjectType, ApiAccessControl accessControl, String sessionIdentifier) throws GeneralizedException, NoSessionException, InsufficientPermissionException{
		
		try{
			// 1 -- Get the relevant right descriptor
			RightDescriptor rightDescriptor = null;
			
			if( subjectType == AccessControlDescriptor.Subject.USER){
				rightDescriptor = accessControl.getUserRight(sessionIdentifier, subjectId, right, false);
			}
			else{
				rightDescriptor = accessControl.getGroupRight(sessionIdentifier, subjectId, right);
			}
			
			// 2 -- Determined if the checkbox should be checked
			boolean isAllowed = false;
			
			if( rightDescriptor != null && rightDescriptor.getRight() == RightDescriptor.Action.PERMIT ){
				isAllowed = true;
			}
			
			// 3 -- Prepare the output
			StringBuffer output = new StringBuffer(512);
			output.append("<input id=\"" + right + "\" type=\"checkbox\" name=\"" + right + "\"");
			
			if( isAllowed )
				output.append(" checked");
			
			output.append("><label for=\"" + right + "\">" + title + "</label><br>");
			
			return output;
		}
		catch(NotFoundException e){
			return new StringBuffer(); //Return nothing if the right does not exist
		}
	}
	
	private static ActionDescriptor performAction(WebConsoleConnectionDescriptor requestDescriptor, ApiAccessControl accessControl ) throws GeneralizedException, NoSessionException{

		// 0 -- Precondition check
		int subjectId = -1;
		AccessControlDescriptor.Subject subjectType = null;

		if( requestDescriptor.request.getParameter("Apply") == null)
			return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
		
		// 1 -- Get the user/group identifier that is being displayed/modified
		//ApiUserManagement userManagement = new ApiUserManagement(Application.getApplication());
		//UserDescriptor userDescriptor = null;
		
		//ApiGroupManagement groupManagement = new ApiGroupManagement(Application.getApplication());
		//GroupDescriptor groupDescriptor = null;
		
		//	 1.1 -- Try to get the user identifier
		try{
			if( requestDescriptor.request.getParameter("UserID") != null ){
				subjectId = Integer.parseInt(requestDescriptor.request.getParameter("UserID"));
				subjectType = AccessControlDescriptor.Subject.USER;
			}
		}
		catch(NumberFormatException e){
			//User ID is invalid
			return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
		}
		
		if( subjectId < 0 ){
		//	 1.2 -- Try to get the group identifier
			try{
				if( requestDescriptor.request.getParameter("GroupID") != null ){
					subjectId = Integer.parseInt(requestDescriptor.request.getParameter("GroupID"));
					subjectType = AccessControlDescriptor.Subject.GROUP;
				}
			}
			catch(NumberFormatException e){
				//Group ID is invalid
				return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
			}
		}
		
		//	 1.3 -- Exit if a valid user or group identifer was not found
		if( subjectId < 0 || subjectType == null )
			return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
		
		
		// 2 -- Apply the rights for the given user or group
		int tabIndex = USER_MANAGEMENT;
		
		if( requestDescriptor.request.getParameter("TabIndex") != null ){
			try{
				tabIndex = Integer.parseInt(requestDescriptor.request.getParameter("TabIndex"));
			}
			catch( NumberFormatException e){
				tabIndex = USER_MANAGEMENT;
			}
		}
		
		int setFailures = 0;
		
		if( tabIndex == USER_MANAGEMENT ){
			if( !setRight( requestDescriptor, "Users.Add", subjectId, subjectType, accessControl) ) setFailures++;
			if( !setRight( requestDescriptor, "Users.Edit", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( requestDescriptor, "Users.View", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( requestDescriptor, "Users.Rights.View", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( requestDescriptor, "Users.Delete", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( requestDescriptor, "Users.Unlock",  subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( requestDescriptor, "Users.UpdatePassword", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( requestDescriptor, "Users.UpdateOwnPassword", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( requestDescriptor, "Users.Sessions.Delete", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( requestDescriptor, "Users.Sessions.View", subjectId, subjectType, accessControl)) setFailures++;
			
			Html.addMessage(MessageType.INFORMATIONAL, "Rights successfully updated", requestDescriptor.userId);
		}
		else if( tabIndex == GROUP_MANAGEMENT ){
			if( !setRight( requestDescriptor, "Groups.Add", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( requestDescriptor, "Groups.View", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( requestDescriptor, "Groups.Edit", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( requestDescriptor, "Groups.Delete", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( requestDescriptor, "Groups.Membership.Edit", subjectId, subjectType, accessControl)) setFailures++;
			
			Html.addMessage(MessageType.INFORMATIONAL, "Rights successfully updated", requestDescriptor.userId);
		}
		else if( tabIndex == SITE_GROUP_MANAGEMENT ){
			if( !setRight( requestDescriptor, "SiteGroups.View", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( requestDescriptor, "SiteGroups.Add", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( requestDescriptor, "SiteGroups.Delete", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( requestDescriptor, "SiteGroups.Edit", subjectId, subjectType, accessControl)) setFailures++;
			
			Html.addMessage(MessageType.INFORMATIONAL, "Rights successfully updated", requestDescriptor.userId);
		}
		else if( tabIndex == SYSTEM_CONFIGURATION ){
			if( !setRight( requestDescriptor, "System.Information.View", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( requestDescriptor, "System.Configuration.Edit", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( requestDescriptor, "Administration.ClearSqlWarnings", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( requestDescriptor, "System.Firewall.View", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( requestDescriptor, "System.Firewall.Edit", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( requestDescriptor, "System.ControlScanner", subjectId, subjectType, accessControl)) setFailures++;
			if( !setRight( requestDescriptor, "SiteGroups.ScanAllRules", subjectId, subjectType, accessControl)) setFailures++;
			
			Html.addMessage(MessageType.INFORMATIONAL, "Rights successfully updated", requestDescriptor.userId);
		}
		
		return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
	}
	
	private static boolean setRight(WebConsoleConnectionDescriptor requestDescriptor, String rightName, int subjectId, AccessControlDescriptor.Subject subjectType, ApiAccessControl accessControl ) throws NoSessionException, GeneralizedException{
		
		AccessControlDescriptor.Action right;
		
		if( requestDescriptor.request.getParameter(rightName) != null)
			right = AccessControlDescriptor.Action.PERMIT;
		else
			right = AccessControlDescriptor.Action.DENY;
		
		//return true;
		RightDescriptor rightDescriptor = new RightDescriptor(right, subjectType, subjectId, rightName);
		return accessControl.setRight(requestDescriptor.sessionIdentifier, rightDescriptor);
	}

}
