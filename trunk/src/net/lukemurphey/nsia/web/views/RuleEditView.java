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
import net.lukemurphey.nsia.scan.ServiceScanRule;
import net.lukemurphey.nsia.scan.ScanRule.ScanRuleLoadFailureException;
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

public class RuleEditView extends View {

	public static final String VIEW_NAME = "rule_editor";
	
	public RuleEditView() {
		super("Rule", VIEW_NAME, Pattern.compile("New|Edit", Pattern.CASE_INSENSITIVE), Pattern.compile("[0-9]*"));
	}

	public static String getURL( int ruleID ) throws URLInvalidException{
		RuleEditView view = new RuleEditView();
		
		return view.createURL("Edit", ruleID);
	}
	
	public static String getURL( long ruleID ) throws URLInvalidException{
		RuleEditView view = new RuleEditView();
		
		return view.createURL("Edit", ruleID);
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
					Dialog.getDialog(response, context, data, "No rule was found with the given ID", "Rule Not Found", DialogType.WARNING);
					return true;
				} catch(NotFoundException e){
					Dialog.getDialog(response, context, data, "No rule was found with the given ID", "Rule Not Found", DialogType.WARNING);
					return true;
				}

				if( rule == null ){
					Dialog.getDialog(response, context, data, "No rule was found with the given ID", "Rule Not Found", DialogType.WARNING);
					return true;
				}

				data.put("rule", rule);
			}

			// 2 -- Get the site group

			//	 2.1 -- Determine the site group ID
			if( rule != null ){
				try{
					siteGroupID = ScanRule.getAssociatedSiteGroup(rule.getRuleId());
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
					Shortcuts.addDashboardHeaders(request, response, data);
					Dialog.getDialog(response, context, data, "The SiteGroup ID provided is not valid", "SiteGroup ID Invalid", DialogType.WARNING, new Link("Return to the Main Dashboard", MainDashboardView.getURL()));
					return true;
				}
			}
			data.put("siteGroupID", siteGroupID);

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
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		} catch (InputValidationException e) {
			throw new ViewFailedException(e);
		}
		
		
		// Get the scan frequency value and units
		int scanFrequencyUnits = 0;
		int scanFrequencyValue = 0;
		
		if( request.getParameter("ScanFrequencyUnits") != null && request.getParameter("ScanFrequencyValue") != null ){
			try{
				scanFrequencyUnits = Integer.parseInt(request.getParameter("ScanFrequencyUnits"));
				scanFrequencyValue = Integer.parseInt(request.getParameter("ScanFrequencyValue"));
			}
			catch(NumberFormatException e){
				scanFrequencyUnits = 0;
				scanFrequencyValue = 0;
			}
		}
		else if( (scanFrequencyUnits == 0 || scanFrequencyValue == 0) && rule != null){
			int frequency = rule.getScanFrequency();
			if( frequency == 0){
				scanFrequencyUnits = 86400;
				scanFrequencyValue = 1;
			}
			else if( ( frequency % 86400) == 0){ //Days
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
		
		data.put("scanFrequencyUnits", scanFrequencyUnits);
		data.put("scanFrequencyValue", scanFrequencyValue);

		// 3 -- Get the menu
		Vector<Link> menu = new Vector<Link>();
		menu.add( new Link("Site Groups") );
		//menu.add( new Link("Add Group", StandardViewList.getURL(SiteGroupEditView.VIEW_NAME, "New")) );
		menu.add( new Link("Edit", StandardViewList.getURL(SiteGroupEditView.VIEW_NAME, "Edit", siteGroup.getGroupId())) );
		menu.add( new Link("New", StandardViewList.getURL(SiteGroupEditView.VIEW_NAME, "New")) );
		menu.add( new Link("Scan Now", "ADDURL") );
		menu.add( new Link("View Exceptions", "ADDURL") );

		data.put("menu", menu);

		// 4 -- Get the breadcrumbs
		Vector<Link> breadcrumbs = new Vector<Link>();
		breadcrumbs.add(  new Link("Main Dashboard", MainDashboardView.getURL()) );
		breadcrumbs.add(  new Link("Site Group: " + siteGroup.getGroupName(), SiteGroupView.getURL(siteGroupID)) );

		if( rule != null ){
			breadcrumbs.add( new Link("Edit Rule", createURL("Edit", rule.getRuleId())) );
			data.put("title", "Rule: " + rule.getRuleType());
		}
		else{
			breadcrumbs.add(  new Link("New Rule", createURL("New") + "?SiteGroupID=" + siteGroupID) );
			data.put("title", "New Rule");
		}
		data.put("breadcrumbs", breadcrumbs);

		//Get the dashboard headers
		Shortcuts.addDashboardHeaders(request, response, data);
		
		if( "Cancel".equalsIgnoreCase( request.getParameter("Action") ) ){
			response.sendRedirect( SiteGroupView.getURL(siteGroupID) );
			return true;
		}
		
		// 5 -- Get the view associated with the rule
		String ruleType = HttpSeekingScanRule.RULE_TYPE;
		
		//	 5.1 -- Get the rule type
		if( rule != null ){
			ruleType = rule.getRuleType();
		}
		else{
			ruleType = request.getParameter("RuleType");
		}
		
		//	 5.2 -- Show the selection form if no rule type was specified
		if( ruleType == null ){
			TemplateLoader.renderToResponse("SelectRule.ftl", data, response);
			return true;
		}
		
		//	 5.3 -- Show the view associated with the rule
		if( ruleType.equals( HttpSeekingScanRule.RULE_TYPE ) ){
			WebDiscoveryRuleEditView view = new WebDiscoveryRuleEditView();
			return view.process(request, response, context, args, data);
		}
		else if( ruleType.equals( ServiceScanRule.RULE_TYPE ) ){
			ServiceScanRuleEditView view = new ServiceScanRuleEditView();
			return view.process(request, response, context, args, data);
		}
		else{
			Dialog.getDialog(response, context, data, "The rule type provided is not recognized", "Rule Type Not Recognized", DialogType.WARNING, new Link("Select Rule Type", createURL("New") + "?SiteGroupID=" + siteGroupID) );
		}
		
		return true;
	}

}
