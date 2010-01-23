package net.lukemurphey.nsia.web;

public class ViewNotFoundException extends Exception {

	private static final long serialVersionUID = 8856516776313825998L;
	
	private String view_name;
	
	public ViewNotFoundException( String view_name ){
		super("View with the name \"" + view_name + "\" was not found");
		this.view_name = view_name;
	}
	
	public String getViewName(){
		return view_name;
	}
	
}
