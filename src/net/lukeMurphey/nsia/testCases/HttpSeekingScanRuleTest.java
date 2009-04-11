package net.lukeMurphey.nsia.testCases;

import net.lukeMurphey.nsia.scanRules.HttpSeekingScanResult;

import net.lukeMurphey.nsia.scanRules.HttpDefinitionScanResult;
import net.lukeMurphey.nsia.scanRules.ScanResultLoader;
import junit.framework.TestCase;
import net.lukeMurphey.nsia.*;

/*import net.lukeMurphey.nsia.scanRules.HttpSeekingScanRule;
import net.lukeMurphey.nsia.scanRules.SignatureMatch;

import java.net.*;
import java.sql.SQLException;*/


public class HttpSeekingScanRuleTest extends TestCase {

	/*public void testScanKnownTrigger() throws BindException, SQLException, InputValidationException, Exception{
		Application application = TestsConfig.getApplicationResource();
		
		HttpSeekingScanRule rule = new HttpSeekingScanRule( application, new Wildcard("*guninski.com*", true), 2000, true );
		rule.setRecursionDepth(2);
		rule.setScanCountLimit(20);
		
		rule.addSeedUrl( new URL( "http://www.guninski.com/popspoof.html" ) );
		
		long start = System.currentTimeMillis();
		HttpSeekingScanResult result = (HttpSeekingScanResult)rule.doScan();
		long end = System.currentTimeMillis();
		long totalTime = (end-start);
		
		HttpSignatureScanResult[] findings = result.getFindings();
		
		System.out.println("Resource scanned in " + ((double)totalTime)/1000 + " s");
		
		System.out.println("Average: " + ((double)totalTime/findings.length)/1000 + " s");
		
		System.out.println("Total files scanned: " + findings.length);
		
		for(int c = 0; c < findings.length; c++){
			SignatureMatch[] matches = findings[c].getSignatureMatches();
			
			System.out.println( findings[c].getUrl().toString() + "(matched " + matches.length + " rules)");
			
			for(int d = 0; d < matches.length; d++){
				System.out.println( "-->\t" + matches[d].getSignatureName() + " = " + matches[d].getMessage() );
			}
		}
	}*/
	
	public void testLoadScanResults() throws Exception{
		Application application = new Application(true);//TestsConfig.getApplicationResource();
		
		if( application == null ){
			throw new Exception("Application cannot be null");
		}
		
		HttpSeekingScanResult result = (HttpSeekingScanResult)ScanResultLoader.getScanResult(851);
		
		HttpDefinitionScanResult[] sigResults = result.getFindings(-1, 10, true);
		
		for(int c = 0; c < sigResults.length; c++){
			System.out.println( sigResults[c].getScanResultID() );
		}
		
	}
	/*
	public void testScan() throws BindException, SQLException, InputValidationException, Exception{
		Application application = TestsConfig.getApplicationResource();
		
		HttpSeekingScanRule rule = new HttpSeekingScanRule( application, new Wildcard("*kenoshachurch.org*", true), 2000, true );
		rule.setRecursionDepth(5);
		rule.setScanCountLimit(150);
		
		rule.addSeedUrl( new URL( "http://kenoshachurch.org/Sitemap" ) );
		
		long start = System.currentTimeMillis();
		HttpSeekingScanResult result = (HttpSeekingScanResult)rule.doScan();
		long end = System.currentTimeMillis();
		long totalTime = (end-start);
		
		HttpSignatureScanResult[] findings = result.getFindings();
		
		System.out.println("Resource scanned in " + ((double)totalTime)/1000 + " s");
		
		System.out.println("Average: " + ((double)totalTime/findings.length)/1000 + " s");
		
		System.out.println("Total files scanned: " + findings.length);
		
		for(int c = 0; c < findings.length; c++){
			SignatureMatch[] matches = findings[c].getSignatureMatches();
			
			System.out.println( findings[c].getUrl().toString() + "(matched " + matches.length + " rules)");
			
			for(int d = 0; d < matches.length; d++){
				System.out.println( "-->\t" + matches[d].getSignatureName() + " = " + matches[d].getMessage() );
			}
		}
	}*/
	
}
