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
import net.lukemurphey.nsia.GroupManagement.GroupDescriptor;
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

public class GroupEnableView extends View {

	public static final String VIEW_NAME = "group_enable";
	
	public GroupEnableView() {
		super("Group/Enable", VIEW_NAME, Pattern.compile("[0-9]+"));
	}

	public static String getURL( GroupDescriptor group ) throws URLInvalidException{
		GroupEnableView view = new GroupEnableView();
		
		return view.createURL(group.getGroupId());
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {

		// 1 -- Check permissions
		try {
			if( Shortcuts.hasRight( context.getSessionInfo(), "Groups.Edit", "Enable user group") == false ){
				context.addMessage("You do not have permission to enable groups", MessageSeverity.WARNING);
				response.sendRedirect( GroupListView.getURL() );
				return true;
			}
		} catch (GeneralizedException e) {
			throw new ViewFailedException(e);
		}
		
		// 2 -- Get the group if one exists
		if( args.length >= 1 ){
			
			// 2.1 -- Get the group ID
			int groupID;
			
			try{
				groupID = Integer.valueOf( args[0] );
			}
			catch( NumberFormatException e ){
				Dialog.getDialog(response, context, data, "The Group ID provided is not valid", "Group ID Invalid", DialogType.WARNING);
				return true;
			}
			
			// 2.2 -- Enable the group
			GroupManagement groupMgmt = new GroupManagement(Application.getApplication());
			
			try{
				if( groupMgmt.enableGroup(groupID) ){
					context.addMessage("Group successfully enabled", MessageSeverity.SUCCESS);
					
					Application.getApplication().logEvent(EventLogMessage.Category.GROUP_REENABLED, new EventLogField[]{
							new EventLogField( EventLogField.FieldName.GROUP_ID, groupID ),
							new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, context.getUser().getUserName() ),
							new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, context.getUser().getUserID() )} );
					
					response.sendRedirect(GroupEditView.getURL(groupID));
					return true;
				}
				else{
					
					Application.getApplication().logEvent(EventLogMessage.Category.GROUP_ID_INVALID, new EventLogField[]{
							new EventLogField( EventLogField.FieldName.GROUP_ID, groupID ),
							new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, context.getUser().getUserName() ),
							new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, context.getUser().getUserID() )} );
					
					Dialog.getDialog(response, context, data, "No group was found with the given ID", "Group Not Found", DialogType.WARNING);
					return true;
				}
			} catch (SQLException e) {
				throw new ViewFailedException(e);
			} catch (NoDatabaseConnectionException e) {
				throw new ViewFailedException(e);
			}
		}
		
		return true;
		
	}

}