package net.lukeMurphey.nsia.htmlInterface;

import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.InputValidationException;
import net.lukeMurphey.nsia.InsufficientPermissionException;
import net.lukeMurphey.nsia.LicenseManagement;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.NotFoundException;
import net.lukeMurphey.nsia.ScannerController;
import net.lukeMurphey.nsia.LicenseManagement.LicenseDescriptor;
import net.lukeMurphey.nsia.htmlInterface.Html.MessageType;
import net.lukeMurphey.nsia.trustBoundary.ApiApplicationConfiguration;
import net.lukeMurphey.nsia.trustBoundary.ApiScannerController;

public class HtmlLicense extends HtmlContentProvider {

	private HtmlLicense(){
		//All methods are static, this class is not instantiable
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, NotFoundException{
		return getHtml( requestDescriptor, null );
	}
	
	public static ContentDescriptor getHtml( WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, NotFoundException{
		StringBuffer body = new StringBuffer();
		String title = "License";
		
		if( actionDesc == null ){
			performAction( requestDescriptor );
		}
		
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		// 1 -- Print out a dialog about the license
		ApiApplicationConfiguration config = new ApiApplicationConfiguration(requestDescriptor.application);
		LicenseDescriptor license = null;
		
		license = config.getLicense(requestDescriptor.sessionIdentifier);
		
		body.append("<p/><div style=\"width: 600px\">");
		
		String licenseKey;
		
		if( requestDescriptor.request.getParameter("LicenseKey") != null ){
			licenseKey = requestDescriptor.request.getParameter("LicenseKey");
		}
		else if(license == null){
			licenseKey = "";
		}
		else if( license.getKey() != null ){
			licenseKey = license.getKey();
		}
		else{
			licenseKey = "";
		}
		
		String keyForm = "<p/>Once you have a license key, enter it below:<form method=\"POST\" action=\"License\"><input class=\"textInput\" size=\"48\" type=\"text\" name=\"LicenseKey\" value=\"" + StringEscapeUtils.escapeHtml( licenseKey ) + "\"><input type=\"Submit\" class=\"button\" value=\"Apply\" name=\"Apply\"></form>";
		
		if( license == null ){
			body.append( Html.getWarningDialog("No License", "The application does not possess a valid license. Thus, the application cannot receive definition updates. Purchase a license at <a href=\"http://ThreatFactor.com/\">ThreatFactor.com now</a>." + keyForm) );
		}
		else if( config.licenseKeyCheckCompleted(requestDescriptor.sessionIdentifier) == false ){
			body.append( Html.getInformationDialog("License Being Verified", "The application license is currently being verified by the application.<p/>If you need support, please go to <a href=\"http://ThreatFactor.com/Support\">ThreatFactor.com</a> now.<p/><form method=\"POST\" action=\"/\"><input type=\"Submit\" class=\"button\" value=\"OK\" name=\"OK\"></form>", null, null) );
		}
		else if( license.getStatus() == LicenseManagement.LicenseStatus.EXPIRED ){
			body.append( Html.getWarningDialog("Expired License", "The license has expired. Thus, the application cannot receive definition updates. Purchase an updated license at <a href=\"http://ThreatFactor.com/\">ThreatFactor.com now</a>.") );
		}
		else if( license.getStatus() == LicenseManagement.LicenseStatus.ACTIVE ){
			body.append( Html.getInformationDialog("License Up to Date", "The application license has been granted to " + license.getLicensee() + " and is valid until " + license.getExpirationDate().toString() + ".<p/>If you need support, please go to <a href=\"http://ThreatFactor.com/Support/\">ThreatFactor.com</a> now.<p/><form method=\"POST\" action=\"/\"><input type=\"Submit\" class=\"button\" value=\"OK\" name=\"OK\"></form>", null, null) );
			//body.append( HtmlOptionDialog.getHtml(requestDescriptor, "License Up to Date", "The application license is valid until " + license.getExpirationDate().toString() + ".", new Hashtable<String, String>(), new String[0], "", HtmlOptionDialog.DIALOG_INFORMATION ).getBody() ); 
		}
		else if( license.getStatus() == LicenseManagement.LicenseStatus.ILLEGAL ){
			body.append( Html.getWarningDialog("Illegal License", "The license has expired. Thus, the application cannot receive definition updates. Purchase an updated license at <a href=\"http://ThreatFactor.com/\">ThreatFactor.com now</a>." + keyForm) );
		}
		else if( license.getKey() != null && license.getKey().length() > 0 && license.getStatus() == LicenseManagement.LicenseStatus.UNLICENSED ){
			body.append( Html.getWarningDialog("License Invalid", "The provided license key is invalid. Go to the <a href=\"http://ThreatFactor.com/Support/\">ThreatFactor support website</a> and check to make sure you entered it correctly if you know you have a valid license. If you need to purchase a license go to <a href=\"http://ThreatFactor.com/\">ThreatFactor.com now</a>." + keyForm) );
		}
		else if( license.getStatus() == null || license.getStatus() == LicenseManagement.LicenseStatus.UNVALIDATED ){
			//String keyEdit = "<p/>Once you have a license key, enter it below:<form method=\"POST\" action=\"License\"><input class=\"textInput\" size=\"48\" type=\"text\" name=\"LicenseKey\" value=\"" + StringEscapeUtils.escapeHtml( licenseKey ) + "\"><input type=\"Submit\" class=\"button\" value=\"Apply\" name=\"Apply\"></form>";
			body.append( Html.getWarningDialog("License Not Yet Validated", "The license could not be validated with ThreatFactor.com. This is likely due to a problem with the network connection. Please make sure NSIA can connect to ThreatFactor.com, otherwise, NSIA will not be able to download updated definitions. <p/>NSIA will attempt to re-validate the license periodically and will validate the license as soon as it can establish a connection to ThreatFactor.com.<p/>If you need additional assistance, go to <a href=\"http://ThreatFactor.com/\">ThreatFactor.com now</a>.") );
		}
		else{ //if( license.getStatus() == LicenseManagement.LicenseStatus.UNLICENSED ){
			body.append( Html.getWarningDialog("No License", "NSIA does not have a valid license. Thus, the application cannot receive definition updates. Purchase an updated license at <a href=\"http://ThreatFactor.com/\">ThreatFactor.com now</a>." + keyForm) );
		}
		
		
		
		
		/*body.append("<p/>ThreatFactor license is necessary in to receive:<ul>");
		body.append("<li>Definition updates: necessary for the system to detect newly released and late-breaking exploits</li>");
		body.append("<li>Support: installation assistance, custom definition support, online knowledgebase, etc</li>");
		body.append("<li>Latest Version: installation assistance, custom definition support, online knowledgebase, etc</li>");
		body.append("</ul>");
		
		body.append("<p/>To get an updated ThreatFactor license, go to <a href=\"ThreatFactor.com/nsia/Purchase/\">ThreatFactor.com</a> now.");
		
		body.append("</div>");*/
		
		// 2 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "System Configuration", "/SystemConfiguration");
		navPath.addPathEntry( "License Management", "/License");
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
	
	private static ActionDescriptor performAction(WebConsoleConnectionDescriptor requestDescriptor) throws InvalidHtmlParameterException, GeneralizedException, NoSessionException, NotFoundException{
		
		ApiApplicationConfiguration appConfig = new ApiApplicationConfiguration (Application.getApplication());
		
		try{
			if( requestDescriptor.request.getParameter("LicenseKey") != null ){
				String licenseKey = requestDescriptor.request.getParameter("LicenseKey");
				
				if( licenseKey != null ){
					licenseKey = licenseKey.trim();
				}
				
				appConfig.setLicenseKey(requestDescriptor.sessionIdentifier, licenseKey);
				Html.addMessage(MessageType.INFORMATIONAL, "License key successfully updated" , requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
			}
			else{
				return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
			}
		}
		catch( InputValidationException e ){
			Html.addMessage(MessageType.WARNING, e.getMessage() , requestDescriptor.userId.longValue());
			return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED );
		}
		catch( InsufficientPermissionException e ){
			Html.addMessage(MessageType.WARNING, "You do not have permission to set the license key" , requestDescriptor.userId.longValue());
			return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED );
		}
	}
}
