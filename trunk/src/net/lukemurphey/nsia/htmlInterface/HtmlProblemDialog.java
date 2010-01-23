package net.lukemurphey.nsia.htmlInterface;

import org.apache.commons.lang.StringEscapeUtils;

public class HtmlProblemDialog extends HtmlContentProvider{
	
	public static final int DIALOG_WARNING = 0;
	public static final int DIALOG_ALERT = 1;
	
	public static ContentDescriptor getHtml( WebConsoleConnectionDescriptor requestDescriptor, String errorTitle, String errorDescription, int dialogType){
		
		return getHtml( requestDescriptor, errorTitle, errorDescription, dialogType, "Console", "Return to Main Dashboard");
	}
	
	public static ContentDescriptor getHtml( WebConsoleConnectionDescriptor requestDescriptor, String errorTitle, String errorDescription, int dialogType, String suggestedLink, String suggestLinkTitle){
		StringBuffer body = new StringBuffer();
		body.append( "&nbsp;<p>&nbsp;<p><table align=\"center\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"80%\" id=\"ErrorContent\" summary=\"Error Information\">" );
		body.append( "<tr><td width=\"9%\" height=\"36\">&nbsp;</td><td width=\"1%\">&nbsp;</td><td width=\"90%\">&nbsp;</td></tr>"  );
		body.append( "<tr><td height=\"32\" align=\"right\" valign=\"top\" rowspan=\"2\">" );
		
		if( dialogType == DIALOG_WARNING)
			body.append( "<img src=\"/32_Warning\" alt=\"Warning\">"  );
		else
			body.append( "<img src=\"/32_Alert\" alt=\"Alert\">"  );
		
		body.append( "</td><td height=\"9\" valign=\"top\"></td><td><span class=\"Text_2\">"  );
		body.append( StringEscapeUtils.escapeHtml( errorTitle ) ).append("</span></td></tr>"  );

		body.append( "<tr><td height=\"34\" valign=\"top\">&nbsp;</td><td>").append( StringEscapeUtils.escapeHtml( errorDescription )).append("<br>&nbsp;<br><a href=\"").append(suggestedLink).append("\"> [").append(suggestLinkTitle).append("]</a></td></tr><tr><td>&nbsp;<p>&nbsp;<p>&nbsp;<p></td></tr></table>"  );
		//body.append( "<tr><td height=\"46\">&nbsp;</td><td><p>&nbsp;</td><td><span class=\"SideNote\">Contact the System Administrator if assistance is necessary</span></td></tr>"  );
		
		return new ContentDescriptor( errorTitle, body);
	}

}
