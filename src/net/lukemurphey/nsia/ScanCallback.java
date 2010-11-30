package net.lukemurphey.nsia;

import java.sql.SQLException;

import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.eventlog.EventLogMessage.EventType;
import net.lukemurphey.nsia.scan.*;

public class ScanCallback {
	
	private ApplicationStateMonitor monitor = null;
	private Application application = null;
	
	public ScanCallback(Application application){
		
		//0 -- Precondition check
		
		//	 0.1 -- Make sure the monitor is not null
		if( application == null ){
			throw new IllegalArgumentException("The application cannot be null");
		}
		
		//	 0.2 -- Make sure the monitor is not null
		monitor = application.getApplicationStateMonitor();
		
		if( monitor == null ){
			throw new IllegalArgumentException("The application state monitor cannot be null");
		}
		
		
		// 1 -- Save the reference to the metrics monitor and application object (note that the monitor was already saved previously)
		this.application = application;
	}
	
	/**
	 * Log the scan result (with= the optional message).
	 * @param resultCode
	 * @param deviations
	 * @param ruleType
	 * @param specimen
	 * @param scanRuleID
	 */
	public void logScanResult( ScanResultCode resultCode, int deviations, String ruleType, String specimen, long scanRuleID ){
		logScanResult(resultCode, deviations, ruleType, specimen, null, scanRuleID, true);
	}
	
	/**
	 * Log the scan result (without the optional message).
	 * @param resultCode
	 * @param deviations
	 * @param ruleType
	 * @param specimen
	 * @param scanRuleID
	 */
	public void logScanResult( ScanResultCode resultCode, int deviations, String ruleType, String specimen, String message, long scanRuleID ){
		logScanResult(resultCode, deviations, ruleType, specimen, message, scanRuleID, true);
	}
	
	/**
	 * Log the scan result (without the optional message).
	 * @param resultCode
	 * @param deviations
	 * @param ruleType
	 * @param specimen
	 * @param scanRuleID
	 */
	public void logScanResult( ScanResultCode resultCode, int deviations, String ruleType, String specimen, long scanRuleID, boolean sendToEventLog ){
		logScanResult(resultCode, deviations, ruleType, specimen, null, scanRuleID, sendToEventLog);
	}
	
	public void logScanResult( ScanResultCode resultCode, int deviations, String ruleType, String specimen, String message, long scanRuleID, boolean sendToEventLog, boolean noteScanCompleted ){
		logScanResult(resultCode, deviations, ruleType, specimen, message, scanRuleID, sendToEventLog, noteScanCompleted, false);
	}
	
	/**
	 * Logs the fact that the scan completed and increases the scan counts to show that a rule completed.
	 * @param resultCode
	 * @param deviations
	 * @param ruleType
	 * @param specimen
	 * @param message
	 * @param scanRuleID
	 * @param sendToEventLog
	 * @param noteScanCompleted
	 * @param ruleCompleted
	 */
	public void logScanResult( ScanResultCode resultCode, int deviations, String ruleType, String specimen, String message, long scanRuleID, boolean sendToEventLog, boolean noteScanCompleted, boolean ruleCompleted ){
		if( sendToEventLog ){
			EventLogMessage logMessage;
			
			// Create the base log message
			if( deviations > 0 ){
				if( ruleCompleted ){
					logMessage = new EventLogMessage(EventType.RULE_COMPLETE_REJECTED);
				}
				else{
					logMessage = new EventLogMessage(EventType.RULE_REJECTED);
				}
			}
			else if( resultCode == ScanResultCode.SCAN_FAILED || resultCode == ScanResultCode.UNREADY ){
				if( ruleCompleted ){
					logMessage = new EventLogMessage(EventType.RULE_COMPLETE_FAILED);
				}
				else{
					logMessage = new EventLogMessage(EventType.RULE_FAILED);
				}
			}
			else{
				if( ruleCompleted ){
					logMessage = new EventLogMessage(EventType.RULE_COMPLETE_ACCEPTED);
				}
				else{
					logMessage = new EventLogMessage(EventType.RULE_ACCEPTED);
				}
			}
			
			// Set the message
			if( message != null ){
				logMessage.addField(new EventLogField(FieldName.MESSAGE, message));
			}
			
			// Set the specimen
			if( specimen != null ){
				logMessage.addField(new EventLogField(FieldName.RULE_SPECIMEN, specimen));
			}
			
			// Get the ID of the site group associated with the rule and add it to the list of details 
			long siteGroupID = -1;
			
			try {
				siteGroupID = ScanRule.getSiteGroupForRule(scanRuleID);
				
				if( siteGroupID >= 0 ){
					logMessage.addField(new EventLogField(FieldName.SITE_GROUP_ID, siteGroupID));
				}
			} catch (SQLException e) {
				application.logExceptionEvent(new EventLogMessage(EventLogMessage.EventType.SQL_EXCEPTION), e);
			} catch (NoDatabaseConnectionException e) {
				application.logExceptionEvent(new EventLogMessage(EventLogMessage.EventType.SQL_EXCEPTION), e);
			}
			
			// Add various related fields
			logMessage.addField(new EventLogField(FieldName.RULE_TYPE, ruleType));
			logMessage.addField(new EventLogField(FieldName.RULE_ID, scanRuleID));
			logMessage.addField(new EventLogField(FieldName.DEVIATIONS, deviations));
			
			application.logEvent(logMessage);
		}
		
		// Send a message to the monitor noting that the scan was completed
		if(noteScanCompleted){
			if( deviations > 0 ){
				monitor.incrementFailedRulesCount();
			}
			else if( resultCode == ScanResultCode.SCAN_FAILED || resultCode == ScanResultCode.UNREADY ){
				monitor.incrementIncompleteRulesCount();
			}
			else{
				monitor.incrementPassedRulesCount();
			}
		}
	}
	
	/**
	 * Log the scan result.
	 * @param resultCode
	 * @param deviations
	 * @param ruleType
	 * @param specimen
	 * @param message
	 * @param scanRuleID
	 */
	public void logScanResult( ScanResultCode resultCode, int deviations, String ruleType, String specimen, String message, long scanRuleID, boolean sendToEventLog ){
		logScanResult( resultCode, deviations, ruleType, specimen, message, scanRuleID, sendToEventLog, true );
	}

}
