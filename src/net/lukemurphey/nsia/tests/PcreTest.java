package net.lukemurphey.nsia.tests;

import junit.framework.TestCase;
import net.lukemurphey.nsia.scan.*;

import java.util.regex.*;

public class PcreTest extends TestCase {
	
	public void testPcreNoCase(){
		Pattern pattern = Pcre.parse( "/apple/i");
		
		Matcher matcher = pattern.matcher("ApPLe");
		
		if( !matcher.find() ){
			fail("PCRE failed to find content");
		}
		
	}

}
