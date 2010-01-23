package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.templates.TemplateLoader;

public class MessageBoxView extends View {
	
	public MessageBoxView() {
		super("MessageBox", "message_box");
	}

	@Override
	protected boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException, URLInvalidException, IOException, ViewNotFoundException {
		// TODO Auto-generated method stub
		return false;
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
	public void getMessageBox( HttpServletResponse response, RequestContext context, Map<String, Object> data, String message, String title, String icon ) throws ViewFailedException{
		try {
			response.getOutputStream().print( getMessageBox( context, data, message, title, icon ) );
		} catch (IOException e) {
			throw new ViewFailedException(e);
		}
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
	public String getMessageBox( RequestContext context, Map<String, Object> data, String message, String title, String icon ) throws ViewFailedException{
		
		data.put("alignCenter", true);
		data.put("icon", icon);
		data.put("title", title);
		data.put("message", message);

		return TemplateLoader.renderToString("MessageBox.ftl", data);
	}

}
