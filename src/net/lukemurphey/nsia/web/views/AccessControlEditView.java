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
import net.lukemurphey.nsia.GroupManagement;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.ObjectPermissionDescriptor;
import net.lukemurphey.nsia.UserManagement;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;
import net.lukemurphey.nsia.web.forms.FieldError;
import net.lukemurphey.nsia.web.forms.FieldErrors;
import net.lukemurphey.nsia.web.templates.TemplateLoader;

public class AccessControlEditView extends View {

	private static final int VALUE_UNDEFINED = -1;
	private static final int VALUE_INVALID = -2;
	public static final String VIEW_NAME = "access_control_editor";
	
	public AccessControlEditView() {
		super("AccessControl", VIEW_NAME, Pattern.compile("[0-9]+"), Pattern.compile("New|Edit", Pattern.CASE_INSENSITIVE), Pattern.compile("(User|Group)?", Pattern.CASE_INSENSITIVE), Pattern.compile("[0-9]*"));
	}
	
	private class ACLDescriptor{
        public AccessControlDescriptor.Action read = AccessControlDescriptor.Action.UNSPECIFIED;
        public AccessControlDescriptor.Action write = AccessControlDescriptor.Action.UNSPECIFIED;
        public AccessControlDescriptor.Action delete = AccessControlDescriptor.Action.UNSPECIFIED;
        public AccessControlDescriptor.Action control = AccessControlDescriptor.Action.UNSPECIFIED;
        public AccessControlDescriptor.Action execute = AccessControlDescriptor.Action.UNSPECIFIED;
        public AccessControlDescriptor.Action create = AccessControlDescriptor.Action.UNSPECIFIED;
        
        public ACLDescriptor(ObjectPermissionDescriptor objectPermissionDescriptor, HttpServletRequest request){
        	
            if( objectPermissionDescriptor != null){
                read = objectPermissionDescriptor.getReadPermission();
                write = objectPermissionDescriptor.getModifyPermission();
                delete = objectPermissionDescriptor.getDeletePermission();
                control = objectPermissionDescriptor.getControlPermission();
                create = objectPermissionDescriptor.getCreatePermission();
                execute = objectPermissionDescriptor.getExecutePermission();
            }
            
            read = getACL(read, request.getParameter("OperationRead"));
            write = getACL(write, request.getParameter("OperationWrite"));
            delete = getACL(delete, request.getParameter("OperationDelete"));
            control = getACL(control, request.getParameter("OperationControl"));
            create = getACL(create, request.getParameter("OperationCreate"));
            execute = getACL(execute, request.getParameter("OperationExecute"));
        }
        
        private AccessControlDescriptor.Action getACL( AccessControlDescriptor.Action aclValue, String argument ){

    		AccessControlDescriptor.Action aclType = aclValue;
    		
    		if( argument != null ){
    			if( argument.equals("Allow"))
    				aclType = AccessControlDescriptor.Action.PERMIT;
    			else if( argument.equals("Deny"))
    				aclType = AccessControlDescriptor.Action.DENY;
    			else if( argument.equals("Undefined"))
    				aclType = AccessControlDescriptor.Action.UNSPECIFIED;
    		}
    		
    		return aclType;
        }
	}
	
	/**
	 * Convert the string description of a permission to a Java AccessControlDescriptor.Action instance.
	 * @param permissionDescription
	 * @return
	 */
	private AccessControlDescriptor.Action convertPermissionFromString(String permissionDescription){
		
		if( permissionDescription == null )
			return AccessControlDescriptor.Action.UNSPECIFIED;
		
		if( permissionDescription.equalsIgnoreCase("Allow"))
			return AccessControlDescriptor.Action.PERMIT;
		else if( permissionDescription.equalsIgnoreCase("Deny"))
			return AccessControlDescriptor.Action.DENY;
		else //if( permissionDescription.equalsIgnoreCase("Undefined"))
			return AccessControlDescriptor.Action.UNSPECIFIED;
			
	}
	
