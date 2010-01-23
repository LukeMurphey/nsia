package net.lukemurphey.nsia.web;

import java.util.Vector;

import net.lukemurphey.nsia.SessionManagement.SessionInfo;

/**
 * Provides a mechanism for managing messages to be used for a during their session.
 * @author Luke
 *
 */
public class SessionMessages {
	
	public enum MessageSeverity{
		INFORMATION, SUCCESS, WARNING, ALERT;
	}
	
	/**
	 * This class represents a session message.
	 * @author Luke
	 *
	 */
	public class SessionMessageEntry{
		private long tracking_number;
		private MessageSeverity severity;
		private String message;
		
		public SessionMessageEntry( long tracking_number, MessageSeverity severity, String message ){
			this.tracking_number = tracking_number;
			this.severity = severity;
			this.message = message;
		}
		
		public void delete(){
			messages.remove(this);
		}
		
		public String getMessage(){
			return message;
		}
		
		public String getMessageAndDelete(){
			delete();
			return message;
		}
		
		public MessageSeverity getSeverity(){
			return severity;
		}
		
		public boolean isInformational(){
			return severity == MessageSeverity.INFORMATION;
		}
		
		public boolean isSuccess(){
			return severity == MessageSeverity.SUCCESS;
		}
		
		public boolean isAlert(){
			return severity == MessageSeverity.ALERT;
		}
		
		public boolean isWarning(){
			return severity == MessageSeverity.WARNING;
		}
	}
	
	private Vector<SessionMessageEntry> messages = new Vector<SessionMessageEntry>();
	
	/**
	 * Deletes the messages associated with the given session.
	 * @param tracking_number
	 */
	public void purgeMessages( int tracking_number ){
		getAndDeleteMessages(tracking_number);
	}
	
	/**
	 * Adds a message to the list.
	 * @param session
	 * @param message
	 * @param sev
	 */
	public void addMessage( SessionInfo session, String message, MessageSeverity sev ){
		SessionMessageEntry msg = new SessionMessageEntry(session.getTrackingNumber(), sev, message);
		
		messages.add(msg);
	}
	
	/**
	 * Gets the messages for the given session and removes them.
	 * @param session
	 * @return
	 */
	public SessionMessageEntry[] getAndDeleteMessages( SessionInfo session ){
		return getAndDeleteMessages( session.getTrackingNumber() );
	}
	
	/**
	 * Gets the messages for the given session and removes them.
	 * @param tracking_number
	 * @return
	 */
	public SessionMessageEntry[] getAndDeleteMessages( long tracking_number ){
		SessionMessageEntry[] entries = getMessages( tracking_number );
		
		for (SessionMessageEntry sessionMessageEntry : entries) {
			messages.remove(sessionMessageEntry);
		}
		
		return entries;
	}
	
	/**
	 * Gets the messages the given session.
	 * @param session
	 * @return
	 */
	public SessionMessageEntry[] getMessages( SessionInfo session ){
		return getMessages( session.getTrackingNumber() );
	}
	
	/**
	 * Gets the messages the given session.
	 * @param tracking_number
	 * @return
	 */
	public SessionMessageEntry[] getMessages( long tracking_number ){
		
		Vector<SessionMessageEntry> msgs = new Vector<SessionMessageEntry>();
		
		for (SessionMessageEntry msg : messages) {
			if( msg.tracking_number == tracking_number ){
				msgs.add(msg);
			}
		}
		
		SessionMessageEntry[] entries = new SessionMessageEntry[msgs.size()];
		msgs.toArray(entries);
		
		return entries;
	}

}
