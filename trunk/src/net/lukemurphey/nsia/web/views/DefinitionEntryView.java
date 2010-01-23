package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.DisallowedOperationException;
import net.lukemurphey.nsia.DuplicateEntryException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.scan.Definition;
import net.lukemurphey.nsia.scan.DefinitionArchive;
import net.lukemurphey.nsia.scan.DefinitionSetLoadException;
import net.lukemurphey.nsia.scan.InvalidDefinitionException;
import net.lukemurphey.nsia.scan.PatternDefinition;
import net.lukemurphey.nsia.scan.ScriptDefinition;
import net.lukemurphey.nsia.scan.UnpurposedDefinitionException;
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

public class DefinitionEntryView extends View {

	public static final String VIEW_NAME = "definition";
	
	public DefinitionEntryView() {
		super("Definition", VIEW_NAME, Pattern.compile("([0-9]*)|(New)", Pattern.CASE_INSENSITIVE));
	}

	protected Definition updateDefinition( HttpServletRequest request, RequestContext context, String code, int localID ) throws InvalidDefinitionException, UnpurposedDefinitionException, ViewFailedException, DisallowedOperationException, DuplicateEntryException{
		
		// 1 -- Parse the definition
		Definition definition = null;
		
		if( "ThreatPattern".equalsIgnoreCase( request.getParameter("Type") ) ){
			if( localID >= 0 ){
				definition = PatternDefinition.parse(code, localID);
			}
			else{
				definition = PatternDefinition.parse(code);
			}
		}
		else{
			if( localID >= 0 ){
				definition = ScriptDefinition.parse(code, localID);
			}
			else{
				definition = ScriptDefinition.parse(code);
			}
		}
		
		// 2 -- Save the definition
		try{
			DefinitionArchive archive = DefinitionArchive.getArchive();
	
			if( localID >= 0 ){
				archive.updateDefinition(definition);
			}
			else{
				archive.addDefinition(definition);
			}
		}
		catch( NoDatabaseConnectionException e ){
			throw new ViewFailedException(e);
		}
		catch( SQLException e ){
			throw new ViewFailedException(e);
		}
		catch( InputValidationException e ){
			throw new ViewFailedException(e);
		}
		catch( DefinitionSetLoadException e ){
			throw new ViewFailedException(e);
		}
		
		// 3 -- Log the result
		Application.getApplication().logEvent(EventLogMessage.Category.DEFINITIONS_UPDATED, new EventLogField[]{
				new EventLogField( EventLogField.FieldName.DEFINITION_ID, definition.getID() ),
				new EventLogField( EventLogField.FieldName.DEFINITION_NAME, definition.getFullName() ),
				new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, context.getSessionInfo().getUserName() ),
				new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, context.getSessionInfo().getUserId() )} );
		
		// 4 -- Return the definition
		return definition;
	}
	
	@Override
	protected boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException, URLInvalidException, IOException, ViewNotFoundException {

		try{
			
			// Redirect if the user pressed cancel
			if( request.getParameter("Cancel") != null ){
				response.sendRedirect( StandardViewList.getURL("definitions_list") );
				return true;
			}
			
			// 1 -- Check rights
			//TODO check rights
			
			// 2 -- Get the definition
			Definition definition = null;
			String code = null;
			String type = null;
			
			if( args.length > 0 && "new".equalsIgnoreCase(args[0]) == false ){
				int definitionID;
				
				try{
					definitionID = Integer.valueOf( args[0] );
					DefinitionArchive archive = DefinitionArchive.getArchive();
					
					definition = archive.getDefinition(definitionID);
					data.put("definition", definition);
				}
				catch( NumberFormatException e ){
					Dialog.getDialog(response, context, data, "The definition ID provided is invalid.", "Definition ID Invalid", DialogType.WARNING);
					return true;
				}
				catch( NotFoundException e ){
					Dialog.getDialog(response, context, data, "No definition provided with the given ID.", "Definition Not Found", DialogType.WARNING);
					return true;
				}
			}
			else if( args.length > 0 && "new".equalsIgnoreCase(args[0]) ){
				Map<String, Object> t_data = new HashMap<String, Object>();
				t_data.put("ID", DefinitionArchive.getArchive().getNextOpenID(false));
					
				if( request.getParameter("Type") != null ){
					type = request.getParameter("Type");
				}
				
				if( "ThreatPattern".equalsIgnoreCase( request.getParameter("Type") ) ){
					code = TemplateLoader.renderToString("DefaultPatternDefinition.ftl", t_data);
				}
				else{
					code = TemplateLoader.renderToString("DefaultScriptDefinition.ftl", t_data);
				}
			}
			
			// 3 -- Process any changes requested
			if( request.getMethod().equalsIgnoreCase("POST")){
				
				// 3.1.1 -- Get the local ID if editing an existing rule
				int localID = -1;
				
				if( request.getParameter("LocalID") != null ){
					try{
						localID = Integer.valueOf( request.getParameter("LocalID") );
					}
					catch( NumberFormatException e ){
						Dialog.getDialog(response, context, data, "The definition ID provided is invalid.", "Definition ID Invalid", DialogType.WARNING);
						return true;
					}
				}
				
				// 3.1.2 -- Get the code for the definition
				String sig_code = request.getParameter("SignatureCode");
				
				if( sig_code == null ){
					sig_code = request.getParameter("SignatureCode2");
				}
				
				code = sig_code;
				
				if( request.getParameter("Type") != null ){
					type = request.getParameter("Type");
				}
				
				// 3.1.3 -- Create the definition
				try{
					definition = updateDefinition(request, context, sig_code, localID);
					
					if( definition != null ){
						response.sendRedirect(createURL( definition.getID() ));
						
						if( localID >= 0 ){
							context.addMessage("Definition updated successfully", MessageSeverity.SUCCESS);
						}
						else{
							context.addMessage("Definition created successfully", MessageSeverity.SUCCESS);
						}
						
						return true;
					}

				} catch(InvalidDefinitionException e){
					context.addMessage(e.getMessage(), MessageSeverity.WARNING);
				} catch(UnpurposedDefinitionException e){
					context.addMessage(e.getMessage(), MessageSeverity.WARNING);
				} catch (DisallowedOperationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (DuplicateEntryException e) {
					context.addMessage(e.getMessage(), MessageSeverity.WARNING);
				}
			}
			
			// 4 -- Prepare the page to be rendered
			data.put("title", "Definition");
			
			//	 4.1 -- Get the breadcrumbs
			Vector<Link> breadcrumbs = new Vector<Link>();
			breadcrumbs.add(  new Link("Main Dashboard", StandardViewList.getURL("main_dashboard")) );
			breadcrumbs.add(  new Link("Definitions", StandardViewList.getURL("definitions_list")) );
			if( definition != null ){
				breadcrumbs.add(  new Link("View Definition", createURL(definition.getID())) );
			}
			else{
				breadcrumbs.add(  new Link("New Definition", createURL("New")) );
			}
			data.put("breadcrumbs", breadcrumbs);
			
			//	 4.2 -- Get the menu
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
			
			if( definition != null ){
				menu.add( new Link("Definition") );
				menu.add( new Link("Delete Definition", StandardViewList.getURL(DefinitionDeleteView.VIEW_NAME, definition.getID())) );
			}
			
			data.put("menu", menu);
			
			//	 4.3 -- Get the dashboard headers
			Shortcuts.addDashboardHeaders(request, response, data);
			
			// 5 -- Render the page
			data.put("definition", definition);
			data.put("ARACHNIDS", Definition.Reference.ARACHNIDS);
			data.put("BUGTRAQ", Definition.Reference.BUGTRAQ);
			data.put("CVE", Definition.Reference.CVE);
			data.put("MCAFEE", Definition.Reference.MCAFEE);
			data.put("NESSUS", Definition.Reference.NESSUS);
			data.put("URL", Definition.Reference.URL);
			
			// For storing the signature while being edited:
			data.put("code", code);
			data.put("type", type);
			
			if( type == null && definition == null ){
				TemplateLoader.renderToResponse("DefinitionSelectTypeView.ftl", data, response);
			}
			else{
				TemplateLoader.renderToResponse("DefinitionEntry.ftl", data, response);
			}
			return true;
		}
		catch(InputValidationException e){
			throw new ViewFailedException(e);
		}
		catch(NoDatabaseConnectionException e){
			throw new ViewFailedException(e);
		}
		catch(SQLException e){
			throw new ViewFailedException(e);
		}
		catch(DefinitionSetLoadException e){
			throw new ViewFailedException(e);
		}
	}

}
