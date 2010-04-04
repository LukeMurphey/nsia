-- SQL Script for Derby

-- 1 -- Create tables
CREATE TABLE ApplicationParameters (
					ParameterID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					ObjectID INTEGER NOT NULL default 0,
					Name VARCHAR(4096) NOT NULL,
					Value VARCHAR(4096),
			PRIMARY KEY  (ParameterID))
            

CREATE TABLE AttemptedLogins (
					AttemptedNameID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					LoginName VARCHAR(4096) default NULL,
					FirstAttempted TIMESTAMP default NULL,
					Attempts INTEGER default NULL,
					TimeBlocked VARCHAR(4096) default NULL,
			PRIMARY KEY  (AttemptedNameID))
            
CREATE TABLE Firewall (
					RuleID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					IpStart VARCHAR(4096) default NULL,
					IpEnd VARCHAR(4096) default NULL,
					Action INTEGER default NULL,
					RuleState INTEGER default NULL,
					ExpirationDate TIMESTAMP default NULL,
					DomainName VARCHAR(4096) default NULL,
			PRIMARY KEY  (RuleID))
            
            
CREATE TABLE Groups (
					GroupID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					GroupName VARCHAR(4096),
					GroupDescription VARCHAR(4096),
					Status INTEGER default NULL,
					PRIMARY KEY  (GroupID)
			)
            
            
CREATE TABLE GroupUsersMap (
					GroupUserMapID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					GroupID INTEGER default NULL,
					UserID INTEGER default NULL,
					PRIMARY KEY  (GroupUserMapID)
			)
            
            
CREATE TABLE ObjectMap (
					ObjectID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					"Table" VARCHAR(4096) default NULL,
					PRIMARY KEY  (ObjectID)
			)
            
            
CREATE TABLE PerformanceMetrics (
					EntryID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					Timestamp TIMESTAMP default NULL,
					MemoryUsed INTEGER default NULL,
					MemoryTotal INTEGER default NULL,
					Threads INTEGER default NULL,
					ResponseTime INTEGER default NULL,
					PRIMARY KEY  (EntryID)
			)
            
            
CREATE TABLE Permissions (
					PermissionID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					ObjectID INTEGER default NULL,
					ParentObjectID INTEGER default NULL,
					UserID INTEGER default NULL,
					GroupID INTEGER default NULL,
					Modify INTEGER default NULL,
					"Create" INTEGER default NULL,
					"Delete" INTEGER default NULL,
					"Read" INTEGER default NULL,
					"Execute" INTEGER default NULL,
					Control INTEGER default NULL,
					PRIMARY KEY  (PermissionID)
			)
            
            
CREATE TABLE Rights (
					RightID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					ObjectID INTEGER default NULL,
					RightName VARCHAR(4096),
					RightDescription VARCHAR(4096),
					RelevantPermissions VARCHAR(4096),
					PRIMARY KEY  (RightID)
			)
            
CREATE TABLE ScanResult (
					ScanResultID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					ScanRuleID INTEGER default NULL,
					ParentScanResultID INTEGER default NULL,
					Deviations INTEGER default NULL,
					Incompletes INTEGER default NULL,
					Accepts INTEGER default NULL,
					ScanDate TIMESTAMP default NULL,
					RuleType VARCHAR(4096),
					ScanResultCode INTEGER default NULL,
					PRIMARY KEY  (ScanResultID)
			)
            
CREATE TABLE ScanRule (
					ScanRuleID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					ObjectID INTEGER default NULL,
					SiteGroupID INTEGER default NULL,
					ScanFrequency INTEGER default NULL,
					RuleType VARCHAR(4096),
					ScanDataObsolete INTEGER default NULL,
					State INTEGER default NULL,
					NotifyThreshold INTEGER default NULL,
					NotifyTimePeriod INTEGER default NULL,
					RuleVersion INTEGER default NULL,
					PRIMARY KEY  (ScanRuleID)
			)
            
            
