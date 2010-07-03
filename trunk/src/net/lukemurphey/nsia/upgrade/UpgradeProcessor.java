package net.lukemurphey.nsia.upgrade;

/**
 * This class performs an upgrade between the current schema and the next one.
 * 
 * @author Luke
 *
 */
public abstract class UpgradeProcessor implements Comparable<UpgradeProcessor> {

	protected int version_major = 0;
	protected int version_minor = 0;
	protected int version_revision = 0;
	
	/**
	 * Default constructor.
	 */
	protected UpgradeProcessor( ){
		
	}
	
	/**
	 * Constructor that sets the version that the upgrader works with (i.e. the version that the upgrader patches the system to).
	 * @param version_major
	 * @param version_minor
	 * @param version_revision
	 */
	protected UpgradeProcessor(int version_major, int version_minor, int version_revision ){
		this.version_major = version_major;
		this.version_minor = version_minor;
		this.version_revision = version_revision;
	}
	
	/**
	 * Perform the upgrade process. Returns a boolean indicating if the upgrade was necessary and applied.
	 * @return
	 */
	public abstract boolean doUpgrade();

	public int compareTo(UpgradeProcessor other) {
		return compareTo( other.version_major, other.version_minor, other.version_revision );
	}

	/**
	 * Compares this upgrader processor to the given version information to determine if an upgrade is necessary.
	 * @param other_version_major
	 * @param other_version_minor
	 * @param other_version_revision
	 * @return
	 */
	public int compareTo( int other_version_major, int other_version_minor, int other_version_revision) {
		
		// Return a negative number if the other object is greater than this one
		if( version_major != other_version_major ){
			return version_major - other_version_major;
		}
		else if( version_minor != other_version_minor ){
			return version_minor - other_version_minor;
		}
		else if( version_revision != other_version_revision ){
			return version_revision - other_version_revision;
		}
		
		return 0;
	}
	
	public boolean isBefore( int other_version_major, int other_version_minor, int other_version_revision) {
		if( compareTo( other_version_major, other_version_minor, other_version_revision ) < 0 ){
			return true;
		}
		else{
			return false;
		}
	}
	
	public boolean isAfter( int other_version_major, int other_version_minor, int other_version_revision) {
		if( compareTo( other_version_major, other_version_minor, other_version_revision ) > 0 ){
			return true;
		}
		else{
			return false;
		}
	}

	/**
	 * Compare the two upgrader processors to each other.
	 * @param o1
	 * @param o2
	 * @return
	 */
	public static int compare(UpgradeProcessor o1, UpgradeProcessor o2) {
		return o1.compareTo(o2);
	}
	
}
