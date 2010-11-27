/*
 * User: Luke
 * Date: 11/27/2010
 * Time: 1:46 PM
 * 
 */

using System;
using NUnit.Framework;
using System.Collections.Generic;
using System.IO;
using ThreatFactor.NSIA.Service;

namespace ThreatFactor.NSIA.Service.Test
{
	[TestFixture]
	public class NSIAServiceTest
	{
		[Test]
		public void TestINIParse()
		{
			
			Console.WriteLine( Directory.GetCurrentDirectory() );
			Dictionary<string, string> config = INI.Parse(@"..\..\config.ini");
			
			Assert.AreEqual( config["JVM.Arguments"], "-xms256" );
			Assert.AreEqual( config["JVM.Path"], @"C:\Program Files\Java\bin\java.exe" );
		}
		
		[Test]
		public void TestIsJavaInPath()
		{
			Assert.True(Java.IsJavaInPath());
		}
		
		[Test]
		public void TestGetInstallPathFromEnv(){
			string path = Java.GetInstallPathFromEnv();
			
			Assert.AreEqual(@"C:\Program Files\Java\jdk1.6.0_20\bin\java.exe", path);
			Assert.True( File.Exists(path) );
		}
		
		[Test]
		public void TestGetInstallPathFromRegistry(){
			string path = Java.GetInstallPathFromRegistry();
			
			Assert.AreEqual(@"C:\Program Files\Java\jre6\bin\java.exe", path);
			Assert.True( File.Exists(path) );
		}
		
		[Test]
		public void TestGetInstallPathFromSystem(){
			string path = Java.GetInstallPathFromSystem();
			
			Assert.AreEqual(@"C:\WINDOWS\system32\java.exe", path);
			Assert.True( File.Exists(path) );
		}
		
		
		[Test]
		public void TestGetJavaInstallPath()
		{
			string path = Java.GetJavaPath();
			
			Assert.AreEqual(@"C:\Program Files\Java\jdk1.6.0_20\bin\java.exe", path);
			Assert.True( File.Exists(path) );
		}
	}
}
