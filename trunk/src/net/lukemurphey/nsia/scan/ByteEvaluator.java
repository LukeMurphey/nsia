package net.lukemurphey.nsia.scan;
import java.util.regex.*;

public class ByteEvaluator extends Evaluator {
	
	private byte[] rule;
	private static final Pattern BYTE_FORMAT = Pattern.compile("[ ]?([A-Fa-f0-9]{2})[ ]?");
	
	public ByteEvaluator( byte[] rule ){
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure rule is not null
		if( rule == null ){
			throw new IllegalArgumentException("The rule must not be null");
		}
		
		//	 0.2 -- Make sure rule is not empty
		if( rule.length <= 0 ){
			throw new IllegalArgumentException("The rule must not be empty");
		}
		
		
		// 1 -- Set the rule
		this.rule = new byte[rule.length];
		System.arraycopy(rule, 0, this.rule, 0, rule.length);
	}
	
	public static ByteEvaluator parse( String rule ){
		Matcher matcher = BYTE_FORMAT.matcher( rule );
		
		byte [] bytes = new byte[128];
		int c = 0;
		
		while( matcher.find() && c < 128){
			String hexChars = matcher.group(1);
			
			//int value = getIntFromHexChar( hexChars.charAt(0), 1 ) + getIntFromHexChar( hexChars.charAt(1), 0 );
			bytes[c] = getIntFromHexChars(hexChars.charAt(1), hexChars.charAt(0));
			c++;
		}
		
		byte [] bytesFinal = new byte[c];
		
		System.arraycopy(bytes, 0, bytesFinal, 0, c);
		
		return new ByteEvaluator(bytesFinal);
	}

	private static int charToNibble ( char c )
	   {
	   if ( '0' <= c && c <= '9' )
	      {
	      return c - '0';
	      }
	   else if ( 'a' <= c && c <= 'f' )
	      {
	      return c - 'a' + 0xa;
	      }
	   else if ( 'A' <= c && c <= 'F' )
	      {
	      return c - 'A' + 0xa;
	      }
	   else
	      {
	      throw new IllegalArgumentException ( "Invalid hex character: " + c );
	      }
	   }

	private static byte getIntFromHexChars( char hexCharLow, char hexCharHigh){
		int high = charToNibble( hexCharHigh );
		int low = charToNibble( hexCharLow );

		return (byte)( ( high << 4 ) | low );
	}
	
	public byte[] getBytesToMatch(){
		byte[] copy = new byte[rule.length];
	    System.arraycopy( rule, 0, copy, 0, rule.length );
		return copy;
	}
	
	public int evaluate( DataSpecimen content, int lastMatch, boolean useBasicEncoding ){
		
		// 1 -- Compute the start offset
		int startLocation = computeStartLocation(lastMatch);
		
		/* Note: when looking for fixed length specimens, the "within" and "depth" options are essentially different ways of saying the same.
		 * One is relative to the start of the string, the other relative to the end.
		 */
		
		// 2 -- Compute the maximum depth
		int maxDepth;
		
		byte[] bytes = content.getBytes();
		
		//	 2.1 -- If the depth is set and within is not, then compute the max depth from the start location
		if( depth > 0 && within <= 0) {
			maxDepth = depth + startLocation;
		}
		
		//	 2.2 -- If within is set and depth is not, then compute the max depth from the within location
		else if( within > 0 && depth <= 0){
			maxDepth = within + startLocation + rule.length;
		}
		
		//	 2.3 -- Take the more restrictive of the two if both are set (this is likely a syntax error)
		else if( within > 0 && depth > 0 ){
			maxDepth = Math.min(within + startLocation + rule.length, depth + startLocation);
		}
		
		//	 2.4 -- Otherwise, look at the entire length of the string
		else{
			maxDepth = bytes.length;
		}
		
		// 3 -- Find the location of the rule content in the specimen
		return ByteEvaluator.locationOf(bytes, rule, startLocation, maxDepth);
	}
	
	/**
	 * Finds the specified Byte array from within the Byte array to search.
	 * @param searchIn
	 * @param searchFor
	 * @param startLocation
	 * @param maximumDepth
	 * @return
	 */
	private static int locationOf( byte[] searchIn, byte[] searchFor, int startLocation, int maximumDepth ){
		
		// 0 -- Precondition Check
		
		//	 0.1 -- Ensure that the byte array to search for is not null
		if( searchFor == null ){
			throw new IllegalArgumentException("The byte array to search for must not be null");
		}
		
		//	 0.2 -- Ensure that the byte array to search for is not empty
		if( searchFor.length == 0 ){
			throw new IllegalArgumentException("The byte array to search for must not be empty");
		}
		
		//	 0.3 -- Ensure that the byte array to search in is not null
		if( searchIn == null ){
			throw new IllegalArgumentException("The byte array to search in must not be null");
		}
		
		//	 0.4 --Ensure that the byte array to search in is not empty
		if( searchFor.length == 0 ){
			throw new IllegalArgumentException("The byte array to search in must not be empty");
		}
		
		
		// 1 -- Find the target array
		int offsetIntoSample = 0;
		
		
		for(int c = startLocation; c < searchIn.length; c++ ){
			
			if( maximumDepth > 0 && c >= maximumDepth ){
				return UNDEFINED;
			}
			
			// Compare the current byte
			if( searchFor[offsetIntoSample] == searchIn[c] ){
				
				// Determine if the entire Byte array has been matched
				if( offsetIntoSample >= (searchFor.length - 1) ){
					return c;
				}
				else{
					offsetIntoSample++;
				}
			}
			else
			{
				offsetIntoSample = 0;
			}
			
		}
		
		return UNDEFINED;
	}

	@Override
	public ReturnType getReturnType() {
		return Evaluator.ReturnType.BYTE_LOCATION;
	}

}
