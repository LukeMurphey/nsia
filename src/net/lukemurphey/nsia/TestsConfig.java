package net.lukemurphey.nsia;

import java.io.File;
import java.net.BindException;
import java.sql.SQLException;

import net.lukemurphey.nsia.Application.RunMode;
import net.lukemurphey.nsia.eventlog.EventLog;


public class TestsConfig {
	public static final String DB_DRIVER = "com.mysql.jdbc.Driver";
	public static final String DB_PATH = "jdbc:mysql://localhost/SiteSentry2";
	public static final String DB_USERNAME = "SiteSentry_Devel";
	public static final String DB_PASSWORD = "pmk5566";
	
	public static final String LOG_FILE = "testLog.log";
	public static final String DEFAULT_DATABASE_PATH = "../tmp/test_database";
	
	private static Application testApp = null;
	
	public synchronized static Application getApplication() throws NoDatabaseConnectionException{
		
		if( testApp != null ){
			testApp = new Application(DEFAULT_DATABASE_PATH);
		}
		
		return testApp;
	}
	
	public synchronized static void stopApplication(){
		if( testApp != null ){
			testApp.shutdown();
			testApp = null;
			
			//Purge the database
			GenericUtils.deleteDirectory( new File(DEFAULT_DATABASE_PATH) );
		}
	}
	
	public static Application getApplicationResource() throws BindException, SQLException, InputValidationException, Exception{
		// 1 -- Setup the database
		Application.startApplication(new String[0], RunMode.CLI);
		Application appRes = Application.getApplication();//new Application(new String[0]);
		//appRes.connectToDatabase(DB_PATH, DB_PASSWORD, DB_DRIVER);
		
		EventLog eventlog = new EventLog();//new File(LOG_FILE));
		appRes.setEventLog(eventlog);
		
		
		// 2 -- return the result
		return appRes;
	}
}
