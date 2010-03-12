package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.DatabaseBackup;
import net.lukemurphey.nsia.DuplicateEntryException;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.WorkerThread;
import net.lukemurphey.nsia.Application.WorkerThreadDescriptor;
import net.lukemurphey.nsia.WorkerThread.State;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.StandardViewList;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;
import net.lukemurphey.nsia.web.templates.TemplateLoader;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class BackupView extends View {

	public BackupView() {
		super("System/Backup", "system_backup");
	}

	public static String getURL() throws URLInvalidException{
		BackupView view = new BackupView();
		return view.createURL();
	}
	
	@Override
	protected boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException, URLInvalidException, IOException, ViewNotFoundException {
		
		// 1 -- Redirect if the user pressed cancel
		if( "Cancel".equalsIgnoreCase( request.getParameter("Selected") ) ) {
			response.sendRedirect(StandardViewList.getURL("main_dashboard"));
			return true;
		}
		
		// 2 -- Determine if the user has permission
		try {
			if( Shortcuts.hasRight( context.getSessionInfo(), "System.Configuration.Edit", "Perform system backup") == false ){
				context.addMessage("You do not have permission to perform database backups", MessageSeverity.WARNING);
				response.sendRedirect( SystemStatusView.getURL() );
				return true;
			}
		} catch (GeneralizedException e) {
			throw new ViewFailedException(e);
		}
		
		// 3 -- Determine if a backup is underway
		WorkerThreadDescriptor backupWorker = getWorkerThread( false );
		
		// 3 -- Start the backup if the user has so requested
		boolean startedNow = false;
		if( "Backup".equalsIgnoreCase( request.getParameter("Selected") ) ) {
			startedNow = true;
			backupWorker = startBackup();
		}
		
		// 4 -- Post the progress dialog if a backup is underway
		
		//	 4.1 -- Determine if just the progress dialog should be shown (e.g. AJAX request) 
		boolean isAjax = (request.getParameter("AJAX") != null);
		
		//	 4.2 -- Post a dialog indicating that the backup is complete if the task is done
		if( isAjax && backupWorker == null ){
			response.getWriter().print( Dialog.getProgressDialog("Backup complete", "Database Backup", 100, new Link("OK", createURL())) );
			return true;
		}
		
		//   4.3 -- Post a dialog indicating the task is complete
		else if( isAjax && (backupWorker == null || backupWorker.getWorkerThread().getStatus() == State.STOPPED) ){
			response.getWriter().print( Dialog.getProgressDialog(backupWorker.getWorkerThread().getStatusDescription(), backupWorker.getWorkerThread().getTaskDescription(), 100, new Link("OK", createURL())) );
			return true;
		}
		
		//	 4.4 -- Post the progress dialog otherwise
		else if( isAjax ){
			response.getWriter().print( Dialog.getProgressDialog(backupWorker.getWorkerThread().getStatusDescription(), backupWorker.getWorkerThread().getTaskDescription(), backupWorker.getWorkerThread().getProgress()) );
			return true;
		}
		
		//	 4.5 -- Add the dashboard headers
		Shortcuts.addDashboardHeaders(request, response, data);
		
		//	 4.6 -- Post the progress dialog
		if( backupWorker != null && (startedNow || backupWorker.getWorkerThread().getStatus() == State.STARTING || backupWorker.getWorkerThread().getStatus() == State.STARTED ) ){
			data.put("ajaxurl", createURL() + "?AJAX=True");
			data.put("title", "Database Backup");
			data.put("content", Dialog.getProgressDialog(backupWorker.getWorkerThread().getStatusDescription(), backupWorker.getWorkerThread().getTaskDescription(), backupWorker.getWorkerThread().getProgress()) );
			
			response.getWriter().println( TemplateLoader.renderToString("AJAXProgressDialog.ftl", data) );
			
			return true;
		}
		
		// 5 -- Post the option to begin a backup if a backup is not underway
		else{
			Dialog.getOptionDialog(response, context, data, "Are you sure you want to create a backup of the database?", "Backup System", DialogType.INFORMATION, new Link( "Backup", createURL()), new Link( "Cancel", createURL()) );
		}
		
		return true;
	}
	
	/**
	 * Gets the backup worker thread if one exists.
	 * @return
	 */
	private WorkerThreadDescriptor getWorkerThread( boolean aliveThreadsOnly ){
		WorkerThreadDescriptor[] workerThreads = Application.getApplication().getWorkerThreadQueue( aliveThreadsOnly  );
		WorkerThreadDescriptor backupWorker = null;
		
		for( int c = 0; c < workerThreads.length; c++){
			if( workerThreads[c].getWorkerThread().getTaskDescription().equalsIgnoreCase("Database Backup")){
				if( workerThreads[c].getWorkerThread().getStatus() == WorkerThread.State.STARTED ||  workerThreads[c].getWorkerThread().getStatus() == WorkerThread.State.INITIALIZED){
					backupWorker = workerThreads[c];
				}
			}
		}
		
		return backupWorker;
	}
	
	/**
	 * Start the backup task.
	 * @return
	 */
	private WorkerThreadDescriptor startBackup( ){
		
		// 1 -- Start the backup
		DatabaseBackup databaseBackup = new DatabaseBackup();
		//WorkerThread databaseBackup = new DummyWorker("Database Backup", 60);
		WorkerThreadDescriptor desc = null;
		
		try{
			desc = Application.getApplication().addWorkerToQueue(databaseBackup, "Database Backup");
			Thread thread = new Thread(databaseBackup);
			thread.start();
		}
		catch( DuplicateEntryException e){
			//Backup task already in progress
		}
		
		// 2 -- Get the thread descriptor
		return desc;
	}

}
