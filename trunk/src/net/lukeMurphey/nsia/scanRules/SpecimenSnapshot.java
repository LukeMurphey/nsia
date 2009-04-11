package net.lukeMurphey.nsia.scanRules;

import java.sql.*;
import java.net.*;
import java.nio.charset.*;
import net.lukeMurphey.nsia.*;
import net.lukeMurphey.nsia.Application.DatabaseAccessType;
import java.util.Vector;
import java.util.Date;

public class SpecimenSnapshot {
	
	private byte[] data;
	private Charset encoding;
	private Date observedTime;
	private String mimeType;
	private int specimenId = -1;
	private int scanResultId = -1;
	private String filename;
	private int actualLength;
	private Application application;

	/**
	 * Private constructor is intended for the static methods that load an instance from a database record.
	 */
	private SpecimenSnapshot(){
		
	}
	
	public SpecimenSnapshot(String mimeType, String encoding, URL url, int actualLength, Date observedTime, byte[] bytes, Application app ){
		this(mimeType, Charset.forName(encoding), url, actualLength, observedTime, bytes, app);
	}
	
	public SpecimenSnapshot(String mimeType, Charset encoding, URL url, int actualLength, Date observedTime, byte[] bytes, Application app ){
		
		// 0 -- Precondition check
		
		
		// 1 -- Initialize the class
		this.mimeType = mimeType;
		this.encoding = encoding;
		this.filename = url.toExternalForm();
		this.actualLength = actualLength;
		this.observedTime = observedTime;
		data = new byte[bytes.length];
		System.arraycopy(bytes, 0, data, 0, bytes.length);
		application = app;
	}
	
	public SpecimenSnapshot(int actualLength, Date observedTime, DataSpecimen dataSpecimen ){
		
		// 0 -- Precondition check
		if( observedTime == null ){
			throw new IllegalArgumentException("The observed time argument cannot be null");
		}
		
		if( dataSpecimen == null ){
			throw new IllegalArgumentException("The data specimen argument cannot be null");
		}
		
		
		// 1 -- Initialize the class
		this.mimeType = dataSpecimen.getContentType();
		this.data = dataSpecimen.getBytes();
		this.filename = dataSpecimen.getFilename();
		this.actualLength = actualLength;
		this.observedTime = observedTime;
	}
	
	public SpecimenSnapshot(Date observedTime, DataSpecimen dataSpecimen ){
		
		// 0 -- Precondition check
		if( observedTime == null ){
			throw new IllegalArgumentException("The observed time argument cannot be null");
		}
		
		if( dataSpecimen == null ){
			throw new IllegalArgumentException("The data specimen argument cannot be null");
		}
		
		
		// 1 -- Initialize the class
		this.mimeType = dataSpecimen.getContentType();
		this.data = dataSpecimen.getBytes();
		this.filename = dataSpecimen.getFilename();
		this.actualLength = -1;
		this.observedTime = observedTime;
	}
	
	/**
	 * Returns the data returned by the remote system encoded with the character set encoding specified by the server.
	 * @return
	 */
	public String getDataAsString(){
		return new String(data, encoding);
	}
	
	/**
	 * Gets the data returned by the remote system.
	 * @return
	 */
	public byte[] getData(){
		return data;
	}
	
	/**
	 * Method returns the length of the data provided by the remote system. In most cases, this will be the same as the length of
	 * the data returned, however, it will be different if the data in the snapshot is a portion of the actual data (many rules
	 * have an upper limit on the size that it will examine, example: may only look at the first 5 MBs).  
	 * @return
	 */
	public int getActualLength(){
		return actualLength;
	}
	
	/**
	 * Get a string specifying the location that the data was obtained from.
	 * @return
	 */
	public String getFilename(){
		return filename;
	}
	
	/**
	 * Get the ID of the specimen. The specimen ID is the unique ID that identifies the resource in the database.  
	 * @return
	 */
	public int specimenID(){
		return specimenId;
	}
	
