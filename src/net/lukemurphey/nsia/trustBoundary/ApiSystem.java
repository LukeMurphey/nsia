package net.lukemurphey.nsia.trustBoundary;

import java.io.IOException;
import java.sql.*;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.ApplicationVersionDescriptor;
import net.lukemurphey.nsia.DelayedShutdown;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.GenericUtils;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NetworkManager;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.VersionManagement;
import net.lukemurphey.nsia.Application.ApplicationStatusDescriptor;
import net.lukemurphey.nsia.Application.ShutdownRequestSource;
import net.lukemurphey.nsia.ApplicationStateMonitor.ApplicationStateDataPoint;
import net.lukemurphey.nsia.UserManagement.UserDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.rest.RESTRequestFailedException;

/**
 * Class provides an interface to the system configuration and status methods.
 * @author luke
 *
 */
public class ApiSystem extends ApiHandler{
	
	private NetworkManager manager;
	
	public ApiSystem(Application appRes) {
		super(appRes);
		
		manager = appRes.getNetworkManager();
	}
	
	/**
	 * Retrieve the database connection information.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public String getDatabaseConnectionInfo( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		checkSession( sessionIdentifier );
		
		checkRight(sessionIdentifier, "System.Information.View");
		
		return appRes.getDatabaseInfo();
	}

	/**
	 * Retrieve the number of active database connections.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public int getDatabaseConnectionCount( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		checkSession( sessionIdentifier );
		
		checkRight(sessionIdentifier, "System.Information.View");
		
		return appRes.getDatabaseConnectionCount();
	}
	
	/**
	 * Retrieve the database driver information.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public String getDatabaseDriver( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		checkSession( sessionIdentifier );
		
		checkRight(sessionIdentifier, "System.Information.View");
		
		return appRes.getDatabaseDriver();
	}
	
	/**
	 * Retrieve the JVM vendor.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public String getJvmVendor( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		checkSession( sessionIdentifier );
		
		checkRight(sessionIdentifier, "System.Information.View");
		
		return appRes.getJvmVendor();
	}
	
	/**
	 * Retrieve the JVM version.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public String getJvmVersion( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		checkSession( sessionIdentifier );
		
		checkRight(sessionIdentifier, "System.Information.View");
		
		return appRes.getJvmVersion();
	}
	
	/**
	 * Retrieve the maximum available amount of memory.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public long getMaxMemory( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		checkSession( sessionIdentifier );
		
		checkRight(sessionIdentifier, "System.Information.View");
		
		return appRes.getMaxMemory();
	}
	
	/**
	 * Retrieve the OS name.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public String getOperatingSystemName( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		checkSession( sessionIdentifier );
		
		checkRight(sessionIdentifier, "System.Information.View");
		
		return appRes.getOperatingSystemName();
	}
	
	/**
	 * Retrieve the operating system information.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public String getOperatingSystemVersion( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		checkSession( sessionIdentifier );
		
		checkRight(sessionIdentifier, "System.Information.View");
		
		return appRes.getOperatingSystemVersion();
	}
	
	/**
	 * Retrieve the database name.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public String getDatabaseName( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		checkSession( sessionIdentifier );
		
		checkRight(sessionIdentifier, "System.Information.View");
		
		try{
			return appRes.getDatabaseName();
		}catch (SQLException e){
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		}catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Retrieve the database version.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public String getDatabaseVersion( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{

		checkRight(sessionIdentifier, "System.Information.View");
		
		try{
			return appRes.getDatabaseVersion();
		}catch (SQLException e){
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		}catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Retrieve the database driver version.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public String getDatabaseDriverVersion( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{

		checkRight(sessionIdentifier, "System.Information.View");
		
		try{
			return appRes.getDatabaseDriverVersion();
		}catch (SQLException e){
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		}catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Retrieve the database driver name.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public String getDatabaseDriverName( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{

		checkRight(sessionIdentifier, "System.Information.View");
		
		try{
			return appRes.getDatabaseDriverName();
		}catch (SQLException e){
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		}catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Retrieve the database meta-data.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public DatabaseMetaData getDatabaseMetaData( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		checkRight(sessionIdentifier, "System.Information.View");
		
		try{
			return appRes.getDatabaseMetaData();
		}catch (SQLException e){
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		}catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Get the time that the application started.
	 * @param sessionIdentifier
	 * @return
	 * @throws GeneralizedException 
	 * @throws InsufficientPermissionException 
	 * @throws NoSessionException 
	 */
	public long getStartTime( String sessionIdentifier ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		checkRight(sessionIdentifier, "System.Information.View");
		return appRes.getStartTime();
	}
	
