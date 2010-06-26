package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.ApplicationConfiguration;
import net.lukemurphey.nsia.EmailAddress;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InvalidLocalPartException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.GenericUtils.SMTPEncryption;
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

public class SystemConfigurationView extends View {

	private enum ParameterTitles{
		AUTH_LOGIN_AGGREGATION("auth_login_aggregation"),
		AUTH_LOGIN_FAILURE_LIMIT("auth_login_failure_limit"),
		AUTH_LOGIN_BANNER("auth_login_banner"),
		AUTH_PASSWORD_HASH_ITERATIONS("auth_password_hash_iterations"),
		
		SESSION_INACTIVITY_THRESHOLD("session_inactivity_lifetime"),
		SESSION_LIFETIME("session_lifetime"),
		SESSION_IDENTIFIER_LIFETIME("session_identifier_lifetime"),
		
		SERVER_SSL_PASSWORD("server_ssl_password"),
		SERVER_PORT("server_port"),
		SERVER_WEB_ACCESS("server_web_access"),
		SERVER_SSL_ENABLED("server_ssl_enabled"),
		SERVER_SSL_KEY_PASSWORD("server_ssl_key_password"),
		SERVER_AUTO_UPDATE_DEFINITIONS("server_auto_update_definitions"),
		
		LOG_SERVER("log_server"),
		LOG_SERVER_PORT("log_server_port"),
		LOG_ENABLED("log_enabled"),
		LOG_FORMAT("log_format"),
		LOG_TRANSPORT("log_transport"),
		
		LICENSE_KEY("license_key"),
		
		EMAIL_FROM_ADDRESS("email_from_address"),
		EMAIL_SMTP_SERVER("email_smtp_server"),
		EMAIL_USERNAME("email_username"),
		EMAIL_PASSWORD("email_password"),
		EMAIL_SMTP_PORT("email_smtp_port"),
		EMAIL_SMTP_ENCRYPTION("email_smtp_encryption"),
		
		SCANNER_HTTP_THREADS("scanner_http_threads"),
		SCANNER_RESCAN_EDITED_RULES("scanner_rescan_edited_rules"),
		SCANNER_SCAN_DEFAULT_ENABLED("scanner_scan_default_enabled");
		
		private String name;
		
		private ParameterTitles( String name ){
			this.name = name;
		}
		
		public String getName(){
			return name;
		}
		
		public boolean equals(String name){
			return this.name.equalsIgnoreCase(name);
		}
	}
	
	public SystemConfigurationView() {
		super("System/Configuration", "system_configuration");
	}

	public static String getURL() throws URLInvalidException{
		SystemConfigurationView view = new SystemConfigurationView();
		return view.createURL();
	}
	
	public enum ParameterType{
		TEXT, MULTILINETEXT, BOOL, SELECT, INTEGER, PASSWORD;
	}
	
	public class SelectParamValue{
		String name;
		String value;
		
		public SelectParamValue(String name, String value){
			this.name = name;
			this.value = value;
		}
		
		public String getName(){
			return name;
		}
		
		public String getValue(){
			return value;
		}
	}
	
	public class Parameter{
		
		private ParameterType type;
		private String name;
		private String id;
		private String value;
		private SelectParamValue[] select_params;
		
		public Parameter( String name, String value, String id, ParameterType type){
			this.name = name;
			this.value = value;
			this.id = id;
			this.type = type;
		}
		
		public Parameter( String name, boolean value, String id, ParameterType type){
			this.name = name;
			
			if( value ){
				this.value = "true";
			}
			else{
				this.value = "false";
			}
			
			this.id = id;
			this.type = type;
		}
		
		public Parameter( String name, long value, String id, ParameterType type){
			this.name = name;
			this.value = String.valueOf(value);
			this.id = id;
			this.type = type;
		}
		
		public Parameter( String name, int value, String id, ParameterType type){
			this.name = name;
			this.value = String.valueOf(value);
			this.id = id;
			this.type = type;
		}
		
		public Parameter( String name, String value, String id, SelectParamValue[] select_params){
			this.name = name;
			this.value = value;
			this.type = ParameterType.SELECT;
			this.id = id;
			this.select_params = select_params;
		}
		
