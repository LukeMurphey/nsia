package net.lukemurphey.nsia.scan;

import java.sql.SQLException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NameIntPair;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import junit.framework.TestCase;
import java.util.*;

import com.martiansoftware.jsap.JSAPException;

public class HttpDefinitionScanResultTest extends TestCase {

	public void testGetSignatureMatchSeveritiesLong() {
		fail("Not yet implemented");
	}

	public void testGetSignatureMatchesApplicationLong() throws SQLException, NoDatabaseConnectionException, InstantiationException, IllegalAccessException, ClassNotFoundException, JSAPException {
		
		Application app = new Application( );
		
		Vector<NameIntPair> results = HttpDefinitionScanResult.getSignatureMatches(app, 851);
		
		for(int c = 0; c < results.size(); c++ ){
			System.out.println( results.get(c).getName() + " = " + results.get(c).getValue() );
		}
		
		app.shutdown();
	}

}
