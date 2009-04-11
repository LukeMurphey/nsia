package net.lukeMurphey.nsia.htmlInterface;

import java.util.Vector;

public class NavigationPath {
	
	public int HTML_COOKIE_CRUMB_HREF = 0;
	public int HTML_COOKIE_CRUMB_TITLE = 1;
	
	private Vector<String[]> cookieCrumbs = new Vector<String[]>();
	
	public NavigationPath(){
		
	}
	
	public void addPathEntry( String name, String location ){
		String[] entry = new String[2];
		entry[HTML_COOKIE_CRUMB_HREF] = location;
		entry[HTML_COOKIE_CRUMB_TITLE] = name;
		cookieCrumbs.add(entry);
	}
	
	public int getLength(){
		return cookieCrumbs.size();
	}
	
	public String getLocation( int c ){
		if( c < cookieCrumbs.size() ){
			String[] entry = (String[])cookieCrumbs.get(c);
			if( entry != null )
				return entry[HTML_COOKIE_CRUMB_HREF];
			else
				return null;
		}
		else
			return null;
	}
	
	public String getName( int c ){
		if( c < cookieCrumbs.size() ){
			String[] entry = (String[])cookieCrumbs.get(c);
			if( entry != null )
				return entry[HTML_COOKIE_CRUMB_TITLE];
			else
				return null;
		}
		else
			return null;
	}
	

}
