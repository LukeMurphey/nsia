package net.lukeMurphey.nsia.scanRules;

public class ScriptClassShutter implements sun.org.mozilla.javascript.internal.ClassShutter {

	public boolean visibleToScripts(String className) {
		System.out.println("Requesting " + className);
		
		/*if( className.startsWith("org") )
			return false;
		
		if( className.startsWith("com") )
			return false;
		
		if( className.startsWith("net") )
			return false;
		
		if( className.startsWith("edu") )
			return false;*/
		
		/*if( className.equals("net.lukeMurphey.nsia.scanRules.Result") ){
			return true;
		}*/
		
		return true;
	}

}
