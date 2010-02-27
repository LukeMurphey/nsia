package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.DuplicateEntryException;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.RuleScanWorker;
import net.lukemurphey.nsia.SiteGroupManagement;
import net.lukemurphey.nsia.SiteGroupScanResult;
import net.lukemurphey.nsia.Application.WorkerThreadDescriptor;
import net.lukemurphey.nsia.SiteGroupManagement.SiteGroupDescriptor;
import net.lukemurphey.nsia.UserManagement.UserDescriptor;
import net.lukemurphey.nsia.WorkerThread.State;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.scan.DefinitionSetLoadException;
import net.lukemurphey.nsia.scan.HttpSeekingScanRule;
import net.lukemurphey.nsia.scan.ScanData;
import net.lukemurphey.nsia.scan.ScanResult;
import net.lukemurphey.nsia.scan.ScanResultCode;
import net.lukemurphey.nsia.scan.ScanRule;
import net.lukemurphey.nsia.scan.ScanRuleLoader;
import net.lukemurphey.nsia.scan.ScanRule.ScanResultLoadFailureException;
import net.lukemurphey.nsia.scan.ScanRule.ScanRuleLoadFailureException;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.Menu;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.StandardViewList;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;
import net.lukemurphey.nsia.web.templates.TemplateLoader;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class SiteGroupView extends View {

	public enum RuleState{
		STAT_RED, STAT_YELLOW, STAT_GREEN, STAT_BLUE;
	}
	
	public static final String VIEW_NAME = "sitegroup_rules";
	
	public SiteGroupView() {
		super("SiteGroup", VIEW_NAME, Pattern.compile("[0-9]+", Pattern.CASE_INSENSITIVE));
	}

	public static String getURL( SiteGroupDescriptor siteGroup ) throws URLInvalidException{
		return getURL(siteGroup.getGroupId());
	}
	
	public static String getURL( int siteGroupID ) throws URLInvalidException{
		SiteGroupView view = new SiteGroupView();
		
		return view.createURL(siteGroupID);
	}
	
	public static String getURL( long siteGroupID ) throws URLInvalidException{
		SiteGroupView view = new SiteGroupView();
		
		return view.createURL(siteGroupID);
	}
	
	public class RuleStatusDescriptor{
		private RuleState status;
		private int deviations;
		private String ruleType;
		private String description;
		private long ruleID;
		private String statusDescription;
		
		public RuleStatusDescriptor( RuleState state, int deviations, String ruleType, String description, long ruleID, String message){
			this.status = state;
			this.deviations = deviations;
			this.ruleType = ruleType;
			this.description = description;
			this.ruleID = ruleID;
			this.statusDescription = message;
		}
		
		public RuleStatusDescriptor( RuleState state, int deviations, String ruleType, String description, long ruleID){
			this.status = state;
			this.deviations = deviations;
			this.ruleType = ruleType;
			this.description = description;
			this.ruleID = ruleID;
		}
		
		public int getDeviations(){
			return deviations;
		}
		
		public String getStatusDescription(){
			return statusDescription;
		}
		
		public RuleState getStatus(){
			return status;
		}
		
		public String getType(){
			return ruleType;
		}
		
		public long getID(){
			return ruleID;
		}
		
		public String getDescription(){
			return description;
		}
		
	}
	
	public class RuleResult{
		Vector<RuleStatusDescriptor> ruleStatuses;
		ScanRule[] scanRules;
		ScanResult[] scanResults;
	}
	
	/**
	 * Get the scan results for the given site-group. 
	 * @param sitegroup
	 * @return
	 * @throws SQLException
	 * @throws InputValidationException
	 * @throws NoDatabaseConnectionException
	 * @throws NotFoundException
	 * @throws ScanResultLoadFailureException
	 * @throws ScanRuleLoadFailureException
	 */
	protected RuleResult getResults( SiteGroupDescriptor sitegroup ) throws SQLException, InputValidationException, NoDatabaseConnectionException, NotFoundException, ScanResultLoadFailureException, ScanRuleLoadFailureException{
		
		// 0 -- Precondition check
		if( sitegroup == null ){
			return null;
		}
		
		// 1 -- Get the scan results and site group details
		ScanResult[] scanResults = null;
		SiteGroupScanResult siteGroupStatus = null;
		ScanData scanData = new ScanData(Application.getApplication());
		
		siteGroupStatus = scanData.getSiteGroupStatus( sitegroup.getGroupId() );

		scanResults = siteGroupStatus.getScanResults();
		ScanRule[] scanRules = ScanRuleLoader.getScanRules( sitegroup.getGroupId() );
		
		Vector<RuleStatusDescriptor> ruleStatues = new Vector<RuleStatusDescriptor>();
		
		// 2 -- Get the status for each rule
		for(int c = 0; c < scanResults.length; c++ ){

			boolean scanResultObsoleted = false;
			boolean scanRuleAvailable = false;

			// 2.1 -- Find the related rule
			ScanRule relatedRule = null;
			for(int d = 0; d < scanRules.length; d++ ){

				if( scanResults[c].getRuleID() == scanRules[d].getRuleId()){
					scanRuleAvailable = true;
					relatedRule = scanRules[d];
					if(scanRules[d].isScanDataObsolete()){
						scanResultObsoleted = true;
					}
				}
			}

			// 2.2 -- Get the number of deviations
			int deviations = scanResults[c].getDeviations();
			
			// 2.2 -- Get the target
			String target;

			if( relatedRule != null && relatedRule.getSpecimenDescription() != null){
				target = relatedRule.getSpecimenDescription();
			}
			else if(scanResults[c].getSpecimenDescription() != null){
				target = scanResults[c].getSpecimenDescription();
			}
			else{
				target = "";
			}

			// 2.3 -- Get the rule ID
			long ruleId = scanResults[c].getRuleID();

			// 2.4 -- Get the rule status
			RuleState level;
			if( scanRuleAvailable ){
				if( scanResults[c].getDeviations() > 0 )
					level = RuleState.STAT_RED;
				else if( !scanResults[c].getResultCode().equals(ScanResultCode.SCAN_COMPLETED)  )
					level = RuleState.STAT_YELLOW;
				else
					level = RuleState.STAT_GREEN;

				// 2.5 -- Get the rule status descriptor
				if(scanResultObsoleted || deviations < 0){
					ruleStatues.add(new RuleStatusDescriptor(level, deviations, scanResults[c].getRuleType(), target, ruleId));
				}
				else if( scanResults[c].getDeviations() == 1  ){
					ruleStatues.add(new RuleStatusDescriptor(level, deviations, scanResults[c].getRuleType(), target, ruleId, "1 deviation"));
				}
				else if( scanResults[c].getDeviations() > 1  ){
					ruleStatues.add(new RuleStatusDescriptor(level, deviations, scanResults[c].getRuleType(), target, ruleId, scanResults[c].getDeviations() + " deviations"));
				}
				else{
					ruleStatues.add(new RuleStatusDescriptor(level, deviations, scanResults[c].getRuleType(), target, ruleId, scanResults[c].getResultCode().getDescription()));
				}
			}
		}

		// 2.6 -- Add the rules that do not have an associated result
		for(int c = 0; c < scanRules.length; c++ ){
			boolean found = false;

			for(int d = 0; d < scanResults.length; d++ ){
				if( scanResults[d].getRuleID() == scanRules[c].getRuleId()){
					found = true;
				}
			}

			if( found == false ){
				String target = StringEscapeUtils.escapeHtml( scanRules[c].getSpecimenDescription() );
				long ruleId = scanRules[c].getRuleId();

				ruleStatues.add(new RuleStatusDescriptor(RuleState.STAT_BLUE, -1, scanRules[c].getRuleType(), target, ruleId));
			}
		}
		
		RuleResult result = new RuleResult();
		result.ruleStatuses = ruleStatues;
		result.scanResults = scanResults;
		result.scanRules = scanRules;
		
		return result;
	}
	
	/**
	 * Get the unique ID for the scanner worker thread.
	 * @param userID
	 * @param siteGroupID
	 * @return
	 */
	public static String getUniqueScanWorkerID( int userID, long siteGroupID ){
		return "Scan by user ID " + userID + " for SiteGroup ID " + siteGroupID;
	}
	
	/**
	 * Delete the given rules.
	 * @param context
	 * @param rules
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws NotFoundException
	 */
	private int deleteRules( RequestContext context, long[] rules ) throws SQLException, NoDatabaseConnectionException, NotFoundException{
		
		// 1 -- Delete the rules
		int rulesDeleted = 0;
		
		for (long ruleId : rules) {
			
			//int groupId = ScanRule.getAssociatedSiteGroup( ruleId );
			
			ScanRule.deleteRule(ruleId);
			rulesDeleted++;
		}
		
		context.addMessage(rulesDeleted + " rules have been deleted", MessageSeverity.SUCCESS);
		
		return rulesDeleted;
		
	}
	
	private void baselineRules( RequestContext context, long[] rules ) throws SQLException, NoDatabaseConnectionException, DefinitionSetLoadException, InputValidationException, ScriptException, IOException, NotFoundException, ScanRuleLoadFailureException{

		int rulesBaselined = 0;
		//ScanData scanData = new ScanData(Application.getApplication());
		
		for (long ruleID : rules) {
			
			ScanRule rule = ScanRuleLoader.getScanRule( ruleID );
			if( rule instanceof HttpSeekingScanRule ){
				HttpSeekingScanRule httpRule = (HttpSeekingScanRule) rule;
				httpRule.baseline();
				rulesBaselined++;
			}
			
			//context.addMessage("Rule " + ruleID + " was unsuccessfully baselined (" + e.getMessage() + ")", MessageSeverity.ALERT);			
		}
		
		context.addMessage(rulesBaselined + " rules have been re-baselined", MessageSeverity.SUCCESS);
	}
	
	/**
	 * Perform a scan of the given rule.
	 * @param sessionIdentifier
	 * @param ruleId
	 * @param archiveResults
	 * @return
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws NoSessionException 
	 * @throws DuplicateEntryException 
	 * @throws ViewFailedException 
	 * @throws NotFoundException 
	 * @throws NoDatabaseConnectionException 
	 * @throws SQLException 
	 */
	private WorkerThreadDescriptor scanRules( RequestContext context, long[] rules, boolean archiveResults ) throws GeneralizedException, InsufficientPermissionException, NoSessionException, DuplicateEntryException, ViewFailedException, SQLException, NoDatabaseConnectionException, NotFoundException{
		
		// 0 -- Precondition check
		
		//	0.1 -- Get the user that is performing the action
		UserDescriptor userDescriptor = context.getUser();
		RuleScanWorker worker = new RuleScanWorker(rules);
		long siteGroupID = -1;
		
		Application appRes = Application.getApplication(); 
		
		// 1 -- Make the user can execute each rule
		try {
			for (long ruleID : rules) {
				if(siteGroupID == -1){
					siteGroupID = ScanRule.getAssociatedSiteGroup(ruleID);
				}
				
				ScanRule scanRule = ScanRuleLoader.getScanRule(ruleID);
				long ruleObjectId = scanRule.getObjectId();
				Shortcuts.checkExecute(context.getSessionInfo(), ruleObjectId, "Scan rule " + ruleID);
			}
			
			// 2 -- Start the thread to scan each rule
			WorkerThreadDescriptor threadDesc = appRes.addWorkerToQueue(worker, getUniqueScanWorkerID(context.getSessionInfo().getUserId(), siteGroupID));
			
			Thread thread = new Thread(worker);
			thread.setName("Scanner started by user " + userDescriptor.getUserName());
			thread.start();
			
			return threadDesc;
			
		}
		catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		}
		catch(DuplicateEntryException e){
			throw e;
		}
		catch (Exception e) {
			StringBuffer rulesString = new StringBuffer();
			
			for (int c = 0; c < rules.length; c++) {
				if( c == 0 ){
					rulesString.append("[").append(c);
				}
				else{
					rulesString.append(",").append(c);
				}
			}
			
			rulesString.append("]");
			
			Application.getApplication().logExceptionEvent( new EventLogMessage(EventLogMessage.Category.OPERATION_FAILED,
					new EventLogField[]{
						new EventLogField( FieldName.OPERATION, "Scan rule" ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( FieldName.RULE_ID, rulesString.toString() )} )
					, e);
			throw new ViewFailedException(e);
		}
	}
	
	protected long[] getRules( HttpServletRequest request ){
		Vector<Long> ruleIDs = new Vector<Long>();
		
		for( String param : request.getParameterValues("RuleID") ){
			ruleIDs.add( Long.valueOf(param));
		}
		
		long[] a = new long[ruleIDs.size()];
		
		for( int c =0; c < a.length; c++ ){
			a[c] = ruleIDs.get(c);
		}
		
		return a;
		
		
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {
		
		try{
			Shortcuts.addDashboardHeaders(request, response, data);
			
			// 1 -- Get the Site Group
			SiteGroupDescriptor siteGroup = null;
			
			if( args.length > 0 ){
				SiteGroupManagement mgmr = new SiteGroupManagement(Application.getApplication());
				try {
					siteGroup = mgmr.getGroupDescriptor( Integer.valueOf( args[0]) );
				} catch (NotFoundException e) {
					Dialog.getDialog(response, context, data, "The SiteGroup ID provided is not valid", "SiteGroup ID Invalid", DialogType.WARNING);
					return true;
				}
				catch (NumberFormatException e) {
					Dialog.getDialog(response, context, data, "The SiteGroup ID provided is not valid", "SiteGroup ID Invalid", DialogType.WARNING);
					return true;
				}
			}
			else{
				Dialog.getDialog(response, context, data, "The SiteGroup ID provided is not valid", "SiteGroup ID Invalid", DialogType.WARNING);
				return true;
			}
			
			// 2 -- Prepare the view
			data.put("title", "Site Groups");
			
			// 	2.1 -- Get the breadcrumbs
			Vector<Link> breadcrumbs = new Vector<Link>();
			breadcrumbs.add( new Link("Main Dashboard", StandardViewList.getURL("main_dashboard")) );
			
			if( siteGroup != null ){
				breadcrumbs.add( new Link("Site Group: " + siteGroup.getGroupName(), createURL( siteGroup.getGroupId() )) );
			}
			
			data.put("breadcrumbs", breadcrumbs);
			
			//	2.2 -- Get the menu
			data.put("menu", Menu.getSiteGroupMenu(context, siteGroup));
			
			// 3 -- Check permissions
			try {
				if( Shortcuts.canRead( context.getSessionInfo(), siteGroup.getObjectId()) == false ){
					data.put("permission_denied_message", "You do not permission to view this site group.");
					//data.put("permission_denied_link", new Link("View Site Group", SiteGroupView.getURL(siteGroup)) );
					TemplateLoader.renderToResponse("PermissionDenied.ftl", data, response);
					return true;
				}
			} catch (GeneralizedException e) {
				throw new ViewFailedException(e);
			}
			
			// 4 -- Determine if the site group is being scanned (show the dialog if it is)
			WorkerThreadDescriptor worker = null;
			
			if( siteGroup != null ){
				String threadName = getUniqueScanWorkerID(context.getSessionInfo().getUserId(), siteGroup.getGroupId());
				worker = Application.getApplication().getWorkerThread( threadName );
			}

			// 5 -- Determine if just the progress dialog should be shown (e.g. AJAX request) 
			boolean isAjax = (request.getParameter("AJAX") != null);
			
			// 6 -- Invoke the action
			boolean startedNow = false;
			
			if( !isAjax ){
				
				// 6.1 -- Start the scanner if requested and not already running
				if( "Scan".equalsIgnoreCase( request.getParameter("Action") ) ) {
					//Shortcuts.checkRight( context.getSessionInfo(), "System.Configuration.Edit"); //TODO Check permissions
					startedNow = true;
					try {
						worker = scanRules(context, getRules(request), true);
					} catch (DuplicateEntryException e) {
						//Ignore, the scanner thread was already started
					}
				}
				
				// 6.2 -- Cancel the running scan
				if( "CancelScan".equalsIgnoreCase( request.getParameter("Action") ) ) {
					worker.getWorkerThread().terminate();
				}
				
				// 6.3 -- Delete the rules selected
				if( "Delete".equalsIgnoreCase( request.getParameter("Action") ) ) {
					deleteRules(context, getRules(request));
				}
				
				// 6.4 -- Baseline the rules selected
				if( "Baseline".equalsIgnoreCase( request.getParameter("Action") ) ) {
					baselineRules(context, getRules(request));
				}
			}
			
			// 7 -- Post the progress dialog if a backup is underway
			
			//	 7.1 -- Post a dialog indicating that the backup is complete if the task is done
			if( isAjax && worker == null ){
				response.getWriter().print( Dialog.getProgressDialog("Scanning complete", "Scanning", 100, new Link("OK", createURL(siteGroup.getGroupId()))) );
				return true;
			}
			
			//   7.2 -- Post a dialog indicating the task is complete
			else if( isAjax && (worker == null || worker.getWorkerThread().getStatus() == State.STOPPED) ){
				response.getWriter().print( Dialog.getProgressDialog(worker.getWorkerThread().getStatusDescription(), worker.getWorkerThread().getTaskDescription(), 100, new Link("OK", createURL(siteGroup.getGroupId()))) );
				return true;
			}
			
			//	 7.3 -- Post the progress dialog otherwise
			else if( isAjax ){
				response.getWriter().print( Dialog.getProgressDialog(worker.getWorkerThread().getStatusDescription(), worker.getWorkerThread().getTaskDescription(), worker.getWorkerThread().getProgress()) );
				return true;
			}
			
			//	 7.4 -- Add the dashboard headers
			Shortcuts.addDashboardHeaders(request, response, data);
			
			//	 7.5 -- Post the progress dialog
			if( worker != null && (startedNow || worker.getWorkerThread().getStatus() == State.STARTING || worker.getWorkerThread().getStatus() == State.STARTED ) ){
				data.put("ajaxurl", createURL(siteGroup.getGroupId()) + "?AJAX=True");
				data.put("title", "Scanning");
				data.put("content", Dialog.getProgressDialog(worker.getWorkerThread().getStatusDescription(), worker.getWorkerThread().getTaskDescription(), worker.getWorkerThread().getProgress()) );
				
				response.getWriter().println( TemplateLoader.renderToString("AJAXProgressDialog.ftl", data) );
				
				return true;
			}
			

			// 8 -- Get a description of the scan results
			RuleResult result;
			try{
				result = getResults(siteGroup);
			} catch(NoDatabaseConnectionException e){
				throw new ViewFailedException(e);
			} catch(SQLException e){
				throw new ViewFailedException(e);
			} catch (NotFoundException e) {
				throw new ViewFailedException(e);
			} catch (ScanResultLoadFailureException e) {
				throw new ViewFailedException(e);
			} catch (ScanRuleLoadFailureException e) {
				throw new ViewFailedException(e);
			}
			
			// 9 -- Prepare the view for the SiteGroup

			//	9.1 -- Add the sitegroup
			data.put("sitegroup", siteGroup);
			if( result != null ){
				data.put("scanrules", result.scanRules);
				data.put("scanresults", result.scanResults);
				data.put("rules", result.ruleStatuses);
			}
			
			//	9.2 -- Render the page
			data.put("STAT_GREEN", RuleState.STAT_GREEN);
			data.put("STAT_BLUE", RuleState.STAT_BLUE);
			data.put("STAT_YELLOW", RuleState.STAT_YELLOW);
			data.put("STAT_RED", RuleState.STAT_RED);
			
			if( siteGroup != null ){
				Shortcuts.addDashboardHeaders(request, response, data, createURL(siteGroup.getGroupId()));
			}
			else{
				Shortcuts.addDashboardHeaders(request, response, data);
			}
			
			if( siteGroup != null ){
				TemplateLoader.renderToResponse("SiteGroup.ftl", data, response);
			}
			
			return true;
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (InputValidationException e) {
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		} catch (NotFoundException e) {
			throw new ViewFailedException(e);
		} catch (DefinitionSetLoadException e) {
			throw new ViewFailedException(e);
		} catch (ScriptException e) {
			throw new ViewFailedException(e);
		} catch (ScanRuleLoadFailureException e) {
			throw new ViewFailedException(e);
		}catch (GeneralizedException e) {
			throw new ViewFailedException(e);
		} catch (InsufficientPermissionException e) {
			throw new ViewFailedException(e);
		} catch (NoSessionException e) {
			throw new ViewFailedException(e);
		}
	}

}
