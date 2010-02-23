package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.scan.DefinitionErrorList;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.Menu;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.StandardViewList;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.templates.TemplateLoader;

public class DefinitionErrorsView extends View {

	public static final String VIEW_NAME = "definitions_errors";
	
	public DefinitionErrorsView() {
		super("Definitions/Errors", VIEW_NAME);
	}

	public static String getURL() throws URLInvalidException{
		DefinitionErrorsView view = new DefinitionErrorsView();
		return view.createURL();
	}
	
	@Override
	protected boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException, URLInvalidException, IOException, ViewNotFoundException {
		
		try{
			// 1 -- Check rights
			//checkRight( context.getSessionInfo(), "System.Configuration.View");
			
			
			// 2 -- Get the definitions with errors
			DefinitionErrorList errors = DefinitionErrorList.load(Application.getApplication());
			data.put("errors", errors);
			
			
			// 3 -- Render the page with the definitions with errors
			data.put("title", "Definitions with Errors");
			
			//	 3.1 -- Add the Breadcrumbs
			Vector<Link> breadcrumbs = new Vector<Link>();
			breadcrumbs.add(  new Link("Main Dashboard", StandardViewList.getURL("main_dashboard")) );
			breadcrumbs.add(  new Link("Definitions", StandardViewList.getURL("definitions_list")) );
			breadcrumbs.add(  new Link("Definition Errors", createURL()) );
			data.put("breadcrumbs", breadcrumbs);
			
			//	 3.2 -- Add the Menu
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
			
			menu.add( new Link("Definitions") );
			menu.add( new Link("Update Now", StandardViewList.getURL(DefinitionsUpdateView.VIEW_NAME)) );
			menu.add( new Link("Create New Definition", StandardViewList.getURL(DefinitionEntryView.VIEW_NAME, "New")));
			menu.add( new Link("Import Definitions", StandardViewList.getURL(DefinitionsImportView.VIEW_NAME) ));
			menu.add( new Link("Export Custom Definitions", StandardViewList.getURL(DefinitionsExportView.VIEW_NAME) ));
			menu.add( new Link("Edit Default Policy", StandardViewList.getURL(DefinitionPolicyView.VIEW_NAME) ));
			
			data.put("menu", Menu.getDefinitionMenu(context));
			
			//	 3.3 -- Get the dashboard headers
			Shortcuts.addDashboardHeaders(request, response, data);
			
			TemplateLoader.renderToResponse("DefinitionErrors.ftl", data, response);
			
			return true;
		}
		catch(SQLException e){
			throw new ViewFailedException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		}
	}

}
