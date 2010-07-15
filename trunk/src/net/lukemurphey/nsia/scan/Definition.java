package net.lukemurphey.nsia.scan;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Definition {
	
	public enum Action{
		ALERT, BLOCK, EVAL
	}
	
	public enum Severity{
		UNDEFINED, LOW, MEDIUM, HIGH
	}
	
	protected Vector<Reference> references = new Vector<Reference>();
	
	/*
	 * This regular expression matches the name of a definition (and extracts it's sub-components). The unescaped expression is as follows:
	 * 
	 * ([-a-zA-Z0-9]+).([-a-zA-Z0-9]+).([-a-zA-Z0-9@]+)
	 * 
	 * An example that would be matched is:
	 * 
	 * Exploit.Suspicious.VBscriptExecution
	 * 
	 */
	private static final Pattern RULE_NAME_REGEX = Pattern.compile("([-a-zA-Z0-9_]+).([-a-zA-Z0-9_]+).([-a-zA-Z0-9@_]+)");
	
	protected String category;
	protected String subCategory;
	protected String name;
	protected String definitionType;
	protected String message;
	protected Severity severity = Severity.UNDEFINED;
	protected int revision;
	protected int id = -1;
	protected Action action;
	protected int localId = -1; //This is the ID assigned when the definition is inserted into the database
	
	public String getName( ) {
		return name;
	}
	
	public String getFullName( ) {
		return category + "." + subCategory + "." + name;
	}
	
	public Action getAction() {
		return action;
	}

	public String getMessage() {
		return message;
	}
	
	public Reference[] getReferences(){
		Reference[] refs = new Reference[references.size()];
		
		references.toArray(refs);
		
		return refs;
	}

	public String getCategoryName() {
		return category;
	}
	
	public Severity getSeverity() {
		return severity;
	}

	public String getSubCategoryName() {
		return subCategory;
	}
	
	public int getID() {
		return id;
	}
	
	public boolean isOfficial(){
		if( id < 1000000 && id > -1){
			return true;
		}
		else{
			return false;
		}
	}
	
	public int getLocalID() {
		return localId;
	}
	
	public int getRevision() {
		return revision;
	}
	
	public static String[] parseName( String name ) throws InvalidDefinitionException{
		if( name == null ){
			throw new InvalidDefinitionException("A name for the definition must be provided");
		}
		
		Matcher matcher = RULE_NAME_REGEX.matcher( name );
		
		if( !matcher.find() ){
			throw new InvalidDefinitionException("The name is not a valid threat definition name");
		}
		
		String[] result = new String[3];
		
		result[0] = matcher.group(1);
		result[1] = matcher.group(2);
		result[2] = matcher.group(3);
		
		if( result[2].length() < 1 ){
			throw new InvalidDefinitionException("The name is not a valid threat definition name");
		}
		else if( result[0].length() < 1 ){
			throw new InvalidDefinitionException("The name is not a valid threat definition name");
		}
		else if( result[1].length() < 1 ){
			throw new InvalidDefinitionException("The name is not a valid threat definition name");
		}
		
		return result;
	}
	
	protected void parseFullName( String name ) throws InvalidDefinitionException{
		
		if( name == null ){
			throw new InvalidDefinitionException("A name for the definition must be provided");
		}
		
		Matcher matcher = RULE_NAME_REGEX.matcher( name );
		
		if( !matcher.find() ){
			throw new InvalidDefinitionException("The name is not a valid threat definition name");
		}
		
		this.category = matcher.group(1);
		this.subCategory = matcher.group(2);
		this.name = matcher.group(3);
		
		if( name.length() < 1 ){
			throw new InvalidDefinitionException("The name is not a valid threat definition name");
		}
		else if( category.length() < 1 ){
			throw new InvalidDefinitionException("The name is not a valid threat definition name");
		}
		else if( subCategory.length() < 1 ){
			throw new InvalidDefinitionException("The name is not a valid threat definition name");
		}
	}
	
	public String getType(){
		return definitionType;
	}
	
	public String toString(){
		return getFullName();
	}
	

	/**
	 * This class represents a definition reference (URL, CVE entry, etc.). These references are based on the Snort reference types. 
	 * @author luke
	 *
	 */
	public static class Reference{
		
		public final static Type BUGTRAQ = new Type(1, "http://www.securityfocus.com/bid/");
		public final static Type CVE = new Type(2, "http://cve.mitre.org/cgi-bin/cvename.cgi?name=");
		public final static Type NESSUS = new Type(3, "http://cgi.nessus.org/plugins/dump.php3?id=");
		public final static Type ARACHNIDS = new Type(4, "http://www.whitehats.com/info/IDS");
		public final static Type MCAFEE = new Type(5, "http://vil.nai.com/vil/dispVirus.asp?virus_k=");
		public final static Type URL = new Type(6, "http://");
		public final static Type OSVDB = new Type(7, "http://osvdb.org/show/osvdb/", "/discuss");
		public final static Type USN = new Type(8, "http://www.ubuntu.com/usn/usn-");
		public final static Type MILWORM = new Type(9, "http://milw0rm.com/exploits");
		public final static Type SECUNIA = new Type(10, "http://secunia.com/advisories/", "/");
		public final static Type RHSA = new Type(11, "http://rhn.redhat.com/errata/", ".html");
		public final static Type MICROSOFT_KB = new Type(12, "http://support.microsoft.com/kb/");
		public final static Type MICROSOFT_SB = new Type(13, "http://www.microsoft.com/technet/security/bulletin/", ".mspx");
		
		/**
		 * This class represents the possible Snort reference types.
		 * See http://www.snort.org/docs/snort_htmanuals/htmanual_2.4/node18.html#SECTION00442000000000000000
		 * @author luke
		 *
		 */
		public static class Type{
			
			private int id;
			private String urlPrefix;
			private String urlSuffix = null;
			
			protected Type( int id, String urlPrefix ){
				this.id = id;
				this.urlPrefix = urlPrefix;
			}
			
			protected Type( int id, String urlPrefix, String urlSuffix ){
				this.id = id;
				this.urlPrefix = urlPrefix;
				this.urlSuffix = urlSuffix;
			}
			
			public String getUrlPrefix(){
				return urlPrefix; 
			}
			
			public String getUrlSuffix(){
				return urlSuffix; 
			}
			
			public boolean equals(Object type){
				
				// 0 -- Precondition check
				if( type.equals(null) ){
					throw new IllegalArgumentException("Type cannot be null");
				}
				
				// 1 -- Compare the types
				if( type instanceof Type){
					return ((Type)type).id == id;
				}
				else{
					return false;
				}
			}
		}
		
		private Type type;
		private String value;
		
		public Reference(Type type, String value){
			this.type = type;
			this.value = value;
		}
		
		public static Reference parse(String value) throws InvalidDefinitionException{
			int firstComma = value.indexOf(',');
			
			if( firstComma < 0 ){
				throw new InvalidDefinitionException("Reference is invalid");
			}
			
			String type = value.substring(0, firstComma).trim();
			String argument = value.substring(firstComma+1).trim();
			
			if( type.equalsIgnoreCase("bugtraq") ){
				return new Reference(Reference.BUGTRAQ, argument);
			}
			else if( type.equalsIgnoreCase("cve") ){
				return new Reference(Reference.CVE, argument);
			}
			else if( type.equalsIgnoreCase("nessus") ){
				return new Reference(Reference.NESSUS, argument);
			}
			else if( type.equalsIgnoreCase("arachnids") ){
				return new Reference(Reference.ARACHNIDS, argument);
			}
			else if( type.equalsIgnoreCase("mcafee") ){
				return new Reference(Reference.MCAFEE, argument);
			}
			else if( type.equalsIgnoreCase("url") ){
				return new Reference(Reference.URL, argument);
			}
			else if( type.equalsIgnoreCase("osvdb") ){
				return new Reference(Reference.OSVDB, argument);
			}
			else if( type.equalsIgnoreCase("usn") ){
				return new Reference(Reference.USN, argument);
			}
			else if( type.equalsIgnoreCase("milworm") ){
				return new Reference(Reference.MILWORM, argument);
			}
			else if( type.equalsIgnoreCase("secunia") ){
				return new Reference(Reference.SECUNIA, argument);
			}
			else if( type.equalsIgnoreCase("rhsa") ){
				return new Reference(Reference.RHSA, argument);
			}
			else if( type.equalsIgnoreCase("microsoft_kb") ){
				return new Reference(Reference.MICROSOFT_KB, argument);
			}
			else if( type.equalsIgnoreCase("microsoft_bulletin") ){
				return new Reference(Reference.MICROSOFT_SB, argument);
			}
			else{
				throw new InvalidDefinitionException("Reference name (\"" + type + "\" is invalid");
			}
		}
		
		public String toString(){
			if( type.getUrlSuffix() == null ){
				return type.getUrlPrefix() + value;
			}
			else{
				return type.getUrlPrefix() + value + type.getUrlSuffix();
			}
		}
		
		public Type getType(){
			return type;
		}
		
		public String getValue(){
			return value;
		}
	}
}
