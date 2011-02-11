package net.lukemurphey.nsia.scan;

import java.util.Vector;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Collections;
import java.io.*;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.*;

import net.lukemurphey.nsia.*;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.scan.DefinitionPolicyDescriptor.DefinitionPolicyAction;
import net.lukemurphey.nsia.scan.scriptenvironment.Result;
import net.lukemurphey.nsia.scan.scriptenvironment.Variables;

import java.sql.SQLException;

import javax.script.ScriptException;
import javax.xml.parsers.*;

import org.apache.commons.lang.StringEscapeUtils;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * This class loads the definitions from the persistent storage system and provides the list of prepared definitions.
 * @author luke
 *
 */
public class DefinitionSet {

	private Vector<Definition> definitions = new Vector<Definition>();
	private Date definitionSetDate = null;
	private String definitionVersionString = null;
	private int countOfCustomDefinitions = -1;
	
	public final static String DEFINITION_SET_DATE_FORMAT = "MMM dd HH:mm:ss Z yyyy";
	
	public enum DefinitionType{
		SCRIPT, PATTERN
	}
	
	/**
	 * Constructs a definition set with the given date and list of definitions.
	 * @param definitionSetDate
	 * @param definitions
	 */
	public DefinitionSet( Date definitionSetDate, Definition[] definitions, String versionIdentifier ){
		
		// 0 -- Precondition check
		if( definitions == null ){
			throw new IllegalArgumentException("The definitions given must not be null");
		}
		
		// 1 -- Add the s and set the parameters
		for( int c = 0; c < definitions.length; c++){
			this.definitions.add( definitions[c] );
		}
		
		this.definitionVersionString = versionIdentifier;
		
		if( definitionSetDate != null ){
			this.definitionSetDate = (Date)definitionSetDate.clone();
		}
		else{
			this.definitionSetDate = null;
		}
		
		getCustomDefinitionsCount();
	}
	
	/**
	 * Constructs a definition set with the given date and list of definitions.
	 * @param definitionSetDate
	 * @param definitions
	 */
	public DefinitionSet( Date definitionSetDate, Vector<Definition> definitions, String version ){
		
		// 0 -- Precondition check
		if( definitions == null ){
			throw new IllegalArgumentException("The definitions given must not be null");
		}
		
		// 1 -- Add the definitions and set the parameters
		this.definitions = definitions;
		this.definitionVersionString = version;
		
		if( definitionSetDate != null ){
			this.definitionSetDate = (Date)definitionSetDate.clone();
		}
		else{
			this.definitionSetDate = null;
		}
		
		getCustomDefinitionsCount();
	}
	
	private static class DefinitionComparator implements java.util.Comparator<Definition> {
        public int compare(Definition sig1, Definition sig2) {
        	
        	int sig1Num = -1;
        	int sig2Num = -1;
        	
        	String fullname1 = sig1.getFullName();
        	String fullname2 = sig2.getFullName();
        	
        	// Find the last digit in the first number
        	for(int c = fullname1.length() - 1; c >= 0; c--){
        		if( !Character.isDigit( fullname1.charAt(c) ) ){
        			break;
        		}
        		else{
        			sig1Num = c;
        		}
        	}
        	
        	// Find the last digit in the second number
        	for(int c = fullname2.length() - 1; c >= 0; c--){
        		if( !Character.isDigit( fullname2.charAt(c) ) ){
        			break;
        		}
        		else{
        			sig2Num = c;
        		}
        	}
        	
        	// Extract the names to determine if the names are equal (sans the number at the end)
        	String nameOnly1;
        	String nameOnly2;
        	
        	if( sig1Num > -1 ){
        		nameOnly1 = fullname1.substring(0,sig1Num);
        	}
        	else{
        		nameOnly1 = fullname1;
        	}
        	
        	if( sig2Num > -1 ){
        		nameOnly2 = fullname2.substring(0,sig2Num);
        	}
        	else{
        		nameOnly2 = fullname2;
        	}
        	
        	int nameResult = nameOnly1.compareTo(nameOnly2);
        	
        	// If the names aren't equal, then just compare the names
        	if( nameResult != 0 ){
        		return nameResult;
        	}
        	
        	// Otherwise, look at the number and determine which goes first
        	else{
        		
        		// If the first has no number, then it should go first
        		if( sig1Num < 0 ){
        			return -1;
        		}
        		
        		// If the second has no number, then it should go second
        		if( sig2Num < 0 ){
        			return 1;
        		}
        		
        		// Otherwise, compare the numbers to determine which goes first
        		try{
        			int num1 = Integer.parseInt(fullname1.substring(sig1Num));
        			int num2 = Integer.parseInt(fullname2.substring(sig2Num));
        		
        			return num1 - num2;
        		}
        		catch(Exception e){
        			return sig1.getFullName().compareTo( sig2.getFullName() );
        		}
        	}
        }
	}
	
