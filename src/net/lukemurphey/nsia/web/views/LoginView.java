package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.ClientData;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.LocalPasswordAuthentication;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NumericalOverflowException;
import net.lukemurphey.nsia.PasswordAuthenticationValidator;
import net.lukemurphey.nsia.SessionManagement;
import net.lukemurphey.nsia.SessionStatus;
import net.lukemurphey.nsia.UserManagement;
import net.lukemurphey.nsia.Authentication.AuthenticationResult;
import net.lukemurphey.nsia.SessionManagement.SessionInfo;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.eventlog.EventLogMessage.Category;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;
import net.lukemurphey.nsia.web.templates.TemplateLoader;

public class LoginView extends View {

	public LoginView() {
		super("Login", "login");
	}

	public static String getURL() throws URLInvalidException{
		LoginView view = new LoginView();
		return view.createURL();
	}
	
	public class Message{
		
		private String message;
		private MessageSeverity sev;
		
		public Message( String message, MessageSeverity sev ){
			this.message = message;
			this.sev = sev;
		}
		
		public MessageSeverity getSeverity(){
			return sev;
		}
		
		public String getMessage(){
			return message;
		}
		
		public String toString(){
			return message;
		}
	}
	
	@Override
	public boolean process( HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data ) throws ViewFailedException {
		
		//If the user is already logged in, then don't bother showing the view
		if( context!= null && context.getSessionInfo() != null && context.getSessionInfo().getSessionStatus() == SessionStatus.SESSION_ACTIVE ){
			try{
				if( request.getParameter("ReturnTo") != null ){
					response.sendRedirect( request.getParameter("ReturnTo") );
					return true;
				}
				else{
					response.sendRedirect( MainDashboardView.getURL() );
					return true;
				}
			}
			catch( IOException e ){
				throw new ViewFailedException(e);
			}
			catch( URLInvalidException e ){
				throw new ViewFailedException(e);
			}
		}
		
		data.put("title", "Login");
		
		Vector<String> headers = new Vector<String>();
		headers.add(DashboardPreLoginPanel.getPanel(request, data));
		
		data.put("dashboard_headers", headers);
		data.put("show_splitter_border", false);
		
		// 1 -- Try to login if requested
		data.put("alert", MessageSeverity.ALERT);
		data.put("information", MessageSeverity.INFORMATION);
		data.put("success", MessageSeverity.SUCCESS);
		data.put("warning", MessageSeverity.WARNING);
		
		if( request.getMethod().equalsIgnoreCase("POST")){
			String username = request.getParameter("Username");
			String password = request.getParameter("Password");
			data.put("username", username);
			
			ClientData clientData;
			
			try {
				clientData = new ClientData( InetAddress.getByName(request.getRemoteAddr() ), request.getHeader("User-Agent"));
			} catch (UnknownHostException e1) {
				clientData = null;
			}
			
			String sessionID = authenticate(username, password, clientData);
			
			if( sessionID != null ){
				//Authentication was successful
				Cookie cookie = new Cookie("SessionID", sessionID);
				cookie.setPath("/");
				
				try {
					if( Application.getApplication().getApplicationConfiguration().isSslEnabled() ){
						cookie.setSecure(true);
					}
				} catch (NoDatabaseConnectionException e1) {
					throw new ViewFailedException(e1);
				} catch (SQLException e1) {
					throw new ViewFailedException(e1);
				} catch (InputValidationException e1) {
					throw new ViewFailedException(e1);
				}
				
				response.addCookie(cookie);
				try {
					String forwardTo = request.getParameter("ForwardTo");
					if( forwardTo != null ){
						response.sendRedirect(forwardTo);
					}
					else{
						response.sendRedirect("/");
					}
				} catch (IOException e) {
					throw new ViewFailedException(e);
				}
				return true;
			}
			else{
				//Authentication attempt failed
				data.put("auth_failed", new Boolean(true));
				data.put("message", new Message("Authentication failed; credentials are incorrect", MessageSeverity.ALERT));
			}
		}
		else if( context != null && context.getSessionInfo() != null ){
			if( context.getSessionInfo().getSessionStatus() == SessionStatus.SESSION_ADMIN_TERMINATED ){
				data.put("message", new Message("Your session was terminated by an administrator", MessageSeverity.ALERT));
			}
			else if( context.getSessionInfo().getSessionStatus() == SessionStatus.SESSION_EXPIRED ){
				data.put("message", new Message("Your session has expired", MessageSeverity.INFORMATION));
			}
			else if( context.getSessionInfo().getSessionStatus() == SessionStatus.SESSION_INACTIVE ){
				data.put("message", new Message("Your session expired due to inactivity", MessageSeverity.INFORMATION));
			}
			else if( context.getSessionInfo().getSessionStatus() == SessionStatus.SESSION_LIFETIME_EXCEEDED ){
				data.put("message", new Message("Your session has expired", MessageSeverity.INFORMATION));
			}
			
			// Insert the username so that the login field is populated
			if( context.getUser() != null ){
				data.put("username", context.getUser().getUserName());
			}
		}
		
		if( request.getParameter("LoggedOut") != null ){
			data.put("message", new Message("You have been successfully logged out", MessageSeverity.INFORMATION));
		}
		
		TemplateLoader.renderToResponse("Login.ftl", data, response);
		
		return true;
	}
	
