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
		Template template = TemplateLoader.getTemplate(templateName);
		PrintWriter writer;
		
		try {
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
