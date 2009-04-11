package net.lukeMurphey.nsia;

import java.sql.*;
import java.util.*;

public abstract class DatabaseInitializer {
	
	protected Connection connection = null;
	
	public enum Result{
		NO_ACTION_NECESSARY,
		UPGRADE_SUCCESS,
		UPGRADE_FAILED,
		TABLE_CREATED,
		TABLE_CREATION_FAILED
	}
	
	protected String[] tableList;
	protected String[] indexList;
	
	protected static class DatabaseInitializationState{
		protected Result applicationParameters;
		protected Result attemptedLogins;
		protected Result firewall;
		protected Result groups;
		protected Result groupUsersMap;
		protected Result httpHashScanResult;
		protected Result httpHashScanRule;
		protected Result httpHeaderScanResult;
		protected Result httpHeaderScanRule;
		protected Result portScanRule;
		protected Result portScanResult;
		protected Result objectMap;
		protected Result performanceMetrics;
		protected Result permissions;
		protected Result rights;
		protected Result scanResult;
		protected Result scanRule;
		protected Result sessions;
		protected Result siteGroups;
		protected Result users;
		protected Result signatures;
		protected Result scriptEnvironment;
		protected Result specimenArchive;
		protected Result eventLog;
		protected Result httpDiscovery;
		protected Result ruleURL;
		protected Result httpSignatureScanResult;
		protected Result matchedRules;
		protected Result ruleFilter;
		protected Result httpDiscoveryResult;
		protected Result signatureExceptions;
		protected Result serviceScanRule;
		protected Result serviceScanResult;
		protected Result actions;
		protected Result eventLogHooks;
		protected Result definitionErrors;
		
		public Result getApplicationParametersTableState(){
			return applicationParameters;
		}
		
		public Result getAttemptedLoginsTableState(){
			return attemptedLogins;
		}
		
		public Result getFirewallTableState(){
			return firewall;
		}
		
		public Result getGroupsTableState(){
			return groups;
		}
		
		public Result getGroupUsersMapTableState(){
			return groupUsersMap;
		}
		
		public Result getStaticHttpScanResultTableState(){
			return httpHashScanResult;
		}
		
		public Result getStaticHttpScanRuleTableState(){
			return httpHashScanRule;
		}
		
		public Result getStaticHttpHeaderScanResultTableState(){
			return httpHeaderScanResult;
		}
		
		public Result getPortScanRuleTableState(){
			return portScanRule;
		}
		
		public Result getPortScanResultTableState(){
			return portScanResult;
		}
		
		public Result getStaticHttpHeaderScanRuleTableState(){
			return httpHeaderScanRule;
		}
		
		public Result getObjectMapTableState(){
			return objectMap;
		}
		
		public Result getPerformanceMetricsTableState(){
			return performanceMetrics;
		}
		
		public Result getPermissionsTableState(){
			return permissions;
		}
		
		public Result getRightsTableState(){
			return rights;
		}
		
		public Result getScanResultTableState(){
			return scanResult;
		}
		
		public Result getScanRuleTableState(){
			return scanRule;
		}
		
		public Result getSessionsTableState(){
			return sessions;
		}
		
		public Result getSiteGroupsTableState(){
			return siteGroups;
		}
		
		public Result getUsersTableState(){
			return users;
		}
		
		public Result getSignaturesTableState(){
			return signatures;
		}
		
		public Result getScriptEnvironmentTableState(){
			return scriptEnvironment;
		}
		
		public Result getSpecimenArchiveTableState(){
			return specimenArchive;
		}
		
		public Result getEventLogTableState(){
			return eventLog;
		}
		
		public Result getRuleURLTableState(){
			return ruleURL;
		}
		
		public Result getHttpDiscoveryTableState(){
			return httpDiscovery;
		}
		
		public Result getHttpSignatureScanResultTableState(){
			return httpSignatureScanResult;
		}
		
		public Result getMatchedRulesTableState(){
			return matchedRules;
		}
		
