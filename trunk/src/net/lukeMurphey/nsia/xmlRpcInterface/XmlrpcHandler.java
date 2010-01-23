package net.lukemurphey.nsia.xmlRpcInterface;

import net.lukemurphey.nsia.Application;


/**
 * This class contains various fields and methods that are common to various classes that makeup the XML-RPC interface to the underyling
 * classes. 
 * @author luke
 *
 */

abstract public class XmlrpcHandler {
	
	protected Application appRes;
	protected static final String EMPTY_STRING = "";
	
	public XmlrpcHandler(Application appRes){
		
		// 0 -- Precondition check
		if( appRes == null )
			throw new IllegalArgumentException("The application resources cannot be null");
		
		// 1 -- Set the parameters
		this.appRes = appRes;
	}
}
