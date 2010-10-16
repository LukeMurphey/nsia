package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.AccessControl;
import net.lukemurphey.nsia.AccessControlDescriptor;
import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.GroupManagement;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.ObjectPermissionDescriptor;
import net.lukemurphey.nsia.UserManagement;
import net.lukemurphey.nsia.AccessControlDescriptor.Action;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.templates.TemplateLoader;

public class AccessControlView extends View {
	
	private static final int VALUE_UNDEFINED = -1;
	private static final int VALUE_INVALID = -2;
	public static final String VIEW_NAME = "access_control";
	
	public AccessControlView() {
		super("AccessControl", VIEW_NAME, Pattern.compile("[0-9]+"));
	}

	public static String getURL( int objectID ) throws URLInvalidException{
		AccessControlView view = new AccessControlView();
		return view.createURL(objectID);
	}
	
	public static String getURL( long objectID ) throws URLInvalidException{
		AccessControlView view = new AccessControlView();
		return view.createURL(objectID);
	}
	
	public static class PermissionDescriptor{
		private ObjectPermissionDescriptor objectDescriptor = null;
		private Object subject;
		
		public PermissionDescriptor( ObjectPermissionDescriptor obj ) throws SQLException, InputValidationException, NoDatabaseConnectionException, NotFoundException{
			
			// 0 -- Precondition check
			if( obj == null ){
				throw new IllegalArgumentException("ObjectPermissionDescriptor cannot be null");
			}
			
			// 1 -- Initialize the class
			this.objectDescriptor = obj;
			
			subject = populateSubject();
		}
		
		public int getSubjectID(){
			return objectDescriptor.getSubjectId();
		}
		
		public long getObjectID(){
			return objectDescriptor.getObjectId();
		}
		
		public AccessControlDescriptor.Subject getSubjectType(){
			return objectDescriptor.getSubjectType();
		}
		
		public boolean isGroup(){
			return objectDescriptor.isGroup();
		}
		
		public boolean isUser(){
			return objectDescriptor.isUser();
		}
		
		public Action getControlPermission(){
			return objectDescriptor.getControlPermission();
		}
		
		public Action getCreatePermission(){
			return objectDescriptor.getCreatePermission();
		}
		
		public Action getDeletePermission(){
			return objectDescriptor.getDeletePermission();
		}
		
		public Action getExecutePermission(){
			return objectDescriptor.getExecutePermission();
		}
		
		public Action getModifyPermission(){
			return objectDescriptor.getModifyPermission();
		}
		
		public Action getReadPermission(){
			return objectDescriptor.getReadPermission();
		}
		
		public Object getSubject(){
			return subject;
		}
		
		private Object populateSubject() throws SQLException, InputValidationException, NoDatabaseConnectionException, NotFoundException{
			int subjectID = objectDescriptor.getSubjectId();
			
			if( objectDescriptor.isGroup() ){
				GroupManagement groupMgmt = new GroupManagement(Application.getApplication());
				return groupMgmt.getGroupDescriptor(subjectID);
			}
			else{
				UserManagement userMgmt = new UserManagement(Application.getApplication());
				return userMgmt.getUserDescriptor(subjectID);
			}
		}
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {
		
		try{
			data.put("title", "Access Control");
			
			// 1 -- Get the object and user information
			AccessControl accessControl = new AccessControl(Application.getApplication());
			
	        //	 1.1 -- Get the object ID
	        long objectId = VALUE_UNDEFINED;
	        if( args.length > 0){
	            try{
	                objectId = Long.parseLong( args[0] );
	            }
	            catch (NumberFormatException e){
	                objectId = VALUE_INVALID;
	            }
	        }
	        
	        //   1.2 -- Get the user or group
	        String subject = request.getParameter("Subject");
	        boolean isEditing = false;
	        
	        //   1.3 -- Get the existing ACL entry (if so requested)
	        
	        if( subject != null){
	            isEditing = true;
	        }
	        
	        //  1.4 -- Post an error if the arguments are invalid
	        if( objectId < 0){
	            //throw new InvalidHtmlParameterException("Invalid Parameter", "The object identifier is invalid", "Console" );
	        }
	        
	        // 2 -- Check permissions
	        if( Shortcuts.canControl(context.getSessionInfo(), objectId, "View access control list") == false ){
	        	data.put("permission_denied_message", "You do not have permission to view the access control list" );
	        	TemplateLoader.renderToResponse("AccessControl.ftl", data, response);
	        	return true;
	        }
	        
	        // 3 -- If just viewing, then get all of the permission descriptors for the object
	        if( isEditing == false ){
	        	ObjectPermissionDescriptor[] descriptors = accessControl.getAllAclEntries(objectId);
	        	PermissionDescriptor[] subject_descriptors = new PermissionDescriptor[descriptors.length];
	        	
	        	for (int c = 0; c < descriptors.length; c++ ) {
					subject_descriptors[c] = new PermissionDescriptor( descriptors[c] );
				}
	        	
	        	data.put("permissions", subject_descriptors);
	        }
			
	        // 4 -- Render the page
	        data.put("objectID", objectId);
	        data.put("GROUP", AccessControlDescriptor.Subject.GROUP);
	        data.put("USER", AccessControlDescriptor.Subject.USER);
	        data.put("DENY", AccessControlDescriptor.Action.DENY);
	        data.put("PERMIT", AccessControlDescriptor.Action.PERMIT);
	        data.put("UNSPECIFIED", AccessControlDescriptor.Action.UNSPECIFIED);
	        
			TemplateLoader.renderToResponse("AccessControl.ftl", data, response);
			return true;
		}
		catch(NoDatabaseConnectionException e){
			throw new ViewFailedException(e);
		}
		catch(SQLException e){
			throw new ViewFailedException(e);
		}
		catch(NotFoundException e){
			throw new ViewFailedException(e);
		}
		catch(InputValidationException e){
			throw new ViewFailedException(e);
		}
		catch(GeneralizedException e){
			throw new ViewFailedException(e);
		}
	}

}
