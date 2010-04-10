package net.lukemurphey.nsia.scan;

import java.util.Vector;
import java.util.regex.*;

import net.lukemurphey.nsia.MimeType;
import net.lukemurphey.nsia.Wildcard;
import net.lukemurphey.nsia.scan.Evaluator.OffsetRelativity;
import net.lukemurphey.nsia.scan.scriptenvironment.Variables;

import org.apache.commons.lang.StringUtils;

public class PatternDefinition extends Definition {


	/*
	 * This regular expression matches the NSIA rule format. The actual sub-components must be parsed separately to determine if the rule completely matches the format.
	 * 
	 * The unescaped expression is as follows:
	 * [ ]*(Block|Alert|Eval|Evaluate)[ ]*\([ ]*((([^\\"]|(\\\\)|(\\"))+)|("(([^\\"]|(\\\\)|(\\"))+)"))?[ ]*\)\s*\{((([^\\"}]|(\\\\)|(\\")|(\\[^"]))++|"(([^\\"])|(\\\\)|(\\")|(\\[^"]))++")*)\}\s*
	 * 
	 */
	private static final Pattern RULE_REGEX = Pattern.compile(
			"[ ]*(Block|Alert|Eval|Evaluate)[ ]*\\([ ]*((([^\\\\\"]|(\\\\\\\\)|(\\\\\"))*?)|(\"(([^\\\\\"]|(\\\\\\\\)|(\\\\\"))+)\"))?[ ]*\\)\\s*\\{((([^\\\\\"}]|(\\\\\\\\)|(\\\\\")|(\\\\[^\"]))++|\"(([^\\\\\"])|(\\\\\\\\)|(\\\\\")|(\\\\[^\"]))++\")*)\\}\\s*",
			Pattern.MULTILINE | Pattern.DOTALL);	
	
	/*
	 * This regular expression matches a NSIA rule option:
	 * 
	 * [ ]*((([a-zA-Z]+)[ ]*((=)[ ]*(\!)?(([^";][^;]*)|("(([^\\"]|(\\\\)*|\\"|\\[^"])+)")))?)[ ]*;?)+
	 * 
	 * 
	 * "[ ]*((([a-zA-Z]+)[ ]*((=)[ ]*(\\!)?(([^\";][^;]*)|(\"(([^\\\\\"]|(\\\\\\\\)*|\\\\\"|\\\\[^\"])+)\")))?)[ ]*;?)+"
	 */
	
	
	/*
	 * This regular expression matches the NSIA rule sub-components. The unescaped expression is as follows:
	 * 
	 * 
	 * [ ]*((([a-zA-Z]+)[ ]*((\!)?[ ]*(=)(([^";][^;]*)|("(([^\\"]|(\\\\)*|\\"|\\[^"])+)")))?)[ ]*;?)+
	 * An example that would be matched is:
	 * 
	 * Class=Exploit; Subclass=RemoteCode; NoCase; Regex!="Trees and cars";
	 * 
	 */
	private static final Pattern RULE_OPTIONS_REGEX = Pattern.compile("[ ]*((([a-zA-Z]+)[ ]*((\\!)?[ ]*(=)(([^\";][^;]*)|(\"(([^\\\\\"]|(\\\\\\\\)*|\\\\\"|\\\\[^\"])+)\")))?)[ ]*;?)+");
	
	private boolean basicEncoding = false;
	
	private Vector<Evaluator> evaluators = new Vector<Evaluator>();
	
	private Variables mustBeSet = new Variables();
	private Variables unSet = new Variables();
	private Variables toggleSet = new Variables();
	private Variables mustNotBeSet = new Variables();
	private Variables set = new Variables();
	
	private String contentTypeRestriction = null;
	private Pattern contentTypeRegex = null;
	
	private String uriRestriction = null;
	private Pattern uriRegex = null;
	
	private String code;
	
	private PatternDefinition(){
		definitionType = "ThreatPattern";
		//The default constructor is private to prevent creation without providing necessary variables 
	}
	
