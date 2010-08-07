package net.lukemurphey.nsia;

import java.net.BindException;

import net.lukemurphey.nsia.Application.RunMode;
import net.lukemurphey.nsia.scan.DefinitionArchive;
import net.lukemurphey.nsia.tools.AppLaunch;

import org.mortbay.util.MultiException;

public class Main {

	public static final void main(String[] args) {
		
		// 1 -- Determine if the GUI should be opened or if the application should be executed as console application
		RunMode runMode = RunMode.CLI;
		
		for (String arg : args) {
			if( "-gui".equalsIgnoreCase(arg) || "--gui".equalsIgnoreCase(arg) || "-g".equalsIgnoreCase(arg) ){
				runMode = RunMode.GUI;
			}
			
			if( "-s".equalsIgnoreCase(arg) || "-service".equalsIgnoreCase(arg) || "--service".equalsIgnoreCase(arg)){
				runMode = RunMode.SERVICE;
			}
		}
		
		// 2 -- Determine if this call is to complete the installation of NSIA
		if( args != null && args.length == 4 && args[0].equalsIgnoreCase("--install")){
			try{
				Application app = Application.startApplication(null, RunMode.CLI, false);
				System.out.println("Completing installation");
				
				if( completeInstall(args[1], args[3], args[2], app) ){
					app.shutdown(false);
					System.exit(0);
				}
				else{
					app.shutdown(false);
					System.exit(-1);
				}
			}
			catch (Exception e) {
				System.err.println("Fatal Exception, application terminating (Stack Trace Follows)");
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
		// 3 -- Start the application
		if( runMode == RunMode.GUI ){
			AppLaunch.main(args);
		}
		else{
			try {
				Application.startApplication( args, runMode );
			} catch (MultiException e){
				if( e.getException(0).getClass() == BindException.class ){
					System.err.println("Port could not be opened, application terminating");
					System.exit(-1);
				}
				else{
					System.err.println("Fatal Exception, application terminating (Stack Trace Follows)");
					e.printStackTrace();
					System.exit(-1);
				}
			} catch (Exception e) {
				System.err.println("Fatal Exception, application terminating (Stack Trace Follows)");
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
	
	private static boolean completeInstall( String username, String password, String realName, Application app ){
		
		UserManagement userManagement = new UserManagement(app);
		
		int errors = 0;
		
		try{
			// Add the user account
			if( userManagement.addAccount(username, realName, password, "SHA-512", 10000, null, true) < 0){
				errors = errors + 1;
			}
			
			// Try to load the default definitions
			DefinitionArchive archive = DefinitionArchive.getArchive();
			archive.loadDefaultDefinitions();
			
			if( errors == 0 ){
				return true;
			}
			else{
				return false;
			}
		}
		catch(Exception e){
			return false;
		}
	}
	
	
	
}
