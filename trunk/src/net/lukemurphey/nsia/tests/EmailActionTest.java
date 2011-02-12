package net.lukemurphey.nsia.tests;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Hashtable;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.InputValidationException;
import net.lukemurphey.nsia.InvalidLocalPartException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogMessage.EventType;
import net.lukemurphey.nsia.extension.ArgumentFieldsInvalidException;
import net.lukemurphey.nsia.extension.PrototypeField;
import net.lukemurphey.nsia.response.ActionFailedException;
import net.lukemurphey.nsia.response.EmailAction;
import net.lukemurphey.nsia.response.EmailActionExtension;
import net.lukemurphey.nsia.rest.RESTRequestFailedException;
import junit.framework.TestCase;

public class EmailActionTest extends TestCase {

	Application app;
	
	public void setUp() throws TestApplicationException{
		app = TestApplication.getApplication();
	}
	
	public void tearDown(){
		TestApplication.stopApplication();
	}
	
	public void testUnconfiguredEmailAction() throws RESTRequestFailedException, IOException, InputValidationException, SQLException, NoDatabaseConnectionException, ArgumentFieldsInvalidException, InvalidLocalPartException {
		//Set a null SMTP server
		if( app.getApplicationConfiguration().getEmailSMTPServer() != null && app.getApplicationConfiguration().getEmailFromAddress() != null ){
			throw new IllegalStateException("Test of the unconfigured email action cannot be performed since both SMTP server and source email address are non-null");
		}
		
		Hashtable<String, String> config = new Hashtable<String, String>();
		
		config.put("ToAddress", "Test.test@gmail.com");
		config.put("Subject", "test");
		config.put("Body", "testing...");
		
		EmailAction action = new EmailAction(config);
		
		EventLogMessage msg = new EventLogMessage(EventType.RULE_COMPLETE_REJECTED);
		
		try{
			action.execute(msg);
		}
		catch(ActionFailedException e){
			return;
		}
		
		fail("Email action failed to trigger an exception due to an improperly configured email server.");
	}
	
	public void testDefaultValuesForEmailAction() throws RESTRequestFailedException, IOException, InputValidationException, SQLException, NoDatabaseConnectionException, ArgumentFieldsInvalidException, InvalidLocalPartException {
		
		EmailActionExtension emailExtension = new EmailActionExtension();
		
		PrototypeField[] fields = emailExtension.getFields();
		
		for (PrototypeField field : fields) {
			if( field.getName().equalsIgnoreCase("Body") && (field.getDefaultValue() == null || field.getDefaultValue().length() == 0) ){
				fail("The default value for the body of the email incident response action has no content");
			}
		}
	}
	
}
