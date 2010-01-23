package net.lukemurphey.nsia.scan;

import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Vector;
import java.util.Iterator;
import java.util.Date;

import javax.script.ScriptException;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.DisallowedOperationException;
import net.lukemurphey.nsia.DuplicateEntryException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.Application.DatabaseAccessType;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.eventlog.EventLogMessage.Category;
import net.lukemurphey.nsia.scan.DefinitionSet.DefinitionType;
import net.lukemurphey.nsia.scan.DefinitionSet.DefinitionVersionID;

import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcException;

public class DefinitionArchive {
	
	public static final String DEFINITION_SUPPORT_API_URL = "https://threatfactor.com/xmlrpc/";
	
	/* The definition version refers to the format of the definitions that definition engine accepts. As updates to the scanning engine are released,
	 * older scanning engines can still receive definitions updates though they won't be able to accept new definitions that use the updated
	 */
	public static final int DEFINITION_VERSION = 1;
	
	private Application application = null;
	private DefinitionSet definitionSet = null;
	
	// The following field is used to identify the next ID that should be used to create a new definition 
	//private int nextDefinitionID = -1;
	
	private static DefinitionArchive archive = null;
	
	public static DefinitionArchive getArchive() throws DefinitionSetLoadException, SQLException, NoDatabaseConnectionException, InputValidationException{
		return getArchive(false);
	}
	/**
	 * Get the global instance of the definition archive.
	 * @return
	 * @throws InputValidationException 
	 * @throws NoDatabaseConnectionException 
	 * @throws SQLException 
	 * @throws DefinitionSetLoadException 
	 */
	public static DefinitionArchive getArchive(boolean ignoreErrors) throws DefinitionSetLoadException, SQLException, NoDatabaseConnectionException, InputValidationException{
		
		if(archive == null){
			archive = new DefinitionArchive( Application.getApplication(), ignoreErrors );
		}

		return archive;
	}
	
	/**
	 * Instantiate a content definition archive by loading the definitions from the database.
	 * @param app
	 * @throws NoDatabaseConnectionException 
	 * @throws SQLException 
	 * @throws DefinitionSetLoadException 
	 * @throws InputValidationException 
	 */
	private DefinitionArchive( Application app, boolean ignoreLoadErrors ) throws DefinitionSetLoadException, SQLException, NoDatabaseConnectionException, InputValidationException{
		
		// 0 -- Precondition check
		if( app == null){
			throw new IllegalArgumentException("The application object cannot be null");
		}
		
		// 1 -- Initialize the class
		application = app;
		
		if( ignoreLoadErrors ){
			try{
				definitionSet = loadDefinitionSet(app);
			}
			catch(DefinitionSetLoadException e){
				definitionSet = new DefinitionSet(new Date(), new Definition[0], null);
			}
		}
		else{
			definitionSet = loadDefinitionSet(app);
		}
		
	}
	
	public int getCustomDefinitionsCount(){
		return definitionSet.getCustomDefinitionsCount();
	}
	
	/**
	 * Get the list of definitions.
	 * @return
	 */
	public Definition[] getDefinitions(){
		return definitionSet.getDefinitions();
	}
	
	/**
	 * Get the next ID that could be used for a definition.
	 * @param returnNextOfficial
	 * @return
	 */
	public int getNextOpenID( boolean returnNextOfficial ){
		return definitionSet.getNextOpenID(returnNextOfficial);
	}
	
	/**
	 * Get the list of definitions, sorted by name.
	 * @return
	 */
	public Definition[] getDefinitionsSorted(){
		return definitionSet.getDefinitionsSorted();
	}
	
	/**
	 * Get the definition with the given identifier.
	 * @return
	 * @throws NotFoundException 
	 */
	public Definition getDefinitionByLocalID(int localId) throws NotFoundException{
		
		for (MetaDefinition metaSig : MetaDefinition.DEFAULT_META_DEFINITIONS) {
			if( metaSig.getLocalID() == localId ){
				return metaSig;
			}
		}
		
		return definitionSet.getDefinitionByLocalID(localId);
	}
	
