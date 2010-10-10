package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.SiteGroupScanResult;
import net.lukemurphey.nsia.Application.ApplicationStatusDescriptor;
import net.lukemurphey.nsia.scan.ScanData;
import net.lukemurphey.nsia.scan.ScanRule.ScanResultLoadFailureException;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.Menu;
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
		super("", VIEW_NAME);
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
		data.put("menu", Menu.getSystemMenu(context));
		
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
		
		// Filter the list of site groups down to the ones that the user can access
		Vector<SiteGroupScanResult> resultsFiltered = new Vector<SiteGroupScanResult>();
		
		for(int c = 0; c < results.length; c++){
			long objectID = results[c].getSiteGroupDescriptor().getObjectId();
			
			try {
				Shortcuts.checkRead(context.getSessionInfo(), objectID);
				resultsFiltered.add( results[c] );
			} catch (InsufficientPermissionException e) {
				// The user does not have permission to see this site-group. Don't let them see it.
			} catch (GeneralizedException e) {
				// An error occurred. Skip this site-group.
			} catch (NoSessionException e) {
				// User does not have a session. Don't let them see this site-group.
			}
		}
		
		// Covert the list of site-groups that the user can access down to the restricted list
		results = new SiteGroupScanResult[resultsFiltered.size()];
		resultsFiltered.toArray(results);
		
		// Populate data for the template
		data.put("sitegroups", results);
		
		// Render the resulting page
		TemplateLoader.renderToResponse("MainDashboard.ftl", data, response);
		
		return true;
	}

}
