package net.lukemurphey.nsia.tests;

import org.junit.Test;

import junit.framework.TestCase;
import net.lukemurphey.nsia.scan.ByteJumpEvaluator;
import net.lukemurphey.nsia.scan.InvalidEvaluatorException;


public class ByteJumpEvaluatorTest{

	@Test
	public void testParse() throws InvalidEvaluatorException{
		ByteJumpEvaluator eval = ByteJumpEvaluator.parse("1 bytes (align-4, little-endian, hexadecimal) x 2");
		
		if( eval.getAlignment() != 4){
			TestCase.fail("The alignment was not correctly parsed");
		}
		
		if( eval.isBigEndian() != false){
			TestCase.fail("The big-endian operator was not correctly parsed");
		}
		
		if( eval.getBase() != 16){
			TestCase.fail("The base was not correctly parsed");
		}
		
		if( eval.getMultiplier() != 2){
			TestCase.fail("The mulitplier was not correctly parsed");
		}
	}
	
	@Test
	public void testParseMultiplierOnly() throws InvalidEvaluatorException{
		ByteJumpEvaluator eval = ByteJumpEvaluator.parse("1 bytes x 2");
		
		if( eval.getAlignment() != 1){
			TestCase.fail("The alignment was not correctly parsed");
		}
		
		if( eval.isBigEndian() != true){
			TestCase.fail("The big-endian was not correctly parsed");
		}
		
		if( eval.getBase() != 10){
			TestCase.fail("The base was not correctly parsed");
		}
		
		if( eval.getMultiplier() != 2){
			TestCase.fail("The mulitplier was not correctly parsed");
		}
	}
	
	@Test
	public void testSingleByte() throws InvalidEvaluatorException{
		ByteJumpEvaluator eval = ByteJumpEvaluator.parse("1 byte");
		eval.setOffset(2);
		
		byte[] data = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 29, 30 };
		int result = eval.evaluate(data);
		
		if( result != 5 ){
			TestCase.fail("The evaluator failed to derive the correct results (returned " + result + ")");
			//fail("The evaluator failed to derive the correct results (returned " + result + ")");
		}
	}
	
	@Test
	public void testMultipleBytes() throws InvalidEvaluatorException{
		ByteJumpEvaluator eval = ByteJumpEvaluator.parse("2 bytes");
		eval.setOffset(1);
		
		byte[] data = new byte[]{0, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 29, 30 };
		int result = eval.evaluate(data);
		
		if( result != 5 ){
			TestCase.fail("The evaluator failed to derive the correct results (returned " + result + ")");
		}
	}
	
	@Test
	public void testMultiplier() throws InvalidEvaluatorException{
		ByteJumpEvaluator eval = ByteJumpEvaluator.parse("1 byte x 2");
		eval.setOffset(2);
		
		byte[] data = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 29, 30 };
		int result = eval.evaluate(data);
		
		if( result != 7 ){
			TestCase.fail("The evaluator failed to derive the correct results (returned " + result + ")");
			//fail("The evaluator failed to derive the correct results (returned " + result + ")");
		}
	}
	
	@Test
	public void testJustPastEdge() throws InvalidEvaluatorException{
		ByteJumpEvaluator eval = ByteJumpEvaluator.parse("2 bytes");
		eval.setOffset(30);
		
		byte[] data = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30 };
		int result = eval.evaluate(data);
		
		if( result != -1 ){
			TestCase.fail("The evaluator failed to derive the correct results (returned " + result + ")");
		}
	}
	
	@Test
	public void testAtEdge() throws InvalidEvaluatorException{
		ByteJumpEvaluator eval = ByteJumpEvaluator.parse("1 byte");
		eval.setOffset(28);
		
		byte[] data = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 1, 29, 30 };
		int result = eval.evaluate(data);
		
		if( result != 30 ){
			TestCase.fail("The evaluator failed to derive the correct results (returned " + result + ")");
		}
	}
}
