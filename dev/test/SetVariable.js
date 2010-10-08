/*
 * Name: Test.Test.SetVariable
 * Version: 1
 * ID: 10003
 * Message: Testing the vector
 * Severity: Low
 */

importPackage(Packages.ThreatScript)

function analyze( httpResponse, variables, environment ){

	variables.set("Test");
	
	return new Result( true, "Whatever");
}