	private void processChange( AccessControl accessControl, ObjectPermissionDescriptor objectPermissionDesc, RequestContext context, HttpServletResponse response, long objectID ) throws IOException, URLInvalidException, NoDatabaseConnectionException, SQLException{
		
		long id = accessControl.setPermissions(objectPermissionDesc);
		
		if(id > 0 ){
			Application.getApplication().logEvent(EventLogMessage.Category.ACCESS_CONTROL_ENTRY_SET, new EventLogField( FieldName.OBJECT_ID,  id ) );
		}
		else{
			Application.getApplication().logEvent(EventLogMessage.Category.ACCESS_CONTROL_ENTRY_SET_FAILED );
		}
		
		context.addMessage("Access control list entry successfully updated", MessageSeverity.SUCCESS);
		response.sendRedirect( AccessControlView.getURL(objectID) );
	}
	
	/**
	 * Process changes to an existing permission descriptor or create a new one. 
	 * @param request
	 * @param response
	 * @param context
	 * @param args
	 * @param data
	 * @return
	 * @throws SQLException 
	 * @throws NoDatabaseConnectionException 
	 * @throws URLInvalidException 
	 * @throws IOException 
	 */
	private boolean processChanges(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data, long objectID) throws NoDatabaseConnectionException, SQLException, IOException, URLInvalidException{
		
		String subject = request.getParameter("Subject");
		
		if( subject == null ){
			FieldErrors errors = new FieldErrors();
			errors.put(new FieldError("Subject", "", "A user or group was not selected") );
			data.put("form_errors", errors);
			return false;
		}
		
		AccessControl accessControl = new AccessControl(Application.getApplication());
		
		// 1 -- Perform the changes if all of the necessary variables were provided 
		if( request.getParameter("OperationRead") != null && request.getParameter("OperationModify") != null 
				&& request.getParameter("OperationControl") != null && request.getParameter("OperationExecute") != null 
				&& request.getParameter("OperationCreate") != null && request.getParameter("OperationDelete") != null ){
			
			// 1.1 -- Populate the permissions defined
			AccessControlDescriptor.Action read = convertPermissionFromString(request.getParameter("OperationRead"));
			AccessControlDescriptor.Action modify = convertPermissionFromString(request.getParameter("OperationModify"));
			AccessControlDescriptor.Action control = convertPermissionFromString(request.getParameter("OperationControl"));
			AccessControlDescriptor.Action execute = convertPermissionFromString(request.getParameter("OperationExecute"));
			AccessControlDescriptor.Action create = convertPermissionFromString(request.getParameter("OperationCreate"));
			AccessControlDescriptor.Action delete = convertPermissionFromString(request.getParameter("OperationDelete"));
			
			// 1.2 -- Determine if the subject is a group or a user
			try{
				if( subject.startsWith("group") ){
					int groupId;
					groupId = Integer.parseInt(subject.substring(5));
					ObjectPermissionDescriptor objectPermissionDesc = new ObjectPermissionDescriptor(read, modify, create, execute, delete, control, AccessControlDescriptor.Subject.GROUP, groupId, objectID );
					
					processChange(accessControl, objectPermissionDesc, context, response, objectID);
					return true;
				}else if( subject.startsWith("user") ){
					int userId;
					userId = Integer.parseInt(subject.substring(4));
					
					ObjectPermissionDescriptor objectPermissionDesc = new ObjectPermissionDescriptor(read, modify, create, execute, delete, control, AccessControlDescriptor.Subject.USER, userId, objectID );
					processChange(accessControl, objectPermissionDesc, context, response, objectID);
					return true;
				}
				else{
					context.addMessage("Access control list entry successfully updated", MessageSeverity.SUCCESS);
					response.sendRedirect( AccessControlView.getURL(objectID) );
					return true;
				}
			}
			catch(NumberFormatException e){
				FieldErrors errors = new FieldErrors();
				errors.put(new FieldError("Subject", "", "A user or group was not selected") );
				data.put("form_errors", errors);
				return false;
			}
		}
		
		return false;
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {
		try{
			// 0 -- Check permissions
			//TODO Check rights
			
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
	        
	        //	 1.2 -- Process the cancel request (if provided)
			if( request.getParameter("Cancel") != null ){
				response.sendRedirect( AccessControlView.getURL(objectId) );
				return true;
			}
			
			//	 1.3 -- Process changes (if requesting)
			else if( "POST".equalsIgnoreCase( request.getMethod() ) ){
				if( processChanges(request, response, context, args, data, objectId) ){
					return true;
				}
			}
	        
	        //   1.4 -- Get the user or group
	        String subject = null;
	        String subjectType = null;
	        int groupId = VALUE_UNDEFINED;
	        int userId = VALUE_UNDEFINED;
	        ObjectPermissionDescriptor objectPermissionDescriptor = null;
	        boolean isEditing = false;
	        
	        //   1.5 -- Get the existing ACL entry (if editing)
	        if( args.length >= 4 ){
	        	subject = args[3];
	        	subjectType = args[2];
	        }
	        
	        if( subject != null){
	            isEditing = true;
	            try{
	                if( subjectType.equalsIgnoreCase("Group") ){
	                    groupId = Integer.parseInt(subject);
	                    objectPermissionDescriptor = accessControl.getGroupPermissions( groupId, objectId);
	                    data.put("permission", new AccessControlView.PermissionDescriptor(objectPermissionDescriptor) );
	                    data.put("subjectType", "Group");
	                    data.put("subjectID", groupId);
	                }
	                else if( subjectType.equalsIgnoreCase("User") ){
	                    userId = Integer.parseInt(subject);
	                    objectPermissionDescriptor = accessControl.getUserPermissions( userId, objectId, false);
	                    data.put("permission", new AccessControlView.PermissionDescriptor(objectPermissionDescriptor) );
	                    data.put("subjectType", "User");
	                    data.put("subjectID", userId);
	                }
	            }
	            catch(NumberFormatException e){
	                //throw new InvalidHtmlParameterException("Invalid Parameter", "The user or group identifier is invalid", "Console" );
	            }
	        }
	        
	        //  1.6 -- Post an error if the arguments are invalid
	        if( objectId < 0){
	            //throw new InvalidHtmlParameterException("Invalid Parameter", "The object identifier is invalid", "Console" );
	        }
	        
	        // 2 -- Get the users and groups available
	        GroupManagement groupMgmt = new GroupManagement(Application.getApplication());
	        data.put("groups", groupMgmt.getGroupDescriptors());
	        
	        UserManagement userMgmt = new UserManagement(Application.getApplication());
	        data.put("users", userMgmt.getUserDescriptors());
			
	        // 3 -- Render the page
	        data.put("title", "Access Control");
	        data.put("isEditing", isEditing);
	        data.put("objectID", objectId);
	        data.put("GROUP", AccessControlDescriptor.Subject.GROUP);
	        data.put("USER", AccessControlDescriptor.Subject.USER);
	        data.put("DENY", AccessControlDescriptor.Action.DENY);
	        data.put("PERMIT", AccessControlDescriptor.Action.PERMIT);
	        data.put("UNSPECIFIED", AccessControlDescriptor.Action.UNSPECIFIED);
	        
	        data.put("VALUE_UNDEFINED", VALUE_UNDEFINED);
	        data.put("VALUE_INVALID", VALUE_INVALID);
	        
	        data.put("userID", userId);
	        data.put("groupID", groupId);
	        
	        ACLDescriptor aclDesc = new ACLDescriptor(objectPermissionDescriptor, request);
	        data.put("read", aclDesc.read);
	        data.put("create", aclDesc.create);
	        data.put("delete", aclDesc.delete);
	        data.put("execute", aclDesc.execute);
	        data.put("write", aclDesc.write);
	        data.put("control", aclDesc.control);
	        
			TemplateLoader.renderToResponse("AccessControlEdit.ftl", data, response);
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
	}

}
