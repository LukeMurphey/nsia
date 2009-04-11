package net.lukeMurphey.nsia.htmlInterface;

import java.sql.SQLException;
import java.util.Vector;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.InsufficientPermissionException;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.NotFoundException;
import net.lukeMurphey.nsia.scanRules.HttpStaticScanResult;
import net.lukeMurphey.nsia.trustBoundary.ApiScanData;
import net.lukeMurphey.nsia.xmlRpcInterface.XmlrpcUserManagement;

public class HtmlStaticScanResult extends HtmlContentProvider {

	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException{
		return getHtml( requestDescriptor, null );
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException{
		
		// 2 -- Perform any pending actions
		if( actionDesc == null )
			actionDesc = performAction( requestDescriptor,  null);
		
		// 1 -- Output the main content
		ApiScanData xScanData = new ApiScanData(Application.getApplication());
		return getRuleView( requestDescriptor, xScanData, actionDesc);
	}
	
	public static StringBuffer getScanResultReport( HttpStaticScanResult scanResult, WebConsoleConnectionDescriptor descriptor ){
		StringBuffer body = new StringBuffer();
		
		body.append( "<table>" );
		
		if( scanResult.getDeviations() == 0 && scanResult.getIncompletes() == 0 ){
			body.append( "<tr><td style=\"vertical-align: top;\" rowspan=\"99\"><img style=\"margin-right: 5px;\" src=\"/32_Check\" alt=\"Pass\"></td>");
		}
		else if( scanResult.getDeviations() == 0 && scanResult.getIncompletes() > 0 ){
			body.append( "<tr><td style=\"vertical-align: top;\" rowspan=\"99\"><img style=\"margin-right: 5px;\" src=\"/32_Warning\" alt=\"Pass\"></td>");
		}
		else {
			body.append( "<tr><td style=\"vertical-align: top;\" rowspan=\"99\"><img style=\"margin-right: 5px;\" src=\"/32_Alert\" alt=\"Pass\"></td>");
		}
		
		body.append( "<td class=\"Text_2\">Data Hash</td><td>").append( scanResult.getActualHashValue()).append( "</td></tr>" );
		body.append( "<tr><td class=\"Text_2\">Response Code</td><td>").append( scanResult.getActualResponseCode()).append( "</td></tr>" );
		body.append( "<tr><td class=\"Text_2\">URL</td><td>").append( scanResult.getUrl()).append( "</td></tr>" );
		body.append( "<tr><td class=\"Text_2\">Scan Time</td><td>").append( scanResult.getScanTime()).append( "</td></tr>" );
		
		body.append( "</table>" );
		
		/*
		body.append( "<table><tr class=\"Background1\"><td class=\"Text_2\">Data Hash</td><td>").append( GenericHtmlGenerator.splitString( scanResult.getActualHashValue() , 32, "<br>" )).append( "</td></tr>" );
		body.append( "<tr class=\"Background1\"><td class=\"Text_2\">Response Code</td><td>").append( scanResult.getActualResponseCode()).append( "</td></tr>" );
		body.append( "<tr class=\"Background1\"><td class=\"Text_2\">URL</td><td>").append( scanResult.getUrl()).append( "</td></tr>" );
		body.append( "<tr class=\"Background1\"><td class=\"Text_2\">Scan Time</td><td>").append( scanResult.getScanTime()).append( "</td></tr>" );
		
		body.append( "</table>" );
		*/
		
		return body;
	}
	
	
	public static ContentDescriptor getScanResultPage( HttpStaticScanResult scanResult, WebConsoleConnectionDescriptor descriptor ) throws GeneralizedException, NoSessionException, InsufficientPermissionException, NotFoundException{
		String title = "Scan Result";
		StringBuffer body = new StringBuffer();
		
		// 1 -- Output the main content
		ApiScanData scanData = new ApiScanData(Application.getApplication());
		long siteGroupId = scanData.getAssociatedSiteGroup(descriptor.sessionIdentifier, scanResult.getRuleID());
		
		// Output the section header
		body.append( Html.getSectionHeader( "HTTP Rule", "Static Content" ) );
		
		body.append( "<table><tr class=\"Background1\"><td class=\"Text_2\">Data Hash</td><td>").append( Html.splitString( scanResult.getActualHashValue() , 32, "<br>" )).append( "</td></tr>" );
		body.append( "<tr class=\"Background1\"><td class=\"Text_2\">Response Code</td><td>").append( scanResult.getActualResponseCode()).append( "</td></tr>" );
		body.append( "<tr class=\"Background1\"><td class=\"Text_2\">URL</td><td>").append( scanResult.getUrl()).append( "</td></tr>" );
		body.append( "<tr class=\"Background1\"><td class=\"Text_2\">Scan Time</td><td>").append( scanResult.getScanTime()).append( "</td></tr>" );
		
		body.append( "</table>" );
		
		
		// 2 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "Site Group",  "/SiteGroup?SiteGroupID=" + siteGroupId );
		navPath.addPathEntry( "Http Rule", "/SiteGroup?RuleID=" + scanResult.getRuleID() );
		String navigationHtml = Html.getNavigationPath( navPath );
		
		
		// 3 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
		
		menuItems.add( new MenuItem("Site Group", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Edit", "/SiteGroup?SiteGroupID=" + siteGroupId, MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Delete", "/SiteGroup?Action=Delete&SiteGroupID=" + siteGroupId, MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Scan Now", "/SiteGroup?Action=Scan&SiteGroupID=" + siteGroupId, MenuItem.LEVEL_TWO) );
		//menuItems.add( new MenuItem("View All", "/", MenuItem.LEVEL_TWO) );
		
		String menuOutput = Html.getMenu( menuItems );
		
		// 4 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor(title, pageOutput);
	}
	
	/**
	 * Get a read-only view of the rule. 
	 * @param request
	 * @param response
	 * @param requestDescriptor
	 * @param httpMethod
	 * @param xScanData
	 * @return
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws SQLException
	 */
	private static ContentDescriptor getRuleView(WebConsoleConnectionDescriptor requestDescriptor, ApiScanData xScanData, ActionDescriptor actionDesc) throws GeneralizedException, NoSessionException{
		String title = "";
		StringBuffer body = new StringBuffer();
		
		// 1 -- Output the main content
		long siteGroupId = -1;//Long.parseLong(request.getParameter("SiteGroupId"));
		long ruleId = -1;
		
		try{
			ruleId = Long.parseLong(requestDescriptor.request.getParameter("RuleID"));
		}
		catch( NumberFormatException e ){
			ruleId = -2;
		}
		
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		// Output the section header
		body.append( Html.getSectionHeader( "HTTP Rule", "Cryptographic Data Hash" ) );
		
		HttpStaticScanResult scanResult = (HttpStaticScanResult)xScanData.getLastScanResult(requestDescriptor.sessionIdentifier, ruleId);
		
		if( scanResult == null ){
			body.append( Html.getDialog("A scan result with the given identifier was not found", "Scan Result Not Found", "32_Warning"));
		}
		else if ( ruleId <= -1 ){
			body.append( Html.getDialog("A valid scan result identifier was not provided", "Invalid Scan Result Identifier", "32_Warning"));
		}
		else{
			body.append( "<table><tr class=\"Background1\"><td class=\"Text_2\">Data Hash</td><td>").append( Html.splitString( scanResult.getActualHashValue() , 32, "<br>" )).append( "</td></tr>" );
			body.append( "<tr class=\"Background1\"><td class=\"Text_2\">Response Code</td><td>").append( scanResult.getActualResponseCode()).append( "</td></tr>" );
			body.append( "<tr class=\"Background1\"><td class=\"Text_2\">URL</td><td>").append( scanResult.getUrl()).append( "</td></tr>" );
			body.append( "<tr class=\"Background1\"><td class=\"Text_2\">Scan Time</td><td>").append( scanResult.getScanTime()).append( "</td></tr>" );
			
			//if ( actionDesc.result == ActionDescriptor.OP_VIEW ){
				//body.append( "" );
			//}
			
			body.append( "</table>" );
		}
		
		
		// 2 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "Site Group",  "/SiteGroup?SiteGroupID=" + siteGroupId );
		navPath.addPathEntry( "Http Rule", "/SiteGroup?RuleID=" + ruleId );
		String navigationHtml = Html.getNavigationPath( navPath );
		
		// 3 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
		
		menuItems.add( new MenuItem("Site Group", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Edit", "/SiteGroup?SiteGroupId=" + requestDescriptor.request.getParameter("SiteGroupId"), MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Delete", "/SiteGroup?Action=Delete&SiteGroupId=" + requestDescriptor.request.getParameter("SiteGroupId"), MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Scan Now", "/SiteGroup?Action=Scan&SiteGroupId=" + requestDescriptor.request.getParameter("SiteGroupId"), MenuItem.LEVEL_TWO) );
		//menuItems.add( new MenuItem("View All", "SiteGroup", MenuItem.LEVEL_TWO) );
		
		String menuOutput = Html.getMenu( menuItems );
		
		// 4 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor(title, pageOutput);
	}
	
	public static String createRow(WebConsoleConnectionDescriptor requestDescriptor) throws GeneralizedException, NoSessionException{
		return "";
	}
	
	private static ActionDescriptor performAction(WebConsoleConnectionDescriptor requestDescriptor, XmlrpcUserManagement xUserManager){
		return new ActionDescriptor( ActionDescriptor.OP_VIEW );
	}
	
	
}
