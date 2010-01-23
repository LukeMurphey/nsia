package net.lukemurphey.nsia.web.views;

import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.templates.TemplateLoader;

public class DashboardRefreshPanel {
	
	public String getPanel( HttpServletRequest request, Map<String, Object> data, String url, HttpServletResponse response ) throws ViewFailedException{
			
		String refreshRate = request.getParameter("refreshRate");
		Cookie[] cookies = request.getCookies();
		
		if( refreshRate != null ){
			response.addCookie(new Cookie("RefreshRate", refreshRate));
		}
		else{
			for( int c = 0; c < cookies.length; c++){
				if( cookies[c].getName().matches("RefreshRate") )
					refreshRate = cookies[c].getValue();
			}
		}
		
		data.put("refresh_url", url);
		
		if( refreshRate == null ){
			refreshRate = "60";
		}
		
		data.put("refresh_rate", refreshRate);		
		
		return TemplateLoader.renderToString("DashboardRefreshEntry.ftl", data);
	}

}
