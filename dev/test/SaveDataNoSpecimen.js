/*
 * Name: Baseline.Property.SaveDataNoSpecimen
 * Version: 1
 * ID: 10002
 * Message: Testing the setting of a script
 * Severity: Low
 */

importPackage(Packages.ThreatScript)

function analyze( httpResponse, variables, environment ){

	var t = environment.get("TestString");

        if( t != null ){
		return new Result( true, "Value set: " + t.getValue() );
	}
	else{
		environment.set( "TestString", "12345678", false );
		return new Result( false, "No value set" );
	}
}
