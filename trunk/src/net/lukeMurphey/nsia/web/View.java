package net.lukemurphey.nsia.web;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * An HTTP view; processes the HTTP request and returns an HTTP response.
 * @author Luke
 *
 */
public abstract class View {
	
	private Pattern[] arguments;
	private String base_path;
	private String[] base_path_split;
	private String name;
	
	private static String PATH_SPLIT_REGEX = "(/|\\\\)";
	
	public View( String base_path, String name, Pattern... arguments){
		
		// 0 -- Precondition check
		if(base_path == null){
			throw new IllegalArgumentException("Base path cannot be null");
		}
		
		if( name == null || name.trim().length() == 0 ){
			throw new IllegalArgumentException("The name of the View URI cannot be null or empty");
		}
		
		// 1 -- Initialize the class
		this.arguments = arguments;
		this.base_path = base_path;
		this.base_path_split = this.base_path.split(PATH_SPLIT_REGEX);
		this.name = name;
	}
	
	/**
	 * Get the name of the view.
	 * @return
	 */
	public String getName(){
		return name;
	}
	
	/**
	 * Process the request and return the HTTP response.
	 * @param request
	 * @param arguments
	 * @return boolean indicating if the view matches the request.
	 */
	public boolean process( HttpServletRequest request, HttpServletResponse response, RequestContext context ) throws ViewFailedException{
		return process( request, response, context, false);
	}

	/**
	 * 
	 * @param request
	 * @param response
	 * @param context
	 * @param ignore_arguments Causes the view to process the arguments even if they don't match
	 * @return
	 * @throws ViewFailedException
	 */
	public boolean process( HttpServletRequest request, HttpServletResponse response, RequestContext context, boolean ignore_arguments ) throws ViewFailedException{
		
		String[] args;
		
		try{
			if( request.getPathInfo() == null ){
				args = parseArgs(request.getServletPath());
			}
			else{
				args = parseArgs(request.getServletPath() + request.getPathInfo());
			}
		}
		catch(URLInvalidException e){
			if( ignore_arguments == false ){
				return false;
			}
			args = new String[0];
		}
		
		try {
			return process( request, response, context, args, Shortcuts.getMapWithBasics(context, request) );
		} catch (IOException e) {
			throw new ViewFailedException(e);
		} catch (URLInvalidException e) {
			throw new ViewFailedException(e);
		} catch (ViewNotFoundException e) {
			throw new ViewFailedException(e);
		}
	}
	
	protected abstract boolean process( HttpServletRequest request, HttpServletResponse response, RequestContext context, String[] args, Map<String, Object> data ) throws ViewFailedException, URLInvalidException, IOException, ViewNotFoundException;
	
	/**
	 * Replaces slashes that may be interpreted as path separators with a value that will not be interpreted as a path separator. 
	 * @param arg
	 * @return
	 */
	protected String escapeSlashes(String arg){
		return arg;
	}
	
	/**
	 * Unescape the slashes in the path.
	 * @param arg
	 * @return
	 */
	protected String unescapeSlashes(String arg){
		return arg;
	}
	
	/**
	 * Returns true if this view handles this request.
	 * @param request
	 * @return
	 */
	public boolean servesRequest( HttpServletRequest request ){
		try {
			parseArgs( request.getPathInfo() );
			return true;
		} catch (URLInvalidException e) {
			return false;
		}
	}
	
	/**
	 * Create a URL for the given arguments and view.
	 * @param provided_args
	 * @return
	 * @throws URLInvalidException
	 */
	public String createURL( Object... provided_args ) throws URLInvalidException{
		
		StringBuffer path = new StringBuffer();
		path.append( base_path );
		
		// 1 -- Append leading slash if necessary
		if( base_path.startsWith("/") == false && base_path.startsWith("\\") == false ){
			path.insert(0, "/");
		}
		
		// 2 -- Throw an exception if the number of provided arguments are more than the possible number
		if( provided_args.length > arguments.length ){
			throw new URLInvalidException("View " + name + " does not accept " + provided_args.length + " arguments" );
		}
		
		// 3 -- Append trailing slash if necessary
		if( provided_args.length > 0 && base_path.endsWith("/") == false && base_path.endsWith("\\") == false ){
			path.append("/");
		}
		
		// 4 -- Convert the arguments to strings
		for( int c = 0; c < provided_args.length; c++ ){
			String current_arg = provided_args[c].toString();
			
			if( arguments[c].matcher(current_arg).matches() == false ){
				throw new URLInvalidException("Argument " + c + " does not match for view " + name);
			}
			else{
				path.append( escapeSlashes(current_arg) );
				path.append("/");
			}
		}
		
		// 4 -- Return the URL
		return path.toString();
		
	}
	
