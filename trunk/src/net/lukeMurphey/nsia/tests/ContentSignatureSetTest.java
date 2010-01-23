package net.lukemurphey.nsia.tests;

import java.io.IOException;
import java.io.StringReader;

import junit.framework.TestCase;

import javax.xml.parsers.*;

import net.lukemurphey.nsia.scan.DefinitionSet;
import net.lukemurphey.nsia.scan.DefinitionSetLoadException;

import org.w3c.dom.*;
import org.xml.sax.*;

public class ContentSignatureSetTest extends TestCase {
	
	public void testLoadXML() throws IOException, SAXException, ParserConfigurationException, DefinitionSetLoadException{
		String xml = TestResources.readFileAsString(TestResources.TEST_RESOURCE_DIRECTORY + "SignaturesList.xml");

		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
		
		DefinitionSet sigs = DefinitionSet.loadFromXml(document);
		
		if( sigs.getDefinitions().length != 2){
			fail("The signature set did not load the expected number of signatures (returned " + sigs.getDefinitions().length + ")");
		}
	}

}
