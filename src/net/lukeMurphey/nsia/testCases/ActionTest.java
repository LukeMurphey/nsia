package net.lukeMurphey.nsia.testCases;

import java.util.Vector;

import net.lukeMurphey.nsia.responseModule.Action.MessageVariable;
import junit.framework.TestCase;

public class ActionTest extends TestCase {
	
	public void testProcessMessage(){
		
		Vector<MessageVariable> vars = new Vector<MessageVariable>();
		
		vars.add( new MessageVariable("$SiteGroupName", "LukeMurphey.net") );
		vars.add( new MessageVariable("$SiteGroupID", "1") );
		vars.add( new MessageVariable("$Deviations", "5") );
		
		String template = "$SiteGroupName has been rejected by ThreatFactor NSIA. $Deviations deviations have been noted at $DetectionTime.";
		
		String result = MessageVariable.processMessageTemplate(template, vars);
		
		if( !result.equalsIgnoreCase("LukeMurphey.net has been rejected by ThreatFactor NSIA. 5 deviations have been noted at [DetectionTime was undefined].") ){
			fail("The processed text did not match the expected output.");
		}
	}

}
