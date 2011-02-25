package net.lukemurphey.nsia.web.middleware;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.SessionStatus;
import net.lukemurphey.nsia.web.ClientAbortException;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.views.LoginView;

public class AuthenticationMiddleware extends Middleware {

	@Override
	public boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context)
			throws MiddlewareException {

		// 1 -- Construct an empty request context if one was not already created
		if( context == null || context.getSessionInfo().getSessionStatus() != SessionStatus.SESSION_ACTIVE ){
			//context = new RequestContext(session_messages);
			
			//Show the login form since the user has failed to login yet
			View view = new LoginView();
			response.setStatus(401); //Set the HTTP response code to 401 so that it can be programmatically determined that authentication is necessary to complete the request
			try {
				view.process(request, response, context, true);
			} catch (ViewFailedException e) {
				throw new MiddlewareException(e);
			} catch (ClientAbortException e) {
				// The connection failed to the client. This is likely due to transient network problem and can be safely ignored.
			}
			
			return true;
		}

		//Fall through, the user is authenticated
		return false;
	}

}