	protected String normalizePath(String path){
		
		if( path.startsWith("\\") ){
			path = path.substring(1);
		}
		
		if( path.startsWith("/") ){
			path = path.substring(1);
		}
		
		if( path.endsWith("/") ){
			path = path.substring(0,path.length()-1);
		}
		
		if( path.endsWith("\\") ){
			path = path.substring(0,path.length()-1);
		}
		
		return path;
		
	}
	
	/**
	 * Parse out the list of optional arguments.
	 * @param path
	 * @return
	 * @throws URLInvalidException
	 */
	protected String[] parseArgs( String path ) throws URLInvalidException{
		
		// 0 -- Precondition check
		if( path == null ){
			throw new IllegalArgumentException("Path cannot be null");
		}
		
		// 1 -- Normalize the path
		path = normalizePath(path);
		String[] path_split = path.split(PATH_SPLIT_REGEX);
		
		// 2 -- Make sure that the base path matches
		
		//	 2.1 -- Make sure the number of arguments is the same
		if(base_path_split.length > path_split.length){
			throw new URLInvalidException("Base path does not match");
		}
		
		//   2.2 -- Make sure the arguments are the same
		for(int c=0; c < base_path_split.length; c++ ){
			if( base_path_split[c].equalsIgnoreCase(path_split[c]) == false ){
				throw new URLInvalidException("Base path does not match");
			}
		}
		/*
		if( path.startsWith(base_path) == false ){
			throw new URLInvalidException("Base path does not match"); //Does not match the base path
		}*/
		
		// 3 -- Parse the arguments
		//String argstr = trimSlashes(path.substring(0, base_path.length()));
		//String[] args = argstr.split("/");
		
		String[] args_final = new String[path_split.length-base_path_split.length];
		System.arraycopy(path_split, base_path_split.length, args_final, 0, args_final.length);
		
		checkNonBaseArgs(args_final);
		return args_final;
		//return checkArgs(args);
	}
	
	protected void checkNonBaseArgs( String[] args ) throws URLInvalidException{
		
		// 1 -- Make sure it doesn't have more args than possible
		if( args.length > arguments.length ){
			throw new URLInvalidException("Too many arguments provided to the view " + name);
		}
		
		// 2 -- Check the provided arguments
		for( int c = 0; c < args.length; c++ ){
			Matcher matcher = arguments[c].matcher(args[c]);
			
			if( matcher.matches() == false ){
				throw new URLInvalidException("Argument " + c + " does not match for view " + args[c]);
			}
		}
		
		// 3 -- Check the arguments that were not supplied to make sure they accept empty values
		for( int c = args.length; c < arguments.length; c++ ){
			if( arguments[c].matcher("").matches() == false ){
				throw new URLInvalidException("Empty argument " + c + " is not allowed to be empty for view " + name);
			}
		}
	}
	
	/**
	 * Compares the provided arguments to the list and validates them (ensures they are correct).
	 * @param args
	 * @return
	 * @throws URLInvalidException 
	 */
	protected String[] checkArgs( String[] args ) throws URLInvalidException{
		
		// 1 -- Make sure we don't have too many arguments
		if( args.length > (arguments.length + base_path_split.length) ){
			throw new URLInvalidException("Too many arguments provided for view " + name);
		}
		
		// 2 -- Check each of the arguments provided
		for( int c = 0; c < args.length; c++ ){
			
			if( c <= base_path_split.length ){
				if( args[c].equalsIgnoreCase( base_path_split[c] ) == false ){
					throw new URLInvalidException("Argument " + c + " does not match for view " + args[c]);
				}
			}
			else if( (c-base_path_split.length) < arguments.length ){
				
				Matcher matcher = arguments[c-base_path_split.length].matcher(args[c]);
				
				if( matcher.matches() == false ){
					throw new URLInvalidException("Argument " + c + " does not match for view " + args[c]);
				}
			}
		}
		
		// 3 -- Check the arguments that were not supplied to make sure they accept empty values
		for( int c = args.length; c < arguments.length; c++ ){
			if( arguments[c].matcher("").matches() == false ){
				throw new URLInvalidException("Empty argument " + c + " is not allowed to be empty for view " + name);
			}
		}
		
		// 4 -- Return the resulting set of arguments (that are confirmed as matching)
		return args;
	}

}
