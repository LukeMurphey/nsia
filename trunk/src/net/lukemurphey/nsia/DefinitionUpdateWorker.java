package net.lukemurphey.nsia;

import net.lukemurphey.nsia.LicenseDescriptor;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.eventlog.EventLogMessage.EventType;
import net.lukemurphey.nsia.scan.DefinitionArchive;
import net.lukemurphey.nsia.scan.DefinitionSet.DefinitionVersionID;

import java.util.TimerTask;

public class DefinitionUpdateWorker extends TimerTask implements WorkerThread  {

	private State state = State.INITIALIZED;
	
	private int startTime;
	private static int EXPECTED_MAX_TIME = 40000;
	private static int THIRD = (EXPECTED_MAX_TIME / 3);
	private DefinitionVersionID versionID = null;
	private boolean checkingForNewDefinitions = false;
	private boolean definitionsCurrent = true;
	private Exception exceptionThrown = null;
	private boolean force = false;
	private String message = null;
	
	public DefinitionUpdateWorker(){
		
	}
	
	/**
	 * This constructor takes a boolean that indicates if the definitions download should be performed even if auto-updating is disabled.
	 * @param force
	 */
	public DefinitionUpdateWorker( boolean force ){
		this.force = force;
	}
	
	public boolean canPause() {
		return false;
	}

	public int getProgress() {
		int currentTime = (int)System.currentTimeMillis();
		
		int diff = currentTime - startTime;
		
		if( diff <= THIRD){
			return (100*diff/THIRD) / 2;
		}
		else if(diff > EXPECTED_MAX_TIME){
			return 100;
		}
		else{
			return 50+ ((100*(diff - THIRD)) / (EXPECTED_MAX_TIME - THIRD))/2;
		}
		
		//progress += 10;
		//return -1;
	}

	public State getStatus() {
		return state;
	}

	public String getStatusDescription() {
		
		if( message != null ){
			return message;
		}
		
		if( checkingForNewDefinitions && state == State.STARTED ){
			return "Checking for new definitions";
		}
		
		if( state == State.INITIALIZED ){
			return "Ready to update";
		}
		else if(state == State.STARTING ){
			return "Preparing to update";
		}
		else if(state == State.STARTED ){
			return "Retrieving updates";
		}
		else if( exceptionThrown != null && state == State.STOPPED ){
			return "Unable to update definitions";
		}
		else if( definitionsCurrent && versionID != null && state == State.STOPPED ){
			return "Definitions current (" + versionID.toString() + ")";
		}
		else if( definitionsCurrent && versionID == null && state == State.STOPPED ){
			return "Definitions current";
		}
		else if( versionID != null && state == State.STOPPED ){
			return "Definitions current";
		}
		else if( versionID == null && state == State.STOPPED ){
			return "Definitions were not updated";
		}
		else{
			return "Retrieving updates";
		}
	}
	
	@Override
	public boolean cancel(){
		terminate();
		return true;
	}

	public String getTaskDescription() {
		return "Definitions Updater";
	}

	public void pause() {
		//Don't do anything, definition updates cannot be paused
	}

	public boolean reportsProgress() {
		return false;
	}

	public void terminate() {
		// TODO Implement termination routine
	}

	@Override
	public void run() {
		
		try {
			if( force == false && Application.getApplication().getApplicationConfiguration().getAutoDefinitionUpdating() == false ){
				state = State.STOPPED;
				return; //Auto-updating disabled, just return
			}
		
			state = State.STARTING;
			startTime = (int)System.currentTimeMillis();
			exceptionThrown = null;

			DefinitionArchive archive = DefinitionArchive.getArchive();
			state = State.STARTED;
			checkingForNewDefinitions = true;
			boolean newDefinitionsAvailable;
			
			newDefinitionsAvailable = archive.isNewDefinitionSetAvailable();
			checkingForNewDefinitions = false;
			
			LicenseDescriptor license = Application.getApplication().getApplicationConfiguration().getLicense(false);
			if( license.isValid() == false ){
				message = "New definitions cannot be downloaded: do not have a valid license";
			}
			else if( newDefinitionsAvailable ){
				versionID = archive.updateDefinitions();
				
				//Log a message indicating that the definitions were updated
				EventLogMessage message = new EventLogMessage(EventType.DEFINITION_SET_UPDATED);
				message.addField(new EventLogField(FieldName.DEFINITION_SET_REVISION, versionID.toString()));
				Application.getApplication().logEvent(message);
			}
			else{
				versionID = archive.getVersionID();
				definitionsCurrent = true;
				
				//Log a message indicating that the definitions were checked but no new ones exist
				EventLogMessage message = new EventLogMessage(EventType.DEFINITIONS_CURRENT);
				message.addField(new EventLogField(FieldName.DEFINITION_SET_REVISION, versionID.toString()));
				Application.getApplication().logEvent(message);
			}
		}
		catch(Exception e){
			EventLogMessage message = new EventLogMessage(EventType.TASK_FAILED);
			message.addField(new EventLogField(FieldName.TASK, this.getTaskDescription()));
			Application.getApplication().logExceptionEvent(message, e);
			exceptionThrown = e;
		}
		
		state = State.STOPPED;
	}

	public Throwable getException() {
		return exceptionThrown;
	}

}
