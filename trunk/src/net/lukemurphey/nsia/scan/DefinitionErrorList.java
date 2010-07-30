package net.lukemurphey.nsia.scan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.Application.DatabaseAccessType;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogMessage.EventType;

/**
 * This class maintains a list of errors noted with definitions.
 * @author Luke
 *
 */
public class DefinitionErrorList {
	
	protected Vector<DefinitionError> definitionsErrors = new Vector<DefinitionError>();
	//protected Application application;
	private static Boolean errorsNoted = null;
	
	public static synchronized boolean errorsNoted(Application application) throws NoDatabaseConnectionException, SQLException{
		if( errorsNoted == null ){
			DefinitionErrorList errors = DefinitionErrorList.load(application);
			
			if( errors != null && errors.getErrorsList() != null && errors.getErrorsList().length > 0 ){
				errorsNoted = true;
			}
			else{
				errorsNoted = false;
			}
		}
		
		return errorsNoted;
	}
	
	public void clearOutdatedErrors( DefinitionSet set ) throws NoDatabaseConnectionException, SQLException{
		
		synchronized (this.definitionsErrors) {
			
			Iterator<DefinitionError> it = this.definitionsErrors.iterator();
			Connection connection = null;
			
			try{
				
				connection = Application.getApplication().getDatabaseConnection(DatabaseAccessType.SCANNER);
				
				while( it.hasNext() ){
					DefinitionError definitionError = it.next();
					
					try {
						Definition definition = set.getDefinition(definitionError.getDefinitionID());
						if (definition.revision != definitionError.definitionVersion ){
							definitionError.clear(connection);
							it.remove();
						}
					} catch (NotFoundException e) {
						definitionError.clear(connection);
						it.remove();
					}
					
				}
			}
			finally{
				if( connection != null ){
					connection.close();
				}
			}
		
		}
		
		//Reset the boolean indicating that errors exist
		if(this.definitionsErrors.size()== 0){
			errorsNoted = false;
		}
	}
	
	public DefinitionError[] getErrorsList(){
		
		DefinitionError[] errors = new DefinitionError[definitionsErrors.size()];
		definitionsErrors.toArray(errors);
		
		return errors;
	}
	
	public static void logError(String definitionName, int definitionVersion, String errorName, String notes, int definitionID, int localDefinitionID){
		DefinitionError error = new DefinitionError( definitionName, definitionVersion, new Date(), errorName, notes, definitionID, localDefinitionID );
		
		try {
			error.save();
			errorsNoted = null;
			
		} catch (NoDatabaseConnectionException e) {
			Application.getApplication().getEventLog().logExceptionEvent(new EventLogMessage(EventType.DATABASE_FAILURE), e);
		} catch (SQLException e) {
			Application.getApplication().getEventLog().logExceptionEvent(new EventLogMessage(EventType.SQL_EXCEPTION), e);
		}
	}
	
	public static DefinitionErrorList load(Application app) throws NoDatabaseConnectionException, SQLException{
		
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try{
			connection = app.getDatabaseConnection(DatabaseAccessType.SCANNER);
			statement = connection.prepareStatement("Select * from DefinitionErrorLog where Relevant = 1");
			
			DefinitionErrorList list = new DefinitionErrorList();
			result = statement.executeQuery();
			
			while( result.next() ){
				list.definitionsErrors.add( DefinitionError.loadFromResult(result) );
			}
			
			return list;
		}
		finally{
			
			if( result != null ){
				result.close();
			}
			
			if( statement != null ){
				statement.close();
			}
			
			if( connection != null ){
				connection.close();
			}
		}
		
	}
}
