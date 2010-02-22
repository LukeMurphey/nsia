package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.eventlog.EventLogSeverity;
import net.lukemurphey.nsia.eventlog.EventLogViewer;
import net.lukemurphey.nsia.eventlog.EventLogViewer.EventLogEntry;
import net.lukemurphey.nsia.eventlog.EventLogViewer.EventLogFilter;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.StandardViewList;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;
import net.lukemurphey.nsia.web.templates.TemplateLoader;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class EventLogEntryView extends View {

	public EventLogEntryView() {
		super("System/Eventlog", "event_log_entry", Pattern.compile("[0-9]{1,16}") );
	}

	public static String getURL(int logEntryID ) throws URLInvalidException{
		EventLogEntryView view = new EventLogEntryView();
		return view.createURL( logEntryID );
	}
	
	@Override
	protected boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException, URLInvalidException, IOException, ViewNotFoundException {
		
		// 1 -- Get the filter
		
		//	 1.1 -- Get the severity filter
		int severity = -1;
		if( request.getParameter("Severity") != null ){
			try{
				severity = Integer.valueOf( request.getParameter("Severity") );
			}
			catch( NumberFormatException e ){
				context.addMessage("The severity value provided is invalid", MessageSeverity.WARNING);
				severity = -1;
			}
		}
		
		//	 1.2 -- Get the content filter
		String contentFilter = null;
		if( request.getParameter("Content") != null ){
			contentFilter = request.getParameter("Content");
		}
		
		//	 1.3 -- Create the resulting filter
		EventLogFilter filter = new EventLogFilter(1);
		int entryID = Integer.valueOf( args[0] );
		filter.setContentFilter(contentFilter);
		filter.setSeverityFilter( EventLogSeverity.getSeverityBySyslogID( severity ) );
		
		// 2 -- Add the necessary options
		data.put("title", "Event Log");
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
		
		EventLogViewer logViewer = new EventLogViewer(Application.getApplication());
		
		try{
			// Determine if a previous item exists
			if( entryID > 0 ){
				filter.setEntryID(entryID - 1);
				EventLogEntry[] entries = logViewer.getEntries(filter);
				
				if( entries != null && entries.length > 0 ){
					data.put("curPrevId", entries[0].getEntryID());
				}
			}
			
			// Determine if a next item exists
			filter.setEntryID(entryID + 1);
			EventLogEntry[] entries = logViewer.getEntries(filter);
			
			if( entries != null && entries.length > 0 ){
				data.put("curNextId", entries[0].getEntryID());
			}
		} catch( SQLException e ){
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		}
		
		//Breadcrumbs
		Vector<Link> breadcrumbs = new Vector<Link>();
		breadcrumbs.add(  new Link("Main Dashboard", StandardViewList.getURL("main_dashboard")) );
		breadcrumbs.add(  new Link("Event Log",StandardViewList.getURL("event_log")) );
		breadcrumbs.add(  new Link("Log Entry: " + entryID, createURL( entryID )) );
		data.put("breadcrumbs", breadcrumbs);
		
		//Menu
		Vector<Link> menu = new Vector<Link>();
		menu.add( new Link("System Administration") );
		menu.add( new Link("System Status", StandardViewList.getURL("system_status")) );
		menu.add( new Link("System Configuration", StandardViewList.getURL("system_configuration")) );
		
		menu.add( new Link("Scanning Engine") );
		if( Application.getApplication().getScannerController().scanningEnabled() ){
			menu.add( new Link("Stop Scanner", StandardViewList.getURL("scanner_stop")) );
		}
		else{
			menu.add( new Link("Start Scanner", StandardViewList.getURL("scanner_start")) );
		}
		menu.add( new Link("View Definitions", StandardViewList.getURL(DefinitionsView.VIEW_NAME)) );
		
		data.put("menu", menu);
		
		//Get the dashboard headers
		Shortcuts.addDashboardHeaders(request, response, data);
		
		// 3 -- Get the log entry and related information
		try {
			data.put("entry", logViewer.getEntry(entryID));
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		} catch (NotFoundException e) {
			Dialog.getDialog(response, context, data, "A log entry with the given identified could not be found", "Log Entry Not Found", DialogType.WARNING);
			return true;
		}
		
		// 4 -- Provide the filtering options
		data.put("severity", severity);
		data.put("contentFilter", contentFilter);
		
		TemplateLoader.renderToResponse("EventLogEntry.ftl", data, response);
		
		return true;
	}

}
