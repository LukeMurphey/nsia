package net.lukemurphey.nsia.tests;

import java.util.ArrayList;
import java.util.List;

import net.lukemurphey.nsia.upgrade.UpgradeProcessor;
import net.lukemurphey.nsia.upgrade.UpgraderList;
import junit.framework.TestCase;

public class UpgraderListTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}
	
	private List<UpgradeProcessor> getUpgradeProcessors(){

		// 1 -- Create the upgraders
		UpgradeProcessor p1 = new UpgradeProcessor(1,0,0) {
			
			@Override
			public boolean doUpgrade() {
				return true;
			}
		};
		
		UpgradeProcessor p2 = new UpgradeProcessor(1,0,2) {
			
			@Override
			public boolean doUpgrade() {
				return true;
			}
		};
		
		UpgradeProcessor p3 = new UpgradeProcessor(2,0,1) {
			
			@Override
			public boolean doUpgrade() {
				return true;
			}
		};
		
		UpgradeProcessor p4 = new UpgradeProcessor(2,2,1) {
			
			@Override
			public boolean doUpgrade() {
				return true;
			}
		};
		
		UpgradeProcessor p5 = new UpgradeProcessor(3,0,0) {
			
			@Override
			public boolean doUpgrade() {
				return true;
			}
		};
		
		UpgradeProcessor p6 = new UpgradeProcessor() {
			
			@Override
			public boolean doUpgrade() {
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
		list.add(p6);
		
		return list;
	}
	
	public void testUpgraderListFilter(){
		
		UpgraderList upgraderList = new UpgraderList();
		
		// 1 -- Populate the list of processors
		List<UpgradeProcessor> list = getUpgradeProcessors();
		for (UpgradeProcessor upgradeProcessor : list) {
			upgraderList.add(upgradeProcessor);
		}
		
		// 2 -- Make sure only the filtered entries are returned
		if( upgraderList.getList(2,0,1).size() != 3 ){
			//This should return all of the upgraders that upgrade the schema to a newer version as well as the ones that should always be executed.
			fail("Incorrect number of upgraders were returned when filtering the list");
		}
	}
	
	public void testUpgraderListAdd(){
		
		UpgraderList upgraderList = new UpgraderList();
		
		// 1 -- Populate the list of processors
		List<UpgradeProcessor> list = getUpgradeProcessors();
		for (UpgradeProcessor upgradeProcessor : list) {
			upgraderList.add(upgradeProcessor);
		}
		
		// 2 -- Make sure all of the entries are returned
		if( list.size() != upgraderList.getList().size() ){
			fail("Not all upgraders were added to the list");
		}
	}

}
