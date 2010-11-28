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
            ServiceBase.Run(new NSIAService());
        }
    }
}
