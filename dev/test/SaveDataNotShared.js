/*
 * Name: Baseline.Property.SaveDataNoSpecimen
 * Version: 1
 * ID: 10003
 * Message: Testing the setting of script data that is not shared with other URLs
 * Severity: Low
 */

importPackage(Packages.ThreatScript)

function analyze( httpResponse, variables, environment ){

	var t = environment.get("TestString");

        if( t != null ){
		return new Result( true, "Value set: " + t.getValue() );
	}
	else{
		environment.set( "TestString", "12345678" );
		return new Result( false, "No value set" );
	}
}
