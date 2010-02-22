package net.lukemurphey.nsia.web.views;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.web.RequestContext;
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
	
	@Override
	public boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException {

		data.put("title", "Login Banner");
		
		TemplateLoader.renderToResponse("Base.ftl", data, response);
		
		return false;
	}

}
