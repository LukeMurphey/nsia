package net.lukemurphey.nsia.scan;

public class EncodingDetectionFailedException extends Exception{
	
	private static final long serialVersionUID = -7379902631984346056L;

	public String getMessage(){
		return "The method was unable to detect the type of encoding used";
	}

}
