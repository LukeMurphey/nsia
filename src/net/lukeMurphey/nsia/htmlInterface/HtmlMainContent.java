package net.lukeMurphey.nsia.htmlInterface;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.NotFoundException;
import net.lukeMurphey.nsia.SessionStatus;
import net.lukeMurphey.nsia.eventLog.EventLogMessage;

public class HtmlMainContent extends HtmlContentProvider {

	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws IOException, NoSessionException{
		return getHtml( requestDescriptor, null );
	}

	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws IOException, NoSessionException {

		try{
			requestDescriptor.response.setStatus(HttpServletResponse.SC_OK);
			String mode = requestDescriptor.request.getParameter("Mode");
			mode = requestDescriptor.request.getServletPath().substring(1);
			
			// * -- Content: Resource Not Found (404)
			// This page will be loaded when the request URI is not to the correct servlet. This can occur when a user types an incorrect URL. 
			/*if( requestDescriptor.request.getRequestURI().startsWith("/Dashboard") == false ){
				return HtmlProblemDialog.getHtml(requestDescriptor, "Resource Not Found (404)", "The resource you requested could not be found.", HtmlProblemDialog.DIALOG_WARNING, "Console", "Main Dashboard");
			}*/

			// * -- Content: No configuration

			// * -- Content: No database connection

			// * -- Content: Login necessary
			if( requestDescriptor.sessionStatus != SessionStatus.SESSION_ACTIVE ){//|| requestDescriptor.authenticationAttempt != requestDescriptor.AUTH_NONE_REQUESTED ){
				return HtmlLogin.getHtml(requestDescriptor, actionDesc);
			}

			// * -- Content: System status
			else if( mode != null && mode.matches("SystemStatus") ){
				return HtmlSystemStatus.getHtml(requestDescriptor, actionDesc);
			}

			// * -- Content: Site group
			else if( mode != null && mode.matches("SiteGroup") ){ // && request.getParameter("SiteGroupID") != null
				return HtmlSiteGroup.getHtml(requestDescriptor, actionDesc);
			}

			// * -- Content: HTTP Rule
			else if( mode != null && mode.matches("ScanRule") ){ //&& request.getParameter("RuleID") != null){
				return HtmlScanRule.getHtml(requestDescriptor, actionDesc);
			}
			
			// * -- Content: HTTP Static Rule
			else if( mode != null && mode.matches("HttpStaticScanRule") ){ //&& request.getParameter("RuleID") != null){
				return HtmlStaticScanRule.getHtml(requestDescriptor, actionDesc);
			}
			
			// * -- Content: HTTP Rule Result
			/*else if( mode != null && mode.matches("ScanResult") && requestDescriptor.request.getParameter("RuleID") != null){
				return HtmlResults_HttpDataHash.getHtml(requestDescriptor, actionDesc);
			}*/

			// * -- Content: HTTP header rule

			// * -- Content: Event log
			else if( mode != null && mode.matches("EventLog") ){
				return HtmlEventLogViewer.getHtml(requestDescriptor, actionDesc);
			}
			
			// * -- Content: User management
			else if( mode != null && mode.matches("UserManagement") ){
				return HtmlUserManagement.getHtml(requestDescriptor, actionDesc);
			}

			// * -- Content: Group management
			else if( mode != null && mode.matches("GroupManagement") ){
				return HtmlGroupManagement.getHtml(requestDescriptor, actionDesc);
			}

			// * -- Content: Sessions
			else if( mode != null && mode.matches("Sessions") ){
				return HtmlSessions.getHtml(requestDescriptor, actionDesc);
			}

			// * -- Content: System Configuration
			else if( mode != null && mode.matches("SystemConfiguration") ){
				return HtmlSystemConfiguration.getHtml(requestDescriptor, actionDesc);
			}

			// * -- Content: Access control lists and rights
			else if( mode != null && mode.matches("AccessControl") ){
				return HtmlAccessControl.getHtml(requestDescriptor, actionDesc);
			}

			// * -- Content: User rights management
			else if( mode != null && mode.matches("UserRights") ){
				return HtmlUserRights.getHtml(requestDescriptor, actionDesc);
			}

			// * -- Content: Scan result history
			else if( mode != null && mode.matches("ScanResult") ){
				return HtmlScanResult.getHtml(requestDescriptor, actionDesc);
			}
			
			// * -- Content: Select rule type
			else if( mode != null && mode.matches("SelectRuleType") ){
				return HtmlSelectRule.getHtml(requestDescriptor, actionDesc);
			}
			
			// * -- Content: Backup database
			else if( mode != null && mode.matches("DatabaseBackup") ){
				return HtmlDatabaseBackup.getHtml(requestDescriptor, actionDesc);
			}
			
			// * -- Content: Definitions set administration
			else if( mode != null && mode.matches("Definitions") ){
				return HtmlDefinitionSet.getHtml(requestDescriptor, actionDesc);
			}
			
			// * -- Content: HTTP Auto-Discovery scan rule
			else if( mode != null && mode.matches("HttpDiscoveryRule") ){
				return HtmlSeekingRule.getHtml(requestDescriptor, actionDesc);
			}
			
			// * -- Content: Service scan rule
			else if( mode != null && mode.matches("SiteScan") ){
				return HtmlSiteScan.getHtml(requestDescriptor, actionDesc);
			}

			// * -- Content: Website service scan
			else if( mode != null && mode.matches("ServiceMonitoring") ){
				return HtmlServiceScanRule.getHtml(requestDescriptor, actionDesc);
			}
			
			// * -- Content: Background Tasks
			else if( mode != null && mode.matches("Tasks") ){
				return HtmlTaskList.getHtml(requestDescriptor, actionDesc);
			}
			
			// * -- Content: Definition Filters
			else if( mode != null && mode.matches("ExceptionManagement") ){
				return HtmlExceptionManagement.getHtml(requestDescriptor, actionDesc);
			}
			
			// * -- Content: Ajax Response
			else if( mode != null && mode.matches("Ajax") ){
				return HtmlAjax.getHtml(requestDescriptor, actionDesc);
			}
			
			// * -- Content: Action Editor
			else if( mode != null && mode.matches("Response") ){
				return HtmlAction.getHtml(requestDescriptor, actionDesc);
			}
			
			// * -- Content: License Management
			else if( mode != null && mode.matches("License") ){
				return HtmlLicense.getHtml(requestDescriptor, actionDesc);
			}
			
			// * -- Content: Scan Policy Management
			else if( mode != null && mode.matches("ScanPolicy") ){
				return HtmlPolicyManagement.getHtml(requestDescriptor, actionDesc);
			}
			
			// * -- Content: System update
			else if( mode != null && mode.matches("Update") ){
				return HtmlUpdate.getHtml(requestDescriptor, actionDesc);
			}
			
			// * -- Content: Definition Errors
			else if( mode != null && mode.matches("DefinitionErrors") ){
				return HtmlDefinitionStatus.getHtml(requestDescriptor, actionDesc);
			}
			
			// * -- Content: Main dashboard (default content)
			else{
				return HtmlMainDashboard.getHtml(requestDescriptor, actionDesc);
			}

			// * -- Content: Help
			
		}
		catch( InvalidHtmlParameterException e){
			requestDescriptor.response.setStatus(HttpServletResponse.SC_OK);
			return HtmlProblemDialog.getHtml(requestDescriptor, e.getIssueTitle(), e.getIssueDescription() + ". Contact the System Administrator if the problem continues.", HtmlProblemDialog.DIALOG_WARNING);
		}
		catch( InvalidHtmlOperationException e){
			requestDescriptor.response.setStatus(HttpServletResponse.SC_OK);
			return HtmlProblemDialog.getHtml(requestDescriptor, e.getIssueTitle(), e.getIssueDescription(), HtmlProblemDialog.DIALOG_WARNING);
		}
		catch( NotFoundException e){
			/* This dialog is intended to be used when a user requests an object (such as site group that does not exist). 
			 * It is not intended to be used for resources that are missing (that is, HTTP 404 errors should not handled
			 * with the below dialog)
			 */
			requestDescriptor.response.setStatus(HttpServletResponse.SC_OK);
			return HtmlProblemDialog.getHtml(requestDescriptor, "Not Found", e.getMessage(), HtmlProblemDialog.DIALOG_WARNING);
		}
		catch (java.lang.RuntimeException e){
			requestDescriptor.response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			Application.getApplication().logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e  ); //Log these errors since they are not typically caught at runtime
			return HtmlProblemDialog.getHtml(requestDescriptor, "Internal Error", "An internal error occurred and the requested operation could not be completed. Contact the System Administrator if assistance is necessary.", HtmlProblemDialog.DIALOG_ALERT);
		}
		catch (GeneralizedException e){
			requestDescriptor.response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return HtmlProblemDialog.getHtml(requestDescriptor, "Internal Error", "An internal error occurred and the requested operation could not be completed. Contact the System Administrator if assistance is necessary.", HtmlProblemDialog.DIALOG_ALERT);
		}
		catch (Exception e){
			requestDescriptor.response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			Application.getApplication().logExceptionEvent( EventLogMessage.Category.INTERNAL_ERROR, e  );
			return HtmlProblemDialog.getHtml(requestDescriptor, "Internal Error", "An internal error occurred and the requested operation could not be completed. Contact the System Administrator if assistance is necessary.", HtmlProblemDialog.DIALOG_ALERT);
		}

	}

}

