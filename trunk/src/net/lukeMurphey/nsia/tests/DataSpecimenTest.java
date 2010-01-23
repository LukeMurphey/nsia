package net.lukemurphey.nsia.tests;

import java.io.IOException;

import net.lukemurphey.nsia.scan.DataSpecimen;
import net.lukemurphey.nsia.scan.EncodingDetectionFailedException;
import junit.framework.TestCase;

public class DataSpecimenTest extends TestCase {
	

	
	public void testCharacterSetDetection() throws IOException, EncodingDetectionFailedException{
		DataSpecimen data;
		
		data = new DataSpecimen("ABCD".getBytes("UTF-16"));
        
		String result = data.getEncoding().displayName();
		
		if( !result.equalsIgnoreCase( "UTF-16BE" ) ){
			fail("Result is : " + result);
		}
	}
	
	public void testBytesUnalteredUTF16() throws IOException, EncodingDetectionFailedException{
		DataSpecimen data;
		
		byte[] bytes = "ABCD".getBytes("UTF-16");
		data = new DataSpecimen(bytes);
        
		byte[] bytes2 = data.getBytes();
		
		if( bytes.length != bytes2.length ){
			fail("Byte lengths are not equal");
		}
	}
	
	public void testBytesUnalteredUTF8() throws IOException, EncodingDetectionFailedException{
		DataSpecimen data;
		
		byte[] bytes = "ABŷCD".getBytes("UTF-8");
		data = new DataSpecimen(bytes);
        
		byte[] bytes2 = data.getBytes();
		
		if( bytes.length != bytes2.length ){
			fail("Byte lengths are not equal");
		}
	}
	
	/*
	public void testSubStringUTF16() throws UnsupportedEncodingException, EncodingDetectionFailedException{
		DataSpecimen data;
		
		data = new DataSpecimen("ABCD".getBytes("UTF-16"));
		
        String newString = data.getSubString(3);
        
        byte[] bytes = newString.getBytes();
        byte[] bytes2 = data.getBytes();
        
		if( newString.charAt(1) != 'B' ){
			fail("Method failed to accurately get a substring (character is " + newString.charAt(1) + ")");
		}
		else if( newString.length() != 4 ){//Note that the length is four because the UTF-16 BOM character at the beginning accounts for one.
			fail("Method failed to accurately get the entire substring (length is " + newString.length() + " for " + newString + ")");
		}
	}
	
	public void testByteLocationUTF16() throws IOException, EncodingDetectionFailedException{
		DataSpecimen data;
		
		byte[] bytes = "ABCD".getBytes("UTF-16");
		
		data = new DataSpecimen(bytes);
        
		String encoding = DataSpecimen.detectedCharacterSet( data.getBytes() );
		
		if( encoding == null || !encoding.equalsIgnoreCase( "UTF-16BE" ) ){
			fail("String encoding was not detected as expected, returned (" + encoding + ")");
		}
		
		int bytePos = data.getByteIndex(2, true);
		
		if( bytePos != 7){
			fail("Byte position was not detected properly: " + bytePos);
		}
	}
	
	public void testByteLocation() throws IOException, EncodingDetectionFailedException{
		DataSpecimen data;
		
		data = new DataSpecimen( "ABCD".getBytes() );
        
		String encoding = DataSpecimen.detectedCharacterSet( data.getBytes() );
		
		if( encoding == null || !encoding.equalsIgnoreCase( "ASCII" ) ){
			fail("String encoding was not detected as expected, returned (" + encoding + ")");
		}
		
		int bytePos = data.getByteIndex(2);
		
		if( bytePos != 2){
			fail("Byte position was not detected properly: " + bytePos);
		}
	}
	
	public void testSubStringUTF8(){
		DataSpecimen data;
		String value = "ABŷCD";
		
		data = new DataSpecimen(value);
        String newString = data.getSubString(3);
        
		if( newString.charAt(0) != 'C' ){
			fail("Method failed to accurately get a substring (" + newString.charAt(0) + ")");
		}
		else if( newString.length() != 2 ){
			fail("Method failed to accurately get the entire substring (length is " + newString.length() + ")");
		}
	}
	
	
	
	public void testBytePositionIdentification() throws IOException, EncodingDetectionFailedException{
		DataSpecimen data;
		String value = "ABŷCD";
		
		
		//byte bytes[] = value.getBytes();
		//System.out.println("Length of String = " + value.length() );
		//System.out.println("Length of Array = " + bytes.length );
		
		
		data = new DataSpecimen(value);
        int result = data.getByteIndex(3, true);
        
		if( result != 4 ){
			fail("Method failed to identify location of character at position 4 (returned " + result + ")");
		}
	}
*/
}
