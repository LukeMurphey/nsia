package net.lukemurphey.nsia.console;

import java.sql.SQLException;
import java.util.Vector;

import net.lukemurphey.nsia.*;

/**
 * This class represents a console command that can be invoked by the console interface.
 * @author Luke Murphey
 *
 */
abstract class ConsoleCommand{
	
	protected String description;
	protected Vector<String> names = new Vector<String>();
	
	protected String sampleArguments;
	protected Application application;
	protected boolean stopListening = false;
	
	public enum CommandResult{
		EXECUTED_CORRECTLY, ERROR, TERMINATE_CONSOLE, SYNTAX_ERROR
	}
	
	public ConsoleCommand( String sampleArguments, String description, Application application, String... names ){
		for( String name : names){
			this.names.add(name);
		}
		
		this.sampleArguments = sampleArguments;
		this.description = description;
		this.application = application;
	}
	
	public abstract CommandResult run(String input[]) throws SQLException, NoDatabaseConnectionException, InputValidationException;
	
	public boolean matchesName(String name){
		for( String curname : names){
			if( curname != null && curname.equalsIgnoreCase(name) ){
				return true;
			}
		}
		
		return false;
	}
	
	public String getName(){
		return names.get(0);
	}
	
	public String getSampleInvocation(){
		
		if( sampleArguments == null){
			return getName();
		}
		else{
			return getName() + " " + sampleArguments;
		}
	}
	
	public String getDescription(){
		return description;
	}
	
	public String getCompleteDescription(){
		return getSampleInvocation() + " " + description;
	}
	
	protected static String getChars( char toInsert, int length ){
		StringBuffer buffer = new StringBuffer();
		
		for(int c = 0; c < length; c++ ){
			buffer.append(toInsert);
		}
		
		return buffer.toString();
	}
	
	protected static String addSpaces( String data, int length ){
		StringBuffer buffer = new StringBuffer(data);
		
		for(int c = 0; c < (length - data.length()); c++ ){
			buffer.append(" ");
		}
		
		return buffer.toString();
	}
	
	protected static String getTableFromString( Vector<String[]> tableData, boolean firstRowIsHeader ){
		
		String[][] arrayArgs = new String[tableData.size()][];
		
		tableData.toArray(arrayArgs);
		
		return getTableFromString(arrayArgs, firstRowIsHeader);
		
	}
	
	protected static String getTableFromString( String[][] tableData, boolean firstRowIsHeader ){
		
		// 1 -- Compute the maximum lengths of each field
		Vector<Integer> lengths = new Vector<Integer>();
		
		for( int row = 0; row < tableData.length; row++){
			
			for( int column = 0; column < tableData[row].length; column++){
				
				if( column >= lengths.size() ){
					lengths.add( tableData[row][column].length() );
				}
				else{
					lengths.set( column, Math.max( lengths.get(column), tableData[row][column].length() ) );
				}
			}
		}
		
		
		// 2 -- Create the resulting table
		StringBuffer buffer = new StringBuffer();
		
		for( int row = 0; row < tableData.length; row++){
			
			// 2.1 -- Output the break for the header   
			if( row == 1 && firstRowIsHeader ){
				
				int totalChars = 0;
				for (Integer integer : lengths) {
					totalChars += integer + 5;
				}
				
				buffer.append( getChars('-', totalChars) );
				buffer.append("\n");
			}
			
			// 2.2 -- Output the data
			for( int column = 0; column < tableData[row].length; column++){
				buffer.append( addSpaces( tableData[row][column], lengths.get(column) + 5 ) );
			}
			
			buffer.append("\n");
		}
		
		// 2.3 -- Output the line if the header is available but no entries exist.
		if( tableData.length == 1 && firstRowIsHeader ){
			int totalChars = 0;
			for (Integer integer : lengths) {
				totalChars += integer + 5;
			}
			
			buffer.append( getChars('-', totalChars) );
		}
		
		// 3 -- Return the result
		return buffer.toString();
	}

	
}
