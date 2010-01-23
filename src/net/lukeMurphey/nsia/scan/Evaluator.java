package net.lukemurphey.nsia.scan;

import java.util.Vector;

public abstract class Evaluator {
	
	public enum ReturnType{
		BYTE_LOCATION, CHARACTER_LOCATION
	}
	
	public static final int UNDEFINED = -1; 
	
	/*
	 * The depth attribute allows the rule writer to specify how far into the data the system should search for the specified pattern.
	 * 
	 * Example: A depth of 5 would tell the system to only look for the specified pattern within the first 5 bytes of the data. 
	 */
	protected int depth = UNDEFINED;
	
	/*
	 * The absolute offset attribute allows the rule writer to specify where to start searching for the pattern. The absolute offset value is relative to the last end of the last evaluator
	 * or to the start of the data for the first evaluator.
	 * 
	 * Example: An offset of 5 indicates that the evaluator should start looking for the specified pattern after the first 5 bytes of the data. 
	 */
	protected int position = UNDEFINED;
	
	/*
	 * The offset attribute allows the rule writer to specify where to start searching for the pattern. The offset value is relative to the last end of the last evaluator
	 * or to the start of the data for the first evaluator.
	 * 
	 * Example: An offset of 5 indicates that the evaluator should start looking for the specified pattern after the first 5 bytes of the data. 
	 */
	protected int offset = UNDEFINED;
	
	/*
	 * The within attribute is an evaluator modifier that makes sure that at most N bytes are between pattern matches. It's designed to be used in conjunction with the offset rule option.
	 */
	protected int within = UNDEFINED;
	
	protected boolean negation = false;
	protected boolean nocase = false;
	protected boolean rawbytes = false;

	public enum OffsetRelativity{
		ABSOLUTE, RELATIVE, UNDEFINED;
	}
	
	protected OffsetRelativity offsetRelativity = OffsetRelativity.UNDEFINED;
	
	/**
	 * The depth keyword allows the rule writer to specify how far into the data the system should search for the specified pattern.
	 * 
	 * Example: A depth of 5 would tell the system to only look for the specified pattern within the first 5 bytes of the data.
	 * @param depth
	 */
	public void setDepth(int maxDepth){
		this.depth = maxDepth;
	}
	
	/**
	 * The position attribute allows the rule writer to specify where the system should start searching for the specified pattern.
	 * 
	 * Example: A position of 5 would tell the system to start looking for the attribute at byte 5. 
	 * @param position
	 */
	public void setPosition(int position){
		this.position = position;
	}
	
	/**
	 * The offset keyword allows the rule writer to specify where to start searching for the pattern. The offset value is relative to the last end of the last evaluator
	 * or to the start of the data for the first evaluator.
	 * 
	 * Example: An offset of 5 indicates that the evaluator should start looking for the specified pattern after the first 5 bytes of the data. 
	 */
	public void setOffset(int offset){
		setOffset(offset, OffsetRelativity.UNDEFINED);
	}

	/**
	 * The offset keyword allows the rule writer to specify where to start searching for the pattern. The offset value is relative to the last end of the last evaluator
	 * or to the start of the data for the first evaluator.
	 * 
	 * Example: An offset of 5 indicates that the evaluator should start looking for the specified pattern after the first 5 bytes of the data. 
	 */
	public void setOffset(int offset, OffsetRelativity offsetType){
		
		if( offsetType == null ){
			offsetRelativity = OffsetRelativity.UNDEFINED;
		}
		
		this.offsetRelativity = offsetType;
		this.offset = offset;
	}
	
	/**
	 * Indicates whether the return value will be the index of a byte matched, or a character matched.
	 * @return
	 */
	public abstract ReturnType getReturnType();
	
	/**
	 * The within keyword is an evaluator modifier that makes sure that at most N bytes are between pattern matches.
	 * It's designed to be used in conjunction with the offset rule option.
	 * @param within
	 */
	public void setWithin(int within){
		this.within = within;
	}

	public void setNegation(boolean negate){
		this.negation = negate;
	}

	protected void setIgnoreCase(boolean ignoreCase){
		nocase = ignoreCase;
	}
	
	public int getDepth(){
		return depth;
	}
	
	public int getOffset(){
		return offset;
	}

	public int getWithin(){
		return within;
	}
	
	public boolean matchWhenNotFound(){
		return negation;
	}

	protected boolean getIgnoreCase(){
		return nocase;
	}
	
	/**
	 * Method returns the first byte to be evaluated in the next evaluator.
	 * @param lastMatch The last matching of the prior evaluator, or -1 if no prior evaluator matched
	 * @return
	 */
	protected int computeStartLocation( int lastMatch ){
		
		if( offsetRelativity == OffsetRelativity.ABSOLUTE ){
			return offset;
		}
		
		int startLocation = 0;
		
		if( offset > 0 ){
			startLocation += offset;
		}
		
		if( lastMatch >= 0 ){
			
			startLocation += lastMatch + 1;//Add one so that the last character is not reevaluated
		}
		
		return startLocation;
	}
	
