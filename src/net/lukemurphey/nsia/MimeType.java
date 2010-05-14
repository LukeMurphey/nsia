package net.lukemurphey.nsia;

import java.io.*;
import eu.medsea.util.*;

import javax.activation.MimetypesFileTypeMap;

import net.lukemurphey.nsia.eventlog.EventLogMessage;

public class MimeType {

	private static MimetypesFileTypeMap typeMap = null;
	
	private static MimetypesFileTypeMap getMimeTypeMap(){
		try{
			if( typeMap == null ){
				typeMap = new MimetypesFileTypeMap("../etc/mime.types" );
			}
		}
		catch(IOException e){
			Application.getApplication().logExceptionEvent( EventLogMessage.EventType.INTERNAL_ERROR, e);
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