	public static PatternDefinition[] parseAll( String rules ) throws InvalidDefinitionException, UnpurposedDefinitionException{
				
		Matcher matcher = RULE_REGEX.matcher( rules );
		
		Vector<PatternDefinition> threatSignatures = new Vector<PatternDefinition>();
		int lastEnd = -1;
		boolean firstRun = true;
		while( matcher.find() ){
			int startOfMatch  = matcher.start();
			
			if( !firstRun ){
				
				if( lastEnd != matcher.start() ){
					String message = "Rule parse exception starting at line " + getLineNumber(rules, lastEnd+1);
					
					if(threatSignatures.size() > 0 ){
						message = message + " (successfully parsed through " +  threatSignatures.get( threatSignatures.size() - 1).getFullName() + ")";
					}
					
					throw new InvalidDefinitionException(message);
				}
			}else if( firstRun && startOfMatch != 0){
				throw new InvalidDefinitionException("Rule parse exception starting at beginning of input (line 1)");
			}
			
			lastEnd = matcher.end();
			firstRun = false;
			
			try{
				threatSignatures.add( parse(matcher.group(0)) );
			}
			catch(InvalidDefinitionException e){
				String message = "Invalid signature exception near line " + getLineNumber(rules,  matcher.start()+1) + ". " + e.getMessage() + ".";
				if(threatSignatures.size() > 0 ){
					message = message + " (successfully parsed through " +  threatSignatures.get( threatSignatures.size() - 1).getFullName() + ")";
				}
				
				throw new InvalidDefinitionException(message, e);
			}
			catch(UnpurposedDefinitionException e){
				String message = "Unpurposed rule exception near line " + getLineNumber(rules,  matcher.start()+1) + ". " + e.getMessage() + ".";
				if(threatSignatures.size() > 0 ){
					message = message + " (successfully parsed through " +  threatSignatures.get( threatSignatures.size() - 1).getFullName() + ")";
				}
				
				throw new UnpurposedDefinitionException(message);
			}
		}
		
		if( lastEnd != rules.length()){
			throw new InvalidDefinitionException("Rule parse exception at end of input, starting at line " + getLineNumber(rules, rules.length()));
		}
		
		PatternDefinition[] signaturesArray = new PatternDefinition[threatSignatures.size()];
		threatSignatures.toArray(signaturesArray);
		
		return signaturesArray;
		
	}
	
	private static int getLineNumber( String data, int characterPosition ){
		if( characterPosition < data.length()){
			data = data.substring(0, characterPosition);
		}
		
		int unixEndlines = StringUtils.countMatches(data, "\r\n");
		int macEndlines = StringUtils.countMatches(data, "\n\r");
		int windowsEndlines = StringUtils.countMatches(data, "\n");
		
		return 1 + Math.max( Math.max(unixEndlines, macEndlines), windowsEndlines);
	}
	
	public static PatternDefinition parse( String rule ) throws InvalidDefinitionException, UnpurposedDefinitionException{
		return parse(rule, -1);
	}
	
	public static PatternDefinition parse( String rule, int localID ) throws InvalidDefinitionException, UnpurposedDefinitionException{
		
		// 0 -- Precondition Check
		
		//	 0.1 -- Make sure rule is not null
		if( rule == null){
			throw new InvalidDefinitionException("The rule cannot be null");
		}
		
		//	 0.2 -- Make sure rule is not empty
		if( rule.isEmpty() ){
			throw new InvalidDefinitionException("The rule must be be empty");
		}
		
		//	 0.3 -- Make sure rule matches the specified format
		Matcher matcher = RULE_REGEX.matcher( rule );
		
		if( !matcher.find() || matcher.groupCount() < 20 ){
			throw new InvalidDefinitionException("The rule does not appear to be valid");
		}
		
		if( !matcher.matches() ){
			throw new InvalidDefinitionException("The rule does not appear to be valid");
		}

		// 1 -- Parse the rule and create the signature
		
		PatternDefinition threatSignature = new PatternDefinition();
		
		//	 1.1 -- Get the action
		String actionString = matcher.group(1).trim();
		if( actionString.equalsIgnoreCase("Alert") ){
			threatSignature.action = Action.ALERT;
		}
		else if( actionString.equalsIgnoreCase("Block") ){
			threatSignature.action = Action.BLOCK;
		}
		else if( actionString.equalsIgnoreCase("Eval") ){
			threatSignature.action = Action.EVAL;
		}
		else if( actionString.equalsIgnoreCase("Evaluate") ){
			threatSignature.action = Action.EVAL;
		}
		
		//	 1.2 -- Get the name
		if(matcher.group(8) != null){
			threatSignature.parseFullName(matcher.group(8));
		}
		else{
			threatSignature.parseFullName(matcher.group(2));
		}
		
		//	 1.3 -- Get a list of the options
		Matcher optionMatcher = RULE_OPTIONS_REGEX.matcher( matcher.group(12) );
		
		Evaluator currentEvaluator = null;
		
		while( optionMatcher.find() ){
			boolean negateValue = false;
			
			if( optionMatcher.group(5) != null && optionMatcher.group(5).equalsIgnoreCase("!") ){
				negateValue = true;
			}
			
			String value = null;
			
			// Get the value with the double-quotes stripped (if quotes exist). Note that this will be null if the value is not quoted.
			if( optionMatcher.group(10) != null ){
				value = optionMatcher.group(10);
			}
			
			//  Get the value if it exists (some options don't have values).
			else if ( optionMatcher.group(7) != null ){
				value = optionMatcher.group(7);
			}
			
			currentEvaluator = threatSignature.acceptOption( currentEvaluator, optionMatcher.group(3), value, negateValue );
			
		}
		
		//	 1.4 -- Save the code for the signature itself
		threatSignature.code = rule;
		
		threatSignature.localId = localID;
		
		// Check the configuration and make sure that the class is properly configured
		threatSignature.checkConfiguration();
		
		return threatSignature;
	}
	
