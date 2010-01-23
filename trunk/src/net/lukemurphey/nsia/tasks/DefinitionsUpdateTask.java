package net.lukemurphey.nsia.tasks;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.DefinitionUpdateWorker;

import java.util.TimerTask;

public class DefinitionsUpdateTask extends TimerTask {
	
	public DefinitionsUpdateTask(Application app){
		
	}
	
	public void run(){
		// 1 -- Get the signature update worker
		DefinitionUpdateWorker worker = new DefinitionUpdateWorker(  );
		
		// 2 -- Start the process of downloading and installing new updates
		worker.run();
	}
	
}
