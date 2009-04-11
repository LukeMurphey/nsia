package net.lukeMurphey.nsia.testCases;

import java.io.UnsupportedEncodingException;

import net.lukeMurphey.nsia.scanRules.DataSpecimen;
import net.lukeMurphey.nsia.scanRules.Evaluator;
import net.lukeMurphey.nsia.scanRules.StringEvaluator;
import junit.framework.TestCase;

public class StringEvaluatorTest extends TestCase {
	
	public void testIgnoreCase(){
		StringEvaluator eval = new StringEvaluator("tree", true);
		
		int result = eval.evaluate( new DataSpecimen("1234567890TrEeare"), Evaluator.UNDEFINED);
		
		if( result != 13 ){
			fail("StringEvaluator failed to identify target at location 13 (returned " + result + ")");
		}
	}
	
	public void testIncludeCase(){
		StringEvaluator eval = new StringEvaluator("TrEe", false);
		
		int result = eval.evaluate( new DataSpecimen("1234567890TrEeare"), Evaluator.UNDEFINED);
		
		if( result != 13 ){
			fail("StringEvaluator failed to identify target at location 10 (returned " + result + ")");
		}
	}
	
	public void testIgnoreCaseNotFound(){
		StringEvaluator eval = new StringEvaluator("Tree", true);
		
		int result = eval.evaluate( new DataSpecimen("1234567890TARESare"), Evaluator.UNDEFINED);
		
		if( result != Evaluator.UNDEFINED ){
			fail("StringEvaluator failed to return 'UNDEFINED' (returned " + result + ")");
		}
	}
	
	public void testIncludeCaseNotFound(){
		StringEvaluator eval = new StringEvaluator("tree", false);
		
		int result = eval.evaluate("1234567890TrEeare", Evaluator.UNDEFINED);
		
		if( result != Evaluator.UNDEFINED ){
			fail("StringEvaluator failed to return 'UNDEFINED' (returned " + result + ")");
		}
	}
	
	public void testIgnoreCaseAtStart(){
		StringEvaluator eval = new StringEvaluator("tree", true);
		
		int result = eval.evaluate("TrEe1234567890are", Evaluator.UNDEFINED);
		
		if( result != 3 ){
			fail("StringEvaluator failed to identify target at location 3 (returned " + result + ")");
		}
	}
	
	public void testIncludeCaseAtStart(){
		StringEvaluator eval = new StringEvaluator("TrEe", false);
		
		int result = eval.evaluate("TrEe1234567890are", Evaluator.UNDEFINED);
		
		if( result != 3 ){
			fail("StringEvaluator failed to identify target at location 3 (returned " + result + ")");
		}
	}
	
	public void testIgnoreCaseAtEnd(){
		StringEvaluator eval = new StringEvaluator("tree", true);
		
		int result = eval.evaluate("1234567890areTrEe", Evaluator.UNDEFINED);
		
		if( result != 16 ){
			fail("StringEvaluator failed to identify target at location 16 (returned " + result + ")");
		}
	}
	
	public void testIncludeCaseAtEnd(){
		StringEvaluator eval = new StringEvaluator("TrEe", false);
		
		int result = eval.evaluate("1234567890areTrEe", Evaluator.UNDEFINED);
		
		if( result != 16 ){
			fail("StringEvaluator failed to identify target at location 16 (returned " + result + ")");
		}
	}
	
	public void testIgnoreCaseWithOffset(){
		StringEvaluator eval = new StringEvaluator("tree", true);
		
		int result = eval.evaluate("1234567890TrEeare", 9);
		
		if( result != 13 ){
			fail("StringEvaluator failed to identify target at location 13 (returned " + result + ")");
		}
	}
	
	public void testIncludeCaseWithOffset(){
		StringEvaluator eval = new StringEvaluator("TrEe", false);
		
		int result = eval.evaluate("1234567890TrEeare", 9);
		
		if( result != 13 ){
			fail("StringEvaluator failed to identify target at location 13 (returned " + result + ")");
		}
	}
	
