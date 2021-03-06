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
		int updated = 0;
		
		Connection conn = null;
		PreparedStatement statement = null;
		PreparedStatement statement2 = null;
		
		try{
			if( app.isUsingInternalDatabase() ){
				
				conn = app.getDatabaseConnection(DatabaseAccessType.ADMIN);
				statement = conn.prepareStatement("ALTER TABLE ScanRule ADD Created TimeStamp default NULL");
				statement2 = conn.prepareStatement("ALTER TABLE ScanRule ADD Modified TimeStamp default NULL");
				
				try{
					
					if( hasColumn(app, "ScanRule", "Created") == false ){
						statement.executeUpdate();
						updated = updated + 1;
					}
					
					if( hasColumn(app, "ScanRule", "Modified") == false ){
						statement2.executeUpdate();
						updated = updated + 1;
					}
				}
				finally{
					conn.close();
					
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
		
		// Determine if any tables were updated.
		if( updated > 0 ){
			return true;
		}
		else{
			return false;
		}
	}
}
