package net.lukeMurphey.nsia.testCases;

import java.io.UnsupportedEncodingException;

import net.lukeMurphey.nsia.scanRules.DataSpecimen;
import net.lukeMurphey.nsia.scanRules.InvalidEvaluatorException;
import net.lukeMurphey.nsia.scanRules.InvalidDefinitionException;
import net.lukeMurphey.nsia.scanRules.RegexEvaluator;
import net.lukeMurphey.nsia.scanRules.UnpurposedDefinitionException;
import junit.framework.TestCase;

public class RegexEvaluatorTest extends TestCase {

	
	public void testBasicRegex() throws InvalidEvaluatorException{
		
		RegexEvaluator regexEval = new RegexEvaluator("/apple/i");
		
		int result = regexEval.evaluate( "0123456789aPple");
		
		if( result != 14 ){
			fail("Regex evaluator failed to find string at position 14 (returned " + result + ")");
		}
		
	}
	
	public void testUTF16() throws UnsupportedEncodingException, InvalidEvaluatorException{
		DataSpecimen data = new DataSpecimen("012aPple1234".getBytes("UTF-16"));
		
		RegexEvaluator regexEval = new RegexEvaluator("/apple/i");
		
		int result = regexEval.evaluate( data );
		
		if( result != 8 ){
			fail("Regex evaluator failed to find string at position 24 (returned " + result + ")");
		}
		
	}
	
	
	
	public void testHexNoopLookup() throws InvalidDefinitionException, UnpurposedDefinitionException, UnsupportedEncodingException, InvalidEvaluatorException{
		
		RegexEvaluator regexEval = new RegexEvaluator("/\\x90/");
		
		byte[] bytes = new byte[]{ 65, 66, 67, (byte)0x90, (byte)0x80, (byte)0x00, (byte)0x09};
		int result = regexEval.evaluate( new DataSpecimen(bytes, true) );
		
		if( result != 3 ){
			fail("Regex evaluator failed to detect value at position 4, (returned " + result + ")");
		}
		
	}
	
	public void testHexNoopLookup2() throws InvalidDefinitionException, UnpurposedDefinitionException, UnsupportedEncodingException, InvalidEvaluatorException{
		
		RegexEvaluator regexEval = new RegexEvaluator("/\\x90/");
		
		byte[] bytes = new byte[]{ 65, 66, 67, (byte)0x90, (byte)0x80, (byte)0x00, (byte)0x09};
		int result = regexEval.evaluate( new DataSpecimen(bytes, true));
		
		if( result != 3 ){
			fail("Regex evaluator failed to detect value at position 4, (returned " + result + ")");
		}
		
	}
	
	public void testRawHexLookup() throws InvalidDefinitionException, UnpurposedDefinitionException, UnsupportedEncodingException, InvalidEvaluatorException{
		
		RegexEvaluator regexEval = new RegexEvaluator("/ABC\\x90/");
		
		byte[] bytes = new byte[]{ 97, 98, 65, 66, 67, (byte)0x90, (byte)0x90 };
		DataSpecimen data = new DataSpecimen(bytes);
		int result = regexEval.evaluate(data);
		
		if( result != 5 ){
			fail("Regex evaluator failed to detect value at position 4, (returned " + result + ")");
		}

	}
	
	public void testUncodeHexLookup() throws InvalidDefinitionException, UnpurposedDefinitionException, UnsupportedEncodingException, InvalidEvaluatorException{
		
		RegexEvaluator regexEval = new RegexEvaluator("/ABC\\u0041/i");
		
		byte[] bytes = "ABCA".getBytes("UTF-16");
		DataSpecimen data = new DataSpecimen(bytes);
		int result = regexEval.evaluate(data);
		
		if( result != 4 ){
			fail("Regex evaluator failed to detect value at position 4, (returned " + result + ")");
		}

	}
	
	public void testBasicRegexCaseSensitivity() throws InvalidEvaluatorException{
		
		RegexEvaluator regexEval = new RegexEvaluator("/apple/");
		
		int result = regexEval.evaluate( "0123456789aPple");
		
		if( result != -1 ){
			fail("Regex evaluator should have rejected (returned " + result + ")");
		}
		
	}
}
