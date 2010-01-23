package net.lukemurphey.nsia.scan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 
 * ByteJump = 4 digits (hex, big-endian, align) x 4
 * @author Luke Murphey
 *
 */
public class ByteJumpEvaluator extends Evaluator {

	private int length = 0;
	private int alignment = 1;
	private int base = 10;
	private DataType dataType; 
	private boolean bigEndian = true;
	private int multiplier = 1;
	//private boolean useAbsoluteValue = false;
	
	/*
	 * Unescaped regular expression:
	 * 
	 * [ ]*([0-9]+)[ ]+(digits|bytes|byte|digit)\s*(\(([-A-Za-z0-9, ]*)\))?\s*(\s*x\s*[0-9]+\s*)?
	 */
	private static final Pattern BYTE_JUMP_REGEX = Pattern.compile("[ ]*([0-9]+)[ ]+(digits|bytes|byte|digit)\\s*(\\(([-A-Za-z0-9, ]*)\\))?\\s*(\\s*x\\s*([0-9]+)\\s*)?", Pattern.MULTILINE);
	
	
	public enum DataType{
		STRING, BYTES
	}
	
	public static ByteJumpEvaluator parse( String value ) throws InvalidEvaluatorException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the rule value is not null
		if( value == null ){
			throw new IllegalArgumentException("The ByteJump evaluator rule cannot be null");
		}
		
		//	 0.2 -- Make sure the rule value is not empty
		if( value.isEmpty() ){
			throw new IllegalArgumentException("The ByteJump evaluator rule cannot be empty");
		}
		
		
		// 1 -- Parse the rule
		ByteJumpEvaluator byteJump = new ByteJumpEvaluator();
		
		Matcher matcher = BYTE_JUMP_REGEX.matcher(value);
		
		if( !matcher.find() ){
			throw new InvalidEvaluatorException("The ByteJump evaluator rule does not appear to be valid");
		}
		
		//	 1.1 -- Get the number of bytes
		try{
			byteJump.length = Integer.parseInt( matcher.group(1) );
		}
		catch(NumberFormatException e){
			throw new InvalidEvaluatorException("The ByteJump evaluator rule does not appear to be valid (number format of byte count is not valid)");
		}
		
		//	 1.2 -- Parse the data type
		if( matcher.group(2) == null ){
			throw new InvalidEvaluatorException("The ByteJump evaluator rule does not appear to be valid (missing data type)");
		}
		else if( matcher.group(2).equalsIgnoreCase("bytes") ){
			byteJump.dataType = DataType.BYTES;
		}
		else if( matcher.group(2).equalsIgnoreCase("digits") ){
			byteJump.dataType = DataType.STRING;
		}
		else if( matcher.group(2).equalsIgnoreCase("byte") ){
			byteJump.dataType = DataType.BYTES;
		}
		else if( matcher.group(2).equalsIgnoreCase("digit") ){
			byteJump.dataType = DataType.STRING;
		}
		else{
			throw new InvalidEvaluatorException("The ByteJump evaluator rule does not appear to be valid (data type is not valid)");
		}

		//	 1.3 -- Parse the options
		if( matcher.group(4) != null ){
			String[] otherOptions = matcher.group(4).split(",");
			
			for( int c = 0; c < otherOptions.length; c++){
				if( otherOptions[c].trim().equalsIgnoreCase("big-endian") || otherOptions[c].trim().equalsIgnoreCase("bigendian")){
					byteJump.bigEndian = true;
				}
				else if( otherOptions[c].trim().equalsIgnoreCase("little-endian") || otherOptions[c].trim().equalsIgnoreCase("littleendian")){
					byteJump.bigEndian = false;
				}
				else if( otherOptions[c].trim().equalsIgnoreCase("hex") || otherOptions[c].trim().equalsIgnoreCase("hexadecimal")){
					byteJump.base = 16;
				}
				else if( otherOptions[c].trim().equalsIgnoreCase("oct") || otherOptions[c].trim().equalsIgnoreCase("octal")){
					byteJump.base = 8;
				}
				/*else if( otherOptions[c].trim().equalsIgnoreCase("abs") || otherOptions[c].trim().equalsIgnoreCase("absolute-value") || otherOptions[c].trim().equalsIgnoreCase("absolutevalue")){
					byteJump.useAbsoluteValue = true;
				}*/
				else if( otherOptions[c].trim().equalsIgnoreCase("align") || otherOptions[c].trim().equalsIgnoreCase("align-4") || otherOptions[c].trim().equalsIgnoreCase("align4") ){
					byteJump.alignment = 4;
				}
				else if( otherOptions[c].trim().equalsIgnoreCase("align8") || otherOptions[c].trim().equalsIgnoreCase("align-8") ){
					byteJump.alignment = 8;
				}
				else{
					//Unacceptable option
					throw new InvalidEvaluatorException("The ByteJump evaluator has an invalid option (" + otherOptions[c].trim() + ")");
				}
			}
		}
		