	/**
	 * Find the definition with the given ID.
	 * @param officialID
	 * @return
	 * @throws NotFoundException
	 */
	public Definition getDefinition(int officialID) throws NotFoundException{
		
		for (MetaDefinition metaSig : MetaDefinition.DEFAULT_META_DEFINITIONS) {
			if( metaSig.getID() == officialID ){
				return metaSig;
			}
		}
		
		return definitionSet.getDefinition(officialID);
	}
	
	/**
	 * Get the definition with the given identifier.
	 * @return
	 * @throws NotFoundException 
	 */
	public Definition getDefinition(String name) throws NotFoundException{
		
		for (MetaDefinition metaSig : MetaDefinition.DEFAULT_META_DEFINITIONS) {
			if( metaSig.getFullName().equalsIgnoreCase(name) ){
				return metaSig;
			}
		}
		
		return definitionSet.getDefinition(name);
	}
	
	/**
	 * Gets a string with an XML format of the rules.
	 * @return
	 */
	public String getAsXML( boolean includeCustomDefinitionsOnly ){
		return definitionSet.getAsXML(includeCustomDefinitionsOnly);
	}
	
	/**
	 * Gets a string with an XML format of the rules.
	 * @return
	 */
	public String getAsXML(){
		return definitionSet.getAsXML();
	}
	
	/**
	 * Get the number of definitions.
	 * @return
	 */
	public int size(){
		return definitionSet.size();
	}
	
	/**
	 * Get the definition set version ID.
	 * @return
	 */
	public DefinitionVersionID getVersionID(){
		return definitionSet.getVersionID();
	}
	
	/**
	 * Get the date that the definitions were updated.
	 * @return
	 */
	public Date getDefinitionSetDate(){
		return definitionSet.getDefinitionSetDate();
	}
	
	/**
	 * Get the definitions set.
	 * @return
	 */
	public DefinitionSet getDefinitionSet(){
		return definitionSet;
	}
	
	/**
	 * Loads the array of definitions from the given database connection.
	 * @param connection
	 * @return
	 * @throws DefinitionSetLoadException
	 * @throws SQLException
	 */
	private static Vector<Definition> loadDefinitions(Connection connection) throws DefinitionSetLoadException, SQLException{
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		
		// 1 -- Connect to the database and load the definitions
		Vector<Definition> definitionsVector = new Vector<Definition>();
		
		try{
		
			preparedStatement = connection.prepareStatement("Select * from Definitions order by DefinitionID ASC");
			
			resultSet = preparedStatement.executeQuery();
			
			while(resultSet.next()){
				int type = resultSet.getInt("Type");
				
				Definition contentDefinition = null;
				
				try{
					if( DefinitionType.SCRIPT.ordinal() == type ){
						contentDefinition = ScriptDefinition.parse( resultSet.getString("Code"), resultSet.getInt("DefinitionID") );
					}
					else if( DefinitionType.PATTERN.ordinal() == type ){
						contentDefinition = PatternDefinition.parse( resultSet.getString("Code"), resultSet.getInt("DefinitionID") );
					}
					
					definitionsVector.add(contentDefinition);
				}
				catch(InvalidDefinitionException e){
					throw new DefinitionSetLoadException( "Definition is invalid", e);
				}
				catch(UnpurposedDefinitionException e){
					throw new DefinitionSetLoadException( "Definition is invalid (has no purpose)", e);
				}
			}
		
		}
		catch( SQLException e){
			throw new DefinitionSetLoadException( "Definition set could not be loaded, SQL exception occured", e);
		}
		finally{
			
			if( preparedStatement != null ){
				preparedStatement.close();
			}
			
			if( resultSet != null ){
				resultSet.close();
			}
		}

		return definitionsVector;
	}
	
	
	/**
	 * Load the definition set from the database.
	 * @param connection
	 * @return
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws DefinitionSetLoadException 
	 * @throws SQLException 
	 * @throws InputValidationException 
	 * @throws NoDatabaseConnectionException 
	 * @throws ParseException
	 */
	private static DefinitionSet loadDefinitionSet(Application application) throws DefinitionSetLoadException, SQLException, NoDatabaseConnectionException, InputValidationException{

		
		Connection connection = null;
		
		try{
			connection = application.getDatabaseConnection(DatabaseAccessType.SCANNER);
		
			Vector<Definition> definitions = loadDefinitions( connection );
			
			SimpleDateFormat dateFormat = new SimpleDateFormat( DefinitionSet.DEFINITION_SET_DATE_FORMAT );
			
			DefinitionSet definitionSet = null;
			
			try {
				if( application.getApplicationParameters().doesParameterExist("_DefinitionDate") == false || application.getApplicationParameters().doesParameterExist("_DefinitionVersion") == false || definitions.size() == 0){
					definitionSet = new DefinitionSet( null, definitions, null);
				}
				else{
					definitionSet = new DefinitionSet( dateFormat.parse(application.getApplicationParameters().getParameter("_DefinitionDate", "")), definitions, application.getApplicationParameters().getParameter("_DefinitionVersion", ""));
				}
			} catch (ParseException e) {
				definitionSet = new DefinitionSet( null, definitions, null); //Load a null definition date if the date is invalid.
			}

			return definitionSet;
		}
		finally{
			
			if( connection != null){
				connection.close();
			}
		}
	}
	
	
	/**
	 * Causes the definition set to save all of the definitions to the database.
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 */
	private void saveToDatabase( DefinitionSet definitionSet ) throws NoDatabaseConnectionException, SQLException{
		
		Connection connection = null;
		
		try{
			connection = application.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			for( int c = 0; c < definitionSet.size(); c++){
				saveToDatabase(connection, definitionSet.get(c));
			}
		}
		finally{
			if( connection != null )
				connection.close();
		}
		
		// Update the list of definition errors
		DefinitionErrorList errors = DefinitionErrorList.load(application);
		errors.clearOutdatedErrors(this.definitionSet);	
		
	}
	
