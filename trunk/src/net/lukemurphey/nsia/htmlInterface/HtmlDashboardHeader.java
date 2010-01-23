package net.lukemurphey.nsia.htmlInterface;

import java.util.Date;

import javax.servlet.http.*;

import org.apache.commons.lang.StringEscapeUtils;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.SessionStatus;
import net.lukemurphey.nsia.SiteGroupManagement;
import net.lukemurphey.nsia.SiteGroupScanResult;
import net.lukemurphey.nsia.Application.ApplicationStatusDescriptor;
import net.lukemurphey.nsia.Application.WorkerThreadDescriptor;
import net.lukemurphey.nsia.LicenseManagement.LicenseDescriptor;
import net.lukemurphey.nsia.LicenseManagement.LicenseStatus;
import net.lukemurphey.nsia.trustBoundary.ApiApplicationConfiguration;
import net.lukemurphey.nsia.trustBoundary.ApiScanData;
import net.lukemurphey.nsia.trustBoundary.ApiScannerController;
import net.lukemurphey.nsia.trustBoundary.ApiSystem;


public class HtmlDashboardHeader extends HtmlContentProvider{
	
	private static final int STATUS_LEVEL_GREEN = 0;
	private static final int STATUS_LEVEL_YELLOW = 1;
	private static final int STATUS_LEVEL_RED = 2;
	
	private static final String SPLITTER = "<td class=\"PanelSplitter\">&nbsp;</td><td>&nbsp;</td>";
	
