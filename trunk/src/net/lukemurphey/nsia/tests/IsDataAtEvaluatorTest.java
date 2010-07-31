package net.lukemurphey.nsia.tests;

import net.lukemurphey.nsia.scan.DataSpecimen;
import net.lukemurphey.nsia.scan.IsDataAtEvaluator;

import junit.framework.TestCase;

public class IsDataAtEvaluatorTest extends TestCase {

	public void testEvaluateFailRelativeAtLEdge() {
		
		IsDataAtEvaluator eval = new IsDataAtEvaluator();
		eval.setOffset(5);
		
		DataSpecimen data = new DataSpecimen("01234");
		
		int result = eval.evaluate(data);
		
		if( result != -1 ){
			fail("Returned value is incorrect (returned " + result + " )");
		}
	}
	
	public void testEvaluateFailAbsoluteAtEdge() {
		
		IsDataAtEvaluator eval = new IsDataAtEvaluator();
		eval.setOffset(5);
		
		DataSpecimen data = new DataSpecimen("01234");
		
		int result = eval.evaluate(data);
		
		if( result != -1 ){
			fail("Returned value is incorrect (returned " + result + " )");
		}
	}
	
	public void testEvaluateFailRelativeWithLastMatchAtEdge() {
		
		IsDataAtEvaluator eval = new IsDataAtEvaluator();
		eval.setOffset(2);
		
		DataSpecimen data = new DataSpecimen("01234");
		
		int result = eval.evaluate(data, 3);
		
		if( result != -1 ){
			fail("Returned value is incorrect (returned " + result + " )");
		}
	}
	
	public void testEvaluateFailAbsoluteWithLastMatchAtEdge() {
		
		IsDataAtEvaluator eval = new IsDataAtEvaluator();
		
		DataSpecimen data = new DataSpecimen("01234");
		
		int result = eval.evaluate(data, 3);
		
		if( result != 3 ){
			fail("Returned value is incorrect (returned " + result + " )");
		}
	}
	
	public void testEvaluateRelativeAtEdge() {
		
		IsDataAtEvaluator eval = new IsDataAtEvaluator();
		eval.setOffset(3);
		
		DataSpecimen data = new DataSpecimen("012345");
		
		int result = eval.evaluate(data, 1);
		
		if( result != 1 ){
			fail("Returned value is incorrect (returned " + result + " )");
		}
	}
	
	public void testEvaluateAbsoluteWithLastMatchAtEdge() {
		
		IsDataAtEvaluator eval = new IsDataAtEvaluator();
		
		DataSpecimen data = new DataSpecimen("01234");
		
		int result = eval.evaluate(data, 3);
		
		if( result != 3 ){
			fail("Returned value is incorrect (returned " + result + " )");
		}
	}
	
	public void testEvaluateAbsolute() {
		
		IsDataAtEvaluator eval = new IsDataAtEvaluator();
		eval.setOffset(4);
		
		DataSpecimen data = new DataSpecimen("0123456789");
		
		int result = eval.evaluate(data);
		
		if( result != 0 ){
			fail("Returned value is incorrect (returned " + result + " )");
		}
		
	}
	
	public void testEvaluateAtEdge() {
		
		IsDataAtEvaluator eval = new IsDataAtEvaluator();
		eval.setOffset(4);
		
		DataSpecimen data = new DataSpecimen("01234");
		
		int result = eval.evaluate(data);
		
		if( result != 0 ){
			fail("Returned value is incorrect (returned " + result + " )");
		}
	}

	public void testIsDataAtEvaluator() {
		IsDataAtEvaluator eval = new IsDataAtEvaluator();
		eval.setOffset(4);
		
		if( eval.getOffset() != 4){
			fail("Offset value returned is incorrect (returned " + eval.getOffset() + ")");
		}

		IsDataAtEvaluator eval2 = new IsDataAtEvaluator();
		eval2.setOffset(12);
		
		if( eval2.getOffset() != 12){
			fail("Offset value returned is incorrect (returned " + eval2.getOffset() + ")");
		}
	}
}