	/**
	 * Get the scan result ID that is associated with the specimen. This method will return -1 if no scan result is associated with this entry.  
	 * @return
	 */
	public int getScanResultID(){
		return scanResultId;
	}
	
	/**
	 * Get the mimetype of the data.
	 * @return
	 */
	public String getMimeType(){
		return mimeType;
	}
	
	/**
	 * Get the date that the sample was observed.
	 * @return
	 */
	public Date getObservedDate(){
		return observedTime;
	}
	
	/**
	 * Retrieve the specimen with the given identifer
	 * @param specimenId
	 * @param app
	 * @return
	 * @throws SQLException
	 * @throws NotFoundException
	 * @throws NoDatabaseConnectionException
	 */
	public static SpecimenSnapshot loadBySpecimenID(int specimenId, Application app) throws SQLException, NotFoundException, NoDatabaseConnectionException{
		
		// 0 -- Precondition check
		if( app == null ){
			throw new IllegalArgumentException("The application object cannot be null");
		}
		
		
		// 1 -- Attempt to retrieve the specimen
		PreparedStatement preparedStatement = null;
		ResultSet result = null;
		Connection connection = null;
		
		try{
			connection = app.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			preparedStatement = connection.prepareStatement("Select * from SpecimenArchive where SpecimenID = ?");
			preparedStatement.setInt(1, specimenId);
			
			result = preparedStatement.executeQuery();
			
			if( result.next()){
				return loadFromRow(result, app);
			}
			else{
				throw new NotFoundException("A specimen could not be found that matches the identifier given");
			}
		}
		finally{
			if( preparedStatement != null ){
				preparedStatement.close();
			}
			
			if( result != null ){
				result.close();
			}
			
			if( connection != null ){
				connection.close();
			}
		}
	}
	
	/**
	 * Retrieve all snapshots associated with the given scan result.
	 * @param scanResultID
	 * @param app
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public static SpecimenSnapshot[] loadByScanResultID(int scanResultID, Application app) throws SQLException, NoDatabaseConnectionException{
		// 0 -- Precondition check
		if( app == null ){
			throw new IllegalArgumentException("The application object cannot be null");
		}
		
		
		// 1 -- Attempt to retrieve the specimen
		PreparedStatement preparedStatement = null;
		ResultSet result = null;
		Connection connection = null;
		Vector<SpecimenSnapshot> snapshots = new Vector<SpecimenSnapshot>();
		
		try{
			connection = app.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			preparedStatement = connection.prepareStatement("Select * from SpecimenArchive where ScanResultID = ?");
			preparedStatement.setInt(1, scanResultID);
			
			result = preparedStatement.executeQuery();
			
			while( result.next()){
				snapshots.add( loadFromRow(result, app) );
			}
		}
		finally{
			if( preparedStatement != null ){
				preparedStatement.close();
			}
			
			if( result != null ){
				result.close();
			}
			
			if( connection != null ){
				connection.close();
			}
		}
		
		SpecimenSnapshot[] snapshotsArray = new SpecimenSnapshot[snapshots.size()];
		snapshots.toArray(snapshotsArray);
		
		return snapshotsArray;
	}
	
	/**
	 * Retrieve the specimen for the given data observed on the given date. 
	 * @param url
	 * @param observedDate
	 * @param app
	 * @return
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws NotFoundException
	 */
	public static SpecimenSnapshot load(URL url, Date observedDate, Application app) throws SQLException, NoDatabaseConnectionException, NotFoundException{
		
		// 0 -- Precondition check
		if( app == null ){
			throw new IllegalArgumentException("The application object cannot be null");
		}
		
		
		// 1 -- Attempt to retrieve the specimen
		PreparedStatement preparedStatement = null;
		ResultSet result = null;
		Connection connection = null;
		
		try{
			connection = app.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			preparedStatement = connection.prepareStatement("Select * from SpecimenArchive where URL = ? and DateObserved = ?");
			preparedStatement.setString(1, url.toString());
			preparedStatement.setDate(2, new java.sql.Date(observedDate.getTime()) );
			
			result = preparedStatement.executeQuery();
			
			if( result.next()){
				return loadFromRow(result, app);
			}
			else{
				throw new NotFoundException("A specimen could not be found that matches the identifier given");
			}
		}
		finally{
			if( preparedStatement != null ){
				preparedStatement.close();
			}
			
			if( result != null ){
				result.close();
			}
			
			if( connection != null ){
				connection.close();
			}
		}
	}
	
