package net.lukeMurphey.nsia.htmlInterface;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.DatabaseBackup;
import net.lukeMurphey.nsia.DuplicateEntryException;
import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.WorkerThread;
import net.lukeMurphey.nsia.Application.WorkerThreadDescriptor;
import net.lukeMurphey.nsia.htmlInterface.Html.MessageType;

public class HtmlDatabaseBackup extends HtmlContentProvider{
	
	public static final int OP_BACKUP_STARTED = 101;
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc  )  throws GeneralizedException, NoSessionException, InvalidHtmlParameterException{
		
		// 1 -- Perform any requested actions
		if( actionDesc == null ){
			performAction( requestDescriptor );
		}
		
		// 2 -- Determine if the database backup is already running by looking through the active threads for one that is backing up the database
		WorkerThreadDescriptor[] workerThreads = Application.getApplication().getWorkerThreadQueue();
		WorkerThreadDescriptor backupWorker = null;
		
		for( int c = 0; c < workerThreads.length; c++){
			if( workerThreads[c].getWorkerThread().getTaskDescription().equalsIgnoreCase("Database Backup")){
				if( workerThreads[c].getWorkerThread().getStatus() == WorkerThread.State.STARTED ||  workerThreads[c].getWorkerThread().getStatus() == WorkerThread.State.INITIALIZED){
					backupWorker = workerThreads[c];
				}
			}
		}
		
		// 3 -- Show the status form if a backup is in progress 
		if( backupWorker != null ){
			try{
				return HtmlOptionDialog.getHtml(requestDescriptor, "Database Backup", backupWorker.getWorkerThread().getStatusDescription(), new Hashtable<String, String>(), new String[]{"Cancel"}, "DatabaseBackup", HtmlOptionDialog.DIALOG_INFORMATION, "DatabaseBackup", "/Ajax/Task/" + java.net.URLEncoder.encode( backupWorker.getUniqueName(), "US-ASCII") );
			}
			catch(UnsupportedEncodingException e){
				//US-ASCII is not supported by the system. This error should not realistically be thrown since practically every system should support US-ASCII. If it does, then just show the standard form.
				return HtmlOptionDialog.getHtml(requestDescriptor, "Database Backup", backupWorker.getWorkerThread().getStatusDescription(), new Hashtable<String, String>(), new String[]{"Cancel"}, "DatabaseBackup", HtmlOptionDialog.DIALOG_INFORMATION, backupWorker.getWorkerThread().getProgress(), "DatabaseBackup" );
			}
		}
		
		// 4 -- Otherwise, show the form to begin a backup
		else{
			return getStartBackupForm( requestDescriptor, null );
		}
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException, InvalidHtmlOperationException{
		
		return getHtml( requestDescriptor, null);
	}
	
	private static ContentDescriptor getStartBackupForm(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ) throws GeneralizedException, NoSessionException, InvalidHtmlParameterException{

		if( Application.getApplication().isUsingInternalDatabase() == false ){
			return HtmlOptionDialog.getHtml(requestDescriptor, "Backup Unsupported", "The database backup feature is only supported for the internal database. Since this application is not using the integrated database, the backup cannot be performed.", new Hashtable<String, String>(),new String[]{"OK"}, "", HtmlOptionDialog.DIALOG_INFORMATION);  
		}
		else{
			Hashtable<String, String> parameters = new Hashtable<String, String>();
			
			parameters.put("Action", "Backup");
			//TODO need to change this form to use the ajax status calls
			return HtmlOptionDialog.getHtml(requestDescriptor, "Backup System", "Are you sure you want to create a backup of the database?", parameters, new String[]{"Backup", "Cancel"}, "DatabaseBackup");
		}
	}
	
	private static ActionDescriptor performAction(WebConsoleConnectionDescriptor requestDescriptor) throws GeneralizedException, NoSessionException{
		String action = requestDescriptor.request.getParameter("Action");
		
		if( action == null )
			return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION );
				
		if( "Cancel".equalsIgnoreCase(requestDescriptor.request.getParameter("Selected") ) ){
			try{
				requestDescriptor.response.sendRedirect("/MainDashboard");
			}
			catch(IOException e){
				e.printStackTrace();
				//oops, could not redirect. Just fall through.	
			}
		}
		
		if( action.equals("Backup") && !"Cancel".equalsIgnoreCase(requestDescriptor.request.getParameter("Selected")) ){
			DatabaseBackup databaseBackup = new DatabaseBackup();
			try{
				Application.getApplication().addWorkerToQueue(databaseBackup, "Database backup");
			}
			catch( DuplicateEntryException e){
				Html.addMessage(MessageType.INFORMATIONAL, "Backup task already in progress", requestDescriptor.userId.longValue());
				return new ActionDescriptor( OP_BACKUP_STARTED );
			}
			
			Thread thread = new Thread(databaseBackup);
			thread.start();
			
			return new ActionDescriptor( OP_BACKUP_STARTED );
		}
		
		return new ActionDescriptor(ActionDescriptor.OP_NO_OPERATION );
		
	}

}
