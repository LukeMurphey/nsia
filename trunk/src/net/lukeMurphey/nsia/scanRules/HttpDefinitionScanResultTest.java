package net.lukeMurphey.nsia.scanRules;

import java.sql.SQLException;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.NameIntPair;
import net.lukeMurphey.nsia.NoDatabaseConnectionException;
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
