package net.lukemurphey.nsia.extension;

public interface FieldValidator {
	
	public static class FieldValidatorResult{
		private String message;
		private boolean validated;
		
		public FieldValidatorResult( String message, boolean validated ){
			this.message = message;
			this.validated = validated;
		}
		
		public FieldValidatorResult( boolean validated ){
			this.message = null;
			this.validated = validated;
		}
		
		public String getMessage(){
			return message;
		}
		
		public boolean validated(){
			return validated;
		}
	}
	
	public FieldValidatorResult validate( String value ); 

}