	/**
	 * Perform the authentication attempt and return the session ID if authentication succeeded.
	 * @param userName
	 * @param password
	 * @param clientData
	 * @return
	 * @throws ViewFailedException
	 */
	private String authenticate( String userName, String password, ClientData clientData ) throws ViewFailedException{
		
		Application app = Application.getApplication();
		
		// 0 -- Precondition Checks
		
		//	 0.1 -- Username cannot be null or empty
		if( userName == null || userName.length() == 0 ){
			if( clientData == null){
				app.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_USERNAME_EMPTY);
			}
			else{
				app.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_USERNAME_EMPTY, new EventLogField( FieldName.SOURCE_ADDRESS, clientData.getSourceAddress().toString() ) );
			}
			
			return null;
		}
		
		//	 0.2 -- Make sure the username is valid
		Pattern nameRegex = Pattern.compile( UserManagement.USERNAME_REGEX );
		Matcher matcher = nameRegex.matcher(userName);
		if( !matcher.matches() ){
			if( clientData == null)
				app.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_USERNAME_EMPTY);
			else
				app.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_USERNAME_EMPTY, new EventLogField( FieldName.SOURCE_ADDRESS, clientData.getSourceAddress().toString() ) );
			
			app.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_USERNAME_ILLEGAL, new EventLogField( FieldName.TARGET_USER_NAME, userName ) );
			return null;
		}
		
		//	 0.3 -- Username must not be overly long (this makes SQL injection more difficult)
		if( userName.length() > UserManagement.USERNAME_LENGTH ){
			if( clientData == null)
				app.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_USERNAME_EMPTY);
			else
				app.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_USERNAME_EMPTY, new EventLogField( FieldName.SOURCE_ADDRESS, clientData.getSourceAddress().toString() ) );
			
			app.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_LENGTH_EXCESSIVE, new EventLogField( FieldName.LENGTH, userName.length()), new EventLogField( FieldName.TARGET_USER_NAME, userName ) );
			return null;
		}
		
		// 1 -- Authenticate
		
		//	 1.1 -- Try to authenticate
		LocalPasswordAuthentication localPasswordAuth = new LocalPasswordAuthentication(app);
		PasswordAuthenticationValidator passwordAuth = new PasswordAuthenticationValidator( password );
		AuthenticationResult result;
		
		try {
			result = localPasswordAuth.authenticate(userName, passwordAuth, clientData);
		} catch (NoSuchAlgorithmException e) {
			app.logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new ViewFailedException(e);
		} catch (SQLException e) {
			app.logExceptionEvent( EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new ViewFailedException(e);
		} catch (InputValidationException e) {
			app.logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			app.logExceptionEvent( EventLogMessage.Category.DATABASE_FAILURE, e );
			throw new ViewFailedException(e);
		} catch (NumericalOverflowException e) {
			app.logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new ViewFailedException(e);
		}
		

		//	 1.2 -- Make a decision on the result
		if( result.getAuthenticationStatus() == AuthenticationResult.AUTH_ACCOUNT_ADMINISTRATIVELY_LOCKED ){
			if( clientData == null)
				app.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_ACCOUNT_DISABLED, new EventLogField( FieldName.TARGET_USER_NAME, userName ));
			else
				app.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_ACCOUNT_DISABLED, new EventLogField( FieldName.TARGET_USER_NAME, userName ) , new EventLogField( FieldName.SOURCE_ADDRESS, clientData.getSourceAddress().toString() ) );
			
			return null;
		}
		else if( result.getAuthenticationStatus() == AuthenticationResult.AUTH_ACCOUNT_BRUTE_FORCE_LOCKED ){
			if( clientData == null)
				app.logEvent(EventLogMessage.Category.AUTHENTICATION_USERNAME_BLOCKED, new EventLogField( FieldName.TARGET_USER_NAME, userName ) );
			else
				app.logEvent(EventLogMessage.Category.AUTHENTICATION_USERNAME_BLOCKED, new EventLogField( FieldName.TARGET_USER_NAME, userName ), new EventLogField( FieldName.SOURCE_ADDRESS, clientData.getSourceAddress().toString() ) );
			
			return null;
		}
		else if( result.getAuthenticationStatus() == AuthenticationResult.AUTH_ACCOUNT_DISABLED ){
			if( clientData == null)
				app.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_ACCOUNT_DISABLED, new EventLogField( FieldName.TARGET_USER_NAME, userName ));
			else
				app.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_ACCOUNT_DISABLED, new EventLogField( FieldName.TARGET_USER_NAME, userName ), new EventLogField( FieldName.SOURCE_ADDRESS, clientData.getSourceAddress().toString() ) );
			
			return null;
		}
		else if( result.getAuthenticationStatus() == AuthenticationResult.AUTH_FAILED ){//This should not be returned
			if( clientData == null)
				app.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_PASSWORD_WRONG, new EventLogField( FieldName.TARGET_USER_NAME, userName ));
			else
				app.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_PASSWORD_WRONG, new EventLogField( FieldName.TARGET_USER_NAME, userName ), new EventLogField( FieldName.SOURCE_ADDRESS, clientData.getSourceAddress().toString() ) );
			
			return null;
		}
		else if( result.getAuthenticationStatus() == AuthenticationResult.AUTH_INVALID_PASSWORD ){
			if( clientData == null)
				app.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_PASSWORD_ILLEGAL, new EventLogField( FieldName.TARGET_USER_NAME, userName ));
			else
				app.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_PASSWORD_ILLEGAL, new EventLogField( FieldName.TARGET_USER_NAME, userName ), new EventLogField( FieldName.SOURCE_ADDRESS, clientData.getSourceAddress().toString() ) );
			
			return null;
		}
		else if( result.getAuthenticationStatus() == AuthenticationResult.AUTH_INVALID_USER ){
			if( clientData == null)
				app.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_USERNAME_INVALID, new EventLogField( FieldName.TARGET_USER_NAME, userName ));
			else
				app.logEvent(EventLogMessage.Category.AUTHENTICATION_FAILED_USERNAME_INVALID, new EventLogField( FieldName.TARGET_USER_NAME, userName ), new EventLogField( FieldName.SOURCE_ADDRESS, clientData.getSourceAddress().toString() ) );
			
			return null;
		}
		else if( result.getAuthenticationStatus() == AuthenticationResult.AUTH_SUCCESS ){
			
			EventLogMessage message = new EventLogMessage(EventLogMessage.Category.AUTHENTICATION_SUCCESS, new EventLogField( FieldName.TARGET_USER_NAME, userName ));
			
			// Get the session information in order to add additional details
			try{
				SessionManagement sessionManagement = new SessionManagement(app);
				SessionInfo sessionInfo = sessionManagement.getSessionInfo(result.getSessionIdentifier());
				message.addField( new EventLogField(FieldName.TARGET_USER_ID, sessionInfo.getUserId()) );
				message.addField( new EventLogField(FieldName.SESSION_TRACKING_NUMBER, sessionInfo.getTrackingNumber()) );
			}
			catch(Exception e){
				app.logExceptionEvent(Category.INTERNAL_ERROR, e);
			}

			if( clientData != null){
				message.addField( new EventLogField( FieldName.SOURCE_ADDRESS, clientData.getSourceAddress().toString() ) );
			}
			
			app.logEvent(message);
			
			return result.getSessionIdentifier();
		}
		else{
			app.logEvent( EventLogMessage.Category.INTERNAL_ERROR, new EventLogField( FieldName.MESSAGE, "Invalid authentication result code"  ));
			return null;
		}
	}

}
