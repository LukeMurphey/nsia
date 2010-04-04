-- SQL Script for MySQL

-- CREATE SCHEMA IF NOT EXISTS `nsia` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ;

-- 1 -- Create tables
CREATE TABLE ApplicationParameters (
					ParameterID INTEGER NOT NULL AUTO_INCREMENT,
					ObjectID INTEGER NOT NULL default 0,
					Name VARCHAR(4096) NOT NULL,
					Value VARCHAR(4096),
			PRIMARY KEY  (ParameterID));
            

CREATE TABLE AttemptedLogins (
					AttemptedNameID INTEGER NOT NULL AUTO_INCREMENT,
					LoginName VARCHAR(4096) NULL default NULL,
					FirstAttempted TIMESTAMP NULL default NULL,
					Attempts INTEGER NULL default NULL,
					TimeBlocked VARCHAR(4096) NULL default NULL,
			PRIMARY KEY  (AttemptedNameID));
            
CREATE TABLE Firewall (
					RuleID INTEGER NOT NULL AUTO_INCREMENT,
					IpStart VARCHAR(4096) NULL default NULL,
					IpEnd VARCHAR(4096) NULL default NULL,
					Action INTEGER NULL default NULL,
					RuleState INTEGER NULL default NULL,
					ExpirationDate TIMESTAMP NULL default NULL,
					DomainName VARCHAR(4096) NULL default NULL,
			PRIMARY KEY  (RuleID));
            
            
CREATE TABLE Groups (
					GroupID INTEGER NOT NULL AUTO_INCREMENT,
					GroupName VARCHAR(4096),
					GroupDescription VARCHAR(4096),
					Status INTEGER default NULL,
					PRIMARY KEY  (GroupID)
			);
            
            
CREATE TABLE GroupUsersMap (
					GroupUserMapID INTEGER NOT NULL AUTO_INCREMENT,
					GroupID INTEGER default NULL,
					UserID INTEGER default NULL,
					PRIMARY KEY  (GroupUserMapID)
			);
            
            
CREATE TABLE ObjectMap (
					ObjectID INTEGER NOT NULL AUTO_INCREMENT,
					`Table` VARCHAR(4096) default NULL,
					PRIMARY KEY  (ObjectID)
			);
            
            
CREATE TABLE PerformanceMetrics (
					EntryID INTEGER NOT NULL AUTO_INCREMENT,
					Timestamp TIMESTAMP NULL default NULL,
					MemoryUsed INTEGER NULL default NULL,
					MemoryTotal INTEGER NULL default NULL,
					Threads INTEGER NULL default NULL,
					ResponseTime INTEGER NULL default NULL,
					PRIMARY KEY  (EntryID)
			);
            
            
CREATE TABLE Permissions (
					PermissionID INTEGER NOT NULL AUTO_INCREMENT,
					ObjectID INTEGER NULL default NULL,
					ParentObjectID INTEGER NULL default NULL,
					UserID INTEGER NULL default NULL,
					GroupID INTEGER NULL default NULL,
					Modify INTEGER NULL default NULL,
					`Create` INTEGER NULL default NULL,
					`Delete` INTEGER NULL default NULL,
					`Read` INTEGER NULL default NULL,
					`Execute` INTEGER default NULL,
					Control INTEGER NULL default NULL,
					PRIMARY KEY  (PermissionID)
			);
            
            
CREATE TABLE Rights (
					RightID INTEGER NOT NULL AUTO_INCREMENT,
					ObjectID INTEGER default NULL,
					RightName VARCHAR(4096),
					RightDescription VARCHAR(4096),
					RelevantPermissions VARCHAR(4096),
					PRIMARY KEY  (RightID)
			);
            
CREATE TABLE ScanResult (
					ScanResultID INTEGER NOT NULL AUTO_INCREMENT,
					ScanRuleID INTEGER NULL default NULL,
					ParentScanResultID INTEGER NULL default NULL,
					Deviations INTEGER NULL default NULL,
					Incompletes INTEGER NULL default NULL,
					Accepts INTEGER NULL default NULL,
					ScanDate TIMESTAMP NULL default NULL,
					RuleType VARCHAR(4096),
					ScanResultCode INTEGER NULL default NULL,
					PRIMARY KEY  (ScanResultID)
			);
            
