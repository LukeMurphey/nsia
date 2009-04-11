package net.lukeMurphey.nsia.scanRules;

import java.util.regex.*;

public class Pcre {
	
	private static final Pattern PCRE = Pattern.compile("/(.*)/([a-zA-Z]*)");
	
	/**
	 * This class allows a Perl-Compatible Regular Expression (PCRE) to be evaluated within Java. The parse method takes a PCRE and creates a Pattern that will match
	 * the same input as the given PCRE. Note that this method supports the following modifiers:
	 * 
	 * <ul>
	 * 	<li>PCRE_CASELESS : case insensitive match "/i"</li>
	 *  <li>PCRE_MULTILINE multiple lines match "/m"</li>
	 *  <li>PCRE_DOTALL dot matches newlines "/s"</li>
	 *  <li>PCRE_EXTENDED ignore whitespaces "/x"</li>
	 * </ul>
	**/
	
	//This class is not instantiable. Private constructor exists only to prevent instantiation. 
	private Pcre(){
		
	}
	
	/**
	 * Create a pattern that functions equivalently to the PCRE provided.
	 */
	public static Pattern parse( String pcre ){
		return parse( pcre, false );
	}
	
	/**
	 * Create a pattern that functions equivalently to the PCRE provided.
	 * @param pcre
	 * @param canonEquality This argument turns the CANON_EQ flag on for the Pattern class. This causes the Java regex engine to consider canonically equivalent characters as identical.
	 * @return
	 */
	public static Pattern parse( String pcre, boolean canonEquality ){
		// 0 -- Precondition check
		
		//	 0.1 -- The PCRE must not be null
		if( pcre == null ){
			throw new IllegalArgumentException("The PCRE must not be null");
		}
		
		//	 0.2 -- The PCRE must not be empty
		if( pcre.length() == 0 ){
			throw new IllegalArgumentException("The PCRE must not be empty");
		}
		
		//	 0.3 -- The PCRE must match a valid format
		Matcher matcher = PCRE.matcher(pcre);
		if( !matcher.find() || matcher.groupCount() < 2 ){
			throw new IllegalArgumentException("The PCRE does not appear to be valid");
		}
		
		
		// 1 -- Create the regular expression
		String options = matcher.group(2);
		String regex = matcher.group(1);

		int flags = 0;
		
		//	 1.1 -- Determine if the regex should be case sensitive
		if( options.contains("i") ){
			flags = flags | Pattern.CASE_INSENSITIVE;
		}
		
		//	 1.2 -- Determine if the regex should include newlines in the dot metacharacter
		if( options.contains("s") ){
			flags = flags | Pattern.DOTALL;
		}
		
		//	 1.3 -- Determine if the regex should be evaluated in multi-line mode
		if( options.contains("m") ){
			flags = flags | Pattern.MULTILINE;
		}
		
		//	 1.4 -- Determine if the regex should be considered extended (includes whitespace and comments)
		if( options.contains("x") ){
			flags = flags | Pattern.COMMENTS;
		}
		
		//	 1.5 -- Set the CANON_EQ flag if requested. 
		if( canonEquality ){
			flags = flags | Pattern.CANON_EQ;
		}
		
		//	 1.6 -- Create the resulting regular expression
		return Pattern.compile(regex, flags);
	}

}
