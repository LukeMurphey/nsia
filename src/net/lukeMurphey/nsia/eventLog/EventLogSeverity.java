package net.lukeMurphey.nsia.eventLog;

import org.apache.log4j.*;

public class EventLogSeverity extends Level {

	private static final long serialVersionUID = -4967298534827859874L;
	
	/* The following constants define the various log levels.
	 * The levels are borrowed from syslog and RFC3164.
	 */ 
	public static final EventLogSeverity DEBUG = new EventLogSeverity(10000, "Debug", 7);
	public static final EventLogSeverity INFORMATIONAL = new EventLogSeverity(20000, "Informational", 6);
	public static final EventLogSeverity NOTICE = new EventLogSeverity(25000, "Notice", 5); //Normal but significant event
	public static final EventLogSeverity WARNING = new EventLogSeverity(30000, "Warning", 4); //Event that may indicate potential harm to the application
	public static final EventLogSeverity ERROR = new EventLogSeverity(40000, "Error", 3); //Application faults
	public static final EventLogSeverity CRITICAL = new EventLogSeverity(43333, "Critical", 2); //Event likely requires action or investigation
	public static final EventLogSeverity ALERT = new EventLogSeverity(46666, "Alert", 1); //Immediate action necessary
	public static final EventLogSeverity EMERGENCY = new EventLogSeverity(50000, "Emergency", 0); //System is unusable
	
	/**
	 * This is intended to be an enumerated type so the constructor is private.
	 */
	private EventLogSeverity(int logInt, String desc, int syslogSeverity){
		super(logInt, desc, syslogSeverity);
	}
	
	/**
	 * Retrieve the integer value represented by this severity level. See RFC 3164.
	 * @precondition None
	 * @postcondition The severity level is returned
	 * @return severity level
	 */
	public int getSeverity(){
		return this.toInt();// logId;
	}
	
	/**
	 * Get the severity level associated with the given ID.
	 * @precondition The event log severity must be valid (0-7). Otherwise, the default severity will be returned.
	 * @postcondition The severity level associated with the identifier.
	 * @param severityId
	 * @return
	 */
	public static EventLogSeverity getSeverityById( int severityId ){
		if( DEBUG.getSeverity() ==  severityId )
			return DEBUG;
		else if( INFORMATIONAL.getSeverity() ==  severityId )
			return INFORMATIONAL;
		else if( ALERT.getSeverity() ==  severityId )
			return ALERT;
		else if( CRITICAL.getSeverity() ==  severityId )
			return CRITICAL;
		else if( EMERGENCY.getSeverity() ==  severityId )
			return EMERGENCY;
		else if( ERROR.getSeverity() ==  severityId )
			return ERROR;
		else if( NOTICE.getSeverity() ==  severityId )
			return NOTICE;
		else if( WARNING.getSeverity() ==  severityId )
			return WARNING;
		
		// Default (if none of the above match)
		//assert
		return NOTICE;
	}
	
}