CREATE TABLE ScanRule (
					ScanRuleID INTEGER NOT NULL AUTO_INCREMENT,
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
			);
            
            
CREATE TABLE Sessions (
					SessionEntryID INTEGER NOT NULL AUTO_INCREMENT,
					UserID INTEGER NULL default NULL,
					TrackingNumber INTEGER NULL default NULL,
					SessionID VARCHAR(4096),
					SessionCreated TIMESTAMP NULL default NULL,
					LastActivity TIMESTAMP NULL default NULL,
					RemoteUserAddress VARCHAR(4096),
					RemoteUserData VARCHAR(4096),
					ConnectionAddress VARCHAR(4096),
					ConnectionData VARCHAR(4096),
					Status INTEGER NULL default NULL,
					SessionIDCreated TIMESTAMP NULL default NULL,
					PRIMARY KEY  (SessionEntryID)
			);
            
            
CREATE TABLE SiteGroups (
					SiteGroupID INTEGER NOT NULL AUTO_INCREMENT,
					Name VARCHAR(4096),
					Description VARCHAR(4096),
					Status INTEGER default NULL,
					ObjectID INTEGER default NULL,
					PRIMARY KEY  (SiteGroupID)
			);
            
CREATE TABLE HttpHeaderScanResult (
					ScanResultID INTEGER default NULL,
					HttpHeaderScanResultID INTEGER NOT NULL AUTO_INCREMENT,
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
			);
            
CREATE TABLE HttpHeaderScanRule (
					HttpHeaderScanRuleID INTEGER NOT NULL AUTO_INCREMENT,
					ScanRuleID INTEGER default NULL,
					MatchAction INTEGER default NULL,
					HeaderName VARCHAR(4096),
					HeaderNameType INTEGER default NULL,
					HeaderValue VARCHAR(4096),
					HeaderValueType INTEGER default NULL,
					PRIMARY KEY  (HttpHeaderScanRuleID)
			);
            
CREATE TABLE HttpHashScanResult (
					HttpHashScanResultID INTEGER NOT NULL AUTO_INCREMENT,
					ScanResultID INTEGER default NULL,
					ActualHashAlgorithm VARCHAR(4096),
					ActualHashData VARCHAR(4096),
					ActualResponseCode INTEGER default NULL,
					ExpectedHashAlgorithm VARCHAR(4096),
					ExpectedHashData VARCHAR(4096),
					ExpectedResponseCode INTEGER default NULL,
					LocationUrl VARCHAR(4096),
					PRIMARY KEY  (HttpHashScanResultID)
			);
            
CREATE TABLE HttpHashScanRule (
					HttpScanRuleID INTEGER NOT NULL AUTO_INCREMENT,
					ScanRuleID INTEGER default NULL,
					LocationUrl VARCHAR(4096),
					HashAlgorithm VARCHAR(4096),
					HashData VARCHAR(4096),
					ResponseCode INTEGER default NULL,
					ActualData VARCHAR(4096),
					DefaultDenyHeaders SMALLINT default NULL,
					PRIMARY KEY  (HttpScanRuleID)
			);
            
CREATE TABLE Users (
					UserID INTEGER NOT NULL AUTO_INCREMENT,
					LoginName TEXT NOT NULL,
					PasswordHash TEXT,
					PasswordHashAlgorithm TEXT default NULL,
					RealName TEXT default NULL,
					PasswordStrength INTEGER default NULL,
					AccountStatus INTEGER default NULL,
					AccountCreated TIMESTAMP NULL default NULL,
					PasswordLastSet TIMESTAMP NULL default NULL,
					PriorLoginLocation TEXT default NULL,
					PasswordHashIterationCount INTEGER default NULL,
					Salt TEXT default NULL,
					EmailAddress TEXT default NULL,
					Unrestricted SMALLINT default NULL,
					PRIMARY KEY  (UserID)
			);

            
CREATE TABLE PortScanRule (
					PortScanRuleID INTEGER NOT NULL AUTO_INCREMENT,
					ScanRuleID INTEGER default NULL,
					Server TEXT,
					PortsToScan TEXT,
					PortsOpen TEXT,
					PortsClosed TEXT,
					PortsNotResponding TEXT,
					PRIMARY KEY (PortScanRuleID)
			);
            
            
