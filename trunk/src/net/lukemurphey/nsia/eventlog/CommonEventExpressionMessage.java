package net.lukemurphey.nsia.eventlog;

import java.util.Vector;

//http://blogs.splunk.com/raffy/2008/03/06/common-event-syntax/
public class CommonEventExpressionMessage {

	public enum CommonEventExpressionField{
		ACTEDON_USER("actedon_user", "User name that is being acted upon. For example password for a specific user was changed."),
		ACTION("action", "The action as reported by the logging device."),
		APP("app", "application layer protocol--e.g. HTTP, HTTPS, SSH, IMAP."),
		BYTES_IN("bytes_in", "How many bytes this device/interface took in."),
		BYTES_OUT("bytes_out", "How many bytes this device/interface sent out."),
		CATEGORY("category", "A category that a device may have assigned an event to."),
		CHANNEL("channel", "802.11 channel number of a wireless transmission"),
		COUNT("count", "The number of times the event has been seen."),
		CVE("cve", "CVE vulnerability reference."),
		DATABASE_NAME("database_name", "Name of a database."),
		DATABASE_TABLE("database_table", "Name of a database table."),
		DATABASE_QUERY("database_query", "Query issued against a database."),
		DELAY("delay", "Delay in seconds. For example the delay when processing an email message."),
		DEST_COUNTRY("dest_country", "Country of where the destination in the log record resides. In case of a point event (e.g., an operating system event), the country is kept here."),
		DEST_HOST("dest_host", "Fully qualified host name of the machine targeted in the record. In case of a point event (e.g., an operating system event), the machine's host name is kept here."),
		DEST_IPV6("dest_ipv6", "IPv6 address of the machine targeted in the record. In case of a point event (e.g., an operating system event), the machine's IPv6 address is kept here."),
		DEST_IP("dest_ip", "IPv4 address of the machine targeted in the record. In case of a point event (e.g., an operating system event), the machine's IP address is kept here."),
		DEST_LAT("dest_lat", "Latitude of the destination in the log record. In case of a point event (e.g., an operating system event), the latitude is kept here."),
		DEST_LONG("dest_long", "Longitude of the destination in the log record. In case of a point event (e.g., an operating system event), the longitude is kept here."),
		DEST_MAC("dest_mac", "Destination MAC (layer 2) address. In case of a point event (e.g., an operating system event), the machine's MAC address is kept here."),
		DEST_NT_DOMAIN("dest_nt_domain", "The Windows NT domain for the machine targeted in the record. In case of a point event (e.g., an operating system event), the machine's NT domain name is kept here. In Windows, this is also called the WORKGROUP."),
		DEST_NT_HOST("dest_nt_host", "The Windows NT host name for the machine targeted in the record. In case of a point event (e.g., an operating system event), the machine's NT host name is kept here. In Windows this is also called the WORKSTATION."),
		DEST_PORT("dest_port", "The network port expressed as the target in the log record. In case of a point event (e.g., an operating system event), the port that was used is kept here."),
		DEST_TRANSLATED_IP("dest_translated_ip", "The translated (e.g., NATted) network address expressed as the destination in the log record."),
		DEST_TRANSLATED_PORT("dest_translated_port", "The translated (e.g., NATted) network port expressed as the destination in the log record."),
		DIRECTION("direction", "The direction the packet is traveling, allowed values: inbound or outbound."),
		DURATION("duration", "The amount of time the event lasted, measured in seconds (e.g., 12.321)."),
		DVC_HOST("dvc_host", "Fully qualified host name of the device reporting the log record."),
		DVC_IPV6("dvc_ipv6", "IPv6 address of the device reporting the log record."),
		DVC_IP("dvc_ip", "IPv4 address of the device reporting the log record."),
		DVC_LOCATION("dvc_location", "Free-text description of the physical location of the device."),
		DVC_MAC("dvc_mac", "MAC (layer 2) address of the device reporting the log record."),
		DVC_NT_DOMAIN("dvc_nt_domain", "Windows domain name of the device reporting the log record."),
		DVC_NT_HOST("dvc_nt_host", "Windows host name of the device reporting the log record."),
		DVC_SEVERITY("dvc_severity", "Severity exactly as reported in the log record. Sometimes called priority."),
		DVC_TIME("dvc_time", "Time at which the device received the log record."),
		END_TIME("end_time", "The event's specified end time."),
		EVENT_ID("event_id", "Number, unique to the application domain, identifying the event. In case of email logs, this is the message ID."),
		FILE_ACCESS_TIME("file_access_time", "The time the file (the object of the event) was accessed."),
		FILE_CREATE_TIME("file_create_time", "The time the file (the object of the event) was created."),
		FILE_HASH("file_hash", "The file hash identifying the file that is object of the event."),
		FILE_MODIFY_TIME("file_modify_time", "The time the file (the object of the event) was altered."),
		FILE_NAME("file_name", "The name of the file that is the object of the event, with no path information."),
		FILE_PATH("file_path", "The path to the file that is the object of the event, without the file name."),
		FILE_PERMISSION("file_permission", "The permissions of the file that is the object of the event."),
		FILE_SIZE("file_size", "The size of the file (in bytes) that is the object of the event."),
		HTTP_CLIENT("http_client", "The HTTP client identified in the event."),
		HTTP_CONTENT_TYPE("http_content_type", "The HTTP content type."),
		HTTP_METHOD("http_method", "The HTTP method used in the event."),
		HTTP_REFERRER("http_referrer", "The HTTP referrer listed in the event."),
		HTTP_RESPONSE("http_response", "The HTTP response code."),
		HTTP_USER_AGENT("http_user_agent", "The HTTP user agent."),
		INBOUND_INTERFACE("inbound_interface", "The interface the record referenced, such as eth0 for a Linux box's first Ethernet card."),
		NAME("name", "Name of the event as reported by the device. The name should not contain information that's already being parsed into fields from the event, such as IP addresses."),
		OUTBOUND_INTERFACE("outbound_interface", "The interface the record referenced, such as eth0 for a Linux box's first Ethernet card."),
		PACKETS_IN("packets_in", "How many packets this device/interface took in."),
		PACKETS_OUT("packets_out", "How many packets this device/interface sent out."),
		PID("pid", "Process id corresponding with the process."),
		PRIORITY("priority", "The priority assigned to the event, in terms of 0 (lowest) to 10 (highest)."),
		PROCESS("process", "Process name involved in generating the log record (e.g., process name mentioned in syslog header)."),
		PRODUCT_VERSION("product_version", "The version of the product that generated the event."),
		PRODUCT("product", "The product that generated the event."),
		PROTO("proto", "network layer protocol--e.g. IP, ICMP, IPsec, ARP."),
		RECEIVER("receiver", "Email recipient."),
		RELAY("relay", "A relay server used to forward a message. For example an email relay."),
		SIGNATURE("signature", "A unique identifier for a class of events. Snort for example uses the SID. Other IDSs use a signature ID, could be the eventID in Windows, could be the firewall rule number."),
		SRC_COUNTRY("src_country", "Country of where the source in the log record resides."),
		SRC_HOST("src_host", "Fully qualified host name of the source machine in the record."),
		SRC_IPV6("src_ipv6", "IPv6 address of the source machine in the record."),
		SRC_IP("src_ip", "IPv4 address of the source machine in the record."),
		SRC_LAT("src_lat", "Latitude of the source in the log record."),
		SRC_LONG("src_long", "Longitude of the source in the log record."),
		SRC_MAC("src_mac", "Source MAC (layer 2) address."),
		SRC_NT_DOMAIN("src_nt_domain", "The Windows NT domain for the source machine in the record."),
		SRC_NT_HOST("src_nt_host", "The Windows NT host for the source machine in the record."),
		SRC_PORT("src_port", "The network port expressed as the source in the log record."),
		SRC_TRANSLATED_IP("src_translated_ip", " NATted) network address expressed as the source in the log record.,"),
		SRC_TRANSLATED_PORT("src_translated_port", "The translated (e.g., NATted) network port expressed as the source in the log record."),
		SRC_USER_ID("src_user_id", "ID number of the user that is the source of an event. The one executing the action."),
		SRC_USER_PRIVILEGE("src_user_privilege", "One of administrator, user, or guest/anonymous, the privilege the source/acting user has assigned."),
		SRC_USER("src_user", "User that is the source of an event. The one executing the action."),
		SENDER("sender", "Email sender."),
		SSID("ssid", "The 802.11 ssid of a wireless transmission."),
		SIZE("size", "The size of an application layer protocol. For example, the size of an email, a document, or an HTTP response."),
		START_TIME("start_time", "The event's specified start time."),
		SUBJECT("subject", "Email subject line."),
		SYSLOG_FACILITY("syslog_facility", "The syslog facility assigned to this record."),
		SYSLOG_PRIORITY("syslog_priority", "The syslog priority assigned to this record."),
		TAX_OBJECT("tax_object", "The Object field from the CEE taxonomy."),
		TAX_ACTION("tax_action", "The Action field from the CEE taxonomy."),
		TAX_STATUS("tax_status", "The Status field from the CEE taxonomy."),
		TCP_FLAGS("tcp_flags", "The TCP flag specified in the event. One or more of SYN, ACK, FIN, RST, URG, or PSH."),
		URL("url", "The URL that is the object of the event."),
		USER_GROUP_ID("user_group_id", "ID number of the user group that is the object of an event."),
		USER_GROUP("user_group", "User group that is the object of an event."),
		USER_ID("user_id", "ID number of the user that is the object of an event."),
		USER_PRIVILEGE("user_privilege", "One of administrator, user, or guest/anonymous, the privilege the user has assigned."),
		USER("user", "User that is the object of an event."),
		VENDOR("vendor", "The vendor who made the product that generated the event."),
		VLAN_ID("vlan_id", "The numeric ID assigned to the vlan in the event."),
		VLAN_NAME("vlan_name", "The name assigned to the vlan in the event.");
		
