package net.lukemurphey.nsia.scan;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang.StringEscapeUtils;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.Application.DatabaseAccessType;
import net.lukemurphey.nsia.SiteGroupManagement.SiteGroupDescriptor;

public class DefinitionPolicyManagement {
	
	private Application application = null;

	public DefinitionPolicyManagement(Application application){
		
		// 0 -- Precondition check
		if( application == null ){
			throw new IllegalArgumentException("The application instance must not be null");
		}
		
		// 1 -- Initialize the class
		this.application = application;
	}
	
	public DefinitionPolicySet getPolicySet() throws NoDatabaseConnectionException, SQLException{
		Connection connection = null;
		
		connection = application.getDatabaseConnection( Application.DatabaseAccessType.SCANNER );
		
		try{
			return DefinitionPolicySet.getPolicySet(connection);
		} finally {
			
			if (connection != null )
				connection.close();
		}
	}
	
	public DefinitionPolicySet getPolicySet( long siteGroupID ) throws NoDatabaseConnectionException, SQLException{
		Connection connection = null;
		
		connection = application.getDatabaseConnection( Application.DatabaseAccessType.SCANNER );
		
		try{
			return DefinitionPolicySet.getPolicySetForSiteGroup(connection, siteGroupID);
		} finally {
			
			if (connection != null )
				connection.close();
		}
	}
	
	public DefinitionPolicySet getPolicySetForRule( int ruleID ) throws NoDatabaseConnectionException, SQLException{
		Connection connection = null;
		
		connection = application.getDatabaseConnection( Application.DatabaseAccessType.SCANNER );
		
		try{
			return DefinitionPolicySet.getPolicySetForRule(connection, ruleID);
		} finally {
			
			if (connection != null )
				connection.close();
		}
	}
	
	public DefinitionPolicyDescriptor getPolicy( int policyID ) throws SQLException, MalformedURLException, NoDatabaseConnectionException{
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet result = null;
		DefinitionPolicyDescriptor sigException = null;
		
		try{
			connection = application.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			statement = connection.prepareStatement("Select * from DefinitionPolicy where DefinitionPolicyID = ?");
			statement.setLong(1, policyID);
			
			result = statement.executeQuery();
			
			if(result.next()){
				sigException = DefinitionPolicyDescriptor.loadFromResult(result);
			}
			
		}
		finally{
			if(statement != null ){
				statement.close();
			}
			
			if(connection != null ){
				connection.close();
			}
			
			if(result != null ){
				result.close();
			}
		}
		
		return sigException;
	}
	
	public int clearCategoryDescriptors(int siteGroupID, String categoryName ) throws NoDatabaseConnectionException, SQLException{
		return clearEntries(siteGroupID, -1, null, null, null, categoryName);
	}
	
	public int clearCategoryDescriptors(SiteGroupDescriptor siteGroup, String categoryName ) throws NoDatabaseConnectionException, SQLException{
		return clearEntries(siteGroup.getGroupId(), -1, null, null, null, categoryName);
	}
	
	public int clearSubCategoryDescriptors(String categoryName, String subCategoryName ) throws NoDatabaseConnectionException, SQLException{
		return clearEntries(-1, -1, null, subCategoryName, null, categoryName);
	}
	
	public int clearSubCategoryDescriptors(int siteGroupID, String categoryName, String subCategoryName ) throws NoDatabaseConnectionException, SQLException{
		return clearEntries(siteGroupID, -1, null, subCategoryName, null, categoryName);
	}
	
	public int clearSubCategoryDescriptors(SiteGroupDescriptor siteGroup, String categoryName, String subCategoryName ) throws NoDatabaseConnectionException, SQLException{
		return clearEntries(siteGroup.getGroupId(), -1, null, subCategoryName, null, categoryName);
	}
	
	private int clearEntries( int siteGroupId, int ruleID, String definitionName, String definitionSubCategory, URL url, String definitionCategory ) throws NoDatabaseConnectionException, SQLException{
		
		Connection connection = null;
		PreparedStatement statement = null;
		
		try{
			connection = application.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			StringBuffer whereClause = new StringBuffer();
			
			// 1 -- Add site group restriction
			if( siteGroupId < 0 ){
				whereClause.append(" (SiteGroupID is null or SiteGroupID = -1) and ");
			}
			else{
				whereClause.append(" SiteGroupID = ");
				whereClause.append(siteGroupId);
				whereClause.append(" and");
			}
			
			// 2 -- Add rule ID restriction
			if( ruleID < 0 ){
				whereClause.append(" (RuleID = -1 or RuleID is null) and ");
			}
			else{
				whereClause.append(" RuleID = ");
				whereClause.append(ruleID);
				whereClause.append(" and");
			}
			
			// 3 -- Add definition name restriction
			if( definitionName == null ){
				whereClause.append(" DefinitionName is null and ");
			}
			else{
				whereClause.append(" DefinitionName = '");
				whereClause.append( StringEscapeUtils.escapeSql( definitionName ) );
				whereClause.append("' and");
			}
			
			// 4 -- Add definition sub-category restriction
			if( definitionSubCategory == null ){
				whereClause.append(" DefinitionSubCategory is null and ");
			}
			else{
				whereClause.append(" DefinitionSubCategory = '");
				whereClause.append( StringEscapeUtils.escapeSql( definitionSubCategory ) );
				whereClause.append("' and");
			}
			
			// 5 -- Add definition category restriction
			if( definitionCategory == null ){
				whereClause.append(" DefinitionCategory is null and ");
			}
			else{
				whereClause.append(" DefinitionCategory = '");
				whereClause.append( StringEscapeUtils.escapeSql( definitionCategory ) );
				whereClause.append("' and");
			}
			
			// 6 -- Add URL restriction
			if( url == null ){
				whereClause.append(" URL is null ");
			}
			else{
				whereClause.append(" URL = '");
				whereClause.append( StringEscapeUtils.escapeSql( url.toExternalForm() ) );
				whereClause.append("'");
			}
			
			statement = connection.prepareStatement( "delete from DefinitionPolicy where " + whereClause.toString() );
			
			return statement.executeUpdate();
		
		}
		finally{
			
			if( statement != null ){
				statement.close();
			}
			
			if( connection != null ){
				connection.close();
			}
		}
		
	}
	
	public boolean deletePolicy(int policyID) throws NoDatabaseConnectionException, SQLException{
		
		// 0 -- precondition check
		
		//	 0.1 -- Make sure the argument is valid
		if( policyID < 1 ){
			throw new IllegalArgumentException("The exception ID must be greater than 0");
		}
		
		// 1 -- Delete the policy
		Connection conn = application.getDatabaseConnection( Application.DatabaseAccessType.SCANNER );
		PreparedStatement preparedStatement = null;
		
		try{
			preparedStatement = conn.prepareStatement("Delete from DefinitionPolicy where DefinitionPolicyID = ?");
			preparedStatement.setLong(1, policyID );
			
			if( preparedStatement.executeUpdate() != 1 )
				return false;
			else
				return true;
		} finally {
			
			if (preparedStatement != null )
				preparedStatement.close();
			
			if (conn != null )
				conn.close();
		}
	}

}