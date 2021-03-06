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
import net.lukemurphey.nsia.scan.ScanResult;

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
			if( GenericUtils.sendMail(toAddress, subjectTmp, bodyTmp) == false ){
				// Throw an exception if we could not send an email
				throw new ActionFailedException("The action failed to send the email because an email server is not properly configured (at least an SMTP server and from address must be defined)"); 
			}
		}
		catch(MessagingException e){
			throw new ActionFailedException("The action failed to send the email: " + e.getMessage(), e);
		}
	}

	/**
	 * This field validator ensures that the email address is valid.
	 * @author Luke Murphey
	 *
	 */
	private static class EmailAddressValidator implements FieldValidator{

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
		layout.addField( new FieldText("ToAddress", "Destination Address", "Enter the destination email address", 1, 1, new EmailAddressValidator()) );
		
		// 2 -- Add the subject
		FieldText subject = new FieldText("Subject", "Subject", "Enter the subject of the email", 1, 1, new MessageValidator("subject"));
		subject.setDefaultValue("NSIA: Deviations Detected in $sitegroup_name");
		layout.addField(subject);
		
		// 3 -- Add the body
		FieldText body = new FieldText("Body", "Body", "Enter the email message body. Note: you can use substitution variables to add in details such as the specimen/URL ($specimen), number of deviations detected ($deviation_count) or date of the finding ($date).", 1, 5, new MessageValidator("body"));
		body.setDefaultValue("NSIA has detected deviations in the site-group $sitegroup_name \n"
				+ "Details:\n"
				+ "------------------------\n"
				+ "    Rule ID: $rule_id\n"
				+ "    Site-group: $sitegroup_name\n"
				+ "    Site-group ID: $sitegroup_id\n"
				+ "    Deviations: $deviation_count URLs rejected\n"
				+ "    Domain: $specimen\n"
				+ "    Scanner: $scanner_host_name ($scanner_url)\n"
				+ "    Detected on: $date \n\n"
				+ "View the detailed results on: $scanner_url_ip/SiteGroup/$sitegroup_id");
		
		layout.addField(body);
		
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
		
		if( subject != null ){
			values.put("Subject", subject);
		}
		
		if( body != null ){
			values.put("Body", body);
		}
		
		return values;
	}

	@Override
	protected void setField(String name, String value) {
		if( "Subject".equals(name) ){
			this.subject = value;
		}
		else if( "Body".equals(name) ){
			this.body = value;
		}
		else if( "ToAddress".equals(name) ){
			try {
				this.toAddress = EmailAddress.getByAddress(value);
			} catch (UnknownHostException e) {
				this.toAddress = null;
			} catch (InvalidLocalPartException e) {
				this.toAddress = null;
			}
		}
	}

	@Override
	public void execute(ScanResult scanResult) throws ActionFailedException {
		String subjectTmp = getMessage(subject, scanResult);
		
		String bodyTmp = getMessage(body, scanResult);
		
		try{
			GenericUtils.sendMail(toAddress, subjectTmp, bodyTmp);
		}
		catch(MessagingException e){
			throw new ActionFailedException("The action failed to send the email: " + e.getMessage(), e);
		}
		
	}
}
