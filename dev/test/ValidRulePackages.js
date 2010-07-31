/*
 * Name: 1.2.3
 * Version: 1
 * ID: 10001
 * Message: This is a test
 * Severity: Low
 */
importPackage(Packages.HTTP);
importPackage(Packages.ThreatScript)


function analyze( httpResponse, operation, variables, environment, defaultRule ){

	//var parser = httpResponse.getDocumentParser();
	var tagNameFilter = new TagNameFilter("script");
	return new Result( false, "Just a test" );
}
