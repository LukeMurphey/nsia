package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;
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
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.Menu;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.StandardViewList;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;
import net.lukemurphey.nsia.web.forms.Field;
import net.lukemurphey.nsia.web.forms.FieldError;
import net.lukemurphey.nsia.web.forms.FieldErrors;
import net.lukemurphey.nsia.web.forms.Form;
import net.lukemurphey.nsia.web.forms.PatternValidator;
import net.lukemurphey.nsia.web.templates.TemplateLoader;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class SiteGroupEditView extends View {

	public static final String VIEW_NAME = "sitegroup"; 
	
	public SiteGroupEditView() {
		super("SiteGroup", VIEW_NAME, Pattern.compile("(New)|(Edit)", Pattern.CASE_INSENSITIVE), Pattern.compile("[0-9]*", Pattern.CASE_INSENSITIVE));
	}
	
	public static String getURL() throws URLInvalidException{
		SiteGroupEditView view = new SiteGroupEditView();
		return view.createURL("New");
	}
	
	public static String getURL( SiteGroupDescriptor siteGroup ) throws URLInvalidException{
		SiteGroupEditView view = new SiteGroupEditView();
		return view.createURL("Edit", siteGroup.getGroupId());
	}

	/**
	 * Get a form that can validate the site group.
	 * @return
	 */
	private Form getSiteGroupForm( ){
		Form form = new Form();
		
		form.addField( new Field("Name", new PatternValidator(Pattern.compile("[-a-z0-9. _]{1,32}", Pattern.CASE_INSENSITIVE), "Name is not valid (can only contain letters, numbers, spaces, periods and underscores)")) );
		form.addField( new Field("Description") );
		
		return form;
	}
	
	/**
	 * Performs the actions of creating or editing site groups. Returns true if the request was handled by the function; if true is returned,
	 * then the calling function does not need to continue creating a response.
	 * @param request
	 * @param response
	 * @param siteGroup
	 * @return
	 * @throws ViewFailedException 
	 * @throws ViewNotFoundException 
	 * @throws URLInvalidException 
	 * @throws IOException 
	 */
	protected boolean performActions( HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data, SiteGroupDescriptor siteGroup ) throws ViewFailedException, IOException, URLInvalidException, ViewNotFoundException{
		try{
			
			SiteGroupManagement siteGroupManager = new SiteGroupManagement(Application.getApplication());
			
			if( "POST".equalsIgnoreCase( request.getMethod() ) ) {
				String name = request.getParameter("Name");
				String description = request.getParameter("Description");
				Form form = getSiteGroupForm();
				
				// 2.1 -- make sure the name is valid
				FieldErrors errors = form.validate(request);
				
				if( errors.size() > 0 ){
					data.put("form_errors", errors);
				}
				else{
					try{
						if( siteGroup == null ){
							
							// Stop if a site-group already exists with the given name
							if(siteGroupManager.getGroupID(name) > 0 ){
								context.addMessage("A Sitegroup already exists with the given name", MessageSeverity.WARNING);
								return false;
							}
							
							int siteGroupID = siteGroupManager.addGroup(name, description);
							
							if( siteGroupID > -1 ){
								
								Application.getApplication().logEvent(EventLogMessage.EventType.SITE_GROUP_ADDED,
										new EventLogField( FieldName.SOURCE_USER_NAME, context.getUser().getUserName() ),
										new EventLogField( FieldName.SOURCE_USER_ID, context.getUser().getUserID() ),
										new EventLogField( FieldName.SITE_GROUP_NAME, name ),
										new EventLogField( FieldName.SITE_GROUP_ID, siteGroupID ) );
								
								context.addMessage("Sitegroup created successfully", MessageSeverity.SUCCESS);
								
								response.sendRedirect( SiteGroupView.getURL( siteGroupID ));
							}
							else{
								
								Application.getApplication().logEvent(EventLogMessage.EventType.OPERATION_FAILED,
										new EventLogField( FieldName.OPERATION, "Add new site-group" ),
										new EventLogField( FieldName.SOURCE_USER_NAME, context.getUser().getUserName() ),
										new EventLogField( FieldName.SOURCE_USER_ID, context.getUser().getUserID() ),
										new EventLogField( FieldName.SITE_GROUP_NAME, name ) );
								
								context.addMessage("Sitegroup was not created successfully", MessageSeverity.WARNING);
								
								return false;
							}
							
							return true;
						}
						else{
							if( siteGroupManager.updateGroupInfo(siteGroup.getGroupId(), name, description) ){

								Application.getApplication().logEvent(EventLogMessage.EventType.SITE_GROUP_MODIFIED,
										new EventLogField( FieldName.SITE_GROUP_ID, siteGroup.getGroupId() ),
										new EventLogField( FieldName.SITE_GROUP_NAME, siteGroup.getGroupName() ) ,
										new EventLogField( FieldName.SOURCE_USER_NAME, context.getUser().getUserName() ),
										new EventLogField( FieldName.SOURCE_USER_ID, context.getUser().getUserID() )
										);

								
								context.addMessage("Sitegroup updated successfully", MessageSeverity.SUCCESS);
								response.sendRedirect( SiteGroupView.getURL( siteGroup.getGroupId() ));
								return true;
							}
							else{
								
								Application.getApplication().logEvent(EventLogMessage.EventType.OPERATION_FAILED, new EventLogField[] {
										new EventLogField( FieldName.OPERATION, "Update site-group" ),
										new EventLogField( FieldName.SITE_GROUP_ID, siteGroup.getGroupId() ),
										new EventLogField( FieldName.SITE_GROUP_NAME, siteGroup.getGroupName() ) ,
										new EventLogField( FieldName.SOURCE_USER_NAME, context.getUser().getUserName() ),
										new EventLogField( FieldName.SOURCE_USER_ID, context.getUser().getUserID() ) }
										);
								
								context.addMessage("Site-group could not be updated", MessageSeverity.WARNING);
								response.sendRedirect( SiteGroupView.getURL( siteGroup.getGroupId() ));
								return true;
							}
						}
					}
					catch(InputValidationException e){
						if( e.getFieldDescription().equalsIgnoreCase("SiteGroupName") ){
							errors.put(new FieldError("Name", e.getAttemptedValue(), "Name is invalid"));
							data.put("form_errors", errors);
						}
					}
				}
			}
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
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
			
			// 1 -- Get the associated site group (if there is one)
			SiteGroupDescriptor siteGroup = null;
			SiteGroupManagement siteGroupManager = new SiteGroupManagement(Application.getApplication());
			
			if( args.length >= 2 ){
				
				int siteGroupID;
				
				try{
					siteGroupID = Integer.valueOf(args[1]);
				}
				catch( NumberFormatException e ){
					Dialog.getDialog(response, context, data, "The Site-group ID provided is not valid", "Site-group ID Invalid", DialogType.WARNING);
					return true;
				}
				
				try {
					siteGroup = siteGroupManager.getGroupDescriptor(siteGroupID);
					data.put("sitegroup", siteGroup);
				} catch (NotFoundException e) {
					Dialog.getDialog(response, context, data, "No Site-group exists with the given ID", "Site-group ID Invalid", DialogType.WARNING);
					return true;
				}
			}
			
			// 2 -- Setup the views
			
			//	 2.1 -- Get the breadcrumbs
			Vector<Link> breadcrumbs = new Vector<Link>();
			breadcrumbs.add(  new Link("Main Dashboard", StandardViewList.getURL("main_dashboard")) );
			if( siteGroup == null ){
				breadcrumbs.add(  new Link("New Site Group", createURL("New")) );
				data.put("title", "New Site Group");
			}
			else{
				breadcrumbs.add( new Link("Site-group: " + siteGroup.getGroupName(), StandardViewList.getURL(SiteGroupView.VIEW_NAME, siteGroup.getGroupId() )) );
				breadcrumbs.add( new Link("Edit Site-group", createURL("Edit", siteGroup.getGroupId())) );
				data.put("title", "Edit " + siteGroup.getGroupName());
			}
			data.put("breadcrumbs", breadcrumbs);
			
			//	 2.2 -- Get the menu
			data.put("menu", Menu.getSiteGroupMenu(context, siteGroup));			
			
			//	 2.3 -- Add the headers
			Shortcuts.addDashboardHeaders(request, response, data);
			
			// 3 -- Check permissions
			try{
				if( siteGroup == null ){//Creating a new site group...
					Shortcuts.checkRight( context.getSessionInfo(), "SiteGroups.Add", "Create new site-group" );
				}
				else{ //Editing an existing site group
					Shortcuts.checkModify(context.getSessionInfo(), siteGroup.getObjectId(), "Edit site-group " + siteGroup.getGroupId() + " (" + siteGroup.getGroupName() + ")");
				}
			}
			catch( NoSessionException e ){
				throw new ViewFailedException(e);
			} catch (GeneralizedException e) {
				throw new ViewFailedException(e);
			} catch (InsufficientPermissionException e) {
				if( siteGroup == null ){
					data.put("permission_denied_message", "You do not have permission to create new site-groups.");
					TemplateLoader.renderToResponse("PermissionDenied.ftl", data, response);
				}
				else{
					data.put("permission_denied_message", "You do not have permission to edit this site-group.");
					data.put("permission_denied_link", new Link("View Site Group", SiteGroupView.getURL(siteGroup)) );
					TemplateLoader.renderToResponse("PermissionDenied.ftl", data, response);
				}
				
				return true;
			}
			
			// 4 -- Perform any actions requested
			if( performActions(request, response, context, args, data, siteGroup) ){
				return true;
			}
			
			TemplateLoader.renderToResponse("SiteGroupEdit.ftl", data, response);
			
			return true;
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (InputValidationException e) {
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		}
	}

}
