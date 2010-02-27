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
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.UserManagement;
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

public class UserGroupMembershipEditView extends View {

	public final static String VIEW_NAME = "user_edit_membership";
	
	public UserGroupMembershipEditView() {
		super("User/Edit/Membership", VIEW_NAME, Pattern.compile("[0-9]+"));
	}

	public static String getURL( UserDescriptor user ) throws URLInvalidException{
		UserGroupMembershipEditView view = new UserGroupMembershipEditView();
		
		return view.createURL(user.getUserID());
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {

		// 1 -- Get the user
		int userID;
		UserManagement userMgmt = new UserManagement(Application.getApplication());
		UserDescriptor user = null;
		
		try{
			userID = Integer.valueOf( args[0] );
			user = userMgmt.getUserDescriptor(userID);
		} catch(NumberFormatException e){
			Dialog.getDialog(response, context, data, "User ID is not valid", "User ID Invalid", DialogType.WARNING);
			return true;
		}  catch(NotFoundException e){
			Dialog.getDialog(response, context, data, "No user was found with the given ID", "User Not Found", DialogType.WARNING);
			return true;
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		}
		
		// 2 -- Check permissions
		try {
			if( Shortcuts.hasRight( context.getSessionInfo(), "Groups.Membership.Edit", "Edit group membership for user ID " + user.getUserID() + " (" + user.getUserName() + ")") == false ){
				context.addMessage("You do not have permission to update the group membership for user accounts", MessageSeverity.WARNING);
				response.sendRedirect( UserView.getURL(user) );
				return true;
			}
		} catch (GeneralizedException e) {
			throw new ViewFailedException(e);
		}
		
		// 3 -- Get the groups included into the list and add or remove from the account as necessary
		String includedGroupString = request.getParameter("IncludedGroups");
		String[] includedGroups = includedGroupString.split(",");
		GroupManagement groupManager = new GroupManagement(Application.getApplication());
		boolean updated = false;
		
		// Determine the desired operation for each group included
		for( int c = 0; c < includedGroups.length; c++ ){
			int groupId = -1;
			
			try{
				groupId = Integer.parseInt( includedGroups[c] );
			}catch(NumberFormatException e){
				//Do nothing, the number is not a valid format and will be skipped
			}
			
			// Determine if the associated check was marked (indicating the command to add membership) or unmarked (no membership); then adjust as necessary
			try{
				if( groupId >= 0 ){
					
					if( request.getParameter( includedGroups[c] ) != null ){
						groupManager.addUserToGroup( userID, groupId );
						updated = true;
						Application.getApplication().logEvent(EventLogMessage.Category.ACCESS_CONTROL_ENTRY_SET, new EventLogField[]{
								new EventLogField( EventLogField.FieldName.GROUP_ID, groupId ),
								new EventLogField( EventLogField.FieldName.TARGET_USER_ID, user.getUserID() ),
								new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, context.getSessionInfo().getUserName() ),
								new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, context.getSessionInfo().getUserId() )} );
						
					}
					else{
						groupManager.removeUserFromGroup( userID, groupId );
						updated = true;
						Application.getApplication().logEvent(EventLogMessage.Category.USER_REMOVED_FROM_GROUP, new EventLogField[]{
								new EventLogField( EventLogField.FieldName.GROUP_ID, groupId ),
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
		
		// 4 -- Redirect to the user view
		response.sendRedirect( UserView.getURL(user) );
		return true;
	}

}
