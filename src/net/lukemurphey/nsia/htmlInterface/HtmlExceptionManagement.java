package net.lukemurphey.nsia.htmlInterface;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.htmlInterface.Html.MessageType;
import net.lukemurphey.nsia.scan.Definition;
import net.lukemurphey.nsia.scan.DefinitionPolicyDescriptor;
import net.lukemurphey.nsia.scan.DefinitionPolicySet;
import net.lukemurphey.nsia.scan.InvalidDefinitionException;
import net.lukemurphey.nsia.scan.DefinitionPolicyDescriptor.DefinitionPolicyAction;
import net.lukemurphey.nsia.scan.DefinitionPolicyDescriptor.DefinitionPolicyType;
import net.lukemurphey.nsia.trustBoundary.ApiDefinitionPolicyManagement;
import net.lukemurphey.nsia.trustBoundary.ApiDefinitionSet;
import net.lukemurphey.nsia.trustBoundary.ApiScanData;

public class HtmlExceptionManagement extends HtmlContentProvider {

	private static final String tableStart = "<table width=\"640\" class=\"DataTable\" summary=\"\"><thead><tr>" +
	"<td colspan=\"2\"><span class=\"Text_3\">Type</span></td>" +
	"<td><span class=\"Text_3\">Exception</span></td>" +
	"<td><span class=\"Text_3\">Options</span></td></tr></thead><tbody>";
	
