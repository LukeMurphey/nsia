package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.SiteGroupScanResult;
import net.lukemurphey.nsia.Application.ApplicationStatusDescriptor;
import net.lukemurphey.nsia.scan.ScanData;
import net.lukemurphey.nsia.scan.ScanRule.ScanResultLoadFailureException;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.StandardViewList;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.templates.TemplateLoader;

public class MainDashboardView extends View {

	public static final String VIEW_NAME = "main_dashboard";
	
	public MainDashboardView() {
		super("Dashboard", VIEW_NAME);
	}
	
	public static String getURL() throws URLInvalidException{
		MainDashboardView view = new MainDashboardView();
		
		return view.createURL();
	}
	
	@Override
	protected boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException, URLInvalidException, IOException, ViewNotFoundException {
		
		data.put("title", "Main Dashboard");
		
		//Breadcrumbs
		Vector<Link> breadcrumbs = new Vector<Link>();
		breadcrumbs.add(  new Link("Main Dashboard", StandardViewList.getURL("main_dashboard")) );
		data.put("breadcrumbs", breadcrumbs);
		
		//Menu
		Vector<Link> menu = new Vector<Link>();
		menu.add( new Link("System Administration") );
		menu.add( new Link("System Status", SystemStatusView.getURL() ) );
		menu.add( new Link("System Configuration", StandardViewList.getURL("system_configuration")) );
		menu.add( new Link("Event Logs", StandardViewList.getURL("event_log")) );
		menu.add( new Link("Shutdown System", StandardViewList.getURL("system_shutdown")) );
		menu.add( new Link("Create Backup", StandardViewList.getURL("system_backup")) );
		
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
		Shortcuts.addDashboardHeaders(request, response, data, createURL());
		
		// Get the system status
		ApplicationStatusDescriptor system_status = Application.getApplication().getManagerStatus();
		data.put("system_status", system_status);
		
		// Get the site groups
		ScanData scanData = new ScanData(Application.getApplication());
		SiteGroupScanResult[] results;
		
		try {
			results = scanData.getSiteGroupStatus();
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		} catch (ScanResultLoadFailureException e) {
			throw new ViewFailedException(e);
		}
		
		//TODO Filter site groups that the user cannot access
		data.put("sitegroups", results);
		
		TemplateLoader.renderToResponse("MainDashboard.ftl", data, response);
		
		return true;
	}

}
