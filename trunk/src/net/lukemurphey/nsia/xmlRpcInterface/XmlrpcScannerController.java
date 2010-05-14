package net.lukemurphey.nsia.xmlRpcInterface;

import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.*;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.scan.HttpStaticScanRule;
import net.lukemurphey.nsia.scan.ScanResult;
import net.lukemurphey.nsia.trustBoundary.ApiScannerController;

public class XmlrpcScannerController extends XmlrpcHandler{
	
	private ApiScannerController untrustScanner;
	
	public XmlrpcScannerController(Application appRes) {
		super(appRes);
		
		untrustScanner = new ApiScannerController( appRes );
	}
	
	/**
	 * Perform a scan of the given rule.
	 * @param sessionIdentifier
	 * @param ruleId
	 * @param archiveResults
	 * @return
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws NoSessionException 
	 */
	public Hashtable<String, Object> scanRule( String sessionIdentifier, int ruleId, boolean archiveResults ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		
		ScanResult scanResult = untrustScanner.scanRule( sessionIdentifier, ruleId, archiveResults );
		return scanResult.toHashtable();
	}
	
	/**
	 * Delete the rule associated with the given identifier.
	 * @param sessionIdentifier
	 * @param ruleId
	 * @return
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws NoSessionException 
	 */
	public boolean deleteRule( String sessionIdentifier, int ruleId ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		return untrustScanner.deleteRule( sessionIdentifier, ruleId );
	}
	
	/**
	 * Perform a scan of the given site group.
	 * @param sessionIdentifier
	 * @param siteGroupId
	 * @param archiveResults
	 * @return
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws NoSessionException 
	 */
	public Vector<Hashtable<String, Object>> scanSiteGroup( String sessionIdentifier, int siteGroupId, boolean archiveResults ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		
		ScanResult[] scanResults = untrustScanner.scanSiteGroup( sessionIdentifier, siteGroupId, archiveResults );
		Vector<Hashtable<String, Object>> scanResultsVector = new Vector<Hashtable<String, Object>>();
		
		for( int c = 0; c < scanResults.length; c++){
			scanResultsVector.add( scanResults[c].toHashtable() );
		}
		
		return scanResultsVector;
	}
	
	/**
	 * Scan all rules. 
	 * @param sessionIdentifier
	 * @param archiveResults
	 * @return
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws NoSessionException 
	 */
	public Vector<Hashtable<String, Object>> scanAllRules( String sessionIdentifier, boolean archiveResults ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		
		ScanResult[] scanResults = untrustScanner.scanAllRules( sessionIdentifier, archiveResults );
		Vector<Hashtable<String, Object>> scanResultsVector = new Vector<Hashtable<String, Object>>();
		
		for( int c = 0; c < scanResults.length; c++){
			scanResultsVector.add( scanResults[c].toHashtable() );
		}
		
		return scanResultsVector;
	}
	
	/**
	 * Scan all rules that are expired.
	 * @param sessionIdentifier
	 * @param archiveResults
	 * @return
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws NoSessionException 
	 */
	public Vector<Hashtable<String, Object>> scanExpiredRules( String sessionIdentifier, boolean archiveResults ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		
		ScanResult[] scanResults = untrustScanner.scanExpiredRules( sessionIdentifier, archiveResults );
		Vector<Hashtable<String, Object>> scanResultsVector = new Vector<Hashtable<String, Object>>();
		
		for( int c = 0; c < scanResults.length; c++){
			scanResultsVector.add( scanResults[c].toHashtable() );
		}
		
		return scanResultsVector;
	}
	
	/**
	 * Scan all rules that have been updated (i.e. the rule was updated but have not been re-evaluated).
	 * @param sessionIdentifier
	 * @param archiveResults
	 * @return
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws NoSessionException 
	 */
	public Vector<Hashtable<String, Object>> scanUpdatedRules( String sessionIdentifier, boolean archiveResults ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		
		ScanResult[] scanResults = untrustScanner.scanUpdatedRules( sessionIdentifier, archiveResults );
		Vector<Hashtable<String, Object>> scanResultsVector = new Vector<Hashtable<String, Object>>();
		
		for( int c = 0; c < scanResults.length; c++){
			scanResultsVector.add( scanResults[c].toHashtable() );
		}
		
		return scanResultsVector;
	}
	
	/**
	 * Disable active scanning. The scanner controller will not invoke scanning of rules unless specifically requested.
	 * @param sessionIdentifier
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws NoSessionException 
	 */
	public void disableScanning( String sessionIdentifier ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		untrustScanner.disableScanning( sessionIdentifier );
	}
	
	/**
	 * Enable active scanning. The scanner controller will invoke scanning of rules according to the configuration.
	 * @param sessionIdentifier
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws NoSessionException 
	 */
	public void enableScanning( String sessionIdentifier ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		untrustScanner.enableScanning( sessionIdentifier );
	}
	
	/**
	 * Retrieve the maximum number of threads allowed to performed scanning.
	 * @param sessionIdentifier
	 * @return
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws NoSessionException 
	 */
	public Integer getMaxScanThreads( String sessionIdentifier ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		return Integer.valueOf( untrustScanner.getMaxScanThreads( sessionIdentifier ) );
	}
	
	/**
	 * Get the polling loop frequency.
	 * @param sessionIdentifier
	 * @return
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws NoSessionException 
	 */
	public Double getPollingLoopFrequency( String sessionIdentifier ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		return new Double( untrustScanner.getPollingLoopFrequency( sessionIdentifier ) );
	}
	
	/**
	 * Set the maximum number of threads allowed to performed scanning.
	 * @param sessionIdentifier
	 * @return
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws NoSessionException 
	 */
	public void setMaxScanThreads( String sessionIdentifier, int threadCount ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		untrustScanner.setMaxScanThreads( sessionIdentifier, threadCount );
	}
	
	/**
	 * Set the polling loop frequency.
	 * @param sessionIdentifier
	 * @return
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws NoSessionException 
	 */
	public void setPollingLoopFrequency( String sessionIdentifier, int pollingFrequency ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		untrustScanner.setPollingLoopFrequency( sessionIdentifier, pollingFrequency );
	}
	
	/**
	 * Gets the state of the scanner.
	 * @param sessionIdentifier
	 * @return
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws NoSessionException 
	 */
	public int getScanningState( String sessionIdentifier) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		return untrustScanner.getScanningState(sessionIdentifier).ordinal();
	}
	
	/**
	 * Create a rule based on the rule described in the hashtable.
	 * @param hashtable
	 * @return
	 * @throws MalformedURLException
	 * @throws GeneralizedException
	 */
	public long createRule( Hashtable<String, Object> hashtable, int siteGroupId ) throws MalformedURLException, GeneralizedException{
		
		// 0 -- Precondition check
		
		//	0.1 -- Make sure the rule is not null
		if( hashtable == null )
			return -1;
		
		//	0.2 -- Make sure the rule type is valid
		String classType = (String)hashtable.get("Class");
		
		if( classType.matches("HttpScan") ){
			HttpStaticScanRule httpScanRule = HttpStaticScanRule.getFromHashtable( hashtable );
			try {
				return httpScanRule.saveToDatabase();
			} catch (IllegalStateException e) {
				appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e );
				throw new GeneralizedException(e);
			} catch (SQLException e) {
				appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e );
				throw new GeneralizedException(e);
			} catch (NoDatabaseConnectionException e) {
				appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e );
				throw new GeneralizedException(e);
			}
		}
		else{
			return -1; //Class match not found, rule cannot be added
		}
	}
}
