package net.lukeMurphey.nsia.htmlInterface;

import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.EmailAddress;
import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.InputValidationException;
import net.lukeMurphey.nsia.InsufficientPermissionException;
import net.lukeMurphey.nsia.InvalidLocalPartException;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.ScannerController;
import net.lukeMurphey.nsia.trustBoundary.ApiApplicationConfiguration;
import net.lukeMurphey.nsia.trustBoundary.ApiScannerController;
import net.lukeMurphey.nsia.htmlInterface.Html.MessageType;


public class HtmlSystemConfiguration extends HtmlContentProvider{

	private static final int PARAMTYPE_TEXT = 0;
	private static final int PARAMTYPE_MULTILINETEXT = 1;
	private static final int PARAMTYPE_BOOL = 2;
	private static final int PARAMTYPE_INTEGER = 3;
	private static final int PARAMTYPE_SELECT = 4;
	private static final int PARAMTYPE_PASSWORD = 5;
	
	private static final int CONFIG_LOGIN_BANNER = 2;
	private static final int CONFIG_AGGREGATE_LOGIN_ATTEMPTS = 3;
	private static final int CONFIG_LIMIT_FAILED_LOGIN_ATTEMPTS = 4;
	private static final int CONFIG_PASSWORD_HASH_ITERATIONS = 5;
	private static final int CONFIG_PASSWORD_HASH_ALGORITHM = 6;
	private static final int CONFIG_SESSION_INACTIVITY_THRESHOLD = 7;
	private static final int CONFIG_MAXIMUM_SESSION_LIFETIME = 8;
	private static final int CONFIG_XML_RPC_ENABLED = 9;
	private static final int CONFIG_WEB_ACCESS_ENABLED = 10;
	private static final int CONFIG_MANAGER_PORT = 11;
	private static final int CONFIG_MAXIMUM_SESSION_ID_LIFETIME = 12;
	private static final int CONFIG_SSL_KEY_PASSWORD = 13;
	private static final int CONFIG_SSL_PASSWORD = 14;
	private static final int CONFIG_SSL_ENABLED = 15;
	private static final int CONFIG_LOG_FORMAT = 16;
	private static final int CONFIG_LICENSE_KEY = 17;
	private static final int CONFIG_LOG_SERVER = 18;
	private static final int CONFIG_LOG_PORT = 19;
	private static final int CONFIG_LOG_PROTOCOL = 20;
	private static final int CONFIG_LOG_ENABLED = 21;
	private static final int CONFIG_AUTO_DEFINITION_UPDATES = 22;
	private static final int CONFIG_EMAIL_FROM_ADDRESS = 23;
	private static final int CONFIG_EMAIL_SMTP_SERVER = 24;
	private static final int CONFIG_EMAIL_USERNAME = 25;
	private static final int CONFIG_EMAIL_PASSWORD = 26;
	private static final int CONFIG_EMAIL_SMTP_PORT = 27;
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException{
		return getHtml( requestDescriptor, null );
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc) throws GeneralizedException, NoSessionException{
		
		// 1 -- Perform any requested operations
		//ApiApplicationParameters appParams = new ApiApplicationParameters (Application.getApplication());
		ApiApplicationConfiguration appConfig = new ApiApplicationConfiguration (Application.getApplication());
		
		if( actionDesc == null )
			actionDesc = performAction( requestDescriptor, appConfig );
		
		// 2 -- Get the content
		return getView(requestDescriptor, actionDesc, appConfig );
		
		//return null;
	}
	
