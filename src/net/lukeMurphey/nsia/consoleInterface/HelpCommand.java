package net.lukeMurphey.nsia.consoleInterface;

import java.util.Vector;

import net.lukeMurphey.nsia.Application;

public class HelpCommand extends ConsoleCommand {

	private ConsoleCommand[] consoleCommands = null;
	
	public HelpCommand( Application application, String... names ){
		super(null, "Display the interactive console command syntax", application, names);
	}
	
	public void setCommands( ConsoleCommand[] consoleCommands){
		this.consoleCommands = new ConsoleCommand[consoleCommands.length];
		System.arraycopy(consoleCommands, 0, this.consoleCommands, 0, this.consoleCommands.length); 
	}
	
	public CommandResult run(String[] input) {
		
		if( input.length >= 1 && matchesName(input[0]) == false && similarCommands(input) == true ){
			
		}
		else if( input.length != 2 || listCommandHelp( input[1] ) == false ){
			
			System.out.println("Interactive console help: type \"help <command>\" for command details");
			listCommands();
			
			if( Application.VERSION_STATUS == null ){
				System.out.println("\n" + Application.APPLICATION_VENDOR + " " + Application.APPLICATION_NAME + " version " + Application.VERSION_MAJOR + "." + Application.VERSION_MINOR + "." + Application.VERSION_REVISION );
			}
			else{
				System.out.println("\n" + Application.APPLICATION_VENDOR + " " + Application.APPLICATION_NAME + " version " + Application.VERSION_MAJOR + "." + Application.VERSION_MINOR + "." + Application.VERSION_REVISION + " (" + Application.VERSION_STATUS + ")");
			}
			System.out.println("http://ThreatFactor.com/");
		}
		
		return CommandResult.EXECUTED_CORRECTLY;
	}
	
	private boolean similarCommands( String[] input ){
		
		Vector<String[]> commands = new Vector<String[]>();
		commands.add(new String[]{"Similar Command", "Usage"});
		
		for( int c = 0; c < consoleCommands.length; c++){
			
			if( consoleCommands[c].getName().toLowerCase().startsWith( input[0].toLowerCase() ) ){
				commands.add(new String[]{consoleCommands[c].getName(), consoleCommands[c].description});
			}
		}
		
		if( commands.size() > 0 ){
			
			System.out.println("Type \"help\" to see the available commands");
			
			if( commands.size() > 1 ){
				String[][] commandsArray = new String[commands.size()][];
				commands.toArray(commandsArray);
				System.out.println();
				System.out.println(ConsoleCommand.getTableFromString(commandsArray, true));
				
			}
			return true;
		}
		else{
			return false;
		}
	}
	
	private boolean listCommandHelp( String name ){
		
		for( int c = 0; c < consoleCommands.length; c++){
			if( consoleCommands[c].matchesName(name) ){
				
				String[][] table = new String[3][];
				
				table[0] = new String[]{"Command Help", ""};
				table[1] = new String[]{"Description:", consoleCommands[c].description};
				table[2] = new String[]{"Syntax:", consoleCommands[c].getSampleInvocation()};
				
				System.out.println(ConsoleCommand.getTableFromString(table, true));
				return true;
			}
		}
		
		return false;
	}
	
	private void listCommands(){
		Vector<String[]> commands = new Vector<String[]>();
		commands.add(new String[]{"Command", "Description"});
		
		for( int c = 0; c < consoleCommands.length; c++){
			commands.add(new String[]{consoleCommands[c].getName(), consoleCommands[c].description});
		}
		
		String[][] commandsArray = new String[commands.size()][];
		commands.toArray(commandsArray);
		
		System.out.println();
		System.out.println(ConsoleCommand.getTableFromString(commandsArray, true));
	}

}
