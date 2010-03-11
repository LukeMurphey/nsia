package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.DefinitionUpdateWorker;
import net.lukemurphey.nsia.DuplicateEntryException;
import net.lukemurphey.nsia.GeneralizedException;
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

public class DefinitionsUpdateView extends View {

	public static final String VIEW_NAME = "definitions_update";
	
	public DefinitionsUpdateView() {
		super("Definitions/Update", VIEW_NAME);
	}

	public static String getURL() throws URLInvalidException{
		DefinitionsUpdateView view = new DefinitionsUpdateView();
		return view.createURL();
	}
	
	/**
	 * Gets the definition update worker (if running)
	 * @return
	 */
	protected DefinitionUpdateWorker getWorkerIfRunning(){
		
		DefinitionUpdateWorker worker = null;
		
		{
			WorkerThreadDescriptor w = Application.getApplication().getWorkerThread("Definitions Update (unscheduled)");
			
			if( w != null ){
				worker = (DefinitionUpdateWorker)w.getWorkerThread();
			}
		}
		
		return worker;
	}
	
	/**
	 * Start an updater process to update the definitions.
	 * @param force
	 * @param userID
	 * @throws DuplicateEntryException
	 */
	protected DefinitionUpdateWorker startUpdate( boolean force, int userID  ) throws DuplicateEntryException{
		
		DefinitionUpdateWorker worker = new DefinitionUpdateWorker( force );
		
		if( userID > -1 ){
			Application.getApplication().addWorkerToQueue(worker, "Definitions Update (unscheduled)", userID);
			new Thread(worker).start();
		}
		else{
			Application.getApplication().addWorkerToQueue(worker, "Definitions Update (unscheduled)");
			new Thread(worker).start();
		}
		
		return worker;
		
	}
	
	@Override
	protected boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException, URLInvalidException, IOException, ViewNotFoundException {
	
		// 1 -- Go to definitions view if the user pressed cancel
		if( request.getParameter("Selected") != null && "Cancel".equalsIgnoreCase( request.getParameter("Selected") ) ){
			response.sendRedirect(StandardViewList.getURL(DefinitionsView.VIEW_NAME));
			return true;
		}
		
		// 2 -- Check rights
		try {
			if( Shortcuts.hasRight( context.getSessionInfo(), "System.Configuration.Edit", "Update definitions") == false ){
				context.addMessage("You do not have permission to update the definitions", MessageSeverity.WARNING);
				response.sendRedirect( DefinitionsView.getURL() );
				return true;
			}
		} catch (GeneralizedException e) {
			throw new ViewFailedException(e);
		}
		
		// 3 -- Determine if the update is already occurring
		DefinitionUpdateWorker worker = getWorkerIfRunning();
		
		// 4 -- Start the update task if necessary
		boolean startedNow = false;
		
		if( request.getParameter("Selected") != null && "Update".equalsIgnoreCase( request.getParameter("Selected") ) && (worker == null || worker.getStatus() != State.STARTED) ){
			try {
				worker = startUpdate( true, context.getSessionInfo().getUserId() );
			} catch (DuplicateEntryException e) {
				// Worker was already running, retrieve it
				worker = getWorkerIfRunning();
			}
			startedNow = true;
		}
		
		// 5 -- Show the progress bar if it is running
		
		//	 5.1 -- Determine if just the progress dialog should be shown (e.g. AJAX request) 
		boolean isAjax = (request.getParameter("AJAX") != null);
		
		//	 5.1 -- Post a dialog indicating that the update is complete if the task is done
		if( isAjax && worker == null ){
			response.getWriter().print( Dialog.getProgressDialog("Definition update complete", "Definition Update", 100, new Link("OK", createURL())) );
			return true;
		}
		
		//	 5.2 -- Post a dialog indicating the task is complete
		else if( isAjax && (worker == null || worker.getStatus() == State.STOPPED) ){
			response.getWriter().print( Dialog.getProgressDialog(worker.getStatusDescription(), worker.getTaskDescription(), 100, new Link("OK", createURL())) );
			return true;
		}
		
		//	 5.3 -- Post the progress dialog otherwise
		else if( isAjax ){
			response.getWriter().print( Dialog.getProgressDialog(worker.getStatusDescription(), worker.getTaskDescription(), worker.getProgress()) );
			return true;
		}
		
		// 6 -- Create the dialog
		Shortcuts.addDashboardHeaders(request, response, data);
		
		//	 6.1 -- Post the progress dialog
		if( worker != null && (startedNow || worker.getStatus() == State.STARTING || worker.getStatus() == State.STARTED)){
			data.put("ajaxurl", createURL() + "?AJAX=True");
			data.put("title", "Definition Update");
			data.put("content", Dialog.getProgressDialog(worker.getStatusDescription(), worker.getTaskDescription(), worker.getProgress()) );
			
			response.getWriter().println( TemplateLoader.renderToString("AJAXProgressDialog.ftl", data) );
			
			return true;
		}
		
		//	 6.2 -- Post the option to begin a definition update if not already underway
		else{
			Dialog.getOptionDialog(response, context, data, "Do you want to update definitions now?", "Update Definitions", DialogType.INFORMATION, new Link( "Update", createURL()), new Link( "Cancel", createURL()) );
		}

		return true;
	}

}
