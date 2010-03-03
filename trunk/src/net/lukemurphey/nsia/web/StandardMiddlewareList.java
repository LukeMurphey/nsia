package net.lukemurphey.nsia.web;

import java.util.Vector;

import net.lukemurphey.nsia.web.middleware.AuthenticationMiddleware;
import net.lukemurphey.nsia.web.middleware.LoginBannerMiddleware;
import net.lukemurphey.nsia.web.middleware.Middleware;
import net.lukemurphey.nsia.web.middleware.SessionActivityMiddleware;

public class StandardMiddlewareList {

	private static Vector<Middleware> middleware = null;
	
	public static synchronized Vector<Middleware> getMiddleware(){
		
		if( middleware == null ){
			middleware = new Vector<Middleware>();
			middleware.add( new LoginBannerMiddleware() );
			middleware.add( new AuthenticationMiddleware() );
			middleware.add( new SessionActivityMiddleware() );
		}

		return middleware;
	}
}
