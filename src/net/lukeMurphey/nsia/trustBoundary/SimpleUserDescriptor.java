package net.lukeMurphey.nsia.trustBoundary;

import net.lukeMurphey.nsia.UserManagement;
import net.lukeMurphey.nsia.UserManagement.UserDescriptor;

public class SimpleUserDescriptor {
	protected String name;
	protected boolean isEnabled;
	protected int id;
	
	public SimpleUserDescriptor( String name, boolean isEnabled, int id){
		this.name = name;
		this.isEnabled = isEnabled;
		this.id = id;
	}
	
	public SimpleUserDescriptor(UserDescriptor userDesc){
		this.name = userDesc.getUserName();
		this.isEnabled = userDesc.getAccountStatus() == UserManagement.AccountStatus.VALID_USER;
		this.id = userDesc.getUserID();
	}
	
	public boolean isEnabled(){
		return isEnabled;
	}
	
	public String getUserName(){
		return name;
	}
	
	public int getUserID(){
		return id;
	}
}
