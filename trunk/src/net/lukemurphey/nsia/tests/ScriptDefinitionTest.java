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
import net.lukemurphey.nsia.scan.scriptenvironment.Result;
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
	
	public void testScriptDefinition() throws ScriptException, InvalidDefinitionException, IOException, NoDatabaseConnectionException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoSuchMethodException, DefinitionEvaluationException, TestApplicationException{
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
	
	public void testSetEnvironmentDataNotShared() throws ScriptException, InvalidDefinitionException, IOException, NoDatabaseConnectionException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoSuchMethodException, DefinitionEvaluationException, TestApplicationException{
		
		// 1 -- Perform a scan against a URL, this should cause the script to save the script data with the isSpecimenSpecific flag as false
		ScriptDefinition sig = getSignatureFromFile( TestResources.TEST_RESOURCE_DIRECTORY + "SaveDataNotShared.js" );
		Application app = TestApplication.getApplication();
		
		{
			HostConfiguration hostConfig = new HostConfiguration();
			hostConfig.setHost("google.com", 80, "http");
			HttpMethod httpMethod = new GetMethod( "/" );
			httpMethod.setFollowRedirects(true);
			HttpClient httpClient = new HttpClient();
			httpClient.executeMethod( hostConfig, httpMethod );
			
			HttpResponseData httpResponse = new HttpResponseData( httpMethod, "http://google.com" );
				
			Result res = sig.evaluate(httpResponse, new Variables(), 1, app.getDatabaseConnection(DatabaseAccessType.SCANNER));
			
			if( !res.getDescription().matches("No value set") ){
				fail("The definition should not have set the variable");
			}
		}
		
		// 2 -- Perform the scan again against a different URL and make sure the script cannot set the value set by the previous scan
		HostConfiguration hostConfig2 = new HostConfiguration();
		hostConfig2.setHost("threatfactor.com", 80, "http");
		HttpMethod httpMethod2 = new GetMethod( "/" );
		httpMethod2.setFollowRedirects(true);
		HttpClient httpClient2 = new HttpClient();
		httpClient2.executeMethod( hostConfig2, httpMethod2 );
		
		HttpResponseData httpResponse2 = new HttpResponseData( httpMethod2, "http://threatfactor.com" );
		Result res2 = sig.evaluate(httpResponse2, new Variables(), 1, app.getDatabaseConnection(DatabaseAccessType.SCANNER));
		
		if( !res2.getDescription().matches("No value set") ){
			fail("The definition should not have set the variable: " + res2.getDescription());
		}
	}
	
	public void testSetEnvironmentDataIsSpecimen() throws ScriptException, InvalidDefinitionException, IOException, NoDatabaseConnectionException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoSuchMethodException, DefinitionEvaluationException, TestApplicationException{
		
		// 1 -- Perform a scan against a URL, this should cause the script to save the script data with the isSpecimenSpecific flag as false
		Application app = TestApplication.getApplication();
		ScriptDefinition sig = getSignatureFromFile( TestResources.TEST_RESOURCE_DIRECTORY + "SaveDataNoSpecimen.js" );
		
		{
			HostConfiguration hostConfig = new HostConfiguration();
			hostConfig.setHost("google.com", 80, "http");
			HttpMethod httpMethod = new GetMethod( "/" );
			httpMethod.setFollowRedirects(true);
			HttpClient httpClient = new HttpClient();
			httpClient.executeMethod( hostConfig, httpMethod );
			
			HttpResponseData httpResponse = new HttpResponseData( httpMethod, "http://google.com" );
			
			Result res = sig.evaluate(httpResponse, new Variables(), 1, app.getDatabaseConnection(DatabaseAccessType.SCANNER));
			
			if( !res.getDescription().matches("No value set") ){
				fail("The definition should not have set the variable");
			}
		}
		
		// 2 -- Perform the scan again against a different URL and see if the script can read the value set by the previous scan
		HostConfiguration hostConfig2 = new HostConfiguration();
		hostConfig2.setHost("threatfactor.com", 80, "http");
		HttpMethod httpMethod2 = new GetMethod( "/" );
		httpMethod2.setFollowRedirects(true);
		HttpClient httpClient2 = new HttpClient();
		httpClient2.executeMethod( hostConfig2, httpMethod2 );
		
		HttpResponseData httpResponse2 = new HttpResponseData( httpMethod2, "http://threatfactor.com" );
		Result res2 = sig.evaluate(httpResponse2, new Variables(), 1, app.getDatabaseConnection(DatabaseAccessType.SCANNER));
		
		if( !res2.getDescription().startsWith("Value set: 12345678") ){
			fail("The definition did not set the variable correctly: " + res2.getDescription());
		}
	}
	
	public void testVector() throws ScriptException, InvalidDefinitionException, IOException, NoDatabaseConnectionException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoSuchMethodException, DefinitionEvaluationException, TestApplicationException{
		
		// 1 -- Perform a scan against a URL, this should cause the script to save the script data with the isSpecimenSpecific flag as false
		Application app = TestApplication.getApplication();
		ScriptDefinition sig = getSignatureFromFile( TestResources.TEST_RESOURCE_DIRECTORY + "Vector.js" );
		
		HostConfiguration hostConfig = new HostConfiguration();
		hostConfig.setHost("google.com", 80, "http");
		HttpMethod httpMethod = new GetMethod( "/" );
		httpMethod.setFollowRedirects(true);
		HttpClient httpClient = new HttpClient();
		httpClient.executeMethod( hostConfig, httpMethod );
		
		HttpResponseData httpResponse = new HttpResponseData( httpMethod, "http://google.com" );
		
		// 2 -- Make sure the Vector saved the values
		Result result = sig.evaluate(httpResponse, new Variables(), 1, app.getDatabaseConnection(DatabaseAccessType.SCANNER));
		
		if( !result.getDescription().equals("1") ){
			fail("The definition did not set the variable correctly: " + result.getDescription());
		}
		
		result = sig.evaluate(httpResponse, new Variables(), 1, app.getDatabaseConnection(DatabaseAccessType.SCANNER));
		
		if( !result.getDescription().equals("2") ){
			fail("The definition did not set the vector entry correctly: " + result.getDescription());
		}
	}
	
	public void testVectorNativeArrayConversion() throws ScriptException, InvalidDefinitionException, IOException, NoDatabaseConnectionException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoSuchMethodException, DefinitionEvaluationException, TestApplicationException{
		
		// 1 -- Perform a scan against a URL, make sure the URL loads the native array items
		Application app = TestApplication.getApplication();
		ScriptDefinition sig = getSignatureFromFile( TestResources.TEST_RESOURCE_DIRECTORY + "VectorNativeArrayConversion.js" );
		
		HostConfiguration hostConfig = new HostConfiguration();
		hostConfig.setHost("google.com", 80, "http");
		HttpMethod httpMethod = new GetMethod( "/" );
		httpMethod.setFollowRedirects(true);
		HttpClient httpClient = new HttpClient();
		httpClient.executeMethod( hostConfig, httpMethod );
		
		HttpResponseData httpResponse = new HttpResponseData( httpMethod, "http://google.com" );
		
		// 2 -- Make sure the Vector saved the values
		Result result = sig.evaluate(httpResponse, new Variables(), 1, app.getDatabaseConnection(DatabaseAccessType.SCANNER));
		
		if( !result.getDescription().equals("2") ){
			fail("The definition did not populate the vector correctly: " + result.getDescription());
		}
		
		result = sig.evaluate(httpResponse, new Variables(), 1, app.getDatabaseConnection(DatabaseAccessType.SCANNER));
		
		if( !result.getDescription().equals("2") ){
			fail("The definition did not populate the vector correctly: " + result.getDescription());
		}
	}
	
	public void testSetVariable() throws ScriptException, InvalidDefinitionException, IOException, NoDatabaseConnectionException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoSuchMethodException, DefinitionEvaluationException, TestApplicationException{
		
		// 1 -- Perform a scan against a URL, make sure the URL loads the native array items
		Application app = TestApplication.getApplication();
		ScriptDefinition sig = getSignatureFromFile( TestResources.TEST_RESOURCE_DIRECTORY + "SetVariable.js" );
		
		HostConfiguration hostConfig = new HostConfiguration();
		hostConfig.setHost("google.com", 80, "http");
		HttpMethod httpMethod = new GetMethod( "/" );
		httpMethod.setFollowRedirects(true);
		HttpClient httpClient = new HttpClient();
		httpClient.executeMethod( hostConfig, httpMethod );
		
		HttpResponseData httpResponse = new HttpResponseData( httpMethod, "http://google.com" );
		
		// 2 -- Make sure the variable was set
		Variables vars = new Variables();
		sig.evaluate(httpResponse,vars, 1, app.getDatabaseConnection(DatabaseAccessType.SCANNER));
		
		if( !vars.isSet("Test") ){
			fail("The definition did not set the correctly");
		}
	}
	
}
