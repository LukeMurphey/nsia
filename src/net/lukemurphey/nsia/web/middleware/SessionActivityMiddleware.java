package net.lukemurphey.nsia.web.middleware;

import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.SessionManagement;
import net.lukemurphey.nsia.SessionStatus;
import net.lukemurphey.nsia.SessionManagement.SessionInfo;
import net.lukemurphey.nsia.web.RequestContext;

public class SessionActivityMiddleware extends Middleware {

	@Override
	public boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context) throws MiddlewareException {
		
		// 1 -- Determine if the context is related to a session
		SessionInfo sessionInfo = context.getSessionInfo();
		
		if( sessionInfo != null && sessionInfo.getSessionStatus() == SessionStatus.SESSION_ACTIVE ){

			// 2 -- Determine if the call was due to a script refreshing a view
			if( request.getParameter("refreshRate") != null ){
				return false;
			}
			
			// 3 -- Reset session activity
			SessionManagement sessionManagement = new SessionManagement( Application.getApplication() );
			
			try {
				sessionManagement.resetSessionActivity( context.getSessionInfo().getSessionIdentifier() );
			} catch (InputValidationException e) {
				throw new MiddlewareException("Exception thrown when reset session activity", e);
			} catch (NoDatabaseConnectionException e) {
				throw new MiddlewareException("Exception thrown when reset session activity", e);
			} catch (SQLException e) {
				throw new MiddlewareException("Exception thrown when reset session activity", e);
			}
		}
		
		return false;
	}
}
