package net.lukemurphey.nsia.scan;

import java.io.IOException;
import java.sql.SQLException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import junit.framework.TestCase;

public class DefinitionSetTest extends TestCase {

	public void testDefinitions() throws DefinitionSetLoadException, SQLException, NoDatabaseConnectionException, InputValidationException, DefinitionUpdateFailedException, ParserConfigurationException, SAXException, IOException {
		
		String xmlString = "<Definitions Date=\"Jan 07 00:00:03 -0600 2011\" Version=\"1.21 release\" ><Definition Type=\"Pattern\" Message=\"Found the fountain of youth\" Name=\"Test.test.fountain_of_youth\" Severity=\"MEDIUM\" ID=\"1000000\" Version=\"1\">Alert(&quot;Test.test.fountain_of_youth&quot;){"
						   	+ "    ID=&quot;1000000&quot;;"
							+ "    Message=&quot;Found the fountain of youth&quot;;"
							+ "    Severity=&quot;Medium&quot;;"
							+ "    String=&quot;fountain of youth&quot;;"
							+ "    Version=1;"
							+ "}</Definition></Definitions>";
		
		DefinitionSet definitionSet = DefinitionSet.loadFromString(xmlString);

		if( definitionSet.getDefinitions().length != 1 ){
			fail("Definitions were not successfully loaded");
		}
	}
	
	public void testLoadEmptyDefinitions() throws DefinitionSetLoadException, SQLException, NoDatabaseConnectionException, InputValidationException, DefinitionUpdateFailedException, ParserConfigurationException, SAXException, IOException {
		
		String xmlString = "<Definitions Date=\"Jan 07 00:00:03 -0600 2011\" Version=\"1.21 release\" ></Definitions>";
		
		try{
			DefinitionSet.loadFromString(xmlString);
		}catch(DefinitionSetLoadException e){
			return;
		}
		
		// This should have thrown an exception
		fail("Definition set loaded an empty set");
	}
	
	public void testLoadDefinitionsWithNoDate() throws DefinitionSetLoadException, SQLException, NoDatabaseConnectionException, InputValidationException, DefinitionUpdateFailedException, ParserConfigurationException, SAXException, IOException {
		
		String xmlString = "<Definitions Version=\"1.21 release\" ><Definition Type=\"Pattern\" Message=\"Found the fountain of youth\" Name=\"Test.test.fountain_of_youth\" Severity=\"MEDIUM\" ID=\"1000000\" Version=\"1\">Alert(&quot;Test.test.fountain_of_youth&quot;){"
						   	+ "    ID=&quot;1000000&quot;;"
							+ "    Message=&quot;Found the fountain of youth&quot;;"
							+ "    Severity=&quot;Medium&quot;;"
							+ "    String=&quot;fountain of youth&quot;;"
							+ "    Version=1;"
							+ "}</Definition></Definitions>";
		
		try{
			DefinitionSet.loadFromString(xmlString);
		}catch(DefinitionSetLoadException e){
			return;
		}
		
		// This should have thrown an exception
		fail("Definition set loaded even though the date field was invalid");
	}
	
}
