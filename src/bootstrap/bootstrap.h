

std::string& triml(std::string& s) {
	int pos(0);
	for ( ; s[pos]==' ' || s[pos]=='\t'|| s[pos]=='\r' || s[pos]=='\n'; ++pos );
		s.erase(0, pos);
	return s;
}

std::string& trimr(std::string& s) {
	int pos(s.size());
	for ( ; pos && s[pos-1]==' ' || s[pos-1]=='\t' || s[pos-1]=='\r' || s[pos-1]=='\n'; --pos );
	s	.erase(pos, s.size()-pos);
	return s;
}

std::string& trim(std::string& s) {
	return triml(trimr(s));
}

/*
 * Determine if a file exists.
 */
int fileExists (const char * fileName)
{
   struct stat buf;
   int i = stat ( fileName, &buf );
      if ( i == 0 )
      {
       return 1;
      }
      return 0;
       
}

/*
 * Retrieve the given property from the config file
 */
std::string getProperty( std::string name, std::string defaultValue ){
	std::string line; 
	std::string value = defaultValue;
	
	std::ifstream configFile;
	configFile.open("../etc/config.ini");
	
	if( configFile.good() ){

		while( !configFile.eof() ){
			
			getline(configFile, line);
			
			trim(line);
			
			int nameStart = line.find(name);
			
			if( nameStart == 0 ){
				
				unsigned int endOfName = line.find("=");
				
				if( endOfName > 0 && endOfName < line.length() ){
					value = line.substr(endOfName + 1);
				}
			}
		}
		
		configFile.close();
	}

	return value;
}

std::string getCommandArgs( bool useGUI = false ){
	std::string line;
	std::string command = " ";
	command.append(getProperty("JVM.Arguments", ""));

	command.append(" -jar nsia.jar");
	
	if( useGUI ){
		command.append(" -gui");
	}

	return command;
}

/*
 * Find the path of the JVM.
 */
std::string findJVM( bool useJavaw = false ){

	std::string javaPath;

	// 1 -- Get the Java installation path from the JAVA_HOME environment variable
	char * javaHome = getenv("JAVA_HOME");

	if( javaHome != NULL ){
		std::string java = javaHome;

		if( useJavaw ){
			java.append("\\bin\\javaw.exe");
		}
		else{
			java.append("\\bin\\java.exe");
		}

		if( fileExists( java.c_str() ) ){
			return java;
		}
	}

	// 2 -- Determine if the Java runtime is in the system32 directory
	char * systemRoot = getenv("SystemRoot");
	
	if( systemRoot != NULL ){
		std::string system32 = systemRoot;

		if( useJavaw ){
			system32.append("\\system32\\javaw.exe");
		}
		else{
			system32.append("\\system32\\java.exe");
		}

		if( fileExists( system32.c_str() ) ){
			return system32;
		}
	}
	
	// 3 -- Determine if the Java runtime is in the syswow64 directory
	if( systemRoot != NULL ){
		std::string syswow64 = systemRoot;

		if( useJavaw ){
			syswow64.append("\\syswow64\\javaw.exe");
		}
		else{
			syswow64.append("\\syswow64\\java.exe");
		}

		if( fileExists( syswow64.c_str() ) ){
			return syswow64;
		}
	}
	
	// 4 -- Assume that Java is in the path
	if( useJavaw ){
		return "javaw.exe";
	}
	else{
		return "java.exe";
	}
	
}
