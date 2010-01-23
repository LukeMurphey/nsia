package net.lukemurphey.nsia.tests;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.*;

import net.lukemurphey.nsia.DerbyDatabaseInitializer;
import net.lukemurphey.nsia.NoDatabaseConnectionException;

import org.apache.commons.dbcp.BasicDataSource;

public class TestResources {

	private static TestResources testRes = null;
	
	public static TestResources getTestResources() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoDatabaseConnectionException{
		if( testRes != null){
			return testRes;
		}
		else{
			testRes = new TestResources();
			testRes.connectToInternalDatabase(false);
			return testRes;
		}
		
	}
	
	public static void closeTest() throws SQLException{
		if(testRes != null){
			testRes.connectionBroker.close();
		}
	}
	
	private org.apache.commons.dbcp.BasicDataSource connectionBroker;
	
	public static final String TEST_RESOURCE_DIRECTORY = "C:\\Users\\Luke Murphey\\workspace\\NSIA\\Development\\Test Case Resources\\";
	
	public static String readFileAsString(String filePath) throws java.io.IOException{
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader = new BufferedReader( new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead=0;
        while((numRead=reader.read(buf)) != -1){
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString();
    }
	
	public Connection getConnection() throws NoDatabaseConnectionException{
		Connection connection = null;
		try {
			connection = connectionBroker.getConnection();
		} catch (SQLException e) {
			throw new NoDatabaseConnectionException(e);
		}

		return connection;
	}
	
	public void connectToInternalDatabase( boolean createIfNonExistant ) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoDatabaseConnectionException{

		// 1 -- Setup the database
		String databaseDriver = "org.apache.derby.jdbc.EmbeddedDriver";
		String databaseLocation;
		
		if( createIfNonExistant )
			databaseLocation = "jdbc:derby:Database;create=true";
		else
			databaseLocation = "jdbc:derby:Database";
		
		
		
		connectionBroker = new BasicDataSource();
		connectionBroker.setMaxActive(50);
		connectionBroker.setDriverClassName(databaseDriver);
		connectionBroker.setUrl(databaseLocation);
		
		DerbyDatabaseInitializer initializer = new DerbyDatabaseInitializer (connectionBroker.getConnection());
		initializer.performSetup();
	}
	
}
