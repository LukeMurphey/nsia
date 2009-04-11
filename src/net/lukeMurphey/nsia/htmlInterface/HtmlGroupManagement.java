package net.lukeMurphey.nsia.htmlInterface;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Vector;
import javax.servlet.ServletException;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.GroupManagement;
import net.lukeMurphey.nsia.InputValidationException;
import net.lukeMurphey.nsia.InsufficientPermissionException;
import net.lukeMurphey.nsia.NoDatabaseConnectionException;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.NotFoundException;
import net.lukeMurphey.nsia.trustBoundary.ApiGroupManagement;
import net.lukeMurphey.nsia.htmlInterface.Html.MessageType;

import org.apache.commons.lang.StringEscapeUtils;

public class HtmlGroupManagement extends HtmlContentProvider {
	
	private static final int OP_MODIFY_NAME_INVALID = 100;
	private static final int OP_MODIFY_DESCRIPTION_INVALID = 101;
	private static final int OP_ADD_NAME_INVALID = 102;
	private static final int OP_ADD_DESCRIPTION_INVALID = 103;
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException, IOException, NotFoundException{
		return getHtml( requestDescriptor, null );
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws IOException, NoSessionException, GeneralizedException, NotFoundException{
		ApiGroupManagement xGroupManagement = new ApiGroupManagement(Application.getApplication());
		String action = requestDescriptor.request.getParameter("Action");

		// 1 -- Perform the actions
		if( actionDesc == null )
			actionDesc = performAction(requestDescriptor,xGroupManagement);

		// 2 -- Display data

		//	2.1 -- Display specific group information 
		if( requestDescriptor.request.getParameter("GroupID") != null && actionDesc.result != ActionDescriptor.OP_DELETE_FAILED && actionDesc.result != ActionDescriptor.OP_DELETE_SUCCESS ){
			return getGroupModify(requestDescriptor, xGroupManagement, actionDesc);
		}
		//	2.2 -- Display new group page
		else if(action != null && action.matches("Add")){
			return getGroupAdd( requestDescriptor, xGroupManagement, actionDesc);
		}
		
		//	2.3 -- Display list
		return getGroupList( requestDescriptor, xGroupManagement, actionDesc);
	}

	/**
	 * Get HTML related to the modification of a Site Group.
	 * @param request
	 * @param response
	 * @param requestDescriptor
	 * @param httpMethod
	 * @param groupManagement
	 * @return
	 * @throws SQLException
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws IOException 
	 * @throws IOException
	 * @throws NotFoundException 
	 */
	private static ContentDescriptor getGroupModify(WebConsoleConnectionDescriptor requestDescriptor, ApiGroupManagement groupManagement, ActionDescriptor actionDesc) throws NoSessionException, GeneralizedException, IOException, NotFoundException {
		
		int groupId ;
		
		try{
			groupId = Integer.parseInt( requestDescriptor.request.getParameter("GroupID") );
		}
		catch(NumberFormatException e){
			return new ContentDescriptor("Group Management", Html.getWarningDialog("", "The group identifier provided is invalid", "/", "Return to Main Dashboard"));
		}
		
		GroupManagement.GroupDescriptor groupDesc;
		
		StringBuffer body = new StringBuffer();
		
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		try {
			groupDesc = groupManagement.getGroupDescriptor( requestDescriptor.sessionIdentifier, groupId );
		} catch (InsufficientPermissionException e) {
			body.append( Html.getWarningNote("You do not have permission to view this group") );
			requestDescriptor.response.sendRedirect("GroupManagement");
			return new ContentDescriptor("Group Management", "Redirecting...");
		}
		
		String groupName =  StringEscapeUtils.escapeHtml( groupDesc.getGroupName() );
		String groupDescription = StringEscapeUtils.escapeHtml( groupDesc.getDescription() );
		
		if( requestDescriptor.request.getParameter("GroupName") != null)
			groupName = StringEscapeUtils.escapeHtml(requestDescriptor.request.getParameter("GroupName"));
		if( requestDescriptor.request.getParameter("Description") != null)
			groupDescription = StringEscapeUtils.escapeHtml(requestDescriptor.request.getParameter("Description"));
		
		body.append( Html.getSectionHeader( "Group Management", null ) );
		body.append( "<table class=\"DataTable\"><form method=\"post\">" );
		body.append( "<tr><td width=\"150\" class=\"TitleText\">Group ID</td><td>").append( groupDesc.getGroupId() ).append( "</td></tr>" );
		
		if( actionDesc.result == OP_MODIFY_NAME_INVALID ){
			body.append( "<tr class=\"ValidationFailed\">" );
		}
		else{
			body.append( "<tr>" );
		}
		
		body.append( "<td class=\"TitleText\">Group Name</td><td><input class=\"textInput\" style=\"width: 350px;\" type=\"text\" name=\"GroupName\" value=\"" ).append(  groupName ).append(  "\"></td></tr>" );
		
		if( actionDesc.result == OP_MODIFY_DESCRIPTION_INVALID ){
			body.append( "<tr class=\"ValidationFailed\">" );
		}
		else{
			body.append( "<tr>" );
		}
		
		body.append( "<td valign=\"top\" class=\"TitleText\">Description</td><td><textarea rows=\"8\" style=\"width: 350px;\" name=\"Description\">" ).append(  groupDescription ).append(  "</textarea></td></tr>" );
		body.append( "<tr class=\"lastRow\"><td colspan=\"99\" align=\"right\"><input type=\"hidden\" name=\"Action\" value=\"Edit\"><input class=\"button\" type=\"submit\" name=\"AddNew\" value=\"Apply Changes\"></td></tr></form></table>" );
		
		
		// 2 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "Group Management", "/GroupManagement" );
		navPath.addPathEntry( "Modify Group", "/GroupManagement?Action=Edit&GroupID=" + requestDescriptor.request.getParameter("GroupID") );
		String navigationHtml = Html.getNavigationPath( navPath );
		
		// 3 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
				
		menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Site Groups", "/", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add Site Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );
				
		menuItems.add( new MenuItem("User Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Users", "/UserManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New User", "/UserManagement?Action=Add", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Logged in Users", "/Sessions", MenuItem.LEVEL_TWO) );
				
		menuItems.add( new MenuItem("Group Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Add New Group", "/GroupManagement?Action=Add", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Manage Rights", "/UserRights?GroupID=" + groupId, MenuItem.LEVEL_TWO) );
		GroupManagement.State groupStatus = groupDesc.getGroupState(); 
		if( groupStatus == GroupManagement.State.ACTIVE )
			menuItems.add( new MenuItem("Disable Group", "/GroupManagement?Action=Disable&GroupID=" + groupId, MenuItem.LEVEL_TWO) );
		else
			menuItems.add( new MenuItem("Enable Group", "/GroupManagement?Action=Enable&GroupID=" + groupId, MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Delete Group", "/GroupManagement?Action=Delete&GroupID=" + groupId, MenuItem.LEVEL_TWO) );
		
		
		String menuOutput = Html.getMenu( menuItems );
		
		// 4 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor( "Group Management", pageOutput );
	}
	
	/**
	 * Get the HTML related to adding an new HTML group.
	 * @param request
	 * @param response
	 * @param webConsoleServlet
	 * @param httpMethod
	 * @param groupManagement
	 * @return
	 * @throws SQLException
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws IOException
	 */
	private static ContentDescriptor getGroupAdd(WebConsoleConnectionDescriptor requestDescriptor, ApiGroupManagement groupManagement, ActionDescriptor actionDesc) throws NoSessionException, GeneralizedException {
			
		StringBuffer body = new StringBuffer();
		String groupName = StringEscapeUtils.escapeHtml( requestDescriptor.request.getParameter("GroupName") );
		String groupDescription = StringEscapeUtils.escapeHtml( requestDescriptor.request.getParameter("Description") );

		body.append(Html.renderMessages(requestDescriptor.userId));
		
		body.append( Html.getSectionHeader( "Group Management", null ) );//(String)userDesc.get("Username")
		body.append( "<table class=\"DataTable\"><form method=\"post\">" );
		
		if( actionDesc.result == OP_ADD_NAME_INVALID ){
			body.append( "<tr class=\"ValidationFailed\">" );
		}
		else{
			body.append( "<tr>" );
		}
		
		if( groupName == null )
			body.append( "<td class=\"TitleText\">Group Name</td><td><input class=\"textInput\" style=\"width: 350px;\" type=\"text\" name=\"GroupName\"></td></tr>" );
		else
			body.append( "<td class=\"TitleText\">Group Name</td><td><input class=\"textInput\" style=\"width: 350px;\" type=\"text\" value=\"" ).append(  groupName ).append(  "\" name=\"GroupName\"></td></tr>" );
		
		if( actionDesc.result == OP_ADD_DESCRIPTION_INVALID ){
			body.append( "<tr class=\"ValidationFailed\">" );
		}
		else{
			body.append( "<tr>" );
		}
		
		if( groupDescription == null )
			body.append( "<tr><td valign=\"top\" class=\"TitleText\">Description</td><td><textarea rows=\"8\" style=\"width: 350px;\" name=\"Description\"></textarea></td></tr>" );
		else
			body.append( "<tr><td valign=\"top\" class=\"TitleText\">Description</td><td><textarea rows=\"8\" style=\"width: 350px;\" name=\"Description\">" ).append(  groupDescription ).append(  "</textarea></td></tr>" );

		body.append( "<tr class=\"lastRow\"><td colspan=\"99\" align=\"right\"><input class=\"button\" type=\"submit\" value=\"Add Group\"></td></tr></form></table>" );
		
		
		// 2 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "Group Management", "/GroupManagement" );
		navPath.addPathEntry( "Add Group", "/GroupManagement?Action=Add");
		String navigationHtml = Html.getNavigationPath( navPath );
		
		// 3 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
				
		menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Site Groups", "/", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add Site Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );
				
		menuItems.add( new MenuItem("User Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Users", "/UserManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New User", "/UserManagement?Action=Add", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Logged in Users", "/Sessions", MenuItem.LEVEL_TWO) );
				
		menuItems.add( new MenuItem("Group Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Groups", "/GroupManagement", MenuItem.LEVEL_TWO) );
		
		String menuOutput = Html.getMenu( menuItems );
		
		// 4 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor( "Group Management", pageOutput );
	}

	/**
	 * Get a row with the information regarding a site group.
	 * @param groupDescriptor
	 * @return
	 */
	private static String createRow( GroupManagement.GroupDescriptor groupDescriptor ){
		StringBuffer output = new StringBuffer();
		output.append( "<tr class=\"Background1\"><td>").append( groupDescriptor.getGroupId() )
			.append( "</td><td><a href=\"GroupManagement?GroupID=").append( groupDescriptor.getGroupId() ).append(  "\">" );
		
		GroupManagement.State groupStatus = groupDescriptor.getGroupState(); 
		if( groupStatus == GroupManagement.State.ACTIVE )
			output.append( "<table><tr><td><img alt=\"Enabled\" src=\"/16_Group\"></a></td>" );
		else
			output.append( "<table><tr><td><img alt=\"Disabled\" src=\"/16_GroupDisabled\"></a></td>" );
		
		output.append( "<td><a href=\"GroupManagement?GroupID=" ).append(  groupDescriptor.getGroupId() ).append(  "\">" ).append(  StringEscapeUtils.escapeHtml( groupDescriptor.getGroupName() )).append(  "</a></td></tr></table></td>" );
		
		if( groupStatus == GroupManagement.State.ACTIVE )
			output.append( "<td>Enabled&nbsp;&nbsp;</td>" );
		else
			output.append( "<td>Disabled&nbsp;&nbsp;</td>" );
			
		if( groupDescriptor.getDescription() == null )
			output.append( "<td class=\"Background1\">(No Description Specified)</td>" );
		else
			output.append( "<td class=\"Background1\">" ).append(  StringEscapeUtils.escapeHtml( Html.shortenString( groupDescriptor.getDescription(), 64 ) )).append(  "</td>" );

		
		return output + "</tr>";
	}
	
	/**
	 * Get an HTML list of the groups.
	 * @param request
	 * @param response
	 * @param webConsoleServlet
	 * @param httpMethod
	 * @param groupManagement
	 * @return
	 * @throws SQLException
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 */
	private static ContentDescriptor getGroupList(WebConsoleConnectionDescriptor requestDescriptor, ApiGroupManagement groupManagement, ActionDescriptor actionDesc) throws NoSessionException, GeneralizedException {
		
		StringBuffer body = new StringBuffer();
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		GroupManagement.GroupDescriptor[] groupDescriptors;
		try {
			
			groupDescriptors = groupManagement.getGroupDescriptors( requestDescriptor.sessionIdentifier );
			body.append( Html.getSectionHeader( "Group Management", null ) );
			
			if( groupDescriptors.length == 0 ){
				body.append( Html.getDialog("No groups have been created yet.<p><a href=\"GroupManagement?Action=Add\">[Create Group Now]</a>", "No Groups", "/32_Information", false) );
			}
			else{
				body.append( "<table><tr class=\"Background0\"><td class=\"Text_3\">Group ID</td><td class=\"Text_3\">Group Name</td><td class=\"Text_3\">Status</td><td class=\"Text_3\">Group Description</td></tr>" );
				for( int c = 0; c < groupDescriptors.length; c++ ){
					body.append( createRow( groupDescriptors[c] ) );
				}
				body.append( "</table>" );
			}
			
		} catch (InsufficientPermissionException e) {
			
			//ContentDescriptor desc = HtmlProblemDialog.getHtml(requestDescriptor, "Insufficient Permission", "You do not have permission to enumerate groups", HtmlProblemDialog.DIALOG_WARNING);
			//body.append(desc.getBody());
			body.append("<p>");
			body.append(Html.getWarningDialog("Insufficient Permission", "You do not have permission to enumerate groups", "Console", "Return to Main Dashboard"));
		}
		
		
		// 2 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "Group Management", "/GroupManagement" );
		String navigationHtml = Html.getNavigationPath( navPath );
		
		// 3 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();

		menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Site Groups", "/", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add Site Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );
				
		menuItems.add( new MenuItem("User Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Users", "/UserManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New User", "/UserManagement?Action=Add", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Logged in Users", "/Sessions", MenuItem.LEVEL_TWO) );
				
		menuItems.add( new MenuItem("Group Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Add New Group", "/GroupManagement?Action=Add", MenuItem.LEVEL_TWO) );
		
		String menuOutput = Html.getMenu( menuItems );
		
		// 4 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor( "Group Management", pageOutput );
	}
	
	/**
	 * Perform any actions requested. 
	 * @param request
	 * @param response
	 * @param webConsoleServlet
	 * @param httpMethod
	 * @param xUserManager
	 * @return
	 * @throws ServletException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws NoDatabaseConnectionException 
	 * @throws SQLException 
	 */
	private static ActionDescriptor performAction(WebConsoleConnectionDescriptor requestDescriptor, ApiGroupManagement xGroupManager) throws GeneralizedException, NoSessionException{
		String action = requestDescriptor.request.getParameter("Action");
		Integer groupID;
		try{
			groupID = new Integer( requestDescriptor.request.getParameter("GroupID") );
		}
		catch( NumberFormatException e ){
			groupID = null;
		}

		String groupName = requestDescriptor.request.getParameter("GroupName");
		String groupDescription = requestDescriptor.request.getParameter("Description");
		
		// 1 -- Perform the operation
		
		//	 1.1 -- No operation
		if( action == null ){
			return new ActionDescriptor( ActionDescriptor.OP_LIST );
		}
		//	 1.2 -- Disable the group
		else if( action.matches("Disable") && groupID != null ){
			try{
				if( xGroupManager.disableGroup( requestDescriptor.sessionIdentifier, groupID.intValue() ) ){
					Html.addMessage(MessageType.INFORMATIONAL, "Group successfully disabled", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_DISABLE_SUCCESS );
				}
				else{
					Html.addMessage(MessageType.WARNING, "Group was unsuccessfully disabled", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_DISABLE_FAILED);
				}
			}
			catch( InsufficientPermissionException e ){
				Html.addMessage(MessageType.WARNING, "You don't have permission to disable the group", requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_DISABLE_FAILED );
			}
		}
		//	 1.3 -- Add a new group
		else if( action.matches("Add") && groupName != null && groupDescription != null ){
			try {
				xGroupManager.addGroup(requestDescriptor.sessionIdentifier, groupName, groupDescription);
			} catch (InsufficientPermissionException e) {
				Html.addMessage(MessageType.WARNING, "You don't have permission to add a group", requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_ADD_FAILED );
			} catch (InputValidationException e) {
				if( e.getFieldDescription().matches("GroupDescription")){
					Html.addMessage(MessageType.WARNING, "Group description is invalid (contains invalid characters)", requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_ADD_DESCRIPTION_INVALID);
				}
				else{
					Html.addMessage(MessageType.WARNING, "Group name is invalid (contains invalid characters)", requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_ADD_NAME_INVALID);
				}
			}
			
			Html.addMessage(MessageType.INFORMATIONAL, "New group added successfully", requestDescriptor.userId.longValue());
			return new ActionDescriptor( ActionDescriptor.OP_ADD_SUCCESS);
		}
		//	 1.4 -- Edit the group
		else if( action.matches("Edit") && groupID != null && groupName != null && groupDescription != null ){
			
			if( groupName.length() == 0){
				Html.addMessage(MessageType.WARNING, "Group name is invalid (contains no characters)", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_MODIFY_NAME_INVALID );
			}
			
			try {
				if( xGroupManager.updateGroupInfo(requestDescriptor.sessionIdentifier, groupID.intValue(), groupName, groupDescription) ){
					Html.addMessage(MessageType.INFORMATIONAL, "Group updated successfully", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
				}
				else{
					Html.addMessage(MessageType.WARNING, "Group not updated successfully", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED);
				}
			} catch (InputValidationException e) {
				if( e.getFieldDescription().matches("GroupDescription")){
					Html.addMessage(MessageType.WARNING, "Group description is invalid (contains invalid characters))", requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_MODIFY_DESCRIPTION_INVALID);
				}
				else{
					Html.addMessage(MessageType.WARNING, "Group name is invalid", requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_MODIFY_NAME_INVALID );
				}
			} catch (InsufficientPermissionException e) {
				Html.addMessage(MessageType.WARNING, "You do not have permission that modify the group", requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED );
			}
		}
		//	1.5 -- Enable the group
		else if( action.matches("Enable") && groupID != null ){
			try{
				if( xGroupManager.enableGroup( requestDescriptor.sessionIdentifier, groupID.intValue() ) ){
					Html.addMessage(MessageType.INFORMATIONAL, "Group successfully enabled", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_ENABLE_SUCCESS );
				}
				else {
					Html.addMessage(MessageType.INFORMATIONAL, "Group successfully enabled", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_ENABLE_FAILED );
				}
			}
			catch( InsufficientPermissionException e ){
				Html.addMessage(MessageType.WARNING, "You don't have permission to enable the group", requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_ENABLE_FAILED );
			}
		}
		//	1.6 -- Delete the group
		else if( action.matches("Delete") && groupID != null ){
			try{
				if( xGroupManager.deleteGroup( requestDescriptor.sessionIdentifier, groupID.intValue() ) ){
					Html.addMessage(MessageType.INFORMATIONAL, "Group successfully deleted", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_DELETE_SUCCESS );
				}
				else{
					Html.addMessage(MessageType.WARNING, "Group was not deleted", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_DELETE_FAILED );
				}
			}
			catch( InsufficientPermissionException e ){
				Html.addMessage(MessageType.WARNING, "You don't have permission to delete the group", requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_DELETE_FAILED );
			}
		}
		
		return new ActionDescriptor( ActionDescriptor.OP_LIST );
	}
}
