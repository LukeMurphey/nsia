
corePath="/home/luke"
rm "$corePath/NSIA/lib/webConsole.war"

#Create the Web-Application Archive (WAR)
cd "$corePath/NSIA/deployment"
jar cvf "$corePath/NSIA/lib/webConsole.war" *

