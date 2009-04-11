@echo off
cd ../../
rm ../NSIA_No_Installer.zip
zip -r ../NSIA_No_Installer ./ -i@./Development/Scripts/includeList.txt