package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.SiteGroupManagement;
import net.lukemurphey.nsia.SiteGroupManagement.SiteGroupDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogHook;
import net.lukemurphey.nsia.eventlog.EventLogSeverity;
import net.lukemurphey.nsia.eventlog.SiteGroupStatusEventLogHook;
import net.lukemurphey.nsia.extension.ArgumentFieldsInvalidException;
import net.lukemurphey.nsia.extension.Extension;
import net.lukemurphey.nsia.extension.ExtensionManager;
import net.lukemurphey.nsia.extension.ExtensionType;
import net.lukemurphey.nsia.response.Action;
import net.lukemurphey.nsia.scan.ScanRule;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.StandardViewList;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;
import net.lukemurphey.nsia.web.forms.FieldError;
import net.lukemurphey.nsia.web.forms.FieldErrors;
import net.lukemurphey.nsia.web.templates.TemplateLoader;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class ActionEditView extends View {

	public enum Scope{
		SITE_GROUP, RULE, GLOBAL
	}
	
	public static final String VIEW_NAME = "sitegroup_action_editor";
	
	public ActionEditView() {
		super("Actions/SiteGroup", VIEW_NAME, Pattern.compile("(Edit)|(New)", Pattern.CASE_INSENSITIVE), Pattern.compile("[0-9]*"));
	}
	
	public static String getURL( int siteGroupID ) throws URLInvalidException{
		ActionEditView view = new ActionEditView();
		return view.createURL("Edit", siteGroupID);
	}
	
	public static String getURL( ) throws URLInvalidException{
		ActionEditView view = new ActionEditView();
		return view.createURL("New");
	}
	
	private Hashtable<String, String> getArguments( HttpServletRequest request ){
		Hashtable<String, String> modulesArguments = new Hashtable<String, String>();
		
		@SuppressWarnings("unchecked")
		Enumeration<String> argsNames = request.getParameterNames();
		
		while(argsNames.hasMoreElements()){
			String name = argsNames.nextElement();
			
			if( name.startsWith("_") ){
				modulesArguments.put( name.substring(1), request.getParameter(name) );
			}
		}
		
		return modulesArguments;
	}
	
	private Extension getExtension( String name ){
		ExtensionManager extensionManager = ExtensionManager.getExtensionManager();
		return extensionManager.getExtension(ExtensionType.INCIDENT_RESPONSE_MODULE, name);
	}
	
	private boolean processUpdateToHook( HttpServletRequest request, HttpServletResponse response, RequestContext context, Map<String, Object> data, SiteGroupDescriptor siteGroup, ScanRule rule, EventLogHook hook ) throws IOException, URLInvalidException, ViewFailedException{
		
		Hashtable<String, String> modulesArguments = getArguments( request );

		Action action;
		
		try{
			action = hook.getAction();
			action.configure(modulesArguments);
			
			hook.saveToDatabase();
			response.sendRedirect( ActionsListView.getURL(siteGroup.getGroupId()) );
			context.addMessage("Action successfully updated", MessageSeverity.SUCCESS);
			return true;
		}
		catch(ArgumentFieldsInvalidException e){
			//Add the forms error object
			FieldErrors errors = new FieldErrors();
			errors.put( new FieldError(e.getInvalidField().getName(), "", e.getMessage()) );
			data.put("form_errors", errors);
			return false;
		}
		catch(NoDatabaseConnectionException e){
			throw new ViewFailedException(e);
		}
		catch(SQLException e){
			throw new ViewFailedException(e);
		}
	}
	
	private boolean processNewHook( HttpServletRequest request, HttpServletResponse response, RequestContext context, Map<String, Object> data, SiteGroupDescriptor siteGroup, ScanRule rule, String selectedActionType ) throws IOException, URLInvalidException, ViewFailedException{
		
		Hashtable<String, String> modulesArguments = getArguments( request );
		
		Extension extension = getExtension( selectedActionType );
		Action action;
		
		try{
			action = (Action)extension.createInstance(modulesArguments);
			SiteGroupStatusEventLogHook hook = new SiteGroupStatusEventLogHook(action, siteGroup.getGroupId(), EventLogSeverity.WARNING);
			hook.saveToDatabase();
			Application.getApplication().getEventLog().addHook(hook);
			response.sendRedirect( ActionsListView.getURL(siteGroup.getGroupId()) );
			context.addMessage("Action successfully created", MessageSeverity.SUCCESS);
			return true;
		}
		catch(ArgumentFieldsInvalidException e){
			//Add the forms error object
			FieldErrors errors = new FieldErrors();
			errors.put( new FieldError(e.getInvalidField().getName(), "", e.getMessage()) );
			data.put("form_errors", errors);
			return false;
		}
		catch(NoDatabaseConnectionException e){
			throw new ViewFailedException(e);
		}
		catch(SQLException e){
			throw new ViewFailedException(e);
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
			//TODO Check rights
			
			// 1 -- Get the site group or rule
			
			//	 1.1 -- Get the rule
			EventLogHook hook = null;
			
			if( "Edit".equalsIgnoreCase(args[0]) && args.length >= 2){
				int hookID = Integer.valueOf( args[1] );
				
				//hook = EventLogHook.loadFromDatabase(Application.getApplication().getDatabaseConnection(DatabaseAccessType.ACTION), hookID);
				hook = Application.getApplication().getEventLog().getHook(hookID);
				if( hook != null ){
					data.put("action", hook.getAction());
				}
			}
			
			data.put("hook", hook);
			
			//	 1.2 -- Get the site group (if appropriate)
			SiteGroupDescriptor siteGroup = null;
			SiteGroupStatusEventLogHook siteGroupHook = null;
			
			if( hook != null && hook instanceof SiteGroupStatusEventLogHook ){
				
				siteGroupHook = (SiteGroupStatusEventLogHook)hook;
			
				try{
					SiteGroupManagement siteGroupMgmt = new SiteGroupManagement(Application.getApplication());
					siteGroup = siteGroupMgmt.getGroupDescriptor(siteGroupHook.getSiteGroupID());
				}
				catch( NotFoundException e ){
					Dialog.getDialog(response, context, data, "The Sitegroup associated with the given action could not be found", "SiteGroup Not Found", DialogType.INFORMATION);
					return true;
				}
				
				data.put("siteGroup", siteGroup);
			}
			else if( hook == null && request.getParameter("SiteGroupID") != null ){
				
				try{
					int siteGroupID = Integer.valueOf( request.getParameter("SiteGroupID") );
					SiteGroupManagement siteGroupMgmt = new SiteGroupManagement(Application.getApplication());
					siteGroup = siteGroupMgmt.getGroupDescriptor(siteGroupID);
				}
				catch(NotFoundException e){
					Dialog.getDialog(response, context, data, "The Sitegroup to create the rule for was not specified", "SiteGroup Not Specified", DialogType.INFORMATION);
					return true;
				}
				
				data.put("siteGroup", siteGroup);
			}
			else{
				Dialog.getDialog(response, context, data, "The Sitegroup to create the rule for was not specified", "SiteGroup Not Specified", DialogType.INFORMATION);
				return true;
			}
			
			// 2 -- Add the menu
			Vector<Link> menu = new Vector<Link>();
			menu.add( new Link("Site Groups") );
			menu.add( new Link("Edit", StandardViewList.getURL(SiteGroupEditView.VIEW_NAME, "Edit", siteGroup.getGroupId())) );
			menu.add( new Link("New", StandardViewList.getURL(SiteGroupEditView.VIEW_NAME, "New")) );
			menu.add( new Link("Scan Now", "ADDURL") );
			menu.add( new Link("View Exceptions", "ADDURL") );
	
			data.put("menu", menu);
			
			// 3 -- Get the breadcrumbs
			Vector<Link> breadcrumbs = new Vector<Link>();
			breadcrumbs.add(  new Link("Main Dashboard", MainDashboardView.getURL()) );
			breadcrumbs.add(  new Link("Site Group: " + siteGroup.getGroupName(), SiteGroupView.getURL(siteGroup.getGroupId())) );
			breadcrumbs.add(  new Link("Actions", ActionsListView.getURL( siteGroup.getGroupId() ) ) );
			
			if( "Edit".equalsIgnoreCase(args[0])){
				breadcrumbs.add(  new Link("Edit Action: " + hook.getAction().getDescription(), createURL( "Edit", hook.getEventLogHookID() ) ) );
				data.put("title", "Edit Incident Response Action");
			}
			else{
				breadcrumbs.add(  new Link("New Action", createURL( "New" ) + "?SiteGroupID=" + siteGroup.getGroupId() ) );
				data.put("title", "New Incident Response Action");
			}
			
			data.put("breadcrumbs", breadcrumbs);
			Shortcuts.addDashboardHeaders(request, response, data);
			
			// 4 -- Get the extension type
			String actionType = request.getParameter("Extension");
			
			ExtensionManager extensionManager = ExtensionManager.getExtensionManager();
			data.put("extension", extensionManager.getExtension(ExtensionType.INCIDENT_RESPONSE_MODULE, actionType) );
			
			// 5 -- Render the appropriate form
			
			//	 5.1 -- Render the form for selecting the action type
			if( hook == null && actionType == null ){
				//Show the form to select the action type
				data.put("extensions", ExtensionManager.getExtensionManager().getExtensions(ExtensionType.INCIDENT_RESPONSE_MODULE));
				TemplateLoader.renderToResponse("ResponseActionSelectType.ftl", data, response);
				return true;
			}
			
			//	 5.2 -- Render the form for editing the selected action type
			else if( "New".equalsIgnoreCase(args[0]) && request.getMethod().equalsIgnoreCase("GET") ){
				TemplateLoader.renderToResponse("ResponseActionEdit.ftl", data, response);
				return true;
			}
			
			//	 5.3 -- Try to process the new form
			else if( "New".equalsIgnoreCase(args[0]) && request.getMethod().equalsIgnoreCase("POST") ){
				if( processNewHook(request, response, context, data, siteGroup, null, actionType) ){
					return true;
				}
				
				TemplateLoader.renderToResponse("ResponseActionEdit.ftl", data, response);
				return true;
			}
			else if( "Edit".equalsIgnoreCase(args[0]) && request.getMethod().equalsIgnoreCase("POST") ){
				if( processUpdateToHook(request, response, context, data, siteGroup, null, hook) ){
					return true;
				}
				
				TemplateLoader.renderToResponse("ResponseActionEdit.ftl", data, response);
				return true;
			}
			else if( "Edit".equalsIgnoreCase(args[0]) ){
				TemplateLoader.renderToResponse("ResponseActionEdit.ftl", data, response);
				return true;
			}
			
			return false;
		
		}
		catch(SQLException e){
			throw new ViewFailedException(e);
		}
		catch(NoDatabaseConnectionException e){
			throw new ViewFailedException(e);
		}
		catch(InputValidationException e){
			throw new ViewFailedException(e);
		}
	}

}
