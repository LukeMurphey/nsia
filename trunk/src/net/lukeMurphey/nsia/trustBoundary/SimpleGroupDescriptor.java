package net.lukeMurphey.nsia.trustBoundary;

import net.lukeMurphey.nsia.GroupManagement;
import net.lukeMurphey.nsia.GroupManagement.GroupDescriptor;

public class SimpleGroupDescriptor{
	protected String name;
	protected boolean isEnabled;
	protected long id;
	
	public SimpleGroupDescriptor( String name, boolean isEnabled, long id){
		this.name = name;
		this.isEnabled = isEnabled;
		this.id = id;
	}
	
	public SimpleGroupDescriptor(GroupDescriptor groupDesc){
		this.name = groupDesc.getGroupName();
		this.isEnabled = groupDesc.getGroupState() == GroupManagement.State.ACTIVE;
		this.id = groupDesc.getGroupId();
	}
	
	public boolean isEnabled(){
		return isEnabled;
	}
	
	public String getName(){
		return name;
	}
	
	public long getIdentifier(){
		return id;
	}
}
