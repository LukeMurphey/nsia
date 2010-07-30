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
import net.lukemurphey.nsia.DisallowedOperationException;
import net.lukemurphey.nsia.EmailAddress;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InvalidLocalPartException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.UserManagement;
import net.lukemurphey.nsia.UserManagement.UserDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.Menu;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;
import net.lukemurphey.nsia.web.forms.EmailAddressValidator;
import net.lukemurphey.nsia.web.forms.Field;
import net.lukemurphey.nsia.web.forms.FieldError;
import net.lukemurphey.nsia.web.forms.FieldErrors;
import net.lukemurphey.nsia.web.forms.Form;
import net.lukemurphey.nsia.web.forms.PasswordValidator;
import net.lukemurphey.nsia.web.forms.PatternValidator;
import net.lukemurphey.nsia.web.templates.TemplateLoader;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class UserEditView extends View {

	public static final String VIEW_NAME = "user_editor"; 
	
	public UserEditView() {
		super("User", VIEW_NAME, Pattern.compile("(New)|(Edit)", Pattern.CASE_INSENSITIVE), Pattern.compile("[0-9]*"));
	}

	public static String getURL( UserDescriptor user ) throws URLInvalidException{
		UserEditView view = new UserEditView();
		
		return view.createURL("Edit", user.getUserID());
	}
	
	public static String getURL( ) throws URLInvalidException{
		UserEditView view = new UserEditView();
		
		return view.createURL("New");
	}
	
	/**
	 * This form checks to make sure both passwords are identical.
	 * @author Luke
	 *
	 */
	private static class UserEditForm extends Form{
		
		@Override
		public FieldErrors validate( HttpServletRequest request ){
			
			FieldErrors errors = super.validate(request);
			
			if( errors.isEmpty() == false ){
				return errors;
			}
			else{
				if( request.getParameter("Password") != null && !request.getParameter("Password").equalsIgnoreCase( request.getParameter("PasswordConfirm") ) ){
					errors.put(new FieldError("PasswordConfirm", request.getParameter("PasswordConfirm"), "The passwords are not identical"));
				}
			}
			
			return errors;
			
		}
		
	}
	
	/**
	 * Get a form that can validate the user information.
	 * @return
	 */
	private Form getUserEditForm( boolean includePasswordFields ){
		UserEditForm form = new UserEditForm();
		
		form.addField( new Field("Username", new PatternValidator(Pattern.compile("[-A-Z0-9a-z_ .]{1,32}", Pattern.CASE_INSENSITIVE), "Username is not valid")) );
		form.addField( new Field("Fullname", new PatternValidator(Pattern.compile("[-A-Z0-9a-z_ ().]{1,128}", Pattern.CASE_INSENSITIVE), "Full name is not valid")) );
		form.addField( new Field("EmailAddress", new EmailAddressValidator("Email address is not valid") ) );
		
		if( includePasswordFields ){
			form.addField( new Field("Password", new PasswordValidator()) );
			form.addField( new Field("PasswordConfirm") );
		}
		
		form.addField( new Field("Unrestricted") );
		
		return form;
	}
	
	private boolean performActions( HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data, UserDescriptor user ) throws ViewFailedException, IOException, URLInvalidException, ViewNotFoundException, DisallowedOperationException{
		try{
			
			UserManagement userManager = new UserManagement(Application.getApplication());
			
			if( "POST".equalsIgnoreCase( request.getMethod() ) ) {
				
				Form form;
				
				if( args.length > 0 && args[0].equalsIgnoreCase("Edit") ){
					form = getUserEditForm(false);
				}
				else{
					form = getUserEditForm(true);
				}
				
				// 1 -- Validate the form
				FieldErrors errors = form.validate(request);
				
				// 2 -- Add the errors to the list (if the form did not validate)
				if( errors.size() > 0 ){
					data.put("form_errors", errors);
				}
				
				// 3 -- Otherwise, update or create the account
				else{
					
					// 3.1 -- Get the field data
					String name = request.getParameter("Username");
					String fullname = request.getParameter("Fullname");
					EmailAddress emailAddress = EmailAddress.getByAddress( request.getParameter("EmailAddress") );
					String password = request.getParameter("Password");
					boolean unrestricted = false;
					
					if( request.getParameter("Unrestricted") != null ){
						unrestricted = true;
					}
					
					try{
						// 3.2 -- Create a new account if one to edit was not provided
						if( user == null ){
							
							//	 0.2 -- Only allow unrestricted accounts to create other unrestricted accounts
							if( !context.getUser().isUnrestricted() && unrestricted == true ){
								Application.getApplication().logEvent( EventLogMessage.EventType.ACCESS_CONTROL_DENY,
										new EventLogField( FieldName.MESSAGE, "Attempt to create unrestricted account from restricted account"),
										new EventLogField( FieldName.SOURCE_USER_NAME, context.getUser().getUserName() ),
										new EventLogField( FieldName.SOURCE_USER_ID, context.getUser().getUserName() )
										);
								throw new DisallowedOperationException("Restricted users cannot create unrestricted accounts");
							}
							
							int userID = userManager.addAccount(name, fullname, password, emailAddress, unrestricted);
							
							if( userID > 0){
								user = userManager.getUserDescriptor(userID);
								
								Application.getApplication().logEvent(EventLogMessage.EventType.USER_ADDED,
										new EventLogField( FieldName.SOURCE_USER_NAME, context.getUser().getUserName() ),
										new EventLogField( FieldName.SOURCE_USER_ID, context.getUser().getUserID() ),
										new EventLogField( FieldName.TARGET_USER_ID, userID ),
										new EventLogField( FieldName.TARGET_USER_NAME, name ) );
								
								context.addMessage("User created successfully", MessageSeverity.SUCCESS);
								response.sendRedirect( UserView.getURL(user) );
								return true;
							}
							else{
								Application.getApplication().logEvent(EventLogMessage.EventType.OPERATION_FAILED,
									new EventLogField( FieldName.OPERATION, "Add user account" ),
									new EventLogField( FieldName.SOURCE_USER_NAME, context.getUser().getUserName() ),
									new EventLogField( FieldName.SOURCE_USER_ID, context.getUser().getUserID() ),
									new EventLogField( FieldName.TARGET_USER_ID, name ) );
								
								context.addMessage("User was not created successfully", MessageSeverity.WARNING);
								response.sendRedirect( UserView.getURL(userID) );
								return false;
							}
						}
						
						// 3.3 -- Edit the existing account
						else{
							
							//	 3.3.1 -- Do not allow restricted accounts to create unrestricted accounts
							if( unrestricted && !context.getUser().isUnrestricted() ){
								
								Application.getApplication().logEvent( EventLogMessage.EventType.ACCESS_CONTROL_DENY,
										new EventLogField( FieldName.SOURCE_USER_NAME, context.getUser().getUserName() ),
										new EventLogField( FieldName.SOURCE_USER_ID, context.getUser().getUserID() ),
										new EventLogField( FieldName.TARGET_USER_NAME, user.getUserName() ),
										new EventLogField( FieldName.TARGET_USER_ID, user.getUserID() ) );
								
								throw new DisallowedOperationException("Restricted users cannot create unrestricted accounts");
							}
							
							else if( userManager.updateAccountEx(user.getUserID(), name, fullname, emailAddress, unrestricted)){
								
								Application.getApplication().logEvent(EventLogMessage.EventType.USER_MODIFIED,
										new EventLogField( FieldName.SOURCE_USER_NAME, context.getUser().getUserName() ),
										new EventLogField( FieldName.SOURCE_USER_ID, context.getUser().getUserID() ),
										new EventLogField( FieldName.TARGET_USER_NAME, user.getUserName() ),
										new EventLogField( FieldName.TARGET_USER_ID, user.getUserID() ) );
								
								context.addMessage("User updated successfully", MessageSeverity.SUCCESS);
								response.sendRedirect( UserView.getURL(user) );
								return true;
							}
							else{
								
								Application.getApplication().logEvent(EventLogMessage.EventType.OPERATION_FAILED,
										new EventLogField( FieldName.OPERATION, "Update user account" ),
										new EventLogField( FieldName.SOURCE_USER_NAME, context.getUser().getUserName() ),
										new EventLogField( FieldName.SOURCE_USER_ID, context.getUser().getUserID() ),
										new EventLogField( FieldName.TARGET_USER_NAME, user.getUserName() ),
										new EventLogField( FieldName.TARGET_USER_ID, user.getUserID() ) );
								
								context.addMessage("User could not be updated", MessageSeverity.WARNING);
								response.sendRedirect( UserView.getURL(user) );
								return true;
							}
						}
					}
					catch(InputValidationException e){
						throw new ViewFailedException(e); //This exception should not be possible since the form should have validated the data beforehand
					}
				}
			}
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new ViewFailedException(e);
		} catch (NotFoundException e) {
			throw new ViewFailedException(e);
		} catch (InvalidLocalPartException e) {
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
		
		if( args.length >= 2 ){
			
			// 1.1 -- Get the user ID
			int userID;
			
			try{
				userID = Integer.valueOf( args[1] );
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
		
		// 2 -- Prepare the page content

		// Get the menu
		data.put("menu", Menu.getUserMenu(context, user));
		
		// Get the breadcrumbs
		Vector<Link> breadcrumbs = new Vector<Link>();
		breadcrumbs.add(  new Link("Main Dashboard", MainDashboardView.getURL()) );
		breadcrumbs.add(  new Link("User Management", UsersView.getURL()) );
		
		if( user != null ){
			breadcrumbs.add(  new Link("View User: " + user.getUserName(), UserView.getURL(user)) );
			breadcrumbs.add(  new Link("Edit User", createURL("Edit", user.getUserID())) );
			data.put("title", "User: " + user);
		}
		else{
			breadcrumbs.add(  new Link("New User", createURL("New")) );
			data.put("title", "New User");
		}
		data.put("breadcrumbs", breadcrumbs);
		
		//Get the dashboard headers
		Shortcuts.addDashboardHeaders(request, response, data);
		
		// 3 -- Check the user's rights
		try {
			if( user == null ){
				if( Shortcuts.hasRight( context.getSessionInfo(), "Users.Add", "Create new user account" ) == false ){
					Shortcuts.getPermissionDeniedDialog(response, data, "You do not have permission to create users");
					return true;
				}
			}
			else{
				if( context.getUser().getUserID() == user.getUserID() && Shortcuts.hasRight( context.getSessionInfo(), "Users.UpdateOwnPassword", "Update user's own account" ) == false ){ //TODO Replace with a more appropriate right name
					Shortcuts.getPermissionDeniedDialog(response, data, "You do not have permission to edit your account");
					return true;
				}
				else if( Shortcuts.hasRight( context.getSessionInfo(), "Users.Edit", "Update another user's account" ) == false ){
						Shortcuts.getPermissionDeniedDialog(response, data, "You do not have permission to edit users");
						return true;
				}
			}
		} catch (GeneralizedException e) {
			throw new ViewFailedException(e);
		}
		
		
		// 2 -- Process the data as necessary
		try {
			if( performActions(request, response, context, args, data, user) ){
				return true; //Method set a redirect, just let it handle the result
			}
		} catch (DisallowedOperationException e) {
			context.addMessage(e.getMessage(), MessageSeverity.WARNING);
		}
		
		
		TemplateLoader.renderToResponse("UserEdit.ftl", data, response);
		
		return true;
	}

}
