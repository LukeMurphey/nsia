package net.lukemurphey.nsia.htmlInterface;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.scan.*;
import net.lukemurphey.nsia.trustBoundary.ApiScanData;

public class HtmlScanRule extends HtmlContentProvider {

	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, InvalidHtmlOperationException, NotFoundException, InputValidationException{
		return HtmlScanRule.getHtml(requestDescriptor, null);
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, InvalidHtmlOperationException, NotFoundException, InputValidationException{
		
		ApiScanData scanData = new ApiScanData(Application.getApplication());
		
		long scanRuleId;
		
		try{
			scanRuleId = Long.parseLong(requestDescriptor.request.getParameter("RuleID") );
			
			ScanRule rule = scanData.getScanRule(requestDescriptor.sessionIdentifier, scanRuleId);
			
			if( actionDesc == null ){
				actionDesc = performAction(requestDescriptor);
			}
			
			actionDesc.addData = rule;
			
			if( rule instanceof HttpStaticScanRule ){
				return HtmlStaticScanRule.getHtml(requestDescriptor);
			}
			else if(rule instanceof HttpSeekingScanRule) {
				return HtmlSeekingRule.getHtml(requestDescriptor);
			}
			else if(rule instanceof ServiceScanRule) {
				return HtmlServiceScanRule.getHtml(requestDescriptor);
			}
			else{
				throw new InvalidHtmlOperationException("Unrecognized Rule Type", "The rule type loaded was not recognized by the rule editor.", "Console");
			}
		} catch( NumberFormatException e){
			throw new InvalidHtmlParameterException("Unrecognized Rule Type", "The rule identifier is not a valid number", "Console");
		} catch (InsufficientPermissionException e) {
			//Html.getDialog("You do not have permission to view this rule", "Insufficient Permission", "32_Warning");
			throw new InvalidHtmlOperationException("Insufficient Permission", "You do not have permission to view this rule", "Console"); //TODO change this out with a more specific dialog (don't just toss the user up to the main dashboard)
		}

	}
	
	
	private static ActionDescriptor performAction( WebConsoleConnectionDescriptor requestDescriptor) throws GeneralizedException, NoSessionException{
		String action = requestDescriptor.request.getParameter("Action");
		
		// 1 -- No action specified
		if( action == null ){
			return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
		}
		
		// 2 -- Action is to scan
		/*else if( action.equalsIgnoreCase("Scan") ){
			long ruleId = 0;
			
			if( requestDescriptor.request.getParameter("RuleID") != null ){
				try{
					ruleId = Long.parseLong( requestDescriptor.request.getParameter("RuleID") );
				}
				catch(NumberFormatException e){
					return new ActionDescriptor(OP_SCAN_FAILED, GenericHtmlGenerator.getWarningNote("The scan rule identifier provided is not valid"));
				}
			}
			
			ApiScannerController scannerController = new ApiScannerController(Application.getApplication());
			ScanResult scanResult = null;
			
			try{
				scanResult = scannerController.scanRule(requestDescriptor.sessionIdentifier, ruleId, true);
			}catch(InsufficientPermissionException e){
				return new ActionDescriptor(OP_SCAN_FAILED, GenericHtmlGenerator.getWarningNote("You do not have permission to scan this rule"));
			}
			
			return new ActionDescriptor(OP_SCAN_SUCCESS, GenericHtmlGenerator.getInfoNote("The scan rule was successfully scanned, result is: " + scanResult.getResultCode().getDescription()));
		}*/
		
		
		return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION);
	}
}
