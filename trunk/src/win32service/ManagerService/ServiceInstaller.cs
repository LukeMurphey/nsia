
using System;
using System.Collections;
using System.ComponentModel;
using System.Configuration.Install;
using System.ServiceProcess;
namespace ManagerService
{
    /// <SUMMARY>
    /// The code below installs the manager service on the local system.
    /// </SUMMARY>
    [RunInstaller(true)]
    public class ManagerServiceInstaller : System.Configuration.Install.Installer
    {

        public static String SERVICE_DESCRIPTION = "Website integrity analysis system and Intrusion Detection System (IDS); can be accessed via a web interface (defaults to port 8443 if encrypted, port 8080 if unencrypted)";
        public static String SERVICE_NAME = "NSIA";
        public static String SERVICE_DISPLAY_NAME = "ThreatFactor NSIA";

        public ManagerServiceInstaller()
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
            Installers.Add( process );
            Installers.Add( serviceAdmin );
        }


        // Note: installer overridden to allow the specification of a description
        // See: http://www.codeproject.com/dotnet/dotnetscmdescription.asp
        public override void Install(IDictionary stateServer)
        {
            Microsoft.Win32.RegistryKey system,
                //HKEY_LOCAL_MACHINE\Services\CurrentControlSet
              currentControlSet,
                //...\Services
              services,
                //...\<Service Name>
              service,
                //...\Parameters - this is where you can put service-specific configuration
              config;

            try
            {
                //Let the project installer do its job
                base.Install(stateServer);

                //Open the HKEY_LOCAL_MACHINE\SYSTEM key
                system = Microsoft.Win32.Registry.LocalMachine.OpenSubKey("System");
                //Open CurrentControlSet
                currentControlSet = system.OpenSubKey("CurrentControlSet");
                //Go to the services key
                services = currentControlSet.OpenSubKey("Services");
                //Open the key for your service, and allow writing
                service = services.OpenSubKey(SERVICE_NAME, true);
                //Add your service's description as a REG_SZ value named "Description"
                service.SetValue("Description", SERVICE_DESCRIPTION);
                //(Optional) Add some custom information your service will use...
                config = service.CreateSubKey("Parameters");
            }
            catch (Exception e)
            {
                Console.WriteLine("An exception was thrown during service installation:\n" + e.ToString());
            }
        }

        public override void Uninstall(IDictionary stateServer)
        {
            Microsoft.Win32.RegistryKey system,
              currentControlSet,
              services,
              service;

            try
            {
                //Drill down to the service key and open it with write permission
                system = Microsoft.Win32.Registry.LocalMachine.OpenSubKey("System");
                currentControlSet = system.OpenSubKey("CurrentControlSet");
                services = currentControlSet.OpenSubKey("Services");
                service = services.OpenSubKey(SERVICE_NAME, true);
                //Delete any keys you created during installation (or that your service created)
                service.DeleteSubKeyTree("Parameters");
                //...
            }
            catch (Exception e)
            {
                Console.WriteLine("Exception encountered while uninstalling service:\n" + e.ToString());
            }
            finally
            {
                //Let the project installer do its job
                base.Uninstall(stateServer);
            }
        }
    }


}