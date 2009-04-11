package net.lukeMurphey.nsia;

import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

import net.lukeMurphey.nsia.LicenseManagement.LicenseDescriptor;
import net.lukeMurphey.nsia.LicenseManagement.LicenseStatus;
import net.lukeMurphey.nsia.eventLog.EventLogField;
import net.lukeMurphey.nsia.eventLog.EventLogMessage;
import net.lukeMurphey.nsia.eventLog.MessageFormatter;
import net.lukeMurphey.nsia.eventLog.MessageFormatterFactory;
import net.lukeMurphey.nsia.eventLog.SyslogNGAppender;
import net.lukeMurphey.nsia.eventLog.EventLogField.FieldName;
import net.lukeMurphey.nsia.eventLog.EventLogMessage.Category;

public class ApplicationConfiguration {

	private ApplicationParameters appParams;
	private Application application;
	private LicenseDescriptor license = null;
	private long licenseLastChecked = -1;
	private boolean licenseBeingChecked = false;
	
	//Static constants are below
	protected static final long DEFAULT_PASSWORD_ITERATION_COUNT = 10000L;
	protected static final String DEFAULT_HASH_ALGORITHM = "sha-512";
	protected static final long DEFAULT_AUTHENTICATION_ATTEMPT_LIMIT = 4;
	protected static final long DEFAULT_AUTHENTICATION_AGGREGATION_PERIOD_SECONDS = 3600;//60 minutes
	protected static final long DEFAULT_SESSION_LIFETIME = 21600;//6 hours
	protected static final long DEFAULT_SESSION_INACTIVITY_THRESHOLD = 3600;//1 hour
	protected static final long DEFAULT_SESSION_ID_LIFETIME = 300;//5 minutes
	protected static final String DEFAULT_LOG_FORMAT = "Native";
	
	public ApplicationConfiguration( Application app ){
		application = app;
		appParams = new ApplicationParameters(app);
	}
	
	
	
	public ApplicationParameters getApplicationParameters(){
		return appParams;
	}
	
	/*public String getShortLoginBanner() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Security.LoginBanner", null);
	}
	
	public void setShortLoginBanner( String value ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		appParams.setParameter("Security.LoginBanner", value);
	}*/
	
	public void setLoginBanner( String value ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		appParams.setParameter("Security.LoginBanner", value);
	}
	
