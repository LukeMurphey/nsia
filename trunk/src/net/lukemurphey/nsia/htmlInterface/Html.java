package net.lukemurphey.nsia.htmlInterface;

import java.util.Hashtable;
import java.util.Vector;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.text.DecimalFormat;

import java.io.PrintWriter;
import java.io.StringWriter;

import net.lukemurphey.nsia.GenericUtils;
import net.lukemurphey.nsia.extension.Extension;
import net.lukemurphey.nsia.extension.FieldFile;
import net.lukemurphey.nsia.extension.FieldLayout;
import net.lukemurphey.nsia.extension.FieldPassword;
import net.lukemurphey.nsia.extension.FieldText;
import net.lukemurphey.nsia.extension.PrototypeField;
import net.lukemurphey.nsia.extension.FieldLayout.FieldRow;


public class Html {
	
	public static int HTML_COOKIE_CRUMB_TITLE = 0;
	public static int HTML_COOKIE_CRUMB_HREF = 1;
	
	public enum MessageType{
		INFORMATIONAL, WARNING, CRITICAL
	}
	
	public static class Message{
		private String message;
		private MessageType type;
		private long userId;
		
		public Message(MessageType type, String message, long userId){
			this.message = message;//StringEscapeUtils.escapeHtml( message );
			this.type = type;
			this.userId = userId;
		}
		
		public MessageType getType(){
			return type;
		}
		
		public String getMessage(){
			return message;
		}
		
		public long getUserID(){
			return userId;
		}
	}
	
	private static Vector<Message> messageQueue = new Vector<Message>();
	
	public static void addMessage( MessageType type, String text, long userID ){
		messageQueue.add(new Message(type, text, userID));
	}
	
	public static String renderMessages( long userID ){
		
		try{
			StringBuffer output = new StringBuffer();
			
			Iterator<Message> it = messageQueue.iterator();
			
			synchronized(messageQueue){
				
				while( it.hasNext() ){
					Message message = it.next();
					
					if( message.userId == userID && message.type == MessageType.INFORMATIONAL){
						output.append( getInfoNote(message.getMessage()) );
						it.remove();
					}
					else {//if( message.userId == userID && message.type == MessageType.WARNING){
						output.append( getWarningNote(message.getMessage()) );
						it.remove();
					}
				}
			}
			
			return output.toString();
		}
		catch(NoSuchElementException e){
			return "";
		}
	}
	
	public static String getButton( String imageUrl, String imageAlt, String url, String text ){
		String output = "<table><tr><td><a href=\"" + url + "\"><img class=\"imagebutton\" src=\"" + imageUrl + "\" alt=\"" + imageAlt + "\"></a></td><td><a href=\"" + url + "\">" + text + "</a></td></tr></table>";
		return output;
	}
	
	public static String getTableFromStrings( Object[] object, int columns){
		
		StringBuffer buffer = new StringBuffer();
		buffer.append("<table>");
		
		for( int c = 0; c < object.length; c++ ){
			
			//System.err.println( c + " % " + columns + " = " + (c % columns));
			if( (c % columns) == 0 ){
				buffer.append( "<tr>" );
				//System.err.println("Start of row");
			}
			else if( (c % columns) == (columns - 1) ){
				buffer.append( "</tr>" );
				//System.err.println("End of row  ");
			}
			
			buffer.append( "<td>" );
			buffer.append( object[c].toString() );
			buffer.append( "</td>" );
		}
		
		buffer.append("</table>");
		
		return buffer.toString();
	}
	
	public static String getInfoNote( String message ){
		String output = "<table><tr><td style=\"vertical-align: top;\"><img src=\"/16_Information\" alt=\"Info\"/></td>";
		output += "<td class=\"InfoText\">" + message + "<td></tr></table>";
		
		return output;
	}
	
	public static String getWarningNote( String message ){
		String output = "<table><tr><td><img src=\"/16_Warning\" alt=\"Warning\"></td>";
		output += "<td class=\"WarnText\">" + message + "<td></tr></table>";

		return output;
	}
	
	public static String getWarningDialog( String title, String message){
		return getWarningDialog( title, message, null, null);
	}
	
