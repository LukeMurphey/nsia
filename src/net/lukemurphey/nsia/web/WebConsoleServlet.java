package net.lukemurphey.nsia.web;

import java.io.IOException;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.SessionManagement;
import net.lukemurphey.nsia.SessionManagement.SessionInfo;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.web.middleware.Middleware;
import net.lukemurphey.nsia.web.views.Dialog;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

import javax.servlet.http.Cookie;

public class WebConsoleServlet extends HttpServlet {

	private static final long serialVersionUID = 8672076908249288190L;
	
	private static ViewList view_list = new ViewList();
	private SessionMessages session_messages = new SessionMessages();
	private Vector<Middleware> middleware = new Vector<Middleware>();
	
	public static final String SERVER_STRING = "ThreatFactor NSIA 1.0";
	
	public WebConsoleServlet(){
		view_list = StandardViewList.getViewList();
		middleware.addAll(StandardMiddlewareList.getMiddleware());
	}
	
	public void doRequest(HttpServletRequest request, HttpServletResponse response ) {
		try{
			
			response.setHeader("Server", SERVER_STRING);
			
			// 1 -- Create the request context
			RequestContext context = null;
			
			//	 1.1 -- Get the session cookie (if exists)
			Cookie[] cookies = request.getCookies();
			String sessionID = null;
			
			if( cookies != null ){
				for (Cookie cookie : cookies) {
					if( cookie.getName().equals("SessionID")){
						sessionID = cookie.getValue();
						break;
					}
				}
			}
			
			//	 1.2 -- Determine if the session ID is for a legitimate session
			if( sessionID != null ){
				Application app = Application.getApplication();
				
				SessionManagement session_mgmt = new SessionManagement(app);
				
				SessionInfo session_info;
				session_info = session_mgmt.getSessionInfo(sessionID);
				
				context = new RequestContext( session_info, session_messages );
			}
						
			// 2 -- Execute the middleware
			for ( Middleware m : middleware ) {
				if( m.process(request, response, context) ){
					return; //Middleware handled the view, no reason to continue
				}
			}
			
			// 3 -- Process the request via the associated view. Stop on the first view that returns true (indicates that it will handle providing the content).
			boolean handled = false;
			
			for (View view : view_list.getViews()) {
				
				try{
					if( view.process(request, response, context) ){
						handled = true;
						break;
					}
				}
				catch(ClientAbortException e){
					//Ignore client abort exceptions since they are not a problem with the application
					Application.getApplication().logEvent(EventLogMessage.EventType.WEB_INFO_LOG,  new EventLogField( EventLogField.FieldName.MESSAGE, "Connection closed"), new EventLogField( EventLogField.FieldName.URL, request.getServletPath()) );
					handled = true;
				}
				catch(ViewFailedException e){
					throw new ServletException("Exception thrown while generating view", e);
				}
			}
			
			// 4 -- View was not found
			if( handled == false ){
				response.resetBuffer();
				response.setStatus(404);
				Application.getApplication().logEvent(EventLogMessage.EventType.WEB_INFO_LOG,  new EventLogField( EventLogField.FieldName.MESSAGE, "Resource not found (404)"), new EventLogField( EventLogField.FieldName.URL, request.getServletPath()) );
				Dialog.getDialog(response, context, Shortcuts.getMapWithBasics(context, request), "The resource you are looking for was not found", "Not Found (404)", DialogType.WARNING, new Link("Return to the Main Dashboard", StandardViewList.getURL("main_dashboard")));
			}
		}
		catch(Throwable t){
			t.printStackTrace();
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.WEB_ERROR, t);
			showServerErrorDialog(request,response, new RequestContext());
		}
		
	}
	
	protected void showServerErrorDialog( HttpServletRequest request, HttpServletResponse response, RequestContext context ){
		response.setStatus(500);
		try {
			Dialog.getDialog(response, context, Shortcuts.getMapWithBasics(context, request), "An internal application error has occurred", "Internal Error", DialogType.CRITICAL);
		} 
		catch (ViewFailedException e) {
			e.printStackTrace();
			//throw new ServletException("Exception thrown while generating view", e);
		}
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
		doRequest(request, response);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
		doRequest(request, response);
	}
	
}
