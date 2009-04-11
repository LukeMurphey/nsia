package net.lukeMurphey.nsia.testCases;

import net.lukeMurphey.nsia.scanRules.ByteTestEvaluator;
import net.lukeMurphey.nsia.scanRules.InvalidEvaluatorException;
import junit.framework.TestCase;

public class ByteTestEvaluatorTest extends TestCase {
	
	public void testGetMaxValue(){
		
		double maxValue = ByteTestEvaluator.getMaxValue(2);
		
		if( maxValue != 65535){
			fail("Value was not calculated correctly (returned " + maxValue + ")");
		}
	}
	
	public void testCalculateValue(){
	
		byte[] bytes = new byte[]{1, 2};
		double value = ByteTestEvaluator.getBigEndianValue(bytes);
		
		if( value != (258)){
			fail("Value was not calculated correctly (returned " + value + ")");
		}
	}
	
	public void testCalculateValue2(){
		
		byte[] bytes = new byte[]{1, 0, 1};
		double value = ByteTestEvaluator.getBigEndianValue(bytes);
		
		if( value != (65537)){
			fail("Value was not calculated correctly (returned " + value + ")");
		}
	}
	
	public void testCalculateValueLittleEndian(){
		
		byte[] bytes = new byte[]{1, 0, 0};
		double value = ByteTestEvaluator.getLittleEndianValue(bytes);
		
		if( value != (1)){
			fail("Value was not calculated correctly (returned " + value + ")");
		}
	}
	
	public void testCalculateValueLittleEndian2(){
		
		byte[] bytes = new byte[]{1, 1, 0};
		double value = ByteTestEvaluator.getLittleEndianValue(bytes);
		
		if( value != (257)){
			fail("Value was not calculated correctly (returned " + value + ")");
		}
	}
	
	public void testBytesInput() throws InvalidEvaluatorException{
		ByteTestEvaluator eval = ByteTestEvaluator.parse("4 bytes > 255");
		
		byte[] data = new byte[]{0, 0, 1, 0, 0};
		int result = eval.evaluate(data);
		
		if( result != 3 ){
			fail("The evaluator failed to derive the correct results (returned " + result + ")");
		}
	}
	
	public void testBytesOutsideBounds() throws InvalidEvaluatorException{
		ByteTestEvaluator eval = ByteTestEvaluator.parse("8 bytes > 255");
		
		byte[] data = new byte[]{0, 0, 1, 0, 0};
		int result = eval.evaluate(data);
		
		if( result != 3 ){
			fail("The evaluator failed to derive the correct results (returned " + result + ")");
		}
	}
	
	public void testStringInput() throws InvalidEvaluatorException{
		ByteTestEvaluator eval = ByteTestEvaluator.parse("4 digits > 255");
		
		int result = eval.evaluate("0256");
		
		if( result != 3 ){
			fail("The evaluator failed to derive the correct results (returned " + result + ")");
		}
	}
	
	public void testStringOutsideBounds() throws InvalidEvaluatorException{
		ByteTestEvaluator eval = ByteTestEvaluator.parse("4 digits > 255");
		
		int result = eval.evaluate( "256" );
		
		if( result != 2 ){
			fail("The evaluator failed to derive the correct results (returned " + result + ")");
		}
	}
	
	public void testBytesSignedNegative() throws InvalidEvaluatorException{
		ByteTestEvaluator eval = ByteTestEvaluator.parse("1 byte = 255");
		eval.setOffset(3);
		
		byte[] data = new byte[]{0, 0, 0, -128, 0};
		int result = eval.evaluate(data);
		
		if( result != 3 ){
			fail("The evaluator failed to derive the correct results (returned " + result + ")");
		}
	}
	
	public void testBytesSignedLittleEndian() throws InvalidEvaluatorException{
		ByteTestEvaluator eval = ByteTestEvaluator.parse("4 bytes = 8 (little-endian)");
		
		byte[] data = new byte[]{8, 0, 0, 0, 0};
		int result = eval.evaluate(data);
		
		if( result != 3 ){
			fail("The evaluator failed to derive the correct results (returned " + result + ")");
		}
	}
	
	public void testBytesSignedLittleEndian2() throws InvalidEvaluatorException{
		ByteTestEvaluator eval = ByteTestEvaluator.parse("4 bytes = 258 (little-endian)");
		
		byte[] data = new byte[]{2, 1, 0, 0, 0};
		int result = eval.evaluate(data);
		
		if( result != 3 ){
			fail("The evaluator failed to derive the correct results (returned " + result + ")");
		}
	}
	
	public void testHex() throws InvalidEvaluatorException{
		ByteTestEvaluator eval = ByteTestEvaluator.parse("4 digits = 31 (hexadecimal)");
		
		int result = eval.evaluate("001F000");
		
		if( result != 3 ){
			fail("The evaluator failed to derive the correct results (returned " + result + ")");
		}
	}
	
	public void testImpossibleValueString(){
		try{
			ByteTestEvaluator.parse("1 digit = 11");
		}
		catch(InvalidEvaluatorException e){
			return;
		}
		
		fail("The parse method failed to recognize that the evaluator will never be able to derive the value provided");
	}
	
	public void testImpossibleValueHexString(){
		try{
			ByteTestEvaluator.parse("2 digits = 257 (hexadecimal)");
		}
		catch(InvalidEvaluatorException e){
			return;
		}
		
		fail("The parse method failed to recognize that the evaluator will never be able to derive the value provided");
	}
	
	public void testImpossibleValueByte(){
		try{
			ByteTestEvaluator.parse("2 bytes = 65537");
		}
		catch(InvalidEvaluatorException e){
			return;
		}
		
		fail("The parse method failed to recognize that the evaluator will never be able to derive the value provided");
	}
	
	public void testHighValueString(){
		try{
			ByteTestEvaluator.parse("1 digit = 9");
		}
		catch(InvalidEvaluatorException e){
			fail("The parse method failed to recognize that the evaluator is within range");
		}
	}
	
	public void testHighValueHexString(){
		try{
			ByteTestEvaluator.parse("2 digits = 255 (hexadecimal)");
		}
		catch(InvalidEvaluatorException e){
			fail("The parse method failed to recognize that the evaluator is within range");
		}
	}
	
	public void testHighValueByte(){
		try{
			ByteTestEvaluator.parse("2 bytes = 65535");
		}
		catch(InvalidEvaluatorException e){
			fail("The parse method failed to recognize that the evaluator is within range");
		}
	}
	
	public void testAbsoluteValue() throws InvalidEvaluatorException{
		
		ByteTestEvaluator byteTest = ByteTestEvaluator.parse("4 digits = 128 (absolute-value)");
		
		int result = byteTest.evaluate("-1289");
		
		if( result != 3 ){
			fail("The evaluator failed to derive the correct results (returned " + result + ")");
		}
	}

}