	public void testMaxDepth(){
		StringEvaluator eval = new StringEvaluator("tree", true);
		eval.setDepth(5);
		
		int result = eval.evaluate("1234567890TrEeare", Evaluator.UNDEFINED);
		
		if( result != -1 ){
			fail("StringEvaluator should have returned undefined (returned " + result + ")");
		}
	}
	
	public void testMaxDepthAtEdge(){
		StringEvaluator eval = new StringEvaluator("tree", true);
		eval.setDepth(14);
		
		int result = eval.evaluate("1234567890TrEeare", Evaluator.UNDEFINED);
		
		if( result != 13 ){
			fail("StringEvaluator failed to identify target at location 13 (returned " + result + ")");
		}
	}
	
	public void testMaxDepthUnderEdge(){
		StringEvaluator eval = new StringEvaluator("tree", true);
		eval.setDepth(13);
		
		int result = eval.evaluate("1234567890TrEeare", Evaluator.UNDEFINED);
		
		if( result != -1 ){
			fail("StringEvaluator should have returned undefined (returned " + result + ")");
		}
	}
	
	public void testMaxDepthAtEdgeCaseSensitive(){
		StringEvaluator eval = new StringEvaluator("tree", false);
		eval.setDepth(14);
		
		int result = eval.evaluate("1234567890treeare", Evaluator.UNDEFINED);
		
		if( result != 13 ){
			fail("StringEvaluator failed to identify target at location 13 (returned " + result + ")");
		}
	}
	
	public void testMaxDepthUnderEdgeCaseSensitive(){
		StringEvaluator eval = new StringEvaluator("tree", false);
		eval.setDepth(13);
		
		int result = eval.evaluate("1234567890treeare", Evaluator.UNDEFINED);
		
		if( result != -1 ){
			fail("StringEvaluator should have returned undefined (returned " + result + ")");
		}
	}
	
	public void testUTF8(){
		StringEvaluator eval = new StringEvaluator("CD", true);
		
		DataSpecimen data = new DataSpecimen("ABŷCD");
		int result = eval.evaluate( data, Evaluator.UNDEFINED);
		
		result = data.getByteIndex(result);
		
		if( result != 5 ){
			fail("StringEvaluator failed to identify target at location 5 (returned " + result + ")");
		}
	}
	
	public void testUTF16() throws UnsupportedEncodingException{
		StringEvaluator eval = new StringEvaluator("CD", false);
		
		DataSpecimen data = new DataSpecimen("ABCDEF".getBytes("UTF-16"));
		
		int result = eval.evaluate( data, Evaluator.UNDEFINED);
		
		result = data.getByteIndex(result);
		
		if( result != 9 ){
			fail("StringEvaluator failed to identify target at location 9 (returned " + result + ")");
		}
	}
	
	public void testUTF16AtEnd() throws UnsupportedEncodingException{
		StringEvaluator eval = new StringEvaluator("CD", false);
		
		DataSpecimen data = new DataSpecimen("ABCD".getBytes("UTF-16"));
		
		int result = eval.evaluate( data, Evaluator.UNDEFINED);
		result = data.getByteIndex(result);
		
		if( result != 9 ){
			fail("StringEvaluator failed to identify target at location 9 (returned " + result + ")");
		}
	}
	
	public void testUTF16AtStart() throws UnsupportedEncodingException{
		StringEvaluator eval = new StringEvaluator("123", false);
		
		DataSpecimen data = new DataSpecimen("123".getBytes("UTF-16"));
		
		int result = eval.evaluate( data, Evaluator.UNDEFINED);
		
		result = data.getByteIndex(result);
		
		if( result != 7 ){
			fail("StringEvaluator failed to identify target at location 7 (returned " + result + ")");
		}
	}
	
	
	public void testFindLocationOf() throws UnsupportedEncodingException{
		
		byte[] bytes = "123A".getBytes("UTF-16");
		DataSpecimen data = new DataSpecimen(bytes);
		
		String test = data.getString();
		
		int result = StringEvaluator.locationOf( test, "123", 0, false, Evaluator.UNDEFINED);
		
		if( result != 1 ){
			fail("StringEvaluator failed to identify target at location 7 (returned " + result + ")");
		}
	}
	
	

}
