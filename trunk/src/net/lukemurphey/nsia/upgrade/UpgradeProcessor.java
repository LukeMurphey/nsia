package net.lukemurphey.nsia.upgrade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.apache.commons.lang.StringEscapeUtils;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.Application.DatabaseAccessType;

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
	public abstract boolean doUpgrade( Application application ) throws UpgradeFailureException;

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
	
	/**
	 * Determines if this upgrader is intended to upgrade the schema to a version later than the given version (that is, this upgrader should be executed against the system with the given version)..
	 * @param other_version_major
	 * @param other_version_minor
	 * @param other_version_revision
	 * @return
	 */
	public boolean isBefore( int other_version_major, int other_version_minor, int other_version_revision) {
		if( compareTo( other_version_major, other_version_minor, other_version_revision ) < 0 ){
			return true;
		}
		else{
			return false;
		}
	}
	
	/**
	 * Determines if this upgrader is intended to upgrade the schema to a version prior than the given version (that is, this upgrader should not be executed against the system with the given version).
	 * @param other_version_major
	 * @param other_version_minor
	 * @param other_version_revision
	 * @return
	 */
	public boolean isAfter( int other_version_major, int other_version_minor, int other_version_revision) {
		if( compareTo( other_version_major, other_version_minor, other_version_revision ) > 0 ){
			return true;
		}
		else{
			return false;
		}
	}

	/**
	 * Determines if the schema upgrader has a version at all. If not, then this upgrader should be executed every time an upgrade is requested.
	 * @return
	 */
	public boolean hasVersion(){
		if( version_major == 0 && version_minor == 0 && version_revision == 0 ){
			return false;
		}
		else{
			return true;
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
	
	/**
	 * Determine if the given column exists in the database.
	 * @param app
	 * @param table
	 * @param column
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 */
	protected static boolean hasColumn(Application app, String table, String column ) throws NoDatabaseConnectionException, SQLException{
		
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try{
			connection = app.getDatabaseConnection(DatabaseAccessType.ADMIN);
			
			statement = connection.prepareStatement("Select * from " + StringEscapeUtils.escapeSql( table ) );
			statement.setMaxRows(1);
			result = statement.executeQuery();
			
			ResultSetMetaData metaData = result.getMetaData();
			
			for (int i = 1; i <= metaData.getColumnCount(); i++) {
				if( metaData.getColumnName(i).equalsIgnoreCase(column.toLowerCase()) ){
					return true;
				}
			}
		}
		finally{
			if( connection != null ){
				connection.close();
			}
			
			if( statement != null ){
				statement.close();
			}
			
			if( result != null ){
				result.close();
			}
		}
		
		return false;
	}
}
