package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GroupManagement;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.GroupManagement.GroupDescriptor;
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
import net.lukemurphey.nsia.web.forms.FieldErrors;
import net.lukemurphey.nsia.web.forms.Form;
import net.lukemurphey.nsia.web.forms.PatternValidator;
import net.lukemurphey.nsia.web.templates.TemplateLoader;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class GroupEditView extends View {

	public static final String VIEW_NAME = "group_editor";
	
	public GroupEditView() {
		super("Group", VIEW_NAME, Pattern.compile("(New)|(Edit)", Pattern.CASE_INSENSITIVE), Pattern.compile("[0-9]*"));
	}
	
	public static String getURL( ) throws URLInvalidException{
		GroupEditView view = new GroupEditView();
		
		return view.createURL("New");
	}
	
	public static String getURL( GroupDescriptor group ) throws URLInvalidException{
		GroupEditView view = new GroupEditView();
		
		return view.createURL("Edit", group.getGroupId());
	}
	
	/**
	 * Get a form that can validate the group.
	 * @return
	 */
	private Form getGroupEditForm( ){
		Form form = new Form();
		
		form.addField( new Field("Name", new PatternValidator(Pattern.compile("[-A-Z0-9a-z_ .]{1,32}", Pattern.CASE_INSENSITIVE), "Group name is not valid")) );
		form.addField( new Field("Description") );
		
		return form;
	}
	
	private boolean performActions( HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data, GroupDescriptor group ) throws ViewFailedException, IOException, URLInvalidException, ViewNotFoundException{
		try{
			
			GroupManagement groupManager = new GroupManagement(Application.getApplication());
			
			if( "POST".equalsIgnoreCase( request.getMethod() ) ) {
				
				Form form = getGroupEditForm();
				
				// 1 -- Validate the form
				FieldErrors errors = form.validate(request);
				
				// 2 -- Add the errors to the list (if the form did not validate)
				if( errors.size() > 0 ){
					data.put("form_errors", errors);
				}
				
				// 3 -- Otherwise, update or create the account
				else{
					
					// 3.1 -- Get the field data
					String name = request.getParameter("Name");
					String description = request.getParameter("Description");
					
					try{
						// 3.2 -- Create a new group if one to edit was not provided
						if( group == null ){
							int groupID = groupManager.addGroup(name, description);
							context.addMessage("Group created successfully", MessageSeverity.SUCCESS);
							group = groupManager.getGroupDescriptor(groupID);
							response.sendRedirect( GroupEditView.getURL(group) );
							return true;
						}
						
						// 3.3 -- Edit the existing group
						else{
							if( groupManager.updateGroupInfo(group.getGroupId(), name, description) ){
								context.addMessage("Group updated successfully", MessageSeverity.SUCCESS);
								response.sendRedirect( GroupEditView.getURL(group) );
								return true;
							}
							else{
								context.addMessage("Group could not be updated", MessageSeverity.WARNING);
								response.sendRedirect( GroupEditView.getURL(group) );
								return true;
							}
						}
					}
					catch(InputValidationException e){
						throw new ViewFailedException(e); //This exception should not be possible since the form should have validated the data beforehand
					}
				}
			}
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		} catch (NotFoundException e) {
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

		// 0 -- Check permissions
		//TODO Check rights
		
		// 1 -- Get the group if one exists
		GroupDescriptor group = null;
		
		if( args.length >= 2 ){
			
			// 1.1 -- Get the group ID
			int groupID;
			
			try{
				groupID = Integer.valueOf( args[1] );
			}
			catch( NumberFormatException e ){
				Dialog.getDialog(response, context, data, "The Group ID provided is not valid", "Group ID Invalid", DialogType.WARNING);
				return true;
			}
			
			// 1.2 -- Get the user descriptor
			GroupManagement groupMgmt = new GroupManagement(Application.getApplication());
			
			try{
				group = groupMgmt.getGroupDescriptor(groupID);
			}
			catch(NotFoundException e){
				Dialog.getDialog(response, context, data, "No group was found with the given ID", "Group Not Found", DialogType.WARNING);
				return true;
			} catch (SQLException e) {
				throw new ViewFailedException(e);
			} catch (NoDatabaseConnectionException e) {
				throw new ViewFailedException(e);
			} catch (InputValidationException e) {
				throw new ViewFailedException(e);
			}
			
			if( group == null ){
				Dialog.getDialog(response, context, data, "No group was found with the given ID", "Group Not Found", DialogType.WARNING);
				return true;
			}
			
			data.put("group", group);
		}
		
		// 2 -- Process the data as necessary
		if( performActions(request, response, context, args, data, group) ){
			return true; //Method set a redirect, just let it handle the result
		}
		
		
		// 3 -- Get the menu
		data.put("menu", Menu.getGroupMenuItems(context, group));
		
		// 4 -- Get the breadcrumbs
		Vector<Link> breadcrumbs = new Vector<Link>();
		breadcrumbs.add(  new Link("Main Dashboard", StandardViewList.getURL("main_dashboard")) );
		breadcrumbs.add(  new Link("Group Management", GroupListView.getURL()) );
		
		if( group != null ){
			breadcrumbs.add(  new Link("Edit Group: " + group.getGroupName(), GroupEditView.getURL(group)) );
			data.put("title", "Group: " + group);
		}
		else{
			breadcrumbs.add(  new Link("New Group", createURL("New")) );
			data.put("title", "New Group");
		}
		data.put("breadcrumbs", breadcrumbs);
		
		//Get the dashboard headers
		Shortcuts.addDashboardHeaders(request, response, data);
		
		TemplateLoader.renderToResponse("GroupEdit.ftl", data, response);
		
		return true;
		
	}

}
