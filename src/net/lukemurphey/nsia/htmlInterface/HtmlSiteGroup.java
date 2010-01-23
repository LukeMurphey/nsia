package net.lukemurphey.nsia.htmlInterface;

import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.DuplicateEntryException;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.SiteGroupManagement;
import net.lukemurphey.nsia.SiteGroupScanResult;
import net.lukemurphey.nsia.WorkerThread;
import net.lukemurphey.nsia.Application.WorkerThreadDescriptor;
import net.lukemurphey.nsia.SiteGroupManagement.SiteGroupDescriptor;
import net.lukemurphey.nsia.WorkerThread.State;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.htmlInterface.Html.MessageType;
import net.lukemurphey.nsia.scan.RuleBaselineException;
import net.lukemurphey.nsia.scan.ScanResult;
import net.lukemurphey.nsia.scan.ScanResultCode;
import net.lukemurphey.nsia.scan.ScanRule;
import net.lukemurphey.nsia.trustBoundary.ApiScanData;
import net.lukemurphey.nsia.trustBoundary.ApiScannerController;
import net.lukemurphey.nsia.trustBoundary.ApiSiteGroupManagement;
import net.lukemurphey.nsia.trustBoundary.ApiTasks;

public class HtmlSiteGroup extends HtmlContentProvider{
		
		private static int STAT_GREEN = 0;
		private static int STAT_YELLOW = 1;
		private static int STAT_RED = 2;
		private static int STAT_BLUE = 3;
		
		private static int OP_SCAN = 100;
		private static int OP_ADD_SITEGROUPNAME_INVALID = 101;
		private static int OP_UPDATE_SITEGROUPNAME_INVALID = 102;
		private static int OP_DELETE_RULE_SUCCESS = 103;
		private static int OP_DELETE_SITEGROUP_CONFIRMATION = 104;
		private static int OP_ALREADY_SCANNING = 105;
		
		
		private static final String tableStart = "<table class=\"DataTable\" summary=\"HeaderEntries\"><thead><tr>" +
				"<td colspan=\"2\"><span class=\"TitleText\">Status</span></td>" +
				"<td><span class=\"TitleText\">Description</span></td>" +
				"<td><span class=\"TitleText\">Type</span></td>" +
				"<td><span class=\"TitleText\">Subject</span></td>" +
				"<td colspan=\"2\"><span class=\"Text_2\">&nbsp;</span></td></tr></thead><tbody>";
		private static final String tableEnd = "<tr class=\"lastRow\"><td colspan=\"99\"><input onClick=\"showHourglass('Scanning...'); pauseCountdown();\" class=\"button\" type=\"submit\" name=\"Action\" value=\"Scan\"><input onClick=\"pauseCountdown();\" class=\"button\" type=\"submit\" name=\"Action\" value=\"Delete\"><input onClick=\"pauseCountdown();\" class=\"button\" type=\"submit\" name=\"Action\" value=\"Baseline\"></td></tr></tbody></table>";
		
		
		public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, InputValidationException, InvalidHtmlOperationException{
			return getHtml( requestDescriptor, null );
		}
		
		/**
		 * Retrieve HTML code for the content related to Site Groups.
		 * @throws InvalidHtmlParameterException 
		 * @throws InvalidHtmlOperationException 
		 * @throws InputValidationException 
		 * @throws InvalidHtmlOperationException 
		 * @throws InsufficientPermissionException 
		 */
		public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, InputValidationException{
			
			// 1 -- Output the main content
			ApiScanData scanData = new ApiScanData(Application.getApplication());
			
			if( actionDesc == null ){
				actionDesc = performAction(requestDescriptor, scanData);
			}
			
			// Show the progress dialog if a scanner was already created
			if( actionDesc.addData != null && ( actionDesc.result == OP_SCAN || actionDesc.result == OP_ALREADY_SCANNING ) ){
				WorkerThreadDescriptor threadDesc = Application.getApplication().getWorkerThread( (String)actionDesc.addData );
				if( threadDesc != null ){
					WorkerThread worker = threadDesc.getWorkerThread();
					if( worker.getStatus() != State.STOPPED ){
						try{
							if( actionDesc.result == OP_ALREADY_SCANNING ){
								return HtmlOptionDialog.getHtml(requestDescriptor, "Scanning", "Resuming scan...", new Hashtable<String, String>(), new String[]{"Cancel"}, "", HtmlOptionDialog.DIALOG_INFORMATION, "SiteGroup?SiteGroupID=" + requestDescriptor.request.getParameter("SiteGroupID"), "/Ajax/Task/" + java.net.URLEncoder.encode( threadDesc.getUniqueName(), "US-ASCII") );
							}
							else{
								return HtmlOptionDialog.getHtml(requestDescriptor, "Scanning", worker.getStatusDescription(), new Hashtable<String, String>(), new String[]{"Cancel"}, "", HtmlOptionDialog.DIALOG_INFORMATION, "SiteGroup?SiteGroupID=" + requestDescriptor.request.getParameter("SiteGroupID"), "/Ajax/Task/" + java.net.URLEncoder.encode( threadDesc.getUniqueName(), "US-ASCII") );
							}
						}
						catch(UnsupportedEncodingException e){
							Application.getApplication().logExceptionEvent(EventLogMessage.Category.WEB_ERROR, e);
							throw new GeneralizedException();
						}
					}
				}
			}
			
			
			if( actionDesc.result == ActionDescriptor.OP_ADD || actionDesc.result == OP_ADD_SITEGROUPNAME_INVALID || actionDesc.result == ActionDescriptor.OP_ADD_FAILED )
				return getSiteGroupNew( requestDescriptor, actionDesc, scanData );
			else if (actionDesc.result == ActionDescriptor.OP_UPDATE || actionDesc.result == ActionDescriptor.OP_UPDATE_FAILED || actionDesc.result == OP_UPDATE_SITEGROUPNAME_INVALID )
				return getSiteGroupEdit( requestDescriptor, actionDesc, scanData );
			else if( actionDesc.result == OP_DELETE_SITEGROUP_CONFIRMATION ){
				Hashtable<String, String> hiddenVars = new Hashtable<String, String>();
				hiddenVars.put("SiteGroupID", requestDescriptor.request.getParameter("SiteGroupID"));
				hiddenVars.put("Action", "Delete");
				
				return HtmlOptionDialog.getHtml(requestDescriptor, "Confirm Deletion", "Are you sure you want to delete the site group?", hiddenVars, new String[] { "Delete", "Cancel" }, "SiteGroup", HtmlOptionDialog.DIALOG_QUESTION);
				
			}
			else if( actionDesc.result == ActionDescriptor.OP_DELETE_SUCCESS )
				return HtmlMainDashboard.getHtml(requestDescriptor);
			else{
				//return HtmlAction.getActionList(requestDescriptor, actionDesc, Scope.SITE_GROUP, 1, new Hashtable<String, String>());
				return getSiteGroupView(requestDescriptor, actionDesc, scanData );
			}
			
		}
		
