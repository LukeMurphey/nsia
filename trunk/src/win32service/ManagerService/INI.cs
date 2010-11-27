/*
 * User: Luke
 * Date: 11/27/2010
 * Time: 1:19 PM
 * 
 */
using System;
using System.Collections.Generic;
using System.IO;

namespace ManagerService
{
	/// <summary>
	/// Provides a method for parsing INI files.
	/// </summary>
	public class INI
	{

		/// <summary>
		/// Parse the config file into a dictionary
		/// </summary>
   		public static Dictionary<string, string> Parse( string filename ){
			
			// The resulting config dictionary
     		Dictionary<string, string> config = new Dictionary<string, string>();
        
        	try
        	{
            StreamReader reader;
            string line;
            reader = File.OpenText(filename);
            line = reader.ReadLine();
            
            while (line != null)
            {
                line = line.Trim();
                int equalSign = line.IndexOf("=");
                
                if (equalSign > 0)
                {
                    string configName = null;
                    string configValue = null;
                    
                    // Get the name
                    configName = line.Substring(0, equalSign).Trim();
                    configValue = line.Substring(equalSign+1);
                    
                    // Add the value
                    if(configName != null ){
                    	config.Add( configName, configValue );
                    }
                }

                line = reader.ReadLine();
            }
            reader.Close();
        }
        catch (Exception)
        {
            return null;// File could not be opened
        }

        return config;

    	}
	}
}
