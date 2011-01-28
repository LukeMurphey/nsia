package net.lukemurphey.nsia.trustBoundary;

import java.net.UnknownHostException;
import java.sql.SQLException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.EmailAddress;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.InvalidLocalPartException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.LicenseDescriptor;
import net.lukemurphey.nsia.UserManagement.UserDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.eventlog.EventLogMessage.EventType;

public class ApiApplicationConfiguration extends ApiHandler{
	
	public ApiApplicationConfiguration( Application application){
		super(application);
	}
	
	public void setAuthenticationAttemptAggregationCount( String sessionIdentifier, long value ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, InputValidationException{
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		UserDescriptor userDesc = getUserInfo(sessionIdentifier);
		
		try {
			appRes.getApplicationConfiguration().setAuthenticationAttemptAggregationCount(value);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		
		appRes.logEvent(new EventLogMessage( EventType.CONFIGURATION_CHANGE,
				new EventLogField(FieldName.PARAMETER, "Authentication Attempt Aggregation Count"), 
				new EventLogField(FieldName.VALUE, value),
				new EventLogField(FieldName.SOURCE_USER_NAME, userDesc.getUserName()),
				new EventLogField(FieldName.SOURCE_USER_ID, userDesc.getUserID())) );
	}
	
	public long getAuthenticationAttemptAggregationCount( String sessionIdentifier ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		try {
			return appRes.getApplicationConfiguration().getAuthenticationAttemptAggregationCount();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}
	
	public void setAuthenticationAttemptLimit( String sessionIdentifier, long value ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, InputValidationException{
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		UserDescriptor userDesc = getUserInfo(sessionIdentifier);
		
		try {
			appRes.getApplicationConfiguration().setAuthenticationAttemptLimit(value);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		
		appRes.logEvent(new EventLogMessage( EventType.CONFIGURATION_CHANGE,
				new EventLogField(FieldName.PARAMETER, "Authentication Attempt Limit"), 
				new EventLogField(FieldName.VALUE, value),
				new EventLogField(FieldName.SOURCE_USER_NAME, userDesc.getUserName()),
				new EventLogField(FieldName.SOURCE_USER_ID, userDesc.getUserID())) );
	}
	
	public long getAuthenticationAttemptLimit( String sessionIdentifier ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		try {
			return appRes.getApplicationConfiguration().getAuthenticationAttemptLimit();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}
	
	public void setHashAlgorithm( String sessionIdentifier, String value ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, InputValidationException{
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		UserDescriptor userDesc = getUserInfo(sessionIdentifier);
		
		try {
			appRes.getApplicationConfiguration().setHashAlgorithm(value);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		
		appRes.logEvent(new EventLogMessage( EventType.CONFIGURATION_CHANGE,
				new EventLogField(FieldName.PARAMETER, "Authentication Hash Algorithm"), 
				new EventLogField(FieldName.VALUE, value),
				new EventLogField(FieldName.SOURCE_USER_NAME, userDesc.getUserName()),
				new EventLogField(FieldName.SOURCE_USER_ID, userDesc.getUserID())) );
	}
	
	public String getHashAlgorithm( String sessionIdentifier ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		try {
			return appRes.getApplicationConfiguration().getHashAlgorithm();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}
	
	public void setHashIterations( String sessionIdentifier, long value ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, InputValidationException{
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		UserDescriptor userDesc = getUserInfo(sessionIdentifier);
		
		try {
			appRes.getApplicationConfiguration().setHashIterations(value);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		
		appRes.logEvent(new EventLogMessage( EventType.CONFIGURATION_CHANGE,
				new EventLogField(FieldName.PARAMETER, "Authentication Hash Iteration Count"), 
				new EventLogField(FieldName.VALUE, value),
				new EventLogField(FieldName.SOURCE_USER_NAME, userDesc.getUserName()),
				new EventLogField(FieldName.SOURCE_USER_ID, userDesc.getUserID())) );
	}
	
	public long getHashIterations( String sessionIdentifier ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		try {
			return appRes.getApplicationConfiguration().getHashIterations();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}
	
	public String getHttpClientId( String sessionIdentifier ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		try {
			return appRes.getApplicationConfiguration().getHttpClientId();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}
	
	public String getKeystore( String sessionIdentifier ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		try {
			return appRes.getApplicationConfiguration().getKeystore();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}
	
	public void setLoginBanner( String sessionIdentifier, String value ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, InputValidationException{
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		UserDescriptor userDesc = getUserInfo(sessionIdentifier);
		
		try {
			appRes.getApplicationConfiguration().setLoginBanner(value);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		
		appRes.logEvent(new EventLogMessage( EventType.CONFIGURATION_CHANGE,
				new EventLogField(FieldName.PARAMETER, "Login Banner"), 
				new EventLogField(FieldName.VALUE, value),
				new EventLogField(FieldName.SOURCE_USER_NAME, userDesc.getUserName()),
				new EventLogField(FieldName.SOURCE_USER_ID, userDesc.getUserID())) );
	}
	
	public String getLoginBanner( String sessionIdentifier ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		try {
			return appRes.getApplicationConfiguration().getLoginBanner();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}
	
	public void setServerPort( String sessionIdentifier, int value ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, InputValidationException{
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		UserDescriptor userDesc = getUserInfo(sessionIdentifier);
		
		try {
			appRes.getApplicationConfiguration().setServerPort(value);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		
		appRes.logEvent(new EventLogMessage( EventType.CONFIGURATION_CHANGE,
				new EventLogField(FieldName.PARAMETER, "Server Port"), 
				new EventLogField(FieldName.VALUE, value),
				new EventLogField(FieldName.SOURCE_USER_NAME, userDesc.getUserName()),
				new EventLogField(FieldName.SOURCE_USER_ID, userDesc.getUserID())) );
	}
	
	public long getServerPort( String sessionIdentifier ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		try {
			return appRes.getApplicationConfiguration().getServerPort();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}
	
	public void setSessionIdentifierLifetime( String sessionIdentifier, int value ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, InputValidationException{
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		UserDescriptor userDesc = getUserInfo(sessionIdentifier);
		
		try {
			appRes.getApplicationConfiguration().setSessionIdentifierLifetime(value);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		
		appRes.logEvent(new EventLogMessage( EventType.CONFIGURATION_CHANGE,
				new EventLogField(FieldName.PARAMETER, "Session Identifier Lifetime"), 
				new EventLogField(FieldName.VALUE, value),
				new EventLogField(FieldName.SOURCE_USER_NAME, userDesc.getUserName()),
				new EventLogField(FieldName.SOURCE_USER_ID, userDesc.getUserID())) );
	}
	
	public long getSessionIdentifierLifetime( String sessionIdentifier ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		try {
			return appRes.getApplicationConfiguration().getSessionIdentifierLifetime();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}
	
	public void setSessionInactivityThreshold( String sessionIdentifier, long value ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, InputValidationException{
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		UserDescriptor userDesc = getUserInfo(sessionIdentifier);
		
		try {
			appRes.getApplicationConfiguration().setSessionInactivityThreshold(value);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		
		appRes.logEvent(new EventLogMessage( EventType.CONFIGURATION_CHANGE,
				new EventLogField(FieldName.PARAMETER, "Session Inactivity Threshold"), 
				new EventLogField(FieldName.VALUE, value),
				new EventLogField(FieldName.SOURCE_USER_NAME, userDesc.getUserName()),
				new EventLogField(FieldName.SOURCE_USER_ID, userDesc.getUserID())) );
	}
	
	
	public long getSessionInactivityThreshold( String sessionIdentifier ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		try {
			return appRes.getApplicationConfiguration().getSessionInactivityThreshold();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}
	
	public void setSessionLifetime( String sessionIdentifier, long value ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, InputValidationException{
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		UserDescriptor userDesc = getUserInfo(sessionIdentifier);
		
		try {
			appRes.getApplicationConfiguration().setSessionLifetime(value);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		
		appRes.logEvent(new EventLogMessage( EventType.CONFIGURATION_CHANGE,
				new EventLogField(FieldName.PARAMETER, "Maximum Session Lifetime"), 
				new EventLogField(FieldName.VALUE, value),
				new EventLogField(FieldName.SOURCE_USER_NAME, userDesc.getUserName()),
				new EventLogField(FieldName.SOURCE_USER_ID, userDesc.getUserID())) );
	}
	
	public long getSessionLifetime( String sessionIdentifier ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		try {
			return appRes.getApplicationConfiguration().getSessionLifetime();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}
	
	public void setSslKeyPassword( String sessionIdentifier, String password ) throws GeneralizedException, InsufficientPermissionException, NoSessionException, InputValidationException{
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		UserDescriptor userDesc = getUserInfo(sessionIdentifier);
		
		try {
			appRes.getApplicationConfiguration().setSslKeyPassword(password);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		
		appRes.logEvent(new EventLogMessage( EventType.CONFIGURATION_CHANGE,
				new EventLogField(FieldName.PARAMETER, "SSL Key Password"), 
				new EventLogField(FieldName.SOURCE_USER_NAME, userDesc.getUserName()),
				new EventLogField(FieldName.SOURCE_USER_ID, userDesc.getUserID())) );
	}
	
	public void setSslPassword( String sessionIdentifier, String password ) throws GeneralizedException, InsufficientPermissionException, NoSessionException, InputValidationException{
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		UserDescriptor userDesc = getUserInfo(sessionIdentifier);
		
		try {
			appRes.getApplicationConfiguration().setSslPassword(password);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		
		appRes.logEvent(new EventLogMessage( EventType.CONFIGURATION_CHANGE,
				new EventLogField(FieldName.PARAMETER, "SSL Password"), 
				new EventLogField(FieldName.SOURCE_USER_NAME, userDesc.getUserName()),
				new EventLogField(FieldName.SOURCE_USER_ID, userDesc.getUserID())) );
	}
	
	public String getSslKeyPassword( String sessionIdentifier ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		try {
			return appRes.getApplicationConfiguration().getSslKeyPassword();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}
	
	public void setLogFormat( String sessionIdentifier, String logFormat ) throws GeneralizedException, InsufficientPermissionException, NoSessionException, InputValidationException{
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		UserDescriptor userDesc = getUserInfo(sessionIdentifier);
		
		try {
			appRes.getApplicationConfiguration().setLogFormat(logFormat);
			
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		
		appRes.logEvent(new EventLogMessage( EventType.CONFIGURATION_CHANGE,
				new EventLogField(FieldName.PARAMETER, "Log Format"), 
				new EventLogField(FieldName.VALUE, logFormat),
				new EventLogField(FieldName.SOURCE_USER_NAME, userDesc.getUserName()),
				new EventLogField(FieldName.SOURCE_USER_ID, userDesc.getUserID())) );
	}
	
	public String getLogFormat( String sessionIdentifier ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		try {
			return appRes.getApplicationConfiguration().getLogFormat();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}
	
	public String getSslPassword( String sessionIdentifier ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		try {
			return appRes.getApplicationConfiguration().getSslPassword();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}
	
	public String getLogServerAddress( String sessionIdentifier ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		try {
			return appRes.getApplicationConfiguration().getLogServerAddress();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}
	
	public void setLogServerAddress( String sessionIdentifier, String value ) throws GeneralizedException, InsufficientPermissionException, NoSessionException, InputValidationException{
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		try {
			appRes.getApplicationConfiguration().setLogServerAddress(value);
			appRes.getEventLog().setLogServer( value );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
	public int getLogServerPort( String sessionIdentifier ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		try {
			return appRes.getApplicationConfiguration().getLogServerPort();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}
	
	public void setLogServerPort( String sessionIdentifier, int value ) throws GeneralizedException, InsufficientPermissionException, NoSessionException, InputValidationException{
		checkRight( sessionIdentifier, "System.Configuration.Edit", "Set log server port");
		
		try {
			appRes.getApplicationConfiguration().setLogServerPort(value);
			appRes.getEventLog().setLogServerPort( value );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
	public void setSslEnabled( String sessionIdentifier, boolean value ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, InputValidationException{
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		UserDescriptor userDesc = getUserInfo(sessionIdentifier);
		
		try {
			appRes.getApplicationConfiguration().setSslEnabled(value);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		
		appRes.logEvent(new EventLogMessage( EventType.CONFIGURATION_CHANGE,
				new EventLogField(FieldName.PARAMETER, "SSL Enabled"), 
				new EventLogField(FieldName.VALUE, String.valueOf( value) ),
				new EventLogField(FieldName.SOURCE_USER_NAME, userDesc.getUserName()),
				new EventLogField(FieldName.SOURCE_USER_ID, userDesc.getUserID())) );
	}
	
	public boolean isSslEnabled( String sessionIdentifier ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		try {
			return appRes.getApplicationConfiguration().isSslEnabled();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}

	public void setLicenseKey( String sessionIdentifier, String value ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, InputValidationException{
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		UserDescriptor userDesc = getUserInfo(sessionIdentifier);
		
		try {
			appRes.getApplicationConfiguration().setLicenseKey(value);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		
		appRes.logEvent(new EventLogMessage( EventType.CONFIGURATION_CHANGE,
				new EventLogField(FieldName.PARAMETER, "License Key"), 
				new EventLogField(FieldName.VALUE, value),
				new EventLogField(FieldName.SOURCE_USER_NAME, userDesc.getUserName()),
				new EventLogField(FieldName.SOURCE_USER_ID, userDesc.getUserID())) );
	}
	
	public String getLicenseKey( String sessionIdentifier ) throws GeneralizedException, NoSessionException{
		//checkRight( sessionIdentifier, "System.Configuration.View");
		checkSession( sessionIdentifier );
		
		try {
			return appRes.getApplicationConfiguration().getLicenseKey();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}
	
	public LicenseDescriptor getLicense( String sessionIdentifier, boolean dontBlock ) throws GeneralizedException, NoSessionException{
		//checkRight( sessionIdentifier, "System.Configuration.View");
		checkSession(sessionIdentifier);
		
		try {
			return appRes.getApplicationConfiguration().getLicense(dontBlock);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}
	
	public boolean licenseKeyCheckCompleted( String sessionIdentifier ){
		return appRes.getApplicationConfiguration().licenseKeyCheckCompleted();
	}
	
	public LicenseDescriptor getLicense( String sessionIdentifier ) throws GeneralizedException, NoSessionException{
		//checkRight( sessionIdentifier, "System.Configuration.View");
		checkSession(sessionIdentifier);
		
		try {
			return appRes.getApplicationConfiguration().getLicense();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}
	
	public void setLogServerProtocol( String sessionIdentifier, String protocol ) throws GeneralizedException, NoSessionException, InsufficientPermissionException, InputValidationException{
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		//checkSession(sessionIdentifier);
		
		try {
			appRes.getApplicationConfiguration().setLogServerProtocol(protocol);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
	public String getLogServerProtocol( String sessionIdentifier ) throws GeneralizedException, NoSessionException, InsufficientPermissionException{
		//checkRight( sessionIdentifier, "System.Configuration.Edit");
		checkSession(sessionIdentifier);
		
		try {
			return appRes.getApplicationConfiguration().getLogServerProtocol();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}
	
	public boolean isLogServerEnabled( String sessionIdentifier ) throws GeneralizedException, NoSessionException, InsufficientPermissionException{
		//checkRight( sessionIdentifier, "System.Configuration.Edit");
		checkSession(sessionIdentifier);
		
		try {
			return appRes.getApplicationConfiguration().getLogServerEnabled();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}
	
	public void setLogServerEnabled( String sessionIdentifier, boolean enable ) throws GeneralizedException, NoSessionException, InsufficientPermissionException, InputValidationException{
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		//checkSession(sessionIdentifier);
		
		try {
			appRes.getApplicationConfiguration().setLogServerEnabled(enable);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} 
	}
	
	public boolean getAutoDefinitionUpdating( String sessionIdentifier ) throws GeneralizedException, NoSessionException, InsufficientPermissionException{
		
		checkSession(sessionIdentifier);
		
		try {
			return appRes.getApplicationConfiguration().getAutoDefinitionUpdating();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}
	
	public void setAutoDefinitionUpdating( String sessionIdentifier, boolean enable ) throws GeneralizedException, NoSessionException, InsufficientPermissionException, InputValidationException{
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		try {
			appRes.getApplicationConfiguration().setAutoDefinitionUpdating(enable);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} 
	}
	
	public EmailAddress getEmailFromAddress( String sessionIdentifier ) throws GeneralizedException, NoSessionException, InsufficientPermissionException{
		
		checkSession(sessionIdentifier);
		
		try {
			return appRes.getApplicationConfiguration().getEmailFromAddress();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			throw new GeneralizedException();
		} catch (UnknownHostException e) {
			//This exception should not happen because an invalid email address should never be saved in the first place
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			return null;
		} catch (InvalidLocalPartException e) {
			//This exception should not happen because an invalid email address should never be saved in the first place
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			return null;
		}
	}
	
	public void setEmailFromAddress( String sessionIdentifier, EmailAddress emailAddress ) throws GeneralizedException, NoSessionException, InsufficientPermissionException, InputValidationException{
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		try {
			appRes.getApplicationConfiguration().setEmailFromAddress(emailAddress);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} 
	}
	
	public String getEmailPassword( String sessionIdentifier ) throws GeneralizedException, NoSessionException, InsufficientPermissionException{
		
		checkSession(sessionIdentifier);
		
		try {
			return appRes.getApplicationConfiguration().getEmailPassword();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}
	
	public void setEmailPassword( String sessionIdentifier, String password ) throws GeneralizedException, NoSessionException, InsufficientPermissionException, InputValidationException{
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		try {
			appRes.getApplicationConfiguration().setEmailPassword(password);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} 
	}
	
	public String getEmailSMTPServer( String sessionIdentifier ) throws GeneralizedException, NoSessionException, InsufficientPermissionException{
		
		checkSession(sessionIdentifier);
		
		try {
			return appRes.getApplicationConfiguration().getEmailSMTPServer();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}
	
	public void setEmailSMTPServer( String sessionIdentifier, String smtpServer ) throws GeneralizedException, NoSessionException, InsufficientPermissionException, InputValidationException{
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		try {
			appRes.getApplicationConfiguration().setEmailSMTPServer(smtpServer);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} 
	}
	
	public String getEmailUsername( String sessionIdentifier ) throws GeneralizedException, NoSessionException, InsufficientPermissionException{
		
		checkSession(sessionIdentifier);
		
		try {
			return appRes.getApplicationConfiguration().getEmailUsername();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}
	
	public void setEmailUsername( String sessionIdentifier, String username ) throws GeneralizedException, NoSessionException, InsufficientPermissionException, InputValidationException{
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		try {
			appRes.getApplicationConfiguration().setEmailUsername(username);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} 
	}
	
	public int getEmailSMTPPort( String sessionIdentifier ) throws GeneralizedException, NoSessionException, InsufficientPermissionException{
		
		checkSession(sessionIdentifier);
		
		try {
			return appRes.getApplicationConfiguration().getEmailSMTPPort();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			throw new GeneralizedException();
		}
	}
	
	public void setEmailSMTPPort( String sessionIdentifier, int port ) throws GeneralizedException, NoSessionException, InsufficientPermissionException, InputValidationException{
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		try {
			appRes.getApplicationConfiguration().setEmailSMTPPort(port);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} 
	}
}
