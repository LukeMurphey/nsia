/*
 * Name: Test.Test.WebClient
 * Version: 1
 * ID: 10006
 * Message: Testing the whether the web client correctly works
 * Severity: Low
 */

importPackage(Packages.ThreatScript)
importPackage(Packages.HTTP);

function analyze( httpResponse, variables, environment ){

	var location = new URL( httpResponse.getLocation() );
	var web_client = new HeadRequest( location.toString() );
	
	var httpResponse = web_client.run();
	
	return new Result( true, httpResponse.getResponseCode() );
}