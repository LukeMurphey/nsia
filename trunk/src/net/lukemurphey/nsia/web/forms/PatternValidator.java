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
	private String defaultMessage = null;
	
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
	
	public PatternValidator( String pattern, String message ){
		
		// 0 -- Precondition check
		if( pattern == null ){
			throw new IllegalArgumentException("The pattern cannot be null");
		}
		
		// 1 -- Initialize the class
		this.pattern = Pattern.compile(pattern);
		this.defaultMessage = message;
	}
	
	public PatternValidator( Pattern pattern, String message ){
		
		// 0 -- Precondition check
		if( pattern == null ){
			throw new IllegalArgumentException("The pattern cannot be null");
		}
		
		// 1 -- Initialize the class
		this.pattern = pattern;
		this.defaultMessage = message;
	}
	
	@Override
	public FieldValidatorResponse validate(String value) {
		Matcher matcher = pattern.matcher(value);
		
		if( matcher.matches() == false ){
			return new FieldValidatorResponse(false, defaultMessage);
		}
		else{
			return new FieldValidatorResponse(true, defaultMessage);
		}
	}

}
