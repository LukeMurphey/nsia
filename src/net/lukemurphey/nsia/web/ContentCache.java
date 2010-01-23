package net.lukemurphey.nsia.web;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Vector;

public class ContentCache {
	
	/**
	 * Represents a cached entry.
	 * @author Luke
	 *
	 */
	public class ContentCacheEntry{
		private String name;
		private long entry_created;
		private long entry_expires;
		private ByteBuffer bytes;
		
		public ContentCacheEntry( String name, int expires_in_seconds ){
			this.name = name;
			entry_created = System.currentTimeMillis();
			entry_expires = entry_created + (1000 * expires_in_seconds);
		}
		
		/**
		 * Get the unique name of the cache entry.
		 * @return
		 */
		public String getUniqueName(){
			return name;
		}
		
		/**
		 * Returns true if the entry has expired.
		 * @return
		 */
		public boolean isExpired(){
			if( System.currentTimeMillis() > entry_expires ){
				return true;
			}
			else{
				return false;
			}
		}
		
		public void write( String content ){
			bytes.put( content.getBytes() );
		}
	}
	
	private Vector<ContentCacheEntry> entries = new Vector<ContentCacheEntry>(); 
	
	/**
	 * Get the entry associated with the given name.
	 * @param name
	 * @return
	 */
	public ContentCacheEntry getEntry(String name){
		Iterator<ContentCacheEntry> it = entries.iterator();
		
		while( it.hasNext() ){
			if( it.next().isExpired() ){
				it.remove();
			}
			else if(it.next().getUniqueName().equalsIgnoreCase(name)){
				return it.next();
			}
		}
		
		return null;
	}
	
	/**
	 * Removes old entries from the list that have expired.
	 */
	public void pruneExpiredEntries(){
		
		Iterator<ContentCacheEntry> it = entries.iterator();
		
		while( it.hasNext() ){
			if( it.next().isExpired() ){
				it.remove();
			}
		}
	}
}