	/**
	 * Adds the definition to the active definition set and saves the new definition to the database. Returns the local ID (the identifier that identifies the definition on this system). 
	 * @param newDefinition
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws DisallowedOperationException 
	 * @throws DuplicateEntryException 
	 */
	public int addDefinition( Definition newDefinition ) throws SQLException, NoDatabaseConnectionException, DisallowedOperationException, DuplicateEntryException{
		
		// 0 -- Precondition Check
		
		//	 0.1 -- Make sure the definition is not null
		if( newDefinition == null){
			throw new IllegalArgumentException("The definition to add cannot be null");
		}
		
		//	 0.2 -- Make sure the definition does not have an ID field that is within the reserved range (the reserved range is for definitions from the official set only)
		if( newDefinition.isOfficial() == true ){
			throw new DisallowedOperationException("A custom definition must have an ID field greater than 1,000,000; IDs less than 1000000 are reserved for definitions from the official set");
		}
		
		
		
		synchronized (archive) {
			
			// 1 -- Try to add the definition to the active set
			definitionSet.add(newDefinition);
			
			// 2 -- Add the definition to the database
			Connection connection = null;
			try{
				
				int localId = -1;
				if( application != null )
				{
					connection = application.getDatabaseConnection(DatabaseAccessType.SCANNER);
					localId = saveToDatabase( connection, newDefinition );
				}
				
				newDefinition.localId = localId;
				
				// Update the list of definition errors
				DefinitionErrorList errors = DefinitionErrorList.load(application);
				errors.clearOutdatedErrors(this.definitionSet);
				
				return localId;
			}
			finally{
				if( connection != null )
					connection.close();
			}
			
				
		}
		
	}
	
	/**
	 * Gets the date of the latest definitions set from the server.
	 * @return
	 * @throws XmlRpcException
	 * @throws IOException
	 * @throws ParseException 
	 */
	public static Date getLatestAvailableDefinitionSetDate() throws XmlRpcException, IOException, ParseException{
		XmlRpcClient client = new XmlRpcClient( DEFINITION_SUPPORT_API_URL );
		
		Vector<Integer> params = new Vector<Integer>();
		params.add(new Integer(DEFINITION_VERSION) );
		
		SimpleDateFormat dateFormat = new SimpleDateFormat(DefinitionSet.DEFINITION_SET_DATE_FORMAT);
		
		Object result = client.execute( "Definitions.latestDate", params );
		
		if ( result != null && result instanceof XmlRpcException ){
			throw (XmlRpcException)result;
		}
        if ( result != null && result instanceof String ){
            return dateFormat.parse(result.toString());
        }
        else{
        	return null;
        }
	}
	