	private static final String tableEnd = "</tbody></table></form>";
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, NotFoundException{
		return getHtml( requestDescriptor, null );
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, NotFoundException{
		
		StringBuffer body = new StringBuffer();
		
		if( actionDesc == null ){
			actionDesc = performAction( requestDescriptor );
		}
		
		// 1 -- Create the page content
		int definitionID = -1;
		int siteGroupId = -1;
		int ruleId = -1;
		
		//	1.1 -- Get the definition identifier
		try{
			definitionID = Integer.valueOf( requestDescriptor.request.getParameter("DefinitionID") );
		}
		catch(NumberFormatException e){
			//return HtmlProblemDialog.getHtml(requestDescriptor, "Invalid Parameter", "The rule identifier provided is invalid", HtmlProblemDialog.DIALOG_WARNING, "Console", "Main Dashboard");
		}
		
		//	1.2 -- Get the site group identifier
		try{
			siteGroupId = Integer.valueOf( requestDescriptor.request.getParameter("SiteGroupID") );
		}
		catch(NumberFormatException e){
			throw new InvalidHtmlParameterException("Invalid Site Group Identifier", "The site group identifier provided is invalid", null);
		}
		
		//	1.3 -- Get the rule identifier
		try{
			ruleId = Integer.valueOf( requestDescriptor.request.getParameter("RuleID") );
		}
		catch(NumberFormatException e){
			throw new InvalidHtmlParameterException("Invalid Rule Identifier", "The rule identifier provided is invalid", null);
		}
		
		
		// 2 -- Display the relevant form
		if( actionDesc.result == ActionDescriptor.OP_ADD || actionDesc.result == ActionDescriptor.OP_ADD_FAILED ){
			body.append(Html.renderMessages(requestDescriptor.userId));
			body.append( getSelectFilterTypeForm(requestDescriptor, definitionID, requestDescriptor.request.getParameter("DefinitionName"), siteGroupId, ruleId) );
		}
		else{
			String returnTo = requestDescriptor.request.getParameter("ReturnTo");
			if( returnTo != null ){
				try{
					requestDescriptor.response.sendRedirect(returnTo);
					return new ContentDescriptor("Redirecting", "Redirecting to <a href=\"" + returnTo + "\">" + returnTo + "</a>");
				}
				catch(IOException e){
					//If the redirect fails, just display the list of exceptions
				}
			}
			
			body.append(Html.renderMessages(requestDescriptor.userId));
			
			try{
				body.append( getFilterList(requestDescriptor, siteGroupId, ruleId) );
			}
			catch(InsufficientPermissionException e){
				body.append( Html.getWarningDialog( "Insufficient Permission", "You do not have permission to view the exceptions associated with the given rule" ) );
			}
		}
		
		
		// 3 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "Site Group", "/SiteGroup?SiteGroupID=" + siteGroupId );
		navPath.addPathEntry( "Scan Rule History", "/ScanResult?RuleID=" + ruleId);
		navPath.addPathEntry( "Exception Management", "/ExceptionManagement?SiteGroupID=" + siteGroupId + "&RuleID=" + ruleId);

		String navigationHtml = Html.getNavigationPath( navPath );
		
		
		// 4 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
				
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
		
		
		// 5 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor( "Exception Management", pageOutput );
	}
	
	private static String getFilterList( WebConsoleConnectionDescriptor requestDescriptor, int siteGroupID, int ruleID ) throws GeneralizedException, NoSessionException, InsufficientPermissionException, NotFoundException{
		StringBuffer body = new StringBuffer();
		
		ApiDefinitionPolicyManagement filterList = new ApiDefinitionPolicyManagement(Application.getApplication());
		body.append( Html.getSectionHeader( "Exception Management", null) );
		
		// 1 -- Get the site group ID if not provided
		if( siteGroupID < 0 ){
			ApiScanData scanData = new ApiScanData(Application.getApplication());
			siteGroupID = (int)scanData.getAssociatedSiteGroup(requestDescriptor.sessionIdentifier, ruleID);
		}
		
		//2 -- Create the Form
		try{
			DefinitionPolicySet ruleFilterSet = filterList.getPolicySet(requestDescriptor.sessionIdentifier, siteGroupID, ruleID);

			//	 2.1 -- Display the results (if successfully loaded)
			if( ruleFilterSet == null || ruleFilterSet.size() == 0 ){
				body.append( Html.getDialog("No exception entries have been defined yet <p><a href=\"/SiteGroup?SiteGroupID=" + siteGroupID + "\">[Return to Site Group View]</a>", "No Exceptions Defined", "/32_Information", false) );
			}
			else{
				body.append("<form method=\"get\" action=\"ExceptionManagement\">");
				body.append("<input type=\"hidden\" name=\"SiteGroupID\" value=\"" + siteGroupID + "\">");
				body.append("<input type=\"hidden\" name=\"RuleID\" value=\"" + ruleID + "\">");
				
				body.append( tableStart );
				for( int c = 0; c < ruleFilterSet.size(); c++){
					body.append( createRow(ruleFilterSet.get(c), ruleID, siteGroupID) );
				}
				
				body.append("<tr class=\"lastRow\"><td colspan=\"99\">");
				body.append("<input type=\"submit\" class=\"button\" name=\"Action\" value=\"Delete\"></td></tr>");
				
				body.append( tableEnd );
				body.append( "</form>" );
			}
		}catch(NotFoundException e){
			body.append( Html.getWarningDialog("Site Group Not Found", "No site group exists with the given identifier" ) );
		}catch(InsufficientPermissionException e){
			body.append( Html.getWarningDialog( "Insufficient Permission", "You do not have permission to view the entries for the given site group." ) );
		}
			
			return body.toString();
	}
	
	private static String getSelectFilterTypeForm( WebConsoleConnectionDescriptor connectionDescriptor, int definitionID, String definitionName, long siteGroupID, long ruleID) throws GeneralizedException, NoSessionException {
		
		ApiDefinitionSet signatureSet = new ApiDefinitionSet(Application.getApplication());
		Definition definition = null;
		
		if( definitionName == null ){
			try{
				definition =  signatureSet.getDefinition(connectionDescriptor.sessionIdentifier, definitionID);
			}
			catch(NotFoundException e){
				return Html.getWarningDialog( "Definition Not Found", "A definition was not found with the given identifier" );
			} catch (InsufficientPermissionException e) {
				return Html.getWarningDialog( "Insufficient Permission", "You do not have permission to perform this operation." );
			}
		}
		else {
			try{
				definition =  signatureSet.getDefinition(connectionDescriptor.sessionIdentifier, definitionName);
			}catch(NotFoundException e){
				return Html.getWarningDialog( "Definition Not Found", "A definition was not found with the given identifier");
			} catch (InsufficientPermissionException e) {
				return Html.getWarningDialog( "Insufficient Permission", "You do not have permission to perform this operation.");
			}
		}
		
		StringBuffer body = new StringBuffer();
		
		body.append("<span class=\"Text_1\">Definition Exception</span><p>");
		
		body.append("<form action=\"ExceptionManagement\" method=\"get\">");
		
		body.append("<input type=\"hidden\" name=\"SiteGroupID\" value=\"" + siteGroupID + "\">");
		if( definitionName == null ){
			body.append("<input type=\"hidden\" name=\"DefinitionID\" value=\"" + definitionID + "\">");
		}
		else{
			body.append("<input type=\"hidden\" name=\"DefinitionName\" value=\"" + definitionName + "\">");
		}
		
		body.append("<input type=\"hidden\" name=\"RuleID\" value=\"" + ruleID + "\">");
		
		if( connectionDescriptor.request.getParameter("URL") != null ){
			body.append("<input type=\"hidden\" name=\"URL\" value=\"" + connectionDescriptor.request.getParameter("URL") + "\">");
		}
		
		if( connectionDescriptor.request.getParameter("ReturnTo") != null ){
			body.append("<input type=\"hidden\" name=\"ReturnTo\" value=\"" + connectionDescriptor.request.getParameter("ReturnTo") + "\">");
		}
		
		body.append("<input type=\"hidden\" name=\"Action\" value=\"New\">");
		
		body.append("<table>");
		
		body.append("<tr><td style=\"vertical-align: top;\" rowspan=\"2\"><input type=\"radio\" name=\"FilterType\" value=\"Definition\"></td><td><span class=\"Text_3\">Filter out definition (" + definition.getFullName() + ")</span></tr>");
		body.append("<tr><td>Filter out findings for this specific definition only<br>&nbsp;</td></tr>");
		
		body.append("<tr><td style=\"vertical-align: top;\" rowspan=\"2\"><input type=\"radio\" name=\"FilterType\" value=\"SubCategory\"></td><td><span class=\"Text_3\">Filter out entire sub-category (" + definition.getCategoryName() + "." + definition.getSubCategoryName() + ")</span></tr>");
		body.append("<tr><td>Filter out findings for this entire sub-category (all definitions within the sub-category)<br>&nbsp;</td></tr>");
		
		body.append("<tr><td style=\"vertical-align: top;\" rowspan=\"2\"><input type=\"radio\" name=\"FilterType\" value=\"Category\"></td><td><span class=\"Text_3\">Filter out category (" + definition.getCategoryName() + ")</span></tr>");
		body.append("<tr><td>Filter out findings for this specific definition<br>&nbsp;</td></tr>");
		
		body.append("<tr><td>&nbsp;</td><td><input class=\"button\" type=\"submit\" value=\"Add Exception\" name=\"Add Exception\">&nbsp;&nbsp;<input class=\"button\" type=\"submit\" value=\"Cancel\" name=\"Cancel\"></td></tr>");
		
		body.append("</table>");
		body.append("</form>");	
		
		return body.toString();
	}
	
	private static String createRow( DefinitionPolicyDescriptor filter, int ruleID, int siteGroupID ){
		
		if( filter.getAction() == DefinitionPolicyAction.INCLUDE ){
			return ""; //Don't bother showing policies that are inclusive
		}
		
		StringBuffer buffer = new StringBuffer();
		
		buffer.append("<tr><td width=\"6\"><input type=\"checkbox\" name=\"ExceptionID\" value=\"").append( filter.getPolicyID() ).append("\"></td>");
		
		if( filter.getPolicyType() == DefinitionPolicyDescriptor.DefinitionPolicyType.CATEGORY ){
			buffer.append("<td>Category</td><td>").append( filter.getDefinitionCategory() ).append(".*");
		}
		else if( filter.getPolicyType() == DefinitionPolicyDescriptor.DefinitionPolicyType.SUBCATEGORY ){
			buffer.append("<td>Sub-Category</td><td>").append( filter.getDefinitionCategory() + "." + filter.getDefinitionSubCategory() ).append(".*");
		}
		else if( filter.getPolicyType() == DefinitionPolicyDescriptor.DefinitionPolicyType.NAME ){
			buffer.append("<td>Definition</td><td>").append( filter.getDefinitionCategory() + "." + filter.getDefinitionSubCategory() + "." + filter.getDefinitionName() );
		}
		else if( filter.getPolicyType() == DefinitionPolicyDescriptor.DefinitionPolicyType.URL ){
			buffer.append("<td>URL</td><td>").append( filter.getURL() );
		}
		
		if( filter.getURL() != null ){
			buffer.append(" for <a href=\"" + filter.getURL().toExternalForm()  + "\" title=\"" + filter.getURL().toExternalForm() + "\">" + Html.shortenString( filter.getURL().toExternalForm(), 32)).append("</a>");
		}
		
		buffer.append("</td>");
		
		buffer.append("<td><table><tr><td class=\"imagebutton\"><a href=\"ExceptionManagement?ExceptionID=" + filter.getPolicyID() + "&SiteGroupID=" + siteGroupID +  "&RuleID=" + ruleID + "&Action=Delete\"><img alt=\"Delete\" src=\"/16_Delete\"></a></td><td><a href=\"ExceptionManagement?ExceptionID=" + filter.getPolicyID() + "&SiteGroupID=" + siteGroupID +  "&RuleID=" + ruleID + "&Action=Delete\">Delete</a></td></tr></table></td></tr>");
		
		return buffer.toString();
	}
	
	private static ActionDescriptor performAction(WebConsoleConnectionDescriptor requestDescriptor) throws InvalidHtmlParameterException, GeneralizedException, NoSessionException, NotFoundException{

		String action = requestDescriptor.request.getParameter("Action");

		if( action == null){
			return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
		}

		// 1 -- User is creating a new filter entry
		else if( action.matches( "New" ) ){

			if( requestDescriptor.request.getParameter("Add Exception") != null ){

				try{

					// 1.1 -- Get the definition ID
					int definitionID = -1;
					String definitionName = null;
					ApiDefinitionSet signatureSet = new ApiDefinitionSet(Application.getApplication());
					Definition definition = null;

					if( requestDescriptor.request.getParameter("DefinitionID") != null ) {
						try{
							definitionID = Integer.parseInt(requestDescriptor.request.getParameter("DefinitionID"));
							definition = signatureSet.getDefinition(requestDescriptor.sessionIdentifier, definitionID);
						}
						catch(NumberFormatException e){
							//throw new InvalidHtmlParameterException("Invalid Parameter", "The definition identifier parameter is invalid", "Console");
						}
					}
					else if( requestDescriptor.request.getParameter("DefinitionName") != null ) {
						definitionName = requestDescriptor.request.getParameter("DefinitionName");
						definition = signatureSet.getDefinition(requestDescriptor.sessionIdentifier, definitionName);
					}
					else{
						throw new InvalidHtmlParameterException("Invalid Parameter", "The definition identifier was not provided", "Console");
					}

					// 1.2 -- Get the site group ID
					int siteGroupID;

					if( requestDescriptor.request.getParameter("SiteGroupID") != null ) {
						try{
							siteGroupID = Integer.parseInt(requestDescriptor.request.getParameter("SiteGroupID"));
						}
						catch(NumberFormatException e){
							throw new InvalidHtmlParameterException("Invalid Parameter", "The site group identifier parameter is invalid", "Console");
						}

						if( siteGroupID < 0 ){
							throw new InvalidHtmlParameterException("Invalid Parameter", "The site group identifier parameter is invalid", "Console");
						}
					}
					else{
						throw new InvalidHtmlParameterException("Invalid Parameter", "The site group identifier was not provided", "Console");
					}

					// 1.3 -- Get the rule ID
					int ruleID;

					if( requestDescriptor.request.getParameter("RuleID") != null ) {
						try{
							ruleID = Integer.parseInt(requestDescriptor.request.getParameter("RuleID"));
						}
						catch(NumberFormatException e){
							throw new InvalidHtmlParameterException("Invalid Parameter", "The rule identifier parameter is invalid", "Console");
						}

						if( ruleID < 0 ){
							throw new InvalidHtmlParameterException("Invalid Parameter", "The rule identifier parameter is invalid", "Console");
						}
					}
					else{
						throw new InvalidHtmlParameterException("Invalid Parameter", "The rule identifier was not provided", "Console");
					}
					
					// 1.4 -- Get the requested filter type
					DefinitionPolicyType filterType = DefinitionPolicyType.NAME;
					String filterTypeString = requestDescriptor.request.getParameter("FilterType");

					if( filterTypeString == null ) {
						return new ActionDescriptor(ActionDescriptor.OP_ADD_FAILED);
					}
					else if( "Category".equalsIgnoreCase( filterTypeString ) ) {
						filterType = DefinitionPolicyType.CATEGORY;
					}
					else if( "SubCategory".equalsIgnoreCase( filterTypeString ) ) {
						filterType = DefinitionPolicyType.SUBCATEGORY;
					}
					else if( "Definition".equalsIgnoreCase( filterTypeString ) ) {
						filterType = DefinitionPolicyType.NAME;
					}
					else {
						Html.addMessage(MessageType.WARNING, "The filter type was not specified", requestDescriptor.userId);
						return new ActionDescriptor(ActionDescriptor.OP_ADD_FAILED);
					}


					// 1.4 -- Get the URL restriction (if provided)
					URL urlRestriction = null;

					if( requestDescriptor.request.getParameter("URL") != null ){
						try{
							urlRestriction = new URL(requestDescriptor.request.getParameter("URL"));
						}
						catch(MalformedURLException e){
							Html.addMessage(MessageType.WARNING, "The URL provided is invalid", requestDescriptor.userId);
							return new ActionDescriptor(ActionDescriptor.OP_ADD_FAILED);
						}
					}
					
					// 1.5 -- Try to add the entry
					ApiDefinitionPolicyManagement filters = new ApiDefinitionPolicyManagement(Application.getApplication());

					if(definition == null){
						Html.addMessage(MessageType.WARNING, "A definition to filter was not provided", requestDescriptor.userId);
						return new ActionDescriptor(ActionDescriptor.OP_ADD);
					}
					else if( filterType == DefinitionPolicyType.NAME ){
						
						try{
							if( urlRestriction == null ){
								filters.addDefinitionNameDescriptor(requestDescriptor.sessionIdentifier, siteGroupID, ruleID, definition.getFullName(), DefinitionPolicyAction.EXCLUDE);
							}
							else{
								filters.addDefinitionNameDescriptor(requestDescriptor.sessionIdentifier, siteGroupID, ruleID, definition.getFullName(), urlRestriction, DefinitionPolicyAction.EXCLUDE);
							}
						}
						// Post a warning if the definition name was invalid 
						catch(InvalidDefinitionException e){
							Html.addMessage(MessageType.WARNING, "Exception was not successfully created (definition name is invalid)", requestDescriptor.userId);
							return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
						}
						
						Html.addMessage(MessageType.INFORMATIONAL, "Exception successfully added", requestDescriptor.userId);
						return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
					}
					else if( filterType == DefinitionPolicyType.CATEGORY ){

						if( urlRestriction == null ){
							filters.addCategoryDescriptor(requestDescriptor.sessionIdentifier, siteGroupID, ruleID, definition.getCategoryName(), DefinitionPolicyAction.EXCLUDE );
						}
						else{
							filters.addCategoryDescriptor(requestDescriptor.sessionIdentifier, siteGroupID, ruleID, definition.getCategoryName(), urlRestriction, DefinitionPolicyAction.EXCLUDE );
						}
						Html.addMessage(MessageType.INFORMATIONAL, "Exception successfully added", requestDescriptor.userId);
						return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
					}
					else if( filterType == DefinitionPolicyType.SUBCATEGORY ){

						if( urlRestriction == null ){
							filters.addSubCategoryDescriptor(requestDescriptor.sessionIdentifier, siteGroupID, ruleID, definition.getCategoryName(), definition.getSubCategoryName(), DefinitionPolicyAction.EXCLUDE );
						}
						else{
							filters.addSubCategoryDescriptor(requestDescriptor.sessionIdentifier, siteGroupID, ruleID, definition.getCategoryName(), definition.getSubCategoryName(), urlRestriction, DefinitionPolicyAction.EXCLUDE );
						}
						Html.addMessage(MessageType.INFORMATIONAL, "Exception successfully added", requestDescriptor.userId);
						return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
					}

				}
				catch(InsufficientPermissionException e){
					Html.addMessage(MessageType.WARNING, "You do not permission to create definition exceptions", requestDescriptor.userId);
				}

			}
			else if( requestDescriptor.request.getParameter("Cancel") != null ){
				return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
			}


			return new ActionDescriptor(ActionDescriptor.OP_ADD);
		}

		// 2 -- User is deleting a filter entry
		else if( action.matches( "Delete" ) ){
			
			Vector<Integer> exceptionsToDelete = new Vector<Integer>();
			
			// 2.1 -- Get a list of the exceptions to delete
			{
				String[] identifiers = requestDescriptor.request.getParameterValues("ExceptionID");
				
				// Show a warning if the the user failed to select any exceptions
				if( identifiers == null || identifiers.length == 0 ) {
					Html.addMessage(MessageType.WARNING, "Please select the exceptions to delete", requestDescriptor.userId);
					return new ActionDescriptor(ActionDescriptor.OP_DELETE_FAILED);
				}
				
				// Convert the exceptions to integers
				for (String stringID : identifiers) {
					
					try{
						exceptionsToDelete.add( new Integer( stringID ) );
					}
					catch(NumberFormatException e){
						//throw new InvalidHtmlParameterException("Invalid Parameter", "The definition identifier parameter is invalid", "Console");
					}
				}
			}
			
			// 2.2 -- Delete each exception
			ApiDefinitionPolicyManagement exceptionManagement = new ApiDefinitionPolicyManagement(Application.getApplication());
			
			try{
				int c = 0;
				for(Integer exceptionID : exceptionsToDelete ){
					if( exceptionManagement.deleteDefinitionPolicyDescriptor( requestDescriptor.sessionIdentifier, exceptionID ) == true ){
						c++;
					}
				}
				
				// Create a message indicating that the exceptions have been deleted
				if( c == 1 ){
					Html.addMessage(MessageType.INFORMATIONAL, "Exception successfully deleted", requestDescriptor.userId);
				}
				else{
					Html.addMessage(MessageType.INFORMATIONAL, c + " exceptions successfully deleted", requestDescriptor.userId);
				}
				
				return new ActionDescriptor(ActionDescriptor.OP_DELETE_SUCCESS);
			}
			catch(InsufficientPermissionException e){
				Html.addMessage(MessageType.WARNING, "You do not permission to delete definition exceptions", requestDescriptor.userId);
				return new ActionDescriptor(ActionDescriptor.OP_DELETE_FAILED);
			}
			
			
		}

		return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
	}
	
}
