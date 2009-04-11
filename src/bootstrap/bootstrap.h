

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

int fileExists (char * fileName)
{
   struct stat buf;
   int i = stat ( fileName, &buf );
      if ( i == 0 )
      {
       return 1;
      }
      return 0;
       
}

std::string getCommandArgs( bool useGUI = false ){
	std::string line; 
	std::string command="";
	
	std::ifstream configFile;
	configFile.open("var/config.ini");
	
	if( configFile.good() ){

		while( !configFile.eof() ){
			
			getline(configFile, line);
			
			trim(line);
			
			int nameStart = line.find("JVM.Arguments");
			
			if( nameStart == 0 ){
				
				unsigned int endOfName = line.find("=");
				
				if( endOfName > 0 && endOfName < line.length() ){
					command.append( line.substr(endOfName + 1) );
				}
			}
		}
		
		configFile.close();
	}

	command.append(" -jar nsia.jar");
	
	if( useGUI ){
		command.append(" -gui");
	}

	return command;
}


/*
  Find the path of the JVM.
  */
/*String findJVM(){
	
	// 0 -- Precondition Check
	
	// 1 -- See if a local (application specific JVM exists)
	
	// 2 -- Determine if a JVM is specified in the environment
	
	// 3 -- Search through the filesystem
}*/
