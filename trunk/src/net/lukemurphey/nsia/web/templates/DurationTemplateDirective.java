package net.lukemurphey.nsia.web.templates;

import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;

public class DurationTemplateDirective implements TemplateDirectiveModel{

	private static final String PARAM_TIME_SECS = "seconds";
	
	@SuppressWarnings("unchecked")
	public void execute(Environment env, Map params, TemplateModel[] model, TemplateDirectiveBody body) throws TemplateException, IOException {
		
		// 1 -- Get the arguments
		Iterator<Entry<String, TemplateModel>> paramIter = params.entrySet().iterator();
		int time_secs = 0;
		boolean time_provided = false;
		
        while (paramIter.hasNext()) {
        	
            Map.Entry<String, TemplateModel> ent = paramIter.next();
            
            String paramName = ent.getKey();
            TemplateModel paramValue = ent.getValue();
            
            if (paramName.equals(PARAM_TIME_SECS)) {
            	if ( (paramValue instanceof TemplateNumberModel) == false) {
            		time_secs = ((TemplateNumberModel)paramValue).getAsNumber().intValue();
            		time_provided = true;
            	}
            }
        }
        
        // 2 -- Make sure the duration value was provided
        if( time_provided == false ){
        	throw new TemplateModelException("Duration was not provided");
        }
		
        // 3 -- Update the template text
        Writer out = env.getOut();
        
        if (body != null) {
        	out.write(getTimeDescription(time_secs));
        }
		
	}
	
	private static String getTimeDescription( long secs ){
		double doubleSecs = secs;
		DecimalFormat twoPlaces = new DecimalFormat("0.00");
		
		if( secs < 60 )
			return secs + " sec";
		else if ( secs < 3600 )
			return twoPlaces.format( doubleSecs/60 ) + " min";
		else if ( secs < 86400 )
			return twoPlaces.format( doubleSecs/3600 ) + " hours";
		else
			return twoPlaces.format( doubleSecs/86400 ) + " days";
	}
	
}
