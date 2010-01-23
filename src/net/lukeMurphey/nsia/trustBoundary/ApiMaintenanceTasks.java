package net.lukemurphey.nsia.trustBoundary;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.DuplicateEntryException;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.ReindexerWorker;

public class ApiMaintenanceTasks extends ApiHandler {

	public ApiMaintenanceTasks(Application appRes) {
		super(appRes);
	}

	public boolean startDatabaseReindexer( String sessionIdentifier ) throws GeneralizedException, NoSessionException, InsufficientPermissionException, DuplicateEntryException{
		
		
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the system is using the internal database
		if( appRes.isUsingInternalDatabase() == false ){
			return false;
		}
		
		//	 0.2 -- Check the permissions
		checkRight( sessionIdentifier, "System.Configuration.Edit");
		
		
		// 1 -- Start the reindexer
		ReindexerWorker worker = new ReindexerWorker();
		
		appRes.addWorkerToQueue(worker, "Index Defragmenter (unscheduled)");
		
		Thread thread = new Thread( worker );
		thread.start();
		return true;
		
	}
	
}
