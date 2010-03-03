package net.lukemurphey.nsia.web.middleware;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.views.LoginBannerView;
import net.lukemurphey.nsia.web.views.MainDashboardView;

public class LoginBannerMiddleware extends Middleware {

	@Override
	public boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context)
			throws MiddlewareException {

		// 1 -- Determine if the banner has already been accepted
		if( request.getCookies() != null ){
			for (Cookie cookie : request.getCookies()) {
				if( "BannerCheck".equalsIgnoreCase( cookie.getName() ) && "Accept".equalsIgnoreCase( cookie.getValue() ) ){
					return false;
				}
			}
		}
		
		// 2 -- Determine if the application has a banner to show
		String banner;
		try {
			banner = Application.getApplication().getApplicationConfiguration().getLoginBanner();
		} catch (NoDatabaseConnectionException e1) {
			throw new MiddlewareException(e1);
		} catch (SQLException e1) {
			throw new MiddlewareException(e1);
		} catch (InputValidationException e1) {
			throw new MiddlewareException(e1);
		}
		
		if( banner == null ){ //No banner to show, just continue
			return false;
		}
		
		// 3 -- Set the cookie if appropriate
		if( "Accept".equalsIgnoreCase( request.getParameter("BannerCheck") ) ){
			Cookie cookie = new Cookie("BannerCheck", "Accept");
			cookie.setPath("/");
			response.addCookie( cookie );
			
			try{
				if( request.getParameter("ReturnTo") != null ){
					response.sendRedirect(request.getParameter("ReturnTo"));
					return true;
				}
				/*else if( !LoginBannerView.getURL().equalsIgnoreCase( request.getPathInfo()) ){
					return false;
				}*/
				else {
					response.sendRedirect(MainDashboardView.getURL());
					return true;
				}
				
				
			} catch(IOException e){
				throw new MiddlewareException(e);
			} catch(URLInvalidException e){
				throw new MiddlewareException(e);
			}
		}
		
		// 4 -- Execute the login banner view
		LoginBannerView view = new LoginBannerView();
		try {
			return view.process(request, response, context);
		} catch (ViewFailedException e) {
			throw new MiddlewareException("Exception thrown while attempting to generate login banner view", e);
		}
	}

}
