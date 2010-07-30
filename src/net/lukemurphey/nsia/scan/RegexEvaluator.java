package net.lukemurphey.nsia.scan;

import java.util.regex.*;

public class RegexEvaluator extends Evaluator {

	Pattern pattern;
	
	public RegexEvaluator( String pcre ) throws InvalidEvaluatorException{
		this(pcre, true);
	}
	
	public RegexEvaluator( String pcre, boolean canonEquality ) throws InvalidEvaluatorException{
		
		// 0 -- Precondition check
		// The PCRE class peforms error checking, therefore the precondition checks will not be duplicated here since they would be redundant
		try{
			pattern = Pcre.parse(pcre, canonEquality);
		}
		catch(IllegalArgumentException e){
			throw new InvalidEvaluatorException( e.getMessage(), e);
		}
	}
	
	public int evaluate(DataSpecimen content) {
		return evaluate( content, -1 );
	}
	
	/**
	 * Causes the evaluator to look for the specifed pattern starting from the last match location. Note that the last match location may be -1 (if no previous matches exist). 
	 */
	@Override
	public int evaluate(DataSpecimen content, int lastMatch, boolean useBasicEncoding ) {
		
		// 1 -- Setup the matcher
		Matcher matcher;
		
		String contentAsString;
		
		if( useBasicEncoding)
			contentAsString = content.getBasicEncodedString();
		else
			contentAsString =  content.getString();
		
		matcher = pattern.matcher( contentAsString );
		
		// 2 -- Determine the limits of the data that the regular expression should search
		int startLocation = computeStartLocation( lastMatch );
		
		/*if( within != UNDEFINED && depth == UNDEFINED){
			matcher = matcher.region(startLocation, within + lastMatch);
		}
		else if( within != UNDEFINED && depth != UNDEFINED){
			matcher = matcher.region(startLocation, Math.min( within + lastMatch, depth + lastMatch));
		}*/
		if( depth != UNDEFINED ){
			matcher = matcher.region(startLocation, Math.min( contentAsString.length(), depth + startLocation) );
		}
		else{
			matcher = matcher.region(startLocation, contentAsString.length() );
		}
		
		if( matcher.find() ){
			
			// Make sure that the start of the finding is before the within specification
			/* Note: the Java regular expression libraries don't allow one to specify the offset by which a pattern must start by. For example, the
			 * libraries do not allow you to specify that the pattern must match within the first 4 characters of the specified region. Therefore,
			 * this will be computed afterwards by determining if the start of the match lies within the region defined by the "within" option.
			 */
			if( within != UNDEFINED && (matcher.start() - startLocation) > within ){
				return UNDEFINED;
			}
			else{
				return matcher.end() - 1;
			}
		}
		else{
			return UNDEFINED;
		}
	}
	
	@Override
	public ReturnType getReturnType() {
		return Evaluator.ReturnType.CHARACTER_LOCATION;
	}

}
