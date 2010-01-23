package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.scan.DefinitionErrorList;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.templates.TemplateLoader;

public class DashboardDefinitionErrorsPanel extends View {

	public DashboardDefinitionErrorsPanel() {
		super("DashboardPanel/DefinitionErrors", "dashboard_panel_definition_errors");
	}

	@Override
	protected boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException, URLInvalidException, IOException, ViewNotFoundException {
		
		String output = getPanel( request, data, Application.getApplication());
		
		if( output != null ){
			response.getOutputStream().println( getPanel( request, data, Application.getApplication()) );
		}
		
		return true;
	}
	
	public String getPanel( HttpServletRequest request, Map<String, Object> data, Application app) throws ViewFailedException{
		
		// 1 -- Determine if definitions with errors were noted
		boolean errors_noted;
		
		try {
			errors_noted = DefinitionErrorList.errorsNoted(app);
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		}
		
		// 2 -- Create the panel
		if(errors_noted){
			return TemplateLoader.renderToString("DashboardDefinitionsErrors.ftl", data);
		}
		else{
			return null; // Panel should not be shown
		}
	}

}
