package net.lukeMurphey.nsia.htmlInterface;

import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.InsufficientPermissionException;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.ScannerController;
import net.lukeMurphey.nsia.trustBoundary.ApiScannerController;
import net.lukeMurphey.nsia.trustBoundary.ApiSystem;

public class HtmlUpdate extends HtmlContentProvider {
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException{
		return getHtml( requestDescriptor, null );
	}
	
	public static ContentDescriptor getHtml( WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException{
		StringBuffer body = new StringBuffer();
		String title = "NSIA Update";
		
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		// 1 -- Print out a dialog about the version
		body.append("<p/><div style=\"width: 600px\">");
		ApiSystem system = new ApiSystem(Application.getApplication());
		String currentVersion = system.getNewestVersionAvailableID(false);
		
		if( currentVersion == null || currentVersion.length() == 0 ){
			body.append( Html.getWarningDialog("Version Check Failed", "The application could not determine if this version of the application is the most current. This may be because an Internet connection to ThreatFactor.com is unavailable. <p/>To get the newest version of NSIA, please go to <a href=\"http://ThreatFactor.com/Support\">ThreatFactor.com</a>.<p/><form method=\"POST\" action=\"/\"><input type=\"Submit\" class=\"button\" value=\"OK\" name=\"OK\"></form>", null, null) );
		}
		else if( system.isNewerVersionAvailableID(false) == false ){
			body.append( Html.getInformationDialog("NSIA Up To Date", "This application is the most current version (" + StringEscapeUtils.escapeHtml( Application.getVersion() ) + ").<p/>Go to <a href=\"http://ThreatFactor.com/Support\">ThreatFactor.com</a> for more information.<p/><form method=\"POST\" action=\"/\"><input type=\"Submit\" class=\"button\" value=\"OK\" name=\"OK\"></form>", null, null) );
		}
		else {
			body.append( Html.getInformationDialog("Update Available", "A new version (" + StringEscapeUtils.escapeHtml( currentVersion ) + ") of the application is available. You are currently using version " + StringEscapeUtils.escapeHtml( Application.getVersion() ) + ".<p/>To update, please go to <a href=\"http://ThreatFactor.com/Support\">ThreatFactor.com</a> now.<p/><form method=\"POST\" action=\"/\"><input type=\"Submit\" class=\"button\" value=\"OK\" name=\"OK\"></form>", null, null) );
		}
		
		
		// 2 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "System Configuration", "/SystemConfiguration");
		navPath.addPathEntry( "System Update", "/Update");
		String navigationHtml = Html.getNavigationPath( navPath );
		
		// 3 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
		
		menuItems.add( new MenuItem("System Administration", null, MenuItem.LEVEL_ONE) );		
		menuItems.add( new MenuItem("Modify Configuration", "/SystemConfiguration", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Event Log", "/EventLog", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Shutdown System", "/SystemStatus?Action=Shutdown", MenuItem.LEVEL_TWO) );
		if( Application.getApplication().isUsingInternalDatabase() ){
			menuItems.add( new MenuItem("Create Backup", "/DatabaseBackup", MenuItem.LEVEL_TWO) );
			menuItems.add( new MenuItem("Defragment Indexes", "/SystemStatus?Action=StartDefragmenter", MenuItem.LEVEL_TWO) );
		}
		
		menuItems.add( new MenuItem("Scanning Engine", null, MenuItem.LEVEL_ONE) );
		ApiScannerController scannerController = new ApiScannerController(Application.getApplication());
		
		try{
			if(scannerController.getScanningState(requestDescriptor.sessionIdentifier) == ScannerController.ScannerState.RUNNING)
				menuItems.add( new MenuItem("Stop Scanner", "/SystemStatus&Action=StopScanner", MenuItem.LEVEL_TWO) );
			else
				menuItems.add( new MenuItem("Start Scanner", "/SystemStatus&Action=StartScanner", MenuItem.LEVEL_TWO) );
		}
		catch(InsufficientPermissionException e){
			//Ignore this, it just means we can show the option to start and restart the scanner
		}
		menuItems.add( new MenuItem("View Definitions", "/Definitions", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Default Scan Policy", "/ScanPolicy", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Site Scan", "/SiteScan", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Site Groups", "/", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add Site Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );
		
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
		
		return new ContentDescriptor( title, pageOutput );
	}

}
