package net.lukemurphey.nsia.tests;

import net.lukemurphey.nsia.*;
import net.lukemurphey.nsia.scan.NetworkPortRange;
import net.lukemurphey.nsia.scan.ScanException;
import net.lukemurphey.nsia.scan.ServiceScanResult;
import net.lukemurphey.nsia.scan.ServiceScanRule;
import junit.framework.TestCase;

public class ServiceScanRuleTest extends TestCase{
	
	public void testScan() throws ScanException{
		NetworkPortRange[] expectedOpen = new NetworkPortRange[1];
		expectedOpen[0] = new NetworkPortRange(80, NetworkPortRange.Protocol.TCP);
		
		/*
		NetworkPortRange[] toScan = new NetworkPortRange[4];
		toScan[0] = new NetworkPortRange(78, NetworkPortRange.Protocol.TCP);
		toScan[1] = new NetworkPortRange(80, NetworkPortRange.Protocol.TCP);
		toScan[2] = new NetworkPortRange(0, 65535, NetworkPortRange.Protocol.TCP);
		toScan[3] = new NetworkPortRange(79, NetworkPortRange.Protocol.TCP);*/
		
		NetworkPortRange[] toScan = new NetworkPortRange[1];
		toScan[0] = new NetworkPortRange(75, 85, NetworkPortRange.Protocol.TCP);
		
		long startTime = System.currentTimeMillis();
		ServiceScanRule scanRule = new ServiceScanRule(
				//new Application(), "66.84.33.195",
				new Application(), "127.0.0.1",
				expectedOpen,
				toScan
		);
		
		ServiceScanResult result = (ServiceScanResult)scanRule.doScan();
		
		long endTime = System.currentTimeMillis();
		System.out.println("Scan completed within " + (endTime - startTime)/1000.0 + " secs");
		
		NetworkPortRange[] range = result.getDifferences();
		
		System.out.println("Deviations: " + range.length);
		
		for(int c = 0; c < range.length; c++){
			if( range[c].getProtocol() == null ) {
				fail( "Protocol is null for item " + c );
			}
			else if( range[c].getState() == null ) {
				fail( "State is null for item " + c  );
			}
			else{
				System.out.println(range[c].getProtocol() + "/" + range[c].getStartPort() + " was found in state " + range[c].getState().toString() );
			}
		}
		
	}

}
