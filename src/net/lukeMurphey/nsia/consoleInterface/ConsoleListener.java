package net.lukeMurphey.nsia.consoleInterface;

import java.io.BufferedReader;


//For the input stream
/*import java.nio.channels.Channels;
import java.io.FileDescriptor;
import java.io.FileInputStream;*/
import java.io.InputStreamReader;

import java.sql.SQLException;
import java.nio.channels.AsynchronousCloseException;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.InputValidationException;
import net.lukeMurphey.nsia.NoDatabaseConnectionException;
import net.lukeMurphey.nsia.consoleInterface.ConsoleCommand.CommandResult;

public class ConsoleListener extends Thread{

	private Application application = null;
	private ConsoleCommand[] consoleCommands = null;
	private ConsoleCommand defaultCommand = null;
	
	private final static int IN_WHITESPACE = 0;
	private final static int IN_ARGUMENT = 1;
	private final static int IN_DOUBLE_QUOTED_ARGUMENT = 2;
	
	private final static int CHAR_WHITESPACE = 0;
	private final static int CHAR_DOUBLE_QUOTE = 1;
	private final static int CHAR = 3;
	
	private boolean continueExecuting = true;
	
	private BufferedReader in = null;
	//private InputStream inputStreamChannel = null;
	private InputStreamReader inputStreamReader = null;
	
	private static ConsoleListener globalConsoleListener = null;
	
	public ConsoleListener(Application application){
		super("Console Listener");
		
		// 0 -- Precondition Check
		if( application == null )
			throw new IllegalArgumentException("The application instance cannot be null");
		
		this.application = application;
		
		consoleCommands = registerCommands();
	}
	
	
	/**
	 * Stops the console listener if one is running.
	 *
	 */
	public static synchronized void stopConsoleListener(){
		if( globalConsoleListener != null){
			globalConsoleListener.stopListener();
		}
	}
	
	public static synchronized ConsoleListener startConsoleListener( ){
		// 0 -- Precondition check
		Application application = Application.getApplication();
		
		if( application == null )
			throw new IllegalArgumentException("The application cannot be null");
		
		
		// 1 -- Create the console listener
		if( globalConsoleListener == null ){
			globalConsoleListener = new ConsoleListener(application);
			
			// TODO Find a permanent solution to the JFreeChart / Console input problem
			/*
			 * The code below creates an unused JFreeChart just so that the initialization code for JFreeChart is run. This is a workaround
			 * for a problem that causes JFreeChart to freeze whenever console input is redirected. Creating a chart before the console input
			 * is accepted prevent the problem from occurring. More information is available at the following URL:
			 * 			
			 * 		http://www.jfree.org/phpBB2/viewtopic.php?p=66469#66469
			 */
			org.jfree.chart.ChartFactory.createPieChart( "Testing", new org.jfree.data.general.DefaultPieDataset(), false, true, false);
			
			globalConsoleListener.start();
		}
		
		return globalConsoleListener;
	}
	
	/**
	 * This method creates a new console listener if one does not already exist and creates one if necessary. A reference is returned to the listener.
	 * @return
	 */
	public static synchronized ConsoleListener getConsoleListener( ){
		return globalConsoleListener;
	}
	
