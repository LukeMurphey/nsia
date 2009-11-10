package net.lukeMurphey.nsia.htmlInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.InputValidationException;
import net.lukeMurphey.nsia.InsufficientPermissionException;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.NotFoundException;
import net.lukeMurphey.nsia.Wildcard;
import net.lukeMurphey.nsia.scanRules.HttpSeekingScanResult;
import net.lukeMurphey.nsia.scanRules.HttpSeekingScanRule;
import net.lukeMurphey.nsia.scanRules.ScanResult;
import net.lukeMurphey.nsia.scanRules.ScanResultCode;
import net.lukeMurphey.nsia.scanRules.ScanRule.ScanRuleLoadFailureException;
import net.lukeMurphey.nsia.trustBoundary.ApiScanData;
import net.lukeMurphey.nsia.trustBoundary.ApiScannerController;
import net.lukeMurphey.nsia.htmlInterface.Html.MessageType;

import org.apache.commons.lang.StringEscapeUtils;

public class HtmlSeekingRule extends HtmlContentProvider{
	
	private static int OP_ADD_DOMAIN_LIMIT_INVALID  = 101;
	private static int OP_ADD_RECURSION_DEPTH_INVALID = 102;
	private static int OP_ADD_SCAN_LIMIT_INVALID = 103;
	private static int OP_ADD_START_ADDRESS_INVALID = 104;
	
	private static int OP_EDIT_DOMAIN_LIMIT_INVALID  = 105;
	private static int OP_EDIT_RECURSION_DEPTH_INVALID = 106;
	private static int OP_EDIT_SCAN_LIMIT_INVALID = 107;
	private static int OP_EDIT_START_ADDRESS_INVALID = 108;
	private static int OP_EDIT_SCANFREQUENCY_INVALID = 109;
	private static int OP_EDIT_RULEID_INVALID = 110;
	private static int OP_EDIT_SITEGROUP_INVALID = 111;
	
	private static int OP_DELETE_RULE_SUCCESS = 112;
	private static int OP_ADD_SCANFREQUENCY_INVALID = 115;
	private static int OP_SCAN_SUCCESS = 116;
	private static int OP_SCAN_FAILED = 117;
	
