package net.lukemurphey.nsia.scan.scriptenvironment;

import net.lukemurphey.nsia.scan.DataSpecimen;
import net.lukemurphey.nsia.scan.EncodingDetectionFailedException;

import org.htmlparser.util.ParserException;

public class Parser {

	public static org.htmlparser.Parser parse( String s ) throws ParserException{
		org.htmlparser.Parser parser = new org.htmlparser.Parser();
		
		if( s != null ){
			
			try{
				String charset = DataSpecimen.detectedCharacterSet(s.getBytes());
			
				parser.setEncoding(charset);
			}
			catch(EncodingDetectionFailedException e){
				// Do nothing, We could not detect the character set and we will have o hope that the parsing library can still parse it.
			}
			catch(ParserException e){
				// Do nothing, We could not detect the character set and we will have o hope that the parsing library can still parse it.
			}
			
			parser.setInputHTML(s);
		}
		
		return parser;
	}
	
}
