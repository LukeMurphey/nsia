package net.lukeMurphey.nsia.htmlInterface;

import java.util.Hashtable;
import java.util.Vector;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.InsufficientPermissionException;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.NotFoundException;
import net.lukeMurphey.nsia.ScannerController;
import net.lukeMurphey.nsia.Application.WorkerThreadDescriptor;
import net.lukeMurphey.nsia.UserManagement.UserDescriptor;
import net.lukeMurphey.nsia.htmlInterface.Html.MessageType;
import net.lukeMurphey.nsia.trustBoundary.ApiTasks;
import net.lukeMurphey.nsia.trustBoundary.ApiUserManagement;
import net.lukeMurphey.nsia.trustBoundary.SimpleUserDescriptor;
import net.lukeMurphey.nsia.trustBoundary.ApiScannerController;

public class HtmlTaskList extends HtmlContentProvider {
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException{
		return getHtml( requestDescriptor, null );
	}
	
	private static ContentDescriptor getStopTaskForm(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException{

		Hashtable<String, String> parameters = new Hashtable<String, String>();
		
		parameters.put("Mode", "Tasks");
		parameters.put("Action", "Stop");
		parameters.put("Confirmed", "True");
		parameters.put("ID", requestDescriptor.request.getParameter("ID"));
		
		return HtmlOptionDialog.getHtml(requestDescriptor, "Stop Task", "Are you sure you want to stop this task?", parameters, new String[]{"Stop", "Cancel"}, "Tasks");
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException{
		
		if( actionDesc == null ){
			actionDesc = performAction( requestDescriptor );
		}
		
		if( actionDesc.result == ActionDescriptor.OP_DELETE){
			return getStopTaskForm(requestDescriptor, actionDesc);
		}
		else{
			return getList(requestDescriptor, actionDesc);
		}
	}
	
	public static ContentDescriptor getList(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException{
		StringBuffer body = new StringBuffer();
		String title = "System Status";
		
		// 1 -- Get the data
		if( actionDesc == null ){
			performAction( requestDescriptor );
		}
		
		body.append(Html.renderMessages(requestDescriptor.userId));
		
		
		//	 1.1 -- Output the list of tasks
		body.append( "<div class=\"SectionHeader\">Background Tasks</div>" );
		
		try{
			ApiTasks taskList = new ApiTasks(Application.getApplication());
			WorkerThreadDescriptor[] threads = taskList.getWorkerThreadQueue(requestDescriptor.sessionIdentifier);
			
			if( threads.length > 0 ){
				body.append( "<p><table class=\"DataTable\">" );
				
				body.append( "<thead><tr><td>Title</td><td>User</td><td>Description</td><td>Progress</td><td>&nbsp;</td></thead><tbody>" );
				
				for( int c = 0; c < threads.length; c++){
					body.append( createRow(threads[c], requestDescriptor ) );
				}
				
				body.append( "</tbody></table>" );
			}
			else{
				body.append( Html.getDialog("No background tasks are currently running.<p><a href=\"Tasks\">[Refresh List]</a>", "No Tasks Running", "/32_Information") );
			}
		}
		catch(InsufficientPermissionException e){
			body.append(Html.getWarningDialog("Insufficient Permission", "You do have permissions to view the list of background tasks"));
		}
		
		// 2 -- Get the menu items
		NavigationPath navPath = new NavigationPath();
		navPath.addPathEntry( "Main Dashboard", "/Dashboard" );
		navPath.addPathEntry( "System Status", "/SystemStatus");
		navPath.addPathEntry( "Task List", "/Tasks");
		String navigationHtml = Html.getNavigationPath( navPath );
		
		// 3 -- Get the navigation bar
		Vector<MenuItem> menuItems = new Vector<MenuItem>();
		
		menuItems.add( new MenuItem("System Administration", null, MenuItem.LEVEL_ONE) );		
		menuItems.add( new MenuItem("Modify Configuration", "/SystemConfiguration", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Event Log", "/EventLog", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Shutdown System", "/SystemStatus?Action=Shutdown", MenuItem.LEVEL_TWO) );
		
		/*if( Application.getApplication().isUsingInternalDatabase() ){
			menuItems.add( new MenuItem("Create Backup", "/DatabaseBackup", MenuItem.LEVEL_TWO) );
			menuItems.add( new MenuItem("Defragment Indexes", "/SystemStatus?Action=StartDefragmenter", MenuItem.LEVEL_TWO) );
		}*/
		
		menuItems.add( new MenuItem("Scanning Engine", null, MenuItem.LEVEL_ONE) );
		ApiScannerController scannerController = new ApiScannerController(Application.getApplication());
		
		try{
			if(scannerController.getScanningState(requestDescriptor.sessionIdentifier) == ScannerController.ScannerState.RUNNING)
				menuItems.add( new MenuItem("Stop Scanner", "/SystemStatus?Action=StopScanner", MenuItem.LEVEL_TWO) );
			else
				menuItems.add( new MenuItem("Start Scanner", "/SystemStatus?Action=StartScanner", MenuItem.LEVEL_TWO) );
		}
		catch(InsufficientPermissionException e){
			//Ignore this, it just means we can show the option to start and restart the scanner
		}
		menuItems.add( new MenuItem("View Definitions", "/Definitions", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Default Scan Policy", "ScanPolicy", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("Site Groups", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Site Groups", "/", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add Site Group", "/SiteGroup?Action=New", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("User Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Users", "/UserManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New User", "/UserManagement?Action=Add", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("View Logged in Users", "/Sessions", MenuItem.LEVEL_TWO) );
		
		menuItems.add( new MenuItem("Group Management", null, MenuItem.LEVEL_ONE) );
		menuItems.add( new MenuItem("List Groups", "/GroupManagement", MenuItem.LEVEL_TWO) );
		menuItems.add( new MenuItem("Add New Group", "/GroupManagement?Action=Add", MenuItem.LEVEL_TWO) );
				
		String menuOutput = Html.getMenu( menuItems );
		
		// 4 -- Compile the result
		String pageOutput = Html.getMainContent(body, menuOutput, navigationHtml);
		
		return new ContentDescriptor( title, pageOutput );
	}
	
	/*private static String createRow2( WorkerThread thread ){
		StringBuffer output = new StringBuffer();
		output.append("<tr><td>");
		//output.append( thread.getTaskDescription() );
		output.append( "<span class=\"Text_3\">" + thread.getTaskDescription() + "</span>");
		output.append( "<br>" + thread.getStatusDescription());
		
		output.append("<div style=\"position:relative; width:198px; height:12px; margin-top: 3px; padding:2px; background-image:url(/SmallProgressBarBlank);\">" );
		output.append( "<div style=\"position:relative; left:1px; width:" + (194 * thread.getProgress() ) / 100  + "px; height:8px; padding:2px; background-image:url(/SmallProgressBar2); layer-background-image:url(/SmallProgressBar2);\"></div></div>" );
		
		//output.append( thread.getStatusDescription() );
		output.append( "</td></tr>" );
		return output.toString();
	}*/
	
	private static String createRow( WorkerThreadDescriptor worker, WebConsoleConnectionDescriptor connectionDescriptor ) throws NoSessionException{
		StringBuffer output = new StringBuffer();
		output.append("<tr>");
		
		output.append( "<td class=\"TitleText\">" + worker.getWorkerThread().getTaskDescription() + "</td>");
		
		output.append( "<td>" );
		if( worker.getUserID() >= 0 ){
			try{
				ApiUserManagement userManagement = new ApiUserManagement(Application.getApplication());
				
				String username = null;
				int userID = -1;
				boolean isUnrestricted = false;
				
				try{
					UserDescriptor userDescriptor = userManagement.getUserDescriptor(connectionDescriptor.sessionIdentifier, worker.getUserID());
					
					if( userDescriptor != null ){
						username = userDescriptor.getUserName();
						userID = userDescriptor.getUserID();
						isUnrestricted = userDescriptor.isUnrestricted();
					}
				}
				catch(InsufficientPermissionException e){
					SimpleUserDescriptor userDescriptor = userManagement.getSimpleUserDescriptor(connectionDescriptor.sessionIdentifier, worker.getUserID());
					
					if( userDescriptor != null ){
						username = userDescriptor.getUserName();
						userID = userDescriptor.getUserID();
						isUnrestricted = false;
					}
				}
				
				if( isUnrestricted ){
					output.append( "<table><tr><td><img style=\"vertical-align: top;\" alt=\"User\" src=\"/16_Admin\"><td>&nbsp;" );
				}
				else{
					output.append( "<table><tr><td><img style=\"vertical-align: top;\" alt=\"User\" src=\"/16_User\"><td>&nbsp;" );
				}
				
				output.append( "<a href=\"UserManagement?UserID=" + userID + "\">" + username + "</a></td></tr></table>" );
			}
			catch(GeneralizedException e){
				output.append( "<table><tr><td><img style=\"vertical-align: top;\" alt=\"User\" src=\"/16_Help\">&nbsp;[Unknown]</td></tr></table>" );
			}
			catch(NotFoundException e){
				output.append( "<table><tr><td><img style=\"vertical-align: top;\" alt=\"User\" src=\"/16_Help\">&nbsp;[Unknown]</td></tr></table>" );
			}
		}
		else{
			output.append( "<table><tr><td><img style=\"vertical-align: top;\" alt=\"System\" src=\"/16_System\">&nbsp;System</td></tr></table>" );
		}
		output.append( "</td>" );
		
		output.append( "<td>" + worker.getWorkerThread().getStatusDescription() + "</td>");
		
		output.append( "<td>" );
		if( worker.getWorkerThread().getProgress() < 0 ){
			output.append("<img style=\"margin-top: 3px;\" src=\"/SmallProgressBarAnimation\" alt=\"Progress Bar\"/>");
		}
		else{
			output.append("<div style=\"position:relative; width:198px; height:12px; margin-top: 3px; padding:2px; background-image:url(/SmallProgressBarBlank);\">" );
			output.append( "<div style=\"position:relative; left:1px; width:" + (194 * Math.min(worker.getWorkerThread().getProgress(), 100) ) / 100  + "px; height:8px; padding:2px; background-image:url(/SmallProgressBar2); layer-background-image:url(/SmallProgressBar2);\"></div></div>" );
		}
		output.append( "</td>" );
		
		output.append( "<td>" );
		output.append( "<table><tr><td><a href=\"Tasks?Action=Stop&ID=" + worker.getUniqueName() + "\">&nbsp;<img src=\"/16_Delete\"></a></td><td><a href=\"Tasks?Action=Stop&ID=" + worker.getUniqueName() + "\">Stop</a></td></tr></table>" );
		output.append( "</td>" );
		//output.append( thread.getStatusDescription() );
		output.append( "</tr>" );
		return output.toString();
	}
	
	
	private static ActionDescriptor performAction(WebConsoleConnectionDescriptor requestDescriptor) throws GeneralizedException, NoSessionException{
		String action = requestDescriptor.request.getParameter("Action");
		
		if( action == null ){
			return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION );
		}
		
		if( action.matches("Stop") && requestDescriptor.request.getParameter("Confirmed") != null ){
			String uniqueName = requestDescriptor.request.getParameter("ID");
			
			if( uniqueName != null ){
				ApiTasks taskList = new ApiTasks(Application.getApplication());
				if( taskList.stopTask(requestDescriptor.sessionIdentifier, uniqueName) == true){
					Html.addMessage(MessageType.INFORMATIONAL, "The stop command was successfully sent", requestDescriptor.userId.longValue());
				}
				else{
					Html.addMessage(MessageType.WARNING, "The stop command could not be successfully sent", requestDescriptor.userId.longValue());
				}
			}
		}
		else if( action.matches("Stop") ){
			return new ActionDescriptor(ActionDescriptor.OP_DELETE );
		}
		
		return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION );
	}
	
}
