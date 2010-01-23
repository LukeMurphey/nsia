package net.lukemurphey.nsia.web;

import java.util.Vector;

public class ViewList {
	
	private Vector<View> views = new Vector<View>();

	/**
	 * Register the view so that it is served by the servlet.
	 * @param new_view
	 * @throws ViewAlreadyExistsException
	 */
	public synchronized void registerView( View new_view ) throws ViewAlreadyExistsException{
		
		// 1 -- Make sure the view does not already exist
		if( findView(new_view.getName()) != null ){
			throw new ViewAlreadyExistsException();
		}
		
		// 2 -- Add the view
		views.add(new_view);
	}
	
	/**
	 * Find the view that matches the given name.
	 * @param name
	 * @return
	 */
	public synchronized View findView( String name ){
		for (View view : views) {
			
			if( view.getName().equalsIgnoreCase( name )){
				return view;
			}
		}
		
		return null;
	}
	
	/**
	 * Get a list of the registered views.
	 * @return
	 */
	public Vector<View> getViews(){
		return views;
	}
	
}
