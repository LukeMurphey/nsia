package net.lukeMurphey.nsia.scanRules;

import java.util.regex.*;

public class ByteTestEvaluator extends Evaluator {

	private int length;
	private double operand;
	private Operator operator;
	private DataType dataType;
	private boolean bigEndian = true;
	private int base = 10;
	private boolean useAbsoluteValue = false;
	
	/*
	 * Unescaped regular expression:
	 * 
	 * [ ]*([0-9]+)[ ]+(digits|bytes|byte|digit)[ ]*(=|>|<=|>=|<)[ ]*([0-9]+)\s*(\(([-A-Za-z0-9, ]*)\))?
	 */
	private static final Pattern BYTE_TEST_REGEX = Pattern.compile("[ ]*([0-9]+)[ ]+(digits|bytes|byte|digit)[ ]*(=|>|<=|>=|<)[ ]*([0-9]+)\\s*(\\(([-A-Za-z0-9, ]*)\\))?", Pattern.MULTILINE);
	
	public enum DataType{
		STRING, BYTES
	}
	
	public enum Operator{
		EQUALS, GREATER_THAN, LESS_THAN, GREATER_OR_EQUAL, LESSER_OR_EQUAL
	}
	
	private ByteTestEvaluator(){
		//Only static methods within this class can instantiate this class using the parameter-less constructor 
	}
	
	public ByteTestEvaluator( int length, Operator operator, DataType dataType, double operand ) throws InvalidEvaluatorException {
		
		// 0 -- Precondition Check
		
		//	 0.1 -- The length must be greater than zero
		if( length <= 0 ){
			throw new IllegalArgumentException("The length must be greater than zero");
		}
		
		//	 0.2 -- The operator must not be null
		if( operator == null ){
			throw new IllegalArgumentException("The operator must not be null");
		}
		
		//	 0.3 -- The dataType must not be null
		if( dataType == null ){
			throw new IllegalArgumentException("The dataType must not be null");
		}
		
		
		// 1 -- Set the values 
		this.length = length;
		this.operator = operator;
		this.dataType = dataType;
		this.operand = operand;
		
		checkConfiguration();
	}
	
