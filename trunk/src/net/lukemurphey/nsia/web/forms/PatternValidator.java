package net.lukemurphey.nsia.web.forms;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates a field based on a regular expression.
 * @author Luke
 *
 */
public class PatternValidator implements FieldValidator {

	private Pattern pattern = null;
	
	public PatternValidator( String pattern ){
		
		// 0 -- Precondition check
		if( pattern == null ){
			throw new IllegalArgumentException("The pattern cannot be null");
		}
		
		// 1 -- Initialize the class
		this.pattern = Pattern.compile(pattern);
	}
	
	public PatternValidator( Pattern pattern ){
		
		// 0 -- Precondition check
		if( pattern == null ){
			throw new IllegalArgumentException("The pattern cannot be null");
		}
		
		// 1 -- Initialize the class
		this.pattern = pattern;
	}
	
	@Override
	public FieldValidatorResponse validate(String value) {
		Matcher matcher = pattern.matcher(value);
		
		if( matcher.matches() == false ){
			return new FieldValidatorResponse(false);
		}
		else{
			return new FieldValidatorResponse(true);
		}
	}

}
