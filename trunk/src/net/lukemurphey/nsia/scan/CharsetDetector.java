package net.lukemurphey.nsia.scan;

import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsICharsetDetectionObserver;

/**
 * This class attempts to identify the character set of the given data using the algorithm used in Mozilla. 
 * @author luke
 *
 */
public class CharsetDetector extends nsDetector {
	
	private static class CharsetDetectorObserver implements nsICharsetDetectionObserver{
		
		public String detectedCharset;
		public boolean found = false;
		public void Notify(String charset){
			detectedCharset = charset;
			found = true;
		}
	}
	
	private CharsetDetectorObserver observer = new CharsetDetectorObserver();
	
	/**
	 * Detect the character set from the given data.
	 * @param sample
	 * @return
	 * @throws EncodingDetectionFailedException
	 */
	public static String detectCharset( byte[] sample ) throws EncodingDetectionFailedException{
		return detectCharset( sample, sample.length);
	}
	
	/**
	 * Detect the character set from the given data.
	 * @param sample
	 * @param length
	 * @return
	 * @throws EncodingDetectionFailedException
	 */
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
	
	private CharsetDetector(){
		super.Init(observer);
	}
}