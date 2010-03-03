package net.lukemurphey.nsia.extension;

public class MessageValidator implements FieldValidator {

	private String name = "message";
	
	public MessageValidator(){
		
	}
	
	public MessageValidator(String fieldName ){
		name = fieldName;
	}
	
	@Override
	public FieldValidatorResult validate(String value) {

		if(value == null || value.length() == 0){
			return new FieldValidatorResult("The " + name + " cannot be blank", false);
		}
		
		return new FieldValidatorResult(true);
	}

}
