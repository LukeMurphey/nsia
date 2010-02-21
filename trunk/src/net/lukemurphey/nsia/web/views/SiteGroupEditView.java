package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
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
import net.lukemurphey.nsia.web.Link;
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

	/**
	 * Get a form that can validate the site group.
	 * @return
	 */
	private Form getSiteGroupForm( ){
		Form form = new Form();
		
		form.addField( new Field("Name", new PatternValidator(Pattern.compile("[-a-z0-9 _]{1,32}", Pattern.CASE_INSENSITIVE), "Name is not valid")) );
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
							int siteGroupID = siteGroupManager.addGroup(name, description);
							context.addMessage("Sitegroup created successfully", MessageSeverity.SUCCESS);
							response.sendRedirect( SiteGroupView.getURL( siteGroupID ));
							return true;
						}
						else{
							if( siteGroupManager.updateGroupInfo(siteGroup.getGroupId(), name, description) ){
								context.addMessage("Sitegroup updated successfully", MessageSeverity.SUCCESS);
								response.sendRedirect( SiteGroupView.getURL( siteGroup.getGroupId() ));
								return true;
							}
							else{
								context.addMessage("Sitegroup could not be updated", MessageSeverity.WARNING);
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
			// 0 -- Check permissions
			//TODO Check rights
			
			// 1 -- Get the associated site group (if there is one)
			SiteGroupDescriptor siteGroup = null;
			SiteGroupManagement siteGroupManager = new SiteGroupManagement(Application.getApplication());
			
			if( args.length >= 2 ){
				
				int siteGroupID;
				
				try{
					siteGroupID = Integer.valueOf(args[1]);
				}
				catch( NumberFormatException e ){
					Dialog.getDialog(response, context, data, "The SiteGroup ID provided is not valid", "SiteGroup ID Invalid", DialogType.WARNING);
					return true;
				}
				
				try {
					siteGroup = siteGroupManager.getGroupDescriptor(siteGroupID);
					data.put("sitegroup", siteGroup);
				} catch (NotFoundException e) {
					Dialog.getDialog(response, context, data, "No SiteGroup exists with the given ID", "SiteGroup ID Invalid", DialogType.WARNING);
					return true;
				}
			}
			
			// 2 -- Perform any actions requested
			if( performActions(request, response, context, args, data, siteGroup) ){
				return true;
			}
			
			// 3 -- Show the edit/new dialog
			
			//	 3.1 -- Get the breadcrumbs
			Vector<Link> breadcrumbs = new Vector<Link>();
			breadcrumbs.add(  new Link("Main Dashboard", StandardViewList.getURL("main_dashboard")) );
			if( siteGroup == null ){
				breadcrumbs.add(  new Link("New Site Group", createURL("New")) );
				data.put("title", "New Site Group");
			}
			else{
				breadcrumbs.add( new Link("Site Group: " + siteGroup.getGroupName(), StandardViewList.getURL(SiteGroupView.VIEW_NAME, siteGroup.getGroupId() )) );
				breadcrumbs.add( new Link("Edit Site Group", createURL("Edit", siteGroup.getGroupId())) );
				data.put("title", "Edit " + siteGroup.getGroupName());
			}
			data.put("breadcrumbs", breadcrumbs);
			
			//	 3.2 -- Get the menu
			Vector<Link> menu = new Vector<Link>();
			menu.add( new Link("Site Groups") );
			if( siteGroup != null ){
				menu.add( new Link("Add Site Group", createURL("New")) );
				menu.add( new Link("Edit Site Group", createURL("Edit", siteGroup.getGroupId())) );
				menu.add( new Link("Edit ACLs", AccessControlView.getURL(siteGroup.getObjectId()), new Link.Attribute("onclick", "\"w=window.open('" + AccessControlView.getURL(siteGroup.getObjectId()) + "', 'AccessControl', 'height=400,width=780,screenX=' + (screen.availWidth - 700)/2 + ',screenY=' + (screen.availHeight - 300)/2 + ',scrollbars=yes,resizable=yes,toolbar=no');return false\"") ) );
				menu.add( new Link("Delete Site Group", StandardViewList.getURL(SiteGroupDeleteView.VIEW_NAME, siteGroup.getGroupId())) );
				menu.add( new Link("View Scan Policy", StandardViewList.getURL(DefinitionPolicyView.VIEW_NAME, siteGroup.getGroupId() ) ) );
				
				menu.add( new Link("Scan Rules") );
				menu.add( new Link("Scan Now", "ADDURL") );
				menu.add( new Link("Add New Rule", "ADDURL") );
				
				menu.add( new Link("Incident Response") );
				menu.add( new Link("Add New Action", ActionEditView.getURL() + "?SiteGroupID=" + siteGroup.getGroupId() ) );
				menu.add( new Link("List Actions", ActionsListView.getURL(siteGroup.getGroupId()) ) );
			}
			else{
				menu.add( new Link("View Default Scan Policy", StandardViewList.getURL(DefinitionPolicyView.VIEW_NAME) ) ); 
			}
			
			data.put("menu", menu);			
			
			//	 3.3 -- Render the page
			Shortcuts.addDashboardHeaders(request, response, data);
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