CREATE TABLE PortScanResult (
					PortScanResultID INTEGER NOT NULL AUTO_INCREMENT,
					ScanResultID INTEGER default NULL,
					PortsOpen TEXT,
					PortsClosed TEXT,
					PortsNotResponding TEXT,
					Server TEXT,
					PRIMARY KEY  (PortScanResultID)
			);
            
CREATE TABLE Definitions (
					DefinitionID INTEGER NOT NULL AUTO_INCREMENT,
					Category VARCHAR(64) NOT NULL,
					SubCategory VARCHAR(64) NOT NULL,
					Name VARCHAR(64) NOT NULL,
					DefaultMessage VARCHAR(255),
					Code TEXT NOT NULL,
					AssignedID INTEGER NOT NULL default -1,
					Version INTEGER NOT NULL,
					Type INTEGER NOT NULL,
					PRIMARY KEY  (DefinitionID)
			);
            
            
CREATE TABLE ScriptEnvironment (
					EnvironmentEntryID INTEGER NOT NULL AUTO_INCREMENT,
					ScriptDefinitionID INTEGER,
					DateScanned TIMESTAMP,
					DefinitionName VARCHAR(255),
					RuleID INTEGER NOT NULL,
					UniqueResourceName TEXT,
					IsCurrent SMALLINT DEFAULT 1,
					ScanResultID INTEGER,
					Name VARCHAR(255) NOT NULL,
					Value BLOB,
					PRIMARY KEY (EnvironmentEntryID)
			);
            
CREATE TABLE SpecimenArchive (
					SpecimenID INTEGER NOT NULL AUTO_INCREMENT,
					ScanResultID INTEGER NOT NULL,
					Encoding VARCHAR(255),
					DateObserved DATETIME NOT NULL,
					Data MEDIUMBLOB,
					MimeType VARCHAR(255),
					URL TEXT,
					ActualLength TEXT,
					PRIMARY KEY  (SpecimenID)
			);
            
CREATE TABLE EventLog (
					EventLogID INTEGER NOT NULL AUTO_INCREMENT,
					LogDate TIMESTAMP NOT NULL,
					Severity INTEGER NOT NULL,
					Title LONG VARCHAR,
					Notes LONG VARCHAR,
					PRIMARY KEY(EventLogID)
			);


CREATE TABLE HttpDiscoveryRule (
					HttpDiscoveryRuleID INTEGER NOT NULL AUTO_INCREMENT,
					ScanRuleID INTEGER,
					RecursionDepth INTEGER NOT NULL,
					ResourceScanLimit INTEGER NOT NULL,
					Domain VARCHAR(255),
					ScanFirstExternal SMALLINT default NULL,
					PRIMARY KEY(HttpDiscoveryRuleID)
			);
            
CREATE TABLE HttpDiscoveryResult (
					HttpDiscoveryResultID INTEGER NOT NULL AUTO_INCREMENT,
					RecursionDepth INTEGER,
					ScanResultID INTEGER NOT NULL,
					ResourceScanLimit INTEGER,
					Domain VARCHAR(255),
					ScanFirstExternal SMALLINT default NULL,
					PRIMARY KEY(HttpDiscoveryResultID)
			);
            
CREATE TABLE RuleURL (
					RuleURLID INTEGER NOT NULL AUTO_INCREMENT,
					ScanRuleID INTEGER NOT NULL,
					URL VARCHAR(4096),
					PRIMARY KEY(RuleURLID)
			);
            
CREATE TABLE SignatureScanResult (
					SignatureScanResultID INTEGER NOT NULL AUTO_INCREMENT,
					ScanResultID INTEGER NOT NULL,
					URL VARCHAR(32000),
					ContentType VARCHAR(4096),
					PRIMARY KEY(SignatureScanResultID)
			);
            
CREATE TABLE MatchedRule (
					MatchedRuleID INTEGER NOT NULL AUTO_INCREMENT,
					ScanResultID INTEGER NOT NULL,
					RuleName VARCHAR(255),
					RuleMessage VARCHAR(32000),
					RuleID INTEGER,
					MatchStart INTEGER,
					MatchLength INTEGER,
					Severity INTEGER,
					PRIMARY KEY(MatchedRuleID)
			);
            
