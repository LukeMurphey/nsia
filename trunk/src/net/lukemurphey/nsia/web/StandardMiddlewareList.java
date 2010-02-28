package net.lukemurphey.nsia.web;

import java.util.Vector;

public class StandardMiddlewareList {

	private static Vector<Middleware> middleware = null;
	
	public static synchronized Vector<Middleware> getMiddleware(){
		
		if( middleware == null ){
			middleware = new Vector<Middleware>();
			middleware.add( new SessionActivityMiddleware() );
		}

		return middleware;
	}
}
