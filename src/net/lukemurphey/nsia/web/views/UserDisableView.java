package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.DisallowedOperationException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
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

public class UserDisableView extends View {

	public UserDisableView() {
		super("User/Disable", "user_disable", Pattern.compile("[0-9]+", Pattern.CASE_INSENSITIVE));
	}
	
	public static String getURL( UserDescriptor user ) throws URLInvalidException{
		UserDisableView view = new UserDisableView();
		
		return view.createURL(user.getUserID());
	}
	
	public boolean disableUser( RequestContext context, int userID ) throws ViewFailedException, DisallowedOperationException{
		
		Application app = Application.getApplication();
		
		try{
			// 0 -- Precondition check
			
			//	 0.1 -- Make sure the user has permission
			//Shortcuts.checkRight( context.getSessionInfo(), "SiteGroups.Delete" ); //TODO Check permissions
			UserManagement userManagement = new UserManagement(Application.getApplication());
			
			// 1 -- Disable the account
			if( context.getUser().getUserID() == userID ){
				throw new DisallowedOperationException("Users are not allowed to disable their own account");
			}
			
			if( userManagement.disableAccount( userID ) ){
				
				app.logEvent(EventLogMessage.Category.USER_DISABLED,
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

		// 1 -- Make sure the argument for the user to disable is provided and is the correct type
		int userID = -1;
		
		//	 1.1 -- Make sure an ID was provided
		if( args.length <= 0 ){
			//Show a dialog indicating that the user ID provided was not provided
			Dialog.getDialog(response, context, data, "The User ID was not provided.", "User ID Invalid", DialogType.WARNING);
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
		
		// 2 -- Disable the user
		try {
			disableUser(context, userID);
			context.addMessage("User successfully disabled", MessageSeverity.SUCCESS);
		} catch (DisallowedOperationException e) {
			context.addMessage("You cannot disable your own account", MessageSeverity.WARNING);
		}
		
		response.sendRedirect( UserView.getURL(userID) );
		
		return true;
	}

}
