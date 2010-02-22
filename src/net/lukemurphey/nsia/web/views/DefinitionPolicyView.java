package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.SiteGroupManagement;
import net.lukemurphey.nsia.Application.DatabaseAccessType;
import net.lukemurphey.nsia.SessionManagement.SessionInfo;
import net.lukemurphey.nsia.SiteGroupManagement.SiteGroupDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.scan.DefinitionArchive;
import net.lukemurphey.nsia.scan.DefinitionPolicyDescriptor;
import net.lukemurphey.nsia.scan.DefinitionPolicyManagement;
import net.lukemurphey.nsia.scan.DefinitionPolicySet;
import net.lukemurphey.nsia.scan.DefinitionSet;
import net.lukemurphey.nsia.scan.DefinitionSetLoadException;
import net.lukemurphey.nsia.scan.DefinitionPolicyDescriptor.DefinitionPolicyAction;
import net.lukemurphey.nsia.scan.DefinitionSet.DefinitionCategory;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.StandardViewList;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;
import net.lukemurphey.nsia.web.templates.TemplateLoader;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class DefinitionPolicyView extends View {

	public static final String VIEW_NAME = "definitions_policy";
	
	public DefinitionPolicyView() {
		super("Definitions/Policy", VIEW_NAME, Pattern.compile("[0-9]*"));
	}

	public static String getURL( int siteGroupID ) throws URLInvalidException{
		DefinitionPolicyView view = new DefinitionPolicyView();
		return view.createURL(siteGroupID);
	}
	
	public static String getURL( SiteGroupDescriptor siteGroup ) throws URLInvalidException{
		DefinitionPolicyView view = new DefinitionPolicyView();
		return view.createURL(siteGroup.getGroupId());
	}
	
	private String getName( String name, String category, String subCategory ){
		String completeName = "";
		
		if( category != null ){
			completeName += category;
		}
		if( subCategory != null ){
			completeName += "." + subCategory;
		}
		if( name != null ){
			completeName += "." + name;
		}
		
		return completeName;
	}
	
	private CategoryDescriptor getDescriptor( DefinitionPolicySet policySet, int siteGroupID, DefinitionCategory definitionCategory ){
		
		DefinitionPolicyDescriptor descriptor = policySet.getMatchingPolicy(siteGroupID, -1, null, definitionCategory.getCategory(), definitionCategory.getSubCategory(), null);
		
		return new CategoryDescriptor( getName(null, definitionCategory.getCategory(), definitionCategory.getSubCategory()) , descriptor);
	}
	
	public class CategoryDescriptor{
		private String name;
		private boolean isDefault;
		private boolean enabled;
		
		public CategoryDescriptor( String categoryName, DefinitionPolicyDescriptor descriptor ){
			
			if( descriptor == null ){
				isDefault = true;
				enabled = true;
			}
			else if( descriptor.getAction() == DefinitionPolicyAction.INCLUDE){
				enabled = true;
				isDefault = false;
			}
			else{
				enabled = false;
				isDefault = false;
			}
			
			name = categoryName;
		}
		
		public boolean isEnabled(){
			return enabled;
		}
		
		
		public boolean isDefault(){
			return isDefault;
		}
		
		public String getName(){
			return name;
		}
		
	}
	
	/**
	 * Parse the definition into the individual parts.
	 * @param def
	 * @return
	 */
	private String[] parseDefinition(String def){
		return StringUtils.split(def, ".");
	}
	
	/**
	 * Clear the default policy for the given category.
	 * @param session
	 * @param categoryName
	 * @param subCategoryName
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws NotFoundException
	 * @throws InsufficientPermissionException
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 */
	public void clearCategoryDescriptors( SessionInfo session, String categoryName, String subCategoryName ) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException, NoDatabaseConnectionException, SQLException{

		// 1 -- Check permissions
		Shortcuts.checkRight(session, "System.Configuration.View", "Clear default policy for " + categoryName + "." + subCategoryName + ".*" ); 
		
		// 2 -- Clear the descriptors
		DefinitionPolicyManagement defMgr = new DefinitionPolicyManagement( Application.getApplication() );
		defMgr.clearSubCategoryDescriptors(categoryName, subCategoryName );
	}
	
	/**
	 * Clear all of the descriptors for the given site-group and category.
	 * @param session
	 * @param categoryName
	 * @param subCategoryName
	 * @param siteGroupDesc
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws NotFoundException
	 * @throws InsufficientPermissionException
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 */
	public void clearCategoryDescriptors( SessionInfo session, String categoryName, String subCategoryName, SiteGroupDescriptor siteGroupDesc ) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException, NoDatabaseConnectionException, SQLException{
		
		// 1 -- Check permissions
		Shortcuts.checkModify(session, siteGroupDesc.getObjectId(), "Clear policies for " + categoryName + "." + subCategoryName + ".* for " + siteGroupDesc.getGroupName() + " (" + siteGroupDesc.getGroupId() + ")" );
		
		// 2 -- Clear the descriptors
		DefinitionPolicyManagement defMgr = new DefinitionPolicyManagement( Application.getApplication() );
		defMgr.clearSubCategoryDescriptors(siteGroupDesc, categoryName, subCategoryName );

	}
	
	/**
	 * Add the definition policy for the given .
	 * @param session
	 * @param categoryName
	 * @param subCategoryName
	 * @param action
	 * @throws GeneralizedException
	 * @throws NoSessionException
	 * @throws NotFoundException
	 * @throws InsufficientPermissionException
	 */
	public void addCategoryDescriptor( SessionInfo session, String categoryName, String subCategoryName, DefinitionPolicyAction action) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException{
		
		Connection connection = null;
		
		try{
			
			// 1 -- Check permissions
			Shortcuts.checkRight(session, "System.Configuration.View", "Set " + action.name().toLowerCase() + " policy for " + categoryName + "." + subCategoryName + ".* at global level" );
			
			// 2 -- Create the filter
			DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createSubCategoryPolicy(categoryName, subCategoryName, action );
			connection = Application.getApplication().getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			filter.saveToDatabase(connection);
		
		} catch (SQLException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.Category.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					Application.getApplication().logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	/**
	 * Adds a new category descriptor for enabling the definitions within the given category.
	 * @param session
	 * @param siteGroupID
	 * @param category
	 * @param subCategory
	 * @param action
	 * @param siteGroupDesc
	 * @return
	 * @throws GeneralizedException
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 */
	private boolean addCategoryDescriptor( SessionInfo session, String category, String subCategory, DefinitionPolicyAction action, SiteGroupDescriptor siteGroupDesc ) throws GeneralizedException, NoDatabaseConnectionException, SQLException{
		Connection connection = null;
		
		try{
			if( Shortcuts.canModify( session, siteGroupDesc.getObjectId(), "Add " + action.name().toLowerCase() + " policy for " + category + "." + subCategory + ".* for " + siteGroupDesc.getGroupName() + " (" + siteGroupDesc.getGroupId() + ")" ) ){
				DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createSubCategoryPolicy(siteGroupDesc.getGroupId(), category, subCategory, action );
				connection = Application.getApplication().getDatabaseConnection(DatabaseAccessType.SCANNER);
				
				filter.saveToDatabase(connection);
				
				return true;
			}
			else{
				return false;
			}
		}
		finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					Application.getApplication().logExceptionEvent(EventLogMessage.Category.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	/**
	 * Process requested changes (such as, enable/disable entries or set to default).
	 * @param request
	 * @param context
	 * @param siteGroupDesc
	 * @throws GeneralizedException
	 * @throws NoDatabaseConnectionException
	 * @throws SQLException
	 * @throws ViewFailedException
	 */
	private void processChanges( HttpServletRequest request, RequestContext context, SiteGroupDescriptor siteGroupDesc ) throws GeneralizedException, NoDatabaseConnectionException, SQLException, ViewFailedException{
		
		// 1 -- Get the action requested
		String action = request.getParameter("Action");
		String[] policies = request.getParameterValues("DefinitionPolicy");
		
		// 2 -- Handle requested to disable categories		
		if( action.equalsIgnoreCase( "Disable" ) ){
				
			// 2.1 -- Make sure some policies were defined
			if( policies == null || policies.length == 0 ){
				context.addMessage("Please select the definition categories to disable", MessageSeverity.WARNING);
				return;
			}
				
			// 2.2 -- Loop through the policies and disable them
			for(String policy : policies){
				String[] definition = parseDefinition(policy);
					
				String category = definition[0];
				String subCategory = definition[1];
					
				try {
					if( siteGroupDesc != null ){
						addCategoryDescriptor(context.getSessionInfo(), category, subCategory, DefinitionPolicyAction.EXCLUDE, siteGroupDesc);
					}
					else{
						addCategoryDescriptor(context.getSessionInfo(), category, subCategory, DefinitionPolicyAction.EXCLUDE);
					}
						
				} catch (InsufficientPermissionException e) {
					context.addMessage("You don't have permissions to edit the scan policy", MessageSeverity.WARNING);
				} catch (NotFoundException e) {
					throw new ViewFailedException(e);
				} catch (NoSessionException e) {
					throw new ViewFailedException(e);
				}
			}
		}
		
		// 3 -- Handle requests to enable categories
		else if( action.equalsIgnoreCase( "Enable" ) ){
			
			// 3.1 -- Make sure some policies were defined
			if( policies == null || policies.length == 0 ){
				context.addMessage("Please select the definition categories to enable", MessageSeverity.WARNING);
				return;
			}
				
			// 3.2 -- Loop through the policies and disable them
			for(String policy : policies){
				String[] definition = parseDefinition(policy);
				
				String category = definition[0];
				String subCategory = definition[1];
				
				try {
					if( siteGroupDesc != null ){
						addCategoryDescriptor(context.getSessionInfo(), category, subCategory, DefinitionPolicyAction.INCLUDE, siteGroupDesc);
					}
					else{
						addCategoryDescriptor(context.getSessionInfo(), category, subCategory, DefinitionPolicyAction.INCLUDE);
					}
						
				} catch (InsufficientPermissionException e) {
					context.addMessage("You don't have permissions to edit the scan policy", MessageSeverity.WARNING);
				} catch (NotFoundException e) {
					throw new ViewFailedException(e);
				} catch (NoSessionException e) {
					throw new ViewFailedException(e);
				}
			}
		}
		
		// 4 -- Handle requests to set status to default (inherit from default policy)
		else if( action.equalsIgnoreCase( "Set Default" ) && siteGroupDesc != null ){
			
			// 4.1 -- Make sure some policies were defined
			if( policies == null || policies.length == 0 ){
				context.addMessage("Please select the definition categories to modify", MessageSeverity.WARNING);
				return;
			}
			
			// 4.2 -- Loop through the policies and enable them
			for(String policy : policies){
				String[] definition = parseDefinition(policy);
				
				String category = definition[0];
				String subCategory = definition[1];
				
				try {
					if( siteGroupDesc != null ){
						clearCategoryDescriptors(context.getSessionInfo(), category, subCategory, siteGroupDesc);
					}
					else{
						clearCategoryDescriptors(context.getSessionInfo(), category, subCategory);
					}
				} catch (InsufficientPermissionException e) {
					context.addMessage("You don't have permissions to edit the scan policy", MessageSeverity.WARNING);
				} catch (NotFoundException e) {
					context.addMessage("You don't have permissions to edit the scan policy", MessageSeverity.WARNING);
				} catch (NoSessionException e) {
					throw new ViewFailedException(e);
				}
				
			}
		}
	}
	
	@Override
	protected boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException, URLInvalidException, IOException, ViewNotFoundException {
		
		try{
			// 1 -- Check rights
			//TODO check rights
			
			
			// 2 -- Get the site group ID (if getting the policy for a site group)
			int siteGroupID = -1;
			SiteGroupDescriptor siteGroup = null;
			
			if( args.length > 0 ){
				try{
					siteGroupID = Integer.valueOf( args[0] );
					SiteGroupManagement siteGroupManagement = new SiteGroupManagement(Application.getApplication());
					siteGroup = siteGroupManagement.getGroupDescriptor(siteGroupID);
				}
				catch( NumberFormatException e ){
					Dialog.getDialog(response, context, data, "The site-group ID provided is invalid", "SiteGroup ID Invalid", DialogType.WARNING);
					return true;
				}
				catch( NotFoundException e ){
					Dialog.getDialog(response, context, data, "No site-group found with the given ID", "SiteGroup ID Invalid", DialogType.WARNING);
					return true;
				}
			}
			
			
			// 3 -- Perform the changes
			if( "POST".equalsIgnoreCase( request.getMethod() ) ){ 
				try {
					processChanges(request, context, siteGroup);
				} catch (GeneralizedException e) {
					throw new ViewFailedException(e);
				}
			}
			
			
			// 4 --  Get the descriptors that outline the categories
			
			//	 4.1 -- Get the policy set
			DefinitionPolicyManagement policyManagement = new DefinitionPolicyManagement(Application.getApplication());
			DefinitionPolicySet policySet;
			
			if( siteGroupID > -1 ){
				policySet = policyManagement.getPolicySet( siteGroupID );
			}
			else{
				policySet = policyManagement.getPolicySet();
			}
			
			//	 4.2 -- Get the categories
			DefinitionArchive archive = DefinitionArchive.getArchive();
			DefinitionSet definitionSet = archive.getDefinitionSet();
			DefinitionCategory[] categories = definitionSet.getListOfSubCategories();
	
			//	 4.3 -- Get the category descriptors
			Vector<CategoryDescriptor> descriptors = new Vector<CategoryDescriptor>();
			
			for (DefinitionCategory category : categories) {
				descriptors.add( getDescriptor(policySet, siteGroupID, category) );
			}
	

			// 5 -- Prepare the page to be rendered
			data.put("title", "Definition Policy");
			
			//	 5.1 -- Get the breadcrumbs
			Vector<Link> breadcrumbs = new Vector<Link>();
			breadcrumbs.add(  new Link("Main Dashboard", StandardViewList.getURL("main_dashboard")) );
			
			if( siteGroupID > -1 ){
				breadcrumbs.add(  new Link("SiteGroups", "") ); //TODO Add SiteGroup links
				breadcrumbs.add(  new Link("SiteGroup", "") );
				breadcrumbs.add(  new Link("Definition Policy", createURL(siteGroupID)) );
			}
			else{
				breadcrumbs.add(  new Link("Definitions", StandardViewList.getURL("definitions_list")) );
				breadcrumbs.add(  new Link("Default Definition Policy", createURL()) );
			}
			
			data.put("breadcrumbs", breadcrumbs);
			
			//	 5.2 -- Get the menu
			Vector<Link> menu = new Vector<Link>();
			menu.add( new Link("System Administration") );
			menu.add( new Link("System Status", StandardViewList.getURL("system_status")) );
			menu.add( new Link("System Configuration", StandardViewList.getURL("system_configuration")) );
			menu.add( new Link("Event Logs", StandardViewList.getURL("event_log")) );
			menu.add( new Link("Shutdown System", StandardViewList.getURL("system_shutdown")) );
			menu.add( new Link("Create Backup", StandardViewList.getURL("system_backup")) );
			
			menu.add( new Link("Scanning Engine") );
			if( Application.getApplication().getScannerController().scanningEnabled() ){
				menu.add( new Link("Stop Scanner", StandardViewList.getURL("scanner_stop")) );
			}
			else{
				menu.add( new Link("Start Scanner", StandardViewList.getURL("scanner_start")) );
			}
			menu.add( new Link("View Definitions", StandardViewList.getURL(DefinitionsView.VIEW_NAME)) );
			
			menu.add( new Link("Definition") );
			menu.add( new Link("New Definition", StandardViewList.getURL(DefinitionEntryView.VIEW_NAME, "New")) );
			
			if( siteGroupID > -1 ){
				menu.add( new Link("Edit Default Policy", StandardViewList.getURL(DefinitionPolicyView.VIEW_NAME)) );
			}
			
			data.put("menu", menu);
			
			//	 5.3 -- Get the dashboard headers
			Shortcuts.addDashboardHeaders(request, response, data);
			
			
			// 6 -- Render the page
			data.put("categories", descriptors);
			data.put("sitegroup", siteGroup);
			TemplateLoader.renderToResponse("DefinitionPolicy.ftl", data, response);
			
			return true;
			
		} catch( DefinitionSetLoadException e ){
			throw new ViewFailedException(e);
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		} catch (InputValidationException e) {
			throw new ViewFailedException(e);
		}
	}

}
