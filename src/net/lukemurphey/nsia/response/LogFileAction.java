package net.lukemurphey.nsia.response;

import java.io.*;

import java.util.Hashtable;
import java.util.Vector;



import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.extension.ArgumentFieldsInvalidException;
import net.lukemurphey.nsia.extension.FieldLayout;
import net.lukemurphey.nsia.extension.FieldText;
import net.lukemurphey.nsia.extension.FileFieldValidator;
import net.lukemurphey.nsia.extension.MessageValidator;
import net.lukemurphey.nsia.extension.PrototypeField;
import net.lukemurphey.nsia.extension.FieldValidator.FieldValidatorResult;
import net.lukemurphey.nsia.scan.ScanResult;


public class LogFileAction extends Action {

	private static final long serialVersionUID = 1L;
	private File file;
	private String template;
	public static final String USER_DESCRIPTION = "Append a message to a file";
	
	public LogFileAction( Hashtable<String, String> arguments ) throws ArgumentFieldsInvalidException{
		super("Log to File", USER_DESCRIPTION);
		
		configure(arguments);
	}
	
	public LogFileAction( long actionID, Application application ){
		super("Log to File", USER_DESCRIPTION);
	}
	
	public LogFileAction( File file, String template ){
		super("Log to File", USER_DESCRIPTION);
		
		// 0 -- Precondition check
		
		//	 0.1 -- The file cannot be null
		if( file == null ){
			throw new IllegalArgumentException("The file to append to cannot be null");
		}
		
		//	 0.2 -- The application cannot be null
		/*if( application == null ){
			throw new IllegalArgumentException("The application reference cannot be null");
		}*/
		
		//	 0.3 -- The template cannot be null
		if( template == null ){
			throw new IllegalArgumentException("The template cannot be null");
		}
		
		// 1 -- Initialize the class
		this.file = file;
		this.template = template;
	}
	
	public Hashtable<String, String> getValues(){
		Hashtable<String, String> values = new Hashtable<String, String>();
		
		values.put("File", this.file.getAbsolutePath());
		values.put("Message", this.template);
		
		return values;
	}
	
	@Override
	public FieldLayout getLayoutWithValues(){
		FieldLayout layout = LogFileAction.getLayout();
		layout.setFieldsValues(this.getValues());
		return layout;
	}
	
	protected void setField(String name, String value){
		
		if( "File".equals(name) ){
			this.file = new File(value);
		}
		else if( "Message".equals(name) ){
			this.template = value;
		}
	}
	
	/**
	 * Get the layout of fields to construct an instance of this class. 
	 * @return
	 */
	public static FieldLayout getLayout(){
		FieldLayout layout = new FieldLayout(1);
		
		// 1 -- Add the message template
		layout.addField( new FieldText("Message", "Message", "Enter the text to write to append to the log file. Note: you can use substitution variables to add in details such as the specimen/URL ($specimen), number of deviations detected ($deviation_count) or date of the finding ($date).", 1, 5, new MessageValidator()) );
		
		// 2 -- Add the file field (the location of the file to append to)
		layout.addField( new FieldText("File", "File", "Enter the full path of the file to apped the log entry to", 1, 1, new FileFieldValidator(false, false, true)) );
		
		// 3 -- Return the resulting layout
		return layout;
	}
	
	@Override
	public void configure( Hashtable<String, String> arguments ) throws ArgumentFieldsInvalidException{
		// 1 -- Get a list of the fields that must be configured
		FieldLayout layout = getLayout();
		PrototypeField[] fields = layout.getFields();
		
		// 2 -- Loop through each field and validate it, then set it
		for(PrototypeField field : fields){
			String value = arguments.get(field.getName());
			
			// 2.1 -- Stop if the field was not included
			if( value == null ){
				throw new ArgumentFieldsInvalidException("The " + field.getName() + " field was not provided", field);
			}
			
			// 2.2 -- If the field was included, then make sure it is valid
			else{
				FieldValidatorResult result = field.validate(value);
				
				if( result.validated() == false ){
					if( result.getMessage() == null ){
						throw new ArgumentFieldsInvalidException("The " + field.getName() + " field is invalid", field);
					}
					else{
						throw new ArgumentFieldsInvalidException(result.getMessage(), field);
					}
				}
				
				// 2.3 -- Store the value since it validated
				setField(field.getName(), value);
			}
		}
	}
	
	public static void appendFile( File file, String text ) throws ActionFailedException{
		
		// 0 -- Precondition check
		if( file == null ){
			throw new ActionFailedException("The file provided is invalid (null)");
		}
		
		if( text == null ){
			throw new ActionFailedException("The text provided is invalid (null)");
		}
		
		
		// 1 -- Append the file
	    try {
	        BufferedWriter out = new BufferedWriter(new FileWriter(file, true));
	        out.write(text);
	        out.close();
	    } catch (IOException e) {
	    	throw new ActionFailedException("The file could not be appended: " + e.getMessage(), e);
	    }
	}
	
	public String toString(){
		return "Log to File";
	}

	@Override
	public void execute(EventLogMessage logMessage) throws ActionFailedException {

		Vector<MessageVariable> vars = MessageVariable.getMessageVariables(logMessage);
		
		String text = Action.MessageVariable.processMessageTemplate(template, vars);
		
		appendFile(file, text);
	}

	@Override
	public String getConfigDescription() {
		return file.getAbsolutePath();
	}

	@Override
	public void execute(ScanResult scanResult) throws ActionFailedException {
		String text = getMessage(template, scanResult);
		
		appendFile(file, text);
	}
	
}
