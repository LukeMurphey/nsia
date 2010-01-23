package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.ApplicationConfiguration;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.Application.ApplicationStatusDescriptor;
import net.lukemurphey.nsia.LicenseManagement.LicenseDescriptor;
import net.lukemurphey.nsia.eventlog.EventLog;
import net.lukemurphey.nsia.htmlInterface.Html;
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

public class SystemStatusView extends View {

	public SystemStatusView() {
		super("System/Status", "system_status");
	}

	protected enum StatType{
		INFO, CHECK, WARNING, ERROR;
	}
	
	public class SystemStat{
		private String title;
		private String message;
		private StatType stat_type;
		
		public SystemStat( String title, String message, StatType stat_type ){
			this.title = title;
			this.message = message;
			this.stat_type = stat_type;
		}
		
		public boolean isError(){
			return stat_type == StatType.ERROR;
		}
		
		public boolean isInfo(){
			return stat_type == StatType.INFO;
		}
		
		public boolean isWarning(){
			return stat_type == StatType.WARNING;
		}
		
		public boolean isCheck(){
			return stat_type == StatType.CHECK;
		}
		
		public String getTitle(){
			return title;
		}
		
		public String getMessage(){
			return message;
		}
		
	}
	
	@Override
	protected boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException, URLInvalidException, IOException, ViewNotFoundException {
		
		// 1 -- Check permissions
		try {
			Shortcuts.checkRight( context.getSessionInfo(), "System.Configuration.View");
		} catch (InsufficientPermissionException e) {
			Dialog.getDialog(response, context, data, "You do not have permission to view the system confogiration", "Permission Denied", DialogType.INFORMATION, new Link("Return to Dashboard", StandardViewList.getURL("main_dashboard")));
		} catch (GeneralizedException e) {
			throw new ViewFailedException(e);
		} catch (NoSessionException e) {
			throw new ViewFailedException(e);
		}
		
		Vector<SystemStat> system_stats = new Vector<SystemStat>();
		
		// 2 -- Get the system stats
		Application application = Application.getApplication();
		ApplicationStatusDescriptor managerStatusDesc = application.getManagerStatus();
		
		//	 2.1 -- Get the memory status
		Application.StatusEntry statusEntry = managerStatusDesc.getStatusEntry("Memory Utilization");
		if (statusEntry.getStatus() == Application.ApplicationStatusDescriptor.STATUS_RED )
			system_stats.add( new SystemStat("Memory Used", Html.getBytesDescription( application.getUsedMemory() )  + " (free memory critical)", StatType.ERROR));
		else if (statusEntry.getStatus()  == Application.ApplicationStatusDescriptor.STATUS_YELLOW)
			system_stats.add( new SystemStat("Memory Used", Html.getBytesDescription( application.getUsedMemory() )  + " (free memory low)", StatType.WARNING));
		else
			system_stats.add( new SystemStat("Memory Used", Html.getBytesDescription( application.getUsedMemory() ), StatType.CHECK));
		
		//	 2.2 -- Get the memory available
		system_stats.add( new SystemStat("Memory Available", Html.getBytesDescription( application.getMaxMemory() ), StatType.INFO));
		
		//	 2.3 -- Get the number of threads
		statusEntry = managerStatusDesc.getStatusEntry("Thread Count");
		if (statusEntry.getStatus() == Application.ApplicationStatusDescriptor.STATUS_RED )
			system_stats.add( new SystemStat("Threads Executing", application.getThreadCount() + " (Thread count excessive)", StatType.ERROR));
		else if (statusEntry.getStatus() == Application.ApplicationStatusDescriptor.STATUS_YELLOW)
			system_stats.add( new SystemStat("Threads Executing", application.getThreadCount() + " (Thread count high)", StatType.WARNING));
		else
			system_stats.add( new SystemStat("Threads Executing", String.valueOf( application.getThreadCount() ), StatType.CHECK));
		
		//	 2.4 -- Get the number of database connections
		statusEntry = managerStatusDesc.getStatusEntry("Database Connections");
		if (statusEntry.getStatus() == Application.ApplicationStatusDescriptor.STATUS_RED )
			system_stats.add( new SystemStat("Database Connections", application.getDatabaseConnectionCount() + " (Connection count excessive)", StatType.ERROR));
		else if (statusEntry.getStatus() == Application.ApplicationStatusDescriptor.STATUS_YELLOW)
			system_stats.add( new SystemStat("Database Connections", application.getDatabaseConnectionCount() + " (Connection count high)", StatType.WARNING));
		else
			system_stats.add( new SystemStat("Database Connections", String.valueOf ( application.getDatabaseConnectionCount()), StatType.CHECK));
		
		//	 2.5 -- Get the scanner status
		statusEntry = managerStatusDesc.getStatusEntry("Scanner Status");
		if ( statusEntry.getStatus()== Application.ApplicationStatusDescriptor.STATUS_RED )
			system_stats.add( new SystemStat("Scanner Status", "Non-Operational", StatType.ERROR));
		else if (statusEntry.getStatus() == Application.ApplicationStatusDescriptor.STATUS_YELLOW)
			system_stats.add( new SystemStat("Scanner Status", "Paused (rules are not being evaluated)", StatType.WARNING));
		else
			system_stats.add( new SystemStat("Scanner Status", "Operational", StatType.CHECK));
		
		//	 2.6 -- Get the uptime
		system_stats.add( new SystemStat("Uptime",  Html.getTimeDescription( application.getUptime()/1000 ), StatType.INFO));
		
		data.put("system_stats", system_stats);
		
		
		// 3 -- Get the license information
		Vector<SystemStat> license_stats = new Vector<SystemStat>();
		ApplicationConfiguration config = application.getApplicationConfiguration();
		
		LicenseDescriptor licenseDescriptor = null;
		String key;
		
		// If validation of the license fails, then post a warning.
		try {
			licenseDescriptor = config.getLicense();
			key = config.getLicenseKey();
		} catch (NoDatabaseConnectionException e1) {
			throw new ViewFailedException(e1);
		} catch (SQLException e1) {
			throw new ViewFailedException(e1);
		} catch (InputValidationException e1) {
			throw new ViewFailedException(e1);
		}

		if( key == null && licenseDescriptor == null ){
			license_stats.add( new SystemStat("Key",  "Validation Failed", StatType.WARNING) );
		}
		else if(licenseDescriptor == null) {
			license_stats.add( new SystemStat("Key",   key + " (" + "Validation Failed" + ")", StatType.WARNING) );
		}
		
		// If the license was returned, then post the information
		if( licenseDescriptor != null ){
			boolean validLicense = licenseDescriptor.isValid();
			
			if( validLicense == false && licenseDescriptor.getKey() != null){
				license_stats.add( new SystemStat("Key", licenseDescriptor.getKey() + " (" + licenseDescriptor.getStatus().getDescription() + ")", StatType.WARNING) );
			}
			else if( validLicense == false && licenseDescriptor.getKey() == null){
				license_stats.add( new SystemStat("Key", licenseDescriptor.getStatus().getDescription(), StatType.WARNING) );
			}
			else if( validLicense == true ){
				license_stats.add( new SystemStat("Key", licenseDescriptor.getKey() + " (" + licenseDescriptor.getStatus().getDescription() + ")", StatType.CHECK) );
			}
			
			if( licenseDescriptor.getExpirationDate() != null){
				if( validLicense == false && licenseDescriptor.getExpirationDate().before( new Date() ) ){
					license_stats.add( new SystemStat("Expiration Date",  licenseDescriptor.getExpirationDate().toString() + " (Expired)", StatType.WARNING) );
				}
				else{
					license_stats.add( new SystemStat("Expiration Date",  licenseDescriptor.getExpirationDate().toString(), StatType.INFO) );
				}
			}
			
			if( licenseDescriptor.getLicensee() != null){
				license_stats.add( new SystemStat("Licensee", licenseDescriptor.getLicensee(), StatType.INFO) );
			}
			
			if( licenseDescriptor.getType() != null && licenseDescriptor.getType().toString() != null){
				license_stats.add( new SystemStat("Type",  licenseDescriptor.getType().toString().toLowerCase(), StatType.INFO) );
			}
		}
		
		data.put("license_stats", license_stats);
		
		
		// 4 -- Print out the log server information
		EventLog eventlog = Application.getApplication().getEventLog();
		
		if( eventlog.isExternalLoggingEnabled() ){
			Vector<SystemStat> log_server_stats = new Vector<SystemStat>();
			
			// 	4.1 -- External log server information
			log_server_stats.add( new SystemStat("Server", eventlog.getLogServerAddress() + ":" + eventlog.getLogServerPort() + " (" + eventlog.getLogServerProtocol() + ")", StatType.INFO) );
			
			//  4.2 -- External log server status
			if( eventlog.isLogServerResponding() ){
				log_server_stats.add( new SystemStat("Status", "No Errors Noted", StatType.CHECK) );
			}
			else{
				log_server_stats.add( new SystemStat("Status", "Log Server Not Responding", StatType.WARNING) );
			}
			
			//  4.3 -- Log cache information
			int max = eventlog.getMaxLogCacheSize();
			int actual = eventlog.getLogCacheSize();
			
			if( actual >= max){
				log_server_stats.add( new SystemStat("Cache", "Log cache filled, events being discarded", StatType.ERROR) );
			}
			else if (actual > 0){
				int percentage = (int)Math.round( actual * 100.0 / max );
				if( percentage == 0 ){
					percentage = 1;
				}
				
				log_server_stats.add( new SystemStat("Cache", percentage + "% buffer used (" + actual + " messages cached)", StatType.WARNING) );
			}
			else{
				log_server_stats.add( new SystemStat("Cache", "0 log messages cached", StatType.CHECK) );
			}
			
			data.put("log_server_stats", log_server_stats);
		}
		
		
		// 5 -- Output the configuration data
		Vector<SystemStat> configuration_stats = new Vector<SystemStat>();
		
		//	 5.1 -- Database connection
		configuration_stats.add( new SystemStat("Database Connection", application.getDatabaseInfo(), StatType.INFO) );
		
		try{
			configuration_stats.add( new SystemStat("Database Name/Version", application.getDatabaseName() + " " + application.getDatabaseVersion(), StatType.INFO) );
			configuration_stats.add( new SystemStat("Database Driver Name/Version", application.getDatabaseDriverName() + " " + application.getDatabaseDriverVersion(), StatType.INFO) );
		}
		catch(SQLException e){
			//Continue on, the database information is not important. The exception may be a transient error and will caught by another error handler if it is not.
		}
		catch(NoDatabaseConnectionException e){
			//Continue on, the database information is not important. The exception may be a transient error and will caught by another error handler if it is not.
		}
		
		//	 5.2 -- Environment configuration
		configuration_stats.add( new SystemStat("JVM Vendor", application.getJvmVendor(), StatType.INFO) );
		configuration_stats.add( new SystemStat("JVM Version", application.getJvmVersion(), StatType.INFO) );
		configuration_stats.add( new SystemStat("Operating System Name", application.getOperatingSystemName(), StatType.INFO) );
		configuration_stats.add( new SystemStat("Operating System Version", application.getOperatingSystemVersion(), StatType.INFO) );
		configuration_stats.add( new SystemStat("Architecture", application.getPlatformArch(), StatType.INFO) );
		configuration_stats.add( new SystemStat("Manager Version", Application.getVersion(), StatType.INFO) );
		configuration_stats.add( new SystemStat("Processor Count", String.valueOf( application.getProcessorCount()), StatType.INFO) );
		configuration_stats.add( new SystemStat("Server Port", String.valueOf( application.getNetworkManager().getServerPort() ), StatType.INFO) );
	
		if(  application.getNetworkManager().sslEnabled() == false )
			configuration_stats.add( new SystemStat("SSL Enabled", "False (data transmission is not confidential)", StatType.WARNING) );
		else
			configuration_stats.add( new SystemStat("SSL Enabled",  "True", StatType.CHECK) );
		
		data.put("configuration_stats", configuration_stats);
		
		// 6 -- Create the menu and breadcrumbs
		//Breadcrumbs
		Vector<Link> breadcrumbs = new Vector<Link>();
		breadcrumbs.add( new Link("Main Dashboard", StandardViewList.getURL("main_dashboard")) );
		breadcrumbs.add( new Link("System Status", StandardViewList.getURL("system_status")) );
		
		data.put("breadcrumbs", breadcrumbs);
		
		//Menu
		Vector<Link> menu = new Vector<Link>();
		menu.add( new Link("System Administration") );
		menu.add( new Link("System Status", StandardViewList.getURL("system_status")) );
		menu.add( new Link("System Configuration", StandardViewList.getURL("system_configuration")) );
		menu.add( new Link("Event Logs", StandardViewList.getURL("event_log")) );
		menu.add( new Link("Shutdown System", StandardViewList.getURL("system_shutdown")) );
		menu.add( new Link("Create Backup", StandardViewList.getURL("system_backup")) );
		
		menu.add( new Link("Scanning Engine") );
		if( Application.getApplication().getScannerController().scanningEnabled() ){
			menu.add( new Link("Stop Scanner", StandardViewList.getURL("scanner_stop")) );
		}
		else{
			menu.add( new Link("Start Scanner", StandardViewList.getURL("scanner_start")) );
		}
		menu.add( new Link("View Definitions", StandardViewList.getURL(DefinitionsView.VIEW_NAME)) );
		
		data.put("menu", menu);
		data.put("title", "System Status");
		
		//Get the dashboard headers
		Shortcuts.addDashboardHeaders(request, response, data, createURL());
		
		TemplateLoader.renderToResponse("SystemStatus.ftl", data, response);
		
		return true;
	}

}
