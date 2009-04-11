package net.lukeMurphey.nsia.scanRules;

import java.sql.SQLException;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.NoDatabaseConnectionException;
import net.lukeMurphey.nsia.NotFoundException;

public class ResponseTimeScan extends ScanRule {

	protected final static String RULE_TYPE = "Availability"; 
	protected String hostname = null;
	
	
	public ResponseTimeScan(Application application){
		super(application);
	}
	
	public ResponseTimeScan(Application application, String hostname){
		super(application);
		this.hostname = hostname;
	}
	
	public void delete() throws SQLException, NoDatabaseConnectionException {
		// TODO Auto-generated method stub
		
	}

	public ScanResult doScan() throws ScanException {
		// TODO Auto-generated method stub
		return null;
	}

	public String getRuleType() {
		return RULE_TYPE;
	}

	public String getSpecimenDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean loadFromDatabase(long scanRuleId) throws NotFoundException, NoDatabaseConnectionException, SQLException, ScanRuleLoadFailureException {
		// TODO Auto-generated method stub
		return false;
	}

	public ScanResult loadScanResult(long scanResultId) throws NotFoundException, NoDatabaseConnectionException, SQLException, ScanResultLoadFailureException {
		// TODO Auto-generated method stub
		return null;
	}

}
