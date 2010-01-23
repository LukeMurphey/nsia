package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.Application.WorkerThreadDescriptor;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.templates.TemplateLoader;

public class DashboardTasksPanel extends View {

	public DashboardTasksPanel() {
		super("DashboardPanel/Tasks", "7");
	}

	@Override
	protected boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException, URLInvalidException, IOException, ViewNotFoundException {
		
		String tasks_panel = getPanel(request, data, Application.getApplication());
		
		if( tasks_panel != null ){
			response.getOutputStream().print(tasks_panel);
		}
		
		return true;
	}
	
	/**
	 * Get a string with HTML for the tasks panel.
	 * @param request
	 * @param data
	 * @param app
	 * @return
	 * @throws ViewFailedException
	 */
	public String getPanel( HttpServletRequest request, Map<String, Object> data, Application app) throws ViewFailedException{
		
		WorkerThreadDescriptor[] threads = app.getWorkerThreadQueue(true);
		
		data.put("tasks", threads);
		
		if( threads.length > 0){
			return TemplateLoader.renderToString("DashboardTasksHeader.ftl", data);
		}
		else{
			return null;
		}
	}

}
