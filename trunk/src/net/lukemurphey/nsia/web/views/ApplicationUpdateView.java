package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.xmlrpc.XmlRpcException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.ApplicationVersionDescriptor;
import net.lukemurphey.nsia.VersionManagement;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.rest.RESTRequestFailedException;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.Menu;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.templates.TemplateLoader;

public class ApplicationUpdateView extends View {

	public static final String VIEW_NAME = "application_update";
	
	public ApplicationUpdateView() {
		super("System/Update", VIEW_NAME);
	}

	public static String getURL() throws URLInvalidException{
		ApplicationUpdateView view = new ApplicationUpdateView();
		return view.createURL();
	}
	
	public static String getNewestVersionAvailableID( boolean dontBlock){
		try {
			ApplicationVersionDescriptor versionInfo = VersionManagement.getNewestVersionAvailableID( dontBlock );
			
			if( versionInfo != null ){
				return versionInfo.toString();
			}
		} catch (RESTRequestFailedException e) {
			Application.getApplication().logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e );
		} catch (IOException e) {
			Application.getApplication().logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e );
		}
		
		return null;
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {
		
		
		
		data.put("title", "Version Update");
		
		Vector<Link> breadcrumbs = new Vector<Link>();
		breadcrumbs.add( new Link("Main Dashboard", MainDashboardView.getURL()) );
		breadcrumbs.add( new Link("System Status", SystemStatusView.getURL()) );
		breadcrumbs.add( new Link("Application Update", ApplicationUpdateView.getURL() ) );
		data.put("breadcrumbs", breadcrumbs);
		
		data.put("menu", Menu.getSystemMenu(context));
		Shortcuts.addDashboardHeaders(request, response, data);
		
		data.put("title", "Version Update");
		
		try{
			data.put("is_newer", VersionManagement.isNewerVersionAvailable( true ));
			data.put("new_version", getNewestVersionAvailableID(true));
		}
		catch(RESTRequestFailedException e){
			//Ignore, dialog will display a warning because the version could not be obtained
			data.put("is_newer", true);
		}
		
		TemplateLoader.renderToResponse("ApplicationUpdate.ftl", data, response);
		return true;
	}

}
