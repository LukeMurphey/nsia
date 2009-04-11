package net.lukeMurphey.nsia.eventLog;


public abstract class MessageFormatter {

	/**
	 * Get a description of the format.
	 */
	public abstract String getDescription();
	
	/**
	 * Get a string representation of the message format.
	 */
	public abstract String formatMessage(EventLogMessage message);	
	
}
