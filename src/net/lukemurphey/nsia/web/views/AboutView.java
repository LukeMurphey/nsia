package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

public class AboutView  extends View{

	public static final String VIEW_NAME = "about";
	
	public AboutView() {
		super("About", VIEW_NAME);
	}

	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {
		
	
		//Breadcrumbs
		Vector<Link> breadcrumbs = new Vector<Link>();
		breadcrumbs.add(  new Link("Main Dashboard", StandardViewList.getURL("main_dashboard")) );
		breadcrumbs.add(  new Link("About NSIA", StandardViewList.getURL("about")) );
		data.put("breadcrumbs", breadcrumbs);
		
		//Menu
		data.put("menu", Menu.getSystemMenu(context));
		
		//Get the dashboard headers
		Shortcuts.addDashboardHeaders(request, response, data, createURL());
		
    	data.put("title", "About NSIA");
    	
    	TemplateLoader.renderToResponse("About.ftl", data, response);
    	return true;
	}

}