	//private static int OP_ADD_SITEGROUP_INVALID = 118;


	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, NotFoundException, InputValidationException, InvalidHtmlOperationException{
		return getHtml( requestDescriptor, null );
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, NotFoundException, InputValidationException, InvalidHtmlOperationException{
		
		ApiScanData scanData = new ApiScanData(Application.getApplication());
		
		// 0 -- Perform any pending actions
		if( actionDesc == null ){
			actionDesc = performAction( requestDescriptor, scanData);
		}
		
		
		// 1 -- Output the main content

		// 1.1 -- Create new rule
		if( actionDesc.result == ActionDescriptor.OP_ADD_SUCCESS || actionDesc.result == ActionDescriptor.OP_UPDATE_SUCCESS ){
			
			// 1.1.1 -- If the site group ID was specified, then have the SiteGroup HTML page render the page 
			requestDescriptor.response.setStatus(301);
			
			if( requestDescriptor.request.getParameter("SiteGroupID") != null ){
				return HtmlSiteGroup.getHtml(requestDescriptor, actionDesc);
			}
			long siteGroupID = -1;
			long ruleId = -1;
			
			// 1.1.2 -- Get the site group ID from the rule ID
			if( requestDescriptor.request.getParameter("RuleID") != null ){
				try{
					ruleId = Integer.parseInt( requestDescriptor.request.getParameter("RuleID") );
				}
				catch(NumberFormatException e){
					requestDescriptor.response.setHeader("Location", requestDescriptor.getLocation());
					return new ContentDescriptor("Redirecting","Redirecting to <a href=\"\">here</a>");
				}
			}
			
			try{
				siteGroupID = scanData.getAssociatedSiteGroup(requestDescriptor.sessionIdentifier, ruleId);
			
				requestDescriptor.response.setHeader("Location", requestDescriptor.getLocation() + "/SiteGroup?SiteGroupID=" + siteGroupID );
			}
			catch(InsufficientPermissionException e){
				requestDescriptor.response.setHeader("Location", requestDescriptor.getLocation());
			}
		}
		
		if( actionDesc.result == ActionDescriptor.OP_ADD || actionDesc.result == OP_ADD_DOMAIN_LIMIT_INVALID || actionDesc.result == OP_ADD_RECURSION_DEPTH_INVALID
				|| actionDesc.result == OP_ADD_SCAN_LIMIT_INVALID || actionDesc.result == OP_ADD_SCANFREQUENCY_INVALID || actionDesc.result == OP_ADD_START_ADDRESS_INVALID ){
			return getRuleEdit( requestDescriptor, actionDesc, scanData);
		}
		
		// 1.2 -- View the results of the rule scan
		else if( actionDesc.result == OP_SCAN_SUCCESS){
			HttpSeekingScanResult scanResult = (HttpSeekingScanResult)actionDesc.addData;
			
			return HtmlScanResult.getScanResultView( requestDescriptor, null, scanResult);
		}
		
		// 1.3 -- Edit rule
		else {//if( actionDesc.result == ActionDescriptor.OP_UPDATE || actionDesc.result == OP_EDIT_DOMAIN_INVALID || actionDesc.result == OP_EDIT_DEPTH_LIMIT_INVALID
				//|| actionDesc.result == OP_EDIT_RESOURCE_LIMIT_INVALID || actionDesc.result == OP_EDIT_SCAN_FREQUENCY_INVALID || actionDesc.result == OP_EDIT_SEED_URLS_INVALID )
			return getRuleEdit( requestDescriptor, actionDesc, scanData);
		}
		
		// 1.4 -- View the rule (default action)
		//else
			//return getRuleView( requestDescriptor, actionDesc, scanData);
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
	 * @throws InvalidHtmlOperationException 
	 * @throws NotFoundException 
	 * @throws Exception
	 */
	private static ContentDescriptor getRuleEdit(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc, ApiScanData scanData) throws NoSessionException, GeneralizedException, InvalidHtmlOperationException{
		String title = "Edit Rule";
		StringBuffer body = new StringBuffer();
		
		// 1 -- Output any messages
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		// 2 -- Output the main content
		String startAddresses = "";
		int recursionDepth = 10;
		int scanLimit = 100;
		String domainLimit = "*";
		int scanFrequencyUnits = 0;
		int scanFrequencyValue = 0;
		long siteGroupId = -1;
		
		//	 2.1  -- Get the relevant fields
		long scanRuleId = -1;
		
		//		2.1.1 -- Get the rule ID
		if( requestDescriptor.request.getParameter("RuleID") != null ){
			try{
				scanRuleId = Long.parseLong( requestDescriptor.request.getParameter("RuleID") );
			}
			catch( NumberFormatException e ){
				return new ContentDescriptor( "HTTP Auto-Discovery Rule", Html.getWarningNote("A valid rule was not provided") );
			}
		}

		HttpSeekingScanRule scanRule = null;
		
		//		2.1.2 -- Get the actual rule
		try{
			//ApiScanData scanData= new ApiScanData( Application.getApplication() );
			
			if( scanRuleId > -1 ){
				scanRule = (HttpSeekingScanRule)scanData.getScanRule( requestDescriptor.sessionIdentifier, scanRuleId );
			}
			
			if( requestDescriptor.request.getParameter("ScanFrequencyUnits") != null && requestDescriptor.request.getParameter("ScanFrequencyValue") != null ){
				try{
					scanFrequencyUnits = Integer.parseInt(requestDescriptor.request.getParameter("ScanFrequencyUnits"));
					scanFrequencyValue = Integer.parseInt(requestDescriptor.request.getParameter("ScanFrequencyValue"));
				}
				catch(NumberFormatException e){
					scanFrequencyUnits = 0;
					scanFrequencyValue = 0;
				}
			}
			else if( (scanFrequencyUnits == 0 || scanFrequencyValue == 0) && scanRule != null){
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
				scanFrequencyUnits = 3600;
				scanFrequencyValue = 1;
			}

			//	 2.1.3 -- Get the various other parameters

			//		2.1.3.1 -- Get the start addresses
			if( requestDescriptor.request.getParameter("StartAddresses2") != null ){
				startAddresses = requestDescriptor.request.getParameter("StartAddresses2");
			}
			else if( requestDescriptor.request.getParameter("StartAddresses") != null ){
				startAddresses = requestDescriptor.request.getParameter("StartAddresses");
			}
			else if(scanRule != null){
				URL[] urls = scanRule.getSeedUrls();
				for(int c = 0; c < urls.length; c++){
					startAddresses += urls[c].toString() + "\n";
				}
			}

			//		2.1.3.2 -- Get the recursion depth
			if( requestDescriptor.request.getParameter("RecursionDepth") != null ){
				try{
					recursionDepth = Integer.parseInt( requestDescriptor.request.getParameter("RecursionDepth") );
				}
				catch( NumberFormatException e ){
					recursionDepth = 1;
				}
			}
			else if(scanRule != null){
				recursionDepth = scanRule.getRecursionDepth();
			}

			//		2.1.3.3 -- Get the limit on the number of resources to scan
			if( requestDescriptor.request.getParameter("ScanLimit") != null ){
				try{
					scanLimit = Integer.parseInt( requestDescriptor.request.getParameter("ScanLimit") );
				}
				catch( NumberFormatException e ){
					scanLimit = 10;
				}
			}
			else if(scanRule != null){
				scanLimit = scanRule.getScanCountLimit();
			}

			//		2.1.3.4 -- Get the domain limit
			if( requestDescriptor.request.getParameter("DomainLimit") != null ){
				domainLimit = requestDescriptor.request.getParameter("DomainLimit");
			}
			else if(scanRule != null){
				domainLimit = scanRule.getDomainRestriction().wildcard();
			}

			//		2.1.4 -- Get the site group ID
			if( requestDescriptor.request.getParameter("SiteGroupID") != null ){
				try{
					siteGroupId = Long.parseLong( requestDescriptor.request.getParameter("SiteGroupID") );
				}
				catch( NumberFormatException e ){
					return new ContentDescriptor( "HTTP Auto-Discovery Rule", Html.getWarningNote("A valid rule was not provided") );
				}
			}
			
			if( siteGroupId <= -1 && scanRuleId > -1 ){
				try{
					siteGroupId = scanData.getAssociatedSiteGroup( requestDescriptor.sessionIdentifier, scanRuleId );
				}catch(InsufficientPermissionException e){
					//Ignore this exception, it should not occur since we clearly have read permissions at this point (we already checked)
					e.printStackTrace();
				}
				catch(NotFoundException e){
					throw new InvalidHtmlOperationException("Site Group Identifier Not Found", "The Site Group associated with the rule could not be identified since the rule does not exist", "/");
				}
			}
			
			
			// 3 -- Output the main content

			//	 3.1 -- Output the section header
			//body.append( GenericHtmlGenerator.getSectionHeader( "HTTP Auto-Discovery Rule", null ) );
			body.append(getScanForm(requestDescriptor, actionDesc, startAddresses, domainLimit, recursionDepth, scanLimit, scanFrequencyUnits, scanFrequencyValue, scanRuleId, siteGroupId));
			

		}catch(InsufficientPermissionException e){
			return HtmlProblemDialog.getHtml(requestDescriptor, "Insufficient Permission", "You do not have permission to view this rule", HtmlProblemDialog.DIALOG_WARNING, "Console", "Return to Main Dashboard");
		}
		catch(NotFoundException e){
			body.append( Html.getDialog("A Rule was Not Found with the Given Identifer", "Rule Not Found", "/22_Warning" ) );
		}
		
		
		// 3 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "Site Group", "/SiteGroup?SiteGroupID=" +siteGroupId );
		
		
		if( scanRuleId > -1 ){
			navPath.addPathEntry( "Edit Rule","/ScanRule?Action=New&RuleID=" + scanRuleId );
		}
		else{
			navPath.addPathEntry( "New Rule","/ScanRule?Action=Edit&RuleID=" + scanRuleId );
		}
		
		String navigationHtml = Html.getNavigationPath( navPath );
		
		
		// 4 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
		
		menuItems.add( new MenuItem("Site Group", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Edit", "/SiteGroup?SiteGroupId=" + requestDescriptor.request.getParameter("SiteGroupID"), MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Delete", "/SiteGroup?Action=Delete&SiteGroupId=" + requestDescriptor.request.getParameter("SiteGroupID"), MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Scan Now", "/ScanRule?Action=Scan&RuleID=" + requestDescriptor.request.getParameter("RuleID"), MenuItem.LEVEL_TWO, "showHourglass('Scanning...');") );
		if( scanRuleId >= 0){
			menuItems.add( new MenuItem("View Exceptions", "/ExceptionManagement?SiteGroupID=" + siteGroupId + "&RuleID=" + scanRuleId, MenuItem.LEVEL_TWO) );
		}
		
		String menuOutput = Html.getMenu( menuItems );
		
		
		// 5 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor(title, pageOutput);
	}
	
	
	private static StringBuffer getScanForm(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc, String startAddresses, String domainLimit, int recursionDepth, int scanLimit, int scanFrequencyUnits, int scanFrequencyValue, long scanRuleId, long siteGroupId ){
	
		StringBuffer body = new StringBuffer();
	
		// 1 -- Create the header
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		body.append( Html.getSectionHeader("HTTP Content Auto-Discovery Rule", "Automatically crawls the website in order to identify malicious content" ) );
	
		
		// 2 -- Output the codepress box with the start addresses 
		body.append( "<script src=\"/codepress/codepress.js\" type=\"text/javascript\"></script>");
	
		body.append( "<script type=\"text/javascript\">");
		body.append( "function submitEditorForm(editorform){");
		body.append( "document.editorform.StartAddresses2.value = cp1.getCode();");
		body.append( "document.editorform.submit();");
		body.append( "return true;");
		body.append( "}");
		body.append( "</script>");
	
		body.append( "<form name=\"editorform\" id=\"editorform\" onSubmit=\"return submitEditorForm(this.form)\" action=\"HttpDiscoveryRule\" method=\"post\"><table class=\"DataTable\">"  );
		body.append( "<input type=\"hidden\" name=\"StartAddresses2\" value=\"" + StringEscapeUtils.escapeHtml( StringEscapeUtils.escapeHtml( startAddresses ) )+ "\">" );
	
		// 3 -- Output scan frequency
		if( actionDesc.result == OP_EDIT_SCANFREQUENCY_INVALID )
			body.append( "<tr class=\"ValidationFailed\">" );
		else
			body.append( "<tr class=\"Background1\">" );

		body.append( "<td class=\"Text_3\">Scan Frequency</td><td>");
		body.append( "<input class=\"textInput\" type=\"text\" name=\"ScanFrequencyValue\" value=\"" ).append(scanFrequencyValue);
		body.append("\">&nbsp;&nbsp;<select name=\"ScanFrequencyUnits\">");

		//		3.1 -- Days option
		body.append( "<option value=\"86400\"" );
		if( scanFrequencyUnits == 84600 )
			body.append(" selected");
		body.append(">Days</option>");

		//		3.2 -- Hours option
		body.append( "<option value=\"3600\"" );
		if( scanFrequencyUnits == 3600 )
			body.append(" selected");
		body.append(">Hours</option>");

		//		3.3 -- Minutes option
		body.append( "<option value=\"60\"" );
		if( scanFrequencyUnits == 60 )
			body.append(" selected");
		body.append(">Minutes</option>");

		//		3.4 -- Seconds option
		body.append( "<option value=\"1\"" );
		if( scanFrequencyUnits == 1 )
			body.append(" selected");
		body.append(">Seconds</option>");

		body.append("</select></td></tr>");
		
		// 4 -- Output the start addresses
		if( actionDesc.result == OP_EDIT_START_ADDRESS_INVALID)
			body.append( "<tr class=\"ValidationFailed\">" );
		else
			body.append( "<tr>" );
	
		body.append( "<td style=\"vertical-align: top;\"><div style=\"margin-top: 5px;\" class=\"TitleText\">Addresses to Scan:</div></td><td><textarea id=\"cp1\" class=\"codepress urls autocomplete-off\" wrap=\"virtual\" rows=\"11\" cols=\"48\" name=\"StartAddresses\">").append( StringEscapeUtils.escapeHtml( startAddresses ) ).append( "</textarea></td></tr>" );
	
		// 5 -- Output the domain limiter
		if( actionDesc.result == OP_EDIT_DOMAIN_LIMIT_INVALID  )
			body.append( "<tr class=\"ValidationFailed\">" );
		else
			body.append( "<tr>" );
	
		body.append( "<td class=\"TitleText\">Domain</td><td><input class=\"textInput\" size=\"40\" type=\"text\" name=\"Domain\" value=\"").append( StringEscapeUtils.escapeHtml( domainLimit ) ).append("\"></td></tr>");
	
		// 6 -- Output the recursion depth
		if( actionDesc.result == OP_EDIT_RECURSION_DEPTH_INVALID  )
			body.append( "<tr class=\"ValidationFailed\">" );
		else
			body.append( "<tr class=\"Background1\">" );
	
		body.append( "<td class=\"TitleText\">Levels to Recurse</td><td><input class=\"textInput\" size=\"40\" type=\"text\" name=\"RecursionDepth\" value=\"").append( recursionDepth ).append("\"></td></tr>");
	
		// 7 -- Output the scan limit
		if( actionDesc.result == OP_ADD_SCAN_LIMIT_INVALID  )
			body.append( "<tr class=\"ValidationFailed\">" );
		else
			body.append( "<tr class=\"Background1\">" );
	
		body.append( "<td class=\"TitleText\">Maximum Number of Resource to Scan</td><td><input class=\"textInput\" size=\"40\" type=\"text\" name=\"ScanLimit\" value=\"").append(  scanLimit ).append("\"></td></tr>");
	
	
		body.append( "<tr class=\"lastRow\"><td class=\"alignRight\" colspan=\"99\">");
		
		if( scanRuleId <= -1 ){
			body.append( "<input class=\"button\" type=\"submit\" value=\"Add\" name=\"Action\">&nbsp;&nbsp;");
			body.append( "<input type=\"hidden\" name=\"SiteGroupID\" value=\"" + siteGroupId + "\">");
		}
		else{
			body.append( "<input class=\"button\" type=\"submit\" value=\"Edit\" name=\"Action\">&nbsp;&nbsp;");
			body.append( "<input type=\"hidden\" name=\"RuleID\" value=\"" + scanRuleId + "\">");
		}
		
		//body.append( "<input class=\"button\" type=\"submit\" onClick=\"showHourglass('Initiating Scan...')\" value=\"Baseline\" name=\"Action\">&nbsp;&nbsp;")
		body.append( "<input class=\"button\" type=\"submit\" value=\"Cancel\" name=\"Action\"></td></tr>"  );
	
		body.append( "</table>" );
	
		return body;
	}
	
	private static URL[] parseURLs(String addresses) throws IOException, InputValidationException{
		Vector<URL> urls = new Vector<URL>();
	
		BufferedReader reader = new BufferedReader( new StringReader(addresses));
		String line;
		while ( (line = reader.readLine()) != null ) {
	
			if( !line.isEmpty() ){
				try{
					urls.add( new URL(line) );
				}
				catch(MalformedURLException e){
					throw new InputValidationException("The URL \"" + line + "\" is invalid", "URL", line);
				}
			}
		}
	
		URL[] urlsArray = new URL[urls.size()];
		urls.toArray(urlsArray);
		return urlsArray;
	}
	
	private static ActionDescriptor performAction(WebConsoleConnectionDescriptor requestDescriptor, ApiScanData scanData) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, NotFoundException{
		
		String action = requestDescriptor.request.getParameter("Action");
		//String submit = requestDescriptor.request.getParameter("Scan");
		
		
		// 1 -- Load the relevant page
		
		//	 1.1 -- View the rule
		if( action == null )
			return new ActionDescriptor( ActionDescriptor.OP_VIEW);
		
		//	 1.2 -- Create a new rule or edit an existing one
		else if( action.matches( "Add" ) || action.matches( "Edit" )  ){
			
			//boolean followRedirects = false;
			int scanFrequency = -1;
			
			// 1.2.1 -- Get the list of start addresses 
			String startAddresses = requestDescriptor.request.getParameter("StartAddresses");
			
			if( startAddresses == null || startAddresses.length() == 0 ){
				startAddresses = requestDescriptor.request.getParameter("StartAddresses2");
			}
			
			if( startAddresses == null ){
				Html.addMessage(MessageType.WARNING, "The list of addresses to scan was not provided", requestDescriptor.userId.longValue());
				return new ActionDescriptor(OP_EDIT_START_ADDRESS_INVALID);
			}
			
			URL[] urls;
			
			try{
				urls = parseURLs(startAddresses);
				
				if( urls.length == 0 ){
					Html.addMessage(MessageType.WARNING, "The list of addresses contains no URLs", requestDescriptor.userId.longValue());
					return new ActionDescriptor(OP_EDIT_START_ADDRESS_INVALID);
				}
			}
			catch(InputValidationException e){
				Html.addMessage(MessageType.WARNING, e.getMessage(), requestDescriptor.userId.longValue());
				return new ActionDescriptor(OP_EDIT_START_ADDRESS_INVALID);
			}
			catch(IOException e){
				Html.addMessage(MessageType.WARNING, "The list of addresses to scan is invalid", requestDescriptor.userId.longValue());
				return new ActionDescriptor(OP_EDIT_START_ADDRESS_INVALID);
			}

			
			// 1.2.2 -- Get the recursion depth
			int recursionDepth = 10;
			
			if( requestDescriptor.request.getParameter("RecursionDepth") != null ){
				try{
					recursionDepth = Integer.parseInt( requestDescriptor.request.getParameter("RecursionDepth") );
				}
				catch(NumberFormatException e){
					Html.addMessage(MessageType.WARNING, "The recursion depth is not a valid number", requestDescriptor.userId.longValue());
					return new ActionDescriptor(OP_EDIT_RECURSION_DEPTH_INVALID);
				}
			}
			
			// 1.2.3 -- Get the scan limit
			int scanLimit = 100;
			
			if( requestDescriptor.request.getParameter("ScanLimit") != null ){
				try{
					scanLimit = Integer.parseInt( requestDescriptor.request.getParameter("ScanLimit") );
				}
				catch(NumberFormatException e){
					Html.addMessage(MessageType.WARNING, "The limit on the number of resources to scan is not a valid number", requestDescriptor.userId.longValue());
					return new ActionDescriptor(OP_EDIT_SCAN_LIMIT_INVALID);
				}
			}
			
			// 1.2.4 -- Get the limit on the domain to scan
			String domainLimit = requestDescriptor.request.getParameter("Domain");
			
			if( domainLimit == null ){
				Html.addMessage(MessageType.WARNING, "The domain is invalid", requestDescriptor.userId.longValue());
				return new ActionDescriptor(OP_EDIT_DOMAIN_LIMIT_INVALID);
			}
			
			// 1.2.5 -- Get the scan frequency
			int scanFrequencyValue;
			int scanFrequencyUnits;
			
			try{
				scanFrequencyValue = Integer.parseInt( requestDescriptor.request.getParameter("ScanFrequencyValue"));
				scanFrequencyUnits  = Integer.parseInt( requestDescriptor.request.getParameter("ScanFrequencyUnits"));
			}
			catch(NumberFormatException e){
				Html.addMessage(MessageType.WARNING, "The scan frequency is invalid", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_EDIT_SCANFREQUENCY_INVALID);
			}
			
			scanFrequency = scanFrequencyValue * scanFrequencyUnits;
			
			if( scanFrequency < 30 ){
				Html.addMessage(MessageType.WARNING, "The scan frequency must be 30 seconds or greater", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_EDIT_SCANFREQUENCY_INVALID);
			}
			
			if( action.matches( "Add" ) ){
				
				// 1.2.6a -- Get the site group identifier
				
				int siteGroupId = -1;
				
				if( requestDescriptor.request.getParameter("SiteGroupID") != null ){
					try{
						siteGroupId = Integer.parseInt( requestDescriptor.request.getParameter("SiteGroupID") );
					}
					catch(NumberFormatException e){
						Html.addMessage(MessageType.WARNING, "The site group identifier is not a valid number", requestDescriptor.userId.longValue());
						return new ActionDescriptor(OP_EDIT_SITEGROUP_INVALID);
					}
				}
				
				try{
					if( scanData.addHttpDiscoveryRule( requestDescriptor.sessionIdentifier, siteGroupId, new Wildcard( domainLimit ), recursionDepth, scanFrequency, urls, scanLimit) >= 0 ){
						Html.addMessage(MessageType.INFORMATIONAL, "Rule successfully added", requestDescriptor.userId.longValue());
						return new ActionDescriptor(ActionDescriptor.OP_ADD_SUCCESS, Integer.valueOf( siteGroupId ));
					}
				} catch(InsufficientPermissionException e){
					Html.addMessage(MessageType.WARNING, "You do not have permission to create rules", requestDescriptor.userId.longValue());
					return new ActionDescriptor(ActionDescriptor.OP_ADD_FAILED, Integer.valueOf(siteGroupId));
				}
				
			}
			else if( action.matches( "Edit" ) ){
				
				// 1.2.6b -- Get the rule identifier
				
				long ruleId = -1;
				
				if( requestDescriptor.request.getParameter("RuleID") != null ){
					try{
						ruleId = Integer.parseInt( requestDescriptor.request.getParameter("RuleID") );
					}
					catch(NumberFormatException e){
						Html.addMessage(MessageType.WARNING, "The rule identifier is not a valid number", requestDescriptor.userId.longValue());
						return new ActionDescriptor(OP_EDIT_SITEGROUP_INVALID );
					}
				}
				
				try{
					scanData.updateHttpDiscoveryRule(requestDescriptor.sessionIdentifier, ruleId, new Wildcard( domainLimit ), recursionDepth, scanFrequency, urls, scanLimit);
					Html.addMessage(MessageType.INFORMATIONAL, "Rule successfully updated", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS);
				}
				catch(InsufficientPermissionException e){
					Html.addMessage(MessageType.WARNING, "You do not have permission to update this rule", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED);
				}
				catch(ScanRuleLoadFailureException e){
					Html.addMessage(MessageType.WARNING, "Rule identifier invalid", requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_EDIT_RULEID_INVALID);
				}
			}
			
			return new ActionDescriptor( ActionDescriptor.OP_ADD );
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
			
			int siteGroupId;
			try{
				siteGroupId = scanData.getAssociatedSiteGroup(requestDescriptor.sessionIdentifier, ruleId);
			}
			catch( InsufficientPermissionException e){
				siteGroupId = -1;
			}
			
			try{
				scanData.deleteRule(requestDescriptor.sessionIdentifier, ruleId);
			}catch(InsufficientPermissionException e){
				Html.addMessage(MessageType.WARNING, "You do not have permission to delete rules", requestDescriptor.userId.longValue());
				return new ActionDescriptor(ActionDescriptor.OP_DELETE_FAILED, Integer.valueOf(siteGroupId));
			}
			
			Html.addMessage(MessageType.INFORMATIONAL, "Rule successfully deleted", requestDescriptor.userId.longValue());
			return new ActionDescriptor( OP_DELETE_RULE_SUCCESS, new Integer(siteGroupId));
		}
		
		// 1.7 -- Scan the rule
		else if( action.matches( "Scan" ) && requestDescriptor.request.getParameter("RuleID") != null){
			ApiScannerController scannerController = new ApiScannerController( Application.getApplication() );
			long ruleId = Long.parseLong( requestDescriptor.request.getParameter("RuleID") );
			ScanResult scanResult;
			try{
				scanResult = scannerController.scanRule(requestDescriptor.sessionIdentifier, ruleId, true );
			}
			catch( InsufficientPermissionException exception){
				Html.addMessage(MessageType.WARNING, "You do not have permission to scan the rule", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_SCAN_FAILED, Long.valueOf(ruleId));
			}
			
			if( scanResult.getResultCode() == ScanResultCode.SCAN_COMPLETED && scanResult.getDeviations() == 1){
				Html.addMessage(MessageType.INFORMATIONAL, "Rule scan completed successfully, 1 deviation observed", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_SCAN_SUCCESS, scanResult);
			}
			else if( scanResult.getResultCode() == ScanResultCode.SCAN_COMPLETED && scanResult.getDeviations() > 1){
				Html.addMessage(MessageType.INFORMATIONAL, "Rule scan completed successfully, " + scanResult.getDeviations() + " deviations observed", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_SCAN_SUCCESS, scanResult);
			}
			else if( scanResult.getResultCode() == ScanResultCode.SCAN_COMPLETED && scanResult.getDeviations() == 0){
				Html.addMessage(MessageType.INFORMATIONAL, "Rule scan completed successfully, 0 deviations observed", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_SCAN_SUCCESS, scanResult);
			}
			else if( scanResult.getResultCode() == ScanResultCode.SCAN_COMPLETED ){
				Html.addMessage(MessageType.INFORMATIONAL, "Rule scan completed successfully, " + scanResult.getDeviations() + " deviations observed", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_SCAN_SUCCESS, scanResult);
			}
			else if( scanResult.getResultCode() == ScanResultCode.SCAN_FAILED ){
				Html.addMessage(MessageType.INFORMATIONAL, "Rule completed with exceptions", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_SCAN_FAILED, scanResult);
			}
			else{
				Html.addMessage(MessageType.INFORMATIONAL, "Rule scan did not successfully complete", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_SCAN_FAILED, scanResult);
			}
		}
		else
			return new ActionDescriptor( ActionDescriptor.OP_VIEW);
		
	}
	
}