CREATE TABLE Sessions (
					SessionEntryID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					UserID INTEGER default NULL,
					TrackingNumber INTEGER default NULL,
					SessionID VARCHAR(4096),
					SessionCreated TIMESTAMP default NULL,
					LastActivity TIMESTAMP default NULL,
					RemoteUserAddress VARCHAR(4096),
					RemoteUserData VARCHAR(4096),
					ConnectionAddress VARCHAR(4096),
					ConnectionData VARCHAR(4096),
					Status INTEGER default NULL,
					SessionIDCreated TIMESTAMP default NULL,
					PRIMARY KEY  (SessionEntryID)
			)
            
            
CREATE TABLE SiteGroups (
					SiteGroupID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					Name VARCHAR(4096),
					Description VARCHAR(4096),
					Status INTEGER default NULL,
					ObjectID INTEGER default NULL,
					PRIMARY KEY  (SiteGroupID)
			)
            
CREATE TABLE HttpHeaderScanResult (
					ScanResultID INTEGER default NULL,
					HttpHeaderScanResultID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					HttpHeaderScanRuleID INTEGER default NULL,
					MatchAction INTEGER default NULL,
					ExpectedHeaderName VARCHAR(4096),
					HeaderNameType INTEGER default NULL,
					ActualHeaderName VARCHAR(4096),
					ExpectedHeaderValue VARCHAR(4096),
					HeaderValueType INTEGER default NULL,
					ActualHeaderValue VARCHAR(4096),
					RuleResult INTEGER default NULL,
					PRIMARY KEY  (HttpHeaderScanResultID)
			)
            
CREATE TABLE HttpHeaderScanRule (
					HttpHeaderScanRuleID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					ScanRuleID INTEGER default NULL,
					MatchAction INTEGER default NULL,
					HeaderName VARCHAR(4096),
					HeaderNameType INTEGER default NULL,
					HeaderValue VARCHAR(4096),
					HeaderValueType INTEGER default NULL,
					PRIMARY KEY  (HttpHeaderScanRuleID)
			)
            
CREATE TABLE HttpHashScanResult (
					HttpHashScanResultID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					ScanResultID INTEGER default NULL,
					ActualHashAlgorithm VARCHAR(4096),
					ActualHashData VARCHAR(4096),
					ActualResponseCode INTEGER default NULL,
					ExpectedHashAlgorithm VARCHAR(4096),
					ExpectedHashData VARCHAR(4096),
					ExpectedResponseCode INTEGER default NULL,
					LocationUrl VARCHAR(4096),
					PRIMARY KEY  (HttpHashScanResultID)
			)
            
CREATE TABLE HttpHashScanRule (
					HttpScanRuleID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					ScanRuleID INTEGER default NULL,
					LocationUrl VARCHAR(4096),
					HashAlgorithm VARCHAR(4096),
					HashData VARCHAR(4096),
					ResponseCode INTEGER default NULL,
					ActualData VARCHAR(4096),
					DefaultDenyHeaders SMALLINT default NULL,
					PRIMARY KEY  (HttpScanRuleID)
			)
            
CREATE TABLE Users (
					UserID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					LoginName VARCHAR(4096) NOT NULL,
					PasswordHash VARCHAR(4096),
					PasswordHashAlgorithm VARCHAR(4096) default NULL,
					RealName VARCHAR(4096) default NULL,
					PasswordStrength INTEGER default NULL,
					AccountStatus INTEGER default NULL,
					AccountCreated TIMESTAMP default NULL,
					PasswordLastSet TIMESTAMP default NULL,
					PriorLoginLocation VARCHAR(4096) default NULL,
					PasswordHashIterationCount INTEGER default NULL,
					Salt VARCHAR(4096) default NULL,
					EmailAddress VARCHAR(4096) default NULL,
					Unrestricted SMALLINT default NULL,
					PRIMARY KEY  (UserID)
			)
            
CREATE TABLE PortScanRule (
					PortScanRuleID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					ScanRuleID INTEGER default NULL,
					Server VARCHAR(4096),
					PortsToScan VARCHAR(16384),
					PortsOpen VARCHAR(16384),
					PortsClosed VARCHAR(16384),
					PortsNotResponding VARCHAR(16384),
					PRIMARY KEY (PortScanRuleID)
			)
            
            
