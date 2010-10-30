package net.lukemurphey.nsia.trustBoundary;

import java.sql.SQLException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.DefinitionUpdateWorker;
import net.lukemurphey.nsia.DisallowedOperationException;
import net.lukemurphey.nsia.DuplicateEntryException;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.SessionManagement.SessionInfo;
import net.lukemurphey.nsia.UserManagement.UserDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.eventlog.EventLogMessage.EventType;
import net.lukemurphey.nsia.scan.Definition;
import net.lukemurphey.nsia.scan.DefinitionArchive;
import net.lukemurphey.nsia.scan.DefinitionArchiveException;
import net.lukemurphey.nsia.scan.DefinitionSetLoadException;
import net.lukemurphey.nsia.scan.DefinitionUpdateFailedException;
import net.lukemurphey.nsia.scan.DefinitionSet.DefinitionCategory;
import net.lukemurphey.nsia.scan.DefinitionSet.DefinitionVersionID;

import java.util.Date;


public class ApiDefinitionSet extends ApiHandler {
	
	DefinitionArchive archive = null;
	
	public ApiDefinitionSet(Application appRes) throws GeneralizedException {
		super(appRes);
		
		try{
			archive = DefinitionArchive.getArchive();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		} catch (DefinitionSetLoadException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
		
	}
	
	/**
	 * Loads the defintion set object. Note that the ignoreLoadErrors argument will cause the class to be created even if the defintion set
	 * contains invalid definitions. This is useful in order to allow updates to be performed to overwrite a bad definition set.
	 * @param appRes
	 * @param ignoreLoadErrors
	 * @throws GeneralizedException
	 */
	public ApiDefinitionSet(Application appRes, boolean ignoreLoadErrors) throws GeneralizedException {
		super(appRes);
		
		try{
			archive = DefinitionArchive.getArchive(ignoreLoadErrors);
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		} catch (DefinitionSetLoadException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
		
	}
	

	/**
	 * Retrieves the list of currently active definitions (sorted).
	 * @param sessionIdentifier
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 */
	public Definition[] getSortedDefinitions( String sessionIdentifier ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		// 0 -- Precondition check
		//UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		
		// 1 -- Perform the operation
		return archive.getDefinitionsSorted();

	}
	
	/**
	 * Returns a list of the sub-categories.
	 * @param sessionIdentifier
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 */
	public DefinitionCategory[] getSubCategories( String sessionIdentifier ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		// 0 -- Precondition check
		//UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		
		// 1 -- Perform the operation
		return archive.getDefinitionSet().getListOfSubCategories();
	}
	
	/**
	 * Returns a list of the categories.
	 * @param sessionIdentifier
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 */
	public String[] getCategories( String sessionIdentifier ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		// 0 -- Precondition check
		//UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		
		// 1 -- Perform the operation
		return archive.getDefinitionSet().getListOfCategories();
	}
	
	/**
	 * Get the next available definition ID. This method returns the next ID in the custom range ( >= 1,000,000).
	 * @param sessionIdentifier
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 */
	public int getNextOpenID( String sessionIdentifier ) throws GeneralizedException, NoSessionException{
		return archive.getNextOpenID(false);
	}
	
	/**
	 * Get the next available definition ID. The returnNextOfficial argument indicates whether or not the returned value should be in the official range (< 1,000,000) or the custom range ( >= 1,000,000).
	 * @param sessionIdentifier
	 * @param returnNextOfficial
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 */
	public int getNextOpenID( String sessionIdentifier, boolean returnNextOfficial ) throws GeneralizedException, NoSessionException{
		return archive.getNextOpenID(returnNextOfficial);
	}
	
	/**
	 * Retrieves the list of currently active definitions.
	 * @param sessionIdentifier
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 */
	public Definition[] getDefinitions( String sessionIdentifier ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		// 0 -- Precondition check
		//UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		
		// 1 -- Perform the operation
		return archive.getDefinitions();

	}
	
	public Definition getDefinition( String sessionIdentifier, int definitionID ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, NotFoundException{
		
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		
		// 1 -- Perform the operation
		return archive.getDefinitionByLocalID(definitionID);

	}
	
	public Definition getDefinition( String sessionIdentifier, String definitionName ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, NotFoundException{
		
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		
		// 1 -- Perform the operation
		return archive.getDefinition(definitionName);

	}
	
	public void updateDefinition( String sessionIdentifier, Definition definition, int definitionID ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, DisallowedOperationException{
		
		// 0 -- Precondition check
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		
		// 1 -- Perform the operation
		try {
			archive.updateDefinition(definition, definitionID);
			
			appRes.logEvent(EventLogMessage.EventType.DEFINITION_SET_UPDATED, new EventLogField[]{
					new EventLogField( EventLogField.FieldName.DEFINITION_ID, definitionID ),
					new EventLogField( EventLogField.FieldName.DEFINITION_NAME, definition.getFullName() ),
					new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
					new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, userDescriptor.getUserID() )} );

		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		} catch (DisallowedOperationException e) {
			appRes.logEvent( EventLogMessage.EventType.OPERATION_FAILED, new EventLogField(FieldName.MESSAGE, e.getMessage() ) );
			throw e;
		} catch (Exception e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.OPERATION_FAILED, e );
			throw new GeneralizedException();
		} 
	}
	
	public void addDefinition( String sessionIdentifier, Definition definition ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, DisallowedOperationException, DuplicateEntryException{
		
		// 0 -- Precondition check
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		
		// 1 -- Perform the operation
		try {
			int definitionID = archive.addDefinition(definition);
			
			appRes.logEvent(EventLogMessage.EventType.DEFINITION_ADDED, new EventLogField[]{
					new EventLogField( EventLogField.FieldName.DEFINITION_ID, definitionID ),
					new EventLogField( EventLogField.FieldName.DEFINITION_NAME, definition.getFullName() ),
					new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
					new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, userDescriptor.getUserID() )} );

		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		} catch (DisallowedOperationException e) {
			appRes.logEvent( EventLogMessage.EventType.OPERATION_FAILED, new EventLogField(FieldName.MESSAGE, e.getMessage() ) );
			throw e;
		} catch (DuplicateEntryException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.OPERATION_FAILED, e );
			throw e;
		} catch (Exception e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.OPERATION_FAILED, e );
			throw new GeneralizedException();
		} 
	}
	
	/**
	 * Removes the given definition from the active definition list.
	 * @param sessionIdentifier
	 * @param definitionID
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws DisallowedOperationException 
	 */
	public void removeDefinition( String sessionIdentifier, int definitionID ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, DisallowedOperationException{
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		
		
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		
		// 1 -- Perform the operation
		try {
			archive.removeByID(definitionID);
			
			appRes.logEvent(EventLogMessage.EventType.DEFINITION_DELETED, new EventLogField[]{
					new EventLogField( EventLogField.FieldName.DEFINITION_ID, definitionID ),
					new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
					new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, userDescriptor.getUserID() )} );
			
		} catch (SQLException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new GeneralizedException();
		} catch (DisallowedOperationException e) {
			appRes.logEvent( EventLogMessage.EventType.OPERATION_FAILED, new EventLogField(FieldName.MESSAGE, e.getMessage() ) );
			throw e;
		} catch (Exception e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.OPERATION_FAILED, e );
			throw new GeneralizedException();
		} 
	}
	
