package net.lukemurphey.nsia.htmlInterface;

import org.apache.commons.lang.StringEscapeUtils;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.ScannerController;
import net.lukemurphey.nsia.SiteGroupManagement;
import net.lukemurphey.nsia.SiteGroupScanResult;
import net.lukemurphey.nsia.Application.ApplicationStatusDescriptor;
import net.lukemurphey.nsia.scan.ScanResultCode;
import net.lukemurphey.nsia.trustBoundary.ApiScanData;
import net.lukemurphey.nsia.trustBoundary.ApiScannerController;

import java.util.*;

public class HtmlMainDashboard extends HtmlContentProvider{
	
	private static int STAT_GREEN = 0;
	private static int STAT_YELLOW = 1;
	private static int STAT_RED = 2;
	
	private HtmlMainDashboard(){
		//Class is non-instantiable
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException{
		return getHtml( requestDescriptor, null );
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException{
		
		StringBuffer output = new StringBuffer();
		String title = "Main Dashboard";
		
		// 1 -- Output the main content
		ApiScanData scanData = new ApiScanData(Application.getApplication());
		SiteGroupScanResult[] siteGroupStatus;
		
		siteGroupStatus = scanData.getSiteGroupStatus( requestDescriptor.sessionIdentifier );
		
		output.append(Html.renderMessages(requestDescriptor.userId));
		
		//	 1.1 -- Perform the scanner operations (if so asked)
		String action = requestDescriptor.request.getParameter("Action");
		if( action != null && action.matches("StartScanner") ){
			ApiScannerController apiScannerController = new ApiScannerController( Application.getApplication() );
			try{
				apiScannerController.enableScanning( requestDescriptor.sessionIdentifier );
				requestDescriptor.appStatusDesc = Application.getApplication().getManagerStatus();
				output.append( Html.getInfoNote("The scanner was successfully given the start command") );
			}catch( InsufficientPermissionException e ){
				output.append( Html.getWarningNote("You do not have permission to control the scanner") );
			}
		}
		else if( action != null && action.matches("StopScanner") ){
			ApiScannerController apiScannerController = new ApiScannerController( Application.getApplication() );
			try{
				apiScannerController.disableScanning( requestDescriptor.sessionIdentifier );
				requestDescriptor.appStatusDesc = Application.getApplication().getManagerStatus();
				output.append( Html.getInfoNote("The scanner was successfully given the stop command") );
			}catch( InsufficientPermissionException e ){
				output.append(  Html.getWarningNote("You do not have permission to control the scanner") );
			}
		}

		//	 1.2 -- Output the manager status
		ApplicationStatusDescriptor managerStatusDesc = requestDescriptor.appStatusDesc;

		output.append( "<span class=\"Text_2\">System Status</span>" );
		if( managerStatusDesc.getOverallStatus() == Application.ApplicationStatusDescriptor.STATUS_GREEN )
			output.append( createRow( STAT_GREEN, "Manager Status", "SystemStatus", managerStatusDesc.getLongDescription(), true ) );
		else if( managerStatusDesc.getOverallStatus() == Application.ApplicationStatusDescriptor.STATUS_RED )
			output.append( createRow( STAT_RED, "Manager Status", "SystemStatus", managerStatusDesc.getLongDescription(), true ) );
		else
			output.append( createRow( STAT_YELLOW, "Manager Status", "SystemStatus", managerStatusDesc.getLongDescription(), true ) );

		output.append( "&nbsp;<br>" );

		//	 1.3 -- Output the site group rows
		// In case no site groups exist:
		if( siteGroupStatus.length == 0 ){
			output.append( Html.getDialog("No resources are being monitored yet. Create a site group and define a rule to begin monitoring.<p><a href=\"SiteGroup?Action=New\">[Create Site Group Now]</a>", "No Monitored Resources", "/32_Information", false) );

		}
		else{
			output.append( "<span class=\"Text_2\">Scan Results</span>" );

			for(int c = 0; c < siteGroupStatus.length; c++ ){

				int level;
				String rowTitle = StringEscapeUtils.escapeHtml( siteGroupStatus[c].getSiteGroupDescriptor().getGroupName() );
				String rowMessage;
				
				if(siteGroupStatus[c].getDeviatingRules() == 1){
					rowMessage = "1 rule has rejected.";
					level = STAT_RED;
				}
				else if(siteGroupStatus[c].getDeviatingRules() > 1){
					rowMessage = siteGroupStatus[c].getDeviatingRules() + " rules have rejected";
					level = STAT_RED;
				}
				else if( siteGroupStatus[c].getIncompleteRules() > 0){
					level = STAT_YELLOW;

					if( siteGroupStatus[c].getIncompleteRules() == 1 )
						rowMessage = "1 rule incompletely evaluated (connection failed)";
					else
						rowMessage = siteGroupStatus[c].getIncompleteRules() + " rules incompletely evaluated (connection failed)";

				}
				else if(siteGroupStatus[c].getResultCode() != null && (siteGroupStatus[c].getIncompleteRules() > 0 || siteGroupStatus[c].getResultCode().equals(ScanResultCode.SCAN_COMPLETED) == false )){
					level = STAT_YELLOW;
					if( siteGroupStatus[c].getIncompleteRules() == 1){
						rowMessage = "1 rule has failed to scan completely";
					}
					else{
						rowMessage =  siteGroupStatus[c].getIncompleteRules() + " rules have failed to scan completely";
					}
				}
				else{
					rowMessage = "0 rules have rejected";
					level = STAT_GREEN;
				}
				
				output.append( createRow( level, rowTitle, "SiteGroup?SiteGroupID=" + siteGroupStatus[c].getSiteGroupId(), rowMessage, siteGroupStatus[c].getSiteGroupDescriptor().getGroupState() == SiteGroupManagement.State.ACTIVE ) );
			}
			
			output.append("<table><tr><td width=\"4\"></td><td><img src=\"/16_Add\"></td><td><a href=\"SiteGroup?Action=New\">[Create another Site Group]</a></td></tr></table>");
		}

		// 2 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		String navigationHtml = Html.getNavigationPath( navPath );
		
		
		// 3 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
		
		menuItems.add( new MenuItem("System Administration", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("System Status", "/SystemStatus", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Modify Configuration", "/SystemConfiguration", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Event Log", "/EventLog", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Shutdown System", "/SystemStatus?Action=Shutdown", MenuItem.LEVEL_TWO) );
		if( Application.getApplication().isUsingInternalDatabase() == true ){
			menuItems.add( new MenuItem("Create Backup", "/DatabaseBackup", MenuItem.LEVEL_TWO) );
		}
	
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
		
		// 4 -- Compile the result
		String pageOutput = Html.getMainContent(output, menuOutput, navigationHtml);
		
		return new ContentDescriptor( title, pageOutput );
	}
	
	private static String createRow( int messageStatusLevel, String title, String link, String message, boolean enabled ){
		
		StringBuffer row = new StringBuffer();
		
		// 1 -- Determine the icon and status indicator color
		String htmlClass;
		String htmlIcon;
		
		if( messageStatusLevel  == STAT_GREEN ){
			if( enabled )
				htmlIcon = "/22_Check";
			else
				htmlIcon = "/22_CheckDisabled";
			htmlClass = "StatGreen";
		}
		else if( messageStatusLevel  == STAT_RED ){
			if( enabled )
				htmlIcon = "/22_Alert";
			else
				htmlIcon = "/22_AlertDisabled";
			htmlClass = "StatRed";
		}
		else{
			if( enabled )
				htmlIcon = "/22_Warning";
			else
				htmlIcon = "/22_WarningDisabled";
			htmlClass = "StatYellow";
		}
		
		if( !enabled )
			htmlClass = "StatGrayDark";
		
		
		
		row.append("<table cellspacing=\"0\" cellpadding=\"0\" width=\"95%\"><tr class=\"BorderRow\"><td align=\"center\" width=\"48\" height=\"48\" class=\"" );
		row.append( htmlClass + "\">" );
		row.append( "<img src=\"" ).append( htmlIcon).append( "\" alt=\"StatusIcon\">" );
		row.append( "</td><td class=\"StatGray\" style=\"border-left: 0px; border-right: 0px;\" width=\"16\">&nbsp;</td><td <td class=\"StatGray\" style=\"border-left: 0px; border-right: 0px;\">" );
		
		if( link != null ){
			row.append( "<a href=\"" + link + "\">" );
		}
		
		row.append( "<span class=\"Text_3\">" );
		row.append( title );
		row.append( "</span></a><br>");
		row.append( message );
		row.append( "</td>" );
		
		row.append( "<td width=\"16\" class=\"" + htmlClass + "\">&nbsp;</td>" );
		
		row.append( "</tr></table><br>" );
		
		
		
		/*
		row.append( "<div style=\"line-heigh t:8em;\">");
		row.append( "<img style=\"vertical-align:middle;\" src=\"" ).append( htmlIcon).append( "\" alt=\"StatusIcon\">" );
		row.append( "</div>" );
		
		row.append( "<div style=\"position: relative;\" width=\"95%\">");
		row.append( "<div class=\"" + htmlClass + "\" style=\"float: left; width: 48px; height: 48px; text-align: center;\">" );
		row.append( "<img style=\" margin-top: 12px; \" src=\"" ).append( htmlIcon).append( "\" alt=\"StatusIcon\"></div>" );
		
		row.append( "<div class=\"StatGray\" style=\"height: 48px;\"><div style=\"margin: 8px 8px 8px 8px;\" class=\"Text_3\">" + title + "</div></div>" );
		
		row.append( "<div class=\"" + htmlClass + "\" style=\"float: left; width: 16px; height: 48px;\"></div>" );
		row.append( "</div>");
		*/
		
		
		/*
		row.append("<table cellspacing=\"0\" cellpadding=\"0\" width=\"95%\"><tr class=\"BorderRow\"><td align=\"center\" rowspan=\"2\" width=\"48\" height=\"48\" class=\"" );
		row.append( htmlClass + "\">" );
		row.append( "<img src=\"" ).append( htmlIcon).append( "\" alt=\"StatusIcon\">" );
		row.append( "</td><td class=\"TopBottomBorder\" rowspan=\"2\" width=\"16\">&nbsp;</td><td class=\"UpperRightBorder\">" );
		
		if( link != null ){
			row.append( "<a href=\"" + link + "\">" );
		}
		
		row.append( "<span class=\"Text_3\">" );
		row.append( title );
		row.append( "</span></a></td><td rowspan=\"2\">&nbsp;</td><td rowspan=\"2\" class=\"" );
		row.append( htmlClass );
		row.append( "\">&nbsp;</td></tr><tr class=\"BorderRow\"><td class=\"LowerRightBorder\"><span class=\"LightText\">" );
		row.append( message );
		row.append( "</span></td></tr><tr><td>&nbsp;</td></tr></table>" );
		*/
		return row.toString();
	}
	
}
