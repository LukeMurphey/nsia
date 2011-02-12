package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.SiteGroupManagement;
import net.lukemurphey.nsia.SiteGroupManagement.SiteGroupDescriptor;
import net.lukemurphey.nsia.scan.HttpSeekingScanRule;
import net.lukemurphey.nsia.scan.ScanResult;
import net.lukemurphey.nsia.scan.ScanResultLoader;
import net.lukemurphey.nsia.scan.ScanRule;
import net.lukemurphey.nsia.scan.ScanRuleLoader;
import net.lukemurphey.nsia.scan.ScanRule.ScanResultLoadFailureException;
import net.lukemurphey.nsia.scan.ScanRule.ScanRuleLoadFailureException;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class ScanResultExportView extends View {

	public static final String VIEW_NAME = "scan_result_export";
	
	public ScanResultExportView() {
		super("ScanResult/Export", VIEW_NAME, Pattern.compile("[0-9]+"));
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {

		try{
			// 1 -- Get the scan result
			ScanResult scanResult;
			int scanResultID;
			
			try{
				scanResultID = Integer.valueOf( args[0] );
			}
			catch( NumberFormatException e ){
				Dialog.getDialog(response, context, data, "A scan result with the given ID was not found", "Scan Result Not Found", DialogType.WARNING);
				return true;
			}
			
			scanResult = ScanResultLoader.getScanResult(scanResultID);
			data.put("scanResult", scanResult);
			
			// 2 -- Get the rule associated with the scan result
			ScanRule rule;
			
			try{
				rule = ScanRuleLoader.getScanRule(scanResult.getRuleID());
			}
			catch(NotFoundException e){
				Dialog.getDialog(response, context, data, "The scan rule associated with the given scan result could not be found", "Scan Rule Not Found", DialogType.WARNING);
				return true;
			}
			data.put("rule", rule);
			
			// 3 -- Get the site group associated with the rule
			int siteGroupID;
			SiteGroupDescriptor siteGroup;
			
			try{
				siteGroupID = ScanRule.getAssociatedSiteGroupID(scanResult.getRuleID());
				SiteGroupManagement siteGroupMgmt = new SiteGroupManagement(Application.getApplication());
				siteGroup = siteGroupMgmt.getGroupDescriptor(siteGroupID);
			
			}
			catch(NotFoundException e){
				Dialog.getDialog(response, context, data, "The site-group associated with the given scan result could not be found", "Site-group Not Found", DialogType.WARNING);
				return true;
			}
			
			data.put("siteGroup", siteGroup);
			
			// 4 -- Check permissions
			if( Shortcuts.canRead( context.getSessionInfo(), siteGroup.getObjectId(), "View scan result for site-group " + siteGroup.getGroupId() + " (" + siteGroup.getGroupName() + ")") == false ){
				Shortcuts.getPermissionDeniedDialog(response, data, "You do not permission to view this site-group");
				return true;
			}
			
			// 5 -- Set the headers required to make the user download the file
			response.setHeader("Content-Type", "text/csv");
			response.setHeader("Content-Disposition", "attachment; filename=\"Scan_Result_" + scanResultID + ".csv\"");
			
			// 6 -- Render the view
			if( rule.getRuleType().equalsIgnoreCase(HttpSeekingScanRule.RULE_TYPE) ){
				WebDiscoveryScanResultExport view = new WebDiscoveryScanResultExport();
				return view.process(request, response, context, args, data);
			}
			/*else if( rule.getRuleType().equalsIgnoreCase(ServiceScanRule.RULE_TYPE) ){
				ServiceScanResultView view = new ServiceScanResultView();
				return view.process(request, response, context, args, data);
			}*/
			else{
				throw new ViewFailedException("The view type \"" + rule.getRuleType() + "\" is not recognized");
			}
		
		
		}
		catch(ScanResultLoadFailureException e){
			Dialog.getDialog(response, context, data, "A scan result with the given ID was not found", "Scan Result Not Found", DialogType.WARNING);
			return true;
		}
		catch(ScanRuleLoadFailureException e){
			Dialog.getDialog(response, context, data, "The scan rule associated with the given scan result could not be found", "Scan Rule Not Found", DialogType.WARNING);
			return true;
		}
		catch(SQLException e){
			throw new ViewFailedException(e);
		}
		catch(NoDatabaseConnectionException e){
			throw new ViewFailedException(e);
		}
		catch(InputValidationException e){
			throw new ViewFailedException(e);
		}
		catch(GeneralizedException e){
			throw new ViewFailedException(e);
		}
	}

}
