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
import net.lukemurphey.nsia.scan.HttpSeekingScanRule;
import net.lukemurphey.nsia.scan.ScanRule;
import net.lukemurphey.nsia.scan.ScanRuleLoader;
import net.lukemurphey.nsia.scan.ScanRule.ScanRuleLoadFailureException;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.StandardViewList;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class RuleEditView extends View {

	public static final String VIEW_NAME = "rule_editor";
	
	public RuleEditView() {
		super("Rule", VIEW_NAME, Pattern.compile("New|Edit", Pattern.CASE_INSENSITIVE), Pattern.compile("[0-9]*"));
	}

	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {
		
		ScanRule rule = null;
		int siteGroupID = 1;
		SiteGroupDescriptor siteGroup = null;
		
		try {

			// 0 -- Check permissions
			//TODO Check rights

			// 1 -- Get the rule if it exists
			if( args.length >= 2 ){

				// 1.1 -- Get the rule ID
				int ruleID;

				try{
					ruleID = Integer.valueOf( args[1] );
				}
				catch( NumberFormatException e ){
					Dialog.getDialog(response, context, data, "The Rule ID provided is not valid", "Rule ID Invalid", DialogType.WARNING);
					return true;
				}

				// 1.2 -- Get the rule
				try{
					rule = ScanRuleLoader.getScanRule(ruleID);
				} catch(ScanRuleLoadFailureException e){
					Dialog.getDialog(response, context, data, "No group was found with the given ID", "Group Not Found", DialogType.WARNING);
					return true;
				} catch(NotFoundException e){
					Dialog.getDialog(response, context, data, "No group was found with the given ID", "Group Not Found", DialogType.WARNING);
					return true;
				}

				if( rule == null ){
					Dialog.getDialog(response, context, data, "No rule was found with the given ID", "Rule Not Found", DialogType.WARNING);
					return true;
				}

				data.put("rule", rule);

				// 2 -- Get the site group

				//	 2.1 -- Determine the site group ID
				if( rule != null ){
					try{
						siteGroupID = ScanRule.getAssociatedSiteGroup(ruleID);
					}
					catch(NotFoundException e1){
						Dialog.getDialog(response, context, data, "No SiteGroup exists with the given ID", "SiteGroup Not Found", DialogType.WARNING);
						return true;
					}
				}
				else{
					try{
						siteGroupID = Integer.valueOf( request.getParameter("SiteGroupID") );
					}
					catch(NumberFormatException e1){
						Dialog.getDialog(response, context, data, "The SiteGroup ID provided is not valid", "SiteGroup ID Invalid", DialogType.WARNING);
						return true;
					}
				}

				//	 2.2 -- Get the Site group descriptor
				try{
					SiteGroupManagement mgmr = new SiteGroupManagement(Application.getApplication());
					siteGroup = mgmr.getGroupDescriptor( siteGroupID );
				} catch (NotFoundException e1) {
					Dialog.getDialog(response, context, data, "The SiteGroup ID provided is not valid", "SiteGroup ID Invalid", DialogType.WARNING);
					return true;
				} catch (NumberFormatException e1) {
					Dialog.getDialog(response, context, data, "The SiteGroup ID provided is not valid", "SiteGroup ID Invalid", DialogType.WARNING);
					return true;
				}
			}
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		} catch (InputValidationException e) {
			throw new ViewFailedException(e);
		}

		// 3 -- Get the menu
		Vector<Link> menu = new Vector<Link>();
		menu.add( new Link("Site Groups") );
		//menu.add( new Link("Add Group", StandardViewList.getURL(SiteGroupEditView.VIEW_NAME, "New")) );
		menu.add( new Link("Edit", StandardViewList.getURL(SiteGroupEditView.VIEW_NAME, "Edit", siteGroup.getGroupId())) );
		menu.add( new Link("Delete", StandardViewList.getURL(SiteGroupEditView.VIEW_NAME, "New")) );
		menu.add( new Link("Scan Now", "ADDURL") );
		menu.add( new Link("View Exceptions", "ADDURL") );

		data.put("menu", menu);

		// 4 -- Get the breadcrumbs
		Vector<Link> breadcrumbs = new Vector<Link>();
		breadcrumbs.add(  new Link("Site Group", SiteGroupView.getURL(siteGroupID)) );

		if( rule != null ){
			breadcrumbs.add( new Link("Edit Rule","ADDURL") );
			data.put("title", "Rule: " + rule);
		}
		else{
			breadcrumbs.add(  new Link("New Rule", createURL("New")) );
			data.put("title", "New Rule");
		}
		data.put("breadcrumbs", breadcrumbs);

		//Get the dashboard headers
		Shortcuts.addDashboardHeaders(request, response, data);
		
		// 5 -- Get the view associated with the rule
		String ruleType = HttpSeekingScanRule.RULE_TYPE;
		
		//	 5.1 -- Get the rule type
		if( rule != null ){
			ruleType = rule.getRuleType();
		}
		else{
			request.getParameter("RuleType");
		}
		
		//	 5.2 -- Show the view associated with the rule
		if( ruleType == HttpSeekingScanRule.RULE_TYPE ){
			WebDiscoveryRuleEditView view = new WebDiscoveryRuleEditView();
			return view.process(request, response, context, args, data);
		}
		
		return true;
	}

}
