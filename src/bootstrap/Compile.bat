windres -i resource.rc -o res.o

g++ -Wall -W bootstrapCLI.cpp res.o -o "../../bin/ThreatFactor NSIA CLI.exe"

g++ -Wall -W -mwindows -Wl,-subsystem,windows bootstrapGUIWin32.cpp res.o -o "../../bin/ThreatFactor NSIA.exe"
pause