	public int evaluate( byte[] data ){
		return evaluate( new DataSpecimen( data ), -1 ); 
	}
	
	public int evaluate( String data ){
		return evaluate( new DataSpecimen( data ), -1 ); 
	}
	
	public int evaluate( byte[] data, int lastMatch ){
		return evaluate( new DataSpecimen( data ), lastMatch ); 
	}
	
	public int evaluate( String data, int lastMatch ){
		return evaluate( new DataSpecimen( data ), lastMatch ); 
	}
	
	/**
	 * The evaluate mathod causes the method to search the input for the data requested.
	 * If found, the method must return the location of the end of the matched pattern (the location of the last byte). Otherwise, it returns -1.
	 * @param data
	 * @return
	 */
	public int evaluate( DataSpecimen data ){
		return evaluate( data, -1 ); 
	}
	
	/**
	 * The evaluate mathod causes the method to search the input for the data requested.
	 * If found, the method must return the location of the end of the matched pattern. Otherwise, it returns -1.
	 * @param data
	 * @param lastMatch
	 * @return
	 */
	public abstract int evaluate( DataSpecimen data, int lastMatch, boolean useBasicEncoding );
	
	public int evaluate( DataSpecimen data, int lastMatch ){
		return evaluate( data, lastMatch, false );
	}
	
	/**
	 * Determines if the given rule is relative to a previous evaluator.
	 * @return
	 */
	public boolean isRelative(){
		if( offsetRelativity != OffsetRelativity.ABSOLUTE && ( getOffset() != UNDEFINED || getWithin() != UNDEFINED) ){
			return true;
		}
		else{
			return false;
		}
	}
	
	/**
	 * Determines if the given evaluator is part of a sequence of evaluators. A sequence is defined as a
	 * series of evaluators that are relative to another evaluator. This method determines if another
	 * evaluator relies on the current ones output.  
	 * @param location
	 * @param evaluators
	 * @return
	 */
	protected static boolean isSequenceStart( int location, Evaluator[] evaluators){
		
		// 0 -- Precondition Check
		if( evaluators == null ){
			throw new IllegalArgumentException("The list of evaluators must not be null");
		}
		
		
		// 1 -- Determine if the evaluator is the last one
		if( location >= evaluators.length){
			return false;
		}
		
		// 2 -- Determine if the next evaluator uses the the result of this one
		if( evaluators[location+1].isRelative() ){
			return true;
		}
		else{
			return false;
		}
	}
	
	/**
	 * Determines if the given evaluator is part of a sequence of evaluators. A sequence is defined as a
	 * series of evaluators that are relative to another evaluator. This method determines if another
	 * evaluator relies on the current ones output.  
	 * @param location
	 * @param evaluators
	 * @return
	 */
	protected static boolean isSequenceStart( int location, Vector<Evaluator> evaluators){
		
		// 0 -- Precondition Check
		if( evaluators == null ){
			throw new IllegalArgumentException("The list of evaluators must not be null");
		}
		
		
		// 1 -- Determine if the evaluator is the last one
		if( location >= evaluators.size() ){
			return false;
		}
		
		// 2 -- Determine if the next evaluator uses the the result of this one
		if( evaluators.get(location+1).isRelative() && ( !evaluators.get(location).isRelative() || ( location == 0 )) ){
			return true;
		}
		else{
			return false;
		}
	}
	
	protected static boolean isRelative( int c, Evaluator[] evaluators){
		
		// 0 -- Precondition Check
		if( evaluators == null ){
			throw new IllegalArgumentException("The list of evaluators must not be null");
		}
		
		
		// 1 -- Determine if the evaluator is the last one
		if( c >= evaluators.length){
			return false;
		}
		
		// 2 -- Determine if the next evaluator uses the the result of this one
		Evaluator evaluator = evaluators[c+1];
		if( evaluator.getOffset() != UNDEFINED || evaluator.getWithin() != UNDEFINED ){
			return true;
		}
		
		return false;
	}
	
	/**
	 * This method that gets the numerical value of the given byte, assuming the bytes are in ordered according to big-endian.
	 * @param bytes
	 * @return
	 */
	public static double getBigEndianValue(byte[] bytes){
		
		double value = 0;
		
		for(int c = 0; c < bytes.length; c++){
			value = value + getUnsignedValue( bytes[c] ) * Math.pow(256, bytes.length - c - 1);
		}
		
		return value;
	}
	
	/**
	 * This method that gets the numerical value of the given byte, assuming the bytes are in ordered according to little-endian.
	 * @param bytes
	 * @return
	 */
	public static double getLittleEndianValue(byte[] bytes){
		
		double value = 0;
		
		for(int c = 0; c < bytes.length; c++){
			value = value + getUnsignedValue( bytes[c] ) * Math.pow(256, c);
		}
		
		return value;
	}
	
	/**
	 * Retrieves the value of the given byte assuming it is unsigned. 
	 * @param value
	 * @return
	 */
	public static int getUnsignedValue( byte value ){
		if( value >= 0 ){
			return value;
		}
		else{
			return 383 + value;
		}
	}
}