		// 1.4 -- Parse the multiplier
		if( matcher.group(6) != null ){
			try{
				byteJump.multiplier = Integer.valueOf( matcher.group(6) );
			}
			catch(NumberFormatException e){
				throw new InvalidEvaluatorException("The ByteJump evaluator has an invalid multiplier (" + matcher.group(6) + ")");
			}
		}
		
		byteJump.checkConfiguration();
		
		// Return the evaluator
		return byteJump;
	}
	
	public int getAlignment(){
		return alignment;
	}
	
	public int getBase(){
		return base;
	}
	
	public boolean isBigEndian(){
		return bigEndian;
	}
	
	public int getMultiplier(){
		return multiplier;
	}
	
	private void checkConfiguration() throws InvalidEvaluatorException{
		
		
	}
	
	@Override
	public int evaluate(DataSpecimen data, int lastMatch,
			boolean useBasicEncoding) {
		
		
		int start = computeStartLocation(lastMatch);
		double value;
		int endLocation;
		
		// 1 -- Get the value of the bytes
		
		//	 1.1 -- Evaluate the data as a string
		if(dataType == DataType.STRING){
			
			// 1.1.1 -- Strip out the data
			String dataString;
			
			if( useBasicEncoding ){
				dataString = data.getBasicEncodedString();
			}
			else{
				dataString = data.getString();
			}
			
			// 1.1.2 -- Compute the end of the number to evaluate
			endLocation = Math.min(dataString.length(), start + length );
			
			String number = dataString.substring(start, endLocation);
			
			// 1.1.3 -- Convert the number into the final value based upon the base selected 
			try{
				if( base == 16 ){
					value = Integer.valueOf(number, 16).intValue();
				}
				else if( base == 8){
					value = Integer.valueOf(number, 8).intValue();
				}
				else{
					value = Double.parseDouble(number);
				}
				
				//Do not allow the bytejump evaluator to go backwards in the packet (otherwise an infinite loop may ensue)
				if( value < 0 ){
					return UNDEFINED;
				}
			}
			catch(NumberFormatException e){
				// Number is invalid, indicate that a match did not occur
				return UNDEFINED;
			}
		}
		
		// 1.2 -- Evaluate the data as raw bytes
		else{
			
			byte[] bytes = data.getBytes();
			
			// 1.2.1 -- Don't attempt to get the value of the bytes if the start location is outside of the data boundary
			if( start >= bytes.length ){
				return UNDEFINED;
			}
			
			// 1.2.2 -- Stop if we have gone off of the end of the array
			// The minimum method call is designed to ensure that the evaluator does not go off the end of the array
			endLocation = start + length;
			
			if( (endLocation) >= bytes.length ){
				return UNDEFINED;
			}

			// 1.2.3 -- Get the bytes to analyze
			byte[] bytesToBeAnalyzed = new byte[ endLocation - start ];
			
			System.arraycopy(bytes, start, bytesToBeAnalyzed, 0, endLocation - start);
			
			// 1.2.4 -- Compute the resulting number
			if( bigEndian ){
				value = getBigEndianValue(bytesToBeAnalyzed);
			}
			else{
				value = getLittleEndianValue(bytesToBeAnalyzed);
			}
		}
		
		// 2 -- Compute the resulting jump to location
		int jumpToValue = 0;
		jumpToValue = (int) ( (multiplier * value) + endLocation );
		
		if( alignment > 1 ){
			int add = alignment - (jumpToValue % alignment);
			jumpToValue += add;
		}
		
		// 3 -- Determine if the value matches the evaluator parameters
		
		//	 3.1 -- Indicate a match failure if the location to jump to off the end of the packet
		if( jumpToValue > data.getBytesLength()){
			return UNDEFINED;
		}
		else{
			return jumpToValue;
		}
	}

	@Override
	public ReturnType getReturnType() {
		return ReturnType.BYTE_LOCATION;
	}

}
