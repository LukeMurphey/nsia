package net.lukemurphey.nsia.tests;

import java.io.IOException;
import java.sql.SQLException;

import junit.framework.TestCase;

import javax.script.ScriptException;


import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.scan.DefinitionEvaluationException;
import net.lukemurphey.nsia.scan.HttpResponseData;
import net.lukemurphey.nsia.scan.InvalidDefinitionException;
import net.lukemurphey.nsia.scan.ScriptDefinition;
import net.lukemurphey.nsia.scan.Variables;

import java.net.*;

public class ScriptDefinitionTest extends TestCase {
	
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
	
	public void test2() throws MalformedURLException{
		URL url = new URL("https://trees.com/Console?A=B");
		
		System.out.println("URL path = " + url.getPath());
		System.out.println("URL file = " + url.getFile());
		System.out.println("URL query = " + url.getQuery());
		
		System.out.println( "URL = " + url.toString() );
	}
	
	public void testSetEnvironmentData() throws ScriptException, InvalidDefinitionException, IOException, NoDatabaseConnectionException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoSuchMethodException, DefinitionEvaluationException{
		ScriptDefinition sig = getSignatureFromFile( TestResources.TEST_RESOURCE_DIRECTORY + "NonLocalScripting.js" );
		
		HostConfiguration hostConfig = new HostConfiguration();
		hostConfig.setHost("analytics.blogspot.com", 80, "http");
		HttpMethod httpMethod = new GetMethod( "/" );
		httpMethod.setFollowRedirects(true);
		HttpClient httpClient = new HttpClient();
		httpClient.executeMethod( hostConfig, httpMethod );
		
		HttpResponseData httpResponse = new HttpResponseData( httpMethod, "analytics.blogspot.com" );
		TestResources testRes = TestResources.getTestResources();
		long ms = System.currentTimeMillis();
		
		if( sig.evaluate(httpResponse, new Variables(), 1, testRes.getConnection()).matched() == false ){
			fail("The script should have matched");
		}
		
		float msTotal = System.currentTimeMillis() - ms;
		
		System.out.println( "" + msTotal/1000.0 + " total seconds for actual script signatures to evaluate"  );
	}
	
