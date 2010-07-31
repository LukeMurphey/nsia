/*
 * Name: Baseline.Property.NonLocalScripting
 * Version: 1
 * ID: 10001
 * Message: Reference to a client-side script residing on a separate domain
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
	var tagNameFilter = new TagNameFilter("script");
	var nodesList = parser.extractAllNodesThatMatch(tagNameFilter); 

	//Analyze each script tag to determine if any have a non-local script reference
	for( var c = 0; c < nodesList.size(); c++ ){

		var tag = nodesList.elementAt(c);
		var src = tag.getAttribute("src");
		
		if (src != null){
			//Determine if the URL is absolute and ensure that the location is local
			var urlMatch = urlRegex.exec( src );

			if( urlMatch != null && urlMatch[3] != null && urlMatch[3] != localhost ){
				return new Result( true, "Non-local script discovered: " + urlMatch[3] );
			}
		}
	}

	return new Result( false, "No non-local scripts detected" );
}
