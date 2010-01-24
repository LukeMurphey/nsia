package net.lukemurphey.nsia.web.forms;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PasswordValidator implements FieldValidator {

	private static final Pattern HAS_UPPER = Pattern.compile("[A-Z]");
	private static final Pattern HAS_LOWER = Pattern.compile("[a-z]");
	private static final Pattern HAS_NUMBER = Pattern.compile("[0-9]");
	private static final Pattern HAS_SPECIAL = Pattern.compile("[^0-9A-Za-z]");
	private static final String REQUIREMENT_DESCRIPTION = "(must have lower and upper case characters, numbers and at least one special character)";
	
	@Override
	public FieldValidatorResponse validate(String value) {
		
		// 1 -- Make sure the password is not empty
		if( value == null || value.length() == 0 ){
			return new FieldValidatorResponse(false, "Password must not be blank");
		}
		
		// 2 -- Make sure the password is at least 8 characters
		if( value.length() < 8 ){
			return new FieldValidatorResponse(false, "Password must be at least 8 characters long");
		}
		
		// 3 -- Make sure the password has mixed case, numbers, letters and at least one special character
		Matcher upperMatcher = HAS_UPPER.matcher(value);
		Matcher lowerMatcher = HAS_LOWER.matcher(value);
		Matcher numberMatcher = HAS_NUMBER.matcher(value);
		Matcher specialMatcher = HAS_SPECIAL.matcher(value);
		
		if( upperMatcher.find() == false ){
			return new FieldValidatorResponse(false, "Password does not have any upper case characters " + REQUIREMENT_DESCRIPTION);
		}
		
		if( lowerMatcher.find() == false ){
			return new FieldValidatorResponse(false, "Password does not have any lower case characters " + REQUIREMENT_DESCRIPTION);
		}
		
		if( numberMatcher.find() == false ){
			return new FieldValidatorResponse(false, "Password does not have any numbers " + REQUIREMENT_DESCRIPTION);
		}
		
		if( specialMatcher.find() == false ){
			return new FieldValidatorResponse(false, "Password does not have any special characters " + REQUIREMENT_DESCRIPTION);
		}
		
		return new FieldValidatorResponse(true);
	}

}
