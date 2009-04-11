package net.lukeMurphey.nsia.eventLog;

import java.util.regex.Pattern;

public class EventLogField{
	
	public static class FieldName{
		public static final FieldName SEVERITY = FieldName.createFieldName("Severity");
		public static final FieldName SITE_GROUP_ID = FieldName.createFieldName("SiteGroupID");
		public static final FieldName SITE_GROUP_NAME = FieldName.createFieldName("SiteGroupName");
		public static final FieldName RULE_ID = FieldName.createFieldName("RuleID");
		public static final FieldName EMAIL_ADDRESS = FieldName.createFieldName("EmailAddress");
		public static final FieldName REAL_NAME = FieldName.createFieldName("RealName");
		public static final FieldName SOURCE_USER_ID = FieldName.createFieldName("SourceUserID");
		public static final FieldName TARGET_USER_ID = FieldName.createFieldName("TargetUserID");
		public static final FieldName SOURCE_USER_NAME = FieldName.createFieldName("SourceUserName");
		public static final FieldName TARGET_USER_NAME = FieldName.createFieldName("TargetUserName");
		public static final FieldName URL = FieldName.createFieldName("URL");
		public static final FieldName DEVIATIONS = FieldName.createFieldName("Deviations");
		public static final FieldName UPTIME = FieldName.createFieldName("Uptime");
		public static final FieldName VERSION = FieldName.createFieldName("Version");
		public static final FieldName RIGHT = FieldName.createFieldName("Right");
		public static final FieldName MESSAGE = FieldName.createFieldName("Message");
		public static final FieldName OBJECT_ID = FieldName.createFieldName("ObjectID");
		public static final FieldName OPERATION = FieldName.createFieldName("Operation");
		public static final FieldName SOURCE_ADDRESS = FieldName.createFieldName("SourceAddress");
		public static final FieldName RULE_SPECIMEN = FieldName.createFieldName("Specimen");
		public static final FieldName DEFINITION_ID = FieldName.createFieldName("DefinitionID");
		public static final FieldName DEFINITION_NAME = FieldName.createFieldName("DefinitionName");
		public static final FieldName RULE_TYPE = FieldName.createFieldName("RuleType");
		public static final FieldName STACK_TRACE = FieldName.createFieldName("StackTrace");
		public static final FieldName TASK = FieldName.createFieldName("Task");
		public static final FieldName SESSION_ID = FieldName.createFieldName("SessionID");
		public static final FieldName GROUP_ID = FieldName.createFieldName("GroupID");
		public static final FieldName GROUP_NAME = FieldName.createFieldName("GroupName");
		public static final FieldName VALUE = FieldName.createFieldName("Value");
		public static final FieldName LENGTH = FieldName.createFieldName("Length");
		public static final FieldName SESSION_TRACKING_NUMBER = FieldName.createFieldName("SessionTrackingNumber");
		public static final FieldName IMPORT_SOURCE = FieldName.createFieldName("ImportSource");
		public static final FieldName RESPONSE_ACTION_ID = FieldName.createFieldName("ResponseActionID");
		public static final FieldName PARAMETER = FieldName.createFieldName("Parameter");
		public static final FieldName FILE = FieldName.createFieldName("File");
		public static final FieldName RESPONSE_ACTION_NAME = FieldName.createFieldName("ResponseActionName");
		public static final FieldName RESPONSE_ACTION_DESC = FieldName.createFieldName("ResponseActionDescription");
		
		
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