	public void run(){
		
		//inputStreamChannel = Channels.newInputStream((new FileInputStream(FileDescriptor.in)).getChannel());
		//inputStreamReader = new InputStreamReader(inputStreamChannel);
		//in = new BufferedReader( inputStreamReader );
		inputStreamReader = new InputStreamReader(System.in);
		in = new BufferedReader( inputStreamReader );
		
		System.out.println("\n" + Application.APPLICATION_VENDOR + " " + Application.APPLICATION_NAME + " " + Application.VERSION_MAJOR + "." + Application.VERSION_MINOR + "." + Application.VERSION_REVISION + " (http://ThreatFactor.com)");
		//System.out.println("We are here to help, just go to http://ThreatFactor.com/");
		
		if( application.getNetworkManager().sslEnabled() )
		{
			if( application.getNetworkManager().getServerPort() != 443 )
			{
				System.out.println("Web server running on: https://127.0.0.1:" + application.getNetworkManager().getServerPort());
			}
			else{
				System.out.println("Web server running on: https://127.0.0.1");
			}
		}
		else
		{
			if( application.getNetworkManager().getServerPort() != 80 )
			{
				System.out.println("Web server running on: http://127.0.0.1:" + application.getNetworkManager().getServerPort());
			}
			else{
				System.out.println("Web server running on: http://127.0.0.1");
			}
		}
		
		System.out.println();
		System.out.println("Interactive console, type help for list of commands");
		
		continueExecuting = true;
		while( continueExecuting ){

			System.out.print("> ");

			try{
				
				String text = in.readLine();
				
				if( continueExecuting && text != null ){
					continueExecuting = runCommand( text.trim() );
				}
			}
			catch(AsynchronousCloseException e){
				//Do nothing, this was likely thrown because the read-line command was interrupted during the shutdown operation
				continueExecuting = false;
			}
			catch(Exception e){
				//Catch the exception and move on, the console listener must not be allowed to exit
				System.err.println("Operation Failed: " + e.getMessage());
				e.printStackTrace();
				
				//Stop listening. Otherwise, an exception loop may occur. 
				continueExecuting = false;
			}
		}
	}
	
	/**
	 * Stop the console listener.
	 *
	 */
	public void stopListener(){
		continueExecuting = false;
		globalConsoleListener.interrupt();
	}
	
	/**
	 * Retrieves a list of the possible commands/
	 * @return
	 */
	private ConsoleCommand[] registerCommands(){
		HelpCommand help = new HelpCommand(this.application, "Help", "?");
		
		ConsoleCommand[] commands = {
				new WebServicesCommand(this.application, "WebService"),
				new ScannerCommand(this.application, "Scanner"),
				new ShowConfigCommand(this.application, "System.Config", "Config"),
				new ShowStatsCommand(this.application, "System.Stats", "Stats"),
				new ShutdownCommand(this.application, "System.Shutdown", "Exit", "Shutdown"),
				new DisableUserCommand(this.application, "User.Disable"),
				new EnableUserCommand(this.application, "User.Enable"),
				new ListUsersCommand(this.application, "User.List"),
				new SetPasswordCommand(this.application, "User.SetPassword"),
				new UnlockUserCommand(this.application, "User.Unlock"),
				new AddUserCommand(this.application, "User.Add"),
				new ShowEventsCommand(this.application, "ShowEvents"),
				new TaskListCommand(this.application, "Task.List"),
				new TaskStopCommand(this.application, "Task.Stop"),
				new EventLogLastCommand(this.application, "EventLog.Last"),
				new EventLogViewCommand(this.application, "EventLog.View"),
				help
		};
		
		
		defaultCommand = help;
		help.setCommands(commands);
		
		return commands;
	}
	
