package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
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
			
			// 1 -- Create the page content
			data.put("title", "Definitions with Errors");
			
			//	 1.1 -- Add the Breadcrumbs
			Vector<Link> breadcrumbs = new Vector<Link>();
			breadcrumbs.add(  new Link("Main Dashboard", StandardViewList.getURL("main_dashboard")) );
			breadcrumbs.add(  new Link("Definitions", StandardViewList.getURL("definitions_list")) );
			breadcrumbs.add(  new Link("Definition Errors", createURL()) );
			data.put("breadcrumbs", breadcrumbs);
			
			//	 1.2 -- Add the Menu
			data.put("menu", Menu.getDefinitionMenu(context));
			
			//	 1.3 -- Get the dashboard headers
			Shortcuts.addDashboardHeaders(request, response, data);
			
			// 2 -- Check rights
			try {
				if( Shortcuts.hasRight( context.getSessionInfo(), "System.Configuration.View", "View list of definitions with errors") == false ){
					Shortcuts.getPermissionDeniedDialog(response, data, "You do not have permission to view the definitions with errors");
					return true;
				}
			} catch (GeneralizedException e) {
				throw new ViewFailedException(e);
			}
			
			// 3 -- Get the definitions with errors
			DefinitionErrorList errors = DefinitionErrorList.load(Application.getApplication());
			data.put("errors", errors);
						
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
