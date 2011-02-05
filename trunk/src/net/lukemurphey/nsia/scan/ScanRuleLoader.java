package net.lukemurphey.nsia.scan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.*;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.scan.ScanRule.ScanRuleLoadFailureException;

/**
 * This class loads various scan rules based upon the type of the rule involved. It will always return a sub-class
 * of Scan but the specific sub-class will depend on the rule type specifier.
 * @author luke
 *
 */
public class ScanRuleLoader {

	/**
	 * Load the rule that matches the rule specified by the type specifier.
	 * @param ruleId
	 * @return
	 * @throws NoDatabaseConnectionException 
	 * @throws SQLException 
	 * @throws ScanRuleLoadFailureException 
	 * @throws Exception 
	 * @throws Exception 
	 * @throws Exception 
	 */
	public static ScanRule getScanRule( long ruleId ) throws NotFoundException, NoDatabaseConnectionException, SQLException, ScanRuleLoadFailureException{
		
		// 1 -- Determine rule type and load basic inherited attributes
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet result = null;
		ScanRule scan = null;
		
		try{
			connection = Application.getApplication().getDatabaseConnection( Application.DatabaseAccessType.SCANNER );
			statement = connection.prepareStatement("Select * from ScanRule where ScanRuleID = ?");
			statement.setLong( 1, ruleId );
			result = statement.executeQuery();
			
			if( result.next() ){
				String ruleType = result.getString("RuleType");
				boolean isScanDataObsolete = result.getBoolean("ScanDataObsolete");
				
				//Load the appropriate rule type
				if( ruleType.matches( HttpStaticScanRule.RULE_TYPE) ){
					scan = new HttpStaticScanRule( Application.getApplication() );
					scan.loadFromDatabase( ruleId );
					scan.scanDataObsolete = isScanDataObsolete;
				}
				else if( ruleType.matches( HttpSeekingScanRule.RULE_TYPE) ){
					scan = new HttpSeekingScanRule( Application.getApplication() );
					scan.loadFromDatabase( ruleId );
					scan.scanDataObsolete = isScanDataObsolete;
				}
				else if( ruleType.matches( ServiceScanRule.RULE_TYPE) ){
					scan = new ServiceScanRule( Application.getApplication() );
					scan.loadFromDatabase( ruleId );
					scan.scanDataObsolete = isScanDataObsolete;
				}
				// Note: Add new rule types above
			}
		}
		finally{
			if (statement != null )
				statement.close();
			
			if (result != null )
				result.close();
			
			if (connection != null )
				connection.close();
		}
		
		
		
		if( scan == null )
			throw new NotFoundException("No rule exists with the given identifier");
		
		return scan;
	}
	
	
	/**
	 * Load all the rules for the site group.
	 * @param ruleId
	 * @return
	 * @throws NoDatabaseConnectionException 
	 * @throws SQLException 
	 * @throws NotFoundException 
	 * @throws ScanRuleLoadFailureException 
	 * @throws Exception 
	 */
	public static ScanRule[] getScanRules( long siteGroupId ) throws NoDatabaseConnectionException, SQLException, NotFoundException, ScanRuleLoadFailureException{
		
		// 1 -- Determine rule type and load basic inherited attributes
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet result = null;
		Vector<ScanRule> scanRulesVector = new Vector<ScanRule>();
		
		try{
			connection = Application.getApplication().getDatabaseConnection( Application.DatabaseAccessType.SCANNER );
			statement = connection.prepareStatement("Select * from ScanRule where SiteGroupID = ?");
			statement.setLong( 1, siteGroupId );
			result = statement.executeQuery();

			ScanRule scan = null;
			
			while( result.next() ){
				String ruleType = result.getString("RuleType");
				long ruleId = result.getLong("ScanRuleID");
				boolean isScanDataObsolete = result.getBoolean("ScanDataObsolete");
				
				//Load the appropriate rule type
				if( ruleType.matches( HttpStaticScanRule.RULE_TYPE) ){
					scan = new HttpStaticScanRule( Application.getApplication() );
					scan.loadFromDatabase( ruleId );
					scanRulesVector.add( scan );
					scan.scanDataObsolete = isScanDataObsolete;
				}
				else if( ruleType.matches( HttpSeekingScanRule.RULE_TYPE) ){
					scan = new HttpSeekingScanRule( Application.getApplication() );
					scan.loadFromDatabase( ruleId );
					scanRulesVector.add( scan );
					scan.scanDataObsolete = isScanDataObsolete;
				}
				else if( ruleType.matches( ServiceScanRule.RULE_TYPE) ){
					scan = new ServiceScanRule( Application.getApplication() );
					scan.loadFromDatabase( ruleId );
					scan.scanDataObsolete = isScanDataObsolete;
					scanRulesVector.add( scan );
				}
				//Note: Add new rule types above
			}
		}
		finally{
			if (statement != null )
				statement.close();
			
			if (result != null )
				result.close();
			
			if (connection != null )
				connection.close();
		}
		
		// 2 -- Convert the rules vector to an array
		ScanRule[] scanRules = new ScanRule[ scanRulesVector.size() ];
		
		for( int c = 0; c < scanRulesVector.size(); c++ ){
			scanRules[c] = scanRulesVector.get(c);
		}
		
		return scanRules;
	}
	
}
