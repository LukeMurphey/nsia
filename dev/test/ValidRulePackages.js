/*
 * Name: 1.2.3
 * Version: 1
 * ID: 10001
 * Message: This is a test
 * Severity: Low
 */


importPackage(Packages.org.htmlparser);
importPackage(Packages.org.htmlparser.filters);
importPackage(Packages.org.htmlparser.util);

importPackage(Packages.signature);
//importPackage(Packages.htmlparser);
//importPackage(Packages.net.lukeMurphey.nsia.scanRules);


function analyze( httpResponse, operation, variables, environment, defaultRule ){

	//var parser = httpResponse.getDocumentParser();
	var tagNameFilter = new TagNameFilter("script");
	return new Result( false, "Just a test" );
}
