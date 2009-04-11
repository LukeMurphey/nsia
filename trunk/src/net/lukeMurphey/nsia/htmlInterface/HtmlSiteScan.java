package net.lukeMurphey.nsia.htmlInterface;

import java.util.Hashtable;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.io.IOException;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.UnsupportedEncodingException;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.DuplicateEntryException;
import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.InputValidationException;
import net.lukeMurphey.nsia.InsufficientPermissionException;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.RuleScanWorker;
import net.lukeMurphey.nsia.scanRules.HttpSeekingScanRule;
import net.lukeMurphey.nsia.scanRules.HttpSeekingScanResult;
import net.lukeMurphey.nsia.scanRules.HttpDefinitionScanResult;
import net.lukeMurphey.nsia.scanRules.ScanResultCode;
import net.lukeMurphey.nsia.scanRules.ScanResultLoader;
import net.lukeMurphey.nsia.scanRules.DefinitionMatch;
import net.lukeMurphey.nsia.scanRules.Definition.Severity;
import net.lukeMurphey.nsia.scanRules.ScanRule.ScanResultLoadFailureException;
import net.lukeMurphey.nsia.trustBoundary.ApiScannerController;
import net.lukeMurphey.nsia.trustBoundary.ApiTasks;
import net.lukeMurphey.nsia.Wildcard;
import net.lukeMurphey.nsia.WorkerThread;
import net.lukeMurphey.nsia.Application.WorkerThreadDescriptor;
import net.lukeMurphey.nsia.htmlInterface.Html.MessageType;


public class HtmlSiteScan extends HtmlContentProvider {

