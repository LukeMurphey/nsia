
set corePath=C:\Users\Luke\workspace\

REM Clean
DEL "%corePath%\NSIA\lib\webConsole.war"

REM Create the Web-Application Archive (WAR)
cd "%corePath%\NSIA\deployment"
"jar" cvf "%corePath%\NSIA\lib\webConsole.war" *