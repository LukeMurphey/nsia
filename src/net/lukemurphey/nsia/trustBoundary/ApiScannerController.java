package net.lukemurphey.nsia.trustBoundary;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

import java.net.*;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.DuplicateEntryException;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.RuleScanWorker;
import net.lukemurphey.nsia.ScanCallback;
import net.lukemurphey.nsia.ScannerController;
import net.lukemurphey.nsia.SiteGroupManagement;
import net.lukemurphey.nsia.Wildcard;
import net.lukemurphey.nsia.SiteGroupManagement.SiteGroupDescriptor;
import net.lukemurphey.nsia.UserManagement.UserDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.scan.DefinitionErrorList;
import net.lukemurphey.nsia.scan.HttpSeekingScanRule;
import net.lukemurphey.nsia.scan.HttpStaticScanResult;
import net.lukemurphey.nsia.scan.HttpStaticScanRule;
import net.lukemurphey.nsia.scan.ScanException;
import net.lukemurphey.nsia.scan.ScanResult;
import net.lukemurphey.nsia.scan.ScanRule;
import net.lukemurphey.nsia.scan.ScanRuleLoader;

public class ApiScannerController extends ApiHandler{
	
	public ApiScannerController(Application appRes) {
		super(appRes);
	}

	private ScannerController scannerController = appRes.getScannerController();
	
	/**
	 * Scan the rule using 
	 * @param sessionIdentifier
	 * @param url
	 * @return
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws NoSessionException 
	 * @throws InputValidationException 
	 * @throws ScanException 
	 */
	public HttpStaticScanResult scanHttpDataHash( String sessionIdentifier, URL url, String hashAlgorithm ) throws GeneralizedException, InsufficientPermissionException, NoSessionException, InputValidationException, ScanException{
		// 0 -- Precondition check
			
		//	 0.1 -- Permission and right check
		checkSession( sessionIdentifier );
		
		//TODO Check object-level permissions
		//UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
			
		// 1 -- Perform the operation
		try {
			HttpStaticScanRule httpScan = new HttpStaticScanRule( appRes, 200, "", hashAlgorithm, false, url, 120 );
			//httpScan.setUrl( url );
		
		
			HttpStaticScanResult result = (HttpStaticScanResult)httpScan.doScan();
			return result;
		} catch (NoSuchAlgorithmException e) {
			throw new GeneralizedException();
		}
	}
	
