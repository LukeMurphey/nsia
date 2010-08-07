package net.lukemurphey.nsia.eventlog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;

public class EventLogMessage {
	
	/**
	 * The event-type enumeration includes all of the potential log messages that can be created.
	 * @author Luke Murphey
	 *
	 */
	public enum EventType{
		USER_ID_ILLEGAL(EventLogSeverity.WARNING, "Input validation failed: User identifier is illegal"), 
		GROUP_ID_ILLEGAL(EventLogSeverity.WARNING, "Input validation failed: Group identifier is illegal"), 
		SITE_GROUP_ID_ILLEGAL(EventLogSeverity.WARNING, "Input validation failed: Site group identifier is illegal"), 
		GROUP_NAME_LENGTH_INVALID(EventLogSeverity.WARNING, "Input validation failed: Group name length is excessive"), 
		GROUP_NAME_ILLEGAL(EventLogSeverity.WARNING, "Input validation failed: Group name is illegal"), 
		GROUP_NAME_EMPTY(EventLogSeverity.WARNING, "Input validation failed: Group name is empty"), 
		GROUP_NAME_NULL(EventLogSeverity.WARNING, "Input validation failed: Group name is null"), 
		USER_NAME_LENGTH_INVALID(EventLogSeverity.WARNING, "Input validation failed: Username length is excessive"), 
		USER_NAME_ILLEGAL(EventLogSeverity.WARNING, "Input validation failed: Username is illegal"),
		USER_NAME_EMPTY(EventLogSeverity.WARNING, "Input validation failed: Username is empty"), 
		USER_NAME_NULL(EventLogSeverity.WARNING, "Input validation failed: Username is null"), 
		REAL_NAME_LENGTH_INVALID(EventLogSeverity.WARNING, "Input validation failed: Actual user name length is excessive"), 
		REAL_NAME_ILLEGAL(EventLogSeverity.WARNING, "Input validation failed: Actual user name is illegal"), 
		REAL_NAME_EMPTY(EventLogSeverity.WARNING, "Input validation failed: Actual user name is empty"), 
		REAL_NAME_NULL(EventLogSeverity.WARNING, "Input validation failed: Actual user name is null"), 
		HASH_ALGORITHM_EMPTY(EventLogSeverity.WARNING, "Input validation failed: Hash algorithm is empty"), 
		HASH_ALGORITHM_NULL(EventLogSeverity.WARNING, "Input validation failed: Hash algorithm is null"), 
		HASH_ITERATION_COUNT_ILLEGAL(EventLogSeverity.WARNING, "Input validation failed: Hash iteration count is illegal"), 
		EMAIL_LOCAL_PART_INVALID(EventLogSeverity.WARNING, "Input validation failed: Email local part invalid"), 
		EMAIL_UNKNOWN_HOST(EventLogSeverity.WARNING, "Input validation failed: Unknown host exception"), 
		PASSWORD_ILLEGAL(EventLogSeverity.WARNING, "Input validation failed: Password illegal"), 
		PASSWORD_EMPTY(EventLogSeverity.WARNING, "Input validation failed: Password is empty"), 
		PASSWORD_NULL(EventLogSeverity.WARNING, "Input validation failed: Password is null"), 
		PASSWORD_WEAK(EventLogSeverity.NOTICE, "Input validation failed: Password strength insufficient"), 
		SYSTEM_PARAMETER_NAME_ILLEGAL(EventLogSeverity.ERROR, "Input validation failed: System parameter name invalid"), 
		SYSTEM_PARAMETER_VALUE_ILLEGAL(EventLogSeverity.ERROR, "Input validation failed: System parameter value invalid"), 
		APPLICATION_STARTED(EventLogSeverity.INFORMATIONAL, "System: Application started"), 
		APPLICATION_SHUTTING_DOWN(EventLogSeverity.NOTICE, "System: Application terminating"), 
		MEMORY_LOW(EventLogSeverity.CRITICAL, "System: Memory low"), 
		MEMORY_CRITICAL(EventLogSeverity.EMERGENCY, "System: Memory critical"), 
		DATABASE_FAILURE(EventLogSeverity.ALERT, "System: Database connection failed"), 
		THREAD_COUNT_HIGH(EventLogSeverity.CRITICAL, "System: Thread count high"), 
		RESPONSE_TIME_HIGH(EventLogSeverity.CRITICAL, "System: Response time high"), 
		THREAD_COUNT_CRITICAL(EventLogSeverity.ALERT, "System: Thread count critical"), 
		RESPONSE_TIME_CRITICAL(EventLogSeverity.ALERT, "System: Response time critical"), 
		FATAL_EXCEPTION(EventLogSeverity.EMERGENCY, "System: Unrecoverable exception, application terminating"), 
		SQL_EXCEPTION(EventLogSeverity.ERROR, "System: Database exception"), 
		SQL_WARNINGS_CLEARED(EventLogSeverity.NOTICE, "System: SQL warnings cleared"), 
		APPLICATION_RESTRICTED_MODE(EventLogSeverity.NOTICE, "System: Application has entered restricted mode"), 
		APPLICATION_OPEN_MODE(EventLogSeverity.NOTICE, "System: Application has entered open mode"), 
		SSL_CERTIFICATE_EXPIRING_SOON(EventLogSeverity.CRITICAL, "System: SSL certificate is near expiration"), 
		SSL_CERTIFICATE_EXPIRED(EventLogSeverity.ALERT, "System: SSL certificate has expired"), 
		DATABASE_DRIVER_UNAVAILABLE(EventLogSeverity.EMERGENCY, "System: Database driver unavailable"), 
		CONFIGURATION_CHANGE(EventLogSeverity.NOTICE, "System: Configuration change"), 
		SCAN_TIME_GREATER_THAN_CYCLE_TIME(EventLogSeverity.WARNING, "System: Scan failed to complete within cycle time"), 
		ILLEGAL_CONFIG(EventLogSeverity.ERROR, "System: Illegal configuration"), 
		INTERNAL_ERROR(EventLogSeverity.ERROR, "System: Non-fatal internal error"), 
		OPERATION_FAILED(EventLogSeverity.ERROR, "System: Operation failed"), 
		SCANNER_STOPPED(EventLogSeverity.NOTICE, "System: Scanner stopped"), 
		SCANNER_STARTED(EventLogSeverity.NOTICE, "System: Scanner started"), 
		DEFINITIONS_UPDATED(EventLogSeverity.NOTICE, "System: Definitions updated"), 
		SCAN_ENGINE_EXCEPTION(EventLogSeverity.ERROR, "System: Scan engine encountered an exception"),
		DEFINITION_DEBUG_MESSAGE(EventLogSeverity.DEBUG, "Scanner: Definition debug message"), 
		DEFINITIONS_EXPORTED(EventLogSeverity.NOTICE, "System: Definition set exported"), 
		DEFINITIONS_ADDED(EventLogSeverity.NOTICE, "System: Definition added"), 
		DEFINITIONS_DELETED(EventLogSeverity.NOTICE, "System: Definition deleted"), 
		DEFINITIONS_MODIFIED(EventLogSeverity.NOTICE, "System: Definition modified"), 
		DEFINITION_ERROR(EventLogSeverity.ERROR, "System: Definition contains an error"), 
		AUTHENTICATION_SUCCESS(EventLogSeverity.NOTICE, "Authentication: Succeeded"), 
		AUTHENTICATION_FAILED_USERNAME_ILLEGAL(EventLogSeverity.WARNING, "Authentication: Failed, username is illegal"), 
		AUTHENTICATION_FAILED_LENGTH_EXCESSIVE(EventLogSeverity.WARNING, "Authentication: Failed, username length is excessive"), 
		AUTHENTICATION_FAILED_USERNAME_EMPTY(EventLogSeverity.WARNING, "Authentication: Failed, username is empty"), 
		AUTHENTICATION_FAILED_USERNAME_INVALID(EventLogSeverity.WARNING, "Authentication: Failed, username is invalid"), 
		AUTHENTICATION_FAILED_USERNAME_BLOCKED(EventLogSeverity.WARNING, "Authentication: Failed, username is blocked"), 
		AUTHENTICATION_USERNAME_BLOCKED(EventLogSeverity.ALERT, "Authentication: Username blocked due to repeated and failed authentication attempts"), 
		AUTHENTICATION_FAILED_PASSWORD_WRONG(EventLogSeverity.WARNING, "Authentication: Failed, password is incorrect"), 
		AUTHENTICATION_FAILED_PASSWORD_ILLEGAL(EventLogSeverity.WARNING, "Authentication: Failed, password is invalid"), 
		AUTHENTICATION_FAILED_ACCOUNT_DISABLED(EventLogSeverity.WARNING, "Authentication: Failed, user account is disabled"), 
		SESSION_ENDED(EventLogSeverity.INFORMATIONAL, "Session: Session ended"), 
		SESSION_INACTIVITY_EXPIRED(EventLogSeverity.NOTICE, "Session: Session expired due to inactivity"), 
		SESSION_ASSIGNED(EventLogSeverity.INFORMATIONAL, "Session: Session assigned"), 
		SESSION_ADMIN_TERMINATED(EventLogSeverity.CRITICAL, "Session: Session administratively terminated"), 
		SESSION_HIJACKED(EventLogSeverity.ALERT, "Session: Session hijacked attempt detected"), 
		SESSION_MAX_TIME_EXCEEDED(EventLogSeverity.WARNING, "Session: Maximum session period exceeded"), 
		SESSION_ID_EMPTY(EventLogSeverity.WARNING, "Session: Session identifier is empty"), 
		SESSION_ID_ILLEGAL(EventLogSeverity.WARNING, "Session: Session identifier is illegal"), 
		SESSION_ID_INVALID(EventLogSeverity.WARNING, "Session: Session identifier is invalid"), 
		SESSION_INVALID_TERMINATION_ATTEMPT(EventLogSeverity.CRITICAL, "Session: Attempt to terminate invalid session"), 
		FIREWALL_PERMIT(EventLogSeverity.DEBUG, "Firewall: Source address permitted"), 
		FIREWALL_DENY(EventLogSeverity.WARNING, "Firewall: Source address denied"), 
		FIREWALL_TEMP_DENY(EventLogSeverity.CRITICAL, "Firewall: Source address denied due to application defensive rule"), 
		FIREWALL_DENY_RULE_CREATED(EventLogSeverity.ALERT, "Firewall: Application self-defense invoked"), 
		FIREWALL_DEFAULT_DENY(EventLogSeverity.WARNING, "Firewall: Source address denied by default"), 
		FIREWALL_DEFAULT_PERMIT(EventLogSeverity.DEBUG, "Firewall: Source address permitted by default"), 
		USER_ADDED(EventLogSeverity.WARNING, "User Management: User added"), 
		USER_MODIFIED(EventLogSeverity.NOTICE, "User Management: User modified"), 
		USER_DISABLED(EventLogSeverity.NOTICE, "User Management: User disabled"), 
		USER_DELETED(EventLogSeverity.WARNING, "User Management: User deleted"), 
		USER_PASSWORD_CHANGED(EventLogSeverity.WARNING, "User Management: User password changed"), 
		USER_ID_INVALID(EventLogSeverity.WARNING, "User Management: User ID invalid"),
		USER_NAME_INVALID(EventLogSeverity.WARNING, "User Management: User name invalid"), 
		USER_REENABLED(EventLogSeverity.WARNING, "User Management: User re-enabled"),
		USER_NAME_UNLOCKED(EventLogSeverity.NOTICE, "User Management: Username unlocked"), 
		GROUP_ADDED(EventLogSeverity.WARNING, "Group Management: Group added"), 
		GROUP_MODIFIED(EventLogSeverity.NOTICE, "Group Management: Group modified"), 
		GROUP_DISABLED(EventLogSeverity.NOTICE, "Group Management: Group disabled"), 
		GROUP_DELETED(EventLogSeverity.WARNING, "Group Management: Group deleted"), 
		GROUP_ID_INVALID(EventLogSeverity.WARNING, "Group Management: Group ID invalid"),
		GROUP_NAME_INVALID(EventLogSeverity.WARNING, "Group Management: Group name invalid"), 
		GROUP_REENABLED(EventLogSeverity.WARNING, "Group Management: Group re-enabled"), 
		USER_ADDED_TO_GROUP(EventLogSeverity.WARNING, "Group Management: User added to group"), 
		USER_REMOVED_FROM_GROUP(EventLogSeverity.NOTICE, "Group Management: User removed from group"), 
		LOG_SERVER_UNAVAILABLE(EventLogSeverity.ALERT, "Event Log: Log server unavailable"), 
		LOGS_BEING_CACHED(EventLogSeverity.CRITICAL, "Event Log: Logs are being cached"), 
		LOG_CACHE_EMPTY(EventLogSeverity.NOTICE, "Event Log: Log cache is empty"), 
		LOG_CACHE_FULL(EventLogSeverity.ALERT, "Event Log: Log cache is full, logs are being discarded"), 
		LOG_SERVER_AVAILABLE(EventLogSeverity.NOTICE, "Event Log: Log server available"), 
		LOG_FILE_WRITE_FAILED(EventLogSeverity.CRITICAL, "Event Log: Log file write failed"),
		LOG_FAILED(EventLogSeverity.WARNING, "Event Log: Log failure"),
		ACCESS_CONTROL_DENY(EventLogSeverity.WARNING, "Access Control: Denied action"), 
		ACCESS_CONTROL_PERMIT(EventLogSeverity.INFORMATIONAL, "Access Control: Permitted action"), 
		ACCESS_CONTROL_DENY_DEFAULT(EventLogSeverity.WARNING, "Access Control: Denied action by default"), 
		ACCESS_CONTROL_PERMIT_DEFAULT(EventLogSeverity.INFORMATIONAL, "Access Control: Permitted action by default"), 
		ACCESS_CONTROL_ENTRY_SET(EventLogSeverity.WARNING, "Access Control: ACL entry set"), 
		ACCESS_CONTROL_ENTRY_UNSET(EventLogSeverity.WARNING, "Access Control: ACL entry unset"), 
		ACCESS_CONTROL_ENTRY_SET_FAILED(EventLogSeverity.ERROR, "Access Control: ACL entry set failed"), 
		ACCESS_CONTROL_ENTRY_UNSET_FAILED(EventLogSeverity.ERROR, "Access Control: ACL entry unset failed"), 
		SITE_GROUP_REENABLED(EventLogSeverity.WARNING, "Site Group Management: Site group re-enabled"), 
		SITE_GROUP_ADDED(EventLogSeverity.WARNING, "Site Group Management: Site group added"), 
		SITE_GROUP_DISABLED(EventLogSeverity.NOTICE, "Site Group Management: Site group disabled"), 
		SITE_GROUP_DELETED(EventLogSeverity.NOTICE, "Site Group Management: Site group deleted"), 
		SITE_GROUP_MODIFIED(EventLogSeverity.NOTICE, "Site Group Management: Site group modified"), 
		SITE_GROUP_ID_INVALID(EventLogSeverity.WARNING, "Site Group Management: Site group invalid"), 
		WEB_ERROR(EventLogSeverity.ERROR, "Web Service: Web interface error"), 
		WEB_XMLRPC_ERROR(EventLogSeverity.ERROR, "Web Service: XML-RPC error"), 
		WEB_ACCESS_LOG(EventLogSeverity.INFORMATIONAL, "Web Service: Access Log"), 
		WEB_INFO_LOG(EventLogSeverity.DEBUG, "Web Service: Informational message"), 
		RULE_REJECTED(EventLogSeverity.WARNING, "Scanner: Rule rejected"), 
		RULE_ACCEPTED(EventLogSeverity.INFORMATIONAL, "Scanner: Rule accepted"), 
		RULE_FAILED(EventLogSeverity.WARNING, "Scanner: Rule execution failed"),
		TASK_FAILED(EventLogSeverity.ERROR, "System: Background task error"),
		TASK_WARNING(EventLogSeverity.WARNING, "System: Background task warning"),
		TASK_STARTED(EventLogSeverity.INFORMATIONAL, "System: Background task started"),
		TASK_TERMINATED(EventLogSeverity.INFORMATIONAL, "System: Background task terminated"),
		TASK_COMPLETED(EventLogSeverity.INFORMATIONAL, "System: Background task completed"),
		RESPONSE_ACTION_DELETED(EventLogSeverity.NOTICE, "Response Module: New action added"),
		RESPONSE_ACTION_ADDED(EventLogSeverity.NOTICE, "Response Module: Action deleted"),
		RESPONSE_ACTION_TRIGGERED(EventLogSeverity.INFORMATIONAL, "Response Module: Action invoked"),
		RESPONSE_ACTION_MODIFIED(EventLogSeverity.NOTICE, "Response Module: Action modified"),
		LICENSE_VALIDATED(EventLogSeverity.NOTICE, "System: License validated"),
		LICENSE_VALIDATION_FAILURE(EventLogSeverity.NOTICE, "System: License could not be validated"),
		LICENSE_INVALID(EventLogSeverity.NOTICE, "System: No valid license"),
		LOG_CACHE_EMPTYING(EventLogSeverity.NOTICE, "Event Log: Cache being forwarded"),
		DEFINITION_MATCH(EventLogSeverity.WARNING, "Scanner: Definition matched"),
		RESPONSE_ACTION_FAILED(EventLogSeverity.WARNING, "Response Module: Action failed"),
		STARTUP_ERROR(EventLogSeverity.WARNING, "System: Startup error"),
		DEFINITION_UPDATE_REQUEST_FAILED(EventLogSeverity.WARNING, "System: Definition update request failed");

