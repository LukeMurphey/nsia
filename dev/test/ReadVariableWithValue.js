/*
 * Name: Test.Test.ReadVariableWithValue
 * Version: 1
 * ID: 10004
 * Message: Testing the vector
 * Severity: Low
 */

importPackage(Packages.ThreatScript)

function analyze( httpResponse, variables, environment ){

	var value = variables.get("Test");
	
	if( value == "This is the value"){
		return new Result( true, "Value was returned correctly");
	}
	else{
		return new Result( false, "Value was not returned correctly");
	}
	
}
