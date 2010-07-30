package net.lukemurphey.nsia.upgrade;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.ApplicationConfiguration;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;

/**
 * This class performs upgrades that require access to the inner parts of NSIA.
 * @author Luke
 *
 */
public class Upgrader {

	private Application app = null;
	
	private static Pattern versionRegex = Pattern.compile("([0-9]+)[.]([0-9]+)[.]([0-9]+)");
	
	public Upgrader(Application application){
		
		// 0 -- Precondition check
		if( application == null ){
			throw new IllegalArgumentException("The application reference cannot be null");
		}
		
		// 1 -- Initialize the class
		this.app = application;
	}
	
	public int peformUpgrades() throws UpgradeFailureException{
		
		// 1 -- Get the schema version so that we can determine which upgraders to run
		SchemaVersion schemaVersion = new SchemaVersion(app);
		
		// 2 -- Get the upgraders that perform changes past the given version
		List<UpgradeProcessor> list = null;
		if( schemaVersion == null ){
			list = UpgraderList.getInstance().getList();
		}
		else{
			list = UpgraderList.getInstance().getList( schemaVersion.getMajor(), schemaVersion.getMinor(), schemaVersion.getRevision() );
		}
		
		// 3 -- Perform the upgrade
		return peformUpgrades( list );
	}
	
	/**
	 * Performs all necessary upgrades.
	 * @return A integer indicating how many upgrades were performed.
	 * @throws UpgradeFailureException
	 */
	public int peformUpgrades( List<UpgradeProcessor> list ) throws UpgradeFailureException{
		
		// 1 -- Determine if an upgrade is necessary
		if( isUpgradeNecessary() == false ){
			return 0;
		}
		
		// 2 -- Perform the relevant upgraders
		int upgradesDone = 0;
		
		for (UpgradeProcessor upgradeProcessor : list) {
			if( upgradeProcessor.doUpgrade( app ) ){
				upgradesDone = upgradesDone + 1;
			}
		}
		
		// 3 -- Save the database schema version
		setSchemaVersion();
		
		return upgradesDone;
	}
	
	/**
	 * Performs all necessary upgrades.
	 * @param list
	 * @param major
	 * @param minor
	 * @param revision
	 * @return
	 * @throws UpgradeFailureException
	 */
	public int peformUpgrades( List<UpgradeProcessor> list, int major, int minor, int revision ) throws UpgradeFailureException{
		
		// 1 -- Determine if an upgrade is necessary
		if( isUpgradeNecessary( major, minor, revision ) == false ){
			return 0;
		}
		
		// 2 -- Perform the relevant upgraders
		int upgradesDone = 0;
		
		for (UpgradeProcessor upgradeProcessor : list) {
			if( upgradeProcessor.doUpgrade( app ) ){
				upgradesDone = upgradesDone + 1;
			}
		}
		
		// 3 -- Save the database schema version
		setSchemaVersion();
		
		return upgradesDone;
	}
	
	/**
	 * Sets the application database schema version to the current version identifier.
	 * @throws UpgradeFailureException
	 */
	private void setSchemaVersion() throws UpgradeFailureException{
		
		ApplicationConfiguration appConfig = app.getApplicationConfiguration();
		
		// No application configuration is available, thus we cannot set the schema version
		if( appConfig == null ){
			return;
		}
		
		try {
			appConfig.setDatabaseSchemaVersion( Application.VERSION_MAJOR + "." + Application.VERSION_MINOR + "." + Application.VERSION_REVISION );
		} catch (NoDatabaseConnectionException e) {
			throw new UpgradeFailureException("Upgrade failed while attempting to reset database schema version", e);
		} catch (SQLException e) {
			throw new UpgradeFailureException("Upgrade failed while attempting to reset database schema version", e);
		} catch (InputValidationException e) {
			throw new UpgradeFailureException("Upgrade failed while attempting to reset database schema version", e);
		}
		
	}
	
	/**
	 * This class describes the database schema version
	 * @author Luke
	 *
	 */
	private static class SchemaVersion{
		
		private int major = 0;
		private int minor = 0;
		private int revision = 0;
		
		public SchemaVersion(Application app ) throws UpgradeFailureException{
			
			// 1 -- Determine the current database schema version
			ApplicationConfiguration appConfig = app.getApplicationConfiguration();
			
			// 2 -- Get the current version
			String ver;
			try {
				ver = appConfig.getDatabaseSchemaVersion();
			} catch (NoDatabaseConnectionException e) {
				throw new UpgradeFailureException(e);
			} catch (SQLException e) {
				throw new UpgradeFailureException(e);
			} catch (InputValidationException e) {
				throw new UpgradeFailureException(e);
			}
			
			// 3 -- Parse the version
			if( ver != null ){
				Matcher m = versionRegex.matcher(ver);
				if( m.matches() && m.groupCount() == 3 ){
					major = Integer.parseInt( m.group(1) );
					minor = Integer.parseInt( m.group(2) );
					revision = Integer.parseInt( m.group(3) );
				}
			}
		}
		
		public int getMajor(){
			return major;
		}
		
		public int getMinor(){
			return minor;
		}
		
		public int getRevision(){
			return revision;
		}
		
	}
	
	/**
	 * Determines if an upgrade operation is necessary.
	 * @return
	 * @throws UpgradeFailureException
	 */
	public boolean isUpgradeNecessary() throws UpgradeFailureException{
		
		// 1 -- Determine the current database schema version
		ApplicationConfiguration appConfig = app.getApplicationConfiguration();
		
		// 2 -- Get the current version
		String ver;
		try {
			ver = appConfig.getDatabaseSchemaVersion();
		} catch (NoDatabaseConnectionException e) {
			throw new UpgradeFailureException(e);
		} catch (SQLException e) {
			throw new UpgradeFailureException(e);
		} catch (InputValidationException e) {
			throw new UpgradeFailureException(e);
		}
		
		// If no schema version exists, then run the upgraders
		if( ver == null ){
			return true;
		}
		
		int major = 0;
		int minor = 0;
		int revision = 0;
		
		// 3 -- Parse the version
		Matcher m = versionRegex.matcher(ver);
		if( m.matches() && m.groupCount() == 3 ){
			major = Integer.parseInt( m.group(1) );
			minor = Integer.parseInt( m.group(2) );
			revision = Integer.parseInt( m.group(3) );
		}
		
		// 4 -- Compare the current application version to the database schema
		return isUpgradeNecessary(major, minor, revision);
		
	}
	
	/**
	 * Determines if an upgrade operation is necessary.
	 * @param major
	 * @param minor
	 * @param revision
	 * @return
	 * @throws UpgradeFailureException
	 */
	public boolean isUpgradeNecessary( int major, int minor, int revision ) throws UpgradeFailureException{
		// Compare the current application version to the database schema
		if( Application.VERSION_MAJOR != major || Application.VERSION_MINOR != minor || Application.VERSION_REVISION != revision ){
			return true;
		}
		else{
			return false;
		}
	}
	
}
