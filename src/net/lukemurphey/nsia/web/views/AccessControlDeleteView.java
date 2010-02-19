package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.AccessControl;
import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class AccessControlDeleteView extends View {

	public static final String VIEW_NAME = "access_control_delete";
	
	public AccessControlDeleteView() {
		//super("AccessControl/Delete", VIEW_NAME, Pattern.compile("[0-9]+"));
		super("AccessControl/Delete", VIEW_NAME, Pattern.compile("[0-9]+"), Pattern.compile("(User|Group)?", Pattern.CASE_INSENSITIVE), Pattern.compile("[0-9]*"));
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {
		
		try{
			// 0 -- Check permissions
			//TODO Check rights
			
			// 1 -- Get the relevant fields
			long objectId = -1;
			long subjectID = -1;
			if( args.length > 0){
				
				// 1.1 -- Get the object ID
				try{
					objectId = Long.parseLong( args[0] );
				}
				catch (NumberFormatException e){
					Dialog.getDialog(response, context, data, "The ACL identifier provided is not valid", "ACL Identifier Not Valid", DialogType.WARNING);
					return true;
				}
				
				// 1.2 -- Get the subject ID
				try{
					subjectID = Long.parseLong( args[2] );
				}
				catch (NumberFormatException e){
					Dialog.getDialog(response, context, data, "The subject identifier provided is not valid", "Subject Identifier Not Valid", DialogType.WARNING);
					return true;
				}
			}
		        
			// 2 -- Delete the entry
			AccessControl accessControl = new AccessControl(Application.getApplication());
			
			if( "User".equalsIgnoreCase( args[1] ) ){
				accessControl.deleteUserPermissions(subjectID, objectId);
				context.addMessage("ACL successfully deleted", MessageSeverity.SUCCESS);
				response.sendRedirect(AccessControlView.getURL(objectId));
				return true;
			}
			else{
				accessControl.deleteGroupPermissions(subjectID, objectId);
				context.addMessage("ACL successfully deleted", MessageSeverity.SUCCESS);
				response.sendRedirect(AccessControlView.getURL(objectId));
				return true;
			}
		}
		catch(SQLException e){
			throw new ViewFailedException(e);
		}
		catch(NoDatabaseConnectionException e){
			throw new ViewFailedException(e);
		}
	}

}