		public Result getRuleFilterTableState(){
			return ruleFilter;
		}
		
		public Result getHttpDiscoveryResultTable(){
			return httpDiscoveryResult;
		}
		
		public Result getSignatureExceptionsTable(){
			return signatureExceptions;
		}
		
		public Result getServiceScanRuleTable(){
			return serviceScanRule;
		}
		
		public Result getDefinitionErrorsTable(){
			return definitionErrors;
		}
	}
	
	public DatabaseInitializer( Connection connection ){
		this.connection = connection;
	}
	
	protected boolean doesTableExist(String tableName){
		for(int c = 0; c < tableList.length; c++){
			
			if( tableList[c].equalsIgnoreCase(tableName))
				return true;
			
		}
		
		return false;
	}
	
	protected void populateIndexList() throws SQLException, NoDatabaseConnectionException{
		Vector<String> indexListVector = new Vector<String>();
		ResultSet rs = null;

		DatabaseMetaData dbm = getConnection().getMetaData();
		rs = dbm.getIndexInfo(null, null, null, false, true);

		while (rs.next())
		{
			String str = rs.getString("INDEX_NAME");
			indexListVector.add(str);
		}
		
		indexList = new String[indexListVector.size()];
		
		for(int c = 0; c < indexListVector.size(); c++){
			indexList[c] = (String)indexListVector.get(c);
		}
	}
	
	protected void populateTableList() throws SQLException, NoDatabaseConnectionException{
		Vector<String> tableListVector = new Vector<String>();
		ResultSet rs = null;

		DatabaseMetaData dbm = getConnection().getMetaData();
		String types[] = { "TABLE" };
		rs = dbm.getTables(null, null, null, types);

		while (rs.next())
		{
			String str = rs.getString("TABLE_NAME");
			tableListVector.add(str);
		}
		
		tableList = new String[tableListVector.size()];
		
		for(int c = 0; c < tableListVector.size(); c++){
			tableList[c] = (String)tableListVector.get(c);
		}
	}
	
	public DatabaseInitializationState performSetup( ) throws SQLException, NoDatabaseConnectionException{
		
		// 1 -- Get a list of tables
		populateTableList();
		
		
		// 2 -- Check the tables
		DatabaseInitializationState initResults = new DatabaseInitializationState();
		
		initResults.applicationParameters = createApplicationParametersTable();
		initResults.attemptedLogins = createAttemptedLoginsTable();
		initResults.firewall = createFirewallTable();
		initResults.groups = createGroupsTable();
		initResults.groupUsersMap = createGroupsUsersMapTable();
		
		initResults.httpHashScanRule = createStaticHttpRuleTable();
		initResults.httpHashScanResult = createStaticHttpResultsTable();
		initResults.httpHeaderScanRule = createStaticHttpHeaderRulesTable();
		initResults.httpHeaderScanResult = createStaticHttpHeaderResultsTable();
		initResults.objectMap = createObjectMapTable();
		
		initResults.performanceMetrics = createPerformanceMetricsTable();
		initResults.permissions = createPermissionsTable();
		initResults.rights = createRightsTable();
		initResults.scanResult = createScanResultTable();
		initResults.scanRule = createScanRuleTable();
		
		initResults.sessions = createSessionsTable();
		initResults.siteGroups = createSiteGroupsTable();
		initResults.users = createUsersTable();
		initResults.signatures = createDefinitionsTable();
		initResults.scriptEnvironment = createScriptEnvironmentTable();
		initResults.eventLog = createEventLogTable();
		
		initResults.httpDiscovery = createHttpDiscoveryRuleTable();
		initResults.ruleURL = createRuleURLTable();
		initResults.httpSignatureScanResult = createSignatureScanResultTable();
		initResults.matchedRules = createMatchedRulesTable();
		initResults.httpDiscoveryResult = createHttpDiscoveryResultTable();
		initResults.signatureExceptions = createDefinitionPolicyTable();
		
		initResults.serviceScanRule = createServiceScanRuleTable();
		initResults.serviceScanResult = createServiceScanResultTable();
		initResults.actions = createActionsTable();
		initResults.eventLogHooks = createEventLogHooksTable();
		initResults.definitionErrors = createDefinitionErrorTable();
		
		// 3 -- Insert the necessary entries
		if( initResults.getRightsTableState() == Result.TABLE_CREATED )
			insertRights();
		
		if( initResults.getUsersTableState() == Result.TABLE_CREATED)
			insertDefaultUser();
		
		getConnection().close();
		
		return initResults;
	}
	
