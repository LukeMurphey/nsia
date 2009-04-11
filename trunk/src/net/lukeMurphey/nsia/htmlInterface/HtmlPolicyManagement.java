package net.lukeMurphey.nsia.htmlInterface;

import java.io.IOException;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.InsufficientPermissionException;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.NotFoundException;
import net.lukeMurphey.nsia.SiteGroupManagement.SiteGroupDescriptor;
import net.lukeMurphey.nsia.htmlInterface.Html.MessageType;
import net.lukeMurphey.nsia.scanRules.DefinitionPolicyDescriptor;
import net.lukeMurphey.nsia.scanRules.DefinitionPolicySet;
import net.lukeMurphey.nsia.scanRules.DefinitionSet.DefinitionCategory;
import net.lukeMurphey.nsia.scanRules.DefinitionPolicyDescriptor.DefinitionPolicyAction;
import net.lukeMurphey.nsia.trustBoundary.ApiDefinitionPolicyManagement;
import net.lukeMurphey.nsia.trustBoundary.ApiDefinitionSet;
import net.lukeMurphey.nsia.trustBoundary.ApiSiteGroupManagement;

public class HtmlPolicyManagement extends HtmlContentProvider {

	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, NotFoundException{
		return getHtml( requestDescriptor, null );
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, NotFoundException{
		StringBuffer body = new StringBuffer();
		
		// 1 -- Perform any pending actions
		if( actionDesc == null ){
			performAction( requestDescriptor );
		}
		
		
		// 2 -- Get the site group identifier
		int siteGroupId = -1;
		
		if(  requestDescriptor.request.getParameter("SiteGroupID") != null ){
			try{
				siteGroupId = Integer.valueOf( requestDescriptor.request.getParameter("SiteGroupID") );
			}
			catch(NumberFormatException e){
				throw new InvalidHtmlParameterException("Invalid Site Group Identifier", "The Site-Group identifier provided is not a valid number", null);
			}
		}
		
		
		// 3 -- Redirect if necessary
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
		
		
		// 4 -- Render the content
		body.append( Html.renderMessages(requestDescriptor.userId) );
		body.append( getCategoryList(requestDescriptor, siteGroupId, true) );
		
		
		// 5 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		
		if( siteGroupId >= 0 ){
			navPath.addPathEntry( "Site Group", "/SiteGroup?SiteGroupID=" + siteGroupId );
			navPath.addPathEntry( "Scan Policy", "/ScanPolicy?SiteGroupID=" + siteGroupId );
		}
		else{
			navPath.addPathEntry( "Scan Policy", "/ScanPolicy" );
		}
		
		String navigationHtml = Html.getNavigationPath( navPath );
		
		
		// 6 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
				
		menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Site Groups", "/", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add Site Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );
		if( siteGroupId >= 0 ){
			menuItems.add( new MenuItem("Add Exception", "ExceptionManagement?Action=New&SiteGroupID=" + siteGroupId, MenuItem.LEVEL_TWO) );
		}
		
		menuItems.add( new MenuItem("User Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Users", "/UserManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New User", "/UserManagement?Action=Add", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Logged in Users", "/Sessions", MenuItem.LEVEL_TWO) );
				
		menuItems.add( new MenuItem("Group Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Groups", "/GroupManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New Group", "/GroupManagement?Action=Add", MenuItem.LEVEL_TWO) );
		
		String menuOutput = Html.getMenu( menuItems );
		
		
		// 7 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor( "Scan Policy Management", pageOutput );
	}
	
	
	private static String getCategoryList( WebConsoleConnectionDescriptor requestDescriptor, int siteGroupID, boolean showSubCategories ) throws GeneralizedException, NoSessionException, NotFoundException{
		StringBuffer body = new StringBuffer();
		
		ApiDefinitionPolicyManagement filterList = new ApiDefinitionPolicyManagement(Application.getApplication());
		ApiDefinitionSet signatureSet = new ApiDefinitionSet( Application.getApplication() );
		
		if( siteGroupID >= 0){
			try {
				ApiSiteGroupManagement groupManagement = new ApiSiteGroupManagement(Application.getApplication());
				
				SiteGroupDescriptor desc = groupManagement.getGroupDescriptor(requestDescriptor.sessionIdentifier, siteGroupID);
				body.append( Html.getSectionHeader( "Scan Policy Management", "Viewing scan policy for Site Group \"" + desc.getGroupName() + "\"") );
			} catch (InsufficientPermissionException e) {
				body.append( Html.getSectionHeader( "Scan Policy Management", "Viewing scan policy for Site Group " + siteGroupID) );
			}
		}
		else{
			body.append( Html.getSectionHeader( "Scan Policy Management", "Viewing global scan policy (default settings for Site Groups)") );
		}
		
		try{
			
			DefinitionPolicySet ruleFilterSet = null;
			
			// 1 -- Display the results (if successfully loaded)
			body.append("<form method=\"post\" action=\"ScanPolicy\">");
			
			if( siteGroupID >= 0){
				body.append("<input type=\"hidden\" name=\"SiteGroupID\" value=\"" + siteGroupID + "\">");
				ruleFilterSet = filterList.getPolicySet(requestDescriptor.sessionIdentifier, siteGroupID);
			}
			else{
				ruleFilterSet = filterList.getPolicySet(requestDescriptor.sessionIdentifier);
			}
			
			if( showSubCategories == false ){
				String[] categories = signatureSet.getCategories(requestDescriptor.sessionIdentifier);
				body.append( "<table class=\"DataTable\" width=\"80%\" summary=\"Definition categories\"><thead><tr><td colspan=\"99\">Category</td></tr></thead>" );
				
				for( int c = 0; c < categories.length; c++){
					//body.append( createPolicyRow(ruleFilterSet.isFiltered(siteGroupID, -1, null, categories., ruleSubCategory, null)) );
					//body.append( createPolicyRow( categories[c], null, null, false) );
					body.append( createPolicyRow( categories[c], null, null, ruleFilterSet, siteGroupID) );
				}
				
				if( categories.length == 0 ){
					body.append( Html.getDialog("No definitions exist yet. Definitions must exist for a scan policy to be defined <p><a href=\"/Definitions\">[View Definitions]</a>", "No Definitions Exist", "/32_Information", false) );
				}
			}
			else{
				DefinitionCategory[] subCategories = signatureSet.getSubCategories(requestDescriptor.sessionIdentifier);
				body.append( "<table class=\"DataTable\" width=\"90%\" summary=\"Definition Categories\"><thead><tr><td colspan=\"99\">Category</td></tr></thead>" );
				
				for( int c = 0; c < subCategories.length; c++){
					//body.append( createPolicyRow( subCategories[c].getCategory(), subCategories[c].getSubCategory(), null, false) );
					body.append( createPolicyRow( subCategories[c].getCategory(), subCategories[c].getSubCategory(), null, ruleFilterSet, siteGroupID) );
				}
				
				if( subCategories.length == 0 ){
					body.append( Html.getDialog("No definitions exist yet. Definitions must exist for a scan policy to be defined <p><a href=\"/Definitions\">[View Definitions]</a>", "No Definitions Exist", "/32_Information", false) );
				}
			}
			
			body.append("<tr class=\"lastRow\"><td colspan=\"99\">");
			body.append("<input type=\"submit\" class=\"button\" name=\"Action\" value=\"Disable\">&nbsp;<input type=\"submit\" class=\"button\" name=\"Action\" value=\"Enable\">");
			
			if( siteGroupID > -1){
				body.append("&nbsp;<input type=\"submit\" class=\"button\" name=\"Action\" value=\"Set Default\">");
			}
			
			body.append("</td></tr>");
			
			body.append( "</table>" );
			body.append( "</form>" );
		}
		/*catch(NotFoundException e){
			body.append( Html.getDialog("No site group exists with the given identifier", "Site Group Not Found", "/32_Warning", false) );
		}*/
		catch(InsufficientPermissionException e){
			body.append( "<p/>&nbsp;" );
			body.append( Html.getWarningDialog( "Insufficient Permission", "You do not have permission to view the entries for the given site group.", "/", "Return to Main Dashboard" ) );
		}
			
			return body.toString();
	}
	
	private static String createPolicyRow( String category, String subCategory, String name, DefinitionPolicySet policySet, int siteGroupID ){
		
		StringBuffer buffer = new StringBuffer();
		
		String completeName = "";
		if( category != null ){
			completeName += category;
		}
		if( subCategory != null ){
			completeName += "." + subCategory;
		}
		if( name != null ){
			completeName += "." + name;
		}
		
		buffer.append("<tr>");
		
		// Output the row with the icon indicating if the policy is category is enabled
		boolean isIncluded = !policySet.isFiltered(siteGroupID, -1, name, category, subCategory, null);
		
		if( isIncluded == true ){
			buffer.append("<td width=\"40\" align=\"center\" class=\"StatGreen\"><img src=\"/22_Check\" alt=\"ok\"></td>");
		}
		else{
			buffer.append("<td width=\"40\" align=\"center\" class=\"StatRed\"><img src=\"/22_Alert\" alt=\"alert\"></td>");
		}
		
		// Output the checkbox
		buffer.append("<td width=\"6\"><input type=\"checkbox\" name=\"DefinitionPolicy\" value=\"").append( completeName ).append("\"></td>");
		
		//Output the name
		buffer.append("<td>");
		buffer.append(completeName);
		buffer.append("</td>");
		
		//Output the status
		DefinitionPolicyDescriptor descriptor = policySet.getMatchingPolicy(siteGroupID, -1, name, category, subCategory, null);
		
		String additionalDescription = "";
		
		if( siteGroupID >= 0 && (descriptor == null || descriptor.getSiteGroupID() < 0 ) ){
			additionalDescription = " (inherited from <a href=\"/ScanPolicy\">default policy</a>)";
		}
		
		if( isIncluded == true ){
			
			buffer.append("<td>Enabled").append(additionalDescription).append("</td>");
		}
		else{
			buffer.append("<td>Disabled").append(additionalDescription).append("</td>");
		}
		
		//Output the buttons to enable/disable the definition category
		/*
		buffer.append("<td>");
		if( isIncluded == true ){
			if( siteGroupID >= 0 ){
				buffer.append( Html.getButton("16_Configure", "Disable", "/ScanPolicy?Action=Disable&SiteGroupID=" + siteGroupID + "&DefinitionPolicy=" + completeName, "Disable") );
			}
			else{
				buffer.append( Html.getButton("16_Configure", "Disable", "/ScanPolicy?Action=Disable&DefinitionPolicy=" + completeName, "Disable") );
			}
		}
		else{
			if( siteGroupID >= 0 ){
				buffer.append( Html.getButton("16_Configure", "Enable", "/ScanPolicy?Action=Enable&SiteGroupID=" + siteGroupID + "&DefinitionPolicy=" + completeName, "Enable") );
			}
			else{
				buffer.append( Html.getButton("16_Configure", "Enable", "/ScanPolicy?Action=Enable&DefinitionPolicy=" + completeName, "Enable") );
			}
		}
		buffer.append("</td></tr>");*/
		buffer.append("</tr>");
		return buffer.toString();
	}
	
	private static ActionDescriptor performAction(WebConsoleConnectionDescriptor requestDescriptor) throws InvalidHtmlParameterException, GeneralizedException, NoSessionException, NotFoundException{

		String action = requestDescriptor.request.getParameter("Action");

		String[] policies = requestDescriptor.request.getParameterValues("DefinitionPolicy");
		
		// 0 -- Parse out the necessary data
		int siteGroupID = -1;
		
		if(  requestDescriptor.request.getParameter("SiteGroupID") != null ){
			try{
				siteGroupID = Integer.valueOf( requestDescriptor.request.getParameter("SiteGroupID") );
			}
			catch(NumberFormatException e){
				throw new InvalidHtmlParameterException("Invalid Site Group Identifier", "The Site-Group identifier provided is not a valid number", null);
			}
		}
		
		// 1 -- No action specified
		if( action == null){
			return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
		}
		
		// 2 -- Disable a definition or category of definitions 
		else if( action.equalsIgnoreCase( "Disable" ) ){
			
			// 2.1 -- Make sure some policies were defined
			if( policies == null || policies.length == 0 ){
				Html.addMessage(MessageType.WARNING, "Please select the definition categories to disable", requestDescriptor.userId);
				return new ActionDescriptor(ActionDescriptor.OP_UPDATE_FAILED);
			}
			
			// 2.2 -- Loop through the policies and disable them
			ApiDefinitionPolicyManagement polManagement = new ApiDefinitionPolicyManagement(Application.getApplication());
			
			for(String policy : policies){
				String[] definition = parseDefinition(policy);
				
				String category = definition[0];
				String subCategory = definition[1];
				
				try {
					if( siteGroupID >= 0 ){
						polManagement.addSubCategoryDescriptor(requestDescriptor.sessionIdentifier, siteGroupID, category, subCategory, DefinitionPolicyAction.EXCLUDE);
					}
					else{
						polManagement.addSubCategoryDescriptor(requestDescriptor.sessionIdentifier, category, subCategory, DefinitionPolicyAction.EXCLUDE);
					}
				} catch (InsufficientPermissionException e) {
					Html.addMessage(MessageType.WARNING, "You don't have permissions to edit the scan policy", requestDescriptor.userId);
					return new ActionDescriptor(ActionDescriptor.OP_UPDATE_FAILED);
				}
			}
		}
		
		// 3 -- Enable a definition or category of definitions 
		else if( action.equalsIgnoreCase( "Enable" ) ){
			
			// 3.1 -- Make sure some policies were defined
			if( policies == null || policies.length == 0 ){
				Html.addMessage(MessageType.WARNING, "Please select the definition categories to enable", requestDescriptor.userId);
				return new ActionDescriptor(ActionDescriptor.OP_UPDATE_FAILED);
			}
			
			// 3.2 -- Loop through the policies and enable them
			ApiDefinitionPolicyManagement polManagement = new ApiDefinitionPolicyManagement(Application.getApplication());
			
			for(String policy : policies){
				String[] definition = parseDefinition(policy);
				
				String category = definition[0];
				String subCategory = definition[1];
				
				try {
					if( siteGroupID >= 0 ){
						polManagement.addSubCategoryDescriptor(requestDescriptor.sessionIdentifier, siteGroupID, category, subCategory, DefinitionPolicyAction.INCLUDE);
					}
					else{
						polManagement.addSubCategoryDescriptor(requestDescriptor.sessionIdentifier, category, subCategory, DefinitionPolicyAction.INCLUDE);
					}
				} catch (InsufficientPermissionException e) {
					Html.addMessage(MessageType.WARNING, "You don't have permissions to edit the scan policy", requestDescriptor.userId);
					return new ActionDescriptor(ActionDescriptor.OP_UPDATE_FAILED);
				}
			}
		}
		
		// 4 -- Clear the policy definition (inherit the default setting)  
		else if( action.equalsIgnoreCase( "Set Default" ) && siteGroupID > -1 ){
			
			// 3.1 -- Make sure some policies were defined
			if( policies == null || policies.length == 0 ){
				Html.addMessage(MessageType.WARNING, "Please select the definition categories to modify", requestDescriptor.userId);
				return new ActionDescriptor(ActionDescriptor.OP_UPDATE_FAILED);
			}
			
			// 3.2 -- Loop through the policies and enable them
			ApiDefinitionPolicyManagement polManagement = new ApiDefinitionPolicyManagement(Application.getApplication());
			
			for(String policy : policies){
				String[] definition = parseDefinition(policy);
				
				String category = definition[0];
				String subCategory = definition[1];
				
				try {
					if( siteGroupID >= 0 ){
						polManagement.clearSubCategoryDescriptors(requestDescriptor.sessionIdentifier, siteGroupID, category, subCategory);
					}
					else{
						polManagement.clearCategoryDescriptors(requestDescriptor.sessionIdentifier, siteGroupID, category);
					}
				} catch (InsufficientPermissionException e) {
					Html.addMessage(MessageType.WARNING, "You don't have permissions to edit the scan policy", requestDescriptor.userId);
					return new ActionDescriptor(ActionDescriptor.OP_UPDATE_FAILED);
				}
			}
		}
		

		return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
	}
	
	public static String[] parseDefinition(String def){
		return StringUtils.split(def, ".");
	}
}
