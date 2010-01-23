package net.lukemurphey.nsia.htmlInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import net.lukemurphey.nsia.Firewall;
import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.ClientData;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.SessionStatus;
import net.lukemurphey.nsia.UserManagement;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.trustBoundary.ApiSessionManagement;

import java.net.InetAddress;
import java.net.UnknownHostException;


/**
 * The web console servlet provides the main interface into the application web console.
 * @author luke
 *
 */
public class WebConsoleServlet extends HttpServlet {
	
	private static final long serialVersionUID = -6871046692116002094L;
	
	// The following attributes indicate if authentication was performed during the given connection
	public final static int AUTH_NONE_REQUESTED = 0;
	public final static int AUTH_ATTEMPT_SUCCESS = 1;
	public final static int AUTH_ATTEMPT_FAIL = 2;
	public final static int AUTH_ATTEMPT_NO_USERNAME = 3;
	public final static int AUTH_ATTEMPT_NO_PASSWORD = 4;
	public final static int AUTH_LOGOUT_SUCCESS = 5;
	public final static int AUTH_LOGOUT_FAIL = 6;
	
	protected Application application;
	private String templateString = null;
	private ContentTemplate contentTemplate = null;
	
	public void doRequest(HttpServletRequest request, HttpServletResponse response, int method ) throws ServletException, IOException {
		try{
			WebConsoleConnectionDescriptor requestDescriptor = new WebConsoleConnectionDescriptor();
			requestDescriptor.request = request;
			requestDescriptor.response = response;
			requestDescriptor.httpMethod = method;
			
			// 1 -- Perform the initial routines
			
			//	 1.1 -- Set the server header
			response.setHeader("Server", HtmlContentProvider.SERVER_STRING);
			
			//	 1.2 -- Determine if the source is allowed
			/*Firewall firewall = Application.getApplication().getFirewall();
			int addressAllowed = firewall.isAllowed( InetAddress.getByName( request.getRemoteAddr() ) );
			if( addressAllowed != Firewall.FIREWALL_ACCEPT && addressAllowed != Firewall.FIREWALL_ACCEPTED_BY_DEFAULT ){
				response.sendError( HttpServletResponse.SC_FORBIDDEN );
				return;
			}*/
			
			handleAuthentication( requestDescriptor );
			
			// 2 -- Render the site
			try{
				renderPage(requestDescriptor);
				//renderPageOld(requestDescriptor);
			}
			//if a session error occurs, try to establish a session and try again
			catch (NoSessionException e){
				handleAuthentication(requestDescriptor);
				//renderPage( request, response, requestDescriptor, method);
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	public String getServerString( HttpServletRequest request){
		String server;
		
		server = request.getScheme() + "://" + request.getServerName();
		if( !request.isSecure() && request.getServerPort() != 80)
			server += ":" + request.getServerPort();
		else if( request.isSecure() && request.getServerPort() != 443)
			server += ":" + request.getServerPort();

		return server;
	}
	
	private void renderPage(WebConsoleConnectionDescriptor requestDescriptor) throws NoSessionException, IOException{

		// 1 -- Render the page
		application = Application.getApplication();
		requestDescriptor.application = application;
		requestDescriptor.appStatusDesc = Application.getApplication().getManagerStatus();
		
		PrintWriter output = requestDescriptor.response.getWriter();
		requestDescriptor.response.setContentType("text/html");
		
		// 1.1 -- Get the template to mark-up
		if( contentTemplate == null ){
			
			InputStream templateInputStream = null;
			
			// 1.1.1 -- Try to load from the local jar file
			templateInputStream = ClassLoader.getSystemResourceAsStream( "template.html" );
			
			// 1.1.2 -- Otherwise, try to load it from the local directory
			if( templateInputStream == null ){
				File templateFile = new File("../src/template.html");
				templateInputStream = new FileInputStream(templateFile.getAbsoluteFile());
			}
			
		
			templateString = "";
			byte[] inputBytes = new byte[1024];
			int bytesRead = templateInputStream.read(inputBytes);
			while( bytesRead > 0 ) {
				templateString += new String( inputBytes, 0, bytesRead, "Cp1252" );
				bytesRead = templateInputStream.read(inputBytes);
			}
			templateInputStream.close();
			
			contentTemplate = new ContentTemplate(templateString);
		}
		
		ContentTemplate.TemplateVariables vars = null;
		
		try{

			// The main content will be executed first in case an pending actions change the output of other content handlers
			ContentDescriptor mainContentDesc;
			mainContentDesc = HtmlMainContent.getHtml( requestDescriptor );
			
			if( !mainContentDesc.isCompletePage()){// requestDescriptor.request.getParameter("Mode") != null && requestDescriptor.request.getParameter("Mode").matches("AccessControl") ){
				output.print( mainContentDesc.getBody() );
				output.close();
				return;
			}
			
			// 2.1 -- Get the user options
			String userOptions = HtmlUserOptions.getHtml( requestDescriptor ).getBody();
			
			// 2.2 -- Get the header dashboard
			ContentDescriptor dashboardContentDesc = HtmlDashboardHeader.getHtml( requestDescriptor );

			// 2.3 -- Get the main content
			if( !requestDescriptor.applicationErrorOccurred ){
				
				//html = StringUtils.replace( html, "<%MainContent%>", mainContentDesc.getBody() );
				//html = StringUtils.replace( html, "<%Title%>", mainContentDesc.getTitle() );
			}
			
			vars = new ContentTemplate.TemplateVariables(mainContentDesc.getTitle(), userOptions, dashboardContentDesc.getBody(), mainContentDesc.getBody(), Application.getVersion() );
			
		}
		catch(NoSessionException e){
			requestDescriptor.response.sendRedirect("Login");
		}
		catch( Exception e){
			
			requestDescriptor.applicationErrorOccurred = true;
			
			// Log the exception if it is unchecked. Unchecked exceptions are not necessarily logged by the trust boundary layer.
			if( e instanceof RuntimeException){
				application.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e);
			}
			
			ContentDescriptor errorContentDesc = HtmlProblemDialog.getHtml(requestDescriptor, "Internal Error", "An internal error occurred and the requested operation could not be completed. Contact the System Administrator if assistance is necessary.", HtmlProblemDialog.DIALOG_ALERT);
			vars = new ContentTemplate.TemplateVariables("Internal Error", "", "", errorContentDesc.getBody(), Application.getVersion());
			requestDescriptor.response.setContentLength(vars.getLength() + contentTemplate.getLength());
			
			contentTemplate.send(vars, output);
			output.close();
		}
		
		// 3 -- Output the result
		if( vars != null ){
			requestDescriptor.response.setContentLength(vars.getLength() + contentTemplate.getLength());
			contentTemplate.send( vars, output);
		}
		
		output.close();
		
	}
	
	private void handleAuthentication( WebConsoleConnectionDescriptor requestDescriptor ){
		try{
			// 1 -- Determine the authentication status
			
			//	 1.1 -- Determine the session status
			Cookie[] cookies = requestDescriptor.request.getCookies();
			
			if( cookies == null ){
				requestDescriptor.sessionStatus = SessionStatus.SESSION_NULL; //No session yet
			}
			else{
				// 1.1.1 -- Find the session ID cookie
				for( int c = 0; c < cookies.length; c++ ){
					if( cookies[c].getName().matches("SessionID") ){
						requestDescriptor.sessionIdentifier = cookies[c].getValue();
					}
				}
				
				// 1.1.2 -- Update the session identifier if it has expired (also, update it if is current so that it always changes)
				ApiSessionManagement xSession = new ApiSessionManagement( Application.getApplication() );
				requestDescriptor.sessionStatus = SessionStatus.getStatusById( xSession.getSessionStatus(requestDescriptor.sessionIdentifier) );
				
				if( requestDescriptor.sessionStatus.equals( SessionStatus.SESSION_IDENTIFIER_EXPIRED) || requestDescriptor.sessionStatus.equals( SessionStatus.SESSION_ACTIVE) ){
					requestDescriptor.sessionIdentifier = xSession.refreshSessionIdentifier( requestDescriptor.sessionIdentifier );
					if( requestDescriptor.sessionIdentifier == null )
						requestDescriptor.sessionStatus = SessionStatus.SESSION_NULL;
					else{
						requestDescriptor.sessionStatus = SessionStatus.getStatusById( xSession.getSessionStatus(requestDescriptor.sessionIdentifier) );
						requestDescriptor.response.addCookie( new Cookie( "SessionID", requestDescriptor.sessionIdentifier) );
						
					}
				}
			}
			
			//	 1.2 -- Process the authentication request (if any)
			String mode = requestDescriptor.request.getParameter("Action");
			
			//		1.2.1 -- Process the login event (if requested)
			if( (requestDescriptor.sessionStatus != SessionStatus.SESSION_ACTIVE || (mode != null && mode.matches("Login")) ) && requestDescriptor.request.getParameter("Username") != null && requestDescriptor.request.getParameter("Password") != null ){
				String username = requestDescriptor.request.getParameter("Username");
				String password = requestDescriptor.request.getParameter("Password");
				
				if( username == null || username.trim().matches("") ){
					requestDescriptor.authenticationAttempt = AUTH_ATTEMPT_NO_USERNAME;
				}
				else if ( password == null || password.trim().matches("") ){
					requestDescriptor.authenticationAttempt = AUTH_ATTEMPT_NO_PASSWORD;
				}
				else{
					ApiSessionManagement xSession = new ApiSessionManagement( Application.getApplication() );
					ClientData clientData;
					
					// 1.2.1.1 -- Get the web client string
					try {
						clientData = new ClientData( InetAddress.getByName( requestDescriptor.request.getRemoteAddr() ) ,requestDescriptor.request.getHeader("User-Agent"));
					} catch (UnknownHostException e1) {
						clientData = null;
					}
					
					// 1.2.1.2 -- attempt authentication
					if( clientData == null ){
						requestDescriptor.sessionIdentifier = xSession.authenticate( username, password );
					}
					else{
						requestDescriptor.sessionIdentifier = xSession.authenticate( username, password, clientData );
					}
					
					// 1.2.1.3 -- Process the result of the authentication attempt
					if( requestDescriptor.sessionIdentifier == null ){
						requestDescriptor.authenticationAttempt = AUTH_ATTEMPT_FAIL;
					}
					else{
						requestDescriptor.authenticationAttempt = AUTH_ATTEMPT_SUCCESS;
						requestDescriptor.sessionStatus = SessionStatus.SESSION_ACTIVE;
						Cookie cookie = new Cookie( "SessionID", requestDescriptor.sessionIdentifier );
						requestDescriptor.response.addCookie( cookie );
						
						ApiSessionManagement sessionManagement = new ApiSessionManagement( Application.getApplication());
						UserManagement.UserDescriptor userDesc = sessionManagement.getUserInfo( requestDescriptor.sessionIdentifier );
						requestDescriptor.userId = Long.valueOf( userDesc.getUserID() );
						requestDescriptor.username = userDesc.getUserName();
						
						Html.addMessage(Html.MessageType.INFORMATIONAL, "Login successful", requestDescriptor.userId.longValue());
						
						//Clear the mode if it requests a logout event (this way the user will not be inadvertently logged out if they refresh with the logout command in the URL
						if( mode != null && mode.matches("Logout")){
							try {
								requestDescriptor.response.sendRedirect("Console");
							} catch (IOException e) {
								/*
								 * This action is non-critical. Thus, it can safely be ignored if it fails.
								 */
							}
						}
					}
				}
			}
			
			// 1.3 -- Process the logout event
			else if( mode != null && mode.matches("Logout") && requestDescriptor.sessionIdentifier != null && requestDescriptor.sessionIdentifier.length() > 0 ){
				ApiSessionManagement xSession = new ApiSessionManagement( Application.getApplication() );
				xSession.terminateSession( requestDescriptor.sessionIdentifier );
				requestDescriptor.authenticationAttempt = AUTH_LOGOUT_SUCCESS;
				requestDescriptor.sessionStatus = SessionStatus.SESSION_NULL;
				Cookie cookie = new Cookie( "SessionID", "" );
				requestDescriptor.response.addCookie( cookie );
			}
			
			// 1.4 -- No authentication request provided
			else{
				requestDescriptor.authenticationAttempt = AUTH_NONE_REQUESTED;
			}
			
			// 1.5 -- Populate the user information if logged in
			if( requestDescriptor.sessionStatus == SessionStatus.SESSION_ACTIVE && requestDescriptor.userId == null ){
				ApiSessionManagement sessionManagement = new ApiSessionManagement( Application.getApplication());
				UserManagement.UserDescriptor userDesc = sessionManagement.getUserInfo( requestDescriptor.sessionIdentifier );
				requestDescriptor.userId = new Long( userDesc.getUserID() );
				requestDescriptor.username = userDesc.getUserName();
			}
		}
		catch( GeneralizedException e){
			requestDescriptor.applicationErrorOccurred = true;
		}
		catch( NoSessionException e){
			requestDescriptor.applicationErrorOccurred = true;
		}
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doRequest(request, response, HtmlContentProvider.METHOD_GET);
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doRequest(request, response, HtmlContentProvider.METHOD_POST);
	}
	
}
