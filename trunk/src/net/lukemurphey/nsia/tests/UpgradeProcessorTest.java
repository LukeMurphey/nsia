package net.lukemurphey.nsia.tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.upgrade.UpgradeProcessor;
import junit.framework.TestCase;

public class UpgradeProcessorTest extends TestCase {

	public void testComparison(){
		
		// 1 -- Create the upgraders
		UpgradeProcessor p1 = new UpgradeProcessor(1,0,0) {

			@Override
			public boolean doUpgrade(Application app) {
				return false;
			}
			
		};
		
		UpgradeProcessor p2 = new UpgradeProcessor(1,0,2) {
			
			@Override
			public boolean doUpgrade(Application app) {
				return true;
			}
		};
		
		UpgradeProcessor p3 = new UpgradeProcessor(2,0,1) {
			
			@Override
			public boolean doUpgrade(Application app) {
				return true;
			}
		};
		
		UpgradeProcessor p4 = new UpgradeProcessor(2,2,1) {
			
			@Override
			public boolean doUpgrade( Application app ) {
				return true;
			}
		};
		
		UpgradeProcessor p5 = new UpgradeProcessor(3,0,0) {
			
			@Override
			public boolean doUpgrade( Application app ) {
				return true;
			}
		};
		
		// Populate the list
		List<UpgradeProcessor> list = new ArrayList<UpgradeProcessor>();
		list.add(p3);
		list.add(p4);
		list.add(p2);
		list.add(p1);
		list.add(p5);
		
		Collections.sort(list);
		
		if( list.get(0) != p1 ){
			fail("The first entry is the wrong item");
		}
		else if( list.get(1) != p2 ){
			fail("The second entry is the wrong item");
		}
		else if( list.get(2) != p3 ){
			fail("The third entry is the wrong item");
		}
		else if( list.get(3) != p4 ){
			fail("The fourth entry is the wrong item");
		} 
		else if( list.get(4) != p5 ){
			fail("The fifth entry is the wrong item");
		}	
		
	}
	
}
