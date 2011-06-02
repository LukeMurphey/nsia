/*
 * Name: Test.Test.HTMLParse
 * Version: 1
 * ID: 10005
 * Message: Testing the parsing of HTML with the ThreatScripting parsing library
 * Severity: Low
 */

importPackage(Packages.ThreatScript)
importPackage(Packages.HTTP)

function analyze( httpResponse, variables, environment ){

	var parser = Parser.parse("<html><head></head><body></body></html>");
	
    var tagNameFilter = new TagNameFilter("head");
    var nodesList = parser.extractAllNodesThatMatch(tagNameFilter);
    
    return new Result( true, nodesList.size() );
}

