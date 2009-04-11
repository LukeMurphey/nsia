package net.lukeMurphey.nsia.scanRules;

import java.nio.charset.*;

import net.lukeMurphey.nsia.MimeType;

import org.mozilla.intl.chardet.*;

/**
 * This class represents data that will be analyzed. The objective of this class is to facilitate conversion between raw bytes and a String by detecting the character set
 * encoding (using the algorithm used by Mozilla/Firefox). This class will normalize the input string to UTF-16. Furthermore, this class will cache the converted data in 
 * order to prevent repeated conversion (for a performance increase) and duplicated code (reduces programmer effort).
 * @author luke
 *
 */
public class DataSpecimen {
	
	private byte[] bytes = null;
	private String string = null;
	private Charset encoding = null;
	private String basicEncodedString = null;
	private String contentType = null;
	private String filename;
	
	/** The Byte Order Mark for big-endian UTF-16 */
    private static byte [] BIGEND_BOM = {(byte) 0xfe, (byte) 0xff};

    /** The Byte Order Mark for little-endian UTF-16 */
    private static byte [] LITTLEEND_BOM = {(byte) 0xff, (byte) 0xfe};
    
    private enum BomType{
    	NONE, LITTLE, BIG
    }
    
    public DataSpecimen( byte[] data, String encoding, String filename, String suggestedContentType ){
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the data is not null 
		if( data == null ){
			throw new IllegalArgumentException("The data cannot be null");
		}
		
		// The initialize methods will do the rest of the precondition checks
		
		
		/* The charset below will be the default character set. ISO-8859-1 was selected as the default because:
		 * 	1) It is extremely common (default for web-pages and most text files)
		 *  2) It is 1 byte in length and uses all 256 characters, meaning the encodings won't remove 
		 *     characters. This is unseful in case the data is binary and a regular expression is being used. 
		 */
		Charset iso88591 = Charset.availableCharsets().get("ISO-8859-1");
		Charset encodingCharset;
		
		if( encoding == null ){
			try {
				encoding = detectedCharacterSet(data);
				encodingCharset = Charset.availableCharsets().get(encoding);
				
				if( encodingCharset == null ){
					encodingCharset = iso88591;
				}
			} catch (EncodingDetectionFailedException e1) {
				encodingCharset = iso88591;
			}
		}
		else{
			encodingCharset = Charset.availableCharsets().get(encoding);
			
			if( encodingCharset == null ){
				encodingCharset = iso88591;
			}
		}
		
		initialize(data, encodingCharset, filename, suggestedContentType);
		contentType = computeContentType(filename, data, suggestedContentType);
		
	}
    
    public DataSpecimen( byte[] data, String encoding, String filename ){
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the data is not null 
		if( data == null ){
			throw new IllegalArgumentException("The data cannot be null");
		}
		
		// The initialize methods will do the rest of the precondition checks
		
		
		/* The charset below will be the default character set. ISO-8859-1 was selected as the default because:
		 * 	1) It is extremely common (default for web-pages and most text files)
		 *  2) It is 1 byte in length and uses all 256 characters, meaning the encodings won't remove 
		 *     characters. This is unseful in case the data is binary and a regular expression is being used. 
		 */
		Charset iso88591 = Charset.availableCharsets().get("ISO-8859-1");
		Charset encodingCharset;
		
		if( encoding == null ){
			try {
				encoding = detectedCharacterSet(data);
				encodingCharset = Charset.availableCharsets().get(encoding);
				
				if( encodingCharset == null ){
					encodingCharset = iso88591;
				}
			} catch (EncodingDetectionFailedException e1) {
				encodingCharset = iso88591;
			}
		}
		else{
			encodingCharset = Charset.availableCharsets().get(encoding);
			
			if( encodingCharset == null ){
				encodingCharset = iso88591;
			}
		}
		
		initialize(data, encodingCharset, filename, null);
		contentType = computeContentType(filename, data, null);
	}
    
