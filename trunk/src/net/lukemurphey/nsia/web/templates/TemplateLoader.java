package net.lukemurphey.nsia.web.templates;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.web.ViewFailedException;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class TemplateLoader {

	private static Configuration config = null;
	
	public static synchronized Configuration getConfig(){
		if( config == null ){
			config = new Configuration();
			config.setSharedVariable("url", new URLTemplateDirective());
			config.setSharedVariable("truncate_chars", new TruncateCharsDirective());
			config.setSharedVariable("dialog", new DialogTemplateDirective());
			config.setClassForTemplateLoading(TemplateLoader.class, "");
		}
		
		return config;
		
	}
	
	public static String renderToString(String templateName, Map<String, Object> data) throws ViewFailedException{
		Template template = TemplateLoader.getTemplate(templateName);
		StringWriter writer = new StringWriter();
		
		try {
			template.process(data, writer);
		} catch (IOException e) {
			throw new ViewFailedException("Exception thrown while rendering template", e);
		} catch (TemplateException e) {
			throw new ViewFailedException("Exception thrown while rendering template", e);
		}
		
		writer.flush();
		return writer.getBuffer().toString();
	}
	
	public static void renderToResponse(String templateName, Map<String, Object> data, HttpServletResponse response) throws ViewFailedException{
		renderToResponse(templateName, data, response, null);
	}
	
	public static void renderToResponse(String templateName, Map<String, Object> data, HttpServletResponse response, String content_type) throws ViewFailedException{
		Template template = TemplateLoader.getTemplate(templateName);
		PrintWriter writer;
		
		try {
			
			//Set the content-type if provided
			if( content_type != null && response.isCommitted() == false ){
				response.setContentType(content_type);
			}
			
			// Set the content-type to HTML if no content-type was provided and the content-type was not set
			else if( content_type == null && response.getContentType() == null  && response.isCommitted() == false ){
				response.setContentType("text/html");
			}
			
			writer = response.getWriter();
			template.process(data, writer);
			
		} catch (IOException e) {
			throw new ViewFailedException("Exception thrown while rendering template", e);
		} catch (TemplateException e) {
			throw new ViewFailedException("Exception thrown while rendering template", e);
		}
		
		writer.flush();  
	}
	
	public static Template getTemplate(String name) throws ViewFailedException{
		try {
			return getConfig().getTemplate(name);
		} catch (IOException e) {
			throw new ViewFailedException("Template named \"" + name + "\" could not be found", e);
		}
	}
	
}
