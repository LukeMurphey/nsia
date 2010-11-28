
#include <string> 
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/stat.h>
#include <fstream>
#include <iostream>

#include "bootstrap.h"


int main( int argc, char* argv[] ){

	// 1 -- Determine if the batch file exists, run it if so
	if( fileExists("Start_Server.bat") == 1 ){
		execl ("./Start_Server.bat", "Start_Server.bat", (char *)0);
	}
  
	if( fileExists("Start_Server.sh") == 1 ){
		execl ("./Start_Server.sh", "Start_Server.sh", (char *)0);
	}

	//If the batch file does not exist, then run it directly
	std::string command = getProperty("JVM.Executable", "java ");
	command.append(getCommandArgs( true ));
	
	system( command.c_str() );
	
	return 0;
}


