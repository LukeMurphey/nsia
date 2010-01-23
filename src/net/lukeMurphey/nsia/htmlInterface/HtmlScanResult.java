package net.lukemurphey.nsia.htmlInterface;

import java.util.Vector;

//import org.apache.commons.lang.StringEscapeUtils;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.ScannerController;
import net.lukemurphey.nsia.scan.HttpSeekingScanResult;
import net.lukemurphey.nsia.scan.HttpSeekingScanRule;
import net.lukemurphey.nsia.scan.HttpStaticScanResult;
import net.lukemurphey.nsia.scan.HttpStaticScanRule;
import net.lukemurphey.nsia.scan.ScanResult;
import net.lukemurphey.nsia.scan.ScanResultCode;
import net.lukemurphey.nsia.scan.ServiceScanResult;
import net.lukemurphey.nsia.scan.ServiceScanRule;
import net.lukemurphey.nsia.trustBoundary.ApiScanData;
import net.lukemurphey.nsia.trustBoundary.ApiScannerController;

public class HtmlScanResult extends HtmlContentProvider {
	
	private static final String tableEnd = "</tbody></table>";
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException{
		return getHtml( requestDescriptor, null );
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException{
		
		/*if( actionDesc == null ){
			actionDesc = performAction( requestDescriptor );
		}*/
		
		if( requestDescriptor.request.getParameter("ResultID") != null || (actionDesc != null && actionDesc.addData != null && actionDesc.addData instanceof ScanResult ) ){
			return getScanResultView( requestDescriptor, actionDesc );
		}
		else{
			return getScanResultsList( requestDescriptor, actionDesc );
		}
	}
	
	private static ContentDescriptor getScanResultsList(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException{
		
		StringBuffer body = new StringBuffer();
		
		body.append(Html.renderMessages(requestDescriptor.userId));

		
		// 1 -- Create the page content
		
		//	1.1-- Get the rule to load
		long scanRuleId = -1;
		
		try{
			scanRuleId = Long.valueOf( requestDescriptor.request.getParameter("RuleID") );
		}
		catch(NumberFormatException e){
			return HtmlProblemDialog.getHtml(requestDescriptor, "Invalid Parameter", "The rule identifier provided is invalid", HtmlProblemDialog.DIALOG_WARNING, "Console", "Main Dashboard");
		}
		
		//	 1.2 -- Get the result ID of the first to load (if supplied)
		long firstScanResultId = -1;
		long lastScanResultId = -1;
		long startEntry = -1;
		boolean resultsBefore = false;
		
		try{
			if( requestDescriptor.request.getParameter("S") != null ){
				lastScanResultId = Long.valueOf( requestDescriptor.request.getParameter("S") );
			}
			
			if( requestDescriptor.request.getParameter("E") != null ){
				firstScanResultId = Long.valueOf( requestDescriptor.request.getParameter("E") );
			}
			
			String action = requestDescriptor.request.getParameter("Action");
			if( action != null && action.equalsIgnoreCase("Previous") ){
				startEntry = firstScanResultId;
				resultsBefore = true;
			}
			else if( action != null && action.equalsIgnoreCase("Next") ){
				startEntry = lastScanResultId;
				resultsBefore = false;
			}
		}
		catch(NumberFormatException e){
			return HtmlProblemDialog.getHtml(requestDescriptor, "Invalid Parameter", "The rule identifier provided is invalid", HtmlProblemDialog.DIALOG_WARNING, "Console", "Main Dashboard");
		}
		
		ApiScanData scanData = new ApiScanData(Application.getApplication());
		ScanResult[] scanResults = null;
		long siteGroupId = -1;
		int count = 20;
		
		long maxEntry = -1;
		long minEntry = -1;
		
		try{
			// 1.2 -- Get the associated site group identifier
			siteGroupId = scanData.getAssociatedSiteGroup(requestDescriptor.sessionIdentifier, scanRuleId);

			// 1.3 -- Get the minimum and maximum result identifiers for the site group
			maxEntry = scanData.getMaxEntry(requestDescriptor.sessionIdentifier, scanRuleId);
			minEntry = scanData.getMinEntry(requestDescriptor.sessionIdentifier, scanRuleId);
			
			// 1.4 -- Get the scan results
			if( startEntry > 0){
				scanResults = scanData.getScanResults(requestDescriptor.sessionIdentifier, startEntry, scanRuleId, count, resultsBefore);
			}
			else{
				scanResults = scanData.getScanResults(requestDescriptor.sessionIdentifier, maxEntry, scanRuleId, count, false);
			}
			
		}
		catch(InsufficientPermissionException e){
			body.append( Html.getWarningDialog("Insufficient Permission", "You do not have permission to view the scan results for the site group") );
		}
		catch(NotFoundException e){
			body.append( Html.getWarningDialog("Site Group Not Found", "No site group exists with the given identifier") );
		}
		
		
		
		//	 1.5 -- Display the results (if successfully loaded)
		if( scanResults == null || scanResults.length == 0 ){
			body.append( Html.getDialog("No scan results exist for the given rule yet.", "No Scan Results", "/32_Information", false) );
		}
		else{
			lastScanResultId = scanResults[scanResults.length - 1].getScanResultID();
			firstScanResultId = scanResults[0].getScanResultID();
			
			body.append( Html.getSectionHeader( "Scan Results", "Viewing " + Math.min(count, scanResults.length) + " results" ) );
			
			body.append( "<table width=\"660px\" class=\"DataTable\"><tbody><tr><td class=\"BackgroundLoading1\" height=\"120\">" );
			body.append( "<img src=\"/RuleScanHistory?RuleID=" + scanRuleId + "&S=" + firstScanResultId + "\" alt=\"Rule History\">" );
			body.append( "</tbody></td></tr></table>" );
			
			body.append( "<table width=\"660px\" class=\"DataTable\" summary=\"\">" );
			
			body.append( "<thead>" );
			
			body.append( "<tr><td width=\"48px\">Status</td><td>Result</td><td colspan=\"3\">Time Scanned</td></tr>" );
			
			body.append( "</thead><tbody>" );
			
			for( int c = 0; c < scanResults.length; c++){
				body.append( createRow(scanResults[c]) );
			}
			
			body.append( tableEnd );
			
			body.append("<br><form action=\"ScanResult\">");
			body.append("<input type=\"hidden\" name=\"RuleID\" value=\"" + scanRuleId + "\">");
			
			if( maxEntry > -1 && firstScanResultId == maxEntry ){
				body.append("<input disabled=\"true\" class=\"buttonDisabled\" type=\"submit\" name=\"Action\" value=\"Previous\">");
			}
			else{
				body.append("<input class=\"button\" type=\"submit\" name=\"Action\" value=\"Previous\">");
			}
			
			if( minEntry > -1 && lastScanResultId == minEntry ){
				body.append("<input disabled=\"true\" class=\"buttonDisabled\" type=\"submit\" name=\"Action\" value=\"Next\">");
			}
			else{
				body.append("<input class=\"button\" type=\"submit\" name=\"Action\" value=\"Next\">");
			}
			
			body.append("<input type=\"hidden\" name=\"S\" value=\"" + lastScanResultId + "\">");
			body.append("<input type=\"hidden\" name=\"E\" value=\"" + firstScanResultId + "\">");
			
			body.append("</form>");
		}
		
		
		// 2 -- Get the navigation bar
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "Site Group", "/SiteGroup?SiteGroupID=" + siteGroupId );
		//navPath.addPathEntry( "Scan Rule", "/ScanRule?RuleID=" + scanRuleId );
		navPath.addPathEntry( "Scan Result History", "/ScanResult?RuleID=" + scanRuleId );
		String navigationHtml = Html.getNavigationPath( navPath );
		
		
		// 3 -- Get the menu items
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
				
		menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
		//menuItems.add( new MenuItem("List Site Groups", "Console", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Scan Results", "/SiteGroup?Action=Edit&SiteGroupID=" + siteGroupId, MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add Site Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Edit Site Group", "/SiteGroup?Action=Edit&SiteGroupID=" + siteGroupId, MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("Scanning Engine", null, MenuItem.LEVEL_ONE) );
		ApiScannerController scannerController = new ApiScannerController(Application.getApplication());
		
		try{
			if(scannerController.getScanningState(requestDescriptor.sessionIdentifier) == ScannerController.ScannerState.RUNNING)
				menuItems.add( new MenuItem("Stop Scanner", "/SystemStatus?Action=StopScanner", MenuItem.LEVEL_TWO) );
			else
				menuItems.add( new MenuItem("Start Scanner", "/SystemStatus?Action=StartScanner", MenuItem.LEVEL_TWO) );
		}
		catch(InsufficientPermissionException e){
			//Ignore this, it just means we can show the option to start and restart the scanner
		}
		menuItems.add( new MenuItem("View Definitions", "/Definitions", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Default Scan Policy", "/ScanPolicy", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Site Scan", "/SiteScan", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Exceptions", "/ExceptionManagement?SiteGroupID=" + siteGroupId + "&RuleID=" + scanRuleId, MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("User Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Users", "/UserManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New User", "/UserManagement?Action=Add", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Logged in Users", "/Sessions", MenuItem.LEVEL_TWO) );
				
		menuItems.add( new MenuItem("Group Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Groups", "/GroupManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New Group", "/GroupManagement?Action=Add", MenuItem.LEVEL_TWO) );
		
		String menuOutput = Html.getMenu( menuItems );
		
		
		// 4 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor( "View Scan Results", pageOutput );
	}
	
	private static ContentDescriptor getScanResultView(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException{
		
		StringBuffer body = new StringBuffer();
		
		/*if( actionDesc == null ){
			actionDesc = performAction( requestDescriptor );
		}*/
		
		actionDesc = new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
		
		body.append(Html.renderMessages(requestDescriptor.userId));

		
		// 1 -- Create the page content
		
		//	1.1 -- Determine if a scan result was passed with the action descriptor
		ScanResult scanResult = null;
		
		if( actionDesc.addData != null && actionDesc.addData instanceof ScanResult ){
			scanResult = (ScanResult)actionDesc.addData;
		}
		
		//	1.2 -- Get the result to load
		long scanResultId = -1;
		
		if(scanResult == null){
			try{
				scanResultId = Long.valueOf( requestDescriptor.request.getParameter("ResultID") );
			}
			catch(NumberFormatException e){
				return HtmlProblemDialog.getHtml(requestDescriptor, "Invalid Parameter", "The scan result identifier provided is invalid", HtmlProblemDialog.DIALOG_WARNING, "Console", "Main Dashboard");
			}
		}
		else{
			scanResultId = scanResult.getScanResultID();
		}
		
		
		long scanRuleId = -1;
		
		ApiScanData scanData = new ApiScanData(Application.getApplication());
		
		long siteGroupId = -1;
		
			try{
				// 1.2 -- Get the scan result
				if( scanResult == null ){
					scanResult = scanData.getScanResult(requestDescriptor.sessionIdentifier, scanResultId);
					
					if( scanResult == null){
						//Scan result was not found
						//TODO deal with error
					}
					
					// 1.3 -- Get the rule to load
					scanRuleId = scanResult.getRuleID();
					
					// 1.4 -- Get the associated site group identifier
					siteGroupId = scanData.getAssociatedSiteGroup(requestDescriptor.sessionIdentifier, scanRuleId);
				}
				
				
				if( scanResult.getRuleType().equalsIgnoreCase(HttpSeekingScanRule.RULE_TYPE)){
					body.append( Html.getSectionHeader("HTTP Auto-Discovery Scan Result", "Scanned: " + scanResult.getScanTime() ) );
					
					body.append( HtmlSeekingScanResult.getScanResultReport((HttpSeekingScanResult)scanResult, requestDescriptor, siteGroupId) );
				}
				else if( scanResult.getRuleType().equalsIgnoreCase(ServiceScanRule.RULE_TYPE)){
					body.append( Html.getSectionHeader("Service Scan Result", "Scanned: " + scanResult.getScanTime() ) );
					
					body.append( HtmlServiceScanResult.getScanResultReport((ServiceScanResult)scanResult, requestDescriptor, siteGroupId) );
				}
				else if( scanResult.getRuleType().equalsIgnoreCase(HttpStaticScanRule.RULE_TYPE)){
					body.append( Html.getSectionHeader("HTTP Static Content Scan Result", "Scanned: " + scanResult.getScanTime() ) );
					body.append( HtmlStaticScanResult.getScanResultReport((HttpStaticScanResult)scanResult, requestDescriptor) );
				}
				
			}
			catch(InsufficientPermissionException e){
				body.append( Html.getWarningDialog("Insufficient Permission", "You do not have permission to view the scan results for the site group") );
			}
			catch(NotFoundException e){
				body.append( Html.getWarningDialog("Site Group Not Found", "No site group exists with the given identifier") );
			}
		
		
		// 2 -- Get the navigation bar
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "Site Group", "/SiteGroup?SiteGroupID=" + siteGroupId );
		//navPath.addPathEntry( "Scan Rule", "/ScanRule?RuleID=" + scanRuleId );
		navPath.addPathEntry( "Scan Result History", "/ScanResult?RuleID=" + scanRuleId );
		navPath.addPathEntry( "View Scan Result", "/ScanResult?ResultID=" + scanResultId );
		String navigationHtml = Html.getNavigationPath( navPath );
		
		
		// 3 -- Get the menu items
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
				
		menuItems.add( new MenuItem("Scan Rule", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("View Exceptions", "/ExceptionManagement?SiteGroupID=" + siteGroupId + "&RuleID=" + scanRuleId , MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Edit Rule", "/ScanRule?SiteGroupID=" + siteGroupId + "&RuleID=" + scanRuleId , MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
		//menuItems.add( new MenuItem("List Site Groups", "Console", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Scan Results", "/SiteGroup?Action=Edit&SiteGroupID=" + siteGroupId, MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add Site Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Edit Site Group", "/SiteGroup?Action=Edit&SiteGroupID=" + siteGroupId, MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("Scanning Engine", null, MenuItem.LEVEL_ONE) );
		ApiScannerController scannerController = new ApiScannerController(Application.getApplication());
		
		try{
			if(scannerController.getScanningState(requestDescriptor.sessionIdentifier) == ScannerController.ScannerState.RUNNING)
				menuItems.add( new MenuItem("Stop Scanner", "/SystemStatus?Action=StopScanner", MenuItem.LEVEL_TWO) );
			else
				menuItems.add( new MenuItem("Start Scanner", "/SystemStatus?Action=StartScanner", MenuItem.LEVEL_TWO) );
		}
		catch(InsufficientPermissionException e){
			//Ignore this, it just means we can show the option to start and restart the scanner
		}
		menuItems.add( new MenuItem("View Definitions", "/Definitions", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Default Scan Policy", "/ScanPolicy", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Site Scan", "/SiteScan", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("User Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Users", "/UserManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New User", "/UserManagement?Action=Add", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Logged in Users", "/Sessions", MenuItem.LEVEL_TWO) );
				
		menuItems.add( new MenuItem("Group Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Groups", "/GroupManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New Group", "/GroupManagement?Action=Add", MenuItem.LEVEL_TWO) );
		
		String menuOutput = Html.getMenu( menuItems );
		
		
		// 4 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor( "View Scan Results", pageOutput );
	}
	
	public static ContentDescriptor getScanResultView(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc, ScanResult scanResult ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException{
		
		StringBuffer body = new StringBuffer();
		
		body.append(Html.renderMessages(requestDescriptor.userId));

		
		// 1 -- Create the page content
		ApiScanData scanData = new ApiScanData(Application.getApplication());
		
		long siteGroupId = -1;

		try{
			siteGroupId = scanData.getAssociatedSiteGroup(requestDescriptor.sessionIdentifier, scanResult.getRuleID());
			
			if( scanResult.getRuleType().equalsIgnoreCase(HttpSeekingScanRule.RULE_TYPE)){
				body.append( Html.getSectionHeader("HTTP Auto-Discovery Scan Result", "Scanned: " + scanResult.getScanTime() ) );
				body.append( HtmlSeekingScanResult.getScanResultReport((HttpSeekingScanResult)scanResult, requestDescriptor) );
			}
			else if( scanResult.getRuleType().equalsIgnoreCase(HttpStaticScanRule.RULE_TYPE)){
				body.append( Html.getSectionHeader("HTTP Static Content Scan Result", "Scanned: " + scanResult.getScanTime() ) );
				body.append( HtmlStaticScanResult.getScanResultReport((HttpStaticScanResult)scanResult, requestDescriptor) );
			}

		}
		catch(InsufficientPermissionException e){
			body.append( Html.getWarningDialog("Insufficient Permission", "You do not have permission to view the scan results for the site group") );
		}
		catch(NotFoundException e){
			body.append( Html.getWarningDialog("Site Group Not Found", "No site group exists with the given identifier") );
		}
		
		
		// 2 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "Site Group", "/SiteGroup?SiteGroupID=" + siteGroupId );
		//navPath.addPathEntry( "Scan Rule", "/ScanRule?RuleID=" + scanRuleId );
		navPath.addPathEntry( "Scan Result History", "/ScanResult?RuleID=" + scanResult.getRuleID() );
		navPath.addPathEntry( "View Scan Result", "/ScanResult?ResultID=" + scanResult.getScanResultID() );
		String navigationHtml = Html.getNavigationPath( navPath );
		
		
		// 3 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
				
		menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
		//menuItems.add( new MenuItem("List Site Groups", "Console", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Scan Results", "/SiteGroup?Action=Edit&SiteGroupID=" + siteGroupId, MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add Site Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Edit Site Group", "/SiteGroup?Action=Edit&SiteGroupID=" + siteGroupId, MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("Scanning Engine", null, MenuItem.LEVEL_ONE) );
		ApiScannerController scannerController = new ApiScannerController(Application.getApplication());
		
		try{
			if(scannerController.getScanningState(requestDescriptor.sessionIdentifier) == ScannerController.ScannerState.RUNNING)
				menuItems.add( new MenuItem("Stop Scanner", "/SystemStatus?Action=StopScanner", MenuItem.LEVEL_TWO) );
			else
				menuItems.add( new MenuItem("Start Scanner", "/SystemStatus?Action=StartScanner", MenuItem.LEVEL_TWO) );
		}
		catch(InsufficientPermissionException e){
			//Ignore this, it just means we can show the option to start and restart the scanner
		}
		menuItems.add( new MenuItem("View Definitions", "/Definitions", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Default Scan Policy", "/ScanPolicy", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Site Scan", "/SiteScan", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("User Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Users", "/UserManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New User", "/UserManagement?Action=Add", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Logged in Users", "/Sessions", MenuItem.LEVEL_TWO) );
				
		menuItems.add( new MenuItem("Group Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Groups", "/GroupManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New Group", "/GroupManagement?Action=Add", MenuItem.LEVEL_TWO) );
		
		String menuOutput = Html.getMenu( menuItems );
		
		
		// 4 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor( "View Scan Results", pageOutput );
	}
	
	/**
	 * Create a row that summarizes a rule.
	 * @param status
	 * @param deviations
	 * @param type
	 * @param description
	 * @param ruleId
	 * @param link
	 * @return
	 */
	private static String createRow( ScanResult scanResult ){
		
		String output = "<tr>";
		
		// 1 -- Output the status icon
		if( scanResult.getDeviations() == 0 && scanResult.getResultCode() == ScanResultCode.SCAN_COMPLETED ){
			output += "<td align=\"center\" class=\"StatGreen\"><img src=\"/22_Check\" alt=\"ok\"></td>";
			output += "<td class=\"Background1\">0 deviations&nbsp;&nbsp;</td>";
		}
		else if( scanResult.getDeviations() > 0 ){
			output += "<td align=\"center\" class=\"StatRed\"><img src=\"/22_Alert\" alt=\"alert\"></td>";
			output += "<td class=\"Background1\">" + scanResult.getDeviations() + " deviations&nbsp;&nbsp;</td>";
		}
		else {
			output += "<td align=\"center\" class=\"StatYellow\"><img src=\"/22_Warning\" alt=\"warning\"></td>";
			output += "<td class=\"Background1\">Scan did not complete successfully: " + scanResult.getResultCode().getDescription() + "&nbsp;&nbsp;</td>";
		}
		
		// 2 -- Output the time that the resource was scanned
		output += "<td class=\"Background1\">" + scanResult.getScanTime() + "&nbsp;&nbsp;</td>";
		
		// 3 -- Output the edit option button
		output += "<td class=\"Background1\"><table><tr><td><a href=\"ScanRule?Action=Edit&RuleID=" + scanResult.getScanResultID() + 
		"\"><img class=\"imagebutton\" alt=\"configure\" src=\"/16_magnifier\"></a></td><td><a href=\"ScanResult?ResultID=" + scanResult.getScanResultID() +	 
		"\">View Details</a></td></tr></table></td></tr>";
		
		
		return output;
	}
	
}