		private EventType(EventLogSeverity severity, String name){
			this.severity = severity;
			this.name = name;
		}
		
		private EventLogSeverity severity;
		private String name;
		
		public EventLogSeverity getSeverity(){
			return severity;
		}
		
		public String getName(){
			return name;
		}
	}
	
	private EventType eventType;
	private Date date;
	private Vector<EventLogField> fields = new Vector<EventLogField>();
	
	public EventLogMessage (EventType eventType ){
		
		// 0 -- Precondition check
		if( eventType == null ){
			throw new IllegalArgumentException("The event-type of the event log message cannot be null");
		}
		
		
		// 1 -- Initialize the class
		this.date = new Date();
		this.eventType = eventType;
	}
	
	public EventLogMessage (EventType eventType, Date date ){
		
		// 0 -- Precondition check
		if( eventType == null ){
			throw new IllegalArgumentException("The event-type of the event log message cannot be null");
		}
		
		if( date == null ){
			throw new IllegalArgumentException("The date of the event log message cannot be null");
		}
		
		
		// 1 -- Initialize the class
		this.date = (Date)date.clone();
		this.eventType = eventType;
	}
	
	public EventLogMessage (EventType eventType, Date date, EventLogField[] fields){
		
		// 0 -- Precondition check
		if( eventType == null ){
			throw new IllegalArgumentException("The event-type of the event log message cannot be null");
		}
		
		if( date == null ){
			throw new IllegalArgumentException("The date of the event log message cannot be null");
		}
		
		
		// 1 -- Initialize the class
		this.date = (Date)date.clone();
		this.eventType = eventType;
		
		for(int c = 0; c < fields.length; c++){
			this.fields.add(fields[c]);
		}
	}
	
