package net.lukeMurphey.nsia.xmlRpcInterface;

import java.sql.SQLException;
import java.util.*;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.InsufficientPermissionException;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.NotFoundException;
import net.lukeMurphey.nsia.SiteGroupScanResult;
import net.lukeMurphey.nsia.scanRules.ScanRule;
import net.lukeMurphey.nsia.scanRules.ScanResult;
import net.lukeMurphey.nsia.trustBoundary.ApiScanData;

public class XmlrpcScanData extends XmlrpcHandler{

	private ApiScanData untrustScanData;
	
	public XmlrpcScanData(Application appRes) {
		super(appRes);
		untrustScanData = new ApiScanData( appRes );
	}

	/**
	 * Get the status of all site groups.
	 * @param sessionIdentifier
	 * @return
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 */
	public Vector<Hashtable<String, Object>> getSiteGroupStatus( String sessionIdentifier ) throws GeneralizedException, NoSessionException 
	{
		SiteGroupScanResult[] siteGroupScanResults = untrustScanData.getSiteGroupStatus( sessionIdentifier );
		Vector<Hashtable<String, Object>> results = new Vector<Hashtable<String, Object>>();
		
		for(int c =0; c < siteGroupScanResults.length; c++){
			results.add( siteGroupScanResults[c].toHashtable() );
		}
			
		return results;
	}
	
	/**
	 * Get the status of the given site group.
	 * @param sessionIdentifier
	 * @param siteGroupId
	 * @return
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws InsufficientPermissionException 
	 * @throws NotFoundException 
	 */
	public Hashtable<String, Object> getSiteGroupStatus( String sessionIdentifier, int siteGroupId ) throws GeneralizedException, NoSessionException, InsufficientPermissionException, NotFoundException 
	{
		SiteGroupScanResult scanResult = untrustScanData.getSiteGroupStatus( sessionIdentifier, siteGroupId);
		if( scanResult != null )
			return scanResult.toHashtable();
		else
			return null;
	}
	
	/**
	 * Retrieve the rule associated with the rule identifier given. 
	 * @param sessionIdentifier
	 * @param ruleId
	 * @return
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws InsufficientPermissionException 
	 * @throws NotFoundException 
	 */
	public Hashtable<String, Object> getRule(String sessionIdentifier, double ruleId) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException{
		
		ApiScanData scanData = new ApiScanData(Application.getApplication());
		
		ScanRule scanRule = scanData.getScanRule(sessionIdentifier, (long)ruleId);
		
		return scanRule.toHashtable();

	}
	
	/**
	 * Get the rule type for the given rule.
	 * @param sessionIdentifier
	 * @param ruleId
	 * @return
	 * @throws SQLException
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 */
	public String getRuleType( String sessionIdentifier, double ruleId ) throws SQLException, NoSessionException, GeneralizedException{
		// 1 -- Get the results
		return untrustScanData.getRuleType( sessionIdentifier, (long)ruleId );
	}
	
	/**
	 * Get the scan associated with the rule and result ID.
	 * @param sessionIdentifier
	 * @param ruleId
	 * @param resultId
	 * @return
	 * @throws SQLException
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 */
	public Hashtable<String, Object> getLastScanResult( String sessionIdentifier, long ruleId ) throws SQLException, NoSessionException, GeneralizedException{

		// 1 -- Get the results
		ScanResult scanResult = untrustScanData.getLastScanResult( sessionIdentifier, ruleId );
		
		if( scanResult == null)
			return null;
		else
			return scanResult.toHashtable();
	}
	
	/**
	 * Get the last scan result for the given rule. 
	 * @param sessionIdentifier
	 * @param ruleId
	 * @return
	 * @throws SQLException
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 */
	public Hashtable<String, Object> getScanResult( String sessionIdentifier, long ruleId ) throws SQLException, NoSessionException, GeneralizedException{

		// 1 -- Get the results
		ScanResult scanResult = untrustScanData.getScanResult( sessionIdentifier, ruleId );
		if( scanResult == null)
			return null;
		else
			return scanResult.toHashtable();
	}
}
