
#include <string> 
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/stat.h>
#include <fstream>
#include <iostream>

#include "bootstrap.h"


int main( ){

	// 1 -- Determine if the batch file exists, run it if so
	if( fileExists("Start_Server.bat") == 1 ){
		execl ("./Start_Server.bat", "Start_Server.bat", (char *)0);
	}
  
	if( fileExists("Start_Server.sh") == 1 ){
		execl ("./Start_Server.sh", "Start_Server.sh", (char *)0);
	}

	// 2 -- Find the path of the JVM
	std::string jvmPath = findJVM( );

	// 3 -- Get the path from the configuration file if it was specified
	std::string command = getProperty("JVM.Executable", jvmPath);
	command.append(getCommandArgs( false ));
	
	// 4 -- Append the arguments
	for(int c = 1; c < argc; c++){
		command.append(" ");
		command.append(argv[c]);
	}

	// 5 -- Run the application
	system( command.c_str() );
	
	return 0;
}


