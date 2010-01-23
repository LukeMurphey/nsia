package net.lukemurphey.nsia.htmlInterface;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

/*import net.lukemurphey.nsia.scanRules.Definition;
import net.lukemurphey.nsia.scanRules.DefinitionMatch;
import net.lukemurphey.nsia.trustBoundary.ApiScanData;*/
import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.DuplicateEntryException;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.ScannerController;
import net.lukemurphey.nsia.WorkerThread;
import net.lukemurphey.nsia.Application.ApplicationStatusDescriptor;
import net.lukemurphey.nsia.Application.WorkerThreadDescriptor;
import net.lukemurphey.nsia.LicenseManagement.LicenseDescriptor;
import net.lukemurphey.nsia.WorkerThread.State;
import net.lukemurphey.nsia.eventlog.EventLog;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.htmlInterface.Html.MessageType;
import net.lukemurphey.nsia.trustBoundary.ApiApplicationConfiguration;
import net.lukemurphey.nsia.trustBoundary.ApiMaintenanceTasks;
import net.lukemurphey.nsia.trustBoundary.ApiScannerController;
import net.lukemurphey.nsia.trustBoundary.ApiSystem;


public class HtmlSystemStatus extends HtmlContentProvider{
	private static final String tableStart = "<table width=\"640px\" class=\"DataTable\">";
	private static final String tableEnd = "</table><p>";
	
	private static int OP_SCANNER_START_SUCCESS = 100;
	private static int OP_SCANNER_START_FAILED = 101;
	private static int OP_SCANNER_STOP_SUCCESS = 102;
	private static int OP_SCANNER_STOP_FAILED = 103;
	private static int OP_SHUTDOWN_FAILED = 104;
	private static int OP_SHUTDOWN_SUCCESS = 105;
	private static int OP_SHUTDOWN_REQUEST = 106;
	private static int OP_REINDEX_SUCCESS = 107;
	private static int OP_REINDEX_FAILED = 108;

	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException{
		return getHtml( requestDescriptor, null );
	}
	