		public String getName(){
			return name;
		}
		
		public String getValue(){
			if( value == null ){
				return "";
			}
			else{
				return value;
			}
		}
		
		public String getId(){
			return id;
		}
		
		public SelectParamValue[] getSelectValues(){
			return select_params;
		}
		
		public boolean isBool(){
			return type == ParameterType.BOOL;
		}
		
		public boolean isInteger(){
			return type == ParameterType.INTEGER;
		}
		
		public boolean isMultiline(){
			return type == ParameterType.MULTILINETEXT;
		}
		
		public boolean isPassword(){
			return type == ParameterType.PASSWORD;
		}
		
		public boolean isSelect(){
			return type == ParameterType.SELECT;
		}
		
		public boolean isText(){
			return type == ParameterType.TEXT;
		}
		
	}
	
	private class FieldValidationFailedException extends Exception{

		private static final long serialVersionUID = 4279640126480506939L;
		
		public FieldValidationFailedException(String message){
			super(message);
		}
		
	}
	
	private long getAsLong( String value ) throws FieldValidationFailedException{
		try{
			return Long.parseLong(value);
		}
		catch( NumberFormatException e){
			throw new FieldValidationFailedException("Value is not a valid number");
		}
	}
	
	private int getAsInt( String value ) throws FieldValidationFailedException{
		try{
			return Integer.parseInt(value);
		}
		catch( NumberFormatException e){
			throw new FieldValidationFailedException("Value is not a valid number");
		}
	}
	
	private boolean getAsBoolean( String value ) throws FieldValidationFailedException{
		
		if( value == null ){
			return false;
		}
		else if( value.equalsIgnoreCase("on")){
			return true;
		}
		
		try{
			return Boolean.parseBoolean(value);
		}
		catch( NumberFormatException e){
			throw new FieldValidationFailedException("Value is not a valid boolean");
		}
	}
	
