package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
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
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.StandardViewList;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.templates.TemplateLoader;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class ExceptionListView extends View {

	public static final String VIEW_NAME = "exception_list";
	
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
		
		
		// 0 -- Check permissions
		//TODO Check rights
		
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
		
		//	 1.2 -- Get the exceptions for the rule
		DefinitionPolicyManagement policyMgmt = new DefinitionPolicyManagement(Application.getApplication());
		DefinitionPolicySet policies = null;
		
		try{
			policies = policyMgmt.getPolicySetForRule( ruleID );
			data.put("policies", policies);
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
			Dialog.getDialog(response, context, data, "A SiteGroup with the given identifier could not be found.", "SiteGroup Not Found", DialogType.WARNING);
			return true;
		}
		
		// 3 -- Create the breadcrumbs
		Vector<Link> breadcrumbs = new Vector<Link>();
		breadcrumbs.add(  new Link("Main Dashboard", MainDashboardView.getURL()) );
		breadcrumbs.add(  new Link("Site Group: " + siteGroup.getGroupName(), SiteGroupView.getURL(siteGroup.getGroupId())) );
		breadcrumbs.add(  new Link("Edit Rule", RuleEditView.getURL(ruleID)) );
		breadcrumbs.add(  new Link("Scan History", ScanResultHistoryView.getURL(ruleID)) );
		breadcrumbs.add(  new Link("Exceptions", createURL(ruleID)) );
		data.put("breadcrumbs", breadcrumbs);
		
		// 4 -- Create the menu
		Vector<Link> menu = new Vector<Link>();
		menu.add( new Link("Site Groups") );
		menu.add( new Link("Add Group", StandardViewList.getURL(SiteGroupEditView.VIEW_NAME, "New")) );
		
		menu.add( new Link("User Management") );
		menu.add( new Link("Add New User", UserEditView.getURL()) );
		menu.add( new Link("View Logged in Users",UserSessionsView.getURL()) );
		
		menu.add( new Link("Group Management") );
		menu.add( new Link("Add New Group",  GroupEditView.getURL() ) );
		data.put("menu", menu);
		
		data.put("title", "Exceptions");
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
