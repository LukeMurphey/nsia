package net.lukemurphey.nsia.web;

public class Link {
	
	private String href = null;
	private String title = null;
	
	public Link(String name, String href ){
		this.title = name;
		this.href = href;
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
	
}