	private void processChanges( HttpServletRequest request, HttpServletResponse response, RequestContext context,  Map<String, Object> data ) throws ViewFailedException, FieldValidationFailedException{
		
		String name = request.getParameter("ParamID");
		String value = request.getParameter("ParamValue");
			
		if( name != null ){
			
			ApplicationConfiguration config = Application.getApplication().getApplicationConfiguration();
			
			try{
				// Authentication setup
				if( value != null && ParameterTitles.AUTH_LOGIN_AGGREGATION.equals(name) ){
					config.setAuthenticationAttemptAggregationCount( getAsLong(value));
					context.addMessage("Login aggregation updated", MessageSeverity.SUCCESS);
				}
				else if( value != null && ParameterTitles.AUTH_LOGIN_BANNER.equals(name) ){
					config.setLoginBanner( value );
					context.addMessage("Login banner updated", MessageSeverity.SUCCESS);
				}
				else if( value != null && ParameterTitles.AUTH_LOGIN_FAILURE_LIMIT.equals(name) ){
					config.setAuthenticationAttemptLimit( getAsLong(value) );
					context.addMessage("Login failure limit updated", MessageSeverity.SUCCESS);
				}
				else if( value != null && ParameterTitles.AUTH_PASSWORD_HASH_ITERATIONS.equals(name) ){
					config.setHashIterations( getAsLong(value) );
					context.addMessage("Password hash iteration count updated", MessageSeverity.SUCCESS);
				}
				
				// Server settings
				else if( ParameterTitles.SERVER_AUTO_UPDATE_DEFINITIONS.equals(name) ){
					config.setAutoDefinitionUpdating( getAsBoolean(value) );
					context.addMessage("Definitions auto-update updated", MessageSeverity.SUCCESS);
				}
				else if( value != null && ParameterTitles.SERVER_PORT.equals(name) ){
					config.setServerPort( getAsInt(value) );
					context.addMessage("Server port updated", MessageSeverity.SUCCESS);
				}
				else if( ParameterTitles.SERVER_SSL_ENABLED.equals(name) ){
					config.setSslEnabled( getAsBoolean(value) );
					context.addMessage("SSL setting updated", MessageSeverity.SUCCESS);
				}
				else if( value != null && ParameterTitles.SERVER_SSL_KEY_PASSWORD.equals(name) ){
					config.setSslKeyPassword( value );
					context.addMessage("SSL key password updated", MessageSeverity.SUCCESS);
				}
				else if( value != null && ParameterTitles.SERVER_SSL_PASSWORD.equals(name) ){
					config.setSslPassword( value );
					context.addMessage("SSL password updated", MessageSeverity.SUCCESS);
				}
				else if( ParameterTitles.SERVER_WEB_ACCESS.equals(name) ){
					//config.set( getAsBoolean(value) );
				}
				else if( ParameterTitles.SERVER_AUTO_UPDATE_DEFINITIONS.equals(name) ){
					config.setAutoDefinitionUpdating( getAsBoolean(value) );
					context.addMessage("Definitions auto-update updated", MessageSeverity.SUCCESS);
				}
				
				// Session management
				else if( value != null && ParameterTitles.SESSION_IDENTIFIER_LIFETIME.equals(name) ){
					config.setSessionIdentifierLifetime( ( getAsLong(value) ) );
					context.addMessage("Session identifier lifetime updated", MessageSeverity.SUCCESS);
				}
				else if( value != null && ParameterTitles.SESSION_INACTIVITY_THRESHOLD.equals(name) ){
					config.setSessionInactivityThreshold( ( getAsLong(value) ) );
					context.addMessage("Session inactivity threshold updated", MessageSeverity.SUCCESS);
				}
				else if( value != null && ParameterTitles.SESSION_LIFETIME.equals(name) ){
					config.setSessionLifetime( ( getAsLong(value) ) );
					context.addMessage("Session lifetime updated", MessageSeverity.SUCCESS);
				}
				
				// Email address setup
				else if( value != null && ParameterTitles.EMAIL_FROM_ADDRESS.equals(name) ){
					try {
						config.setEmailFromAddress( EmailAddress.getByAddress(value) );
						context.addMessage("Email from address updated", MessageSeverity.SUCCESS);
					} catch (UnknownHostException e) {
						throw new FieldValidationFailedException("The host in the email address is invalid");
					} catch (InvalidLocalPartException e) {
						throw new FieldValidationFailedException("Local part of the email address is invalid");
					}
				}
				else if( value != null && ParameterTitles.EMAIL_PASSWORD.equals(name) ){
					config.setEmailPassword( value );
					context.addMessage("Email password updated", MessageSeverity.SUCCESS);
				}
				else if( value != null && ParameterTitles.EMAIL_SMTP_PORT.equals(name) ){
					config.setEmailSMTPPort( getAsInt( value ) );
					context.addMessage("Email SMTP port updated", MessageSeverity.SUCCESS);
				}
				else if( value != null && ParameterTitles.EMAIL_SMTP_SERVER.equals(name) ){
					config.setEmailSMTPServer( value );
					context.addMessage("SMTP server updated", MessageSeverity.SUCCESS);
				}
				else if( value != null && ParameterTitles.EMAIL_USERNAME.equals(name) ){
					config.setEmailUsername( value );
					context.addMessage("SMTP username updated", MessageSeverity.SUCCESS);
				}
				else if( value != null && ParameterTitles.EMAIL_SMTP_ENCRYPTION.equals(name) ){
					config.setEmailSMTPEncryption( SMTPEncryption.valueOf(value) );
					context.addMessage("SMTP encryption setting updated", MessageSeverity.SUCCESS);
				}
				
				// Log configuration
				else if( ParameterTitles.LOG_ENABLED.equals(name) ){
					config.setLogServerEnabled( getAsBoolean(value) );
					context.addMessage("Log setting updated", MessageSeverity.SUCCESS);
				}
				else if( value != null && ParameterTitles.LOG_FORMAT.equals(name) ){
					config.setLogFormat( value );
					context.addMessage("Log format updated", MessageSeverity.SUCCESS);
				}
				else if( value != null && ParameterTitles.LOG_SERVER.equals(name) ){
					config.setLogServerAddress( value );
					context.addMessage("Log server updated", MessageSeverity.SUCCESS);
				}
				else if( value != null && ParameterTitles.LOG_SERVER_PORT.equals(name) ){
					config.setLogServerPort( getAsInt(value) );
					context.addMessage("Log server port updated", MessageSeverity.SUCCESS);
				}
				else if( value != null && ParameterTitles.LOG_TRANSPORT.equals(name) ){
					config.setLogServerProtocol(value);
					context.addMessage("Log transport updated", MessageSeverity.SUCCESS);
				}
				else if( value != null && ParameterTitles.LICENSE_KEY.equals(name) ){
					config.setLicenseKey(value);
					context.addMessage("License key updated", MessageSeverity.SUCCESS);
				}
				
				// Scanner configuration
				else if( value != null && ParameterTitles.SCANNER_HTTP_THREADS.equals(name) ){
					config.setMaxHTTPScanThreads( getAsInt(value) );
					context.addMessage("Scanner HTTP thread limit updated", MessageSeverity.SUCCESS);
				}
				
				else if( ParameterTitles.SCANNER_RESCAN_EDITED_RULES.equals(name) ){
					config.setRescanOnEditEnabled( getAsBoolean(value) );
					context.addMessage("Scanner re-scan setting updated", MessageSeverity.SUCCESS);
				}
				else if( ParameterTitles.SCANNER_SCAN_DEFAULT_ENABLED.equals(name) ){
					config.setDefaultScanningEnabled( getAsBoolean(value) );
					context.addMessage("Scanner default setting updated", MessageSeverity.SUCCESS);
				}
			}
			catch(NoDatabaseConnectionException e){
				throw new ViewFailedException(e);
			} catch (SQLException e) {
				throw new ViewFailedException(e);
			} catch (InputValidationException e) {
				throw new FieldValidationFailedException(e.getMessage());
			} 
		}
		
	}
	
