package net.lukemurphey.nsia.web.templates;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.views.Dialog;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

public class DialogTemplateDirective implements TemplateDirectiveModel{

	public static final String PARAM_MESSAGE = "message";
	public static final String PARAM_TITLE = "title";
	public static final String PARAM_TYPE = "type";
	
	@SuppressWarnings("unchecked")
	@Override
	public void execute(Environment env, Map params, TemplateModel[] model, TemplateDirectiveBody body) throws TemplateException, IOException {

		// 1 -- Get the arguments
		Iterator<Entry<String, TemplateModel>> paramIter = params.entrySet().iterator(); //<String, TemplateModel>
		String message = null;
		String title = null;
		String type_str = null;
		
        while (paramIter.hasNext()) {
            Map.Entry<String, TemplateModel> ent = paramIter.next();
            
            String paramName = ent.getKey();
            TemplateModel paramValue = ent.getValue();
            
            if (paramName.equals(PARAM_MESSAGE)) {
            	message = paramValue.toString();
            }
            else if (paramName.equals(PARAM_TITLE)) {
            	title = paramValue.toString();
            }
            else if (paramName.equals(PARAM_TYPE)) {
            	type_str = paramValue.toString();
            }
        }
        
        // 2 -- Make sure the arguments were provided
        if( message == null ){
        	throw new TemplateModelException("Dialog message was not provided");
        }
        
        if( title == null ){
        	throw new TemplateModelException("Dialog title was not provided");
        }
        
        DialogType type = DialogType.INFORMATION;
        if( type_str == null ){
        	type = DialogType.INFORMATION;
        }
        else if( type_str.equalsIgnoreCase("Info") || type_str.equalsIgnoreCase("Information") || type_str.equalsIgnoreCase("Informational") ){
        	type = DialogType.INFORMATION;
        }
        else if( type_str.equalsIgnoreCase("Warn") || type_str.equalsIgnoreCase("Warning")){
        	type = DialogType.WARNING;
        }
		else if( type_str.equalsIgnoreCase("Crit") || type_str.equalsIgnoreCase("Critical") ){
			type = DialogType.CRITICAL;
		}
        else{
        	throw new TemplateModelException("Dialog type is not valid");
        }
        
        // 3 -- Get the Dialog
        String dialog;
		try {
			dialog = Dialog.getDialog( message, title, type );
		} catch (ViewFailedException e) {
			throw new TemplateModelException("Dialog could not be generated", e);
		}
        
        // 4 -- Update the template text
        Writer out = env.getOut();
        out.write(dialog);
	}

}
