package net.lukemurphey.nsia.scan;

/**
 * This class is intended to provide an enumerated type describing the result codes of a specimen scan.
 * @author luke
 *
 */
public class ScanResultCode {
	private int id = 0;
	private String desc = null;
	
	public static final ScanResultCode UNREADY = new ScanResultCode(0, "Not Ready");
	public static final ScanResultCode SCAN_COMPLETED = new ScanResultCode(1, "Scan Completed");
	public static final ScanResultCode SCAN_FAILED = new ScanResultCode(2, "Scan failed to complete");
	public static final ScanResultCode READY = new ScanResultCode(3, "Ready");
	public static final ScanResultCode PENDING = new ScanResultCode(4, "Not Scanned Yet");
	public static final ScanResultCode SCAN_TERMINATED = new ScanResultCode(5, "Scan terminated");
	
	/**
	 * Constructor is private since this an enumerated type.
	 * @param resultId
	 * @param description
	 */
	private ScanResultCode( int resultId, String description ){
		id = resultId;
		desc = description;
	}
	
	/**
	 * Returns the ID associated with the result code.
	 * @precondition None
	 * @postcondition The ID is returned
	 */
	public int getId(){
		return id;
	}
	
	/**
	 * Returns a string representing the result code.
	 * @precondition None
	 * @postcondition A description of the result code is returned
	 */
	public String toString(){
		return desc;
	}
	
	/**
	 * Returns a string representing the result code.
	 * @precondition None
	 * @postcondition A description of the result code is returned
	 */
	public String getDescription(){
		return desc;
	}
	
	/**
	 * Get a result code that matches the given ID, or return null if the ID is invalid.
	 * @precondition The ID must be valid
	 * @postcondition A scan result code will be returned, or null if the ID is invalid
	 * @param id
	 * @return
	 */
	public static ScanResultCode getScanResultCodeById( int id ){
		if( id == UNREADY.getId() )
			return UNREADY;
		else if( id == SCAN_COMPLETED.getId() )
			return SCAN_COMPLETED;
		else if( id == READY.getId() )
			return READY;
		else if( id == SCAN_FAILED.getId() )
			return SCAN_FAILED;
		else if( id == SCAN_TERMINATED.getId() )
			return SCAN_TERMINATED;
		else
			return null;
	}
	
	/**
	 * Determines if two result codes are equal.
	 * @param resultCode
	 * @return
	 */
	public boolean equals( ScanResultCode resultCode ){
		if( resultCode == null ){
			return false;
		}
		
		return (id == resultCode.id);
	}
	
	public int hashCode(){
		return id;
	}
	
	public boolean equals( Object obj ){
		if( obj instanceof ScanResultCode ){
			return equals( (ScanResultCode)obj);
		}
		else{
			return false;
		}
	}
}
