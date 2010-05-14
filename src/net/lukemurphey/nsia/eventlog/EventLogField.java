package net.lukemurphey.nsia.eventlog;

import java.util.regex.Pattern;

public class EventLogField{
	
	public static class FieldName{
		public static final FieldName SEVERITY = FieldName.createFieldName("severity");
		public static final FieldName SITE_GROUP_ID = FieldName.createFieldName("sitegroup_id");
		public static final FieldName SITE_GROUP_NAME = FieldName.createFieldName("sitegroup_name");
		public static final FieldName RULE_ID = FieldName.createFieldName("rule_id");
		public static final FieldName EMAIL_ADDRESS = FieldName.createFieldName("email_address");
		public static final FieldName REAL_NAME = FieldName.createFieldName("real_name");
		public static final FieldName SOURCE_USER_ID = FieldName.createFieldName("source_user_id");
		public static final FieldName TARGET_USER_ID = FieldName.createFieldName("target_user_id");
		public static final FieldName SOURCE_USER_NAME = FieldName.createFieldName("source_user_name");
		public static final FieldName TARGET_USER_NAME = FieldName.createFieldName("target_user_name");
		public static final FieldName URL = FieldName.createFieldName("url");
		public static final FieldName DEVIATIONS = FieldName.createFieldName("deviation_count");
		public static final FieldName UPTIME = FieldName.createFieldName("uptime");
		public static final FieldName VERSION = FieldName.createFieldName("version");
		public static final FieldName RIGHT = FieldName.createFieldName("right");
		public static final FieldName MESSAGE = FieldName.createFieldName("message");
		public static final FieldName OBJECT_ID = FieldName.createFieldName("object_id");
		public static final FieldName OPERATION = FieldName.createFieldName("operation");
		public static final FieldName SOURCE_ADDRESS = FieldName.createFieldName("source_address");
		public static final FieldName RULE_SPECIMEN = FieldName.createFieldName("specimen");
		public static final FieldName DEFINITION_ID = FieldName.createFieldName("definition_id");
		public static final FieldName DEFINITION_NAME = FieldName.createFieldName("definition_name");
		public static final FieldName DEFINITION_REVISION = FieldName.createFieldName("definition_revision");
		public static final FieldName RULE_TYPE = FieldName.createFieldName("rule_type");
		public static final FieldName STACK_TRACE = FieldName.createFieldName("stack_trace");
		public static final FieldName TASK = FieldName.createFieldName("task");
		public static final FieldName SESSION_ID = FieldName.createFieldName("session_id");
		public static final FieldName GROUP_ID = FieldName.createFieldName("group_id");
		public static final FieldName GROUP_NAME = FieldName.createFieldName("group_name");
		public static final FieldName VALUE = FieldName.createFieldName("value");
		public static final FieldName LENGTH = FieldName.createFieldName("length");
		public static final FieldName SESSION_TRACKING_NUMBER = FieldName.createFieldName("session_tracking_number");
		public static final FieldName IMPORT_SOURCE = FieldName.createFieldName("import_source");
		public static final FieldName RESPONSE_ACTION_ID = FieldName.createFieldName("response_action_id");
		public static final FieldName PARAMETER = FieldName.createFieldName("parameter");
		public static final FieldName FILE = FieldName.createFieldName("file");
		public static final FieldName RESPONSE_ACTION_NAME = FieldName.createFieldName("response_action_name");
		public static final FieldName RESPONSE_ACTION_DESC = FieldName.createFieldName("response_action_description");
		
		
		private final Pattern FIELD_NAME_REGEX = Pattern.compile("[A-Za-z0-9]+");
		private String name;
	
		public FieldName( String name ){
			
			// 0 -- Precondition check
			if( FIELD_NAME_REGEX.matcher(name).matches() == false ){
				throw new IllegalArgumentException("The name of the field is invalid");
			}
			
			this.name = name;
		}
		
		private FieldName(){
			//Only used so the static instances do not have to worry about input validation (can just create the class and set the name without the name being checked)
		}
		
		private static FieldName createFieldName( String name ){
			FieldName field = new FieldName();
			field.name = name;
			return field;
		}
		
		public String getSimpleNameFormat(){
			return name;
		}
	}
	
	private FieldName name;
	private String description;
	
	public EventLogField( FieldName name, String description ){
		this.name = name;
		this.description = description;
	}
	
	public EventLogField( FieldName name, long value ){
		this.name = name;
		this.description = Long.toString(value);
	}
	
	public EventLogField( FieldName name, int value ){
		this.name = name;
		this.description = Integer.toString(value);
	}
	
	public FieldName getName(){
		return name;
	}
	
	public String getDescription(){
		return description;
	}
	
	public String toString(){
		return name.getSimpleNameFormat() + " = " + escapeField(description);
	}
	
	private static String escapeField( String value ){
		
		String tempString = null;
		
		// Set value to literal "null" if field is null
		if(value == null){
			return "null";
		}
		
		// Put value in quotes if it contains whitespace
		if( value.contains(" ") ){
			tempString ="\"" + org.apache.commons.lang.StringUtils.replace(value, "\"", "\\\"") + "\"";
		}
		
		// Put value in quotes if it contains a comma
		else if(value.contains(",")){
			tempString = "\"" + org.apache.commons.lang.StringUtils.replace(value, "\"", "\\\"") + "\"";
		}
		
		// Put value in quotes if it contains a quotation mark and escape the quotation mark
		else if(value.contains("\"")){
			tempString = "\"" + org.apache.commons.lang.StringUtils.replace(value, "\"", "\\\"") + "\"";
		}
		
		else{
			tempString = value;
		}
		
		return tempString;
	}
}




