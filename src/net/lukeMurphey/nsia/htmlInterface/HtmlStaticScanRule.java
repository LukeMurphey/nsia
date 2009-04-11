package net.lukeMurphey.nsia.htmlInterface;

import java.security.NoSuchAlgorithmException;

import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;

import java.net.*;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.InputValidationException;
import net.lukeMurphey.nsia.InsufficientPermissionException;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.NotFoundException;
import net.lukeMurphey.nsia.scanRules.HttpHeaderRule;
import net.lukeMurphey.nsia.scanRules.HttpStaticScanRule;
import net.lukeMurphey.nsia.scanRules.HttpStaticScanResult;
import net.lukeMurphey.nsia.scanRules.ScanResult;
import net.lukeMurphey.nsia.scanRules.ScanException;
import net.lukeMurphey.nsia.scanRules.ScanResultCode;
import net.lukeMurphey.nsia.scanRules.ScanRule.ScanRuleLoadFailureException;
import net.lukeMurphey.nsia.trustBoundary.ApiScanData;
import net.lukeMurphey.nsia.trustBoundary.ApiScannerController;
import net.lukeMurphey.nsia.htmlInterface.Html.MessageType;


public class HtmlStaticScanRule extends HtmlContentProvider {
	
	private static int OP_HEADER_OP = 101;
	private static int OP_ADD_URL_INVALID = 102;
	private static int OP_ADD_RESPONSE_CODE_INVALID = 103;
	private static int OP_ADD_HASH_INVALID = 104;
	private static int OP_ADD_URL_INVALID_AUTOPOP = 105;
	private static int OP_ADD_AUTOPOP_FAILED = 106;
	private static int OP_ADD_AUTOPOP_SUCCESS = 107;
	private static int OP_ADD_SITEGROUP_INVALID = 108;
	private static int OP_EDIT_HASH_INVALID = 109;
	private static int OP_EDIT_RESPONSE_CODE_INVALID = 110;
	private static int OP_EDIT_AUTOPOP_SUCCESS = 111;
	private static int OP_EDIT_AUTOPOP_FAILED = 112;
	private static int OP_EDIT_URL_INVALID = 113;
	private static int OP_EDIT_RULE_ID_INVALID = 114;
	private static int OP_EDIT_URL_INVALID_AUTOPOP = 115;
	private static int OP_DELETE_RULE_SUCCESS = 116;
	//private static int OP_DELETE_RULE_FAILED = 117;
	private static int OP_EDIT_SCAN_FREQUENCY_INVALID = 118;
	private static int OP_ADD_SCANFREQUENCY_INVALID = 119;
	private static int OP_SCAN_SUCCESS = 120;
	private static int OP_SCAN_FAILED = 121;
	
