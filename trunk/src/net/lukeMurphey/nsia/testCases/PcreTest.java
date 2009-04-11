package net.lukeMurphey.nsia.testCases;

import junit.framework.TestCase;
import net.lukeMurphey.nsia.scanRules.*;
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