	/**
	 * The given class provides a parsed version of the identifier that describes a set of definitions. 
	 * @author Luke Murphey
	 *
	 */
	public static class DefinitionVersionID{
		private int formatID;
		private int revisionID;
		private String extendedInfo;
		private Date revisionDate;
		
		private static final String VERSION_FORMAT_REGEX = "([0-9]+)\\.([0-9]+)[ \\t]*(.*)" ;
		
		public DefinitionVersionID( String version ){
			this( version, null);
		}
		
		public DefinitionVersionID( String version, Date revisionDate ){
			
			// 0 -- Precondition check
			if( version == null ){
				throw new IllegalArgumentException("The version identifier cannot be null");
			}
			
			
			// 1 -- Parse the subcomponents
			Pattern pattern = Pattern.compile(VERSION_FORMAT_REGEX);
			Matcher matcher = pattern.matcher(version);
			
			if( matcher.matches() == false ){
				throw new IllegalArgumentException("The version identifier does not match the expected format");
			}
			
			if( matcher.groupCount() != 2 && matcher.groupCount() != 3 ){
				throw new IllegalArgumentException("The version identifier does not match the expected format");
			}
			
			formatID = Integer.parseInt(matcher.group(1));
			revisionID = Integer.parseInt(matcher.group(2));
			
			if( matcher.groupCount() == 3){
				extendedInfo = matcher.group(3);
				
				if(extendedInfo != null && extendedInfo.isEmpty() ){
					extendedInfo = null;
				}
			}
			
			// 2 -- Store the date field
			this.revisionDate = revisionDate;
		}
		
		public int formatID(){
			return formatID;
		}
		
		public int revisionID(){
			return revisionID;
		}
		
		public String extendedInfo(){
			return extendedInfo;
		}
		
		public Date getRevisionDate(){
			return revisionDate;
		}
		
		public boolean equals( DefinitionVersionID versionID ){
			if( versionID != null
					&& versionID.formatID == this.formatID
					&& versionID.revisionID == this.revisionID
					&& (
							//Make sure the extendedInfo are both null
							(versionID.extendedInfo == null && this.extendedInfo == null )
							//or they are identical 
							|| (versionID.extendedInfo != null && versionID.extendedInfo.equalsIgnoreCase(this.extendedInfo)) ) ){
				return true;
			}
			else{
				return false;
			}
		}
		
		public boolean equals( Object obj ){
			if( obj instanceof DefinitionVersionID ){
				return equals( (DefinitionVersionID)obj);
			}
			else{
				return false;
			}
		}
		
		public String toString(){
			if( extendedInfo != null && extendedInfo.length() > 0){
				return formatID + "." + revisionID + " " + extendedInfo;
			}
			else{
				return formatID + "." + revisionID;
			}
		}
	}
	
