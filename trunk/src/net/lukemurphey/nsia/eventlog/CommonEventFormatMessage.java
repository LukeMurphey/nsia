package net.lukemurphey.nsia.eventlog;

import java.util.Vector;

public class CommonEventFormatMessage {
	
	//These fields are the predefined keys that make up the Common Extension Dictionary Field
	public enum CommonExtensionDictionaryField {
		DEVICE_ACTION("act", "deviceAction"),
		APPLICATION_PROTOCOL("app", "applicationProtocol"),
		BYTES_IN("in", "bytesIn"),
		BYTES_OUT("out", "bytesOut"),
		DESTINATION_ADDRESS("dst", "destinationAddress"),
		DESTINATION_HOST_NAME("dhost", "destinationHostName"),
		DESTINATION_MAC_ADDRESS("dmac", "destinationMacAddress"),
		DESTINATION_NT_DOMAIN("dntdom", "destinationNtDomain"),
		DESTINATION_PORT("dpt", "destinationPort"),
		DESTINATION_PROCESS_NAME("dproc", "destinationProcessName"),
		DESTINATION_USER_ID("duid", "destinationUserId"),
		DESTINATION_USER_PRIVILEGES("dpriv", "destinationUserPrivileges"),
		DESTINATION_USER_NAME("duser", "destinationUserName"),
		END_TIME("end", "endTime"),
		FILE_NAME("fname", "fileName"),
		FILE_SIZE("fsize", "fileSize"),
		MESSAGE("msg", "message"),
		RECEIPT_TIME("rt", "receiptTime"),
		REQUEST("request", "requestUrl"),
		SOURCE_ADDRESS("src", "sourceAddress"),
		SOURCE_HOST_NAME("shost", "sourceHostName"),
		SOURCE_MAC_ADDRESS("smac", "sourceMacAddress"),
		SOURCE_NT_DOMAIN("sntdom", "sourceNtDomain"),
		SOURCE_PORT("spt", "sourcePort"),
		SOURCE_USER_ID("suid", "sourceUserId"),
		SOURCE_USER_PRIVILEGES("spriv", "sourceUserPrivileges"),
		SOURCE_USER("suser", "sourceUserName"),
		START_TIME("start", "startTime"),
		TRANSPORT_PROTOCOL("proto", "transportProtocol");
		
		private CommonExtensionDictionaryField(String name, String desc) {
			this.name = name;
			this.desc = desc;
		}
		
		private final String name;
		private final String desc;
		
		public String toString() {
			return name;
		}
		
		public String getName() {
			return name;
		}
		
		public String getFullName(){
			return desc;
		}
	}
	
	// This class represents an extension field
	public static class ExtensionField{
		
		private String name;
		private String value = null;
		
		public ExtensionField( CommonExtensionDictionaryField cedField, String value){
			this.name = cedField.getName();
			this.value = String.valueOf( value );
		}
		
		public ExtensionField( String name, String value){
			this.name = name;
			this.value = String.valueOf( value );
		}
		
		public ExtensionField( String name, long value){
			this.name = name;
			this.value = String.valueOf( value );
		}
		
		public String getName(){
			return name;
		}
		
		public String getValue(){
			return value;
		}
		
		public String toString(){
			return escapeField(name, false) + "=" + escapeField(value, false);
		}
	}
	
	public static final int VERSION = 0;
	private static final String DELIMINATOR = "|";
	
	//Required fields
	private String deviceVendor = null;
	private String deviceProduct = null;
	private String deviceVersion = null;
	private String deviceSignatureID = null;
	private String name = null;
	private int deviceSeverity = 0;
	
	//Extension fields
	Vector<ExtensionField> extensionFields = new Vector<ExtensionField>();
	
	public CommonEventFormatMessage( String deviceVendor, String deviceProduct, String deviceVersion, Vector<EventLogField> fields ){
		EventLogField[] fieldsArray = new EventLogField[fields.size()];
		
		fields.toArray(fieldsArray);
		
		initialize(deviceVendor, deviceProduct, deviceVersion, fieldsArray);
	}
	
	public CommonEventFormatMessage( String deviceVendor, String deviceProduct, String deviceVersion, EventLogField[] fields ){
		
		initialize( deviceVendor, deviceProduct, deviceVersion, fields);
		
	}
	
	public CommonEventFormatMessage( String deviceVendor, String deviceProduct, String deviceVersion, String signatureID, String name, int deviceSeverity){
		
		// 0 -- Precondition check
		
		//	 0.1 -- Check the device vendor field
		if( deviceVendor == null || deviceVendor.length() == 0){
			throw new IllegalArgumentException("The device vendor field is required and cannot be null or blank");
		}
		
		//	 0.2 -- Check the device product field
		if( deviceProduct == null || deviceProduct.length() == 0){
			throw new IllegalArgumentException("The device product field is required and cannot be null or blank");
		}
		
		//	 0.3 -- Check the device version field
		if( deviceVersion == null || deviceVersion.length() == 0){
			throw new IllegalArgumentException("The device version field is required and cannot be null or blank");
		}
		
		//	 0.4 -- Check the signature ID field
		if( signatureID == null || signatureID.length() == 0){
			throw new IllegalArgumentException("The signature ID field is required and cannot be null or blank");
		}
		
		//	 0.5 -- Check the name field
		if( name == null || name.length() == 0){
			throw new IllegalArgumentException("The name field is required and cannot be null or blank");
		}
		
		//	 0.6 -- Check the severity field
		if( deviceSeverity < 0 || deviceSeverity > 10){
			throw new IllegalArgumentException("The severity field is out of range (must not be less than 0 or greater than 10)");
		}
		
		
		// 1 -- Initialize the class
		this.deviceVendor = escapeField(deviceVendor, true);
		this.deviceProduct = escapeField(deviceProduct, true);
		this.deviceVersion = escapeField(deviceVersion, true);
		this.deviceSignatureID = escapeField(signatureID, true);
		this.name = escapeField(name, true);
		this.deviceSeverity = deviceSeverity;
	}
	
