package net.lukeMurphey.nsia.htmlInterface;


import org.apache.commons.lang.StringEscapeUtils;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.WorkerThread;
import net.lukeMurphey.nsia.Application.WorkerThreadDescriptor;
import net.lukeMurphey.nsia.eventLog.EventLogField;
import net.lukeMurphey.nsia.eventLog.EventLogMessage;
import net.lukeMurphey.nsia.eventLog.EventLogField.FieldName;
import net.lukeMurphey.nsia.eventLog.EventLogMessage.Category;

public class HtmlAjax extends HtmlContentProvider{
	
	public static ContentDescriptor getHtml( WebConsoleConnectionDescriptor requestDescriptor ){
		return getHtml( requestDescriptor, null);
	}
	
	public static ContentDescriptor getHtml( WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor action ){
		
		try{
			// 1 -- Split up the path
			String[] path = requestDescriptor.request.getRequestURI().substring(1).split("[\\\\/]");
			
			// Decode each portion before continuing
			for (int c = 0; c < path.length; c++) {
				path[c] = java.net.URLDecoder.decode(path[c], "ASCII");
			}
			
			String module = null;
		
			// 2 -- Make sure that path is valid
			if( path == null || path.length < 2 ){
				return new ContentDescriptor("", "Path provided is invalid (missing module identifier)", false);
			}
			else{
				module = path[1];
			}
				
			// 3 -- Determine which of the AJAX code generators need to be executed
			StringBuffer buffer = new StringBuffer();
			buffer.append("<?xml version=\"1.0\" ?>");
			
			//	 3.1 -- Task descriptor
			if( module.equalsIgnoreCase("Task") && path.length >= 3 ){
				String taskID = path[2];
				
				WorkerThreadDescriptor desc = Application.getApplication().getWorkerThread(taskID);
				
				if(desc == null){
					return new ContentDescriptor("", "No task found with the given ID", false);
				}
				
				WorkerThread thread = desc.getWorkerThread();
				
				if(thread == null){
					return new ContentDescriptor("", "No thread associated with the task", false);
				}
				
				// Create the XML response
				buffer.append("<Task>");
				buffer.append("<Progress>").append( thread.getProgress() ).append("</Progress>");
				buffer.append("<StatusDescription>").append( StringEscapeUtils.escapeXml( thread.getStatusDescription() ) ).append("</StatusDescription>");
				buffer.append("<State>").append( StringEscapeUtils.escapeXml( thread.getStatus().toString() ) ).append("</State>");
				buffer.append("</Task>");
				
				requestDescriptor.response.setContentType("text/xml");
				return new ContentDescriptor("", buffer,false);
			}
			
			
			// 4 -- No generators selected
			return new ContentDescriptor("", "No AJAX XML generator matched the arguments provided", false);
		}
		catch(Exception e){
			Application.getApplication().logExceptionEvent( new EventLogMessage(Category.INTERNAL_ERROR, new EventLogField( FieldName.MESSAGE, "Exception caught when processing ajax request")), e);
			return new ContentDescriptor("", "An error occured while generating the response to the request", false);
		}
	}

}
