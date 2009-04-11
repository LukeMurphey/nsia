package net.lukeMurphey.nsia.htmlInterface;

import java.util.Vector;

import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.scanRules.NetworkPortRange;
import net.lukeMurphey.nsia.scanRules.ServiceScanResult;
import net.lukeMurphey.nsia.scanRules.NetworkPortRange.Protocol;
import net.lukeMurphey.nsia.scanRules.NetworkPortRange.SocketState;

public class HtmlServiceScanResult {

	public static ContentDescriptor getScanResultPage( ServiceScanResult scanResult, WebConsoleConnectionDescriptor descriptor, int siteGroupID ) throws GeneralizedException, NoSessionException{
		StringBuffer body = new StringBuffer();
		String title = "Scan Result";
		body.append( getScanResultReport(scanResult, descriptor, siteGroupID) );
		
		
		// 3 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		if( siteGroupID >= 0){
			navPath.addPathEntry( "Site Group", "/SiteGroup?SiteGroupID=" + siteGroupID );
		}
		navPath.addPathEntry( "Scan Result", "/ScanResult?ResultID=" + scanResult.getScanResultID() );
		String navigationHtml = Html.getNavigationPath( navPath );
		
		
		// 4 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
		
		menuItems.add( new MenuItem("Website Scan", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Begin a Site Scan", "SiteScan?Action=New", MenuItem.LEVEL_TWO) );
		
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
		
		
		// 5 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor( title, pageOutput );
	}
	
	public static StringBuffer getScanResultReport( ServiceScanResult scanResult, WebConsoleConnectionDescriptor descriptor, long siteGroupID) throws GeneralizedException, NoSessionException{

		// 1 -- Print out the messages
		StringBuffer body = new StringBuffer();
		
		body.append(Html.renderMessages(descriptor.userId));
		
		// 2 -- Print out the result table
		body.append( "<table cellpadding=\"2\">" );
		
		body.append( "<tr class=\"Background0\"><td colspan=\"2\" class=\"Text_3\">Scan Parameters</td></tr>" );
		body.append( createSummaryRow( "Deviations", Integer.toString( scanResult.getDeviations() ) ) );
		body.append( createSummaryRow( "Ports Scanned", Integer.toString( scanResult.getAccepts() + scanResult.getDeviations() + scanResult.getIncompletes() ) ) );//scanResult.getFindings().length
		body.append( createSummaryRow( "Server", scanResult.getSpecimenDescription() ) );
		
		body.append( "</table><p/>" );
		
		// 3 -- Print out list of violations
		body.append( "<table><tr class=\"Background0\"><td colspan=\"2\" class=\"Text_3\">Deviations</td></tr><tr class=\"Background1\">" );
		
		//	 3.1 -- Print out graph
		body.append( "<td width=\"125px\">" );
		body.append( "<img src=\"/ServiceScanDeviations?ResultID=" + scanResult.getScanResultID() + "&H=125\"/>");
		body.append( "</td>" );
		
		//	 3.2 -- Print out details
		body.append( "<td width=\"325px\">" );
		
		
		NetworkPortRange[] diff = scanResult.getDifferences();
		for(int c = 0; c < diff.length; c++){
			
			if(  diff[c].getState() == SocketState.OPEN ){
				body.append( "<div style=\"background-image: url('/GreenShine'); padding: 2px; border: 1px solid green; margin:3px; float:left\">" );
			}
			else{
				body.append( "<div style=\"background-image: url('/DarkGrayShine'); padding: 2px; border: 1px solid gray; margin:3px; float:left\">" );
			}
			body.append( diff[c].toString() + "<br/>[" + diff[c].getState().toString().toLowerCase() + "]" );
			body.append( "</div>" );
		}
		
		
		body.append( "</td>" );
		
		body.append( "</tr></table>" );
		
		// 4 -- Print out TCP results
		body.append( "<table><tr class=\"Background0\"><td colspan=\"2\" class=\"Text_3\">TCP Overview</td></tr><tr class=\"Background1\">" );
		
		//	 4.1 -- Print out graph
		body.append( "<td width=\"125px\">" );
		body.append( "<img src=\"/TCPSummary?ResultID=" + scanResult.getScanResultID() + "&H=125\"/>");
		body.append( "</td>" );
		
		//	 4.2 -- Print out details
		body.append( "<td width=\"325px\">" );
		NetworkPortRange[] scanned = scanResult.getPortsScanned();
		int count = 0;
		for(int c = 0; c < scanned.length; c++){

			if( scanned[c].getProtocol() == Protocol.TCP){
				
				count++;
				if(  scanned[c].getState() == SocketState.OPEN ){
					body.append( "<div style=\"background-image: url('/GreenShine'); padding: 2px; border: 1px solid green; margin:3px; float:left\">" );
				}
				else{
					body.append( "<div style=\"background-image: url('/DarkGrayShine'); padding: 2px; border: 1px solid gray; margin:3px; float:left\">" );
				}
				body.append( scanned[c].toString() + "<br/>[" + scanned[c].getState().toString().toLowerCase() + "]" );
				body.append( "</div>" );
			}
			
		}
		
		
		body.append( "</td>" );
		
		body.append( "</tr></table>" );
		
		// 5 -- Print out UDP results
		body.append( "<table><tr class=\"Background0\"><td colspan=\"2\" class=\"Text_3\">UDP Overview</td></tr><tr class=\"Background1\">" );
		
		//	 5.1 -- Print out graph
		body.append( "<td width=\"125px\">" );
		body.append( "<img src=\"/UDPSummary?ResultID=" + scanResult.getScanResultID() + "&H=125\"/>");
		body.append( "</td>" );
		
		//	 5.2 -- Print out details
		body.append( "<td width=\"325px\">" );
		count = 0;
		for(int c = 0; c < scanned.length; c++){
			
			if( scanned[c].getProtocol() == Protocol.UDP){
				count++;
				if(  scanned[c].getState() == SocketState.OPEN ){ //background-image: url('/GreenShine'); 
					body.append( "<div style=\"background-image: url('/GreenShine'); padding: 2px; border: 1px solid green; margin:3px; float:left\">" );
				}
				else{//background-image: url('/GrayShine'); 
					body.append( "<div style=\"background-image: url('/DarkGrayShine'); padding: 2px; border: 1px solid gray; margin:3px; float:left\">" );
				}
				body.append( scanned[c].toString() + "<br/>[" + scanned[c].getState().toString().toLowerCase() + "]" );
				body.append( "</div>" );
			}
		}
		
		
		body.append( "</td>" );
		
		body.append( "</tr></table>" );
		
		
		return body;
	}
	
	private static String createSummaryRow( String name, String value ){
		return "<tr class=\"Background1\"><td class=\"Text_3\">" + name + "</td><td class=\"Text_3\">" + value + "</td></tr>";
	}
}