	public String toString(){
		return category + "." + subCategory + "." + name;
	}
	
	public String getContentTypePattern(){
		return contentTypeRestriction;
	}
	
	public String getRuleCode(){
		return code;
	}
	
	private void checkConfiguration() throws InvalidDefinitionException, UnpurposedDefinitionException{

		// 1 -- Make sure the category name was provided
		if( category == null || category.isEmpty() ){
			throw new InvalidDefinitionException("Class name was not provided and is not optional");
		}
		
		// 2 -- Make sure the sub-category name was provided
		if( subCategory == null || subCategory.isEmpty() ){
			throw new InvalidDefinitionException("Sub-class name was not provided and is not optional");
		}
		
		// 3 -- Make sure the name was provided
		if( name == null || name.isEmpty() ){
			throw new InvalidDefinitionException("Signature name was not provided and is not optional");
		}
		
		// 4 -- Make sure the message was provided
		if( message == null || message.isEmpty() ){
			throw new InvalidDefinitionException("Message was not provided and is not optional");
		}
		
		// 5 -- Make sure the action was provided
		if( action == null ){
			throw new InvalidDefinitionException("Action was not provided name was not provided and is not optional");
		}
		
		// 6 -- Make sure the rule has a purpose
		
		//	 6.1 -- If the action is to evaluate, then the rule must set or unset a value. Otherwise, it does nothing.
		if( action != null && action == Action.EVAL && set.size() == 0 && unSet.size() == 0 ){
			throw new UnpurposedDefinitionException("This rule has no purpose. The action of this rule is to evaluate, therefore the rule must set or unset a value with a Set or an UnSet option. Otherwise, it does nothing.");
		}
		//	 6.2 -- If the rule has no evaluators and does not set or unset a value, then it serves no purpose.
		if( contentTypeRegex == null && uriRegex == null && evaluators.size() == 0 && set.size() == 0 && unSet.size() == 0 ){
			throw new UnpurposedDefinitionException("This rule does nothing since it neither sets or unsets any values and does not evaluate any input.");
		}
		
		// 7 -- Make sure that the set variable and unset variable commands are not ambiguous
		for( int c = 0; c < unSet.size(); c++){
			for( int d = 0; d < set.size(); d++){
				if( unSet.get(c).equalsIgnoreCase( set.get(d) ) )
					throw new InvalidDefinitionException("This rule contains ambiguous variables (includes 'Set' and 'UnSet' options for \"" + set.get(d) + "\")");
			}
			
			for( int d = 0; d < toggleSet.size(); d++){
				if( unSet.get(c).equalsIgnoreCase( toggleSet.get(d) ) )
					throw new InvalidDefinitionException("This rule contains ambiguous variables (includes 'Toggle' and 'UnSet' options for \"" + toggleSet.get(d) + "\")");
			}
		}
		
		for( int c = 0; c < set.size(); c++){
			
			for( int d = 0; d < toggleSet.size(); d++){
				if( set.get(c).equalsIgnoreCase( toggleSet.get(d) ) )
					throw new InvalidDefinitionException("This rule contains ambiguous variables (includes 'Toggle' and 'Set' options for \"" + toggleSet.get(d) + "\")");
			}
		}
		
		// 8 -- Make sure that the ifset variable and ifnotset variable commands are not ambiguous
		for( int c = 0; c < mustBeSet.size(); c++){
			for( int d = 0; d < mustNotBeSet.size(); d++){
				if( mustNotBeSet.get(d).equalsIgnoreCase( mustBeSet.get(c) ) )
					throw new InvalidDefinitionException("This rule contains ambiguous variable qualification (includes 'IfSet' and 'IfNOtSet' options for \"" + mustNotBeSet.get(d) + "\")");
			}
		}
		
		// 9 -- Make sure the rule does not contain mized-mode evaluators (patterns)
		if( hasMixedModeSignatures() ){
			throw new InvalidDefinitionException("This rule contains mixed-mode patterns. Patterns that operate on a byte-level cannot be combined with patterns that operator at the character level because most encodings do not have the same number of bytes as characters. Use the BasicEncoding option to analyze the data assuming a single byte of data per character.)");
		}
		
		// 10 -- Make sure the severity was set
		if( severity == Severity.UNDEFINED && action != Definition.Action.EVAL ){
			throw new InvalidDefinitionException("The severity was not defined.");
		}
		
		// 11 -- Make sure the none of the evaluators that make it obvious that they will never match
		checkForEvaluatorsWithInvalidPosition();
	}
	