	public void testUsePackageShortCuts() throws ScriptException, InvalidDefinitionException, IOException, NoDatabaseConnectionException, SQLException, NoSuchMethodException, InstantiationException, IllegalAccessException, ClassNotFoundException, DefinitionEvaluationException{
		
		HostConfiguration hostConfig = new HostConfiguration();
		hostConfig.setHost("analytics.blogspot.com", 80, "http");
		HttpMethod httpMethod = new GetMethod( "/" );
		httpMethod.setFollowRedirects(true);
		HttpClient httpClient = new HttpClient();
		httpClient.executeMethod( hostConfig, httpMethod );
		
		HttpResponseData httpResponse = new HttpResponseData( httpMethod, "analytics.blogspot.com" );
		
		ScriptDefinition sig = getSignatureFromFile( TestResources.TEST_RESOURCE_DIRECTORY + "ValidRulePackages.js" );
		
		//System.out.println( "\n\nScript loaded...\n\n"  );
		
		sig.evaluate(httpResponse, new Variables(), 1, TestResources.getTestResources().getConnection());
		
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
	
	/*public void testSetEnvironmentData() throws ScriptException, InvalidSignatureException, IOException, NoDatabaseConnectionException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		ScriptSignature setSig = getSignatureFromFile( TestResources.TEST_RESOURCE_DIRECTORY + "SaveData.js" );
		ScriptSignature loadSig = getSignatureFromFile( TestResources.TEST_RESOURCE_DIRECTORY + "LoadData.js");
		ScriptSignature deleteSig = getSignatureFromFile( TestResources.TEST_RESOURCE_DIRECTORY + "DeleteData.js");
		
		HostConfiguration hostConfig = new HostConfiguration();
		hostConfig.setHost("LukeMurphey.net", 80, "http");
		HttpMethod httpMethod = new GetMethod( "/" );
		httpMethod.setFollowRedirects(true);
		HttpClient httpClient = new HttpClient();
		httpClient.executeMethod( hostConfig, httpMethod );
		
		HttpResponseData httpResponse = new HttpResponseData( httpMethod );
		TestResources testRes = TestResources.getTestResources();
		long ms = System.currentTimeMillis();
		
		// First, try to set the data variable
		if( setSig.evaluate(httpResponse, new Variables(), 1, testRes.getConnection()) == true ){
			fail("The script should not have found the variable");
		}
		
		// Make sure the data was loaded
		if( loadSig.evaluate(httpResponse, new Variables(), 1, testRes.getConnection()) == true ){
			fail("The script should have found the variable");
		}
		
		// Delete the variable
		if( deleteSig.evaluate(httpResponse, new Variables(), 1, testRes.getConnection()) == true ){
			fail("The script should have returned false");
		}
		
		// Try to load it again
		if( loadSig.evaluate(httpResponse, new Variables(), 1, testRes.getConnection()) == false ){
			fail("The script should not have found the variable");
		}
		
		float msTotal = System.currentTimeMillis() - ms;
		
		System.out.println( "" + msTotal/1000.0 + " total seconds for actual script signatures to evaluate (4 times)"  );
	}*/

	/*

	public void testInstantiation() throws ScriptException, HttpException, IOException, InvalidSignatureException{
		
		//This test assumes that the data on the given site has an MD5 hash of 55B5EFEC352FA389C7A8135A95E64AC7
		HostConfiguration hostConfig = new HostConfiguration();
		hostConfig.setHost("LukeMurphey.net", 80, "http");
		HttpMethod httpMethod = new GetMethod( "/" );
		httpMethod.setFollowRedirects(true);
		HttpClient httpClient = new HttpClient();
		httpClient.executeMethod( hostConfig, httpMethod );
		
		HttpResponseData httpResponse = new HttpResponseData( httpMethod );

		String script = "var md5 = Analysis.md5(HttpResponse);" +
				"if( md5 != \"55B5EFEC352FA389C7A8135A95E64AC7\"){" +
				"\tResult.matched = false;" +
				"}else{" +
				"\tResult.matched = true;" +
				"}";
		
		ScriptSignature scriptSig = new ScriptSignature(script);
		
		boolean result = scriptSig.evaluate( httpResponse, new Variables() );
		
		if( result != true ){
			fail("The script should have returned true");
		}
	}
	
	public void testRules() throws ScriptException, HttpException, IOException, InvalidSignatureException{
		
		HostConfiguration hostConfig = new HostConfiguration();
		hostConfig.setHost("cnn.com", 80, "http");
		HttpMethod httpMethod = new GetMethod( "/" );
		httpMethod.setFollowRedirects(true);
		HttpClient httpClient = new HttpClient();
		httpClient.executeMethod( hostConfig, httpMethod );
		
		HttpResponseData httpResponse = new HttpResponseData( httpMethod );

		String script = ThreatSignatureTest.readFileAsString("/home/luke/NSIA/Development/Rules/Check Scripts/NonLocalHyperLinks.js");
		
		ScriptSignature scriptSig = new ScriptSignature(script);
		
		boolean result = scriptSig.evaluate( httpResponse, new Variables() );
		
		if( result != true ){
			fail("The script should have returned true");
		}
	}
	
	public void testRules2() throws ScriptException, HttpException, IOException, InvalidSignatureException{
		String address = "bantr.com";
		
		HostConfiguration hostConfig = new HostConfiguration();
		hostConfig.setHost(address, 80, "http");
		HttpMethod httpMethod = new GetMethod( "/" );
		httpMethod.setFollowRedirects(true);
		HttpClient httpClient = new HttpClient();
		httpClient.executeMethod( hostConfig, httpMethod );
		
		HttpResponseData httpResponse = new HttpResponseData( httpMethod );
		
		String script = ThreatSignatureTest.readFileAsString("/home/luke/NSIA/Development/Rules/Check Scripts/NonLocalScripting.js");
		
		ScriptSignature scriptSig = new ScriptSignature(script);
		
		boolean result = scriptSig.evaluate( httpResponse, new Variables() );
		
		if( result != true ){
			fail("The script should have returned true");
		}
	}
	 */
	
}