	private static final String DEFAULT_HASH_ALGORITHM = "SHA1";
	
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, NotFoundException, InputValidationException{
		return getHtml( requestDescriptor, null );
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, NotFoundException, InputValidationException{
		
		ApiScanData scanData = new ApiScanData(Application.getApplication());
		
		// 0 -- Perform any pending actions
		if( actionDesc == null )
			actionDesc  = performAction( requestDescriptor, scanData);
		
		
		// 1 -- Output the main content
		
		// 1.1 -- Perform header operations
		if( actionDesc.result == OP_HEADER_OP )
			return HtmlRules_HttpDataHash_Header.getHtml( requestDescriptor, scanData );
		
		if( actionDesc.result == OP_DELETE_RULE_SUCCESS )
			return HtmlSiteGroup.getHtml(requestDescriptor, actionDesc);
		
		// 1.2 -- Create new rule
		else if( actionDesc.result == ActionDescriptor.OP_ADD || actionDesc.result == OP_ADD_HASH_INVALID || actionDesc.result == OP_ADD_RESPONSE_CODE_INVALID
				|| actionDesc.result == OP_ADD_URL_INVALID || actionDesc.result == OP_ADD_URL_INVALID_AUTOPOP || actionDesc.result == OP_ADD_AUTOPOP_FAILED || actionDesc.result == OP_ADD_SCANFREQUENCY_INVALID 
				|| actionDesc.result == OP_ADD_AUTOPOP_SUCCESS )
			return getRuleNew( requestDescriptor, actionDesc, scanData);
		
		// 1.3 -- Edit rule
		else if( actionDesc.result == ActionDescriptor.OP_UPDATE || actionDesc.result == OP_EDIT_HASH_INVALID || actionDesc.result == OP_EDIT_RESPONSE_CODE_INVALID
				|| actionDesc.result == OP_EDIT_URL_INVALID || actionDesc.result == OP_EDIT_URL_INVALID_AUTOPOP || actionDesc.result == OP_EDIT_AUTOPOP_FAILED 
				|| actionDesc.result == OP_EDIT_AUTOPOP_SUCCESS )
			return getRuleEdit( requestDescriptor, actionDesc, scanData);
		
		// 1.4 -- View the rule (default action)
		else
			return getRuleView( requestDescriptor, actionDesc, scanData);
	}

	
	/**
	 * Show the view for creating a new rule.
	 * @param request
	 * @param response
	 * @param requestDescriptor
	 * @param httpMethod
	 * @param actionDesc
	 * @param xScanData
	 * @return
	 * @throws GeneralizedException 
	 * @throws NoSessionException 
	 * @throws NotFoundException 
	 * @throws Exception
	 */
	private static ContentDescriptor getRuleEdit(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc, ApiScanData xScanData) throws NoSessionException, GeneralizedException, NotFoundException{
		String title = "";
		StringBuffer body = new StringBuffer();
		
		// 1 -- Output any messages
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		// 2 -- Output the main content
		String url = null;
		int responseCode = -1;
		String fingerprint = null;
		int scanFrequencyUnits = 0;
		int scanFrequencyValue = 0;
		
		//	 2.1  -- Get the relevant fields
		long scanRuleId = -1;
		
		//		2.1.1 -- Get the rule ID
		if( requestDescriptor.request.getParameter("RuleID") != null ){
			try{
				scanRuleId = Long.parseLong( requestDescriptor.request.getParameter("RuleID") );
			}
			catch( NumberFormatException e ){
				return new ContentDescriptor( "HTTP Message Digest Rule", Html.getWarningNote("A valid rule was not provided") );
			}
		}
		
		//		2.1.2 -- Get the actual rule
		try{
			ApiScanData scanData= new ApiScanData( Application.getApplication() );
			HttpStaticScanRule scanRule = (HttpStaticScanRule)scanData.getScanRule( requestDescriptor.sessionIdentifier, scanRuleId );

			if( requestDescriptor.request.getParameter("ScanFrequencyUnits") != null && requestDescriptor.request.getParameter("ScanFrequencyValue") != null ){
				scanFrequencyUnits = Integer.parseInt(requestDescriptor.request.getParameter("ScanFrequencyUnits"));
				scanFrequencyValue = Integer.parseInt(requestDescriptor.request.getParameter("ScanFrequencyValue"));
			}

			//		2.1.3 -- Get the various other parameters
			if( actionDesc.addData != null){
				HttpStaticScanResult result = (HttpStaticScanResult)actionDesc.addData;

				url = result.getUrl().toString();
				responseCode = result.getActualResponseCode();
				
				if( result.getActualHashValue() != null )
					fingerprint = result.getActualHashValue();//GenericHtmlGenerator.splitString( result.getActualHashValue() , 32, "\n" );
				else
					fingerprint = "";
				
			}
			else{
				// 		2.1.3.1 -- Get the URL
				if( requestDescriptor.request.getParameter("URL") != null )
					url = requestDescriptor.request.getParameter("URL");
				else
					url =scanRule.getUrl().toString();

				// 		2.1.3.2 -- Get the response code
				if( requestDescriptor.request.getParameter("ResponseCode") != null ){
					try{
						responseCode = Integer.parseInt( requestDescriptor.request.getParameter("ResponseCode") );
					}
					catch( NumberFormatException e ){
						responseCode = -1;
					}
				}

				if( responseCode == -1)
					responseCode = scanRule.getExpectedResponseCode();

				// 		2.1.3.3 -- Get the hash
				if( requestDescriptor.request.getParameter("Hash") != null )
					fingerprint = requestDescriptor.request.getParameter("Hash");
				else
					fingerprint = scanRule.getExpectedDataHashValue();

				// 		2.1.3.4 -- Get the scan frequency
				if( scanFrequencyUnits == 0 || scanFrequencyValue == 0){
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
			}

			// 3 -- Output the main content

			//	 3.1 -- Output the section header
			body.append( Html.getSectionHeader( "HTTP Static Content Rule", null ) );

			body.append( "<form action=\"HttpStaticScanRule\" method=\"post\"><table>"  );

			//	3.2 -- Output scan frequency
			if( actionDesc.result == OP_EDIT_SCAN_FREQUENCY_INVALID )
				body.append( "<tr class=\"ValidationFailed\">" );
			else
				body.append( "<tr class=\"Background1\">" );


			body.append( "<td class=\"Text_3\">Scan Frequency</td><td>");
			body.append( "<input class=\"textInput\" type=\"text\" name=\"ScanFrequencyValue\" value=\"" ).append(scanFrequencyValue);
			body.append("\">&nbsp;&nbsp;<select name=\"ScanFrequencyUnits\">");

			//		3.2.1 -- Days option
			body.append( "<option value=\"86400\"" );
			if( scanFrequencyUnits == 84600 )
				body.append(" selected");
			body.append(">Days</option>");

			//		3.2.2 -- Hours option
			body.append( "<option value=\"3600\"" );
			if( scanFrequencyUnits == 3600 )
				body.append(" selected");
			body.append(">Hours</option>");

			//		3.2.3 -- Minutes option
			body.append( "<option value=\"60\"" );
			if( scanFrequencyUnits == 60 )
				body.append(" selected");
			body.append(">Minutes</option>");

			//		3.2.4 -- Seconds option
			body.append( "<option value=\"1\"" );
			if( scanFrequencyUnits == 1 )
				body.append(" selected");
			body.append(">Seconds</option>");

			body.append("</select></td></tr>");

			//	3.3 -- Output URL
			if( actionDesc.result == OP_EDIT_URL_INVALID || actionDesc.result == OP_EDIT_URL_INVALID_AUTOPOP )
				body.append( "<tr class=\"ValidationFailed\">" );
			else
				body.append( "<tr class=\"Background1\">" );

			body.append( "<td class=\"Text_3\">URL</td><td><input class=\"textInput\" size=\"48\" type=\"text\" name=\"URL\" value=\"").append( StringEscapeUtils.escapeHtml( url ) ).append("\"></td></tr>");

			//	3.4 -- Output response code
			if( actionDesc.result == OP_EDIT_RESPONSE_CODE_INVALID)
				body.append( "<tr class=\"ValidationFailed\">" );
			else
				body.append( "<tr class=\"Background1\">" );

			if( responseCode >= 0)
				body.append( "<td class=\"Text_3\">Response Code</td><td><input class=\"textInput\" size=\"48\" type=\"text\" name=\"ResponseCode\" value=\"" ).append( responseCode ).append( "\"></td></tr>" );
			else
				body.append( "<td class=\"Text_3\">Response Code</td><td><input class=\"textInput\" size=\"48\" type=\"text\" name=\"ResponseCode\" value=\"\"></td></tr>"  );

			//  3.5 -- Output hash
			if( actionDesc.result == OP_EDIT_HASH_INVALID)
				body.append( "<tr class=\"ValidationFailed\">" );
			else
				body.append( "<tr class=\"Background1\">" );

			body.append( "<td class=\"Text_3\">Data Fingerprint</td><td><textarea wrap=\"virtual\" rows=\"6\" cols=\"48\" name=\"Hash\">").append( StringEscapeUtils.escapeHtml( fingerprint ) ).append( "</textarea></td></tr>" );

			// 3.6 -- Output form end
			body.append( "<tr class=\"Background3\"><td class=\"alignRight\" colspan=\"99\"><input type=\"hidden\" name=\"Action\" value=\"Edit\">")
			.append( "<input type=\"hidden\" name=\"RuleID\" value=\"").append( scanRuleId).append( "\">" )
			.append( "<input class=\"button\" type=\"submit\" onClick=\"showHourglass('Scanning...')\" value=\"Auto-Populate\" name=\"AutoPopulate\">&nbsp;&nbsp;")
			.append( "<input class=\"button\" type=\"submit\" value=\"Apply Changes\" name=\"Submit\"></td></tr>"  );

			if ( actionDesc.result == ActionDescriptor.OP_VIEW ){
				body.append( "" );
			}

			body.append( "</table></form>" );

		}catch(InsufficientPermissionException e){
			return HtmlProblemDialog.getHtml(requestDescriptor, "Insufficient Permission", "You do not have permission to view this rule", HtmlProblemDialog.DIALOG_WARNING, "Console", "Return to Main Dashboard");
		}
		
		long siteGroupId;
		
		// 3 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		try{
			siteGroupId = xScanData.getAssociatedSiteGroup( requestDescriptor.sessionIdentifier, scanRuleId );
			navPath.addPathEntry( "Site Group", "/SiteGroup?SiteGroupID=" +siteGroupId );
		}catch(InsufficientPermissionException e){
			//Ignore this exception, it should not occur since we clearly have read permissions at this point (we already checked)
		}
		
		navPath.addPathEntry( "Edit Rule","/HttpStaticScanRule?Action=Edit&RuleID=" + scanRuleId );
		String navigationHtml = Html.getNavigationPath( navPath );
		
		// 4 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
		
		menuItems.add( new MenuItem("Site Group", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Edit", "/SiteGroup?SiteGroupId=" + requestDescriptor.request.getParameter("SiteGroupId"), MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Delete", "/SiteGroup?Action=Delete&SiteGroupId=" + requestDescriptor.request.getParameter("SiteGroupId"), MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Scan Now", "/SiteGroup?Action=Scan&SiteGroupId=" + requestDescriptor.request.getParameter("SiteGroupId"), MenuItem.LEVEL_TWO, "showHourglass('Scanning...');") );
		
		String menuOutput = Html.getMenu( menuItems );
		
		// 5 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor(title, pageOutput);
	}
	
	
	/**
	 * Show the view for creating a new rule.
	 * @param request
	 * @param response
	 * @param requestDescriptor
	 * @param httpMethod
	 * @param actionDesc
	 * @param xScanData
	 * @return
	 * @throws Exception
	 */
	private static ContentDescriptor getRuleNew( WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc, ApiScanData xScanData){
		String title = "";
		StringBuffer body = new StringBuffer();
		
		// 1 -- Output any messages
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		// 2 -- Output the main content
		String url = null;
		int responseCode = -1;
		String fingerprint = null;
		int scanFrequencyUnits = 60;
		int scanFrequencyValue = 15;
		
		if( actionDesc.addData != null){
			HttpStaticScanResult result = (HttpStaticScanResult)actionDesc.addData;
			
			url = result.getUrl().toString();
			responseCode = result.getActualResponseCode();
			if( result.getActualHashValue() != null )
				fingerprint = result.getActualHashValue();
			else
				fingerprint = "";
			
		}
		else{
			
			if( requestDescriptor.request.getParameter("URL") != null )
				url = requestDescriptor.request.getParameter("URL");
			else
				url ="";
			
			if( requestDescriptor.request.getParameter("ResponseCode") != null ){
				try{
					responseCode = Integer.parseInt( requestDescriptor.request.getParameter("ResponseCode") );
				}
				catch( NumberFormatException e ){
					responseCode = -1;
				}
			}
			
			if( requestDescriptor.request.getParameter("Hash") != null )
				fingerprint = requestDescriptor.request.getParameter("Hash");
			else
				fingerprint ="";
		}
		
		if( requestDescriptor.request.getParameter("ScanFrequencyUnits") != null && requestDescriptor.request.getParameter("ScanFrequencyValue") != null ){
			scanFrequencyUnits = Integer.parseInt(requestDescriptor.request.getParameter("ScanFrequencyUnits"));
			scanFrequencyValue = Integer.parseInt(requestDescriptor.request.getParameter("ScanFrequencyValue"));
		}
		
		// 2 -- Output the main content
		long siteGroupId = -1;
		try{
			siteGroupId = Long.parseLong(requestDescriptor.request.getParameter("SiteGroupID"));
		}
		catch( NumberFormatException e ){
			siteGroupId = -1;
		}
		
		// 2.1 -- Output the section header
		body.append( Html.getSectionHeader( "HTTP Message Digest Rule", "Cryptographic Data Fingerprint" )  );
		
		if( siteGroupId == -1 ){
			return new ContentDescriptor( "HTTP Message Digest Rule", Html.getWarningNote("No site group exists with the given site group identifier"));
		}
		
		body.append( "<form action=\"HttpStaticScanRule\" method=\"post\"><table>"  );
		
		// 2.2 -- Output scan frequency
		if( actionDesc.result == OP_ADD_SCANFREQUENCY_INVALID )
			body.append( "<tr class=\"ValidationFailed\">"  );
		else
			body.append( "<tr class=\"Background1\">"  );		
		
		body.append( "<td class=\"Text_3\">Scan Frequency</td><td>");
		body.append( "<input class=\"textInput\" type=\"text\" name=\"ScanFrequencyValue\" value=\"" ).append(scanFrequencyValue);
		body.append("\">&nbsp;&nbsp;<select name=\"ScanFrequencyUnits\">");
		
		//		3.2.1 -- Days option
		body.append( "<option value=\"86400\"" );
		if( scanFrequencyUnits == 84600 )
			body.append(" selected");
		body.append(">Days</option>");
		
		//		3.2.2 -- Hours option
		body.append( "><option value=\"3600\"" );
		if( scanFrequencyUnits == 3600 )
			body.append(" selected");
		body.append(">Hours</option>");
		
		//		3.2.3 -- Minutes option
		body.append( "><option value=\"60\"" );
		if( scanFrequencyUnits == 60 )
			body.append(" selected");
		body.append(">Minutes</option>");
		
		//		3.2.4 -- Seconds option
		body.append( "><option value=\"1\"" );
		if( scanFrequencyUnits == 1 )
			body.append(" selected");
		body.append(">Seconds</option>");
		
		body.append("</select></td></tr>");
		
		// 2.3 -- Output URL
		if( actionDesc.result == OP_ADD_URL_INVALID || actionDesc.result == OP_ADD_URL_INVALID_AUTOPOP )
			body.append( "<tr class=\"ValidationFailed\">"  );
		else
			body.append( "<tr class=\"Background1\">"  );
		
		body.append( "<td class=\"Text_3\">URL</td><td><input class=\"textInput\" size=\"48\" type=\"text\" name=\"URL\" value=\"").append( StringEscapeUtils.escapeHtml(  url ) ).append( "\"></td></tr>" );
		
		// 2.4 -- Output response code
		if( actionDesc.result == OP_ADD_RESPONSE_CODE_INVALID)
			body.append( "<tr class=\"ValidationFailed\">" );
		else
			body.append( "<tr class=\"Background1\">" );
		
		if( responseCode >= 0)
			body.append( "<td class=\"Text_3\">Response Code</td><td><input class=\"textInput\" size=\"48\" type=\"text\" name=\"ResponseCode\" value=\"").append( responseCode ).append( "\"></td></tr>" );
		else
			body.append( "<td class=\"Text_3\">Response Code</td><td><input class=\"textInput\" size=\"48\" type=\"text\" name=\"ResponseCode\" value=\"\"></td></tr>" );
		
		//  2.5 -- Output hash
		if( actionDesc.result == OP_ADD_HASH_INVALID)
			body.append( "<tr class=\"ValidationFailed\">" );
		else
			body.append( "<tr class=\"Background1\">" );
		
		body.append( "<td class=\"Text_3\">Data Fingerprint</td><td><textarea wrap=\"virtual\" rows=\"6\" cols=\"48\" name=\"Hash\">").append( StringEscapeUtils.escapeHtml( fingerprint ) ).append( "</textarea></td></tr>" );
		
		// 2.6 -- Output form end
		body.append( "<tr class=\"Background3\"><td class=\"alignRight\" colspan=\"99\"><input type=\"hidden\" name=\"Action\" value=\"New\"><input type=\"hidden\" name=\"SiteGroupID\" value=\"").append( siteGroupId).append( "\"><input class=\"button\" type=\"submit\" onClick=\"showHourglass('Scanning...')\" value=\"Auto-Populate\" name=\"AutoPopulate\">&nbsp;&nbsp;<input class=\"button\" type=\"submit\" value=\"Add Rule\" name=\"Submit\"></td></tr>" );
		
		if ( actionDesc.result == ActionDescriptor.OP_VIEW ){
			body.append( ""  );
		}
		
		body.append( "</table></form>" );
		
		
		// 2 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "Site Group", "/SiteGroup?SiteGroupID=" + siteGroupId );
		navPath.addPathEntry( "New Rule","/HttpStaticScanRule?Action=New&SiteGroupID=" + siteGroupId );
		String navigationHtml = Html.getNavigationPath( navPath );
		
		// 3 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
		
		menuItems.add( new MenuItem("Site Group", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Edit", "/SiteGroup?SiteGroupId=" + requestDescriptor.request.getParameter("SiteGroupId"), MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Delete", "/SiteGroup?Action=Delete&SiteGroupId=" + requestDescriptor.request.getParameter("SiteGroupId"), MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Scan Now", "/SiteGroup?Action=Scan&SiteGroupId=" + requestDescriptor.request.getParameter("SiteGroupId"), MenuItem.LEVEL_TWO, "showHourglass('Scanning...');") );
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
	 * @throws NotFoundException 
	 * @throws Exception 
	 */
	private static ContentDescriptor getRuleView(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc, ApiScanData xScanData) throws NoSessionException, GeneralizedException, NotFoundException{
		String title = "";
		StringBuffer body = new StringBuffer();
		
		// 1 -- Perform any pending actions
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		
		// 2 -- Output the main content
		
		//	 2.1 -- Get the rule identifier
		long ruleId = -1;
		
		try{
			ruleId = Long.parseLong(requestDescriptor.request.getParameter("RuleID"));
		}
		catch( NumberFormatException e ){
			ruleId = -2;
		}
		
		//	 2.2 -- See if the ID is new due to an addition of a new rule
		if( ruleId < 0 ){
			Long ruleId_Long = (Long)actionDesc.addData;
			if( ruleId_Long != null )
				ruleId = ruleId_Long.longValue();
		}
		
		//	 2.3 -- See if the user just scanned the rule
		HttpStaticScanResult scanResult = null;
		if( "Scan".equalsIgnoreCase(requestDescriptor.request.getParameter("Action")) && actionDesc.addData != null){
			scanResult = (HttpStaticScanResult)actionDesc.addData;
		}
		
		long siteGroupId = -1;
		try{
			// 2.4 -- Get the rule
			HttpStaticScanRule scan = (HttpStaticScanRule)xScanData.getScanRule(requestDescriptor.sessionIdentifier, ruleId);
			siteGroupId = xScanData.getAssociatedSiteGroup( requestDescriptor.sessionIdentifier, ruleId );
			
			// 2.5 -- Output the section header
			body.append( Html.getSectionHeader( "HTTP Message Digest Rule", "Cryptographic Data Fingerprint" ) );
			
			// 2.6 -- Stop if the rule identifier is invalid or not provided
			if( ruleId < 0 ){
				body.append( Html.getWarningNote("A valid rule identifier was not provided") );
				return new ContentDescriptor( "HTTP Rule", body );
			}
			
			if( scan == null ){
				return new ContentDescriptor( "HTTP Message Digest Rule", Html.getWarningDialog("Invalid Rule Identifier", "No rule exists for the given rule identifier.<br>&nbsp;<br><a href=\"Console\">[Main Dashboard]</a>"));
			}
			
			body.append( "<form action=\"HttpStaticScanRule\" method=\"post\"><table>"  );
			
			//	 2.7 -- Translate the scan frequency into a more readable form
			int frequency = scan.getScanFrequency();
			int scanFrequencyValue = -1;
			String scanFrequencyUnits;
			
			if( ( frequency % 86400) == 0){ //Days
				scanFrequencyUnits = "Days";
				scanFrequencyValue = frequency / 86400;
			}
			else if( ( frequency % 3600) == 0){ //Hours
				scanFrequencyUnits = "Hours";
				scanFrequencyValue = frequency / 3600;
			}
			else if( ( frequency % 60) == 0){ //Minutes
				scanFrequencyUnits = "Minutes";
				scanFrequencyValue = frequency / 60;
			}
			else { //Seconds
				scanFrequencyUnits = "Seconds";
				scanFrequencyValue = frequency;
			}
	
			//	 2.8 -- Output the rule attributes 
			
			//		2.8.1 -- Scan frequency
			body.append( "<tr class=\"Background1\"><td class=\"Text_3\">Scan Frequency</td><td>Evaluated every " ).append( scanFrequencyValue ).append( " " ).append( scanFrequencyUnits ).append( "</td></tr>" );
			body.append( "<tr class=\"Background1\"><td class=\"Text_3\">URL</td><td>" ).append( StringEscapeUtils.escapeHtml( scan.getUrl().toString() ) ).append( "</td></tr>" );
			
			//		2.8.2 -- Response code
			if( scanResult != null && scanResult.getExpectedResponseCode() != scanResult.getActualResponseCode() )
				body.append( "<tr class=\"ValidationFailed\">" );
			else
				body.append( "<tr class=\"Background1\">" );
			
			body.append( "<td class=\"Text_3\">Response Code</td><td>").append( scan.getExpectedResponseCode()).append( "</td></tr>" );
			
			//		2.8.3 -- Data fingerprint (hashcode)
			if( scanResult != null && !scanResult.getActualHashValue().equals( scanResult.getExpectedHashValue() ) )
				body.append( "<tr class=\"ValidationFailed\">" );
			else
				body.append( "<tr class=\"Background1\">" );
			body.append( "<td class=\"Text_3\">Data Fingerprint</td><td>").append( Html.splitString( StringEscapeUtils.escapeHtml( scan.getExpectedDataHashValue() ) , 64, "<br>" ) ).append( "</td></tr>" );
			
			//	 2.9 -- Finish the form and the print the header rules
			body.append( "<tr class=\"Background3\"><td class=\"alignRight\" colspan=\"99\"><input type=\"hidden\" name=\"Action\" value=\"Edit\"><input type=\"hidden\" name=\"RuleID\" value=\"" ).append( ruleId ).append( "\"><input class=\"button\" type=\"submit\" value=\"Edit Rule\" name=\"Submit\"></td></tr>" );
			body.append( "</table></form>" );
			body.append( createHeaderRuleTable( scan.getHeaderRules(), ruleId ) );
			
		}catch(InsufficientPermissionException e){
			return HtmlProblemDialog.getHtml(requestDescriptor, "Insufficient Permission", "You do not have permission to view this rule", HtmlProblemDialog.DIALOG_WARNING, "Console", "Return to Main Dashboard");
		}
		
		// 3 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		
		if( siteGroupId >= 0 ){
			navPath.addPathEntry( "Site Group", "/SiteGroup?SiteGroupID=" + siteGroupId );
		}

		navPath.addPathEntry( "View Rule","/HttpStaticScanRule?RuleID=" + ruleId );
		String navigationHtml = Html.getNavigationPath( navPath );

		// 4 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();

		menuItems.add( new MenuItem("Rule", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Add Header Rule", "/HttpStaticScanRule?Action=NewHeaderRule&RuleID=" + ruleId, MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Edit", "/HttpStaticScanRule?RuleID=" + ruleId, MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Delete", "/HttpStaticScanRule?Action=Delete&RuleID=" + ruleId, MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Scan Now", "/HttpStaticScanRule?Action=Scan&RuleID=" + ruleId, MenuItem.LEVEL_TWO, "showHourglass('Scanning...');") );
		
		
		String menuOutput = Html.getMenu( menuItems );
		
		// 5 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor(title, pageOutput);
	}
	
	/**
	 * Create the table listing the header rules.
	 * @param headerRules
	 * @return
	 */
	private static String createHeaderRuleTable( HttpHeaderRule[] headerRules, long ruleId ){
		
		// 1 -- Start the table
		StringBuffer output = new StringBuffer();
		output.append( "<br><span class=\"Text_2\">HTTP Header Rules</span>" );
		
		output.append( "<table><tr class=\"Background0\"><td class=\"Text_3\">Rule Action</td><td class=\"Text_3\" colspan=\"2\">Header</td><td class=\"Text_3\" colspan=\"4\">Value</td></tr>" );
		
		// 2 -- Pring the rows
		if( headerRules.length == 0 ){
			output.append( "<tr class=\"Background1\"><td width=\"300\" class=\"InfoText\" colspan=\"7\">No header rules exist</td></tr>" );
		}
		
		for( int c = 0; c < headerRules.length; c++){
			
			HttpHeaderRule headerRule = headerRules[c];
			
			// 2.1 -- Print the rule action
			if( headerRule.getRuleType() == HttpStaticScanRule.MUST_MATCH )
				output.append( "<tr class=\"Background1\"><td class=\"GreenText\">== (must match)</td>" );
			else
				output.append( "<tr class=\"Background1\"><td class=\"RedText\">!= (must not match)</td>" );
			
			// 2.2 -- Print the name rule
			if( headerRule.getNameRuleType() != HttpHeaderRule.RULE_TYPE_STRING ){
				output.append( "<td>" ).append( headerRule.getHeaderNameRegex().pattern() ).append( "</td>" );
				output.append( "<td>(regex comparison)</td>" );
			}
			else{
				output.append( "<td>" ).append( headerRule.getHeaderNameString() ).append( "</td>" );
				output.append( "<td>(string comparison)</td>" );
			}				
			
			// 2.3 -- Print the value rule
			if( headerRule.getValueRuleType() != HttpHeaderRule.RULE_TYPE_STRING  ){
				output.append( "<td>" ).append( headerRule.getHeaderValueRegex().pattern() ).append( "</td>" );
				output.append( "<td>(regex comparison)</td>" );
			}
			else{
				output.append( "<td>" ).append( headerRule.getHeaderValueString() ).append( "</td>" );
				output.append( "<td>(string comparison)</td>" );
			}			
			
			// 2.4 -- Output the delete option
			output.append( "<td>").append( Html.getButton("/16_Delete", "Delete", "HttpStaticScanRule?Action=DeleteHeaderRule&HeaderRuleID=" + headerRule.getRuleId() + "&RuleID=" + ruleId, "Delete" )).append( "</td>" );
			
			// 2.5 -- Output the edit option
			output.append( "<td>").append( Html.getButton("/16_Configure", "Edit", "HttpStaticScanRule?Action=EditHeaderRule&HeaderRuleID=" + headerRule.getRuleId() + "&RuleID=" + ruleId, "Edit" ) ).append( "</td>" );
		}
		
		// 3 -- Close the table tag
		output.append( "</table>" );
		
		return output.toString();
	}
	
	private static ActionDescriptor performAction(WebConsoleConnectionDescriptor requestDescriptor, ApiScanData scanData) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, NotFoundException{
		
		String action = requestDescriptor.request.getParameter("Action");
		// 1 -- Load the relevant page
		
		//	 1.1 -- View the rule
		if( action == null )
			return new ActionDescriptor( ActionDescriptor.OP_VIEW);
		
		//	 1.2 -- Create a new rule
		else if( action.matches( "New" ) ){
			
			int responseCode;
			URL url;
			String hash;
			int siteGroupId = -1;
			boolean followRedirects = false;
			int scanFrequency = -1;
			
			if( requestDescriptor.request.getParameter("Submit") == null && requestDescriptor.request.getParameter("AutoPopulate") == null )
				return new ActionDescriptor( ActionDescriptor.OP_ADD );
			
			// 1.2.1 -- Get the site group ID
			try{
				siteGroupId = Integer.parseInt( requestDescriptor.request.getParameter("SiteGroupID") );
			}
			catch( NumberFormatException e ){
				Html.addMessage(MessageType.WARNING, "The site group ID is not valid", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_ADD_SITEGROUP_INVALID );
			}
			
			// 1.2.2 -- Get the scan frequency
			int scanFrequencyValue = Integer.parseInt( requestDescriptor.request.getParameter("ScanFrequencyValue"));
			int scanFrequencyUnits  = Integer.parseInt( requestDescriptor.request.getParameter("ScanFrequencyUnits"));
			scanFrequency = scanFrequencyValue * scanFrequencyUnits;
			
			if( scanFrequency < 30 ){
				Html.addMessage(MessageType.WARNING, "The scan frequency must be 30 seconds or greater", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_ADD_SCANFREQUENCY_INVALID );
			}
			
			
			// 1.2.3 -- Get the URL
			try {
				url = new URL( requestDescriptor.request.getParameter("URL") );
			} catch (MalformedURLException e) {
				Html.addMessage(MessageType.WARNING, "The URL is not valid", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_ADD_URL_INVALID );
			}
			if( url == null ){
				Html.addMessage(MessageType.WARNING, "The URL is not valid", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_ADD_URL_INVALID );
			}
			
			// 1.2.4 -- Auto-populate if requested
			if( requestDescriptor.request.getParameter("AutoPopulate") != null ){
				
				// 1.2.4.1 -- Get the data
				ApiScannerController apiScannerController = new ApiScannerController( Application.getApplication() );
				HttpStaticScanResult scanResult;
				
				try {
					scanResult = apiScannerController.scanHttpDataHash( requestDescriptor.sessionIdentifier, url, DEFAULT_HASH_ALGORITHM );
				} catch (InsufficientPermissionException e) {
					Html.addMessage(MessageType.WARNING, "You do not have permission to perform scans", requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_ADD_AUTOPOP_FAILED );
				} catch (InputValidationException e) {
					Html.addMessage(MessageType.WARNING, e.getMessage(), requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_ADD_URL_INVALID_AUTOPOP );
				} catch (ScanException e) {
					Html.addMessage(MessageType.WARNING, "The scan failed due to an error", requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_ADD_AUTOPOP_FAILED );
				}
				
				// 1.2.4.2 -- Return the result
				if( scanResult.getResultCode() == ScanResultCode.SCAN_FAILED ){
					Html.addMessage(MessageType.WARNING, "The site was not successfully scanned, the scan failed", requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_ADD_AUTOPOP_FAILED, scanResult );
				}
				else{
					Html.addMessage(MessageType.INFORMATIONAL, "Fields successfully auto-populated", requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_ADD_AUTOPOP_SUCCESS, scanResult );
				}
			}
			
			// 1.2.5 -- Get the response code
			if( requestDescriptor.request.getParameter("ResponseCode") == null ){
				Html.addMessage(MessageType.WARNING, "The response code is invalid (must be greater than 0)", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_ADD_RESPONSE_CODE_INVALID );
			}
			
			try{
				responseCode = Integer.parseInt( requestDescriptor.request.getParameter("ResponseCode") );
			} catch ( NumberFormatException e ){
				Html.addMessage(MessageType.WARNING, "The response code is invalid (must be greater than 0)", requestDescriptor.userId.longValue());
				return new ActionDescriptor(OP_ADD_RESPONSE_CODE_INVALID );
			}
			
			// 1.2.6 -- Get the hash
			hash = requestDescriptor.request.getParameter("Hash");
			if( hash == null || hash.trim().length() != 40 ){
				Html.addMessage(MessageType.WARNING, "The hash value is not valid", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_ADD_HASH_INVALID );
			}
			
			try {
				long ruleId = scanData.addHttpDataHashRule( requestDescriptor.sessionIdentifier, siteGroupId, responseCode, hash, DEFAULT_HASH_ALGORITHM, followRedirects, url, scanFrequency );
				if( ruleId < 0 ){
					Html.addMessage(MessageType.WARNING, "The rule was not successfully added", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_ADD_FAILED );
				}
				else{
					Html.addMessage(MessageType.INFORMATIONAL, "Rule was added successfully", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_ADD_SUCCESS, Long.valueOf( ruleId ));
				}
				
			} catch (NoSuchAlgorithmException e) {
				Html.addMessage(MessageType.WARNING, "The hash value is not valid", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_ADD_HASH_INVALID );
			} catch (IllegalStateException e) {
				Html.addMessage(MessageType.WARNING, "The hash value is not valid", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_ADD_HASH_INVALID );
			} catch (InputValidationException e) {
				if( e.getFieldDescription().matches("ExpectedResponseCode")){
					Html.addMessage(MessageType.WARNING, e.getMessage(), requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_ADD_RESPONSE_CODE_INVALID );
				}
				else{ //if( e.getFieldDescription().matches("URLHostname"))
					Html.addMessage(MessageType.WARNING, e.getMessage(), requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_ADD_URL_INVALID );
				}
			} catch (InsufficientPermissionException e) {
				Html.addMessage(MessageType.WARNING, "You do not have permission to update rules", requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED );
			}
		}
		
		// 1.3 -- Edit the rule 
		else if( action.matches( "Edit" ) ){
			int responseCode;
			URL url;
			String hash;
			long ruleId = -1;
			boolean followRedirects = false;
			int scanFrequency = -1;
			
			if( (requestDescriptor.request.getParameter("Submit")==null || !requestDescriptor.request.getParameter("Submit").matches("Apply Changes")) && requestDescriptor.request.getParameter("AutoPopulate") == null )
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE );
			
			// 1.3.1 -- Get the site group ID
			try{
				ruleId = Long.parseLong( requestDescriptor.request.getParameter("RuleID") );
			}
			catch( NumberFormatException e ){
				Html.addMessage(MessageType.WARNING, "The rule ID is not valid", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_EDIT_RULE_ID_INVALID );
			}
			
			// 1.3.2 -- Get the URL
			try {
				url = new URL( requestDescriptor.request.getParameter("URL") );
			} catch (MalformedURLException e) {
				Html.addMessage(MessageType.WARNING, "The URL is not valid", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_EDIT_URL_INVALID );
			}
			if( url == null ){
				Html.addMessage(MessageType.WARNING, "The URL is not valid", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_EDIT_URL_INVALID );
			}
			
			// 1.3.3 -- Auto-populate if requested
			if( requestDescriptor.request.getParameter("AutoPopulate") != null ){
				
				// 1.3.3.1 -- Get the data
				ApiScannerController apiScannerController = new ApiScannerController( Application.getApplication() );
				HttpStaticScanResult scanResult;
				
				try {
					scanResult = apiScannerController.scanHttpDataHash( requestDescriptor.sessionIdentifier, url, DEFAULT_HASH_ALGORITHM );
				} catch (InsufficientPermissionException e) {
					Html.addMessage(MessageType.WARNING, "You do not have permission to perform scans", requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_EDIT_AUTOPOP_FAILED );
				} catch (InputValidationException e) {
					Html.addMessage(MessageType.WARNING, e.getMessage(), requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_EDIT_URL_INVALID_AUTOPOP );
				} catch (ScanException e) {
					Html.addMessage(MessageType.WARNING, "The scan failed due to an error", requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_ADD_AUTOPOP_FAILED );
				}
				
				
				// 1.3.3.2 -- Return the result
				Html.addMessage(MessageType.INFORMATIONAL, "Fields successfully auto-populated", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_EDIT_AUTOPOP_SUCCESS, scanResult );
			}
			
			// 1.3.4 -- Get the response code
			if( requestDescriptor.request.getParameter("ResponseCode") == null ){
				Html.addMessage(MessageType.WARNING, "The response code is invalid (must be greater than 0)", requestDescriptor.userId.longValue());
				return new ActionDescriptor(OP_EDIT_RESPONSE_CODE_INVALID );
			}
			
			try{
				responseCode = Integer.parseInt( requestDescriptor.request.getParameter("ResponseCode") );
			} catch ( NumberFormatException e ){
				Html.addMessage(MessageType.WARNING, "The response code is invalid (must be greater than 0)", requestDescriptor.userId.longValue());
				return new ActionDescriptor(OP_EDIT_RESPONSE_CODE_INVALID );
			}
			
			// 1.3.5 -- Get the hash
			hash = requestDescriptor.request.getParameter("Hash");
			if( hash == null || hash.trim().length() != 40 ){
				Html.addMessage(MessageType.WARNING, "The hash value is not valid", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_EDIT_HASH_INVALID );
			}
			
			// 1.3.6 -- Get the scan frequency
			int scanFrequencyUnits =  Integer.parseInt( requestDescriptor.request.getParameter("ScanFrequencyUnits") );
			int scanFrequencyValue =  Integer.parseInt( requestDescriptor.request.getParameter("ScanFrequencyValue") );
			scanFrequency = scanFrequencyUnits * scanFrequencyValue;
			
			if( scanFrequency < 30 ){
				Html.addMessage(MessageType.WARNING, "The scan frequency must be 30 seconds or greater", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_EDIT_SCAN_FREQUENCY_INVALID );
			}
			
			
			try {
				scanData.updateHttpDataHashRule( requestDescriptor.sessionIdentifier, ruleId, responseCode, hash, DEFAULT_HASH_ALGORITHM, followRedirects, url, scanFrequency );
				Html.addMessage(MessageType.INFORMATIONAL, "Rule was updated successfully", requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS, Long.valueOf(ruleId));
			} catch (NoSuchAlgorithmException e) {
				Html.addMessage(MessageType.WARNING, "The hash value is not valid", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_EDIT_HASH_INVALID );
			} catch (IllegalStateException e) {
				Html.addMessage(MessageType.WARNING, "The hash value is not valid", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_EDIT_HASH_INVALID );
			} catch (ScanRuleLoadFailureException e) {
				Html.addMessage(MessageType.WARNING, "A scan rule could not found with the given identifer", requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED );
			} catch (InsufficientPermissionException e) {
				Html.addMessage(MessageType.WARNING, "You do not have permission to update rules", requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED );
			}
		}
		
		// 1.4 -- Edit the header rule or create a new one
		else if( action.matches( "EditHeaderRule" ) || action.matches( "NewHeaderRule" )){
			return new ActionDescriptor( OP_HEADER_OP );
		}
		
		// 1.5 -- Delete the header rule
		else if( action.matches( "DeleteHeaderRule" ) ){
			long headerRuleId = -1;
			try{
				headerRuleId = Long.parseLong( requestDescriptor.request.getParameter("HeaderRuleID") );
			}
			catch( NumberFormatException e ){
				Html.addMessage(MessageType.WARNING, "A valid header rule identifier was not provided", requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_DELETE_FAILED );
			}
			
			try{
				if( scanData.deleteHttpHeaderRule( requestDescriptor.sessionIdentifier, headerRuleId ) ){
					Html.addMessage(MessageType.WARNING, "The header rule was successfully deleted", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_DELETE_SUCCESS );
				}
				else{
					Html.addMessage(MessageType.WARNING, "The header rule was not successfully deleted", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_DELETE_FAILED );
				}
			} catch (NotFoundException e){
				Html.addMessage(MessageType.WARNING, "The header rule was not successfully deleted", requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_DELETE_FAILED );
			} catch (ScanRuleLoadFailureException e){
				Html.addMessage(MessageType.WARNING, "The header rule was not successfully deleted", requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_DELETE_FAILED );
			} catch (InsufficientPermissionException e) {
				Html.addMessage(MessageType.WARNING, "You do not have permission to delete header rules", requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED );
			}
		}
		// 1.6 -- Delete a rule
		else if( action.matches( "Delete" ) && requestDescriptor.request.getParameter("RuleID") != null){
			long ruleId;
			
			try{
				ruleId = Long.parseLong( requestDescriptor.request.getParameter("RuleID") );
			}
			catch(NumberFormatException e){
				throw new InvalidHtmlParameterException("Invalid Parameter","RuleID to delete is invalid", "Console");
			}
			
			long siteGroupId;
			try{
				siteGroupId = scanData.getAssociatedSiteGroup(requestDescriptor.sessionIdentifier, ruleId);
			}
			catch( InsufficientPermissionException e){
				siteGroupId = -1;
			}
			
			try{
				scanData.deleteRule(requestDescriptor.sessionIdentifier, ruleId);
			} catch (InsufficientPermissionException e) {
				Html.addMessage(MessageType.WARNING, "You do not have permission to delete rules", requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_DELETE_FAILED );
			}
			
			Html.addMessage(MessageType.INFORMATIONAL, "Rule successfully deleted", requestDescriptor.userId.longValue());
			return new ActionDescriptor( OP_DELETE_RULE_SUCCESS, new Long(siteGroupId));
		}
		
		// 1.7 -- Scan the rule
		else if( action.matches( "Scan" ) && requestDescriptor.request.getParameter("RuleID") != null){
			ApiScannerController scannerController = new ApiScannerController( Application.getApplication() );
			long ruleId = Long.parseLong( requestDescriptor.request.getParameter("RuleID") );
			ScanResult scanResult;
			try{
				scanResult = scannerController.scanRule(requestDescriptor.sessionIdentifier, ruleId, false );
			}
			catch( InsufficientPermissionException exception){
				Html.addMessage(MessageType.WARNING, "You do not have permission to scan the rule", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_SCAN_FAILED, new Long(ruleId));
			}
			
			if( scanResult.getResultCode() == ScanResultCode.SCAN_COMPLETED && scanResult.getDeviations() == 1){
				Html.addMessage(MessageType.INFORMATIONAL, "Rule scan completed successfully, 1 deviation observed", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_SCAN_SUCCESS, scanResult);
			}
			if( scanResult.getResultCode() == ScanResultCode.SCAN_COMPLETED && scanResult.getDeviations() == 0){
				Html.addMessage(MessageType.INFORMATIONAL, "Rule scan completed successfully, 0 deviations observed", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_SCAN_SUCCESS, scanResult);
			}
			else if( scanResult.getResultCode() == ScanResultCode.SCAN_COMPLETED ){
				Html.addMessage(MessageType.INFORMATIONAL, "Rule scan completed successfully, " + scanResult.getDeviations() + " deviations observed", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_SCAN_SUCCESS, scanResult);
			}
			else{ //if( scanResult.getResultCode() == ScanResultCode.CONNECTION_FAILED )
				Html.addMessage(MessageType.WARNING, "Rule scan could not complete", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_SCAN_FAILED, scanResult);
			}
		}
		else{
			return new ActionDescriptor( ActionDescriptor.OP_VIEW );
		}
		
	}
	
}
