package net.lukeMurphey.nsia.scanRules;

import java.sql.ResultSet;
import java.util.Date;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.NoDatabaseConnectionException;
import net.lukeMurphey.nsia.Application.DatabaseAccessType;

public class DefinitionError{
	
	protected String definitionName;
	protected int definitionVersion;
	protected Date dateFirstOccurred;
	protected Date dateLastOccurred;
	protected String errorName;
	protected String notes;
	protected int definitionID;
	protected int localDefinitionID;
	protected int ID = -1;
	
	private static final Object DEFINITION_ERROR_MUTEX = new Object();
	
	protected DefinitionError(String definitionName, int definitionVersion, Date dateFirstOccurred, Date dateLastOccurred, String errorName, int definitionID, int localDefinitionID){
		this.definitionName = definitionName;
		this.definitionVersion = definitionVersion;
		this.dateFirstOccurred = dateFirstOccurred;
		this.dateLastOccurred = dateLastOccurred;
		this.errorName = errorName;
		this.definitionID = definitionID;
		this.localDefinitionID = localDefinitionID;
	}
	
	protected DefinitionError(String definitionName, int definitionVersion, Date dateLastOccurred, String errorName, String notes, int definitionID, int localDefinitionID){
		this.definitionName = definitionName;
		this.definitionVersion = definitionVersion;
		this.dateFirstOccurred = new Date();
		this.dateLastOccurred = dateLastOccurred;
		this.errorName = errorName;
		this.notes = notes;
		this.definitionID = definitionID;
		this.localDefinitionID = localDefinitionID;
	}
	
	protected DefinitionError(String definitionName, int definitionVersion, Date dateFirstOccurred, Date dateLastOccurred, String errorName, String notes, int definitionID, int localDefinitionID){
		this.definitionName = definitionName;
		this.definitionVersion = definitionVersion;
		this.dateFirstOccurred = dateFirstOccurred;
		this.dateLastOccurred = dateLastOccurred;
		this.errorName = errorName;
		this.notes = notes;
		this.definitionID = definitionID;
		this.localDefinitionID = localDefinitionID;
	}
	
	protected DefinitionError(String definitionName, int definitionVersion, Date dateFirstOccurred, Date dateLastOccurred, String errorName, String notes, int definitionID, int localDefinitionID, int ID){
		this.definitionName = definitionName;
		this.definitionVersion = definitionVersion;
		this.dateFirstOccurred = dateFirstOccurred;
		this.dateLastOccurred = dateLastOccurred;
		this.errorName = errorName;
		this.notes = notes;
		this.definitionID = definitionID;
		this.localDefinitionID = localDefinitionID;
		this.ID = ID;
	}
	
	protected static DefinitionError loadFromResult( ResultSet result ) throws SQLException{
		
		String definitionName = result.getString("DefinitionName");
		int definitionVersion = result.getInt("DefinitionVersion");
		Date dateFirstOccurred = result.getDate("DateFirstOccurred");
		Date dateLastOccurred = result.getDate("DateLastOccurred");
		String errorName = result.getString("ErrorName");
		String notes = result.getString("Notes");
		int definitionID = result.getInt("DefinitionID");
		int localDefinitionID = result.getInt("LocalDefinitionID");
		int ID = result.getInt("DefinitionErrorLogID");
		
		return new DefinitionError(definitionName, definitionVersion, dateFirstOccurred, dateLastOccurred, errorName, notes, definitionID, localDefinitionID, ID );
	}
	
	private int updateExistingEntry(Date lastObserved, String definitionName, int definitionVersion, String errorName, String notes  ) throws NoDatabaseConnectionException, SQLException{
		Connection conn = null;
		PreparedStatement statement = null;
		
		try{
			conn = Application.getApplication().getDatabaseConnection(DatabaseAccessType.SCANNER);
			statement = conn.prepareStatement("Update DefinitionErrorLog set DateLastOccurred = ?, Notes = ? where DefinitionName = ? and DefinitionVersion = ? and ErrorName = ? ");
			statement.setDate(1, new java.sql.Date( lastObserved.getTime() ));
			statement.setString(2, notes);
			statement.setString(3, definitionName);
			statement.setInt(4, definitionVersion);
			statement.setString(5, errorName);
			
			
			return statement.executeUpdate();
		}
		finally{
			if( conn != null ){
				conn.close();
			}
			
			if( statement != null ){
				statement.close();
			}
		}
			
	}
	
	public void save() throws NoDatabaseConnectionException, SQLException{
		
		synchronized(DEFINITION_ERROR_MUTEX){
			// Try to update the existing entry first
			if( updateExistingEntry(this.dateLastOccurred, this.definitionName, this.definitionVersion, this.errorName, this.notes) >= 1 ){
				return;
			}
			
			Connection conn = null;
			PreparedStatement statement = null;
			
			try{
				conn = Application.getApplication().getDatabaseConnection(DatabaseAccessType.SCANNER);
				statement = conn.prepareStatement("insert into DefinitionErrorLog (DefinitionName, DefinitionVersion, DateFirstOccurred, DateLastOccurred, ErrorName, Notes, DefinitionID, LocalDefinitionID) values (?, ?, ?, ?, ?, ?, ?, ?)");
				
				statement.setString(1, definitionName);
				statement.setInt(2, definitionVersion );
				statement.setDate(3, new java.sql.Date( dateFirstOccurred.getTime() ));
				statement.setDate(4, new java.sql.Date( dateLastOccurred.getTime() ));
				statement.setString(5, errorName);
				statement.setString(6, notes);
				statement.setInt(7, definitionID);
				statement.setInt(8, localDefinitionID);
				
				statement.executeUpdate();
			}
			finally{
				if( conn != null ){
					conn.close();
				}
				
				if( statement != null ){
					statement.close();
				}
			}
		}
	}
	
	public String getDefinitionName(){
		return definitionName;
	}
	
	public int getDefinitionVersion(){
		return definitionVersion;
	}
	
	public int getDefinitionID(){
		return definitionID;
	}
	
	public Date getDateFirstOccurred(){
		return dateFirstOccurred;
	}
	
	public Date getDateLastOccurred(){
		return dateLastOccurred;
	}
	
	public String getErrorName(){
		return errorName;
	}
	
	public String getNotes(){
		return notes;
	}
	
	public int getLocalDefinitionID(){
		return localDefinitionID;
	}
	
	protected void clear(Connection connection) throws NoDatabaseConnectionException, SQLException{
		PreparedStatement statement = null;
		
		try{
			if( ID > -1 ){
				statement = connection.prepareStatement("DELETE FROM DefinitionErrorLog WHERE DefinitionErrorLogID = ?");
				statement.setInt(1, this.ID);
				
				statement.execute();
			}
		}
		finally{
			if( statement != null ){
				statement.close();
			}
		}
		
	}
	
}