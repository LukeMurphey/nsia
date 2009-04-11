package net.lukeMurphey.nsia.trustBoundary;

import net.lukeMurphey.nsia.Application;
import net.lukeMurphey.nsia.DuplicateEntryException;
import net.lukeMurphey.nsia.GeneralizedException;
import net.lukeMurphey.nsia.InsufficientPermissionException;
import net.lukeMurphey.nsia.NoSessionException;
import net.lukeMurphey.nsia.ReindexerWorker;

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