	protected Connection getConnection() throws NoDatabaseConnectionException{
		
		// 0 -- Precondition Check: make sure a database connection is available		
		if( connection == null )
			throw new NoDatabaseConnectionException();
		
		
		// 1 -- Return the connection
		return connection;
	}
	
	protected void insertRights( )throws SQLException, NoDatabaseConnectionException {
		
		// 1 -- User management
		insertRight( "Users.Add", "Create New Users");
		insertRight( "Users.Edit", "Edit User");
		insertRight( "Users.View", "View Users' Details (including rights)");
		insertRight( "Users.Delete", "Delete Users");
		insertRight( "Users.Unlock", "Unlock Accounts (due to repeated authentication attempts)");
		insertRight( "Users.UpdatePassword", "Update Other's Password (applies only to the other users' accounts)");
		insertRight( "Users.UpdateOwnPassword", "Update Account Details (applies only to the users' own account)");
		insertRight( "Users.Sessions.Delete", "Delete Users' Sessions (kick users off)");
		insertRight( "Users.Sessions.View", "View Users' Sessions (see who is logged in)");
		
		// 2 -- Group management
		insertRight( "Groups.Add", "Create New Groups");
		insertRight( "Groups.View", "View Groups");
		insertRight( "Groups.Edit", "Edit Groups");
		insertRight( "Groups.Delete", "Delete Groups");
		insertRight( "Groups.Membership.Edit", "Manage Group Membership");
		
		// 3 -- Site group management
		insertRight( "SiteGroups.View", "View Site Groups");
		insertRight( "SiteGroups.Add", "Create New Site Group");
		insertRight( "SiteGroups.Delete", "Delete Site Groups");
		insertRight( "SiteGroups.Edit", "Edit Site Groups");
		
		// 4 -- System configuration
		insertRight( "System.Information.View", "View System Information and Status");
		insertRight( "System.Configuration.Edit", "Modify System Configuration");
		insertRight( "System.Firewall.View", "View Firewall Configuration");
		insertRight( "System.Firewall.Edit", "Change Firewall Configuration");
		insertRight( "System.ControlScanner", "Start/Stop Scanner");
		insertRight( "SiteGroups.ScanAllRules", "Allow Gratuitous Scanning of All Rules");
		
	}
	
	protected synchronized long allocateObjectId( String databaseTableDescription ) throws SQLException, NoDatabaseConnectionException{
		
		connection = getConnection();
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try{
			statement = connection.prepareStatement("Insert into ObjectMap(\"Table\") values(?)", PreparedStatement.RETURN_GENERATED_KEYS);
			statement.setString( 1, databaseTableDescription );
			
			statement.executeUpdate();
			
			result = statement.getGeneratedKeys();
			
			if( result.next() )
				return result.getLong(1);
			else
				return -1;
		}
		finally{
			if (statement != null )
				statement.close();
			
			if (result != null )
				result.close();
		}
		
	}
	
	protected boolean insertRight( String rightName, String rightDescription )throws SQLException, NoDatabaseConnectionException {
		
		// 1 -- Allocate the object ID
		long objectId = allocateObjectId( "UserRights");
		
		// 2 -- Insert the right
		PreparedStatement statement = null;
		
		try{
			statement = connection.prepareStatement("insert into Rights(RightName, RightDescription, ObjectID) values (?,?,?)");
			statement.setString(1, rightName);
			statement.setString(2, rightDescription);
			statement.setLong(3, objectId);
			
			int results = statement.executeUpdate();
			
			if( results > 0 )
				return true;
			else
				return false;
			
		}
		finally{
			if (statement != null )
				statement.close();
		}
	}
	