	@Override
	protected boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException, URLInvalidException, IOException, ViewNotFoundException {
		
		try{
			
			// 1 -- Prepare the view
			data.put("title", "System Configuration");
			
			//	 1.1 -- Breadcrumbs
			Vector<Link> breadcrumbs = new Vector<Link>();
			breadcrumbs.add( new Link("Main Dashboard", StandardViewList.getURL("main_dashboard")) );
			breadcrumbs.add( new Link("System Status", StandardViewList.getURL("system_status")) );
			breadcrumbs.add( new Link("System Configuration", StandardViewList.getURL("system_configuration")) );
			
			data.put("breadcrumbs", breadcrumbs);
			
			//	 1.2 -- Menu
			data.put("menu", Menu.getSystemMenu(context));
			
			//Get the dashboard headers
			Shortcuts.addDashboardHeaders(request, response, data);
			
			// 2 -- Check rights
			if( Shortcuts.hasRight( context.getSessionInfo(), "System.Configuration.View", "View system configuration") == false ){
				Shortcuts.getPermissionDeniedDialog(response, data, "You do not have permission to view the system configuration");
				return true;
			}
			
			// 3 -- Process any changes requested
			if( request.getMethod().equalsIgnoreCase("POST") ){
				
				if( Shortcuts.hasRight( context.getSessionInfo(), "System.Configuration.Edit", "Change system configuration") == false ){
					Shortcuts.getPermissionDeniedDialog(response, data, "You do not have permission to change the system configuration");
					return true;
				}
				
				try {
					processChanges(request, response, context, data);
				} catch (FieldValidationFailedException e) {
					context.getSessionMessages().addMessage(context.getSessionInfo(), e.getMessage(), MessageSeverity.WARNING);
					data.put("validation_failed", true);
				}
			}

			ApplicationConfiguration appConfig = Application.getApplication().getApplicationConfiguration();

			// 4 -- Add the authentication configuration options
			Vector<Parameter> authentication_options = new Vector<Parameter>();
			authentication_options.add( new Parameter("Period to Aggregate Login Attempts", appConfig.getAuthenticationAttemptAggregationCount(), ParameterTitles.AUTH_LOGIN_AGGREGATION.getName(), ParameterType.INTEGER) );
			authentication_options.add( new Parameter("Limit of Failed Authentication Attempts", appConfig.getAuthenticationAttemptLimit(), ParameterTitles.AUTH_LOGIN_FAILURE_LIMIT.getName(), ParameterType.INTEGER) );
			authentication_options.add( new Parameter("Login Banner", appConfig.getLoginBanner(), ParameterTitles.AUTH_LOGIN_BANNER.getName(), ParameterType.MULTILINETEXT) );
			authentication_options.add( new Parameter("Password Hash Iterations", appConfig.getHashIterations(),ParameterTitles.AUTH_PASSWORD_HASH_ITERATIONS.getName(), ParameterType.INTEGER) );
			data.put("authentication_options", authentication_options);

			// 5 -- Add the Session configuration options
			Vector<Parameter> session_options = new Vector<Parameter>();
			session_options.add( new Parameter("Session Inactivity Threshold", appConfig.getSessionInactivityThreshold(), ParameterTitles.SESSION_INACTIVITY_THRESHOLD.getName(), ParameterType.INTEGER) );
			session_options.add( new Parameter("Maximum Session Identifier Lifetime", appConfig.getSessionLifetime(), ParameterTitles.SESSION_IDENTIFIER_LIFETIME.getName(), ParameterType.INTEGER) );
			session_options.add( new Parameter("Session Identifier Lifetime", appConfig.getSessionIdentifierLifetime(), ParameterTitles.SESSION_IDENTIFIER_LIFETIME.getName(), ParameterType.INTEGER) );
			data.put("session_options", session_options);

			// 6 -- Add the Server configuration options
			Vector<Parameter> server_options = new Vector<Parameter>();
			server_options.add( new Parameter("Manager Port", appConfig.getServerPort(), ParameterTitles.SERVER_PORT.getName(), ParameterType.INTEGER) );
			//server_options.add( new Parameter("Enable Web Access", "true", ParameterTitles.SERVER_WEB_ACCESS.getName(), ParameterType.BOOL) );
			server_options.add( new Parameter("Enable SSL", appConfig.isSslEnabled(), ParameterTitles.SERVER_SSL_ENABLED.getName(), ParameterType.BOOL) );
			server_options.add( new Parameter("SSL Password", appConfig.getSslPassword(), ParameterTitles.SERVER_SSL_PASSWORD.getName(), ParameterType.PASSWORD) );
			server_options.add( new Parameter("SSL Key Password", appConfig.getSslKeyPassword(), ParameterTitles.SERVER_SSL_KEY_PASSWORD.getName(), ParameterType.PASSWORD) );
			server_options.add( new Parameter("Auto-Update Definitions", appConfig.getAutoDefinitionUpdating(), ParameterTitles.SERVER_AUTO_UPDATE_DEFINITIONS.getName(), ParameterType.BOOL) );
			data.put("server_options", server_options);

			// 7 -- Add the Logging configuration options
			Vector<Parameter> logging_options = new Vector<Parameter>();
			logging_options.add( new Parameter("Syslog Server Address", appConfig.getLogServerAddress(), ParameterTitles.LOG_SERVER.getName(), ParameterType.TEXT) );
			logging_options.add( new Parameter("Syslog Server Port", appConfig.getLogServerPort(), ParameterTitles.LOG_SERVER_PORT.getName(), ParameterType.INTEGER) );
			logging_options.add( new Parameter("Logging Enabled", appConfig.getLogServerEnabled(), ParameterTitles.LOG_ENABLED.getName(), ParameterType.BOOL) );

			SelectParamValue[] log_formats = new SelectParamValue[3];
			log_formats[0] = new SelectParamValue("Native", "Native");
			log_formats[1] = new SelectParamValue("Common Event Format", "Common Event Format (ArcSight)");
			log_formats[2] = new SelectParamValue("Common Event Expression", "Common Event Expression (Splunk)");

			logging_options.add( new Parameter("Log Format", appConfig.getLogFormat(), ParameterTitles.LOG_FORMAT.getName(), log_formats) );

			SelectParamValue[] log_transport = new SelectParamValue[2];
			log_transport[0] = new SelectParamValue("UDP", "UDP");
			log_transport[1] = new SelectParamValue("TCP", "TCP");

			logging_options.add( new Parameter("Transport Protocol", appConfig.getLogFormat(), ParameterTitles.LOG_TRANSPORT.getName(), log_transport) );
			data.put("logging_options", logging_options);

			Vector<Parameter> license_options = new Vector<Parameter>();
			license_options.add( new Parameter("License Key", appConfig.getLicenseKey(), ParameterTitles.LICENSE_KEY.getName(), ParameterType.TEXT) );
			data.put("license_options", license_options);

			// 8 -- Add the email configuration options
			Vector<Parameter> email_options = new Vector<Parameter>();

			EmailAddress email;
			try {
				email = appConfig.getEmailFromAddress();

				if( email == null ){
					email_options.add( new Parameter("From Address", "", ParameterTitles.EMAIL_FROM_ADDRESS.getName(), ParameterType.TEXT) );
				}
				else{
					email_options.add( new Parameter("From Address", appConfig.getEmailFromAddress().toString(), ParameterTitles.EMAIL_FROM_ADDRESS.getName(), ParameterType.TEXT) );
				}

			} catch (InvalidLocalPartException e) {
				throw new ViewFailedException(e);
			}

			email_options.add( new Parameter("SMTP Server", appConfig.getEmailSMTPServer(), ParameterTitles.EMAIL_SMTP_SERVER.getName(), ParameterType.TEXT) );
			email_options.add( new Parameter("SMTP Username", appConfig.getEmailUsername(), ParameterTitles.EMAIL_USERNAME.getName(), ParameterType.TEXT) );
			email_options.add( new Parameter("SMTP Password", appConfig.getEmailPassword(), ParameterTitles.EMAIL_PASSWORD.getName(), ParameterType.PASSWORD) );
			email_options.add( new Parameter("SMTP Port", appConfig.getEmailSMTPPort(), ParameterTitles.EMAIL_SMTP_PORT.getName(), ParameterType.INTEGER) );
			
			SelectParamValue[] smtp_encryption = new SelectParamValue[4];
			smtp_encryption[0] = new SelectParamValue(SMTPEncryption.NONE.toString(), "None");
			smtp_encryption[1] = new SelectParamValue(SMTPEncryption.STARTTLS.toString(), "StartTLS");
			smtp_encryption[2] = new SelectParamValue(SMTPEncryption.TLS.toString(), "TLS");
			smtp_encryption[3] = new SelectParamValue(SMTPEncryption.SSL.toString(), "SSL");
			
			email_options.add( new Parameter("SMTP Encryption", appConfig.getEmailSMTPEncryption().toString(), ParameterTitles.EMAIL_SMTP_ENCRYPTION.getName(), smtp_encryption) );
			
			data.put("email_options", email_options);

			if( request.getParameter("ParamID") != null ){
				data.put("selected", request.getParameter("ParamID"));
			}

			// 9 -- Add the scanner options
			Vector<Parameter> scanner_options = new Vector<Parameter>();
			scanner_options.add( new Parameter("Maximum HTTP Scan Threads", appConfig.getMaxHTTPScanThreads(), ParameterTitles.SCANNER_HTTP_THREADS.getName(), ParameterType.INTEGER) );
			scanner_options.add( new Parameter("Re-Scan Rules Automatically After Editing", appConfig.isRescanOnEditEnabled(), ParameterTitles.SCANNER_RESCAN_EDITED_RULES.getName(), ParameterType.BOOL) );
			scanner_options.add( new Parameter("Scanner Default State", appConfig.isDefaultScanningEnabled(), ParameterTitles.SCANNER_SCAN_DEFAULT_ENABLED.getName(), ParameterType.BOOL) );
			data.put("scanner_options", scanner_options);
			
			TemplateLoader.renderToResponse("SystemConfiguration.ftl", data, response);

			return true;
		}
		catch( InputValidationException e ){
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (GeneralizedException e) {
			throw new ViewFailedException(e);
		}
	}

}
