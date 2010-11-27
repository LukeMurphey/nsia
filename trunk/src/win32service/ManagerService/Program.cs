using System;
using System.Collections.Generic;
using System.Text;
using System.Diagnostics;
using System.IO;
using System.Threading;
using System.ServiceProcess;

namespace ThreatFactor.NSIA.Service
{

    class Program
    {
        /// <summary>
        /// The main entry point for the application.
        /// </summary>
        [STAThread]
        static void Main(string[] Args)
        {
            // 1 -- Determine if the user is attempting to install or uninstall the service
            /*Boolean install = false;
            Boolean uninstall = false;

            if (Args.Length > 1 && Args[1].Equals("install", StringComparison.CurrentCultureIgnoreCase))
                install = true;

            if (Args.Length > 1 && Args[1].Equals("uninstall", StringComparison.CurrentCultureIgnoreCase))
                uninstall = true;
            
            // 1.1 -- Install the service and exit
            if (install)
            {
                ManagerServiceInstaller serviceInstaller = new ManagerServiceInstaller();
                serviceInstaller.
            }*/

            // 2 -- Run the service
            ServiceBase.Run(new NSIAService());
        }
    }
}
