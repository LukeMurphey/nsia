package net.lukemurphey.nsia.scan;

public class LineParseException extends Exception {

	private static final long serialVersionUID = 1230469028849960480L;
	
	private int lineNumber;
	private int columnNumber;
	
	public LineParseException( String message, int line, int column){
		super(message);
		lineNumber = line;
		columnNumber = column;
	}
	
	public LineParseException( String message, int line){
		super(message);
		lineNumber = line;
		columnNumber = 0;
	}
	
	public int getLineNumber(){
		return lineNumber;
	}
	
	public int getColumnNumber(){
		return columnNumber;
	}

}
