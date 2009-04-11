set corePath=../../bin

cd %corePath%

REM jar cvfm %corePath%/bin/nsia.jar %corePath%/doc/scripts/Manifest.txt %corePath%/src/template.html %corePath%/bin/net
cp ../src/template.html .
jar cvfm ../bin/nsia.jar ../doc/scripts/Manifest.txt template.html net
del template.html