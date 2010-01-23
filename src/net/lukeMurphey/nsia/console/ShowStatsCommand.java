package net.lukemurphey.nsia.console;

import java.text.DecimalFormat;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.Application.ApplicationStatusDescriptor;

public class ShowStatsCommand extends ConsoleCommand {

	public ShowStatsCommand(Application application, String... names) {
		super(null, "Displays system statistics (memory used, threads, etc.)", application, names);
	}

	public CommandResult run(String[] input) {

		// 0 -- Precondition Check
		if( input.length > 1 ){
			System.out.println("Error: too many arguments provided, syntax of the command is \"" + getSampleInvocation() + "\"");
			return CommandResult.ERROR;
		}
			
		
		ApplicationStatusDescriptor statusDesc = application.getManagerStatus();
		
		String[][] table = new String[7][2];
		
		//System.out.println("\nSystem Status:");
		//System.out.println("--------------------------");
		
		table[0] = new String[] { "System Status:", ""};
		
		if( statusDesc.getOverallStatus() == Application.ApplicationStatusDescriptor.STATUS_YELLOW ){
			table[1] = new String[] { "Overall Status:", "Warning (Yellow), " + statusDesc.getShortDescription()};
		}
		else if( statusDesc.getOverallStatus() == Application.ApplicationStatusDescriptor.STATUS_RED ){
			table[1] = new String[] { "Overall Status:", "Critical (Red), " + statusDesc.getShortDescription()};
		}
		else if( statusDesc.getOverallStatus() == Application.ApplicationStatusDescriptor.STATUS_GREEN ){
			table[1] = new String[] { "Overall Status:", "Normal (Green), " + statusDesc.getShortDescription()};
		}
		
		table[2] = new String[] { "Memory Available:", getBytesDescription( application.getMaxMemory() )};
		table[3] = new String[] { "Memory Used:", getBytesDescription( application.getUsedMemory() )};
		table[4] = new String[] { "Active Database Connections:", String.valueOf( application.getDatabaseConnectionCount() ) };
		table[5] = new String[] { "Uptime:", getTimeDescription( application.getUptime()/1000 ) };
		table[6] = new String[] { "Threads Executing:", String.valueOf( application.getThreadCount() )};
		
		System.out.println(getTableFromString(table, true));
		
		return CommandResult.EXECUTED_CORRECTLY;
	}
	
	private static String getBytesDescription( long bytes ){
		double bytesDouble = bytes;
		DecimalFormat twoPlaces = new DecimalFormat("0.00");
		
		if( bytes < 1024 )
			return bytes + " Bytes";
		else if ( bytes < 1048576 )
			return twoPlaces.format( bytesDouble/1024 ) + " KB";
		else if ( bytes < 1073741824 )
			return twoPlaces.format( bytesDouble/1048576 ) + " MB";
		else
			return twoPlaces.format( bytesDouble/1073741824 ) + " GB";
	}
	
	private static String getTimeDescription( long secs ){
		double doubleSecs = secs;
		DecimalFormat twoPlaces = new DecimalFormat("0.00");
		
		if( secs < 60 )
			return secs + " sec";
		else if ( secs < 3600 )
			return twoPlaces.format( doubleSecs/60 ) + " min";
		else if ( secs < 86400 )
			return twoPlaces.format( doubleSecs/3600 ) + " hours";
		else
			return twoPlaces.format( doubleSecs/86400 ) + " days";
	}

}
