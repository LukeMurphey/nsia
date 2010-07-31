package net.lukemurphey.nsia.tests;

import java.sql.SQLException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.ScannerController;
import net.lukemurphey.nsia.scan.ScanResult;
import net.lukemurphey.nsia.scan.ScanResultCode;

import junit.framework.TestCase;

public class ScannerControllerTest extends TestCase {
	
	
	ScannerController scannerController;
	Application app = null;
	
	public void setUp() throws TestApplicationException{
		app = TestApplication.getApplication();
		scannerController = new ScannerController(app);
	}
	
	public void tearDown(){
		TestApplication.stopApplication();
	}

	public void testScanningEnabled() {
		scannerController.disableScanning();
		
		if( scannerController.scanningEnabled() == true ){
			fail("Scanning was not properly disabled");
		}
		
		scannerController.enableScanning();
		
		if( scannerController.scanningEnabled() == false ){
			fail("Scanning was not properly Enabled");
		}
	}

	public void testEnterScanningLoop() throws SQLException, Exception {
		ScannerController scannerController2 = new ScannerController(app);
		scannerController2.setMaxScanThreads(5);
		scannerController2.start();
		
		Thread.sleep(5000);
		scannerController2.disableScanning();
		
		while(!scannerController2.isAlive()){
			Thread.sleep(1000);
		}
		
		if( scannerController.scanningEnabled() ){
			fail("Scanner failed to exit scanning loop");
		}
	}

	public void testGetMaxScanThreads() {
		scannerController.setMaxScanThreads(5);
		if( scannerController.getMaxScanThreads() != 5){
			fail("Max thread count not properly retained");
		}
	}

	public void testScanExpiredRules() {

	}

	public void testScanUpdatedRules() throws SQLException, Exception {
		
	}

	public void testScanRule() throws SQLException, Exception {
		ScanResult scanResult = scannerController.scanRule(1, false);
		
		if( scanResult.getResultCode() != ScanResultCode.SCAN_COMPLETED ){
			fail("Scan did not complete (" + scanResult.getResultCode().toString() + ")");
		}
	}


	public void testScanAllRules() {

	}

	public void testGetPollingLoopFrequency() {
		scannerController.setPollingLoopFrequency(60000);
		
		if( scannerController.getPollingLoopFrequency() != 60000){
			fail("Polling loop frequency not properly retained");
		}
	}

}
