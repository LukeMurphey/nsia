package net.lukemurphey.nsia.web;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.AccessControl;
import net.lukemurphey.nsia.AccessControlDescriptor;
import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.ObjectPermissionDescriptor;
import net.lukemurphey.nsia.RightDescriptor;
import net.lukemurphey.nsia.SessionManagement;
import net.lukemurphey.nsia.SessionStatus;
import net.lukemurphey.nsia.UserManagement;
import net.lukemurphey.nsia.UserManagement.UserDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.web.templates.TemplateLoader;
import net.lukemurphey.nsia.web.views.DashboardDefinitionErrorsPanel;
import net.lukemurphey.nsia.web.views.DashboardLicensePanel;
import net.lukemurphey.nsia.web.views.DashboardRefreshPanel;
import net.lukemurphey.nsia.web.views.DashboardStatusPanel;
import net.lukemurphey.nsia.web.views.DashboardTasksPanel;

public class Shortcuts {
	
	protected static final boolean DEFAULT_DENY = true; 

	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param rightName
	 * @param operationTitle
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	public static void checkRight( SessionManagement.SessionInfo sessionInfo, String rightName ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		checkRight( sessionInfo, rightName, null );
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionInfo
	 * @param rightName
	 * @param annotation
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 */
	public static void checkRight( SessionManagement.SessionInfo sessionInfo, String rightName, String annotation ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		checkRight( sessionInfo, rightName, false, null );
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param rightName
	 * @param operationTitle
	 * @param annotation
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	public static void checkRight( SessionManagement.SessionInfo sessionInfo, String rightName, boolean checkSession, String annotation ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		if( checkSession && sessionInfo.getSessionStatus() != SessionStatus.SESSION_ACTIVE ){
			throw new NoSessionException(sessionInfo.getSessionStatus());
		}
		
		if( hasRight(sessionInfo, rightName, annotation) == false ){
			throw new InsufficientPermissionException();
		}
	}
	
	/**
	 * Returns a boolean indicating if the user has the given right.
	 * @param sessionInfo
	 * @param rightName
	 * @return
	 * @throws GeneralizedException
	 */
	public static boolean hasRight( SessionManagement.SessionInfo sessionInfo, String rightName ) throws GeneralizedException{
		return hasRight( sessionInfo, rightName, null );
	}
	
	/**
	 * Returns a boolean indicating if the user has the given right.
	 * @param sessionInfo
	 * @param rightName
	 * @param annotation
	 * @return
	 * @throws GeneralizedException
	 */
	public static boolean hasRight( SessionManagement.SessionInfo sessionInfo, String rightName, String annotation ) throws GeneralizedException{
		Application appRes = Application.getApplication();
		AccessControl accessControl = new AccessControl(appRes);
		UserManagement userManagement = new UserManagement(appRes);
		
		try {	
			
			if( sessionInfo.getSessionStatus() != SessionStatus.SESSION_ACTIVE ){
				return false;
			}
			
			RightDescriptor acl = null;
			
			try{
				acl = accessControl.getUserRight(sessionInfo.getUserId(), rightName, true);
			}
			catch(NotFoundException e){
				acl = null;
			}
			
			UserDescriptor user = userManagement.getUserDescriptor( sessionInfo.getUserId());
			String userName = null;
			
			if( user != null )
				userName = user.getUserName();
			
			EventLogField[] fields;
			
			if( annotation != null ){
				fields = new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation),
						new EventLogField( FieldName.RIGHT, rightName ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID, sessionInfo.getUserId() ) };
			}
			else{
				fields = new EventLogField[] {
						new EventLogField( FieldName.RIGHT, rightName ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID, sessionInfo.getUserId() ) };
			}
			
			//Determine if the permissions are sufficient to allow access
			if( user.isUnrestricted() == true ){
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, fields );
				return true;
			}
			else if( acl == null ){
				if( !DEFAULT_DENY ){
					return false;
				}
				else{
					appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, fields );
					return false;
				}
			}
			else if( acl.getRight() == AccessControlDescriptor.Action.PERMIT ){
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, fields );
				return true;
			}
			else if( acl.getRight() == AccessControlDescriptor.Action.DENY ){
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY, fields );
				return false;
			}
			else{
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, fields );
				return false;
			}
			
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch (NotFoundException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		}
	}
	
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param objectId
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	public static void checkCreate( SessionManagement.SessionInfo sessionInfo, long objectId ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		checkCreate( sessionInfo, objectId, null);
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionInfo
	 * @param objectId
	 * @param annotation
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 */
	public static void checkCreate( SessionManagement.SessionInfo sessionInfo, long objectId, String annotation) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		checkCreate(sessionInfo, objectId, false, annotation);
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param objectId
	 * @param annotation
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	public static void checkCreate( SessionManagement.SessionInfo sessionInfo, long objectId, boolean checkSession, String annotation ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		if( checkSession && sessionInfo.getSessionStatus() != SessionStatus.SESSION_ACTIVE ){
			throw new NoSessionException(sessionInfo.getSessionStatus());
		}
		
		if( canCreate(sessionInfo, objectId, annotation) == false ){
			throw new InsufficientPermissionException();
		}
	}
	
	/**
	 * Returns a boolean indicating if the user has create permissions.
	 * @param sessionInfo
	 * @param objectId
	 * @return
	 * @throws GeneralizedException
	 */
	public static boolean canCreate( SessionManagement.SessionInfo sessionInfo, long objectId ) throws GeneralizedException{
		return canCreate(sessionInfo, objectId, null);
	}
	
	/**
	 * Returns a boolean indicating if the user has create permissions.
	 * @param sessionInfo
	 * @param objectId
	 * @param annotation
	 * @return
	 * @throws GeneralizedException
	 */
	public static boolean canCreate( SessionManagement.SessionInfo sessionInfo, long objectId, String annotation ) throws GeneralizedException{
		Application appRes = Application.getApplication();
		AccessControl accessControl = new AccessControl(appRes);
		UserManagement userManagement = new UserManagement(appRes);
		
		try {
			
			if( sessionInfo.getSessionStatus() != SessionStatus.SESSION_ACTIVE ){
				return false;
			}
			
			ObjectPermissionDescriptor acl = accessControl.getUserPermissions(sessionInfo.getUserId(), objectId, true);
			
			long userId = sessionInfo.getUserId();
			UserDescriptor user = userManagement.getUserDescriptor( sessionInfo.getUserId());
			String userName = null;
			
			if( user != null )
				userName = user.getUserName();
			
			
			//Determine if the permissions are sufficient to allow access
			if( user.isUnrestricted() == true ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Create" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
						new EventLogField( FieldName.TARGET_USER_ID , userId ) } );
				
				return true;
			}
			else if( acl == null )
				if( !DEFAULT_DENY )
					return false;
				else{
					
					appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, new EventLogField[] {
							new EventLogField( FieldName.MESSAGE, annotation ),
							new EventLogField( FieldName.OPERATION, "Create" ),
							new EventLogField( FieldName.OBJECT_ID, objectId ),
							new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
							new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
							new EventLogField( FieldName.TARGET_USER_ID , userId ) } );
					
					return false;
				}
			else if( acl.getCreatePermission() == AccessControlDescriptor.Action.PERMIT ){
								
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Create" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
						new EventLogField( FieldName.TARGET_USER_ID , userId ) } );
				
				return true;
			}
			else if( acl.getCreatePermission() == AccessControlDescriptor.Action.DENY ){
								
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Create" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
						new EventLogField( FieldName.TARGET_USER_ID , userId ) } );
				
				return false;
			}
			else{
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Create" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
						new EventLogField( FieldName.TARGET_USER_ID , userId ) } );
				
				return false;
			}
			
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch( NotFoundException e){
			return false;
		}
	}
	
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param objectId
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	public static void checkExecute( SessionManagement.SessionInfo sessionInfo, long objectId ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		checkExecute( sessionInfo, objectId, null);
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionInfo
	 * @param objectId
	 * @param annotation
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 */
	public static void checkExecute( SessionManagement.SessionInfo sessionInfo, long objectId, String annotation) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		checkModify(sessionInfo, objectId, false, annotation);
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param objectId
	 * @param annotation
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	public static void checkExecute( SessionManagement.SessionInfo sessionInfo, long objectId, boolean checkSession, String annotation) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		if( checkSession && sessionInfo.getSessionStatus() != SessionStatus.SESSION_ACTIVE ){
			throw new NoSessionException(sessionInfo.getSessionStatus());
		}
		
		if( canExecute(sessionInfo, objectId, annotation) == false ){
			throw new InsufficientPermissionException();
		}
	}
	
	/**
	 * Returns a boolean indicating if the user has execute permissions.
	 * @param sessionInfo
	 * @param objectId
	 * @return
	 */
	public static boolean canExecute( SessionManagement.SessionInfo sessionInfo, long objectId){
		return canExecute(sessionInfo, objectId, null);
	}
	
	/**
	 * Returns a boolean indicating if the user has execute permissions.
	 * @param sessionInfo
	 * @param objectId
	 * @param annotation
	 * @return
	 */
	public static boolean canExecute( SessionManagement.SessionInfo sessionInfo, long objectId, String annotation){
		Application appRes = Application.getApplication();
		AccessControl accessControl = new AccessControl(appRes);
		UserManagement userManagement = new UserManagement(appRes);
		
		try {
			
			if( sessionInfo.getSessionStatus() != SessionStatus.SESSION_ACTIVE ){
				return false;
			}
			
			ObjectPermissionDescriptor acl = accessControl.getUserPermissions(sessionInfo.getUserId(), objectId, true);
			
			long userId = sessionInfo.getUserId();
			UserDescriptor user = userManagement.getUserDescriptor( sessionInfo.getUserId());
			String userName = null;
			
			if( user != null )
				userName = user.getUserName();
			
			//Determine if the permissions are sufficient to allow access
			if( user.isUnrestricted() == true ){
		
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Execute" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
						new EventLogField( FieldName.TARGET_USER_ID , userId ) } );

				return true;
			}
			else if( acl == null )
				if( !DEFAULT_DENY )
					return false;
				else{
					
					appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, new EventLogField[] {
							new EventLogField( FieldName.MESSAGE, annotation ),
							new EventLogField( FieldName.OPERATION, "Execute" ),
							new EventLogField( FieldName.OBJECT_ID, objectId ),
							new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
							new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
							new EventLogField( FieldName.TARGET_USER_ID , userId ) } );
					
					return false;
				}
			else if( acl.getExecutePermission() == AccessControlDescriptor.Action.PERMIT ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Execute" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
						new EventLogField( FieldName.TARGET_USER_ID , userId ) } );
				
				return true;
			}
			else if( acl.getExecutePermission() == AccessControlDescriptor.Action.DENY ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Execute" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
						new EventLogField( FieldName.TARGET_USER_ID , userId ) } );
				
				return false;
			}
			else{
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Execute" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
						new EventLogField( FieldName.TARGET_USER_ID , userId ) } );
				
				return false;
			}
			
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			return false;
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			return false;
		} catch( NotFoundException e){
			return false;
		}
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param objectId
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	public static void checkModify( SessionManagement.SessionInfo sessionInfo, long objectId ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		checkModify(sessionInfo, objectId, null);
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionInfo
	 * @param objectId
	 * @param annotation
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 */
	public static void checkModify( SessionManagement.SessionInfo sessionInfo, long objectId, String annotation) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		checkModify(sessionInfo, objectId, false, annotation);
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param objectId
	 * @param annotation
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	public static void checkModify( SessionManagement.SessionInfo sessionInfo, long objectId, boolean checkSession, String annotation) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		if( checkSession && sessionInfo.getSessionStatus() != SessionStatus.SESSION_ACTIVE ){
			throw new NoSessionException(sessionInfo.getSessionStatus());
		}
		
		if( canModify(sessionInfo, objectId, annotation) == false ){
			throw new InsufficientPermissionException();
		}
	}
	
	/**
	 * Returns a boolean indicating if the user has modify permissions.
	 * @param sessionInfo
	 * @param objectId
	 * @return
	 * @throws GeneralizedException
	 */
	public static boolean canModify( SessionManagement.SessionInfo sessionInfo, long objectId) throws GeneralizedException{
		return canModify(sessionInfo, objectId, null);
	}
	
	/**
	 * Returns a boolean indicating if the user has modify permissions.
	 * @param sessionInfo
	 * @param objectId
	 * @param annotation
	 * @return
	 * @throws GeneralizedException
	 */
	public static boolean canModify( SessionManagement.SessionInfo sessionInfo, long objectId, String annotation) throws GeneralizedException{
		Application appRes = Application.getApplication();
		AccessControl accessControl = new AccessControl(appRes);
		UserManagement userManagement = new UserManagement(appRes);
		
		try {		
			
			if( sessionInfo.getSessionStatus() != SessionStatus.SESSION_ACTIVE ){
				return false;
			}
			
			ObjectPermissionDescriptor acl = accessControl.getUserPermissions(sessionInfo.getUserId(), objectId, true);
			
			long userId = sessionInfo.getUserId();
			UserDescriptor user = userManagement.getUserDescriptor( sessionInfo.getUserId());
			String userName = null;
			
			if( user != null )
				userName = user.getUserName();
			
			//Determine if the permissions are sufficient to allow access
			if( user.isUnrestricted() == true ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Modify"),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
						new EventLogField( FieldName.TARGET_USER_ID , userId ) } );

				
				return true;
			}
			else if( acl == null )
				if( !DEFAULT_DENY )
					return false;
				else{
					
					appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, new EventLogField[] {
							new EventLogField( FieldName.MESSAGE, annotation ),
							new EventLogField( FieldName.OPERATION, "Modify"),
							new EventLogField( FieldName.OBJECT_ID, objectId ),
							new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
							new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
							new EventLogField( FieldName.TARGET_USER_ID , userId ) } );
					
					return false;
				}
			else if( acl.getModifyPermission() == AccessControlDescriptor.Action.PERMIT ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Modify"),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
						new EventLogField( FieldName.TARGET_USER_ID , userId ) } );
				
				return true;
			}
			else if( acl.getModifyPermission() == AccessControlDescriptor.Action.DENY ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Modify"),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
						new EventLogField( FieldName.TARGET_USER_ID , userId ) } );
				
				return false;
			}
			else{
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Modify"),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() ),
						new EventLogField( FieldName.TARGET_USER_ID , userId ) } );
				
				return false;
			}
			
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch( NotFoundException e){
			return false;
		}
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param objectId
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	public static void checkControl( SessionManagement.SessionInfo sessionInfo, long objectId) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		checkControl( sessionInfo, objectId, null );
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionInfo
	 * @param objectId
	 * @param annotation
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 */
	public static void checkControl( SessionManagement.SessionInfo sessionInfo, long objectId, String annotation) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		checkControl(sessionInfo, objectId, false, annotation);
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param objectId
	 * @param annotation
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	public static void checkControl( SessionManagement.SessionInfo sessionInfo, long objectId, boolean checkSession, String annotation) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		if( checkSession && sessionInfo.getSessionStatus() != SessionStatus.SESSION_ACTIVE ){
			throw new NoSessionException(sessionInfo.getSessionStatus());
		}
		
		if( canControl(sessionInfo, objectId, annotation) == false ){
			throw new InsufficientPermissionException();
		}
	}
	
	/**
	 * Returns a boolean indicating if the user has control permissions.
	 * @param sessionInfo
	 * @param objectId
	 * @return
	 * @throws GeneralizedException 
	 */
	public static boolean canControl( SessionManagement.SessionInfo sessionInfo, long objectId) throws GeneralizedException{
		return canControl(sessionInfo, objectId, null);
	}
	
	/**
	 * Returns a boolean indicating if the user has control permissions.
	 * @param sessionInfo
	 * @param objectId
	 * @param annotation
	 * @return
	 * @throws GeneralizedException 
	 */
	public static boolean canControl( SessionManagement.SessionInfo sessionInfo, long objectId, String annotation) throws GeneralizedException{
		
		Application appRes = Application.getApplication();
		AccessControl accessControl = new AccessControl(appRes);
		UserManagement userManagement = new UserManagement(appRes);
		
		try {
			
			if( sessionInfo.getSessionStatus() != SessionStatus.SESSION_ACTIVE ){
				return false;
			}
			
			ObjectPermissionDescriptor acl = accessControl.getUserPermissions(sessionInfo.getUserId(), objectId, true);
			
			UserDescriptor user = userManagement.getUserDescriptor( sessionInfo.getUserId());
			String userName = null;
			
			if( user != null )
				userName = user.getUserName();
			
			//Determine if the permissions are sufficient to allow access
			if( user.isUnrestricted() == true ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Control" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
				
				return true;
			}
			else if( acl == null )
				if( !DEFAULT_DENY )
					return false;
				else{
					
					appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, new EventLogField[] {
							new EventLogField( FieldName.MESSAGE, annotation ),
							new EventLogField( FieldName.OPERATION, "Control" ),
							new EventLogField( FieldName.OBJECT_ID, objectId ),
							new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
							new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
					
					return false;
				}
			else if( acl.getControlPermission() == AccessControlDescriptor.Action.PERMIT ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Control" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
				
				return true;
			}
			else if( acl.getControlPermission() == AccessControlDescriptor.Action.DENY ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Control" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
				
				return false;
			}
			else{
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Control" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
				
				return false;
			}
			
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch( NotFoundException e){
			return false;
		}
	}
	
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param objectId
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	public static void checkDelete( SessionManagement.SessionInfo sessionInfo, long objectId) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		checkDelete(sessionInfo, objectId, null);
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionInfo
	 * @param objectId
	 * @param annotation
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 */
	public static void checkDelete( SessionManagement.SessionInfo sessionInfo, long objectId, String annotation) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		checkDelete(sessionInfo, objectId, false, annotation);
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param objectId
	 * @param annotation
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	public static void checkDelete( SessionManagement.SessionInfo sessionInfo, long objectId, boolean checkSession, String annotation) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		if( checkSession && sessionInfo.getSessionStatus() != SessionStatus.SESSION_ACTIVE ){
			throw new NoSessionException(sessionInfo.getSessionStatus());
		}
		
		if( canDelete(sessionInfo, objectId, annotation) == false ){
			throw new InsufficientPermissionException();
		}
	}
	
	/**
	 * Returns a boolean indicating if the user has delete permissions.
	 * @param sessionInfo
	 * @param objectId
	 * @return
	 * @throws GeneralizedException
	 */
	public static boolean canDelete( SessionManagement.SessionInfo sessionInfo, long objectId) throws GeneralizedException{
		return canDelete(sessionInfo, objectId, null);
	}
	
	/**
	 * Returns a boolean indicating if the user has delete permissions.
	 * @param sessionInfo
	 * @param objectId
	 * @param annotation
	 * @return
	 * @throws GeneralizedException
	 */
	public static boolean canDelete( SessionManagement.SessionInfo sessionInfo, long objectId, String annotation) throws GeneralizedException{
		Application appRes = Application.getApplication();
		AccessControl accessControl = new AccessControl(appRes);
		UserManagement userManagement = new UserManagement(appRes);
		
		try {
			
			if( sessionInfo.getSessionStatus() != SessionStatus.SESSION_ACTIVE ){
				return false;
			}
			
			ObjectPermissionDescriptor acl = accessControl.getUserPermissions(sessionInfo.getUserId(), objectId, true);
			
			UserDescriptor user = userManagement.getUserDescriptor( sessionInfo.getUserId());
			String userName = null;
			
			if( user != null )
				userName = user.getUserName();
			
			//Determine if the permissions are sufficient to allow access
			if( user.isUnrestricted() == true ){
	
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Delete" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );

				return true;
			}
			else if( acl == null )
				if( !DEFAULT_DENY )
					return false;
				else{
					
					appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, new EventLogField[] {
							new EventLogField( FieldName.MESSAGE, annotation ),
							new EventLogField( FieldName.OPERATION, "Delete" ),
							new EventLogField( FieldName.OBJECT_ID, objectId ),
							new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
							new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
					
					return false;
				}
			else if( acl.getDeletePermission() == AccessControlDescriptor.Action.PERMIT ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Delete" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
				
				return true;
			}
			else if( acl.getDeletePermission() == AccessControlDescriptor.Action.DENY ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Delete" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
				
				return false;
			}
			else{
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Delete" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
				
				return false;
			}
			
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch( NotFoundException e){
			return false;
		}
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param objectId
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	public static void checkRead( SessionManagement.SessionInfo sessionInfo, long objectId) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		checkRead( sessionInfo, objectId, null);
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionInfo
	 * @param objectId
	 * @param annotation
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 */
	public static void checkRead( SessionManagement.SessionInfo sessionInfo, long objectId, String annotation) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		checkRead(sessionInfo, objectId, false, annotation);
	}
	
	/**
	 * Determine if the user can perform the given operation.
	 * @param sessionIdentifier
	 * @param objectId
	 * @param annotation
	 * @return
	 * @throws InsufficientPermissionException
	 * @throws GeneralizedException
	 * @throws NoSessionException 
	 */
	public static void checkRead( SessionManagement.SessionInfo sessionInfo, long objectId, boolean checkSession, String annotation) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		if( checkSession && sessionInfo.getSessionStatus() != SessionStatus.SESSION_ACTIVE ){
			throw new NoSessionException(sessionInfo.getSessionStatus());
		}
		
		if( canRead(sessionInfo, objectId, annotation) == false ){
			throw new InsufficientPermissionException();
		}
	}
	
	/**
	 * Returns a boolean indicating if the user has read permissions.
	 * @param sessionInfo
	 * @param objectId
	 * @return
	 * @throws GeneralizedException
	 */
	public static boolean canRead( SessionManagement.SessionInfo sessionInfo, long objectId) throws GeneralizedException{
		return canRead(sessionInfo, objectId, null);
	}
	
	/**
	 * Returns a boolean indicating if the user has read permissions.
	 * @param sessionInfo
	 * @param objectId
	 * @param annotation
	 * @return
	 * @throws GeneralizedException
	 */
	public static boolean canRead( SessionManagement.SessionInfo sessionInfo, long objectId, String annotation) throws GeneralizedException{
		
		Application appRes = Application.getApplication();
		AccessControl accessControl = new AccessControl(appRes);
		UserManagement userManagement = new UserManagement(appRes);
		
		try {
			if( sessionInfo.getSessionStatus() != SessionStatus.SESSION_ACTIVE ){
				return false;
			}
			
			ObjectPermissionDescriptor acl = accessControl.getUserPermissions(sessionInfo.getUserId(), objectId, true);
			
			UserDescriptor user = userManagement.getUserDescriptor( sessionInfo.getUserId());
			String userName = null;
			
			if( user != null )
				userName = user.getUserName();
			
			//Determine if the permissions are sufficient to allow access
			if( user.isUnrestricted() == true ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Read" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
				
				return true;
			}
			else if( acl == null )
				if( !DEFAULT_DENY )
					return false;
				else{
					
					appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, new EventLogField[] {
							new EventLogField( FieldName.MESSAGE, annotation ),
							new EventLogField( FieldName.OPERATION, "Read" ),
							new EventLogField( FieldName.OBJECT_ID, objectId ),
							new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
							new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
					
					return false;
				}
			else if( acl.getReadPermission() == AccessControlDescriptor.Action.PERMIT ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_PERMIT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Read" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
				
				return true;
			}
			else if( acl.getReadPermission() == AccessControlDescriptor.Action.DENY ){
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Read" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
				
				return false;
			}
			else{
				
				appRes.logEvent(EventLogMessage.Category.ACCESS_CONTROL_DENY_DEFAULT, new EventLogField[] {
						new EventLogField( FieldName.MESSAGE, annotation ),
						new EventLogField( FieldName.OPERATION, "Read" ),
						new EventLogField( FieldName.OBJECT_ID, objectId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, userName ),
						new EventLogField( FieldName.SOURCE_USER_ID , sessionInfo.getUserId() )} );
				
				return false;
			}
			
		} catch (SQLException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e );
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			appRes.logExceptionEvent(EventLogMessage.Category.INTERNAL_ERROR, e );
			throw new GeneralizedException();
		} catch( NotFoundException e){
			return false;
		}
	}
	
	public static void addDashboardHeaders( HttpServletRequest request, HttpServletResponse response, Map<String, Object> data) throws ViewFailedException{
		addDashboardHeaders(request, response, data, null);
	}
	
	public static void addDashboardHeaders( HttpServletRequest request, HttpServletResponse response, Map<String, Object> data, String refresh_url) throws ViewFailedException{
		
		Vector<String> panels = new Vector<String>();
		
		// 1 -- Get the status panel
		DashboardStatusPanel status_panel = new DashboardStatusPanel();
		panels.add( status_panel.getPanel(request, data, Application.getApplication() ) );
		
		// 2 -- Get the refresh panel (if requested)
		if( refresh_url != null ){
			DashboardRefreshPanel refresh_panel = new DashboardRefreshPanel();
			panels.add( refresh_panel.getPanel(request, data, refresh_url, response ) );
		}
		
		// 3 -- Get the definition errors panel
		DashboardDefinitionErrorsPanel definitions_panel = new DashboardDefinitionErrorsPanel();
		String definition_errors = definitions_panel.getPanel(request, data, Application.getApplication());
		
		if( definition_errors != null ){
			panels.add( definition_errors );
		}
		
		// 4 -- Add the tasks panel
		DashboardTasksPanel tasks_panel = new DashboardTasksPanel();
		String tasks = tasks_panel.getPanel(request, data, Application.getApplication());
		
		if( tasks != null ){
			panels.add( tasks );
		}
		
		// 5 -- Get the license warning
		DashboardLicensePanel license_panel = new DashboardLicensePanel();
		String license_warning = license_panel.getPanel(request, data, Application.getApplication());
		
		if( license_warning != null ){
			panels.add( license_warning );
		}
		
		// 6 -- Populate the data
		data.put("dashboard_headers", panels);
	}
	
	/**
	 * Gets the complete path used to invoke the given servlet.
	 * @param request
	 * @return
	 */
	public static String getPath( HttpServletRequest request ){
		if( request.getPathInfo() == null ){
			return request.getServletPath();
		}
		else{
			return request.getServletPath() + request.getPathInfo();
		}
	}
	
	/**
	 * This class is used by the template for retrieving information about the request being performed.
	 * @author Luke
	 *
	 */
	public static class RequestTemplateHelper{
		
		private HttpServletRequest request = null;
		
		public RequestTemplateHelper(HttpServletRequest request){
			this.request = request;
		}
		
		public String getParameter(String name){
			return request.getParameter(name);
		}
		
		public String[] getParameters(String name){
			return request.getParameterValues(name);
		}
		
		public String getMethod(){
			return request.getMethod();
		}
		
		public String getThisURL(){
			return getPath(request);
		}
		
	}
	
	/**
	 * Get a map with the basic values that are necessary for every page.
	 * @param context
	 * @return
	 */
	public static Map<String, Object> getMapWithBasics( RequestContext context, HttpServletRequest request ){
		Map<String, Object> data = new HashMap<String, Object>();
		
		data.put("version", Application.getVersion());
		data.put("request", new RequestTemplateHelper(request) );
		
		if( context != null && context.getSessionInfo() != null ){
			data.put("session", context.getSessionInfo());
			
			// Add the user options if the user has a session
			if( context.getSessionInfo().getSessionStatus() == SessionStatus.SESSION_ACTIVE ){
				
				data.put("context", context);
				
				Vector<Link> user_options = new Vector<Link>();
				
				try {
					user_options.add( new Link("[Logout]", StandardViewList.getURL("Logout")) );
					user_options.add( new Link("[Change Password]", StandardViewList.getURL("Logout")) ); //TODO replace with real link to change password
				} catch (URLInvalidException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ViewNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				data.put("upperbar_options", user_options);
			}
		}
		
		return data;
	}
	
	
	/**
	 * Removes slashes at the beginning and end of the string.
	 * @param path
	 * @return
	 */
	public String trimSlashes(String path){
		StringBuffer tmp = new StringBuffer(path);
		
		// Remove the leading slash
		if( tmp.charAt(0) == '/' || tmp.charAt(0) == '\\' ){
			tmp.deleteCharAt(0);
		}
		
		// Remove the trailing slash
		if( tmp.charAt(path.length() - 1) == '/' || tmp.charAt(path.length() - 1) == '\\' ){
			tmp.deleteCharAt(0);
		}
		
		return tmp.toString();
	}
	
	public static void getPermissionDeniedDialog( HttpServletResponse response, Map<String, Object> data, String message, boolean showSimple ) throws ViewFailedException{
		getPermissionDeniedDialog(response, data, message, null, showSimple);
	}
	
	public static void getPermissionDeniedDialog( HttpServletResponse response, Map<String, Object> data, String message ) throws ViewFailedException{
		getPermissionDeniedDialog(response, data, message, null);
	}
	
	public static void getPermissionDeniedDialog( HttpServletResponse response, Map<String, Object> data, String message, Link link ) throws ViewFailedException{
		getPermissionDeniedDialog(response, data, message, link, false);
	}
	
	public static void getPermissionDeniedDialog( HttpServletResponse response, Map<String, Object> data, String message, Link link, boolean showSimple ) throws ViewFailedException{
		data.put("permission_denied_message", message);
		data.put("permission_denied_link", link );
		data.put("show_simple", showSimple );
		TemplateLoader.renderToResponse("PermissionDenied.ftl", data, response);
	}
}
