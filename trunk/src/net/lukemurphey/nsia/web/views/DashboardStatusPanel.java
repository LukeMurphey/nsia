package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.SiteGroupScanResult;
import net.lukemurphey.nsia.Application.ApplicationStatusDescriptor;
import net.lukemurphey.nsia.scan.ScanData;
import net.lukemurphey.nsia.scan.ScanRule.ScanResultLoadFailureException;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.templates.TemplateLoader;

public class DashboardStatusPanel extends View {

	public DashboardStatusPanel() {
		super("DashboardPanel/Status", "dashboard_panel_status");
	}

	@Override
	protected boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException, URLInvalidException, IOException, ViewNotFoundException {
		
		populateData(data, Application.getApplication());
		
		TemplateLoader.renderToResponse("DashboardStatusHeader.ftl", data, response);
		
		return true;
	}
	
	public enum StatusIndicator{
		GREEN, YELLOW, RED;
	}
	
	public class StatusDescriptor{
		
		private StatusIndicator status;
		private String status_desc;
		
		public StatusDescriptor(StatusIndicator status, String description){
			this.status = status;
			this.status_desc = description;
		}
		
		public boolean getStatusGreen(){
			return status == StatusIndicator.GREEN;
		}
		
		public boolean getStatusRed(){
			return status == StatusIndicator.RED;
		}
		
		public boolean getStatusYellow(){
			return status == StatusIndicator.YELLOW;
		}
		
		public String getStatusDescription(){
			return status_desc;
		}
	}
	
	/**
	 * Populate the map with the data necessary to render the view. 
	 * @param data
	 * @param app
	 * @throws ViewFailedException 
	 */
	private void populateData(Map<String, Object> data, Application app) throws ViewFailedException{
		
		// 1 -- Get manager status description
		ApplicationStatusDescriptor status_desc = app.getManagerStatus();
		
		if( status_desc.getOverallStatus() == ApplicationStatusDescriptor.STATUS_GREEN ){
			data.put("manager", new StatusDescriptor(StatusIndicator.GREEN, "Manager Status: " + status_desc.getShortDescription() ) );
		}
		else if( status_desc.getOverallStatus() == ApplicationStatusDescriptor.STATUS_RED ){
			data.put("manager", new StatusDescriptor(StatusIndicator.RED, "Manager Status: " + status_desc.getShortDescription() ) );
		}
		else{
			data.put("manager", new StatusDescriptor(StatusIndicator.YELLOW, "Manager Status: " + status_desc.getShortDescription() ) );
		}
		
		// 2 -- Get scanner status description
		int rejected = 0;
		int incomplete = 0;
		
		ScanData scanData = new ScanData(app);
		SiteGroupScanResult[] siteGroupScanResults;
		
		try {
			siteGroupScanResults = scanData.getSiteGroupStatus( );
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		} catch (ScanResultLoadFailureException e) {
			throw new ViewFailedException(e);
		}
		
		for (SiteGroupScanResult siteGroupScanResult : siteGroupScanResults) {
			if( siteGroupScanResult.getSiteGroupDescriptor().isEnabled() ){
				if(siteGroupScanResult.getDeviatingRules() > 0){
					rejected = rejected + 1;
				}
				else if(siteGroupScanResult.getIncompleteRules() > 0){
					incomplete = incomplete + 1;
				}
			}
		}
		
		if( rejected > 0 ){
			data.put("scanner", new StatusDescriptor(StatusIndicator.RED, "Current Status: " + rejected + " Non-Compliant Sites") );
		}
		else if( incomplete > 0 ){
			data.put("scanner", new StatusDescriptor(StatusIndicator.YELLOW, "Current Status: " + incomplete + " Sites Incompletely Evaluated") );
		}
		else{
			data.put("scanner", new StatusDescriptor(StatusIndicator.GREEN, "Current Status: No Deviations Noted") );
		}
		
		
		
	}
	
	/**
	 * Get the panel as a string with the HTML code.
	 * @param data
	 * @param app
	 * @return
	 * @throws ViewFailedException
	 */
	public String getPanel( HttpServletRequest request, Map<String, Object> data, Application app) throws ViewFailedException{
		
		populateData(data, app);
		
		return TemplateLoader.renderToString("DashboardStatusHeader.ftl", data);
	}

}
