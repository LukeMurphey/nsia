package net.lukemurphey.nsia;

import java.io.*;

import eu.medsea.util.*;

import javax.activation.MimetypesFileTypeMap;

import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;

public class MimeType {

	private static MimetypesFileTypeMap typeMap = null;
	
	private static MimetypesFileTypeMap getMimeTypeMap(){
		
		if( typeMap == null ){
			
			// 1 -- Determine if the mime-types file exists and load it if it does
			String fileName = "../etc/mime.types";
			
			try{	
				File file = new File( fileName );
				
				if( file.exists() ){
					typeMap = new MimetypesFileTypeMap( fileName );
				}
			}
			catch(IOException e){
				EventLogMessage message = new EventLogMessage( EventLogMessage.EventType.INTERNAL_ERROR );
				message.addField( new EventLogField(FieldName.MESSAGE, "Unable to load mime-types file") );
				message.addField( new EventLogField(FieldName.FILE, fileName) );
				
				Application.getApplication().logExceptionEvent( message, e);
			}
			
			// 2 -- Otherwise, load the embedded mime.type file
			InputStream in = null;
			
			if( typeMap == null ){
				try{
					in = MimeType.class.getResourceAsStream("mime.types");
					typeMap = new MimetypesFileTypeMap( in );
				}
				finally{
					if( in != null ){
						try {
							in.close();
						} catch(IOException e){
							
							EventLogMessage message = new EventLogMessage( EventLogMessage.EventType.INTERNAL_ERROR );
							message.addField( new EventLogField(FieldName.MESSAGE, "Unable to load embedded mime.types file") );
							
							Application.getApplication().logExceptionEvent( message, e);
						}
					}
				}
			}
		}
		return typeMap;
	}
	
	/**
	 * Get the mimetype from the file name (not the file contents).
	 * @param filename
	 * @return
	 */
	public static String getMimeTypeFromName(String filename){
		
		// 1 -- Return null (no content-type identified) if the filename is null
		if(filename == null){
			return null;
		}
		
		// 2 -- Determine if the filename has an extension (don't try to get the mimetype if no file extension exists, it won't return a valid response)
		int index = filename.lastIndexOf(".");
		if( index < 0){
			return null;
		}
		
		// 3 -- Try to get the type from the filename
		//Note that the method below returns "octet-stream" if no match could be found based on filename
		return getMimeTypeMap().getContentType(filename);
	}
	
	private static String filterLookupFailure( String contentType ){
		if( contentType == null || contentType.equalsIgnoreCase(MimeUtil.UNKNOWN_MIME_TYPE)){
			return null;
		}
		else{
			return contentType;
		}
	}
	
	/**
	 * Get the mimetype from the file. This method will attempt to extract the file type from the file contents, then from the name (if the contents do not reveal the file type).
	 * @param file
	 * @return
	 */
	public static String getMimeType(File file){
		String mimeType = MimeUtil.getMimeType(file);
		
		if( mimeType == null || mimeType.equalsIgnoreCase( MimeUtil.UNKNOWN_MIME_TYPE ) ){
			return getMimeTypeFromName(file.getName());
		}
		else{
			return mimeType;
		}
	}
	
	/**
	 * Get the mimetype from the file contents.
	 * @param content
	 * @return
	 */
	public static String getMimeType(byte[] content){
		
		if( content == null || content.length == 0){
			return null;
		}
		
		String mimeType = MimeUtil.getMimeType(content);
		return filterLookupFailure(mimeType);
	}
	
	/**
	 * Get the mimetype from the file contents, then it will use the name if the contents do not reveal the file type.
	 * @param content
	 * @return
	 */
	public static String getMimeType(byte[] content, String filename){
		
		String mimeType =null;
		
		if( content != null && content.length > 0 ){
			mimeType = MimeUtil.getMimeType(content);
		}
		
		mimeType = filterLookupFailure(mimeType);
		
		if(mimeType == null){
			return getMimeTypeFromName(filename);
		}
		else{
			return mimeType;
		}
	}
	
	/**
	 * Get the mimetype from the file contents, then it will use the name if the contents do not reveal the file type.
	 * @param content
	 * @return
	 */
	public static String getMimeType(byte[] content, String filename, String defaultMimeType){
		String mimeType = null;
		
		if( content != null && content.length > 0){
			mimeType = MimeUtil.getMimeType(content);
		}
		//System.out.println( "Detected mime type = " + mimeType );
		mimeType = filterLookupFailure(mimeType);
		
		if(mimeType == null){
			mimeType = getMimeTypeFromName(filename);
			
			if( "application/octet-stream".equalsIgnoreCase(mimeType) ){
				return defaultMimeType;
			}
			else{
				return mimeType;
			}
		}
		else{
			return mimeType;
		}
	}
}
