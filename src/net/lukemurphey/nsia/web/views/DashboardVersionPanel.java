package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.xmlrpc.XmlRpcException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.VersionManagement;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.templates.TemplateLoader;

public class DashboardVersionPanel extends View {

	public DashboardVersionPanel() {
		super("DashboardPanel/NewVersion", "dashboard_panel_version");
	}
	
	public String getPanel( HttpServletRequest request, Map<String, Object> data, Application app) throws ViewFailedException{
		
		try{
			if( VersionManagement.isNewerVersionAvailableID( true ) ){
				String newestVersion = ApplicationUpdateView.getNewestVersionAvailableID(true);
				data.put("new_version", newestVersion);
				return TemplateLoader.renderToString("DashboardVersionWarning.ftl", data);
			}
		}
		catch(IOException e){
			throw new ViewFailedException(e);
		} catch (XmlRpcException e) {
			throw new ViewFailedException(e);
		}
		
		return null;
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {

		String tasks_panel = getPanel(request, data, Application.getApplication());
		
		if( tasks_panel != null ){
			response.getOutputStream().print(tasks_panel);
		}
		
		return true;
	}

}
