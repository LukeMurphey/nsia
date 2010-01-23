package net.lukemurphey.nsia.xmlRpcInterface;

import java.sql.*;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.Application.ApplicationStatusDescriptor;
import net.lukemurphey.nsia.ApplicationStateMonitor.ApplicationStateDataPoint;
import net.lukemurphey.nsia.trustBoundary.ApiSystem;

import java.util.*;

/**
 * Class provides an XML-RPC interface to the system configuration and status methods.
 * @author luke
 *
 */
public class XmlrpcSystem extends XmlrpcHandler{
	
	private ApiSystem system;
	
	public XmlrpcSystem(Application appRes) {
		super(appRes);
		system = new ApiSystem( appRes );
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
		return system.getDatabaseConnectionInfo(sessionIdentifier);
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
		return system.getDatabaseDriver(sessionIdentifier);
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
		return system.getJvmVendor(sessionIdentifier);
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
		return system.getJvmVersion(sessionIdentifier);
	}
	
	/**
	 * Retrieve the maximum available amount of memory.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public double getMaxMemory( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		return system.getMaxMemory(sessionIdentifier);
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
		return system.getOperatingSystemName(sessionIdentifier);
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
		return system.getOperatingSystemVersion(sessionIdentifier);
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
		return system.getPlatformArch(sessionIdentifier);
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
		return system.getProcessorCount(sessionIdentifier);
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
		return system.getServerPort(sessionIdentifier);
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
		return system.getSslEnabled(sessionIdentifier);
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
		return system.getThreadCount(sessionIdentifier);
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
		return system.getDatabaseConnectionCount(sessionIdentifier);
	}
	
	/**
	 * Retrieve the manager uptime.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public double getUptime( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		//Timestamp timestamp = new Timestamp( system.getUptime(sessionIdentifier) );
		return system.getUptime(sessionIdentifier);
	}
	
	/**
	 * Retrieve the amount of the memory used.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 */
	public double getUsedMemory( String sessionIdentifier ) throws NoSessionException, GeneralizedException, InsufficientPermissionException{
		return system.getUsedMemory(sessionIdentifier);
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
		return system.getVersion(sessionIdentifier);
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
		return system.getSqlWarnings(sessionIdentifier);
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
		String loginBanner = system.getLoginBanner();
		
		if( loginBanner == null )
			return EMPTY_STRING;
		else
			return loginBanner;
	}
	
	/**
	 * Get the manager status descriptor.
	 * @return
	 * @throws GeneralizedException 
	 * @throws NoSessionException 
	 */
	public Hashtable<String, Object> getManagerStatus(String sessionIdentifier) throws GeneralizedException, NoSessionException{
	
		ApplicationStatusDescriptor statusDesc = system.getManagerStatus(sessionIdentifier);
		
		Hashtable<String, Object> hash = new Hashtable<String, Object>();
		hash.put("LongDescription", statusDesc.getLongDescription());
		hash.put("ShortDescription", statusDesc.getShortDescription());
		hash.put("DatabaseConnectionCount", Integer.valueOf(statusDesc.getStatusEntry("Database Connections").getStatus()));
		hash.put("MemoryStatus", Integer.valueOf(statusDesc.getStatusEntry("Memory Utilization").getStatus()));
		hash.put("ScannerStatus", Integer.valueOf(statusDesc.getStatusEntry("Scanner Status").getStatus()));
		hash.put("StatusLevel", Integer.valueOf(statusDesc.getOverallStatus()));
		hash.put("ThreadStatus", Integer.valueOf(statusDesc.getStatusEntry("Thread Count").getStatus()));
		
		return hash;
	}
	
	
	/**
	 * Retrieves statistics in regards to the operational state of the manager.
	 * @return
	 * @throws GeneralizedException 
	 * @throws NoSessionException 
	 */
	public Vector<Hashtable<String, Object>> getOperationalMetrics(String sessionIdentifier) throws GeneralizedException, NoSessionException{
	
		ApplicationStateDataPoint[] dataPoints = system.getOperationalMetrics(sessionIdentifier);
		
		Vector<Hashtable<String, Object>> dataPointsVector = new Vector<Hashtable<String, Object>>();
		
		for(int c = 0; c < dataPoints.length; c++){
			dataPointsVector.add(dataPoints[c].toHashtable());
		}
		
		return dataPointsVector;
	}
	
}
