package net.lukemurphey.nsia.trustBoundary;

import java.net.MalformedURLException;
import java.net.URL;

import java.sql.SQLException;
import java.sql.Connection;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.Application.DatabaseAccessType;
import net.lukemurphey.nsia.SiteGroupManagement.SiteGroupDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.scan.Definition;
import net.lukemurphey.nsia.scan.DefinitionPolicyDescriptor;
import net.lukemurphey.nsia.scan.DefinitionPolicyManagement;
import net.lukemurphey.nsia.scan.DefinitionPolicySet;
import net.lukemurphey.nsia.scan.InvalidDefinitionException;
import net.lukemurphey.nsia.scan.DefinitionPolicyDescriptor.DefinitionPolicyAction;

public class ApiDefinitionPolicyManagement extends ApiHandler{

	private DefinitionPolicyManagement exceptionManagement = null;
	
	public ApiDefinitionPolicyManagement(Application appRes) {
		super(appRes);
		exceptionManagement = new DefinitionPolicyManagement(appRes);
	}
	
	public boolean deleteDefinitionPolicyDescriptor( String sessionIdentifier, int exceptionID ) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException{
		
		try{
			try{
				DefinitionPolicyDescriptor definitionPolicy = exceptionManagement.getPolicy(exceptionID);
				
				if( definitionPolicy != null ){
					int siteGroupID = definitionPolicy.getSiteGroupID();
					
					if( siteGroupID >= 0 ){
						SiteGroupDescriptor siteGroupDesc = siteGroupManagement.getGroupDescriptor(siteGroupID);
					
						checkModify(sessionIdentifier, siteGroupDesc.getObjectId(), "Remove policy " + exceptionID + " for site group " + siteGroupDesc.getGroupName() + " (" + siteGroupDesc.getGroupId() + ")");
					}
					else{
						//checkRight(sessionIdentifier, "System.Configuration");
						checkSession(sessionIdentifier);
					}
				}
				else{
					checkSession(sessionIdentifier);
				}
			}
			catch(MalformedURLException e){
				checkSession(sessionIdentifier);
			}
			
			return exceptionManagement.deletePolicy(exceptionID);
		
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
	}
	
	public void addCategoryDescriptor( String sessionIdentifier, int siteGroupID, int ruleID, String definitionCategory, DefinitionPolicyAction action) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException{
		
		Connection connection = null;
		
		try{
			SiteGroupDescriptor siteGroupDesc = siteGroupManagement.getGroupDescriptor(siteGroupID);
			
			checkModify(sessionIdentifier, siteGroupDesc.getObjectId(), "Set " + action.name().toLowerCase() + " policy for " + definitionCategory + ".* for " + siteGroupDesc.getGroupName() + " (" + siteGroupDesc.getGroupId() + ") and rule ID " + ruleID );
			
			DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createCategoryPolicy(siteGroupID, ruleID, definitionCategory, action);
			connection = appRes.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			filter.saveToDatabase(connection);
		
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		} finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public void addCategoryDescriptor( String sessionIdentifier, int siteGroupID, int ruleID, String definitionCategory, URL url, DefinitionPolicyAction action) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException{
		
		Connection connection = null;
		
		try{
			SiteGroupDescriptor siteGroupDesc = siteGroupManagement.getGroupDescriptor(siteGroupID);
			
			checkModify(sessionIdentifier, siteGroupDesc.getObjectId(), "Set " + action.name().toLowerCase() + " policy for " + definitionCategory + ".* for " + siteGroupDesc.getGroupName() + " (" + siteGroupDesc.getGroupId() + ") and URL " + url.toExternalForm() +" and rule ID " + ruleID );
			
			DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createCategoryPolicy(siteGroupID, ruleID, definitionCategory, url, action);
			connection = appRes.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			filter.saveToDatabase(connection);
		
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		} finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public void addCategoryDescriptor( String sessionIdentifier, int siteGroupID, String definitionCategory, URL url, DefinitionPolicyAction action) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException{
		
		Connection connection = null;
		
		try{
			SiteGroupDescriptor siteGroupDesc = siteGroupManagement.getGroupDescriptor(siteGroupID);
			
			checkModify(sessionIdentifier, siteGroupDesc.getObjectId(), "Set " + action.name().toLowerCase() + " policy for " + definitionCategory + ".* for " + siteGroupDesc.getGroupName() + " (" + siteGroupDesc.getGroupId() + ") and URL " + url.toExternalForm() );
			
			DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createCategoryPolicy(siteGroupID, definitionCategory, url, action);
			connection = appRes.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			filter.saveToDatabase(connection);
		
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		} finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public void addCategoryDescriptor( String sessionIdentifier, int siteGroupID, String definitionCategory, DefinitionPolicyAction action) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException{
		
		Connection connection = null;
		
		try{
			SiteGroupDescriptor siteGroupDesc = siteGroupManagement.getGroupDescriptor(siteGroupID);
			
			checkModify(sessionIdentifier, siteGroupDesc.getObjectId(), "Set " + action.name().toLowerCase() + " policy for " + definitionCategory + ".* for " + siteGroupDesc.getGroupName() + " (" + siteGroupDesc.getGroupId() + ")" );
			
			DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createCategoryPolicy(siteGroupID, definitionCategory, action);
			connection = appRes.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			filter.saveToDatabase(connection);
		
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		} finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public void addSubCategoryDescriptor( String sessionIdentifier, String categoryName, String subCategoryName, DefinitionPolicyAction action) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException{
		
		Connection connection = null;
		
		try{
			checkRight(sessionIdentifier, "System.Configuration.View", "Set " + action.name().toLowerCase() + " policy for " + categoryName + "." + subCategoryName + ".* at global level" );
			
			DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createSubCategoryPolicy(categoryName, subCategoryName, action );
			connection = appRes.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			filter.saveToDatabase(connection);
		
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public void addSubCategoryDescriptor( String sessionIdentifier, int siteGroupID, String categoryName, String subCategoryName, DefinitionPolicyAction action) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException{
		
		Connection connection = null;
		
		try{
			SiteGroupDescriptor siteGroupDesc = siteGroupManagement.getGroupDescriptor(siteGroupID);
			
			checkModify(sessionIdentifier, siteGroupDesc.getObjectId(), "Add " + action.name().toLowerCase() + " policy for " + categoryName + "." + subCategoryName + ".* for " + siteGroupDesc.getGroupName() + " (" + siteGroupDesc.getGroupId() + ")" );
			
			DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createSubCategoryPolicy(siteGroupID, categoryName, subCategoryName, action );
			connection = appRes.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			filter.saveToDatabase(connection);
		
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		} finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public void addSubCategoryDescriptor( String sessionIdentifier, int siteGroupID, int ruleID, String categoryName, String subCategoryName, URL url, DefinitionPolicyAction action) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException{
		
		Connection connection = null;
		
		try{
			SiteGroupDescriptor siteGroupDesc = siteGroupManagement.getGroupDescriptor(siteGroupID);
			
			checkModify(sessionIdentifier, siteGroupDesc.getObjectId(), "Add exception for " + categoryName + "." + subCategoryName + ".* for " + siteGroupDesc.getGroupName() + " (" + siteGroupDesc.getGroupId() + ") and URL " + url.toExternalForm() + " and rule ID " + ruleID );
			
			DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createSubCategoryPolicy(siteGroupID, ruleID, categoryName, subCategoryName, url, action );
			connection = appRes.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			filter.saveToDatabase(connection);
		
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		} finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public void addSubCategoryDescriptor( String sessionIdentifier, int siteGroupID, int ruleID, String categoryName, String subCategoryName, DefinitionPolicyAction action) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException{
		
		Connection connection = null;
		
		try{
			SiteGroupDescriptor siteGroupDesc = siteGroupManagement.getGroupDescriptor(siteGroupID);
			
			checkModify(sessionIdentifier, siteGroupDesc.getObjectId(), "Add exception for " + categoryName + "." + subCategoryName + ".* for " + siteGroupDesc.getGroupName() + " (" + siteGroupDesc.getGroupId() + ") and rule ID " + ruleID );
			
			DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createSubCategoryPolicy(siteGroupID, ruleID, categoryName, subCategoryName, action );
			connection = appRes.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			filter.saveToDatabase(connection);
		
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		} finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public void addSubCategoryDescriptor( String sessionIdentifier, int siteGroupID, String categoryName, String subCategoryName, URL url, DefinitionPolicyAction action) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException{
		
		Connection connection = null;
		
		try{
			SiteGroupDescriptor siteGroupDesc = siteGroupManagement.getGroupDescriptor(siteGroupID);
			
			checkModify(sessionIdentifier, siteGroupDesc.getObjectId(), "Add exception for " + categoryName + "." + subCategoryName + ".* for " + siteGroupDesc.getGroupName() + " (" + siteGroupDesc.getGroupId() + ") and URL " + url.toExternalForm() );
			
			DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createSubCategoryPolicy(siteGroupID, categoryName, subCategoryName, url, action );
			connection = appRes.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			filter.saveToDatabase(connection);
		
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		} finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public void addDefinitionNameDescriptor( String sessionIdentifier, int siteGroupID, int ruleID, String rulename, DefinitionPolicyAction action) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException, InvalidDefinitionException{
		
		Connection connection = null;
		
		try{
			SiteGroupDescriptor siteGroupDesc = siteGroupManagement.getGroupDescriptor(siteGroupID);
			
			checkModify(sessionIdentifier, siteGroupDesc.getObjectId(), "Add exception for " + rulename + " for " + siteGroupDesc.getGroupName() + " (" + siteGroupDesc.getGroupId() + ") and " + ruleID );
			
			String[] sigName = Definition.parseName(rulename);
			
			DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createDefinitionPolicy(sigName[0], sigName[1], sigName[2], null, action, ruleID, siteGroupID);
			
			connection = appRes.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			filter.saveToDatabase(connection);
		
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		} finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public void addDefinitionNameDescriptor( String sessionIdentifier, int siteGroupID, String rulename, DefinitionPolicyAction action) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException, InvalidDefinitionException{
		
		Connection connection = null;
		
		try{
			SiteGroupDescriptor siteGroupDesc = siteGroupManagement.getGroupDescriptor(siteGroupID);
			
			checkModify(sessionIdentifier, siteGroupDesc.getObjectId(), "Add exception for " + rulename + " for " + siteGroupDesc.getGroupName() + " (" + siteGroupDesc.getGroupId() + ")" );
			
			String[] sigName = Definition.parseName(rulename);
			
			DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createDefinitionPolicy(siteGroupID, sigName[0], sigName[1], sigName[2], action);
			
			connection = appRes.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			filter.saveToDatabase(connection);
		
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		} finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public void addDefinitionNameDescriptor( String sessionIdentifier, int siteGroupID, int ruleID, String rulename, URL url, DefinitionPolicyAction action) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException, InvalidDefinitionException{
		
		Connection connection = null;
		
		try{
			SiteGroupDescriptor siteGroupDesc = siteGroupManagement.getGroupDescriptor(siteGroupID);
			
			checkModify(sessionIdentifier, siteGroupDesc.getObjectId(), "Add exception for " + rulename + " for site group \"" + siteGroupDesc.getGroupName() + "\" (" + siteGroupDesc.getGroupId() + ") and URL " + url.toExternalForm() + " and rule ID " + ruleID );
			
			String[] sigName = Definition.parseName(rulename);
			
			DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createDefinitionPolicy(siteGroupID, ruleID, sigName[0], sigName[1], sigName[2], url, action);
			
			connection = appRes.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			filter.saveToDatabase(connection);
		
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		} finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public void addSignatureNameDescriptor( String sessionIdentifier, int siteGroupID, String rulename, URL url, DefinitionPolicyAction action) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException, InvalidDefinitionException{
		
		Connection connection = null;
		
		try{
			SiteGroupDescriptor siteGroupDesc = siteGroupManagement.getGroupDescriptor(siteGroupID);
			
			checkModify(sessionIdentifier, siteGroupDesc.getObjectId(), "Add exception for " + rulename + " for site group \"" + siteGroupDesc.getGroupName() + "\" (" + siteGroupDesc.getGroupId() + ") and URL " + url.toExternalForm() );
			
			String[] sigName = Definition.parseName(rulename);
			
			DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createDefinitionPolicy(siteGroupID, sigName[0], sigName[1], sigName[2], url, action);
			
			connection = appRes.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			filter.saveToDatabase(connection);
		
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		} finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public DefinitionPolicySet getPolicySet( String sessionIdentifier, int siteGroupId ) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException{
		Connection connection = null;
		try{
			SiteGroupDescriptor siteGroupDesc = siteGroupManagement.getGroupDescriptor(siteGroupId);
			
			checkRead(sessionIdentifier, siteGroupDesc.getObjectId(), "Get definition policy for site group \"" + siteGroupDesc.getGroupName() + "\" (" + siteGroupId + ")");
			connection = appRes.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			return DefinitionPolicySet.getPolicySetForSiteGroup(connection, siteGroupId);
		
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
		finally{
			if(connection != null ){
				try{
					connection.close();
				}
				catch(SQLException e){
					appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public DefinitionPolicySet getPolicySet( String sessionIdentifier, int siteGroupId, int ruleID ) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException{
		Connection connection = null;
		try{
			SiteGroupDescriptor siteGroupDesc = siteGroupManagement.getGroupDescriptor(siteGroupId);
			
			checkRead(sessionIdentifier, siteGroupDesc.getObjectId(), "Get definition exceptions for site group \"" + siteGroupDesc.getGroupName() + "\" (" + siteGroupId + ") and rule ID " + ruleID);
			connection = appRes.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			return DefinitionPolicySet.getPolicySetForSiteGroup(connection, siteGroupId, ruleID);
		
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
		finally{
			if(connection != null ){
				try{
					connection.close();
				}
				catch(SQLException e){
					appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public DefinitionPolicySet getPolicySet( String sessionIdentifier ) throws GeneralizedException, NoSessionException, InsufficientPermissionException{
		Connection connection = null;
		try{
			
			checkRight(sessionIdentifier, "System.Configuration.View", "Get global scan policy");
			connection = appRes.getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			return DefinitionPolicySet.getPolicySet(connection);
		
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		finally{
			if(connection != null ){
				try{
					connection.close();
				}
				catch(SQLException e){
					appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public int clearCategoryDescriptors( String sessionIdentifier, int siteGroupID, String categoryName) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException{
		
		try{
			SiteGroupDescriptor siteGroupDesc = siteGroupManagement.getGroupDescriptor(siteGroupID);
			
			checkModify(sessionIdentifier, siteGroupDesc.getObjectId(), "Clear policies for " + categoryName + ".* for " + siteGroupDesc.getGroupName() + " (" + siteGroupDesc.getGroupId() + ")" );
			
			DefinitionPolicyManagement defMgr = new DefinitionPolicyManagement( Application.getApplication() );
			return defMgr.clearCategoryDescriptors(siteGroupID, categoryName );
		
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
	}
	
	public int clearSubCategoryDescriptors( String sessionIdentifier, int siteGroupID, String categoryName, String subCategoryName) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException{
		
		try{
			SiteGroupDescriptor siteGroupDesc = siteGroupManagement.getGroupDescriptor(siteGroupID);
			
			checkModify(sessionIdentifier, siteGroupDesc.getObjectId(), "Clear policies for " + categoryName + "." + subCategoryName + ".* for " + siteGroupDesc.getGroupName() + " (" + siteGroupDesc.getGroupId() + ")" );
			
			DefinitionPolicyManagement defMgr = new DefinitionPolicyManagement( Application.getApplication() );
			return defMgr.clearSubCategoryDescriptors(siteGroupID, categoryName, subCategoryName );
		
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} catch (InputValidationException e) {
			appRes.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e);
			throw new GeneralizedException();
		}
	}
}
