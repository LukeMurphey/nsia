package net.lukeMurphey.nsia;

public interface DatabaseSerialization {

	public Object load(int ID, Application application);
	
	public void save();
	
}
