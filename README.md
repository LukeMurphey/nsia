# NSIA: Network System Integrity Analysis

NSIA is an application for detecting defacements, errors, information leaks and other types of security problems on websites and web applications (think of it as a "website IDS").

See http://threatfactor.com

## Screenshots

### Overview Dashboard ###

The main dashboard clearly displays the status of the scanner and of the monitored websites. 
![Dashboard](doc/screenshots/Screenshot_Dashboard.jpg "Dashboard")


### Scan Result Report ###

Scan reports indicate what resources were discovered and provides details on what issues were observed. 
![Scan Result Report](doc/screenshots/Screenshot_Scan_report.png "Scan Result Report")

### ThreatScript Signature ###

ThreatScript signatures perform deep content analysis and can detect significant changes. 
![ThreatScript Signature](doc/screenshots/Screenshot_ThreatScript.jpg "ThreatScript Signature")

### ThreatPattern Signature ###

ThreatPattern signatures can identify known-bad elements such as attacks, offensive language and information leaks. 
![ThreatPattern Signature](doc/screenshots/Screenshot_Threatpattern.png "ThreatPattern Signature")

### Rule Status Overview ###

The rule status page shows indicates the current state of each rule; including which have found issues, were unable to scan (for example, due to an outage in the webserver) or those rules that have not been scanned yet. 
![Rule Status Overview](doc/screenshots/Screenshot_SiteGroupStatus.png "Rule Status Overview")

### Rule History Overview ###

The rule history page displays the status of past scans. 
![Rule History Overview](doc/screenshots/Screenshot_RuleHistory.png "Rule History Overview")

### Access Control ###

NSIA was designed for multiple users and supports sophisticated access controls.
![Access Control](doc/screenshots/Screenshot_AccessControl.png "Access Control")

### System Status ###

The system status page displays the operational status of the scanner, including memory usage, number of threads, number of rules evaluated, etc.

![System Status](doc/screenshots/Screenshot_SystemStatus.png "System Status")

### Scan Policy Management ###

Individual scan policies can be defined in order to tailor the scans according to the websites begin scanned. 
![Scan Policy Management](doc/screenshots/Screenshot_ScanPolicy.png "Scan Policy Management")

### Integrated Database ###

NSIA features a built-in database (no DBA required). NSIA maintains the database by creating backups, defragmenting indexes, etc. You can also use an external database instead. 
![Integrated Database](doc/screenshots/Screenshot_Database.png "Integrated Database")