		/**
		 * Edit the given site group.
		 * @param request
		 * @param response
		 * @param requestDescriptor
		 * @param httpMethod
		 * @param actionDesc
		 * @param scanData
		 * @return
		 * @throws GeneralizedException
		 * @throws NoSessionException
		 * @throws InvalidHtmlParameterException 
		 * @throws InvalidHtmlOperationException 
		 * @throws InsufficientPermissionException 
		 */
		private static ContentDescriptor getSiteGroupEdit(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc, ApiScanData scanData ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException {
			StringBuffer body = new StringBuffer();
			
			// 1 -- Get the site group ID
			int siteGroupId;
			
			try{
				siteGroupId = Integer.parseInt(requestDescriptor.request.getParameter("SiteGroupID"));
			}
			catch( NumberFormatException e ){
				siteGroupId = -2;
			}
			
			// 1.1 -- Stop if the identifier was not provided
			if( siteGroupId < 0 ){
				throw new InvalidHtmlParameterException("Invalid Site Group Identifier", "The site group identifier given is invalid", "Console");
				/*body.append( GenericHtmlGenerator.getSectionHeader( "Site Group Status", null ) );
				body.append( GenericHtmlGenerator.getWarningDialog("Site Group" , "A valid site group identifier was not provided" ) );
				return new ContentDescriptor("Site Group Management", body);*/
			}
			
			// 1.2 -- Print out any messages
			body.append(Html.renderMessages(requestDescriptor.userId));
			 
			
			// 2 -- Load the site group
			ApiSiteGroupManagement siteGroupManager = new ApiSiteGroupManagement( Application.getApplication() );
			SiteGroupDescriptor siteGroupDesc =null;
			
			try {
				
				siteGroupDesc = siteGroupManager.getGroupDescriptor( requestDescriptor.sessionIdentifier, siteGroupId );
				
				// 2.1a -- Stop if the site group was not found
				/*if( siteGroupDesc == null ){
					throw new InvalidHtmlParameterException("Invalid Site Group ID", "No site group was found with the given site group ID", "Console");
				}*/
				
				// 2.2a -- Output the section header
				body.append( Html.getSectionHeader( "Edit Site Group", StringEscapeUtils.escapeHtml( siteGroupDesc.getGroupName() ) ) );
				
				// 2.3a -- Show the form
				String siteGroupName;
				String siteGroupDescription;
				
				//		2.3.1a -- Get the parameter values
				if( requestDescriptor.request.getParameter("Name") != null )
					siteGroupName = StringEscapeUtils.escapeHtml( requestDescriptor.request.getParameter("Name") );
				else
					siteGroupName = StringEscapeUtils.escapeHtml( siteGroupDesc.getGroupName() );
				
				if( requestDescriptor.request.getParameter("Description") != null )
					siteGroupDescription = StringEscapeUtils.escapeHtml( requestDescriptor.request.getParameter("Description")  );
				else
					siteGroupDescription = StringEscapeUtils.escapeHtml( siteGroupDesc.getDescription() );
				
				//		2.3.2a -- Output the form
				body.append( "<form action=\"SiteGroup\" method=\"post\"><input type=\"hidden\" name=\"SiteGroupID\" value=\"").append( siteGroupId ).append( "\"><table>" );
				body.append( "<tr class=\"Background1\"><td class=\"Text_3\">Site Group Name</td><td><input class=\"textInput\" style=\"width: 350px;\" type=\"text\" name=\"Name\" value=\"").append( siteGroupName ).append( "\"></td></tr>" );
				body.append( "<tr class=\"Background1\"><td class=\"Text_3\">Description</td><td><textarea style=\"width: 350px;\" rows=\"8\" name=\"Description\">").append( siteGroupDescription ).append( "</textarea></td></tr>" );
				body.append( "<tr class=\"Background3\"><td class=\"alignRight\" colspan=\"2\"><input type=\"hidden\" name=\"Action\" value=\"Edit\"><input class=\"button\" type=\"Submit\" name=\"Submit\" value=\"Apply Changes\"></td></tr>" );
				body.append( "</table></form>" );
				
			} catch (InsufficientPermissionException e) {

				// 2.1b -- Output the dialog 
				body.append("<p>");
				body.append(Html.getWarningDialog("Insufficient Permission", "You do not have permission to view this site group.", "Console", "Return to Main Dashboard"));
				
			} catch (NotFoundException e){
				throw new InvalidHtmlParameterException("Invalid Site Group ID", "No site group was found with the given site group ID", "Console");
			}
			
			//
			
			// 2 -- Get the menu items
			NavigationPath navPath = new NavigationPath();
			navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
			navPath.addPathEntry( "Site Group", "/SiteGroup?SiteGroupID=" + siteGroupId );
			String navigationHtml = Html.getNavigationPath( navPath );
			
			// 3 -- Get the navigation bar
			Vector<MenuItem> menuItems = new Vector<MenuItem>();
					
			menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
			menuItems.add( new MenuItem("Edit Group", "/SiteGroup?SiteGroupID=" + requestDescriptor.request.getParameter("SiteGroupID"), MenuItem.LEVEL_TWO) );
			menuItems.add( new MenuItem("Add Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );
			menuItems.add( new MenuItem("Delete", "/SiteGroup?Action=Delete&SiteGroupID=" + requestDescriptor.request.getParameter("SiteGroupID"), MenuItem.LEVEL_TWO) );
			if( siteGroupDesc != null)
				menuItems.add( new MenuItem("Edit ACLs", "/AccessControl?ObjectID=" + siteGroupDesc.getObjectId(), MenuItem.LEVEL_TWO, "w=window.open('AccessControl?ObjectID=" + siteGroupDesc.getObjectId() + "', 'AccessControl', 'height=400,width=780,screenX=' + (screen.availWidth - 700)/2 + ',screenY=' + (screen.availHeight - 300)/2 + ',scrollbars=yes,resizable=yes,toolbar=no');return false") );
			menuItems.add( new MenuItem("View Scan Policy", "/ScanPolicy?SiteGroupID=" + siteGroupId, MenuItem.LEVEL_TWO) );
			
			/*menuItems.add( new MenuItem("User Management", null, MenuItem.LEVEL_ONE) );
			menuItems.add( new MenuItem("List Users", "UserManagement", MenuItem.LEVEL_TWO) );
			menuItems.add( new MenuItem("Add New User", "UserManagement?Action=Add", MenuItem.LEVEL_TWO) );
			menuItems.add( new MenuItem("View Logged in Users", "Sessions", MenuItem.LEVEL_TWO) );
					
			menuItems.add( new MenuItem("Group Management", null, MenuItem.LEVEL_ONE) );
			menuItems.add( new MenuItem("Add Group", "GroupManagement?Action=Add", MenuItem.LEVEL_TWO) );*/
			
			menuItems.add( new MenuItem("Scan Rules", null, MenuItem.LEVEL_ONE) );
			menuItems.add( new MenuItem("Scan Now", "/SiteGroup?Action=Scan&SiteGroupID=" + requestDescriptor.request.getParameter("SiteGroupID"), MenuItem.LEVEL_TWO, "showHourglass('Scanning...'); pauseCountdown();") );
			menuItems.add( new MenuItem("Add New Rule", "/SelectRuleType?SiteGroupID=" + requestDescriptor.request.getParameter("SiteGroupID"), MenuItem.LEVEL_TWO) );
			
			menuItems.add( new MenuItem("Incident Response", null, MenuItem.LEVEL_ONE) );
			menuItems.add( new MenuItem("List Actions", "/Response?SiteGroupID=" + siteGroupId, MenuItem.LEVEL_TWO) );
			menuItems.add( new MenuItem("Add New Action", "/Response?SiteGroupID=" + siteGroupId + "&Action=New", MenuItem.LEVEL_TWO) );
			
			
			String menuOutput = Html.getMenu( menuItems );
			
			// 4 -- Compile the result
			String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
			
			return new ContentDescriptor ("View Site Group", pageOutput);
			
		}
		
		/**
		 * View the given site group.
		 * @param request
		 * @param response
		 * @param requestDescriptor
		 * @param httpMethod
		 * @param actionDesc
		 * @param scanData
		 * @return
		 * @throws GeneralizedException
		 * @throws NoSessionException
		 * @throws InvalidHtmlParameterException 
		 * @throws InvalidHtmlOperationException 
		 * @throws InputValidationException 
		 * @throws InsufficientPermissionException 
		 */
		private static ContentDescriptor getSiteGroupView(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc, ApiScanData scanData ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, InputValidationException {
			StringBuffer body = new StringBuffer();
			
			int siteGroupId;
			
			
			// 1 -- Try to load the site group ID from the parameter list
			try{ 
				siteGroupId = Integer.parseInt(requestDescriptor.request.getParameter("SiteGroupID"));
			}
			catch( NumberFormatException e ){
				siteGroupId = -2;
			}
			
			//	 1.1 -- If we still don't have a valid ID, then try to load it from the action descriptor
			if( siteGroupId < 0 ){
				Integer siteGroupIdInt = (Integer)actionDesc.addData;
				if( siteGroupIdInt != null )
					siteGroupId = siteGroupIdInt.intValue();
			}
			
			//	 1.2 -- Try to get the site group identifier from the scan rule identifier (if provided)
			if( siteGroupId < 0 && requestDescriptor.request.getParameter("RuleID") != null ){
				try{ 
					long ruleID = Long.parseLong( requestDescriptor.request.getParameter("RuleID") );
					
					siteGroupId = scanData.getAssociatedSiteGroup(requestDescriptor.sessionIdentifier, ruleID);
				}
				catch( NotFoundException e ){
					siteGroupId = -1;
				}
				catch( InsufficientPermissionException e ){
					siteGroupId = -1;
				}
			}
			
			//	 1.3 -- Stop if the site group ID is invalid
			if( siteGroupId < 0 ){
				throw new InvalidHtmlParameterException("Invalid Site Group Identifier", "The site group identifier given is invalid", "Console");
			}
			
			//	 1.4 -- Output any pending messages
			body.append(Html.renderMessages(requestDescriptor.userId));
			
			
			// 2 -- Output the main content
			ScanResult[] scanResults = null;
			SiteGroupScanResult siteGroupStatus = null;
			
			try{

				siteGroupStatus = scanData.getSiteGroupStatus( requestDescriptor.sessionIdentifier, siteGroupId );

				scanResults = siteGroupStatus.getScanResults();
				ScanRule[] scanRules = scanData.getScanRules(requestDescriptor.sessionIdentifier, siteGroupId);

				// 2.1a -- Output the section header
				body.append( Html.getSectionHeader( "Site Group Status", siteGroupStatus.getSiteGroupDescriptor().getGroupName() ) );

				// 2.2a -- Output the tables
				if( scanResults.length == 0 && scanRules.length == 0){

					body.append( Html.getDialog("No rules exist yet. Define a rule set to begin monitoring.<p><a href=\"SelectRuleType?SiteGroupID=" + siteGroupId + "\">[Create Rule Now]</a>", "No Rules", "/32_Information", false) );

				}
				else{

					body.append("<form action=\"SiteGroup\"><input type=\"hidden\" name=\"SiteGroupID\" value=\"" + siteGroupId + "\">");
					body.append( tableStart );

					// 2.2.1a -- Print out the rules that have a result
					for(int c = 0; c < scanResults.length; c++ ){

						boolean scanResultObsoleted = false;
						boolean scanRuleAvailable = false;

						// Find the related rule
						ScanRule relatedRule = null;
						for(int d = 0; d < scanRules.length; d++ ){

							if( scanResults[c].getRuleID() == scanRules[d].getRuleId()){
								scanRuleAvailable = true;
								relatedRule = scanRules[d];
								if(scanRules[d].isScanDataObsolete())
									scanResultObsoleted = true;
							}
						}

						int level;
						int deviations = scanResults[c].getDeviations();
						String ruleType = StringEscapeUtils.escapeHtml( scanResults[c].getRuleType() );

						String target;

						if( relatedRule != null && relatedRule.getSpecimenDescription() != null){
							target = StringEscapeUtils.escapeHtml( relatedRule.getSpecimenDescription() );
						}
						else if(scanResults[c].getSpecimenDescription() != null){
							target = StringEscapeUtils.escapeHtml( scanResults[c].getSpecimenDescription() );
						}
						else{
							target = "";
						}

						long ruleId = scanResults[c].getRuleID();

						if( scanRuleAvailable ){
							if( scanResults[c].getDeviations() > 0 )
								level = STAT_RED;
							else if( !scanResults[c].getResultCode().equals(ScanResultCode.SCAN_COMPLETED)  )
								level = STAT_YELLOW;
							else
								level = STAT_GREEN;

							if(scanResultObsoleted || deviations < 0)
								body.append( createRow( STAT_BLUE, -1, ruleType, target, ruleId, "SiteGroup?Action=View&SiteGroupID=" + siteGroupId, siteGroupId ) );
							else if( scanResults[c].getDeviations() == 1  )
								body.append( createRow( level, deviations, ruleType, target, ruleId, "SiteGroup?Action=View&SiteGroupID=" + siteGroupId, "1 deviation", siteGroupId ) );
							else if( scanResults[c].getDeviations() > 1  )
								body.append( createRow( level, deviations, ruleType, target, ruleId, "SiteGroup?Action=View&SiteGroupID=" + siteGroupId, scanResults[c].getDeviations() + " deviations", siteGroupId ) );
							else
								body.append( createRow( level, deviations, ruleType, target, ruleId, "SiteGroup?Action=View&SiteGroupID=" + siteGroupId, scanResults[c].getResultCode().getDescription(), siteGroupId ) );
						}
					}

					// 2.2.2a -- Print out the rules that do not have an associated result
					for(int c = 0; c < scanRules.length; c++ ){
						boolean found = false;

						for(int d = 0; d < scanResults.length; d++ ){
							if( scanResults[d].getRuleID() == scanRules[c].getRuleId()){
								found = true;
							}
						}

						if( found == false ){
							String ruleType = StringEscapeUtils.escapeHtml( scanRules[c].getRuleType() );
							String target = StringEscapeUtils.escapeHtml( scanRules[c].getSpecimenDescription() );
							long ruleId = scanRules[c].getRuleId();

							body.append( createRow( STAT_BLUE, -1, ruleType, target, ruleId, "SiteGroup?Action=View&SiteGroupID=" + siteGroupId, siteGroupId ) );
						}
					}

					body.append( tableEnd );
					
					body.append("</form>");
				}
			} catch( InsufficientPermissionException e){

				// 2.1b -- Output the dialog 
				body.append("<p>");
				body.append(Html.getWarningDialog("Insufficient Permission", "You do not have permission to view this site group.", "Console", "Return to Main Dashboard"));
			} catch (NotFoundException e) {
				throw new InvalidHtmlParameterException("Invalid Site Group Identifier", "The site group identifier given is invalid", "Console");
			}


			// 3 -- Get the menu items
			NavigationPath navPath = new NavigationPath();
			navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
			navPath.addPathEntry( "Site Group", "/SiteGroup?SiteGroupID=" + siteGroupId );
			String navigationHtml = Html.getNavigationPath( navPath );
			
			
			// 4 -- Get the navigation bar
			Vector<MenuItem> menuItems = new Vector<MenuItem>();
			
			menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
			menuItems.add( new MenuItem("Add Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );
			menuItems.add( new MenuItem("Edit Group", "/SiteGroup?Action=Edit&SiteGroupID=" + siteGroupId, MenuItem.LEVEL_TWO) );
			if( siteGroupStatus != null )
				menuItems.add( new MenuItem("Edit ACLs", "/AccessControl?ObjectID=" + siteGroupStatus.getSiteGroupDescriptor().getObjectId(), MenuItem.LEVEL_TWO, "w=window.open('AccessControl?ObjectID=" + siteGroupStatus.getSiteGroupDescriptor().getObjectId() + "', 'AccessControl', 'height=400,width=780,screenX=' + (screen.availWidth - 700)/2 + ',screenY=' + (screen.availHeight - 300)/2 + ',scrollbars=yes,resizable=yes,toolbar=no');return false") );
			menuItems.add( new MenuItem("Delete Group", "/SiteGroup?Action=Delete&SiteGroupID=" + siteGroupId, MenuItem.LEVEL_TWO) );
			if( siteGroupStatus != null ){
				if( siteGroupStatus.getSiteGroupDescriptor().getGroupState() != SiteGroupManagement.State.ACTIVE )
					menuItems.add( new MenuItem("Enable Group", "/SiteGroup?Action=Enable&SiteGroupID=" + siteGroupId, MenuItem.LEVEL_TWO) );
				else
					menuItems.add( new MenuItem("Disable Group", "/SiteGroup?Action=Disable&SiteGroupID=" + siteGroupId, MenuItem.LEVEL_TWO) );
			}
			
			menuItems.add( new MenuItem("View Scan Policy", "/ScanPolicy?SiteGroupID=" + siteGroupId, MenuItem.LEVEL_TWO) );
			
			/*menuItems.add( new MenuItem("User Management", null, MenuItem.LEVEL_ONE) );
			menuItems.add( new MenuItem("List Users", "UserManagement", MenuItem.LEVEL_TWO) );
			menuItems.add( new MenuItem("Add New User", "UserManagement?Action=Add", MenuItem.LEVEL_TWO) );
			menuItems.add( new MenuItem("View Logged in Users", "Sessions", MenuItem.LEVEL_TWO) );
			
			menuItems.add( new MenuItem("Group Management", null, MenuItem.LEVEL_ONE) );
			menuItems.add( new MenuItem("List Groups", "GroupManagement", MenuItem.LEVEL_TWO) );
			menuItems.add( new MenuItem("Add New Group", "GroupManagement?Action=Add", MenuItem.LEVEL_TWO) );*/
			
			menuItems.add( new MenuItem("Scan Rules", null, MenuItem.LEVEL_ONE) );
			menuItems.add( new MenuItem("Scan Now", "/SiteGroup?Action=Scan&SiteGroupID=" + siteGroupId, MenuItem.LEVEL_TWO, "showHourglass('Scanning...'); pauseCountdown();") );
			menuItems.add( new MenuItem("Add New Rule", "/SelectRuleType?SiteGroupID=" + siteGroupId, MenuItem.LEVEL_TWO) );
			
			menuItems.add( new MenuItem("Incident Response", null, MenuItem.LEVEL_ONE) );
			menuItems.add( new MenuItem("List Actions", "/Response?SiteGroupID=" + siteGroupId, MenuItem.LEVEL_TWO) );
			menuItems.add( new MenuItem("Add New Action", "/Response?SiteGroupID=" + siteGroupId + "&Action=New", MenuItem.LEVEL_TWO) );
			
			String menuOutput = Html.getMenu( menuItems );
			
			// 5 -- Compile the result
			String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
			
			return new ContentDescriptor ("View Site Group", pageOutput);
			
		}
		

		/**
		 * Create a page for adding a new site group.
		 * @param request
		 * @param response
		 * @param requestDescriptor
		 * @param httpMethod
		 * @param actionDesc
		 * @param scanData
		 * @return
		 * @throws GeneralizedException
		 * @throws NoSessionException
		 * @throws InvalidHtmlParameterException 
		 */
		private static ContentDescriptor getSiteGroupNew(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc, ApiScanData scanData ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException {
			StringBuffer body = new StringBuffer();
			
			body.append(Html.renderMessages(requestDescriptor.userId));

			// 1.1 -- Output the section header
			body.append( Html.getSectionHeader( "Add a New Site Group", null ) );

			// 1.2 -- Show the form
			String siteGroupName;
			String siteGroupDescription;

			if( requestDescriptor.request.getParameter("Name") != null )
				siteGroupName = StringEscapeUtils.escapeHtml( requestDescriptor.request.getParameter("Name") );
			else
				siteGroupName = "";

			if( requestDescriptor.request.getParameter("Description") != null )
				siteGroupDescription = StringEscapeUtils.escapeHtml( requestDescriptor.request.getParameter("Description") );
			else
				siteGroupDescription = "";

			body.append( "<form action=\"SiteGroup\" method=\"post\"><table>" );
			body.append( "<tr class=\"Background1\"><td class=\"Text_3\">Site Group Name</td><td><input class=\"textInput\" style=\"width: 350px;\" type=\"text\" name=\"Name\" value=\"").append( siteGroupName ).append(  "\"></td></tr>" );
			body.append( "<tr class=\"Background1\"><td class=\"Text_3\">Description</td><td><textarea style=\"width: 350px;\" rows=\"8\" name=\"Description\">").append(  siteGroupDescription ).append( "</textarea></td></tr>" );
			body.append( "<tr class=\"Background3\"><td class=\"alignRight\" colspan=\"2\"><input type=\"hidden\" name=\"Action\" value=\"New\"><input class=\"button\" type=\"Submit\" value=\"Add Site Group\"></td></tr>" );
			body.append( "</table></form>" );

			
			// 2 -- Get the menu items
			NavigationPath navPath = new NavigationPath();
			navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
			navPath.addPathEntry( "Site Group", "/SiteGroup?Action=New" );
			String navigationHtml = Html.getNavigationPath( navPath );
			
			// 3 -- Get the navigation bar
			Vector<MenuItem> menuItems = new Vector<MenuItem>();
			
			menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
			menuItems.add( new MenuItem("View List", "/", MenuItem.LEVEL_TWO) );
			
			/*menuItems.add( new MenuItem("User Management", null, MenuItem.LEVEL_ONE) );
			menuItems.add( new MenuItem("List Users", "UserManagement", MenuItem.LEVEL_TWO) );
			menuItems.add( new MenuItem("Add New User", "UserManagement?Action=Add", MenuItem.LEVEL_TWO) );
			menuItems.add( new MenuItem("View Logged in Users", "Sessions", MenuItem.LEVEL_TWO) );
			
			menuItems.add( new MenuItem("Group Management", null, MenuItem.LEVEL_ONE) );
			menuItems.add( new MenuItem("List Groups", "GroupManagement", MenuItem.LEVEL_TWO) );
			menuItems.add( new MenuItem("Add New Group", "GroupManagement?Action=Add", MenuItem.LEVEL_TWO) );
			
			menuItems.add( new MenuItem("Incident Response", null, MenuItem.LEVEL_ONE) );
			menuItems.add( new MenuItem("List Actions", "Response?SiteGroupID=" + siteGroupId, MenuItem.LEVEL_TWO) );
			menuItems.add( new MenuItem("Add New Action", "Response?SiteGroupID=" + siteGroupId + "&Action=New", MenuItem.LEVEL_TWO) );*/
			
			String menuOutput = Html.getMenu( menuItems );
			
			// 4 -- Compile the result
			String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
			
			return new ContentDescriptor ("View Site Group", pageOutput);
			
		}
		
		public static String getUniqueScanWorkerID( long userID, int siteGroupID ){
			return "Scan by user ID " + userID + " for SiteGroup ID " + siteGroupID;
		}
		
		/**
		 * Perform any actions that are requested.
		 * @param request
		 * @param response
		 * @param requestDescriptor
		 * @param httpMethod
		 * @param scanData
		 * @return
		 * @throws GeneralizedException
		 * @throws NoSessionException
		 * @throws InvalidHtmlOperationException 
		 * @throws InsufficientPermissionException 
		 */
		private static ActionDescriptor performAction(WebConsoleConnectionDescriptor requestDescriptor, ApiScanData scanData ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException{
			
			String action = requestDescriptor.request.getParameter("Action");
			int groupId = -1;
			
			
			// 1 -- Load the necessary fields
			
			//	 1.1 -- Get the site group ID (if available)
			if( requestDescriptor.request.getParameter("SiteGroupID") != null){
				try{
					groupId = Integer.parseInt( requestDescriptor.request.getParameter("SiteGroupID") );
				}
				catch( NumberFormatException e ){
					//Do nothing, the number was the wrong format and will be noted as such
					groupId = -2;
				}
			}
			
			
			// 2 -- Perform the action and decide on the content
			
			// 	 2.1 -- No operation requested
			if( action == null ){
				return new ActionDescriptor( ActionDescriptor.OP_NO_OPERATION );
			}
			
			// The code within the loop below operations involving scan rules 
			String[] ruleIDs = requestDescriptor.request.getParameterValues("RuleID");
			int rulesScanned = 0;
			int rulesDeleted = 0;
			
			if( ruleIDs != null ){
				for (String idStr : ruleIDs) {
					
					// Convert the string to a long
					long ruleId;
					
					try{
						ruleId = Long.parseLong( idStr );
					}
					catch(NumberFormatException e){
						throw new InvalidHtmlParameterException("Invalid Parameter","RuleID to delete is invalid", "Console");
					}
					
					// 2.2 -- Delete the rule
					if( action.matches( "Delete" )){
						
						if( groupId < 0 ){
							try{
								groupId = scanData.getAssociatedSiteGroup(requestDescriptor.sessionIdentifier, ruleId);
							}
							catch( InsufficientPermissionException e){
								Html.addMessage(MessageType.WARNING, "You do not have permission to view this site group", requestDescriptor.userId.longValue());
							}
							catch( NotFoundException e){
								return new ActionDescriptor( ActionDescriptor.OP_NO_OPERATION );//Could not find a site group with the given identifier. Just ignore this for now, the operation cannot be completed without a valid site group ID. 
							}
						}
						
						try{
							scanData.deleteRule(requestDescriptor.sessionIdentifier, ruleId);
							rulesDeleted++;
						}
						catch(InsufficientPermissionException e){
							Html.addMessage(MessageType.WARNING, "You do not have permission to delete rules", requestDescriptor.userId.longValue());
							return new ActionDescriptor(ActionDescriptor.OP_DELETE_FAILED);
						}catch(NotFoundException e){
							Html.addMessage(MessageType.WARNING, e.getMessage(), requestDescriptor.userId.longValue());
							return new ActionDescriptor(ActionDescriptor.OP_DELETE_FAILED);
						}
					}
					
					// 	 2.3 -- Scan a rule
					/*else if ( action.matches( "Scan" ) && ruleId >= 0 ){
						ApiScannerController scannerController = new ApiScannerController( Application.getApplication() );
						try {
							scannerController.scanRule( requestDescriptor.sessionIdentifier, scanRuleId, true );
							rulesScanned++;
						} catch (InsufficientPermissionException e) {
							//throw new InvalidHtmlOperationException("Insufficient Permission", "You do not have permission to scan this site group.", "SiteGroup?SiteGroupID=" + groupId);
							GenericHtmlGenerator.addMessage(MessageType.WARNING, "You do not have permission to scan this rule", requestDescriptor.userId);
						}
					}*/
				}
				
				// 2.3 -- Stop a scan
				if ( action.matches( "Scan" ) && "Cancel".equalsIgnoreCase( requestDescriptor.request.getParameter("Selected") ) ){
					String scanTask = getUniqueScanWorkerID(requestDescriptor.userId, groupId);
					ApiTasks tasks = new ApiTasks(Application.getApplication());
					
					if( tasks.stopTask(requestDescriptor.sessionIdentifier, scanTask) ){
						//Task is being stopped
					}
					
				}
				
				// 2.4 -- Scan rules
				else if ( action.matches( "Scan" )){
					ApiScannerController scannerController = new ApiScannerController( Application.getApplication() );

					long[] ruleIDslong = new long[ruleIDs.length];

					for (int c = 0; c < ruleIDs.length; c++ ) {
						ruleIDslong[c] = Long.parseLong( ruleIDs[c] );
					}

					try{
						return new ActionDescriptor( OP_SCAN, scannerController.scanRules( requestDescriptor.sessionIdentifier, ruleIDslong, true ));
					}
					catch (InsufficientPermissionException e) {
						//throw new InvalidHtmlOperationException("Insufficient Permission", "You do not have permission to scan this site group.", "SiteGroup?SiteGroupID=" + groupId);
						Html.addMessage(MessageType.WARNING, "You do not have permission to scan the given rules", requestDescriptor.userId);
					} catch (DuplicateEntryException e) {
						return new ActionDescriptor( OP_ALREADY_SCANNING, getUniqueScanWorkerID(requestDescriptor.userId, groupId) );
					}

					return new ActionDescriptor( OP_SCAN );
					
				}
				
				// 2.5 -- Baseline a rule
				else if ( action.matches( "Baseline" ) ){
					
					int rulesBaselined = 0;
					//int rulesBaselineFailed = 0;
					
					for (String string : ruleIDs) {
						// Get the ruleID to baseline
						long ruleID = 0;
						
						try{
							ruleID = Long.valueOf(string);
							
							// Call baseline
							if( scanData.baselineRule(ruleID) ){
								rulesBaselined++;
							}
						}
						catch(NumberFormatException e){
							//Skip this rule since the identifier is invalid
						}
						catch (RuleBaselineException e) {
							e.printStackTrace();
							//Baselining this rule failed, post a warning
							Html.addMessage(MessageType.CRITICAL, "Rule " + ruleID + " was unsuccessfully baselined (" + e.getMessage() + ")", requestDescriptor.userId.longValue());
						}
						
					}
					
					Html.addMessage(MessageType.INFORMATIONAL, rulesBaselined + " rules have been re-baselined", requestDescriptor.userId.longValue());
					return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
					
				}
				
				//Display the relevant messages if the operation was successful
				if( rulesScanned > 0){
					Html.addMessage(MessageType.INFORMATIONAL, "Scan complete", requestDescriptor.userId.longValue());
					return new ActionDescriptor( OP_SCAN );
				}
				if( rulesDeleted > 0){
					if( rulesDeleted == 1 ){
						Html.addMessage(MessageType.INFORMATIONAL, "Rule successfully deleted", requestDescriptor.userId.longValue());
						return new ActionDescriptor( OP_DELETE_RULE_SUCCESS, Integer.valueOf(groupId));
					}
					else{
						Html.addMessage(MessageType.INFORMATIONAL, rulesDeleted + " rules successfully deleted", requestDescriptor.userId.longValue());
						return new ActionDescriptor( OP_DELETE_RULE_SUCCESS, Integer.valueOf(groupId));
					}
				}
				
				
			}// End if rule IDs exist
			
			// 	 2.6 -- Scan the site group
			else if ( action.matches( "Scan" ) && groupId >= 0 ){
				
				
				
				Html.addMessage(MessageType.WARNING, "No rules were selected to scan", requestDescriptor.userId);
				return new ActionDescriptor( ActionDescriptor.OP_NO_OPERATION );
				
				/*ApiScannerController scannerController = new ApiScannerController( Application.getApplication() );
				try {
					scannerController.scanSiteGroup( requestDescriptor.sessionIdentifier, groupId, true );
					// ScanResult[] scanResult
				} catch (InsufficientPermissionException e) {
					//throw new InvalidHtmlOperationException("Insufficient Permission", "You do not have permission to scan this site group.", "SiteGroup?SiteGroupID=" + groupId);
					Html.addMessage(MessageType.WARNING, "You do not have permission to scan this site group", requestDescriptor.userId);
				}
				
				Html.addMessage(MessageType.INFORMATIONAL, "Scan complete", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_SCAN );*/
			}
			
			//	 2.7 -- Add a new site group
			else if ( action.matches( "New" ) ){
				String name = requestDescriptor.request.getParameter("Name");
				String description = requestDescriptor.request.getParameter("Description");
				
				ApiSiteGroupManagement siteGroupManager = new ApiSiteGroupManagement(Application.getApplication() );
				if( name != null ){
					try {
						int newGroupId = siteGroupManager.addGroup( requestDescriptor.sessionIdentifier, name, description );
						
						if( newGroupId < 0 ){
							Html.addMessage(MessageType.WARNING, "The site group could not be added", requestDescriptor.userId.longValue());
							return new ActionDescriptor( ActionDescriptor.OP_ADD_FAILED );
						}
						else{
							Html.addMessage(MessageType.INFORMATIONAL, "The site group was successfully created", requestDescriptor.userId.longValue());
							return new ActionDescriptor( ActionDescriptor.OP_ADD_SUCCESS, Integer.valueOf( newGroupId ));
						}
					} catch (InsufficientPermissionException e) {
						Html.addMessage(MessageType.WARNING, "You do not have permission to create site groups", requestDescriptor.userId);
					} catch (InputValidationException e) {
						Html.addMessage(MessageType.WARNING, "The site group name provided is invalid", requestDescriptor.userId.longValue());
						return new ActionDescriptor( OP_ADD_SITEGROUPNAME_INVALID );
					}
				}
				
				return new ActionDescriptor( ActionDescriptor.OP_ADD );
			}
			
			// 2.8 -- Edit a site group
			else if ( action.matches( "Edit" ) ){
				String name = requestDescriptor.request.getParameter("Name");
				String description = requestDescriptor.request.getParameter("Description");
				
				ApiSiteGroupManagement siteGroupManager = new ApiSiteGroupManagement(Application.getApplication() );

				if( requestDescriptor.request.getParameter("Submit") != null ){
					try {
						if( !siteGroupManager.updateGroupInfo( requestDescriptor.sessionIdentifier, groupId, name, description ) ){
							Html.addMessage(MessageType.WARNING, "Site group was not updated successfully", requestDescriptor.userId.longValue());
							return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED );
						}
						else{
							Html.addMessage(MessageType.INFORMATIONAL, "Site group updated successfully", requestDescriptor.userId.longValue());
							return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
						}
						
					} catch (InsufficientPermissionException e) {
						Html.addMessage(MessageType.WARNING, "You do not have permission to modify this site group", requestDescriptor.userId.longValue());
					} catch (InputValidationException e) {
						Html.addMessage(MessageType.WARNING, "The site group name provided is invalid", requestDescriptor.userId.longValue());
						return new ActionDescriptor( OP_UPDATE_SITEGROUPNAME_INVALID );
					}
				}
				
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE );
			}
			
			// 2.9 -- Delete the site group
			else if ( action.matches( "Delete" ) && groupId >= 0 ){
				
				if( "Delete".equalsIgnoreCase( requestDescriptor.request.getParameter("Selected") ) ){
				
					ApiSiteGroupManagement siteGroupManager = new ApiSiteGroupManagement(Application.getApplication() );
					try {
						if( siteGroupManager.deleteGroup(requestDescriptor.sessionIdentifier, groupId ) ){
							Html.addMessage(MessageType.INFORMATIONAL, "Site group deleted", requestDescriptor.userId.longValue());
							return new ActionDescriptor( ActionDescriptor.OP_DELETE_SUCCESS );
						}
						else{
							Html.addMessage(MessageType.WARNING, "Site group was not successfully deleted", requestDescriptor.userId.longValue());
							return new ActionDescriptor( ActionDescriptor.OP_DELETE_FAILED );
						}
					} catch (InsufficientPermissionException e) {
						Html.addMessage(MessageType.WARNING, "You do not have permission to delete this site group", requestDescriptor.userId.longValue());
						return new ActionDescriptor( ActionDescriptor.OP_DELETE_FAILED );
					}
				}
				else if( !"Cancel".equalsIgnoreCase( requestDescriptor.request.getParameter("Selected") ) ){
					return new ActionDescriptor(OP_DELETE_SITEGROUP_CONFIRMATION);
				}
				
			}
			
			// 2.10 -- Disable the site group
			else if ( action.matches( "Disable" ) && groupId >= 0 ){
				ApiSiteGroupManagement siteGroupManager = new ApiSiteGroupManagement(Application.getApplication() );
				try {
					if( siteGroupManager.disableGroup(requestDescriptor.sessionIdentifier, groupId ) ){
						Html.addMessage(MessageType.INFORMATIONAL, "Site group disabled", requestDescriptor.userId.longValue());
						return new ActionDescriptor( ActionDescriptor.OP_DELETE_SUCCESS );
					}
					else{
						Html.addMessage(MessageType.WARNING, "Site group was not successfully disabled", requestDescriptor.userId.longValue());
						return new ActionDescriptor( ActionDescriptor.OP_DELETE_FAILED );
					}
				} catch (InsufficientPermissionException e) {
					Html.addMessage(MessageType.WARNING, "You do not have permission to disable this site group", requestDescriptor.userId.longValue());
				}
				
			}
			
			// 2.11 -- Enable the site group
			else if ( action.matches( "Enable" ) && groupId >= 0 ){
				ApiSiteGroupManagement siteGroupManager = new ApiSiteGroupManagement(Application.getApplication() );
				try {
					if( siteGroupManager.enableGroup(requestDescriptor.sessionIdentifier, groupId ) ){
						Html.addMessage(MessageType.INFORMATIONAL, "Site group enabled, the given rules will be automatically scanned", requestDescriptor.userId.longValue());
						return new ActionDescriptor( ActionDescriptor.OP_ENABLE_SUCCESS );
					}
					else{
						Html.addMessage(MessageType.WARNING, "Site group was not successfully enabled", requestDescriptor.userId.longValue());
						return new ActionDescriptor( ActionDescriptor.OP_ENABLE_FAILED );
					}
						
				} catch (InsufficientPermissionException e) {
					Html.addMessage(MessageType.WARNING, "You do not have permission to enable this site group", requestDescriptor.userId.longValue());
				}
			}
			
			//	 3 -- Return default operation if no action was matched
			return new ActionDescriptor( ActionDescriptor.OP_NO_OPERATION );
				
		}
		
		private static String createRow( int status, int deviations, String type, String description, long ruleId, String link, long siteGroupId){
			return createRow(status, deviations, type, description, ruleId, link, null, siteGroupId);
		}
		
		/**
		 * Create a row that summarizes a rule.
		 * @param status
		 * @param deviations
		 * @param type
		 * @param description
		 * @param ruleId
		 * @param link
		 * @return
		 */
		private static String createRow( int status, int deviations, String type, String description, long ruleId, String linkn, String statusDescription, long siteGroupID ){
			String output = "<tr>";
			
			// 1 -- Output the status icon
			if( status == STAT_GREEN )
				output += "<td width=\"40\" align=\"center\" class=\"StatGreen\"><img src=\"/22_Check\" alt=\"ok\"></td>";
			else if( status == STAT_RED )
				output += "<td width=\"40\" align=\"center\" class=\"StatRed\"><img src=\"/22_Alert\" alt=\"alert\"></td>";
			else if( status == STAT_BLUE )
				output += "<td width=\"40\" align=\"center\" class=\"StatBlue\"><img src=\"/22_CheckBlue\" alt=\"ok\"></td>";
			else
				output += "<td width=\"40\" align=\"center\" class=\"StatYellow\"><img src=\"/22_Warning\" alt=\"warning\"></td>";
			output += "<td align=\"center\"><input type=\"checkbox\" name=\"RuleID\" value=\"" + ruleId + "\"></td>";
			
			// 2 -- Output the deviation count
			if( statusDescription == null ){
				if(  deviations == -1 )
					output += "<td class=\"Background1\">Not scanned yet&nbsp;&nbsp;</td>";
				else if(  status == STAT_YELLOW )
					output += "<td class=\"Background1\">Connection failed&nbsp;&nbsp;</td>";
				else if( deviations == 1 )
					output += "<td class=\"Background1\">" + deviations + " deviation&nbsp;&nbsp;</td>";
				else
					output += "<td class=\"Background1\">" + deviations + " deviations&nbsp;&nbsp;</td>";
			}else{
				output += "<td class=\"Background1\">" + statusDescription + "&nbsp;&nbsp;</td>";
			}
			
			// 3 -- Output the rule type and description field
			output += "<td class=\"Background1\">" + type + "&nbsp;&nbsp;</td>" +
			"<td class=\"Background1\">" + description + "&nbsp;&nbsp;</td>";
			
			// 4 -- Output the delete option button
			/*output += "<td class=\"Background1\"><table><tr><td><a href=\"SiteGroup?Action=DeleteRule&RuleID=" + ruleId + "&SiteGroupID=" + siteGroupID +
			"\"><img alt=\"delete\" src=\"/16_Delete\"></a></td><td><a href=\"SiteGroup?Action=DeleteRule&RuleID=" + ruleId + "&SiteGroupID=" + siteGroupID +
			"\">Delete</a></td></tr></table></td></td>";*/
			
			// 5 -- Output the edit option button
			output += "<td class=\"Background1\"><table><tr><td><a href=\"ScanRule?Action=Edit&RuleID=" + ruleId + 
			"\"><img class=\"imagebutton\" alt=\"configure\" src=\"/16_Configure\"></a></td><td><a href=\"ScanRule?RuleID=" + ruleId +	 
			"\">Details</a></td></tr></table></td>";
			
			// 6 -- Output the scan result view button
			output += "<td class=\"Background1\"><table><tr><td><a href=\"ScanResult?RuleID=" + ruleId + 
			"\"><img class=\"imagebutton\" alt=\"scan results\" src=\"/16_BarChart\"></a></td><td><a href=\"ScanResult?RuleID=" + ruleId +	 
			"\">Scan History</a></td></tr></table></td>";
			
			// 7 -- Output the scan button
			/*output += "<td class=\"Background1\"><table><tr><td><a href=\"SiteGroup?Action=Scan&RuleID=" + ruleId + 
			"\"><img class=\"imagebutton\" alt=\"scan\" src=\"/16_Play\"></a></td><td><a href=\"SiteGroup?Action=Scan&RuleID=" + ruleId +	 
			"\">Scan</a></td></tr></table></td>";*/
			
			output += "</tr>";
			return output;
		}

}