	@Override
	public int evaluate(DataSpecimen data, int lastMatch, boolean useBasicEncoding ) {
		
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
				
				if( useAbsoluteValue ){
					value = Math.abs(value);
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
			endLocation = Math.min(start + length, bytes.length - 1);
			
			if( start >= bytes.length ){
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
		
		
		// 2 -- Determine if the value matches the evaluautor parameters
		if( matches( value, operator, operand) ){
			return endLocation - 1;
		}
		else{
			return UNDEFINED;
		}
	}
	
	private static boolean matches( double observedValue, Operator operator, double expected ){
		
		if( operator == Operator.EQUALS ){
			return observedValue == expected;
		}
		else if( operator == Operator.GREATER_OR_EQUAL ){
			return observedValue >= expected;
		}
		else if( operator == Operator.GREATER_THAN ){
			return observedValue > expected;
		}
		else if( operator == Operator.LESS_THAN ){
			return observedValue < expected;
		}
		else if( operator == Operator.LESSER_OR_EQUAL ){
			return observedValue <= expected;
		}
		
		return false;
		
	}
	
	public static double getMaxValue( int byteCount ){
		if( byteCount <= 0){
			return 0;
		}
		
		return Math.pow(256, byteCount) - 1;
	}
	
	public static ByteTestEvaluator parse( String value ) throws InvalidEvaluatorException{
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the rule value is not null
		if( value == null ){
			throw new IllegalArgumentException("The ByteTest evaluator rule cannot be null");
		}
		
		//	 0.2 -- Make sure the rule value is not empty
		if( value.isEmpty() ){
			throw new IllegalArgumentException("The ByteTest evaluator rule cannot be empty");
		}
		
		
		// 1 -- Parse the rule
		ByteTestEvaluator byteTest = new ByteTestEvaluator();
		
		Matcher matcher = BYTE_TEST_REGEX.matcher(value);
		
		if( !matcher.find() ){
			throw new InvalidEvaluatorException("The ByteTest evaluator rule does not appear to be valid");
		}
		
		//	 1.1 -- Get the number of bytes
		try{
			byteTest.length = Integer.parseInt( matcher.group(1) );
		}
		catch(NumberFormatException e){
			throw new InvalidEvaluatorException("The ByteTest evaluator rule does not appear to be valid (number format of byte count is not valid)");
		}
		
		//	 1.2 -- Parse the data type
		if( matcher.group(2) == null ){
			throw new InvalidEvaluatorException("The ByteTest evaluator rule does not appear to be valid (missing data type)");
		}
		else if( matcher.group(2).equalsIgnoreCase("bytes") ){
			byteTest.dataType = DataType.BYTES;
		}
		else if( matcher.group(2).equalsIgnoreCase("digits") ){
			byteTest.dataType = DataType.STRING;
		}
		else if( matcher.group(2).equalsIgnoreCase("byte") ){
			byteTest.dataType = DataType.BYTES;
		}
		else if( matcher.group(2).equalsIgnoreCase("digit") ){
			byteTest.dataType = DataType.STRING;
		}
		else{
			throw new InvalidEvaluatorException("The ByteTest evaluator rule does not appear to be valid (data type is not valid)");
		}
		
		//	 1.3 -- Parse the operator
		if( matcher.group(3) == null ){
			throw new InvalidEvaluatorException("The ByteTest evaluator rule does not appear to be valid (missing operator)");
		}
		else if( matcher.group(3).equalsIgnoreCase("=") ){
			byteTest.operator = Operator.EQUALS;
		}
		else if( matcher.group(3).equalsIgnoreCase(">=") ){
			byteTest.operator = Operator.GREATER_OR_EQUAL;
		}
		else if( matcher.group(3).equalsIgnoreCase(">") ){
			byteTest.operator = Operator.GREATER_THAN;
		}
		else if( matcher.group(3).equalsIgnoreCase("<") ){
			byteTest.operator = Operator.LESS_THAN;
		}
		else if( matcher.group(3).equalsIgnoreCase("<=") ){
			byteTest.operator = Operator.LESSER_OR_EQUAL;
		}
		else{
			throw new InvalidEvaluatorException("The ByteTest evaluator rule does not appear to be valid (operator is not valid)");
		}
		
		//	 1.4 --  Parse the value operand
		try{
			byteTest.operand = Long.parseLong( matcher.group(4) );
		}
		catch( NumberFormatException e){
			throw new InvalidEvaluatorException("The ByteTest evaluator rule does not appear to be valid (number format of test value is not valid)");
		}
		
		//	 1.5 -- Parse the options
		if( matcher.group(6) != null ){
			String[] otherOptions = matcher.group(6).split(",");
			
			for( int c = 0; c < otherOptions.length; c++){
				if( otherOptions[c].trim().equalsIgnoreCase("big-endian") || otherOptions[c].trim().equalsIgnoreCase("bigendian")){
					byteTest.bigEndian = true;
				}
				else if( otherOptions[c].trim().equalsIgnoreCase("little-endian") || otherOptions[c].trim().equalsIgnoreCase("littleendian")){
					byteTest.bigEndian = false;
				}
				else if( otherOptions[c].trim().equalsIgnoreCase("hex") || otherOptions[c].trim().equalsIgnoreCase("hexadecimal")){
					byteTest.base = 16;
				}
				
				else if( otherOptions[c].trim().equalsIgnoreCase("abs") || otherOptions[c].trim().equalsIgnoreCase("absolute-value") || otherOptions[c].trim().equalsIgnoreCase("absolutevalue")){
					byteTest.useAbsoluteValue = true;
				}
			}
		}
		
		byteTest.checkConfiguration();
		
		// Return the evaluator
		return byteTest;
	}

	private void checkConfiguration() throws InvalidEvaluatorException{
		
		double maximumValuePossible;
		
		// 1 -- Derive the maximum value
		if( dataType == DataType.BYTES ){
			maximumValuePossible = getMaxValue(length);
		}
		else{
			maximumValuePossible = Math.pow(base, length);
		}
		
		
		// 2 -- Determine if the value is out of range
		if( operator == Operator.GREATER_OR_EQUAL && operand > maximumValuePossible ){
			throw new InvalidEvaluatorException("This evaluator has no purpose, it will never match the input because the length of the input is not sufficient to trigger the evaluator (the maximum possible is " + maximumValuePossible + ")");
		}
		else if( operator == Operator.GREATER_THAN && operand >= maximumValuePossible ){
			throw new InvalidEvaluatorException("This evaluator has no purpose, it will never match the input because the length of the input is not sufficient to trigger the evaluator (the maximum possible is " + maximumValuePossible + ")");
		}
		else if( operator == Operator.EQUALS && operand > maximumValuePossible ){
			throw new InvalidEvaluatorException("This evaluator has no purpose, it will never match the input because the length of the input is not sufficient to trigger the evaluator (the maximum possible is " + maximumValuePossible + ")");
		}
		else if( operator == Operator.LESS_THAN && operand > maximumValuePossible ){
			throw new InvalidEvaluatorException("This evaluator has no purpose, it will always match the input because the length of the input is not sufficient to exceed the value given (the maximum possible is " + maximumValuePossible + ")");
		}
		else if( operator == Operator.LESSER_OR_EQUAL && operand > maximumValuePossible ){
			throw new InvalidEvaluatorException("This evaluator has no purpose, it will always match the input because the length of the input is not sufficient to exceed the value given (the maximum possible is " + maximumValuePossible + ")");
		}
		
		
	}
	
	@Override
	public ReturnType getReturnType() {
		if( dataType == DataType.BYTES)
			return Evaluator.ReturnType.BYTE_LOCATION;
		else
			return Evaluator.ReturnType.CHARACTER_LOCATION;
	}

}