	public DataSpecimen( byte[] data, String encoding ){
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the data is not null 
		if( data == null ){
			throw new IllegalArgumentException("The data cannot be null");
		}
		
		// The initialize methods will do the rest of the precondition checks
		
		
		/* The charset below will be the default character set. ISO-8859-1 was selected as the default because:
		 * 	1) It is extremely common (default for web-pages and most text files)
		 *  2) It is 1 byte in length and uses all 256 characters, meaning the encodings won't remove 
		 *     characters. This is unseful in case the data is binary and a regular expression is being used. 
		 */
		Charset iso88591 = Charset.availableCharsets().get("ISO-8859-1");
		Charset encodingCharset;
		
		if( encoding == null ){
			try {
				encoding = detectedCharacterSet(data);
				encodingCharset = Charset.availableCharsets().get(encoding);
				
				if( encodingCharset == null ){
					encodingCharset = iso88591;
				}
			} catch (EncodingDetectionFailedException e1) {
				encodingCharset = iso88591;
			}
		}
		else{
			encodingCharset = Charset.availableCharsets().get(encoding);
			
			if( encodingCharset == null ){
				encodingCharset = iso88591;
			}
		}
		
		initialize(data, encodingCharset, null, null);
		contentType = computeContentType(filename, data, null);
	}
	
	public DataSpecimen( byte[] data, boolean rawBytes ){

		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the data is not null 
		if( data == null ){
			throw new IllegalArgumentException("The data cannot be null");
		}
		
		// The initialize methods will do the rest of the precondition checks
		
		
		// 1 -- Initialize the class
		Charset iso88591 = Charset.availableCharsets().get("ISO-8859-1");
		if( rawBytes ){
			initialize(data, iso88591, null, null );
		}
		else{
			Charset encodingCharset;

			try {
				String encoding = detectedCharacterSet(data);
				encodingCharset = Charset.availableCharsets().get(encoding);

				if( encodingCharset == null ){
					encodingCharset = iso88591;
				}
			} catch (EncodingDetectionFailedException e1) {
				encodingCharset = iso88591;
			}

			
			initialize(data, encodingCharset, null, null);
		}
		
		contentType = computeContentType(filename, data, null);
	}
	
	public DataSpecimen( byte[] data ){
		this(data, null);
	}
	
	public DataSpecimen( String data ){
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the data is not null 
		if( data == null ){
			throw new IllegalArgumentException("The data cannot be null");
		}
		
		Charset iso88591 = Charset.availableCharsets().get("ISO-8859-1");
		Charset encodingCharset;
		String encoding;
		
		this.string = data;
		bytes = data.getBytes();
		
		try {
			encoding = detectedCharacterSet(bytes);
			encodingCharset = Charset.availableCharsets().get(encoding);
			
			if( encodingCharset == null){
				encodingCharset = iso88591;
			}
		} catch (EncodingDetectionFailedException e) {
			encodingCharset = iso88591;
		}
		
		initialize(data, encodingCharset);
		contentType = computeContentType(null, bytes, null);
	}
	
	private void initialize( byte[] data, Charset encoding, String filename, String contentType ){
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the data is not null 
		if( data == null ){
			throw new IllegalArgumentException("The data cannot be null");
		}
		
		//	 0.2 -- Make sure the encoding is not null 
		if( encoding == null ){
			throw new IllegalArgumentException("The character set encoding cannot be null");
		}
		
		// 1 -- Copy the byte data over and create the string version
		bytes = new byte[data.length];
		System.arraycopy(data, 0, bytes, 0, Math.min( bytes.length, data.length) );//Note: the minimum method is used in case the array is change after this method is invoked
		
		string = new String(bytes, encoding);
		this.encoding = encoding;
		
		this.filename = filename;
	}
	