	/**
	 * Returns the definition set version ID.
	 * @return
	 */
	public DefinitionVersionID getVersionID(){
		if( definitionVersionString == null ) {
			return null;
		}
		
		return new DefinitionVersionID( definitionVersionString );
	}
	
	
	/**
	 * Gets the list of definitions sorted by the definition name.
	 * @return
	 */
	public Definition[] getDefinitionsSorted(){
		
		Vector<Definition> sortedList = new Vector<Definition>( this.definitions.size() );
		
		synchronized(this){
			sortedList.addAll(this.definitions);
		}
		
		Collections.sort(sortedList, new DefinitionComparator() );
		 
		Definition[] definitionsArray = new Definition[ sortedList.size() ];
		sortedList.toArray( definitionsArray );
		 
		return definitionsArray;
	}
	
	/**
	 * Gets the list of definitions in array format.
	 * @return
	 */
	public Definition[] getDefinitions(){

		Definition[] sigsArray = new Definition[definitions.size()];
		
		synchronized(this){
			definitions.toArray(sigsArray);
		}
		
		return sigsArray;
	}
	
	/**
	 * Get the date that the definition set was originally compiled. Note that this is date is for the official definition set,
	 * (does not represent custom rules).
	 * @return
	 */
	public Date getDefinitionSetDate(){
		
		if( definitionSetDate == null ){
			return null;
		}
		else{
			return (Date)definitionSetDate.clone();
		}
	}
	
	/**
	 * This function replicates the Element.getTextContent() method that is available in JRE 1.5 and above.
	 * @param element
	 * @return
	 */
	private static String getTextContent(Node element){
		if( element.getNodeType() == Element.DOCUMENT_NODE || element.getNodeType() == Element.DOCUMENT_TYPE_NODE || element.getNodeType() == Element.NOTATION_NODE){
			return null;
		}
		else if( element.getNodeType() == Element.TEXT_NODE || element.getNodeType() == Element.CDATA_SECTION_NODE || element.getNodeType() == Element.COMMENT_NODE || element.getNodeType() == Element.PROCESSING_INSTRUCTION_NODE){
			return element.getNodeValue();
		}
		else{
			StringBuffer buffer = new StringBuffer();
			
			NodeList nodes = element.getChildNodes();
			
			for(int c = 0; c < nodes.getLength(); c++ ){
				buffer.append( getTextContent(nodes.item(c)) );
			}
			
			return buffer.toString();
		}
	}
	
	/**
	 * Load the definitions from the XML given. This method is useful for loading new definitions from a file on disk or from a website.
	 * @param document
	 * @return
	 * @throws InvalidDefinitionException 
	 * @throws DOMException 
	 * @throws UnpurposedDefinitionException 
	 * @throws DefinitionSetLoadException 
	 */
	public static DefinitionSet loadFromXml( Document document ) throws DefinitionSetLoadException{
		
		// 0 -- Precondition Check
		if( document == null ){
			throw new IllegalArgumentException("The XML document cannot be null");
		}
		
		// 1 -- Get the definition set parameters
		Element rootElement = document.getDocumentElement();
		
		String date = rootElement.getAttribute("Date");
		String version = rootElement.getAttribute("Version");
		
		// 2 -- Load each of the definitions
		Vector<Definition> sigs = new Vector<Definition>();
		NodeList definitionNodes = rootElement.getElementsByTagName("Definition");


		for( int c = 0; c < definitionNodes.getLength(); c++){
			Element element = (Element)definitionNodes.item(c);

			String type = element.getAttribute("Type");
			Text text = (Text)element.getFirstChild();
			String code = text.getNodeValue();
			
			try{
				if( type == null ){
					throw new DefinitionSetLoadException("The type field cannot be null");
				}
				else if( type.equalsIgnoreCase("Script") ){
					Definition contentSig = ScriptDefinition.parse( code );
					sigs.add(contentSig);
				}
				else if( type.equalsIgnoreCase("Pattern") ){
					Definition contentSig = PatternDefinition.parse( code );
					sigs.add(contentSig);
				}
			}
			catch( UnpurposedDefinitionException e){
				throw new DefinitionSetLoadException("Invalid definition observed (has not purpose): " + getTextContent(element), e);
			}
			catch( InvalidDefinitionException e){
				throw new DefinitionSetLoadException("Invalid definition detected (could not be parsed): " + getTextContent(element), e);
			}
		}

		
		// 3 -- Convert the vector to an array
		Definition[] definitions = new Definition[sigs.size()];
		sigs.toArray(definitions);
		
		
		// 4 -- Create and return the definition set
		SimpleDateFormat dateFormat = new SimpleDateFormat(DEFINITION_SET_DATE_FORMAT);
		DefinitionSet sigSet;
		try{
			sigSet = new DefinitionSet( dateFormat.parse(date), definitions, version );
			return sigSet;
		}
		catch(ParseException e){
			//throw new DefinitionSetLoadException("Date field is invalid: " + date, e);
			sigSet =  new DefinitionSet( null, definitions, version );
			return sigSet;
		}
		
	}
	