	public EventLogMessage (EventType eventType, EventLogField... fields){
		
		// 0 -- Precondition check
		if( eventType == null ){
			throw new IllegalArgumentException("The event-type of the event log message cannot be null");
		}
		
		if( fields == null ){
			throw new IllegalArgumentException("The event log field cannot be null");
		}
		
		// 1 -- Initialize the class
		this.date = new Date();
		this.eventType = eventType;
		
		for (EventLogField eventlogField : fields) {
			this.fields.add(eventlogField);
		}
		
	}
	
	public Date getDate(){
		return (Date)date.clone();
	}
	
	public EventLogSeverity getSeverity(){
		return eventType.getSeverity();
	}
	
	public EventType getEventType(){
		return eventType;
	}
	
	public String getMessageName(){
		return eventType.getName();
	}
	
	/**
	 * Add the given field to the list of fields.
	 * @param field
	 */
	public void addField( EventLogField field ){
		
		// 0 -- Precondition check
		if( field == null ){
			throw new IllegalArgumentException("The field cannot be null");
		}
		
		// 1 -- Add the field
		fields.add(field);
	}
	
	/**
	 * Get the fields in an array format.
	 * @return
	 */
	public EventLogField[] getFields(){
		
		EventLogField[] fieldsArray = new EventLogField[this.fields.size()];
		
		this.fields.toArray(fieldsArray);
		return fieldsArray;
	}
	
