package net.lukemurphey.nsia.eventlog;

public class MessageFormatterFactory {
	
	private MessageFormatterFactory(){
		//Not instantiable
	}
	
	public static MessageFormatter getFormatter(String formatter){
		
		if( "Native".equalsIgnoreCase(formatter) ){
			return null;
		}
		else if( formatter.equalsIgnoreCase("CEF") || formatter.equalsIgnoreCase("Common Event Format")){
			return new CommonEventFormatMessageFormatter();
		}
		else if( formatter.equals("CEE") || formatter.equalsIgnoreCase("Common Event Expression")){
			return new CommonEventExpressionMessageFormatter();
		}
		
		return null;
		
	}
	
}
