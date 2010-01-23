
set corePath=C:\Users\Luke\workspace\ThreatFactor NSIA

REM Clean
DEL "%corePath%\lib\webConsole.war"

REM Create the Web-Application Archive (WAR)
cd "%corePath%\src\webarchive"
"jar" cvf "%corePath%\lib\webConsole.war" *