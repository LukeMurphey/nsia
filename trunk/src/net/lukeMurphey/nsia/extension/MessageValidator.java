package net.lukeMurphey.nsia.extension;

public class MessageValidator implements FieldValidator {

	@Override
	public FieldValidatorResult validate(String value) {

		if(value == null || value.length() == 0){
			return new FieldValidatorResult("The message cannot be blank", false);
		}
		
		return new FieldValidatorResult(true);
	}

}
