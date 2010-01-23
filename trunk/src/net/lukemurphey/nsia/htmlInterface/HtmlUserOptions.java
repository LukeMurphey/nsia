package net.lukemurphey.nsia.htmlInterface;

import net.lukemurphey.nsia.GeneralizedException;
import net.lukemurphey.nsia.NoSessionException;
import net.lukemurphey.nsia.SessionStatus;

import org.apache.commons.lang.StringEscapeUtils;

public class HtmlUserOptions extends HtmlContentProvider{

	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor ) throws GeneralizedException, NoSessionException{
		return getHtml( requestDescriptor, null );
	}
	
	public static ContentDescriptor getHtml(WebConsoleConnectionDescriptor requestDescriptor, ActionDescriptor actionDesc ){
		
		if( requestDescriptor.sessionStatus != SessionStatus.SESSION_ACTIVE ){
			return new ContentDescriptor("UserPreferences", "&nbsp;");
		}
		else{
			StringBuffer output = new StringBuffer( 256 );
			
			output.append( "<div class=\"LightText\">Logged in as <u>");
			output.append( StringEscapeUtils.escapeHtml( requestDescriptor.username ) );
			output.append( "</u>" );
			output.append( createEntry( "[Logout]","Login?Action=Logout" ) );
			output.append( createEntry( "[Change Password]","UserManagement?Action=UpdatePassword&UserID=" + requestDescriptor.userId ) );
			output.append( "</div>" );
			
			return new ContentDescriptor("UserPreferences", output);
		}
		
	}
	
	private static StringBuffer createEntry( String text, String relativeLink ){
		StringBuffer output = new StringBuffer( 256 );
		
		output.append( "&nbsp;&nbsp;&nbsp;<a class=\"LightText\" href=\"");
		output.append( relativeLink + "\">" + text + "</a>" );
		
		return output;
	}
}
