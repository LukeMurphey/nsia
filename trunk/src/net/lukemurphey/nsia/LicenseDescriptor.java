package net.lukemurphey.nsia;

import java.util.Date;

/**
 * Provides a description of a license.
 * @author Luke Murphey
 *
 */
public class LicenseDescriptor{
	private String licensee;
	private String licenseKey;
	private Date expires;
	private Type licenseType;
	private LicenseStatus status;
	
	/**
	 * Describes the type of the license
	 * @author Luke Murphey
	 *
	 */
	public enum Type{
		DEMO, STANDARD, PARTNER, NON_PROFIT, BETA_TEST, UNDEFINED, FREE;
	}
	
	/**
	 * Describes the license status.
	 * @author Luke Murphey
	 *
	 */
	public enum LicenseStatus{
		EXPIRED("Expired"), DISABLED("Disabled"), ILLEGAL("Illegal Key"), UNLICENSED("Unlicensed"), DEMO("Demo License"), ACTIVE("Active"), UNVALIDATED("Unvalidated");
		
		private String description;
		private LicenseStatus(String description){
			this.description = description;
		}
		
		public String getDescription(){
			return description;
		}
	}
	
	private LicenseDescriptor( ){
		
	}
	
	protected LicenseDescriptor( LicenseStatus status ){
		this.expires = null;
		this.licensee = null;
		this.licenseKey = null;
		this.licenseType = null;
		this.status = status;
	}
	
	public LicenseDescriptor( String licensee, String licenseKey, Date expires, Type licenseType, LicenseStatus status){
		this.expires = expires;
		this.licensee = licensee;
		this.licenseKey = licenseKey;
		this.licenseType = licenseType;
		this.status = status;
	}
	
	/**
	 * Create a unchecked license signifying that the license could not be checked.
	 * @param licenseKey
	 * @return
	 */
	public static LicenseDescriptor uncheckedLicense( String licenseKey ){
		LicenseDescriptor desc = new LicenseDescriptor();
		desc.expires = null;
		desc.licensee = null;
		desc.licenseKey = licenseKey;
		desc.licenseType = null;
		desc.status = LicenseStatus.UNVALIDATED;
		desc.isValid();
		
		return desc;
	}
	
	/**
	 * Get the license key.
	 * @return
	 */
	public String getKey(){
		return licenseKey;
	}
	
	/**
	 * Get the expiration date of the license.
	 * @return
	 */
	public Date getExpirationDate(){
		if( expires != null ){
			return (Date)expires.clone();
		}
		else{
			return null;
		}
	}
	
	/**
	 * Get the type of the license.
	 * @return
	 */
	public Type getType(){
		return licenseType;
	}
	
	/**
	 * Get the status of the license.
	 * @return
	 */
	public LicenseStatus getStatus(){
		return status;
	}
	
	/**
	 * Get the person who this license it licensed to.
	 * @return
	 */
	public String getLicensee(){
		return licensee;
	}
	
	/**
	 * Determine if the license is valid. If the license is past the expiration date, then the status will be changed to expired.
	 * @return
	 */
	public boolean isValid(){
		
		// 1 -- Determine if the status is active
		if( getStatus() != LicenseStatus.ACTIVE ){
			return false;
		}
		
		// 2 -- Determine if the license has expired
		if( getExpirationDate().before( new Date() )){
			status = LicenseStatus.EXPIRED;
			return false;
		}
		
		return true;
	}
}