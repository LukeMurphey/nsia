package net.lukemurphey.nsia.trustBoundary;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Vector;
import java.util.Hashtable;


import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.MaxMinCount;
import net.lukemurphey.nsia.NameIntPair;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.SiteGroupScanResult;
import net.lukemurphey.nsia.Wildcard;
import net.lukemurphey.nsia.SiteGroupManagement.SiteGroupDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.scan.Definition;
import net.lukemurphey.nsia.scan.DefinitionMatch;
import net.lukemurphey.nsia.scan.DefinitionSetLoadException;
import net.lukemurphey.nsia.scan.HttpDefinitionScanResult;
import net.lukemurphey.nsia.scan.HttpHeaderRule;
import net.lukemurphey.nsia.scan.HttpHeaderScanResult;
import net.lukemurphey.nsia.scan.HttpSeekingScanResult;
import net.lukemurphey.nsia.scan.HttpSeekingScanRule;
import net.lukemurphey.nsia.scan.HttpStaticScanRule;
import net.lukemurphey.nsia.scan.NetworkPortRange;
import net.lukemurphey.nsia.scan.RuleBaselineException;
import net.lukemurphey.nsia.scan.ScanResult;
import net.lukemurphey.nsia.scan.ScanResultLoader;
import net.lukemurphey.nsia.scan.ScanRule;
import net.lukemurphey.nsia.scan.ScanRuleLoader;
import net.lukemurphey.nsia.scan.ServiceScanRule;
import net.lukemurphey.nsia.scan.ScanRule.ScanResultLoadFailureException;
import net.lukemurphey.nsia.scan.ScanRule.ScanRuleLoadFailureException;


import java.util.regex.*;

import java.io.IOException;
import java.net.URL;

import javax.script.ScriptException;


/**
 * This class provides a wrapper to the scan data. This class respects the ACLs and logs appropriate events. 
 * @author luke
 *
 */
public class ApiScanData extends ApiHandler{
	
	public ApiScanData(Application appRes) {
		super(appRes);
	}

