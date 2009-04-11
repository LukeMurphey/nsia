package net.lukeMurphey.nsia;

public class NameStringPair {

	private String name;
	private String value;

	public NameStringPair(String name, String value){
		this.name = name;
		this.value = value;
	}

	public String getName(){
		return name;
	}

	public String getValue(){
		return value;
	}
}
