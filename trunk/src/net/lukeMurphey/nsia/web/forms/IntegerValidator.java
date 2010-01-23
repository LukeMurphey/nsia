package net.lukemurphey.nsia.web.forms;

public class IntegerValidator implements FieldValidator {

	private Integer minimum = null;
	private Integer maximum = null;
	
	public IntegerValidator(){}
	public IntegerValidator(int minimum, int maximum){
		if( minimum > maximum ){
			throw new IllegalArgumentException("The minimum cannot be greater than the maximum");
		}
		
		this.minimum = minimum;
		this.maximum = maximum;
	}
	public IntegerValidator(int maximum){
		this.maximum = maximum;
	}
	
	@Override
	public FieldValidatorResponse validate(String value) {
		
		// 1 -- Get the value as an integer
		int valueInt;
		
		try{
			valueInt = Integer.valueOf(value);
		}
		catch( NumberFormatException e ){
			return new FieldValidatorResponse(false, "Not a valid integer");
		}
		
		// 2 -- Make sure the value is within the range
		if( minimum != null && valueInt < minimum ){
			return new FieldValidatorResponse(false, "Cannot be less than " + minimum);
		}
		
		if( maximum != null && valueInt > maximum ){
			return new FieldValidatorResponse(false, "Cannot be greater than " + maximum);
		}
		
		return new FieldValidatorResponse(true);
	}

}
