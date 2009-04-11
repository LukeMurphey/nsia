package net.lukeMurphey.nsia.responseModule;

import java.io.IOException;
import java.io.File;
import java.util.Hashtable;

import net.lukeMurphey.nsia.eventLog.EventLogMessage;
import net.lukeMurphey.nsia.extension.ArgumentFieldsInvalidException;
import net.lukeMurphey.nsia.extension.FieldLayout;
import net.lukeMurphey.nsia.extension.FieldText;
import net.lukeMurphey.nsia.extension.FileFieldValidator;
import net.lukeMurphey.nsia.extension.PrototypeField;
import net.lukeMurphey.nsia.extension.FieldValidator.FieldValidatorResult;

public class CommandAction extends Action{

	private static final long serialVersionUID = 1L;
	
	private String command;
	private File workingDirectory = null;
	private static final String USER_DESCRIPTION = "Run a Program";
	
	public CommandAction( Hashtable<String, String> arguments ) throws ArgumentFieldsInvalidException{
		super("Command", USER_DESCRIPTION);
		
		configure(arguments);
	}
	
	public CommandAction( String command, File workingDirectory ){
		super("Command", USER_DESCRIPTION);
		
		// 0 -- Precondition check
		if( command == null ){
			throw new IllegalArgumentException( "The command to execute cannot be null" );
		}
		
		if( workingDirectory == null ){
			throw new IllegalArgumentException( "The working directory cannot be null" );
		}
		
		
		// 1 -- Initialize the class
		this.command = command;
		this.workingDirectory = workingDirectory;
	}
	
	public CommandAction( String command ){
		super("Command", USER_DESCRIPTION);
		
		// 0 -- Precondition check
		if( command == null ){
			throw new IllegalArgumentException( "The command to execute cannot be null" );
		}
		
		
		// 1 -- Initialize the class
		this.command = command;
		this.workingDirectory = null;
	}
	
	public Hashtable<String, String> getValues(){
		Hashtable<String, String> values = new Hashtable<String, String>();
		
		values.put("Command", this.command);
		
		if( this.workingDirectory != null ){
			values.put("WorkingDirectory", this.workingDirectory.getAbsolutePath());
		}
		
		return values;
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
	
	/**
	 * Set the field that is associated with the given name.
	 * @param name
	 * @param value
	 */
	private void setField(String name, String value){
		
		if( "Command".equals(name) ){
			this.command = value;
		}
		else if( "WorkingDirectory".equals(name) ){
			
			// If the value is empty, then no working directory is to be set; therefore, set the working directory to null so it will be noted as unnecessary
			if( value.length() == 0 ){
				this.workingDirectory = null;
			}
			else{
				this.workingDirectory = new File(value);
			}
		}
	}
	
	/**
	 * Execute the given command within the given working directory (the working directory will be ignored if null)
	 * @param command
	 * @param workingDirectory will be ignored if null
	 * @throws ActionFailedException
	 */
	private void runCommand( String command, File workingDirectory ) throws ActionFailedException{
		try{
			if( workingDirectory == null ){
				Runtime.getRuntime().exec(command);
			}
			else{
				Runtime.getRuntime().exec(command, new String[0], workingDirectory);
			}
		}
		catch(IOException e){
			throw new ActionFailedException("Attempt to run command (" + command + ") failed: " + e.getMessage(), e );
		}
	}
	
	@Override
	public FieldLayout getLayoutWithValues(){
		FieldLayout layout = CommandAction.getLayout();
		layout.setFieldsValues(this.getValues());
		return layout;
	}
	
	/**
	 * Get the layout of fields to construct an instance of this class. 
	 * @return
	 */
	public static FieldLayout getLayout(){
		FieldLayout layout = new FieldLayout(1);
		
		// 1 -- Add the command field (the location of the executable to run)
		layout.addField( new FieldText("Command", "Command", 1, 1, new FileFieldValidator(true, false, true)) );
		
		// 2 -- Add the working directory field (the location of the executable to run)
		layout.addField( new FieldText("WorkingDirectory", "Working Directory", 1, 1, new FileFieldValidator(true, true, false)) );
		
		// 3 -- Return the resulting layout
		return layout;
	}

	@Override
	public void execute(EventLogMessage logMessage) throws ActionFailedException {
		runCommand( command, workingDirectory );
	}
	
	@Override
	public String getConfigDescription(){
		return command;
	}

	/*@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		
		command = in.readUTF();
		String workingDirectory = in.readUTF();
		
		if(workingDirectory.length() > 0){
			this.workingDirectory = new File(workingDirectory);
		}
		else{
			this.workingDirectory = null;
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeChars(command);
		
		if( workingDirectory != null){
			out.writeChars(workingDirectory.getAbsolutePath());
		}
		else{
			out.writeChars("");
		}
	}*/
	
}