	/*
	 * Determines if a new definition set is available.
	 */
	public boolean isNewDefinitionSetAvailable(){
		try{
			DefinitionVersionID versionID = getLatestAvailableDefinitionSetID();
			
			if( this.definitionSet.getVersionID() == null ){
				return true;
			}
			else{
				return versionID.revisionID() != this.definitionSet.getVersionID().revisionID();
			}
			//return versionID.revisionID() > this.definitionSet.getVersionID().revisionID();
			
		}
		catch(Exception e){
			Application.getApplication().logExceptionEvent( new EventLogMessage(Category.INTERNAL_ERROR, new EventLogField( FieldName.MESSAGE, "Error observed when checking for new definitions")), e);
		}
		
		return false;
	}
	
	/**
	 * Retrieves the latest set of definitions from the server and applies them.
	 * @return
	 * @throws IOException 
	 * @throws XmlRpcException 
	 * @throws DefinitionSetLoadException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws DefinitionArchiveException 
	 * @throws DefinitionUpdateFailedException 
	 */
	public DefinitionVersionID updateDefinitions() throws DefinitionUpdateFailedException{
		
		try{
			String key;
			
			key = application.getApplicationConfiguration().getLicenseKey();
			
			if( key == null ){
				return null;
			}
			
			XmlRpcClient client = new XmlRpcClient( DEFINITION_SUPPORT_API_URL );
			
			Vector<Object> params = new Vector<Object>();
			params.add(new Integer(DEFINITION_VERSION) );
			params.add( key );
			
			Object result = client.execute( "Definitions.latestDefinitions", params );
	
			if ( result != null && result instanceof XmlRpcException ){
				throw new DefinitionUpdateFailedException( "Error when attempting to retrieve definition updates from server", (XmlRpcException)result );
			}
	        if ( result != null && result instanceof String ){
	            String definitionsXml = (String)result;
	            DefinitionSet definitionSet = DefinitionSet.loadFromString(definitionsXml);
	            
	            DefinitionArchive.archive.updateDefinitions(definitionSet, true);//Note: the argument of true means that all existing definitions marked as "Official" will be removed first (since they will be replaced)
	            return definitionSet.getVersionID();
	        }
	        else{
	        	return null;
	        }
		} catch(XmlRpcException e){
			throw new DefinitionUpdateFailedException( "Error when attempting to retrieve definition updates from server", e );
		} catch (ParserConfigurationException e) {
			throw new DefinitionUpdateFailedException( "Definitions loaded from server are corrupted", e );
		} catch (SAXException e) {
			throw new DefinitionUpdateFailedException( "Error when attempting to retrieve definition updates from server", e );
		} catch (IOException e) {
			throw new DefinitionUpdateFailedException( "Error when attempting to retrieve definition updates from server", e );
		} catch (DefinitionSetLoadException e) {
			throw new DefinitionUpdateFailedException( "Error when attempting to load definition updates from server", e );
		} catch (DefinitionArchiveException e) {
			throw new DefinitionUpdateFailedException( "Error when attempting to load definition updates from server", e );
		} catch (NoDatabaseConnectionException e) {
			throw new DefinitionUpdateFailedException( "Could not retrieve license key", e );
		} catch (SQLException e) {
			throw new DefinitionUpdateFailedException( "Could not retrieve license key", e );
		} catch (InputValidationException e) {
			throw new DefinitionUpdateFailedException( "License is invalid, load definition updates from server", e );
		}
	}
	
	/**
	 * Gets the version identifier of the latest definition set from the server.
	 * @return
	 * @throws XmlRpcException
	 * @throws IOException
	 */
	public static DefinitionVersionID getLatestAvailableDefinitionSetID() throws XmlRpcException, IOException{
		XmlRpcClient client = new XmlRpcClient( DEFINITION_SUPPORT_API_URL );
		
		Vector<Integer> params = new Vector<Integer>();
		params.add(new Integer(DEFINITION_VERSION) );
		
		Object result = client.execute( "Definitions.latestID", params );

		if ( result != null && result instanceof XmlRpcException ){
			throw (XmlRpcException)result;
		}
        if ( result != null && result instanceof String ){
            return new DefinitionVersionID( result.toString() );
        }
        else{
        	return null;
        }
	}
	
