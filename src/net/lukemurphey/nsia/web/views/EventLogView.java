package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.eventlog.EventLogSeverity;
import net.lukemurphey.nsia.eventlog.EventLogViewer;
import net.lukemurphey.nsia.eventlog.EventLogViewer.EventLogEntry;
import net.lukemurphey.nsia.eventlog.EventLogViewer.EventLogFilter;
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
import net.lukemurphey.nsia.web.templates.TemplateLoader;

public class EventLogView extends View {

	public static final int ENTRIES_PER_PAGE = 25;
	
	public EventLogView() {
		super("System/Eventlog", "event_log");
	}
	
	public static String getURL() throws URLInvalidException{
		EventLogView view = new EventLogView();
		return view.createURL();
	}
	
	/**
	 * Represents a set of log entries returned from a query.
	 * @author Luke
	 *
	 */
	private static class LogEntries{
		EventLogEntry[] logEntries;
		int firstEntry;
		int lastEntry;
		
		public LogEntries( int firstEntry, int lastEntry, EventLogEntry[] logEntries){
			this.logEntries = logEntries;
			this.firstEntry = firstEntry;
			this.lastEntry = lastEntry;
		}
	}
	
	/**
	 * Gets the log entries for the given filter.
	 * @param severity
	 * @param contentFilter
	 * @param sessionIdentifier
	 * @param startEntry
	 * @param getNext
	 * @return
	 * @throws ViewFailedException
	 */
	private LogEntries getLogEntries( int severity, String contentFilter, int startEntry, boolean getNext ) throws ViewFailedException{
		
		try{
			
			// 1 -- Determine if the user clicked the "Previous" button
			boolean getEventsAfterID = getNext;		
			EventLogViewer logViewer = new EventLogViewer(Application.getApplication());
			
			// 2 -- Create the log filter
			EventLogFilter filter = new EventLogFilter(ENTRIES_PER_PAGE);
			EventLogSeverity eventlogSeverity = null;
			
			if( severity == EventLogSeverity.EMERGENCY.getSyslogEquivalent() ){
				eventlogSeverity = EventLogSeverity.EMERGENCY;
			}
			else if( severity == EventLogSeverity.CRITICAL.getSyslogEquivalent() ){
				eventlogSeverity = EventLogSeverity.CRITICAL;
			}
			else if( severity == EventLogSeverity.ALERT.getSyslogEquivalent() ){
				eventlogSeverity = EventLogSeverity.ALERT;
			}
			else if( severity == EventLogSeverity.ERROR.getSyslogEquivalent() ){
				eventlogSeverity = EventLogSeverity.ERROR;
			}
			else if( severity == EventLogSeverity.NOTICE.getSyslogEquivalent() ){
				eventlogSeverity = EventLogSeverity.NOTICE;
			}
			else if( severity == EventLogSeverity.WARNING.getSyslogEquivalent() ){
				eventlogSeverity = EventLogSeverity.WARNING;
			}
			else if( severity == EventLogSeverity.INFORMATIONAL.getSyslogEquivalent() ){
				eventlogSeverity = EventLogSeverity.INFORMATIONAL;
			}
			else if( severity == EventLogSeverity.DEBUG.getSyslogEquivalent() ){
				eventlogSeverity = EventLogSeverity.DEBUG;
			}
			
			if( eventlogSeverity != null ){
				filter.setSeverityFilter(eventlogSeverity);
			}
			
			if( contentFilter != null && !contentFilter.isEmpty()){
				filter.setContentFilter(contentFilter);
			}
			
			// 3 -- Compute the start entry
			int firstEntry = logViewer.getMinEntryID( contentFilter, eventlogSeverity);
			int lastEntry = logViewer.getMaxEntryID( contentFilter, eventlogSeverity);
			
			if( startEntry > -1 ){
				filter.setEntryID(startEntry, getEventsAfterID);
			}
			else{
				filter.setEntryID(lastEntry, false);
			}
			
			EventLogEntry[] entries = logViewer.getEntries(filter);
			
			return new LogEntries( firstEntry, lastEntry, entries );
		}
		catch( SQLException e ){
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		}
	}
	
