package net.lukemurphey.nsia.htmlInterface;

import java.util.Vector;

import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.NoSessionException;

public class HtmlSelectRule extends HtmlContentProvider {
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException{
		return getHtml( requestDescriptor, null );
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException{
		
		StringBuffer body = new StringBuffer();
		
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		// 1 -- Create the page content
		long siteGroupId = -1;
		
		try{
			siteGroupId = Long.valueOf( requestDescriptor.request.getParameter("SiteGroupID") );
		}
		catch(NumberFormatException e){
			return HtmlProblemDialog.getHtml(requestDescriptor, "Invalid Parameter", "The Site Group identifier provided was invalid", HtmlProblemDialog.DIALOG_WARNING, "Console", "[Main Dashboard]");
		}
		
		body.append("<div style=\"padding:5px;\"><span class=\"Text_2\">Select Type of Rule To Add<span></div>");
		//body.append(HtmlSelectRule.createRow("Availability Analysis", "Determines if a server is functional and responding quickly using a ICMP echo requests (ping)", "Availability?Action=New&SiteGroupID=" + siteGroupId));
		body.append(HtmlSelectRule.createRow("Service Monitoring", "Analyses open ports on a server and alerts if new ports open or exiting ports close", "ServiceMonitoring?Action=New&SiteGroupID=" + siteGroupId));
		body.append(HtmlSelectRule.createRow("Static HTTP Content", "Analyses HTTP content that is not expected to change often and alerts if the content changes", "HttpStaticScanRule?Action=New&SiteGroupID=" + siteGroupId));
		//body.append(HtmlSelectRule.createRow("Dynamic HTTP Content", "Analyses dynamic HTTP content with a number of algorithms and alerts if the change in content appears to be a security issue (such as a defacement)", "DynamicHTTP?Action=New&SiteGroupID=" + siteGroupId));
		body.append(HtmlSelectRule.createRow("HTTP Content Auto-Discovery ", "Automatically discovers HTTP content and analyzes it for rogue content. This rule is less sensitive to issues than the <a href=\"DynamicHTTP?Action=New\">Dynamic HTTP</a> and <a href=\"StaticHTTP?Action=New\">Static HTTP</a> rules but does not require a list of resources to monitor.", "HttpDiscoveryRule?Action=New&SiteGroupID=" + siteGroupId));
		
		
		// 2 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "Site Group", "/SiteGroup?SiteGroupID=" + siteGroupId );
		navPath.addPathEntry( "Add Rule", "/SelectRuleType?SiteGroupID=" + siteGroupId );
		//navPath.addPathEntry( "Modify Group", "/GroupManagement?Action=Edit&GroupID=" + requestDescriptor.request.getParameter("GroupID") );
		String navigationHtml = Html.getNavigationPath( navPath );
		
		
		// 3 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
				
		menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Site Groups", "/", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add Site Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );
		//menuItems.add( new MenuItem("Add Site Group", "SiteGroup?Action=Edit&SiteGroupID=" + siteGroupId, MenuItem.LEVEL_TWO) );
		
				
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
		
		return new ContentDescriptor( "Create New Rule", pageOutput );
	}
	
	private static StringBuffer createRow( String name, String description, String link){
		StringBuffer buffer = new StringBuffer();
		buffer.append("<div style=\"padding:5px;\">");
		buffer.append("<div class=\"ToolIcon\" style=\"float:left;\"><a href=\"" + link + "\"><img src=\"/32_Add\"></a></div>");
		
		buffer.append("<div style=\"position:relative; left:8px;\"><a href=\"" + link + "\"><span class=\"Text_2\">").append(name).append("</span></a><br>").append(description).append("</div>");
		buffer.append("<br></div>");
		
		return buffer;
	}

}
