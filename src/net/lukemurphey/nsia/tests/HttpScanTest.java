package net.lukemurphey.nsia.tests;

import junit.framework.TestCase;
import java.net.*;
import java.sql.SQLException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.TestsConfig;
import net.lukemurphey.nsia.scan.HttpStaticScanResult;
import net.lukemurphey.nsia.scan.HttpStaticScanRule;
import net.lukemurphey.nsia.scan.ScanException;
import net.lukemurphey.nsia.scan.ScanResult;
import net.lukemurphey.nsia.scan.ScanRule.ScanRuleLoadFailureException;

public class HttpScanTest extends TestCase {

	Application appRes;
	
	public static void main(String[] args) {
	}

	public HttpScanTest() throws BindException, SQLException, InputValidationException, Exception{
		appRes = TestsConfig.getApplicationResource();
		
	}
	
	public void testLoadRuleFromDatabase() throws MalformedURLException, SQLException, ScanException, NoDatabaseConnectionException, ScanRuleLoadFailureException{
		HttpStaticScanRule httpScan = new HttpStaticScanRule(appRes);
		
		if( !httpScan.loadFromDatabase(1) )
			fail("Database load failed");
		
		ScanResult result = httpScan.doScan();
		HttpStaticScanResult httpResult = (HttpStaticScanResult)result;
		if( result.getDeviations() != 0){
			System.out.println( "Deviations: " + result.getDeviations());
			System.out.println( "Response code: " + httpResult.getActualResponseCode());
			System.out.println( "Hash value: " + httpResult.getActualHashValue() );
			fail("Unexpected deviations were observed");
		}
	}
	
	public void testSaveRuleToDatabase() throws MalformedURLException, SQLException, ScanException, NoDatabaseConnectionException, ScanRuleLoadFailureException{
		HttpStaticScanRule httpScan = new HttpStaticScanRule(appRes);
		
		if( !httpScan.loadFromDatabase(1) )
			fail("Database load failed");
		
		ScanResult result = httpScan.doScan();
		HttpStaticScanResult httpResult = (HttpStaticScanResult)result;
		if( result.getDeviations() != 0){
			System.out.println( "Deviations: " + result.getDeviations());
			System.out.println( "Response code: " + httpResult.getActualResponseCode());
			System.out.println( "Hash value: " + httpResult.getActualHashValue() );
			fail("Unexpected deviations were observed");
		}
		
		result.saveToDatabase(appRes.getDatabaseConnection(Application.DatabaseAccessType.SCANNER), 1);
	}
	
	/*
	 * Test method for 'net.lukemurphey.siteSentry.HttpScan.doScan()'
	 */
	/*public void testDoScanNoConnection() throws FileNotFoundException, InstantiationException, IllegalAccessException, ClassNotFoundException, JSAPException, NoDatabaseConnectionException {
		EventLog eventlog;
		Application applicationResources = TestsConfig.getApplicationResource();
		
		try {
			eventlog = new EventLog(new File("testLog.log"));
			applicationResources.setEventLog(eventlog);
		} catch (FileNotFoundException e) {
			fail("The event log could not be created since the log file could be prepared: " + e.getMessage());
			return;
		}
		
		HttpScan httpScan = new HttpScan(applicationResources);
		
		try {
			httpScan.setUrl(new URL("http://192.168.10.253:22/"));
		} catch (MalformedURLException e) {
			fail("The URL is not valid: " + e.getMessage());
			return;
		}
		
		HttpScanResult scanResult;
		httpScan.setExpectedResponseCode(302);
		
		try {
			httpScan.setExpectedDataHash("SHA-512", "535fcb04d86e85b6da1c9d94bd672a2f8fcdcba0a9dfeb8f7d53996b62f92adaabf288b4fc01eb48ee7d4aa4819c7ccf0f2a97e76b019662917b0268b6881819");
			scanResult = (HttpScanResult)httpScan.doScan();
		} catch (NoSuchAlgorithmException e) {
			fail("The hash algorithm is not supported: " + e.getMessage());
			return;
		}
		
		if( scanResult.getResultCode() != ScanResultCode.CONNECTION_FAILED)
			fail("The scan result code does not indicate a failed connection: " + scanResult.getResultCode().toString());
		
	}*/
	
	/*
	 * Test method for 'net.lukemurphey.siteSentry.HttpScan.doScan()'
	 */
	/*public void testDoScan() throws FileNotFoundException, InstantiationException, IllegalAccessException, ClassNotFoundException, JSAPException, NoDatabaseConnectionException {
				
		EventLog eventlog;
		Application applicationResources = TestsConfig.getApplicationResource();
		
		try {
			eventlog = new EventLog(new File("testLog.log"));
			applicationResources.setEventLog(eventlog);
		} catch (FileNotFoundException e) {
			fail("The event log could not be created since the log file could be not be prepared: " + e.getMessage());
			return;
		}
		
		HttpScan httpScan = new HttpScan(applicationResources);
		httpScan.setFollowRedirects(true);
		
		try {
			httpScan.setUrl(new URL("http://lukemurphey.net/"));
		} catch (MalformedURLException e) {
			fail("The URL is not valid: " + e.getMessage());
			return;
		}
		
		httpScan.setExpectedResponseCode(302);
		httpScan.addHeaderRule("Server","Apache",HttpScan.MUST_MATCH,123);
		HttpScanResult scanResult;
		
		try {
			httpScan.setExpectedDataHash("SHA-512", "535fcb04d86e85b6da1c9d94bd672a2f8fcdcba0a9dfeb8f7d53996b62f92adaabf288b4fc01eb48ee7d4aa4819c7ccf0f2a97e76b019662917b0268b6881819");
			scanResult = (HttpScanResult)httpScan.doScan();
		} catch (NoSuchAlgorithmException e) {
			fail("The hash algorithm is not supported: " + e.getMessage());
			return;
		}
		
		System.out.println( "Response code: " + scanResult.getActualResponseCode());
		System.out.println( "Hash value: " + scanResult.getActualHashValue() );
		
		HttpHeaderScanResult[] headerScanResults = scanResult.getHeaderRuleResults();
		
		for( int c = 0; c < headerScanResults.length; c++ ){
			System.out.print("Header[" + c + "]: ");
			
			if( headerScanResults[c].getRuleState() == HttpHeaderScanResult.ACCEPTED )
				System.out.println("Accepted");
			else if( headerScanResults[c].getRuleState() == HttpHeaderScanResult.REJECTED )
				System.out.println("Rejected");
			else if( headerScanResults[c].getRuleState() == HttpHeaderScanResult.ACCEPTED_BY_DEFAULT )
				System.out.println("Accepted by Default");
			else if( headerScanResults[c].getRuleState() == HttpHeaderScanResult.REJECTED_BY_DEFAULT )
				System.out.println("Rejected by Default");
			
			System.out.println( "\t" + headerScanResults[c].getValueRule() + "::" + headerScanResults[c].getRuleId() );
			System.out.println( "\t" + headerScanResults[c].getActualValue() + "::" + headerScanResults[c].getActualName() );
		}
		
	}*/

}
