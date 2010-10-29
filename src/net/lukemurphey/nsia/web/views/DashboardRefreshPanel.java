package net.lukemurphey.nsia.web.views;

import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.templates.TemplateLoader;

public class DashboardRefreshPanel {
	
	public String getPanel( HttpServletRequest request, Map<String, Object> data, String url, HttpServletResponse response ) throws ViewFailedException{
			
		// Get the refresh rate from the cookie if it exists
		String refreshRate = request.getParameter("refreshRate");
		Cookie[] cookies = request.getCookies();
		
		// Add the cookie if it does not already exist and was provided via the argument
		if( refreshRate != null ){
			response.addCookie(new Cookie("RefreshRate", refreshRate));
		}
		
		// Otherwise, try to find the refresh rate
		else{
			for( int c = 0; c < cookies.length; c++){
				if( cookies[c].getName().matches("RefreshRate") )
					refreshRate = cookies[c].getValue();
			}
		}
		
		// Put the refresh URL in the template 
		data.put("refresh_url", url);
		
		// Assign a default refresh rate
		if( refreshRate == null ){
			refreshRate = "30";
		}
		
		// Put the refresh rate in the template 
		data.put("refresh_rate", refreshRate);
		
		// Put an argument in the list of the view is being refreshed via an AJAX call
		boolean isajax = false;
		
		if( request.getParameter("isajax") != null ){
			isajax = true;
		}
		
		data.put("isajax", isajax);
		
		// Render the template
		return TemplateLoader.renderToString("DashboardRefreshEntry.ftl", data);
	}

}
