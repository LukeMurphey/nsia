package net.lukemurphey.nsia.scan;

import org.apache.commons.lang.StringUtils;

public class StringEvaluator extends Evaluator {

	private String rule;
	
	public StringEvaluator( String rule, boolean ignoreCase ){
		
		// 0 -- Precondition Check
		if( rule == null ){
			throw new IllegalArgumentException("The rule must not be null");
		}
		
		if( rule.length() == 0 ){
			throw new IllegalArgumentException("The rule must not be empty");
		}
		
		// 1 -- Initialize the class
		this.rule = rule;
		super.nocase = ignoreCase;
	}
	
	public int evaluate( DataSpecimen content ){
		return evaluate( content, -1 );
	}
	
	public int evaluate( DataSpecimen content, int lastMatch, boolean useBasicEncoding  ){
		// 1 -- Compute the start offset
		int startLocation = computeStartLocation(lastMatch);
		
		int foundAt;
		
		// 2 -- Compute the maximum depth
		int maxDepth;
		String target;
		
		//	 2.1 -- Get a copy of the target; need to work out the encoding since it changes the size
		if( useBasicEncoding ){
			target = content.getBasicEncodedString();
		}
		else{
			target = content.getString();
		}
		
		/* Note: when looking for fixed length specimens, the "within" and "depth" options are essentially different ways of saying the same.
		 * One is relative to the start of the string, the other relative to the end.
		 */
		
		//	 2.2 -- If the depth is set and within is not, then compute the max depth from the start location
		if( depth > 0 && within <= 0) {
			maxDepth = depth + startLocation;
		}
		
		//	 2.3 -- If within is set and depth is not, then compute the max depth from the within location
		else if( within > 0 && depth <= 0){
			maxDepth = within + startLocation + rule.length();
		}
		
		//	 2.4 -- Take the more restrictive of the two if both are set (this is likely a syntax error)
		else if( within > 0 && depth > 0 ){
			maxDepth = Math.min(within + startLocation + rule.length(), depth + startLocation);
		}
		
		//	 2.5 -- Otherwise, look at the entire length of the string
		else{
			maxDepth = target.length();
		}
		
		// 3 -- Find the location of the rule content in the specimen
		foundAt = StringEvaluator.locationOf( target, rule, startLocation, nocase, maxDepth);
		
		if( foundAt < 0 ){
			return foundAt;
		}
		else
			return foundAt + rule.length() - 1 ;
	}
	
	public String getStringToMatch(){
		return rule;
	}
	
	/**
	 * Finds the specified String from within the String to search.
	 * @param searchIn
	 * @param searchFor
	 * @param startLocation
	 * @param ignoreCase
	 * @param maximumDepth How deep the string matched is allowed to go (based upon the end of the matched string)
	 * @return
	 */
	public static int locationOf( String searchIn, String searchFor, int startLocation, boolean ignoreCase, int maximumDepth ){
		
		// 0 -- Precondition Check
		
		//	 0.1 -- Ensure that the string to search for is not null
		if( searchFor == null ){
			throw new IllegalArgumentException("The string to search for must not be null");
		}
		
		//	 0.2 -- Ensure that the string to search for is not empty
		if( searchFor.isEmpty() ){
			throw new IllegalArgumentException("The string to search for must not be empty");
		}
		
		//	 0.3 -- Ensure that the string to search in is not null
		if( searchIn == null ){
			throw new IllegalArgumentException("The string to search in must not be null");
		}
		
		//	 0.4 --Ensure that the string to search in is not empty
		if( searchFor.isEmpty() ){
			throw new IllegalArgumentException("The string to search in must not be empty");
		}
		
		
		// 1 -- Search for the string
		
		//	 1.1 -- Case insensitive Search
		if( ignoreCase ){
			int offsetIntoSample = 0;
			
			for( int c = startLocation; c < searchIn.length(); c++){
				
				if( maximumDepth > 0 && c >= maximumDepth ){
					return UNDEFINED;
				}
				
				// Compare the current character (regardless of case)
				if( Character.toUpperCase(searchIn.charAt(c)) == Character.toUpperCase(searchFor.charAt(offsetIntoSample)) ){
					
					// Determine if the entire string has been matched
					if( offsetIntoSample >= (searchFor.length()-1) ){
						return c - searchFor.length() + 1;//Make sure to subtract the length of the originating string since the result should be the location that the string started (not ended).
					}
					else{
						offsetIntoSample++;
					}
				}
				else{
					offsetIntoSample = 0;
				}
			}
		}
		
		//	 1.2 -- Case sensitive search
		else{
			if( maximumDepth < 0){
				return StringUtils.indexOf(searchIn, searchFor, startLocation);
			}
			else{
				int result = StringUtils.indexOf(searchIn, searchFor, startLocation);
				
				if( (result + searchFor.length()) > maximumDepth ){
					return UNDEFINED;
				}
				else{
					return result;
				}
			}
		}
		
		return UNDEFINED;
	}
	
	@Override
	public ReturnType getReturnType() {
		return Evaluator.ReturnType.CHARACTER_LOCATION;
	}
}
