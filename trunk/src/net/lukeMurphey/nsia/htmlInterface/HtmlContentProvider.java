package net.lukeMurphey.nsia.htmlInterface;


public class HtmlContentProvider {
	public static final String SERVER_STRING = "ThreatFactor NSIA 1.0";
	
	// The following determine the method used to generate the call to perform the request
	public static final int METHOD_GET = 0;
	public static final int METHOD_POST = 1;
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws Exception{
		return null;
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws Exception{
		return getHtml( requestDescriptor, null );
	}
	
	
}
