package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
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
import net.lukemurphey.nsia.Application.DatabaseAccessType;
import net.lukemurphey.nsia.SiteGroupManagement.SiteGroupDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.eventlog.EventLogMessage.EventType;
import net.lukemurphey.nsia.scan.Definition;
import net.lukemurphey.nsia.scan.DefinitionArchive;
import net.lukemurphey.nsia.scan.DefinitionPolicyDescriptor;
import net.lukemurphey.nsia.scan.DefinitionSetLoadException;
import net.lukemurphey.nsia.scan.InvalidDefinitionException;
import net.lukemurphey.nsia.scan.ScanRule;
import net.lukemurphey.nsia.scan.DefinitionPolicyDescriptor.DefinitionPolicyAction;
import net.lukemurphey.nsia.scan.DefinitionPolicyDescriptor.DefinitionPolicyType;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.Menu;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;
import net.lukemurphey.nsia.web.forms.FieldError;
import net.lukemurphey.nsia.web.forms.FieldErrors;
import net.lukemurphey.nsia.web.templates.TemplateLoader;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class ExceptionEditView extends View {

	public static String VIEW_NAME = "exception_editor";
	
	public ExceptionEditView() {
		super("Exception/New", VIEW_NAME, Pattern.compile("[0-9]*"));
	}

	public static String getURL( int ruleID ) throws URLInvalidException{
		ExceptionEditView view = new ExceptionEditView();
		return view.createURL( ruleID );
	}
	
	public void addCategoryDescriptor( RequestContext context, SiteGroupDescriptor siteGroupDesc, int ruleID, String definitionCategory, DefinitionPolicyAction action) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException{
		
		Connection connection = null;
		
		try{
			Shortcuts.checkModify(context.getSessionInfo(), siteGroupDesc.getObjectId(), "Set " + action.name().toLowerCase() + " policy for " + definitionCategory + ".* for " + siteGroupDesc.getGroupName() + " (" + siteGroupDesc.getGroupId() + ") and rule ID " + ruleID );
			
			DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createCategoryPolicy(siteGroupDesc.getGroupId(), ruleID, definitionCategory, action);
			connection = Application.getApplication().getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			filter.saveToDatabase(connection);
		
		} catch (SQLException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					Application.getApplication().logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public void addCategoryDescriptor( RequestContext context, SiteGroupDescriptor siteGroupDesc, int ruleID, String definitionCategory, URL url, DefinitionPolicyAction action) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException{
		
		Connection connection = null;
		
		try{
			Shortcuts.checkModify(context.getSessionInfo(), siteGroupDesc.getObjectId(), "Set " + action.name().toLowerCase() + " policy for " + definitionCategory + ".* for " + siteGroupDesc.getGroupName() + " (" + siteGroupDesc.getGroupId() + ") and URL " + url.toExternalForm() +" and rule ID " + ruleID );
			
			DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createCategoryPolicy(siteGroupDesc.getGroupId(), ruleID, definitionCategory, url, action);
			connection = Application.getApplication().getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			filter.saveToDatabase(connection);
		
		} catch (SQLException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					Application.getApplication().logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public void addCategoryDescriptor( RequestContext context, SiteGroupDescriptor siteGroupDesc, String definitionCategory, URL url, DefinitionPolicyAction action) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException{
		
		Connection connection = null;
		
		try{
			Shortcuts.checkModify(context.getSessionInfo(), siteGroupDesc.getObjectId(), "Set " + action.name().toLowerCase() + " policy for " + definitionCategory + ".* for " + siteGroupDesc.getGroupName() + " (" + siteGroupDesc.getGroupId() + ") and URL " + url.toExternalForm() );
			
			DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createCategoryPolicy(siteGroupDesc.getGroupId(), definitionCategory, url, action);
			connection = Application.getApplication().getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			filter.saveToDatabase(connection);
		
		} catch (SQLException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					Application.getApplication().logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public void addCategoryDescriptor( RequestContext context, SiteGroupDescriptor siteGroupDesc, String definitionCategory, DefinitionPolicyAction action) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException{
		
		Connection connection = null;
		
		try{
			Shortcuts.checkModify(context.getSessionInfo(), siteGroupDesc.getObjectId(), "Set " + action.name().toLowerCase() + " policy for " + definitionCategory + ".* for " + siteGroupDesc.getGroupName() + " (" + siteGroupDesc.getGroupId() + ")" );
			
			DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createCategoryPolicy(siteGroupDesc.getGroupId(), definitionCategory, action);
			connection = Application.getApplication().getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			filter.saveToDatabase(connection);
		
		} catch (SQLException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					Application.getApplication().logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public void addSubCategoryDescriptor( RequestContext context, SiteGroupDescriptor siteGroupDesc, String categoryName, String subCategoryName, DefinitionPolicyAction action) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException{
		
		Connection connection = null;
		
		try{
			Shortcuts.checkModify(context.getSessionInfo(), siteGroupDesc.getObjectId(), "Add " + action.name().toLowerCase() + " policy for " + categoryName + "." + subCategoryName + ".* for " + siteGroupDesc.getGroupName() + " (" + siteGroupDesc.getGroupId() + ")" );
			
			DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createSubCategoryPolicy(siteGroupDesc.getGroupId(), categoryName, subCategoryName, action );
			connection = Application.getApplication().getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			filter.saveToDatabase(connection);
		
		} catch (SQLException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					Application.getApplication().logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public void addSubCategoryDescriptor( RequestContext context, SiteGroupDescriptor siteGroupDesc, int ruleID, String categoryName, String subCategoryName, URL url, DefinitionPolicyAction action) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException{
		
		Connection connection = null;
		
		try{
			Shortcuts.checkModify(context.getSessionInfo(), siteGroupDesc.getObjectId(), "Add exception for " + categoryName + "." + subCategoryName + ".* for " + siteGroupDesc.getGroupName() + " (" + siteGroupDesc.getGroupId() + ") and URL " + url.toExternalForm() + " and rule ID " + ruleID );
			
			DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createSubCategoryPolicy(siteGroupDesc.getGroupId(), ruleID, categoryName, subCategoryName, url, action );
			connection = Application.getApplication().getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			filter.saveToDatabase(connection);
		
		} catch (SQLException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					Application.getApplication().logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public void addSubCategoryDescriptor( RequestContext context, SiteGroupDescriptor siteGroupDesc, int ruleID, String categoryName, String subCategoryName, DefinitionPolicyAction action) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException{
		
		Connection connection = null;
		
		try{
			Shortcuts.checkModify(context.getSessionInfo(), siteGroupDesc.getObjectId(), "Add exception for " + categoryName + "." + subCategoryName + ".* for " + siteGroupDesc.getGroupName() + " (" + siteGroupDesc.getGroupId() + ") and rule ID " + ruleID );
			
			DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createSubCategoryPolicy(siteGroupDesc.getGroupId(), ruleID, categoryName, subCategoryName, action );
			connection = Application.getApplication().getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			filter.saveToDatabase(connection);
		
		} catch (SQLException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					Application.getApplication().logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public void addSubCategoryDescriptor( RequestContext context, SiteGroupDescriptor siteGroupDesc, String categoryName, String subCategoryName, URL url, DefinitionPolicyAction action) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException{
		
		Connection connection = null;
		
		try{
			Shortcuts.checkModify(context.getSessionInfo(), siteGroupDesc.getObjectId(), "Add exception for " + categoryName + "." + subCategoryName + ".* for " + siteGroupDesc.getGroupName() + " (" + siteGroupDesc.getGroupId() + ") and URL " + url.toExternalForm() );
			
			DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createSubCategoryPolicy(siteGroupDesc.getGroupId(), categoryName, subCategoryName, url, action );
			connection = Application.getApplication().getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			filter.saveToDatabase(connection);
		
		} catch (SQLException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					Application.getApplication().logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public void addDefinitionNameDescriptor( RequestContext context, SiteGroupDescriptor siteGroupDesc, int ruleID, String rulename, DefinitionPolicyAction action) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException, InvalidDefinitionException{
		
		Connection connection = null;
		
		try{
			Shortcuts.checkModify(context.getSessionInfo(), siteGroupDesc.getObjectId(), "Add exception for " + rulename + " for " + siteGroupDesc.getGroupName() + " (" + siteGroupDesc.getGroupId() + ") and " + ruleID );
			
			String[] sigName = Definition.parseName(rulename);
			
			DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createDefinitionPolicy(sigName[0], sigName[1], sigName[2], null, action, ruleID, siteGroupDesc.getGroupId());
			
			connection = Application.getApplication().getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			filter.saveToDatabase(connection);
		
		} catch (SQLException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		}
		finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					Application.getApplication().logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public void addDefinitionNameDescriptor( RequestContext context, SiteGroupDescriptor siteGroupDesc, String rulename, DefinitionPolicyAction action) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException, InvalidDefinitionException{
		
		Connection connection = null;
		
		try{
			Shortcuts.checkModify(context.getSessionInfo(), siteGroupDesc.getObjectId(), "Add exception for " + rulename + " for " + siteGroupDesc.getGroupName() + " (" + siteGroupDesc.getGroupId() + ")" );
			
			String[] sigName = Definition.parseName(rulename);
			
			DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createDefinitionPolicy(siteGroupDesc.getGroupId(), sigName[0], sigName[1], sigName[2], action);
			
			connection = Application.getApplication().getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			filter.saveToDatabase(connection);
		
		} catch (SQLException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					Application.getApplication().logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public void addDefinitionNameDescriptor( RequestContext context, SiteGroupDescriptor siteGroupDesc, int ruleID, String rulename, URL url, DefinitionPolicyAction action) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException, InvalidDefinitionException{
		
		Connection connection = null;
		
		try{
			Shortcuts.checkModify(context.getSessionInfo(), siteGroupDesc.getObjectId(), "Add exception for " + rulename + " for site-group \"" + siteGroupDesc.getGroupName() + "\" (" + siteGroupDesc.getGroupId() + ") and URL " + url.toExternalForm() + " and rule ID " + ruleID );
			
			String[] sigName = Definition.parseName(rulename);
			
			DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createDefinitionPolicy(siteGroupDesc.getGroupId(), ruleID, sigName[0], sigName[1], sigName[2], url, action);
			
			connection = Application.getApplication().getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			filter.saveToDatabase(connection);
		
		} catch (SQLException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					Application.getApplication().logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	public void addSignatureNameDescriptor( RequestContext context, SiteGroupDescriptor siteGroupDesc, String rulename, URL url, DefinitionPolicyAction action) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException, InvalidDefinitionException{
		
		Connection connection = null;
		
		try{
			
			Shortcuts.checkModify(context.getSessionInfo(), siteGroupDesc.getObjectId(), "Add exception for " + rulename + " for site-group \"" + siteGroupDesc.getGroupName() + "\" (" + siteGroupDesc.getGroupId() + ") and URL " + url.toExternalForm() );
			
			String[] sigName = Definition.parseName(rulename);
			
			DefinitionPolicyDescriptor filter = DefinitionPolicyDescriptor.createDefinitionPolicy(siteGroupDesc.getGroupId(), sigName[0], sigName[1], sigName[2], url, action);
			
			connection = Application.getApplication().getDatabaseConnection(DatabaseAccessType.SCANNER);
			
			filter.saveToDatabase(connection);
		
		} catch (SQLException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
			throw new GeneralizedException();
		} catch (NoDatabaseConnectionException e) {
			Application.getApplication().logExceptionEvent(EventLogMessage.EventType.DATABASE_FAILURE, e);
			throw new GeneralizedException();
		} finally{
			if( connection != null ){
				try{
					connection.close();
				}
				catch (SQLException e) {
					Application.getApplication().logExceptionEvent(EventLogMessage.EventType.SQL_EXCEPTION, e);
				}
			}
		}
	}
	
	/**
	 * Processes a redirect to the relevant location (following performing an operation).
	 * @param request
	 * @param response
	 * @param context
	 * @param siteGroup
	 * @param ruleID
	 * @throws IOException
	 * @throws URLInvalidException
	 */
	private void processReturn(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			SiteGroupDescriptor siteGroup, int ruleID) throws IOException, URLInvalidException{
		
		if( request.getParameter("ReturnTo") != null ){
			response.sendRedirect(request.getParameter("ReturnTo"));
		}
		else{
			response.sendRedirect( ExceptionListView.getURL(ruleID) );
		}
	}
	
	private boolean createException(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data,
			String url, Definition definition, SiteGroupDescriptor siteGroup, int ruleID) throws GeneralizedException, NoSessionException, NotFoundException, InsufficientPermissionException, IOException, URLInvalidException{
		
		if( request.getParameter("Cancel") != null ){
			if( request.getParameter("ReturnTo") != null ){
				response.sendRedirect(request.getParameter("ReturnTo"));
				return true;
			}
			else{
				response.sendRedirect( RuleEditView.getURL(ruleID) );
				return true;
			}
		}
		
		// 1 -- Get the relevant fields
		DefinitionPolicyType filterType = DefinitionPolicyType.NAME;
		String filterTypeString = request.getParameter("FilterType");
		URL urlRestriction = null;
		
		if( url != null ){
			try{
				urlRestriction = new URL(url);
			}
			catch(MalformedURLException e){
				FieldErrors fieldErrors = new FieldErrors();
				fieldErrors.put(new FieldError("URL", "", "URL is not valid"));
				data.put("form_errors", fieldErrors);
				return false;
			}
		}
		
		if( "Category".equalsIgnoreCase( filterTypeString ) ) {
			filterType = DefinitionPolicyType.CATEGORY;
		}
		else if( "SubCategory".equalsIgnoreCase( filterTypeString ) ) {
			filterType = DefinitionPolicyType.SUBCATEGORY;
		}
		else if( "Definition".equalsIgnoreCase( filterTypeString ) ) {
			filterType = DefinitionPolicyType.NAME;
		}
		else {
			FieldErrors fieldErrors = new FieldErrors();
			fieldErrors.put(new FieldError("FilterType", "", "Please indicate the type of exception to be created"));
			data.put("form_errors", fieldErrors);
			return false;
		}
		
		// 2 -- Update the policy
		
		//	 2.1 -- Handle a name-based policy
		if( filterType == DefinitionPolicyType.NAME ){
			
			try{
				if( urlRestriction == null ){
					addDefinitionNameDescriptor(context, siteGroup, ruleID, definition.getFullName(), DefinitionPolicyAction.EXCLUDE);
				}
				else{
					addDefinitionNameDescriptor(context, siteGroup, ruleID, definition.getFullName(), urlRestriction, DefinitionPolicyAction.EXCLUDE);
				}
			}
			// Post a warning if the definition name was invalid 
			catch(InvalidDefinitionException e){
				FieldErrors fieldErrors = new FieldErrors();
				fieldErrors.put(new FieldError("DefinitionName", "", "Definition name is invalid"));
				data.put("form_errors", fieldErrors);
				return false;
			}
			
			context.addMessage("Exception successfully added", MessageSeverity.SUCCESS);
			processReturn(request, response, context, siteGroup, ruleID);
			return true;
		}
		
		//	 2.2 -- Handle a category exception
		else if( filterType == DefinitionPolicyType.CATEGORY ){

			if( urlRestriction == null ){
				addCategoryDescriptor(context, siteGroup, ruleID, definition.getCategoryName(), DefinitionPolicyAction.EXCLUDE );
			}
			else{
				addCategoryDescriptor(context, siteGroup, ruleID, definition.getCategoryName(), urlRestriction, DefinitionPolicyAction.EXCLUDE );
			}
			
			context.addMessage("Exception successfully added", MessageSeverity.SUCCESS);
			processReturn(request, response, context, siteGroup, ruleID);
			return true;
		}
		
		//	 2.3 --Handle a sub-category exception
		else if( filterType == DefinitionPolicyType.SUBCATEGORY ){

			if( urlRestriction == null ){
				addSubCategoryDescriptor(context, siteGroup, ruleID, definition.getCategoryName(), definition.getSubCategoryName(), DefinitionPolicyAction.EXCLUDE );
			}
			else{
				addSubCategoryDescriptor(context, siteGroup, ruleID, definition.getCategoryName(), definition.getSubCategoryName(), urlRestriction, DefinitionPolicyAction.EXCLUDE );
			}
			
			context.addMessage("Exception successfully added", MessageSeverity.SUCCESS);
			
			
			// Log that the exception was created
			Application.getApplication().logEvent( new EventLogMessage( EventType.RULE_EXCEPTION_ADDED,
					new EventLogField( FieldName.SOURCE_USER_NAME, context.getUser().getUserName() ),
					new EventLogField( FieldName.SOURCE_USER_ID, context.getUser().getUserID() ),
					new EventLogField( FieldName.RULE_ID, ruleID ),
					new EventLogField( FieldName.GROUP_ID, siteGroup.getGroupId() ),
					new EventLogField( FieldName.GROUP_NAME, siteGroup.getGroupName() ))
					);
			
			processReturn(request, response, context, siteGroup, ruleID);
			return true;
		}
		
		return false;
		
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {
		
		// 1 -- Get the exceptions
		Shortcuts.addDashboardHeaders(request, response, data);
		
		//	 1.1 -- Get the rule ID
		int ruleID;
		
		try{
			ruleID = Integer.valueOf( args[0] );
			data.put("ruleID", ruleID);
		}
		catch(NumberFormatException e ){
			Dialog.getDialog(response, context, data, "The rule ID provided is invalid", "Rule ID invalid", DialogType.WARNING);
			return true;
		}
		
		// 2 -- Get the SiteGroup
		SiteGroupDescriptor siteGroup = null;
		DefinitionArchive definitonArchive = null;
		try {
			definitonArchive = DefinitionArchive.getArchive();
			int siteGroupID = ScanRule.getSiteGroupForRule(ruleID);
			SiteGroupManagement siteGroupMgmt = new SiteGroupManagement(Application.getApplication());
			siteGroup = siteGroupMgmt.getGroupDescriptor(siteGroupID);
			data.put("siteGroupID", siteGroupID);
			data.put("siteGroup", siteGroup);
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (InputValidationException e) {
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		} catch (NotFoundException e) {
			Dialog.getDialog(response, context, data, "A site-group with the given identifier could not be found.", "Site-group Not Found", DialogType.WARNING);
			return true;
		} catch (DefinitionSetLoadException e) {
			throw new ViewFailedException(e);
		} 
		
		// 3 -- Get the associated definition
		Definition definition = null;
		String definitionName = request.getParameter("DefinitionName");
		
		try{
			if( request.getParameter("DefinitionID") != null ){
				int definitionID;
				definitionID = Integer.valueOf( request.getParameter("DefinitionID") );
				data.put("definitionID", definitionID);
				definition = definitonArchive.getDefinition(definitionID);
				
			}
			else if ( definitionName != null ){
				definition = definitonArchive.getDefinition(definitionName);
				data.put("definition", definition);
			}
			else{
				Dialog.getDialog(response, context, data, "The definition ID provided is not valid", "Definition ID Invalid", DialogType.WARNING);
				return true;
			}
		}
		catch( NumberFormatException e ){
			Dialog.getDialog(response, context, data, "The definition ID provided is not valid", "Definition ID Invalid", DialogType.WARNING);
			return true;
		} catch (NotFoundException e) {
			Dialog.getDialog(response, context, data, "A definition with the given name was not found", "Definition Not Found", DialogType.WARNING);
			return true;
		}
		
		data.put("URL", request.getParameter("URL"));
		data.put("definitionName", definitionName);
		
		// 4 -- Create the breadcrumbs
		Vector<Link> breadcrumbs = new Vector<Link>();
		breadcrumbs.add(  new Link("Main Dashboard", MainDashboardView.getURL()) );
		breadcrumbs.add(  new Link("Site Group: " + siteGroup.getGroupName(), SiteGroupView.getURL(siteGroup.getGroupId())) );
		breadcrumbs.add(  new Link("Edit Rule", RuleEditView.getURL(ruleID)) );
		breadcrumbs.add(  new Link("Scan History", ScanResultHistoryView.getURL(ruleID)) );
		breadcrumbs.add(  new Link("Exceptions", createURL(ruleID)) );
		data.put("breadcrumbs", breadcrumbs);
		
		// 5 -- Create the menu
		data.put("menu", Menu.getScanRuleMenu(context, siteGroup, ruleID));
		
		data.put("title", "Exceptions");
		
		// 6 -- Check rights
		try {
			if( Shortcuts.canModify(context.getSessionInfo(), siteGroup.getObjectId(), "Create exception") == false ){
				
				String returnTo = request.getParameter("ReturnTo");
				Link link = null;
				
				if( returnTo != null ){
					link = new Link("Return to previous page", returnTo);
				}
				else{
					link = new Link("Return to Scan Result", ScanResultHistoryView.getURL(ruleID));
				}
				Shortcuts.getPermissionDeniedDialog(response, data, "You do not have permission to edit this site-group", link);
				return true;
			}
		} catch (GeneralizedException e) {
			throw new ViewFailedException(e);
		}
		
		// 7 -- Perform any changes
		try {
			if( "POST".equalsIgnoreCase(request.getMethod()) && createException(request, response, context, args, data, request.getParameter("URL"), definition, siteGroup, ruleID) ){
				return true;
			}
		} catch (GeneralizedException e) {
			throw new ViewFailedException(e);
		} catch (NoSessionException e) {
			throw new ViewFailedException(e);
		} catch (NotFoundException e) {
			throw new ViewFailedException(e);
		} catch (InsufficientPermissionException e) {
			Shortcuts.getPermissionDeniedDialog(response, data, "You do not have permission to create exceptions for this site-group");
			return true;
		}

		
		// 5 -- Render the page
		TemplateLoader.renderToResponse("ExceptionEdit.ftl", data, response);
		return true;
	}

}
