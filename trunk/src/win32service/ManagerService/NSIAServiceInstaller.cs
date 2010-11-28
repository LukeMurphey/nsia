
using System;
using System.Collections;
using System.ComponentModel;
using System.Configuration.Install;
using System.ServiceProcess;

namespace ThreatFactor.NSIA.Service
{
    /// <SUMMARY>
    /// The code below installs the manager service on the local system.
    /// </SUMMARY>
    [RunInstaller(true)]
    public class NSIAServiceInstaller : System.Configuration.Install.Installer
    {

        public static String SERVICE_DESCRIPTION = "Website integrity analysis system and Intrusion Detection System (IDS); can be accessed via a web interface (defaults to port 8443 if encrypted, port 8080 if unencrypted)";
        public static String SERVICE_NAME = "NSIA";
        public static String SERVICE_DISPLAY_NAME = "ThreatFactor NSIA";

        public NSIAServiceInstaller()
        {
            ServiceProcessInstaller process = new ServiceProcessInstaller();

            process.Account = ServiceAccount.LocalSystem;

            ServiceInstaller serviceAdmin = new ServiceInstaller();

            serviceAdmin.StartType = ServiceStartMode.Manual;
            serviceAdmin.ServiceName = SERVICE_NAME;
            serviceAdmin.DisplayName = SERVICE_DISPLAY_NAME;

            // Microsoft didn't add the ability to add a
            // description for the services we are going to install
            // To work around this we'll have to add the
            // information directly to the registry but I'll leave
            // this exercise for later.


            // now just add the installers that we created to our
            // parents container, the documentation
            // states that there is not any order that you need to
            // worry about here but I'll still
            // go ahead and add them in the order that makes sense.
            Installers.Add(process);
            Installers.Add(serviceAdmin);
        }
    }


}