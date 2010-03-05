package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.WorkerThread;
import net.lukemurphey.nsia.Application.WorkerThreadDescriptor;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.StandardViewList;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.SessionMessages.MessageSeverity;
import net.lukemurphey.nsia.web.views.Dialog.DialogType;

public class TaskStopView extends View {

	public TaskStopView() {
		super("System/Task/Stop", "task_stop", Pattern.compile(".*", Pattern.CASE_INSENSITIVE));
	}

	public static String getURL( String taskName ) throws URLInvalidException{
		TaskStopView view = new TaskStopView();
		return view.createURL(taskName);
	}
	
	@Override
	protected boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException, URLInvalidException, IOException, ViewNotFoundException {

		Shortcuts.addDashboardHeaders(request, response, data);
		
		// 1 -- Make sure the user has permission
		try {
			if( Shortcuts.hasRight( context.getSessionInfo(), "System.Configuration.Edit", "Stop background task") == false ){
				context.addMessage("You do not have permission to stop running tasks.", MessageSeverity.WARNING);
				response.sendRedirect( TaskListView.getURL() );
				return true;
			}
		} catch (GeneralizedException e) {
			throw new ViewFailedException(e);
		}
		
		// 2 -- Find the task
		String name;
		
		if( args.length <= 0 ){
			//Show dialog since user did not provide a task to terminate
			Dialog.getDialog(response, context, data, "The name of the task to terminate was not provided.", "Task Name Not Provided", DialogType.WARNING, new Link("Return to the list of running tasks", StandardViewList.getURL("task_list")));
			return true;
		}
		else{
			name = args[0];
		}
		
		WorkerThreadDescriptor thread = getWorkerThread(name, true);
		
		// 3 -- Stop if no task exists
		if( thread == null ){
			Dialog.getDialog(response, context, data, "No running task was found with the given name.", "Task Not Found", DialogType.WARNING, new Link("Return to the list of running tasks", StandardViewList.getURL("task_list")));
			return true;
		}
		
		// 4 -- Confirm that the user wants to terminate the task
		if( "Stop".equalsIgnoreCase( request.getParameter("Selected") ) == false ){
			Dialog.getOptionDialog(response, context, data, "Are you sure you want to stop the task?.", "Stop Task?", DialogType.INFORMATION, new Link("Stop", createURL(thread.getUniqueName())), new Link("Cancel", StandardViewList.getURL("task_list")));
			return true;
		}
		
		// 5 -- Terminate the task
		thread.getWorkerThread().terminate();
		context.addMessage("Task was given terminate command", MessageSeverity.SUCCESS);
		
		// 5 -- Forward to the task list
		response.sendRedirect(StandardViewList.getURL("task_list"));
		return true;
	}

	private WorkerThreadDescriptor getWorkerThread( String name, boolean aliveThreadsOnly ){
		WorkerThreadDescriptor[] workerThreads = Application.getApplication().getWorkerThreadQueue( aliveThreadsOnly  );
		WorkerThreadDescriptor worker = null;
		
		for( int c = 0; c < workerThreads.length; c++){
			if( workerThreads[c].getUniqueName().equalsIgnoreCase(name)){
				if( workerThreads[c].getWorkerThread().getStatus() == WorkerThread.State.STARTED || workerThreads[c].getWorkerThread().getStatus() == WorkerThread.State.INITIALIZED){
					worker = workerThreads[c];
				}
			}
		}
		
		return worker;
	}
	
}
