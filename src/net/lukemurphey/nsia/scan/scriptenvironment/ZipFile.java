package net.lukemurphey.nsia.scan.scriptenvironment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.Vector;

import net.lukemurphey.nsia.scan.DataSpecimen;
import net.lukemurphey.nsia.scan.CharsetDetector;
import net.lukemurphey.nsia.scan.EncodingDetectionFailedException;

/**
 * This class represents zip archive and allows information about the archive to be obtained and files to be extracted
 * @author Luke
 *
 */
public class ZipFile {

	// Default maximum size of a file (in bytes) that will be returned from an archive
	public static final int DEFAULT_FILE_SIZE_LIMIT = 1048576; // 1 MB
	
	/**
	 * This class is used to build an internal representation of the files in the archive.
	 * @author Luke
	 *
	 */
	private class ZipFileEntry{
		
		private String name;
		private String comment;
		private long compressedSize;
		private long crc;
		private int method;
		private long size;
		private long time;
		
		/**
		 * This constructor populates the class based on the ZipEntry from the archive.
		 * @param ze
		 */
		public ZipFileEntry( ZipEntry ze ){
			name = ze.getName();
			comment = ze.getComment();
			compressedSize = ze.getCompressedSize();
			crc = ze.getCrc();
			method = ze.getMethod();
			size = ze.getSize();
			time = ze.getTime();
		}
		
		/**
		 * Gets the file name.
		 * @return
		 */
		public String getName(){
			return name;
		}
		
		/**
		 * Gets the file comment if it exists.
		 * @return
		 */
		public String getComment(){
			return comment;
		}
		
		/**
		 * Gets the comressed size as a long.
		 * @return
		 */
		public long getCompressedSize(){
			return compressedSize;
		}
		
		/**
		 * Gets the CRC as a long.
		 * @return
		 */
		public long getCrc(){
			return crc;
		}
		
		/**
		 * Get the method.
		 * @return
		 */
		public int getMethod(){
			return method;
		}
		
		/**
		 * Gets the uncompressed size of the file.
		 * @return
		 */
		public long getSize(){
			return size;
		}
		
		/**
		 * Gets the file timestamp as a long.
		 * @return
		 */
		public long getTime(){
			return time;
		}
	}
	
	// The list of the files in the archive
	private Vector<ZipFileEntry> entries = new Vector<ZipFileEntry>();
	
	// The data specimen that was used to populate the archive
	private DataSpecimen dataSpecimen;
	
	/**
	 * This constructor populates the archive from the data contained in the given data specimen.
	 * @param d
	 * @throws IOException
	 */
	public ZipFile( DataSpecimen d ) throws IOException{
		dataSpecimen = d;
		populateZipEntries();
	}
	
	/**
	 * Populates the internal list of files.
	 * @throws IOException
	 */
	private void populateZipEntries() throws IOException{
		entries = getZipFileEntries(dataSpecimen); 
	}
	
	/**
	 * Retrieves the list of files from the data specimen (which presumably represents a zip file).
	 * @param dataSpecimen
	 * @return
	 * @throws IOException
	 */
	private Vector<ZipFileEntry> getZipFileEntries( DataSpecimen dataSpecimen ) throws IOException{
		Vector<ZipFileEntry> temp_entries = new Vector<ZipFileEntry>();
		ByteArrayInputStream bis = null;
		ZipInputStream zin = null;
		
		try{
			bis = new ByteArrayInputStream( dataSpecimen.getBytes() );
			zin = new ZipInputStream( bis );
			
			ZipEntry entry;
			
			while((entry = zin.getNextEntry()) != null) {
				temp_entries.add( new ZipFileEntry(entry) );
			}
		}
		finally{
			
			// Close the input streams.
			if( bis != null ){
				bis.close();
			}
			
			if( zin != null ){
				zin.close();
			}
		}
		
		return temp_entries;
	}
	
	/**
	 * Get a list of the filenames.
	 * @return
	 */
	public String[] getFilenames(){
		Vector<String> v = new Vector<String>();
		
		for(ZipFileEntry zfe : entries ){
			v.add(zfe.getName());
		}
		
		String a[] = new String[v.size()];
		return v.toArray(a);
	}
	
	/**
	 * Get the ZipEntry that corresponds to the given file name.
	 * @param filename
	 * @return
	 */
	private ZipFileEntry getEntry( String filename ){
		
		if( filename == null ){
			return null;
		}
		
		for (ZipFileEntry entry : entries) {
			if( filename.equalsIgnoreCase( entry.getName() ) ){
				return entry;
			}
		}
		
		return null;
		
	}
	
	/**
	 * Indicates if a file by the given name exists.
	 * @param filename
	 * @return
	 */
	public boolean hasFile( String filename ){
		if( getEntry(filename) != null ){
			return true;
		}
		else{
			return false;
		}
	}
	