CREATE TABLE ServiceScanRule (
					ServiceScanRuleID INTEGER NOT NULL AUTO_INCREMENT,
					ScanRuleID INTEGER NOT NULL,
					PortsOpen VARCHAR(4096),
					PortsToScan VARCHAR(4096),
					Server VARCHAR(255),
					PRIMARY KEY(ServiceScanRuleID)
			);
            
CREATE TABLE ServiceScanResult (
					ServiceScanResultID INTEGER NOT NULL AUTO_INCREMENT,
					ScanResultID INTEGER NOT NULL,
					PortsScanned VARCHAR(4096),
					PortsExpectedOpen VARCHAR(4096),
					PortsUnexpectedClosed VARCHAR(4096),
					PortsUnexpectedOpen VARCHAR(4096),
					PortsUnexpectedNotResponding VARCHAR(4096),
					Server VARCHAR(255),
					PRIMARY KEY(ServiceScanResultID)
			);
            
CREATE TABLE DefinitionPolicy (
					DefinitionPolicyID INTEGER NOT NULL AUTO_INCREMENT,
					SiteGroupID INTEGER NOT NULL,
					DefinitionID INTEGER,
					RuleID INTEGER,
					DefinitionName VARCHAR(255),
					DefinitionCategory VARCHAR(255),
					DefinitionSubCategory VARCHAR(255),
					Action INTEGER,
					URL VARCHAR(4096),
					PRIMARY KEY(DefinitionPolicyID)
			);
            
CREATE TABLE Action (
					ActionID INTEGER NOT NULL AUTO_INCREMENT,
					State BLOB,
					PRIMARY KEY(ActionID)
			);
            
CREATE TABLE EventLogHook (
					EventLogHookID INTEGER NOT NULL AUTO_INCREMENT,
					ActionID INTEGER NOT NULL,
					TypeID INTEGER,
					SiteGroupID INTEGER,
					RuleID INTEGER,
					MinimumSeverity INTEGER,
					Enabled INTEGER default 1,
					State BLOB,
					PRIMARY KEY(EventLogHookID)
			);

CREATE TABLE DefinitionErrorLog (
					DefinitionErrorLogID INTEGER NOT NULL AUTO_INCREMENT,
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
			);
            
-- 2 -- Create Indexes
Create index EventLogSeverityIndex on EventLog(Severity);
Create index EventLogDateIndex on EventLog(LogDate);
Create index EventLogDateSeverityIndex on EventLog(LogDate, Severity);

Create index EventLogDateIndexDesc on EventLog(LogDate Desc);
Create index EventLogDateSeverityIndexDesc on EventLog(LogDate, Severity Desc);
Create index EventLogIDIndexDesc on EventLog(EventLogID Desc);

Create index DefinitionPolicySiteGroupIndex on DefinitionPolicy(SiteGroupID);
Create index DefinitionScanResultIDIndex on SignatureScanResult(ScanResultID);
Create index DefinitionScanResultContentTypeIndex on SignatureScanResult(ContentType);

Create index ScanResultRuleIDIndex on ScanResult(ScanRuleID);
Create index ScanResultParentIDIndex on ScanResult(ParentScanResultID);
Create index ScriptEnvironmentScanResultIDIndexDesc on ScriptEnvironment(ScanResultID Desc);

-- 3 -- Populate Rights
insert into ObjectMap(`Table`, ObjectID) values("Rights", 1);
insert into Rights(RightName, RightDescription, ObjectID) values ('Users.Add', 'Create New Users', 1);
insert into ObjectMap(`Table`, ObjectID) values("Rights", 2);
insert into Rights(RightName, RightDescription, ObjectID) values ('Users.Edit', 'Edit User', 2);
insert into ObjectMap(`Table`, ObjectID) values("Rights", 3);
insert into Rights(RightName, RightDescription, ObjectID) values ('Users.View', 'View Users\' Details (including rights)', 3);
insert into ObjectMap(`Table`, ObjectID) values("Rights", 4);
insert into Rights(RightName, RightDescription, ObjectID) values ('Users.Delete', 'Delete Users', 4);
insert into ObjectMap(`Table`, ObjectID) values("Rights", 5);
insert into Rights(RightName, RightDescription, ObjectID) values ('Users.Unlock', 'Unlock Accounts (due to repeated authentication attempts)', 5);
insert into ObjectMap(`Table`, ObjectID) values("Rights", 6);
insert into Rights(RightName, RightDescription, ObjectID) values ('Users.UpdatePassword', 'Update Other\'s Password (applies only to the other users\' accounts)', 6);
insert into ObjectMap(`Table`, ObjectID) values("Rights", 7);
insert into Rights(RightName, RightDescription, ObjectID) values ('Users.UpdateOwnPassword', 'Update Account Details (applies only to the users\' own account)', 7);
insert into ObjectMap(`Table`, ObjectID) values("Rights", 8);
insert into Rights(RightName, RightDescription, ObjectID) values ('Users.Sessions.Delete', 'Delete Users\' Sessions (kick users off)', 8);
insert into ObjectMap(`Table`, ObjectID) values("Rights", 9);
insert into Rights(RightName, RightDescription, ObjectID) values ('Users.Sessions.View', 'View Users\' Sessions (see who is logged in)', 9);