	private Evaluator acceptOption( Evaluator evaluator, String option, String value, boolean negate ) throws InvalidDefinitionException{
		option = option.trim();
		
		Evaluator newEvaluator = null;
		
		if( value != null ){
			value = escapeString( value );
			
			// Message
			if( option.equalsIgnoreCase("Message") ){
				message = value;
			}
			
			// Regex evaluator
			else if( option.equalsIgnoreCase("Regex") ){
				try{
					RegexEvaluator regexEvaluator = new RegexEvaluator(value, false);
					this.evaluators.add(regexEvaluator);
					newEvaluator = regexEvaluator;
					regexEvaluator.negation = negate;
				}
				catch(PatternSyntaxException e){
					throw new InvalidDefinitionException("The regex evaluator is invalid (" + e.getMessage() + ")");
				}
				catch(InvalidEvaluatorException e){
					throw new InvalidDefinitionException("The regex evaluator is invalid (" + e.getMessage() + ")");
				}
				
				/* Set the maximum depth (if defined)
				if( this.maximumDepth > 0 ){
					newEvaluator.setDepth(this.maximumDepth);
				}*/
			}
			
			// String evaluator
			else if( option.equalsIgnoreCase("String") ){
				StringEvaluator stringEvaluator = new StringEvaluator(value, false);
				this.evaluators.add(stringEvaluator);
				newEvaluator = stringEvaluator;
				stringEvaluator.negation = negate;
				
				/* Set the maximum depth (if defined)
				if( this.maximumDepth > 0 ){
					newEvaluator.setDepth(this.maximumDepth);
				}*/
			}
			
			// Byte evaluator
			else if( option.equalsIgnoreCase("Byte") ){
				ByteEvaluator byteEvaluator = ByteEvaluator.parse(value);
				this.evaluators.add(byteEvaluator);
				newEvaluator = byteEvaluator;
				byteEvaluator.negation = negate;
				
				/* Set the maximum depth (if defined)
				if( this.maximumDepth > 0 ){
					newEvaluator.setDepth(this.maximumDepth);
				}*/
			}
			
			// ByteTest evaluator
			else if( option.equalsIgnoreCase("ByteTest") ){
				try{
					ByteTestEvaluator byteTestEvaluator = ByteTestEvaluator.parse(value);
					this.evaluators.add(byteTestEvaluator);
					newEvaluator = byteTestEvaluator;
					byteTestEvaluator.negation = negate;
				}
				catch(PatternSyntaxException e){
					throw new InvalidDefinitionException("The ByteTest evaluator is invalid (" + e.getMessage() + ")");
				}
				catch(InvalidEvaluatorException e){
					throw new InvalidDefinitionException("The ByteTest evaluator is invalid (" + e.getMessage() + ")");
				}
				
				/* Set the maximum depth (if defined)
				if( this.maximumDepth > 0 ){
					newEvaluator.setDepth(this.maximumDepth);
				}*/
			}
			
			// ByteJump evaluator
			else if( option.equalsIgnoreCase("ByteJump") ){
				try{
					ByteJumpEvaluator byteJumpEvaluator = ByteJumpEvaluator.parse(value);
					this.evaluators.add(byteJumpEvaluator);
					newEvaluator = byteJumpEvaluator;
					
					if( negate ){
						throw new InvalidDefinitionException("The ByteJump evaluator cannot be negated");
						//byteJumpEvaluator.negation = negate;
					}
				}
				catch(PatternSyntaxException e){
					throw new InvalidDefinitionException("The ByteJump evaluator is invalid (" + e.getMessage() + ")");
				}
				catch(InvalidEvaluatorException e){
					throw new InvalidDefinitionException("The ByteJump evaluator is invalid (" + e.getMessage() + ")");
				}
			}
			
			// "If set" qualifier
			else if( option.equalsIgnoreCase("IfSet") ){
				mustBeSet.set( value );
			}
			
			// "If not set" qualifier
			else if( option.equalsIgnoreCase("IfNotSet") ){
				mustNotBeSet.set( value );
			}
			
			// "unset" operator
			else if( option.equalsIgnoreCase("UnSet") ){
				unSet.set( value );
			}
			
			// "set" operator
			else if( option.equalsIgnoreCase("Set") ){
				set.set( value );
			}
			
			// "toggle" operator
			else if( option.equalsIgnoreCase("Toggle") ){
				toggleSet.set( value );
			}
			
			// Rule version
			else if( option.equalsIgnoreCase("Version") ){
				try{
					revision = Integer.parseInt(value);
				}
				catch(NumberFormatException e){
					throw new InvalidDefinitionException("The value for the version is not a valid integer (" + value + ")");
				}
			}
			
			// Rule ID
			else if( option.equalsIgnoreCase("ID") ){
				try{
					id = Integer.parseInt(value);
				}
				catch(NumberFormatException e){
					throw new InvalidDefinitionException("The value for the ID is not a valid integer (" + value + ")");
				}
				
				if( id < 1 ){
					throw new InvalidDefinitionException("The value for the ID must be greater than zero");
				}
			}
			
			// Reference
			else if( option.equalsIgnoreCase("Reference") ){
				Reference reference = Reference.parse(value);
				references.add( reference );
			}
			
			// Depth option
			else if( option.equalsIgnoreCase("Depth") ){
				
				int depth = -1;
				
				try{
					depth  = Integer.parseInt(value);
				}
				catch(NumberFormatException e){
					throw new InvalidDefinitionException("The value for the depth option is not a valid integer (" + value + ")");
				}
				
				if( evaluator == null ){
					throw new InvalidDefinitionException("The depth option can only be used after an evaluator has been defined");
				}
				else if( evaluator.depth > 0 ){
					throw new InvalidDefinitionException("The depth option is ambiguously defined (set more than once for the same evaluator)");
				}
				else if( depth <= 0 ){
					throw new InvalidDefinitionException("The depth option must be greater than zero (otherwise the signature evaluates no data)");
				}
				else{
					evaluator.setDepth( depth );
				}
				
			}
			
			// Within option
			else if( option.equalsIgnoreCase("Within") ){
				int within = -1;
				
				try{
					within  = Integer.parseInt(value);
				}
				catch(NumberFormatException e){
					throw new InvalidDefinitionException("The value for the within option is not a valid integer (" + value + ")");
				}
				
				if( evaluator == null ){
					throw new InvalidDefinitionException("The within option can only be used after an evaluator has been defined");
				}
				else if( within <= 0 ){
					throw new InvalidDefinitionException("The within option must be greater than zero (otherwise the signature can never match)");
				}
				else {
					evaluator.setWithin(within);
				}
			}
					
			// Offset option
			else if( option.equalsIgnoreCase("Offset") ){
				Offset offset = new Offset(value);
				
				/*try{
					offset  = Integer.parseInt(value);
				}
				catch(NumberFormatException e){
					throw new InvalidSignatureException("The value for the offset option is not a valid integer (" + value + ")");
				}*/
				
				if( evaluator == null ){
					throw new InvalidDefinitionException("The offset option can only be used after an evaluator has been defined");
				}
				else if(offset.getOffsetType() == OffsetRelativity.RELATIVE && this.evaluators.size() < 2){
					throw new InvalidDefinitionException("A relative offset was specified, however, the given evaluator has no other evaluators to be relative to. Specify an absolute offset if you want the offset to be relative to beginning of the data.");
				}
				else {
					evaluator.setOffset(offset.getValue(), offset.getOffsetType());
				}
			}
			
			//Severity field
			else if( option.equalsIgnoreCase("Severity") ){
				if( value.equalsIgnoreCase("low") ){
					severity = Severity.LOW;
				}
				else if( value.equalsIgnoreCase("medium") || value.equalsIgnoreCase("med") ){
					severity = Severity.MEDIUM;
				}
				else if( value.equalsIgnoreCase("high") ){
					severity = Severity.HIGH;
				}
				else{
					throw new InvalidDefinitionException("The severity level given (" + value + ") is invalid");
				}
			}
			
			//Content-Type field
			else if( option.equalsIgnoreCase("ContentType") ){
				
				contentTypeRestriction = value;
				try{
					if( contentTypeRestriction == null ){
						throw new InvalidDefinitionException("The content-type was not specified");
					}
					else if( contentTypeRestriction.startsWith("/") ){
						contentTypeRegex = Pcre.parse(contentTypeRestriction);
					}
					else{
						Wildcard wildcard = new Wildcard(contentTypeRestriction, true);
						contentTypeRegex = wildcard.getPattern();
					}
				}
				catch(PatternSyntaxException e){
					throw new InvalidDefinitionException("The content-type evaluator is invalid (" + e.getMessage() + ")");
				}
				
			}
			
			//URI field
			else if( option.equalsIgnoreCase("URI") ){
				
				try{
					uriRestriction = value;
					
					if( uriRestriction == null ){
						throw new InvalidDefinitionException("The URI was not specified");
					}
					else if( uriRestriction.startsWith("/") ){
						uriRegex = Pcre.parse(uriRestriction);
					}
					else{
						Wildcard wildcard = new Wildcard(uriRestriction, true);
						uriRegex = wildcard.getPattern();
					}
				}
				catch(PatternSyntaxException e){
					throw new InvalidDefinitionException("The URI option is invalid (" + e.getMessage() + ")");
				}
				
			}
			
			//This line accepts any option that is invalid/undefined
			else{
				throw new InvalidDefinitionException("The following option is invalid: " + option);
			}

		}
		else{
			
			//BasicEncoding
			if( option.equalsIgnoreCase("BasicEncoding") ){
				basicEncoding = true;
			}
			
			//IgnoreCase
			else if( option.equalsIgnoreCase("IgnoreCase") || option.equalsIgnoreCase("NoCase") ){
				if( evaluator != null ){
					try{
						StringEvaluator stringEvaluator = (StringEvaluator) evaluator;
						stringEvaluator.setIgnoreCase(true);
					}
					catch(ClassCastException e){
						throw new InvalidDefinitionException("The IgnoreCase option can only be used with the String evaluator");
					}
				}
				else{
					throw new InvalidDefinitionException("The IgnoreCase option can only be used after a String evaluator has been defined");
				}
			}
			
			// IsDataAt evaluator
			else if( option.equalsIgnoreCase("IsDataAt") ){
				IsDataAtEvaluator isDataAtEvaluator = new IsDataAtEvaluator();
				this.evaluators.add(isDataAtEvaluator);
				newEvaluator = isDataAtEvaluator;
				isDataAtEvaluator.negation = negate;
			}
			
			// This line accepts any option that is invalid/undefined
			else{
				throw new InvalidDefinitionException("The following option is invalid: " + option);
			}
		}
		
		if( newEvaluator != null ){
			return newEvaluator;
		}
		else{
			return evaluator;
		}
	}

