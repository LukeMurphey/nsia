package net.lukemurphey.nsia.tests;

import junit.framework.TestCase;
import net.lukemurphey.nsia.*;
import net.lukemurphey.nsia.scan.HttpDefinitionScanResult;
import net.lukemurphey.nsia.scan.HttpSeekingScanResult;
import net.lukemurphey.nsia.scan.ScanResultLoader;

public class HttpSeekingScanRuleTest extends TestCase {

	Application app = null;
	
	public void setUp() throws TestApplicationException{
		app = TestApplication.getApplication();
	}
	
	public void tearDown(){
		TestApplication.stopApplication();
	}
	
	public void testLoadScanResults() throws Exception{
		
		HttpSeekingScanResult result = (HttpSeekingScanResult)ScanResultLoader.getScanResult(1);
		
		HttpDefinitionScanResult[] sigResults = result.getFindings(-1, 10, true);
		
		if( sigResults == null ){
			fail("The scan findings were not returned (returned null instead)");
		}
		else if( sigResults.length != 5 ){
			fail("The wrong number of findings were returned (" + sigResults.length + " instead of 6)");
		}
	}	
}
