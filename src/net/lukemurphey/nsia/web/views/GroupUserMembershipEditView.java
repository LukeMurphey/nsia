package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
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
import net.lukemurphey.nsia.UserManagement.UserDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class GroupUserMembershipEditView extends View {

	public final static String VIEW_NAME = "group_edit_membership";
	
	public GroupUserMembershipEditView() {
		super("Group/Edit/Membership", VIEW_NAME, Pattern.compile("[0-9]+"));
	}

	public static String getURL( UserDescriptor user ) throws URLInvalidException{
		GroupUserMembershipEditView view = new GroupUserMembershipEditView();
		
		return view.createURL(user.getUserID());
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {

		// 1 -- Get the group
		int groupID;
		GroupManagement groupMgmt = new GroupManagement(Application.getApplication());
		GroupDescriptor group = null;
		
		try{
			groupID = Integer.valueOf( args[0] );
			group = groupMgmt.getGroupDescriptor(groupID);
		} catch(NumberFormatException e){
			Dialog.getDialog(response, context, data, "Group ID is not valid", "Group ID Invalid", DialogType.WARNING);
			return true;
		}  catch(NotFoundException e){
			Dialog.getDialog(response, context, data, "No group was found with the given ID", "Group Not Found", DialogType.WARNING);
			return true;
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		} catch (InputValidationException e) {
			throw new ViewFailedException(e);
		}
		
		// 2 -- Check permissions
		try {
			if( Shortcuts.hasRight( context.getSessionInfo(), "Groups.Membership.Edit", "Edit group membership for group ID " + group.getGroupId() + " (" + group.getGroupName() + ")") == false ){
				context.addMessage("You do not have permission to update the group membership for user accounts", MessageSeverity.WARNING);
				response.sendRedirect( GroupEditView.getURL(group) );
				return true;
			}
		} catch (GeneralizedException e) {
			throw new ViewFailedException(e);
		}
		
		// 3 -- Get the groups included into the list and add or remove from the account as necessary
		String includedUserString = request.getParameter("IncludedUsers");
		
		// Don't bother continuing if the included user string is null
		if( includedUserString == null ){
			response.sendRedirect( GroupView.getURL(group) );
			return true;
		}
		
		String[] includedUsers = includedUserString.split(",");
		GroupManagement groupManager = new GroupManagement(Application.getApplication());
		UserManagement userMgmt = new UserManagement(Application.getApplication());
		boolean updated = false;
		
		// Determine the desired operation for each user included
		for( int c = 0; c < includedUsers.length; c++ ){
			int userId = -1;
			
			try{
				userId = Integer.parseInt( includedUsers[c] );
			}catch(NumberFormatException e){
				//Do nothing, the number is not a valid format and will be skipped
			}
			
			// Determine if the associated check was marked (indicating the command to add membership) or unmarked (no membership); then adjust as necessary
			try{
				if( userId >= 0 ){
					
					UserDescriptor user = userMgmt.getUserDescriptor(userId);
					
					if( request.getParameter( includedUsers[c] ) != null ){
						groupManager.addUserToGroup( userId, group.getGroupId() );
						updated = true;
						Application.getApplication().logEvent(EventLogMessage.EventType.USER_ADDED_TO_GROUP, new EventLogField[]{
								new EventLogField( EventLogField.FieldName.GROUP_ID, group.getGroupId() ),
								new EventLogField( EventLogField.FieldName.TARGET_USER_ID, user.getUserID() ),
								new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, context.getSessionInfo().getUserName() ),
								new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, context.getSessionInfo().getUserId() )} );
						
					}
					else{
						groupManager.removeUserFromGroup( user.getUserID(), group.getGroupId() );
						updated = true;
						Application.getApplication().logEvent(EventLogMessage.EventType.USER_REMOVED_FROM_GROUP, new EventLogField[]{
								new EventLogField( EventLogField.FieldName.GROUP_ID, group.getGroupId() ),
								new EventLogField( EventLogField.FieldName.TARGET_USER_ID, user.getUserID() ),
								new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, context.getSessionInfo().getUserName() ),
								new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, context.getSessionInfo().getUserId() )});
					}
				}
			} catch (SQLException e) {
				throw new ViewFailedException(e);
			} catch (NotFoundException e) {
				throw new ViewFailedException(e);
			} catch (NoDatabaseConnectionException e) {
				throw new ViewFailedException(e);
			}
		}
		
		// Add a message if updates were performed
		if( updated ){
			context.addMessage("Group membership updated", MessageSeverity.SUCCESS);
		}
		
		// 4 -- Redirect to the group view
		response.sendRedirect( GroupView.getURL(group) );
		return true;
	}

}
