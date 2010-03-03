package net.lukemurphey.nsia.web.middleware;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.web.RequestContext;

public abstract class Middleware {

	public abstract boolean process( HttpServletRequest request, HttpServletResponse response, RequestContext context ) throws MiddlewareException;
	
}
