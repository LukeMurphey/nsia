package net.lukeMurphey.nsia.htmlInterface;

import net.lukeMurphey.nsia.AccessControlDescriptor;
import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.InsufficientPermissionException;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.NotFoundException;
import net.lukeMurphey.nsia.ObjectPermissionDescriptor;
import net.lukeMurphey.nsia.htmlInterface.Html.MessageType;
import net.lukeMurphey.nsia.trustBoundary.*;

public class HtmlAccessControl extends HtmlContentProvider {

	private static final int OP_SUBJECT_INVALID = 100;
	
	private static final int VALUE_UNDEFINED = -1;
	private static final int VALUE_INVALID = -2;
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, InvalidHtmlOperationException{
		return getHtml( requestDescriptor, null );
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException{
		StringBuffer body = new StringBuffer();
		
		//HTML Page begin
		body.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
		body.append("<html>"); 
		body.append("<head>");
		body.append("<link rel=\"shortcut icon\" href=\"/16_appicon.ico\" type=\"image/x-icon\">");
		body.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=windows-1252\">");
		body.append("<title>Access Control</title>");
		body.append("<style type=\"text/css\">");
		body.append("@import url(Stylesheet.css);");
		body.append("</style>");
		body.append("</head>");
		body.append("<body class=\"ContentMain\" face=\"Arial, sans-serif\">");
		
		ApiAccessControl accessControl = new ApiAccessControl( Application.getApplication() );
		ApiGroupManagement groupManagement = new ApiGroupManagement( Application.getApplication() );
		ApiUserManagement userManagement = new ApiUserManagement( Application.getApplication() );
		
		if( actionDesc == null )
			actionDesc = performAction( requestDescriptor, accessControl );
		
		ContentDescriptor content;
		
		if( actionDesc.result == ActionDescriptor.OP_VIEW || actionDesc.result == ActionDescriptor.OP_UPDATE_SUCCESS
				|| actionDesc.result == ActionDescriptor.OP_DELETE_SUCCESS) 
			content =  getACLViewForm( requestDescriptor, accessControl, groupManagement, userManagement, actionDesc );
		else
			content =  getACLEditForm( requestDescriptor, accessControl, groupManagement, userManagement, actionDesc );
		
		body.append( content.getBody() );
		
		// 4 -- End the HTML page
		body.append("</body></html>");
		
		return new ContentDescriptor( "Access Control List", body, false );
	}
	
