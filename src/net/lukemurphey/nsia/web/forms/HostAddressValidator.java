package net.lukemurphey.nsia.web.forms;

import net.lukemurphey.nsia.HostAddress;

public class HostAddressValidator implements FieldValidator {

	private String defaultMessage = null;
	
	public HostAddressValidator(){
		
	}
	
	public HostAddressValidator( String defaultMessage ){
		this.defaultMessage = defaultMessage;
	}
	
	@Override
	public FieldValidatorResponse validate(String value) {
		try{
			new HostAddress(value);
		}
		catch( IllegalArgumentException e){
			if( defaultMessage != null ){
				return new FieldValidatorResponse(false, defaultMessage);
			}
			else{
				return new FieldValidatorResponse(false, "The domain provided is invalid");
			}
		}
		
		return new FieldValidatorResponse(true);
	}

}
