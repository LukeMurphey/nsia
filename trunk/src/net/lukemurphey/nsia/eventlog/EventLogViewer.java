package net.lukemurphey.nsia.eventlog;

import java.text.SimpleDateFormat;
import java.util.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang.StringEscapeUtils;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.Application.DatabaseAccessType;

public class EventLogViewer {
	
	private Application application;
	
	public static class EventLogFilter{
		protected String content = null;
		protected int entryId = -1;
		protected int count = -1;
		protected boolean entriesAfterId = true;
		protected EventLogSeverity severity = null;
		
		public EventLogFilter(int count){
			setCount(count);
		}
		
		public void setCount(int count){
			// 0 -- Precondition check
			if( count <= 0){
				throw new IllegalArgumentException("The number of entries to return must be greater than zero");
			}
			
			this.count = count;
		}
		
		public void setSeverityFilter(EventLogSeverity severity){
			this.severity = severity;
		}
		
		
		public void setContentFilter(String contentFilter){
			content = contentFilter;
		}
		
		public void setEntryID(int entryId){			
			this.entryId = entryId;
		}
		
		public void setEntryID(int entryId, boolean entriesAfterId){			
			this.entryId = entryId;
			this.entriesAfterId = entriesAfterId;
		}
		
		public boolean isEntriesAfterId(){
			return entriesAfterId;
		}
	}
	
	public static class EventLogEntry{
		protected EventLogSeverity severity;
		protected Date logDate;
		protected String message;
		protected String notes;
		protected int entryId;
		
		protected EventLogEntry( EventLogSeverity severity, Date logDate, String message, int entryId){
			this.severity = severity;
			this.logDate = logDate;
			this.message = message;
			this.notes = null;
			this.entryId = entryId;
		}
		
		protected EventLogEntry( EventLogSeverity severity, Date logDate, String message, String notes, int entryId){
			this.severity = severity;
			this.logDate = logDate;
			this.message = message;
			this.notes = notes;
			this.entryId = entryId;
		}
		
		public EventLogSeverity getSeverity(){
			return severity;
		}
		
		public Date getDate(){
			return (Date)logDate.clone();
		}
		
		public String getFormattedDate(){
			SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy hh:mm:ss zzz");
		    return sdf.format(logDate);		 
		}
		
		public String getMessage(){
			return message;
		}
		
		public int getEntryID(){
			return entryId;
		}
		
		public String getNotes(){
			return notes;
		}
	}
	
	public EventLogViewer( Application app){
		application = app;
	}
	
	private EventLogEntry logEntryFromRow(ResultSet resultSet) throws SQLException{
		
		java.sql.Timestamp date;
		int severityId;
		int logId;
		String title;
		String notes;
		
		date = resultSet.getTimestamp("LogDate");
		notes = resultSet.getString("Notes");
		title = resultSet.getString("Title");
		severityId = resultSet.getInt("Severity");
		logId = resultSet.getInt("EventLogID");
		
		EventLogSeverity severity;
		
		severity = EventLogSeverity.getSeverityById(severityId);
		
		return new EventLogEntry(severity, date, title, notes, logId);
		
	}
	
	private EventLogEntry[] getEntries( PreparedStatement statement, boolean invertOrder ) throws SQLException{
		ResultSet resultset = null;
		Vector<EventLogEntry> entriesVector = new Vector<EventLogEntry>();
		
		try{
			
			resultset = statement.executeQuery();
			
			while(resultset.next()){
				entriesVector.add( logEntryFromRow(resultset) );
			}
			
		}
		finally{
			if( resultset != null){
				resultset.close();
			}
		}
		
		
		EventLogEntry[] events = new EventLogEntry[entriesVector.size()];
		
		if( invertOrder == false ){
			entriesVector.toArray(events);
		}
		else{
			for( int c = 0; c < entriesVector.size(); c++){
				events[events.length - 1 - c] = entriesVector.get(c);
			}
		}
		
		return events;
	}
	
