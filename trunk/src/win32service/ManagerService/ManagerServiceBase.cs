
using System;
using System.Collections.Generic;
using System.Text;
using System.Diagnostics;
using System.IO;
using System.Threading;
using System.ServiceProcess;

public class ManagerServiceBase : ServiceBase
{
    public static readonly String APPLICATION_NAME = "ThreatFactor NSIA";
    private Process process;
    private Boolean shutdownProcessInitiated = false;
    private static readonly int FORCE_SHUTDOWN_SECONDS = 60;

    private String getJVMArguments()
    {
        String jvmArguments = "";
        try
        {
            StreamReader reader;
            string line;
            reader = File.OpenText("config.ini");
            line = reader.ReadLine();
            while (line != null && jvmArguments.Length == 0)
            {
                line = line.Trim();
                if (line.StartsWith("JVM.Arguments"))
                {
                    int equalSign = line.IndexOf("=");
                    if (equalSign > 0)
                    {
                        jvmArguments = line.Substring(equalSign+1);
                    }
                }

                line = reader.ReadLine();
            }
            reader.Close();
        }
        catch (Exception)
        {
            //Could not read the file, just continue
        }

        return jvmArguments;

    }

    /// <SUMMARY>
    /// Start the manager
    /// </SUMMARY>
    protected override void OnStart(string[] args)
    {
        process = new Process();

        //Reset the service to the install directory as opposed to C:\Windows\System32
        Environment.CurrentDirectory = System.AppDomain.CurrentDomain.BaseDirectory;
 
        process.StartInfo.FileName = "java";
        process.StartInfo.Arguments = getJVMArguments() + " -jar nsia.jar --service" ;

        process.StartInfo.RedirectStandardOutput = true;
        process.StartInfo.RedirectStandardInput = true;
        process.StartInfo.RedirectStandardError = true;
        process.StartInfo.CreateNoWindow = true;
        process.StartInfo.UseShellExecute = false;
        process.StartInfo.WorkingDirectory = System.AppDomain.CurrentDomain.BaseDirectory;

        process.EnableRaisingEvents = true;
        process.Exited += new EventHandler(managerStopped);

        try
        {
            process.Start();
            EventLog.WriteEntry(APPLICATION_NAME, "NSIA started as process ID " + process.Id, EventLogEntryType.Information);
        }
        catch (Exception e)
        {
            EventLog.WriteEntry(APPLICATION_NAME, "NSIA could not be start: " + e.Message, EventLogEntryType.Error);
            this.Stop();
        }
    }

    protected void ConsoleOutput(object sendingProcess, DataReceivedEventArgs outLine)
    {
        EventLog.WriteEntry(APPLICATION_NAME, outLine.Data, EventLogEntryType.Information);
    }

    protected void managerStopped(object sender, EventArgs e)
    {
        if ( shutdownProcessInitiated == false && process.ExitCode == -1 )
        {
            EventLog.WriteEntry(APPLICATION_NAME, "NSIA has stopped unexpectedly, the service will now shutdown too", EventLogEntryType.Error);
        }
        else if (shutdownProcessInitiated == true && process.ExitCode != 0)
        {
            EventLog.WriteEntry(APPLICATION_NAME, "NSIA terminated with exit code " + process.ExitCode, EventLogEntryType.Warning);
        }

        this.Stop();
    }

    /// <SUMMARY>
    /// Stop the service but sending the "shutdown" command to the manager.
    /// </SUMMARY>
    protected override void OnStop()
    {
        EventLog.WriteEntry(APPLICATION_NAME, "NSIA given shutdown command", EventLogEntryType.Information);

        shutdownProcessInitiated = true;
        try
        {
            if (process.HasExited == false)
            {
                StreamWriter streamWriter = process.StandardInput;
                streamWriter.WriteLine("System.Shutdown");

                for (int c = 0; c < FORCE_SHUTDOWN_SECONDS && !process.HasExited; c++)
                {
                    Thread.Sleep(1000);
                }

                if (!process.HasExited)
                {
                    EventLog.WriteEntry(APPLICATION_NAME, "NSIA did not shutdown in " + FORCE_SHUTDOWN_SECONDS + " seconds, the system will be forcibly terminated", EventLogEntryType.Error);
                    process.Kill();
                }
            }
        }
        catch (Exception e)
        {
            EventLog.WriteEntry(APPLICATION_NAME, "Error occurred while attempting to shutdown: " + e.Message, EventLogEntryType.Error);
        }

        EventLog.WriteEntry(APPLICATION_NAME, "NSIA service stopped", EventLogEntryType.Information);
    }
}