package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.NumericalOverflowException;
import net.lukemurphey.nsia.UserManagement;
import net.lukemurphey.nsia.UserManagement.UserDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class UserUnlockView extends View {

	public static final String VIEW_NAME = "user_unlock";
	
	public UserUnlockView() {
		super("User/Unlock", VIEW_NAME, Pattern.compile("[0-9]+"));
	}

	public static String getURL( UserDescriptor user ) throws URLInvalidException{
		UserUnlockView view = new UserUnlockView();
		return view.createURL(user.getUserID());
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {

		// 0 -- CHeck permissions
		//checkRight( sessionIdentifier, "Users.Unlock");
		//TODO Check rights
		
		// 1 -- Get the user
		int userID;
		UserManagement userMgmt = new UserManagement(Application.getApplication());
		UserDescriptor user = null;
		
		try{
			userID = Integer.valueOf( args[0] );
			user = userMgmt.getUserDescriptor(userID);
		} catch(NumberFormatException e){
			Dialog.getDialog(response, context, data, "The given user ID is invalid", "User ID Invalid", DialogType.WARNING);
			return true;
		} catch(NotFoundException e){
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
		
		// 2 -- Clear the authentication failed count for the user
		try {
			userMgmt.clearAuthFailedCount(user);
			
			Application.getApplication().logEvent(EventLogMessage.Category.USER_NAME_UNLOCKED,
					new EventLogField( FieldName.TARGET_USER_NAME, user.getUserName()),
					new EventLogField( FieldName.TARGET_USER_ID, user.getUserID()),
					new EventLogField( FieldName.SOURCE_USER_NAME, context.getUser().getUserName() ),
					new EventLogField( FieldName.SOURCE_USER_ID, context.getUser().getUserID() )
					);
			
			context.addMessage("User account unlocked", MessageSeverity.SUCCESS);
			
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (NumericalOverflowException e) {
			throw new ViewFailedException(e);
		} catch (InputValidationException e) {
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		}
		
		// 3 -- Redirect
		response.sendRedirect( UserView.getURL(user) );
		return true;
	}

}