	/**
	 * Identifies a field with the given name and returns it. Method returns null if no field matches the name.
	 * @param name
	 * @return
	 */
	public EventLogField getField(FieldName name){
		for (EventLogField field : fields) {
			if( field.getName() == name ){
				return field;
			}
		}
		
		return null;//Field was not found
	}
	
	/**
	 * Get a string containing just the field data (as opposed to the complete message).
	 * The argument indicates whether or not to include the curly braces before and after
	 * the message (only applies if the message is greater than zero length). 
	 * @return
	 */
	public String getFieldsAsString( ){
		return getFieldsAsString( true );
	}
	
	/**
	 * Get a string containing just the field data (as opposed to the complete message).
	 * @return
	 */
	public String getFieldsAsString( boolean includeCurlyBraces ){
		StringBuffer buffer = new StringBuffer();
		
		if( fields != null && fields.size() > 0 ){
			if( includeCurlyBraces ) {
				buffer.append(" { ");
			}
			
			for(int c = 0; c < fields.size();  c++){
				
				// Add the comma if this is not the first field
				if( c > 0 ){
					buffer.append(", ");
				}
				
				// Add the field
				buffer.append(fields.get(c).toString());
				
			}
			
			if( includeCurlyBraces ) {
				buffer.append(" }");
			}
		}
		
		return buffer.toString();
	}
	
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		
		buffer.append("[");
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/dd/MM HH:mm:ss zzz");
		
		buffer.append(dateFormat.format(date));
		buffer.append(", ").append( eventType.getSeverity().toString() );
		
		buffer.append("] ");
		buffer.append( eventType.name );
		
		buffer.append( getFieldsAsString() );
		
		return buffer.toString();
		
	}
}