	/**
	 * Gets the definition version identifier for the currently available definition set.
	 * @param sessionIdentifier
	 * @return
	 * @throws GeneralizedException
	 */
	public DefinitionVersionID getLatestAvailableDefinitionSetID(String sessionIdentifier) throws GeneralizedException{
		
		try{
			return DefinitionArchive.getLatestAvailableDefinitionSetID();
		}
		catch(Exception e){
			Application.getApplication().getEventLog().logExceptionEvent(new EventLogMessage(EventType.INTERNAL_ERROR), e);
			throw new GeneralizedException(e);
		}
		
	}
	
	/**
	 * Updates the definitions if a new set is available.
	 * @throws DuplicateEntryException 
	 * @throws NoSessionException 
	 * @throws GeneralizedException 
	 * @throws InsufficientPermissionException 
	 * @throws NoSessionException 
	 * @throws GeneralizedException 
	 * @throws InsufficientPermissionException 
	 * @throws DefinitionUpdateFailedException 
	 */
	public void updateDefinitionsAsWorker(String sessionIdentifier ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, DuplicateEntryException{
		updateDefinitionsAsWorker( sessionIdentifier, false );
	}
	
	/**
	 * Updates the definitions if a new set is available.
	 * @throws NoSessionException 
	 * @throws GeneralizedException 
	 * @throws InsufficientPermissionException 
	 * @throws DefinitionUpdateFailedException 
	 */
	public void updateDefinitionsAsWorker(String sessionIdentifier, boolean force ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, DuplicateEntryException{
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		
		// 1 -- Perform the operation
		DefinitionUpdateWorker worker = new DefinitionUpdateWorker( force );
		int userId = -1;
		
		try{
			SessionInfo info = sessionManagement.getSessionInfo(sessionIdentifier);
			if(info != null ){
				userId = info.getUserId();
			}
			else{
				throw new GeneralizedException();//No session information found for the user
			}
		} catch(NoDatabaseConnectionException e){
			appRes.logExceptionEvent(new EventLogMessage(EventType.INTERNAL_ERROR), e);
		} catch(InputValidationException e){
			appRes.logExceptionEvent(new EventLogMessage(EventType.INTERNAL_ERROR), e);
		} catch (SQLException e) {
			appRes.logExceptionEvent(new EventLogMessage(EventType.INTERNAL_ERROR), e);
		}
		
		if( userId > -1 ){
			appRes.addWorkerToQueue(worker, "Definitions Update (unscheduled)", userId);
			new Thread(worker).start();
		}
	}
	
	/**
	 * Determines if a new definition set is available.
	 * @throws NoSessionException 
	 * @throws GeneralizedException 
	 * @throws InsufficientPermissionException 
	 * @throws DefinitionUpdateFailedException 
	 */
	public DefinitionVersionID updateDefinitions(String sessionIdentifier) throws InsufficientPermissionException, GeneralizedException, NoSessionException, DefinitionUpdateFailedException{
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		
		// 1 -- Perform the operation
		return archive.updateDefinitions();
		
		//TODO log error when getting new definitions
	}
	
	/**
	 * Determines if a new definition set is available.
	 */
	public boolean areNewDefinitionsAvailable(String sessionIdentifier){
		return archive.isNewDefinitionSetAvailable();
	}
	
	/**
	 * Retrieves the version identifier for the currently active definition set.
	 * @param sessionIdentifier
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 */
	public DefinitionVersionID getDefinitionSetVersionID( String sessionIdentifier ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		
		// 1 -- Perform the operation
		return archive.getVersionID();

	}
	
	public int getCustomDefinitionsCount( String sessionIdentifier ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		
		// 1 -- Perform the operation
		return archive.getCustomDefinitionsCount();

	}
	
	/**
	 * Retrieves the date that the currently active definition set was last updated with official definitions.
	 * @param sessionIdentifier
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 */
	public Date getDefinitionSetDate( String sessionIdentifier ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		
		// 1 -- Perform the operation
		return archive.getDefinitionSetDate();

	}

	/**
	 * Retrieves the currently active definition set as an XML file.
	 * @param sessionIdentifier
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 */
	public String exportDefinitions( String sessionIdentifier, boolean includeCustomDefinitionsOnly ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		// 0 -- Precondition check
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		
		// 1 -- Perform the operation
		appRes.logEvent(EventLogMessage.EventType.DEFINITIONS_EXPORTED, new EventLogField[]{
				new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
				new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, userDescriptor.getUserID() )} );
		
		return archive.getAsXML( includeCustomDefinitionsOnly );

	}
	
	/**
	 * Retrieves the currently active definition set as an XML file.
	 * @param sessionIdentifier
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 */
	public String exportDefinitions( String sessionIdentifier ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		// 0 -- Precondition check
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "System.Configuration.View");
		
		
		// 1 -- Perform the operation
		appRes.logEvent(EventLogMessage.EventType.DEFINITIONS_EXPORTED, new EventLogField[]{
				new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
				new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, userDescriptor.getUserID() )} );
		
		return archive.getAsXML();

	}
	
	/**
	 * Replace the active definition set with the given XML file.
	 * @param sessionIdentifier
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws DefinitionSetLoadException 
	 */
	public void importDefinitions( String sessionIdentifier, String xmlString, boolean replaceOfficialOnly ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, DefinitionSetLoadException, DefinitionArchiveException{
		
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		UserDescriptor userDescriptor = getUserInfo( sessionIdentifier );
		
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		
		// 1 -- Perform the operation
		try {
			archive.updateDefinitions(xmlString, replaceOfficialOnly);
			
			appRes.logEvent(EventLogMessage.EventType.DEFINITION_SET_UPDATED, new EventLogField[]{
					new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, userDescriptor.getUserName() ),
					new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, userDescriptor.getUserID() ),
					new EventLogField( EventLogField.FieldName.IMPORT_SOURCE, "Local file" )} );
			
		} catch (DefinitionArchiveException e) {
			appRes.logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e );
			throw e;
		} 
	}
}