CREATE TABLE PortScanResult (
					PortScanResultID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					ScanResultID INTEGER default NULL,
					PortsOpen VARCHAR(16384),
					PortsClosed VARCHAR(16384),
					PortsNotResponding VARCHAR(16384),
					Server VARCHAR(4096),
					PRIMARY KEY  (PortScanResultID)
			)
            
CREATE TABLE Definitions (
					DefinitionID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					Category VARCHAR(64) NOT NULL,
					SubCategory VARCHAR(64) NOT NULL,
					Name VARCHAR(64) NOT NULL,
					DefaultMessage VARCHAR(255),
					Code LONG VARCHAR NOT NULL,
					AssignedID INTEGER NOT NULL default -1,
					Version INTEGER NOT NULL,
					Type INTEGER NOT NULL,
					PRIMARY KEY  (DefinitionID)
			)
            
            
CREATE TABLE ScriptEnvironment (
					EnvironmentEntryID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					ScriptDefinitionID INTEGER,
					DateScanned TIMESTAMP,
					DefinitionName VARCHAR(255),
					RuleID INTEGER NOT NULL,
					UniqueResourceName VARCHAR(4096),
					IsCurrent SMALLINT DEFAULT 1,
					ScanResultID INTEGER,
					Name VARCHAR(255) NOT NULL,
					Value BLOB,
					PRIMARY KEY (EnvironmentEntryID)
			)
            
CREATE TABLE SpecimenArchive (
					SpecimenID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					ScanResultID INTEGER NOT NULL,
					Encoding VARCHAR(255),
					DateObserved DATETIME NOT NULL,
					Data BLOB(64 M),
					MimeType VARCHAR(255),
					URL VARCHAR(4096),
					ActualLength LONG VARCHAR,
					PRIMARY KEY  (SpecimenID)
			)
            
CREATE TABLE EventLog (
					EventLogID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					LogDate TIMESTAMP NOT NULL,
					Severity INTEGER NOT NULL,
					Title LONG VARCHAR,
					Notes LONG VARCHAR,
					PRIMARY KEY(EventLogID)
			)


CREATE TABLE HttpDiscoveryRule (
					HttpDiscoveryRuleID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					ScanRuleID INTEGER,
					RecursionDepth INTEGER NOT NULL,
					ResourceScanLimit INTEGER NOT NULL,
					Domain VARCHAR(255),
					ScanFirstExternal SMALLINT default NULL,
					PRIMARY KEY(HttpDiscoveryRuleID)
			)
            
CREATE TABLE HttpDiscoveryResult (
					HttpDiscoveryResultID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					RecursionDepth INTEGER,
					ScanResultID INTEGER NOT NULL,
					ResourceScanLimit INTEGER,
					Domain VARCHAR(255),
					ScanFirstExternal SMALLINT default NULL,
					PRIMARY KEY(HttpDiscoveryResultID)
			)
            
CREATE TABLE RuleURL (
					RuleURLID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					ScanRuleID INTEGER NOT NULL,
					URL VARCHAR(4096),
					PRIMARY KEY(RuleURLID)
			)
            
CREATE TABLE SignatureScanResult (
					SignatureScanResultID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					ScanResultID INTEGER NOT NULL,
					URL VARCHAR(32000),
					ContentType VARCHAR(4096),
					PRIMARY KEY(SignatureScanResultID)
			)
            
CREATE TABLE MatchedRule (
					MatchedRuleID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					ScanResultID INTEGER NOT NULL,
					RuleName VARCHAR(255),
					RuleMessage VARCHAR(32000),
					RuleID INTEGER,
					MatchStart INTEGER,
					MatchLength INTEGER,
					Severity INTEGER,
					PRIMARY KEY(MatchedRuleID)
			)
            