	private void initialize( String deviceVendor, String deviceProduct, String deviceVersion, EventLogField[] fields ){
		this.deviceVendor = escapeField(deviceVendor, true);
		this.deviceProduct = escapeField(deviceProduct, true);
		this.deviceVersion = escapeField(deviceVersion, true);
		
		for(int c = 0; c < fields.length; c++){
			
			// 1 -- Load the necessary attributes
			if( fields[c].getName() == EventLogField.FieldName.TARGET_USER_NAME ){
				addExtensionField( new ExtensionField(CommonExtensionDictionaryField.DESTINATION_USER_NAME, fields[c].getDescription()) );
			}
			else if( fields[c].getName() == EventLogField.FieldName.TARGET_USER_ID ){
				addExtensionField( new ExtensionField(CommonExtensionDictionaryField.DESTINATION_USER_ID, fields[c].getDescription()) );
			}
			else if( fields[c].getName() == EventLogField.FieldName.SOURCE_USER_ID ){
				addExtensionField( new ExtensionField(CommonExtensionDictionaryField.SOURCE_USER_ID, fields[c].getDescription()) );
			}
			else if( fields[c].getName() == EventLogField.FieldName.SOURCE_USER_NAME ){
				addExtensionField( new ExtensionField(CommonExtensionDictionaryField.SOURCE_USER, fields[c].getDescription()) );
			}
			else if( fields[c].getName() == EventLogField.FieldName.MESSAGE ){
				addExtensionField( new ExtensionField(CommonExtensionDictionaryField.MESSAGE, fields[c].getDescription()) );
			}
			else if( fields[c].getName() == EventLogField.FieldName.SEVERITY ){
				//this.deviceSeverity = escapeField(fields[c].getDescription(), true); //Need to try to convert to int, or ignore
			}
		
			// 2 -- Load the other attributes
			else{
				//TODO Need to make sure that name does not overlap with existing entry
				addExtensionField( new ExtensionField(fields[c].getName().getSimpleNameFormat(), fields[c].getDescription())  );
			}
		}
	}
	
	public String getDeviceVendor(){
		return deviceVendor;
	}
	
	public String getDeviceProduct(){
		return deviceProduct;
	}
	public String getDeviceVersion(){
		return deviceVersion;
	}
	public String getDeviceSignatureID(){
		return deviceSignatureID;
	}
	public String getName(){
		return name;
	}
	
	public int getSeverity(){
		return deviceSeverity;
	}
	
	public void addExtensionField(ExtensionField field){
		
		// 0 -- Precondition check
		if( field == null){
			throw new IllegalArgumentException("The extension field cannot be null");
		}
		
		// 1 -- Perform the action
		extensionFields.add(field);
	}
	
	public ExtensionField[] getExtensionFields(){
		ExtensionField[] fields = new ExtensionField[extensionFields.size()];
		extensionFields.toArray(fields);
		return fields;
	}
	
	public static String escapeField( String field, boolean isPrefix ){
		
		String tempString = null;
		
		// 1 -- Escape slashes (if prefix)
		if( isPrefix == true ){
			tempString = org.apache.commons.lang.StringUtils.replace(field, "\\", "\\\\");
		}
		// 2 -- Escape the equal sign (if not a prefix)
		else{
			tempString = org.apache.commons.lang.StringUtils.replace(field, "=", "\\=");
		}
		
		// 3 -- Escape bars
		if( isPrefix == true ){
			tempString = org.apache.commons.lang.StringUtils.replace(field, "|", "\\|");
		}
		
		return tempString;
	}
	
	public String getCEFMessage(){
		StringBuffer message = new StringBuffer();
		
		message.append("CEF:");
		message.append(VERSION);
		
		message.append(DELIMINATOR);
		message.append(deviceVendor);
		
		message.append(DELIMINATOR);
		message.append(deviceProduct);
		
		message.append(DELIMINATOR);
		message.append(deviceVersion);
		
		message.append(DELIMINATOR);
		message.append(deviceSignatureID);
		
		message.append(DELIMINATOR);
		message.append(name);
		
		message.append(DELIMINATOR);
		message.append(deviceSeverity);
		
		// Insert a deliminator just before the extension fields
		if( extensionFields.size() > 0 ){
			message.append(DELIMINATOR);
		}
		
		// Append the extensions
		for (ExtensionField extensionField : extensionFields) {
			message.append(" ");
			message.append(extensionField.toString());
		}
		
		return message.toString();
	}
	
	@Override
	public String toString(){
		return getCEFMessage();
	}
	
}
