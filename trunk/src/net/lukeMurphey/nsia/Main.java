package net.lukeMurphey.nsia;

import java.net.BindException;

import net.lukeMurphey.nsia.Application.RunMode;
import net.lukeMurphey.nsia.tools.AppLaunch;

import org.mortbay.util.MultiException;

public class Main {

	public static final void main(String[] args) {
		
		// 1 -- Determine if the GUI should be opened or if the application should be executed as console application
		RunMode runMode = RunMode.CLI;
		
		for (String arg : args) {
			if( "-gui".equalsIgnoreCase(arg) || "--gui".equalsIgnoreCase(arg) || "-g".equalsIgnoreCase(arg) ){
				runMode = RunMode.GUI;
			}
			
			if( "-service".equalsIgnoreCase(arg) || "--service".equalsIgnoreCase(arg)){
				runMode = RunMode.SERVICE;
			}
		}
		
		//2 -- Start the application
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
	
	
	
}