	/**
	 * Update the definition in the database the corresponds to the given ID. 
	 * @param definition
	 * @param localId
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws DisallowedOperationException 
	 */
	public void updateDefinition( Definition definition, int localId ) throws NoDatabaseConnectionException, SQLException, DisallowedOperationException{
		
		// 0 -- Precondition Check
		
		//	 0.1 -- Make sure the definition is not null
		if( definition == null ){
			throw new IllegalArgumentException("The definition to update cannot be null");
		}
		
		//	 0.2 -- Make sure the definition does not have an ID field that is within the reserved range (the reserved range is for definitions from the official set only)
		if( definition.isOfficial() ){
			throw new DisallowedOperationException("A custom definition must have an ID field greater than 1,000,000; IDs less than 1000000 are reserved for definitions from the official set");
		}
		
		
		// 1 -- Update the record in the database
		Connection connection = null;	
		PreparedStatement installDefinition = null;
		
		synchronized (archive) {
			try{
				connection = application.getDatabaseConnection(DatabaseAccessType.SCANNER);
				
				installDefinition = connection.prepareStatement("Update Definitions set Category=?, SubCategory=?, Name=?, DefaultMessage=?, AssignedID=?, Version=?, Code=?, Type=? where DefinitionID = ?");
				
				installDefinition.setString(1, definition.getCategoryName());
				installDefinition.setString(2, definition.getSubCategoryName());
				installDefinition.setString(3, definition.getName());
				installDefinition.setString(4, definition.getMessage());
				
				installDefinition.setInt(5, definition.getID());
				installDefinition.setInt(6, definition.getRevision());
				
				if( definition instanceof ScriptDefinition ){
					ScriptDefinition script = (ScriptDefinition)definition;
					installDefinition.setString(7, script.getScript());
					installDefinition.setInt(8, DefinitionType.SCRIPT.ordinal());
				}
				else if( definition instanceof PatternDefinition ){
					PatternDefinition threatSig = (PatternDefinition)definition;
					installDefinition.setString(7, threatSig.getRuleCode());
					installDefinition.setInt(8, DefinitionType.PATTERN.ordinal());
				}
				
				installDefinition.setInt(9, localId);
				
				if( installDefinition.executeUpdate() > 0 ){
					//Replace the definition in memory
					definitionSet.replaceDefinition(definition, localId);
				}
				
			}
			finally{
				if( installDefinition != null )
					installDefinition.close();
				
				if( connection != null )
					connection.close();
			}
			
			// Update the list of definition errors
			DefinitionErrorList errors = DefinitionErrorList.load(application);
			errors.clearOutdatedErrors(this.definitionSet);	
		}
	}
	
	/**
	 * Update the definition in the database.
	 * @param definition
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws DisallowedOperationException 
	 */
	public void updateDefinition( Definition definition ) throws NoDatabaseConnectionException, SQLException, DisallowedOperationException{
		// 0 -- Precondition Check
		if( definition == null ){
			throw new IllegalArgumentException("The definition to update cannot be null");
		}
		
		if( definition.localId < 0 ){
			throw new IllegalArgumentException("The definition local ID is not valid");
		}
		
		updateDefinition(definition, definition.localId);
		
	}
	
