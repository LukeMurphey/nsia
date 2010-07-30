package net.lukemurphey.nsia.tests;

import java.io.IOException;
import java.sql.SQLException;
import java.lang.reflect.*;

import javax.script.ScriptException;

import java.sql.*;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.scan.DefinitionEvaluationException;
import net.lukemurphey.nsia.scan.HttpResponseData;
import net.lukemurphey.nsia.scan.InvalidDefinitionException;
import net.lukemurphey.nsia.scan.ScriptClassLoader;
import net.lukemurphey.nsia.scan.ScriptDefinition;
import net.lukemurphey.nsia.scan.scriptenvironment.Variables;

import junit.framework.TestCase;

public class ScriptClassLoaderTest extends TestCase {

	
	protected static class RunThread extends Thread{
		
		protected String script;
		protected ScriptDefinition scriptSig;
		protected HttpResponseData httpResponse;
		protected long ruleId;
		protected Variables variables;
		protected Connection connection;
		
		public RunThread(String script, HttpResponseData httpResponse, long ruleId, Variables variables, Connection connection) throws InvalidDefinitionException{
			this.setContextClassLoader(new ScriptClassLoader());
			this.script = script;
			this.httpResponse = httpResponse;
			this.ruleId = ruleId;
			this.variables = variables;
			this.connection = connection;
			
			try {
				Class<?> loadedClass = this.getContextClassLoader().loadClass("net.lukemurphey.nsia.scanRules.ScriptSignature");
				
				Method method = loadedClass.getMethod("parse", new Class<?>[]{String.class});
				
				Object returned = method.invoke(loadedClass, new Object[]{script});
				
				scriptSig = (ScriptDefinition)returned;
				return;
				
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			
			scriptSig = ScriptDefinition.parse(script);
		}
		
		public void run(){
			
			try {
				this.getContextClassLoader().loadClass("java.lang.String");
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
			}
			
			try {
				scriptSig.evaluate(httpResponse, variables, ruleId, connection);
			} catch (DefinitionEvaluationException e) {
				e.printStackTrace();
			}

        }
	}
	
	public void testBasicScriptLoader() throws ScriptException, NoDatabaseConnectionException, SQLException, NoSuchMethodException, HttpException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, InvalidDefinitionException, InterruptedException{
		String sig = TestResources.readFileAsString( TestResources.TEST_RESOURCE_DIRECTORY + "NonLocalScripting.js" );
		
		HostConfiguration hostConfig = new HostConfiguration();
		hostConfig.setHost("analytics.blogspot.com", 80, "http");
		HttpMethod httpMethod = new GetMethod( "/" );
		httpMethod.setFollowRedirects(true);
		HttpClient httpClient = new HttpClient();
		httpClient.executeMethod( hostConfig, httpMethod );
		
		HttpResponseData httpResponse = new HttpResponseData( httpMethod, "analytics.blogspot.com" );
		
		RunThread thread = new RunThread(sig, httpResponse, 1, new Variables(), TestResources.getTestResources().getConnection() );
		thread.setContextClassLoader(new ScriptClassLoader());
		
		thread.start();
		
		Thread.sleep(1000);
		
	}
}