	private void initialize( String data, Charset encoding ){
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the data is not null 
		if( data == null ){
			throw new IllegalArgumentException("The data cannot be null");
		}
		
		//	 0.2 -- Make sure the encoding is not null 
		if( encoding == null ){
			throw new IllegalArgumentException("The character set encoding cannot be null");
		}
		
		string = new String(bytes, encoding);
		bytes = string.getBytes();
		this.encoding = encoding;
	}
	
	private String computeContentType(String filename, byte[] contents, String suggestedContentType){
		return MimeType.getMimeType(contents, filename, suggestedContentType);
	}
	
	public String getString(){
		return string;
	}
	
	public String getFilename(){
		return filename;
	}
	
	public String getContentType(){
		return contentType;
	}
	
	public byte[] getBytes(){
		if( bytes == null ){
			bytes = string.getBytes();
			return bytes.clone();
		}
		else
		{
			return bytes.clone();
		}
	}
	
	public static String detectedCharacterSet( byte[] sample ) throws EncodingDetectionFailedException{
		return CharsetDetector.detectCharset(sample);
	}
	
	private static class CharsetDetectorObserver implements nsICharsetDetectionObserver{
		
		public String detectedCharset;
		public boolean found = false;
		public void Notify(String charset){
			detectedCharset = charset;
			found = true;
		}
	}
	
	/**
	 * This class attempts to identify the character set of the given data using the algorithm used in Mozilla. 
	 * @author luke
	 *
	 */
	private static class CharsetDetector extends nsDetector {
		
		private CharsetDetectorObserver observer = new CharsetDetectorObserver();
		
		public static String detectCharset( byte[] sample ) throws EncodingDetectionFailedException{
			return detectCharset( sample, sample.length);
		}
		
		public static String detectCharset( byte[] sample, int length ) throws EncodingDetectionFailedException{
			
			CharsetDetector charsetDetect = new CharsetDetector();
			
			if( charsetDetect.isAscii(sample, sample.length) ){
				return "ASCII";
			}
			else{
				charsetDetect.DoIt(sample, length, false);
			}
			
			if( charsetDetect.observer.found == true){
				return charsetDetect.observer.detectedCharset;
			}
			else{
				throw new EncodingDetectionFailedException();
			}
			
		}
		
		public CharsetDetector(){
			super.Init(observer);
		}
	}
	
	public static byte[] subArray( byte[] bytes, int start){
		return subArray( bytes, start, -1);
	}
	
	public int getStringLength(){
		return string.length();
	}
	
	public int getBytesLength(){
		return bytes.length;
	}
	
	public Charset getEncoding(){
		return encoding;
	}
	
	public static byte[] subArray( byte[] bytes, int start, int end ){
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the array is not null
		if( bytes == null ){
			throw new IllegalArgumentException("The byte array cannot be null");
		}
		
		//	 0.2 -- Make sure the start value is not less than zero
		if( start < 0 ){
			throw new IllegalArgumentException("The start position in the array cannot be 0");
		}
		
		//	 0.3 -- Make sure the start value is not greater than the end position
		if( end > -1 && start > end ){
			throw new IllegalArgumentException("The start position must not be greater than the end value");
		}
		
		//	 0.4 -- Make sure the end value is not off of the end of the array
		if( end > (bytes.length - 1)){
			throw new IllegalArgumentException("The end position must not be greater than the number of bytes");
		}
		
		
		// 1 -- Create the array subset
		if( end < 0 ){
			end = bytes.length - 1;
		}
		
		byte[] subset = new byte[end - start + 1];
		System.arraycopy(bytes, start, subset, 0, end-start+1);
		
		return subset;
	}
	