	private HtmlDashboardHeader(){
		//All methods are static, this class is not instantiable
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException{
		return getHtml( requestDescriptor, null );
	}
	
	public static ContentDescriptor getHtml( WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException{
		
		String title = "Dashboard";
		StringBuffer body = new StringBuffer();
		ApiSystem system = new ApiSystem( Application.getApplication() );
		int panelsDisplayed = 0;
		
		// 1 -- Create the output
		
		// 	 1.1 -- Output the banner
		if( requestDescriptor.sessionStatus != SessionStatus.SESSION_ACTIVE ){
			
			String bannerString = null;
			
			try {
				bannerString = system.getLoginBanner();
				if( bannerString != null && bannerString.length() > 100 ){
					bannerString = Html.shortenString(bannerString, 100) + "<a href=\"/Login?ShowBanner\">[More]</a>";
				}
				
			} catch (GeneralizedException e) {
				bannerString = null;
			}
			
			if( bannerString == null )
				bannerString = "Access is for authorized users only. Use of this system subject to monitoring.";
			
			body.append( "<table><tr><td><img src=\"/16_Information\"></td><td>").append( bannerString).append("</td></tr></table>" );
			panelsDisplayed++;
		}
		
		//	 1.2 -- Output the normal header
		else{
			// 1.2.1 -- Determine the status
			ApplicationStatusDescriptor statusDesc = requestDescriptor.appStatusDesc;
			
			int managerStatus = STATUS_LEVEL_GREEN;
			int scannerStatus = STATUS_LEVEL_GREEN;
			
			// Get the manager status
			if( statusDesc.getOverallStatus() == Application.ApplicationStatusDescriptor.STATUS_GREEN ){
				managerStatus = STATUS_LEVEL_GREEN; 
			}
			else if( statusDesc.getOverallStatus() == Application.ApplicationStatusDescriptor.STATUS_RED ){
				managerStatus = STATUS_LEVEL_RED;
			}else
				managerStatus = STATUS_LEVEL_YELLOW;
			
			// Get the scan status
			ApiScanData scanData = new ApiScanData( Application.getApplication() );
			SiteGroupScanResult[] siteGroupStatus;
			
			siteGroupStatus = scanData.getSiteGroupStatus( requestDescriptor.sessionIdentifier );
			
			int deviantSiteGroups = 0;
			int incompleteEvaluationSiteGroups = 0;
			
			for(int c = 0; c < siteGroupStatus.length; c++){
				if( siteGroupStatus[c].getDeviations() > 0 && siteGroupStatus[c].getSiteGroupDescriptor().getGroupState() == SiteGroupManagement.State.ACTIVE ){
					if( siteGroupStatus[c].getDeviations() == siteGroupStatus[c].getIncompleteRules())
						incompleteEvaluationSiteGroups++;
					else
						deviantSiteGroups++;
				}
			}
			
			String managerStateDescription = "Manager Status: " + statusDesc.getShortDescription();
			String scannerStateDescription;
			
			if( deviantSiteGroups == 0 && incompleteEvaluationSiteGroups == 0 ){
				scannerStatus = STATUS_LEVEL_GREEN;
				scannerStateDescription = "Current Status: Compliant";
			}
			else if (deviantSiteGroups == 1){
				scannerStatus = STATUS_LEVEL_RED;
				scannerStateDescription = "Current Status: 1 Non-Compliant Site";
			}
			else if (deviantSiteGroups > 1){
				scannerStatus = STATUS_LEVEL_RED;
				scannerStateDescription = "Current Status: " + deviantSiteGroups + " Non-Compliant Sites";
			}
			else{//if (incompleteEvaluationSiteGroups > 0){//== deviantSiteGroups){
				scannerStatus = STATUS_LEVEL_YELLOW;
				if( incompleteEvaluationSiteGroups > 1)
					scannerStateDescription = "Current Status: " + deviantSiteGroups + " Warnings";
				else
					scannerStateDescription = "Current Status: 1 Warning";
			}

			
			body.append("<table><tr>");
			
			// 1.2.2 -- Get the status indicator
			String statusEntry = "<td><table class=\"PanelEntry\">";
			
			if( scannerStatus == STATUS_LEVEL_GREEN ){
				statusEntry += "<tr><td><img src=\"/16_LEDgreen\" alt=\"Green\"></td><td nowrap>" + scannerStateDescription + "</td></tr>";
			}
			else if( scannerStatus == STATUS_LEVEL_RED ){
				statusEntry += "<tr><td><img src=\"/16_LEDred\" alt=\"Green\"></td><td nowrap>" + scannerStateDescription + "</td></tr>";
			}
			else{
				statusEntry += "<tr><td><img src=\"/16_LEDyellow\" alt=\"Green\"></td><td nowrap>" + scannerStateDescription + "</td></tr>";
			}
			
			if( managerStatus == STATUS_LEVEL_GREEN ){
				statusEntry += "<tr><td><img src=\"/16_LEDgreen\" alt=\"Green\"></td><td nowrap>" + managerStateDescription + "</td></tr>";
			}
			else if( managerStatus == STATUS_LEVEL_RED ){
				statusEntry += "<tr><td><img src=\"/16_LEDred\" alt=\"Green\"></td><td nowrap>" + managerStateDescription + "</td></tr>";
			}
			else{
				statusEntry += "<tr><td><img src=\"/16_LEDyellow\" alt=\"Green\"></td><td nowrap>" + managerStateDescription + "</td></tr>";
			}
			
			statusEntry += "</table></td>";
			body.append( statusEntry );
			panelsDisplayed++;
			
			String entry;
			
			// 1.3 -- Get the notifications
			/*entry = getNotifications(  requestDescriptor );
			if( entry != null ){
				body.append(SPLITTER);
				body.append(entry);
				panelsDisplayed++;
			}*/
			
			// 1.4 -- Output the refresh entry
			entry = getRefreshEntry( requestDescriptor  );
			if( entry != null ){
				body.append(SPLITTER);
				body.append(entry);
				panelsDisplayed++;
			}
			
			// 1.5 -- Output the license warning
			ApiApplicationConfiguration config = new ApiApplicationConfiguration(requestDescriptor.application);
			
			LicenseDescriptor license = config.getLicense(requestDescriptor.sessionIdentifier, true);
			
			if( config.licenseKeyCheckCompleted(requestDescriptor.sessionIdentifier) && (license == null || license.isValid() == false )){
				entry = getLicenseWarning(requestDescriptor, license);
				if( entry != null ){
					body.append(SPLITTER);
					body.append(entry);
					panelsDisplayed++;
				}
			}
			
			// 1.6 -- Output the background task entry
			entry = getTasksEntry( requestDescriptor  );
			if( entry != null ){
				body.append(SPLITTER);
				body.append(entry);
				panelsDisplayed++;
			}
			
			// 1.7 -- Get the version checker
			if( system.isNewerVersionAvailableID(true) ){
				String version = system.getNewestVersionAvailableID(true);
				entry = getVersionWarning(requestDescriptor, version);
				if( entry != null ){
					body.append(SPLITTER);
					body.append(entry);
					panelsDisplayed++;
				}
			}
			
			// 1.8 -- Get the definition errors warning
			entry = getDefinitionErrors(requestDescriptor);
			if( entry != null ){
				body.append(SPLITTER);
				body.append(entry);
				panelsDisplayed++;
			}
			
			
			// A single banner looks somewhat strange without a splitter so add one if only one panel was displayed.
			if( panelsDisplayed == 1 ){
				body.append(SPLITTER);
			}
			
			// 2 -- Close the table
			body.append("</td>");
			body.append("</tr></table>");
			
		}
		
		ContentDescriptor contentDesc = new ContentDescriptor( title, body );
		return contentDesc;
	}
	
	/**
	 * Get the dashboard entry for the automatic refresh functionality.
	 */
	private static String getRefreshEntry(  WebConsoleConnectionDescriptor requestDescriptor  ){
		
		// 1 -- Determine if a refresh header entry is needed
		String mode = requestDescriptor.request.getServletPath().substring(1);
		String action = requestDescriptor.request.getParameter("Action");
		
		if( mode == null ){
			//Continue, default page is being shown
		}
		else if ( mode.matches("SystemStatus") ){
			// Continue, system status page is being shown
		}
		else if ( mode.matches("Sessions") ){
			// Continue, session status page is being shown
		}
		else if ( mode.matches("Dashboard") ){
			// Continue, session status page is being shown
		}
		else if( mode.matches("SiteGroup") && requestDescriptor.request.getParameter("SiteGroupID") != null && (action == null || action.matches("Delete") ) ){
			//Continue, site group viewer can be refreshed
		}
		else if ( mode.matches("ScanResult") && requestDescriptor.request.getParameter("RuleID") != null ){
			// Continue, scan results history page is being shown
		}
		else
			return null;
		
		StringBuffer body = new StringBuffer();
		body.append("<td><script language=\"JavaScript\" type=\"text/javascript\">");//<script type=\"text/javascript\" language=\"javascript\" src=\"/RefreshScript.js\"></script>
		body.append("if (document.all || document.getElementById)\n");
		body.append("startit();\n");
		body.append("else\n");
		body.append("window.onload = startit;\n");
		body.append("</script><table><tr><td><span class=\"Text_3\">Automatic Update</span></td>");
		
		body.append("<td rowspan=\"200\"><form method=\"get\" action=\"" + mode + "\" name=\"reloadForm\">");
		
		if( requestDescriptor.request.getParameter("SiteGroupID") != null )
			body.append("<input type=\"hidden\" name=\"SiteGroupID\" value=\"" + requestDescriptor.request.getParameter("SiteGroupID") + "\">");
		if( requestDescriptor.request.getParameter("RuleID") != null )
			body.append("<input type=\"hidden\" name=\"RuleID\" value=\"" + requestDescriptor.request.getParameter("RuleID") + "\">");
		
		body.append("<select name=\"refreshRate\" onchange='onRateChange(this)'>");
		
		String refreshRate = requestDescriptor.request.getParameter("refreshRate");
		Cookie[] cookies = requestDescriptor.request.getCookies();
		
		if( refreshRate != null ){
			requestDescriptor.response.addCookie(new Cookie("RefreshRate", refreshRate));
		}
		else{
			for( int c = 0; c < cookies.length; c++){
				if( cookies[c].getName().matches("RefreshRate") )
					refreshRate = cookies[c].getValue();
			}
		}
		
		// Set the refresh rate per the setting
		if( refreshRate != null ){
			if( refreshRate.matches("Disable"))
				body.append("<script language=\"JavaScript\">setRefreshRate('Disable'); resetCountDown();</script>");//body += "<script language=\"JavaScript\">setRefreshRate(" + refreshRate + ");</script>";
			else
				body.append("<script language=\"JavaScript\">setRefreshRate(" + refreshRate + "); resetCountDown();</script>");//body += "<script language=\"JavaScript\">setRefreshRate(" + refreshRate + ");</script>";
		}
		
		// Output the options and select the relevant one
		if( refreshRate != null && refreshRate.matches("15"))
			body.append("<option value=\"15\" selected>15 seconds</option>");
		else
			body.append("<option value=\"15\">15 seconds</option>");
		
		if( refreshRate != null && refreshRate.matches("30"))
			body.append("<option value=\"30\" selected>30 seconds</option>");
		else
			body.append("<option value=\"30\">30 seconds</option>");
		
		if( refreshRate == null || (refreshRate != null && refreshRate.matches("60") ))
			body.append("<option value=\"60\" selected>1 minute</option>");
		else
			body.append("<option value=\"60\">1 minute</option>");
		
		if( refreshRate != null && refreshRate.matches("120"))
			body.append("<option value=\"120\" selected>2 minutes</option>");
		else
			body.append("<option value=\"120\">2 minutes</option>");
		
		if( refreshRate != null && refreshRate.matches("300"))
			body.append("<option value=\"300\" selected>5 minutes</option>");
		else
			body.append("<option value=\"300\">5 minutes</option>");
		
		if( refreshRate != null && refreshRate.matches("600"))
			body.append("<option value=\"600\" selected>10 minutes</option>");
		else
			body.append("<option value=\"600\">10 minutes</option>");
		
		if( refreshRate != null && refreshRate.matches("900"))
			body.append("<option value=\"900\" selected>15 minutes</option>");
		else
			body.append("<option value=\"900\">15 minutes</option>");
		
		/*if( refreshRate != null && refreshRate.matches("Disable"))
			body.append("<option value=\"Disable\" selected>Disable</option>";
		else
			body.append("<option value=\"Disable\">Disable</option>";*/
		
		body.append( "</select></form></td>");
		
		body.append( "<tr><td width=\"200\"><span style=\"cursor: hand;\" onclick=\"onPlayPauseClick()\">");
		body.append( "<img style=\"display: inline\" id=\"refresh_pause\" src=\"/16_Pause\"><img style=\"display: none\" id=\"refresh_play\" src=\"/16_Play\"></span>");
		body.append( "&nbsp;Refresh <span id=\"countDownText\">---</span>");
		//body.append( "<form name=\"reloadForm\"></form>";
		body.append( "</td></tr></table>");
		
		return body.toString();
		
	}
	
	/*private static String getNotifications(WebConsoleConnectionDescriptor requestDescriptor){
		
		// 1 -- Output the result
		String body = "<td><table><tr><td colspan=\"2\"><span class=\"Text_3\">Notifications</span></td></tr>";
		
		body += "<tr><td><img src=\"/16_Information\"></td><td>No unacknowledged notifications <a href=\"Notifications\">[View]</a></td></tr>";
		//body += "<tr><td><img src=\"/16_Warning\"></td><td>2 unacknowledged notifications <a href=\"Notifications\">[View]</a></td></tr>";
		
		// 2 -- Complete the entry
		body += "</td></tr></table>";
		
		return body;
	}*/
	
	private static String getVersionWarning(WebConsoleConnectionDescriptor requestDescriptor, String newVersion){
		
		// 1 -- Output the result
		String body = "<td><table><tr><td colspan=\"2\"><span class=\"Text_3\">Updated Version Available</span></td></tr>";
		
		body += "<tr><td><img src=\"/16_Information\"></td><td>Version " + newVersion + " is available <a href=\"/Update\">[More]</a></td></tr>";
		//body += "<tr><td><img src=\"/16_Warning\"></td><td>2 unacknowledged notifications <a href=\"Notifications\">[View]</a></td></tr>";
		
		// 2 -- Complete the entry
		body += "</td></tr></table>";
		
		return body;
	}
	
	private static String getLicenseWarning(WebConsoleConnectionDescriptor requestDescriptor, LicenseDescriptor license){
		
		// 1 -- Output the result
		StringBuffer body = new StringBuffer();
		
		if( license == null || license.getStatus() == null || license.getStatus() == LicenseStatus.UNVALIDATED ){
			body.append("<td><table><tr><td colspan=\"2\"><span class=\"RedText\">License not validated</span></td></tr>");
		}
		else{
			body.append("<td><table><tr><td colspan=\"2\"><span class=\"RedText\">License invalid</span></td></tr>");
		}
		
		if( license == null || license.getStatus() == null || license.getStatus() == LicenseStatus.UNVALIDATED ){
			body.append("<tr><td><img src=\"/16_Warning\"></td><td>License could not be validated <a href=\"/License\">[Details]</a></td></tr>");
		}
		else if( license.getExpirationDate() != null && license.getExpirationDate().before(new Date()) ){
			body.append("<tr><td><img src=\"/16_Warning\"></td><td>The application license has expired <a href=\"/License\">[Fix Now]</a></td></tr>");
		}
		else if( license.getStatus() == LicenseStatus.UNLICENSED ) {
			body.append("<tr><td><img src=\"/16_Warning\"></td><td>The application is unlicensed <a href=\"/License\">[Fix Now]</a></td></tr>");
		}
		else {
			body.append("<tr><td><img src=\"/16_Warning\"></td><td>The application license is " + license.getStatus().getDescription() + " <a href=\"/License\">[Fix Now]</a></td></tr>");
		}
		
		//body += "<tr><td><img src=\"/16_Warning\"></td><td>2 unacknowledged notifications <a href=\"Notifications\">[View]</a></td></tr>";
		
		// 2 -- Complete the entry
		body.append("</td></tr></table>");
		
		return body.toString();
	}
	
	private static String getTasksEntry(WebConsoleConnectionDescriptor requestDescriptor){
		
		// 1 -- Get the list of worker threads
		Application app = Application.getApplication();
		
		WorkerThreadDescriptor[] threads = app.getWorkerThreadQueue(true);
		String body;
			
		//	 1.1 -- Don't show the entry if no worker threads exist
		if( threads.length == 0 )
			return null;
		
		//	 1.2 -- Show details on the one worker thread if only one exists
		if( threads.length == 1 ){
			/*if( threads.get(0).reportsProgress() ){
				body = "<td><span class=\"Text_3\">" + threads.get(0).getTaskDescription() + "</span><div style=\"position:relative; width:198px; height:12px; margin-top: 11px; padding:2px; background-image:url(/SmallProgressBarBlank);\">";
				body += "<div style=\"position:relative; left:1px; width:" + (194 * threads.get(0).getProgress() ) / 100  + "px; height:8px; padding:2px; background-image:url(/SmallProgressBar2); layer-background-image:url(SmallProgressBar2);\"></div></div></td></tr>";
			}
			else*/{
				body = "<td><table><tr><td><span class=\"Text_3\">" + StringEscapeUtils.escapeHtml( threads[0].getWorkerThread().getTaskDescription() ) + "</span></td></tr>";
				body += "<tr><td>" + StringEscapeUtils.escapeHtml( Html.shortenString( threads[0].getWorkerThread().getStatusDescription(), 32 ) ) + " <a href=\"/Tasks\">[View]</a></td></tr>";
				body += "</td></tr></table>";
			}
		}
		else{
			body = "<td><table><tr><td><span class=\"Text_3\">" + threads.length + " Tasks Running</span></td></tr>";
			body += "<tr><td>Multiple background tasks are running <a href=\"/Tasks\">[View]</a></td></tr>";
			body += "</td></tr></table>";
		}
		
		// 2 -- Complete the entry
		return body;
	}

	private static String getDefinitionErrors(WebConsoleConnectionDescriptor requestDescriptor){
		
		// 1 -- Get the list of worker threads
		Application app = Application.getApplication();
		
		ApiScannerController controller = new ApiScannerController(app);
		String body = null;
		
		try{
			boolean errorsNoted = controller.definitionsErrorsNoted(requestDescriptor.sessionIdentifier);
			
			if( errorsNoted == true ){
				body = "<td><table><tr><td colspan=\"2\"><span class=\"RedText\">Definition Errors</span></td></tr>";
				body += "<tr><td>At least one definition has an error <a href=\"/DefinitionErrors\">[View]</a></td></tr>";
				body += "</td></tr></table>";
			}
		}
		catch (NoSessionException e) {
			//This has been logged already in the trust boundary.
		} catch (GeneralizedException e) {
			//This has been logged already in the trust boundary.
		}
		
		// 2 -- Complete the entry
		return body;
	}
	
}
