package net.lukemurphey.nsia.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class Middleware {

	protected abstract void process( HttpServletRequest request, HttpServletResponse response, RequestContext context ) throws MiddlewareException;
	
}
