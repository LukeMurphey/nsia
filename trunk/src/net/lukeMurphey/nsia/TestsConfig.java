package net.lukeMurphey.nsia;

import java.net.BindException;
import java.sql.SQLException;

import net.lukeMurphey.nsia.Application.RunMode;
import net.lukeMurphey.nsia.eventLog.EventLog;


public class TestsConfig {
	public static final String DB_DRIVER = "com.mysql.jdbc.Driver";
	public static final String DB_PATH = "jdbc:mysql://localhost/SiteSentry2";
	public static final String DB_USERNAME = "SiteSentry_Devel";
	public static final String DB_PASSWORD = "pmk5566";
	
	public static final String LOG_FILE = "testLog.log";
	
	public static Application getApplicationResource() throws BindException, SQLException, InputValidationException, Exception{
		// 1 -- Setup the database
		Application.startApplication(new String[0], RunMode.CLI);
		Application appRes = Application.getApplication();//new Application(new String[0]);
		//appRes.connectToDatabase(DB_PATH, DB_PASSWORD, DB_DRIVER);
		
		EventLog eventLog = new EventLog();//new File(LOG_FILE));
		appRes.setEventLog(eventLog);
		
		
		// 2 -- return the result
		return appRes;
	}
}