	/**
	 * Find and execute the command per the arguments given.
	 * @param command
	 * @return Boolean indicating of the console listener should continue executing
	 * @throws SQLException
	 * @throws NoDatabaseConnectionException
	 * @throws InputValidationException 
	 */
	private boolean runCommand( String command ) throws SQLException, NoDatabaseConnectionException, InputValidationException{
		
		if(command.length() == 0)
			return true;
		
		boolean syntaxProblems = false;
		//String[] input = StringUtils.split(command);
		String[] input;
		
		try{
			input = parseCommand( command );
		}catch( QuoteSequenceException e){
			System.out.println( e.getDescription() );
			return true;
		}
		 
		
		// 1 -- Try to execute the desired command
		
		for( int c = 0; c < consoleCommands.length; c++){
			
			if( consoleCommands[c].matchesName( input[0] )){
				System.out.println();//Inserts a space before the command to make easier for the user to parse the output
				CommandResult result = consoleCommands[c].run(input);
				syntaxProblems = (result == CommandResult.ERROR);
				
				if( result == CommandResult.TERMINATE_CONSOLE )
					return false;
				else
					return true;
			}
			
		}
		
		// 2 -- Show a message if the command was not recognized (execution will not get to this point unless the command failed). 
		System.out.println("Command not recognized");
		syntaxProblems = true;
		
		// 3 -- Show the help if the syntax used was incorrect
		if( syntaxProblems ){
			defaultCommand.run(input);
			/*System.out.println("Type \"help\" to see the available commands");
			//boolean firstPrinted = false;
			Vector<String[]> commands = new Vector<String[]>();
			commands.add(new String[]{"Similar Command", "Usage"});
			
			for( int c = 0; c < consoleCommands.length; c++){
				
				if( consoleCommands[c].getName().toLowerCase().startsWith( input[0].toLowerCase() ) ){
					commands.add(new String[]{consoleCommands[c].getName(), consoleCommands[c].description});
				}
			}
			
			if( commands.size() > 1 ){
				String[][] commandsArray = new String[commands.size()][];
				commands.toArray(commandsArray);
				System.out.println();
				System.out.println(ConsoleCommand.getTableFromString(commandsArray, true));
			}*/
		}
		
		// 4 -- Return true, noting that the console listener should not shutdown
		return true;
		
	}
	
	private String[] parseCommand( String input ) throws QuoteSequenceException{
		StringBuffer[] arguments = new StringBuffer[10];
		
		arguments = parseCommand(input, 0, arguments, IN_WHITESPACE, 0);
		
		// 2 -- Determine the number of final arguments
		int numberOfArgs = 0;
		for( int c = 0; c < arguments.length; c++){
			if( arguments[c] != null )
				numberOfArgs++;
		}
		
		// 3 -- Convert the arguments to Strings
		String[] argumentsString = new String[numberOfArgs];
		
		for( int c = 0; c < arguments.length; c++){
			if( arguments[c] != null )
				argumentsString[c] = new String( arguments[c] );
		}
		
		// 4 -- Return the result
		return argumentsString;
		
		
	}
	
