package net.lukemurphey.nsia.htmlInterface;

import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;

import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.htmlInterface.Html.MessageType;
import net.lukemurphey.nsia.scan.HttpHeaderRule;
import net.lukemurphey.nsia.scan.HttpHeaderScanResult;
import net.lukemurphey.nsia.scan.HttpStaticScanRule;
import net.lukemurphey.nsia.trustBoundary.ApiScanData;

public class HtmlRules_HttpDataHash_Header extends HtmlContentProvider {
	
	private static final int OP_ADD_NAME_INVALID = 100;
	private static final int OP_ADD_VALUE_INVALID = 101;
	private static final int OP_EDIT_NAME_INVALID = 102;
	private static final int OP_EDIT_VALUE_INVALID = 103;
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ApiScanData scanData ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, NotFoundException, InputValidationException{
		return getHtml( requestDescriptor, null, scanData );
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc, ApiScanData scanData ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, NotFoundException, InputValidationException{
		
		//ApiScanData scanData = new ApiScanData(Application.getApplication());
		
		// 2 -- Perform any pending actions
		if( actionDesc == null )
			actionDesc = performAction( requestDescriptor, scanData);
		
		// 1 -- Output the main content
		
		//	 1.1 -- Delete the header rule
		if( actionDesc.result == ActionDescriptor.OP_DELETE_SUCCESS ){
			return HtmlStaticScanRule.getHtml(requestDescriptor);
		}
		
		//	 1.2 -- View the header rule
		//if( actionDesc.result == ActionDescriptor.OP_UPDATE )
		return getHeaderRuleView( requestDescriptor, actionDesc, scanData);
		
		//	 1.3 -- Create new header rule
		
		//	 1.4 -- Edit header rule
		
		//	 1.5 -- View the header rule (default action)
		
		
	}
	
	/**
	 * 
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
	private static ContentDescriptor getHeaderRuleView( WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc, ApiScanData xScanData) throws NoSessionException, GeneralizedException, NotFoundException{
		String title = "Scan Rule : View Headers";
		StringBuffer body = new StringBuffer();
		
		// 1 -- Perform any pending actions
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		// 2 -- Output the main content
		
		//	 2.1 -- Get the relevant rule identifiers
		long headerRuleId = -1;
		long ruleId = -1;
		
		try{
			headerRuleId = Long.parseLong(requestDescriptor.request.getParameter("HeaderRuleID"));
		} catch( NumberFormatException e ){
			headerRuleId = -2;
		}
		
		try{
			ruleId = Long.parseLong(requestDescriptor.request.getParameter("RuleID"));
		} catch( NumberFormatException e ){
			ruleId = -2;
		}
		
		if( ruleId < 0 ){
			ruleId = xScanData.resolveHttpHeaderRuleId( requestDescriptor.sessionIdentifier, headerRuleId );
		}
		
		//TODO make sure that at least one of the identifiers are set
		
		//	 2.2 -- Get the relevant fields
		String headerName = "";
		int headerNameType = -1;
		String headerValue = "";
		int headerValueType = -1;
		int matchAction = HttpStaticScanRule.MUST_MATCH;
		long siteGroupId;
		
		try{
			siteGroupId = xScanData.getAssociatedSiteGroup( requestDescriptor.sessionIdentifier, ruleId );
		}catch( InsufficientPermissionException e){
			siteGroupId = -1;
		}
		
		//		2.2.1 -- Load the parameters from a pre-existing rule (if modifying a previous rule)
		try{
		if( headerRuleId >= 0 ){
			HttpStaticScanRule scan = (HttpStaticScanRule)xScanData.getScanRule(requestDescriptor.sessionIdentifier, ruleId);
			
			//TODO Insert error handler if rule could not be found
			/*if( scan == null ){
				
			}*/
			HttpHeaderRule[] headerRules = scan.getHeaderRules();
			HttpHeaderRule headerRule = null;
			
			//Get the the header rule
			for( int c = 0; c < headerRules.length; c++){
				if( headerRules[c].getRuleId() == headerRuleId )
					headerRule = headerRules[c];
			}
			
			if( headerRule.getNameRuleType() != HttpHeaderRule.RULE_TYPE_STRING){
				headerName = headerRule.getHeaderNameRegex().pattern();
				headerNameType = HttpHeaderRule.RULE_TYPE_REGEX;
			}
			else {
				headerName = headerRule.getHeaderNameString();
				headerNameType = HttpHeaderRule.RULE_TYPE_STRING;
			}
			
			if( headerRule.getValueRuleType() != HttpHeaderRule.RULE_TYPE_STRING){
				headerValue = headerRule.getHeaderValueRegex().pattern();
				headerValueType = HttpHeaderRule.RULE_TYPE_REGEX;
			}
			else {
				headerValue = headerRule.getHeaderValueString();
				headerValueType = HttpHeaderRule.RULE_TYPE_STRING;
			}
			
			matchAction = headerRule.getRuleType();
		}
		
