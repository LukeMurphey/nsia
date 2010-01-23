package net.lukemurphey.nsia.tests;

import java.net.BindException;
import java.sql.SQLException;

import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.ScannerController;
import net.lukemurphey.nsia.TestsConfig;
import net.lukemurphey.nsia.scan.ScanResult;
import net.lukemurphey.nsia.scan.ScanResultCode;

import junit.framework.TestCase;

public class ScannerControllerTest extends TestCase {
	ScannerController scannerController;
	
	public static void main(String[] args) {
	}

	public ScannerControllerTest(String name) throws BindException, SQLException, InputValidationException, Exception {
		super(name);
		scannerController = new ScannerController(TestsConfig.getApplicationResource());
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.ScannerController.scanningEnabled()'
	 */
	public void testScanningEnabled() {
		scannerController.disableScanning();
		if( scannerController.scanningEnabled() == true )
			fail("Scanning was not properly disabled");
		
		scannerController.enableScanning();
		
		if( scannerController.scanningEnabled() == false )
			fail("Scanning was not properly Enabled");
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.ScannerController.enterScanningLoop()'
	 */
	public void testEnterScanningLoop() throws SQLException, Exception {
		ScannerController scannerController2 = new ScannerController(TestsConfig.getApplicationResource());
		scannerController2.setMaxScanThreads(5);
		scannerController2.start();
		
		Thread.sleep(5000);
		scannerController2.disableScanning();
		while(!scannerController2.isAlive()){
			System.out.println("Scanner thread still alive");
			Thread.sleep(1000);
		}
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.ScannerController.getMaxScanThreads()'
	 */
	public void testGetMaxScanThreads() {
		scannerController.setMaxScanThreads(5);
		if( scannerController.getMaxScanThreads() != 5)
			fail("Max thread count not properly retained");
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.ScannerController.scanExpiredRules(boolean)'
	 */
	public void testScanExpiredRules() {

	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.ScannerController.scanUpdatedRules(boolean)'
	 */
	public void testScanUpdatedRules() throws SQLException, Exception {
		
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.ScannerController.scanRule(long, boolean)'
	 */
	public void testScanRule() throws SQLException, Exception {
		ScanResult scanResult = scannerController.scanRule(1, false);
		if( scanResult.getResultCode() != ScanResultCode.SCAN_COMPLETED )
			fail("Scan did not complete (" + scanResult.getResultCode().toString() + ")");
	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.ScannerController.scanAllRules(boolean)'
	 */
	public void testScanAllRules() {

	}

	/*
	 * Test method for 'net.lukemurphey.siteSentry.ScannerController.getPollingLoopFrequency()'
	 */
	public void testGetPollingLoopFrequency() {
		scannerController.setPollingLoopFrequency(60000);
		if( scannerController.getPollingLoopFrequency() != 60000)
			fail("Polling loop frequency not properly retained");
	}

}