	@Override
	protected boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException, URLInvalidException, IOException, ViewNotFoundException {
		
		// 1 -- Prepare the view
		data.put("title", "Event Log");
		
		//Breadcrumbs
		Vector<Link> breadcrumbs = new Vector<Link>();
		breadcrumbs.add(  new Link("Main Dashboard", StandardViewList.getURL("main_dashboard")) );
		breadcrumbs.add(  new Link("Event Log", createURL()) );
		data.put("breadcrumbs", breadcrumbs);
		
		//Menu
		data.put("menu", Menu.getSystemMenu(context));
		
		//Get the dashboard headers
		Shortcuts.addDashboardHeaders(request, response, data);
		
		// 2 -- Check permissions
		try {
			if( Shortcuts.hasRight( context.getSessionInfo(), "System.Information.View", "View event logs") == false ){
				Shortcuts.getPermissionDeniedDialog(response, data, "You do not have permission to view the event logs");
				return true;
			}
		} catch (GeneralizedException e) {
			throw new ViewFailedException(e);
		}
		
		// 3 -- Get the filter
		int startEntry = -1;
		boolean getEventsAfterID = false;
		boolean isFiltered = false;
		
		//	 3.1 -- Determine if the user clicked the "Next" button
		if( request.getParameter("Action") != null && request.getParameter("Action").equalsIgnoreCase("Previous")){
			isFiltered = true;
			getEventsAfterID = false;
			
			try{
				startEntry = Integer.parseInt( request.getParameter("PrevID") ) - 1;
			}
			catch(NumberFormatException e){
				e.printStackTrace();
				//body.append(Html.getWarningDialog("Illegal Log Entry ID", "The log entry ID given is not a valid number."));
			}
		}
		
		//	 3.2 -- Determine if the user clicked the "Next" button
		else if( request.getParameter("Action") != null && request.getParameter("Action").equalsIgnoreCase("Next")){
			isFiltered = true;
			getEventsAfterID = true;
			
			try{
				startEntry = Integer.parseInt( request.getParameter("Start") );
			}
			catch(NumberFormatException e){
				//body.append(Html.getWarningDialog("Illegal Log Entry ID", "The log entry ID given is not a valid number."));
			}
		}
		
		//	 3.3 -- Get the severity filter
		int severity = -1;
		if( request.getParameter("Severity") != null ){
			isFiltered = true;
			
			try{
				severity = Integer.valueOf( request.getParameter("Severity") );
			}
			catch( NumberFormatException e ){
				context.addMessage("The severity value provided is invalid", MessageSeverity.WARNING);
				severity = -1;
			}
		}
		
		//	 3.4 -- Get the content filter
		String contentFilter = null;
		if( request.getParameter("Content") != null ){
			isFiltered = true;
			contentFilter = request.getParameter("Content");
		}
		
		
		// 4 -- Add the necessary options
		data.put("severity", severity);
		data.put("contentfilter", contentFilter);		
		data.put("emergency", EventLogSeverity.EMERGENCY);
		data.put("alert", EventLogSeverity.ALERT);
		data.put("critical", EventLogSeverity.CRITICAL);
		data.put("error", EventLogSeverity.ERROR);
		data.put("warning", EventLogSeverity.WARNING);
		data.put("notice", EventLogSeverity.NOTICE);
		data.put("informational", EventLogSeverity.INFORMATIONAL);
		data.put("debug", EventLogSeverity.DEBUG);

		// If the view is not being filtered, then replace the dashboard headers with one that refreshes
		if( !isFiltered ){
			Shortcuts.addDashboardHeaders(request, response, data, createURL());
		}
		
		LogEntries entries = getLogEntries( severity, contentFilter, startEntry, getEventsAfterID);
		
		if( entries.logEntries.length > 0 ){
			data.put("hasnext", (entries.lastEntry > entries.logEntries[entries.logEntries.length-1].getEntryID()) );
			data.put("hasprev", (entries.firstEntry < entries.logEntries[0].getEntryID()) );
		}
		
		data.put("entries", entries.logEntries );
		
		TemplateLoader.renderToResponse("EventLog.ftl", data, response);
		
		return true;
	}

}
