package net.lukemurphey.nsia.htmlInterface;

import java.io.IOException;
import java.util.*;
import java.io.PrintWriter;

public class ContentTemplate {

	private static int TYPE_SUBSTITUION = 0;
	private static int TYPE_CONTENT = 1;
	
	private int staticContentSize = 0;
	
	private static int TITLE = 0;
	private static int USER_OPTIONS = 1;
	private static int DASHBOARD_HEADER = 2;
	private static int MAIN_CONTENT = 3;
	private static int VERSION = 4;
	
	/**
	 * Represents an element of a template content.
	 * @author Luke Murphey
	 *
	 */
	private interface Element{
		public int getTypeID();
		
		public int getVarID();
		
		public String getValue();
	}
	
	/**
	 * Represents a block of content.
	 * @author Luke Murphey
	 *
	 */
	private static class SubstitutionElement implements Element{
		
		private int name;
		
		public SubstitutionElement( String name ){
			
			if( name.equalsIgnoreCase("Title") ){
				this.name = ContentTemplate.TITLE;
			}
			else if( name.equalsIgnoreCase("UserOptions") ){
				this.name = ContentTemplate.USER_OPTIONS;
			}
			else if( name.equalsIgnoreCase("DashboardHeader") ){
				this.name = ContentTemplate.DASHBOARD_HEADER;
			}
			else if( name.equalsIgnoreCase("MainContent") ){
				this.name = ContentTemplate.MAIN_CONTENT;
			}
			else if( name.equalsIgnoreCase("Version") ){
				this.name = ContentTemplate.VERSION;
			}
		}
		
		@Override
		public int getVarID(){
			return name;
		}
		
		@Override
		public int getTypeID(){
			return TYPE_SUBSTITUION;
		}

		@Override
		public String getValue() {
			return "";
		}
	}
	
	/**
	 * An element that will be replaced with other text.
	 * @author Luke Murphey
	 *
	 */
	private static class ContentElement implements Element{
		
		private String value;
		
		public ContentElement ( String value ){
			this.value = value;
		}
		
		@Override
		public String getValue(){
			return value;
		}
		
		@Override
		public int getTypeID(){
			return TYPE_CONTENT;
		}

		@Override
		public int getVarID() {
			return -1;
		}
	}
	
	private Vector<Element> elements = new Vector<Element>();
	
	/**
	 * A class that represents the list of arguments that will be used to produce the final content.
	 * @author Luke Murphey
	 *
	 */
	public static class TemplateVariables{
		private String title;
		String userOptions;
		String dashboardHeader;
		String mainContent;
		String version;
		
		private int size;
		
		public TemplateVariables( String title, String userOptions, String dashboardHeader, String mainContent, String version ){
			this.title = title;
			this.userOptions = userOptions;
			this.dashboardHeader = dashboardHeader;
			this.mainContent = mainContent;
			this.version = version;
			
			size += this.title.length();
			size += this.userOptions.length();
			size += this.dashboardHeader.length();
			size += this.mainContent.length();
			size += this.version.length();
		}
		
		public int getLength(){
			return size;
		}
	}
	
	/**
	 * A class that represents the final content.
	 * @param content
	 */
	public ContentTemplate( String content ){
		
		// 1 -- Convert the content into a compiled object
		int startIndex = 0;
		int lastIndex = content.indexOf("<%");
		int endIndex = -1;
		
		while( lastIndex > 0 ){
			
			ContentElement contentElem = new ContentElement( content.substring(startIndex, lastIndex) );
			elements.add( contentElem );
			
			staticContentSize += contentElem.getValue().length();
			
			endIndex = content.indexOf("%>", lastIndex);
			
			elements.add( new SubstitutionElement( content.substring(lastIndex+2, endIndex) ) );
			
			startIndex = endIndex + 2;
			lastIndex = content.indexOf("<%", lastIndex+1);
		}
		
		// Add any content that remains after the last variable
		endIndex = content.lastIndexOf("%>");
		
		if( endIndex > 0 ){
			ContentElement contentElem = new ContentElement( content.substring(endIndex+2, content.length()) );
			elements.add( contentElem );
			staticContentSize += contentElem.getValue().length();
		}
	}
	
	public int getLength(){
		return staticContentSize;
	}
	
	public int send( TemplateVariables vars, PrintWriter writer ) throws IOException{
		int contentLength = staticContentSize + vars.getLength();
		
		for (Element curElement : elements) {
			if( curElement.getTypeID() == ContentTemplate.TYPE_SUBSTITUION ){
				
				//SubstitutionElement subElement = (SubstitutionElement)curElement;
				
				if( curElement.getVarID() == TITLE ){
					writer.print( vars.title );
				}
				else if( curElement.getVarID() == DASHBOARD_HEADER ){
					writer.print( vars.dashboardHeader );
				}
				else if( curElement.getVarID() == MAIN_CONTENT ){
					writer.print( vars.mainContent );
				}
				else if( curElement.getVarID() == USER_OPTIONS ){
					writer.print( vars.userOptions );
				}
				else if( curElement.getVarID() == VERSION ){
					writer.print( vars.version );
				}
			}
			else{
				writer.print( curElement.getValue() );
			}
		}
		
		return contentLength;
		
	}
	
	
}
