package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.UserManagement;
import net.lukemurphey.nsia.UserManagement.UserDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class UserEnableView extends View {

	public UserEnableView() {
		super("User/Enable", "user_enable", Pattern.compile("[0-9]+", Pattern.CASE_INSENSITIVE));
	}

	public static String getURL( UserDescriptor user ) throws URLInvalidException{
		UserEnableView view = new UserEnableView();
		
		return view.createURL(user.getUserID());
	}
	
	public boolean enableUser( RequestContext context, int userID ) throws ViewFailedException{
		
		Application app = Application.getApplication();
		
		try{

			UserManagement userManagement = new UserManagement(Application.getApplication());
			
			// 1 -- Enable the account
			if( userManagement.enableAccount( userID ) ){
				
				app.logEvent(EventLogMessage.Category.USER_REENABLED,
						new EventLogField( FieldName.TARGET_USER_ID, userID ),
						new EventLogField( FieldName.SOURCE_USER_NAME, context.getUser().getUserName() ),
						new EventLogField( FieldName.SOURCE_USER_ID, context.getUser().getUserID() )
						);
				
				return true;
			}
			else{
				
				app.logEvent(EventLogMessage.Category.USER_ID_INVALID,
						new EventLogField( FieldName.TARGET_USER_ID, userID ),
						new EventLogField( FieldName.SOURCE_USER_NAME, context.getUser().getUserName() ),
						new EventLogField( FieldName.SOURCE_USER_ID, context.getUser().getUserID() )
						);
				
				return true;
			}
		}catch (SQLException e){
			app.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new ViewFailedException(e);
		}catch (NoDatabaseConnectionException e) {
			app.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e );
			throw new ViewFailedException(e);
		}catch (InputValidationException e) {
			app.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new ViewFailedException(e);
		} /*catch (NotFoundException e) {
			app.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new ViewFailedException(e);
		} catch (InsufficientPermissionException e) {
			app.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new ViewFailedException(e);
		} catch (GeneralizedException e) {
			app.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new ViewFailedException(e);
		} catch (NoSessionException e) {
			app.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new ViewFailedException(e);
		}*/
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {
		
		// 1 -- Make sure the argument for the user to enable is provided and is the correct type
		int userID = -1;
		
		//	 1.1 -- Make sure an ID was provided
		if( args.length <= 0 ){
			//Show a dialog indicating that the user ID provided was not provided
			Dialog.getDialog(response, context, data, "The User ID was not provided.", "User ID Invalid", DialogType.WARNING);
			return true;
		}
		
		//	 1.2 -- Make sure the ID is the correct format
		else{
			//Show a dialog indicating that the user ID provided was not valid
			try{
				userID = Integer.valueOf(args[0]);
			}
			catch(NumberFormatException e){
				Dialog.getDialog(response, context, data, "The User ID provided is invalid.", "User ID Invalid", DialogType.WARNING);
				return true;
			}
		}
		
		// 2 -- Check the user's permissions
		try {
			if( Shortcuts.hasRight( context.getSessionInfo(), "Users.Edit", "Enable user ID " + userID) == false ){
				context.addMessage("You do not have permission to enable users", MessageSeverity.WARNING);
				response.sendRedirect( UserView.getURL(userID) );
				return true;
			}
		} catch (GeneralizedException e) {
			throw new ViewFailedException(e);
		}
		
		// 3 -- Enable the user
		enableUser(context, userID);
		
		context.addMessage("User successfully enabled", MessageSeverity.SUCCESS);
		response.sendRedirect( UserView.getURL(userID) );
		
		return true;
	}

}