	public static String getInformationDialog( String title, String message, String suggestedLink, String suggestedLinkTitle){
		String output = "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\"><tr><td class=\"AlignedTop\" rowspan=\"2\"><img src=\"/32_Information\" alt=\"Warning\">&nbsp;&nbsp;</td>";
		output += "<td class=\"Text_2\"><span class=\"Text_2\">" + title + "</span><td></tr>";
		output += "<tr><td>" + message + "<td></tr>";
		
		if( suggestedLink != null && suggestedLinkTitle != null){
			output += "<tr><td>&nbsp;</td><td>&nbsp;<td></tr>";
			output += "<tr><td>&nbsp;</td><td><a href=\"" + suggestedLink + "\">[" + suggestedLinkTitle + "]</a><td></tr>";
		}
		
		return output + "</table>";
	}
	
	public static String getWarningDialog( String title, String message, String suggestedLink, String suggestedLinkTitle){
		String output = "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\"><tr><td class=\"AlignedTop\" rowspan=\"2\"><img src=\"/32_Warning\" alt=\"Warning\">&nbsp;&nbsp;</td>";
		output += "<td class=\"Text_2\"><span class=\"WarnText\">" + title + "</span><td></tr>";
		output += "<tr><td>" + message + "<td></tr>";
		
		if( suggestedLink != null && suggestedLinkTitle != null){
			output += "<tr><td>&nbsp;</td><td>&nbsp;<td></tr>";
			output += "<tr><td>&nbsp;</td><td><a href=\"" + suggestedLink + "\">[" + suggestedLinkTitle + "]</a><td></tr>";
		}
		
		return output + "</table>";
	}
	
	public static String getDialog( String message, String title, String icon ){
		return getDialog( message, title, icon, false);
	}
	
	public static String getDialog( String message, String title, String icon, boolean alignCenter ){
		String output;
		output = "<table";
		
		if( alignCenter == true ){
			output += " align=\"center\"";
		}
		
		output += " width=\"70%\"><tr><td colspan=\"99\">&nbsp;</td></tr><tr><td width=\"32\" valign=\"top\" rowspan=\"2\"><img src=\"" + icon + "\" alt=\"icon\"></td><td class=\"Text_2\">";
		output += title + "</td></tr><tr><td>";
		output += message;
		output += "<p></table>";
		
		return output;
	}
	
	public static String getTimeDescription( long secs ){
		double doubleSecs = secs;
		DecimalFormat twoPlaces = new DecimalFormat("0.00");
		
		if( secs < 60 )
			return secs + " sec";
		else if ( secs < 3600 )
			return twoPlaces.format( doubleSecs/60 ) + " min";
		else if ( secs < 86400 )
			return twoPlaces.format( doubleSecs/3600 ) + " hours";
		else
			return twoPlaces.format( doubleSecs/86400 ) + " days";
	}
	
	/**
	 * Shorten a string to the desired length and append "..." in the most appropriate location.
	 * @param input
	 * @param desiredLength
	 * @return
	 */
	public static String shortenString( String input, int desiredLength){
		return GenericUtils.shortenString(input, desiredLength);
	}
	
	public static String getNavigationPath( NavigationPath cookieCrumbs ){
		
		String output = "";//"<table><tr>";
		
		for( int c = 0; c < cookieCrumbs.getLength(); c++ ){
			
			if( c != 0)
				output += " / ";

			output += "<a class=NavBar href=\"" + cookieCrumbs.getLocation(c) + "\">"  + cookieCrumbs.getName(c) + "</a>";
			
		}
		
		return output;
	}
	
	public static String getMenu( Vector<MenuItem> menuItems){
		
		String output = "";
		
		for( int c = 0; c < menuItems.size(); c++){
			MenuItem menuEntry = menuItems.get(c);
			
			if( c > 0 && menuEntry.level == MenuItem.LEVEL_ONE )
				output += "<br>&nbsp;";
			if( c > 0 )
				output += "<br>";

			// output the offset
			if( menuEntry.level == MenuItem.LEVEL_TWO ){
				output += "&nbsp;&nbsp;<img alt=\"*\" src=\"/Arrow\">";
			}
			else
				output += "&nbsp;";
			
			// Output the entry title + link
			if( menuEntry.href != null){
				if( menuEntry.onClick == null )
					output += "<a href=\"" + menuEntry.href + "\">" + menuEntry.title + "</a>";
				else
					output += "<a onClick=\"" + menuEntry.onClick + "\" href=\"" + menuEntry.href + "\">" + menuEntry.title + "</a>";
			}
			else
				output += menuEntry.title;
		}

		return output;
	}
	
