@echo off
cd ../../
rm bin/NSIA_No_Installer.zip
zip -r bin/NSIA_No_Installer ./ -i@./doc/scripts/includeList.txt