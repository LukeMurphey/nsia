package net.lukeMurphey.nsia.htmlInterface;

import java.io.IOException;
import java.util.*;

import org.apache.commons.lang.StringEscapeUtils;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.InsufficientPermissionException;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.SessionManagement;
import net.lukeMurphey.nsia.SessionStatus;
import net.lukeMurphey.nsia.trustBoundary.ApiSessionManagement;
import net.lukeMurphey.nsia.htmlInterface.Html.MessageType;


public class HtmlSessions extends HtmlContentProvider{
	
	//public static final String tableStart = "<p><table cellspacing=\"0\"><tr><td class=\"Background0\"><span class=\"Text_2\">User ID</span></td><td class=\"Background0\"><span class=\"Text_2\">User Name</span></td><td class=\"Background0\"><span class=\"Text_2\">Session Assigned</span></td><td colspan=\"2\" class=\"Background0\"><span class=\"Text_2\">Session Status</span></td></tr>";
	public static final String tableStart = "<p><table class=\"DataTable\"><thead><tr><td>User ID</td><td >User Name</td><td>Session Assigned</td><td colspan=\"2\">Session Status</td></tr></thead>";
	
	private static final int OP_TERMINATE_SUCCESS = 100;
	private static final int OP_TERMINATE_FAILED = 101;
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException{
		return getHtml( requestDescriptor, null );
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException{
		StringBuffer body = new StringBuffer();
		String title = "Logged in Users";
		
		// 1 -- Get the data	
		ApiSessionManagement xSession = new ApiSessionManagement( Application.getApplication() );
		
		//	 1.1 -- Perform any pending actions
		performAction( requestDescriptor, xSession);
		
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		//	 1.2 -- Output the session list
		
		SessionManagement.SessionInfo[] sessions = null;
		
		try {
			sessions = xSession.getUserSessions( requestDescriptor.sessionIdentifier );
			body.append( "<div class=\"Text_1\">Current Sessions</div>Lists all users currently logged in" );
		} catch (NoSessionException e) {
			body.append("<p>");
			body.append(Html.getWarningDialog("Insufficient Permission", "You do not have permission to view sessions", "Console", "Return to Main Dashboard"));
		} catch (InsufficientPermissionException e){
			body.append("<p>");
			body.append(Html.getWarningDialog("Insufficient Permission", "You do not have permission to view sessions", "Console", "Return to Main Dashboard"));
		}
		
		if( sessions != null ){
			body.append( tableStart );
			body.append( "<tbody>" );
			for (int c = 0; c < sessions.length; c++ ){
				if( sessions[c].getSessionStatus().equals( SessionStatus.SESSION_ACTIVE) )
					body.append( createRow( sessions[c], (c % 2) != 0 ) );
			}
			body.append( "</tbody>" );
			body.append( "</table>" );
		}
		else{
			//body.append("<p>");
			//body.append(GenericHtmlGenerator.getWarningDialog("Insufficient Permission", "You do not have permission to view sessions"));
		}
		
		
		// 2 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "User Sessions", "/Sessions" );
		String navigationHtml = Html.getNavigationPath( navPath );
		
		
		// 3 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
		
		menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Add Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("User Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Users", "/UserManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New User", "/UserManagement?Action=Add", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("Group Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Groups", "/GroupManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New Group", "/GroupManagement?Action=Add", MenuItem.LEVEL_TWO) );	
		
		String menuOutput = Html.getMenu( menuItems );
		
		
		// 4 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor( title, pageOutput );
	}
	
	private static ActionDescriptor performAction(WebConsoleConnectionDescriptor requestDescriptor, ApiSessionManagement xSession) throws GeneralizedException, NoSessionException{
		
		// 1 -- Terminate session
		String action = requestDescriptor.request.getParameter("Action");
		
		if( action == null )
			return new ActionDescriptor( ActionDescriptor.OP_NO_OPERATION );
		
		try{
			if( action.matches("Terminate") && requestDescriptor.request.getParameter("TrackingNum") != null ){
				Long trackingNumber = Long.valueOf( requestDescriptor.request.getParameter("TrackingNum") );
				if( xSession.terminateSession( requestDescriptor.sessionIdentifier, trackingNumber.longValue()) == true ){
					if( xSession.getSessionStatus(requestDescriptor.sessionIdentifier) == SessionStatus.SESSION_NULL.getStatusId() ){
						try {
							requestDescriptor.response.resetBuffer();
							requestDescriptor.response.sendRedirect("Console");
						} catch (IOException e) {
							/* This redirect is simply to avoid a strange message if the user destroyed their own session.
							 * This avoids a potentially strange or cryptic message in regards to the user's permissions.
							 * Since, this is not critical, this exception will be ignored
							 */
						}
					}
					
					Html.addMessage(MessageType.INFORMATIONAL, "Session successfully terminated", requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_TERMINATE_SUCCESS );
				}
				else{
					Html.addMessage(MessageType.WARNING, "Session termination failed", requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_TERMINATE_FAILED );
				}
			}
		}
		catch (InsufficientPermissionException e){
			Html.addMessage(MessageType.WARNING, "You do not have permission to terminate the session", requestDescriptor.userId.longValue());
			return new ActionDescriptor( OP_TERMINATE_FAILED );
		}
		
		return new ActionDescriptor( ActionDescriptor.OP_NO_OPERATION );
	}
	
	private static String createRow( SessionManagement.SessionInfo sessionData, boolean isEvenRow ){
		
		if( sessionData.getSessionStatus()!= SessionStatus.SESSION_ACTIVE )
			return null;
		
		StringBuffer output = new StringBuffer();
		if( isEvenRow ){ 
			output.append("<tr class=\"even\">");
		}
		else{
			output.append("<tr class=\"odd\">");
		}
		output.append( "<td>").append( sessionData.getUserId() ).append("</td>");
		output.append( "<td><span class=\"Centered\"><a href=\"UserManagement?UserID=").append( sessionData.getUserId() + "\"><img src=\"/16_User\"></a><a href=\"UserManagement?UserID=").append( sessionData.getUserId() ).append("\">").append( StringEscapeUtils.escapeHtml( sessionData.getUserName() )).append( "</a></span></td>" );
		output.append( "<td>").append( sessionData.getSessionCreated() ).append( "</td>" );
		output.append( "<td>").append( sessionData.getSessionStatus().getDescription() ).append( "</td>" );
		output.append( "<td>").append( Html.getButton("/16_Delete", "Terminate", "/Sessions&Action=Terminate&TrackingNum=" + sessionData.getTrackingNumber(), "Terminate")).append("</td>");
		
		return output.toString();
	}
}
