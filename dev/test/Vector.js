/*
 * Name: Test.Test.Vector
 * Version: 1
 * ID: 10003
 * Message: Testing the vector
 * Severity: Low
 */

importPackage(Packages.ThreatScript)

function analyze( httpResponse, variables, environment ){

	var v = environment.get("Vector");
    
    if( v != null ){
    	v = v.getValue();
    	v.push("two");
		return new Result( true, v.length() );
	}
	else{
		v = new Vector();
		v.push("one");
		environment.set( "Vector", v, false );
		return new Result( false, v.length() );
	}
}
