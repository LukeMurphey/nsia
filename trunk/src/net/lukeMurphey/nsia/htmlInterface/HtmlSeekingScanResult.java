package net.lukemurphey.nsia.htmlInterface;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

import org.apache.commons.lang.StringEscapeUtils;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.MaxMinCount;
import net.lukemurphey.nsia.NameIntPair;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.scan.DefinitionMatch;
import net.lukemurphey.nsia.scan.HttpDefinitionScanResult;
import net.lukemurphey.nsia.scan.HttpSeekingScanResult;
import net.lukemurphey.nsia.scan.ScanResultCode;
import net.lukemurphey.nsia.scan.Definition.Severity;
import net.lukemurphey.nsia.trustBoundary.ApiScanData;

public class HtmlSeekingScanResult {
	
	private HtmlSeekingScanResult(){
		//Not instantiable
	}
	
	private static int RESULTS_PER_PAGE = 30;
	
	
	public static ContentDescriptor getScanResultPage( HttpSeekingScanResult scanResult, WebConsoleConnectionDescriptor descriptor, int siteGroupID ) throws GeneralizedException, NoSessionException{
	
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
	
	public static StringBuffer getScanResultReport( HttpSeekingScanResult scanResult, WebConsoleConnectionDescriptor descriptor ) throws GeneralizedException, NoSessionException{
		return getScanResultReport(scanResult, descriptor, -1);
	}
	
	private static StringBuffer getDefinitionMatchTable(ApiScanData scanData, HttpSeekingScanResult scanResult, WebConsoleConnectionDescriptor descriptor, long siteGroupID) throws GeneralizedException, NoSessionException{
		StringBuffer body = new StringBuffer();
		
		Vector<NameIntPair> definitionMatches = scanData.getHTTPSeekingDefinitionMatches(descriptor.sessionIdentifier, scanResult.getScanResultID() );
		
		body.append( "<p><table width=\"700px\" cellpadding=\"2\">" );
		
		if( definitionMatches.size() == 0 ){
			body.append( "<tr class=\"Background0\"><td height=\"8px\" class=\"Text_3\">Definitions Matched</td>");
			body.append( "</tr><tr class=\"Background1\"><td>" + Html.getInfoNote("No definitions matches observed") +"</td></tr>" );
			
		}
		else{
			body.append( "<tr class=\"Background0\"><td height=\"8px\" colspan=\"3\" class=\"Text_3\">Definitions Matched</td>");
		
			if( definitionMatches.size() >= 5){
				body.append( "<td width=\"400px\" rowspan=\"99\" style=\"vertical-align:top\" class=\"BackgroundLoading1\"><img alt=\"ContentTypes\" src=\"/SeverityResults?ResultID=" + scanResult.getScanResultID() + "&H=" + (25 + (definitionMatches.size() * 23)) + "\"</td></tr>" );
			}
			else{
				body.append( "<td width=\"400px\" rowspan=\"99\" style=\"vertical-align:top\" class=\"BackgroundLoading1\"><img alt=\"ContentTypes\" src=\"/SeverityResults?ResultID=" + scanResult.getScanResultID() + "\"</td></tr>" );
			}
			
			for( int c = 0; c < definitionMatches.size(); c++){
				body.append( getDefinitionMatchRow( definitionMatches.get(c), scanResult.getRuleID(), descriptor, siteGroupID, scanResult.getScanResultID() ) );
			}
			
			if( definitionMatches.size() < 6){
				body.append("<tr class=\"Background1\"><td height=\"" + ((6-definitionMatches.size())*22) + "\" colspan=\"3\"></td></tr>");
			}
		}
		
		body.append( "</table>" );
		
		return body;
	}
	
	
	private static StringBuffer getContentTypeTable( ApiScanData scanData, HttpSeekingScanResult scanResult, long siteGroupID, WebConsoleConnectionDescriptor descriptor){
		StringBuffer body = new StringBuffer();
		
		Vector<NameIntPair> contentTypesCount = scanResult.getDiscoveredContentTypes();
		
		body.append( "<p><table width=\"700px\" cellpadding=\"2\">" );
		body.append( "<tr class=\"Background0\"><td height=\"8px\" colspan=\"2\" class=\"Text_3\">Content Types</td><td style=\"vertical-align:top\" width=\"400px\" rowspan=\"99\" class=\"BackgroundLoading1\">" );
		
		if( contentTypesCount.size() >= 5){
			body.append("<img alt=\"ContentTypes\" src=\"/ContentTypeResults?ResultID=" + scanResult.getScanResultID() + "&H=" + (25 + (contentTypesCount.size() * 20)) + "\"</td></tr>" );
		}
		else{
			body.append("<img alt=\"ContentTypes\" src=\"/ContentTypeResults?ResultID=" + scanResult.getScanResultID() + "\"</td></tr>" );
		}
		
		if( contentTypesCount.isEmpty() ){
			body.append( "<tr class=\"Background1\"><td colspan=\"99\">" + Html.getInfoNote("No resources scanned") +"</td></tr>" );
		}
		else{
			
			for(int c = 0; c < contentTypesCount.size(); c++){
				body.append( getContentTypeRow(contentTypesCount.get(c).getName(), contentTypesCount.get(c).getValue(), scanResult.getScanResultID() , siteGroupID, descriptor ) );
			}
		}
		
		if( contentTypesCount.size() < 5){
			body.append("<tr class=\"Background1\"><td height=\"" + ((5-contentTypesCount.size())*30) + "px\" colspan=\"2\"></td></tr>");
		}
		
		body.append( "</table>" );
		
		return body;
		
	}
	public static StringBuffer getScanResultReport( HttpSeekingScanResult scanResult, WebConsoleConnectionDescriptor descriptor, long siteGroupID) throws GeneralizedException, NoSessionException{

		StringBuffer body = new StringBuffer();
		
		body.append(Html.renderMessages(descriptor.userId));
		
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
		body.append( createSummaryRow( "Resources Scanned", Integer.toString( scanResult.getAccepts() + scanResult.getDeviations() + scanResult.getIncompletes() ) ) );//scanResult.getFindings().length
		body.append( createSummaryRow( "Domain", scanResult.getSpecimenDescription() ) );
		
		body.append( "</table>" );
		
		// Print out the list of definitions fired
		ApiScanData scanData = new ApiScanData(Application.getApplication());
		body.append( HtmlSeekingScanResult.getDefinitionMatchTable(scanData, scanResult, descriptor, siteGroupID) );
		
		
		// Print out the list of content types observed
		body.append( getContentTypeTable(scanData, scanResult, siteGroupID, descriptor) );
		
		
		// Print out the resources scanned
		String scanRuleFilter = descriptor.request.getParameter("RuleFilter");
		String scanRuleFilterEscaped = null;
		String contentTypeFilter = descriptor.request.getParameter("ContentTypeFilter");
		String contentTypeFilterEscaped = null;
		
		HttpDefinitionScanResult.SignatureScanResultFilter filter;
		
		if( contentTypeFilter != null && contentTypeFilter.equalsIgnoreCase("[unknown]" )){
			filter = new HttpDefinitionScanResult.SignatureScanResultFilter("", scanRuleFilter);
		}
		else{
			filter = new HttpDefinitionScanResult.SignatureScanResultFilter(contentTypeFilter, scanRuleFilter);
		}
		
		HttpDefinitionScanResult[] findings; //scanResult.getFindings();
		MaxMinCount maxMinCount = null;
		
		long firstScanResultId = -1;
		long lastScanResultId = -1;
		long startEntry = -1;
		boolean resultsBefore = false;
		
		try{
			if( descriptor.request.getParameter("S") != null ){
				firstScanResultId = Long.valueOf( descriptor.request.getParameter("S") );
			}
			
			if( descriptor.request.getParameter("E") != null ){
				lastScanResultId = Long.valueOf( descriptor.request.getParameter("E") );
			}
			
			String action = descriptor.request.getParameter("Action");
			if( action != null && action.equalsIgnoreCase("Previous") ){
				startEntry = firstScanResultId;
				resultsBefore = true;
			}
			else if( action != null && action.equalsIgnoreCase("Next") ){
				startEntry = lastScanResultId;
				resultsBefore = false;
			}
		}
		catch(NumberFormatException e){
			return new StringBuffer( HtmlProblemDialog.getHtml(descriptor, "Invalid Parameter", "The result identifier provided is invalid", HtmlProblemDialog.DIALOG_WARNING, "/", "Main Dashboard").getBody() );
		}
		
		
		try{
			maxMinCount = scanData.getHTTPSeekingResultInfo(descriptor.sessionIdentifier, scanResult.getScanResultID(), filter);
			findings = scanResult.getFindings(startEntry, RESULTS_PER_PAGE, filter, resultsBefore);
			if( findings.length > 0 ){
				firstScanResultId = findings[0].getScanResultID();
				lastScanResultId = findings[findings.length - 1].getScanResultID();
			}
		}
		catch(Exception e){
			e.printStackTrace();
			System.out.println(e.getMessage());
			findings = new HttpDefinitionScanResult[0]; //TODO deal with this exception
		}
		
		body.append("<p>");
		
		if( scanRuleFilter != null ){
			scanRuleFilterEscaped = StringEscapeUtils.escapeHtml(scanRuleFilter);
			
			if( scanRuleFilter != null ){
				StringBuffer note;
				if( siteGroupID > -1 ){
					note = new StringBuffer("<a href=\"ScanResult?ResultID=" + scanResult.getScanResultID() );
				}
				else{
					note = new StringBuffer("<a href=\"SiteScan?ResultID=" + scanResult.getScanResultID() );
				}
				
				note.append( "\">[Clear Filter]</a>");
				body.append(Html.getDialog("Displaying findings that matched the \"<u>" + scanRuleFilterEscaped + "</u>\" definition.<br>" + note.toString(), "Filters Applied", "/32_Information") );
			}
		}
		else if( contentTypeFilter != null ){
			contentTypeFilterEscaped = StringEscapeUtils.escapeHtml(contentTypeFilter);

			if( contentTypeFilter != null ){
				
				StringBuffer note;
				if( siteGroupID > -1 ){
					note = new StringBuffer("<a href=\"ScanResult?ResultID=" + scanResult.getScanResultID() );
				}
				else{
					note = new StringBuffer("<a href=\"SiteScan?ResultID=" + scanResult.getScanResultID() );
				}
				
				note.append( "\">[Clear Filter]</a>");
				body.append(Html.getDialog("Displaying findings that are of type \"<u>" + contentTypeFilterEscaped + "</u>\".<br>" + note.toString(), "Filters Applied", "/32_Information") );
			}
		}
		else{
			body.append("&nbsp;<p>");
		}
		
		body.append( "<table width=\"700\" cellpadding=\"2\">" );
		body.append( "<tr class=\"Background0\"><td colspan=\"99\" class=\"Text_3\">Scan Findings</td></tr>" );
		
		if( findings.length == 0){
			body.append( "<tr><td colspan=\"99\">" + Html.getDialog("No resources where scanned during the scan phase.", "No Findings", "/32_Information") +"<td></tr>" );
		}
		else{
			
			for(int c = 0; c < findings.length; c++ ){
				body.append( createFindingRow(findings[c], c, scanRuleFilterEscaped, contentTypeFilterEscaped, siteGroupID, scanResult.getRuleID() ) );
			}
		}
		
		body.append( "</table>" );
		
		
		// Print out the "next" and "previous" buttons
		body.append("<br><form action=\"ScanResult\">");
		body.append("<input type=\"hidden\" name=\"ResultID\" value=\"" + scanResult.getScanResultID() + "\">");
		
		if( maxMinCount != null && firstScanResultId == maxMinCount.getMin() ){
			body.append("<input disabled=\"true\" class=\"buttonDisabled\" type=\"submit\" name=\"Action\" value=\"Previous\">");
		}
		else{
			body.append("<input class=\"button\" type=\"submit\" name=\"Action\" value=\"Previous\">");
		}
		
		if( maxMinCount != null && lastScanResultId == maxMinCount.getMax() ){
			body.append("<input disabled=\"true\" class=\"buttonDisabled\" type=\"submit\" name=\"Action\" value=\"Next\">");
		}
		else{
			body.append("<input class=\"button\" type=\"submit\" name=\"Action\" value=\"Next\">");
		}
		
		if( contentTypeFilterEscaped != null ){
			body.append("<input type=\"hidden\" name=\"ContentTypeFilter\" value=\"" + contentTypeFilterEscaped + "\">");
		}
		
		if( scanRuleFilterEscaped != null ){
			body.append("<input type=\"hidden\" name=\"RuleFilter\" value=\"" + scanRuleFilterEscaped + "\">");
		}
		
		body.append("<input type=\"hidden\" name=\"S\" value=\"" + firstScanResultId + "\">");
		body.append("<input type=\"hidden\" name=\"E\" value=\"" + lastScanResultId + "\">");
		
		body.append("</form>");
		
		return body;
	}
	
	private static StringBuffer getDefinitionMatchRow(NameIntPair match, long scanRuleID, WebConsoleConnectionDescriptor descriptor, long siteGroupID, long scanResultID){
		StringBuffer body = new StringBuffer();

		String returnUrl;
		
		try{
			returnUrl = URLEncoder.encode( ("ScanResult?ResultID=" + scanResultID ), "UTF-8" );
		}
		catch(UnsupportedEncodingException e){
			returnUrl = null;
		}
		
		if( siteGroupID >= 0){
			body = body.append( "<tr class=\"Background1\"><td height=\"8px\" class=\"Text_3\"><a href=\"ScanResult?RuleFilter=" + match.getName() + "&ResultID=" + scanResultID );
		}
		else{
			body = body.append( "<tr class=\"Background1\"><td height=\"8px\" class=\"Text_3\"><a href=\"SiteScan?RuleFilter=" + match.getName() + "&ResultID=" + scanResultID );
		}
		
		body = body.append("\">" + match.getName() + "</a></td>");
		
		if( siteGroupID >= 0){
			body = body.append("<td class=\"Text_3\" width=\"8\"><a href=\"ExceptionManagement?Action=New&RuleID=" + scanRuleID + "&SiteGroupID=" + siteGroupID + "&DefinitionName=" + match.getName() + "&ReturnTo=" + returnUrl + "\"><img class=\"imagebutton\" alt=\"Filter\" src=\"/16_Filter\"></a></td>" );
		}
		
		body = body.append("<td height=\"8px\" class=\"Text_3\">" + match.getValue() + "</td></tr>" );
		
		return body;
	}
	
	private static StringBuffer getContentTypeRow(String contentType, int count, long scanResultId, long siteGroupID, WebConsoleConnectionDescriptor descriptor){
		StringBuffer body = new StringBuffer();
		
		if( siteGroupID > 0 ){
			body = body.append( "<tr class=\"Background1\"><td height=\"8px\" class=\"Text_3\"><a href=\"ScanResult?ContentTypeFilter=" + contentType + "&ResultID=" + scanResultId);
		}
		else{
			body = body.append( "<tr class=\"Background1\"><td height=\"8px\" class=\"Text_3\"><a href=\"SiteScan?ContentTypeFilter=" + contentType + "&ResultID=" + scanResultId);
		}
		
		body = body.append( "\">" + contentType + "</a></td><td height=\"8px\" class=\"Text_3\">" + count + "</td></tr>" );
		
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
	
	static class ContentTypeCount{
		
		public ContentTypeCount( String contentType ){
			count = 1;
			this.contentType = contentType;
		}
		
		public String contentType;
		public int count;
	}
	
	private static String createFindingRow( HttpDefinitionScanResult result, int index, String scanRuleFilter, String contentTypeFilter, long siteGroupID, long ruleID){
		StringBuffer body = new StringBuffer();
		
		Severity severity = Severity.UNDEFINED;
		boolean definitionMatchesFilter = true;
		
		if( contentTypeFilter != null ){
			
			definitionMatchesFilter = false;
			
			if( result.getContentType() == null && "[unknown]".equalsIgnoreCase(contentTypeFilter) ){
				definitionMatchesFilter = true;
			}
			else if( result.getContentType() == null && !"[unknown]".equalsIgnoreCase(contentTypeFilter) ){
				return "";
			}
			else if( result.getContentType() != null && result.getContentType().equalsIgnoreCase(contentTypeFilter) ){
				definitionMatchesFilter = true;
			}
			else{
				return "";
			}
		}
		
		if( result.getDefinitionMatches() != null && result.getDefinitionMatches().length > 0 ){
			
			if( scanRuleFilter != null ){
				definitionMatchesFilter = false;
				for(int c = 0; c < result.getDefinitionMatches().length; c++){
					if( result.getDefinitionMatches()[c].getDefinitionName().equalsIgnoreCase(scanRuleFilter) ){
						definitionMatchesFilter = true;
					}
				}
			}
			
			if( definitionMatchesFilter == false ){
				return "";
			}
			
			severity = maxSeverity(result.getDefinitionMatches());
		}
		else if(scanRuleFilter != null){
			return "";
		}
		
		if( result.getResultCode() == ScanResultCode.SCAN_COMPLETED &&  result.getDeviations() == 0 ){
			body.append("<tr class=\"Background1\"><td width=\"22\" style=\"vertical-align: top;\" class=\"StatGreenSmall\"><img src=\"/22_Check\" alt=\"OK\"></td>");
		}
		else if( result.getResultCode() == ScanResultCode.SCAN_COMPLETED &&  result.getDeviations() > 0 ){
			if( severity == Severity.HIGH ){
				body.append("<tr class=\"Background1\"><td width=\"22\" style=\"vertical-align: top;\" class=\"StatRedSmall\"><img src=\"/22_Alert\" alt=\"Alert\"></td>");
			}
			else if(severity == Severity.MEDIUM){
				body.append("<tr class=\"Background1\"><td width=\"22\" style=\"vertical-align: top;\" class=\"StatYellowSmall\"><img src=\"/22_Warning\" alt=\"Warning\"></td>");
			}
			else{
				body.append("<tr class=\"Background1\"><td width=\"22\" style=\"vertical-align: top;\" class=\"StatBlueSmall\"><img src=\"/22_Information\" alt=\"Info\"></td>");
			}
		}
		else {
			body.append("<tr class=\"Background1\"><td width=\"22\" style=\"vertical-align: top;\" class=\"StatYellowSmall\"><img src=\"/22_Warning\" alt=\"Warning\"></td>");
		}
		
		body.append("<td title=\"" + result.getUrl().toString()  + "\" style=\"vertical-align:middle;\"><img style=\"display: none;\" id=\"finding" + index + "on\" onclick=\"toggle('finding" + index + "')\" src=\"/9_TreeNodeOpen\" alt=\"Node\"><img id=\"finding" + index + "off\" onclick=\"toggle('finding" + index + "')\" src=\"/9_TreeNodeClosed\" alt=\"Node\">&nbsp;<span class=\"Text_3\">" + Html.shortenString(result.getUrl().toString(), 64) + "&nbsp;&nbsp;&nbsp;</span>");
		
		DefinitionMatch[] matches = result.getDefinitionMatches();
		
		if(matches.length > 0 ){
			body.append("<div style=\"display: none;\" id=\"finding" + index + "\">");
			
			for( int c = 0; c < matches.length; c++){
				body.append("<p>&nbsp;&nbsp;&nbsp;<strong>" + matches[c].getDefinitionName() + ":</strong>");
				
				if( siteGroupID > -1 ){
					String encodedURL;
					try{
						encodedURL = URLEncoder.encode( result.getSpecimenDescription(), "UTF-8" );
					}catch (UnsupportedEncodingException e) {
						encodedURL = null;
					}
					
					if( encodedURL != null ){
						String returnUrl;
						
						try{
							returnUrl = URLEncoder.encode( ("ScanResult?ResultID=" + result.getParentScanResultID() ), "UTF-8" );
						}
						catch(UnsupportedEncodingException e){
							returnUrl = null;
						}
						
						body.append("<a href=\"/ExceptionManagement?Action=New&RuleID=" + ruleID + "&SiteGroupID=" + siteGroupID + "&DefinitionName=" + matches[c].getDefinitionName() + "&URL=" + encodedURL + "&ReturnTo=" + returnUrl + "\">&nbsp;(Create Exception)</a>");
					}
				}
				
				body.append("<br>&nbsp;&nbsp;&nbsp;" + matches[c].getMessage() + "");
			}
			
			body.append("</div>");
		}
		else{
			body.append("<div style=\"display: none;\" id=\"finding" + index + "\"><p>&nbsp;&nbsp;&nbsp;No Definitions Matched</div>");
		}
		
		body.append("</td>");
		
		if( result.getResultCode() == ScanResultCode.SCAN_COMPLETED &&  result.getDeviations() == 0){
			body.append("<td width=\"180\" style=\"vertical-align: top;\">No Issues Found&nbsp;&nbsp;&nbsp;</td><td class=\"StatGreenSmall\">&nbsp;</td>");
		}
		else if( result.getResultCode() == ScanResultCode.SCAN_COMPLETED &&  result.getDeviations() > 0 ){
			if( severity == Severity.HIGH ){
				body.append("<td width=\"180\" style=\"vertical-align: top;\">Definitions Matched&nbsp;&nbsp;&nbsp;</td><td class=\"StatRedSmall\">&nbsp;</td>");
			}
			else if(severity == Severity.MEDIUM){
				body.append("<td width=\"180\" style=\"vertical-align: top;\">Definitions Matched&nbsp;&nbsp;&nbsp;</td><td class=\"StatYellowSmall\">&nbsp;</td>");
			}
			else{
				body.append("<td width=\"180\" style=\"vertical-align: top;\">Definitions Matched&nbsp;&nbsp;&nbsp;</td><td class=\"StatBlueSmall\">&nbsp;</td>");
			}
		}
		else{ //if( result.getResultCode() == ScanResultCode.PARSE_FAILED  ){
			body.append("<td width=\"180\" style=\"vertical-align: top;\">Scan issues noted&nbsp;&nbsp;&nbsp;</td><td class=\"StatYellowSmall\">&nbsp;</td>");
		}
		
		body.append("</tr>");
		
		return body.toString();
	}
	
	private static String createSummaryRow( String name, String value ){
		return "<tr class=\"Background1\"><td class=\"Text_3\">" + name + "</td><td class=\"Text_3\">" + value + "</td></tr>";
	}

}
