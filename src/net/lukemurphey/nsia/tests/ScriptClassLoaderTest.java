package net.lukemurphey.nsia.tests;

import net.lukemurphey.nsia.scan.ScriptClassLoader;

import junit.framework.TestCase;

public class ScriptClassLoaderTest extends TestCase {
	
	public void testGetShortcut() throws ClassNotFoundException{
		ScriptClassLoader classLoader = new ScriptClassLoader();
		classLoader.loadClass("ThreatScript.Result");
	}
}
