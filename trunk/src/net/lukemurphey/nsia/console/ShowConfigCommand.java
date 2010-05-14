package net.lukemurphey.nsia.console;

import java.sql.SQLException;
import java.util.Vector;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.eventlog.EventLogMessage;

public class ShowConfigCommand extends ConsoleCommand {

	public ShowConfigCommand(Application application, String... names) {
		super(null, "Displays the system configuration (listening port, number of CPUs, etc)", application, names);
	}

	public CommandResult run(String[] input) {
		// 0 -- Precondition Check
		if( input.length > 1 ){
			System.out.println("Error: too many arguments provided, syntax of the command is \"" + getSampleInvocation() + "\"");
			return CommandResult.ERROR;
		}
			
		
		Vector<String[]> table = new Vector<String[]>();
		table.add( new String[] { "System Configuration:", ""} );
		
		//System.out.println("\nSystem Configuration:");
		//System.out.println("--------------------------");
		
		table.add( new String[] { "JVM Vendor:", application.getJvmVendor()} );
		table.add( new String[] { "JVM Version:", application.getJvmVersion()});
		
		table.add( new String[] { "Database Connection:", application.getDatabaseInfo()} );
		table.add( new String[] { "Database Driver:", application.getDatabaseDriver()} );
		
		try {
			table.add( new String[] { "Database Driver Name/Version:", application.getDatabaseDriverName() + ", " + application.getDatabaseDriverVersion()} );
			table.add( new String[] { "Database Name/Version:", application.getDatabaseName() + ", " + application.getDatabaseVersion()} );
			
		} catch (SQLException e) {
			application.logExceptionEvent( EventLogMessage.EventType.SQL_EXCEPTION, e );
		} catch (NoDatabaseConnectionException e) {
			//application.logExceptionEvent(StringTable.MSGID_DATABASE_FAILURE, e);
		}

		table.add( new String[] { "Operating System:", application.getOperatingSystemName()} );
		table.add( new String[] { "Operating System Version:", application.getOperatingSystemVersion()} );
		
		table.add( new String[] { "Platform Architecture:", application.getPlatformArch()} );
		table.add( new String[] { "Processor Count:", String.valueOf( application.getProcessorCount() )} );
		
		table.add( new String[] { "Server Port:", String.valueOf( application.getNetworkManager().getServerPort() )} );
		table.add( new String[] { "SSL Enabled:", String.valueOf( application.getNetworkManager().sslEnabled() )} );
		
		String [][] tableData = new String[table.size()][];
		table.toArray(tableData);
		
		System.out.println(getTableFromString(tableData, true));
		
		return CommandResult.EXECUTED_CORRECTLY;
	}

}