		//		2.2.2 -- Load any parameters specified by an argument
		
		//			2.2.2.1 -- Header name
		if( requestDescriptor.request.getParameter("HeaderName") != null )
			headerName = requestDescriptor.request.getParameter("HeaderName");
		
		//			2.2.2.2 -- Header name type
		if( headerNameType == -1 ){
			if( requestDescriptor.request.getParameter("HeaderNameType") == null || "Regex".matches( requestDescriptor.request.getParameter("HeaderNameType") ) )
				headerNameType = HttpHeaderScanResult.RULE_TYPE_REGEX;
			else
				headerNameType = HttpHeaderScanResult.RULE_TYPE_STRING;
		}
		
		//			2.2.2.3 -- Header value
		if( requestDescriptor.request.getParameter("HeaderValue") != null )
			headerValue = requestDescriptor.request.getParameter("HeaderValue");
		
		//			2.2.2.4 -- Header value type
		if( headerValueType == -1 ){
			if( requestDescriptor.request.getParameter("HeaderValueType") == null || "Regex".matches( requestDescriptor.request.getParameter("HeaderValueType") ) )
				headerValueType = HttpHeaderScanResult.RULE_TYPE_REGEX;
			else
				headerValueType = HttpHeaderScanResult.RULE_TYPE_STRING;
		}
		
		//			2.2.2.5 -- Match action
		if( requestDescriptor.request.getParameter("MatchAction") == null || "MustMatch".matches( requestDescriptor.request.getParameter("MatchAction") ) )
			matchAction = HttpStaticScanRule.MUST_MATCH;
		else
			matchAction = HttpStaticScanRule.MUST_NOT_MATCH;
		
		
		//	3 -- Create the form (main content)
		
		//		3.1 -- Section header
		body.append( Html.getSectionHeader( "HTTP Header Rule", null ) );
		
		body.append( "<form action=\"ScanRule\" method=\"post\"><table>" );
		
		//		3.2 -- Header name
		if( actionDesc.result == OP_ADD_NAME_INVALID)
			body.append( "<tr class=\"ValidationFailed\">");
		else
			body.append( "<tr class=\"Background1\">");
		
		body.append( "<td colspan=\"2\" class=\"Text_3\">Header Name</td></tr>" );

		body.append( "<tr class=\"Background1\"><td><input class=\"textInput\" type=\"text\" size=\"48\" name=\"HeaderName\" value=\"").append( StringEscapeUtils.escapeHtml( headerName ) ).append( "\"></td><td><select name=\"HeaderNameType\">");
		if( headerNameType == HttpHeaderScanResult.RULE_TYPE_REGEX )
			body.append( "<option value=\"Regex\" selected>Regex Comparison</option><option value=\"String\">String Comparison</option></select></tr>" );
		else
			body.append( "<option value=\"Regex\">Regex Comparison</option><option value=\"String\" selected>String Comparison</option></select></tr>" );
		body.append( "<tr><td colspan=\"99\">&nbsp;</td></tr>"  );
		
		//		3.3 -- Header value
		if( actionDesc.result == OP_ADD_VALUE_INVALID)
			body.append( "<tr class=\"ValidationFailed\">");
		else
			body.append( "<tr class=\"Background1\">");
		
		body.append( "<td colspan=\"2\" class=\"Text_3\">Header Value</td></tr>"  );
		body.append( "<tr class=\"Background1\"><td><input class=\"textInput\" type=\"text\" size=\"48\" name=\"HeaderValue\" value=\"").append( StringEscapeUtils.escapeHtml( headerValue ) ).append( "\"></td><td><select name=\"HeaderValueType\">" );
		if( headerValueType == HttpHeaderScanResult.RULE_TYPE_REGEX )
			body.append( "<option value=\"Regex\" selected>Regex Comparison</option><option value=\"String\">String Comparison</option></select></tr>" );
		else
			body.append( "<option value=\"Regex\">Regex Comparison</option><option value=\"String\" selected>String Comparison</option></select></tr>" );
		body.append( "<tr><td colspan=\"99\">&nbsp;</td></tr>"  );
		