	/**
	 * Method gets a substring of the data in this specimen, starting with the first byte following the given byte location.
	 * @param startAfterByte
	 * @return
	 */
	protected String getSubString( int startAfterByte ){
		
		// 0 -- Precondition check
		
		//	 0.1 -- Make sure the byte position is equal to or greater than zero
		if( startAfterByte < 0 ){
			throw new IllegalArgumentException("The byte position must be greater than zero");
		}
		
		//	 0.2 -- Make sure the byte position is within the bounds of the data
		/*if( bytePosition < 0 ){
			throw new IllegalArgumentException("The byte position must be greater than zero");
		}*/
		
		// 1 -- Determine if the data contains the BOM (byte-order mark) and readd it if it does (otherwise, the string will be corrupted)
		BomType bomChars = bomCharsType(bytes);
		
		byte[] newBytes = subArray(getBytes(), startAfterByte+1);
		
		//Reinsert BOM characters, otherwise the string will not be valid
		if( bomChars == BomType.BIG ){
			byte[] newBytesCorrected = new byte[newBytes.length+2];
			newBytesCorrected[0] = BIGEND_BOM[0];
			newBytesCorrected[1] = BIGEND_BOM[1];
			System.arraycopy(newBytes, 0, newBytesCorrected, 2, newBytes.length);
			newBytes = newBytesCorrected;
		}
		else if( bomChars == BomType.LITTLE ){
			byte[] newBytesCorrected = new byte[newBytes.length+2];
			newBytesCorrected[0] = LITTLEEND_BOM[0];
			newBytesCorrected[1] = LITTLEEND_BOM[1];
			System.arraycopy(newBytes, 0, newBytesCorrected, 2, newBytes.length);
			newBytes = newBytesCorrected;
		}
		else{
			newBytes = subArray(getBytes(), startAfterByte+1);
		}
		
		if( encoding != null ){
			return new String(newBytes, encoding);
		}
		else{
			return new String(newBytes);
		}
	}
	
	private static BomType bomCharsType( byte[] bytes ){
		/* Determine if the first character is the Byte-Order mark (BOM)
		 * See http://www.streambase.com/developers/library/articles/handlingUTF16characterstreams/
		 * 
		 * Note that check below should be equivalent to:
		 * if( Character.isIdentifierIgnorable( getString().charAt(0) ) == true ) {
		 */
		if((bytes[0] == BIGEND_BOM[0] && bytes[1] == BIGEND_BOM[1])) {
			return BomType.BIG;
		}
		else if(bytes[0] == LITTLEEND_BOM[0] && bytes[1] == LITTLEEND_BOM[1]){
			return BomType.LITTLE;
		}
		else{
			return BomType.NONE;
		}
	}
	
	public String getBasicEncodedString(){
		if( basicEncodedString == null ){
			Charset iso88591 = Charset.availableCharsets().get("ISO-8859-1");
			
			if( encoding == iso88591){
				basicEncodedString = string;
				return basicEncodedString;
			}
			else{
				basicEncodedString = new String(bytes, iso88591);
				return basicEncodedString;
			}
		}
		else
		{
			return basicEncodedString;
		}
	}
	
	public int getByteIndex( int charPosition ){
		return getByteIndex( charPosition, false );
	}
	
	public void setEncoding( Charset encoding ){
		this.encoding = encoding;
	}
	
	/**
	 * Returns an integer that indicates that the last byte that is part of the character at the position given.
	 * @param charPosition
	 * @param addOffsetForBom The offset does not account for the BOM (meaning that char position needs to be incremented by one if the BOM exists)
	 * @return
	 */
	private int getByteIndex( int charPosition, boolean valueExcludesBom ){
		
		BomType bomType = bomCharsType( getBytes() );
		
		if( valueExcludesBom == true ){
			if( bomType == BomType.BIG || bomType == BomType.LITTLE) {
				
				//Add one to get past the byte-order mark (it really is not a character, Java should probably remove this automatically but doesn't since it is not officially part of the standard)
				charPosition += 1;
				
			}
		}
		
		String temp = getString().substring(0, charPosition + 1);
		byte[] tempBytes;
		
			if( encoding != null )
				tempBytes = temp.getBytes(encoding);
			else
				tempBytes = temp.getBytes();
			
			return tempBytes.length - 1;
	}

}
