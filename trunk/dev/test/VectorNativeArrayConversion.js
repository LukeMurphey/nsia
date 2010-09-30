/*
 * Name: Test.Test.Vector2
 * Version: 1
 * ID: 10004
 * Message: Testing the vector
 * Severity: Low
 */

importPackage(Packages.ThreatScript)

function analyze( httpResponse, variables, environment ){

	var v = environment.get("Vector");
    
    if( v != null ){
    	v = v.getValue();
		return new Result( true, v.length() );
	}
	else{
		v = new Vector();
		
		a = ["one", "two"]
		
		v.pushAll(a);
		environment.set( "Vector", v, false );
		return new Result( false, v.length() );
	}
}
