package net.lukemurphey.nsia.trustBoundary;

import java.sql.SQLException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.eventlog.EventLogMessage;


/**
 * The following class serves as an interface to application parameter class that contains various
 * application configuration options.
 * @author luke
 *
 */
public class ApiApplicationParameters extends ApiHandler{

	public ApiApplicationParameters(Application appRes) {
		super(appRes);
	}
	
	/**
	 * Get the application configuration parameter with the given name. 
	 * @param name
	 * @param defaultValue
	 * @return
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException 
	 * @throws NoSessionException 
	 */
	public long getParameter( String sessionIdentifier, String name, long defaultValue ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		try {
			return appRes.getApplicationParameters().getParameter(name, defaultValue);
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (IllegalArgumentException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Get the application configuration parameter with the given name.
	 * @param name
	 * @param defaultValue
	 * @return
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException 
	 * @throws NoSessionException 
	 */
	public String getParameter( String sessionIdentifier, String name, String defaultValue ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		try {
			return appRes.getApplicationParameters().getParameter(name, defaultValue);
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (IllegalArgumentException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Set the application configuration parameter with the given name.
	 * @param name
	 * @param defaultValue
	 * @return
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException 
	 * @throws NoSessionException 
	 */
	public void setParameter( String sessionIdentifier, String name, String defaultValue ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		try {
			appRes.getApplicationParameters().setParameter(name, defaultValue);
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
	 * Set the application configuration parameter with the given name.
	 * @param name
	 * @param defaultValue
	 * @return
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException 
	 * @throws NoSessionException 
	 */
	public void setParameter( String sessionIdentifier, String name, long defaultValue ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		try {
			appRes.getApplicationParameters().setParameter(name, String.valueOf( defaultValue ));
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
	 * Determine if the parameter has been defined.
	 * @param name
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException 
	 * @throws NoSessionException 
	 */
	public boolean doesParameterExist( String sessionIdentifier, String name ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		try {
			return appRes.getApplicationParameters().doesParameterExist( name );
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

}