	private PreparedStatement getQuery( EventLogFilter filter, Connection connection, boolean countOnly ) throws SQLException{
		StringBuffer where = new StringBuffer();
		
		// 1 -- Create the where clause
		boolean needsAnd = false;
		
		//	 1.1 -- Add the content filter
		if( filter.content != null && !filter.content.isEmpty() ){
			where.append("( lower(Title) like ? or lower(Notes) like ? )");
			needsAnd = true;
		}
		
		//	 1.2 -- Add the start ID
		if( filter.entryId >= 0 ){
			if( needsAnd ){
				where.append(" and ");
			}
			
			if( filter.isEntriesAfterId() ){
				where.append("EventLogID >= ?");
			}
			else{
				where.append("EventLogID <= ?");
			}
			
			needsAnd = true;
		}
		
		//	 1.3 -- Add the severity filter
		if( filter.severity != null ){
			if( needsAnd ){
				where.append(" and ");
			}
			
			where.append("Severity >= ?");
			
			needsAnd = true;
		}
		
		PreparedStatement statement = null;
		String whereStatement = where.toString();
		
		if( countOnly ){
			if( whereStatement != null && whereStatement.length() > 0 ){
				statement = connection.prepareStatement("Select count(*) as EntriesCount from EventLog where " + whereStatement );
			}
			else{
				statement = connection.prepareStatement("Select count(*) as EntriesCount from EventLog where " + whereStatement );
			}
		}
		else{
			if( whereStatement != null && whereStatement.length() > 0 ){
				if( filter.isEntriesAfterId() ){
					statement = connection.prepareStatement("Select * from EventLog where " + whereStatement + " order by EventLogID asc" );
				}
				else{
					statement = connection.prepareStatement("Select * from EventLog where " + whereStatement + " order by EventLogID desc" );
				}
			}
			else{
				if( filter.isEntriesAfterId() ){
					statement = connection.prepareStatement("Select * from EventLog order by EventLogID asc" );
				}
				else{
					statement = connection.prepareStatement("Select * from EventLog order by EventLogID desc" );
				}
			}
		}
		
		
		// 2 -- Set the parameters
		int parameters = 0;
		
		//	 2.1 -- Add the content filter
		if( filter.content != null && !filter.content.isEmpty() ){
			statement.setString(1, "%" + StringEscapeUtils.escapeSql( filter.content.toLowerCase() ) + "%");
			statement.setString(2, "%" + StringEscapeUtils.escapeSql( filter.content.toLowerCase() ) + "%");
			parameters += 2;
		}
		
		//	 2.2 -- Add the start ID
		if( filter.entryId >= 0 ){
			parameters++;
			statement.setInt(parameters, filter.entryId);
		}
		
		//	 2.3 -- Add the severity filter
		if( filter.severity != null ){
			parameters++;
			statement.setInt(parameters, filter.severity.getSeverity());
		}
		
		statement.setMaxRows(filter.count);
		
		return statement;
	}
	
	/**
	 * Get a list of the event log entries that match the given filter. 
	 * @param filter
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 */
	public EventLogEntry[] getEntries( EventLogFilter filter ) throws NoDatabaseConnectionException, SQLException{
		
		Connection connection = null;
		PreparedStatement statement = null;

		try{
			connection = application.getDatabaseConnection(DatabaseAccessType.EVENT_LOG);
			statement = getQuery(filter, connection, false);
			
			if( filter.isEntriesAfterId() ){
				return getEntries(statement, false);
			}
			else{
				return getEntries(statement, true);
			}
			
		}
		finally{
			if( connection != null){
				connection.close();
			}
			
			if( statement != null){
				statement.close();
			}
		}
	}
	
	public int getMaxEntryID( String content, EventLogSeverity severity ) throws NoDatabaseConnectionException, SQLException{
		return getMaxEntryIDInternal( content, severity);
	}
	
	public int getMaxEntryID( String content ) throws NoDatabaseConnectionException, SQLException{
		return getMaxEntryIDInternal( content, null);
	}
	
