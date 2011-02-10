package net.lukemurphey.nsia;

import net.lukemurphey.nsia.LicenseDescriptor.LicenseStatus;
import net.lukemurphey.nsia.eventlog.EventLogField;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import net.lukemurphey.nsia.eventlog.EventLogField.FieldName;
import net.lukemurphey.nsia.eventlog.EventLogMessage.EventType;
import net.lukemurphey.nsia.rest.LicenseInfo;
import net.lukemurphey.nsia.rest.RESTRequestFailedException;

import java.sql.SQLException;
import java.util.regex.Pattern;

public class LicenseManagement {
	
	private static final Pattern LICENSE_KEY_REGEX = Pattern.compile("[a-zA-Z0-9]{4,4}(-?[a-zA-Z0-9]{4,4}){4,4}");
	public static final String LICENSE_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	
	//private Application application = null;
	
	private LicenseManagement(){
		
	}
	
	public static boolean validateLicenseKey( String key ) throws LicenseValidationException, InputValidationException{
		
		LicenseDescriptor licenseKey = getKeyInfo( key );
		
		return licenseKey.isValid();
	}
	
	public static LicenseDescriptor getKeyInfo( String key ) throws LicenseValidationException, InputValidationException{
		
		// 1 -- Return a unlicensed key descriptor if the key is null
		if( key == null ){
			return new LicenseDescriptor(LicenseStatus.UNLICENSED);
		}
		
		// 2 -- Make sure the license key is legal
		if( LICENSE_KEY_REGEX.matcher(key).matches() == false ){
			throw new InputValidationException("The license key is not a valid key", "LicenseKey", key);
		}
		
		// 3 -- Get the license information from the server
		String uniqueInstallID = null;
		
		// Try to get the unique installation identifier
		try{
			Application.getApplication().getApplicationConfiguration().getUniqueInstallationID();
		}
		catch(InputValidationException e){
			//Could not get a unique install ID. Log the problem and just use an empty string.
			uniqueInstallID = "";
			Application.getApplication().logExceptionEvent( new EventLogMessage( EventType.INTERNAL_ERROR, new EventLogField(FieldName.MESSAGE, "Unable to get unique installation identifier") ), e);
		} catch (NoDatabaseConnectionException e) {
			//Could not get a unique install ID. Log the problem and just use an empty string.
			uniqueInstallID = "";
			Application.getApplication().logExceptionEvent( new EventLogMessage( EventType.INTERNAL_ERROR, new EventLogField(FieldName.MESSAGE, "Unable to get unique installation identifier") ), e);
		} catch (SQLException e) {
			//Could not get a unique install ID. Log the problem and just use an empty string.
			uniqueInstallID = "";
			Application.getApplication().logExceptionEvent( new EventLogMessage( EventType.INTERNAL_ERROR, new EventLogField(FieldName.MESSAGE, "Unable to get unique installation identifier") ), e);
		}
		
		try{
			LicenseInfo licenseInfo = new LicenseInfo(uniqueInstallID, key);
			
			return licenseInfo.getLicenseInformation();
		}
		catch(RESTRequestFailedException e){
			throw new LicenseValidationException("Unable to get information about the license");
		}
	}

}

