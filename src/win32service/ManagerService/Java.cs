/*
 * User: Luke
 * Date: 11/27/2010
 * Time: 2:16 PM
 * 
 */
using System;
using System.ComponentModel;
using System.Diagnostics;
using System.IO;

namespace ManagerService
{
	/// <summary>
	/// Provides methods for finding information about the installed Java runtime.
	/// </summary>
	public class Java
	{
		
    	private static string NormalizePath( String path ){
    	
    		// 1 -- If the path is null then just return it
    		if( path == null ){
    			return path;
    		}
    	
    		// 2 -- Add the trailing slash if it does not exist
    		if( !path.EndsWith(@"\") && !path.EndsWith("/") ){
    			path += @"\";
    		}
    	
    		return path;
    	}
    
		/// <summary>
		/// Get the Java installatin path from the registry
		/// </summary>
		/// <returns></returns>
		public static string GetInstallPathFromRegistry(){
			string javaKey = "SOFTWARE\\JavaSoft\\Java Runtime Environment\\";
	    	using (Microsoft.Win32.RegistryKey rk = Microsoft.Win32.Registry.LocalMachine.OpenSubKey(javaKey))
	    	{
	    		
	    		if( rk == null ){
	    			return null;
	    		}
	    		
	          string currentVersion = rk.GetValue("CurrentVersion", "").ToString();
	          
	          if(currentVersion.Length == 0 || currentVersion == null){
	          	return null;
	          }
	          
	          using (Microsoft.Win32.RegistryKey key = rk.OpenSubKey(currentVersion))
	          {
	          	if( key == null ){
	          		return null;
	          	}
	          	
	           	return key.GetValue("JavaHome").ToString() + @"\bin\java.exe";
	          }
	    	}
	    	
	    	return null;
		}
		
		/// <summary>
		/// Get the install path of Java from the JAVA_HOMEenvironment variable
		/// </summary>
		/// <returns></returns>
		public static string GetInstallPathFromEnv(){
	    	string environmentPath = Environment.GetEnvironmentVariable("JAVA_HOME");
	    	
	    	if (!string.IsNullOrEmpty(environmentPath))
	    	{
	    		return NormalizePath(environmentPath) + @"bin\java.exe";
	    	}
	    	
	    	return null;
		}
		
		/// <summary>
		/// Get the install path of Java if it exists iin the systme directory
		/// </summary>
		/// <returns></returns>
		public static string GetInstallPathFromSystem(){
			string path = Environment.GetFolderPath(Environment.SpecialFolder.System);
			path += @"\java.exe";
			
			if( File.Exists( path ) ){
				return path;
			}
			else{
				return null;
			}
		}
		
		private static string GetRuntime(string javaPath ){
			return javaPath + @"bin\java.exe";
		}
		
		/// <summary>
		/// Gets the installation path of Java 
		/// </summary>
		/// <returns></returns>
    	public static string GetJavaPath()
    	{
    		string path = null;
    		
			// 1 -- Get the Java installation path from the JAVA_HOME environment variable
			path = GetInstallPathFromEnv();
			
			if( path != null){
				return path;
			}
			
	    	// 2 -- Get the Java runtime from the registry
			path = GetInstallPathFromRegistry();
			
			if( path != null){
				return path;
			}
	    	
	    	// 3 -- Determine if the Java runtime is in the system32 drectory
	    	path = GetInstallPathFromSystem();
	    	
	    	if( path != null){
				return path;
			}
	    	
	    	// No Java was found, try running it directly (hoping it is in the path)
	    	return "java";
    	}
   	 
    	/// <summary>
    	/// Determines if Java is available inthe system directory
    	/// </summary>
    	/// <returns></returns>
    	public static bool IsJavaInSystem(){
    		string path = Environment.GetFolderPath(Environment.SpecialFolder.System);
    		return File.Exists( path );
    	}
    	
    	/// <summary>
    	/// Determines if Java is on the path.
    	/// </summary>
    	/// <returns>Boolean indicating if Java is on the system path</returns>
    	public static bool IsJavaInPath(){
	    	
	    	//Try running Java to see if it exists
	    	Process process = new Process();
	    	
	    	process.StartInfo.FileName = "java";
	    	process.StartInfo.Arguments = " -version";
	    	process.StartInfo.CreateNoWindow = true;
	    	process.StartInfo.UseShellExecute = false;
	      
	    	try{
		    	process.Start();
		    	
		    	while( process.HasExited == false ){
		    		//Wait until the process completes
		    		System.Threading.Thread.Sleep(200);
		    	}
	    	
	    	}
	    	catch(Win32Exception){
	    		return false;
	    	}
	    	
	    	return true;
	    	
	    }
	}
}