	protected void insertDefaultUser( )throws SQLException, NoDatabaseConnectionException {
		
	}
	
	protected abstract Result createApplicationParametersTable()throws SQLException, NoDatabaseConnectionException ;
	protected abstract Result createAttemptedLoginsTable()throws SQLException, NoDatabaseConnectionException ;
	protected abstract Result createFirewallTable()throws SQLException, NoDatabaseConnectionException ;
	protected abstract Result createGroupsTable()throws SQLException, NoDatabaseConnectionException ;
	protected abstract Result createGroupsUsersMapTable()throws SQLException, NoDatabaseConnectionException ;
	
	protected abstract Result createStaticHttpRuleTable()throws SQLException, NoDatabaseConnectionException ;
	protected abstract Result createStaticHttpResultsTable()throws SQLException, NoDatabaseConnectionException ;
	protected abstract Result createStaticHttpHeaderRulesTable()throws SQLException, NoDatabaseConnectionException ;
	protected abstract Result createStaticHttpHeaderResultsTable()throws SQLException, NoDatabaseConnectionException ;
	protected abstract Result createObjectMapTable()throws SQLException, NoDatabaseConnectionException ;
	
	protected abstract Result createPerformanceMetricsTable()throws SQLException, NoDatabaseConnectionException ;
	protected abstract Result createPermissionsTable()throws SQLException, NoDatabaseConnectionException ;
	protected abstract Result createRightsTable()throws SQLException, NoDatabaseConnectionException ;
	protected abstract Result createScanResultTable()throws SQLException, NoDatabaseConnectionException ;
	protected abstract Result createScanRuleTable()throws SQLException, NoDatabaseConnectionException ;
	protected abstract Result createPortScanResultTable()throws SQLException, NoDatabaseConnectionException ;
	protected abstract Result createPortScanRuleTable()throws SQLException, NoDatabaseConnectionException ;
	
	protected abstract Result createSessionsTable()throws SQLException, NoDatabaseConnectionException ;
	protected abstract Result createSiteGroupsTable()throws SQLException, NoDatabaseConnectionException ;
	protected abstract Result createUsersTable()throws SQLException, NoDatabaseConnectionException ;
	protected abstract Result createDefinitionsTable()throws SQLException, NoDatabaseConnectionException ;
	protected abstract Result createScriptEnvironmentTable()throws SQLException, NoDatabaseConnectionException ;
	protected abstract Result createSpecimenArchiveTable()throws SQLException, NoDatabaseConnectionException ;
	protected abstract Result createEventLogTable()throws SQLException, NoDatabaseConnectionException ;
	
	protected abstract Result createHttpDiscoveryRuleTable() throws SQLException, NoDatabaseConnectionException;
	protected abstract Result createRuleURLTable() throws SQLException, NoDatabaseConnectionException;
	protected abstract Result createSignatureScanResultTable() throws SQLException, NoDatabaseConnectionException;
	protected abstract Result createMatchedRulesTable() throws SQLException, NoDatabaseConnectionException;
	protected abstract Result createDefinitionPolicyTable() throws SQLException, NoDatabaseConnectionException;
	protected abstract Result createHttpDiscoveryResultTable() throws SQLException, NoDatabaseConnectionException;
	
	protected abstract Result createServiceScanRuleTable() throws SQLException, NoDatabaseConnectionException;
	protected abstract Result createServiceScanResultTable() throws SQLException, NoDatabaseConnectionException;
	protected abstract Result createActionsTable() throws SQLException, NoDatabaseConnectionException;
	protected abstract Result createEventLogHooksTable() throws SQLException, NoDatabaseConnectionException;
	protected abstract Result createDefinitionErrorTable() throws SQLException, NoDatabaseConnectionException;
	
	
	protected abstract void postTableCreation() throws SQLException, NoDatabaseConnectionException;

}