	/**
	 * Gets the definition with the given definition identifier.
	 * @param id
	 * @return
	 * @throws NotFoundException
	 */
	public Definition getDefinition( int id ) throws NotFoundException{
		
		synchronized ( this ) {
			
			Iterator<Definition> iterator = definitions.iterator();
	
			while(iterator.hasNext()){
				Definition definition = iterator.next();
				if( definition.id == id ){
					return definition;
				}
			}
			
			throw new NotFoundException("No definition could be found with the given identifier");
			
		}
	}
	
	/**
	 * Gets the definition with the given definition identifier. Note that this method looks up the definition using the "local ID", or the key value used to store the entry in the database.
	 * @param id
	 * @return
	 * @throws NotFoundException
	 */
	public Definition getDefinitionByLocalID( int id ) throws NotFoundException{
		
		synchronized ( this ) {
			Iterator<Definition> iterator = definitions.iterator();
	
			while(iterator.hasNext()){
				Definition definition = iterator.next();
				if( definition.localId == id ){
					return definition;
				}
			}
			
			throw new NotFoundException("No definition could be found with the given identifier");
		}
	}
	
	/**
	 * Gets the definition with the given definition name.
	 * @param id
	 * @return
	 * @throws NotFoundException
	 */
	public Definition getDefinition( String name ) throws NotFoundException{
		
		synchronized ( this ) {
			Iterator<Definition> iterator = definitions.iterator();
	
			while(iterator.hasNext()){
				Definition definition = iterator.next();
				if( definition.getFullName().equals( name ) ){
					return definition;
				}
			}
			
			throw new NotFoundException("No definition could be found with the given identifier");
		}
	}
	
	/**
	 * Adds the definition to the active definition set and saves the new definition to the database. Returns the local ID (the identifier that identifies the definition on this system). 
	 * @param newDefinition
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public void addDefinition( Definition newDefinition ) throws SQLException, NoDatabaseConnectionException{
		
		// 0 -- Precondition Check
		if( newDefinition == null){
			throw new IllegalArgumentException("The definition to add cannot be null");
		}
		
		// 2 -- Add the definition to the active set
		synchronized (this) {
			definitions.add(newDefinition);
		}
		
		getCustomDefinitionsCount();
		
	}
	
	/**
	 * Imports the given definitions into the list of active definitions.
	 * @param input
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws UnpurposedDefinitionException 
	 * @throws InvalidDefinitionException 
	 */
	public void importPatternDefinitions( String input ) throws SQLException, NoDatabaseConnectionException, InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition[] definitions = PatternDefinition.parseAll(input);
		
		synchronized(this){
			for( int c = 0; c < definitions.length; c++){
				addDefinition( definitions[c] );
			}
		}
		
