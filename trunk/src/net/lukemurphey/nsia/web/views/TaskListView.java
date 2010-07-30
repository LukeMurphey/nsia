package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.NoDatabaseConnectionException;
import net.lukemurphey.nsia.NotFoundException;
import net.lukemurphey.nsia.UserManagement;
import net.lukemurphey.nsia.WorkerThread;
import net.lukemurphey.nsia.Application.WorkerThreadDescriptor;
import net.lukemurphey.nsia.UserManagement.UserDescriptor;
import net.lukemurphey.nsia.web.Link;
import net.lukemurphey.nsia.web.Menu;
import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.Shortcuts;
import net.lukemurphey.nsia.web.StandardViewList;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.templates.TemplateLoader;

public class TaskListView extends View {

	public TaskListView() {
		super("System/Tasks", "task_list", Pattern.compile("[-A-Za-z_0-9]*", Pattern.CASE_INSENSITIVE));
	}

	public static String getURL( ) throws URLInvalidException{
		TaskListView view = new TaskListView();
		return view.createURL();
	}
	
	public static class TaskDescriptor{
		
		WorkerThread thread;
		UserDescriptor user;
		String uniqueName;
		
		public TaskDescriptor( WorkerThread thread, UserDescriptor user, String uniqueName ){
			this.thread = thread;
			this.user = user;
			this.uniqueName = uniqueName;
		}
		
		public TaskDescriptor( WorkerThread thread, int userID, String uniqueName ) throws ViewFailedException{
			
			if( userID >= 0 ){
				UserManagement userManagement = new UserManagement(Application.getApplication());
				
				try {
					this.user = userManagement.getUserDescriptor(userID);
				} catch (SQLException e) {
					throw new ViewFailedException(e);
				} catch (NoDatabaseConnectionException e) {
					throw new ViewFailedException(e);
				} catch (NotFoundException e) {
					throw new ViewFailedException(e);
				}
			}
			else{
				this.user = null;
			}
			
			this.thread = thread;
			this.uniqueName = uniqueName;
			
		}
		
		
		public UserDescriptor getUser(){
			return user;
		}
		
		public String getTaskDescription(){
			return thread.getTaskDescription();
		}
		
		public String getStatusDescription(){
			return thread.getStatusDescription();
		}
		
		public int getProgress(){
			return thread.getProgress();
		}
		
		public String getUniqueName(){
			return uniqueName;
		}
	}
	
	@Override
	protected boolean process(HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data) throws ViewFailedException, URLInvalidException, IOException, ViewNotFoundException {
		
		// 1 -- Populate the page content
		Shortcuts.addDashboardHeaders(request, response, data, createURL());
		data.put("menu", Menu.getSystemMenu(context));
		data.put("title", "Task List");
		
		//Breadcrumbs
		Vector<Link> breadcrumbs = new Vector<Link>();
		breadcrumbs.add(  new Link("Main Dashboard", StandardViewList.getURL("main_dashboard")) );
		breadcrumbs.add(  new Link("System Status", StandardViewList.getURL("system_status")) );
		breadcrumbs.add(  new Link("Task List", StandardViewList.getURL("task_list")) );
		data.put("breadcrumbs", breadcrumbs);
		
		// 2 -- Check the permissions
		try{
			if( Shortcuts.hasRight( context.getSessionInfo(), "System.Information.View", "View running tasks") == false ){
				Shortcuts.getPermissionDeniedDialog(response, data, "You do not have permission view the running tasks");
				return true;
			}
		}
		catch(GeneralizedException e){
			throw new ViewFailedException(e);
		}
		
		// 3 -- Get the threads
		WorkerThreadDescriptor[] threads = Application.getApplication().getWorkerThreadQueue(true);
		
		TaskDescriptor[] tasks = new TaskDescriptor[threads.length];
		
		for( int c = 0; c < threads.length; c++ ){
			tasks[c] = new TaskDescriptor(threads[c].getWorkerThread(), threads[c].getUserID(), threads[c].getUniqueName());
		}
		
		data.put("tasks", tasks);
		
		// 4 -- Render the task list
		TemplateLoader.renderToResponse("TaskList.ftl", data, response);
		
		return true;
	}

}