insert into ObjectMap(`Table`, ObjectID) values("Rights", 10);
insert into Rights(RightName, RightDescription, ObjectID) values ('Groups.Add', 'Create New Groups', 10);
insert into ObjectMap(`Table`, ObjectID) values("Rights", 11);
insert into Rights(RightName, RightDescription, ObjectID) values ('Groups.View', 'View Groups', 11);
insert into ObjectMap(`Table`, ObjectID) values("Rights", 12);
insert into Rights(RightName, RightDescription, ObjectID) values ('Groups.Edit', 'Edit Groups', 12);
insert into ObjectMap(`Table`, ObjectID) values("Rights", 13);
insert into Rights(RightName, RightDescription, ObjectID) values ('Groups.Delete', 'Delete Groups', 13);
insert into ObjectMap(`Table`, ObjectID) values("Rights", 14);
insert into Rights(RightName, RightDescription, ObjectID) values ('Groups.Membership.Edit', 'Manage Group Membership', 14);


insert into ObjectMap(`Table`, ObjectID) values("Rights", 16);
insert into Rights(RightName, RightDescription, ObjectID) values ('SiteGroups.View', 'View Site Groups', 16);
insert into ObjectMap(`Table`, ObjectID) values("Rights", 17);
insert into Rights(RightName, RightDescription, ObjectID) values ('SiteGroups.Add', 'Create New Site Group', 17);
insert into ObjectMap(`Table`, ObjectID) values("Rights", 18);
insert into Rights(RightName, RightDescription, ObjectID) values ('SiteGroups.Delete', 'Delete Site Groups', 18);
insert into ObjectMap(`Table`, ObjectID) values("Rights", 19);
insert into Rights(RightName, RightDescription, ObjectID) values ('SiteGroups.Edit', 'Edit Site Groups', 19);


insert into ObjectMap(`Table`, ObjectID) values("Rights", 20);
insert into Rights(RightName, RightDescription, ObjectID) values ('System.Information.View', 'View System Information and Status', 20);
insert into ObjectMap(`Table`, ObjectID) values("Rights", 21);
insert into Rights(RightName, RightDescription, ObjectID) values ('System.Configuration.Edit', 'Modify System Configuration', 21);
insert into ObjectMap(`Table`, ObjectID) values("Rights", 22);
insert into Rights(RightName, RightDescription, ObjectID) values ('System.Configuration.View', 'View System Configuration', 22);
insert into ObjectMap(`Table`, ObjectID) values("Rights", 23);
insert into Rights(RightName, RightDescription, ObjectID) values ('System.Firewall.View', 'View Firewall Configuration', 23);
insert into ObjectMap(`Table`, ObjectID) values("Rights", 24);
insert into Rights(RightName, RightDescription, ObjectID) values ('System.Firewall.Edit', 'Change Firewall Configuration', 24);
insert into ObjectMap(`Table`, ObjectID) values("Rights", 25);
insert into Rights(RightName, RightDescription, ObjectID) values ('System.ControlScanner', 'Start/Stop Scanner', 25);
insert into ObjectMap(`Table`, ObjectID) values("Rights", 26);
insert into Rights(RightName, RightDescription, ObjectID) values ('SiteGroups.ScanAllRules', 'Allow Gratuitous Scanning of All Rules', 26);
insert into ObjectMap(`Table`, ObjectID) values("Rights", 27);
insert into Rights(RightName, RightDescription, ObjectID) values ('System.Shutdown', 'Shutdown system', 27);