	/**
	 * Get the content descriptor for viewing an ACL
	 * @param request
	 * @param response
	 * @param requestDescriptor
	 * @param httpMethod
	 * @param accessControl
	 * @param groupManagement
	 * @param userManagement
	 * @return
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws InvalidHtmlParameterException 
	 */
	private static ContentDescriptor getACLViewForm( WebConsoleConnectionDescriptor requestDescriptor, ApiAccessControl accessControl, ApiGroupManagement groupManagement, ApiUserManagement userManagement, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException{
		StringBuffer body = new StringBuffer();
		Long objectId = (Long)actionDesc.addData;
		
		if( objectId == null ){
			try{
				objectId = Long.valueOf( Long.parseLong( requestDescriptor.request.getParameter("ObjectID") ));
			}
			catch(NumberFormatException e){
				throw new InvalidHtmlParameterException("Invalid Parameter", "A valid object identifier was not provided", "Console");
			}
		}
		
		// Header
		body.append( "<form action=\"AccessControl\" method=\"post\"><table cellpadding=\"7\" cellspacing=\"0\" width=\"100%\" align=\"center\">");
		body.append( "<tr><td colspan=\"99\" class=\"TopBottomBorder2\">&nbsp;</td></tr>" );
		body.append( "<tr><td class=\"TopBottomBorder2\" style=\"width: 20px; background-color: #FFFFFF;\"><img style=\"margin-top: 4px;\" src=\"/32_Lock\" alt=\"ACL\"></td><td class=\"TopBottomBorder2\" style=\"background-color: #FFFFFF;\" ><span class=\"Text_2\">Access Control</span><br/>View, modify the ACLs</td></tr>" );
		body.append( "</table>" );
		
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		ObjectPermissionDescriptor[] objectPermissionDescriptors;
		
		
		try {
			objectPermissionDescriptors = accessControl.getAllAclEntries( requestDescriptor.sessionIdentifier, objectId.longValue());
		
			//if( objectPermissionDescriptors.length == 0){
				//body.append( Html.getDialog( "No ACL entries exist yet.<br><a href=\"/AccessControl?ObjectID=" + requestDescriptor.request.getParameter("ObjectID") + "&Action=New\">[Create New Entry]</a>", "No ACLs Exist", "32_Information") );
			//}
			
			// Output the table start
			body.append("<br><table width=\"100%\"><tr class=\"Background0\"><td width=\"128px\" class=\"Text_2\">Name</td><td width=\"55px\" class=\"Text_2\">ID</td><td width=\"55px\" class=\"Text_2\">Read</td><td width=\"55px\" class=\"Text_2\">Modify</td><td width=\"55px\" class=\"Text_2\">Delete</td><td width=\"55px\" class=\"Text_2\">Execute</td><td width=\"55px\" class=\"Text_2\">Control</td><td width=\"55px\" class=\"Text_2\">Create</td><td width=\"15%\" colspan=\"2\">&nbsp;</td></tr>");
			
			for( int c = 0; c < objectPermissionDescriptors.length; c++ ){
				try{
					body.append( createRow( objectPermissionDescriptors[c], userManagement, groupManagement, requestDescriptor  ));
				}
				catch(NotFoundException e){
					//Ignore this exception, this may be thrown when an ACL was deleted after we obtained the ACL list. Ignoring this exception will simply skip the now missing ACL entry  
				}
			}
			
			if( objectPermissionDescriptors.length == 0){
				body.append( "<tr class=\"Background3\"><td colspan=\"99\">" );
				body.append( Html.getInfoNote("No Access Control List Entries Exist") );
				body.append( "</td></tr>" );
			}
			
			// Output the end of the table
			body.append("<tr class=\"Background3\"><td align=\"Right\" colspan=\"10\"><input type=\"hidden\" name=\"ObjectID\" value=\"" + requestDescriptor.request.getParameter("ObjectID") + "\"><input class=\"button\" type=\"Submit\" value=\"New Entry\" name=\"New\">&nbsp;&nbsp;&nbsp;<input onClick=\"javascript:window.close();\" class=\"button\" type=\"Submit\" value=\"Close\"></td></tr>");
		} catch (InsufficientPermissionException e) {
			body.append( "<div style=\"margin-top: 32px; margin-left: 32px;\">" );
			body.append( Html.getWarningDialog("Insufficient Permission", "You do not have permission to view the Access Control List Entry."));
			body.append("<br/><input style=\"margin-top: 8px; margin-left: 40px;\" onClick=\"javascript:window.close();\" class=\"button\" type=\"Button\" value=\"Close\">");
			body.append( "</div>" );
		}
		
		body.append("</table></form>");
		
		return new ContentDescriptor( "Access Control", body, false );
	}
	
	/**
	 * Get the a form that allows editing an ACL
	 * @param request
	 * @param response
	 * @param requestDescriptor
	 * @param httpMethod
	 * @param accessControl
	 * @param groupManagement
	 * @param userManagement
	 * @return
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws InvalidHtmlOperationException 
	 * @throws InvalidHtmlParameterException 
	 */
	private static ContentDescriptor getACLEditForm(WebConsoleConnectionDescriptor requestDescriptor, ApiAccessControl accessControl, ApiGroupManagement groupManagement, ApiUserManagement userManagement, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException{
		StringBuffer body = new StringBuffer();
		
		// 1 -- Output the start of the HTML table
		
		//	 1.1 -- Output the Header
		body.append("<form action=\"AccessControl\" method=\"post\"><table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" align=\"center\">");
		body.append( "<tr><td class=\"TopBottomBorder2\">&nbsp;</td></tr>" );
		body.append( "<tr><td class=\"TopBottomBorder2\"><div style=\"padding-top: 6px; padding-bottom: 6px; background-color: #FFFFFF;\"><div style=\"height: 36px; position:relative; left: 10px;\"><img src=\"/32_Lock\" alt=\"ACL\"><div style=\"position:relative; left: 40px; top: -32px;\"><div class=\"Text_2\">Access Control</div>View, modify the ACLs</div></div></div></td></tr>" );
		
		body.append( "<tr><td>&nbsp;" );
		body.append(Html.renderMessages(requestDescriptor.userId));
		body.append( "</td></tr>" );
		
		
		
		//	 1.2 -- Output the table start
		body.append("<tr><td>");
		body.append("<table width=\"100%\"><tr class=\"Background1\">");
		body.append("<td class=\"Text_2\">Name</td>");
		body.append("<td class=\"Text_2\">Operation</td></tr>");
		
		
		// 2 -- Parse the necessary parameters
		
		//	 2.1 -- Get the object ID
		long objectId = VALUE_UNDEFINED;
		if( requestDescriptor.request.getParameter("ObjectID") != null ){
			try{
				objectId = Long.parseLong( requestDescriptor.request.getParameter("ObjectID") );
			}
			catch (NumberFormatException e){
				objectId = VALUE_INVALID;
			}
		}
		
		//	 2.2 -- Get the user or group
		String subject = requestDescriptor.request.getParameter("Subject");
		int groupId = VALUE_UNDEFINED;
		int userId = VALUE_UNDEFINED;
		ObjectPermissionDescriptor objectPermissionDescriptor = null;
		boolean isEditing = false;
		
		//	 2.3 -- Get the existing ACL entry (if so requested)
		try{
		if( subject != null){
			isEditing = true;
			try{
				if( subject.startsWith("group") ){
					groupId = Integer.parseInt(subject.substring(5));
					objectPermissionDescriptor = accessControl.getGroupPermissions(requestDescriptor.sessionIdentifier,groupId, objectId);
					
				}
				else if( subject.startsWith("user") ){
					userId = Integer.parseInt(subject.substring(4));
					objectPermissionDescriptor = accessControl.getUserPermissions(requestDescriptor.sessionIdentifier, userId, objectId,false);
				}
			}
			catch(NumberFormatException e){
				throw new InvalidHtmlParameterException("Invalid Parameter", "The user or group identifier is invalid", "Console" );
			}
		}
		
		//	2.4 -- Post an error if the arguments are invalid
		if( objectId < 0){
			throw new InvalidHtmlParameterException("Invalid Parameter", "The object identifier is invalid", "Console" );
		}
		
		
		// 3 -- Output the content to view and modify the ACL
		
		//	 3.1 -- Create the list of users and groups
		body.append("<tr><td class=\"AlignedTop\"><table width=\"100%\">");
		
		//try{
		//		3.1.1 -- List of users
		SimpleUserDescriptor[] descriptors = userManagement.getSimpleUserDescriptors(requestDescriptor.sessionIdentifier);
		for( int c = 0; c < descriptors.length; c++){
			body.append("<tr><td width=\"3\"><input type=\"radio\" name=\"Subject\" value=\"user" + descriptors[c].getUserID() + "\"");
			
			if( descriptors[c].getUserID() == userId)
				body.append(" checked");
			
			if( isEditing )
				body.append(" disabled><td>");
			else
				body.append("><td>");
			
			if( !descriptors[c].isEnabled() )
				body.append("<td width=\"3\"><img src=\"/16_UserDisabled\" alt=\"user" + descriptors[c].getUserID() + "\">");
			else
				body.append("<td width=\"3\"><img src=\"/16_User\" alt=\"user" + descriptors[c].getUserID() + "\">");
			
			body.append("<td><td>"+descriptors[c].getUserName()+" (User)<td></tr>");
		}
		//}
		//catch( InsufficientPermissionException e){
		
		//}
		
		//try{
		//		3.1.2 -- List of groups
		SimpleGroupDescriptor[] groupDescriptors = groupManagement.getSimpleGroupDescriptors(requestDescriptor.sessionIdentifier);
		for( int c = 0; c < groupDescriptors.length; c++){
			body.append("<tr><td width=\"3\"><input type=\"radio\" name=\"Subject\" value=\"group" + groupDescriptors[c].getIdentifier() + "\"");
			
			if( groupDescriptors[c].getIdentifier() == groupId)
				body.append(" checked");
			
			if( isEditing )
				body.append(" disabled><td>");
			else
				body.append("><td>");
			
			if( !groupDescriptors[c].isEnabled())
				body.append("<td width=\"3\"><img src=\"/16_GroupDisabled\" alt=\"group" + groupDescriptors[c].getIdentifier() + "\">");
			else
				body.append("<td width=\"3\"><img src=\"/16_Group\" alt=\"group" + groupDescriptors[c].getIdentifier() + "\">");
			
			
			
			body.append("<td><td>"+groupDescriptors[c].getName()+" (Group)<td></tr>");
		}
		
		}
		catch( InsufficientPermissionException e){
			body.append("<p>");
			body.append(Html.getWarningDialog("Insufficient Permission", "You do not have permission to view and update the access control lists for this object", "Console", "Return to Main Dashboard"));
		}
		
		body.append("</table>");
		
		// 3.2 -- Create the list of operations
		body.append("<td class=\"AlignedTop\"><table>");
		
		AccessControlDescriptor.Action read = AccessControlDescriptor.Action.UNSPECIFIED;
		AccessControlDescriptor.Action write = AccessControlDescriptor.Action.UNSPECIFIED;
		AccessControlDescriptor.Action delete = AccessControlDescriptor.Action.UNSPECIFIED;
		AccessControlDescriptor.Action control = AccessControlDescriptor.Action.UNSPECIFIED;
		AccessControlDescriptor.Action execute = AccessControlDescriptor.Action.UNSPECIFIED;
		AccessControlDescriptor.Action create = AccessControlDescriptor.Action.UNSPECIFIED;
		
		if( objectPermissionDescriptor != null){
			read = objectPermissionDescriptor.getReadPermission();
			write = objectPermissionDescriptor.getModifyPermission();
			delete = objectPermissionDescriptor.getDeletePermission();
			control = objectPermissionDescriptor.getControlPermission();
			create = objectPermissionDescriptor.getCreatePermission();
			execute = objectPermissionDescriptor.getExecutePermission();
		}
		
		//	 3.2.1 -- Read operation
		body.append("<tr><td>Read:</td><td><select name=\"OperationRead\">");
		body.append(getOptions( read, requestDescriptor.request.getParameter("OperationRead")));
		
		//	 3.2.2 -- Write operation
		body.append("<tr><td>Modify:</td><td><select name=\"OperationModify\">");
		body.append(getOptions( write, requestDescriptor.request.getParameter("OperationModify")));
		
		//	 3.2.3 -- Execute operation
		body.append("<tr><td>Execute:</td><td><select name=\"OperationExecute\">");
		body.append(getOptions( execute, requestDescriptor.request.getParameter("OperationExecute")));
		
		//	 3.2.4 -- Delete operation
		body.append("<tr><td>Delete:</td><td><select name=\"OperationDelete\">");
		body.append(getOptions( delete, requestDescriptor.request.getParameter("OperationDelete")));
		
		//	 3.2.5 -- Control operation
		body.append("<tr><td>Control:</td><td><select name=\"OperationControl\">");
		body.append(getOptions( control, requestDescriptor.request.getParameter("OperationControl")));
		
		//	 3.2.6 -- Create operation
		body.append("<tr><td>Create:</td><td><select name=\"OperationCreate\">");
		body.append(getOptions( create, requestDescriptor.request.getParameter("OperationCreate")));
		
		body.append("</table></td></tr>");
		
		body.append("</table></td></tr>");
		
		// Output the end of the table
		body.append("<tr class=\"Background3\"><td align=\"Right\" colspan=\"7\"><input type=\"hidden\" name=\"ObjectID\" value=\"" + objectId + "\"><input class=\"button\" type=\"Submit\" value=\"Apply Changes\" name=\"Apply\">&nbsp;&nbsp;&nbsp;<input class=\"button\" type=\"Submit\" value=\"Cancel\" name=\"Cancel\">&nbsp;&nbsp;&nbsp;<input class=\"button\" onClick=\"javascript:window.close();\" type=\"Button\" value=\"Close\">");
		
		if(isEditing && groupId != VALUE_UNDEFINED)
			body.append("<input type=\"hidden\" name=\"Subject\" value=\"group" + groupId + "\"><input type=\"hidden\" name=\"Action\" value=\"Edit\">");
		else if(isEditing && userId != VALUE_UNDEFINED)
			body.append("<input type=\"hidden\" name=\"Subject\" value=\"user" + userId + "\"><input type=\"hidden\" name=\"Action\" value=\"Edit\">");
		else
			body.append("<input type=\"hidden\" name=\"Action\" value=\"New\">");
		
		body.append("</td></tr></table></tr></td></table></form>");
		
		return new ContentDescriptor( "Access Control", body, false );
	}
	
	private static AccessControlDescriptor.Action convertPermissionFromString(String permissionDescription){
		
		if( permissionDescription == null )
			return AccessControlDescriptor.Action.UNSPECIFIED;
		
		if( permissionDescription.equals("Allow"))
			return AccessControlDescriptor.Action.PERMIT;
		else if( permissionDescription.equals("Deny"))
			return AccessControlDescriptor.Action.DENY;
		else //if( permissionDescription.equals("Undefined"))
			return AccessControlDescriptor.Action.UNSPECIFIED;
			
	}
	
	private static StringBuffer getOptions( AccessControlDescriptor.Action aclValue, String argument ){
		StringBuffer body = new StringBuffer();
		
		AccessControlDescriptor.Action aclType = aclValue;
		
		if( argument != null ){
			if( argument.equals("Allow"))
				aclType = AccessControlDescriptor.Action.PERMIT;
			else if( argument.equals("Deny"))
				aclType = AccessControlDescriptor.Action.DENY;
			else if( argument.equals("Undefined"))
				aclType = AccessControlDescriptor.Action.UNSPECIFIED;
		}
		
		if( aclType == AccessControlDescriptor.Action.DENY){
			body.append("<option value=\"Allow\">Allow");
			body.append("<option value=\"Deny\" selected>Deny");
			body.append("<option value=\"Undefined\">Undefined</select></td></tr>");
		}
		else if( aclType == AccessControlDescriptor.Action.PERMIT){
			body.append("<option value=\"Allow\" selected>Allow");
			body.append("<option value=\"Deny\">Deny");
			body.append("<option value=\"Undefined\">Undefined</select></td></tr>");
		}
		else if( aclType == AccessControlDescriptor.Action.UNSPECIFIED){
			body.append("<option value=\"Allow\">Allow");
			body.append("<option value=\"Deny\">Deny");
			body.append("<option value=\"Undefined\" selected>Undefined</select></td></tr>");
		}
		
		return body;
	}
	
	private static StringBuffer createRow( ObjectPermissionDescriptor objectPermissionDescriptor, ApiUserManagement userManagement, ApiGroupManagement groupManagement, WebConsoleConnectionDescriptor requestDesc ) throws NoSessionException, GeneralizedException, InsufficientPermissionException, NotFoundException{
		StringBuffer buffer = new StringBuffer();
		
		// 1 -- Resolve the user or group information
		SimpleUserDescriptor userDesc = null;
		SimpleGroupDescriptor groupDesc = null;
		
		//	 1.1 -- Determine if the user can view 
		
		if( objectPermissionDescriptor.isGroup() )
			groupDesc = groupManagement.getSimpleGroupDescriptor(requestDesc.sessionIdentifier, objectPermissionDescriptor.getSubjectId());
		else
			userDesc = userManagement.getSimpleUserDescriptor(requestDesc.sessionIdentifier, objectPermissionDescriptor.getSubjectId());
		
		// 2 -- Print out the row
		
		//	2.1 -- Print icon
		if( objectPermissionDescriptor.isGroup() ){
			if( groupDesc.isEnabled() ){
				buffer.append("<tr class=\"Background1\"><td><table><tr><td><img alt=\"Group_Active\" src=\"/16_Group\"></td>");
			}
			else{
				buffer.append("<tr class=\"Background1\"><td><table><tr><td><img alt=\"Group_Disabled\" src=\"/16_GroupDisabled\"></td>");
			}
		}
		else{
			if( userDesc.isEnabled() ){
				buffer.append("<tr class=\"Background1\"><td><table><tr><td><img alt=\"User_Active\" src=\"/16_User\"></td>");
			}
			else{
				buffer.append("<tr class=\"Background1\"><td><table><tr><td><img alt=\"User_Disabled\" src=\"/16_UserDisabled\"></td>");
			}
		}
		
		//	2.2 -- Print name and identifier
		if( objectPermissionDescriptor.isGroup() ){
			buffer.append("<td>" + groupDesc.getName() + "</td></tr></table>");
			buffer.append("<td>Group " +objectPermissionDescriptor.getSubjectId() + "</td>");
		}
		else{
			buffer.append("<td>" + userDesc.getUserName() + "</td></tr></table>");
			buffer.append("<td>User " +objectPermissionDescriptor.getSubjectId() + "</td>");
		}

		//	2.3 -- Print read
		buffer.append( getActionCell( objectPermissionDescriptor.getReadPermission(), "Read" ) );
		
		//	2.4 -- Print modify
		buffer.append( getActionCell( objectPermissionDescriptor.getModifyPermission(), "Modify" ) );
		
		//	2.5 -- Print delete
		buffer.append( getActionCell( objectPermissionDescriptor.getDeletePermission(), "Delete" ) );
		
		//	2.6 -- Print execute
		buffer.append( getActionCell( objectPermissionDescriptor.getExecutePermission(), "Execute" ) );
		
		//	2.7 -- Print control
		buffer.append( getActionCell( objectPermissionDescriptor.getControlPermission(), "Control" ) );
		
		//	2.8 -- Print create
		buffer.append( getActionCell( objectPermissionDescriptor.getCreatePermission(), "Create" ) );
		
		//	2.9 -- Print the edit button
		if( objectPermissionDescriptor.getSubjectType() == AccessControlDescriptor.Subject.USER)
			buffer.append("<td><table><tr><td><img class=\"imagebutton\" alt=\"edit\" src=\"/16_Configure\"></td><td><a href=\"AccessControl?Action=Edit&Subject=user" + objectPermissionDescriptor.getSubjectId() + "&ObjectID=" + objectPermissionDescriptor.getObjectId() + "\">Edit</a></td></tr></table></td>");
		else
			buffer.append("<td><table><tr><td><img class=\"imagebutton\" alt=\"edit\" src=\"/16_Configure\"></td><td><a href=\"AccessControl?Action=Edit&Subject=group" + objectPermissionDescriptor.getSubjectId() + "&ObjectID=" + objectPermissionDescriptor.getObjectId() + "\">Edit</a></td></tr></table></td>");
		
		//	2.10 -- Print the delete button
		if( objectPermissionDescriptor.getSubjectType() == AccessControlDescriptor.Subject.USER)
			buffer.append("<td><table><tr><td><img class=\"imagebutton\" alt=\"delete\" src=\"/16_Delete\"></td><td><a href=\"AccessControl?Action=Delete&Subject=user" + objectPermissionDescriptor.getSubjectId() + "&ObjectID=" + objectPermissionDescriptor.getObjectId() + "\">Delete</a></td></tr></table></td>");
		else
			buffer.append("<td><table><tr><td><img class=\"imagebutton\" alt=\"delete\" src=\"/16_Delete\"></td><td><a href=\"AccessControl?Action=Delete&Subject=group" + objectPermissionDescriptor.getSubjectId() + "&ObjectID=" + objectPermissionDescriptor.getObjectId() + "\">Delete</a></td></tr></table></td>");
		
		// end the row
		buffer.append("</tr>");
		
		return buffer;
	}
	
	private static String getActionCell( AccessControlDescriptor.Action indicator, String permissionType ){
		
		if( indicator == AccessControlDescriptor.Action.PERMIT )
			return "<td><table><tr><td><img src=\"/16_up\" alt=\"" + permissionType + "\"></td><td>Allow</td></tr></table></td>";
		else if( indicator == AccessControlDescriptor.Action.DENY )
			return "<td><table><tr><td><img src=\"/16_down\" alt=\"" + permissionType + "\"></td><td>Deny</td></tr></table></td>";
		else
			return "<td>&nbsp;</td>";
	}
	
	private static ActionDescriptor performAction(WebConsoleConnectionDescriptor requestDescriptor, ApiAccessControl accessControl) throws GeneralizedException, NoSessionException{
		
		//TODO Modify simple descriptors to throw exception unless connected to a valid object ID with control permissions
		
		long objectId = VALUE_UNDEFINED;
		
		if(requestDescriptor.request.getParameter("ObjectID") != null){
			try{
				objectId = Long.parseLong(requestDescriptor.request.getParameter("ObjectID"));
			}
			catch(NumberFormatException e){
				objectId = VALUE_INVALID;
			}
		}
		
		boolean cancelSet = requestDescriptor.request.getParameter("Cancel") != null;
		
		if( cancelSet ){
			return new ActionDescriptor(ActionDescriptor.OP_VIEW);
		}
		
		
		// 2 -- Is editing an existing ACL entry
		if( requestDescriptor.request.getParameter("Action") != null && requestDescriptor.request.getParameter("Action").equals("Edit")){
			String subject = requestDescriptor.request.getParameter("Subject");
			
			if( cancelSet ){
				return new ActionDescriptor(ActionDescriptor.OP_UPDATE);
			}
			
			// 2.1 -- Determine if the subject is valid
			if(objectId == VALUE_INVALID){
				Html.addMessage(MessageType.WARNING, "The object identifier is invalid", requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE);
			}
			
			if( requestDescriptor.request.getParameter("OperationRead") != null && requestDescriptor.request.getParameter("OperationModify") != null 
					&& requestDescriptor.request.getParameter("OperationControl") != null && requestDescriptor.request.getParameter("OperationExecute") != null 
					&& requestDescriptor.request.getParameter("OperationCreate") != null && requestDescriptor.request.getParameter("OperationDelete") != null ){
				
				// 2.2 -- Populate the permissions defined
				AccessControlDescriptor.Action read = convertPermissionFromString(requestDescriptor.request.getParameter("OperationRead"));
				AccessControlDescriptor.Action modify = convertPermissionFromString(requestDescriptor.request.getParameter("OperationModify"));
				AccessControlDescriptor.Action control = convertPermissionFromString(requestDescriptor.request.getParameter("OperationControl"));
				AccessControlDescriptor.Action execute = convertPermissionFromString(requestDescriptor.request.getParameter("OperationExecute"));
				AccessControlDescriptor.Action create = convertPermissionFromString(requestDescriptor.request.getParameter("OperationCreate"));
				AccessControlDescriptor.Action delete = convertPermissionFromString(requestDescriptor.request.getParameter("OperationDelete"));
				
				// 2.3 -- Determine if the subject is a group or a user
				try{
					if( subject.startsWith("group") ){
						int groupId;
						groupId = Integer.parseInt(subject.substring(5));
						ObjectPermissionDescriptor objectPermissionDesc = new ObjectPermissionDescriptor(read, modify, create, execute, delete, control, AccessControlDescriptor.Subject.GROUP, groupId, objectId );
						
						try{
							accessControl.setPermissions(requestDescriptor.sessionIdentifier, objectPermissionDesc);
						}
						catch(InsufficientPermissionException e){
							Html.addMessage(MessageType.WARNING, "You do not have permission to update the access control list entry", requestDescriptor.userId.longValue());
							return new ActionDescriptor(ActionDescriptor.OP_UPDATE_FAILED);
						}
						
						Html.addMessage(MessageType.INFORMATIONAL, "Access control list entry successfully updated", requestDescriptor.userId.longValue());
						return new ActionDescriptor(ActionDescriptor.OP_UPDATE_SUCCESS, Long.valueOf( objectId ));
					}else if( subject.startsWith("user") ){
						int userId;
						userId = Integer.parseInt(subject.substring(4));
						
						ObjectPermissionDescriptor objectPermissionDesc = new ObjectPermissionDescriptor(read, modify, create, execute, delete, control, AccessControlDescriptor.Subject.USER, userId, objectId );
						try{
							accessControl.setPermissions(requestDescriptor.sessionIdentifier, objectPermissionDesc);
						}
						catch(InsufficientPermissionException e){
							Html.addMessage(MessageType.WARNING, "You do not have permission to update the access control list entry", requestDescriptor.userId.longValue());
							return new ActionDescriptor(ActionDescriptor.OP_UPDATE_FAILED);
						}
						
						Html.addMessage(MessageType.INFORMATIONAL, "Access control list entry successfully updated", requestDescriptor.userId.longValue());
						return new ActionDescriptor(ActionDescriptor.OP_UPDATE_SUCCESS, Long.valueOf( objectId ));
					}
					else{
						Html.addMessage(MessageType.WARNING, "A user or group was not selected", requestDescriptor.userId.longValue());
						return new ActionDescriptor( OP_SUBJECT_INVALID);
					}
				}
				catch(NumberFormatException e){
					Html.addMessage(MessageType.WARNING, "A user or group was not selected", requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_SUBJECT_INVALID );
				}
			}
			
			return new ActionDescriptor(ActionDescriptor.OP_UPDATE);
		}
		
		
		// 3 -- Defining a new entry
		else if( requestDescriptor.request.getParameter("New") != null || (requestDescriptor.request.getParameter("Action") != null && requestDescriptor.request.getParameter("Action").equals("New"))){
			String subject = requestDescriptor.request.getParameter("Subject");
			
			// 3.1 -- Determine if a subject is provided
			if(subject == null ){
				if( requestDescriptor.request.getParameter("Apply") != null ){
					Html.addMessage(MessageType.WARNING, "Please select the user or group to modify", requestDescriptor.userId.longValue());
				}
				
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE );
			}
			else if( cancelSet ){
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE );
			}
			
			// 3.2 -- Populate the permissions defined
			AccessControlDescriptor.Action read = convertPermissionFromString(requestDescriptor.request.getParameter("OperationRead"));
			AccessControlDescriptor.Action modify = convertPermissionFromString(requestDescriptor.request.getParameter("OperationModify"));
			AccessControlDescriptor.Action control = convertPermissionFromString(requestDescriptor.request.getParameter("OperationControl"));
			AccessControlDescriptor.Action execute = convertPermissionFromString(requestDescriptor.request.getParameter("OperationExecute"));
			AccessControlDescriptor.Action create = convertPermissionFromString(requestDescriptor.request.getParameter("OperationCreate"));
			AccessControlDescriptor.Action delete = convertPermissionFromString(requestDescriptor.request.getParameter("OperationDelete"));
			
			// 3.3 -- Determine if the subject is a group or a user
			try{
			if( subject.startsWith("group") ){
				int groupId;
				groupId = Integer.parseInt(subject.substring(5));
				ObjectPermissionDescriptor objectPermissionDesc = new ObjectPermissionDescriptor(read, modify, create, execute, delete, control, AccessControlDescriptor.Subject.GROUP, groupId, objectId );
				
				try{
					accessControl.setPermissions(requestDescriptor.sessionIdentifier, objectPermissionDesc);
				}
				catch(InsufficientPermissionException e){
					Html.addMessage(MessageType.WARNING, "You do not have permission to update the access control list entry", requestDescriptor.userId.longValue());
					return new ActionDescriptor(ActionDescriptor.OP_UPDATE_FAILED);
				}
				
				Html.addMessage(MessageType.INFORMATIONAL, "Access control list entry successfully updated", requestDescriptor.userId.longValue());
				return new ActionDescriptor(ActionDescriptor.OP_UPDATE_SUCCESS, Long.valueOf(objectId));
			}else if( subject.startsWith("user") ){
				int userId;
				userId = Integer.parseInt(subject.substring(4));
				
				ObjectPermissionDescriptor objectPermissionDesc = new ObjectPermissionDescriptor(read, modify, create, execute, delete, control, AccessControlDescriptor.Subject.USER, userId, objectId );
				try{
					accessControl.setPermissions(requestDescriptor.sessionIdentifier, objectPermissionDesc);
				}
				catch(InsufficientPermissionException e){
					Html.addMessage(MessageType.WARNING, "You do not have permission to update the access control list entry", requestDescriptor.userId.longValue());
					return new ActionDescriptor(ActionDescriptor.OP_UPDATE_FAILED);
				}
				
				Html.addMessage(MessageType.INFORMATIONAL, "Access control list entry successfully updated", requestDescriptor.userId.longValue());
				return new ActionDescriptor(ActionDescriptor.OP_UPDATE_SUCCESS, new Long(objectId));
			}
			else{
				Html.addMessage(MessageType.WARNING, "A user or group was not selected", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_SUBJECT_INVALID );
			}
			}
			catch(NumberFormatException e){
				Html.addMessage(MessageType.WARNING, "A user or group was not selected", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_SUBJECT_INVALID );
			}
		}
		
		
		// 4 -- Deleting an entry 
		else if( requestDescriptor.request.getParameter("Action") != null && requestDescriptor.request.getParameter("Action").equals("Delete")){
			String subject = requestDescriptor.request.getParameter("Subject");
			
			if( subject == null){
				Html.addMessage(MessageType.WARNING, "A valid user or group identifier was not provided", requestDescriptor.userId.longValue());
				return new ActionDescriptor(ActionDescriptor.OP_DELETE_FAILED);
			}
			
			if( subject.startsWith("group") ){
				long groupId;
				groupId = Long.parseLong(subject.substring(5));
				
				try{
					accessControl.deleteGroupPermissions(requestDescriptor.sessionIdentifier, groupId, objectId);
				}
				catch(InsufficientPermissionException e){
					Html.addMessage(MessageType.WARNING, "You do not have permission to update the access control list entry", requestDescriptor.userId.longValue());
					return new ActionDescriptor(ActionDescriptor.OP_DELETE_FAILED);
				}
				
				Html.addMessage(MessageType.INFORMATIONAL, "Access control list entry deleted", requestDescriptor.userId.longValue());
				return new ActionDescriptor(ActionDescriptor.OP_DELETE_SUCCESS, new Long(objectId));
			}else if( subject.startsWith("user") ){
				long userId;
				userId = Long.parseLong(subject.substring(4));
				
				try{
					accessControl.deleteUserPermissions(requestDescriptor.sessionIdentifier, userId, objectId);
				}
				catch(InsufficientPermissionException e){
					Html.addMessage(MessageType.WARNING, "You do not have permission to update the access control list entry", requestDescriptor.userId.longValue());
					return new ActionDescriptor(ActionDescriptor.OP_DELETE_FAILED);
				}
			}
			
			Html.addMessage(MessageType.INFORMATIONAL, "Access control list entry deleted", requestDescriptor.userId.longValue());
			return new ActionDescriptor( ActionDescriptor.OP_DELETE_SUCCESS );
		}
		else
			return new ActionDescriptor( ActionDescriptor.OP_VIEW, new Long(objectId) );
	}
}