	public int getMaxEntryID( EventLogSeverity severity ) throws NoDatabaseConnectionException, SQLException{
		return getMaxEntryIDInternal( null, severity);
	}
	
	public int getMaxEntryID( ) throws NoDatabaseConnectionException, SQLException{
		return getMaxEntryIDInternal( null, null);
	}
	
	public int getMinEntryID( String content, EventLogSeverity severity ) throws NoDatabaseConnectionException, SQLException{
		return getMinEntryIDInternal( content, severity);
	}
	
	public int getMinEntryID( String content ) throws NoDatabaseConnectionException, SQLException{
		return getMinEntryIDInternal( content, null);
	}
	
	public int getMinEntryID( EventLogSeverity severity ) throws NoDatabaseConnectionException, SQLException{
		return getMinEntryIDInternal( null, severity);
	}
	
	public int getMinEntryID( ) throws NoDatabaseConnectionException, SQLException{
		return getMinEntryIDInternal( null, null);
	}
	
	private int getMaxEntryIDInternal( String content, EventLogSeverity severity ) throws NoDatabaseConnectionException, SQLException{
		
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		
		if( content != null && content.length() == 0){
			content = null;
		}
		
		try{
			connection = application.getDatabaseConnection(DatabaseAccessType.EVENT_LOG);
			
			if( content == null && severity == null ){
				statement = connection.prepareStatement("Select EventLogID from EventLog order by EventLogID desc");
			}
			else if(content == null && severity != null ){
				statement = connection.prepareStatement("Select EventLogID from EventLog where Severity >= ? order by EventLogID desc");
				statement.setInt(1, severity.getSeverity());
			}
			else if(content != null && severity != null ){
				statement = connection.prepareStatement("Select EventLogID from EventLog where ( lower(Title) like ? or lower(Notes) like ? ) and Severity >= ? order by EventLogID desc");
				statement.setString(1, "%" + StringEscapeUtils.escapeSql( content.toLowerCase() ) + "%");
				statement.setString(2, "%" + StringEscapeUtils.escapeSql( content.toLowerCase() ) + "%");
				statement.setInt(3, severity.getSeverity());
			}
			else{ //if(content != null && severity == null ){
				statement = connection.prepareStatement("Select EventLogID from EventLog where ( lower(Title) like ? or lower(Notes) like ? ) order by EventLogID desc");
				statement.setString(1, "%" + StringEscapeUtils.escapeSql( content.toLowerCase() ) + "%");
				statement.setString(2, "%" + StringEscapeUtils.escapeSql( content.toLowerCase() ) + "%");
			}
			
			statement.setMaxRows(1);
			resultSet = statement.executeQuery();
			
			if( resultSet.next() ){
				return resultSet.getInt(1);
			}
			else{
				return -1;
			}
		}
		finally{
			if( connection != null ){
				connection.close();
			}
			
			if( statement != null ){
				statement.close();
			}
			
			if( resultSet != null ){
				resultSet.close();
			}
		}
	}
	
	private int getMinEntryIDInternal( String content, EventLogSeverity severity ) throws NoDatabaseConnectionException, SQLException{
		
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		
		if( content != null && content.length() == 0){
			content = null;
		}
		
		try{
			connection = application.getDatabaseConnection(DatabaseAccessType.EVENT_LOG);
			
			if( content == null && severity == null ){
				statement = connection.prepareStatement("Select EventLogID from EventLog order by EventLogID asc");
			}
			else if(content == null && severity != null ){
				statement = connection.prepareStatement("Select EventLogID from EventLog where Severity >= ? order by EventLogID asc");
				statement.setInt(1, severity.getSeverity());
			}
			else if(content != null && severity != null ){
				statement = connection.prepareStatement("Select EventLogID from EventLog where ( lower(Title) like ? or lower(Notes) like ? ) and Severity >= ? order by EventLogID asc");
				statement.setString(1, "%" + StringEscapeUtils.escapeSql( content.toLowerCase() ) + "%");
				statement.setString(2, "%" + StringEscapeUtils.escapeSql( content.toLowerCase() ) + "%");
				statement.setInt(3, severity.getSeverity());
			}
			else {//if(content != null && severity == null ){
				statement = connection.prepareStatement("Select EventLogID from EventLog where ( lower(Title) like ? or lower(Notes) like ? ) order by EventLogID asc");
				statement.setString(1, "%" + StringEscapeUtils.escapeSql( content.toLowerCase() ) + "%");
				statement.setString(2, "%" + StringEscapeUtils.escapeSql( content.toLowerCase() ) + "%");
			}
			
			statement.setMaxRows(1);
			resultSet = statement.executeQuery();
			
			if( resultSet.next() ){
				return resultSet.getInt(1);
			}
			else{
				return -1;
			}
		}
		finally{
			if( connection != null ){
				connection.close();
			}
			
			if( statement != null ){
				statement.close();
			}
			
			if( resultSet != null ){
				resultSet.close();
			}
		}
	}
	