		private String name;
		private String description;
		
		private CommonEventExpressionField(String name, String description) {
			this.name = name;
		}
		
		public String toString() {
			return name;
		}
		
		public String getName() {
			return name;
		}
		
		public String getDescription() {
			return description;
		}
	}

	// This class represents an extension field
	public static class ExtensionField{
		
		private String name;
		private String value = null;
		
		public ExtensionField( CommonEventExpressionField ceeField, String value){
			this.name = ceeField.getName();
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
		
		public ExtensionField( String name, int value){
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
			return escapeField(name) + "=" + escapeField(value);
		}
	}
	
	private static final String DELIMINATOR = ",";
	
	//Extension fields
	Vector<ExtensionField> extensionFields = new Vector<ExtensionField>();
	
	public CommonEventExpressionMessage( ){
		
	}
	
	public CommonEventExpressionMessage( ExtensionField... fields ){
		for( ExtensionField field : fields){
			extensionFields.add(field);
		}
			
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
	
	public static String escapeField( String field ){
		
		String tempString = field;
		boolean escape =false;
		
		// 1 -- Handle nulls
		if(tempString == null){
			return "null";
		}
		
		// 2 -- Escape equals signs
		if( field.contains("=") ){
			tempString = org.apache.commons.lang.StringUtils.replace(field, "=", "\\=");
			escape = true;
		}
		
		// 3 -- Escape commas
		/*if( field.contains(",") ){
			tempString = org.apache.commons.lang.StringUtils.replace(field, ",", "\\,");
			escape = true;
		}*/
		
		// 4 -- Escape quotes
		if( field.contains("\"") ){
			tempString = org.apache.commons.lang.StringUtils.replace(field, "\"", "\\\"");
			escape = true;
		}
		
		// 5 -- Add quotes if the string contains whitespace
		if( escape == true || field.contains(" ") ){
			tempString = "\"" + tempString + "\"";
		}
		
		return tempString;
	}
	
	public String getCEEMessage(){
		StringBuffer message = new StringBuffer();
		
		// Append the extensions
		for (ExtensionField extensionField : extensionFields) {
			if(message.length() > 0){
				message.append(DELIMINATOR);
			}
			
			message.append(extensionField.toString());
		}
		
		return message.toString();
	}
	
	@Override
	public String toString(){
		return getCEEMessage();
	}
}