	/**
	 * Removes the definition from the list of active definitions as well as from the list contained in the database.
	 * @param localId
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws DisallowedOperationException 
	 * @throws DisallowedOperationException 
	 */
	public void removeByID( int ID ) throws SQLException, NoDatabaseConnectionException, DisallowedOperationException{
		// 0 -- Precondition check
		if( application == null){
			throw new IllegalStateException("The definition set does not have an instance of an application object");
		}
		
		
		Connection connection = null;
		PreparedStatement deleteStatement = null;
		
		// 1 -- Find the definition
		synchronized (archive) {
			
			Definition definition = null;
			
			try{
				definition = archive.getDefinition(ID);
			}
			catch(NotFoundException e){
				//Definition not found; therefore, no deletion is necessary. 
				return;
			}
			
			// 2 -- Make sure the definition is not one from the official set (these cannot be deleted since a new definition set update would re-add it anyways)
			if( definition != null && definition.isOfficial() ){
				throw new DisallowedOperationException("Definitions from the official set cannot be deleted");
			}
			
			// 3 -- Delete the definition from the database
			try{
				connection = application.getDatabaseConnection(DatabaseAccessType.SCANNER);
				
				deleteStatement = connection.prepareStatement("Delete from Definitions where DefinitionID = ?");
				deleteStatement.setInt(1, definition.getLocalID());
				
				deleteStatement.execute();
				
				// 4 -- Remove the definition from the active definition set
				if( definition != null ){
					definitionSet.remove(definition);
				}
			}
			finally{
				if( connection != null ){
					connection.close();
				}
				
				if( deleteStatement != null ){
					deleteStatement.close();
				}
			}
			
			// Update the list of definition errors
			DefinitionErrorList errors = DefinitionErrorList.load(application);
			errors.clearOutdatedErrors(this.definitionSet);	
		}
	}
	