	/**
	 * This class parses an offset option and provides the offset value and type.
	 * @author Luke Murphey
	 *
	 */
	private static class Offset{
		
		/*
		 * This regular expression matches the offset value which can be provided in multiple formats.
		 */
		private static final Pattern OFFSET_REGEX = Pattern.compile("[ ]*([0-9]+)[ ]*(\\([ ]*((relative)|(absolute))[ ]*\\))?[ ]*");
		
		private int offset;
		private OffsetRelativity offsetType = OffsetRelativity.UNDEFINED;
		
		public Offset( String value ) throws InvalidDefinitionException{
			parse( value );
		}
		
		private void parse(String value) throws InvalidDefinitionException{
			Matcher matcher = OFFSET_REGEX.matcher(value);
			
			if( matcher.matches() ){
				String num = matcher.group(1);
				String type = matcher.group(3);
				
				try{
					offset = Integer.parseInt(num);
				}
				catch(NumberFormatException e){
					throw new InvalidDefinitionException("The offset value provided is not a valid number: " + num);
				}
				
				if( type != null && "relative".equalsIgnoreCase(type)){
					offsetType = OffsetRelativity.RELATIVE;
				}
				else if( type != null && "absolute".equalsIgnoreCase(type)){
					offsetType = OffsetRelativity.ABSOLUTE;
				}
				
			}
			else{
				throw new InvalidDefinitionException("The offset option provided is not valid: " + value);
			}
		}
		
