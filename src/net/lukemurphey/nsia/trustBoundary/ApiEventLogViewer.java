package net.lukemurphey.nsia.trustBoundary;

import java.sql.SQLException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogSeverity;
import net.lukemurphey.nsia.eventlog.EventLogViewer;
import net.lukemurphey.nsia.eventlog.EventLogViewer.EventLogEntry;
import net.lukemurphey.nsia.eventlog.EventLogViewer.EventLogFilter;

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
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
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
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
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
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
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
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
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
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
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
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
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
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
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
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
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
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
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
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
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
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
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
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
}