		//		3.4 -- Match action (rule type)
		body.append( "<tr class=\"Background1\"><td colspan=\"2\" class=\"Text_3\">MatchAction</td></tr>"  );
		body.append( "<tr class=\"Background1\"><td colspan=\"2\">"  );
		if( matchAction == HttpStaticScanRule.MUST_MATCH ){
			body.append("<table><tr><td><input type=\"radio\" name=\"MatchAction\" value=\"MustMatch\" checked></td><td><img src=\"/Match\" alt=\"==\"></td><td>Must Match (fail if at least one header does not match)</td></tr>" );
			body.append("<tr><td><input type=\"radio\" name=\"MatchAction\" value=\"MustNotMatch\"></td><td><img src=\"/NotMatch\" alt=\"!=\"></td><td>Must Not Match (fail if at least any header matches)</td></tr></table>" );
		}
		else{
			body.append("<table><tr><td><input type=\"radio\" name=\"MatchAction\" value=\"MustMatch\"></td><td><img src=\"/Match\" alt=\"==\"></td><td>Must Match (fail if at least one header does not match)</td></tr>" );
			body.append("<tr><td><input type=\"radio\" name=\"MatchAction\" value=\"MustNotMatch\" checked></td><td><img src=\"/NotMatch\" alt=\"!=\"></td><td>Must Not Match (fail if at least any header matches)</td></tr></table>" );
		}
		
		//		3.5 -- Form options and table end
		if( actionDesc.result == ActionDescriptor.OP_ADD || actionDesc.result == OP_ADD_NAME_INVALID || actionDesc.result == OP_ADD_VALUE_INVALID ){
			body.append( "<tr class=\"Background3\"><td class=\"alignRight\" colspan=\"99\"><input class=\"button\" type=\"submit\" name=\"Submit\" value=\"Add Rule\">");
			body.append( "<input type=\"Hidden\" name=\"Action\" value=\"NewHeaderRule\"><input type=\"Hidden\" name=\"RuleID\" value=\"" + ruleId + "\">");
		}
		else {
			body.append( "<tr class=\"Background3\"><td class=\"alignRight\" colspan=\"99\"><input class=\"button\" type=\"submit\" name=\"Submit\" value=\"Apply Changes\">");
			body.append( "<input type=\"Hidden\" name=\"Action\" value=\"EditHeaderRule\"><input type=\"Hidden\" name=\"HeaderRuleID\" value=\"" + headerRuleId + "\">");
		}
		