		public int getValue(){
			return offset;
		}
		
		public OffsetRelativity getOffsetType(){
			return offsetType;
		}
	}
	
	private static String escapeString(String value){
		
		String result;
		
		result = StringUtils.replace( value, "\\\\", "\\");
		result = StringUtils.replace( result, "\\\"", "\"");
		
		return result;
	}
	
	public void setName( String name ) {
		this.name = name;
	}
	
	public void setAction( Action action ) {
		this.action = action;
	}

	public void setNotes( String notes) {
		this.message = notes;
	}

	public void setTypeName( String primaryName ) {
		this.category = primaryName;
	}

	public void setSubTypeName( String secondaryName ) {
		this.subCategory = secondaryName;
	}
	
	public void addEvaluator( Evaluator evaluator ) {
		evaluators.add(evaluator);
	}
	
	/*public boolean evaluate( DataSpecimen data, Variables variables ){
		//return evaluate(data.getBytes(), variables, data.)
	}*/
	
	public boolean evaluate( String input, Variables variables ) throws InvalidDefinitionException{
		return evaluate( new DataSpecimen(input), variables );
	}
	
	public boolean evaluate( byte[] input, Variables variables ) throws InvalidDefinitionException{
		return evaluate( new DataSpecimen(input), variables );
	}
	
