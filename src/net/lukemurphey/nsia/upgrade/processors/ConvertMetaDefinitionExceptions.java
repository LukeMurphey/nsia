package net.lukemurphey.nsia.upgrade.processors;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.Application.DatabaseAccessType;
import net.lukemurphey.nsia.upgrade.UpgradeFailureException;
import net.lukemurphey.nsia.upgrade.UpgradeProcessor;

public class ConvertMetaDefinitionExceptions extends UpgradeProcessor{

	@Override
	public boolean doUpgrade(Application application)
			throws UpgradeFailureException {
		
		Connection conn = null;
		
		try{
			if( application.isUsingInternalDatabase() ){
				conn = application.getDatabaseConnection(DatabaseAccessType.ADMIN);
				convertSubCategoryException(conn, "ContentError", "Error", "Anomaly", "Quality", true);
				
			}
		}
		catch( SQLException e ){
			throw new UpgradeFailureException("Exception throw while attempting to add the 'created' and 'updated' columns to the scan rule table", e);
		}
		catch( NoDatabaseConnectionException e ){
			throw new UpgradeFailureException("Exception throw while attempting to add the 'created' and 'updated' columns to the scan rule table", e);
		}
		
		return true;
	}
	
	private void convertCategoryException( Connection conn, String oldCategoryName, String newCategoryName ) throws SQLException{
		PreparedStatement statement = null;
		
		try{
			statement = conn.prepareStatement("Update DefinitionPolicy set DefinitionCategory = ? where DefinitionCategory = ? and DefinitionSubCategory is null and DefinitionName is null");
			
			statement.setString(1, newCategoryName);
			statement.setString(2, oldCategoryName);
			
			statement.executeUpdate();
			
		}
		finally{
			if( statement != null ){
				statement.close();
			}
		}
	}
	
	private void convertSubCategoryException( Connection conn, String oldSubCategoryName, String newSubCategoryName, String oldCategoryName, String newCategoryName, boolean convertAll ) throws SQLException{
		
		PreparedStatement statement = null;
		
		try{
			
			if( convertAll ){
				statement = conn.prepareStatement("Update DefinitionPolicy set DefinitionCategory = ?, DefinitionSubCategory = ? where DefinitionCategory = ? and DefinitionSubCategory = ?");
			}
			else{
				statement = conn.prepareStatement("Update DefinitionPolicy set DefinitionCategory = ?, DefinitionSubCategory = ? where DefinitionCategory = ? and DefinitionSubCategory = ? and DefinitionName is null");
			}
			
			
			statement.setString(1, newCategoryName);
			statement.setString(2, newSubCategoryName);
			
			statement.setString(3, oldCategoryName);
			statement.setString(4, oldSubCategoryName);
			
			statement.executeUpdate();
			
		}
		finally{
			if( statement != null ){
				statement.close();
			}
		}
	}
	
	private void convertDefinitionException( Connection conn, String oldSubCategoryName, String newSubCategoryName, String oldCategoryName, String newCategoryName, String oldDefinitionName,  String newDefinitionName ) throws SQLException{
		
		PreparedStatement statement = null;
		
		try{
			statement = conn.prepareStatement("Update DefinitionPolicy set DefinitionCategory = ?, DefinitionSubCategory = ?, DefinitionName =? where DefinitionCategory = ? and DefinitionSubCategory = ? and DefinitionName = ?");
			
			statement.setString(1, newCategoryName);
			statement.setString(2, newSubCategoryName);
			statement.setString(3, newDefinitionName);
			
			statement.setString(4, oldCategoryName);
			statement.setString(5, oldSubCategoryName);
			statement.setString(6, oldDefinitionName);
			
			statement.executeUpdate();
			
		}
		finally{
			if( statement != null ){
				statement.close();
			}
		}
	}

}
