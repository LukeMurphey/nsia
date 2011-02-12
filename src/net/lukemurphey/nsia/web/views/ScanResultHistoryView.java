package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.SiteGroupManagement;
import net.lukemurphey.nsia.SiteGroupManagement.SiteGroupDescriptor;
import net.lukemurphey.nsia.scan.ScanResult;
import net.lukemurphey.nsia.scan.ScanResultCode;
import net.lukemurphey.nsia.scan.ScanResultLoader;
import net.lukemurphey.nsia.scan.ScanRule;
import net.lukemurphey.nsia.scan.ScanRule.ScanResultLoadFailureException;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.Menu;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.templates.TemplateLoader;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class ScanResultHistoryView extends View {

	public static final String VIEW_NAME = "scan_result_history";
	
	public ScanResultHistoryView() {
		super("ScanResult/History", VIEW_NAME, Pattern.compile("[0-9]+"));
	}

	public static String getURL( int ruleID ) throws URLInvalidException{
		ScanResultHistoryView view = new ScanResultHistoryView();
		
		return view.createURL(ruleID);
	}
	
	public static String getURL( long ruleID ) throws URLInvalidException{
		ScanResultHistoryView view = new ScanResultHistoryView();
		
		return view.createURL(ruleID);
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {
		
		try{
			
			// 1 -- Get the rule
			int ruleID = Integer.valueOf( args[0] );
			
			// 2 -- Get the site group ID
			int siteGroupID;
			SiteGroupDescriptor siteGroup = null;
			
			try{
				siteGroupID = ScanRule.getAssociatedSiteGroupID(ruleID);
				SiteGroupManagement siteGroupMgmt = new SiteGroupManagement(Application.getApplication());
				siteGroup = siteGroupMgmt.getGroupDescriptor(siteGroupID);
			}
			catch( NotFoundException e ){
				Dialog.getDialog(response, context, data, "The Site-group associated with the rule could not be found", "Site-group Invalid", DialogType.WARNING);
				return true;	
			}
			
			// 3 -- Get the menu
			data.put("menu", Menu.getScanRuleMenu(context, siteGroup, ruleID));
			
			// 4 -- Get the breadcrumbs
			Vector<Link> breadcrumbs = new Vector<Link>();
			breadcrumbs.add(  new Link("Main Dashboard", MainDashboardView.getURL()) );
			breadcrumbs.add(  new Link("Site Group: " + siteGroup.getGroupName(), SiteGroupView.getURL(siteGroupID)) );
			breadcrumbs.add(  new Link("Edit Rule", RuleEditView.getURL(ruleID)) );
			breadcrumbs.add(  new Link("Scan History", ScanResultHistoryView.getURL(ruleID)) );
			data.put("breadcrumbs", breadcrumbs);
			
			data.put("title", "Scan Rule History");
			Shortcuts.addDashboardHeaders(request, response, data);
			
			// 5 -- Check rights
			if( Shortcuts.canRead( context.getSessionInfo(), siteGroup.getObjectId(), "View scan result history for site-group " + siteGroup.getGroupId() + " (" + siteGroup.getGroupName() + ")") == false ){
				Shortcuts.getPermissionDeniedDialog(response, data, "You do not permission to view this site-group");
				return true;
			}
			
			// 6 -- Get the search parameters
			long firstScanResultId = -1;
			long lastScanResultId = -1;
			long startEntry = -1;
			boolean resultsBefore = false;
			boolean filtered = false;
			
			try{
				if( request.getParameter("S") != null ){
					lastScanResultId = Long.valueOf( request.getParameter("S") );
					filtered = true;
				}
				
				if( request.getParameter("E") != null ){
					firstScanResultId = Long.valueOf( request.getParameter("E") );
					filtered = true;
				}
				
				String action = request.getParameter("Action");
				if( action != null && action.equalsIgnoreCase("Previous") ){
					startEntry = firstScanResultId;
					resultsBefore = true;
					filtered = true;
				}
				else if( action != null && action.equalsIgnoreCase("Next") ){
					startEntry = lastScanResultId;
					resultsBefore = false;
					filtered = true;
				}
			}
			catch(NumberFormatException e){
				Dialog.getDialog(response, context, data, "An invalid argument was provided", "Invalid Argument", DialogType.WARNING);
				return true;
			}
			
			// 7 -- Add dashboard headers that include a refresh panel if the search is on the first page
			if( filtered == false ){
				Shortcuts.addDashboardHeaders(request, response, data, getURL(ruleID));
			}
			
			// 8 -- Get the results
			ScanResult[] scanResults = null;
			
			int count = 20;
			
			long maxEntry = -1;
			long minEntry = -1;
			
			//	 8.1 -- Get the minimum and maximum result identifiers for the rule
			maxEntry = ScanResultLoader.getMaxEntry(ruleID);
			minEntry = ScanResultLoader.getMinEntry(ruleID);
			
			//	 8.2 -- Get the scan results
			if( startEntry > 0){
				scanResults =  ScanResultLoader.getScanResults(ruleID, startEntry, count, resultsBefore);
			}
			else{
				scanResults =  ScanResultLoader.getScanResults(ruleID, maxEntry, count, false);
				//scanResults = scanData.getScanResults(requestDescriptor.sessionIdentifier, maxEntry, scanRuleId, count, false);
			}
			
			if( scanResults.length > 0 ){
				lastScanResultId = scanResults[scanResults.length - 1].getScanResultID();
				firstScanResultId = scanResults[0].getScanResultID();
			}
			
			data.put("count", count);
			data.put("results", scanResults);
			data.put("minEntry", minEntry);
			data.put("maxEntry", maxEntry);
			data.put("lastScanResultID", lastScanResultId);
			data.put("firstScanResultID", firstScanResultId);
			data.put("ruleID", ruleID);
			data.put("siteGroupID", siteGroupID);
			
			data.put("SCAN_COMPLETED", ScanResultCode.SCAN_COMPLETED);
			data.put("PENDING", ScanResultCode.PENDING);
			data.put("READY", ScanResultCode.READY);
			data.put("SCAN_FAILED", ScanResultCode.SCAN_FAILED);
			data.put("UNREADY", ScanResultCode.UNREADY);
			
			// Add the current time to the variables list so that an argument can be added to the images list in order to force the browser to reload the images if they are reloaded by an AJAX request
			data.put("current_time_millis", System.currentTimeMillis());
			
			TemplateLoader.renderToResponse("ScanResultHistory.ftl", data, response);
		
		} catch(NoDatabaseConnectionException e){
			throw new ViewFailedException(e);
		} catch(SQLException e){
			throw new ViewFailedException(e);
		} catch(ScanResultLoadFailureException e){
			throw new ViewFailedException(e);
		} catch(InputValidationException e){
			throw new ViewFailedException(e);
		} catch(GeneralizedException e){
			throw new ViewFailedException(e);
		}
		
		return true;
	}

}