		body.append( "</td></tr></table></form>" );		
		}catch(InsufficientPermissionException e){
			body.append(Html.getWarningDialog("Insufficient Permission", "You do not have permission to view this rule" , "Console", "Return to Main Dashboard"));
		}
		
		// 4 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "Site Group", "/SiteGroup?SiteGroupID=" + siteGroupId );
		navPath.addPathEntry( "Http Rule", "/ScanRule?RuleID=" + ruleId );
		if( actionDesc.result == ActionDescriptor.OP_ADD || actionDesc.result == OP_ADD_NAME_INVALID || actionDesc.result == OP_ADD_VALUE_INVALID  )
			navPath.addPathEntry( "New Http Header Rule", "/ScanRule?Action=NewHeaderRule&RuleID=" + ruleId );
		else
			navPath.addPathEntry( "Edit Http Header Rule", "/ScanRule?Action=EditHeaderRule&HeaderRuleID=" + headerRuleId );
		String navigationHtml = Html.getNavigationPath( navPath );
		
		
		// 5 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
		
		menuItems.add( new MenuItem("Site Group", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Edit", "/SiteGroup?SiteGroupId=" + requestDescriptor.request.getParameter("SiteGroupId"), MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Delete", "/SiteGroup?Action=Delete&SiteGroupId=" + requestDescriptor.request.getParameter("SiteGroupId"), MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Scan Now", "/SiteGroup?Action=Scan&SiteGroupId=" + requestDescriptor.request.getParameter("SiteGroupId"), MenuItem.LEVEL_TWO) );
		//menuItems.add( new MenuItem("View All", "/SiteGroup", MenuItem.LEVEL_TWO) );
		
		String menuOutput = Html.getMenu( menuItems );
		
		// 6 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor(title, pageOutput);
	}
	
	/**
	 * Perform any pending actions.
	 * @param request
	 * @param response
	 * @param requestDescriptor
	 * @param httpMethod
	 * @param scanData
	 * @return
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 */
	private static ActionDescriptor performAction( WebConsoleConnectionDescriptor requestDescriptor, ApiScanData scanData) throws GeneralizedException, NoSessionException{
		
		String action = requestDescriptor.request.getParameter("Action");
		// 1 -- Load the relevant page
		
		//	 1.1 -- View the header rule
		if( action == null )
			return new ActionDescriptor( ActionDescriptor.OP_VIEW);
		
		//	 1.2 -- Create a new header rule
		else if( action.matches( "NewHeaderRule" ) ){

			if( requestDescriptor.request.getParameter("Submit") == null && requestDescriptor.request.getParameter("AutoPopulate") == null )
				return new ActionDescriptor( ActionDescriptor.OP_ADD );
			
			// 1.2.1 -- Get the attributes
			long ruleId = -1;
			String headerName;
			int headerNameType = HttpHeaderScanResult.RULE_TYPE_STRING;
			String headerValue;
			int headerValueType = HttpHeaderScanResult.RULE_TYPE_STRING;
			int matchAction = HttpStaticScanRule.MUST_MATCH;
			
			//	 1.2.1.1 -- Get the rule ID
			try{
				ruleId = Long.parseLong( requestDescriptor.request.getParameter("RuleID") );
			}
			catch( NumberFormatException e ){
				Html.addMessage(MessageType.WARNING, "A rule identifier was not provided", requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_ADD_FAILED );
			}
			
			//	 1.2.1.2 -- Get the header name
			if( requestDescriptor.request.getParameter("HeaderName") != null )
				headerName = requestDescriptor.request.getParameter("HeaderName");
			else{
				Html.addMessage(MessageType.WARNING, "The header name is invalid", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_ADD_NAME_INVALID );
			}
			
			//	 1.2.1.3 -- Get the header name type
			if( requestDescriptor.request.getParameter("HeaderValueType") == null || "Regex".matches( requestDescriptor.request.getParameter("HeaderValueType") ) )
				headerValueType = HttpHeaderScanResult.RULE_TYPE_REGEX;
			else
				headerValueType = HttpHeaderScanResult.RULE_TYPE_STRING;
			
			//	 1.2.1.4 -- Get the header value
			if( requestDescriptor.request.getParameter("HeaderValue") != null )
				headerValue = requestDescriptor.request.getParameter("HeaderValue");
			else{
				Html.addMessage(MessageType.WARNING, "The header value is invalid", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_ADD_VALUE_INVALID );
			}
			
			//	 1.2.1.5 -- Get the header value type
			if( requestDescriptor.request.getParameter("HeaderNameType") == null || "Regex".matches( requestDescriptor.request.getParameter("HeaderNameType") ) )
				headerNameType = HttpHeaderScanResult.RULE_TYPE_REGEX;
			else
				headerNameType = HttpHeaderScanResult.RULE_TYPE_STRING;
			
			//	 1.2.1.6 -- Get the rule action
			if( requestDescriptor.request.getParameter("MatchAction") == null || "MustMatch".matches( requestDescriptor.request.getParameter("MatchAction") ) )
				matchAction = HttpStaticScanRule.MUST_MATCH;
			else
				matchAction = HttpStaticScanRule.MUST_NOT_MATCH;
			
			try{
				if( scanData.addHttpHeaderRule( requestDescriptor.sessionIdentifier, ruleId, headerName, headerNameType, headerValue, headerValueType, matchAction ) ){
					Html.addMessage(MessageType.INFORMATIONAL, "The header rule was added successfully", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_ADD_SUCCESS);
				}
				else{
					Html.addMessage(MessageType.WARNING, "The header rule was not added successfully", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_ADD_FAILED);
				}
			}
			catch( InputValidationException e ){
				if( e.getFieldDescription().matches("HeaderName") ){
					Html.addMessage(MessageType.INFORMATIONAL, e.getMessage(), requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_ADD_NAME_INVALID );
				}
				else{
					Html.addMessage(MessageType.INFORMATIONAL, e.getMessage(), requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_ADD_VALUE_INVALID);
				}
			}
			catch( NotFoundException e ){
				Html.addMessage(MessageType.WARNING, e.getMessage(), requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED );
			}
			catch( InsufficientPermissionException e ){
				Html.addMessage(MessageType.WARNING, "You do not have permission to update the rule", requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED );
			}
			
		}
		
		//	 1.3 -- Edit a header rule
		else if( action.matches( "EditHeaderRule" ) ){

			if( requestDescriptor.request.getParameter("Submit") == null && requestDescriptor.request.getParameter("AutoPopulate") == null )
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE );
			
			// 1.3.1 -- Get the attributes
			long headerRuleId = -1;
			String headerName;
			int headerNameType = HttpHeaderScanResult.RULE_TYPE_STRING;
			String headerValue;
			int headerValueType = HttpHeaderScanResult.RULE_TYPE_STRING;
			int matchAction = HttpStaticScanRule.MUST_MATCH;
			
			//	 1.3.1.1 -- Get the rule ID
			try{
				headerRuleId = Long.parseLong( requestDescriptor.request.getParameter("HeaderRuleID") );
			}
			catch( NumberFormatException e ){
				Html.addMessage(MessageType.WARNING, "A rule identifier was not provided", requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED );
			}
			
			//	 1.3.1.2 -- Get the header name
			if( requestDescriptor.request.getParameter("HeaderName") != null )
				headerName = requestDescriptor.request.getParameter("HeaderName");
			else{
				Html.addMessage(MessageType.WARNING, "The header name is invalid", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_EDIT_NAME_INVALID );
			}
			
			//	 1.3.1.3 -- Get the header name type
			if( requestDescriptor.request.getParameter("HeaderValueType") == null || "Regex".matches( requestDescriptor.request.getParameter("HeaderValueType") ) ){
				headerValueType = HttpHeaderScanResult.RULE_TYPE_REGEX;
			}
			else{
				headerValueType = HttpHeaderScanResult.RULE_TYPE_STRING;
			}
			
			//	 1.3.1.4 -- Get the header value
			if( requestDescriptor.request.getParameter("HeaderValue") != null )
				headerValue = requestDescriptor.request.getParameter("HeaderValue");
			else{
				Html.addMessage(MessageType.WARNING, "The header value is invalid", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_EDIT_VALUE_INVALID);
			}
			
			//	 1.3.1.5 -- Get the header value type
			if( requestDescriptor.request.getParameter("HeaderNameType") == null || "Regex".matches( requestDescriptor.request.getParameter("HeaderNameType") ) )
				headerNameType = HttpHeaderScanResult.RULE_TYPE_REGEX;
			else
				headerNameType = HttpHeaderScanResult.RULE_TYPE_STRING;
			
			//	 1.3.1.6 -- Get the rule action
			if( requestDescriptor.request.getParameter("MatchAction") == null || "MustMatch".matches( requestDescriptor.request.getParameter("MatchAction") ) )
				matchAction = HttpStaticScanRule.MUST_MATCH;
			else
				matchAction = HttpStaticScanRule.MUST_NOT_MATCH;
			
			try{
				if( scanData.updateHttpHeaderRule( requestDescriptor.sessionIdentifier, headerRuleId, headerName, headerNameType, headerValue, headerValueType, matchAction ) ){
					Html.addMessage(MessageType.INFORMATIONAL, "The header rule was updated successfully", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
				}
				else{
					Html.addMessage(MessageType.WARNING, "The header rule was not updated successfully", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED );
				}
			}
			catch( InputValidationException e ){
				if( e.getFieldDescription().matches("HeaderName") ){
					Html.addMessage(MessageType.WARNING, e.getMessage(), requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_EDIT_NAME_INVALID );
				}
				else{
					Html.addMessage(MessageType.WARNING, e.getMessage(), requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_EDIT_VALUE_INVALID );
				}
			}
			catch( NotFoundException e ){
				Html.addMessage(MessageType.WARNING, "The header rule was not updated because the rule could not be found", requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED );
			}
			catch( InsufficientPermissionException e ){
				Html.addMessage(MessageType.WARNING, "You do not have permission to update rules", requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED );
			}
		}
		// 1.4 -- Delete the header rule
		/*else if( action.matches( "DeleteHeaderRule" ) ){
			long headerRuleId = -1;
			try{
				headerRuleId = Long.parseLong( requestDescriptor.request.getParameter("HeaderRuleID") );
			}
			catch( NumberFormatException e ){
				return new ActionDescriptor( ActionDescriptor.OP_DELETE_FAILED, GenericHtmlGenerator.getWarningDialog("Invalid Input", "A valid header rule identifier was not provided") );
			}
			
			try{
				if( scanData.deleteHttpHeaderRule( requestDescriptor.sessionIdentifier, headerRuleId ) )
					return new ActionDescriptor( ActionDescriptor.OP_DELETE_SUCCESS, GenericHtmlGenerator.getInfoNote("The header rule was successfully deleted") );
				else
					return new ActionDescriptor( ActionDescriptor.OP_DELETE_FAILED, GenericHtmlGenerator.getWarningNote("The header rule was not successfully deleted") );
				
			} catch (NotFoundException e){
				return new ActionDescriptor( ActionDescriptor.OP_DELETE_FAILED, GenericHtmlGenerator.getWarningNote("The header rule was not successfully deleted") );
			}
		}*/
		else{ //if( action.matches("EditHeaderRule")){
			return new ActionDescriptor( ActionDescriptor.OP_VIEW);
		}
		
	}
	
	
}
