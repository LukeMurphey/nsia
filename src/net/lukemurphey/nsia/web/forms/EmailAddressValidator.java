package net.lukemurphey.nsia.web.forms;

import java.net.UnknownHostException;

import net.lukemurphey.nsia.EmailAddress;
import net.lukemurphey.nsia.InvalidLocalPartException;

public class EmailAddressValidator implements FieldValidator {

	private String defaultMessage = null;
	
	public EmailAddressValidator(){ }
	public EmailAddressValidator( String defaultMessage ){
		this.defaultMessage = defaultMessage;
	}
	
	public FieldValidatorResponse validate(String value) {
		
		String msg = defaultMessage;
		
		if( msg == null ){
			msg = "Email address is not valid";
		}
		
		try {
			EmailAddress.getByAddress(value);
		} catch (UnknownHostException e) {
			return new FieldValidatorResponse(false, msg + " (domain is invalid)");
		} catch (InvalidLocalPartException e) {
			return new FieldValidatorResponse(false, msg + " (local part is invalid [part before the @ symbol])");
		}
		
		return new FieldValidatorResponse(true);
	}

}