CREATE TABLE ServiceScanRule (
					ServiceScanRuleID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					ScanRuleID INTEGER NOT NULL,
					PortsOpen VARCHAR(4096),
					PortsToScan VARCHAR(4096),
					Server VARCHAR(255),
					PRIMARY KEY(ServiceScanRuleID)
			)
            
CREATE TABLE ServiceScanResult (
					ServiceScanResultID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					ScanResultID INTEGER NOT NULL,
					PortsScanned VARCHAR(4096),
					PortsExpectedOpen VARCHAR(4096),
					PortsUnexpectedClosed VARCHAR(4096),
					PortsUnexpectedOpen VARCHAR(4096),
					PortsUnexpectedNotResponding VARCHAR(4096),
					Server VARCHAR(255),
					PRIMARY KEY(ServiceScanResultID)
			)
            
CREATE TABLE DefinitionPolicy (
					DefinitionPolicyID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					SiteGroupID INTEGER NOT NULL,
					DefinitionID INTEGER,
					RuleID INTEGER,
					DefinitionName VARCHAR(255),
					DefinitionCategory VARCHAR(255),
					DefinitionSubCategory VARCHAR(255),
					Action INTEGER,
					URL VARCHAR(4096),
					PRIMARY KEY(DefinitionPolicyID)
			)
            
CREATE TABLE Action (
					ActionID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					State BLOB,
					PRIMARY KEY(ActionID)
			)
            
CREATE TABLE EventLogHook (
					EventLogHookID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					ActionID INTEGER NOT NULL,
					TypeID INTEGER,
					SiteGroupID INTEGER,
					RuleID INTEGER,
					MinimumSeverity INTEGER,
					Enabled INTEGER default 1,
					State BLOB,
					PRIMARY KEY(EventLogHookID)
			)

CREATE TABLE DefinitionErrorLog (
					DefinitionErrorLogID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
					DateLastOccurred TIMESTAMP NOT NULL,
					DateFirstOccurred TIMESTAMP NOT NULL,
					Notes VARCHAR(4096),
					DefinitionName VARCHAR(4096) NOT NULL,
					DefinitionVersion INTEGER NOT NULL,
					DefinitionID INTEGER NOT NULL,
					LocalDefinitionID INTEGER,
					Relevant INTEGER default 1,
					ErrorName VARCHAR(4096) NOT NULL,
					PRIMARY KEY(DefinitionErrorLogID)
			)
            
-- 2 -- Create Indexes
Create index EventLogSeverityIndex on EventLog(Severity)
Create index EventLogDateIndex on EventLog(LogDate)
Create index EventLogDateSeverityIndex on EventLog(LogDate, Severity)

Create index EventLogDateIndexDesc on EventLog(LogDate Desc)
Create index EventLogDateSeverityIndexDesc on EventLog(LogDate, Severity Desc)
Create index EventLogIDIndexDesc on EventLog(EventLogID Desc)

Create index DefinitionPolicySiteGroupIndex on DefinitionPolicy(SiteGroupID)
Create index DefinitionScanResultIDIndex on SignatureScanResult(ScanResultID)
Create index DefinitionScanResultContentTypeIndex on SignatureScanResult(ContentType)

Create index ScanResultRuleIDIndex on ScanResult(ScanRuleID)
Create index ScanResultParentIDIndex on ScanResult(ParentScanResultID)
Create index ScriptEnvironmentScanResultIDIndexDesc on ScriptEnvironment(ScanResultID Desc)