	public static String getMainContent( String mainContent, String menu, String cookieCrumbs ){
		String output = "<table align=\"center\" width=\"100%\"><tr><td rowspan=\"99\" width=\"24\">&nbsp;</td><td colspan=\"3\">";
		output += cookieCrumbs + "<br>&nbsp;</td></tr>";
		output += "<tr><td width=\"20%\" valign=\"top\">";
		output += menu + "</td><td width=\"8\" class=\"SmallSplitterDark\">&nbsp;</td><td width=\"80%\" valign=\"top\">";
		output += mainContent;
		output += "</td></tr></table>";
		
		return output;
	}
	
	public static String getMainContent( StringBuffer mainContent, String menu, String cookieCrumbs ){
		StringBuffer output = new StringBuffer();
		output.append("<table cellspacing=\"0\" align=\"left\" width=\"95%\"><tr>");
		output.append("<td rowspan=\"3\" class=\"Menu\" width=\"220px\" valign=\"top\"><br>");
		output.append( menu ).append("</td><td rowspan=\"3\" width=\"16\">&nbsp;</td><td valign=\"top\">");
		
		output.append("<div style=\"margin-bottom: 16px;\" class=\"BottomBorder\"><br>");//valign=\"bottom\"
		output.append(cookieCrumbs + "&nbsp;</div>");
		output.append("<div>");
		output.append( mainContent );
		output.append("</div>");
		
		output.append("</td></tr></table>");
		/*output.append("<tr><td valign=\"top\"><br>");
		output.append( mainContent );
		output.append("</td></tr><tr><td>&nbsp;</td></tr></table>");*/
		
		return output.toString();
	}
	
	public static String getSectionHeader( String title ){
		return getSectionHeader( title, null);
	}
	
	public static String getSectionHeader( String title, String subTitle ){
		String output = "<span class=\"Text_1\">" + title + "</span>";
		if( subTitle != null ){
			output += "<br><span class=\"LightText\">" + subTitle + "</span>";
		}
		return output + "<p>";
	}
	
	public static String getBytesDescription( long bytes ){
		double bytesDouble = bytes;
		DecimalFormat twoPlaces = new DecimalFormat("0.00");
		
		if( bytes < 1024 )
			return bytes + " Bytes";
		else if ( bytes < 1048576 )
			return twoPlaces.format( bytesDouble/1024 ) + " KB";
		else if ( bytes < 1073741824 )
			return twoPlaces.format( bytesDouble/1048576 ) + " MB";
		else
			return twoPlaces.format( bytesDouble/1073741824 ) + " GB";
	}
	
	public static String splitString( String input, int charsPerLine, String parseString ){
		if( input == null )
			return null;
		
		if( input.length() <= charsPerLine )
			return input;
		
		int pointer = 0;
		String output = null;
		
		while( (pointer * charsPerLine ) < input.length() ){
			if( output == null )
				output = input.substring( 0, charsPerLine ) + parseString;
			else if( (( pointer + 1) * charsPerLine) >= input.length() )
				output += input.substring( pointer * charsPerLine ) + parseString;
			else
				output += input.substring( pointer * charsPerLine , pointer * charsPerLine + charsPerLine ) + parseString;
			
			pointer += 1;
		}
		
		return output;
	}
	
	public static String addBreaks( String input ){
		if( input != null ){
			input = input.replaceAll( "\t", "&nbsp;&nbsp;");
			input = input.replaceAll( "\r\n", "<br>");
			return input.replaceAll( "\n", "<br>");
		}
		else
			return null;
	}
	
