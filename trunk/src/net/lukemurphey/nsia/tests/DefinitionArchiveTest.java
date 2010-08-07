package net.lukemurphey.nsia.tests;

import java.sql.SQLException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.scan.DefinitionArchive;
import net.lukemurphey.nsia.scan.DefinitionSetLoadException;
import net.lukemurphey.nsia.scan.DefinitionUpdateFailedException;
import net.lukemurphey.nsia.scan.DefinitionSet.DefinitionVersionID;
import junit.framework.TestCase;

public class DefinitionArchiveTest extends TestCase {

	Application app = null;
	
	public void setUp() throws TestApplicationException{
		app = TestApplication.getApplication(true);
	}
	
	public void tearDown(){
		TestApplication.stopApplication();
	}

	public void testLoadDefaultDefinitions() throws DefinitionSetLoadException, SQLException, NoDatabaseConnectionException, InputValidationException, DefinitionUpdateFailedException {
		
		DefinitionArchive archive = DefinitionArchive.getArchive();
		
		DefinitionVersionID defs = archive.loadDefaultDefinitions();
		
		if( defs == null ){
			fail("Definitions could not be installed");
		}
	}

}
