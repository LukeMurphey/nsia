package net.lukeMurphey.nsia;

import java.sql.Timestamp;
import java.util.Hashtable;

import net.lukeMurphey.nsia.SiteGroupManagement.SiteGroupDescriptor;
import net.lukeMurphey.nsia.scanRules.ScanResult;
import net.lukeMurphey.nsia.scanRules.ScanResultCode;

public class SiteGroupScanResult {
	protected ScanResultCode resultCode = null;
	protected SiteGroupDescriptor siteGroupDescriptor;
	protected ScanResult[] scanResults;
	protected int deviationCount;
	protected int deviatingRules;
	protected int incompleteRules;
	protected Timestamp endScanTime;
	protected Timestamp startScanTime;
	protected long siteGroupId;
	
	public SiteGroupScanResult( long siteGroupId, SiteGroupDescriptor descriptor, ScanResult[] results ){
		deviationCount = 0;
		deviatingRules = 0;
		incompleteRules = 0;
		
		for( int c = 0; c < results.length; c++ ){
			
			if( results[c] != null ){
				// Count the deviations
				if( results[c].getDeviations() > 0){
					
					if( results[c].getResultCode().equals(ScanResultCode.SCAN_COMPLETED) == false){
						incompleteRules++;
					}
					//else{//if()
					deviationCount += results[c].getDeviations();
					deviatingRules++;
					//}
				}
				
				// Determine the overall status
				resultCode = ScanResultCode.SCAN_COMPLETED;//Assume scans completed unless otherwise noted
				if( results[c].getResultCode().equals(ScanResultCode.SCAN_COMPLETED) == false ){
					resultCode = ScanResultCode.SCAN_FAILED;
					incompleteRules++;
				}
			}
		}
		
		siteGroupDescriptor = descriptor;
		scanResults = new ScanResult[results.length];
		System.arraycopy(results, 0, scanResults, 0, results.length);
		
		this.siteGroupId = siteGroupId;
	}
	
	public SiteGroupDescriptor getSiteGroupDescriptor(){
		return siteGroupDescriptor;
	}
	
	public ScanResult[] getScanResults(){
		ScanResult[] scanResultsCopy = new ScanResult[scanResults.length];
		System.arraycopy(scanResults, 0, scanResultsCopy, 0, scanResults.length);
		
		return scanResultsCopy;
	}
	
	/**
	 * Get the number of deviations noted in the rule results for the given site group analysis phase. 
	 * @return
	 */
	public int getDeviations(){
		return deviationCount;
	}
	
	/**
	 * Get the number of rules that failed to accept. 
	 * @return
	 */
	public int getDeviatingRules(){
		return deviatingRules;
	}
	
	/**
	 * Get the number of rules that failed to run completely. 
	 * @return
	 */
	public int getIncompleteRules(){
		return incompleteRules;
	}
	
	/**
	 * Get the time that the scan started (i.e. the time of the oldest scan rule). 
	 * @return
	 */
	public Timestamp getStartScanTime(){
		return startScanTime;
	}
	
	/**
	 * Get the time that the scan ended (i.e. the time of the last rule).
	 * @return
	 */
	public Timestamp getEndScanTime(){
		return endScanTime;
	}
	
	/**
	 * Get the overall scan result code. This is based upon a pessimistic summary that includes the result
	 * code of the most significant issue. That is, the result code will indicate a incomplete scan if one rule
	 * does not complete, or scan failure if at least one rule is found in violation.
	 * @return
	 */
	public ScanResultCode getResultCode(){
		return resultCode;
	}
	
	/**
	 * Create a hash table version of this result code.
	 * @return
	 */
	public Hashtable<String, Object> toHashtable(){
		Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
		
		hashtable.put("SiteGroupID", Double.valueOf( siteGroupId ) );
		hashtable.put("SiteGroupResultCode", Integer.valueOf( getResultCode().getId() ) );
		hashtable.put("IncompleteRuleScans", Integer.valueOf( incompleteRules ) );
		
		if( startScanTime != null)
			hashtable.put("StartScanTime",  Double.valueOf( startScanTime.getTime() ) );
		
		if( endScanTime != null)
			hashtable.put("EndScanTime",  Double.valueOf( endScanTime.getTime() ) );
		
		
		hashtable.put("Deviations", Integer.valueOf( deviationCount ) );
		hashtable.put("DeviatingRules", Integer.valueOf( deviatingRules ) );
		
		hashtable.put("SiteGroupDescriptor", siteGroupDescriptor.toHashtable());
		
		/*Vector scanResultsVector = new Vector();
		
		for( int c = 0; c < scanResults.length; c++ ){
			scanResultsVector.add(scanResults[c]);
		}
		
		hashtable.put("ScanResults", scanResultsVector);*/
		
		return hashtable;
	}
	
	/**
	 * Get the identifier of the site group that was scanned.
	 * @return
	 */
	public long getSiteGroupId(){
		return siteGroupId;
	}
}
