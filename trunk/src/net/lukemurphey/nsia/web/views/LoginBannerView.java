package net.lukemurphey.nsia.web.views;

import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.templates.TemplateLoader;

public class LoginBannerView extends View {

	public LoginBannerView() {
		super("Login/Banner", "login_banner");
	}
	
	public static String getURL() throws URLInvalidException{
		LoginBannerView view = new LoginBannerView();
		return view.createURL();
	}
	
	public boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context) throws ViewFailedException {
		return processInternal( request, response, context, new String[0], null );
	}
	
	@Override
	public boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException {
		return processInternal( request, response, context, args, data );
	}
	
	private boolean processInternal(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException {
		
		if( data == null ){
			data = Shortcuts.getMapWithBasics(context, request);
		}
		
		Vector<String> headers = new Vector<String>();
		headers.add(DashboardPreLoginPanel.getPanel(request, data));
		
		data.put("dashboard_headers", headers);
		data.put("show_splitter_border", false);
		
		data.put("title", "Login Banner");
		
		try {
			data.put("login_banner", Application.getApplication().getApplicationConfiguration().getLoginBanner());
		} catch (NoDatabaseConnectionException e) {
			throw new ViewFailedException(e);
		} catch (SQLException e) {
			throw new ViewFailedException(e);
		} catch (InputValidationException e) {
			throw new ViewFailedException(e);
		}
		
		TemplateLoader.renderToResponse("LoginBanner.ftl", data, response);
		
		return true;
	}

}
