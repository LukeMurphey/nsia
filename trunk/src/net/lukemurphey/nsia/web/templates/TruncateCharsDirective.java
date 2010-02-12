package net.lukemurphey.nsia.web.templates;

import java.io.IOException;
import java.io.Writer;
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

public class TruncateCharsDirective implements TemplateDirectiveModel{

	public static final String PARAM_STRING_LENGTH = "length";
	public static final String PARAM_STRING_VALUE = "string";
	
	@SuppressWarnings("unchecked")
	@Override
	public void execute(Environment env, Map params, TemplateModel[] model, TemplateDirectiveBody body) throws TemplateException, IOException {

		// 1 -- Get the arguments
		Iterator<Entry<String, TemplateModel>> paramIter = params.entrySet().iterator();
		int string_length = 32;
		
        while (paramIter.hasNext()) {
            Map.Entry<String, TemplateModel> ent = paramIter.next();
            
            String paramName = ent.getKey();
            TemplateModel paramValue = ent.getValue();
            
            if (paramName.equals(PARAM_STRING_LENGTH)) {
            	if ( (paramValue instanceof TemplateNumberModel) == true) {
            		string_length = ((TemplateNumberModel)paramValue).getAsNumber().intValue();
            	}
            }
        }
        
        // 2 -- Make sure the length is valid
        if( string_length <= 0 ){
        	throw new TemplateModelException("String length must be greater than zero");
        }
        
        // 3 -- Truncate the string as necessary
        if (body != null) {
        	body.render( new TruncateWriter( env.getOut(), string_length ) );
        }
	}
	
    /**
     * A {@link Writer} that truncates the character stream to the size specified {@link Writer}.
     */ 
    private static class TruncateWriter extends Writer {
       
        private final Writer out;
        private int max_length;
        private boolean add_ellipses;
        private boolean reached_end = false;
        
        public TruncateWriter (Writer out, int max_length) {
            this.out = out;
            
            if( max_length > 3 ){
            	add_ellipses = true;
            	this.max_length = max_length - 3;
            }
            else{
            	this.max_length = max_length;
            	add_ellipses = false;
            }
        }

        public void write(char[] cbuf, int off, int len) throws IOException {
        	
        	// 1 -- Make sure that we still have output to provide
        	if( reached_end || off > max_length ){
        		return;
        	}
        	
        	// 2 -- Truncate the string
            char[] transformedCbuf;
            boolean hit_end;
            
            //	 2.1 -- Determine the correct size for the character array
            if( (off + len) > max_length){
            	if(add_ellipses == true){
            		transformedCbuf = new char[max_length-off+3];
            	}
            	else{
            		transformedCbuf = new char[max_length-off];
            	}
            	hit_end = true;
            }
            else{
            	transformedCbuf = new char[len];
            	hit_end = false;
            }
            
            //	 2.2 -- Output the result
            if( hit_end && add_ellipses ){
            	for (int i = 0; i < transformedCbuf.length-3; i++) { //Make room for the ellipses
                    transformedCbuf[i] = cbuf[i + off];
                }
            	
            	transformedCbuf[transformedCbuf.length-3] = '.';
            	transformedCbuf[transformedCbuf.length-2] = '.';
            	transformedCbuf[transformedCbuf.length-1] = '.';
            	
            	reached_end = true;
            }
            else{
            	for (int i = 0; i < transformedCbuf.length; i++) {
                    transformedCbuf[i] = cbuf[i + off];
                }
            }
            
            out.write(transformedCbuf);
        }

        public void flush() throws IOException {
            out.flush();
        }

        public void close() throws IOException {
            out.close();
        }
    }

}