	private int saveToDatabase( Connection connection, Definition definition ) throws SQLException{
		PreparedStatement installDefinition = null;
		ResultSet resultSet = null;
		int localId = -1;
		
		try{
			installDefinition = connection.prepareStatement("Insert into Definitions (Category, SubCategory, Name, DefaultMessage, AssignedID, Version, Code, Type) values(?, ?, ?, ?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
			
			installDefinition.setString(1, definition.getCategoryName());
			installDefinition.setString(2, definition.getSubCategoryName());
			installDefinition.setString(3, definition.getName());
			installDefinition.setString(4, definition.getMessage());
			
			installDefinition.setInt(5, definition.getID());
			installDefinition.setInt(6, definition.getRevision());
			
			if( definition instanceof ScriptDefinition ){
				ScriptDefinition script = (ScriptDefinition)definition;
				installDefinition.setString(7, script.getScript());
				installDefinition.setInt(8, DefinitionType.SCRIPT.ordinal());
			}
			else if( definition instanceof PatternDefinition ){
				PatternDefinition threatSig = (PatternDefinition)definition;
				installDefinition.setString(7, threatSig.getRuleCode());
				installDefinition.setInt(8, DefinitionType.PATTERN.ordinal());
			}
			
			installDefinition.executeUpdate();
			
			resultSet = installDefinition.getGeneratedKeys();
			
			if( resultSet.next() ){
				localId = resultSet.getInt(1);
			}
		}
		finally{
			if( installDefinition != null )
				installDefinition.close();
			
			if( resultSet != null )
				resultSet.close();
		}
		
		return localId;
		
	}
	
	/**
	 * Removes all definitions from the list of active definitions as well as from the list contained in the database.
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 */
	public void clear( boolean removeOfficialOnly ) throws SQLException, NoDatabaseConnectionException{
		
		// 0 -- Precondition check
		if( application == null){
			throw new IllegalStateException("The definition set does not have an instance of an application object");
		}
		
		// 1 -- Delete all definitions from the database
		Connection connection = null;
		PreparedStatement deleteStatement = null;
		
		synchronized (archive) {
			try{
				connection = application.getDatabaseConnection(DatabaseAccessType.SCANNER);
				if( removeOfficialOnly ){
					deleteStatement = connection.prepareStatement("Delete from Definitions where AssignedID > -1 and AssignedID < 1000000");
				}
				else{
					deleteStatement = connection.prepareStatement("Delete from Definitions");
				}
		
				deleteStatement.execute();
				
				// 2 -- Remove the definition from the active definition set
				if( removeOfficialOnly ){
					
					Iterator<Definition> i = definitionSet.iterator();
					
				    while (i.hasNext()) {
				    	Definition definition = i.next();
				      if( definition.getID() > -1 ){
				    	  i.remove();
				      }
				    }
				}
				else{
					definitionSet.clear();
				}
			}
			finally{
				if( connection != null ){
					connection.close();
				}
				
				if( deleteStatement != null ){
					deleteStatement.close();
				}
			}
		}
	}
	
	/**
	 * Causes the existing definitions to be overwritten with the ones in the list.
	 * @param definitionSet
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws InputValidationException 
	 * @throws DefinitionSetLoadException 
	 * @throws DefinitionArchiveException 
	 */
	public void updateDefinitions( DefinitionSet definitionSet, boolean replaceOfficialOnly ) throws DefinitionArchiveException{
		
		try{
			// 0 -- Precondition check
			if( definitionSet == null ){
				throw new IllegalArgumentException("The definition set to replace the current one cannot be null");
			}
			
			synchronized (archive) {
				// 1 -- Set a field that indicates that the definition update is in progress (so the application will re-download the definitions if it crashes during the installation)
				application.getApplicationParameters().setParameter("_DefinitionUpdateInProgress", "true");
				
				// 2 -- Set the new definitions in memory
				SimpleDateFormat dateFormat = new SimpleDateFormat(DefinitionSet.DEFINITION_SET_DATE_FORMAT);
				
				if( definitionSet.getDefinitionSetDate() != null ){
					application.getApplicationParameters().setParameter("_DefinitionDate", dateFormat.format( definitionSet.getDefinitionSetDate() ) );
				}
				else{
					throw new DefinitionArchiveException("Definition set date is invalid");
				}
				
				if( definitionSet.getVersionID() != null ){
					application.getApplicationParameters().setParameter("_DefinitionVersion", definitionSet.getVersionID().toString() ) ;
				}
				else{
					throw new DefinitionArchiveException("Definition set version identifier is invalid");
				}
				
				// Throw an error and stop if the definition set contains nothing
				if( definitionSet.size() == 0 ){
					throw new DefinitionArchiveException("Downloaded definition set is invalid (contains no definitions)");
				}
				
				// 3 -- Remove the old definitions from the database
				clear(replaceOfficialOnly);
				
				// 4 -- Synchronize the database to the definitions in memory
				this.saveToDatabase( definitionSet );
				
				// 6 -- Note that the definitions in memory have been successfully synchronized to the ones on disk
				application.getApplicationParameters().setParameter("_DefinitionUpdateInProgress", "false");
				
				// 5 -- Load the definitions back into memory (completely reload them all)
				this.definitionSet = loadDefinitionSet(application);
			}
		}
		catch(SQLException e){
			throw new DefinitionArchiveException("Failed to update of definitions", e);
		}
		catch(NoDatabaseConnectionException e){
			throw new DefinitionArchiveException("Failed to update of definitions", e);
		}
		catch(InputValidationException e){
			throw new DefinitionArchiveException("Failed to update of definitions", e);
		}
		catch(DefinitionSetLoadException e){
			throw new DefinitionArchiveException("Failed to update of definitions", e);
		}
		
	}
	
	public Vector<DefinitionMatch> evaluate( HttpResponseData response, boolean recurseLinkedUrls) throws ScriptException, NoDatabaseConnectionException, SQLException, NoSuchMethodException, InvalidDefinitionException{
		return definitionSet.scan(response);
	}
	
	/**
	 * Causes the existing definitions to be overwritten with the ones in the list.
	 * @param definitionSet
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws DefinitionSetLoadException 
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws InputValidationException 
	 */
	public void updateDefinitions( String xmlString, boolean replaceOfficialOnly ) throws DefinitionArchiveException{
		try{
			DefinitionSet definitionSet = DefinitionSet.loadFromString(xmlString);
			
			updateDefinitions( definitionSet, replaceOfficialOnly );
		}
		catch(DefinitionSetLoadException e){
			throw new DefinitionArchiveException("Failed to update the definitions (definitions could not be parsed and loaded)", e);
		}
		catch(SAXException e){
			throw new DefinitionArchiveException("Failed to update the definitions (definitions could not be parsed and loaded)", e);
		}
		catch(IOException e){
			throw new DefinitionArchiveException("Failed to update the definitions (definitions could not be parsed and loaded)", e);
		}
		catch(ParserConfigurationException e){
			throw new DefinitionArchiveException("Failed to update the definitions (definitions could not be parsed and loaded)", e);
		}
	}
}
