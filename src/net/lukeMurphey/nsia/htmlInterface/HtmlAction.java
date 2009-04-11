package net.lukeMurphey.nsia.htmlInterface;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.InsufficientPermissionException;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.NotFoundException;
import net.lukeMurphey.nsia.SiteGroupManagement.SiteGroupDescriptor;
import net.lukeMurphey.nsia.eventLog.EventLogHook;
import net.lukeMurphey.nsia.eventLog.SiteGroupStatusEventLogHook;
import net.lukeMurphey.nsia.extension.ArgumentFieldsInvalidException;
import net.lukeMurphey.nsia.extension.Extension;
import net.lukeMurphey.nsia.extension.ExtensionType;
import net.lukeMurphey.nsia.extension.FieldLayout;
import net.lukeMurphey.nsia.htmlInterface.Html.MessageType;
import net.lukeMurphey.nsia.responseModule.Action;
import net.lukeMurphey.nsia.trustBoundary.ApiActions;
import net.lukeMurphey.nsia.trustBoundary.ApiExtensions;
import net.lukeMurphey.nsia.trustBoundary.ApiSiteGroupManagement;

public class HtmlAction extends HtmlContentProvider{
	
	public static final int OP_SELECT_ACTION = 101;
	public static final int OP_CONFIGURE_ACTION = 102;
	public static final int OP_LIST_ACTION = 103;
	
	public static final int OP_SELECT_ACTION_RULE = 104;
	public static final int OP_CONFIGURE_ACTION_RULE = 105;
	public static final int OP_LIST_ACTION_RULE = 106;
	
	public static final int OP_SELECT_ACTION_SITEGROUP = 107;
	public static final int OP_CONFIGURE_ACTION_SITEGROUP = 108;
	public static final int OP_LIST_ACTION_SITEGROUP = 109;
	
