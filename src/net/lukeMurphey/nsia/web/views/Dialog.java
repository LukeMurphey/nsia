package net.lukemurphey.nsia.web.views;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.templates.TemplateLoader;

public class Dialog {

	public enum DialogType{
		INFORMATION, WARNING, CRITICAL;
	}
	
	/**
	 * Render a message to the provided response object.
	 * @param response
	 * @param context
	 * @param data
	 * @param message
	 * @param title
	 * @param icon
	 * @throws ViewFailedException
	 */
	public static void getDialog( HttpServletResponse response, RequestContext context, Map<String, Object> data, String message, String title, DialogType type ) throws ViewFailedException{
		String content = getDialog( message, title, type, null );
		data.put("content", content);
		data.put("title", title);
			
		TemplateLoader.renderToResponse("CenteredDialog.ftl", data, response);
	}
	
	/**
	 * 
	 * @param response
	 * @param context
	 * @param data
	 * @param message
	 * @param title
	 * @param type
	 * @param buttons
	 * @throws ViewFailedException
	 */
	public static void getOptionDialog( HttpServletResponse response, RequestContext context, Map<String, Object> data, String message, String title, DialogType type, Link... buttons ) throws ViewFailedException{
		String content = getOptionDialog(  message, title, type, buttons );
		data.put("content", content);
		data.put("title", title);
		data.put("buttons", buttons);
		
		TemplateLoader.renderToResponse("CenteredDialog.ftl", data, response);
	}
	
	/**
	 * Render a message to the provided response object.
	 * @param response
	 * @param context
	 * @param data
	 * @param message
	 * @param title
	 * @param icon
	 * @param suggested
	 * @throws ViewFailedException
	 */
	public static void getDialog( HttpServletResponse response, RequestContext context, Map<String, Object> data, String message, String title, DialogType type, Link suggested ) throws ViewFailedException{

		String content = getDialog( message, title, type, suggested );
		data.put("content", content);
		data.put("title", title);
			
		TemplateLoader.renderToResponse("CenteredDialog.ftl", data, response);
	}
	
	/**
	 * Gets a progress dialog and posts it to the given response object.
	 * @param response
	 * @param context
	 * @param data
	 * @param message
	 * @param title
	 * @param progress
	 * @throws ViewFailedException
	 */
	public static void getProgressDialog( HttpServletResponse response, RequestContext context, Map<String, Object> data, String message, String title, int progress, Link... buttons ) throws ViewFailedException{

		String content = getProgressDialog( message, title, progress, new Link("Cancel", "Cancel") );
		data.put("content", content);
		data.put("title", title);
		data.put("buttons", buttons);
		data.put("progress", Math.min(progress, 100));
		
		TemplateLoader.renderToResponse("CenteredDialog.ftl", data, response);
	}
	
	/**
	 * Gets a progress dialog.
	 * @param message
	 * @param title
	 * @param progress
	 * @param buttons
	 * @return
	 * @throws ViewFailedException
	 */
	public static String getProgressDialog( String message, String title, int progress, Link... buttons ) throws ViewFailedException{
		
		Map<String, Object> data = new HashMap<String, Object>();
		
		data.put("title", title);
		data.put("message", message);
		data.put("buttons", buttons);
		data.put("progress", progress);
		data.put("icon", "/media/img/32_Information.gif");
		
		return TemplateLoader.renderToString("Dialog.ftl", data);
	}

	
	/**
	 * Get a string representing a message box.
	 * @param context
	 * @param data
	 * @param message
	 * @param title
	 * @param icon
	 * @return
	 * @throws ViewFailedException
	 */
	public static String getDialog( String message, String title, DialogType type ) throws ViewFailedException{
		return getDialog( message, title, type, null);
	}
	
	/**
	 * Get a string representing a message box.
	 * @param context
	 * @param data
	 * @param message
	 * @param title
	 * @param icon
	 * @param suggested
	 * @return
	 * @throws ViewFailedException
	 */
	public static String getDialog( String message, String title, DialogType type, Link suggested ) throws ViewFailedException{
		
		Map<String, Object> data = new HashMap<String, Object>();
		
		if( type == DialogType.CRITICAL ){
			data.put("icon", "/32_Alert");
			data.put("warn", true);
		}
		else if( type == DialogType.WARNING ){
			data.put("icon", "/32_Warning");
			data.put("warn", true);
		}
		else{
			data.put("icon", "/32_Information");
		}
		
		data.put("title", title);
		data.put("message", message);
		
		if( suggested != null ){
			data.put("suggested", suggested);
		}

		return TemplateLoader.renderToString("Dialog.ftl", data);
	}
	
	/**
	 * Gets a dialog that displays multiple options for the user.
	 * @param message
	 * @param title
	 * @param type
	 * @param buttons
	 * @return
	 * @throws ViewFailedException
	 */
	public static String getOptionDialog( String message, String title, DialogType type, Link... buttons) throws ViewFailedException{
		
		Map<String, Object> data = new HashMap<String, Object>();
		
		if( type == DialogType.CRITICAL ){
			data.put("icon", "/32_Alert");
		}
		else if( type == DialogType.WARNING ){
			data.put("icon", "/32_Warning");
		}
		else{
			data.put("icon", "/32_Information");
		}
		
		data.put("title", title);
		data.put("message", message);
		data.put("buttons", buttons);

		return TemplateLoader.renderToString("Dialog.ftl", data);
	}

}
