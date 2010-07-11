package net.lukemurphey.nsia.upgrade.processors;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.Application.DatabaseAccessType;
import net.lukemurphey.nsia.upgrade.UpgradeFailureException;
import net.lukemurphey.nsia.upgrade.UpgradeProcessor;

public class ScanRuleCreatedTimestamps extends UpgradeProcessor {

	public ScanRuleCreatedTimestamps(){
		super(0, 8, 104);
	}
	
	@Override
	public boolean doUpgrade( Application app ) throws UpgradeFailureException {
		int updatedRows = 0;
		
		try{
			if( app.isUsingInternalDatabase() ){
				
				Connection conn = app.getDatabaseConnection(DatabaseAccessType.ADMIN);
				PreparedStatement statement = conn.prepareStatement("ALTER TABLE ScanRule ADD Created TimeStamp default NULL");
				PreparedStatement statement2 = conn.prepareStatement("ALTER TABLE ScanRule ADD Modified TimeStamp default NULL");
				
				try{
					updatedRows = statement.executeUpdate();
					updatedRows = updatedRows + statement2.executeUpdate();
				}
				finally{
					if( conn != null ){
						conn.close();
					}
					
					if( statement != null ){
						statement.close();
					}
					
					if( statement2 != null ){
						statement2.close();
					}
				}
					
			}
		}
		catch( SQLException e ){
			throw new UpgradeFailureException("Exception throw while attempting to add the 'created' and 'updated' columns to the scan rule table", e);
		}
		catch( NoDatabaseConnectionException e ){
			throw new UpgradeFailureException("Exception throw while attempting to add the 'created' and 'updated' columns to the scan rule table", e);
		}
		
		// Determine if any rules were updated.
		if( updatedRows > 0 ){
			return true;
		}
		else{
			return false;
		}
	}
}
