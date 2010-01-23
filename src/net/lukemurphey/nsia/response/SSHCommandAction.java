package net.lukemurphey.nsia.response;

import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.extension.ArgumentFieldsInvalidException;
import net.lukemurphey.nsia.extension.FieldLayout;
import net.lukemurphey.nsia.extension.FieldPassword;
import net.lukemurphey.nsia.extension.FieldText;
import net.lukemurphey.nsia.extension.MessageValidator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Hashtable;
import java.util.Vector;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.Session;
import com.trilead.ssh2.StreamGobbler;

public class SSHCommandAction extends Action {

	private static final long serialVersionUID = 1L;
	
	private String hostname = null;
	private String username = null;
	private String password = null;
	private String commands = null;
	public static final String USER_DESCRIPTION = "Run a command via an SSH session";
	
	protected SSHCommandAction( Hashtable<String, String> arguments ) {
		super("SSH Command", USER_DESCRIPTION);
		
		this.hostname = arguments.get("Hostname");
		this.username = arguments.get("Username");
		this.password = arguments.get("Password");
		this.commands = arguments.get("Commands");
		
	}
	
	protected SSHCommandAction(String hostname, String username, String password, String commandList ) {
		super("SSH Command", USER_DESCRIPTION);
		
		// 0 -- Precondition check
		
		//	 0.1 -- The host name cannot be null
		if( hostname == null ){
			throw new IllegalArgumentException("The hostname to connect to cannot be null");
		}
		
		//	 0.2 -- The host name cannot be empty
		if( hostname.isEmpty() ){
			throw new IllegalArgumentException("The hostname to connect to cannot be empty");
		}
		
		//	 0.3 -- The username cannot be null
		if( username == null ){
			throw new IllegalArgumentException("The username cannot be null");
		}
		
		//	 0.4 -- The user name cannot be empty
		if( username.isEmpty() ){
			throw new IllegalArgumentException("The username cannot be empty");
		}
		
		//	 0.5 -- The password cannot be null
		if( password == null ){
			throw new IllegalArgumentException("The password cannot be null");
		}
		
		//	 0.6 -- The list of commands cannot be null
		if( commandList == null ){
			throw new IllegalArgumentException("The list of commands to execute cannot be null");
		}
		
		//	 0.7 -- The list of commands cannot be empty
		if( commandList.isEmpty() ){
			throw new IllegalArgumentException("The list of commands to execute cannot be empty");
		}
		
		
		// 1 -- Initialize the class
		this.hostname = hostname;
		this.username = username;
		this.password = password;
	}

	@Override
	public Hashtable<String, String> getValues(){
		Hashtable<String, String> values = new Hashtable<String, String>();
		
		values.put("Hostname", this.hostname);
		values.put("Username", this.username);
		values.put("Password", this.password);
		values.put("Commands", this.commands);
		
		return values;
	}
	
	@Override
	public FieldLayout getLayoutWithValues(){
		FieldLayout layout = SSHCommandAction.getLayout();
		layout.setFieldsValues(this.getValues());
		return layout;
	}

	
	@Override
	public void execute(EventLogMessage logMessage) throws ActionFailedException {

		try{
			// 1 -- Process the commands to get the actual command list
			String[] processedCommands = splitUpCommands(commands);
			Vector<MessageVariable> vars = new Vector<MessageVariable>();
			
			for (int c = 0; c < processedCommands.length; c++) {
				processedCommands[c] = MessageVariable.processMessageTemplate(processedCommands[c], vars);
			}
			
			// 2 -- Execute the commands
			runSSHCommands(processedCommands, hostname, username, password);
		}
		catch(IOException e){
			throw new ActionFailedException("Exception thrown while attempting to execute SSH commands", e);
		}
	}

	public static FieldLayout getLayout(){
		FieldLayout layout = new FieldLayout(1);
		
		// 1 -- Add the hostname field
		layout.addField( new FieldText("Hostname", "Hostname", 1, 1, new MessageValidator()) );
		
		// 2 -- Add the username field
		layout.addField( new FieldText("Username", "Login Name", 1, 1, new MessageValidator()) );
		
		// 3 -- Add the password field
		layout.addField( new FieldPassword("Password", "Password", 1, new MessageValidator()) );
		
		// 4 -- Add the commands list field
		layout.addField( new FieldText("Commands", "Commands", 1, 10, new MessageValidator()) );
		
		// 3 -- Return the resulting layout
		return layout;
	}
	
	private String[] splitUpCommands(String commands) throws IOException{
		StringReader stringReader = new StringReader(commands);
		BufferedReader reader = new BufferedReader(stringReader);
		
		Vector<String> commandList = new Vector<String>();
		
		String lastLine = null;
		while((lastLine = reader.readLine()) != null){
			commandList.add( lastLine );
		}
		
		String[] commandsArray = new String[commandList.size()];
		commandList.toArray(commandsArray);
		
		return commandsArray;
	}
	private void runSSHCommands(String[] commands, String hostname, String username, String password) throws IOException{
		
		Connection conn = null;
		Session sess = null;
		
		try
		{
			// Create a connection to the SSH server
			conn = new Connection(hostname);
			conn.connect();

			/* Authenticate.
			 * If you get an IOException saying something like
			 * "Authentication method password not supported by the server at this stage."
			 * then the SSH server does not support password authentication.
			 */
			boolean isAuthenticated = conn.authenticateWithPassword(username, password);

			if (isAuthenticated == false){
				throw new IOException("Authentication failed.");
			}

			// Execute each command
			for(String command : commands){
				// Create a session
				sess = conn.openSession();
				
				sess.execCommand(command);
				
				/* 
				 * This basic example does not handle stderr, which is sometimes dangerous
				 * (please read the FAQ).
				 */
				// Read in standard output
				InputStream stdout = new StreamGobbler(sess.getStdout());

				BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
				StringBuffer standardOut = new StringBuffer();
				
				while (true)
				{
					String line = br.readLine();
					if (line == null)
						break;
					standardOut.append(line);
				}

				//int exitCode = sess.getExitStatus();
				sess.close();
			}
		}
		finally{
			if( conn != null ){
				conn.close();
			}
		}
	}
	
	@Override
	public void configure( Hashtable<String, String> arguments ) throws ArgumentFieldsInvalidException{
		this.hostname = arguments.get("Hostname");
		this.username = arguments.get("Username");
		this.password = arguments.get("Password");
		this.commands = arguments.get("Commands");
	}
	
	@Override
	public String getConfigDescription() {
		return username + "@" + hostname;
	}

}
