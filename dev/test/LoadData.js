/*
 * Name: 1.2.3
 * Version: 1
 * ID: 100001
 * Message: This is a test
 * Severity: Low
 */

Result.matched = false;
Result.description = "Test";

var data = Data.get( "Test" );

if( data.length < 1 || data[0].getValue() != "Value" ){
	Result.matched = true;
	Result.description = "Data was not the expected value";
}
