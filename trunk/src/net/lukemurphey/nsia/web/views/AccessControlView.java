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
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.ObjectPermissionDescriptor;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;

public class AccessControlView extends View {
	
	private static final int VALUE_UNDEFINED = -1;
	private static final int VALUE_INVALID = -2;
	public static final String VIEW_NAME = "access_control_editor";
	
	public AccessControlView() {
		super("AccessControl", VIEW_NAME, Pattern.compile("[0-9]+"));
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
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {
		
		try{
			// 0 -- Check permissions
			
			// 1 -- Get the object and user information
			AccessControl accessControl = new AccessControl(Application.getApplication());
			
	        //	 1.1 -- Get the object ID
	        long objectId = VALUE_UNDEFINED;
	        if( request.getParameter("ObjectID") != null ){
	            try{
	                objectId = Long.parseLong( request.getParameter("ObjectID") );
	            }
	            catch (NumberFormatException e){
	                objectId = VALUE_INVALID;
	            }
	        }
	        
	        //   1.2 -- Get the user or group
	        String subject = request.getParameter("Subject");
	        int groupId = VALUE_UNDEFINED;
	        int userId = VALUE_UNDEFINED;
	        ObjectPermissionDescriptor objectPermissionDescriptor = null;
	        boolean isEditing = false;
	        
	        //   1.3 -- Get the existing ACL entry (if so requested)
	        if( subject != null){
	            isEditing = true;
	            try{
	                if( subject.startsWith("group") ){
	                    groupId = Integer.parseInt(subject.substring(5));
	                    objectPermissionDescriptor = accessControl.getGroupPermissions( groupId, objectId);
	                    
	                }
	                else if( subject.startsWith("user") ){
	                    userId = Integer.parseInt(subject.substring(4));
	                    objectPermissionDescriptor = accessControl.getUserPermissions( userId, objectId, false);
	                }
	            }
	            catch(NumberFormatException e){
	                //throw new InvalidHtmlParameterException("Invalid Parameter", "The user or group identifier is invalid", "Console" );
	            }
	        }
	        
	        //  1.4 -- Post an error if the arguments are invalid
	        if( objectId < 0){
	            //throw new InvalidHtmlParameterException("Invalid Parameter", "The object identifier is invalid", "Console" );
	        }
			
			// TODO Auto-generated method stub
			return false;
		}
		catch(NoDatabaseConnectionException e){
			throw new ViewFailedException(e);
		}
		catch(SQLException e){
			throw new ViewFailedException(e);
		}
	}

}