	/**
	 * Retrieves the entry matching the given filter.
	 * @param filter
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws NotFoundException
	 */
	public EventLogEntry getEntry( EventLogFilter filter ) throws NoDatabaseConnectionException, SQLException, NotFoundException{
		
		Connection connection = null;
		PreparedStatement statement = null;

		try{
			connection = application.getDatabaseConnection(DatabaseAccessType.EVENT_LOG);
			filter.count = 1;
			statement = getQuery(filter, connection, false);
			
			EventLogEntry[] entries = getEntries(statement, true);
			
			if( entries.length >= 1){
				return entries[0];
			}
			else{
				throw new NotFoundException("A log entry was not found with the given entry ID (" + filter.entryId + ")");
			}
			
		}
		finally{
			if( connection != null){
				connection.close();
			}
			
			if( statement != null){
				statement.close();
			}
		}
	}
	
	public EventLogEntry getEntry( int entryId ) throws SQLException, NoDatabaseConnectionException, NotFoundException{
		
		Connection connection = null;
		PreparedStatement statement = null;
		
		try{
			connection = application.getDatabaseConnection(DatabaseAccessType.EVENT_LOG);
			
			statement = connection.prepareStatement("Select * from EventLog where EventLogID = ?");
			statement.setInt(1, entryId);
			
			EventLogEntry[] entries = getEntries( statement, false );
			
			if( entries.length == 0 ){
				throw new NotFoundException("A log entry was not found with the given entry ID (" + entryId + ")");
			}
			else{
				return entries[0];
			}
		}
		finally{
			if( connection != null){
				connection.close();
			}
			
			if( statement != null){
				statement.close();
			}
		}
	}
	
	/**
	 * Retrieves a count of the total number of log entries.
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public int getEntriesCount( ) throws SQLException, NoDatabaseConnectionException{
		
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultset = null;
		
		try{
			connection = application.getDatabaseConnection(DatabaseAccessType.EVENT_LOG);
			
			statement = connection.prepareStatement("Select count(*) as EntriesCount from EventLog");
			
			resultset = statement.executeQuery();
			
			if( resultset.next() ){
				return resultset.getInt("EntriesCount");
			}
			else{
				return 0;
			}
		}
		finally{
			if( connection != null){
				connection.close();
			}
			
			if( statement != null){
				statement.close();
			}
			
			if( resultset != null){
				resultset.close();
			}
		}
	}
	
	/**
	 * Get a count of the log entries that match the given filter. 
	 * @param filter
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 */
	public int getEntriesCount( EventLogFilter filter ) throws NoDatabaseConnectionException, SQLException{
		
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultset = null;
		
		try{
			connection = application.getDatabaseConnection(DatabaseAccessType.EVENT_LOG);
			statement = getQuery(filter, connection, true);
			
			resultset = statement.executeQuery();
			
			if( resultset.next() ){
				return resultset.getInt("EntriesCount");
			}
			else{
				return 0;
			}
		}
		finally{
			if( connection != null){
				connection.close();
			}
			
			if( statement != null){
				statement.close();
			}
			
			if( resultset != null){
				resultset.close();
			}
		}
	}

}
