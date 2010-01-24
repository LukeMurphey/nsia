package net.lukemurphey.nsia.web.forms;

import java.net.UnknownHostException;

import net.lukemurphey.nsia.EmailAddress;
import net.lukemurphey.nsia.InvalidLocalPartException;

public class EmailAddressValidator implements FieldValidator {

	@Override
	public FieldValidatorResponse validate(String value) {
		
		try {
			EmailAddress.getByAddress(value);
		} catch (UnknownHostException e) {
			return new FieldValidatorResponse(false, "The domain is invalid");
		} catch (InvalidLocalPartException e) {
			return new FieldValidatorResponse(false, "The local part is invalid (part before the @ symbol)");
		}
		
		return new FieldValidatorResponse(true);
	}

}
