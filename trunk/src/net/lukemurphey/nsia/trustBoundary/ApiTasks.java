package net.lukemurphey.nsia.trustBoundary;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.DuplicateEntryException;
import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.InsufficientPermissionException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.WorkerThread;
import net.lukemurphey.nsia.Application.WorkerThreadDescriptor;


public class ApiTasks extends ApiHandler {

	public ApiTasks(Application appRes) {
		super(appRes);
	}
	
	public WorkerThreadDescriptor[] getWorkerThreadQueue( String sessionIdentifier ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		return getWorkerThreadQueue( sessionIdentifier, true );
	}
	
	public WorkerThreadDescriptor getWorkerThread( String sessionIdentifier, String uniqueName ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "System.Information.View");
		
		
		// 1 -- Get the thread
		return appRes.getWorkerThread( uniqueName );
	}
	
	public void addWorkerToQueue( String sessionIdentifier, WorkerThread thread, String uniqueName, int userID ) throws InsufficientPermissionException, GeneralizedException, NoSessionException, DuplicateEntryException{
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "System.Information.View");
		
		
		// 1 -- Get the worker threads
		appRes.addWorkerToQueue(thread, uniqueName, userID );
	}
	
	public WorkerThreadDescriptor[] getWorkerThreadQueue( String sessionIdentifier, boolean returnAliveThreadsOnly ) throws InsufficientPermissionException, GeneralizedException, NoSessionException{
		
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		checkRight( sessionIdentifier, "System.Information.View");
		
		
		// 1 -- Get the worker threads
		return appRes.getWorkerThreadQueue(returnAliveThreadsOnly);
	}
	
	public boolean stopTask( String sessionIdentifier, String uniqueName ){
		// 0 -- Precondition check
		
		//	0.1 -- Permission and right check
		//checkRight( sessionIdentifier, "System.Information.View");//TODO Get correct right
		
		
		// 1 -- Get the worker threads
		return appRes.stopWorkerThread(uniqueName);
	}
	
}