-- 3 -- Populate Rights
insert into ObjectMap("Table") values(1)
insert into Rights(RightName, RightDescription, ObjectID) values ('Users.Add', 'Create New Users', 1)
insert into ObjectMap("Table") values(2)
insert into Rights(RightName, RightDescription, ObjectID) values ('Users.Edit', 'Edit User', 2)
insert into ObjectMap("Table") values(3)
insert into Rights(RightName, RightDescription, ObjectID) values ('Users.View', 'View Users'' Details (including rights)', 3)
insert into ObjectMap("Table") values(4)
insert into Rights(RightName, RightDescription, ObjectID) values ('Users.Delete', 'Delete Users', 4)
insert into ObjectMap("Table") values(5)
insert into Rights(RightName, RightDescription, ObjectID) values ('Users.Unlock', 'Unlock Accounts (due to repeated authentication attempts)', 5)
insert into ObjectMap("Table") values(6)
insert into Rights(RightName, RightDescription, ObjectID) values ('Users.UpdatePassword', 'Update Other''s Password (applies only to the other users'' accounts)', 6)
insert into ObjectMap("Table") values(7)
insert into Rights(RightName, RightDescription, ObjectID) values ('Users.UpdateOwnPassword', 'Update Account Details (applies only to the users'' own account)', 7)
insert into ObjectMap("Table") values(8)
insert into Rights(RightName, RightDescription, ObjectID) values ('Users.Sessions.Delete', 'Delete Users'' Sessions (kick users off)', 8)
insert into ObjectMap("Table") values(9)
insert into Rights(RightName, RightDescription, ObjectID) values ('Users.Sessions.View', 'View Users'' Sessions (see who is logged in)', 9)


insert into ObjectMap("Table") values(10)
insert into Rights(RightName, RightDescription, ObjectID) values ('Groups.Add', 'Create New Groups', 10)
insert into ObjectMap("Table") values(11)
insert into Rights(RightName, RightDescription, ObjectID) values ('Groups.View', 'View Groups', 11)
insert into ObjectMap("Table") values(12)
insert into Rights(RightName, RightDescription, ObjectID) values ('Groups.Edit', 'Edit Groups', 12)
insert into ObjectMap("Table") values(13)
insert into Rights(RightName, RightDescription, ObjectID) values ('Groups.Delete', 'Delete Groups', 13)
insert into ObjectMap("Table") values(14)
insert into Rights(RightName, RightDescription, ObjectID) values ('Groups.Membership.Edit', 'Manage Group Membership', 14)


insert into ObjectMap("Table") values(16)
insert into Rights(RightName, RightDescription, ObjectID) values ('SiteGroups.View', 'View Site Groups', 16)
insert into ObjectMap("Table") values(17)
insert into Rights(RightName, RightDescription, ObjectID) values ('SiteGroups.Add', 'Create New Site Group', 17)
insert into ObjectMap("Table") values(18)
insert into Rights(RightName, RightDescription, ObjectID) values ('SiteGroups.Delete', 'Delete Site Groups', 18)
insert into ObjectMap("Table") values(19)
insert into Rights(RightName, RightDescription, ObjectID) values ('SiteGroups.Edit', 'Edit Site Groups', 19)


insert into ObjectMap("Table") values(20)
insert into Rights(RightName, RightDescription, ObjectID) values ('System.Information.View', 'View System Information and Status', 20)
insert into ObjectMap("Table") values(21)
insert into Rights(RightName, RightDescription, ObjectID) values ('System.Configuration.Edit', 'Modify System Configuration', 21)
insert into ObjectMap("Table") values(22)
insert into Rights(RightName, RightDescription, ObjectID) values ('System.Configuration.View', 'View System Configuration', 22)
insert into ObjectMap("Table") values(23)
insert into Rights(RightName, RightDescription, ObjectID) values ('System.Firewall.View', 'View Firewall Configuration', 23)
insert into ObjectMap("Table") values(24)
insert into Rights(RightName, RightDescription, ObjectID) values ('System.Firewall.Edit', 'Change Firewall Configuration', 24)
insert into ObjectMap("Table") values(25)
insert into Rights(RightName, RightDescription, ObjectID) values ('System.ControlScanner', 'Start/Stop Scanner', 25)
insert into ObjectMap("Table") values(26)
insert into Rights(RightName, RightDescription, ObjectID) values ('SiteGroups.ScanAllRules', 'Allow Gratuitous Scanning of All Rules', 26)
insert into ObjectMap("Table") values(27)
insert into Rights(RightName, RightDescription, ObjectID) values ('System.Shutdown', 'Shutdown system', 27)