	public static String getStackTrace(Throwable t)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        t.printStackTrace(pw);
        pw.flush();
        sw.flush();
        return sw.toString();
    }
	
	public static String getConfigForm( WebConsoleConnectionDescriptor requestDescriptor, FieldLayout layout, Hashtable<String, String> arguments ){
		StringBuffer buffer = new StringBuffer();
		
		for( FieldRow row : layout.getLayout() ){
			
			buffer.append("<tr>");
			
			for( PrototypeField field : row.getFields() ){
				buffer.append("<td class=\"Text_3\" colspan=\"" + field.getLayoutWidth() + "\"><div style=\"display: table-cell; vertical-align: top\">");
				buffer.append(field.getTitle());
				buffer.append("</div></td>");
				
				if( field instanceof FieldFile){
					buffer.append("<td colspan=\"" + field.getLayoutWidth() + "\">");
					buffer.append("<input enctype=\"multipart/form-data\" style=\"width: 400px\" type=\"file\" name=\"_" + field.getName() + "\">");
					buffer.append("</td>");
				}
				else if( field instanceof FieldText){
					
					FieldText text = (FieldText)field;
					
					if( text.getHeight() > 1 ){
						buffer.append("<td colspan=\"" + field.getLayoutWidth() + "\">");
						buffer.append("<textarea rows=\"" + text.getHeight() + "\" style=\"width: 400px\" name=\"_" + field.getName() + "\">");
						
						if( arguments.containsKey(field.getName())){
							buffer.append(arguments.get(field.getName()));
						}
						else if( field.getDefaultValue() != null ){
							buffer.append(field.getDefaultValue());
						}
						
						buffer.append("</textarea>");
						buffer.append("</td>");
					}
					else{
						buffer.append("<td colspan=\"" + field.getLayoutWidth() + "\">");
						buffer.append("<input class=\"textInput\" style=\"width: 400px\" type=\"text\" name=\"_" + field.getName() + "\"");
						
						if( arguments.containsKey(field.getName())){
							buffer.append(" value=\"");
							buffer.append(arguments.get(field.getName()));
							buffer.append("\"");
						}
						else if( field.getDefaultValue() != null ){
							buffer.append(" value=\"");
							buffer.append(field.getDefaultValue());
							buffer.append("\"");
						}
						
						buffer.append(">");
						buffer.append("</td>");
					}
				}
				else if( field instanceof FieldPassword){
					
					//FieldPassword text = (FieldPassword)field;
					
					buffer.append("<td colspan=\"" + field.getLayoutWidth() + "\">");
					buffer.append("<input class=\"textInput\" style=\"width: 400px\" type=\"password\" name=\"_" + field.getName() + "\">");
					buffer.append("</td>");
				}
				else{
					buffer.append("<td colspan=\"" + field.getLayoutWidth() + "\">");
					buffer.append("<input class=\"textInput\" style=\"width: 400px\" type=\"text\" name=\"_" + field.getName() + "\">");
					buffer.append("</td>");
				}
			}
			
			buffer.append("</tr>");
		}
		
		return buffer.toString();
	}
	
	public static String getConfigForm( WebConsoleConnectionDescriptor requestDescriptor, Extension extension, Hashtable<String, String> arguments ){
		StringBuffer buffer = new StringBuffer();
		
		FieldLayout layout = extension.getFieldLayout();
		
		for( FieldRow row : layout.getLayout() ){
			
			buffer.append("<tr>");
			
			for( PrototypeField field : row.getFields() ){
				buffer.append("<td class=\"Text_3\" colspan=\"" + field.getLayoutWidth() + "\"><div style=\"display: table-cell; vertical-align: top\">");
				buffer.append(field.getTitle());
				buffer.append("</div></td>");
				
				if( field instanceof FieldFile){
					buffer.append("<td colspan=\"" + field.getLayoutWidth() + "\">");
					buffer.append("<input enctype=\"multipart/form-data\" style=\"width: 400px\" type=\"file\" name=\"_" + field.getName() + "\">");
					buffer.append("</td>");
				}
				else if( field instanceof FieldText){
					
					FieldText text = (FieldText)field;
					
					if( text.getHeight() > 1 ){
						buffer.append("<td colspan=\"" + field.getLayoutWidth() + "\">");
						buffer.append("<textarea rows=\"" + text.getHeight() + "\" style=\"width: 400px\" name=\"_" + field.getName() + "\"></textarea>");
						buffer.append("</td>");
					}
					else{
						buffer.append("<td colspan=\"" + field.getLayoutWidth() + "\">");
						buffer.append("<input class=\"textInput\" style=\"width: 400px\" type=\"text\" name=\"_" + field.getName() + "\">");
						buffer.append("</td>");
					}
				}
				else if( field instanceof FieldPassword){
					
					//FieldPassword text = (FieldPassword)field;
					
					buffer.append("<td colspan=\"" + field.getLayoutWidth() + "\">");
					buffer.append("<input class=\"textInput\" style=\"width: 400px\" type=\"password\" name=\"_" + field.getName() + "\">");
					buffer.append("</td>");
				}
				else{
					buffer.append("<td colspan=\"" + field.getLayoutWidth() + "\">");
					buffer.append("<input class=\"textInput\" style=\"width: 400px\" type=\"text\" name=\"_" + field.getName() + "\">");
					buffer.append("</td>");
				}
			}
			
			buffer.append("</tr>");
		}
		
		return buffer.toString();
	}
}