	private static final int OP_EDIT_START_ADDRESS_INVALID = 101;
	private static final int OP_EDIT_DOMAIN_LIMIT_INVALID = 102;
	private static final int OP_EDIT_RECURSION_DEPTH_INVALID = 103;
	private static final int OP_EDIT_SCAN_LIMIT_INVALID = 104;
	private static final int OP_SCAN_STARTED = 105;
	private static final int OP_SCAN_START_FAILED = 106;
	private static final int OP_SCAN_TERMINATED = 107;
	private static final int OP_SCAN_AGAIN = 108;
	
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException{
		return getHtml( requestDescriptor, null );
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException{
	
		StringBuffer body = new StringBuffer();
		String title = "Website Compliance Scan";
		
		// 1 -- Get the parameters
		String startAddresses = "";
		int recursionDepth = 10;
		int scanLimit = 100;
		String domainLimit = "*";
		
		
		// 2 -- Determine which form to display
		try{
			
			// 2.1 -- Perform the action if not done yet
			if( actionDesc == null ){
				actionDesc = performAction(requestDescriptor);
			}
			
			// 2.2 -- Display any messages after performing the action
			body.append(Html.renderMessages(requestDescriptor.userId));
			
			// 2.3 -- Determine if a scan thread is already running
			ApiTasks tasks = new ApiTasks(Application.getApplication());
			String uniqueDesc = "Sitescan/" + requestDescriptor.userId;
			WorkerThreadDescriptor worker = tasks.getWorkerThread(requestDescriptor.sessionIdentifier, uniqueDesc);
			RuleScanWorker thread = null;
			
			if( worker != null ){
				thread = (RuleScanWorker)worker.getWorkerThread();
			}
			
			// 2.4 -- Chose which form to use
			
			//	 2.4.1 -- User wants to create a new scan
			if(actionDesc.result == OP_SCAN_AGAIN){
				body.append( getScanForm(requestDescriptor, actionDesc, startAddresses, domainLimit, recursionDepth, scanLimit) );
			}
			
			//	 2.4.2 -- Scan is already started, display a status dialog
			else if( thread != null && (thread.getStatus() != WorkerThread.State.STOPPED || actionDesc.result == OP_SCAN_STARTED)){
				Hashtable<String, String> args = new Hashtable<String, String>();
				args.put("Action", "Cancel");
				args.put("Mode", "SiteScan");
				
				try{
					return HtmlOptionDialog.getHtml(requestDescriptor, "Site Scan in Progress", thread.getStatusDescription(), args, new String[]{"Cancel"}, "", HtmlOptionDialog.DIALOG_INFORMATION, "SiteScan", "/Ajax/Task/" + java.net.URLEncoder.encode( uniqueDesc, "US-ASCII") );
				}
				catch(UnsupportedEncodingException e){
					return HtmlOptionDialog.getHtml(requestDescriptor, "Site Scan in Progress", "Scan operation is currently in progress: " + thread.getStatusDescription(), args, new String[]{"Cancel"}, "SiteScan", HtmlOptionDialog.DIALOG_INFORMATION, thread.getProgress(), requestDescriptor.getLocation() + "/SiteScan" );
				}
			}
			
			//	 2.4.3 -- Show the scan report
			else if(thread != null && (thread.getStatus() == WorkerThread.State.STOPPED) && thread.getScanResultIDs().length > 0 ){
				body.append( Html.getSectionHeader("Website Scan Report", null ) );
				//body.append( getScanReport(requestDescriptor, actionDesc, thread.getResult()) );
				long[] results = thread.getScanResultIDs();
				body.append( HtmlSeekingScanResult.getScanResultReport( (HttpSeekingScanResult)ScanResultLoader.getScanResult( results[0] ), requestDescriptor ) );
			}
			
			//	 2.4.4 -- Default option: display the form to begin a scan
			else{
				body.append( getScanForm(requestDescriptor, actionDesc, startAddresses, domainLimit, recursionDepth, scanLimit) );
			}
		}
		catch(InsufficientPermissionException e){
			body.append(Html.getWarningDialog("Insufficient Permission", "You do have permissions to perform this operation"));
		} catch (ScanResultLoadFailureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		// 3 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "Scan Website", "/SiteScan" );
		String navigationHtml = Html.getNavigationPath( navPath );
		
		
		// 4 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
		
		menuItems.add( new MenuItem("Website Scan", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Begin a Site Scan", "/SiteScan?Action=New", MenuItem.LEVEL_TWO) );
		
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
	
	public static StringBuffer getScanReport( WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc, HttpSeekingScanResult scanResult ){
		return getScanReport(requestDescriptor, actionDesc, scanResult, requestDescriptor.request.getParameter("RuleFilter"));
	}
	
	public static StringBuffer getScanReport( WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc, HttpSeekingScanResult scanResult, String scanRuleFilter ){
		
		StringBuffer body = new StringBuffer();
		
		body.append("<script>\nfunction toggle(obj) {")
		.append("	var el = document.getElementById(obj);")
		.append("	var e3 = document.getElementById(obj+'off');")
		.append("	var e2 = document.getElementById(obj+'on');")
		.append("	if ( el.style.display != 'none' ) {")
		.append("		el.style.display = 'none';")
		.append("		e2.style.display = 'none';")
		.append("		e3.style.display = '';")
		.append("	}")
		.append("	else {")
		.append("		el.style.display = '';")
		.append("		e2.style.display = '';")
		.append("		e3.style.display = 'none';")
		.append("	}")
		.append("}\n</script>");
		
		// Print out the result table
		body.append( "<table cellpadding=\"2\">" );
		
		body.append( "<tr class=\"Background0\"><td colspan=\"2\" class=\"Text_3\">Scan Parameters</td></tr>" );
		body.append( createSummaryRow( "Deviations", Integer.toString( scanResult.getDeviations() ) ) );
		body.append( createSummaryRow( "Resources Scanned", Integer.toString( scanResult.getFindings().length ) ) );
		body.append( createSummaryRow( "Domain", scanResult.getSpecimenDescription() ) );
		
		body.append( "</table>" );
		
		// Print out the list of signatures fired
		SignatureMatchCount[] signatureMatches = HtmlSiteScan.getSignatureMatchCount(scanResult.getFindings());
		
		body.append( "<p><table cellpadding=\"2\">" );
		body.append( "<tr class=\"Background0\"><td colspan=\"2\" class=\"Text_3\">Signatures Matched</td></tr>" );
		
		if( signatureMatches.length == 0){
			body.append( "<tr class=\"Background1\"><td colspan=\"99\">" + Html.getInfoNote("No signature matches observed") +"</td></tr>" );
		}
		else{
			for( int c = 0; c < signatureMatches.length; c++){
				body.append( getSignatureMatchRow( signatureMatches[c], requestDescriptor) );
			}
		}
		
		body.append( "</table>" );
		
		// Print out the resources scanned
		HttpDefinitionScanResult[] findings = scanResult.getFindings();
		
		String scanRuleFilterEscaped = null;
		
		body.append("<p>");
		
		if( scanRuleFilter != null ){
			scanRuleFilterEscaped = StringEscapeUtils.escapeHtml(scanRuleFilter);
			String scanResultId = requestDescriptor.request.getParameter("ResultID");
			
			if( scanRuleFilter != null ){
				
				StringBuffer note = new StringBuffer("<a href=\"SiteScan");
				
				if( scanResultId != null ){
					note.append( "?ResultID=" + StringEscapeUtils.escapeHtml( scanResultId ) );
				}
				
				note.append( "\">[Clear Filter]</a>");
				
				body.append(Html.getDialog("Displaying findings that matched the \"<u>" + scanRuleFilterEscaped + "</u>\" signature.<br>" + note.toString(), "Filters Applied", "/32_Information") );
				//getInfoNote("Displaying findings that matched the " + scanRuleFilterEscaped + " signature"));
			}
		}else{
			body.append("&nbsp;<p>");
		}
		
		body.append( "<table width=\"700\" cellpadding=\"2\">" );
		body.append( "<tr class=\"Background0\"><td colspan=\"99\" class=\"Text_3\">Scan Findings</td></tr>" );
		
		
		
		if( findings.length == 0){
			body.append( "<tr><td colspan=\"99\">" + Html.getDialog("No resources where scanned during the scan phase.", "No Findings", "/32_Information") +"<td></tr>" );
		}
		else{
			
			for(int c = 0; c < findings.length; c++ ){
				body.append( createFindingRow(findings[c], c, scanRuleFilterEscaped) );
			}
		}
		
		
		body.append( "</table>" );
		
		return body;
	}
	
	private static StringBuffer getSignatureMatchRow(SignatureMatchCount matchCount, WebConsoleConnectionDescriptor requestDescriptor){
		StringBuffer body = new StringBuffer();
		String scanResultId = StringEscapeUtils.escapeHtml( requestDescriptor.request.getParameter("ResultID") );
		
		body = body.append( "<tr class=\"Background1\"><td class=\"Text_3\"><a href=\"SiteScan?RuleFilter=" + matchCount.name );
		
		if( scanResultId != null ){
			body = body.append( "&ResultID=" + scanResultId );
		}
		
		body = body.append( "\">" + matchCount.name + "</a></td><td class=\"Text_3\">" + matchCount.count + "</td></tr>" );
		
		return body;
	}
	
	private static Severity maxSeverity( DefinitionMatch[] matches){
		Severity severity = Severity.LOW;
		
		for( int c = 0; c < matches.length; c++ ){
			if( matches[c].getSeverity() == Severity.HIGH){
				return Severity.HIGH;
			}
			
			else if( matches[c].getSeverity() == Severity.MEDIUM){
				severity = Severity.MEDIUM;
			}
		}
		
		return severity;
	}
	
	static class SignatureMatchCount{
		
		public SignatureMatchCount( String name ){
			count = 1;
			this.name = name;
		}
		
		public String name;
		public int count;
	}
	
	private static SignatureMatchCount[] getSignatureMatchCount( HttpDefinitionScanResult[] results ){
		Vector<SignatureMatchCount> resultVector = new Vector<SignatureMatchCount>();
		
		for( int c = 0; c < results.length; c++){
			updateSignatureMatchCount(results[c].getDefinitionMatches(), resultVector);
		}
		
		SignatureMatchCount[] result = new SignatureMatchCount[resultVector.size()];
		resultVector.toArray(result);
		
		return result;
		
	}
	
	private static void updateSignatureMatchCount( DefinitionMatch[] matches, Vector<SignatureMatchCount> resultVector ){
		
		for( int c = 0; c < matches.length; c++){
			
			boolean found = false;
			
			for( int d = 0; d < resultVector.size(); d++){
				if( resultVector.get(d).name.equalsIgnoreCase(matches[c].getDefinitionName())){
					resultVector.get(d).count++;
					found = true;
				}
			}
			
			if(found == false ){
				resultVector.add( new SignatureMatchCount(StringEscapeUtils.escapeHtml( matches[c].getDefinitionName())));
			}
		}
	}
	
	private static String createFindingRow( HttpDefinitionScanResult result, int index, String scanRuleFilter ){
		StringBuffer body = new StringBuffer();
		
		Severity severity = Severity.UNDEFINED;
		boolean signatureMatchesFilter = true;
		
		if( result.getDefinitionMatches() != null && result.getDefinitionMatches().length > 0 ){
			
			if( scanRuleFilter != null ){
				signatureMatchesFilter = false;
				for(int c = 0; c < result.getDefinitionMatches().length; c++){
					if( result.getDefinitionMatches()[c].getDefinitionName().equalsIgnoreCase(scanRuleFilter) ){
						signatureMatchesFilter = true;
					}
				}
			}
			
			if( signatureMatchesFilter == false ){
				return "";
			}
			
			severity = maxSeverity(result.getDefinitionMatches());
		}
		else if(scanRuleFilter != null){
			return "";
		}
		
		if( result.getResultCode() == ScanResultCode.SCAN_COMPLETED &&  result.getDeviations() == 0 ){
			body.append("<tr class=\"Background1\"><td width=\"22\" style=\"vertical-align: top;\" class=\"StatGreenColorOnly\"><img src=\"/22_Check\" alt=\"OK\"></td>");
		}
		else if( result.getResultCode() == ScanResultCode.SCAN_COMPLETED &&  result.getDeviations() > 0 ){
			if( severity == Severity.HIGH ){
				body.append("<tr class=\"Background1\"><td width=\"22\" style=\"vertical-align: top;\" class=\"StatRedColorOnly\"><img src=\"/22_Alert\" alt=\"Alert\"></td>");
			}
			else if(severity == Severity.MEDIUM){
				body.append("<tr class=\"Background1\"><td width=\"22\" style=\"vertical-align: top;\" class=\"StatYellowColorOnly\"><img src=\"/22_Warning\" alt=\"Warning\"></td>");
			}
			else{
				body.append("<tr class=\"Background1\"><td width=\"22\" style=\"vertical-align: top;\" class=\"StatBlueColorOnly\"><img src=\"/22_Information\" alt=\"Info\"></td>");
			}
		}
		else {
			body.append("<tr class=\"Background1\"><td width=\"22\" style=\"vertical-align: top;\" class=\"StatYellowColorOnly\"><img src=\"/22_Warning\" alt=\"Warning\"></td>");
		}
		
		body.append("<td title=\"" + result.getUrl().toString()  + "\" style=\"vertical-align:middle;\"><img style=\"display: none;\" id=\"finding" + index + "on\" onclick=\"toggle('finding" + index + "')\" src=\"/9_TreeNodeOpen\" alt=\"Node\"><img id=\"finding" + index + "off\" onclick=\"toggle('finding" + index + "')\" src=\"/9_TreeNodeClosed\" alt=\"Node\">&nbsp;<span class=\"Text_3\">" + Html.shortenString(result.getUrl().toString(), 64) + "&nbsp;&nbsp;&nbsp;</span>");
		
		DefinitionMatch[] matches = result.getDefinitionMatches();
		
		if(matches.length > 0 ){
			body.append("<div style=\"display: none;\" id=\"finding" + index + "\">");
			
			for( int c = 0; c < matches.length; c++){
				body.append("<p>&nbsp;&nbsp;&nbsp;<strong>" + matches[c].getDefinitionName() + ":</strong><br>&nbsp;&nbsp;&nbsp;" + matches[c].getMessage() + "");
			}
			
			body.append("</div>");
		}
		else{
			body.append("<div style=\"display: none;\" id=\"finding" + index + "\"><p>&nbsp;&nbsp;&nbsp;No Signatures Matched</div>");
		}
		
		body.append("</td>");
		
		if( result.getResultCode() == ScanResultCode.SCAN_COMPLETED &&  result.getDeviations() == 0){
			body.append("<td width=\"180\" style=\"vertical-align: top;\">No Issues Found&nbsp;&nbsp;&nbsp;</td><td class=\"StatGreenColorOnly\">&nbsp;</td>");
		}
		else if( result.getResultCode() == ScanResultCode.SCAN_COMPLETED &&  result.getDeviations() > 0 ){
			if( severity == Severity.HIGH ){
				body.append("<td width=\"180\" style=\"vertical-align: top;\">Signatures Matched&nbsp;&nbsp;&nbsp;</td><td class=\"StatRedColorOnly\">&nbsp;</td>");
			}
			else if(severity == Severity.MEDIUM){
				body.append("<td width=\"180\" style=\"vertical-align: top;\">Signatures Matched&nbsp;&nbsp;&nbsp;</td><td class=\"StatYellowColorOnly\">&nbsp;</td>");
			}
			else{
				body.append("<td width=\"180\" style=\"vertical-align: top;\">Signatures Matched&nbsp;&nbsp;&nbsp;</td><td class=\"StatBlueColorOnly\">&nbsp;</td>");
			}
		}
		else{ //if( result.getResultCode() == ScanResultCode.PARSE_FAILED  ){
			body.append("<td width=\"180\" style=\"vertical-align: top;\">Scan issues noted&nbsp;&nbsp;&nbsp;</td><td class=\"StatYellowColorOnly\">&nbsp;</td>");
		}
		
		body.append("</tr>");
		
		return body.toString();
	}
	
	private static String createSummaryRow( String name, String value ){
		return "<tr class=\"Background1\"><td class=\"Text_3\">" + name + "</td><td class=\"Text_3\">" + value + "</td></tr>";
	}
	
	private static StringBuffer getScanForm(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc, String startAddresses, String domainLimit, int recursionDepth, int scanLimit ){
		
		StringBuffer body = new StringBuffer();
		
		// 1 -- Create the header
		body.append( Html.getSectionHeader("Website Scan", "Scan a website to identify malicious or dangerous content." ) );
		
		body.append( "<script src=\"/codepress/codepress.js\" type=\"text/javascript\"></script>");
		
		body.append( "<script type=\"text/javascript\">");
		body.append( "function submitEditorForm(editorform){");
		body.append( "document.editorform.StartAddresses2.value = cp1.getCode();");
		body.append( "document.editorform.submit();");
		body.append( "return true;");
		body.append( "}");
		body.append( "</script>");
		
		body.append( "<form name=\"editorform\" id=\"editorform\" onSubmit=\"return submitEditorForm(this.form)\" action=\"SiteScan\" method=\"post\"><table class=\"DataTable\">"  );
		body.append( "<input type=\"hidden\" name=\"StartAddresses2\" value=\"" + StringEscapeUtils.escapeHtml( StringEscapeUtils.escapeHtml( startAddresses ) )+ "\">" );
		
		// 2 -- Output the start addresses
		if( actionDesc.result == OP_EDIT_START_ADDRESS_INVALID)
			body.append( "<tr class=\"ValidationFailed\">" );
		else
			body.append( "<tr>" );

		body.append( "<td style=\"vertical-align: top;\"><div style=\"margin-top: 5px;\" class=\"TitleText\">Addresses to Scan:</div></td><td><textarea id=\"cp1\" class=\"codepress urls\" wrap=\"virtual\" rows=\"11\" cols=\"48\" name=\"StartAddresses\">").append( StringEscapeUtils.escapeHtml( startAddresses ) ).append( "</textarea></td></tr>" );

		// 3 -- Output the domain limiter
		if( actionDesc.result == OP_EDIT_DOMAIN_LIMIT_INVALID  )
			body.append( "<tr class=\"ValidationFailed\">" );
		else
			body.append( "<tr>" );
		
		body.append( "<td class=\"TitleText\">Domain</td><td><input class=\"textInput\" size=\"40\" type=\"text\" name=\"Domain\" value=\"").append( StringEscapeUtils.escapeHtml( domainLimit ) ).append("\"></td></tr>");
		
		// 4 -- Output the recursion depth
		if( actionDesc.result == OP_EDIT_RECURSION_DEPTH_INVALID  )
			body.append( "<tr class=\"ValidationFailed\">" );
		else
			body.append( "<tr class=\"Background1\">" );

		body.append( "<td class=\"TitleText\">Levels to Recurse</td><td><input class=\"textInput\" size=\"40\" type=\"text\" name=\"RecursionDepth\" value=\"").append( recursionDepth ).append("\"></td></tr>");
		
		// 5 -- Output the scan limit
		if( actionDesc.result == OP_EDIT_SCAN_LIMIT_INVALID  )
			body.append( "<tr class=\"ValidationFailed\">" );
		else
			body.append( "<tr class=\"Background1\">" );

		body.append( "<td class=\"TitleText\">Maximum Number of Resource to Scan</td><td><input class=\"textInput\" size=\"40\" type=\"text\" name=\"ScanLimit\" value=\"").append(  scanLimit ).append("\"></td></tr>");
		
		
		body.append( "<tr class=\"lastRow\"><td class=\"alignRight\" colspan=\"99\"><input type=\"hidden\" name=\"Action\" value=\"Scan\">")
		.append( "<input class=\"button\" type=\"submit\" onClick=\"showHourglass('Initiating Scan...')\" value=\"Scan\" name=\"Scan\">&nbsp;&nbsp;")
		.append( "<input class=\"button\" type=\"submit\" value=\"Cancel\" name=\"Cancel\"></td></tr>"  );
		
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
	
	private static ActionDescriptor performAction( WebConsoleConnectionDescriptor requestDescriptor) throws GeneralizedException, NoSessionException, InsufficientPermissionException{
		
		String action = requestDescriptor.request.getParameter("Action");
		
		// 1 -- No action specified
		if( action == null ){
			return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
		}
		
		// 2 -- Action is to scan
		else if( action.equalsIgnoreCase("Scan") ){
			
			// 1.1 -- Get the list of start addresses 
			String startAddresses = requestDescriptor.request.getParameter("StartAddresses");
			
			if( startAddresses == null || startAddresses.length() == 0 ){
				startAddresses = requestDescriptor.request.getParameter("StartAddresses2");
			}
			
			if( startAddresses == null ){
				Html.addMessage(MessageType.WARNING, "The list of addresses to scan was not provided", requestDescriptor.userId.longValue());
				return new ActionDescriptor(OP_EDIT_START_ADDRESS_INVALID );
			}
			
			URL[] urls;
			
			try{
				urls = parseURLs(startAddresses);
			}
			catch(InputValidationException e){
				Html.addMessage(MessageType.WARNING, e.getMessage(), requestDescriptor.userId.longValue());
				return new ActionDescriptor(OP_EDIT_START_ADDRESS_INVALID );
			}
			catch(IOException e){
				Html.addMessage(MessageType.WARNING, "The list of addresses to scan is invalid", requestDescriptor.userId.longValue());
				return new ActionDescriptor(OP_EDIT_START_ADDRESS_INVALID );
			}

			
			// 1.2 -- Get the recursion depth
			int recursionDepth = 10;
			
			if( requestDescriptor.request.getParameter("RecursionDepth") != null ){
				try{
					recursionDepth = Integer.parseInt( requestDescriptor.request.getParameter("RecursionDepth") );
				}
				catch(NumberFormatException e){
					Html.addMessage(MessageType.WARNING, "The recursion depth is not a valid number", requestDescriptor.userId.longValue());
					return new ActionDescriptor(OP_EDIT_RECURSION_DEPTH_INVALID );
				}
			}
			
			
			// 1.3 -- Get the scan limit
			int scanLimit = 100;
			
			if( requestDescriptor.request.getParameter("ScanLimit") != null ){
				try{
					scanLimit = Integer.parseInt( requestDescriptor.request.getParameter("ScanLimit") );
				}
				catch(NumberFormatException e){
					Html.addMessage(MessageType.WARNING, "The limit on the number of resources to scan is not a valid number", requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_EDIT_SCAN_LIMIT_INVALID );
				}
			}
			
			// 1.4 -- Get the limit on the domain to scan
			String domainLimit = requestDescriptor.request.getParameter("Domain");
			
			if( domainLimit == null ){
				Html.addMessage(MessageType.WARNING, "The domain is invalid", requestDescriptor.userId.longValue());
				return new ActionDescriptor(OP_EDIT_DOMAIN_LIMIT_INVALID );
			}
			
			// 1.5 -- Start the background task
			ApiScannerController scannerController = new ApiScannerController(Application.getApplication());
			try {
				Html.addMessage(MessageType.INFORMATIONAL, "Scan was successfully initiated", requestDescriptor.userId.longValue());

				WorkerThread rule = scannerController.doSiteScan(requestDescriptor.sessionIdentifier, urls, new Wildcard(domainLimit, true), scanLimit, recursionDepth, false);
				
				return new ActionDescriptor( OP_SCAN_STARTED, rule);
			} catch (DuplicateEntryException e) {
				Html.addMessage(MessageType.WARNING, "A scanner has already been initiated. Please wait until the previous scanner completes or cancel it to start another scanner.", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_SCAN_START_FAILED );
			}
		}
		
		// 2 -- Action is to cancel an existing scan
		else if( action.equalsIgnoreCase("Cancel") ){
			ApiTasks tasks = new ApiTasks(Application.getApplication());
			
			WorkerThreadDescriptor descriptor = tasks.getWorkerThread(requestDescriptor.sessionIdentifier, "Sitescan/" + requestDescriptor.userId);
			if( descriptor != null ){
				HttpSeekingScanRule thread = (HttpSeekingScanRule)descriptor.getWorkerThread();
				thread.terminate();
			}
			
			Html.addMessage(MessageType.INFORMATIONAL, "Scan is being terminated", requestDescriptor.userId.longValue());
			return new ActionDescriptor( OP_SCAN_TERMINATED );
		}
		
		// 3 -- Action is create a new scan 
		else if( action.equalsIgnoreCase("New") ){
			return new ActionDescriptor(OP_SCAN_AGAIN);
		}
		
		return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
	}
	
}
