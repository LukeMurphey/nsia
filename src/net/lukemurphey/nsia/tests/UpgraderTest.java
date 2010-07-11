package net.lukemurphey.nsia.tests;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.upgrade.UpgradeFailureException;
import net.lukemurphey.nsia.upgrade.UpgradeProcessor;
import net.lukemurphey.nsia.upgrade.Upgrader;
import net.lukemurphey.nsia.upgrade.UpgraderList;
import junit.framework.TestCase;

public class UpgraderTest extends TestCase {

	Application app = null;
	
	@Override
	public void setUp(){
		app = getDummyApplication();
		
	}
	
	public Application getDummyApplication(){
		Application app = new Application();
		return app;
	}
	
	public void testUpgrader() throws UpgradeFailureException{
		
		// Create an upgrade processor
		UpgradeProcessor p1 = new UpgradeProcessor(1,0,1) {
			
			@Override
			public boolean doUpgrade( Application app ) {
				return true;
			}
		};
		
		UpgraderList list = new UpgraderList();
		list.add(p1);
		
		Upgrader upgrader = new Upgrader(app);
		
		if( upgrader.isUpgradeNecessary( 1, 0, 0) == false ){
			fail("Upgrader indicated that no upgrade was necessary");
		}
		
		int result = upgrader.peformUpgrades(list.getList(), 1, 0, 0 );
		
		if( result != 1 ){
			fail("Upgrader returned an incorrect count of upgraders executed (" + result + ")");
		}
	}
	
}
