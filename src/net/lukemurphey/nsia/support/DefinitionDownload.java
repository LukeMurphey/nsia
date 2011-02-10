package net.lukemurphey.nsia.support;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import net.lukemurphey.nsia.rest.DefinitionsDownload;
import net.lukemurphey.nsia.rest.RESTRequestFailedException;
import net.lukemurphey.nsia.scan.DefinitionArchive;
import net.lukemurphey.nsia.scan.DefinitionSet.DefinitionVersionID;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;

public class DefinitionDownload {

	public static final String APPLICATION_NAME = "definition_download";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// 1 -- Try to parse the arguments
		JSAP jsap = null;
		try {
			jsap = getCommandLineProcessor();
		} catch (JSAPException e) {
			System.out.println("Could not get a command-line parser");
			e.printStackTrace();
			System.exit(1);
		}
		
		JSAPResult commandLineData = jsap.parse( args );
		
		//Show help if the command-line data parsing failed
		if( !commandLineData.success() ){
			System.err.println();
            System.err.println("Usage: " + APPLICATION_NAME);
            System.err.println( jsap.getHelp() );
            System.exit(1);
		}
		
		boolean verbose = false;
		if( commandLineData.getBoolean("verbose") ){
			verbose = true;
		}
		
		// 2 -- Get the current set definition ID and date
		String licenseKey = commandLineData.getString("licenseKey");
		
		//	 2.1 -- Get the definition set version
		DefinitionVersionID definitionVersionID = null;
		
		try {
			//System.out.print("Getting definition set version information from threatfactor.com...");
			definitionVersionID = DefinitionArchive.getLatestAvailableDefinitionSetID();
			//System.out.println("Done");
		} catch (RESTRequestFailedException e) {
			System.out.println("Unable to get version of current definitions: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("Unable to get version of current definitions: " + e.getMessage());
		}
		
		if( definitionVersionID != null && verbose){
			System.out.println("Current definition set ID: " + definitionVersionID);
		}
		
		//	 2.2 -- Get the definition set date
		Date definitionDate = null;
		
		try {
			//System.out.print("Getting definition set date from threatfactor.com...");
			definitionDate = DefinitionArchive.getLatestAvailableDefinitionSetDate();
			//System.out.println("Done");
		} catch (RESTRequestFailedException e) {
			System.out.println("Unable to get date of current definitions: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("Unable to get date of current definitions: " + e.getMessage());
		} catch (ParseException e) {
			System.out.println("Unable to get date of current definitions: " + e.getMessage());
		}
		
		if( definitionDate != null && verbose ){
			System.out.println("Current definition set date: " + definitionDate);
		}
		
		// 3 -- Get the definitions as a string
		String definitionsXML = null;
		
		try {
			
			// Print a message indicating that definition are being downloaded
			if( verbose){
				System.out.print("Downloading definitions from threatfactor.com...");
			}
			
			// Get the definitions as XML
			definitionsXML = DefinitionsDownload.getDefinitionsAsString(licenseKey, "Command-line");
			
			// Print a message indicating that definition are downloaded
			if( verbose){
				System.out.println("Done");
			}
		} catch (RESTRequestFailedException e) {
			System.err.println();
            System.err.println("Unable to retrieve the definitions from threatfactor.com");
            System.exit(1);
		}
		
		// 4 -- Make sure the definitions are not null
		if( definitionsXML == null){
			System.err.println();
            System.err.println("No definitions exist on threatfactor.com");
            System.exit(1);
		}
		
		// 5 -- Get the file name 
		String outputFileName = commandLineData.getString("outputFile");
		
		if( outputFileName == null && definitionVersionID != null ){
			outputFileName = "definitions " + definitionVersionID.toString() + ".xml";
		}
		else if( outputFileName == null ){
			outputFileName = "definitions.xml";
		}
		
		// 6 -- Save the definitions to the file
		try {
			BufferedWriter out = new BufferedWriter( new FileWriter(outputFileName) );
			
			if( verbose){
				System.out.print("Writing definitions to \"" + outputFileName + "\"...");
			}
			
			out.write(definitionsXML);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		if( verbose ){
			System.out.println("Done");
		}

	}
	
	/**
	 * Create a command-line parser for parsing the command-line arguments
	 * @return
	 * @throws JSAPException
	 */
	private static JSAP getCommandLineProcessor() throws JSAPException{
		JSAP jsap = new JSAP();
		
		// 1 -- Output file option (-f)
		FlaggedOption opt1 = new FlaggedOption("outputFile")
        		.setStringParser(JSAP.STRING_PARSER)
        		//.setDefault("./definitions.xml")
        		.setRequired(false)
        		.setShortFlag('f')
        		.setLongFlag("file");
		opt1.setHelp("The name of the file to store the definitions in");
		
		jsap.registerParameter(opt1);
		
		// 2 -- Verbose messages option (-v)
		Switch sw1 = new Switch("verbose")
        		.setShortFlag('v')
        		.setLongFlag("verbose");
		sw1.setHelp("Output messages to standard output");
		
		jsap.registerParameter(sw1);
		
		// 3 -- GUI mode option (-gui)
		/*
		Switch sw2 = new Switch("gui").setShortFlag('g').setLongFlag("gui");
		sw2.setHelp("Starts the application with the GUI interface");
		
		jsap.registerParameter(sw2);*/
		
		// 4 -- License number
		UnflaggedOption opt2 = new UnflaggedOption("licenseKey")
			.setRequired(true);
		
		jsap.registerParameter(opt2);
		
		// Return the processor
		return jsap;
	}

}
