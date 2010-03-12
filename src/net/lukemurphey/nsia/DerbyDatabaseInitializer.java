package net.lukemurphey.nsia;

import java.sql.*;
import java.util.Vector;

public class DerbyDatabaseInitializer extends DatabaseInitializer {

	public class DatabaseIndex{
		protected String indexName;
		protected String spec;
		
		public DatabaseIndex( String indexName, String spec){
			
			// 0 -- Precondition check
			
			
			// 1 -- Initialize the class
			this.indexName = indexName;
			this.spec = spec;
		}
		
		public String getIndexName(){
			return indexName;
		}
		
		public void drop() throws SQLException{
			
			PreparedStatement statement = null;
			
			try{
				if( connection != null ){
					statement = connection.prepareStatement("drop index " + indexName);
					statement.execute();
				}
			}
			finally{
				if( statement != null ){
					statement.close();
				}
			}
			
		}
		
		public boolean create(boolean dropIfExists) throws SQLException{
			
			PreparedStatement statement = null;
			
			try{
				if( connection != null ){
					statement = connection.prepareStatement("Create index " + indexName + " on " + spec);
					return statement.execute();
				}
			}
			finally{
				if( statement != null ){
					statement.close();
				}
			}
			
			
			return false;
		}
		
	}
	
	private Vector<DatabaseIndex> indexes = new Vector<DatabaseIndex>();
	
	public DerbyDatabaseInitializer( Connection connection ){
		super(connection);
	}

	public Vector<DatabaseIndex> getDatabaseIndexManagers(){
		
		if( indexes.size() == 0 ){
			indexes.add( new DatabaseIndex("EventLogSeverityIndex", "EventLog(Severity)") );
			indexes.add( new DatabaseIndex("EventLogDateIndex", "EventLog(LogDate)") );
			indexes.add( new DatabaseIndex("EventLogDateSeverityIndex", "EventLog(LogDate, Severity)") );
			indexes.add( new DatabaseIndex("EventLogDateIndexDesc", "EventLog(LogDate Desc)") );
			indexes.add( new DatabaseIndex("EventLogDateSeverityIndexDesc", "EventLog(LogDate, Severity Desc)") );
			indexes.add( new DatabaseIndex("EventLogIDIndexDesc", "EventLog(EventLogID Desc)") );
			indexes.add( new DatabaseIndex("DefinitionPolicySiteGroupIndex", "DefinitionPolicy(SiteGroupID)") );
			indexes.add( new DatabaseIndex("DefinitionScanResultIDIndex", "SignatureScanResult(ScanResultID)") );
			indexes.add( new DatabaseIndex("DefinitionScanResultContentTypeIndex", "SignatureScanResult(ContentType)") );
			indexes.add( new DatabaseIndex("ScanResultRuleIDIndex", "ScanResult(ScanRuleID)") );
			indexes.add( new DatabaseIndex("ScanResultParentIDIndex", "ScanResult(ParentScanResultID)") );
			indexes.add( new DatabaseIndex("ScriptEnvironmentScanResultIDIndexDesc", "ScriptEnvironment(ScanResultID Desc)") );
		}
		
		return indexes;
		
	}
	