	private static ContentDescriptor getShutdownForm(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException{

		Hashtable<String, String> parameters = new Hashtable<String, String>();
		
		parameters.put("Mode", "SystemStatus");
		parameters.put("Action", "Shutdown");
		
		return HtmlOptionDialog.getHtml(requestDescriptor, "Shutdown System", "Are you sure you want to shutdown the manager?", parameters, new String[]{"Shutdown", "Cancel"}, "SystemStatus");
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException{
		
		//Perform any pending actions
		if( actionDesc == null ){
			actionDesc = performAction( requestDescriptor );
		}
		
		// Show the progress dialog if the index defragmenter was started
		WorkerThreadDescriptor threadDesc = Application.getApplication().getWorkerThread("Index Defragmenter (unscheduled)");
		if( threadDesc != null ){
			WorkerThread worker = threadDesc.getWorkerThread();
			if( worker.getStatus() != State.STOPPED ){
				try{
					return HtmlOptionDialog.getHtml(requestDescriptor, "Defragmenting Indexes", worker.getStatusDescription(), new Hashtable<String, String>(), new String[]{"Cancel"}, "", HtmlOptionDialog.DIALOG_INFORMATION, "SystemStatus", "/Ajax/Task/" + java.net.URLEncoder.encode( threadDesc.getUniqueName(), "US-ASCII") );
				}
				catch(UnsupportedEncodingException e){
					Application.getApplication().logExceptionEvent(EventLogMessage.Category.WEB_ERROR, e);
					throw new GeneralizedException();
				}
			}
		}

		
		if( actionDesc.result == OP_SHUTDOWN_REQUEST  )
			return getShutdownForm(requestDescriptor, actionDesc);
		else
			return getStatusForm(requestDescriptor, actionDesc);
	}
	
	public static ContentDescriptor getStatusForm(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException{
		StringBuffer body = new StringBuffer();
		String title = "System Status";
		
		// 1 -- Get the data
		ApplicationStatusDescriptor managerStatusDesc =  requestDescriptor.appStatusDesc;
		//ApiSystem system = new ApiSystem(Application.getApplication());
		
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		//	 1.1 -- Output the performance metrics
		body.append( "<div class=\"SectionHeader\">Operational Metrics</div>" );
		body.append( tableStart );
		
		//		1.1.1 -- Output the memory status
		Application.StatusEntry statusEntry = managerStatusDesc.getStatusEntry("Memory Utilization");
		if (statusEntry.getStatus() == Application.ApplicationStatusDescriptor.STATUS_RED )
			body.append( createRow( "Memory Used", getErrorNote( Html.getBytesDescription( requestDescriptor.application.getUsedMemory() )  + " (free memory critical)") ) );
		else if (statusEntry.getStatus()  == Application.ApplicationStatusDescriptor.STATUS_YELLOW)
			body.append(createRow( "Memory Used", getWarningNote( Html.getBytesDescription( requestDescriptor.application.getUsedMemory() ) + " (free memory low)" ) ) );
		else
			body.append(createRow( "Memory Used", getCheckNote( Html.getBytesDescription( requestDescriptor.application.getUsedMemory() ) ) ) );
		
		//		1.1.2 -- Output the memory available
		body.append( createRow( "Memory Available",  getInfoNote( Html.getBytesDescription( requestDescriptor.application.getMaxMemory()) ) ) );
		
		//		1.1.3 -- Output the number of threads
		statusEntry = managerStatusDesc.getStatusEntry("Thread Count");
		if (statusEntry.getStatus() == Application.ApplicationStatusDescriptor.STATUS_RED )
			body.append(createRow( "Threads Executing",  getErrorNote( String.valueOf ( requestDescriptor.application.getThreadCount()) + " (Thread count excessive)") ));
		else if (statusEntry.getStatus() == Application.ApplicationStatusDescriptor.STATUS_YELLOW)
			body.append(createRow( "Threads Executing",  getWarningNote( String.valueOf ( requestDescriptor.application.getThreadCount()) + " (Thread count high)") ));
		else
			body.append(createRow( "Threads Executing",  getCheckNote( String.valueOf ( requestDescriptor.application.getThreadCount())) ) );
		
		//		1.1.4 -- Output the number of database connections
		statusEntry = managerStatusDesc.getStatusEntry("Database Connections");
		if (statusEntry.getStatus() == Application.ApplicationStatusDescriptor.STATUS_RED )
			body.append(createRow( "Database Connections",  getErrorNote( String.valueOf ( requestDescriptor.application.getDatabaseConnectionCount()) + " (Connection count excessive)") ));
		else if (statusEntry.getStatus() == Application.ApplicationStatusDescriptor.STATUS_YELLOW)
			body.append(createRow( "Database Connections",  getWarningNote( String.valueOf ( requestDescriptor.application.getDatabaseConnectionCount()) + " (Connection count high)") ));
		else
			body.append(createRow( "Database Connections",  getCheckNote( String.valueOf ( requestDescriptor.application.getDatabaseConnectionCount())) ) );
		
		//		1.1.5 -- Output the scanner status
		statusEntry = managerStatusDesc.getStatusEntry("Scanner Status");
		if ( statusEntry.getStatus()== Application.ApplicationStatusDescriptor.STATUS_RED )
			body.append(createRow( "Scanner Status",  getErrorNote( "Non-Operational") ) );
		else if (statusEntry.getStatus() == Application.ApplicationStatusDescriptor.STATUS_YELLOW)
			body.append(createRow( "Scanner Status",  getWarningNote( "Paused (rules are not being evaluated)") ) );
		else
			body.append(createRow( "Scanner Status",  getCheckNote( "Operational") ) );
		
		//		1.1.6 -- Output the uptime
		body.append( createRow( "Uptime",  getInfoNote( Html.getTimeDescription( requestDescriptor.application.getUptime()/1000 ) ) ) );
		
		//	 	1.1.7 -- Output the graph
		body.append( "<tr><td height=\"120px\" colspan=\"2\" class=\"BackgroundLoading1\"><span class=\"AlignCenter\"><img src=\"/StatusGraph\" alt=\"StatusGraph\"></span></td></tr>");
		body.append( "<tr><td height=\"120px\" colspan=\"2\" class=\"BackgroundLoading1\"><span class=\"AlignCenter\"><img src=\"/RulesEvalGraph\" alt=\"RuleStatGraph\"></span></td></tr>");
		
		body.append( tableEnd );
		
		//	 1.2 -- Output the license information
		body.append( "<div class=\"Text_2\">License</div>");
		body.append(tableStart);
		
		ApiApplicationConfiguration config = new ApiApplicationConfiguration(requestDescriptor.application);
		
		LicenseDescriptor licenseDescriptor = null;
		
		// If validation of the license fails, then post a warning.
		licenseDescriptor = config.getLicense(requestDescriptor.sessionIdentifier);

		String key = config.getLicenseKey(requestDescriptor.sessionIdentifier);

		if( key == null && licenseDescriptor == null ){
			body.append( createRow( "Key",  getWarningNote( "Validation Failed" ) ) );
		}
		else if(licenseDescriptor == null) {
			body.append( createRow( "Key",  getWarningNote( key + " (" + "Validation Failed" + ")" ) ) );
		}
		
		// If the license was returned, then post the information
		if( licenseDescriptor != null ){
			boolean validLicense = licenseDescriptor.isValid();
			
			if( validLicense == false && licenseDescriptor.getKey() != null){
				body.append( createRow( "Key", getWarningNote(  licenseDescriptor.getKey() + " (" + licenseDescriptor.getStatus().getDescription() + ")" ) ) );
			}
			else if( validLicense == false && licenseDescriptor.getKey() == null){
				body.append( createRow( "Key", getWarningNote( licenseDescriptor.getStatus().getDescription()) ) );
			}
			else if( validLicense == true ){
				body.append( createRow( "Key",  getCheckNote( licenseDescriptor.getKey() + " (" + licenseDescriptor.getStatus().getDescription() + ")" )  ) );
			}
			
			if( licenseDescriptor.getExpirationDate() != null){
				if( validLicense == false && licenseDescriptor.getExpirationDate().before( new Date() ) ){
					body.append( createRow( "Expiration Date", getWarningNote(  licenseDescriptor.getExpirationDate().toString() + " (Expired)" ) ) );
				}
				else{
					body.append( createRow( "Expiration Date", getInfoNote(  licenseDescriptor.getExpirationDate().toString() ) ) );
				}
			}
			
			if( licenseDescriptor.getLicensee() != null){
				body.append( createRow( "Licensee", getInfoNote(  licenseDescriptor.getLicensee() ) ) );
			}
			
			if( licenseDescriptor.getType() != null && licenseDescriptor.getType().toString() != null){
				body.append( createRow( "Type", getInfoNote(  licenseDescriptor.getType().toString().toLowerCase() ) ) );
			}
		}
		
		body.append( tableEnd );
		
		//	 1.3 -- Print out the log server information
		EventLog eventlog = Application.getApplication().getEventLog();
		
		if( eventlog.isExternalLoggingEnabled() ){
			body.append( "<div class=\"Text_2\">External Log Server</div>");
			body.append(tableStart);
			
			// 	1.3.1 -- External log server information
			body.append(createRow( "Server",  getInfoNote( eventlog.getLogServerAddress() + ":" + eventlog.getLogServerPort() + " (" + eventlog.getLogServerProtocol() + ")") ));
			
			//  1.3.2 -- External log server status
			if( eventlog.isLogServerResponding() ){
				body.append(createRow( "Status",  getCheckNote( "No Errors Noted") ));
			}
			else{
				body.append(createRow( "Status",  getWarningNote( "Log Server Not Responding") ));
			}
			
			//  1.3.3 -- Log cache information
			int max = eventlog.getMaxLogCacheSize();
			int actual = eventlog.getLogCacheSize();
			
			if( actual >= max){
				body.append(createRow( "Cache",  getErrorNote( "Log cache filled, events being discarded" ) ));
			}
			else if (actual > 0){
				int percentage = (int)Math.round( actual * 100.0 / max );
				if( percentage == 0 ){
					percentage = 1;
				}
				
				body.append(createRow( "Cache",  getWarningNote( percentage + "% buffer used (" + actual + " messages cached)") ));
			}
			else{
				body.append(createRow( "Cache",  getCheckNote( "0 log messages cached" ) ));
			}
			
			
			body.append( tableEnd );
		}
		
		
		//	 1.4 -- Output the configuration data
		body.append( "<div class=\"Text_2\">Configuration</div>");
		body.append(tableStart);
		
		//		1.4.1 -- Database connection
		body.append(createRow( "Database Connection",  getInfoNote( requestDescriptor.application.getDatabaseInfo()) ));
		
		try{
			body.append(createRow( "Database Name/Version",  getInfoNote( requestDescriptor.application.getDatabaseName() + " " + requestDescriptor.application.getDatabaseVersion() ) ));
			body.append(createRow( "Database Driver Name/Version",  getInfoNote( requestDescriptor.application.getDatabaseDriverName() + " " + requestDescriptor.application.getDatabaseDriverVersion() ) ));
			
		}
		catch(SQLException e){
			//Continue on, the database information is not important. The exception may be a transient error and will caught by another error handler if it is not.
		}
		catch(NoDatabaseConnectionException e){
			//Continue on, the database information is not important. The exception may be a transient error and will caught by another error handler if it is not.
		}
		
		//		1.4.2 -- Environment configuration
		body.append( createRow( "JVM Vendor", getInfoNote(requestDescriptor.application.getJvmVendor()) ) );
		body.append( createRow( "JVM Version", getInfoNote(requestDescriptor.application.getJvmVersion()) ));
		body.append( createRow( "Operating System Name", getInfoNote(requestDescriptor.application.getOperatingSystemName()) ));
		body.append( createRow( "Operating System Version", getInfoNote(requestDescriptor.application.getOperatingSystemVersion()) ));
		body.append( createRow( "Architecture", getInfoNote(requestDescriptor.application.getPlatformArch()) ));
		body.append( createRow( "Manager Version", getInfoNote(Application.getVersion()) ));
		body.append( createRow( "Processor Count", getInfoNote(String.valueOf( requestDescriptor.application.getProcessorCount())) ));
		body.append( createRow( "Server Port",  getInfoNote(String.valueOf( requestDescriptor.application.getNetworkManager().getServerPort()) )));
	
		if(  requestDescriptor.application.getNetworkManager().sslEnabled() == false )
			body.append(createRow( "SSL Enabled", getWarningNote("False (data transmission is not confidential)") ));
		else
			body.append(createRow( "SSL Enabled", getCheckNote( "True" ) ));
		body.append(tableEnd );
		
		/*
		// 1.5 -- Output the signatures firings
		body.append( "<div class=\"Text_2\">Most Recent Signatures Triggered</div>");
		body.append( "<table width=\"640px\" class=\"DataTable\"><tbody>" );//<thead><tr><td>Signature</td></tr></thead>
		
		ApiScanData scanData = new ApiScanData(Application.getApplication());
		DefinitionMatch[] sigs = scanData.getLastSignaturesMatched(requestDescriptor.sessionIdentifier, 10);
		for(int c = 0; c < sigs.length; c++){
			if( sigs[c].getSeverity() == Definition.Severity.HIGH ){
				body.append( "<tr><td width=\"16px\" class=\"StatRedSmall\"><img style=\"padding: 3px;\" src=\"/16_Alert\"></td>");
			}
			else if( sigs[c].getSeverity() == Definition.Severity.MEDIUM ){
				body.append( "<tr><td width=\"16px\" class=\"StatYellowSmall\"><img style=\"padding: 3px;\" src=\"/16_Warning\"></td>");
			}
			else if( sigs[c].getSeverity() == Definition.Severity.LOW ){
				body.append( "<tr><td width=\"16px\" class=\"StatYellowSmall\"><img style=\"padding: 3px;\" src=\"/16_Warning\"></td>");
			}
			else {
				body.append( "<tr class=\"background1\"><td width=\"16px\" class=\"StatBlueSmall\"><img style=\"padding: 3px;\" src=\"/16_Information\"></td>");
			}
			
			body.append( "<td><a href=\"Signatures?Action=Edit&ID=" + sigs[c].getDefinitionID() + "\">" + sigs[c].getDefinitionName() + "</a></td>");
			
			if( sigs[c].getSeverity() == Definition.Severity.HIGH ){
				body.append( "<td class=\"StatRedSmall\">&nbsp;</td></tr>");
			}
			else if( sigs[c].getSeverity() == Definition.Severity.MEDIUM ){
				body.append( "<td class=\"StatYellowSmall\">&nbsp;</td></tr>");
			}
			else if( sigs[c].getSeverity() == Definition.Severity.LOW ){
				body.append( "<td class=\"StatYellowSmall\">&nbsp;</td></tr>");
			}
			else {
				body.append( "<td class=\"StatBlueSmall\">&nbsp;</td></tr>");
			}
		}
		
		body.append( "</tbody></table>" );*/
		
		// 2 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "System Status", "/SystemStatus");
		String navigationHtml = Html.getNavigationPath( navPath );
		
		// 3 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
		
		menuItems.add( new MenuItem("System Administration", null, MenuItem.LEVEL_ONE) );		
		menuItems.add( new MenuItem("Modify Configuration", "/SystemConfiguration", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Event Log", "/EventLog", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Shutdown System", "/SystemStatus?Action=Shutdown", MenuItem.LEVEL_TWO) );
		if( Application.getApplication().isUsingInternalDatabase() == true ){
			menuItems.add( new MenuItem("Create Backup", "/DatabaseBackup", MenuItem.LEVEL_TWO) );
			menuItems.add( new MenuItem("Defragment Indexes", "/SystemStatus?Action=StartDefragmenter", MenuItem.LEVEL_TWO) );
		}
		
		
		menuItems.add( new MenuItem("Scanning Engine", null, MenuItem.LEVEL_ONE) );
		ApiScannerController scannerController = new ApiScannerController(Application.getApplication());
		
		try{
			if(scannerController.getScanningState(requestDescriptor.sessionIdentifier) == ScannerController.ScannerState.RUNNING)
				menuItems.add( new MenuItem("Stop Scanner", "/SystemStatus?Action=StopScanner", MenuItem.LEVEL_TWO) );
			else
				menuItems.add( new MenuItem("Start Scanner", "/SystemStatus?Action=StartScanner", MenuItem.LEVEL_TWO) );
		}
		catch(InsufficientPermissionException e){
			//Ignore this, it just means we can show the option to start and restart the scanner
		}
		menuItems.add( new MenuItem("View Definitions", "/Definitions", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Default Scan Policy", "/ScanPolicy", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Site Scan", "/SiteScan", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Site Groups", "/", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add Site Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("User Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Users", "/UserManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New User", "/UserManagement?Action=Add", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Logged in Users", "/Sessions", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("Group Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Groups", "/GroupManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New Group", "/GroupManagement?Action=Add", MenuItem.LEVEL_TWO) );
				
		String menuOutput = Html.getMenu( menuItems );
		
		// 4 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor( title, pageOutput );
	}
	
	private static String createRow( String title, String body ){
		StringBuffer output = new StringBuffer();
		output.append("<tr><td width=\"300\" class=\"Background1\"><div class=\"Text_3\">");
		output.append( title );
		output.append( "</div></td><td class=\"Background1\">");
		output.append( body );
		output.append( "</td></tr>" );
		return output.toString();
	}
	
	private static String getWarningNote( String message ){
		StringBuffer output = new StringBuffer();

		output.append( "<table><tr><td><img src=\"/16_Warning\" alt=\"Warning\"></td>" );
		output.append( "<td class=\"WarnText\">" );
		output.append( message );
		output.append( "</td></tr></table>" );
		
		return output.toString();
	}
	
	private static String getCheckNote( String message ){
		StringBuffer output = new StringBuffer();
		
		output.append( "<table><tr><td><img src=\"/16_Check\" alt=\"OK\"></td>" );
		output.append( "<td>" );
		output.append( message );
		output.append( "</td></tr></table>" );
		//output.append( "<img style=\"vertical-align: middle; margin: 3px 2px 3px 2px;\" src=\"/16_Check\" alt=\"OK\">" );
		//output.append( message );
		
		return output.toString();
	}
	
	private static String getErrorNote( String message ){
		StringBuffer output = new StringBuffer();
		output.append( "<table><tr><td><img src=\"/16_Alert\" alt=\"Alert\"></td>" );
		output.append( "<td class=\"WarnText\">" );
		output.append( message );
		output.append( "</td></tr></table>" );
		
		return output.toString();
	}
	
	private static String getInfoNote( String message ){
		StringBuffer output = new StringBuffer();
		output.append( "<table><tr><td><img src=\"/16_Information\" alt=\"Info\"></td>" );
		output.append( "<td>" );
		output.append( message );
		output.append(  "</td></tr></table>" );
		
		return output.toString();
	}
	
	/*private static String getImageNote( String message, String image, String altText ){
		StringBuffer output = new StringBuffer();
		output.append( "<table><tr><td><img src=\"" );
		output.append( image );
		output.append( "\" alt=\"" );
		output.append( altText );
		output.append( "\"></td>" );
		output.append( "<td>" );
		output.append( message );
		output.append( "</td></tr></table>" );
		
		//String output = "<table><tr><td><img src=\"/16_Information\" alt=\"Info\"></td>";
		//output += "<td>" + message + "<td></tr></table>";
		
		return output.toString();
	}*/
	
	private static ActionDescriptor performAction(WebConsoleConnectionDescriptor requestDescriptor) throws GeneralizedException, NoSessionException{
		String action = requestDescriptor.request.getParameter("Action");
		
		if( action == null )
			return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION );
		
		if( action.matches("StartScanner") ){
			ApiScannerController apiScannerController = new ApiScannerController( Application.getApplication() );
			try{
				apiScannerController.enableScanning( requestDescriptor.sessionIdentifier );
				requestDescriptor.appStatusDesc = Application.getApplication().getManagerStatus();
				Html.addMessage(MessageType.INFORMATIONAL, "The scanner was successfully given the start command", requestDescriptor.userId.longValue());
				
				return new ActionDescriptor( OP_SCANNER_START_SUCCESS );
			}catch( InsufficientPermissionException e ){
				Html.addMessage(MessageType.WARNING, "You do not have permission to control the scanner", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_SCANNER_START_FAILED );
			}
		}
		else if( action.matches("StopScanner") ){
			ApiScannerController apiScannerController = new ApiScannerController( Application.getApplication() );
			try{
				apiScannerController.disableScanning( requestDescriptor.sessionIdentifier );
				requestDescriptor.appStatusDesc = Application.getApplication().getManagerStatus();
				
				Html.addMessage(MessageType.INFORMATIONAL, "The scanner was successfully given the stop command", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_SCANNER_STOP_SUCCESS );
			}catch( InsufficientPermissionException e ){
				Html.addMessage(MessageType.WARNING, "You do not have permission to control the scanner", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_SCANNER_STOP_FAILED );
			}
		}
		if( action.matches("StartDefragmenter") ){
			ApiMaintenanceTasks apiMaintenanceTasks = new ApiMaintenanceTasks( Application.getApplication() );
			try{
				if( apiMaintenanceTasks.startDatabaseReindexer( requestDescriptor.sessionIdentifier ) == true ){
					return new ActionDescriptor( OP_REINDEX_SUCCESS );
				}
				
				return new ActionDescriptor( OP_REINDEX_FAILED );
				
			}catch( InsufficientPermissionException e ){
				Html.addMessage(MessageType.WARNING, "You do not have permission to re-index the database", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_REINDEX_SUCCESS );
			}
			catch( DuplicateEntryException e ){
				Html.addMessage(MessageType.WARNING, "The database reindexer was already started", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_REINDEX_FAILED );
			}
		}
		else if( action.matches("Shutdown") ){
			
			if(requestDescriptor.request.getParameter("Selected") == null ){
				return new ActionDescriptor(OP_SHUTDOWN_REQUEST);
			}
			else if(requestDescriptor.request.getParameter("Selected") != null && requestDescriptor.request.getParameter("Selected").equals("Shutdown") ){
				ApiSystem system = new ApiSystem(Application.getApplication());
				
				try{
					system.shutdownSystem(requestDescriptor.sessionIdentifier);
				}catch( InsufficientPermissionException e ){
					Html.addMessage(MessageType.WARNING, "You do not have permission to shutdown the server", requestDescriptor.userId.longValue());

					return new ActionDescriptor( OP_SHUTDOWN_FAILED );
				}
				
				return new ActionDescriptor(OP_SHUTDOWN_SUCCESS);
			}		

		}
		
		return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION );
	}
}
