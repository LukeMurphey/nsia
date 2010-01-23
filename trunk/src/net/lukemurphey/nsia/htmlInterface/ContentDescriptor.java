package net.lukemurphey.nsia.htmlInterface;

/**
 * The following class represents the results of processing the component of a page.
 * @author luke
 *
 */
public class ContentDescriptor {

	private String title;
	private String body;
	private boolean isCompletePage = true;
	
	ContentDescriptor( String title, String body ){
		this.title = title;
		this.body = body;
	}
	
	ContentDescriptor( String title, StringBuffer body ){
		this.title = title;
		this.body = body.toString();
	}
	
	ContentDescriptor( String title, String body, boolean isCompletePage ){
		this.title = title;
		this.body = body;
		this.isCompletePage = isCompletePage;
	}
	
	ContentDescriptor( String title, StringBuffer body, boolean isCompletePage ){
		this.title = title;
		this.body = body.toString();
		this.isCompletePage = isCompletePage;
	}
	
	public String getBody(){
		return body;
	}
	
	public String getTitle(){
		return title;
	}
	
	public boolean isCompletePage(){
		return isCompletePage;
	}
}