	/**
	 * Get the comment of the file with the given name (if it exists)
	 * @param filename
	 * @return
	 */
	public String getComment( String filename ){
		
		ZipFileEntry zfe = getEntry(filename);
		
		if( zfe != null ){
			return zfe.getComment();
		}
		
		return null;
	}
	
	/**
	 * Get the size of the file with the given name (if it exists)
	 * @param filename
	 * @return
	 */
	public long getSize( String filename ){
		
		ZipFileEntry zfe = getEntry(filename);
		
		if( zfe != null ){
			return zfe.getSize();
		}
		
		return -1;
	}
	
	/**
	 * Get the timestamp of the file with the given name (if it exists)
	 * @param filename
	 * @return
	 */
	public long getTime( String filename ){
		
		ZipFileEntry zfe = getEntry(filename);
		
		if( zfe != null ){
			return zfe.getTime();
		}
		
		return -1;
	}
	
	/**
	 * Get the CRC of the file with the given name (if it exists)
	 * @param filename
	 * @return
	 */
	public long getCrc( String filename ){
		
		ZipFileEntry zfe = getEntry(filename);
		
		if( zfe != null ){
			return zfe.getCrc();
		}
		
		return -1;
	}
	
	/**
	 * Get the compression method of the file with the given name (if it exists)
	 * @param filename
	 * @return
	 */
	public long getMethod( String filename ){
		
		ZipFileEntry zfe = getEntry(filename);
		
		if( zfe != null ){
			return zfe.getMethod();
		}
		
		return -1;
	}
	
	/**
	 * Get the compressed size of the file with the given name (if it exists)
	 * @param filename
	 * @return
	 */
	public long getCompressedSize( String filename ){
		
		ZipFileEntry zfe = getEntry(filename);
		
		if( zfe != null ){
			return zfe.getCompressedSize();
		}
		
		return -1;
	}
	
	public byte[] getFileAsBytes( String filename ) throws IOException{
		return getFileAsBytes(filename, DEFAULT_FILE_SIZE_LIMIT);
	}
	
	/**
	 * 
	 * @param filename
	 * @return
	 * @throws IOException 
	 */
	public byte[] getFileAsBytes( String filename, int sizelimit ) throws IOException{
		
		// Make sure the filename is not null
		if( filename == null ){
			return null;
		}
		
		// Get the file bytes
		ByteArrayInputStream bis = null;
		ZipInputStream zin = null;
		ZipEntry entry = null;
		
		try{
			bis = new ByteArrayInputStream( dataSpecimen.getBytes() );
			zin = new ZipInputStream( bis );
			
			while((entry = zin.getNextEntry()) != null) {
				if( filename.equalsIgnoreCase( entry.getName() ) ){
					
					long maxSizeLong = Math.min(sizelimit, entry.getSize());
					int maxSize;
					
					// Convert the long to an integer. Convert it to the greatest maximum integer if too large for an integer.
					if( maxSizeLong > Integer.MAX_VALUE ){
						maxSize = Integer.MAX_VALUE;
					}
					else{
						maxSize = (int)maxSizeLong;
					}
					
					// Make sure the value does not go beyond the size limit
					maxSize = Math.min(sizelimit, maxSize);
					
					// Initialize the buffer
					byte[] b = new byte[ maxSize ];
					
					// Read in the file
					zin.read(b);
					
					// Return the result
					return b;
				}
			}
		}
		finally{
			
			// Close the input streams.
			if( bis != null ){
				bis.close();
			}
			
			if( zin != null ){
				zin.close();
			}
		}
		
		// File not found, returning null...
		return null;
	}
	
	/**
	 * Get the file (as a string) that corresponds to the given filename. The character set will be automatically detected
	 * and UTF-8 will be used if the encoding cannot be determined. 
	 * @param filename
	 * @return
	 * @throws IOException 
	 */
	public String getFileAsString( String filename ) throws IOException{
		
		// 1 -- Get the file bytes
		byte[] fileBytes = getFileAsBytes(filename, DEFAULT_FILE_SIZE_LIMIT);
		
		if( fileBytes == null ){
			return null;
		}
		
		// 2 -- Determine the character set of the contained file
		Charset utf8 = Charset.availableCharsets().get("UTF8");
		Charset charset = null;
		
		try {
			String encoding = CharsetDetector.detectCharset(fileBytes);
			charset = Charset.availableCharsets().get(encoding);
			
			if( charset == null ){
				charset = utf8;
			}
		} catch (EncodingDetectionFailedException e1) {
			charset = utf8;
		}
		
		// 3 -- Get the text of the file
		String filetext = null;
		
		if( charset == null ){
			filetext = new String(fileBytes);
		}
		else{
			filetext = new String(fileBytes, charset);
		}
		
		return filetext;
		
	}
}
