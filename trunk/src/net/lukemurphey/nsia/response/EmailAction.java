package net.lukemurphey.nsia.response;

import javax.mail.*;

import net.lukemurphey.nsia.EmailAddress;
import net.lukemurphey.nsia.GenericUtils;
import net.lukemurphey.nsia.InvalidLocalPartException;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.extension.ArgumentFieldsInvalidException;
import net.lukemurphey.nsia.extension.FieldLayout;
import net.lukemurphey.nsia.extension.FieldText;
import net.lukemurphey.nsia.extension.FieldValidator;
import net.lukemurphey.nsia.extension.MessageValidator;
import net.lukemurphey.nsia.extension.PrototypeField;

import java.net.UnknownHostException;
import java.util.*;

public class EmailAction extends Action {
	
	private static final long serialVersionUID = 1L;
	private EmailAddress toAddress;
	private String body;
	private String subject;

	public EmailAction( Hashtable<String, String> arguments ) throws ArgumentFieldsInvalidException{
		super("Email", "Create and send an email message");
		
		configure(arguments);
	}
	
	@Override
	public void execute(EventLogMessage logMessage) throws ActionFailedException {
		Vector<MessageVariable> vars = MessageVariable.getMessageVariables(logMessage);
		
		String subjectTmp = Action.MessageVariable.processMessageTemplate(subject, vars);
		
		String bodyTmp = Action.MessageVariable.processMessageTemplate(body, vars);
		
		try{
			GenericUtils.sendMail(toAddress, subjectTmp, bodyTmp);
		}
		catch(MessagingException e){
			throw new ActionFailedException("The action failed to send the email: " + e.getMessage(), e);
		}
	}

	private static class EmailAddressValidator implements FieldValidator{

		@Override
		public FieldValidatorResult validate(String value) {
			
			try{
				EmailAddress.getByAddress(value);
			}
			catch(InvalidLocalPartException e){
				return new FieldValidatorResult( e.getMessage(), false );
			}
			catch (UnknownHostException e) {
				return new FieldValidatorResult( e.getMessage(), false );
			}
			catch (IllegalArgumentException e) {
				return new FieldValidatorResult( e.getMessage(), false );
			}
			
			return new FieldValidatorResult( true );
		}
		
	}
	
	/**
	 * Get the layout of fields to construct an instance of this class. 
	 * @return
	 */
	public static FieldLayout getLayout(){
		FieldLayout layout = new FieldLayout(1);
		
		// 1 -- Add the destination address
		layout.addField( new FieldText("ToAddress", "Destination Address", 1, 1, new EmailAddressValidator()) );
		
		// 2 -- Add the subject
		layout.addField( new FieldText("Subject", "Subject", 1, 1, new MessageValidator()) );
		
		// 3 -- Add the body
		layout.addField( new FieldText("Body", "Body", 1, 5, new MessageValidator()) );
		
		// 4 -- Return the resulting layout
		return layout;
	}
	
	@Override
	public FieldLayout getLayoutWithValues(){
		FieldLayout layout = EmailAction.getLayout();
		layout.setFieldsValues(this.getValues());
		return layout;
	}

	@Override
	public void configure( Hashtable<String, String> arguments ) throws ArgumentFieldsInvalidException{
		
		// 1 -- Validate the fields
		for(PrototypeField field : getLayout().getFields()){
			field.validate(arguments.get(field.getName()));
		}
		
		// 2 -- Update the action with the field data
		try {
			toAddress = EmailAddress.getByAddress( arguments.get("ToAddress") );
		}
		catch (UnknownHostException e)
		{
			throw new ArgumentFieldsInvalidException(e.getMessage(), new FieldText("ToAddress", "Destination Address", 1, 1, new EmailAddressValidator()) );
		}
		catch (InvalidLocalPartException e)
		{
			throw new ArgumentFieldsInvalidException(e.getMessage(), new FieldText("ToAddress", "Destination Address", 1, 1, new EmailAddressValidator()) );
		}
		catch (IllegalArgumentException e)
		{
			throw new ArgumentFieldsInvalidException(e.getMessage(), new FieldText("ToAddress", "Destination Address", 1, 1, new EmailAddressValidator()) );
		}
		
		body = arguments.get("Body");
		subject = arguments.get("Subject");
		
	}
	
	
	@Override
	public String getConfigDescription() {
		if(toAddress == null){
			return "(undefined address)";
		}
		else{
			return toAddress.toString();
		}
	}
	
	public Hashtable<String, String> getValues(){
		Hashtable<String, String> values = new Hashtable<String, String>();
		
		if( toAddress == null ){
			values.put("ToAddress", "");
		}
		else{
			values.put("ToAddress", toAddress.toString());
		}
		
		values.put("Subject", subject);
		values.put("Body", body);
		
		return values;
	}
}
