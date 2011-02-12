package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.SiteGroupManagement;
import net.lukemurphey.nsia.SiteGroupManagement.SiteGroupDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.StandardViewList;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class SiteGroupEnableView extends View {

	public static final String VIEW_NAME = "sitegroup_enable";
	
	public SiteGroupEnableView() {
		super("SiteGroup/Enable", VIEW_NAME, Pattern.compile("[0-9]+"));
	}

	public static String getURL( SiteGroupDescriptor siteGroup ) throws URLInvalidException{
		return SiteGroupEnableView.getURL(siteGroup.getGroupId());
	}
	
	public static String getURL( int siteGroupID ) throws URLInvalidException{
		SiteGroupEnableView view = new SiteGroupEnableView();
		return view.createURL(siteGroupID);
	}

	public boolean enableGroup( RequestContext context, int groupId ) throws ViewFailedException, NotFoundException, InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		Application app = Application.getApplication();
		
		try{
			// 0 -- Precondition check
			
			//	 0.1 -- Make sure the user has permission
			SiteGroupDescriptor siteGroup;
			SiteGroupManagement siteGroupManagement = new SiteGroupManagement(Application.getApplication());
			siteGroup = siteGroupManagement.getGroupDescriptor(groupId);
			
			Shortcuts.checkModify(context.getSessionInfo(), siteGroup.getObjectId(), "Enable site-group");
			
			// 1 -- Enable the group
			if( siteGroupManagement.enableGroup( groupId ) ){
				
				app.logEvent(EventLogMessage.EventType.SITE_GROUP_REENABLED,
						new EventLogField( FieldName.SITE_GROUP_ID, groupId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, context.getUser().getUserName() ),
						new EventLogField( FieldName.SOURCE_USER_ID, context.getUser().getUserID() )
						);
				
				return true;
			}
			else{
				
				app.logEvent(EventLogMessage.EventType.SITE_GROUP_ID_INVALID,
						new EventLogField( FieldName.SITE_GROUP_ID, groupId ),
						new EventLogField( FieldName.SOURCE_USER_NAME, context.getUser().getUserName() ),
						new EventLogField( FieldName.SOURCE_USER_ID, context.getUser().getUserID() )
						);
				
				return true;
			}
		}catch (SQLException e){
			app.logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e );
			throw new ViewFailedException(e);
		}catch (NoDatabaseConnectionException e) {
			app.logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e );
			throw new ViewFailedException(e);
		}catch (InputValidationException e) {
			app.logExceptionEvent(EventLogMessage.EventType.INTERNAL_ERROR, e );
			throw new ViewFailedException(e);
		}
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {
		
		// 1 -- Make sure the argument for the site group to enable is provided and is the correct type
		int siteGroupID = -1;
		
		//	 1.1 -- Make sure an ID was provided
		if( args.length <= 0 ){
			//Show a dialog indicating that the site group ID provided was not provided
			Dialog.getDialog(response, context, data, "The Site-group ID was not provided.", "Site-group ID Invalid", DialogType.WARNING);
			return true;
		}
		
		//	 1.2 -- Make sure the ID is the correct format
		else{
			//Show a dialog indicating that the site group ID provided was not invalid
			try{
				siteGroupID = Integer.valueOf(args[0]);
			}
			catch(NumberFormatException e){
				Dialog.getDialog(response, context, data, "The Site-group ID provided is invalid.", "Site-group ID Invalid", DialogType.WARNING);
				return true;
			}
		}
		
		// 2 -- Enable the group
		try {
			enableGroup(context, siteGroupID);
			context.addMessage("Site-group successfully enabled", MessageSeverity.SUCCESS);
		} catch (InsufficientPermissionException e) {
			context.addMessage("You do not have permission to enable this site-group", MessageSeverity.WARNING);
		} catch (GeneralizedException e) {
			throw new ViewFailedException(e);
		} catch (NoSessionException e) {
			throw new ViewFailedException(e);
		} catch (NotFoundException e) {
			Dialog.getDialog(response, context, data, "No Site-group exists with the given identifier", "Site-group Not Found", DialogType.WARNING);
			return true;
		}
		
		
		response.sendRedirect( StandardViewList.getURL(SiteGroupView.VIEW_NAME, siteGroupID) );
		
		return true;
	}

}