	public static final int OP_SELECT_ACTION_GLOBAL = 110;
	public static final int OP_CONFIGURE_ACTION_GLOBAL = 111;
	public static final int OP_LIST_ACTION_GLOBAL = 112;
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc  )  throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, InsufficientPermissionException, InvalidHtmlOperationException{
		
		if( actionDesc == null ){
			actionDesc = performAction(requestDescriptor);
		}
		
		if( actionDesc.addData != null && actionDesc.addData instanceof EventLogHook ){
			return getSiteGroupActionEdit(requestDescriptor, actionDesc, (EventLogHook)actionDesc.addData);
		}
		else{
			return getSiteGroupActionEdit(requestDescriptor, actionDesc, null);
		}
		
		
	}
	
	private static ContentDescriptor selectAction( WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		StringBuffer buffer = new StringBuffer();
		
		buffer.append("<div class=\"Text_1\">Select Response Type</div>");
		
		buffer.append("<form method=\"get\">");
		ApiExtensions apiExtensions = new ApiExtensions(Application.getApplication());
		
		Extension[] extensions = apiExtensions.getExtensions( requestDescriptor.sessionIdentifier, ExtensionType.INCIDENT_RESPONSE_MODULE );
		
		for( Extension extension : extensions){
			buffer.append( "<input type=\"radio\" name=\"Extension\" value=\"" + extension.getName() + "\"/>" + extension.getDescription() + "<br>" );
		}
		
		buffer.append("<input type=\"hidden\" name=\"SiteGroupID\" value=\"" + requestDescriptor.request.getParameter("SiteGroupID") + "\">");
		buffer.append("<input type=\"hidden\" name=\"Action\" value=\"New\">");
		buffer.append("&nbsp;<p/><input class=\"button\" value=\"Create\" type=\"submit\">&nbsp;<input class=\"button\" name=\"Cancel\" value=\"Cancel\" type=\"submit\">");
		
		buffer.append("</form>");
		
		return new ContentDescriptor("Select Action", buffer.toString());
		
	}
	
	public enum Scope{
		SITE_GROUP, RULE, GLOBAL
	}
	
	public static ContentDescriptor getActionList( WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc, Scope scope, long ID, Hashtable<String, String> hiddenVariables ){
		
		// 1 -- Turn the list of arguments into a single string
		StringBuffer args = new StringBuffer();
		
		Enumeration<String> names = hiddenVariables.keys();
		
		while( names.hasMoreElements() ){
			String name = names.nextElement();
			args.append(name).append("=");
			args.append( hiddenVariables.get(name) );
		}
		
		
		// 2 -- Get the list of action associated with the given scope
		ApiActions actions = new ApiActions( Application.getApplication() );
		EventLogHook hooks[] = null;
		
		if( scope == Scope.SITE_GROUP ){
			hooks = actions.getSiteGroupEventLogHooks(requestDescriptor.sessionIdentifier, ID);
		}
		else if( scope == Scope.RULE ){
			hooks = actions.getRuleStatusEventLogHooks(requestDescriptor.sessionIdentifier, ID);
		}
		else{
			hooks = actions.getSystemStatusEventLogHooks(requestDescriptor.sessionIdentifier);
		}
		
		StringBuffer buffer = new StringBuffer();
		
		if( hooks.length > 0 ){
			
			buffer.append("<span class=\"Text_1\">");
			buffer.append("Incident Response Actions");
			
			if( scope == Scope.SITE_GROUP ){
				buffer.append("<form method=\"post\" action=\"Response\"><input type=\"hidden\" name=\"SiteGroupID\" value=\"" + ID + "\">");
			}
			
			buffer.append("</span><p/>");
			buffer.append("<table width=\"600px\" class=\"DataTable\" summary=\"List of Response Actions\">");
			buffer.append("<thead><tr><td colspan=\"2\">Type</td><td colspan=\"2\">Description</td></tr></thead><tbody>");
			
			for( int c = 0; c < hooks.length; c++ ){
				buffer.append( HtmlAction.getRow( hooks[c], args.toString() ));
			}
			
			buffer.append("<tr><td colspan=\"99\"><input class=\"button\" name=\"Action\" value=\"Delete\" type=\"submit\"></td></tr></tbody></table></form>");
		}
		else{
			buffer.append( Html.getDialog("No actions exist yet for the given site-group<br><a href=\"Response?SiteGroupID=" + ID + "&Action=New\">[create a new action now]</a>", "No Actions Exist", "/32_Information", false) );
		}
		
		return new ContentDescriptor("Actions List", buffer);
	}
	
	private static ContentDescriptor getSiteGroupActionEdit(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc, EventLogHook hook ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException {
		StringBuffer body = new StringBuffer();
		
		// 1 -- Load the required data values
		
		//	 1.1 -- Get the site group ID
		int siteGroupId = -1;
		
		try{
			if( requestDescriptor.request.getParameter("SiteGroupID") != null ){
				siteGroupId = Integer.parseInt(requestDescriptor.request.getParameter("SiteGroupID"));
			}
			else{
				if( hook instanceof SiteGroupStatusEventLogHook){
					siteGroupId = ((SiteGroupStatusEventLogHook)hook).getSiteGroupID();
				}
			}
		}
		catch( NumberFormatException e ){
			siteGroupId = -2;
		}
		
		//	 1.2 -- Get the action ID (if provided)
		/*long actionId;
		
		try{
			actionId = Long.parseLong(requestDescriptor.request.getParameter("ID"));
		}
		catch( NumberFormatException e ){
			actionId = -2;
		}*/
		
		//	 1.3 -- Stop if the identifier was not provided
		if( siteGroupId < 0 && hook == null){
			throw new InvalidHtmlParameterException("Invalid Site Group Identifier", "The site group identifier given is invalid", "Console");
		}
		
		//	 1.4 -- Print out any messages
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		
		// 2 -- Load the site group
		ApiSiteGroupManagement siteGroupManager = new ApiSiteGroupManagement( Application.getApplication() );
		//ApiActions apiActions = new ApiActions( Application.getApplication() );
		SiteGroupDescriptor siteGroupDesc = null;
		
		try {
			// 2.1 -- Show the form to select an action
			if( actionDesc.result == OP_SELECT_ACTION_SITEGROUP || actionDesc.result == OP_SELECT_ACTION ){
				body.append( HtmlAction.selectAction(requestDescriptor, actionDesc).getBody() );
			}
			
			// 2.2 -- Show the form to edit an existing action or create a new one
			else if( actionDesc.result == OP_CONFIGURE_ACTION || actionDesc.result == OP_CONFIGURE_ACTION_SITEGROUP ){
				Extension extension = null;
				FieldLayout layout;
				
				// 2.2.1 -- If an object was not provided, then show the form to configure a new action and get the layout in order to create the editing form
				if( hook == null ){
					String name = requestDescriptor.request.getParameter("Extension");
					
					ApiExtensions extensions = new ApiExtensions(Application.getApplication());
					extension = extensions.getExtension(requestDescriptor.sessionIdentifier, ExtensionType.INCIDENT_RESPONSE_MODULE, name);
					layout = extension.getFieldLayout();
					
					if( name == null ){
						throw new InvalidHtmlParameterException("Extension Does Not Exist", "An extension cannot have a null name.", "");
					}
				}
				
				// 2.2.2 -- Otherwise, get the layout from the object in order to create the editing form
				else{
					layout = hook.getAction().getLayoutWithValues();
				}
				
				// 2.2.3 -- If the object was not provided, then provide a header indicating that we are creating a new action 
				if( hook == null ){ 
					body.append("<div class=\"Text_2\">Add a New Response</div>Response type: " + extension.getDescription() + "<p/>");
				}
				
				// 2.2.4 -- If an object was provided, then get print a header indicating that we are editing an existing entry
				else{
					body.append("<div class=\"Text_2\">Edit Response Action</div>Response type: " + hook.getAction().getDescription() + "<p/>");
				}
				
				// 2.2.5 -- Show the configuration form
				body.append("<form method=\"post\" action=\"/Response\"><table class=\"DataTable\" summary=\"Actions\">");
				body.append("<tbody>");
				
				body.append( Html.getConfigForm(requestDescriptor, layout, new Hashtable<String, String>() ) );
				body.append("<tr class=\"Background3\"><td class=\"alignRight\" colspan=\"99\">");
				
				if( hook == null ){
					body.append("<input type=\"hidden\" name=\"Action\" value=\"New\">");
					body.append("<input type=\"hidden\" name=\"Extension\" value=\"" + requestDescriptor.request.getParameter("Extension") + "\">");
					body.append("<input type=\"submit\" class=\"button\" name=\"Create\" value=\"Create\">");
				}
				else{
					body.append("<input type=\"hidden\" name=\"Action\" value=\"Edit\">");
					body.append("<input type=\"hidden\" name=\"ActionID\" value=\"" + hook.getEventLogHookID() + "\">");
					body.append("<input type=\"submit\" class=\"button\" name=\"Edit\" value=\"Edit\">");
					//body.append("<input type=\"hidden\" name=\"SiteGroupID\" value=\"" + siteGroupId + "\">");
				}
				
				body.append("<input type=\"hidden\" name=\"SiteGroupID\" value=\"" + siteGroupId + "\">");
				body.append("<input type=\"submit\" class=\"button\" value=\"Cancel\" name=\"Cancel\"></td></tr>");
				body.append("</tbody></table></form>");
				
			}
			
			// 2.3 -- Show the list of available actions
			else{
				siteGroupDesc = siteGroupManager.getGroupDescriptor( requestDescriptor.sessionIdentifier, siteGroupId );
				Hashtable<String, String> vars = new Hashtable<String, String>();
				vars.put( "SiteGroupID", Long.toString( siteGroupId ) );
				
				body.append( HtmlAction.getActionList(requestDescriptor, actionDesc, HtmlAction.Scope.SITE_GROUP, siteGroupId, vars).getBody() );
			}
			
		} catch (InsufficientPermissionException e) {

			// 2.1b -- Output the dialog 
			body.append("<p>");
			body.append(Html.getWarningDialog("Insufficient Permission", "You do not have permission to view this site group.", "Console", "Return to Main Dashboard"));
			
		} catch (NotFoundException e){
			throw new InvalidHtmlParameterException("Invalid Site Group ID", "No site group was found with the given site group ID", "Console");
		}
		
		
		// 3 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "Site Group", "/SiteGroup?SiteGroupID=" + siteGroupId );
		navPath.addPathEntry( "Response Actions", "/Response?SiteGroupID=" + siteGroupId );
		
		if( hook != null ){
			navPath.addPathEntry( "Edit Response Action", "/Response?SiteGroupID=" + siteGroupId + "&Action=Edit&ActionID=" + hook.getEventLogHookID() );
		}
		else if(actionDesc.result == OP_CONFIGURE_ACTION || actionDesc.result == OP_CONFIGURE_ACTION_SITEGROUP || actionDesc.result == OP_SELECT_ACTION_SITEGROUP || actionDesc.result == OP_SELECT_ACTION ){
			navPath.addPathEntry( "New Response Action", "/Response?SiteGroupID=" + siteGroupId + "&Action=New" );
		}
		
		String navigationHtml = Html.getNavigationPath( navPath );
		
		
		// 4 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
				
		menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Edit Group", "/SiteGroup?SiteGroupID=" + requestDescriptor.request.getParameter("SiteGroupID"), MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Delete", "/SiteGroup?Action=Delete&SiteGroupID=" + requestDescriptor.request.getParameter("SiteGroupID"), MenuItem.LEVEL_TWO) );
		if( siteGroupDesc != null)
			menuItems.add( new MenuItem("Edit ACLs", "/AccessControl?ObjectID=" + siteGroupDesc.getObjectId(), MenuItem.LEVEL_TWO, "w=window.open('AccessControl?ObjectID=" + siteGroupDesc.getObjectId() + "', 'AccessControl', 'height=400,width=780,screenX=' + (screen.availWidth - 700)/2 + ',screenY=' + (screen.availHeight - 300)/2 + ',scrollbars=yes,resizable=yes,toolbar=no');return false") );
		menuItems.add( new MenuItem("Exceptions", "/ExceptionManagement?SiteGroupID=" + siteGroupId, MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("Incident Response", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("Add New Action", "/Response?SiteGroupID=" + siteGroupId + "&Action=New", MenuItem.LEVEL_TWO) );
		
		if( actionDesc.result == OP_CONFIGURE_ACTION || actionDesc.result == OP_CONFIGURE_ACTION_SITEGROUP || actionDesc.result == OP_SELECT_ACTION_SITEGROUP || actionDesc.result == OP_SELECT_ACTION  ){
			menuItems.add( new MenuItem("List Actions", "/Response?SiteGroupID=" + siteGroupId, MenuItem.LEVEL_TWO) );
		}
		
		if( hook != null ){
			menuItems.add( new MenuItem("Delete Action", "/Response?SiteGroupID=" + siteGroupId + "&Action=Delete&ActionID=" + hook.getEventLogHookID(), MenuItem.LEVEL_TWO) );
		}
		
		
		
		String menuOutput = Html.getMenu( menuItems );
		
		
		// 5 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor ("New Incident Response Action", pageOutput);
		
	}
	
	private static String getRow( EventLogHook hook, String extraArgs ){
		
		if( hook == null ){
			return "";
		}
		
		StringBuffer buffer = new StringBuffer("<tr><td width=\"2px\"><input name=\"ActionID\" value=\"" + hook.getEventLogHookID() + "\" type=\"checkbox\"/></td><td>");
		
		buffer.append(hook.getAction().getDescription());
		buffer.append("</td><td>");
		
		buffer.append(hook.getAction().getConfigDescription());
		buffer.append("</td><td><table><tr><td><a href=\"/Response?Action=Edit&ActionID=" + hook.getEventLogHookID() + "\"><img class=\"imagebutton\" src=\"/16_Configure\"/></a></td><td><a href=\"/Response?Action=Edit&ActionID=" + hook.getEventLogHookID() + "\">Edit</a></td></tr></table></td></tr>");
		return buffer.toString();
	}
	
	private static ActionDescriptor performAction(WebConsoleConnectionDescriptor requestDescriptor) throws GeneralizedException, NoSessionException, InsufficientPermissionException, InvalidHtmlOperationException{
		
		// 1 -- Load the various arguments
		
		//	 1.1 -- Determine the action
		String action = requestDescriptor.request.getParameter("Action");
		
		//	 1.2 -- Get the associated identifier
		int siteGroupID = -1;
		long scanRuleID = -1;
		int responseActionID = -1;
		
		if( requestDescriptor.request.getParameter("Cancel") != null ){
			return new ActionDescriptor(OP_LIST_ACTION);
		}
		
		//	 	 1.2.1 -- Get the scan rule ID (if available)
		if( requestDescriptor.request.getParameter("RuleID") != null ){
			try{
				scanRuleID = Long.parseLong( requestDescriptor.request.getParameter("RuleID") );
			}
			catch( NumberFormatException e ){
				//Do nothing, the number was the wrong format and will be noted as such
				scanRuleID = -2;
			}
		}
		
		//	 	 1.2.2 -- Get the site group ID (if available)
		if( requestDescriptor.request.getParameter("SiteGroupID") != null){
			try{
				siteGroupID = Integer.parseInt( requestDescriptor.request.getParameter("SiteGroupID") );
			}
			catch( NumberFormatException e ){
				//Do nothing, the number was the wrong format and will be noted as such
				siteGroupID = -2;
			}
		}
		
		//	 	 1.2.3 -- Get the action ID (if available)
		if( requestDescriptor.request.getParameter("ActionID") != null){
			try{
				responseActionID = Integer.parseInt( requestDescriptor.request.getParameter("ActionID") );
			}
			catch( NumberFormatException e ){
				//Do nothing, the number was the wrong format and will be noted as such
				responseActionID = -2;
			}
		}
		
		// 2 -- Perform the appropriate action
		
		//	 2.1 -- Determine the scope of the command (rule, site group, global)
		Scope scope = Scope.GLOBAL;
		if( siteGroupID > 0 ){
			scope = Scope.SITE_GROUP;
		}
		else if ( scanRuleID > 0 ){
			scope = Scope.RULE;
		}
		
		//	 2.2 -- If no action was provided then assume that the action is to list 
		if( action == null && scope == Scope.GLOBAL ){
			return new ActionDescriptor( OP_LIST_ACTION_GLOBAL );
		}
		else if( action == null && scope == Scope.RULE ){
			return new ActionDescriptor( OP_LIST_ACTION_RULE );
		}
		else if( action == null && scope == Scope.SITE_GROUP ){
			return new ActionDescriptor( OP_LIST_ACTION_SITEGROUP );
		}
		
		//	 2.3 -- Load the arguments for the module
		Hashtable<String, String> modulesArguments = new Hashtable<String, String>();
		
		@SuppressWarnings("unchecked")
		Enumeration<String> argsNames = requestDescriptor.request.getParameterNames();
		
		while(argsNames.hasMoreElements()){
			String name = argsNames.nextElement();
			
			if( name.startsWith("_") ){
				modulesArguments.put( name.substring(1), requestDescriptor.request.getParameter(name) );
			}
		}
		
		String selectedActionType = requestDescriptor.request.getParameter("Extension");
		
		//	 2.4 -- If the verb is to create a new action, then makes sure the proper arguments were provided
		if( action.equals("New") ){
			
			// 2.4.1 -- No arguments provided, show the action selection page
			if( modulesArguments.size() == 0 && selectedActionType == null ){
				
				if( scope == Scope.SITE_GROUP ){
					return new ActionDescriptor( OP_SELECT_ACTION_SITEGROUP );
				}
				else if( scope == Scope.RULE ){
					return new ActionDescriptor( OP_SELECT_ACTION_RULE );
				}
				else{
					return new ActionDescriptor( OP_SELECT_ACTION_GLOBAL );
				}
				
			}
			
			// 2.4.2 -- No arguments provided; however, the type of action was specified so show the configuration page
			else if( modulesArguments.size() == 0 && selectedActionType != null ){
				
				//ApiExtensions extensions = new ApiExtensions( Application.getApplication() );
				//Extension extension = extensions.getExtension(requestDescriptor.sessionIdentifier, ExtensionType.INCIDENT_RESPONSE_MODULE, selectedActionType);
				
				if( scope == Scope.SITE_GROUP ){
					return new ActionDescriptor( OP_CONFIGURE_ACTION_SITEGROUP );
				}
				else if( scope == Scope.RULE ){
					return new ActionDescriptor( OP_CONFIGURE_ACTION_RULE );
				}
				else{
					return new ActionDescriptor( OP_CONFIGURE_ACTION_GLOBAL );
				}
				
			}
			
			// 2.4.3 -- Arguments provided, try to create or modify the action (show the configuration page if an error occurs)
			else if( modulesArguments.size() > 0 && selectedActionType != null ){
				
				ApiExtensions extensions = new ApiExtensions( Application.getApplication() );
				Extension extension = extensions.getExtension(requestDescriptor.sessionIdentifier, ExtensionType.INCIDENT_RESPONSE_MODULE, selectedActionType);
				
				if (extension == null){
					
				}
				
				Action responseAction = null;
				try{
					responseAction = (Action)extension.createInstance(modulesArguments);
				}
				catch(ArgumentFieldsInvalidException e){
					//Post a warning
					Html.addMessage(MessageType.WARNING, e.getMessage(), requestDescriptor.userId);
					
					if( scope == Scope.SITE_GROUP ){
						return new ActionDescriptor( OP_CONFIGURE_ACTION_SITEGROUP );
					}
					else if( scope == Scope.RULE ){
						return new ActionDescriptor( OP_CONFIGURE_ACTION_RULE );
					}
					else{
						return new ActionDescriptor( OP_CONFIGURE_ACTION_GLOBAL );
					}
				}
				
				ApiActions apiActions = new ApiActions(Application.getApplication());
				apiActions.addSiteGroupAction(requestDescriptor.sessionIdentifier, responseAction, siteGroupID);
				Html.addMessage(MessageType.INFORMATIONAL, "Action was successfully added", requestDescriptor.userId);
				
				if( scope == Scope.SITE_GROUP ){
					return new ActionDescriptor( OP_LIST_ACTION_SITEGROUP );
				}
				else if( scope == Scope.RULE ){
					return new ActionDescriptor( OP_LIST_ACTION_RULE );
				}
				else{
					return new ActionDescriptor( OP_LIST_ACTION_GLOBAL );
				}
				
			}
			
		}
		
		//	 2.5 -- Edit the action
		else if( action.equals("Edit") ){
			
			//	 2.5.1 -- If no action identifier was provided, then display a warning
			if( responseActionID < 0 ){
				throw new InvalidHtmlOperationException("Identifier Missing","An action identifier was not provided, thus, the action cannot be edited", null);
			}
			
			//	 2.5.2 -- If no arguments were provided, then just display the action
			if( modulesArguments.size() == 0 ){
				ApiActions actions = new ApiActions( Application.getApplication() );
				EventLogHook hook = actions.getEventLogHook(requestDescriptor.sessionIdentifier, responseActionID);
				return new ActionDescriptor(OP_CONFIGURE_ACTION, hook );
			}
			
			//	 2.5.3 -- If arguments were provided, then try to modify the action
			else {
				ApiActions actions = new ApiActions( Application.getApplication() );
				
				try {
					actions.updateEventLogHook(requestDescriptor.sessionIdentifier, responseActionID, modulesArguments);
				} catch (ArgumentFieldsInvalidException e) {
					Html.addMessage(MessageType.WARNING, "Response action was not modified: " + e.getMessage(), requestDescriptor.userId);
					
					EventLogHook hook = actions.getEventLogHook(requestDescriptor.sessionIdentifier, responseActionID);
					
					return new ActionDescriptor(OP_CONFIGURE_ACTION, hook );
				}
		
				return new ActionDescriptor( ActionDescriptor.OP_NO_OPERATION );
			}
			
		}
		
		//	 2.6 -- Delete the action
		else if( action.equals("Delete") ){
			
			String[] actionIDs = requestDescriptor.request.getParameterValues("ActionID");
			
			// 2.6.1 -- Stop if no action identifiers where provided
			if(actionIDs == null || actionIDs.length == 0){
				Html.addMessage(MessageType.WARNING, "No actions selected", requestDescriptor.userId);
				return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION );
			}
			
			// 2.6.2 -- Delete the action(s)
			long[] actionIDsLong = new long[actionIDs.length];
			
			for (int c = 0; c < actionIDs.length; c++) {
				try{
					long actionID = Long.valueOf(actionIDs[c]);
					actionIDsLong[c] = actionID;
				}
				
				// 2.6.2.1 -- Show a warning and stop of one of the identifiers is invalid
				catch(NumberFormatException e){
					Html.addMessage(MessageType.WARNING, "Invalid action ID provided", requestDescriptor.userId);
					return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION );
				}
				
				// 2.6.2.2 -- Delete the action
				ApiActions actions = new ApiActions(Application.getApplication());
				actions.deleteAction(requestDescriptor.sessionIdentifier, actionIDsLong);
			}
			
			// 2.6.2.3 -- Show a message indicating that the action was deleted
			if( actionIDsLong.length == 1 ){
				Html.addMessage(MessageType.INFORMATIONAL, "Response action deleted", requestDescriptor.userId);
			}
			else{
				Html.addMessage(MessageType.INFORMATIONAL, "" + actionIDsLong.length + " response actions deleted", requestDescriptor.userId);
			}
		}
		
		return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION );
		
	}

}