		getCustomDefinitionsCount();
	}
	
	public int getOfficialDefinitionsCount(){
		return definitions.size() - getCustomDefinitionsCount();
	}
	
	public int getCustomDefinitionsCount(){
		
		synchronized ( this ) {
			countOfCustomDefinitions = 0;

			Iterator<Definition> it = definitions.iterator();

			while( it.hasNext() ){
				Definition definition = it.next();

				if( definition.isOfficial() == false ){
					countOfCustomDefinitions++;
				}
			}
			
			return countOfCustomDefinitions;
		}
	}
	
	/**
	 * Output the definitions as an XML file.
	 * @return
	 */
	public String getAsXML(){
		return getAsXML(false);
	}
	
	/**
	 * Output the definitions as an XML file.
	 * @return
	 */
	public String getAsXML( boolean getLocalOnly ){
		
		synchronized (this) {
			
			SimpleDateFormat dateFormat = new SimpleDateFormat(DefinitionSet.DEFINITION_SET_DATE_FORMAT);
			
			StringBuffer xml = new StringBuffer();
			
			if( getLocalOnly ){
				xml.append( "<Definitions Type=\"Custom\" Date=\"" + dateFormat.format( definitionSetDate ) + "\" Version=\"" + definitionVersionString + "\" >\n" );	
			}
			else{
				xml.append( "<Definitions Date=\"" + dateFormat.format( definitionSetDate ) + "\" Version=\"" + definitionVersionString + "\" >\n" );
			}
			
			Iterator<Definition> it = definitions.iterator();
			
			while( it.hasNext() ){
				
				Definition definition = it.next();
				
				// Don't output the definition it is an official definition and only custom definitions should be included
				if( getLocalOnly == true && definition.isOfficial() ){
					continue;
				}
				
				if( definition instanceof PatternDefinition ){
					
					PatternDefinition pattern = (PatternDefinition)definition;
					
					xml.append( "<Definition Type=\"Pattern\" " + getAttributes(pattern) + ">" );
					xml.append( StringEscapeUtils.escapeXml( pattern.getRuleCode() ) );
					xml.append( "</Definition>" );
				}
				else if( definition instanceof ScriptDefinition ){
					
					ScriptDefinition script = (ScriptDefinition)definition;
					
					xml.append( "<Definition Type=\"Script\" " + getAttributes(script) + ">" );
					xml.append( StringEscapeUtils.escapeXml( script.getScript() ) );
					xml.append( "</Definition>" );
				}
			}
			
			xml.append( "</Definitions>\n" );
			
			return xml.toString();
		}
	}
	
	private static String getAttributes( Definition definition ){
		if( definition != null ){
			return "Message=\"" + StringEscapeUtils.escapeXml( definition.getMessage() ) + "\" Name=\"" + StringEscapeUtils.escapeXml( definition.getFullName() ) + "\" Severity=\"" + StringEscapeUtils.escapeXml( definition.getSeverity().toString() ) + "\" ID=\"" + definition.getID() + "\" Version=\"" + definition.getRevision() + "\"";
		}
		else{
			return "";
		}
	}
	
	/**
	 * Loads the definition set from the given file.
	 * @param file
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws DefinitionSetLoadException
	 */
	public static DefinitionSet loadFromFile( File file ) throws ParserConfigurationException, SAXException, IOException, DefinitionSetLoadException{

		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
		
		Document document = documentBuilder.parse( file );
		
		return loadFromXml(document);
		
	}
	
	/**
	 * Loads the definition set from the given string.
	 * @param file
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws DefinitionSetLoadException
	 */
	public static DefinitionSet loadFromString( String xmlString ) throws ParserConfigurationException, SAXException, IOException, DefinitionSetLoadException{

		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
		
		Document document = documentBuilder.parse( new InputSource(new StringReader(xmlString)) );
		
		return loadFromXml(document);
		
	}

	public DefinitionMatchResultSet scan(HttpResponseData httpResponse) throws ScriptException, NoDatabaseConnectionException, SQLException, NoSuchMethodException, InvalidDefinitionException{
		return scan(httpResponse, null, -1, -1);
	}
	
	/**
	 * Represents the results of a scan.
	 * @author Luke
	 *
	 */
	public static class DefinitionMatchResultSet{
		
		private Vector<DefinitionMatch> definitionMatches = new Vector<DefinitionMatch>();
		
		private Vector<URLToScan> extractedURLs = new Vector<URLToScan>();
		
		public DefinitionMatchResultSet( Vector<DefinitionMatch> definitionMatches, Vector<URLToScan> extractedURLs){
			if( definitionMatches != null) {
				this.definitionMatches.addAll(definitionMatches);
			}
			
			if( extractedURLs != null) {
				this.extractedURLs.addAll(extractedURLs);
			}
		}
		
		public Vector<DefinitionMatch> getDefinitionMatches(){
			return definitionMatches;
		}
		
		public Vector<URLToScan> getExtractedURLs(){
			return extractedURLs;
		}
		
	}
	
	/**
	 * Determines if the finding provided is included in the policy (otherwise, it is excluded and should be ignored).
	 * @param httpResponse
	 * @param definitionPolicySet
	 * @param siteGroupID
	 * @param ruleID
	 * @param definition
	 * @return
	 */
	private boolean isIncludedInPolicy( HttpResponseData httpResponse, DefinitionPolicySet definitionPolicySet, long siteGroupID, long ruleID, Definition definition){
		
		if( definitionPolicySet != null ){
			if( definitionPolicySet.getPolicyAction(siteGroupID, ruleID, definition.getName(), definition.getCategoryName(), definition.getSubCategoryName(), httpResponse.getLocation()) != DefinitionPolicyAction.EXCLUDE
				&& definitionPolicySet.getPolicyAction(siteGroupID, ruleID, definition.getName(), definition.getCategoryName(), definition.getSubCategoryName(), httpResponse.getRequestedLocation()) != DefinitionPolicyAction.EXCLUDE
			   ){
				return true;
			}
		}
		else{
			return true;
		}
		
		return false;
		
	}
	
	public DefinitionMatchResultSet scan(HttpResponseData httpResponse, DefinitionPolicySet definitionPolicySet, long siteGroupID, long ruleID) throws NoDatabaseConnectionException, SQLException, InvalidDefinitionException{
		
		synchronized ( this ) {
			
			// The list of definition matches:
			Vector<DefinitionMatch> definitionMatches = new Vector<DefinitionMatch>();
			
			// The list of URLs to scan that have been extracted:
			Vector<URLToScan> extractedURLs = new Vector<URLToScan>();
			
			// The list of variables set by the definition:
			Variables variables = new Variables();
			
			// The definition to be evaluated:
			Iterator<Definition> iterator = definitions.iterator();
			
			while( iterator.hasNext() ){
				
				Definition definition = iterator.next();
				
				Result result;
				
				try{
					// Execute if a script
					if( definition instanceof ScriptDefinition){
						
						ScriptDefinition script = (ScriptDefinition)definition;
						result = script.evaluate(httpResponse, variables, ruleID);
						
						// Add the scan result to the list if it matched and if it is not ignored by the scan policy
						if( isIncludedInPolicy(httpResponse, definitionPolicySet, siteGroupID, ruleID, script ) ){
							
							// Add the extracted URLs
							if( result.getURLs() != null ){
								extractedURLs.addAll(result.getURLs());
							}
							
							// Add the result to the list of matches if it matches
							if( result.matched() == true ){
								definitionMatches.add( new DefinitionMatch(script.getFullName(), result.getDescription(), script.severity, script.getLocalID() , result.detectStart, result.detectEnd) );
							}
						}
						
					}
					
					// Evaluate if a standard definition
					else if( definition instanceof PatternDefinition){
						
						PatternDefinition sig = (PatternDefinition)definition;
						DataSpecimen specimen = httpResponse.getDataSpecimen();
						boolean matched = false;
						
						// Evaluate the specimen if it is not null (otherwise, it will through an exception)
						if( specimen != null ){
							matched = sig.evaluate(specimen, variables);
						}
						
						// Report a result if the specimen matches and is not filtered
						if( matched == true && isIncludedInPolicy(httpResponse, definitionPolicySet, siteGroupID, ruleID, sig ) ){
							definitionMatches.add( new DefinitionMatch(sig.getFullName(), sig.getMessage(), sig.severity, sig.getLocalID()) );						
						}
						
					}
				}
				catch( InvalidDefinitionException e ){
					//Log the error and move on if requested
					Application.getApplication().logExceptionEvent( new EventLogMessage( EventLogMessage.EventType.SCAN_ENGINE_EXCEPTION, new EventLogField(FieldName.RULE_ID, ruleID) ), e);
				} catch (DefinitionEvaluationException e) {
					if( e.getDefinitionID() >= 0){
						Application.getApplication().logExceptionEvent( new EventLogMessage( EventLogMessage.EventType.SCAN_ENGINE_EXCEPTION, new EventLogField(FieldName.RULE_ID, ruleID), new EventLogField(FieldName.DEFINITION_ID, e.getDefinitionID()), new EventLogField(FieldName.DEFINITION_NAME, e.getDefinitionName()) ), e);
					}
					else{
						Application.getApplication().logExceptionEvent( new EventLogMessage( EventLogMessage.EventType.SCAN_ENGINE_EXCEPTION, new EventLogField(FieldName.RULE_ID, ruleID) ), e);
					}
				}
			}
			
			return new DefinitionMatchResultSet(definitionMatches, extractedURLs);
		}
	}
	
	/**
	 * Get a list of the definition categories.
	 * @return
	 */
	public String[] getListOfCategories(){
		synchronized ( this ) {	
			
			Vector<String> categories = new Vector<String>();
			
			for( Definition sig : this.definitions ){
				
				boolean found = false;
				
				// Determine if the category is in the list already
				for( int c = 0; c < categories.size() && found == false; c++ ){
					if( categories.get(c).equalsIgnoreCase(sig.category) ){
						found = true;
					}
				}
				
				if( found == false ){
					categories.add(sig.category);
				}
			}
			
			String[] result = new String[categories.size()];
			
			categories.toArray(result);
			
			return result;
		}
	}
	
	public static class DefinitionCategory{
		private String category;
		private String subCategory;
		
		public DefinitionCategory( String category, String subCategory ){
			this.category = category;
			this.subCategory = subCategory;
		}
		
		public String getCategory(){
			return category;
		}
		
		public String getSubCategory(){
			return subCategory;
		}
	}
	
	private static class DefinitionCategoryComparator implements java.util.Comparator<DefinitionCategory> {

		public int compare(DefinitionCategory first, DefinitionCategory second ) {
			
			if( first.getCategory().equalsIgnoreCase(second.getCategory()) ){
				return first.getSubCategory().compareTo(second.getSubCategory());
			}
			else{
				return first.getCategory().compareTo(second.getCategory());
			}
		}
		
	}
	
	/**
	 * Get a list of the definition sub-categories.
	 * @return
	 */
	public DefinitionCategory[] getListOfSubCategories(){
		
		synchronized ( this ) {
				
			Vector<DefinitionCategory> categories = new Vector<DefinitionCategory>();
			
			for( Definition sig : this.definitions ){
				
				boolean found = false;
				
				// Determine if the category is in the list already
				for( int c = 0; c < categories.size() && found == false; c++ ){
					if( categories.get(c).getCategory().equalsIgnoreCase(sig.category) && categories.get(c).getSubCategory().equalsIgnoreCase(sig.subCategory)){
						found = true;
					}
				}
				
				if( found == false ){
					categories.add( new DefinitionCategory( sig.category, sig.subCategory) );
				}
			}
			
			Collections.sort(categories, new DefinitionCategoryComparator() );
			
			DefinitionCategory[] result = new DefinitionCategory[categories.size()];
			
			categories.toArray(result);
			
			return result;
		}
	}
	
	/**
	 * Get the number of definitions in the set.
	 * @return
	 */
	public int size(){
		return definitions.size();
	}
	
	
	/**
	 * Gets the definition whose ID matches the one given. Returns null if no definition exists with the given ID.
	 * @param definitionID
	 * @return
	 */
	public Definition getByID(int definitionID){
		
		synchronized ( this ) {
			for (Definition definition : this.definitions) {
				if( definition.getID() == definitionID ){
					return definition;
				}
			}
			
			return null;
		}
	}

	/**
	 * Gets the content definition at the given location.
	 * @param c
	 * @return
	 */
	public Definition get(int c){
		return definitions.get(c);
	}
	
	/**
	 * Removes the definition at the given index from the list.
	 * @param c
	 * @return
	 */
	public Definition remove(int c){
		synchronized (this) {
			Definition def = definitions.remove(c);
			getCustomDefinitionsCount();
			return def;
		}
	}
	
	/**
	 * Removes the given definition from the list.
	 * @param c
	 * @return
	 */
	public void remove(Definition definition){
		synchronized (this) {
			definitions.remove(definition);
			getCustomDefinitionsCount();
		}
	}
	
	/**
	 * Remove the definition with the given ID.
	 * @param definitionID
	 */
	public void removeByID( int definitionID ){
		Definition def = getByID(definitionID);
		
		if( def != null ){
			remove(def);
		}
	}
	
	/**
	 * Clears all definitions from the list.
	 * @param c
	 * @return
	 */
	public void clear(){
		synchronized (this) {
			definitions.clear();
		}
	}
	
	/**
	 * Replace the definition with the given local ID with the new one provided in the argument.
	 * @param definition
	 * @param localId
	 */
	public void replaceDefinition(Definition definition, int localId){
		
		// 0 -- Precondition check
		if( definition == null ){
			throw new IllegalArgumentException("The replacement definition must not be null");
		}
		
		// 1 -- Replace the definition
		synchronized (this) {
			ListIterator<Definition> it = definitions.listIterator();
			
			
			while(it.hasNext()){
				if( it.next().localId == localId ){
					definition.localId = localId;
					it.set(definition);
				}
			}
		}
		
		getCustomDefinitionsCount();
	}
	
	/**
	 * Add the definition to the set.
	 * @param definition
	 * @throws DuplicateEntryException 
	 */
	public void add(Definition definition) throws DuplicateEntryException{
		
		synchronized (this) {
			
			// 0 -- Precondition check
			
			//	 0.1 -- Make sure the definition is not null
			if( definition == null){
				throw new IllegalArgumentException("The definition cannot be added since it is null");
			}
			
			//	 0.2 -- Make sure that the ID does not already exist
			if( getByID(definition.getID()) != null ){
				throw new DuplicateEntryException("A definition with ID " + definition.getID() + " already exists. Note that the next available ID is " + getNextOpenID(false) + ".");
			}
			
			// 1 -- Add the definition
			definitions.add(definition);
		}
	}
	
	/**
	 * Get the number of the next definition ID that is available.
	 * @param returnNextOfficial
	 * @return
	 */
	public int getNextOpenID( boolean returnNextOfficial ){
		synchronized (this) {
			
			int highestAllocated = -1;
			
			for (Definition definition : definitions) {
				if( definition.getID() > highestAllocated ){
					
					if( definition.isOfficial() && returnNextOfficial == true ){
						highestAllocated = definition.getID();
					}
					else if( definition.isOfficial() == false && returnNextOfficial == false){
						highestAllocated = definition.getID();
					}
				}
			}
			
			// Get the next ID that should be used
			if( highestAllocated <= 0){
				
				if( returnNextOfficial ){
					return 1;
				}
				else{
					return 1000000;
				}
			}
			else{
				return highestAllocated + 1;
			}
		}
	}
	
	/**
	 * Get an iterator for accessing the definitions.
	 * @return
	 */
	public Iterator<Definition> iterator(){
		return definitions.iterator();
	}
}
;