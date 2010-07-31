/*
 * Name: 1.2.3
 * Version: 1
 * ID: 100001
 * Message: This is a test
 * Severity: Low
 */

importPackage(Packages.net.lukeMurphey.nsia.scanRules);

function analyze( httpResponse, operation, variables, environment, defaultRule ){

	var data = environment.get( "Test" );

	if( data.length < 1 || data[0].getValue() != "Value" ){
		return new Result( true, "Data was not the expected value");
	}
	else{
		environment.removeAll();//Data.remove( 0 );
	}

	return new Result( false, "Test");
}
