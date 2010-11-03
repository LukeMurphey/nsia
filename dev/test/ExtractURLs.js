/*
 * Name: Extraction.LinkExtraction.FiveURLs
 * Version: 1
 * ID: 10001
 * Message: Returns five(5) URLs
 * Severity: Low
 */

//Import the HTML parser
importPackage(Packages.HTTP);
importPackage(Packages.ThreatScript)

function analyze( httpResponse, variables, environment ){

	a = new Array();
	a[0] = new URL("http://test1.com");
	a[1] = new URL("http://test2.com");
	a[2] = new URL("http://test3.com");
	a[3] = new URL("http://test4.com");
	a[4] = new URL("http://test5.com");
	
	result = new Result( false, "Returning five URLs" );
	result.addURLs( a );
	
	return result;
}
