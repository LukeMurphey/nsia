/*
 * Name: Baseline.Property.URLExists
 * Version: 1
 * ID: 10001
 * Message: Contains a URL
 * Severity: Low
 */

//Import the HTML parser
importPackage(Packages.HTTP);
importPackage(Packages.ThreatScript)

function analyze( httpResponse, operation, variables, environment, defaultRule ){

	var parser = httpResponse.getDocumentParser();
	var localhost = httpResponse.getLocation();
	var urlRegex = /(ftp|http|https):\/\/(\w+:{0,1}\w*@)?([-a-zA-Z0-9.]+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?/;

	//Get a list of all script tags
	var tagNameFilter = new TagNameFilter("a");
	var nodesList = parser.extractAllNodesThatMatch(tagNameFilter); 

	//Analyze each a tag to determine if any are URLs
	for( var c = 0; c < nodesList.size(); c++ ){

		var tag = nodesList.elementAt(c);
		var href = tag.getAttribute("href");
		
		if (href != null){
			return new Result(true, "URL exists: " + href);
		}
	}

	return new Result( false, "No URLs detected" );
}