	public String getLoginBanner() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Security.LoginBanner", null);
	}
	
	public void setHashIterations( long value ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		if( value <= 0 )
			throw new InputValidationException("The hash iteration count is invalid (must be greater than or equal to one)", "Hash Iterations", String.valueOf( value ) );
		
		appParams.setParameter("Security.PasswordHashIterations", String.valueOf( value ) ); 
	}
	
	public long getHashIterations() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		long hashIterationCount = appParams.getParameter( "Security.PasswordHashIterations", DEFAULT_PASSWORD_ITERATION_COUNT );
		
		//	 1.1 -- Determine the hash iteration count
		if( hashIterationCount < 1 ){
			application.logEvent( EventLogMessage.Category.INTERNAL_ERROR, new EventLogField( FieldName.MESSAGE, "Hash iteration count parameter is invalid (" + hashIterationCount + ")" ) );
		}
		return appParams.getParameter( "Security.PasswordHashIterations", DEFAULT_PASSWORD_ITERATION_COUNT );
	}
	
	public void setHashAlgorithm( String value ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		
		try {
			MessageDigest.getInstance(value);
		} catch (NoSuchAlgorithmException e) {
			throw new InputValidationException( "The algorithm given is invalid", "Hash Algorithm", value); // NOPMD by luke on 5/26/07 11:14 AM
		}
		
		appParams.setParameter("Security.PasswordHashAlgorithm", value);
	}
	
	public String getHashAlgorithm() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Security.PasswordHashAlgorithm", DEFAULT_HASH_ALGORITHM );
	}
	
	public String getHttpClientId() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Administration.HTTPClientID", "ThreatFactor NSIA " + Application.getVersion());
	}
	
	public void setSslEnabled( boolean value ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		if( value )
			appParams.setParameter("Administration.EnableSSL", "1" );
		else
			appParams.setParameter("Administration.EnableSSL", "0" );
	}
	
	public boolean isSslEnabled() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Administration.EnableSSL", 0) == 1;
	}
	
	public void setServerPort( int value ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		if( value < 0 || value > 65535 )
			throw new InputValidationException("The manager port is invalid (must be within 0-65535)", "Manager Port", String.valueOf( value ));
		
		appParams.setParameter("Administration.ServerPort", String.valueOf( value ));
	}
	
	public int getServerPort() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		int serverPort;
		if( isSslEnabled() )
			serverPort = (int)appParams.getParameter("Administration.ServerPort", 8443);
		else
			serverPort = (int)appParams.getParameter("Administration.ServerPort", 8080);
		
		return serverPort;
	}
	
	public String getKeystore() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter( "Administration.SSLKeystore", "./keystore");
	}
	
	public String getSslPassword() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter( "Administration.SSLPassword", null);
	}
	
	public String getSslKeyPassword() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter( "Administration.SSLKeyPassword", null);
	}
	
	public void setSslPassword( String value ) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		appParams.setParameter( "Administration.SSLPassword", value);
	}
	
	public void setSslKeyPassword( String value ) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		appParams.setParameter( "Administration.SSLKeyPassword", value);
	}
	
	public String getLogFormat() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter( "Administration.LogFormat", DEFAULT_LOG_FORMAT);
	}
	
	public void setLogFormat( String value ) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		
		MessageFormatter formatter = MessageFormatterFactory.getFormatter(value);
		
		application.getEventLog().setMessageFormatter(formatter);
		appParams.setParameter( "Administration.LogFormat", value);
	}
	
	public void setAuthenticationAttemptAggregationCount( long value ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		if( value < 2 ){
			throw new InputValidationException( "Authentication attempt aggregation is invalid (must be 2 or more)", "Authentication Attempt Count", String.valueOf( value ) );
		}
		
		appParams.setParameter("Security.AuthenticationAttemptAggregationPeriod",String.valueOf(value) );
	}
	
	public long getAuthenticationAttemptAggregationCount() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Security.AuthenticationAttemptAggregationPeriod", DEFAULT_AUTHENTICATION_AGGREGATION_PERIOD_SECONDS);
	}
	
	public void setAuthenticationAttemptLimit( long value ) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		if( value < 0 )
			throw new InputValidationException( "Authentication attempt limit is invalid (must be 0 or more)", "Authentication Attempt Limit", String.valueOf( value ) );
		
	}
	
	public long getAuthenticationAttemptLimit() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Security.MaximumAuthenticationAttemptLimit",DEFAULT_AUTHENTICATION_ATTEMPT_LIMIT);
	}
	
	public void setSessionLifetime(long value) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		if( value < -1 ) // Session lifetime is disabled
			throw new InputValidationException("The maximum session lifetime is invalid (must not be less than -1)", "Session Lifetime", String.valueOf(value));
		else if( value > 1000000L ){ //Session lifetime is too long (may result in a numerical overflow)
			throw new InputValidationException("The maximum session lifetime is too long (must not be greater than 1,000,000 seconds)", "Session Lifetime", String.valueOf(value));
		}
		else if( value < 300 ){ // Session lifetime is too short (less than 5 minutes)
			throw new InputValidationException("The maximum session lifetime is too short (must not be less than 300 seconds)", "Session Lifetime", String.valueOf(value));
		}
		
		appParams.setParameter("Security.SessionLifetime", String.valueOf(value));
	}
	
	/**
	 * Resolve the session lifetime (the time that a session can be valid for before requiring authentication). This
	 * method deals with issues where the specified value is invalid.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public long getSessionLifetime() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		long maximumSessionSecs = appParams.getParameter("Security.SessionLifetime", DEFAULT_SESSION_LIFETIME);
		
		if( maximumSessionSecs < 1 ) // Session lifetime is disabled
			return -1;
		else if( maximumSessionSecs > 1000000L ){ //Session lifetime is too long (may result in a numerical overflow)
			application.logEvent( EventLogMessage.Category.ILLEGAL_CONFIG, new EventLogField( FieldName.MESSAGE,"Maximum session time too long (" + maximumSessionSecs + ")" ) );
			return 1000000L;
		}
		else if( maximumSessionSecs < 300 ){ // Session lifetime is too short (less than 5 minutes)
			application.logEvent( EventLogMessage.Category.ILLEGAL_CONFIG, new EventLogField( FieldName.MESSAGE, "Maximum session time too short (" + maximumSessionSecs + ")" ) );
			return DEFAULT_SESSION_LIFETIME;
		}
		else
			return maximumSessionSecs;
		
	}
	
	public long getSessionInactivityThreshold() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Security.SessionInactivityThreshold", DEFAULT_SESSION_INACTIVITY_THRESHOLD);
	}
	
	public void setSessionInactivityThreshold(long value) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		if( value < -1 )
			throw new InputValidationException( "Session inactivity threshold is invalid (must not be less then 60 seconds)", "Session Inactivity Threshold", String.valueOf( value ));
		else if (value != -1 && value < 60 )
			throw new InputValidationException( "Session inactivity threshold is invalid (must not be less then 60 seconds)", "Session Inactivity Threshold", String.valueOf( value ));
		
		appParams.setParameter("Security.SessionInactivityThreshold", String.valueOf( value ));
	}
	
	public void setSessionIdentifierLifetime( long value ) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		if( value < -1 )
			throw new InputValidationException("The session identifier lifetime is invalid (must not be less than -1)", "Session Identifier Lifetime", String.valueOf( value ));
		
		appParams.setParameter("Security.SessionIdentifierLifetime", String.valueOf( value ));
	}
	
	public long getSessionIdentifierLifetime() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Security.SessionIdentifierLifetime", DEFAULT_SESSION_ID_LIFETIME);
	}
	
	public String getLogServerProtocol() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Administration.LogServerProtocol", "UDP");
	}
	
	public int getLogServerPort() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return (int)appParams.getParameter("Administration.LogServerPort", 514);
	}
	
	public String getLogServerAddress() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Administration.LogServerAddress", null);
	}
	
	public boolean getLogServerEnabled() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Administration.LogServerEnabled", 0) == 1;
	}
	
	public void setLogServerPort( int port ) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		
		if( port < 0 || port > 65535){
			throw new InputValidationException("The port is invalid, must be between within the range of 0-65535", "Syslog Server Port", String.valueOf(port));
		}
		
		appParams.setParameter("Administration.LogServerPort", String.valueOf(port));
		
		setupSyslog();
	}
	
	protected void setupSyslog(){
		try{
			String protocol = getLogServerProtocol();
			
			if( protocol.equalsIgnoreCase("TCP")){
				application.getEventLog().setLogServer(getLogServerAddress(), getLogServerPort(), SyslogNGAppender.Protocol.TCP, getLogServerEnabled());
			}
			else{
				application.getEventLog().setLogServer(getLogServerAddress(), getLogServerPort(), SyslogNGAppender.Protocol.UDP, getLogServerEnabled());
			}
			
			MessageFormatter formatter = MessageFormatterFactory.getFormatter( application.getApplicationConfiguration().getLogFormat() );
			application.getEventLog().setMessageFormatter( formatter );
		}
		catch(Exception e){
			//This exception can be thrown when all of the necessary settings are not defined yet.
		}
		
	}
	
	public void setLogServerAddress( String address ) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		
		if( address == null ){
			throw new InputValidationException("The address is invalid, cannot be null", "Syslog Server Address", address);
		}
		else if( address.isEmpty() ){
			throw new InputValidationException("The address is invalid, cannot be empty", "Syslog Server Address", address);
		}
		
		//TODO Need to check syslog server address for validity before accepting
		
		appParams.setParameter("Administration.LogServerAddress", address);
		setupSyslog();
	}
	
	
	public void setLogServerProtocol(String protocol) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		
		if (protocol == null){
			throw new InputValidationException("The protocol is invalid, cannot be null", "Syslog Server Protocol", protocol);
		}
		
		if( protocol.equalsIgnoreCase("UDP") == true || protocol.equalsIgnoreCase("TCP") == true){
			appParams.setParameter("Administration.LogServerProtocol", protocol);
		}
		else{
			throw new InputValidationException("The protocol is invalid, must be either TCP or UDP", "Syslog Server Protocol", protocol);
		}
		
		setupSyslog();
	}
	
	public void setLogServerEnabled(boolean enable) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		if( enable )
			appParams.setParameter("Administration.LogServerEnabled", "1" );
		else
			appParams.setParameter("Administration.LogServerEnabled", "0" );
		
		setupSyslog();
	}
	
	
	public String getLicenseKey() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Administration.LicenseKey", null);
	}
	
	/**
	 * Returns true if the application has a valid, un-expired license.
	 * @return
	 */
	public boolean isLicenseValid(){
		return LicenseManagement.validate(license);
	}
	
	/**
	 * Gets the license associated with the application. Null is returned if no license exists.
	 * @return
	 * @throws InputValidationException 
	 * @throws SQLException 
	 * @throws NoDatabaseConnectionException 
	 * @throws LicenseValidationException 
	 */
	public LicenseDescriptor getLicense( ) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return getLicense( false );
	}
	
	/**
	 * This boolean indicates if the system ever attempted to check the license key. If false, then that fact that a license key is unvalidated
	 * may be due to that fact that the license key simply was not checked (versus not being checked due to a network issue).
	 * @return
	 */
	public boolean licenseKeyCheckCompleted(){
		return (licenseLastChecked > 0);
	}
	
	/**
	 * Gets the license associated with the application. Null is returned if no license exists.
	 * @return
	 * @throws InputValidationException 
	 * @throws SQLException 
	 * @throws NoDatabaseConnectionException 
	 * @throws LicenseValidationException 
	 */
	public synchronized LicenseDescriptor getLicense( boolean dontBlock ) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		
		// 1 -- Determine if a license check is unnecessary because it was checked already
		boolean lastCheckFresh;
		
		// 1.1 -- If the license could not be validated, then try every 15 minutes
		if(license != null && license.getStatus() == LicenseStatus.UNVALIDATED ){
			lastCheckFresh = (licenseLastChecked + 900000) > System.currentTimeMillis();
		}
		// 1.2 -- If the last date checked is less than 8 hours and the last validation was successful, then just return the last license (prevents checking the license repeatedly)
		else {
			lastCheckFresh = (licenseLastChecked + 28800000) > System.currentTimeMillis();
		}
		
		if( licenseLastChecked > -1 && lastCheckFresh ){
			return license;
		}
		
		// 2 -- Return the license information
		String key = appParams.getParameter("Administration.LicenseKey", null);
		if( key == null ){
			try{
				licenseLastChecked = System.currentTimeMillis();
				license = LicenseManagement.getKeyInfo(null);
			}
			catch(LicenseValidationException e){
				//This exception should never be thrown since a null license key should not require the license management sub-system to check the key
			}
			
			return license;
		}
		else if( license!= null && lastCheckFresh == false && dontBlock == true ){
			LicenseChecker checker = new LicenseChecker();
			checker.start();
			
			return license;
		}
		else if( (license == null || lastCheckFresh == false) && dontBlock == true )
		{
			if( key != null && licenseBeingChecked == false ){
				license = LicenseDescriptor.uncheckedLicense(key);
				
				LicenseChecker checker = new LicenseChecker();
				checker.start();
			}
			
			return license;
		}
		else if( license == null || lastCheckFresh == false ){
			return fetchLicense();
		}
		else
		{
			return license;
		}
	}
	
	private class LicenseChecker extends Thread{
		
		public UncaughtExceptionHandler exHandler = null; 
		
		public void run(){
			try{
				licenseBeingChecked = true;
				fetchLicense();
			}
			catch(Exception e){
				if( exHandler != null ){
					exHandler.uncaughtException(this, e);
				}
			}
			finally{
				licenseBeingChecked = false;
			}
		}
	}
	
	private LicenseDescriptor fetchLicense( ) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		
		String key = appParams.getParameter("Administration.LicenseKey", null);

		try{
			license = LicenseManagement.getKeyInfo(key);
		}
		catch(LicenseValidationException e){
			application.logExceptionEvent(Category.LICENSE_VALIDATION_FAILURE, e);
			
			license = LicenseDescriptor.uncheckedLicense(key);
		}
		
		licenseLastChecked = System.currentTimeMillis();
		return license;
	}
	
	/**
	 * Sets the given license key as the key for the application. The application will attempt to validate the key and retrieve the relevant license descriptor from the server.
	 * @param licenseKey
	 * @throws LicenseValidationException 
	 * @throws InputValidationException 
	 * @throws NoDatabaseConnectionException 
	 * @throws SQLException 
	 */
	public void setLicenseKey(String licenseKey) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		
		try {
			license = LicenseManagement.getKeyInfo(licenseKey);
			appParams.setParameter("Administration.LicenseKey", licenseKey);
		} catch (LicenseValidationException e) {
			//The license could not be validated with the server right now, save it and try again later
			appParams.setParameter("Administration.LicenseKey", licenseKey);
			this.license = null;
		}
	}
	
	/**
	 * Determines if definitions will be automatically updated.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public boolean getAutoDefinitionUpdating() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Administration.AutoUpdateDefinitions", 0) == 1;
	}
	
	/**
	 * Sets whether or not definitions will be automatically updated.
	 * @param autoUpdate
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public void setAutoDefinitionUpdating(boolean autoUpdate) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		if( autoUpdate == true ){
			appParams.setParameter("Administration.AutoUpdateDefinitions",  String.valueOf(1));
		}
		else{
			appParams.setParameter("Administration.AutoUpdateDefinitions",  String.valueOf(0));
		}
	}
	
	public void setEmailFromAddress(EmailAddress fromAddress) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		appParams.setParameter("Administration.EmailFromAddress", fromAddress.toString());
	}
	
	public EmailAddress getEmailFromAddress() throws NoDatabaseConnectionException, SQLException, InputValidationException, UnknownHostException, InvalidLocalPartException{
		
		String email = appParams.getParameter("Administration.EmailFromAddress", null);
		
		if( email == null ){
			return null;
		}
		else{
			return EmailAddress.getByAddress(email);
		}
	}
	
	public void setEmailSMTPServer(String smtpServer) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		appParams.setParameter("Administration.EmailSMTPServer", smtpServer);
	}
	
	public String getEmailSMTPServer() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Administration.EmailSMTPServer", null);
	}
	
	public void setEmailUsername(String username) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		appParams.setParameter("Administration.EmailUsername", username);
	}
	
	public String getEmailUsername() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Administration.EmailUsername", null);
	}
	
	public void setEmailPassword(String password) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		appParams.setParameter("Administration.EmailPassword", password);
	}
	
	public String getEmailPassword() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Administration.EmailUsername", null);
	}
	
	public void setEmailSMTPPort(int port) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		
		if( port < 0 || port > 65535){
			throw new InputValidationException("The port provided is invalid", "SMTP Port", String.valueOf(port));
		}
		
		appParams.setParameter("Administration.EmailSMTPPort", String.valueOf(port));
	}
	
	public int getEmailSMTPPort() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return (int)appParams.getParameter("Administration.EmailSMTPPort", 25);
	}
}
