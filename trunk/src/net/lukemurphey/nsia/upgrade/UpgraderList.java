package net.lukemurphey.nsia.upgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * Includes a list of upgrade processors.
 * @author Luke
 *
 */
public class UpgraderList {
	
	//The static list of upgraders
	private static UpgraderList standardList = null;
	
	// The internal list of upgraders
	private List<UpgradeProcessor> upgraders = new ArrayList<UpgradeProcessor>();
	
	/**
	 * Get a reference to the shared instance of the upgrader list.
	 * @return
	 */
	public static synchronized UpgraderList getInstance(){
		if( standardList == null ){
			standardList = new UpgraderList();
			standardList.populateList();
		}
		
		return standardList;
	}
	
	/**
	 * Populate the internal list of upgraders.
	 */
	private void populateList(){
		
		// 1 -- Create the list
		upgraders = new ArrayList<UpgradeProcessor>();
		
		// 2 -- Add the upgraders to the list
		
		// 3 -- Sort the list
		Collections.sort(upgraders);
	}
	
	/**
	 * Get the complete list of upgraders.
	 * @return
	 */
	public List<UpgradeProcessor> getList(){
		
		if( upgraders == null ){
			populateList();
		}
		
		return upgraders;
	}
	
	/**
	 * Add the given upgrader to the list.
	 * @param processor
	 */
	public void add(UpgradeProcessor processor){
		upgraders.add(processor);
	}
	
	/**
	 * Get a list of all the upgraders that are after the given version identifier.
	 * @param version_major
	 * @param version_minor
	 * @param version_revision
	 * @return
	 */
	public List<UpgradeProcessor> getList( int version_major, int version_minor, int version_revision ){
		
		List<UpgradeProcessor> upgraders = getList();
		
		List<UpgradeProcessor> relevantUpgraders = new ArrayList<UpgradeProcessor>();
		
		for (UpgradeProcessor upgradeProcessor : upgraders) {
			//Add the upgrader to the list if it can upgrade the given schema to a later version or if is not associated with a version (should always be executed).
			if( upgradeProcessor.hasVersion() == false || upgradeProcessor.isAfter(version_major, version_minor, version_revision) ){
				relevantUpgraders.add(upgradeProcessor);
			}
		}
		
		return relevantUpgraders;
	}
	
}
