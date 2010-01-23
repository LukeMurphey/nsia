package net.lukemurphey.nsia.htmlInterface;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.SessionStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WebConsoleConnectionDescriptor {

	// The following fields are relative to the session status
	protected SessionStatus sessionStatus = SessionStatus.SESSION_NULL;
	protected String sessionIdentifier;
	protected Application.ApplicationStatusDescriptor appStatusDesc;
	protected Application application;
	protected boolean applicationErrorOccurred = false;
	protected int authenticationAttempt = WebConsoleServlet.AUTH_NONE_REQUESTED;
	protected Long userId;
	protected String username;
	
	protected int httpMethod;
	protected HttpServletRequest request;
	protected HttpServletResponse response;
	
	protected String message = null;
	
	public String getLocation(){
		
		String location = request.getScheme() + "://" + request.getServerName();
		
		if( request.getScheme().equalsIgnoreCase("http") && request.getServerPort() != 80 ){
			location += ":" + request.getServerPort();
		}
		else if( request.getScheme().equalsIgnoreCase("https") && request.getServerPort() != 443 ){
			location += ":" + request.getServerPort();
		}
		
		return location;
		
	}
}
