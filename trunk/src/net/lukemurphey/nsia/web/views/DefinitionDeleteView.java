package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.DisallowedOperationException;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.scan.DefinitionArchive;
import net.lukemurphey.nsia.scan.DefinitionSetLoadException;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.StandardViewList;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class DefinitionDeleteView extends View{

	public static final String VIEW_NAME = "definition_delete";
	
	public DefinitionDeleteView() {
		super("Definition/Delete", VIEW_NAME, Pattern.compile("[0-9]+", Pattern.CASE_INSENSITIVE));
	}

	public static String getURL( int definitionID ) throws URLInvalidException{
		DefinitionDeleteView view = new DefinitionDeleteView();
		return view.createURL(definitionID);
	}
	
	@Override
	protected boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException, URLInvalidException, IOException, ViewNotFoundException {
		
		// 1 -- Get the definition ID
		int definitionID = -1;
		
		if( args.length >= 1){
			try{
				definitionID = Integer.valueOf( args[0] );
			}
			catch( NumberFormatException e ){
				Dialog.getDialog(response, context, data, "The definition ID provided is invalid.", "Definition ID Invalid", DialogType.WARNING);
				return true;
			}
		}
		
		// 2 -- Redirect if the user pressed cancel
		if( "Cancel".equalsIgnoreCase(request.getParameter("Selected")) ){
			response.sendRedirect( StandardViewList.getURL(DefinitionEntryView.VIEW_NAME, definitionID) );
			return true;
		}
		
		// 2 -- Check rights
		try {
			if( Shortcuts.hasRight( context.getSessionInfo(), "System.Configuration.Edit", "Delete definition") == false ){
				context.addMessage("You do not have permission to delete definitions", MessageSeverity.WARNING);
				response.sendRedirect( DefinitionsView.getURL() );
				return true;
			}
		} catch (GeneralizedException e) {
			throw new ViewFailedException(e);
		}
		
		// 3 -- Delete the definition
		if( "Delete".equalsIgnoreCase(request.getParameter("Selected")) ){
			
			try{
				DefinitionArchive archive = DefinitionArchive.getArchive();
				archive.removeByID(definitionID);
				
				Application.getApplication().logEvent(EventLogMessage.Category.DEFINITIONS_DELETED, new EventLogField[]{
						new EventLogField( EventLogField.FieldName.DEFINITION_ID, definitionID ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, context.getUser().getUserName() ),
						new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, context.getUser().getUserID() )} );
				
				context.addMessage("Definition successfully deleted", MessageSeverity.INFORMATION);
				response.sendRedirect( StandardViewList.getURL(DefinitionsView.VIEW_NAME) );
				return true;
			}
			catch( DisallowedOperationException e ){
				Dialog.getDialog(response, context, data, e.getMessage(), "Delete Disallowed", DialogType.WARNING);
				return true;
			} catch (SQLException e) {
				throw new ViewFailedException(e);
			} catch (NoDatabaseConnectionException e) {
				throw new ViewFailedException(e);
			} catch (DefinitionSetLoadException e) {
				throw new ViewFailedException(e);
			} catch (InputValidationException e) {
				throw new ViewFailedException(e);
			}
		}
		
		// 4 -- Show the dialog to confirm the deletion
		else{
			Shortcuts.addDashboardHeaders(request, response, data);
			Dialog.getOptionDialog(response, context, data,"Are you sure you want to delete the definition?", "Confirm Deletion", DialogType.WARNING, new Link("Delete", createURL(definitionID)), new Link("Cancel", createURL(definitionID)));
		}

		return true;
	}

}
