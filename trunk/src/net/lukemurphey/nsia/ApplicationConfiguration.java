package net.lukemurphey.nsia;

import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.UUID;
import java.util.regex.Pattern;

import net.lukemurphey.nsia.GenericUtils.SMTPEncryption;
import net.lukemurphey.nsia.LicenseDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.MessageFormatter;
import net.lukemurphey.nsia.eventlog.MessageFormatterFactory;
import net.lukemurphey.nsia.eventlog.SyslogNGAppender;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.eventlog.EventLogMessage.EventType;

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
	protected static final long DEFAULT_SIMULTANEOUS_HTTP_CONNECTIONS = 10;
	protected static final String DEFAULT_LOG_FORMAT = "Native";
	
	/**
	 * Constructor that takes an application object (used to retrieve the parameters from).
	 * @param app
	 */
	public ApplicationConfiguration( Application app ){
		application = app;
		appParams = new ApplicationParameters(app);
	}
	
	
	/**
	 * Gets the application parameters object used to get the configuration values.
	 * @return
	 */
	public ApplicationParameters getApplicationParameters(){
		return appParams;
	}
	
	/*public String getShortLoginBanner() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Security.LoginBanner", null);
	}
	
	public void setShortLoginBanner( String value ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		appParams.setParameter("Security.LoginBanner", value);
	}*/
	
	/**
	 * Sets the banner shown on the web-interface before logging in.
	 */
	public void setLoginBanner( String value ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		appParams.setParameter("Security.LoginBanner", value);
	}
	
	/**
	 * Get the banner shown on the web-interface before logging in.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	
	public String getLoginBanner() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Security.LoginBanner", null);
	}
	
	/**
	 * Sets the number of iterations passwords are hashed before being stored. This is used to prevent offline password cracking.
	 * @param value
	 * @throws InputValidationException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public void setHashIterations( long value ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		if( value <= 0 )
			throw new InputValidationException("The hash iteration count is invalid (must be greater than or equal to one)", "Hash Iterations", String.valueOf( value ) );
		
		appParams.setParameter("Security.PasswordHashIterations", String.valueOf( value ) ); 
	}
	
	/**
	 * Gets the number of iterations passwords are hashed before being stored. This is used to prevent offline password cracking.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public long getHashIterations() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		long hashIterationCount = appParams.getParameter( "Security.PasswordHashIterations", DEFAULT_PASSWORD_ITERATION_COUNT );
		
		//	 1.1 -- Determine the hash iteration count
		if( hashIterationCount < 1 ){
			application.logEvent( EventLogMessage.EventType.INTERNAL_ERROR, new EventLogField( FieldName.MESSAGE, "Hash iteration count parameter is invalid (" + hashIterationCount + ")" ) );
		}
		return appParams.getParameter( "Security.PasswordHashIterations", DEFAULT_PASSWORD_ITERATION_COUNT );
	}
	
	/**
	 * Set the default hash algorithm used when hashing passwords.
	 * @param value
	 * @throws InputValidationException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public void setHashAlgorithm( String value ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		
		try {
			MessageDigest.getInstance(value);
		} catch (NoSuchAlgorithmException e) {
			throw new InputValidationException( "The algorithm given is invalid", "Hash Algorithm", value); // NOPMD by luke on 5/26/07 11:14 AM
		}
		
		appParams.setParameter("Security.PasswordHashAlgorithm", value);
	}
	
	/**
	 * Get the default hash algorithm used when hashing passwords.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public String getHashAlgorithm() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Security.PasswordHashAlgorithm", DEFAULT_HASH_ALGORITHM );
	}
	
	/**
	 * Get the HTTP client ID used when the scan engine accesses web-content.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public String getHttpClientId() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Administration.HTTPClientID", "ThreatFactor NSIA " + Application.getVersion());
	}
	
	/**
	 * Enables or disables SSL for the web-interface.
	 * @param value
	 * @throws InputValidationException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public void setSslEnabled( boolean value ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		if( value )
			appParams.setParameter("Administration.EnableSSL", "1" );
		else
			appParams.setParameter("Administration.EnableSSL", "0" );
	}
	
	/**
	 * Returns a boolean indicating if SSL is enabled for the web-interface.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public boolean isSslEnabled() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Administration.EnableSSL", 0) == 1;
	}
	
	/**
	 * Set the port used for the internal web-interface.
	 * @param value
	 * @throws InputValidationException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public void setServerPort( int value ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		if( value < 0 || value > 65535 )
			throw new InputValidationException("The manager port is invalid (must be within 0-65535)", "Manager Port", String.valueOf( value ));
		
		appParams.setParameter("Administration.ServerPort", String.valueOf( value ));
	}
	
	/**
	 * Get the port used for the internal web-interface. 
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public int getServerPort() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		int serverPort;
		if( isSslEnabled() )
			serverPort = (int)appParams.getParameter("Administration.ServerPort", 8443);
		else
			serverPort = (int)appParams.getParameter("Administration.ServerPort", 8080);
		
		return serverPort;
	}
	
	/**
	 * Set the maximum number of scan threads allowed.
	 * @param value
	 * @throws InputValidationException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public void setMaxHTTPScanThreads( int value ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		if( value < 0 || value > 25 )
			throw new InputValidationException("The maximum number of simultaneous HTTP scan threads is too high (cannot be greater than 25)", "HTTP Scan Threads", String.valueOf( value ));
		
		appParams.setParameter("Administration.HTTPScanThreads", String.valueOf( value ));
	}
	
	/**
	 * Get the maximum number of scan threads allowed.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public int getMaxHTTPScanThreads() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return (int)appParams.getParameter("Administration.HTTPScanThreads", 2);
	}
	
	/**
	 * Get the location of the keystore (used for storing the SSL certificate).
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public String getKeystore() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter( "Administration.SSLKeystore", "../etc/keystore");
	}
	
	/**
	 * Get the SSL password.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public String getSslPassword() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter( "Administration.SSLPassword", null);
	}
	
	/**
	 * Get the SSL key password.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public String getSslKeyPassword() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter( "Administration.SSLKeyPassword", null);
	}
	
	/**
	 * Set the SSL password.
	 * @param value
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public void setSslPassword( String value ) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		appParams.setParameter( "Administration.SSLPassword", value);
	}
	
	/**
	 * Set the SSL key password.
	 * @param value
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public void setSslKeyPassword( String value ) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		appParams.setParameter( "Administration.SSLKeyPassword", value);
	}
	
	/**
	 * Get the format to be when creating the log messages.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public String getLogFormat() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter( "Administration.LogFormat", DEFAULT_LOG_FORMAT);
	}
	
	/**
	 * Set the format to be when creating the log messages. 
	 * @param value
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public void setLogFormat( String value ) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		
		MessageFormatter formatter = MessageFormatterFactory.getFormatter(value);
		
		application.getEventLog().setMessageFormatter(formatter);
		appParams.setParameter( "Administration.LogFormat", value);
	}
	
	/**
	 * Set the amount of time that login attempts will be aggregated for purposes of locking account accounts due to repeated failed authentication attempts.
	 * @param value
	 * @throws InputValidationException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public void setAuthenticationAttemptAggregationCount( long value ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		if( value < 2 ){
			throw new InputValidationException( "Authentication attempt aggregation is invalid (must be 2 or more)", "Authentication Attempt Count", String.valueOf( value ) );
		}
		
		appParams.setParameter("Security.AuthenticationAttemptAggregationPeriod",String.valueOf(value) );
	}
	
	/**
	 * Get the amount of time that login attempts will be aggregated for purposes of locking account accounts due to repeated failed authentication attempts.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public long getAuthenticationAttemptAggregationCount() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Security.AuthenticationAttemptAggregationPeriod", DEFAULT_AUTHENTICATION_AGGREGATION_PERIOD_SECONDS);
	}
	
	/**
	 * Set the limit on the number of failed logins before a login name is locked.
	 * @param value
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public void setAuthenticationAttemptLimit( long value ) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		if( value < 0 )
			throw new InputValidationException( "Authentication attempt limit is invalid (must be 0 or more)", "Authentication Attempt Limit", String.valueOf( value ) );
		
	}
	
	/**
	 * Retrieve the limit on the number of failed logins before a login name is locked.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public long getAuthenticationAttemptLimit() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Security.MaximumAuthenticationAttemptLimit",DEFAULT_AUTHENTICATION_ATTEMPT_LIMIT);
	}
	
	/**
	 * Set the maximum time that a session is allowed to exist without re-authentication.
	 * @param value
	 * @throws InputValidationException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
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
			application.logEvent( EventLogMessage.EventType.ILLEGAL_CONFIG, new EventLogField( FieldName.MESSAGE,"Maximum session time too long (" + maximumSessionSecs + ")" ) );
			return 1000000L;
		}
		else if( maximumSessionSecs < 300 ){ // Session lifetime is too short (less than 5 minutes)
			application.logEvent( EventLogMessage.EventType.ILLEGAL_CONFIG, new EventLogField( FieldName.MESSAGE, "Maximum session time too short (" + maximumSessionSecs + ")" ) );
			return DEFAULT_SESSION_LIFETIME;
		}
		else
			return maximumSessionSecs;
		
	}
	
	/**
	 * Get the amount of time that is allowed without activity before a session identifier is invalidated.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public long getSessionInactivityThreshold() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Security.SessionInactivityThreshold", DEFAULT_SESSION_INACTIVITY_THRESHOLD);
	}
	
	/**
	 * Set the amount of time that is allowed without activity before a session identifier is invalidated.
	 * @param value
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public void setSessionInactivityThreshold(long value) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		if( value < -1 )
			throw new InputValidationException( "Session inactivity threshold is invalid (must not be less then 60 seconds)", "Session Inactivity Threshold", String.valueOf( value ));
		else if (value != -1 && value < 60 )
			throw new InputValidationException( "Session inactivity threshold is invalid (must not be less then 60 seconds)", "Session Inactivity Threshold", String.valueOf( value ));
		
		appParams.setParameter("Security.SessionInactivityThreshold", String.valueOf( value ));
	}
	
	/**
	 * Set the maximum session identifier lifetime.
	 * @param value
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public void setSessionIdentifierLifetime( long value ) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		if( value < -1 )
			throw new InputValidationException("The session identifier lifetime is invalid (must not be less than -1)", "Session Identifier Lifetime", String.valueOf( value ));
		
		appParams.setParameter("Security.SessionIdentifierLifetime", String.valueOf( value ));
	}
	
	/**
	 * Get the maximum duration of the session identifier.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public long getSessionIdentifierLifetime() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Security.SessionIdentifierLifetime", DEFAULT_SESSION_ID_LIFETIME);
	}
	
	/**
	 * Get the transport protocol to use when sending the log messages (TCP or UDP).
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public String getLogServerProtocol() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Administration.LogServerProtocol", "UDP");
	}
	
	/**
	 * Get the port to send the log messages to.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public int getLogServerPort() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return (int)appParams.getParameter("Administration.LogServerPort", 514);
	}
	
	/**
	 * Get the address to send the log messages to.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public String getLogServerAddress() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Administration.LogServerAddress", null);
	}
	
	/**
	 * Determine if external logging is enabled.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public boolean getLogServerEnabled() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Administration.LogServerEnabled", 0) == 1;
	}
	
	/**
	 * Set the port to be used to send the log messages to.
	 * @param port
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public void setLogServerPort( int port ) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		
		if( port < 0 || port > 65535){
			throw new InputValidationException("The port is invalid, must be between within the range of 0-65535", "Syslog Server Port", String.valueOf(port));
		}
		
		appParams.setParameter("Administration.LogServerPort", String.valueOf(port));
		
		setupSyslog();
	}
	
	/**
	 * Reconfigure the syslog setup per the updated configuration.
	 */
	protected void setupSyslog(){
		try{
			String protocol = getLogServerProtocol();
			
			// Select the protocol accordingly
			if( protocol.equalsIgnoreCase("TCP")){
				application.getEventLog().setLogServer(getLogServerAddress(), getLogServerPort(), SyslogNGAppender.Protocol.TCP, getLogServerEnabled());
			}
			else{
				application.getEventLog().setLogServer(getLogServerAddress(), getLogServerPort(), SyslogNGAppender.Protocol.UDP, getLogServerEnabled());
			}
			
			// Set the message format 
			MessageFormatter formatter = MessageFormatterFactory.getFormatter( application.getApplicationConfiguration().getLogFormat() );
			application.getEventLog().setMessageFormatter( formatter );
		}
		catch(Exception e){
			//This exception can be thrown when all of the necessary settings are not defined yet.
		}
		
	}
	
	public void setLogServerAddress( String address ) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the address is not null
		if( address == null ){
			throw new InputValidationException("The address is invalid, cannot be null", "Syslog Server Address", "");
		}
		
		//	 0.2 -- Make sure the address is not empty
		else if( address.isEmpty() ){
			throw new InputValidationException("The address is invalid, cannot be empty", "Syslog Server Address", address);
		}
		
		// 0.3 -- Need to check syslog server address for validity before accepting
		if( Pattern.matches("[-a-zA-Z0-9.]+", address) == false ){
			throw new InputValidationException("The server address is not a valid DNS name or IP address", "Syslog Server Address", address);
		}
		
		// 1 -- Save the parameter
		appParams.setParameter("Administration.LogServerAddress", address);
		
		// 2 -- Reconfigure the syslog server per the new setup
		setupSyslog();
	}
	
	
	public void setLogServerProtocol(String protocol) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		
		// 0 -- Validate the parameters
		
		//	 0.1 -- Make sure the parameter is not null
		if (protocol == null){
			throw new InputValidationException("The protocol is invalid, cannot be null", "Syslog Server Protocol", "");
		}
		
		//	 0.2 -- Make sure the parameter is a valid protocol
		if( protocol.equalsIgnoreCase("UDP") == true || protocol.equalsIgnoreCase("TCP") == true){
			appParams.setParameter("Administration.LogServerProtocol", protocol);
		}
		else{
			throw new InputValidationException("The protocol is invalid, must be either TCP or UDP", "Syslog Server Protocol", protocol);
		}
		
		// 1 -- Reconfigure the syslog setup since the configuration has changed
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
	public synchronized boolean isLicenseValid(){
		return license.isValid();
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
	 * Gets the license associated with the application. Null is returned if the license could not yet be checked.
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
		if(license != null && license.getStatus() == LicenseDescriptor.LicenseStatus.UNVALIDATED ){
			lastCheckFresh = (licenseLastChecked + 900000) > System.currentTimeMillis();
		}
		
		// 1.2 -- If the last date checked is less than 8 hours and the last validation was successful, then just return the last license (prevents checking the license repeatedly)
		else {
			lastCheckFresh = (licenseLastChecked + 28800000) > System.currentTimeMillis();
		}
		
		// Return the current license information if it is current
		if( licenseLastChecked > -1 && lastCheckFresh ){
			return license;
		}
		
		// 2 -- Return the license information
		String key = appParams.getParameter("Administration.LicenseKey", null);
		
		//	 2.1 -- If the key is null then get a descriptor that indicates that the application is unlicensed
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
		
		//	 2.2 -- Start checking the license in the background if the license has not yet been check
		else if( license!= null && lastCheckFresh == false && dontBlock == true ){
			LicenseChecker checker = new LicenseChecker();
			checker.start();
			
			return license;
		}
		
		//	 2.3 -- Start checking the license in the background if the license has not yet been check
		else if( (license == null || lastCheckFresh == false) && dontBlock == true )
		{
			if( key != null && licenseBeingChecked == false ){
				license = LicenseDescriptor.uncheckedLicense(key);
				
				LicenseChecker checker = new LicenseChecker();
				checker.start();
			}
			
			return license;
		}
		
		//	 2.4 -- Get the license
		else if( license == null || lastCheckFresh == false ){
			return fetchLicense();
		}
		
		//	 2.5 -- Return the license
		else
		{
			return license;
		}
	}
	
	/**
	 * This class obtains licensing information from the licensing server.
	 * @author Luke
	 *
	 */
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
	
	/**
	 * Try to obtain the license information from ThreatFactor.com. 
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	private LicenseDescriptor fetchLicense( ) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		
		String key = appParams.getParameter("Administration.LicenseKey", null);

		try{
			license = LicenseManagement.getKeyInfo(key);
		}
		catch(LicenseValidationException e){
			application.logExceptionEvent(EventType.LICENSE_VALIDATION_FAILURE, e);
			
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
	public synchronized void setLicenseKey(String licenseKey) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		
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
	
	/**
	 * Set the email address to use as the source address when sending email via SMTP.
	 * @param fromAddress
	 * @throws InputValidationException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public void setEmailFromAddress(EmailAddress fromAddress) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		appParams.setParameter("Administration.EmailFromAddress", fromAddress.toString());
	}
	
	/**
	 * Get the email address to use as the source address when sending email via SMTP.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 * @throws UnknownHostException
	 * @throws InvalidLocalPartException
	 */
	public EmailAddress getEmailFromAddress() throws NoDatabaseConnectionException, SQLException, InputValidationException, UnknownHostException, InvalidLocalPartException{
		
		String email = appParams.getParameter("Administration.EmailFromAddress", null);
		
		if( email == null ){
			return null;
		}
		else{
			return EmailAddress.getByAddress(email);
		}
	}
	
	/**
	 * Set the server to use for sending email via SMTP.
	 * @param smtpServer
	 * @throws InputValidationException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public void setEmailSMTPServer(String smtpServer) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		appParams.setParameter("Administration.EmailSMTPServer", smtpServer);
	}
	
	/**
	 * Get the server to use for sending email via SMTP.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public String getEmailSMTPServer() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Administration.EmailSMTPServer", null);
	}
	
	/**
	 * Set the username to use when sending email via SMTP.
	 * @param username
	 * @throws InputValidationException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public void setEmailUsername(String username) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		appParams.setParameter("Administration.EmailUsername", username);
	}
	
	/**
	 * Get the username to use when sending email via SMTP.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public String getEmailUsername() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Administration.EmailUsername", null);
	}
	
	/**
	 * Set the password to use when sending email via SMTP.
	 * @param password
	 * @throws InputValidationException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public void setEmailPassword(String password) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		appParams.setParameter("Administration.EmailPassword", password);
	}
	
	/**
	 * Get the password to use when sending email via SMTP.
	 * 
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public String getEmailPassword() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Administration.EmailPassword", null);
	}
	
	/**
	 * Set the port to use when sending email via SMTP.
	 * @param port
	 * @throws InputValidationException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public void setEmailSMTPPort(int port) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		
		if( port < 0 || port > 65535){
			throw new InputValidationException("The port provided is invalid", "SMTP Port", String.valueOf(port));
		}
		
		appParams.setParameter("Administration.EmailSMTPPort", String.valueOf(port));
	}
	
	/**
	 * Get the email port to use when sending email with SMTP. 
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public int getEmailSMTPPort() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return (int)appParams.getParameter("Administration.EmailSMTPPort", 25);
	}
	
	/**
	 * Set the SMTP encryption to be used for sending emails.
	 * @param enc
	 * @throws InputValidationException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public void setEmailSMTPEncryption( SMTPEncryption enc ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		if( enc == null ){
			enc = SMTPEncryption.NONE;
		}
		
		appParams.setParameter("Administration.EmailSMTPEncryption", enc.toString().toUpperCase());
	}
	
	/**
	 * Get the SMTP encryption to be used for sending emails.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public SMTPEncryption getEmailSMTPEncryption() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		
		String setting = appParams.getParameter("Administration.EmailSMTPEncryption", "NONE");
		
		SMTPEncryption enc = SMTPEncryption.valueOf( setting.toUpperCase() );
		
		if( enc == null ){
			return SMTPEncryption.NONE;
		}
		else{
			return enc;
		}
	}
	
	/**
	 * Enable or disable whether or not rules that are edited should be re-scanned (as opposed to waiting for the next scan cycle).
	 * @param enable
	 * @return
	 * @throws InputValidationException
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public void setRescanOnEditEnabled( boolean enable ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		if( enable ){
			appParams.setParameter("Scanner.ReScanOnEdit", String.valueOf(1));
		}
		else{
			appParams.setParameter("Scanner.ReScanOnEdit", String.valueOf(0));
		}
		
	}
	
	/**
	 * Return a boolean determining whether or not rules that are edited should be re-scanned (as opposed to waiting for the next scan cycle).
	 * @return
	 * @throws InputValidationException 
	 * @throws SQLException 
	 * @throws NoDatabaseConnectionException 
	 */
	public boolean isRescanOnEditEnabled() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Scanner.ReScanOnEdit", 0) == 1;
	}
	
	/**
	 * Enable or disable whether or not the scanner is enabled by default.
	 * @param enable
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public void setDefaultScanningEnabled( boolean enable ) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		if( enable ){
			appParams.setParameter("Scanner.DefaultEnabled", String.valueOf(1));
		}
		else{
			appParams.setParameter("Scanner.DefaultEnabled", String.valueOf(0));
		}
	}
	
	/**
	 * Return a boolean determining whether or not the scanner is on by default.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public boolean isDefaultScanningEnabled() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Scanner.DefaultEnabled", 0) == 1;
	}
	
	/**
	 * Set the version of the database schema.
	 * @param schemaVersion
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public void setDatabaseSchemaVersion( String schemaVersion ) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		appParams.setParameter("Administration.DatabaseSchemaVersion", schemaVersion);
	}
	
	/**
	 * Get the version of the database schema.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public String getDatabaseSchemaVersion() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		return appParams.getParameter("Administration.DatabaseSchemaVersion", null);
	}
	
	/**
	 * Gets an identifier that can be used to uniquely identify this instance. The ID is generated if one does not yet exist yet.
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public synchronized String getUniqueInstallationID() throws NoDatabaseConnectionException, SQLException, InputValidationException{
		String uniqueID = appParams.getParameter("Administration.UniqueInstallationID", "");
		
		if( uniqueID.length() == 0 ){
			uniqueID = UUID.randomUUID().toString();
			
			appParams.setParameter("Administration.UniqueInstallationID", uniqueID);
		}
		
		return uniqueID;
	}


	/**
	 * Determine if automatic database defragmentation is enabled.
	 * @return
	 * @throws InputValidationException 
	 * @throws SQLException 
	 * @throws NoDatabaseConnectionException 
	 */
	public boolean isAutomaticDefragmentationEnabled() throws NoDatabaseConnectionException, SQLException, InputValidationException {
		return appParams.getParameter("Administration.AutoDefragEnabled", 0) == 1;
	}
	
	/**
	 * Enable automatic databse defragmentation.
	 * @param enable
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public void setAutomaticDefragmentation( boolean enable ) throws NoDatabaseConnectionException, SQLException, InputValidationException {
		if( enable == true ){
			appParams.setParameter("Administration.AutoDefragEnabled",  String.valueOf(1));
		}
		else{
			appParams.setParameter("Administration.AutoDefragEnabled",  String.valueOf(0));
		}
	}
}
