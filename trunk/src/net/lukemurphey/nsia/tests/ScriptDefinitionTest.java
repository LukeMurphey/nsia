package net.lukemurphey.nsia.tests;

import java.io.IOException;
import java.sql.SQLException;

import junit.framework.TestCase;

import javax.script.ScriptException;


import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.Application.DatabaseAccessType;
import net.lukemurphey.nsia.scan.DefinitionEvaluationException;
import net.lukemurphey.nsia.scan.HttpResponseData;
import net.lukemurphey.nsia.scan.InvalidDefinitionException;
import net.lukemurphey.nsia.scan.ScriptDefinition;
import net.lukemurphey.nsia.scan.scriptenvironment.Variables;

public class ScriptDefinitionTest extends TestCase {
	
	public void tearDown(){
		TestApplication.stopApplication();
	}
	
	protected static ScriptDefinition getSignatureFromFile( String file ) throws ScriptException, InvalidDefinitionException, IOException{
		String script = TestResources.readFileAsString(file);
		
		return ScriptDefinition.parse(script);
	}
	
	public void testParseMultilineComments() throws ScriptException, InvalidDefinitionException, IOException{
		ScriptDefinition sig = getSignatureFromFile( TestResources.TEST_RESOURCE_DIRECTORY + "ValidRule.js" );
		if( !sig.getMessage().matches("This is a test")
				|| !sig.toString().matches("1.2.3") ){
			fail("The rule was not parsed correctly");
		}
	}
	
	public void testParseQuotedName() throws ScriptException, InvalidDefinitionException, IOException{
		ScriptDefinition sig = getSignatureFromFile( TestResources.TEST_RESOURCE_DIRECTORY + "ValidRuleQuoted.js" );
		if( !sig.getMessage().matches("This is a test")
				|| !sig.toString().matches("1.2.3") ){
			fail("The rule was not parsed correctly: " + sig.getMessage());
		}
	}
	
	public void testParseSingleLineComment() throws ScriptException, InvalidDefinitionException, IOException{
		getSignatureFromFile( TestResources.TEST_RESOURCE_DIRECTORY + "ValidRuleLineComments.js" );
	}
	
	public void testSetEnvironmentData() throws ScriptException, InvalidDefinitionException, IOException, NoDatabaseConnectionException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoSuchMethodException, DefinitionEvaluationException, TestApplicationException{
		ScriptDefinition sig = getSignatureFromFile( TestResources.TEST_RESOURCE_DIRECTORY + "URLExists.js" );
		
		HostConfiguration hostConfig = new HostConfiguration();
		hostConfig.setHost("google.com", 80, "http");
		HttpMethod httpMethod = new GetMethod( "/" );
		httpMethod.setFollowRedirects(true);
		HttpClient httpClient = new HttpClient();
		httpClient.executeMethod( hostConfig, httpMethod );
		
		HttpResponseData httpResponse = new HttpResponseData( httpMethod, "http://google.com" );
		//long ms = System.currentTimeMillis();
		
		Application app = TestApplication.getApplication();
		if( sig.evaluate(httpResponse, new Variables(), 1, app.getDatabaseConnection(DatabaseAccessType.SCANNER)).matched() == false ){
			fail("The script should have matched");
		}
		
		//float msTotal = System.currentTimeMillis() - ms;
		//System.out.println( "" + msTotal/1000.0 + " total seconds for actual script signatures to evaluate"  );
	}
	
	public void testUsePackageShortCuts() throws ScriptException, InvalidDefinitionException, IOException, NoDatabaseConnectionException, SQLException, NoSuchMethodException, InstantiationException, IllegalAccessException, ClassNotFoundException, DefinitionEvaluationException, TestApplicationException{
		
		HostConfiguration hostConfig = new HostConfiguration();
		hostConfig.setHost("google.com", 80, "http");
		HttpMethod httpMethod = new GetMethod( "/" );
		httpMethod.setFollowRedirects(true);
		HttpClient httpClient = new HttpClient();
		httpClient.executeMethod( hostConfig, httpMethod );
		
		HttpResponseData httpResponse = new HttpResponseData( httpMethod, "http://google.com" );
		
		ScriptDefinition sig = getSignatureFromFile( TestResources.TEST_RESOURCE_DIRECTORY + "ValidRulePackages.js" );
		
		Application app = TestApplication.getApplication();
		sig.evaluate(httpResponse, new Variables(), 1, app.getDatabaseConnection(DatabaseAccessType.SCANNER));
		
		if( !sig.getMessage().matches("This is a test")
				|| !sig.toString().matches("1.2.3") ){
			fail("The rule was not parsed correctly");
		}
	}
	
	public void testInvasiveFlagSet() throws InvalidDefinitionException{
		String rule ="";
		
		rule += "/*";
		rule += "* Name: Test.Test.InvasiveFlag\n";
		rule += "* Version: 1\n";
		rule += "* ID: 203\n";
		rule += "* Message: Test\n";
		rule += "* Severity: Low\n";
		rule += "* Invasive: True\n";
		rule += "*/\n";
		
		rule += "function analyze( httpResponse, operation, variables, environment, defaultRule ){\n";
		rule += "	return new Result( false, \"Just a Test\" );\n";
		rule += "}";
		
		ScriptDefinition sig = ScriptDefinition.parse(rule);
		
		if( sig.isInvasive() == false ){
			fail("The rule invasive flag was not set properly");
		}
	}
	
}
