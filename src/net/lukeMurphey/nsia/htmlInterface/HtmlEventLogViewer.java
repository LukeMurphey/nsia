package net.lukeMurphey.nsia.htmlInterface;

import java.util.Vector;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.InputValidationException;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.NotFoundException;
import net.lukeMurphey.nsia.eventLog.EventLogSeverity;
import net.lukeMurphey.nsia.eventLog.EventLogViewer.EventLogEntry;
import net.lukeMurphey.nsia.eventLog.EventLogViewer.EventLogFilter;
import net.lukeMurphey.nsia.trustBoundary.ApiEventLogViewer;

import org.apache.commons.lang.StringEscapeUtils;

public class HtmlEventLogViewer extends HtmlContentProvider {

	private static final int ENTRIES_PER_PAGE = 25; 
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, InputValidationException{
		return getHtml( requestDescriptor, null );
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, InputValidationException{
		
		// 0 -- Perform any pending actions
		if( actionDesc == null )
			actionDesc  = performAction( requestDescriptor );
		
		if( requestDescriptor.request.getParameter("EntryID") != null || requestDescriptor.request.getParameter("PrevEntryID") != null ){
			return getLogEntry(requestDescriptor, actionDesc );
		}
		
		return getLogList(requestDescriptor, actionDesc );

	}
	
	private static ContentDescriptor getLogEntry(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, InputValidationException {
		StringBuffer body = new StringBuffer();
		
		// 1 -- Output any messages
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		
		// 2 --Load the relevant parameters
		int logEntry = -1;
		
		if( requestDescriptor.request.getParameter("Action") != null && requestDescriptor.request.getParameter("Action").equalsIgnoreCase("[Previous]") && requestDescriptor.request.getParameter("PrevEntryID") != null ){
			try{
				logEntry = Integer.parseInt( requestDescriptor.request.getParameter("PrevEntryID") );
			}
			catch(NumberFormatException e){
				body.append(Html.getWarningDialog("Illegal Log Entry ID", "The log entry ID given is not a valid number."));
			}
		}
		
		else if( requestDescriptor.request.getParameter("EntryID") != null ){
			try{
				logEntry = Integer.parseInt( requestDescriptor.request.getParameter("EntryID") );
			}
			catch(NumberFormatException e){
				body.append(Html.getWarningDialog("Illegal Log Entry ID", "The log entry ID given is not a valid number."));
			}
		}
		
		if( logEntry > -1 ){
			ApiEventLogViewer logViewer = new ApiEventLogViewer(Application.getApplication());
			
			try{
				EventLogEntry entry = null;
				
				EventLogFilter filter = new EventLogFilter(3);
				filter.setEntryID(logEntry-1);
				EventLogEntry[] entries = logViewer.getEntries( requestDescriptor.sessionIdentifier, filter );
				
				int curPrevId = -1;
				int curNextId = -1;
				
				for(int c = 0; c < entries.length; c++){
					
					if(entries[c].getEntryID() == logEntry ){
						entry = entries[c];
					}
					else if(entries[c].getEntryID() == logEntry - 1 ){
						curPrevId = entries[c].getEntryID();
					}
					else if(entries[c].getEntryID() == logEntry + 1 ){
						curNextId = entries[c].getEntryID();
					}
					
				}
				
				if(entry == null ){
					throw new NotFoundException("The log entry item was not found");
				}
				
				body.append( "<div style=\"position:relative;\">" );
				
				if( entry.getSeverity() == EventLogSeverity.EMERGENCY ){
					body.append("<div style=\"position:absolute; left:0px;\"><img src=\"/32_Alert\" alt=\"Error\"></div>");
				}
				else if( entry.getSeverity() == EventLogSeverity.ALERT ){
					body.append("<div style=\"position:absolute; left:0px;\"><img src=\"/32_Alert\" alt=\"Error\"></div>");
				}
				else if( entry.getSeverity() == EventLogSeverity.CRITICAL ){
					body.append("<div style=\"position:absolute; left:0px;\"><img src=\"/32_Alert\" alt=\"Error\"></div>");
				}
				else if( entry.getSeverity() == EventLogSeverity.ERROR ){
					body.append("<div style=\"position:absolute; left:0px;\"><img src=\"/32_Warning\" alt=\"Error\"></div>");
				}
				else if( entry.getSeverity() == EventLogSeverity.WARNING ){
					body.append("<div style=\"position:absolute; left:0px;\"><img src=\"/32_Warning\" alt=\"Error\"></div>");
				}
				else if( entry.getSeverity() == EventLogSeverity.NOTICE ){
					body.append("<div style=\"position:absolute; left:0px;\"><img src=\"/32_Check\" alt=\"Error\"></div>");
				}
				else if( entry.getSeverity() == EventLogSeverity.INFORMATIONAL || entry.getSeverity() == EventLogSeverity.DEBUG){
					body.append("<div style=\"position:absolute; left:0px;\"><img src=\"/32_Information\" alt=\"Error\"></div>");
				}
				
				//body.append("<div><table cellpadding=\"2\">");
				body.append("<div style=\"position: relative; left: 32px;\"><table cellpadding=\"2\">");
				body.append("<tr><td class=\"Text_2\">Event</td><td>" + StringEscapeUtils.escapeHtml(entry.getMessage()) + "</td></tr>");
				body.append("<tr><td class=\"Text_2\">Severity</td><td>" + StringEscapeUtils.escapeHtml(entry.getSeverity().toString()) + "</td></tr>");
				body.append("<tr><td class=\"Text_2\">Date</td><td>" + StringEscapeUtils.escapeHtml( entry.getFormattedDate() ) + "</td></tr>");
				if( entry.getNotes() != null ){
					body.append("<tr><td class=\"Text_2\">Notes</td><td>" + StringEscapeUtils.escapeHtml( entry.getNotes() ) + "</td></tr>");
				}
				body.append("</table><p>");
				body.append( "<a href=\"EventLog\">[Return to Event Log List]</a><p>");
				body.append( "<form method=\"post\" action=\"EventLog\">" );
				
				if( curPrevId >= 0 ){
					body.append( "<input class=\"button\" type=\"Submit\" name=\"Action\" value=\"[Previous]\">" );
					body.append( "<input type=\"hidden\" name=\"PrevEntryID\" value=\"" + curPrevId + "\">" );
				}
				else{
					body.append( "<input disabled class=\"buttonDisabled\" type=\"Submit\" name=\"Action\" value=\"[Previous]\">" );
				}
				
				if( curNextId >= 0 ){
					body.append( "<input class=\"button\" type=\"Submit\" name=\"Action\" value=\"[Next]\">" );
					body.append( "<input type=\"hidden\" name=\"EntryID\" value=\"" + curNextId + "\">" );
				}
				else{
					body.append( "<input disabled class=\"buttonDisabled\" type=\"Submit\" name=\"Action\" value=\"[Next]\">" );
				}
				
				body.append( "</form></div></div>" );
				
			}
			catch(NotFoundException e){
				body.append(Html.getWarningDialog("Invalid Log Entry ID", "No log entries exist with the given ID."));
			}
		}
		
		
		// 3 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "Event Log", "/EventLog");
		navPath.addPathEntry( "Event Log Entry", "/EventLog?EntryID=" + logEntry);
		String navigationHtml = Html.getNavigationPath( navPath );
		
		
		// 4 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
		
		menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Add Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("User Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Users", "/UserManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New User", "/UserManagement?Action=Add", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Logged in Users", "/Sessions", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("Group Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Groups", "/GroupManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New Group", "/GroupManagement?Action=Add", MenuItem.LEVEL_TWO) );
		
		String menuOutput = Html.getMenu( menuItems );
		
		
		// 5 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor ("Event Log", pageOutput);
	}
	
	private static ContentDescriptor getLogList(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, InputValidationException {
		StringBuffer body = new StringBuffer();
		
		// 1 -- Output any messages
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		
		// 2 -- Get the parameters
		int severity = -1;
		String contentFilter = "";
		
		//	 2.1 -- Get the severity filter
		if( requestDescriptor.request.getParameter("Severity") != null ){
			try{
				severity = Integer.parseInt( requestDescriptor.request.getParameter("Severity") );
			}
			catch(NumberFormatException e){
				severity = -1;
			}
		}
		
		//	 2.2 -- Get the content filter
		contentFilter = requestDescriptor.request.getParameter("Content");
		
		int startEntry = -1;
		
		//	 2.3 -- Determine if the user clicked the "Previous" button
		boolean getEventsAfterID = false;
		if( requestDescriptor.request.getParameter("Action") != null && requestDescriptor.request.getParameter("Action").equalsIgnoreCase("[Previous]")){
			getEventsAfterID = false;
			
			try{
				startEntry = Integer.parseInt( requestDescriptor.request.getParameter("PrevID") ) - 1;
			}
			catch(NumberFormatException e){
				body.append(Html.getWarningDialog("Illegal Log Entry ID", "The log entry ID given is not a valid number."));
			}
		}
		//	 2.4 -- Determine if the user clicked the "Next" button
		else if( requestDescriptor.request.getParameter("Action") != null && requestDescriptor.request.getParameter("Action").equalsIgnoreCase("[Next]")){
			getEventsAfterID = true;
			
			try{
				startEntry = Integer.parseInt( requestDescriptor.request.getParameter("Start") );
			}
			catch(NumberFormatException e){
				body.append(Html.getWarningDialog("Illegal Log Entry ID", "The log entry ID given is not a valid number."));
			}
		}
		
		
		// 3 -- Prepare the filter and Internal log viewer 
		ApiEventLogViewer logViewer = new ApiEventLogViewer(Application.getApplication());
		
		//	 3.1 -- Create the log filter
		EventLogFilter filter = new EventLogFilter(ENTRIES_PER_PAGE);
		EventLogSeverity eventLogSeverity = null;
		
		if( severity >= 0 ){
			eventLogSeverity = EventLogSeverity.getSeverityById(severity);
			filter.setSeverityFilter(eventLogSeverity);
		}
		
		if( contentFilter != null && !contentFilter.isEmpty()){
			filter.setContentFilter(contentFilter);
		}
		
		//	 3.2 -- Compute the start entry
		int firstEntry = logViewer.getMinEntryID( requestDescriptor.sessionIdentifier, contentFilter, eventLogSeverity);
		
		int lastEntry = logViewer.getMaxEntryID( requestDescriptor.sessionIdentifier, contentFilter, eventLogSeverity);
		
		if( startEntry > -1 ){
			filter.setEntryID(startEntry, getEventsAfterID);
		}
		else{
			filter.setEntryID(lastEntry, false);
		}
		
		
		// 4 -- Display the form
		body.append( "<span class=\"Text_1\">Event Log</span><br>&nbsp;" );
		
		//	 4.1 -- Get the actual entries
		
		EventLogEntry[] entries;
		entries = logViewer.getEntries(requestDescriptor.sessionIdentifier, filter);
		
		//	 4.2 -- Output the filter form
		body.append( "<form action=\"EventLog\" method=\"get\"><table>");
		body.append( "<tr class=\"Background0\"><td colspan=\"99\"><span class=\"Text_3\">Log Entry Filter</span></td></tr>");
		body.append( "<tr class=\"Background1\"><td><span class=\"Text_3\">Severity Filter:</span></td><td>Display all entries that are at least as severe as <select name=\"Severity\">");
		
		body.append( getSeveritySelect( severity, null) );
		body.append( getSeveritySelect( severity, EventLogSeverity.EMERGENCY) );
		body.append( getSeveritySelect( severity, EventLogSeverity.ALERT) );
		body.append( getSeveritySelect( severity, EventLogSeverity.CRITICAL) );
		body.append( getSeveritySelect( severity, EventLogSeverity.ERROR) );
		body.append( getSeveritySelect( severity, EventLogSeverity.WARNING) );
		body.append( getSeveritySelect( severity, EventLogSeverity.NOTICE) );
		body.append( getSeveritySelect( severity, EventLogSeverity.INFORMATIONAL) );
		body.append( getSeveritySelect( severity, EventLogSeverity.DEBUG) );
		
		body.append( "</select></td></tr>");
		
		body.append( "<tr class=\"Background1\"><td><span class=\"Text_3\">Content Filter:</span></td><td>Display all entries that contain <input class=\"textInput\" type=\"Text\" name=\"Content\"");
		
		if( contentFilter != null ){
			body.append( " value=\"" + contentFilter +  "\"");
		}
		
		body.append( "></td></td>" );
		
		body.append( "<tr class=\"Background3\"><td colspan=\"99\"><input class=\"button\" type=\"Submit\" name=\"Apply\" value=\"Apply\"></td></tr>");
		body.append( "</table></form><p>");
		
		
		//	 4.3 -- Output the log data
		
		//		4.3.1a -- No entries found
		if( entries.length == 0 ){
			body.append( "&nbsp;<p>" );
					
			if( severity >= 0 || contentFilter != null){
				body.append( Html.getDialog("No log entries exist for the given filter.", "No Log Entries Match", "/32_Information", false) );
			}
			else
			{
				body.append( Html.getDialog("No log entries exist yet", "No Log Entries", "/32_Information", false) );// getWarningDialog("No Log Entries", "No log entries exist yet");
			}
		}
		//		4.3.1b -- Display the entries
		else{
			
			//body.append( "<table><tr class=\"Background0\"><td colspan=\"2\"><span class=\"Text_3\">Severity</span></td><td><span class=\"Text_3\">Time</span></td><td><span class=\"Text_3\">Event ID</span></td><td><span class=\"Text_3\">Message</span></td><td width=\"500px\"><span class=\"Text_3\">Notes</span></td></tr>" );
			body.append( "<table width=\"100%\"><tr class=\"Background0\"><td colspan=\"2\"><span class=\"Text_3\">Severity</span></td><td><span class=\"Text_3\">Time</span></td><td><span class=\"Text_3\">Event ID</span></td><td><span class=\"Text_3\">Message</span></td><td><span class=\"Text_3\">Notes</span></td></tr>" );

			for( int c = entries.length-1; c >= 0; c--){ 
				body.append( createRow( entries[c] ) );
			}
			
			body.append( "</table><p>" );
			
			// To show "next" and "previous" buttons...
			int nextStartEntry;

			nextStartEntry = entries[entries.length-1].getEntryID() + 1;

			body.append( "<form action=\"EventLog\" method=\"get\">" );

			// Determine if the "next" button should be displayed
			if(  entries[entries.length-1].getEntryID() < lastEntry ){
				body.append( "<input class=\"button\" type=\"Submit\" name=\"Action\" value=\"[Next]\">" );
			}
			else{
				body.append( "<input disabled class=\"buttonDisabled\" type=\"Submit\" name=\"Action\" value=\"[Next]\">" );
			}

			// Determine if the "previous" button should be displayed
			if(  entries[0].getEntryID() > firstEntry ){
				body.append( "<input class=\"button\" type=\"Submit\" name=\"Action\" value=\"[Previous]\">" );
			}
			else{
				body.append( "<input disabled class=\"buttonDisabled\" type=\"Submit\" name=\"Action\" value=\"[Previous]\">" );
			}

			if( contentFilter != null ){
				body.append( "<input class=\"button\" type=\"hidden\" name =\"Content\" value=\"" + contentFilter + "\">" );
			}

			if( severity >= 0 ){
				body.append( "<input class=\"button\" type=\"hidden\" name =\"Severity\" value=\"" + severity + "\">" );
			}

			body.append( "<input class=\"button\" type=\"hidden\" name=\"Start\" value=\"" + nextStartEntry + "\">" );
			body.append( "<input class=\"button\" type=\"hidden\" name=\"PrevID\" value=\"" + entries[0].getEntryID() + "\">" );

		}
		
		// 5 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "Event Log", "/EventLog");
		String navigationHtml = Html.getNavigationPath( navPath );
		
		
		// 6 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
		
		menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Add Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("User Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Users", "/UserManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New User", "/UserManagement?Action=Add", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Logged in Users", "/Sessions", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("Group Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Groups", "/GroupManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New Group", "/GroupManagement?Action=Add", MenuItem.LEVEL_TWO) );
		
		String menuOutput = Html.getMenu( menuItems );
		
		
		// 7 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor ("Event Log", pageOutput);
		
	}
	
	private static StringBuffer getSeveritySelect( int severity, EventLogSeverity logSeverity ){
		StringBuffer body = new StringBuffer();
		
		if( logSeverity == null){
			if( severity == -1){
				body.append( "<option selected value=\"-1\"></option>" );
			}
			else{
				body.append( "<option value=\"-1\"></option>" );
			}
		}
		else if( severity == logSeverity.getSeverity() ){
			body.append( "<option selected value=\"" + logSeverity.getSeverity() + "\">" + logSeverity.toString() + "</option>" );
		}
		else{
			body.append( "<option value=\"" + logSeverity.getSeverity() + "\">" + logSeverity.toString() + "</option>" );
		}
		
		return body;
	}
	
	private static StringBuffer createRow( EventLogEntry logEntry){
		
		StringBuffer buffer = new StringBuffer();
		
		buffer.append("<tr class=\"Background1\">");
		
		if( logEntry.getSeverity() == EventLogSeverity.EMERGENCY){
			buffer.append("<td class=\"StatRedSmall\"><span style=\"vertical-align: middle;\"><img src=\"/16_Alert\" alt=\"Emergency\"></span></td><td>" + logEntry.getSeverity().toString() + "</td>");
		}
		else if( logEntry.getSeverity() == EventLogSeverity.ALERT){
			buffer.append("<td class=\"StatRedSmall\"><span style=\"vertical-align: middle;\"><img src=\"/16_Alert\" alt=\"Alert\"></span></td><td>" + logEntry.getSeverity().toString() + "</td>");
		}
		else if( logEntry.getSeverity() == EventLogSeverity.CRITICAL){
			buffer.append("<td class=\"StatRedSmall\"><span style=\"vertical-align: middle;\"><img src=\"/16_Alert\" alt=\"Critical\"></span></td><td>" + logEntry.getSeverity().toString() + "</td>");
		}
		else if( logEntry.getSeverity() == EventLogSeverity.ERROR){
			buffer.append("<td class=\"StatYellowSmall\"><span style=\"vertical-align: middle;\"><img src=\"/16_Warning\" alt=\"Error\"></span></td><td>" + logEntry.getSeverity().toString() + "</td>");
		}
		else if( logEntry.getSeverity() == EventLogSeverity.WARNING){
			buffer.append("<td class=\"StatYellowSmall\"><span style=\"vertical-align: middle;\"><img src=\"/16_Warning\" alt=\"Warning\"></span></td><td>" + logEntry.getSeverity().toString() + "</td>");
		}
		else if( logEntry.getSeverity() == EventLogSeverity.INFORMATIONAL){
			buffer.append("<td width=\"16px\" class=\"StatBlueSmall\"><span style=\"vertical-align: middle;\"><img src=\"/16_Information\" alt=\"Informational\"></span></td><td>" + logEntry.getSeverity().toString() + "</td>");
		}
		else if( logEntry.getSeverity() == EventLogSeverity.NOTICE){
			buffer.append("<td class=\"StatGreenSmall\"><span style=\"vertical-align: middle;\"><img src=\"/16_Check\" alt=\"Notice\"></span></td><td>" + logEntry.getSeverity().toString() + "</td>");
		}
		else {//if( logEntry.getSeverity() == EventLogSeverity.DEBUG){
			buffer.append("<td class=\"StatBlueSmall\"><span style=\"vertical-align: middle;\"><img src=\"/16_Information\" alt=\"Debug\"></span></td><td>" + logEntry.getSeverity().toString() + "</td>");
		}
		
		buffer.append("<td>" + logEntry.getDate().toString() + "</td>");
		buffer.append("<td><a href=\"EventLog?EntryID=" + logEntry.getEntryID() + "\">[" + logEntry.getEntryID() + "]</a</td>");
		buffer.append("<td>" + Html.shortenString( StringEscapeUtils.escapeHtml( logEntry.getMessage() ), 70) + "</td>");
		
		if( logEntry.getNotes() != null){
			buffer.append("<td>" + Html.shortenString( StringEscapeUtils.escapeHtml( cutToComma( logEntry.getNotes() ) ), 40 ) + "</td>");
		}
		else{
			buffer.append("<td></td>");
		}
		
		
		buffer.append("</tr>");
		
		return buffer;
		
	}
	
	private static String cutToComma(String message){
		if( message != null ){
			int i = message.indexOf(",");
			if( i > 0 ){
				return message.substring(0, i) + " ...";
			}
		}
		return message;
	}
	
	private static ActionDescriptor performAction( WebConsoleConnectionDescriptor requestDescriptor) throws GeneralizedException, NoSessionException{
		
		//String action = requestDescriptor.request.getParameter("Action");
		
		// 1 -- No action specified
		return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
		
	}
	
}
