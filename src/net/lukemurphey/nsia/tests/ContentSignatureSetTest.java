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
		String xml = TestResources.readFileAsString(TestResources.getTestResourcePath() + "NSIA.Definitions");

		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
		
		DefinitionSet sigs = DefinitionSet.loadFromXml(document);
		
		if( sigs.getDefinitions().length != 1893){
			fail("The signature set did not load the expected number of signatures (returned " + sigs.getDefinitions().length + ")");
		}
	}

}