	@Override
	protected Result createApplicationParametersTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "ApplicationParameters" ) ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			statement = connection.prepareStatement("CREATE TABLE ApplicationParameters (" +
					"ParameterID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"ObjectID INTEGER NOT NULL default 0," +
					"Name VARCHAR(4096) NOT NULL," +
					"Value VARCHAR(4096)," +
			"PRIMARY KEY  (ParameterID))");

			statement.execute();

		}
		finally{

			if( statement != null){
				statement.close();
			}
		}


		return DatabaseInitializer.Result.TABLE_CREATED;

	}

	@Override
	protected Result createAttemptedLoginsTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "AttemptedLogins" ) ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			statement = connection.prepareStatement("CREATE TABLE AttemptedLogins (" +
					"AttemptedNameID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"LoginName VARCHAR(4096) default NULL," +
					"FirstAttempted TIMESTAMP default NULL," +
					"Attempts INTEGER default NULL," +
					"TimeBlocked VARCHAR(4096) default NULL," +
			"PRIMARY KEY  (AttemptedNameID))");

			statement.execute();

		}
		finally{

			if( statement != null){
				statement.close();
			}
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}

	@Override
	protected Result createFirewallTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "Firewall" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			statement = connection.prepareStatement("CREATE TABLE Firewall (" +
					"RuleID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"IpStart VARCHAR(4096) default NULL," +
					"IpEnd VARCHAR(4096) default NULL," +
					"Action INTEGER default NULL," +
					"RuleState INTEGER default NULL," +
					"ExpirationDate TIMESTAMP default NULL," +
					"DomainName VARCHAR(4096) default NULL," +
			"PRIMARY KEY  (RuleID))");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}

	@Override
	protected Result createGroupsTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "Groups" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			statement = connection.prepareStatement("CREATE TABLE Groups (" +
					"GroupID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"GroupName VARCHAR(4096)," +
					"GroupDescription VARCHAR(4096)," +
					"Status INTEGER default NULL," +
					"PRIMARY KEY  (GroupID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}

	@Override
	protected Result createGroupsUsersMapTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "GroupUsersMap" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			statement = connection.prepareStatement("CREATE TABLE GroupUsersMap (" +
					"GroupUserMapID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"GroupID INTEGER default NULL," +
					"UserID INTEGER default NULL," +
					"PRIMARY KEY  (GroupUserMapID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}

	@Override
	protected Result createObjectMapTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "ObjectMap" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			statement = connection.prepareStatement("CREATE TABLE ObjectMap (" +
					"ObjectID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"\"Table\" VARCHAR(4096) default NULL," +
					"PRIMARY KEY  (ObjectID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}

	@Override
	protected Result createPerformanceMetricsTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "PerformanceMetrics" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			statement = connection.prepareStatement("CREATE TABLE PerformanceMetrics (" +
					"EntryID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"Timestamp TIMESTAMP default NULL," +
					"MemoryUsed INTEGER default NULL," +
					"MemoryTotal INTEGER default NULL," +
					"Threads INTEGER default NULL," +
					"ResponseTime INTEGER default NULL," +
					"PRIMARY KEY  (EntryID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}

	@Override
	protected Result createPermissionsTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "Permissions" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			statement = connection.prepareStatement("CREATE TABLE Permissions (" +
					"PermissionID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"ObjectID INTEGER default NULL," +
					"ParentObjectID INTEGER default NULL," +
					"UserID INTEGER default NULL," +
					"GroupID INTEGER default NULL," +
					"Modify INTEGER default NULL," +
					"\"Create\" INTEGER default NULL," +
					"\"Delete\" INTEGER default NULL," +
					"\"Read\" INTEGER default NULL," +
					"\"Execute\" INTEGER default NULL," +
					"Control INTEGER default NULL," +
					"PRIMARY KEY  (PermissionID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}

	@Override
	protected Result createRightsTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "Rights" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			statement = connection.prepareStatement("CREATE TABLE Rights (" +
					"RightID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"ObjectID INTEGER default NULL," +
					"RightName VARCHAR(4096)," +
					"RightDescription VARCHAR(4096)," +
					"RelevantPermissions VARCHAR(4096)," +
					"PRIMARY KEY  (RightID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}

	@Override
	protected Result createScanResultTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "ScanResult" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			statement = connection.prepareStatement("CREATE TABLE ScanResult (" +
					"ScanResultID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"ScanRuleID INTEGER default NULL," +
					"ParentScanResultID INTEGER default NULL," +
					"Deviations INTEGER default NULL," +
					"Incompletes INTEGER default NULL," +
					"Accepts INTEGER default NULL," +
					"ScanDate TIMESTAMP default NULL," +
					"RuleType VARCHAR(4096)," +
					"ScanResultCode INTEGER default NULL," +
					"PRIMARY KEY  (ScanResultID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}

	@Override
	protected Result createScanRuleTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		// 1 -- Determine if the table already exists
		if( doesTableExist( "ScanRule" ) == true ){
			return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
		}

		// 2 -- Create the table
		try{
			connection = getConnection();
			statement = connection.prepareStatement("CREATE TABLE ScanRule (" +
					"ScanRuleID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"ObjectID INTEGER default NULL," +
					"SiteGroupID INTEGER default NULL," +
					"ScanFrequency INTEGER default NULL," +
					"RuleType VARCHAR(4096)," +
					"ScanDataObsolete INTEGER default NULL," +
					"State INTEGER default NULL," +
					"NotifyThreshold INTEGER default NULL," +
					"NotifyTimePeriod INTEGER default NULL," +
					"RuleVersion INTEGER default NULL," +
					"PRIMARY KEY  (ScanRuleID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}

		return DatabaseInitializer.Result.TABLE_CREATED;
	}

	@Override
	protected Result createSessionsTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "Sessions" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			statement = connection.prepareStatement("CREATE TABLE Sessions (" +
					"SessionEntryID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"UserID INTEGER default NULL," +
					"TrackingNumber INTEGER default NULL," +
					"SessionID VARCHAR(4096)," +
					"SessionCreated TIMESTAMP default NULL," +
					"LastActivity TIMESTAMP default NULL," +
					"RemoteUserAddress VARCHAR(4096)," +
					"RemoteUserData VARCHAR(4096)," +
					"ConnectionAddress VARCHAR(4096)," +
					"ConnectionData VARCHAR(4096)," +
					"Status INTEGER default NULL," +
					"SessionIDCreated TIMESTAMP default NULL," +
					"PRIMARY KEY  (SessionEntryID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}

	@Override
	protected Result createSiteGroupsTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "SiteGroups" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			statement = connection.prepareStatement("CREATE TABLE SiteGroups (" +
					"SiteGroupID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"Name VARCHAR(4096)," +
					"Description VARCHAR(4096)," +
					"Status INTEGER default NULL," +
					"ObjectID INTEGER default NULL," +
					"PRIMARY KEY  (SiteGroupID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}

	@Override
	protected Result createStaticHttpHeaderResultsTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "HttpHeaderScanResult" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			statement = connection.prepareStatement("CREATE TABLE HttpHeaderScanResult (" +
					"ScanResultID INTEGER default NULL," +
					"HttpHeaderScanResultID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"HttpHeaderScanRuleID INTEGER default NULL," +
					"MatchAction INTEGER default NULL," +
					"ExpectedHeaderName VARCHAR(4096)," +
					"HeaderNameType INTEGER default NULL," +
					"ActualHeaderName VARCHAR(4096)," +
					"ExpectedHeaderValue VARCHAR(4096)," +
					"HeaderValueType INTEGER default NULL," +
					"ActualHeaderValue VARCHAR(4096)," +
					"RuleResult INTEGER default NULL," +
					"PRIMARY KEY  (HttpHeaderScanResultID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}

	@Override
	protected Result createStaticHttpHeaderRulesTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "HttpHeaderScanRule" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			statement = connection.prepareStatement("CREATE TABLE HttpHeaderScanRule (" +
					"HttpHeaderScanRuleID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"ScanRuleID INTEGER default NULL," +
					"MatchAction INTEGER default NULL," +
					"HeaderName VARCHAR(4096)," +
					"HeaderNameType INTEGER default NULL," +
					"HeaderValue VARCHAR(4096)," +
					"HeaderValueType INTEGER default NULL," +
					"PRIMARY KEY  (HttpHeaderScanRuleID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}

	@Override
	protected Result createStaticHttpResultsTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "HttpHashScanResult" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			statement = connection.prepareStatement("CREATE TABLE HttpHashScanResult (" +
					"HttpHashScanResultID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"ScanResultID INTEGER default NULL," +
					"ActualHashAlgorithm VARCHAR(4096)," +
					"ActualHashData VARCHAR(4096)," +
					"ActualResponseCode INTEGER default NULL," +
					"ExpectedHashAlgorithm VARCHAR(4096)," +
					"ExpectedHashData VARCHAR(4096)," +
					"ExpectedResponseCode INTEGER default NULL," +
					"LocationUrl VARCHAR(4096)," +
					"PRIMARY KEY  (HttpHashScanResultID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}

	@Override
	protected Result createStaticHttpRuleTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "HttpHashScanRule" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			statement = connection.prepareStatement("CREATE TABLE HttpHashScanRule (" +
					"HttpScanRuleID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"ScanRuleID INTEGER default NULL," +
					"LocationUrl VARCHAR(4096)," +
					"HashAlgorithm VARCHAR(4096)," +
					"HashData VARCHAR(4096)," +
					"ResponseCode INTEGER default NULL," +
					"ActualData VARCHAR(4096)," +
					"DefaultDenyHeaders SMALLINT default NULL," +
					"PRIMARY KEY  (HttpScanRuleID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}

	@Override
	protected Result createUsersTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "Users" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			statement = connection.prepareStatement("CREATE TABLE Users (" +
					"UserID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"LoginName VARCHAR(4096) NOT NULL," +
					"PasswordHash VARCHAR(4096)," +
					"PasswordHashAlgorithm VARCHAR(4096) default NULL," +
					"RealName VARCHAR(4096) default NULL," +
					"PasswordStrength INTEGER default NULL," +
					"AccountStatus INTEGER default NULL," +
					"AccountCreated TIMESTAMP default NULL," +
					"PasswordLastSet TIMESTAMP default NULL," +
					"PriorLoginLocation VARCHAR(4096) default NULL," +
					"PasswordHashIterationCount INTEGER default NULL," +
					"Salt VARCHAR(4096) default NULL," +
					"EmailAddress VARCHAR(4096) default NULL," +
					"Unrestricted SMALLINT default NULL," +
					"PRIMARY KEY  (UserID)" +
			")");

			statement.execute();

		}
		finally{
			if( statement != null)
				statement.close();
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}

	@Override
	protected Result createPortScanRuleTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "PortScanRule" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			statement = connection.prepareStatement("CREATE TABLE PortScanRule (" +
					"PortScanRuleID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"ScanRuleID INTEGER default NULL," +
					"Server VARCHAR(4096)," +
					"PortsToScan VARCHAR(16384)," +
					"PortsOpen VARCHAR(16384)," +
					"PortsClosed VARCHAR(16384)," +
					"PortsNotResponding VARCHAR(16384)," +
					"PRIMARY KEY (PortScanRuleID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}

	@Override
	protected Result createPortScanResultTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "PortScanResult" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			statement = connection.prepareStatement("CREATE TABLE PortScanResult (" +
					"PortScanResultID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"ScanResultID INTEGER default NULL," +
					"PortsOpen VARCHAR(16384)," +
					"PortsClosed VARCHAR(16384)," +
					"PortsNotResponding VARCHAR(16384)," +
					"Server VARCHAR(4096)," +
					"PRIMARY KEY  (HttpHashScanResultID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}

	@Override
	protected Result createDefinitionsTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "Definitions" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			statement = connection.prepareStatement("CREATE TABLE Definitions (" +
					"DefinitionID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"Category VARCHAR(64) NOT NULL," +
					"SubCategory VARCHAR(64) NOT NULL," +
					"Name VARCHAR(64) NOT NULL," +
					"DefaultMessage VARCHAR(255)," +
					"Code LONG VARCHAR NOT NULL," +
					"AssignedID INTEGER NOT NULL default -1," +
					"Version INTEGER NOT NULL," +
					"Type INTEGER NOT NULL," +
					"PRIMARY KEY  (DefinitionID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}

	@Override
	protected Result createScriptEnvironmentTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "ScriptEnvironment" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			statement = connection.prepareStatement("CREATE TABLE ScriptEnvironment (" +
					"EnvironmentEntryID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"ScriptDefinitionID INTEGER," +
					"DateScanned TIMESTAMP," +
					"DefinitionName VARCHAR(255)," +
					"RuleID INTEGER NOT NULL," +
					"UniqueResourceName VARCHAR(4096)," +
					"IsCurrent SMALLINT DEFAULT 1," +
					"ScanResultID INTEGER," +
					"Name VARCHAR(255) NOT NULL," +
					"Value BLOB," +
					"PRIMARY KEY (EnvironmentEntryID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}

	@Override
	protected Result createSpecimenArchiveTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "SpecimenArchive" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			statement = connection.prepareStatement("CREATE TABLE SpecimenArchive (" +
					"SpecimenID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"ScanResultID INTEGER NOT NULL," +
					"Encoding VARCHAR(255)," +
					"DateObserved DATETIME NOT NULL," +
					"Data BLOB(64 M)," +
					"MimeType VARCHAR(255)," +
					"URL VARCHAR(4096)," +
					"ActualLength LONG VARCHAR," +
					"PRIMARY KEY  (SpecimenID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}

	@Override
	protected Result createEventLogTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "EventLog" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();

			statement = connection.prepareStatement("CREATE TABLE EventLog (" +
					"EventLogID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"LogDate TIMESTAMP NOT NULL," +
					"Severity INTEGER NOT NULL," +
					"Title LONG VARCHAR," +
					"Notes LONG VARCHAR," +
					"PRIMARY KEY(EventLogID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}


	@Override
	protected Result createHttpDiscoveryRuleTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "HttpDiscoveryRule" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();

			statement = connection.prepareStatement("CREATE TABLE HttpDiscoveryRule (" +
					"HttpDiscoveryRuleID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"ScanRuleID INTEGER," +
					"RecursionDepth INTEGER NOT NULL," +
					"ResourceScanLimit INTEGER NOT NULL," +
					"Domain VARCHAR(255)," +
					"ScanFirstExternal SMALLINT default NULL," +
					"PRIMARY KEY(HttpDiscoveryRuleID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}
	

	@Override
	protected Result createHttpDiscoveryResultTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;
		
		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "HttpDiscoveryResult" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();

			statement = connection.prepareStatement("CREATE TABLE HttpDiscoveryResult (" +
					"HttpDiscoveryResultID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"RecursionDepth INTEGER," +
					"ScanResultID INTEGER NOT NULL," +
					"ResourceScanLimit INTEGER," +
					"Domain VARCHAR(255)," +
					"ScanFirstExternal SMALLINT default NULL," +
					"PRIMARY KEY(HttpDiscoveryResultID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}


	@Override
	protected Result createRuleURLTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "RuleURL" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();

			statement = connection.prepareStatement("CREATE TABLE RuleURL (" +
					"RuleURLID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"ScanRuleID INTEGER NOT NULL," +
					"URL VARCHAR(4096)," +
					"PRIMARY KEY(RuleURLID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}


	@Override
	protected Result createSignatureScanResultTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "SignatureScanResult" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();

			statement = connection.prepareStatement("CREATE TABLE SignatureScanResult (" +
					"SignatureScanResultID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"ScanResultID INTEGER NOT NULL," +
					"URL VARCHAR(32000)," +
					"ContentType VARCHAR(4096)," +
					"PRIMARY KEY(SignatureScanResultID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}

	@Override
	protected Result createMatchedRulesTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "MatchedRule" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			
			statement = connection.prepareStatement("CREATE TABLE MatchedRule (" +
					"MatchedRuleID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"ScanResultID INTEGER NOT NULL," +
					"RuleName VARCHAR(255)," +
					"RuleMessage VARCHAR(32000)," +
					"RuleID INTEGER," +
					"MatchStart INTEGER," +
					"MatchLength INTEGER," +
					"Severity INTEGER," +
					"PRIMARY KEY(MatchedRuleID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}


		return DatabaseInitializer.Result.TABLE_CREATED;
	}
	
	@Override
	protected Result createServiceScanRuleTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "ServiceScanRule" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			
			statement = connection.prepareStatement("CREATE TABLE ServiceScanRule (" +
					"ServiceScanRuleID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"ScanRuleID INTEGER NOT NULL," +
					"PortsOpen VARCHAR(4096)," +
					"PortsToScan VARCHAR(4096)," +
					"Server VARCHAR(255)," +
					"PRIMARY KEY(ServiceScanRuleID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}

		return DatabaseInitializer.Result.TABLE_CREATED;
	}
	
	@Override
	protected Result createServiceScanResultTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "ServiceScanResult" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			
			statement = connection.prepareStatement("CREATE TABLE ServiceScanResult (" +
					"ServiceScanResultID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"ScanResultID INTEGER NOT NULL," +
					"PortsScanned VARCHAR(4096)," +
					"PortsExpectedOpen VARCHAR(4096)," +
					"PortsUnexpectedClosed VARCHAR(4096)," +
					"PortsUnexpectedOpen VARCHAR(4096)," +
					"PortsUnexpectedNotResponding VARCHAR(4096)," +
					"Server VARCHAR(255)," +
					"PRIMARY KEY(ServiceScanResultID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}

		return DatabaseInitializer.Result.TABLE_CREATED;
	}
	
	@Override
	protected Result createDefinitionPolicyTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "DefinitionPolicy" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			
			statement = connection.prepareStatement("CREATE TABLE DefinitionPolicy (" +
					"DefinitionPolicyID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"SiteGroupID INTEGER NOT NULL," +
					"DefinitionID INTEGER," +
					"RuleID INTEGER," +
					"DefinitionName VARCHAR(255)," +
					"DefinitionCategory VARCHAR(255)," +
					"DefinitionSubCategory VARCHAR(255)," +
					"Action INTEGER," +
					"URL VARCHAR(4096)," +
					"PRIMARY KEY(DefinitionPolicyID)" +
			")");

			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}

		return DatabaseInitializer.Result.TABLE_CREATED;
	}

	@Override
	protected Result createActionsTable() throws SQLException,
			NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "Action" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			
			statement = connection.prepareStatement("CREATE TABLE Action (" +
					"ActionID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"State BLOB," +
					"PRIMARY KEY(ActionID)" +
			")");
			
			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}

		return DatabaseInitializer.Result.TABLE_CREATED;
	}
	
	@Override
	protected Result createEventLogHooksTable() throws SQLException,
			NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "EventLogHook" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			
			statement = connection.prepareStatement("CREATE TABLE EventLogHook (" +
					"EventLogHookID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"ActionID INTEGER NOT NULL," +
					"TypeID INTEGER," +
					"SiteGroupID INTEGER," +
					"RuleID INTEGER," +
					"MinimumSeverity INTEGER," +
					"Enabled INTEGER default 1," +
					"State BLOB," +
					"PRIMARY KEY(ActionID)" +
			")");
			
			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}

		return DatabaseInitializer.Result.TABLE_CREATED;
	}
	
	protected void postTableCreation(){
		
	}

	@Override
	protected Result createDefinitionErrorTable() throws SQLException, NoDatabaseConnectionException {

		PreparedStatement statement = null;

		try{
			// 1 -- Determine if the table already exists
			if( doesTableExist( "DefinitionErrorLog" ) == true ){
				return DatabaseInitializer.Result.NO_ACTION_NECESSARY;
			}

			// 2 -- Create the table
			connection = getConnection();
			
			statement = connection.prepareStatement("CREATE TABLE DefinitionErrorLog (" +
					"DefinitionErrorLogID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
					"DateLastOccurred TIMESTAMP NOT NULL," +
					"DateFirstOccurred TIMESTAMP NOT NULL," +
					"Notes VARCHAR(4096)," +
					"DefinitionName VARCHAR(4096) NOT NULL," +
					"DefinitionVersion INTEGER NOT NULL," +
					"DefinitionID INTEGER NOT NULL," +
					"LocalDefinitionID INTEGER," +
					"Relevant INTEGER default 1," +
					"ErrorName VARCHAR(4096) NOT NULL," +
					"PRIMARY KEY(DefinitionErrorLogID)" +
			")");
			
			statement.execute();

		}
		finally{

			if( statement != null)
				statement.close();
		}

		return DatabaseInitializer.Result.TABLE_CREATED;
	}
}
