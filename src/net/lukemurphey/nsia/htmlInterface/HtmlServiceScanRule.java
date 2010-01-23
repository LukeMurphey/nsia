package net.lukemurphey.nsia.htmlInterface;

import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.htmlInterface.Html.MessageType;
import net.lukemurphey.nsia.scan.LineParseException;
import net.lukemurphey.nsia.scan.NetworkPortRange;
import net.lukemurphey.nsia.scan.ServiceScanRule;
import net.lukemurphey.nsia.trustBoundary.ApiScanData;

public class HtmlServiceScanRule extends HtmlContentProvider {

	public static final int OP_EDIT_SERVER_ADDRESS_INVALID = 101;
	public static final int OP_EDIT_PORTS_TO_SCAN_INVALID = 102;
	public static final int OP_EDIT_PORTS_EXPECTED_OPEN_INVALID = 103;
	private static final int OP_EDIT_RULE_ID_INVALID = 104;
	private static final int OP_EDIT_SITE_GROUP_ID_INVALID = 105;
	private static final int OP_EDIT_SCAN_FREQUENCY_INVALID = 106;
	
	
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, InvalidHtmlOperationException{
		return getHtml( requestDescriptor, null );
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, InvalidHtmlOperationException{
	
		// 1 -- Get the parameters
		StringBuffer body = new StringBuffer();
		String title = "Service Scan Rule";
		
		long scanRuleId = -1;
		long siteGroupId = -1;
		
		try{
			if( actionDesc == null ){
				actionDesc = performAction(requestDescriptor);
			}
			else {
				actionDesc = new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
			}
			
			String portsToScan = "";
			String portsExpectedOpen = "";
			String serverAddress = "";
			int scanFrequencyUnits = 0;
			int scanFrequencyValue = 0;
			ServiceScanRule scanRule = null;
			
			//		1.1 -- Get the rule ID
			if( requestDescriptor.request.getParameter("RuleID") != null ){
				try{
					scanRuleId = Long.parseLong( requestDescriptor.request.getParameter("RuleID") );
				}
				catch( NumberFormatException e ){
					return new ContentDescriptor( title, Html.getWarningNote("A valid rule was not provided") );
				}
			}
			else if( actionDesc.result == ActionDescriptor.OP_ADD_SUCCESS && actionDesc.addData != null && actionDesc.addData instanceof Long ){
				scanRuleId = (Long)actionDesc.addData;
			}
			
			// Load the associated rule
			if( scanRuleId >= 0 ){
				ApiScanData scanData = new ApiScanData(Application.getApplication());
				
				scanRule = (ServiceScanRule)scanData.getScanRule(requestDescriptor.sessionIdentifier, scanRuleId);
				
				portsToScan = NetworkPortRange.convertToString( scanRule.getPortToScan() );
				portsExpectedOpen = NetworkPortRange.convertToString( scanRule.getPortsExpectedOpen() );
				serverAddress = scanRule.getServerAddress();
			}
			
			//		1.2 -- Get the site group ID
			if( requestDescriptor.request.getParameter("SiteGroupID") != null ){
				try{
					siteGroupId = Long.parseLong( requestDescriptor.request.getParameter("SiteGroupID") );
					//System.out.println( "SiteGroupID = " + requestDescriptor.request.getParameter("SiteGroupID") );
				}
				catch( NumberFormatException e ){
					return new ContentDescriptor( title, Html.getWarningNote("The site group identfier is invalid") );
				}
			}
			else if( scanRuleId > -1 && siteGroupId <= 0 ){
				try{
					ApiScanData scanData = new ApiScanData(Application.getApplication());
					siteGroupId = scanData.getAssociatedSiteGroup( requestDescriptor.sessionIdentifier, scanRuleId );

				}catch(InsufficientPermissionException e){
					//Ignore this exception, it should not occur since we clearly have read permissions at this point (we already checked)
				}
				catch(NotFoundException e){
					throw new InvalidHtmlOperationException("Site Group Identifier Not Found", "The Site Group associated with the rule could not be identified since the rule does not exist", "/");
				}
			}
			
			//		1.3 -- Get the scan frequency
			if( (scanFrequencyUnits == 0 || scanFrequencyValue == 0) && scanRule != null){
				int frequency = scanRule.getScanFrequency();
				if( ( frequency % 86400) == 0){ //Days
					scanFrequencyUnits = 86400;
					scanFrequencyValue = frequency / 86400;
				}
				else if( ( frequency % 3600) == 0){ //Hours
					scanFrequencyUnits = 3600;
					scanFrequencyValue = frequency / 3600;
				}
				else if( ( frequency % 60) == 0){ //Minutes
					scanFrequencyUnits = 60;
					scanFrequencyValue = frequency / 60;
				}
				else { //Seconds
					scanFrequencyUnits = 1;
					scanFrequencyValue = frequency;
				}
			}
			else{
				scanFrequencyUnits = 86400;
				scanFrequencyValue = 1;
			}
			
			//		1.4 -- Get the ports to scan
			if( requestDescriptor.request.getParameter("Server") != null ){
				serverAddress = requestDescriptor.request.getParameter("Server");
			}
			
			//		1.5 -- Get the ports expected open
			if( requestDescriptor.request.getParameter("PortsExpectedOpen2") != null ){
				portsExpectedOpen = requestDescriptor.request.getParameter("PortsExpectedOpen2");
			}
			else if( requestDescriptor.request.getParameter("PortsExpectedOpen") != null ){
				portsExpectedOpen = requestDescriptor.request.getParameter("PortsExpectedOpen");
			}
			
			//		1.6 -- Get the ports to scan
			if( requestDescriptor.request.getParameter("PortsToScan2") != null ){
				portsToScan = requestDescriptor.request.getParameter("PortsToScan2");
			}
			else if( requestDescriptor.request.getParameter("PortsToScan") != null ){
				portsToScan = requestDescriptor.request.getParameter("PortsToScan");
			}
	
			
			// 2 -- Output the form
			body.append(Html.renderMessages(requestDescriptor.userId));
			
			body.append( "<script src=\"/codepress/codepress.js\" type=\"text/javascript\"></script>");
			
			body.append( "<script type=\"text/javascript\">");
			body.append( "function submitEditorForm(editorform){");
			//body.append( "alert(\"Code is\" + cp1.getCode() + \"=\" + cp2.getCode());");
			body.append( "document.editorform.PortsToScan2.value = cp1.getCode();");
			body.append( "document.editorform.PortsExpectedOpen2.value = cp2.getCode();");
			body.append( "document.editorform.submit();");
			body.append( "return true;");
			body.append( "}");
			body.append( "</script>");
			
			body.append( Html.getSectionHeader("Service Monitoring", null ) );
			body.append( "<form name=\"editorform\" id=\"editorform\" onSubmit=\"return submitEditorForm(this.form)\" action=\"ServiceMonitoring\" method=\"get\"><table class=\"DataTable\">"  );
	
			//	2.1 -- Output scan frequency
			if( actionDesc.result == OP_EDIT_SCAN_FREQUENCY_INVALID )
				body.append( "<tr class=\"ValidationFailed\">" );
			else
				body.append( "<tr class=\"Background1\">" );
			
			body.append( "<td class=\"Text_3\">Scan Frequency</td><td>");
			body.append( "<input class=\"textInput\" type=\"text\" name=\"ScanFrequencyValue\" value=\"" ).append(scanFrequencyValue);
			body.append("\">&nbsp;&nbsp;<select name=\"ScanFrequencyUnits\">");
			
	
			//		2.1.1 -- Days option
			body.append( "<option value=\"86400\"" );
			if( scanFrequencyUnits == 84600 )
				body.append(" selected");
			body.append(">Days</option>");
	
			//		2.1.2 -- Hours option
			body.append( "<option value=\"3600\"" );
			if( scanFrequencyUnits == 3600 )
				body.append(" selected");
			body.append(">Hours</option>");
	
			//		2.1.3 -- Minutes option
			body.append( "<option value=\"60\"" );
			if( scanFrequencyUnits == 60 )
				body.append(" selected");
			body.append(">Minutes</option>");
	
			//		2.1.4 -- Seconds option
			body.append( "<option value=\"1\"" );
			if( scanFrequencyUnits == 1 )
				body.append(" selected");
			body.append(">Seconds</option>");
	
			body.append("</select></td></tr>");
			
			//	2.2 -- Output server address
			if( actionDesc.result == OP_EDIT_SERVER_ADDRESS_INVALID  )
				body.append( "<tr class=\"ValidationFailed\">" );
			else
				body.append( "<tr class=\"Background1\">" );
	
			body.append( "<td class=\"Text_3\">Server Address</td><td><input class=\"textInput\" size=\"40\" type=\"text\" name=\"Server\" value=\"").append( StringEscapeUtils.escapeHtml( serverAddress ) ).append("\"></td></tr>");
			
			//	 2.3 -- Ports to Scan
			if( actionDesc.result == OP_EDIT_PORTS_TO_SCAN_INVALID)
				body.append( "<tr class=\"ValidationFailed\">" );
			else
				body.append( "<tr class=\"Background1\">" );
	
			body.append( "<td style=\"vertical-align: top;\" class=\"Text_3\">Ports to Scan</td><td><textarea id=\"cp1\" class=\"codepress ports\" wrap=\"virtual\" rows=\"6\" cols=\"48\" name=\"PortsToScan\">").append( StringEscapeUtils.escapeHtml( portsToScan ) ).append( "</textarea>");
			body.append( "<input type=\"hidden\" name=\"PortsToScan2\" value=\"").append( StringEscapeUtils.escapeHtml( portsToScan ) ).append( "\">" );
			body.append( "</td></tr>" );
	
			//	 2.4 -- Ports expected open
			if( actionDesc.result == OP_EDIT_PORTS_EXPECTED_OPEN_INVALID)
				body.append( "<tr class=\"ValidationFailed\">" );
			else
				body.append( "<tr class=\"Background1\">" );
	
			body.append( "<td style=\"vertical-align: top;\" class=\"Text_3\">Ports Expected Open</td><td><textarea id=\"cp2\" class=\"codepress ports\" wrap=\"virtual\" rows=\"6\" cols=\"48\" name=\"PortsExpectedOpen\">").append( StringEscapeUtils.escapeHtml( portsExpectedOpen ) ).append( "</textarea>");
			body.append( "<input type=\"hidden\" name=\"PortsExpectedOpen2\" value=\"").append( StringEscapeUtils.escapeHtml( portsExpectedOpen ) ).append( "\">" );
			body.append( "</td></tr>" );
	
			body.append( "<tr class=\"Background3\"><td class=\"alignRight\" colspan=\"99\">");
			
			if( scanRuleId >= 0 ) {
				body.append( "<input type=\"hidden\" name=\"Action\" value=\"Edit\">")
				.append( "<input type=\"hidden\" name=\"RuleID\" value=\"").append( scanRuleId ).append( "\">" );
			}
			
			else if( siteGroupId >= 0 ){
				body.append( "<input type=\"hidden\" name=\"Action\" value=\"New\">")
				.append( "<input type=\"hidden\" name=\"SiteGroupID\" value=\"").append( siteGroupId ).append( "\">");
			}

			
			//body.append( "<input class=\"button\" type=\"submit\" onClick=\"showHourglass('Scanning...')\" value=\"Auto-Populate\" name=\"AutoPopulate\">&nbsp;&nbsp;")
			body.append( "<input class=\"button\" type=\"submit\" value=\"Apply Changes\" name=\"Submit\"></td></tr>"  );
			
			body.append( "</table>" );
		} catch (NotFoundException e) {
			body.append( Html.getWarningDialog("Item Not Found", e.getMessage() ) );
		}catch (InsufficientPermissionException e) {
			body.append( Html.getWarningDialog("Insufficient Permission", e.getMessage() ) );
		}
		
		// 3 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "Site Group", "/SiteGroup?SiteGroupID=" +siteGroupId );		
		
		if( scanRuleId > -1 ){
			navPath.addPathEntry( "Edit Rule","/ScanRule?Action=Edit&RuleID=" + scanRuleId );
		}
		else{
			navPath.addPathEntry( "New Rule","/ScanRule?Action=New&SiteGroupID=" + siteGroupId );
		}
		
		String navigationHtml = Html.getNavigationPath( navPath );
		
		
		// 4 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
		
		menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Add Group", "sSiteGroup?Action=New", MenuItem.LEVEL_TWO) );
		
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
	
	private static ActionDescriptor performAction(WebConsoleConnectionDescriptor requestDescriptor) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, NotFoundException{
		
		String action = requestDescriptor.request.getParameter("Action");
		//String submit = requestDescriptor.request.getParameter("Scan");
		
		
		// 1 -- Load the relevant page
		
		//	 1.1 -- View the rule
		if( action == null ){
			return new ActionDescriptor( ActionDescriptor.OP_VIEW);
		}
		
		//	 1.2 -- Create a new rule or edit an existing one
		else if( action.equalsIgnoreCase( "New" ) || action.equalsIgnoreCase( "Edit" )  ){
			long scanRuleId = -1;
			int siteGroupId = -1;
			String portsToScan = null;
			String portsExpectedOpen = null;
			NetworkPortRange[] portsToScanRanges = null;
			NetworkPortRange[] portsExpectedOpenRanges = null;
			String serverAddress = "";
			int scanFrequencyValue = 12;
			int scanFrequencyUnits = 10;
			
			//Determine if changes have been submitted
			if( action.equalsIgnoreCase( "New" ) && requestDescriptor.request.getParameter("Submit") == null ){
				return new ActionDescriptor(ActionDescriptor.OP_ADD);
			}
			
			if( action.equalsIgnoreCase( "Edit" ) && requestDescriptor.request.getParameter("Submit") == null ){
				return new ActionDescriptor(ActionDescriptor.OP_UPDATE);
			}
			
			//		1.2.1 -- Get the rule ID
			if( requestDescriptor.request.getParameter("RuleID") != null ){
				try{
					scanRuleId = Long.parseLong( requestDescriptor.request.getParameter("RuleID") );
				}
				catch( NumberFormatException e ){
					Html.addMessage(MessageType.WARNING, "The list of addresses to scan was not provided", requestDescriptor.userId.longValue());
					return new ActionDescriptor(OP_EDIT_RULE_ID_INVALID);
				}
			}
			
			//		1.2.2 -- Get the site group ID
			if( requestDescriptor.request.getParameter("SiteGroupID") != null ){
				try{
					siteGroupId = Integer.parseInt( requestDescriptor.request.getParameter("SiteGroupID") );
				}
				catch( NumberFormatException e ){
					Html.addMessage(MessageType.WARNING, "A valid site group identfier was not provided", requestDescriptor.userId.longValue());
					return new ActionDescriptor(OP_EDIT_SITE_GROUP_ID_INVALID);
				}
			}
			
			//		1.2.3 -- Get the scan frequency
			if( requestDescriptor.request.getParameter("ScanFrequencyValue") != null && requestDescriptor.request.getParameter("ScanFrequencyUnits") != null  ){
				
				//		1.2.3.1 -- Parse the scan frequency
				try{
					scanFrequencyValue = Integer.parseInt( requestDescriptor.request.getParameter("ScanFrequencyValue"));
					scanFrequencyUnits  = Integer.parseInt( requestDescriptor.request.getParameter("ScanFrequencyUnits"));
				}
				catch( NumberFormatException e ){
					Html.addMessage(MessageType.WARNING, "A valid scan frequency was not provided", requestDescriptor.userId.longValue());
					return new ActionDescriptor(OP_EDIT_SCAN_FREQUENCY_INVALID);
				}
				
				//		1.2.3.2 -- Make sure the scan frequency is not too low
				if( scanFrequencyUnits == 1 && scanFrequencyValue < 30){
					Html.addMessage(MessageType.WARNING, "The scan frequency cannot be less than 30 seconds", requestDescriptor.userId.longValue());
					return new ActionDescriptor(OP_EDIT_SCAN_FREQUENCY_INVALID);
				}
			}
			
			//		1.2.4 -- Get the ports to scan
			if( requestDescriptor.request.getParameter("Server") != null && requestDescriptor.request.getParameter("Server").length() > 0 ){
				serverAddress = requestDescriptor.request.getParameter("Server");
			}
			else{
				Html.addMessage(MessageType.WARNING, "A server to scan was not provided", requestDescriptor.userId.longValue());
				return new ActionDescriptor(OP_EDIT_SERVER_ADDRESS_INVALID);
			}
			
			//		1.2.5 -- Get the ports to scan
			if( requestDescriptor.request.getParameter("PortsToScan2") != null ){
				portsToScan = requestDescriptor.request.getParameter("PortsToScan2");
			}
			else if( requestDescriptor.request.getParameter("PortsToScan") != null ){
				portsToScan = requestDescriptor.request.getParameter("PortsToScan");
				
			}
			else{
				Html.addMessage(MessageType.WARNING, "A list of ports be scanned was not provided", requestDescriptor.userId.longValue());
				return new ActionDescriptor(OP_EDIT_PORTS_TO_SCAN_INVALID);
			}
			
			try{
				portsToScanRanges = NetworkPortRange.parseRange(portsToScan);
			}
			catch(LineParseException e){
				Html.addMessage(MessageType.WARNING, "A list of ports to scan is invalid: " + e.getMessage(), requestDescriptor.userId.longValue());
				return new ActionDescriptor(OP_EDIT_PORTS_TO_SCAN_INVALID);
			}
			
			//		1.2.6 -- Get the ports expected open
			if( requestDescriptor.request.getParameter("PortsExpectedOpen2") != null ){
				portsExpectedOpen = requestDescriptor.request.getParameter("PortsExpectedOpen2");
			}
			else if( requestDescriptor.request.getParameter("PortsExpectedOpen") != null ){
				portsExpectedOpen = requestDescriptor.request.getParameter("PortsExpectedOpen");
			}
			else{
				Html.addMessage(MessageType.WARNING, "A list of ports that are expected to be open was not provided", requestDescriptor.userId.longValue());
				return new ActionDescriptor(OP_EDIT_PORTS_EXPECTED_OPEN_INVALID);
			}
			
			try{
				portsExpectedOpenRanges = NetworkPortRange.parseRange(NetworkPortRange.SocketState.OPEN, portsExpectedOpen);
			}
			catch(LineParseException e){
				Html.addMessage(MessageType.WARNING, "A list of ports that are expected to be open is invalid: " + e.getMessage(), requestDescriptor.userId.longValue());
				return new ActionDescriptor(OP_EDIT_PORTS_EXPECTED_OPEN_INVALID);
			}
			
			//		1.2.7 -- Make sure the SiteGroup ID (is creating a new rule) or Rule ID (if updating a rule) was provided
			if( action.equalsIgnoreCase("New") && siteGroupId <= 0){
				Html.addMessage(MessageType.WARNING, "A valid SiteGroup ID was not provided", requestDescriptor.userId.longValue());
				return new ActionDescriptor(OP_EDIT_SITE_GROUP_ID_INVALID);
			}
			else if( action.equalsIgnoreCase("Edit") && scanRuleId < 0){
				Html.addMessage(MessageType.WARNING, "A valid rule ID was not provided", requestDescriptor.userId.longValue());
				return new ActionDescriptor(OP_EDIT_RULE_ID_INVALID);
			}
			
			//		1.2.8 -- Perform the operation
			ApiScanData scanData = new ApiScanData(Application.getApplication());
			
			if( action.equalsIgnoreCase("New") ){
				try {
					long rule_id = scanData.addServiceScanRule(requestDescriptor.sessionIdentifier, siteGroupId, serverAddress, portsToScanRanges, portsExpectedOpenRanges, scanFrequencyUnits * scanFrequencyValue );
					Html.addMessage(MessageType.INFORMATIONAL, "Rule successfully created", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_ADD_SUCCESS, rule_id);
				} catch (InsufficientPermissionException e) {
					Html.addMessage(MessageType.WARNING, "You do not have permission to create a new rule", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_ADD_FAILED);
				}
			}
			else if( action.equalsIgnoreCase("Edit") ){
				try {
					scanData.updateServiceScanRule(requestDescriptor.sessionIdentifier, scanRuleId, serverAddress, portsToScanRanges, portsExpectedOpenRanges, scanFrequencyUnits * scanFrequencyValue );
					Html.addMessage(MessageType.INFORMATIONAL, "Rule successfully modified", requestDescriptor.userId.longValue());
				} catch (InsufficientPermissionException e) {
					Html.addMessage(MessageType.WARNING, "You do not have permission to update this rule", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED);
				}
			}
		}
		
		return new ActionDescriptor( ActionDescriptor.OP_NO_OPERATION);
		
	}
}
