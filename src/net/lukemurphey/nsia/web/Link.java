package net.lukemurphey.nsia.web;

import java.util.Vector;

public class Link {
	
	public static class Attribute{
		private String name;
		private String value;
		
		public Attribute(String name, String value){
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
	
	private String href = null;
	private String title = null;
	private Vector<Attribute> attributes = new Vector<Attribute>();
	
	public Link(String name, String href ){
		this.title = name;
		this.href = href;
	}
	
	public Link(String name, String href, Attribute... attrs ){
		this.title = name;
		this.href = href;
		
		for (Attribute attribute : attrs) {
			attributes.add(attribute);
		}
	}
	
	public Link(String name ){
		this.title = name;
	}
	
	public String getLink(){
		return href;
	}
	
	public String getTitle(){
		return title;
	}
	
	public Attribute[] getAttributes(){
		Attribute[] attributesTemp = new Attribute[attributes.size()];
		attributes.toArray(attributesTemp);
		return attributesTemp;
	}
	
}
