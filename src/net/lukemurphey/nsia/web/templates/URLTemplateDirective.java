package net.lukemurphey.nsia.web.templates;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import net.lukemurphey.nsia.web.StandardViewList;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.ViewNotFoundException;

import freemarker.core.Environment;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;

public class URLTemplateDirective implements TemplateDirectiveModel{

	public static final String PARAM_VIEW_NAME = "name";
	public static final String PARAM_ARGS = "args";
	
	@SuppressWarnings("unchecked")
	public void execute(Environment env, Map params, TemplateModel[] model, TemplateDirectiveBody body) throws TemplateException, IOException {

		// 1 -- Get the arguments
		Iterator<Entry<String, TemplateModel>> paramIter = params.entrySet().iterator(); //<String, TemplateModel>
		String view_name = null;
		Vector<String> view_args = new Vector<String>();
		
        while (paramIter.hasNext()) {
            Map.Entry<String, TemplateModel> ent = paramIter.next();
            
            String paramName = ent.getKey();
            TemplateModel paramValue = ent.getValue();
            
            if (paramName.equals(PARAM_VIEW_NAME)) {
            	
            	view_name = paramValue.toString();
            	
            }
            else if (paramName.equals(PARAM_ARGS)) {
            	
            	if ( paramValue instanceof TemplateCollectionModel ) {
            		TemplateCollectionModel collection = ((TemplateCollectionModel)paramValue);
                	
                	TemplateModelIterator i = collection.iterator();
                	
                	while(i.hasNext()){
                		view_args.add(i.next().toString());
                	}
            	}
            	else if ( paramValue instanceof SimpleSequence ) {
            		List<Object> collection = ((SimpleSequence)paramValue).toList();
                	
                	Iterator<Object> i = collection.iterator();
                	
                	while(i.hasNext()){
                		view_args.add(i.next().toString());
                	}
            	}
            	else{
            		throw new TemplateModelException("The \"" + PARAM_ARGS + "\" parameter must be a collection or a simple sequence.");
            	}
            	
            	
            }
        }
        
        // 2 -- Make sure the name argument was provided
        if( view_name == null ){
        	throw new TemplateModelException("View name was not provided");
        }
        
        // 3 -- Get the URL
        Object[] args_array = new String[view_args.size()];
        view_args.toArray(args_array);
        String url;
        
        try {
			url = StandardViewList.getURL(view_name, args_array);
		} catch (URLInvalidException e) {
			throw new TemplateModelException("The URL constructed for view \"" + view_name + "\" is invalid.");
		} catch (ViewNotFoundException e) {
			throw new TemplateModelException("View \"" + view_name + "\" was not found.");
		}
        
        
        // 4 -- Update the template text
        Writer out = env.getOut();
        out.write(url);
	}

}
