package net.lukemurphey.nsia.htmlInterface;

import org.apache.commons.lang.StringEscapeUtils;
import java.util.Hashtable;
import java.util.Enumeration;

public class HtmlOptionDialog extends HtmlContentProvider{

	public static final int DIALOG_INFORMATION = 0;
	public static final int DIALOG_QUESTION = 1;
	
	/*public static ContentDescriptor getHtml( WebConsoleConnectionDescriptor requestDescriptor, String title, String description, String[] hiddenVariables){
		
		return getHtml( requestDescriptor, tTitle, description, dialogType, "Console", "Return to Main Dashboard");
	}*/
	
	public static ContentDescriptor getHtml( WebConsoleConnectionDescriptor requestDescriptor, String title, String description, Hashtable<String, String> hiddenVariables, String[] buttons, String action, int messageboxIcon, String ajaxPath, String finalLocation){
		return getHtml( requestDescriptor, title, description, hiddenVariables, buttons, action, messageboxIcon, true, 0, ajaxPath, finalLocation);
	}
	
	public static ContentDescriptor getHtml( WebConsoleConnectionDescriptor requestDescriptor, String title, String description, Hashtable<String, String> hiddenVariables, String[] buttons, String action, int messageboxIcon, String ajaxPath){
		return getHtml( requestDescriptor, title, description, hiddenVariables, buttons, action, messageboxIcon, true, -1, ajaxPath, null);
	}
	
	public static ContentDescriptor getHtml( WebConsoleConnectionDescriptor requestDescriptor, String title, String description, Hashtable<String, String> hiddenVariables, String[] buttons, String action){
		return getHtml( requestDescriptor, title, description, hiddenVariables, buttons, action, DIALOG_QUESTION, false, -1, null, null);
	}
	
	public static ContentDescriptor getHtml( WebConsoleConnectionDescriptor requestDescriptor, String title, String description, Hashtable<String, String> hiddenVariables, String[] buttons, String action, int messageboxIcon){
		return getHtml( requestDescriptor, title, description, hiddenVariables, buttons, action, messageboxIcon, false, -1, null, null);
	}

	public static ContentDescriptor getHtml( WebConsoleConnectionDescriptor requestDescriptor, String title, String description, Hashtable<String, String> hiddenVariables, String[] buttons, String action, int messageboxIcon, int progressBarPercentage){
		return getHtml( requestDescriptor, title, description, hiddenVariables, buttons, action, messageboxIcon, true, progressBarPercentage, null, null);
	}
	
	public static ContentDescriptor getHtml( WebConsoleConnectionDescriptor requestDescriptor, String title, String description, Hashtable<String, String> hiddenVariables, String[] buttons, String action, int messageboxIcon, int progressBarPercentage, String refreshLocation){
		return getHtml( requestDescriptor, title, description, hiddenVariables, buttons, action, messageboxIcon, true, progressBarPercentage, refreshLocation, null);
	}
	
	private static ContentDescriptor getHtml( WebConsoleConnectionDescriptor requestDescriptor, String title, String description, Hashtable<String, String> hiddenVariables, String[] buttons, String action, int messageboxIcon, boolean showProgressBar, int progressBarPercentage, String refreshLocation, String ajaxPath){
		
		StringBuffer body = new StringBuffer();
		body.append( "&nbsp;<p>&nbsp;<p><table align=\"center\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"80%\" id=\"DialogContent\" summary=\"Question\">" );
		body.append( "<tr><td width=\"9%\" height=\"36\">&nbsp;</td><td width=\"1%\">&nbsp;</td><td width=\"90%\">&nbsp;</td></tr>"  );
		body.append( "<tr><td height=\"32\" align=\"right\" valign=\"top\" rowspan=\"2\">" );
		
		if( messageboxIcon ==  DIALOG_QUESTION ){
			body.append( "<img src=\"/32_Question\" alt=\"Question\">"  );
		}
		else{
			body.append( "<img src=\"/32_Information\" alt=\"Question\">"  );
		}
		
		body.append( "</td><td height=\"9\" valign=\"top\"></td><td><span class=\"Text_1\">"  );
		body.append( StringEscapeUtils.escapeHtml( title ) ).append("</span></td></tr>"  );

		body.append( "<tr><td height=\"34\" valign=\"top\">&nbsp;</td><td><span id=\"Description\">").append( StringEscapeUtils.escapeHtml( description )).append("</span></td></tr>");
		boolean usesAjax = (ajaxPath != null);
		
		progressBarPercentage = Math.min(progressBarPercentage, 100);
		
		if( usesAjax == true && ajaxPath != null ){
			//body.append("<script type=\"text/javascript\" src=\"/jtrace/jsTrace.js\"></script><script type=\"text/javascript\" src=\"/jtrace/dom-drag.js\"></script>");
			body.append("<script>var ajaxsrc=\"").append(ajaxPath).append("\";var finalLocation=\"").append( refreshLocation ).append("\";</script>");
			body.append("<script type=\"text/javascript\" src=\"/ajax.js\"></script>");
		}
		else if( showProgressBar && refreshLocation != null ){
			body.append("<script>function refresh(){ window.location.replace( '" + refreshLocation + "' ); }");
			body.append("setTimeout( \"refresh()\", 5*1000 )");
			body.append("</script>");
		}
		
		if( showProgressBar && progressBarPercentage >= 0 ){
			body.append( "<tr><td>&nbsp;</td><td height=\"34\" valign=\"top\">&nbsp;</td><td><div style=\"position:relative; width:370px; height:20px; padding:5px; background-image:url(/ProgressBarBlank); background-repeat: no-repeat;\">");
			body.append( "<div id=\"ProgressBar\" style=\"top:4; left:4; width:").append( (363 * progressBarPercentage ) / 100 ).append("px; height:9px; padding:5px; background-image:url(/ProgressBar); layer-background-image:url(/ProgressBar); background-repeat: repeat-x;\"></div></div></td></tr>");
		}
		else if( showProgressBar ){
			body.append( "<tr><td>&nbsp;</td><td height=\"34\" valign=\"top\">&nbsp;</td><td><img alt=\"Working...\" src=\"/ProgressBarAnimation\"></td></tr>");
		}
		
		body.append( "<tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;<br><form action=\"" + action + "\" method=\"post\">");
		
		// Create the hidden variables
		for (Enumeration<String> e = hiddenVariables.keys(); e.hasMoreElements();)
	    {
			Object key = e.nextElement();
			body.append("<input type=\"hidden\" name=\"").append( key ).append("\" value=\"").append(hiddenVariables.get( key )).append("\">");
	    }
		
		for (int c = 0; c < buttons.length; c++)
	    {
			body.append("<span align=\"center\"><input class=\"button\" type=\"submit\" value=\"").append( buttons[c] ).append("\" name=\"Selected\"></span>&nbsp;");
	    }

		body.append("</form></div></td></tr>");
		body.append( "<tr><td>&nbsp;<p>&nbsp;<p>&nbsp;<p></td></tr></table>");
		//body.append( "<tr><td height=\"46\">&nbsp;</td><td><p>&nbsp;</td><td><span class=\"SideNote\">Contact the System Administrator if assistance is necessary</span></td></tr>"  );
		
		return new ContentDescriptor( title, body);
	}
	
}