	public boolean definitionsErrorsNoted( String sessionIdentifier ) throws NoSessionException, GeneralizedException{
		// 0 -- Precondition check
		
		//	 0.1 -- Permission and right check
		checkSession( sessionIdentifier );
		
		try{
			return DefinitionErrorList.errorsNoted(appRes);
		}
		catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}
		catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Get a list of the definitions that have errors.
	 * @param sessionIdentifier
	 * @return
	 * @throws NoSessionException
	 * @throws GeneralizedException
	 */
	public DefinitionErrorList getDefinitionsErrorList( String sessionIdentifier ) throws NoSessionException, GeneralizedException{
		// 0 -- Precondition check
		
		//	 0.1 -- Permission and right check
		checkSession( sessionIdentifier );
		
		try {
			return DefinitionErrorList.load(appRes);
		}
		catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		}
		catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		}
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
	 * @throws DuplicateEntryException 
	 */
	public String scanRules( String sessionIdentifier, long[] rules, boolean archiveResults ) throws GeneralizedException, InsufficientPermissionException, NoSessionException, DuplicateEntryException{
		
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		RuleScanWorker worker = new RuleScanWorker(rules);
		long siteGroupID = -1;
		
		try {
			for (long ruleID : rules) {
				if(siteGroupID == -1){
					siteGroupID = ScanRule.getAssociatedSiteGroup(ruleID);
				}
				
				ScanRule scanRule = ScanRuleLoader.getScanRule(ruleID);
				long ruleObjectId = scanRule.getObjectId();
				checkExecute(sessionIdentifier, ruleObjectId, "Scan rule " + ruleID);
			}
			
			// 1 -- Perform the operation
			appRes.addWorkerToQueue(worker, "Scan by user ID " + userDescriptor.getUserID() + " for SiteGroup ID " + siteGroupID);
			
			Thread thread = new Thread(worker);
			thread.setName("Scanner started by user " + userDescriptor.getUserName());
			thread.start();
			return "Scan by user ID " + userDescriptor.getUserID() + " for SiteGroup ID " + siteGroupID;
			
		}
		catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		}
		catch(DuplicateEntryException e){
			throw e;		
		}
		catch (Exception e) {
			StringBuffer rulesString = new StringBuffer();
			
			for (int c = 0; c < rules.length; c++) {
				if( c == 0 ){
					rulesString.append("[").append(c);
				}
				else{
					rulesString.append(",").append(c);
				}
			}
			
			rulesString.append("]");
			
			appRes.logExceptionEvent( new EventLogMessage(EventLogMessage.Category.OPERATION_FAILED,
					new EventLogField[]{
						new EventLogField( FieldName.OPERATION, "Scan rule" ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( FieldName.RULE_ID, rulesString.toString() )} )
					, e);
			throw new GeneralizedException();
		}
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
	public ScanResult scanRule( String sessionIdentifier, long ruleId, boolean archiveResults ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		try {
			ScanRule scanRule = ScanRuleLoader.getScanRule(ruleId);
			long ruleObjectId = scanRule.getObjectId();
			checkExecute(sessionIdentifier, ruleObjectId, "Scan rule " + ruleId);
			
			// 1 -- Perform the operation
			ScanResult scanResult = scannerController.scanRule( ruleId, archiveResults );
			return scanResult;
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (Exception e) {//TODO Replace this exception with code that will log it (for potential remediation) 
			
			appRes.logExceptionEvent( new EventLogMessage(EventLogMessage.Category.OPERATION_FAILED,
					new EventLogField[]{
						new EventLogField( FieldName.OPERATION, "Scan rule" ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( FieldName.RULE_ID, ruleId )} )
					, e);
			
			throw new GeneralizedException();
		}
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
	public boolean deleteRule( String sessionIdentifier, long ruleId ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		try {
			
			//ScanRule scanRule = ScanRuleLoader.getScanRule(ruleId);
			int siteGroupID = ScanRule.getAssociatedSiteGroup(ruleId);
			
			SiteGroupDescriptor desc = siteGroupManagement.getGroupDescriptor(siteGroupID);
			checkDelete(sessionIdentifier, desc.getObjectId(), "Delete rule " + ruleId + " from site group \"" + desc.getGroupName() + "\" (" + desc.getGroupId() + ")" );
			
			// 1 -- Perform the operation
			return ScanRule.deleteRule( ruleId );
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (Exception e) {
			
			appRes.logExceptionEvent( new EventLogMessage(EventLogMessage.Category.OPERATION_FAILED,
					new EventLogField[]{
						new EventLogField( FieldName.OPERATION, "Delete rule" ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
						new EventLogField( FieldName.RULE_ID, ruleId )} )
					, e);
			
			throw new GeneralizedException();
		}
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
	public ScanResult[] scanSiteGroup( String sessionIdentifier, int siteGroupId, boolean archiveResults ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		
		// 0 -- Precondition check
		try {
			// 0.1 -- Permission and right check
			SiteGroupManagement.SiteGroupDescriptor siteGroupDesc = siteGroupManagement.getGroupDescriptor(siteGroupId);
			
			if( siteGroupDesc == null )
				return null;
			
			checkExecute( sessionIdentifier, siteGroupDesc.getObjectId(), "Scan all rules for site group " + siteGroupId );
			
		// 1 -- Perform the operation
			
			ScanResult[] scanResults = scannerController.scanSiteGroup( siteGroupId, archiveResults );
			return scanResults;
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (Exception e) {
			appRes.logExceptionEvent(EventLogMessage.Category.OPERATION_FAILED, e);//"operation = scan site group, username = " + userDescriptor.getUserName() + ", user ID = " + userDescriptor.getUserId() + ", site group ID = " + siteGroupId );
			throw new GeneralizedException();
		}
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
	public ScanResult[] scanAllRules( String sessionIdentifier, boolean archiveResults ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "SiteGroups.ScanAllRules");
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		
		// 1 -- Perform the operation
		try {
			ScanResult[] scanResults = scannerController.scanAllRules( archiveResults );
			return scanResults;
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (Exception e) {
			
			appRes.logExceptionEvent( new EventLogMessage(EventLogMessage.Category.OPERATION_FAILED,
					new EventLogField[]{
						new EventLogField( FieldName.OPERATION, "Scan all rules" ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() )} )
					, e);
			
			throw new GeneralizedException();
		}
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
	public ScanResult[] scanExpiredRules( String sessionIdentifier, boolean archiveResults ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "SiteGroups.ScanAllRules");
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		
		// 1 -- Perform the operation
		try {
			ScanResult[] scanResults = scannerController.scanExpiredRules( archiveResults );
			
			return scanResults;
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (Exception e) {
			
			appRes.logExceptionEvent( new EventLogMessage(EventLogMessage.Category.OPERATION_FAILED,
					new EventLogField[]{
						new EventLogField( FieldName.OPERATION, "Scan expired rules" ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() )} )
					, e);
			
			throw new GeneralizedException();
		}
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
	public ScanResult[] scanUpdatedRules( String sessionIdentifier, boolean archiveResults ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "SiteGroups.ScanAllRules");
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		
		// 1 -- Perform the operation
		try {
			ScanResult[] scanResults = scannerController.scanUpdatedRules( archiveResults );
			
			return scanResults;
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (Exception e) {
			
			appRes.logExceptionEvent( new EventLogMessage(EventLogMessage.Category.OPERATION_FAILED,
					new EventLogField[]{
						new EventLogField( FieldName.OPERATION, "Scan updated rules" ),
						new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() )} )
					, e);
			
			throw new GeneralizedException();
		}
	}
	
	/**
	 * Disable active scanning. The scanner controller will not invoke scanning of rules unless specifically requested.
	 * @param sessionIdentifier
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws NoSessionException 
	 */
	public void disableScanning( String sessionIdentifier ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "System.ControlScanner");
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		appRes.logEvent( EventLogMessage.Category.SCANNER_STOPPED,
				new EventLogField( FieldName.SOURCE_USER_NAME,  userDescriptor.getUserName()),
				new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ) );
		
		// 1 -- Perform the operation
		scannerController.disableScanning( );
	}
	
	/**
	 * Enable active scanning. The scanner controller will invoke scanning of rules according to the configuration.
	 * @param sessionIdentifier
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws NoSessionException 
	 */
	public void enableScanning( String sessionIdentifier ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "System.ControlScanner");
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		
		appRes.logEvent( EventLogMessage.Category.SCANNER_STARTED,
				new EventLogField( FieldName.SOURCE_USER_NAME,  userDescriptor.getUserName()),
				new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ) );
		
		// 1 -- Perform the operation
		scannerController.enableScanning( );
	}
	
	/**
	 * Retrieve the maximum number of threads allowed to performed scanning.
	 * @param sessionIdentifier
	 * @return
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws NoSessionException 
	 */
	public int getMaxScanThreads( String sessionIdentifier ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		// 1 -- Perform the operation
		return scannerController.getMaxScanThreads( );
	}
	
	/*public void startScanner( String sessionIdentifier ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "Administration.StartScanner");
		
		// 1 -- Perform the operation
		scannerController.enableScanning();
		//scannerController.setMaxScanThreads( 5 );
		//scannerController.start();
	}
	
	public void stopScanner( String sessionIdentifier ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		checkExecute( sessionIdentifier, "Administration.ShutdownScanner");
		
		// 1 -- Perform the operation
		scannerController.disableScanning();
	}*/
	
	/**
	 * Get the polling loop frequency.
	 * @param sessionIdentifier
	 * @return
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws NoSessionException 
	 */
	public long getPollingLoopFrequency( String sessionIdentifier ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		// 1 -- Perform the operation
		return scannerController.getPollingLoopFrequency( );
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
		
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		
		// 1 -- Perform the operation
		scannerController.setMaxScanThreads( threadCount );

		appRes.logEvent( new EventLogMessage(EventLogMessage.Category.CONFIGURATION_CHANGE,
							new EventLogField[]{
								new EventLogField( FieldName.PARAMETER, "Maximum thread count" ),
								new EventLogField( FieldName.VALUE, threadCount ),
								new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
								new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() )} )
							);
		
	}
	
	/**
	 * Gets the state of the scanner.
	 * @param sessionIdentifier
	 * @return
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws NoSessionException 
	 */
	public ScannerController.ScannerState getScanningState( String sessionIdentifier ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		//checkModify( sessionIdentifier, "System.ControlScanner");
		checkSession(sessionIdentifier);
		//UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		
		
		// 1 -- Perform the operation
		return scannerController.getScanningState();
		//appRes.logEvent(EventLogMessage.Category.CONFIGURATION_CHANGE, "configuration = polling frequency, new value = " + pollingFrequency + ", username = " + userDescriptor.getUserName() + ", user ID = " + userDescriptor.getUserId());
	}
	
	/**
	 * Set the polling loop frequency.
	 * @param sessionIdentifier
	 * @return
	 * @throws GeneralizedException
	 * @throws InsufficientPermissionException
	 * @throws NoSessionException 
	 */
	public void setPollingLoopFrequency( String sessionIdentifier, long pollingFrequency ) throws GeneralizedException, InsufficientPermissionException, NoSessionException{
		
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		
		
		// 1 -- Perform the operation
		scannerController.setPollingLoopFrequency( pollingFrequency );
		
		appRes.logEvent( new EventLogMessage(EventLogMessage.Category.CONFIGURATION_CHANGE,
							new EventLogField[]{
								new EventLogField( FieldName.PARAMETER, "Polling frequency" ),
								new EventLogField( FieldName.VALUE, pollingFrequency ),
								new EventLogField( FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
								new EventLogField( FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() )} )
							);
		
	}
	
	/**
	 * Starts a site scan with the given parameters. This method causes a worker thread to be added to the application's background thread queue.
	 * @param sessionIdentifier
	 * @param seedUrls
	 * @param domainLimit
	 * @param scanLimit
	 * @param recursionDepth
	 * @param includeFirstLevelLinks
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws DuplicateEntryException 
	 */
	public RuleScanWorker doSiteScan( String sessionIdentifier, URL[] seedUrls, Wildcard domainLimit, int scanLimit, int recursionDepth, boolean includeFirstLevelLinks) throws InsufficientPermissionException, GeneralizedException, NoSessionException, DuplicateEntryException{
		
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "System.PerformScan");
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		
		
		// 1 -- Start the scanner thread
		HttpSeekingScanRule scanRule = new HttpSeekingScanRule(appRes, domainLimit, includeFirstLevelLinks);
		scanRule.setRecursionDepth(recursionDepth);
		scanRule.setScanCountLimit(scanLimit);
		scanRule.addSeedUrls(seedUrls);
		
		ScanCallback callback = new ScanCallback(appRes);
		scanRule.setCallback(callback);
		
		RuleScanWorker worker = new RuleScanWorker(scanRule);
		appRes.addWorkerToQueue(worker, "SiteScan/" + userDescriptor.getUserID(), userDescriptor.getUserID());
		
		Thread thread = new Thread(worker);
		thread.setPriority( 3 );
		thread.start();
		/*
		appRes.addWorkerToQueue(scanRule, "SiteScan/" + userDescriptor.getUserID(), userDescriptor.getUserID());
		
		Thread thread = new Thread(scanRule);
		thread.start();
		*/
		
		return worker;
	}
	
}
