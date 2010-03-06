package net.lukemurphey.nsia.support;

import java.util.*;
import java.io.*;
import java.text.*;

import net.lukemurphey.nsia.scan.*;

public class DefinitionSupport {

	public static String readFileAsString(String filePath) throws java.io.IOException{
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader = new BufferedReader( new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead=0;
        while((numRead=reader.read(buf)) != -1){
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString();
    }
	
	public static final void main(String[] args) {
		
		
		// 0 -- Precondition check
		if( args.length != 4 && args.length != 3 ){
			System.err.println("The correct number of arguments was not provided: NSIAsupport <import_directory> <definitions_file> <definitions_ver> <definition_date>");
			System.err.println("\timport_directory : The directory containing the definitions to import");
			System.err.println("\tdefinitions_file  : File to write the definitions to");
			System.err.println("\tdefinition_ver    : The version of the definition set");
			System.err.println("\tdefinition_date   : The date of the definitions (optional, will use the current date and time if excluded))");
			return;
		}
		
		// 1 -- Load all of the signatures
		Vector<Definition> definitions = new Vector<Definition>(10000);
		
		File dir = new File(args[0]);
	    
	    String[] children = dir.list();
	    
	    if (children == null) {
	        // Either dir does not exist or is not a directory
	    } else {

	    	// Get the script rules
	    	for (int i=0; i<children.length; i++) {
	    		
	    		// Get filename of file or directory
	    		String filename = children[i];

	    		try{
	    			
	    			if( filename.endsWith(".js"))
	    			{
	    				System.out.println("Loading definition in " + filename);
	    				
	    				String ruleCode = readFileAsString( args[0] + "/" + filename );

	    				ScriptDefinition definition = ScriptDefinition.parse(ruleCode);
	    				
	    				if( definition.getID() <= 0 ){
	    					System.err.println( "Definition in " + filename + " does not have a valid ID (" + definition.getFullName() + ")");
	    				}
	    				else{
	    					definitions.add(definition);
	    				}
	    			}
	    		}
	    		catch(IOException e){
	    			System.out.println("IOException from " + filename);
	    			e.printStackTrace();
	    		}
	    		catch(InvalidDefinitionException e){
	    			System.out.println("Definition is invalid from " + filename);
	    		}
	    	}

	    	// Get the pattern definitions
	    	for (int i=0; i<children.length; i++) {
	    		
	    		
	    		// Get filename of file or directory
	    		String filename = children[i];

	    		try{
	    			if( filename.endsWith(".rule") )
	    			{
	    				System.out.println("Loading definitions in " + filename);
	    				
	    				String ruleCode = readFileAsString( args[0] + "/" + filename );

	    				PatternDefinition[] sigs = PatternDefinition.parseAll(ruleCode);
	    				
	    				for( int c = 0; c < sigs.length; c++){
	    					
	    					PatternDefinition definition = sigs[c];
	    					
	    					if( definition.getID() <= 0 ){
		    					System.err.println( "Definition in " + filename + " does not have a valid ID (" + definition.getFullName() + ")");
		    				}
		    				else{
		    					definitions.add(definition);
		    				}
	    					
	    					
	    				}
	    			}
	    		}
	    		catch(IOException e){
	    			System.out.println("IOException from " + filename);
	    			e.printStackTrace();
	    		}
	    		catch(InvalidDefinitionException e){
	    			System.out.println("Definition is invalid from " + filename + "(" + e.getMessage() + ")");
	    		}
	    		catch(UnpurposedDefinitionException e){
	    			System.out.println("Definition is invalid (unpurposed) from " + filename);
	    		}
	    	}
	    }

	    
	    // 2 -- Save the definition list to a file
	    try{
	    	SimpleDateFormat dateFormat = new SimpleDateFormat(DefinitionSet.DEFINITION_SET_DATE_FORMAT);
	    	//dateFormat.setLenient(true);
	    	Date date;
	    	
	    	if( args.length > 3 ){
	    		date = dateFormat.parse(args[3]);
	    	}
	    	else{
	    		date = new Date();
	    	}
	    	
	    	DefinitionSet definitionSet = new DefinitionSet( date, definitions, args[2]); //"July 21, 2007 2:22:42 PM GMT"
	    	String xml = definitionSet.getAsXML();
	    	
	    	FileWriter fileWriter = new FileWriter(args[1]);
	    	BufferedWriter buffWriter = new BufferedWriter(fileWriter);
	    	buffWriter.write(xml);
	    	buffWriter.close();
	    }
	    catch(ParseException e){
	    	System.err.println("Date format is invalid, should be in the format \"" + DefinitionSet.DEFINITION_SET_DATE_FORMAT + "\": "+ e);
	    	return;
	    }
	    catch(IOException e)
	    {
	    	System.err.println("File could not be written: "+ e);
	    }
	    
	    System.out.println( "Done, " +  definitions.size() + " definitions exported");
	    
	}
}
