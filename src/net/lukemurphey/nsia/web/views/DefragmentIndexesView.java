package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.DuplicateEntryException;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.ReindexerWorker;
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

public class DefragmentIndexesView extends View {

	public static final String VIEW_NAME = "defragment_indexes";
	
	public DefragmentIndexesView() {
		super("System/ReIndex", VIEW_NAME);
	}

	public static String getURL() throws URLInvalidException{
		DefragmentIndexesView view = new DefragmentIndexesView();
		return view.createURL();
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
	
	protected WorkerThreadDescriptor startReindexerWorker() {
		
		// 1 -- Start the reindexer
		ReindexerWorker worker = new ReindexerWorker();
		WorkerThreadDescriptor desc = null;
		
		try{
			desc = Application.getApplication().addWorkerToQueue(worker, "Index Defragmenter (unscheduled)");
			Thread thread = new Thread( worker );
			thread.start();
		}
		catch(DuplicateEntryException e){
			//Worker was already started
		}
		
		return desc;
	}
	
	@Override
	protected boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException, URLInvalidException, IOException, ViewNotFoundException {
		
		// 1 -- Redirect if the user pressed cancel
		if( "Cancel".equalsIgnoreCase( request.getParameter("Selected") ) ) {
			response.sendRedirect(StandardViewList.getURL("main_dashboard"));
			return true;
		}
		
		// 2 -- Check rights
		try {
			if( Shortcuts.hasRight( context.getSessionInfo(), "System.Configuration.Edit") == false ){
				context.addMessage("You do not have permission to defragment the database indexes", MessageSeverity.WARNING);
				response.sendRedirect( SystemStatusView.getURL() );
				return true;
			}
		} catch (GeneralizedException e) {
			throw new ViewFailedException(e);
		}
		
		// 3 -- Get the indexer worker if it is already running
		WorkerThreadDescriptor worker = getWorkerThread( false );
		
		// 4 -- Start the indexer if requested and not already running
		boolean startedNow = false;
		if( "Reindex".equalsIgnoreCase( request.getParameter("Selected") ) ) {
			//Shortcuts.checkRight( context.getSessionInfo(), "System.Configuration.Edit"); //TODO Check permissions
			startedNow = true;
			worker = startReindexerWorker();
		}
		
		// 5 -- Post the progress dialog if a backup is underway
		
		//	 5.1 -- Determine if just the progress dialog should be shown (e.g. AJAX request) 
		boolean isAjax = (request.getParameter("AJAX") != null);
		
		//	 5.2 -- Post a dialog indicating that the backup is complete if the task is done
		if( isAjax && worker == null ){
			response.getWriter().print( Dialog.getProgressDialog("Reindexing complete", "Reindex Database", 100, new Link("OK", createURL())) );
			return true;
		}
		
		//   5.3 -- Post a dialog indicating the task is complete
		else if( isAjax && (worker == null || worker.getWorkerThread().getStatus() == State.STOPPED) ){
			response.getWriter().print( Dialog.getProgressDialog(worker.getWorkerThread().getStatusDescription(), worker.getWorkerThread().getTaskDescription(), 100, new Link("OK", createURL())) );
			return true;
		}
		
		//	 5.4 -- Post the progress dialog otherwise
		else if( isAjax ){
			response.getWriter().print( Dialog.getProgressDialog(worker.getWorkerThread().getStatusDescription(), worker.getWorkerThread().getTaskDescription(), worker.getWorkerThread().getProgress()) );
			return true;
		}
		
		//	 5.5 -- Add the dashboard headers
		Shortcuts.addDashboardHeaders(request, response, data);
		
		//	 5.6 -- Post the progress dialog
		if( worker != null && (startedNow || worker.getWorkerThread().getStatus() == State.STARTING || worker.getWorkerThread().getStatus() == State.STARTED ) ){
			data.put("ajaxurl", createURL() + "?AJAX=True");
			data.put("title", "Reindex Database");
			data.put("content", Dialog.getProgressDialog(worker.getWorkerThread().getStatusDescription(), worker.getWorkerThread().getTaskDescription(), worker.getWorkerThread().getProgress()) );
			
			response.getWriter().println( TemplateLoader.renderToString("AJAXProgressDialog.ftl", data) );
			
			return true;
		}
		
		// 6 -- Post the option to begin a backup if a backup is not underway
		else{
			Dialog.getOptionDialog(response, context, data, "Are you sure you want to re-index the database?", "Reindex Database", DialogType.INFORMATION, new Link( "Reindex", createURL()), new Link( "Cancel", createURL()) );
		}
		
		
		// 5 -- Show the status of the indexer if it is running
		
		// 6 -- Show the form to request starting the indexer
		
		
		return true;
	}

}