	private StringBuffer[] parseCommand(String input, int position, StringBuffer[] arguments, int state, int currentArgumentPosition) throws QuoteSequenceException{
		
		// 1 -- Determine the state of the input
		
		//	 1.1 -- Determine the type of character
		int currentCharType;
		if(  input.charAt(position) == '"' )
			currentCharType = CHAR_DOUBLE_QUOTE;
		else if( input.charAt(position) == ' ' || input.charAt(position) == '\t' )
			currentCharType = CHAR_WHITESPACE;
		else
			currentCharType = CHAR;
		
		//	 1.2 -- Determine if we are at the end of the input
		boolean atEnd = position >= (input.length() -1 );

		//	 1.3 -- Consume the escaped the double quote
		if( atEnd == false && input.charAt(position) == '\\' ){
			if( input.charAt(position + 1) == '"' ){
				boolean escapedCharAtEnd = (position + 1) >= (input.length() -1 ); 
				
				// 1.3.1 -- Escaped double quote is last character
				if( escapedCharAtEnd ){
					if( state == IN_DOUBLE_QUOTED_ARGUMENT )
						throw new QuoteSequenceException(input, position + 1);
					else if( state == IN_ARGUMENT ){
						arguments[currentArgumentPosition].append('"');
						return arguments;
					}
					else if( state == IN_WHITESPACE){
						arguments[currentArgumentPosition] = new StringBuffer();
						arguments[currentArgumentPosition].append('"');
						return arguments;
					}

				}

				// 1.3.2 -- Escaped double quote is not the last character
				else{
					if( state == IN_DOUBLE_QUOTED_ARGUMENT )
						return parseCommand(input, position+2, arguments, IN_DOUBLE_QUOTED_ARGUMENT, currentArgumentPosition);
					else if( state == IN_ARGUMENT ){
						return parseCommand(input, position+2, arguments, IN_ARGUMENT, currentArgumentPosition);
					}
					else if( state == IN_WHITESPACE){
						arguments[currentArgumentPosition] = new StringBuffer();
						arguments[currentArgumentPosition].append('"');
						return parseCommand(input, position+2, arguments, IN_ARGUMENT, currentArgumentPosition);
					}
				}

			}
		}
		
		// 2 -- If currently in an argument
		if( state == IN_ARGUMENT ){
			
			// 2.1 -- Last character is a quote (error)
			if( atEnd && currentCharType == CHAR_DOUBLE_QUOTE){
				throw new QuoteSequenceException(input, position );
			}
			
			// 2.2 -- Append the last character and return
			if( atEnd && (currentCharType == CHAR )  ){
				arguments[currentArgumentPosition].append(input.charAt(position));
				return arguments;
			}
			
			// 2.3 -- Last character is whitespace
			if( atEnd && currentCharType == CHAR_WHITESPACE ){
				return arguments;
			}
			
			// 2.4 -- Character is part of the existing argument
			if( currentCharType == CHAR ){
				arguments[currentArgumentPosition].append(input.charAt(position));
				return parseCommand( input, position+1, arguments, IN_ARGUMENT, currentArgumentPosition);
			}
			
			// 2.5 -- Character is a double quote, don't include it in the input (this is how the Windows CLI works)
			if( currentCharType == CHAR_DOUBLE_QUOTE ){
				return parseCommand( input, position+1, arguments, IN_WHITESPACE, currentArgumentPosition);
			}
			
			// 2.6 -- Character is whitespace
			if( currentCharType == CHAR_WHITESPACE ){
				return parseCommand( input, position+1, arguments, IN_WHITESPACE, currentArgumentPosition+1);
			}
		}
		
		// 3 -- If currently in whitespace
		else if( state == IN_WHITESPACE ){
			
			// 3.1 -- At end of input
			if( atEnd && currentCharType == CHAR_WHITESPACE ){
				return arguments;
			}
			
			// 3.2 -- Found more whitespace, consume character and continue
			else if( currentCharType == CHAR_WHITESPACE ){
				return parseCommand( input, position+1, arguments, IN_WHITESPACE, currentArgumentPosition);
			}
			
			// 3.3 -- Found the start of an argument
			else if( currentCharType == CHAR ){
				arguments[currentArgumentPosition] = new StringBuffer();
				arguments[currentArgumentPosition].append(input.charAt(position));
				
				if( atEnd )
					return arguments;
				else
					return parseCommand( input, position+1, arguments, IN_ARGUMENT, currentArgumentPosition);
			}
			
			// 3.4 -- Found the start of a double-quoted argument
			else if( atEnd && currentCharType == CHAR_DOUBLE_QUOTE ){
				throw new QuoteSequenceException( input, position );
			}
			
			// 3.6 -- Found the start of a double-quoted argument
			else if( currentCharType == CHAR_DOUBLE_QUOTE ){
				arguments[currentArgumentPosition] = new StringBuffer();
				return parseCommand( input, position+1, arguments, IN_DOUBLE_QUOTED_ARGUMENT, currentArgumentPosition);
			}
		}
		
		// 4 -- If currently in double-quoted argument
		else if( state == IN_DOUBLE_QUOTED_ARGUMENT){
			
			// 3.1 -- At end of input without the closing double quotes
			if( atEnd && (currentCharType == CHAR_WHITESPACE || currentCharType == CHAR) ){
				throw new QuoteSequenceException( input, position );
			}
			
			// 3.2 -- At characters
			if( currentCharType == CHAR_WHITESPACE || currentCharType == CHAR ){
				arguments[currentArgumentPosition].append(input.charAt(position));
				return parseCommand( input, position+1, arguments, IN_DOUBLE_QUOTED_ARGUMENT, currentArgumentPosition);
			}
			
			// 3.3 -- Found the end of a double-quoted argument
			if( currentCharType == CHAR_DOUBLE_QUOTE ){
				if( atEnd )
					return arguments;
				else
					return parseCommand( input, position+1, arguments, IN_WHITESPACE, currentArgumentPosition+1);
			}
		}
		
		return arguments;
		
	}

}
