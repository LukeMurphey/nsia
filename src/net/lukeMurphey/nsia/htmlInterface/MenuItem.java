package net.lukeMurphey.nsia.htmlInterface;

public class MenuItem{
	public static final int LEVEL_ONE = 0;
	public static final int LEVEL_TWO = 1;
	public static final int LEVEL_BREAK = 2;
	
	public String href;
	public String title;
	public String onClick;
	public int level;
	
	public MenuItem( String title, String href, int level ){
		this.title = title;
		this.href = href;
		this.level = level;
	}
	
	public MenuItem( String title, String href, int level, String onClick ){
		this.title = title;
		this.href = href;
		this.level = level;
		this.onClick = onClick; 
	}
}