	public boolean evaluate( byte[] input, Variables variables, String filename, String suggestedContentType ) throws InvalidDefinitionException{
		
		// 1 -- Determine if the content-type matches
		if( contentTypeRegex != null ){ //Skip this check if the content-type is null (means there is no content-type restriction).
			String mimeType = MimeType.getMimeType(input, filename, suggestedContentType);
			
			if( mimeType != null ){
				Matcher matcher = contentTypeRegex.matcher(mimeType);
				
				if(!matcher.matches()){ //Return false if the content-type does not match
					return false;
				}
			}
		}
		
		return evaluate( new DataSpecimen(input), variables );
	}
	
	private void checkForEvaluatorsWithInvalidPosition() throws InvalidDefinitionException{
		for(int c = 0; c < evaluators.size(); c++){
			if( evaluators.get(c) instanceof ByteEvaluator){
				checkForEvaluatorWithInvalidPosition(evaluators.get(c), ((ByteEvaluator) evaluators.get(c)).getBytesToMatch().length, c);
			}
			else if( evaluators.get(c) instanceof StringEvaluator){
				checkForEvaluatorWithInvalidPosition(evaluators.get(c), ((StringEvaluator) evaluators.get(c)).getStringToMatch().length(), c);
			}
		}
	}
	
	private void checkForEvaluatorWithInvalidPosition( Evaluator eval, int specimenLength, int index ) throws InvalidDefinitionException{
		int depth = eval.getDepth();
		
		if( depth < 0 ){
			return;
		}
		
		if( depth < specimenLength ){
			throw new InvalidDefinitionException("The evaluator at position " + index + " has a depth limit that will not allow the signature to ever match. The depth limit of " + depth + " will not allow an evaluator of length " + specimenLength + " to ever match.");
		}
	}
	
	private boolean hasMixedModeSignatures(){
		
		if( basicEncoding ){
			return false;
		}
		
		Evaluator.ReturnType returnType = null;
		
		for( int c = 0; c < evaluators.size(); c++){
			
			if( c == 0 || !evaluators.get(c).isRelative() ){
				returnType = evaluators.get(c).getReturnType();
			}
			else if(returnType != evaluators.get(c).getReturnType() ){
				return true;
			}
			
		}
		
		return false;
	}
	
	private boolean evaluateFromPosition( int evaluatorIndex, DataSpecimen input ) throws InvalidDefinitionException{
		
		// 1 -- Determine the maximum number of bytes/characters to evaluate
		int position = Evaluator.UNDEFINED;
		int length;
		int currentEvaluatorIndex;
		
		
		// 2 -- try to match each of the evaluators in the chain
		Evaluator startEvaluator = evaluators.get(evaluatorIndex);
		Evaluator.ReturnType returnType = startEvaluator.getReturnType();
		
		if( returnType == Evaluator.ReturnType.BYTE_LOCATION ){
			length = input.getBytesLength();
		}
		else{
			length = input.getStringLength();
		}
		
		
		//	 2.1 -- Try each of the positions to see if a rule match is possible
		while( position < (length -1) ){
			
			//Start by evaluating the first rule
			position = startEvaluator.evaluate(input, position, basicEncoding);
			
			// Determine if the first match and return if it did not
			// Was not found...
			if( position <= Evaluator.UNDEFINED && startEvaluator.matchWhenNotFound() == false ){
				return false;
			}
			// Was found, but is not a match if it was (since it was negated)...
			else if( position > Evaluator.UNDEFINED && startEvaluator.matchWhenNotFound() == true ){
				return false;
			}
			
			// The code below this point will only be executed if the first rule matched something
			currentEvaluatorIndex = evaluatorIndex + 1;
			boolean continueEvaluatingRelativeRules = true;
			int priorFinding = position;
			
			while( continueEvaluatingRelativeRules ){
				
				// Determine if this the last rule in the chain
				if( currentEvaluatorIndex >= evaluators.size() ){
					return true; //The rule was matched above, and there are no rules relative to this one 
				}
				
				Evaluator nextEvaluator = evaluators.get(currentEvaluatorIndex);
				
				if( !nextEvaluator.isRelative() ){
					return true; //The rule was matched above, and there are no rules relative to this one
				}
				
				if( basicEncoding == false && nextEvaluator.getReturnType() != returnType ){
					throw new InvalidDefinitionException("This signature contains mixed byte-mode and character-mode rules");
				}
				
				priorFinding = nextEvaluator.evaluate(input, priorFinding);
				
				// Was not found...
				if( priorFinding <= Evaluator.UNDEFINED && nextEvaluator.matchWhenNotFound() == false ){
					continueEvaluatingRelativeRules = false;
				}
				// Was found, but is not a match if it was (since it was negated)...
				else if( priorFinding > Evaluator.UNDEFINED && nextEvaluator.matchWhenNotFound() == true ){
					continueEvaluatingRelativeRules = false;
				}
				
				currentEvaluatorIndex++;
			}
			
			
			//Start at the next position
			position++;
		}
		
		return false;
	}
	
