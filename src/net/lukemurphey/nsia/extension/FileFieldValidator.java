package net.lukemurphey.nsia.extension;

import java.io.File;

public class FileFieldValidator implements FieldValidator {

	private boolean ensureFileExists;
	private boolean acceptFolder;
	private boolean acceptFile;
	
	public FileFieldValidator( boolean ensureFileExists, boolean acceptFolder, boolean acceptFile ){
		
		// 0 -- Precondition check
		
		// Make sure the validator is set to accept either a file or folder (otherwise, it always rejects)
		if( acceptFolder == false && acceptFile == false ){
			throw new IllegalArgumentException("This validator must accept either a file or folder (or both); this validator was set to reject both files and folders and will therefore reject everything");
		}
		
		
		// 1 -- Initialize the class
		this.ensureFileExists = ensureFileExists;
		this.acceptFolder = acceptFolder;
		this.acceptFile = acceptFile;
	}
	
	public FieldValidatorResult validate(String value) {
		
		File file = new File(value);
		
		// 1 -- Determine if the file/folder exists
		if( value == null || value.trim().length() == 0 ){
			if( acceptFile && acceptFolder ){
				return new FieldValidatorResult("The location cannot be empty", false);
			}
			else if( acceptFile ){
				return new FieldValidatorResult("The file cannot be empty", false);
			}
			else{
				return new FieldValidatorResult("The directory cannot be empty", false);
			}
		}
		
		if( ensureFileExists == true && file.exists() == false ){
			
			// Return a message indicating that the file/folder doesn't exist; but tailor the message accordingly
			if( acceptFolder && acceptFile ){
				return new FieldValidatorResult("The given location does not exist", false);
			}
			else if( acceptFolder ){
				return new FieldValidatorResult("The directory does not exist", false);
			}
			else {
				return new FieldValidatorResult("The file does not exist", false);
			}
		}
		
		// 2 -- Determine if the value given is a folder (and reject if it must be a file)
		if( file.isDirectory() && acceptFolder == false ){
			return new FieldValidatorResult("The path given is a file (not a folder)", false);
		}
		
		// 3 -- Determine if the value given is a folder (and reject if it must be a file)
		if( file.isDirectory() == false && acceptFile == false ){
			return new FieldValidatorResult("The path given is a folder (not a file)", false);
		}
		
		// 4 -- If all the tests passed, then accept the input
		return new FieldValidatorResult(true);
	}

}
