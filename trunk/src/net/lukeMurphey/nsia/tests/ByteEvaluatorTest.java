package net.lukemurphey.nsia.tests;

import net.lukemurphey.nsia.scan.ByteEvaluator;
import net.lukemurphey.nsia.scan.DataSpecimen;
import junit.framework.TestCase;

public class ByteEvaluatorTest extends TestCase {
	
	
	public void testParse(){

		ByteEvaluator eval = ByteEvaluator.parse("FE 42 43");
		
		int result = eval.evaluate( new DataSpecimen( new byte[]{-2,66,67} ) );
		
		if( result != 2 ){
			fail("Failed to find item at position 2 (returned " + result + ")");
		}
		
	}
	
	public void testSuccess(){

		ByteEvaluator eval = new ByteEvaluator( "tree".getBytes() );
		
		int result = eval.evaluate( new DataSpecimen( "1234567890tree" ) );
		
		if( result != 13 ){
			fail("Failed to find item at position 13 (returned " + result + ")");
		}
		
	}
	
	public void testFail(){

		ByteEvaluator eval = new ByteEvaluator( "DoesNotExist".getBytes() );
		
		int result = eval.evaluate( new DataSpecimen( "1234567890tree" ) );
		
		if( result != -1 ){
			fail("Rule identifed a match even though none exists (returned " + result + ")");
		}
		
	}
	
	public void testParseInvalidRule(){

		try{
			new ByteEvaluator( "".getBytes() );
		}
		catch( IllegalArgumentException e){
			return;
		}
		
		fail("Method should have throw exception since argument was invalid)");
		
	}

}
