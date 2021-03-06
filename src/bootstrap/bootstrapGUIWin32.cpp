
#include <string> 
#include <unistd.h>
#include <sys/stat.h>
#include <fstream>

#include <windows.h>
#include "bootstrap.h"


/*
 * This program starts up the server by detecting the most appropriate Java runtime environment and setting up various other parameters in order to ensure that the server starts up correctly.
 */
int WinMain(HINSTANCE,HINSTANCE,LPSTR,int){	
	
	// 1 -- Find the path of the JVM
	std::string jvmPath = findJVM( true );

	// 2 -- Get the command path
	std::string command = getCommandArgs(true);

	// 3 -- Start NSIA
	HINSTANCE hInst = ShellExecute(0,                           
			"open",          // Operation to perform
			getProperty("JVM.Executable", jvmPath).c_str(),         // Application name
			command.c_str(), // Additional parameters
			0,               // Default directory
			SW_SHOW);

	// 4 -- Handle the return code
	if(reinterpret_cast<int>(hInst) <= 32)
	{
		// Could not start application
		switch(reinterpret_cast<int>(hInst))
		{
		case 0:
			// The operating system is out of memory or resources.
			break;

		case ERROR_FILE_NOT_FOUND: //or SE_ERR_FNF
			// The specified file was not found.
			break;

		case ERROR_PATH_NOT_FOUND: //or SE_ERR_PNF
			// The specified path was not found.
			break;

		case ERROR_BAD_FORMAT:
			// The .exe file is invalid (non-Microsoft Win32 .exe or error in .exe image).
			break;

		case SE_ERR_ACCESSDENIED:
			// The operating system denied access to the specified file.
			break;

		case SE_ERR_ASSOCINCOMPLETE:
			// The file name association is incomplete or invalid.
			break;

		case SE_ERR_DDEBUSY:
			// The Dynamic Data Exchange (DDE) transaction could not be completed because
			// other DDE transactions were being processed.
			break;

		case SE_ERR_DDEFAIL:
			// The DDE transaction failed.
			break;

		case SE_ERR_DDETIMEOUT:
			// The DDE transaction could not be completed because the request timed out.
			break;

		case SE_ERR_DLLNOTFOUND:
			// The specified dynamic-link library (DLL) was not found.

		case SE_ERR_NOASSOC:
			// There is no application associated with the given file name extension.
			// This error will also be returned if you attempt to print a file that is
			// not printable.
			break;

		case SE_ERR_OOM:
			// There was not enough memory to complete the operation.
			break;

		case SE_ERR_SHARE:
			// A sharing violation occurred.
			break;
		}
	}

	return 0;
}