	public boolean evaluate( DataSpecimen input, Variables variables ) throws InvalidDefinitionException{
		
		// 0 -- Precondition
		
		//	 0.1 -- Make sure input is not null
		if( input == null ){
			throw new IllegalArgumentException("The data specimen must not be null");
		}
		
		
		// 1 -- Check the variables to determine if the rule should even be evaluated
		
		//	 1.1 -- Check for the variables that must have been set already
		boolean variableSet = false;
		
		if( mustBeSet.size() == 0){
			variableSet = true;
		}
		else{
			for(int c = 0; c < mustBeSet.size(); c++ ){
				if( variables.isSet( mustBeSet.get(c) ) ){
					variableSet = true;
				}
			}
		}
		
		if( !variableSet ){
			return false;
		}
		
		//	 1.2 -- Check for the variables that must not have been set already
		if( mustNotBeSet.size() != 0)
		{
			for(int c = 0; c < mustNotBeSet.size(); c++ ){
				if( variables.isSet( mustNotBeSet.get(c) ) ){
					return false;
				}
			}
		}
		
		
		// 2 -- Determine if the content-type matches
		if( contentTypeRegex != null ){ //Skip this check if the content-type is null (means there is no content-type restriction).
			String mimeType = input.getContentType();
			
			if( mimeType != null ){
				Matcher matcher = contentTypeRegex.matcher(mimeType);
				
				if(!matcher.matches()){ //Return false if the content-type does not match
					return false;
				}
			}
		}
		
		// 3 -- Determine if the URI matches
		if( uriRegex != null ){ //Skip this check if the URI is null (means there is no URI restriction).
			String uri = input.getFilename();
			
			if( uri != null ){
				Matcher matcher = uriRegex.matcher(uri);
				
				if(!matcher.matches()){ //Return false if the uri does not match
					return false;
				}
			}
		}
		
		// 4 -- Analyze the data using the evaluator set
		for( int c = 0; c < evaluators.size(); c++){
			
			//Try to match the rule with the content, return if no match could be found
			if( c == 0 || !evaluators.get(c).isRelative() ){
				
				if( evaluateFromPosition(c, input) == false ){
					return false;
				}
			}
		}
		
		
		/* Determine if the rule has not yet rejected simply because of insufficient information. The rule evaluation engine will not reject
		 * the specimen if it doesn't contain a URI or a content-type. Instead, it simply skips the URI and/or content-type checks if they
		 * are not provided (the alternative would be to pass all content that didn't provide a URI or content-type).
		 * 
		 * However, the system needs to determine if the content was not cleared just because it didn't have the required data. Therefore,
		 * the test below will determine if the content was not cleared just because it doesn't have an evaluators and didn't provide a URI
		 * and/or content-type 
		 */
		if( evaluators.size() == 0 ){
			
			// If the signature has both URI and content-type checks (but no content evaluators), then accept if the content-type and filename are not set 
			if( contentTypeRegex != null && uriRegex != null && (input.getContentType() == null || input.getFilename() == null) ){
				return false;
			}
			// The rule has a content-type check but no filename was provided
			else if( contentTypeRegex != null && input.getContentType() == null ){
				return false;
			}
			// The rule has a URI check but no URI (filename) was provided
			else if( uriRegex != null && input.getFilename() == null ){
				return false;
			}
			
		}
		
		// Note: execution will only continue beyond this point if the rule matched. The code below assumes that the rule matches the input. 
		
		
		// 5 -- Modify the variables list as appropriate
		
		//	 5.1 -- Unset the variables that must be unset
		for( int c = 0; c < unSet.size(); c++ ){
			variables.unSet(unSet.get(c));
		}
		
		//	 5.2 -- Set the variables that must be set
		for( int c = 0; c < set.size(); c++ ){
			variables.set(set.get(c));
		}
		
		//	 5.3 -- Set/UnSet the variables that must be toggled
		for( int c = 0; c < toggleSet.size(); c++ ){
			
			if( variables.isSet( toggleSet.get(c) ) ){
				variables.unSet( toggleSet.get(c) );
			}
			else{
				variables.set( toggleSet.get(c) );
			}
		}
		
		return true;
	}

}
