package net.lukemurphey.nsia.console;

public class QuoteSequenceException extends Exception{

	private static final long serialVersionUID = 0L;
	private int position;
	private String input;

	public QuoteSequenceException(String input, int position){
		super("Mismatched double quotes detected");
		this.position = position;
		this.input = input;
	}
	
	public String getDescription(){
		return "Error: out of sequence double quote at position " + position + "\n" + input + "\n" + getSpaces(position) + "^";
	}
	
	public String getSpaces(int numberOfSpaces){
		StringBuffer spaces = new StringBuffer();
		
		for(int c = 0; c < numberOfSpaces; c++){
			spaces.append(" ");
		}
		
		return spaces.toString();
	}
}
