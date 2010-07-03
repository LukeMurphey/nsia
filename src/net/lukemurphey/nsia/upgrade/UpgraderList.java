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
	
	// The list of upgraders
	private static List<UpgradeProcessor> upgraders = null;
	
	/**
	 * Populate the internal list of upgraders.
	 */
	private static void populateList(){
		
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
	public static List<UpgradeProcessor> getList(){
		
		if( upgraders == null ){
			populateList();
		}
		
		return upgraders;
	}
	
	/**
	 * Get a list of all the upgraders that are after the given version identifier.
	 * @param version_major
	 * @param version_minor
	 * @param version_revision
	 * @return
	 */
	public static List<UpgradeProcessor> getList( int version_major, int version_minor, int version_revision ){
		
		List<UpgradeProcessor> upgraders = getList();
		
		List<UpgradeProcessor> relevantUpgraders = new ArrayList<UpgradeProcessor>();
		
		for (UpgradeProcessor upgradeProcessor : upgraders) {
			
			if( upgradeProcessor.isAfter(version_major, version_minor, version_revision) ){
				relevantUpgraders.add(upgradeProcessor);
			}
		}
		
		return relevantUpgraders;
	}
	
}
