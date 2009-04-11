package net.lukeMurphey.nsia.trustBoundary;

import java.sql.SQLException;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.NoDatabaseConnectionException;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.NotFoundException;
import net.lukeMurphey.nsia.eventLog.EventLogMessage;
import net.lukeMurphey.nsia.eventLog.EventLogSeverity;
import net.lukeMurphey.nsia.eventLog.EventLogViewer;
import net.lukeMurphey.nsia.eventLog.EventLogViewer.EventLogEntry;
import net.lukeMurphey.nsia.eventLog.EventLogViewer.EventLogFilter;

public class ApiEventLogViewer extends ApiHandler {

	private EventLogViewer logViewer;
	
	public ApiEventLogViewer(Application appRes) {
		super(appRes);
		
		logViewer = new EventLogViewer(appRes);
	}
	
	public EventLogEntry[] getEntries( String sessionIdentifier, EventLogFilter filter ) throws GeneralizedException, NoSessionException{
		
		// 0 -- Precondition check
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		// 1 -- Get the entries
		try {
			return logViewer.getEntries( filter );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
	public EventLogEntry getEntry( String sessionIdentifier, int entryId ) throws GeneralizedException, NoSessionException, NotFoundException{
		
		// 0 -- Precondition check
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		// 1 -- Get the entries
		try {
			return logViewer.getEntry( entryId );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
	public int getEntriesCount( String sessionIdentifier ) throws GeneralizedException, NoSessionException{
		
		// 0 -- Precondition check
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		// 1 -- Get the entries
		try {
			return logViewer.getEntriesCount();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
	public int getEntriesCount( String sessionIdentifier, EventLogFilter filter ) throws GeneralizedException, NoSessionException{
		
		// 0 -- Precondition check
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		// 1 -- Get the entries
		try {
			return logViewer.getEntriesCount(filter);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
	public int getMaxEntryID( String sessionIdentifier ) throws GeneralizedException, NoSessionException{
		
		// 0 -- Precondition check
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		// 1 -- Get the entries
		try {
			return logViewer.getMaxEntryID();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}

	public int getMaxEntryID( String sessionIdentifier, String contentFilter, EventLogSeverity severityFilter ) throws GeneralizedException, NoSessionException{
		
		// 0 -- Precondition check
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		// 1 -- Get the entries
		try {
			return logViewer.getMaxEntryID(contentFilter, severityFilter);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
	public int getMaxEntryID( String sessionIdentifier, String contentFilter ) throws GeneralizedException, NoSessionException{
		
		// 0 -- Precondition check
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		// 1 -- Get the entries
		try {
			return logViewer.getMaxEntryID(contentFilter);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
	public int getMaxEntryID( String sessionIdentifier, EventLogSeverity severityFilter ) throws GeneralizedException, NoSessionException{
		
		// 0 -- Precondition check
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		// 1 -- Get the entries
		try {
			return logViewer.getMaxEntryID( severityFilter );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
	public int getMinEntryID( String sessionIdentifier ) throws GeneralizedException, NoSessionException{
		
		// 0 -- Precondition check
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		// 1 -- Get the entries
		try {
			return logViewer.getMinEntryID();
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}

	public int getMinEntryID( String sessionIdentifier, String contentFilter, EventLogSeverity severityFilter ) throws GeneralizedException, NoSessionException{
		
		// 0 -- Precondition check
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		// 1 -- Get the entries
		try {
			return logViewer.getMinEntryID(contentFilter, severityFilter);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
	public int getMinEntryID( String sessionIdentifier, String contentFilter ) throws GeneralizedException, NoSessionException{
		
		// 0 -- Precondition check
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		// 1 -- Get the entries
		try {
			return logViewer.getMinEntryID(contentFilter);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
	public int getMinEntryID( String sessionIdentifier, EventLogSeverity severityFilter ) throws GeneralizedException, NoSessionException{
		
		// 0 -- Precondition check
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		// 1 -- Get the entries
		try {
			return logViewer.getMinEntryID( severityFilter );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
}