	/**
	 * Get the status of all site groups.
	 * @param sessionIdentifier
	 * @return
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws NotFoundException 
	 */
	public SiteGroupScanResult[] getSiteGroupStatus( String sessionIdentifier ) throws GeneralizedException, NoSessionException 
	{
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		//TODO Respect ACLs (for each SiteGroup)
		
		// 1 -- Retrieve the data
		try {

			SiteGroupScanResult[] siteGroupScanResults = scanData.getSiteGroupStatus( );

			return siteGroupScanResults;
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		catch (ScanResultLoadFailureException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Gets the site group that owns the given rule identifier.
	 * @param sessionIdentifier
	 * @param ruleId
	 * @return
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws InsufficientPermissionException 
	 * @throws NotFoundException 
	 */
	public int getAssociatedSiteGroup( String sessionIdentifier, long ruleId ) throws GeneralizedException, NoSessionException, InsufficientPermissionException, NotFoundException{
		
		// 0 -- Precondition Check
		
		// 0.1 -- Make sure the user has a valid session and can read the rule
		try {
			//ScanRule scanRule = ScanRuleLoader.getScanRule(ruleId);
			//long ruleObjectId = scanRule.getObjectId();
			//checkRead(sessionIdentifier, ruleObjectId, "Get site group associated with rule " + ruleId);
			
			int siteGroupID = ScanRule.getAssociatedSiteGroupID( ruleId );
			
			SiteGroupDescriptor siteGroupDescriptor = this.siteGroupManagement.getGroupDescriptor(siteGroupID);

			checkRead(sessionIdentifier, siteGroupDescriptor.getObjectId(), "Get site group associated with scan rule " + ruleId);
			
			return siteGroupID;
			
			
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException(e);
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException(e);
		}
		/*} catch (ScanRuleLoadFailureException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e);
			throw new GeneralizedException(e);
		}*/
		catch(InputValidationException e){
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}

	}
	
	public MaxMinCount getEntryInfo( String sessionIdentifier, long ruleId ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, NotFoundException{
		// 0 -- Make sure the user has permission to read the site group
		try{
			int siteGroupID = ScanRule.getAssociatedSiteGroupID(ruleId);
			
			SiteGroupDescriptor siteGroupDesc = siteGroupManagement.getGroupDescriptor(siteGroupID);
			
			checkRead(sessionIdentifier, siteGroupDesc.getObjectId(), "Get scan result history for site group \"" + siteGroupDesc.getGroupName() + "\" (" + siteGroupID + ")");
			
			// 1 -- Get the results
			return ScanResultLoader.getEntryInfo(ruleId);
		}
		catch(NoDatabaseConnectionException e){
			appRes.logExceptionEvent( EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		catch(SQLException e){
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		}
		catch(InputValidationException e){
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
	}
	
	public long getMaxEntry( String sessionIdentifier, long ruleId ) throws GeneralizedException, InsufficientPermissionException, NoSessionException, NotFoundException{
		// 0 -- Make sure the user has permission to read the site group
		try{
			int siteGroupID = ScanRule.getAssociatedSiteGroupID(ruleId);
			
			SiteGroupDescriptor siteGroupDesc = siteGroupManagement.getGroupDescriptor(siteGroupID);
			
			checkRead(sessionIdentifier, siteGroupDesc.getObjectId(), "Get maximum scan result identifier for site group \"" + siteGroupDesc.getGroupName() + "\" (" + siteGroupID + ")");
			
			// 1 -- Get the results
			return ScanResultLoader.getMaxEntry(ruleId);
		}
		catch(NoDatabaseConnectionException e){
			appRes.logExceptionEvent( EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		catch(SQLException e){
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		}
		catch(InputValidationException e){
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
	}
	
	public long getMinEntry( String sessionIdentifier, long ruleId ) throws GeneralizedException, InsufficientPermissionException, NoSessionException, NotFoundException{
		// 0 -- Make sure the user has permission to read the site group
		try{
			int siteGroupID = ScanRule.getAssociatedSiteGroupID(ruleId);
			
			SiteGroupDescriptor siteGroupDesc = siteGroupManagement.getGroupDescriptor(siteGroupID);
			
			checkRead(sessionIdentifier, siteGroupDesc.getObjectId(), "Get minimum scan result identifier for site group \"" + siteGroupDesc.getGroupName() + "\" (" + siteGroupID + ")");
			
			// 1 -- Get the results
			return ScanResultLoader.getMinEntry(ruleId);
		}
		catch(NoDatabaseConnectionException e){
			appRes.logExceptionEvent( EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		catch(SQLException e){
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		}
		catch(InputValidationException e){
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
	}
	
	public ScanResult[] getScanResults( String sessionIdentifier, long firstScanResultID, long ruleId, int count, boolean getResultsAfter ) throws NoSessionException, GeneralizedException, InsufficientPermissionException, NotFoundException{
		
		// 0 -- Make sure the user has permission to read the site group
		try{
			int siteGroupID = ScanRule.getAssociatedSiteGroupID(ruleId);
			
			SiteGroupDescriptor siteGroupDesc = siteGroupManagement.getGroupDescriptor(siteGroupID);
			checkRead(sessionIdentifier, siteGroupDesc.getObjectId(), "Get scan results for site group \"" + siteGroupDesc.getGroupName() + "\" (" + siteGroupID + ")");
			
			// 1 -- Get the results
			ScanResult[] results = ScanResultLoader.getScanResults(ruleId, firstScanResultID, count, getResultsAfter);
			return results;
		}
		catch(NoDatabaseConnectionException e){
			appRes.logExceptionEvent( EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		catch(SQLException e){
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		}
		catch(InputValidationException e){
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
		catch (ScanResultLoadFailureException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
	}
	
	public ScanResult[] getScanResults( String sessionIdentifier, long firstScanResultID, long ruleId, int count ) throws NoSessionException, GeneralizedException, InsufficientPermissionException, InputValidationException, NotFoundException{
		
		// 0 -- Make sure the user has permission to read the site group
		try{
			int siteGroupID = ScanRule.getAssociatedSiteGroupID(ruleId);
			
			SiteGroupDescriptor siteGroupDesc = siteGroupManagement.getGroupDescriptor(siteGroupID);
			checkRead(sessionIdentifier, siteGroupDesc.getObjectId(), "Get scan results for site group \"" + siteGroupDesc.getGroupName() + "\" (" + siteGroupID + ")");
			
			// 1 -- Get the results
			ScanResult[] results = ScanResultLoader.getScanResults(ruleId, firstScanResultID, count, false);
			return results;
		}
		catch(NoDatabaseConnectionException e){
			appRes.logExceptionEvent( EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		catch(SQLException e){
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		}
		catch (ScanResultLoadFailureException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
	}
	
	public ScanResult[] getScanResults( String sessionIdentifier, long ruleId, int count ) throws NoSessionException, GeneralizedException, InsufficientPermissionException, InputValidationException, NotFoundException{
		
		// 0 -- Make sure the user has permission to read the site group
		try{
			int siteGroupID = ScanRule.getAssociatedSiteGroupID(ruleId);
			
			SiteGroupDescriptor siteGroupDesc = siteGroupManagement.getGroupDescriptor(siteGroupID);
			checkRead(sessionIdentifier, siteGroupDesc.getObjectId(), "Get scan results for site group \"" + siteGroupDesc.getGroupName() + "\" (" + siteGroupID + ")");
			
			// 1 -- Get the results
			ScanResult[] results = ScanResultLoader.getScanResults(ruleId, count);
			return results;
		}
		catch(NoDatabaseConnectionException e){
			appRes.logExceptionEvent( EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		catch(SQLException e){
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		}
		catch (ScanResultLoadFailureException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
	}
	
	public ScanRule[] getScanRules( String sessionIdentifier, int siteGroupId ) throws NoSessionException, GeneralizedException, InsufficientPermissionException, InputValidationException, NotFoundException{
		
		// 0 -- Make sure the user has permission to read the site group
		try{
			SiteGroupDescriptor siteGroupDesc = siteGroupManagement.getGroupDescriptor(siteGroupId);
			
			checkRead(sessionIdentifier, siteGroupDesc.getObjectId(), "Get rules for site group \"" + siteGroupDesc.getGroupName()  + "\" (" + siteGroupId + ")");
			
			// 1 -- Get the rules
			ScanRule[] rules = ScanRuleLoader.getScanRules( siteGroupId );
			return rules;
		}
		catch(NoDatabaseConnectionException e){
			appRes.logExceptionEvent( EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		catch(SQLException e){
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		}
		catch(ScanRuleLoadFailureException e){
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
	}
	
	
	/**
	 * Load the scan rule specified by the rule identifier.
	 * @param sessionIdentifier
	 * @param scanRuleId
	 * @return
	 * @throws GeneralizedException 
	 * @throws NoSessionException 
	 * @throws Exception 
	 */
	public ScanRule getScanRule( String sessionIdentifier, long scanRuleId ) throws NoSessionException, GeneralizedException, NotFoundException, InsufficientPermissionException{
		ScanRule scanRule = null;
		
		try {
			// 0 -- Precondition check
			// 	0.1 -- Make sure the user has a valid session and has permission to read the rule
			
			// Get the site group that contains this rule to determine if the user has permission to view this group
			int siteGroupId = ScanRule.getAssociatedSiteGroupID(scanRuleId);
			SiteGroupDescriptor siteGroupDescriptor = siteGroupManagement.getGroupDescriptor(siteGroupId);
			
			// Determine if the user can view this site group
			checkRead(sessionIdentifier, siteGroupDescriptor.getObjectId(), "Get scan rule " + scanRuleId + " for site group \"" + siteGroupDescriptor.getGroupName()  + "\" (" + siteGroupId + ")");
			
			
			// 1 -- Get the scan rule
			scanRule = ScanRuleLoader.getScanRule( scanRuleId );
			
			if( scanRule == null )
				return null;
			
			//checkRead(sessionIdentifier, scanRule.getObjectId());
			
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException(e);
		} catch (NoDatabaseConnectionException e) {
			Application.getApplication().logExceptionEvent( EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException(e);
		} catch (ScanRuleLoadFailureException e) {
			throw new GeneralizedException(e);
		} catch (InputValidationException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException(e);
		}
		
		return scanRule;
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
	public SiteGroupScanResult getSiteGroupStatus( String sessionIdentifier, int siteGroupId ) throws GeneralizedException, NoSessionException, InsufficientPermissionException, NotFoundException 
	{
		//	 0.1 -- Make sure the user has a valid session and has permission to view the group
		checkSession( sessionIdentifier );

		// 1 -- Retrieve the data
		try {
			SiteGroupScanResult scanResult = scanData.getSiteGroupStatus( siteGroupId );
			
			long objectId = scanResult.getSiteGroupDescriptor().getObjectId();
			checkRead(sessionIdentifier, objectId, "Get status for site group \"" + scanResult.getSiteGroupDescriptor().getGroupName() + "\" (" + siteGroupId + ")");
			
			return scanResult;
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (ScanResultLoadFailureException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
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
	public String getRuleType( String sessionIdentifier, long ruleId ) throws NoSessionException, GeneralizedException{
		
		// 0 -- Precondition check
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		// 1 -- Get the results
		try {
			return scanData.getRuleType( ruleId );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Retrieves a count of the rule severities triggered for the given scan result.
	 * @param sessionIdentifier
	 * @param resultId
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 */
	public Hashtable<Definition.Severity, Integer> getHTTPDefinitionMatchSeverities(String sessionIdentifier, long resultId) throws NoSessionException, GeneralizedException{
		
		// 0 -- Precondition check
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		// 1 -- Get the results
		try {
			return HttpDefinitionScanResult.getSignatureMatchSeverities(resultId);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
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
	public ScanResult getScanResult( String sessionIdentifier, long resultId ) throws NoSessionException, GeneralizedException{
		
		// 0 -- Precondition check
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		// 1 -- Get the results
		ScanResult scanResult;
		try {
			scanResult = scanData.getScanResult( resultId );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (ScanResultLoadFailureException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
		
		return scanResult;
	}
	
	/**
	 * Get the information 
	 * @param sessionIdentifier
	 * @param scanResultID
	 * @return
	 * @throws GeneralizedException 
	 */
	public MaxMinCount getHTTPSeekingResultInfo( String sessionIdentifier, long scanResultID, HttpDefinitionScanResult.SignatureScanResultFilter filter  ) throws GeneralizedException{
		try {
			return HttpSeekingScanResult.getScanResultInfo(scanResultID, filter, this.appRes);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Get the rules that matched the given rule (along with the count).
	 * @param sessionIdentifier
	 * @param parentScanResultID
	 * @return
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 */
	public Vector<NameIntPair> getHTTPSeekingDefinitionMatches( String sessionIdentifier, long parentScanResultID ) throws GeneralizedException, NoSessionException{
		try {
			return HttpDefinitionScanResult.getSignatureMatches(parentScanResultID);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Get the content-types that matched the given rule (along with the count).
	 * @param sessionIdentifier
	 * @param scanResultID
	 * @return
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 */
	public Vector<NameIntPair> getDiscoveredContentTypes( String sessionIdentifier, long scanResultID ) throws GeneralizedException, NoSessionException{
		
		// 0 -- Precondition check
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		// 1 -- Get the results
		try {
			return HttpSeekingScanResult.getDiscoveredContentTypes(scanResultID);
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
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
	public ScanResult getLastScanResult( String sessionIdentifier, long ruleId ) throws NoSessionException, GeneralizedException{

		// 0 -- Precondition check
		//	 0.1 -- Make sure the user has a valid session
		checkSession( sessionIdentifier );
		
		// 1 -- Get the results
		ScanResult scanResult;
		try {
			scanResult = scanData.getLastScanResult( ruleId );
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (ScanResultLoadFailureException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
		
		if( scanResult == null)
			return null;
		else
			return scanResult;
	}
	
	/**
	 * Add a new service scan rule.
	 * 
	 * @param sessionIdentifier
	 * @param siteGroupId
	 * @param serverAddress
	 * @param portsToScan
	 * @param portsExpectedOpen
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws NotFoundException
	 */
	public long addServiceScanRule( String sessionIdentifier,  int siteGroupId, String serverAddress, NetworkPortRange[] portsToScan, NetworkPortRange[] portsExpectedOpen, int scanFrequency ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, NotFoundException{
		try{
			// 0 -- Precondition check
			
			//	 0.1 -- Check permissions
			SiteGroupDescriptor desc = siteGroupManagement.getGroupDescriptor(siteGroupId);
			checkCreate(sessionIdentifier, desc.getGroupId(), "Add service scan rule to site group \"" + desc.getGroupName() +"\" (" + desc.getGroupId() + ")");
			
			// 1 -- Create the rule
			ServiceScanRule rule = new ServiceScanRule(appRes, serverAddress, portsExpectedOpen, portsToScan);
			rule.setScanFrequency(scanFrequency);
			return rule.saveNewRuleToDatabase(siteGroupId);
			
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Update the given service scan rule. 
	 * 
	 * @param sessionIdentifier
	 * @param siteGroupId
	 * @param serverAddress
	 * @param portsToScan
	 * @param portsExpectedOpen
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws NotFoundException
	 */
	public long updateServiceScanRule( String sessionIdentifier,  long scanRuleId, String serverAddress, NetworkPortRange[] portsToScan, NetworkPortRange[] portsExpectedOpen, int scanFrequency ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, NotFoundException{
		try{
			// 0 -- Precondition check
			
			//	 0.1 -- Find the associated site group
			int siteGroupId = ScanRule.getAssociatedSiteGroupID(scanRuleId);
			
			//	 0.2 -- Check permissions
			SiteGroupDescriptor desc = siteGroupManagement.getGroupDescriptor(siteGroupId);
			checkCreate(sessionIdentifier, desc.getGroupId(), "Update service scan rule " + scanRuleId + " for site group \"" + desc.getGroupName() +"\" (" + desc.getGroupId() + ")");
			
			// 1 -- Update the rule
			ServiceScanRule rule = (ServiceScanRule)ScanRuleLoader.getScanRule(scanRuleId);
			
			rule.setServerAddress( serverAddress );
			rule.setPortsToScan(portsToScan);
			rule.setPortsExpectedOpen(portsExpectedOpen);
			rule.setScanFrequency(scanFrequency);
			return rule.saveToDatabase();
			
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}catch (ScanRuleLoadFailureException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Creates an HTTP Discovery rule.
	 * @param siteGroupId
	 * @param restrictToDomain
	 * @param recursionDepth
	 * @param scanFrequency
	 * @param urls
	 * @param scanCountLimit
	 * @return
	 * @throws GeneralizedException
	 * @throws NotFoundException 
	 * @throws NoDatabaseConnectionException 
	 * @throws InputValidationException 
	 * @throws SQLException 
	 * @throws NoSessionException 
	 * @throws InsufficientPermissionException 
	 */
	public long addHttpDiscoveryRule( String sessionIdentifier, int siteGroupId, Wildcard restrictToDomain, int recursionDepth, int scanFrequency, URL[] urls, int scanCountLimit ) throws GeneralizedException, NotFoundException, InsufficientPermissionException, NoSessionException{
		try{
			// 0 -- Precondition check
			
			//	 0.1 -- Check permissions
			SiteGroupDescriptor desc = siteGroupManagement.getGroupDescriptor(siteGroupId);
			checkCreate(sessionIdentifier, desc.getGroupId(), "Add HTTP auto-discovery rule to site group \"" + desc.getGroupName() +"\" (" + desc.getGroupId() + ")");
			
			
			// 1 -- Create the rule
			HttpSeekingScanRule rule = new HttpSeekingScanRule(Application.getApplication(), restrictToDomain, scanFrequency, false);
			rule.setScanCountLimit(scanCountLimit );
			rule.addSeedUrls(urls);
			rule.setRecursionDepth(recursionDepth);
			
			
			return rule.saveNewRuleToDatabase(siteGroupId);
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Update the given HTTP discovery rule.
	 * 
	 * @param sessionIdentifier
	 * @param ruleId
	 * @param restrictToDomain
	 * @param recursionDepth
	 * @param scanFrequency
	 * @param urls
	 * @param scanCountLimit
	 * @return
	 * @throws GeneralizedException
	 * @throws NotFoundException
	 * @throws ScanRuleLoadFailureException
	 * @throws InsufficientPermissionException
	 * @throws NoSessionException
	 */
	public long updateHttpDiscoveryRule( String sessionIdentifier, long ruleId, Wildcard restrictToDomain, int recursionDepth, int scanFrequency, URL[] urls, int scanCountLimit ) throws GeneralizedException, NotFoundException, ScanRuleLoadFailureException, InsufficientPermissionException, NoSessionException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Check permissions
		try{
			int siteGroupId = ScanRule.getAssociatedSiteGroupID(ruleId);
			SiteGroupDescriptor desc = siteGroupManagement.getGroupDescriptor(siteGroupId);
			checkModify(sessionIdentifier, desc.getGroupId(), "Update HTTP/Auto-Discovery rule " + ruleId);
		
		
			// 1 -- Update the rule
			HttpSeekingScanRule rule = (HttpSeekingScanRule)ScanRuleLoader.getScanRule( ruleId );
			rule.setScanCountLimit(scanCountLimit );
			rule.setSeedUrls(urls);
			rule.setRecursionDepth(recursionDepth);
			rule.setScanFrequency(scanFrequency);
			rule.setDomainRestriction(restrictToDomain);
			
			return rule.saveToDatabase();
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Create a new data hash rule corresponding to the given parameters.
	 * @param expectedResponseCode
	 * @param expectedDataHash
	 * @param expectedDataHashAlgorithm
	 * @param followRedirects
	 * @param specimenUrl
	 * @param scanFrequency
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws IllegalStateException
	 * @throws GeneralizedException 
	 * @throws InputValidationException 
	 * @throws NotFoundException 
	 * @throws NoDatabaseConnectionException 
	 * @throws SQLException 
	 * @throws SQLException
	 * @throws NoSessionException 
	 * @throws InsufficientPermissionException 
	 */
	public long addHttpDataHashRule( String sessionIdentifier, int siteGroupId, int expectedResponseCode, String expectedDataHash, String expectedDataHashAlgorithm, boolean followRedirects, URL specimenUrl, int scanFrequency ) throws NoSuchAlgorithmException, GeneralizedException, InputValidationException, NotFoundException, InsufficientPermissionException, NoSessionException{
		
		try {
			// 0 -- Precondition check
			//	 0.1 -- Check permissions
			SiteGroupDescriptor desc = siteGroupManagement.getGroupDescriptor(siteGroupId);
			checkCreate(sessionIdentifier, desc.getGroupId(), "Add static HTTP rule to site group \"" + desc.getGroupName() +"\" (" + desc.getGroupId() + ")");


			// 1 -- Create the rule
			HttpStaticScanRule httpScan = new HttpStaticScanRule( Application.getApplication(), expectedResponseCode, expectedDataHash, expectedDataHashAlgorithm, followRedirects, specimenUrl, scanFrequency );

			return httpScan.saveNewRuleToDatabase( siteGroupId );
		} catch (IllegalStateException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Create a new data hash rule corresponding to the given parameters.
	 * @param expectedResponseCode
	 * @param expectedDataHash
	 * @param expectedDataHashAlgorithm
	 * @param followRedirects
	 * @param specimenUrl
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws IllegalStateException
	 * @throws GeneralizedException 
	 * @throws ScanRuleLoadFailureException 
	 * @throws NotFoundException 
	 * @throws NoSessionException 
	 * @throws InsufficientPermissionException 
	 * @throws SQLException
	 */
	public void updateHttpDataHashRule( String sessionIdentifier, long ruleId, int expectedResponseCode, String expectedDataHash, String expectedDataHashAlgorithm, boolean followRedirects, URL specimenUrl, int scanFrequency ) throws NoSuchAlgorithmException, GeneralizedException, NotFoundException, ScanRuleLoadFailureException, InsufficientPermissionException, NoSessionException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Check permissions
		try{
			int siteGroupId = ScanRule.getAssociatedSiteGroupID(ruleId);
			SiteGroupDescriptor desc = siteGroupManagement.getGroupDescriptor(siteGroupId);
			checkModify(sessionIdentifier, desc.getGroupId(), "Update HTTP static rule " + ruleId);
		
		
			// 1 -- Update the rule
			HttpStaticScanRule httpScan = (HttpStaticScanRule)ScanRuleLoader.getScanRule( ruleId );//new HttpScan( Application.getApplication(), expectedResponseCode, expectedDataHash, expectedDataHashAlgorithm, followRedirects, specimenUrl );
			httpScan.setExpectedResponseCode( expectedResponseCode );
			httpScan.setExpectedDataHash( expectedDataHashAlgorithm, expectedDataHash );
			httpScan.setFollowRedirects( followRedirects );
			httpScan.setUrl( specimenUrl );
			httpScan.setScanFrequency(scanFrequency);
			
			httpScan.saveToDatabase( ruleId );
		} catch (IllegalStateException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Add a HTTP header rule to the given HTTP rule.
	 * @param sessionIdentifier
	 * @param ruleId
	 * @param headerName
	 * @param headerNameType
	 * @param headerValue
	 * @param headerValueType
	 * @param matchAction
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InputValidationException 
	 * @throws NotFoundException 
	 * @throws InsufficientPermissionException 
	 */
	public boolean addHttpHeaderRule( String sessionIdentifier, long ruleId, String headerName, int headerNameType, String headerValue, int headerValueType, int matchAction  ) throws NoSessionException, GeneralizedException, InputValidationException, NotFoundException, InsufficientPermissionException{

		boolean ruleAddSuccess = false;

		try{

			// 0 -- Precondition check

			//	 0.1 -- Check permissions
			int siteGroupId = ScanRule.getAssociatedSiteGroupID(ruleId);
			SiteGroupDescriptor desc = siteGroupManagement.getGroupDescriptor(siteGroupId);
			checkModify(sessionIdentifier, desc.getGroupId(), "Add HTTP header rule to rule " + ruleId);


			// 1 -- Get the rule and make sure the type is compatible
			HttpStaticScanRule scan;
			try {
				scan = (HttpStaticScanRule)ScanRuleLoader.getScanRule( ruleId );
			} catch (Exception e) {
				throw new GeneralizedException();
			}

			if( scan == null )
				return false;

			// 2 -- Add the header
			if( headerNameType == HttpHeaderScanResult.RULE_TYPE_REGEX && headerValueType == HttpHeaderScanResult.RULE_TYPE_REGEX ){
				Pattern finalHeaderName;
				Pattern finalHeaderValue;

				try{
					finalHeaderName = Pattern.compile( headerName );
				}
				catch( PatternSyntaxException e ){
					throw new InputValidationException( "The header name is not a valid regular expression", "HeaderName", headerName );
				}

				try{
					finalHeaderValue = Pattern.compile( headerValue );
				}
				catch( PatternSyntaxException e ){
					throw new InputValidationException( "The header value is not a valid regular expression", "HeaderValue", headerValue );
				}
				ruleAddSuccess = scan.addHeaderRule( new HttpHeaderRule( finalHeaderName, finalHeaderValue, matchAction) );
			}
			else if( headerNameType == HttpHeaderScanResult.RULE_TYPE_REGEX && headerValueType == HttpHeaderScanResult.RULE_TYPE_STRING ){
				Pattern finalHeaderName;
				try{
					finalHeaderName = Pattern.compile( headerName );
				}
				catch( PatternSyntaxException e ){
					throw new InputValidationException( "The header name is not a valid regular expression", "HeaderName", headerName );
				}
				ruleAddSuccess = scan.addHeaderRule( new HttpHeaderRule( finalHeaderName, headerValue, matchAction) );
			}
			else if( headerNameType == HttpHeaderScanResult.RULE_TYPE_STRING && headerValueType == HttpHeaderScanResult.RULE_TYPE_REGEX ){
				Pattern finalHeaderValue;
				try{
					finalHeaderValue = Pattern.compile( headerValue );
				}
				catch( PatternSyntaxException e ){
					throw new InputValidationException( "The header value is not a valid regular expression", "HeaderValue", headerValue );
				}
				ruleAddSuccess = scan.addHeaderRule( new HttpHeaderRule( headerName, finalHeaderValue, matchAction) );
			}
			else {//( headerNameType == HttpHeaderScanResult.RULE_TYPE_REGEX && headerValueType == HttpHeaderScanResult.RULE_TYPE_REGEX ){
				ruleAddSuccess = scan.addHeaderRule( new HttpHeaderRule( headerName, headerValue, matchAction) );
			}

			// 3 -- Save the new rule
			if( ruleAddSuccess )
				try {
					scan.saveToDatabase();
				} catch (IllegalStateException e) {
					appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e);
					throw new GeneralizedException();
				} catch (SQLException e) {
					appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e);
					throw new GeneralizedException();
				} catch (NoDatabaseConnectionException e) {
					appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
					throw new GeneralizedException();
				}
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
		
		// 4 -- Return result
		return ruleAddSuccess;
	}
	
	public boolean deleteHttpHeaderRule( String sessionIdentifier, long headerRuleId ) throws GeneralizedException, NoSessionException, NotFoundException, ScanRuleLoadFailureException, InsufficientPermissionException{

		// 0 -- Precondition checks
		//Permission will be checked below
		
		// 1 -- Get the rule and make sure the type is compatible
		long ruleId;
		try {
			ruleId = HttpStaticScanRule.resolveRuleId( headerRuleId );
	
			// Check permissions
			int siteGroupId = ScanRule.getAssociatedSiteGroupID(ruleId);
			SiteGroupDescriptor desc = siteGroupManagement.getGroupDescriptor(siteGroupId);
			checkModify(sessionIdentifier, desc.getGroupId(), "Delete HTTP header rule for rule " + ruleId );
			
			//1.1 -- Load the scan class with the given header rule
			HttpStaticScanRule scan;
			scan = (HttpStaticScanRule)ScanRuleLoader.getScanRule( ruleId );
			
			boolean removeSuccess = scan.removeHeaderRule( headerRuleId );
			scan.saveToDatabase();
			//HttpScan.deleteHeaderRule( headerRuleId );
			return removeSuccess;
			
		} catch (SQLException e1) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e1);
			throw new GeneralizedException();
		} catch (IllegalStateException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
		
	}
	
	/**
	 * Update the header rule to match the values given. 
	 * @param sessionIdentifier
	 * @param headerRuleId
	 * @param headerName
	 * @param headerNameType
	 * @param headerValue
	 * @param headerValueType
	 * @param matchAction
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws InputValidationException
	 * @throws NotFoundException 
	 * @throws InsufficientPermissionException 
	 */
	public boolean updateHttpHeaderRule( String sessionIdentifier, long headerRuleId, String headerName, int headerNameType, String headerValue, int headerValueType, int matchAction  ) throws NoSessionException, GeneralizedException, InputValidationException, NotFoundException, InsufficientPermissionException{

		// 0 -- Precondition checks
		//Permissions will checked below
		
		
		// 1 -- Get the rule and make sure the type is compatible
		long ruleId;
		try {
			ruleId = HttpStaticScanRule.resolveRuleId( headerRuleId );
			
			//Check permissions
			int siteGroupId = ScanRule.getAssociatedSiteGroupID(ruleId);
			SiteGroupDescriptor desc = siteGroupManagement.getGroupDescriptor(siteGroupId);
			checkModify(sessionIdentifier, desc.getGroupId(), "Update HTTP header rule for rule " + ruleId );
			
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		
		HttpStaticScanRule scan;
		try {
			scan = (HttpStaticScanRule)ScanRuleLoader.getScanRule( ruleId );
		} catch (Exception e) {
			throw new GeneralizedException();
		}
		
		if( scan == null )
			return false;
		
		// 2 -- Add the header
		boolean ruleUpdateSuccess = false;
		if( headerNameType == HttpHeaderScanResult.RULE_TYPE_REGEX && headerValueType == HttpHeaderScanResult.RULE_TYPE_REGEX ){
			Pattern finalHeaderName;
			Pattern finalHeaderValue;
			
			try{
				finalHeaderName = Pattern.compile( headerName );
			}
			catch( PatternSyntaxException e ){
				throw new InputValidationException( "The header name is not a valid regular expression", "HeaderName", headerName );
			}
			
			try{
				finalHeaderValue = Pattern.compile( headerValue );
			}
			catch( PatternSyntaxException e ){
				throw new InputValidationException( "The header value is not a valid regular expression", "HeaderValue", headerValue );
			}
			
			ruleUpdateSuccess = scan.updateHeaderRule( finalHeaderName, finalHeaderValue, matchAction, headerRuleId );
		}
		else if( headerNameType == HttpHeaderScanResult.RULE_TYPE_REGEX && headerValueType == HttpHeaderScanResult.RULE_TYPE_STRING ){
			Pattern finalHeaderName;
			try{
				finalHeaderName = Pattern.compile( headerName );
			}
			catch( PatternSyntaxException e ){
				throw new InputValidationException( "The header name is not a valid regular expression", "HeaderName", headerName );
			}
			ruleUpdateSuccess = scan.updateHeaderRule( finalHeaderName, headerValue, matchAction, headerRuleId );
		}
		else if( headerNameType == HttpHeaderScanResult.RULE_TYPE_STRING && headerValueType == HttpHeaderScanResult.RULE_TYPE_REGEX ){
			Pattern finalHeaderValue;
			try{
				finalHeaderValue = Pattern.compile( headerValue );
			}
			catch( PatternSyntaxException e ){
				throw new InputValidationException( "The header value is not a valid regular expression", "HeaderValue", headerValue );
			}
			ruleUpdateSuccess = scan.updateHeaderRule( headerName, finalHeaderValue, matchAction, headerRuleId );
		}
		else { //( headerNameType == HttpHeaderScanResult.RULE_TYPE_REGEX && headerValueType == HttpHeaderScanResult.RULE_TYPE_REGEX ){
			ruleUpdateSuccess = scan.updateHeaderRule( headerName, headerValue, matchAction, headerRuleId );
		}
		
		// 3 -- Save the new rule
		if( ruleUpdateSuccess )
			try {
				scan.saveToDatabase();
			} catch (IllegalStateException e) {
				appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e);
				throw new GeneralizedException();
			} catch (SQLException e) {
				appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e);
				throw new GeneralizedException();
			} catch (NoDatabaseConnectionException e) {
				appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
				throw new GeneralizedException();
			}
		
		// 4 -- Return result
		return ruleUpdateSuccess;
	}
	
	public boolean baselineRule( long ruleID ) throws RuleBaselineException{
		
		// 0 -- Precondition check
		//TODO check permissions to baseline the given rule
		
		// 1 -- Load and baseline the rule
		try{
			ScanRule rule = ScanRuleLoader.getScanRule( ruleID );
			if( rule instanceof HttpSeekingScanRule ){
				HttpSeekingScanRule httpRule = (HttpSeekingScanRule) rule;
				return httpRule.baseline();
			}
			
			return false;
		}catch(SQLException e){
			throw new RuleBaselineException("SQL exception occurred while baselining rule", e);
		} catch (NotFoundException e) {
			throw new RuleBaselineException("Rule was not found", e);
		} catch (NoDatabaseConnectionException e) {
			throw new RuleBaselineException("A database connection could not be established", e);
		} catch (ScanRuleLoadFailureException e) {
			throw new RuleBaselineException("Scan result could not be loaded while baselining rule", e);
		} catch (DefinitionSetLoadException e) {
			throw new RuleBaselineException("Definition set could not be loaded while baselining rule", e);
		} catch (InputValidationException e) {
			throw new RuleBaselineException("Input validation failed occurred while baselining rule", e);
		} catch (ScriptException e) {
			throw new RuleBaselineException("ScriptException occurred while baselining rule", e);
		} catch (IOException e) {
			throw new RuleBaselineException("IOException occurred while baselining rule", e);
		}
	}
	
	/**
	 * Delete the rule with the given identifier.
	 * @param sessionIdentifier
	 * @param ruleId
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 * @throws InsufficientPermissionException 
	 * @throws NotFoundException 
	 */
	public void deleteRule(String sessionIdentifier, long ruleId) throws GeneralizedException, InsufficientPermissionException, NoSessionException, NotFoundException{
		try {
			
			//Check permissions
			int siteGroupId = ScanRule.getAssociatedSiteGroupID(ruleId);
			SiteGroupDescriptor desc = siteGroupManagement.getGroupDescriptor(siteGroupId);
			checkModify(sessionIdentifier, desc.getGroupId(), "Delete rule " + ruleId );
			
			ScanRule rule = ScanRuleLoader.getScanRule(ruleId);
			rule.delete();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		} catch (ScanRuleLoadFailureException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
		
	}
	
	/**
	 * Resolve the rule identifier that is associated with the given header rule identifier.
	 * @param sessionIdentifier
	 * @param headerRuleId
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 * @throws NotFoundException 
	 */
	public long resolveHttpHeaderRuleId( String sessionIdentifier, long headerRuleId ) throws NoSessionException, GeneralizedException, NotFoundException{
		
		checkSession( sessionIdentifier );
		
		long ruleId;
		
		try{
			ruleId = HttpStaticScanRule.resolveRuleId( headerRuleId );
		}
		catch( SQLException e ){
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		
		return ruleId;
	}
	
	/**
	 * Returns the last signatures that successfully matched
	 * @param sessionIdentifier
	 * @param count
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 */
	public DefinitionMatch[] getLastSignaturesMatched( String sessionIdentifier, int count ) throws NoSessionException, GeneralizedException{
		checkSession( sessionIdentifier );
		
		try{
			return scanData.getLastSignaturesMatched(count);
		}
		catch( SQLException e ){
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
	}
}
