package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.scan.DefinitionArchive;
import net.lukemurphey.nsia.scan.DefinitionSetLoadException;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;

public class DefinitionsExportView extends View {

	public static final String VIEW_NAME = "definitions_export";
	
	public DefinitionsExportView() {
		super("Definitions/Export", VIEW_NAME);
	}
	
	public static String getURL() throws URLInvalidException{
		DefinitionsExportView view = new DefinitionsExportView();
		return view.createURL();
	}

	@Override
	protected boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException, URLInvalidException, IOException, ViewNotFoundException {

		// 1 -- Check rights
		try {
			if( Shortcuts.hasRight( context.getSessionInfo(), "System.Configuration.View", "Export definitions") == false ){
				context.addMessage("You do not have permission to export definitions", MessageSeverity.WARNING);
				response.sendRedirect( DefinitionsView.getURL() );
				return true;
			}
		} catch (GeneralizedException e) {
			throw new ViewFailedException(e);
		}
		
		// 2 -- Export the definitions
		Application.getApplication().logEvent(EventLogMessage.EventType.DEFINITIONS_EXPORTED, new EventLogField[]{
				new EventLogField( EventLogField.FieldName.SOURCE_USER_NAME, context.getSessionInfo().getUserName() ),
				new EventLogField( EventLogField.FieldName.SOURCE_USER_ID, context.getSessionInfo().getUserId() )} );
		
		try{
			DefinitionArchive archive = DefinitionArchive.getArchive();
			
			response.getOutputStream().print(archive.getAsXML( true ));
			response.setHeader("Content-Type", "text/xml");
			response.setHeader("Content-disposition", "attachment; definitions.xml");
		}
		catch( DefinitionSetLoadException e ){
			throw new ViewFailedException(e);
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		} catch (InputValidationException e) {
			throw new ViewFailedException(e);
		}
		
		return true;
	}

}
