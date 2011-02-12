package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.MaxMinCount;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.SiteGroupManagement;
import net.lukemurphey.nsia.SiteGroupManagement.SiteGroupDescriptor;
import net.lukemurphey.nsia.scan.DefinitionPolicyDescriptor;
import net.lukemurphey.nsia.scan.DefinitionPolicyManagement;
import net.lukemurphey.nsia.scan.DefinitionPolicySet;
import net.lukemurphey.nsia.scan.ScanRule;
import net.lukemurphey.nsia.scan.DefinitionPolicyDescriptor.DefinitionPolicyAction;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.Menu;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.templates.TemplateLoader;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class ExceptionListView extends View {

	public static final String VIEW_NAME = "exception_list";
	public static final int EXCEPTIONS_PER_PAGE = 25;
	
	public ExceptionListView() {
		super("Exceptions", VIEW_NAME, Pattern.compile("[0-9]+"));
	}
	
	public static String getURL( int ruleID ) throws URLInvalidException{
		ExceptionListView view = new ExceptionListView();
		
		return view.createURL(ruleID);
	}
	
	public static String getURL( long ruleID ) throws URLInvalidException{
		ExceptionListView view = new ExceptionListView();
		
		return view.createURL(ruleID);
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {
		
		// 1 -- Get the exceptions
		Shortcuts.addDashboardHeaders(request, response, data);
		
		//	 1.1 -- Get the rule ID
		int ruleID;
		
		try{
			ruleID = Integer.valueOf( args[0] );
			data.put("ruleID", ruleID);
		}
		catch(NumberFormatException e ){
			Dialog.getDialog(response, context, data, "The rule ID provided is invalid", "Rule ID invalid", DialogType.WARNING);
			return true;
		}
		
		//	 1.2 -- Get the page number
		int page = 1;
		
		String pageString = request.getParameter("Page");
		
		if( pageString != null ){
			try{
				page = Integer.parseInt(pageString);
			}
			catch( NumberFormatException e){
				// Ignore the fact that the page number was wrong
			}
		}
		
		data.put("page", page);
		
		if( request.getParameter("Search") != null ){
			data.put("search", request.getParameter("Search"));
		}
		
		DefinitionPolicySet policies = null;
		DefinitionPolicyManagement policyMgmt = new DefinitionPolicyManagement(Application.getApplication());
		
		try{
			
			//1.3 -- Get the search text
			String searchText = request.getParameter("Search");
			
			//1.4 -- Get the exceptions for the rule
			if( searchText != null && searchText.trim().length() > 0 ){
				policies = policyMgmt.getPolicySetForRule( ruleID, EXCEPTIONS_PER_PAGE, page, searchText );
			}
			else{
				policies = policyMgmt.getPolicySetForRule( ruleID, EXCEPTIONS_PER_PAGE, page );
			}
			data.put("policies", policies);
			
			//1.5 -- Get information necessary for pagination
			MaxMinCount maxMinCount = policyMgmt.getScanPolicyInfoForRule(ruleID, searchText);
			
			DefinitionPolicyDescriptor policyDesc = null;
			
			// Determine if we are at the beginning of the entries
			if( policies.size() > 0){
				policyDesc = policies.get(0);
				
				if( policyDesc.getPolicyID() > maxMinCount.getMin() ){
					data.put("backEnabled", true);
				}
				else{
					data.put("backEnabled", false);
				}
			}
			else{
				data.put("backEnabled", false);
			}
			
			// Determine if we are at the end of the entries
			if( policies.size() > 0){
				policyDesc = policies.get( policies.size() - 1 );
				
				if( policyDesc.getPolicyID() < maxMinCount.getMax() ){
					data.put("nextEnabled", true);
				}
				else{
					data.put("nextEnabled", false);
				}
			}
			else{
				data.put("nextEnabled", false);
			}
			
		}
		catch(SQLException e){
			throw new ViewFailedException(e);
		}
		catch(NoDatabaseConnectionException e){
			throw new ViewFailedException(e);
		}
		
		// 2 -- Get the SiteGroup
		SiteGroupDescriptor siteGroup = null;
		try {
			int siteGroupID = ScanRule.getSiteGroupForRule(ruleID);
			SiteGroupManagement siteGroupMgmt = new SiteGroupManagement(Application.getApplication());
			siteGroup = siteGroupMgmt.getGroupDescriptor(siteGroupID);
			data.put("siteGroupID", siteGroupID);
			data.put("siteGroup", siteGroup);
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (InputValidationException e) {
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		} catch (NotFoundException e) {
			Dialog.getDialog(response, context, data, "A site-group with the given identifier could not be found.", "Site-group Not Found", DialogType.WARNING);
			return true;
		}
		
		// 3 -- Create the breadcrumbs
		Vector<Link> breadcrumbs = new Vector<Link>();
		breadcrumbs.add( new Link("Main Dashboard", MainDashboardView.getURL()) );
		breadcrumbs.add( new Link("Site Group: " + siteGroup.getGroupName(), SiteGroupView.getURL(siteGroup.getGroupId())) );
		breadcrumbs.add( new Link("Edit Rule", RuleEditView.getURL(ruleID)) );
		breadcrumbs.add( new Link("Scan History", ScanResultHistoryView.getURL(ruleID)) );
		breadcrumbs.add( new Link("Exceptions", createURL(ruleID)) );
		data.put("breadcrumbs", breadcrumbs);
		
		// 4 -- Create the menu
		data.put("menu", Menu.getGenericMenu(context));
		data.put("title", "Exceptions");
		
		// 5 -- Check permissions
		try {
			if( Shortcuts.canRead(context.getSessionInfo(), siteGroup.getObjectId(), "View exceptions for site-group ID " + siteGroup.getGroupId() + " (" + siteGroup.getGroupName() + ")") == false ){
				Shortcuts.getPermissionDeniedDialog(response, data, "You do not have permission to view the exceptions associated with this site-group");
				return true;
			}
		} catch (GeneralizedException e) {
			throw new ViewFailedException(e);
		}
		
		// 6 -- Render the page
		data.put("INCLUDE", DefinitionPolicyAction.INCLUDE);
		data.put("EXCLUDE", DefinitionPolicyAction.EXCLUDE);
		data.put("CATEGORY", DefinitionPolicyDescriptor.DefinitionPolicyType.CATEGORY);
		data.put("NAME", DefinitionPolicyDescriptor.DefinitionPolicyType.NAME);
		data.put("SUBCATEGORY", DefinitionPolicyDescriptor.DefinitionPolicyType.SUBCATEGORY);
		data.put("URL", DefinitionPolicyDescriptor.DefinitionPolicyType.URL);
		
		// 5 -- Render the page
		TemplateLoader.renderToResponse("ExceptionsList.ftl", data, response);
		return true;
	}

}