	public void save() throws SQLException, NoDatabaseConnectionException{
		synchronized(this){
			if( specimenId < 0 ){
				insert();
			}
			else{
				update();
			}
		}
	}
	
	private void update() throws SQLException, NoDatabaseConnectionException{
		
		Connection connection = null;
		ResultSet resultSet = null;
		PreparedStatement statement = null;
		
		try{
			connection = application.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			statement = connection.prepareStatement("Update SpecimenArchive Set ScanResultID = ?, Encoding = ?, DateObserved = ?, Data = ?, MimeType = ?, URL = ?, ActualLength = ? where SpecimenID = ?");
			
			statement.setInt(1, scanResultId);
			statement.setString(2, encoding.displayName());
			statement.setDate(3, new java.sql.Date(observedTime.getTime()) );
			statement.setBytes(4, data);
			statement.setString(5, mimeType);
			statement.setString(6, filename);
			statement.setInt(7, actualLength);
			statement.setInt(8, specimenId);
			
			statement.executeUpdate();
			
		}
		finally{
			if( connection != null ){
				connection.close();
			}
			
			if( resultSet != null ){
				resultSet.close();
			}
			
			if( statement != null ){
				statement.close();
			}
		}
	}
	
	private void insert() throws NoDatabaseConnectionException, SQLException{
		Connection connection = null;
		ResultSet resultSet = null;
		PreparedStatement statement = null;
		
		try{
			connection = application.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			statement = connection.prepareStatement("Insert into SpecimenArchive (ScanResultID, Encoding, DateObserved, Data, MimeType, URL, ActualLength) values (?,?,?,?,?,?,?)", PreparedStatement.RETURN_GENERATED_KEYS);
			
			statement.setInt(1, scanResultId);
			statement.setString(2, encoding.displayName());
			statement.setDate(3, new java.sql.Date(observedTime.getTime()) );
			statement.setBytes(4, data);
			statement.setString(5, mimeType);
			statement.setString(6, filename);
			statement.setInt(7, actualLength);
			
			statement.execute();
			
			resultSet = statement.getGeneratedKeys();
			
			if(resultSet.next()){
				specimenId = resultSet.getInt(1);
			}
			else{
				specimenId = -1;
			}
			
		}
		finally{
			if( connection != null ){
				connection.close();
			}
			
			if( resultSet != null ){
				resultSet.close();
			}
			
			if( statement != null ){
				statement.close();
			}
		}
	}
	
	/**
	 * Create a new specimen snapshot from the result set given. This method assumes that result.next() has already been called; failure
	 * to call this method first will generate and exception.
	 * @param result
	 * @param app
	 * @return
	 * @throws SQLException
	 */
	private static SpecimenSnapshot loadFromRow( ResultSet result, Application app ) throws SQLException{
		
		SpecimenSnapshot snapshot = new SpecimenSnapshot();
		
		snapshot.specimenId = result.getInt("SpecimentID");
		snapshot.scanResultId = result.getInt("ScanResultID");
		snapshot.actualLength = result.getInt("ActualLength");
		snapshot.application = app;
		
		Blob dataBlob = result.getBlob("Data");
		snapshot.data = dataBlob.getBytes(0, Integer.MAX_VALUE);
		
		String encoding = result.getString("Encoding");
		snapshot.encoding = Charset.forName(encoding);
		
		snapshot.mimeType = result.getString("MimeType");
		snapshot.observedTime = result.getDate("DateObserved");
		
		snapshot.filename = result.getString("URL");
		
		return snapshot;
	}
	
}
