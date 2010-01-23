package net.lukemurphey.nsia.scan;

import java.nio.charset.Charset;

public interface PreProcessor {

	/**
	 * This class represents data that has been preprocessed.
	 * @author Luke Murphey
	 *
	 */
	public class PreProcessedData{
		private Charset charset = null;
		private String string = null;
		private byte[] bytes = null;
		private String preprocessorName;
		
		protected PreProcessedData( String preprocessorName, String string, Charset charset ) {
			this.preprocessorName = preprocessorName;
			this.string = string;
			this.charset = charset;
		}
		
		protected PreProcessedData( String preprocessorName, byte[] bytes ) {
			this.preprocessorName = preprocessorName;
			this.bytes = new byte[bytes.length];
			
			System.arraycopy(bytes, 0, this.bytes , 0, bytes.length);
			
			this.bytes = bytes;
		}
		
		public String getPreProcessorName(){
			return preprocessorName;
		}
		
		public boolean isString(){
			if( string != null ){
				return true;
			}
			else{
				return false;
			}
		}
		
		public byte[] getBytes(){
			if( string != null ){
				return string.getBytes();
			}
			else{
				return bytes;
			}
		}
		
		public String getString(){
			if( string != null ){
				return string;
			}
			else{
				return new String(bytes);
			}
		}
		
		public Charset getCharset(){
			return charset;
		}
	}
	
	/**
	 * Returns that name that describes the preprocessor (used in signatures in order to identify the requested preprocessor).
	 * @return
	 */
	public String getName();
	
	public String preProcessString( String data );
	
	public byte[] preProcessBytes( byte[] data );
	
}
