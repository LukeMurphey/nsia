package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.LocalPasswordAuthentication;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.NumericalOverflowException;
import net.lukemurphey.nsia.PasswordAuthenticationValidator;
import net.lukemurphey.nsia.PasswordInvalidException;
import net.lukemurphey.nsia.UserManagement;
import net.lukemurphey.nsia.UserManagement.UserDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.Menu;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.StandardViewList;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;
import net.lukemurphey.nsia.web.forms.Field;
import net.lukemurphey.nsia.web.forms.FieldError;
import net.lukemurphey.nsia.web.forms.FieldErrors;
import net.lukemurphey.nsia.web.forms.Form;
import net.lukemurphey.nsia.web.forms.PasswordValidator;
import net.lukemurphey.nsia.web.templates.TemplateLoader;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class UserPasswordUpdateView extends View {

	public final static String VIEW_NAME = "user_password";
	
	public UserPasswordUpdateView() {
		super("User/UpdatePassword", VIEW_NAME, Pattern.compile("[0-9]+"));
	}
	
	public static String getURL( UserDescriptor user ) throws URLInvalidException{
		UserPasswordUpdateView view = new UserPasswordUpdateView();
		
		return view.createURL(user.getUserID());
	}

	/**
	 * This form checks to make sure both passwords are identical.
	 * @author Luke
	 *
	 */
	private static class UserPasswordUpdateForm extends Form{
		
		public UserPasswordUpdateForm(){
			addField( new Field("YourPassword") );
			addField( new Field("Password", new PasswordValidator()) );
			addField( new Field("PasswordConfirm") );
		}
		
		@Override
		public FieldErrors validate( HttpServletRequest request ){
			
			FieldErrors errors = super.validate(request);
			
			if( errors.isEmpty() == false ){
				return errors;
			}
			else{
				
				if( request.getParameter("Password") != null && !request.getParameter("Password").equalsIgnoreCase( request.getParameter("PasswordConfirm") ) ){
					errors.put(new FieldError("Password", request.getParameter("PasswordConfirm"), "The passwords are not identical"));
				}
			}
			
			return errors;
		}
	}
	
	private boolean performActions( HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data, UserDescriptor user) throws ViewFailedException, NotFoundException, PasswordInvalidException, IOException{
		try{
			
			UserManagement userManager = new UserManagement(Application.getApplication());
			
			if( "POST".equalsIgnoreCase( request.getMethod() ) ) {
				
				Form form = new UserPasswordUpdateForm();
				
				// 1 -- Validate the form
				FieldErrors errors = form.validate(request);
				
				// 2 -- Add the errors to the list (if the form did not validate)
				if( errors.size() > 0 ){
					data.put("form_errors", errors);
				}
				else{
					// 3 -- Check the user's password
					LocalPasswordAuthentication localAuth = new LocalPasswordAuthentication( Application.getApplication() );
					System.out.print("Password: " + request.getParameter("YourPassword"));
					if( !localAuth.checkPassword( user.getUserID(), new PasswordAuthenticationValidator( request.getParameter("YourPassword") ) ) ){
						errors.put(new FieldError("YourPassword", "", "Your current password is incorrect"));
						data.put("form_errors", errors);
						throw new PasswordInvalidException();
					}
					
					// 4 -- Otherwise, update the password
					String newPassword = request.getParameter("Password");
					userManager.changePassword(user, newPassword);
					
					Application.getApplication().logEvent(EventLogMessage.EventType.USER_PASSWORD_CHANGED, new EventLogField[]{
							new EventLogField( EventLogField.FieldName.TARGET_USER_NAME, user.getUserName() ),
							new EventLogField( EventLogField.FieldName.TARGET_USER_ID, user.getUserID() ),
							new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, context.getSessionInfo().getUserName() ),
							new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, context.getSessionInfo().getUserId() )} );
					
					if( user != null && context.getUser() != null && user.getUserID() == context.getUser().getUserID()){
						context.addMessage("Your password was successfully updated", MessageSeverity.SUCCESS);
					}
					else{
						context.addMessage("The password for the " + user.getUserName() + " account was successfully updated", MessageSeverity.SUCCESS);
					}
					
					response.sendRedirect(UserView.getURL(user));
					return true;
				}
			}
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new ViewFailedException(e);
		} catch(InputValidationException e){
			throw new ViewFailedException(e);
		} catch( NumericalOverflowException e ){
			throw new ViewFailedException(e);
		} catch (URLInvalidException e) {
			throw new ViewFailedException(e);
		}
		
		return false;
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {
		
		// 1 -- Get the user account if one exists
		UserDescriptor user = null;
		
		if( args.length >= 1 ){
			
			// 1.1 -- Get the user ID
			int userID;
			
			try{
				userID = Integer.valueOf( args[0] );
			}
			catch( NumberFormatException e ){
				Dialog.getDialog(response, context, data, "The User ID provided is not valid", "User ID Invalid", DialogType.WARNING);
				return true;
			}
			
			// 1.2 -- Get the user descriptor
			UserManagement userMgmt = new UserManagement(Application.getApplication());
			
			try{
				user = userMgmt.getUserDescriptor(userID);
			}
			catch(NotFoundException e){
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
		}
		
		// 2 -- Make sure we have a user to change the password of
		if( user == null ){
			Dialog.getDialog(response, context, data, "No user provided to update", "User Not Found", DialogType.WARNING);
			return true;
		}
		
		// 3 -- Check permissions
		try{
			if( context.getUser().getUserID() == user.getUserID() ){ // Wants to change their own password
				if( Shortcuts.hasRight( context.getSessionInfo(), "Users.UpdateOwnPassword", "Update user's own password" ) == false ){
					context.addMessage("You do not have permission to update your password", MessageSeverity.WARNING);
					response.sendRedirect( UserView.getURL(user) );
					return true;
				}
			}
			else{ // Wants to change someone else's password
				if( Shortcuts.hasRight( context.getSessionInfo(), "Users.UpdatePassword", "Update another user's password" ) == false ){
					context.addMessage("You do not have permission to update user passwords", MessageSeverity.WARNING);
					response.sendRedirect( UserView.getURL(user) );
					return true;
				}
			} 
		}
		catch(GeneralizedException e){
			throw new ViewFailedException(e);
		}
		
		// 2 -- Process the data as necessary
		try{
			if( performActions(request, response, context, args, data, user) ){
				return true; //Method set a redirect, just let it handle the result
			}
		} catch( PasswordInvalidException e ){
			
		} catch( NotFoundException e ){
			Dialog.getDialog(response, context, data, "No user was found with the given ID", "User Not Found", DialogType.WARNING);
			return true;
		}
		
		
		// 3 -- Get the menu
		data.put("menu", Menu.getUserMenu(context, user));
		
		
		// 4 -- Get the breadcrumbs
		Vector<Link> breadcrumbs = new Vector<Link>();
		breadcrumbs.add( new Link("Main Dashboard", StandardViewList.getURL("main_dashboard")) );
		breadcrumbs.add( new Link("User Management", UsersView.getURL()) );
		breadcrumbs.add( new Link("View User: " + user.getUserName(), UserView.getURL(user)) );
		breadcrumbs.add( new Link("Edit User", UserEditView.getURL(user)) );
		breadcrumbs.add( new Link("Update Password", createURL( user.getUserID())) );
		
		data.put("breadcrumbs", breadcrumbs);
		data.put("title", "Update Password");
		
		//Get the dashboard headers
		Shortcuts.addDashboardHeaders(request, response, data);
		
		TemplateLoader.renderToResponse("UserPasswordUpdate.ftl", data, response);
		
		return true;
		
	}

}
