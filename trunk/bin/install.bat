@echo off
set newline=^& echo.

REM 1 -- Determine if a JRE exists
echo.
echo.
echo Step 1 of 5: Make sure a Java runtime exists
echo ---------------------------------------------------

@reg query "HKLM\SOFTWARE\JavaSoft\Java Runtime Environment" || goto JAVA_NOT_INSTALLED
goto JAVA_INSTALLED

:JAVA_NOT_INSTALLED
echo No Java runtime was found. Please install a Java runtime (JRE) to run NSIA.
goto DONE_UNSUCCESSFUL

:JAVA_INSTALLED
echo A Java runtime was found.

REM 2 -- Determine if the system appears to have been configured already
echo.
echo.
echo Step 2 of 5: Make sure that NSIA has not been configured yet
echo ---------------------------------------------------
IF EXIST ../var/database GOTO NSIA_CONFIGURED
goto NSIA_NOT_CONFIGURED

:NSIA_CONFIGURED
echo NSIA has already been setup, no need to continue
goto DONE_UNSUCCESSFUL

:NSIA_NOT_CONFIGURED
echo An existing NSIA database was not found


REM 3 -- Get the username
echo.
echo.
echo Step 3 of 5: Setup the administrator account
echo ---------------------------------------------------
echo Enter a username for the administrator account. Note that the username can contain letters, numbers, dashes and periods:
set /p username=

REM 4 -- Get the password
echo.
echo Enter a password:
set /p password=

REM 5 -- Initialize the application
echo.
echo.
echo Step 4 of 5: Initialize NSIA
echo ---------------------------------------------------
REM java -jar nsia.jar --install $username $username $password
java -jar nsia.jar --install %username% %username% %password%

echo NSIA was successfully installed!
echo Now, all you have to do is run it

REM 6 -- Print out the success message and give the user the option to run NSIA now
echo.
echo.
echo Step 5 of 5: Start NSIA
echo ---------------------------------------------------

REM    6.1 -- Tell the user how to run it in the future
echo.
echo.
echo You can start NSIA by running "ThreatFactor NSIA.exe"
echo.
echo Otherwise, you can run it with the following command: "java -jar nsia.jar"

REM    6.2 -- Give the user the option to install NSIA as a service
CHOICE /C YN /M "Would you like to install NSIA as a service; [y]es or [n]o?"
IF ERRORLEVEL 2 goto DONT_INSTALL_AS_SERVICE
IF ERRORLEVEL 1 goto INSTALL_AS_SERVICE

:INSTALL_AS_SERVICE
echo.
echo Installing NSIA service...

REM Get the local path
for %%x in (%0) do set BatchPath=%%~dpsx
for %%x in (%BatchPath%) do set BatchPath=%%~dpsx

REM Get the path to the service executable
set ServiceExe=\"%BatchPath%ThreatFactor NSIA Service.exe\"

REM Create the service
echo sc.exe create "nsia" DisplayName= "Threatfactor NSIA" binPath= "%ServiceExe%" start= auto || goto SERVICE_INSTALL_FAILED
sc.exe create "nsia" DisplayName= "Threatfactor NSIA" binPath= "%ServiceExe%" start= auto || goto SERVICE_INSTALL_FAILED

echo Done
goto START_NOW_SERVICE

:SERVICE_INSTALL_FAILED
echo NSIA could not be installed as a service (perhaps you do not have sufficient permission?)
pause

:DONT_INSTALL_AS_SERVICE
echo.
REM Continue on

REM    6.3 -- Give the option to run it now
CHOICE /C YN /M "Would you like to run NSIA now; [y]es or [n]o?"
IF ERRORLEVEL 2 goto DONT_START_NOW
IF ERRORLEVEL 1 goto START_NOW_GuI
REM CHOICE /C:yn /N /T:N,10 [y]es or [n]o?
REM set result=%ERRORLEVEL%
:START_NOW
echo Starting NSIA...
goto START_NOW_CLI

:START_NOW_CLI
echo Starting NSIA...
goto DONE_SUCCESSFUL

:START_NOW_GUI
"Threatfactor NSIA.exe"
goto DONE_SUCCESSFUL

:START_NOW_SERVICE
echo Starting NSIA as a service...
net start nsia
ping localhost -n 7 > nul
echo Done
start http://127.0.0.1:8080
goto DONE_SUCCESSFUL

:DONT_START_NOW
goto DONE_SUCCESSFUL

:DONE_SUCCESSFUL
echo.
echo NSIA was successfully installed!
echo Visit http://ThreatFactor.com to obtain a license in order to obtain the latest definitions
goto DONE

:DONE_UNSUCCESSFUL
echo NSIA was not successfully installed

:DONE