	private static ContentDescriptor getView(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc, ApiApplicationConfiguration appConfig ) throws GeneralizedException, NoSessionException{
		
		// 1 -- Get the main content
		StringBuffer body = new StringBuffer();
		
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		body.append( Html.getSectionHeader("System Configuration", null ) );
		
		int openedNode = -1;
		if( requestDescriptor.request.getParameter("ParamID") != null ){
			try{
				openedNode = Integer.parseInt( requestDescriptor.request.getParameter("ParamID") );
			}
			catch( NumberFormatException e){
				openedNode = -2;
			}
		}
		
		try{
			body.append( "<table width=\"100%\">" );
			
		//	 1.1 -- Get the Authentication configuration
		body.append( createHeaderRow("Authentication Subsystem") );

		//		1.1.1 -- Security.LoginBanner
		body.append( createParameterRow( CONFIG_LOGIN_BANNER, openedNode == CONFIG_LOGIN_BANNER, appConfig, requestDescriptor.sessionIdentifier ) );
		
		//		1.1.2 -- Security.AuthenticationAttemptAggregationPeriod
		body.append( createParameterRow( CONFIG_AGGREGATE_LOGIN_ATTEMPTS, openedNode == CONFIG_AGGREGATE_LOGIN_ATTEMPTS, appConfig, requestDescriptor.sessionIdentifier ) );
		//body.append( createParameterRow( 3, "Security.AuthenticationAttemptAggregationPeriod", "Period to Aggregate Login Attempts", " seconds", 3600,  openedNode == 3, PARAMTYPE_INTEGER, appParams, requestDescriptor.sessionIdentifier ) );
		
		//		1.1.3 -- Security.MaximumAuthenticationAttemptLimit
		body.append( createParameterRow( CONFIG_LIMIT_FAILED_LOGIN_ATTEMPTS, openedNode == CONFIG_LIMIT_FAILED_LOGIN_ATTEMPTS, appConfig, requestDescriptor.sessionIdentifier ) );
		//body.append( createParameterRow( 4, "Security.MaximumAuthenticationAttemptLimit", "Limit of Failed Authentication Attempts", null, 4,  openedNode == 4, PARAMTYPE_INTEGER, appParams, requestDescriptor.sessionIdentifier ) );

		//		1.1.4 -- Security.PasswordHashIterations
		body.append( createParameterRow( CONFIG_PASSWORD_HASH_ITERATIONS, openedNode == CONFIG_PASSWORD_HASH_ITERATIONS, appConfig, requestDescriptor.sessionIdentifier ) );
		//body.append( createParameterRow( 5, "Security.PasswordHashIterations", "Password Hash Iterations", null, 10000L,  openedNode == 5, PARAMTYPE_INTEGER, appParams, requestDescriptor.sessionIdentifier ) );

		//		1.1.5 -- Security.PasswordHashAlgorithm
		//body.append( createParameterRow( CONFIG_PASSWORD_HASH_ALGORITHM, openedNode == CONFIG_PASSWORD_HASH_ALGORITHM, appConfig, requestDescriptor.sessionIdentifier ) );
		//body.append( createParameterRow( 6, "Security.PasswordHashAlgorithm", "Password Hash Algorithm", null, "sha-512",  openedNode == 6, PARAMTYPE_TEXT, appParams, requestDescriptor.sessionIdentifier ) );

		// 1.2 -- Get the Session configuration
		body.append( "<tr><td>&nbsp;</td></tr>" );
		body.append( createHeaderRow("Session Management Subsystem") );
		
		//		1.2.1 -- Security.SessionInactivityThreshold
		body.append( createParameterRow( CONFIG_SESSION_INACTIVITY_THRESHOLD, openedNode == CONFIG_SESSION_INACTIVITY_THRESHOLD, appConfig, requestDescriptor.sessionIdentifier ) );
		//body.append( createParameterRow( 7, "Security.SessionInactivityThreshold", "Session Inactivity Threshold", null, 3600,  openedNode == 7, PARAMTYPE_INTEGER, appParams, requestDescriptor.sessionIdentifier ) );

		//		1.2.2 -- Security.SessionLifetime
		body.append( createParameterRow( CONFIG_MAXIMUM_SESSION_LIFETIME, openedNode == CONFIG_MAXIMUM_SESSION_LIFETIME, appConfig, requestDescriptor.sessionIdentifier ) );
		//body.append( createParameterRow( 8, "Security.LoginBanner", "Short Login Banner", null, "",  openedNode == 8, PARAMTYPE_INTEGER, appParams, requestDescriptor.sessionIdentifier ) );

		//		1.2.3 -- Security.SessionIdentifierLifetime
		//body.append( createParameterRow( CONFIG_MAXIMUM_SESSION_ID_LIFETIME, openedNode == CONFIG_MAXIMUM_SESSION_ID_LIFETIME, appConfig, requestDescriptor.sessionIdentifier ) );
		//body.append( createParameterRow( 9, "Security.SessionIdentifierLifetime", "Maximum Session Identifier Lifetime", " seconds", 300,  openedNode == 9, PARAMTYPE_INTEGER, appParams, requestDescriptor.sessionIdentifier ) );

		// 1.3 -- Get the Server configuration
		body.append( "<tr><td>&nbsp;</td></tr>" );
		body.append( createHeaderRow("Server Subsystem") );
		
		//		1.3.1 -- Administration.EnableXMLRPC
		//body.append( createParameterRow( CON, openedNode == CONFIG_LOGIN_BANNER, appConfig, requestDescriptor.sessionIdentifier ) );
		//body.append( createParameterRow( 10, "Administration.EnableXMLRPC", "Enable XML-RPC Service", null, "true",  openedNode == 10, PARAMTYPE_BOOL, appParams, requestDescriptor.sessionIdentifier ) );

		//		1.3.2 -- Administration.EnableWebServer
		body.append( createParameterRow( CONFIG_WEB_ACCESS_ENABLED, openedNode == CONFIG_WEB_ACCESS_ENABLED, appConfig, requestDescriptor.sessionIdentifier ) );
		//body.append( createParameterRow( 11, "Administration.EnableWebServer", "Enable Web Access", null, "true",  openedNode == 11, PARAMTYPE_BOOL, appParams, requestDescriptor.sessionIdentifier ) );

		//		1.3.3 -- Administration.ServerPort
		body.append( createParameterRow( CONFIG_MANAGER_PORT, openedNode == CONFIG_MANAGER_PORT, appConfig, requestDescriptor.sessionIdentifier ) );
		
		//		1.3.4 -- Administration.SslEnabled
		body.append( createParameterRow( CONFIG_SSL_ENABLED, openedNode == CONFIG_SSL_ENABLED, appConfig, requestDescriptor.sessionIdentifier ) );
		
		//		1.3.5 -- Administration.SslKeyPassword
		body.append( createParameterRow( CONFIG_SSL_KEY_PASSWORD, openedNode == CONFIG_SSL_KEY_PASSWORD, appConfig, requestDescriptor.sessionIdentifier ) );
		
		//		1.3.6 -- Administration.SslPassword
		body.append( createParameterRow( CONFIG_SSL_PASSWORD, openedNode == CONFIG_SSL_PASSWORD, appConfig, requestDescriptor.sessionIdentifier ) );
		
		//		1.3.7 -- Administration.AutoUpdateDefinitions
		body.append( createParameterRow( CONFIG_AUTO_DEFINITION_UPDATES, openedNode == CONFIG_AUTO_DEFINITION_UPDATES, appConfig, requestDescriptor.sessionIdentifier ) );
		
		
		// 1.4 -- Get the Logging configuration
		body.append( "<tr><td>&nbsp;</td></tr>" );
		body.append( createHeaderRow("Logging Subsystem") );
		
		//		1.4.1 -- Administration.LogFormat
		body.append( createParameterRow( CONFIG_LOG_FORMAT, openedNode == CONFIG_LOG_FORMAT, appConfig, requestDescriptor.sessionIdentifier ) );
		
		//		1.4.2 -- Administration.LogServerAddress
		body.append( createParameterRow( CONFIG_LOG_SERVER, openedNode == CONFIG_LOG_SERVER, appConfig, requestDescriptor.sessionIdentifier ) );
		
		//		1.4.3 -- Administration.LogServerPort
		body.append( createParameterRow( CONFIG_LOG_PORT, openedNode == CONFIG_LOG_PORT, appConfig, requestDescriptor.sessionIdentifier ) );
		
		//		1.4.4 -- Administration.LogServerEnabled
		body.append( createParameterRow( CONFIG_LOG_ENABLED, openedNode == CONFIG_LOG_ENABLED, appConfig, requestDescriptor.sessionIdentifier ) );
		
		//		1.4.5 -- Administration.LogServerProtocol
		body.append( createParameterRow( CONFIG_LOG_PROTOCOL, openedNode == CONFIG_LOG_PROTOCOL, appConfig, requestDescriptor.sessionIdentifier ) );
		
		
		// 1.5 -- Get the License key
		body.append( "<tr><td>&nbsp;</td></tr>" );
		body.append( createHeaderRow("License") );
		
		//		1.5.1 -- Administration.LogFormat
		body.append( createParameterRow( CONFIG_LICENSE_KEY, openedNode == CONFIG_LICENSE_KEY, appConfig, requestDescriptor.sessionIdentifier ) );
		
		
		// 1.6 -- Get the License key
		body.append( "<tr><td>&nbsp;</td></tr>" );
		body.append( createHeaderRow("Email") );
		
		//		1.6.1 -- Administration.EmailSMTPServer
		body.append( createParameterRow( CONFIG_EMAIL_SMTP_SERVER, openedNode == CONFIG_EMAIL_SMTP_SERVER, appConfig, requestDescriptor.sessionIdentifier ) );
		
		//		1.6.2 -- Administration.EmailFromAddress
		body.append( createParameterRow( CONFIG_EMAIL_FROM_ADDRESS, openedNode == CONFIG_EMAIL_FROM_ADDRESS, appConfig, requestDescriptor.sessionIdentifier ) );
		
		//		1.6.3 -- Administration.EmailUsername
		body.append( createParameterRow( CONFIG_EMAIL_USERNAME, openedNode == CONFIG_EMAIL_USERNAME, appConfig, requestDescriptor.sessionIdentifier ) );
		
		//		1.6.4 -- Administration.EmailPassword
		body.append( createParameterRow( CONFIG_EMAIL_PASSWORD, openedNode == CONFIG_EMAIL_PASSWORD, appConfig, requestDescriptor.sessionIdentifier ) );
		
		//		1.6.5 -- Administration.EmailSmtpPort
		body.append( createParameterRow( CONFIG_EMAIL_SMTP_PORT, openedNode == CONFIG_EMAIL_SMTP_PORT, appConfig, requestDescriptor.sessionIdentifier ) );
		
		body.append( "</table>" );
		
		}
		catch( InsufficientPermissionException e){
			body.append("<p>");
			body.append(Html.getWarningDialog("Insufficient Permission", "You do not have permission to view the system configuration.", "Console", "Return to Main Dashboard"));
			
			//throw new InvalidHtmlOperationException("Insufficient Permission", "You do not have permission to view the system configuration.","SystemConfiguration");
		}
		
		//	 1.2 -- Get the server configuration
		
		// 2 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "System Configuration", "/SystemConfiguration");
		String navigationHtml = Html.getNavigationPath( navPath );
		
		// 3 -- Get the navigation bar		
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
		
		menuItems.add( new MenuItem("System Administration", null, MenuItem.LEVEL_ONE) );
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
		
		menuItems.add( new MenuItem("System Status", "/SystemStatus", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Event Log", "/EventLog", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Shutdown System", "/SystemStatus?Action=Shutdown", MenuItem.LEVEL_TWO) );
		if( Application.getApplication().isUsingInternalDatabase() == true ){
			menuItems.add( new MenuItem("Create Backup", "/DatabaseBackup", MenuItem.LEVEL_TWO) );
			menuItems.add( new MenuItem("Defragment Indexes", "/SystemStatus?Action=StartDefragmenter", MenuItem.LEVEL_TWO) );
		}
		menuItems.add( new MenuItem("View Definitions", "/Definitions", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Add Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("List Groups", "/SiteGroup", MenuItem.LEVEL_TWO) );
		
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
		return new ContentDescriptor( "System Configuration", pageOutput);
	}
	
	
	private static String createParameterRow( long paramId, boolean expand, ApiApplicationConfiguration appConfig, String sessionId ) throws GeneralizedException, InsufficientPermissionException, NoSessionException, IllegalArgumentException{
		String startLink = "<a name=\"" + paramId + "\" href=SystemConfiguration?ParamID=" + paramId + "#" + paramId + ">";
		
		StringBuffer output = new StringBuffer();
		String paramDescription;
		String paramValue = null;
		Hashtable<String, String> selectList = null;
		int type;
		
		// 1 -- Output the row
		if( paramId == CONFIG_AGGREGATE_LOGIN_ATTEMPTS ){
			paramDescription = "Period to Aggregate Login Attempts";
			if( expand )
				paramValue = StringEscapeUtils.escapeHtml( String.valueOf( appConfig.getAuthenticationAttemptAggregationCount(sessionId) ) );
			type = PARAMTYPE_INTEGER;
		}
		else if( paramId == CONFIG_LIMIT_FAILED_LOGIN_ATTEMPTS ){
			paramDescription = "Limit of Failed Authentication Attempts";
			if( expand )
				paramValue = StringEscapeUtils.escapeHtml( String.valueOf( appConfig.getAuthenticationAttemptLimit(sessionId) ) );
			type = PARAMTYPE_INTEGER;
		}
		else if( paramId == CONFIG_LOGIN_BANNER ){
			paramDescription = "Login Banner";
			if( expand )
				paramValue = StringEscapeUtils.escapeHtml( appConfig.getLoginBanner(sessionId) );
			type = PARAMTYPE_MULTILINETEXT;
		}
		else if( paramId == CONFIG_MANAGER_PORT ){
			paramDescription = "Manager Port";
			if( expand )
				paramValue = StringEscapeUtils.escapeHtml( String.valueOf( appConfig.getServerPort(sessionId) ) );
			type = PARAMTYPE_INTEGER;
		}
		else if( paramId == CONFIG_MAXIMUM_SESSION_LIFETIME ){
			paramDescription = "Maximum Session Identifier Lifetime";
			if( expand )
				paramValue = StringEscapeUtils.escapeHtml( String.valueOf( appConfig.getSessionLifetime(sessionId) ) );
			type = PARAMTYPE_TEXT;
		}
		/*else if( paramId == CONFIG_PASSWORD_HASH_ALGORITHM ){
			paramDescription = "Password Hash Algorithm";
			if( expand )
				paramValue = StringEscapeUtils.escapeHtml( appConfig.getHashAlgorithm(sessionId) );
			type = PARAMTYPE_TEXT;
		}*/
		else if( paramId == CONFIG_PASSWORD_HASH_ITERATIONS ){
			paramDescription = "Password Hash Iterations";
			if( expand )
				paramValue = StringEscapeUtils.escapeHtml( String.valueOf( appConfig.getHashIterations(sessionId) ) );
			type = PARAMTYPE_INTEGER;
		}
		else if( paramId == CONFIG_SESSION_INACTIVITY_THRESHOLD ){
			paramDescription = "Session Inactivity Threshold";
			if( expand )
				paramValue = StringEscapeUtils.escapeHtml( String.valueOf( appConfig.getSessionInactivityThreshold(sessionId) ) );
			type = PARAMTYPE_INTEGER;
		}
		else if( paramId == CONFIG_WEB_ACCESS_ENABLED ){
			paramDescription = "Enable Web Access";
			if( expand )
				paramValue = "true";//String.valueOf( appConfig.(sessionId) );
			type = PARAMTYPE_BOOL;
		}
		else if( paramId == CONFIG_MAXIMUM_SESSION_ID_LIFETIME ){
			paramDescription = "Session Identifier Lifetime";
			if( expand )
				paramValue = StringEscapeUtils.escapeHtml( String.valueOf( appConfig.getSessionIdentifierLifetime(sessionId) ) );
			type = PARAMTYPE_INTEGER;
		}
		else if( paramId == CONFIG_SSL_ENABLED ){
			paramDescription = "Enable SSL";
			if( expand )
				paramValue = Boolean.valueOf(appConfig.isSslEnabled(sessionId)).toString();

			type = PARAMTYPE_BOOL;
		}
		else if( paramId == CONFIG_SSL_KEY_PASSWORD ){
			paramDescription = "SSL Key Password";
			if( expand )
				paramValue = StringEscapeUtils.escapeHtml( appConfig.getSslKeyPassword(sessionId) );
			type = PARAMTYPE_TEXT;
		}
		else if( paramId == CONFIG_SSL_PASSWORD ){
			paramDescription = "SSL Password";
			if( expand )
				paramValue = StringEscapeUtils.escapeHtml( appConfig.getSslPassword(sessionId) );
			type = PARAMTYPE_TEXT;
		}
		else if( paramId == CONFIG_XML_RPC_ENABLED ){
			paramDescription = "Enable XML-RPC Service";
			if( expand )
				paramValue = Boolean.valueOf(appConfig.isSslEnabled(sessionId)).toString();

			type = PARAMTYPE_BOOL;
		}
		else if( paramId == CONFIG_LICENSE_KEY ){
			paramDescription = "License Key";
			if( expand )
				paramValue = StringEscapeUtils.escapeHtml( appConfig.getLicenseKey(sessionId) );
			type = PARAMTYPE_TEXT;
		}
		else if( paramId == CONFIG_LOG_SERVER ){
			paramDescription = "Syslog Server Address";
			if( expand )
				paramValue = StringEscapeUtils.escapeHtml( appConfig.getLogServerAddress(sessionId) );
			type = PARAMTYPE_TEXT;
		}
		else if( paramId == CONFIG_LOG_PORT ){
			paramDescription = "Syslog Server Port";
			if( expand )
				paramValue = String.valueOf( appConfig.getLogServerPort(sessionId) );
			type = PARAMTYPE_TEXT;
		}
		else if( paramId == CONFIG_LOG_ENABLED ){
			paramDescription = "Logging Enabled";
			if( expand )
				paramValue = String.valueOf( appConfig.isLogServerEnabled(sessionId) );
			type = PARAMTYPE_BOOL;
		}
		else if( paramId == CONFIG_LOG_FORMAT ){
			paramDescription = "Log Format";
			if( expand )
				paramValue = StringEscapeUtils.escapeHtml( appConfig.getLogFormat(sessionId) );
			
			selectList = new Hashtable<String, String>();
			selectList.put("Native", "Native");
			selectList.put("Common Event Format", "Common Event Format (ArcSight)");
			selectList.put("Common Event Expression", "Common Event Expression (Splunk)");
			
			type = PARAMTYPE_SELECT;
		}
		else if( paramId == CONFIG_LOG_PROTOCOL ){
			paramDescription = "Transport Protocol";
			if( expand )
				paramValue = StringEscapeUtils.escapeHtml( appConfig.getLogServerProtocol(sessionId) );
			
			selectList = new Hashtable<String, String>();
			selectList.put("TCP", "TCP");
			selectList.put("UDP", "UDP");
			
			type = PARAMTYPE_SELECT;
		}
		else if( paramId == CONFIG_AUTO_DEFINITION_UPDATES ){
			paramDescription = "Auto-Update Definitions";
			if( expand )
				paramValue = String.valueOf( appConfig.getAutoDefinitionUpdating(sessionId) );
			type = PARAMTYPE_BOOL;
		}
		else if( paramId == CONFIG_EMAIL_FROM_ADDRESS ){
			paramDescription = "From Address";
			if( expand ){
				EmailAddress email = appConfig.getEmailFromAddress(sessionId);
				if( email == null ){
					paramValue = "";
				}
				else{
					paramValue = StringEscapeUtils.escapeHtml( email.toString() );
				}
			}
			type = PARAMTYPE_TEXT;
		}
		else if( paramId == CONFIG_EMAIL_SMTP_SERVER ){
			paramDescription = "SMTP Server";
			if( expand )
				paramValue = StringEscapeUtils.escapeHtml( appConfig.getEmailSMTPServer(sessionId) );
			type = PARAMTYPE_TEXT;
		}
		else if( paramId == CONFIG_EMAIL_USERNAME ){
			paramDescription = "SMTP Username";
			if( expand )
				paramValue = StringEscapeUtils.escapeHtml( appConfig.getEmailUsername(sessionId) );
			type = PARAMTYPE_TEXT;
		}
		else if( paramId == CONFIG_EMAIL_PASSWORD ){
			paramDescription = "SMTP Password";
			if( expand )
				paramValue = StringEscapeUtils.escapeHtml( appConfig.getEmailPassword(sessionId) );
			type = PARAMTYPE_PASSWORD;
		}
		else if( paramId == CONFIG_EMAIL_SMTP_PORT ){
			paramDescription = "SMTP Port";
			if( expand )
				paramValue = String.valueOf(appConfig.getEmailSMTPPort(sessionId));
			type = PARAMTYPE_TEXT;
		}
		else{
			throw new IllegalArgumentException("The parameter ID is invalid (" + paramId + ")");
			
		}
		
		if( expand ){
			
			if( paramValue == null )
				paramValue = "";
			
			output.append("<tr class=\"Background2\"><td>")
			.append( "<table width=\"100%\"><tr><td width=\"1%\">").append( startLink ).append( "<img alt=\"-\" src=\"/9_TreeNodeOpen\"></a></td>" )
			.append("<td>" ).append( startLink ).append( paramDescription ).append( "</a></td></tr>");
		
			if( type == PARAMTYPE_INTEGER ){
				output.append( "<tr><td>&nbsp;</td><td colspan=\"2\"><form method=\"post\" action=\"/SystemConfiguration\"><input type=\"hidden\" name=\"ParamSet\" value=\"true\"><input type=\"hidden\" name=\"ParamID\" value=\"" + paramId + "\"><input class=\"textInput\" size=\"48\" name=\"ParamValue\" value=\"" + paramValue + "\">&nbsp;&nbsp;<input class=\"button\" type=\"submit\" value=\"Apply\"></form></td></tr></table></td></tr>" );
			}
			else if( type == PARAMTYPE_TEXT ){
				output.append( "<tr><td>&nbsp;</td><td colspan=\"2\"><form method=\"post\" action=\"/SystemConfiguration\"><input type=\"hidden\" name=\"ParamSet\" value=\"true\"><input type=\"hidden\" name=\"ParamID\" value=\"" + paramId + "\"><input class=\"textInput\" size=\"48\" name=\"ParamValue\" value=\"" + paramValue + "\">&nbsp;&nbsp;<input class=\"button\" type=\"submit\" value=\"Apply\"></form></td></tr></table></td></tr>" );
			}
			else if( type == PARAMTYPE_BOOL ){
				if(paramValue.equals("true"))
					output.append( "<tr><td>&nbsp;</td><td colspan=\"2\"><form method=\"post\" action=\"/SystemConfiguration\"><input type=\"hidden\" name=\"ParamSet\" value=\"true\"><input type=\"hidden\" name=\"ParamID\" value=\"" + paramId + "\"><table><tr><td><input type=\"CheckBox\" name=\"ParamValue\" checked>Enabled&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td><td><input class=\"button\" type=\"submit\" value=\"Apply\"></form></td></tr></table></td></tr></table></td></tr>" );
				else
					output.append( "<tr><td>&nbsp;</td><td colspan=\"2\"><form method=\"post\" action=\"/SystemConfiguration\"><input type=\"hidden\" name=\"ParamSet\" value=\"true\"><input type=\"hidden\" name=\"ParamID\" value=\"" + paramId + "\"><table><tr><td><input type=\"CheckBox\" name=\"ParamValue\">Enabled&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td><td><input class=\"button\" type=\"submit\" value=\"Apply\"></form></td></tr></table></td></tr></table></td></tr>" );
			}
			else if( type == PARAMTYPE_MULTILINETEXT ){
				output.append( "<tr><td>&nbsp;</td><td colspan=\"2\"><form method=\"post\" action=\"/SystemConfiguration\"><input type=\"hidden\" name=\"ParamID\" value=\"" + paramId + "\"><textarea cols=\"48\" rows=\"5\" name=\"ParamValue\">" + paramValue + "</textarea><p><input class=\"button\" type=\"submit\" value=\"Apply\"></form></td></tr></table></td></tr>" );
			}
			else if( type == PARAMTYPE_PASSWORD ){
				output.append( "<tr><td>&nbsp;</td><td colspan=\"2\"><form method=\"post\" action=\"/SystemConfiguration\"><input type=\"hidden\" name=\"ParamSet\" value=\"true\"><input type=\"hidden\" name=\"ParamID\" value=\"" + paramId + "\"><input type=\"password\" class=\"textInput\" size=\"48\" name=\"ParamValue\"><br/><input type=\"password\" class=\"textInput\" size=\"48\" name=\"PasswordConfirm\">&nbsp;&nbsp;<input class=\"button\" type=\"submit\" value=\"Apply\"></form></td></tr></table></td></tr>" );
			}
			else if( type == PARAMTYPE_SELECT ){
				output.append( "<tr><td>&nbsp;</td><td colspan=\"2\"><form method=\"post\" action=\"/SystemConfiguration\"><input type=\"hidden\" name=\"ParamID\" value=\"" + paramId + "\"><select name=\"ParamValue\">");
				Enumeration<String> names = selectList.keys();
				
				while(names.hasMoreElements()){
					String name = names.nextElement();
					String value = selectList.get(name);
					
					output.append("<option value=\"").append(name);
					
					if( name.equals(paramValue) ){
						output.append("\" selected>").append(value).append("</option>");
					}
					else{
						output.append("\">").append(value).append("</option>");
					}
				}
				
				output.append( "</select><input class=\"button\" type=\"submit\" value=\"Apply\"></form></td></tr></table></td></tr>" );
			}
		}
		else{
			output.append( "<tr class=\"Background1\"><td><table><tr><td>").append( startLink ).append( "<img alt=\"-\" src=\"/9_TreeNodeClosed\"></a></td><td>").append( startLink ).append( paramDescription ).append("</a></td></tr></table></td></tr>");
		}
	
		return output.toString();
	}
	
	
	private static String createHeaderRow( String name ){
		StringBuffer output = new StringBuffer();
		output.append( "<tr><td class=\"Text_2\">" );
		output.append(name);
		output.append( "</td></tr>" );
		return output.toString();
	}
	
	private static ActionDescriptor performAction( WebConsoleConnectionDescriptor requestDescriptor, ApiApplicationConfiguration appConfig ) throws GeneralizedException, NoSessionException{
		
		//1 -- Get the parameter ID
		int paramId = -1;
		if( requestDescriptor.request.getParameter("ParamID") != null ){
			try{
				paramId = Integer.parseInt( requestDescriptor.request.getParameter("ParamID") );
			}
			catch( NumberFormatException e){
				paramId = -2;
			}
		}
		else
			return new ActionDescriptor( ActionDescriptor.OP_NO_OPERATION );
			//return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED, GenericHtmlGenerator.getWarningNote("The parameter identifier is invalid"));
		
		
		// 2 -- Get the value
		String value;
		if( requestDescriptor.request.getParameter("ParamValue") != null ){
			value = requestDescriptor.request.getParameter("ParamValue");
		}
		else
			value = null;
		
		
		// 3 -- Set the value
		try{
		//	3.1 -- Parameters with boolean data type
		boolean boolValue = value != null;
		if( requestDescriptor.request.getParameter("ParamSet") != null ){
			
			if( paramId == CONFIG_SSL_ENABLED ){
				 appConfig.setSslEnabled(requestDescriptor.sessionIdentifier, boolValue);
				 Html.addMessage(MessageType.INFORMATIONAL, "System configuration successfully updated" , requestDescriptor.userId.longValue());
				 return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
			}
			else if( paramId == CONFIG_WEB_ACCESS_ENABLED ){
				//appConfig.setSslEnabled(requestDescriptor.sessionIdentifier, boolValue);
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
			 }
			else if( paramId == CONFIG_XML_RPC_ENABLED ){
				//appConfig.setSslEnabled(requestDescriptor.sessionIdentifier, boolValue);
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
			 }
			else if( paramId == CONFIG_LOG_ENABLED ){
				appConfig.setLogServerEnabled(requestDescriptor.sessionIdentifier, boolValue);
				Html.addMessage(MessageType.INFORMATIONAL, "System configuration successfully updated" , requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
			}
			else if( paramId == CONFIG_AUTO_DEFINITION_UPDATES ){
				appConfig.setAutoDefinitionUpdating(requestDescriptor.sessionIdentifier, boolValue);
				Html.addMessage(MessageType.INFORMATIONAL, "System configuration successfully updated" , requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
			}
		}
		
		// 3.2 -- Parameters with string data type
		
		// Note: all the methods below require that the value be set, therefore, exit if the value is null
		if( value == null )
			return new ActionDescriptor( ActionDescriptor.OP_NO_OPERATION );
		
		
		if( paramId == CONFIG_PASSWORD_HASH_ALGORITHM ){
			appConfig.setHashAlgorithm(requestDescriptor.sessionIdentifier, value);
			Html.addMessage(MessageType.INFORMATIONAL, "System configuration successfully updated" , requestDescriptor.userId.longValue());
			return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
		}		
		else if( paramId == CONFIG_LOGIN_BANNER ){
			appConfig.setLoginBanner(requestDescriptor.sessionIdentifier, value);
			Html.addMessage(MessageType.INFORMATIONAL, "System configuration successfully updated" , requestDescriptor.userId.longValue());
			return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
		}
		else if( paramId == CONFIG_SSL_KEY_PASSWORD ){
			appConfig.setSslKeyPassword(requestDescriptor.sessionIdentifier, value);
			Html.addMessage(MessageType.INFORMATIONAL, "System configuration successfully updated" , requestDescriptor.userId.longValue());
			return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
		}
		else if( paramId == CONFIG_SSL_PASSWORD ){
			appConfig.setSslPassword(requestDescriptor.sessionIdentifier, value);
			Html.addMessage(MessageType.INFORMATIONAL, "System configuration successfully updated" , requestDescriptor.userId.longValue());
			return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
		}
		else if( paramId == CONFIG_LOG_FORMAT ){
			appConfig.setLogFormat(requestDescriptor.sessionIdentifier, value);
			Html.addMessage(MessageType.INFORMATIONAL, "System configuration successfully updated" , requestDescriptor.userId.longValue());
			return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
		}
		else if( paramId == CONFIG_LICENSE_KEY ){
			appConfig.setLicenseKey(requestDescriptor.sessionIdentifier, value);
			Html.addMessage(MessageType.INFORMATIONAL, "System configuration successfully updated" , requestDescriptor.userId.longValue());
			return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
		}
		else if( paramId == CONFIG_LOG_SERVER ){
			appConfig.setLogServerAddress(requestDescriptor.sessionIdentifier, value);
			Html.addMessage(MessageType.INFORMATIONAL, "System configuration successfully updated" , requestDescriptor.userId.longValue());
			return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
		}
		else if( paramId == CONFIG_LOG_PROTOCOL ){
			appConfig.setLogServerProtocol(requestDescriptor.sessionIdentifier, value);
			Html.addMessage(MessageType.INFORMATIONAL, "System configuration successfully updated" , requestDescriptor.userId.longValue());
			return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
		}
		else if( paramId == CONFIG_EMAIL_FROM_ADDRESS ){
			try{
				appConfig.setEmailFromAddress(requestDescriptor.sessionIdentifier, EmailAddress.getByAddress( value ) );
				Html.addMessage(MessageType.INFORMATIONAL, "System configuration successfully updated" , requestDescriptor.userId.longValue());
			}
			catch(InvalidLocalPartException e){
				Html.addMessage(MessageType.WARNING, "The local part of the email address is invalid (the part before the @ symbol)" , requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED );
			} catch (UnknownHostException e) {
				Html.addMessage(MessageType.WARNING, "The domain part of the email address is invalid (part after the @ symbol)" , requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED );
			}
			
			return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
		}
		else if( paramId == CONFIG_EMAIL_PASSWORD ){
			appConfig.setEmailPassword(requestDescriptor.sessionIdentifier, value);
			Html.addMessage(MessageType.INFORMATIONAL, "System configuration successfully updated" , requestDescriptor.userId.longValue());
			return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
		}
		else if( paramId == CONFIG_EMAIL_SMTP_SERVER ){
			appConfig.setEmailSMTPServer(requestDescriptor.sessionIdentifier, value);
			Html.addMessage(MessageType.INFORMATIONAL, "System configuration successfully updated" , requestDescriptor.userId.longValue());
			return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
		}
		else if( paramId == CONFIG_EMAIL_USERNAME ){
			appConfig.setEmailUsername(requestDescriptor.sessionIdentifier, value);
			Html.addMessage(MessageType.INFORMATIONAL, "System configuration successfully updated" , requestDescriptor.userId.longValue());
			return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
		}
		
		//	3.3 -- Parameters with long data type
		long longValue = -1;
		if( requestDescriptor.request.getParameter("ParamValue") != null ){
			try{
				longValue = Long.parseLong( requestDescriptor.request.getParameter("ParamValue") );
			}
			catch( NumberFormatException e){
				longValue  = -2; //return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED,GenericHtmlGenerator.getWarningNote("The provided value is invalid") );
			}
		}
		
		if( longValue >= 0 ){
			if( paramId == CONFIG_AGGREGATE_LOGIN_ATTEMPTS ){
				appConfig.setAuthenticationAttemptAggregationCount(requestDescriptor.sessionIdentifier, longValue);
				Html.addMessage(MessageType.INFORMATIONAL, "System configuration successfully updated" , requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
			}
			else if( paramId == CONFIG_LIMIT_FAILED_LOGIN_ATTEMPTS ){
				appConfig.setAuthenticationAttemptLimit(requestDescriptor.sessionIdentifier, longValue);//setAuthenticationAttemptLimit
				Html.addMessage(MessageType.INFORMATIONAL, "System configuration successfully updated" , requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
			}
			else if( paramId == CONFIG_MAXIMUM_SESSION_LIFETIME ){
				appConfig.setSessionLifetime(requestDescriptor.sessionIdentifier, longValue);
				Html.addMessage(MessageType.INFORMATIONAL, "System configuration successfully updated" , requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
			}
			else if( paramId == CONFIG_PASSWORD_HASH_ITERATIONS ){
				appConfig.setHashIterations(requestDescriptor.sessionIdentifier, longValue);
				Html.addMessage(MessageType.INFORMATIONAL, "System configuration successfully updated" , requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
			}
			else if( paramId == CONFIG_SESSION_INACTIVITY_THRESHOLD ){
				appConfig.setSessionInactivityThreshold(requestDescriptor.sessionIdentifier, longValue);
				Html.addMessage(MessageType.INFORMATIONAL, "System configuration successfully updated" , requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
			}
			else if( paramId == CONFIG_MANAGER_PORT ){
				appConfig.setServerPort(requestDescriptor.sessionIdentifier, (int)longValue);
				Html.addMessage(MessageType.INFORMATIONAL, "System configuration successfully updated" , requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
			}
			else if( paramId == CONFIG_MAXIMUM_SESSION_ID_LIFETIME ){
				appConfig.setSessionIdentifierLifetime(requestDescriptor.sessionIdentifier, (int)longValue);
				Html.addMessage(MessageType.INFORMATIONAL, "System configuration successfully updated" , requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
			}
			else if( paramId == CONFIG_LOG_PORT ){
				appConfig.setLogServerPort(requestDescriptor.sessionIdentifier, (int)longValue);
				Html.addMessage(MessageType.INFORMATIONAL, "System configuration successfully updated" , requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
			}
			else if( paramId == CONFIG_EMAIL_SMTP_PORT ){
				appConfig.setEmailSMTPPort(requestDescriptor.sessionIdentifier, (int)longValue);
				Html.addMessage(MessageType.INFORMATIONAL, "System configuration successfully updated" , requestDescriptor.userId.longValue());
				return new ActionDescriptor( ActionDescriptor.OP_UPDATE_SUCCESS );
			}
		}
		
		}
		catch( InputValidationException e ){
			Html.addMessage(MessageType.WARNING, "System configuration update failed: " + e.getMessage() , requestDescriptor.userId.longValue());
			return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED );
		}
		catch( InsufficientPermissionException e ){
			Html.addMessage(MessageType.WARNING, "You do not have permission to set the parameter" , requestDescriptor.userId.longValue());
			return new ActionDescriptor( ActionDescriptor.OP_UPDATE_FAILED );
		}
		
		return new ActionDescriptor( ActionDescriptor.OP_NO_OPERATION );
	}
	
}