	/**
	 * Retrieve the platform architecture description.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public String getPlatformArch( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		checkSession( sessionIdentifier );
		
		checkRight(sessionIdentifier, "System.Information.View");
		
		return appRes.getPlatformArch();
	}
	
	/**
	 * Retrieve the processor count.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public int getProcessorCount( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		checkSession( sessionIdentifier );
		
		checkRight(sessionIdentifier, "System.Information.View");
		
		return appRes.getProcessorCount();
	}
	
	/**
	 * Retrieve port of the manager.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public int getServerPort( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		checkSession( sessionIdentifier );
		
		checkRight(sessionIdentifier, "System.Information.View");
		
		return manager.getServerPort();
	}
	
	/**
	 * Determine if SSL is enabled.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public boolean getSslEnabled( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		checkSession( sessionIdentifier );
		
		checkRight(sessionIdentifier, "System.Information.View");
		
		return manager.sslEnabled();
	}
	
	/**
	 * Retrieve the number of threads.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public int getThreadCount( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		checkSession( sessionIdentifier );
		
		checkRight(sessionIdentifier, "System.Information.View");
		
		return appRes.getThreadCount();
	}
	
	/**
	 * Retrieve the manager uptime.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public long getUptime( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		checkSession( sessionIdentifier );
		
		checkRight(sessionIdentifier, "System.Information.View");
		
		return appRes.getUptime();
	}
	
	/**
	 * Retrieve the amount of the memory used.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public long getUsedMemory( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		checkSession( sessionIdentifier );
		
		checkRight(sessionIdentifier, "System.Information.View");
		
		return appRes.getUsedMemory();
	}
	
	/**
	 * Retrieve the application version.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public String getVersion( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		checkSession( sessionIdentifier );
		
		checkRight(sessionIdentifier, "System.Information.View");
		
		return Application.getVersion();
	}
	
	/**
	 * Causes the system to shutdown.
	 * @param sessionIdentifier
	 * @throws NoSessionException 
	 * @throws GeneralizedException 
	 * @throws InsufficientPermissionException 
	 */
	public void shutdownSystem( String sessionIdentifier ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		checkRight(sessionIdentifier, "System.Shutdown");
		
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );		
		
		appRes.logEvent(EventLogMessage.EventType.APPLICATION_SHUTTING_DOWN,
				new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName()),
				new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID()),
				new EventLogField( FieldName.UPTIME, GenericUtils.getTimeDescription(appRes.getUptime()/1000) ));
		DelayedShutdown shutdownWorker = new DelayedShutdown(10, appRes, ShutdownRequestSource.API);
		Thread thread = new Thread(shutdownWorker);
		thread.start();
		//appRes.shutdown( Application.ShutdownRequestSource.API );
	}

	/**
	 * Get the SQL warnings reported from the database server.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public String[] getSqlWarnings( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		
		checkRight(sessionIdentifier, "System.Information.View");
		
		try {
			return appRes.getSqlWarnings();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Retrieves the login banner.
	 * @return
	 * @throws GeneralizedException 
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws InputValidationException
	 */
	public String getLoginBanner() throws GeneralizedException {
		try {
			return appRes.getApplicationConfiguration().getLoginBanner();
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		} catch (IllegalArgumentException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Retrieves the status of the manager.
	 * @return
	 * @throws GeneralizedException 
	 * @throws NoSessionException 
	 */
	public ApplicationStatusDescriptor getManagerStatus(String sessionIdentifier) throws GeneralizedException, NoSessionException {
		checkSession(sessionIdentifier);
		
		try {
			return appRes.getManagerStatus();
		} catch (IllegalArgumentException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		}
	}
	
	
	/**
	 * Retrieves statistics in regards to the operational state of the manager.
	 * @return
	 * @throws GeneralizedException 
	 * @throws NoSessionException 
	 */
	public ApplicationStateDataPoint[] getOperationalMetrics(String sessionIdentifier) throws GeneralizedException, NoSessionException {
		checkSession(sessionIdentifier);
		
		try {
			return appRes.getMetricsData();
		} catch (IllegalArgumentException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Gets the version identifier associated with the newest instance of NSIA.
	 * @return
	 * @throws GeneralizedException
	 */
	public ApplicationVersionDescriptor getNewestVersionAvailableID( boolean dontBlock) throws GeneralizedException{
		try {
			return VersionManagement.getNewestVersionAvailableID( dontBlock );
		} catch (RESTRequestFailedException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (IOException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		}
	}
	
	public boolean isNewerVersionAvailableID( boolean dontBlock) throws GeneralizedException{
		try {
			return VersionManagement.isNewerVersionAvailable( dontBlock );
		} catch (RESTRequestFailedException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (IOException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		}
	}
	
}
